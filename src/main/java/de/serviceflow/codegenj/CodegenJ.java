/*
 * Copyright 2016 Oliver Rode
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.serviceflow.codegenj;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import de.serviceflow.codegenj.Node.Interface;
import de.serviceflow.codegenj.Node.Interface.Property;
import de.serviceflow.codegenj.Node.Interface.Member.Annotation;
import de.serviceflow.codegenj.java.CodeGenerationProcessing;
import de.serviceflow.codegenj.java.InterfaceSourceGenerator;
import de.serviceflow.codegenj.java.MakefileGenerator;
import de.serviceflow.codegenj.java.ObjectManagerGenerator;

/**
 * Generate Java Code base on XML Introspection Data Format and gdbus-codegen.
 * 
 * @see <a href=
 *      "https://dbus.freedesktop.org/doc/dbus-specification.html">dbus-specification</a>
 * @see <a href=
 *      "https://developer.gnome.org/gio/stable/gdbus-codegen.html">gdbus-codegen</a>
 */
public class CodegenJ {

	private static CodegenJ instance = null;

	public static void main(String[] args) {

		new CodegenJ().run(args);
	}

	public final CodeGenerationProcessing initHelper = new CodeGenerationProcessing();
	private String xmlFile;
	private String destination = "target";
	private String busname = "my.dum";
	private String iprefix = "my.dum.";
	private String library = "dummydbus";

	public CodegenJ() {
		instance = this;
	}

	public void run(String[] args) {
		String xmlfile;
		try {
			initHelper.initializeProcessing();
			xmlfile = parseOptions(args);
			Node rootNode = parseXML(xmlfile);
			generateCode(rootNode, null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (JAXBException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (SAXException e) {
			System.err.println(e.getLocalizedMessage());
			System.exit(-1);
		} catch (XMLStreamException e) {
			System.err.println(e.getLocalizedMessage());
			System.exit(-1);
		}
	}

	public String parseOptions(String[] args) throws FileNotFoundException {
		if (args.length < 1 || !stripOptions(args)) {
			System.err.println("Usage: CodegenJ [-d <destination>] -l <library> -b <busname> -i <interfaceprefix> <xmlfile>");
			System.exit(-1);
		}

		return xmlFile;
	}

	private boolean stripOptions(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				if ("-d".equals(args[i])) {
					if (++i == args.length) {
						return false;
					}
					destination = args[i];
				}
				else if ("-b".equals(args[i])) {
					if (++i == args.length) {
						return false;
					}
					busname = args[i];
				}
				else if ("-i".equals(args[i])) {
					if (++i == args.length) {
						return false;
					}
					iprefix = args[i];
				}
				else if ("-l".equals(args[i])) {
					if (++i == args.length) {
						return false;
					}
					library = args[i];
				}
				else
					return false;
			} else if (xmlFile != null) {
				return false;
			} else {
				xmlFile = args[i];
			}
		}
		return xmlFile != null;
	}

	public Node parseXML(String xmlfile) throws JAXBException, SAXException,
			XMLStreamException, FileNotFoundException {
		SchemaFactory schemaFactory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		Source schemaSource = new StreamSource(getClass().getResourceAsStream(
				"/" + "dbus.xsd"), getClass().getResource("/" + "dbus.xsd")
				.toString());

		Schema schema = schemaFactory.newSchema(schemaSource);

		JAXBContext jaxbContext = JAXBContext.newInstance(Node.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(new DBusValidationEventHandler());

		Node node = (Node) unmarshaller.unmarshal(new FileInputStream(xmlfile));

		XMLInputFactory xif = XMLInputFactory.newFactory();
		XMLStreamReader xsr = xif.createXMLStreamReader(new FileInputStream(
				xmlfile));

		String comment = null;
		Stack<ProcessingObjective> xmlStack = new Stack<ProcessingObjective>();
		ProcessingObjective currentObject = null;
		while (xsr.hasNext()) {
			if (xsr.getEventType() == XMLStreamConstants.COMMENT) {
				comment = xsr.getText();
			} else if (xsr.getEventType() == XMLStreamConstants.START_ELEMENT) {
				xmlStack.push(currentObject);
				String name = xsr.getName().toString();
				if (name.equals("node")) {
					currentObject = node;
				} else {
					if (currentObject != null) {
						currentObject = currentObject
								.getProcessingObjective(name);
					}
				}
				if (currentObject != null) {
					currentObject.startScan();
				}
				if (comment != null && currentObject != null) {
					currentObject.setComment(comment);
				}
				comment = null;
			} else if (xsr.getEventType() == XMLStreamConstants.END_ELEMENT) {
				if (currentObject != null) {
					currentObject.endScan();
				}
				currentObject = xmlStack.pop();
			}
			xsr.next();
		}

		return node;
	}

	/**
	 * Major code generation flow.
	 */
	public void generateCode(Node node, Node parent) {
		Map<String, String> parameters = new HashMap<String, String>();

		parameters.put("node.name", node.getName());
		parameters.put("busname", busname);
		parameters.put("iprefix", iprefix);
		parameters.put("library", library);
		StringBuffer headerFiles = new StringBuffer();
		StringBuffer classlist = new StringBuffer();
		StringBuffer objFiles = new StringBuffer();

		/*
		 * prepare collector generation
		 */
		for (Interface i : node.getInterfaces()) {
			for (Annotation a : i.getAnnotations()) {
				if ("de.serviceflow.codegenj.CollectorAPI".equals(a.getName())) {
					String mname = a.getValue();
					int index = mname.indexOf('#');
					if (index < 0) {
						throw new Error(
								"Value of annotation de.serviceflow.codegenj.CollectorAPI must be 'class#method' at interface "
										+ i.getName());
					}
					String targettemplate = mname.substring(0, index);

					String templateclassname = "de.serviceflow.codegenj.ObjectManager";
					if (templateclassname.equals(targettemplate)) {
						node.getInterfaceCollectorMap().put(i, a.getValue());
						continue;
					}
					for (Interface i2 : node.getInterfaces()) {
						templateclassname = i2.getName();
						if (templateclassname.equals(targettemplate)) {
							i2.getInterfaceCollectorMap().put(i, a.getValue());
							templateclassname = null;
							break;
						}
					}
					if (templateclassname != null) {
						throw new Error(
								"Value of annotation de.serviceflow.codegenj.CollectorAPI contains a unkown target interface class at interface "
										+ i.getName()
										+ ": "
										+ templateclassname);
					}
				}
			}
			for (Property p : i.getProperties()) {
				for (Annotation a : p.getAnnotations()) {
					if ("de.serviceflow.codegenj.CollectorAPI".equals(a.getName())) {
						if (!"read".equals(p.getAccess())) {
							throw new Error(
									"property-access at annotation de.serviceflow.codegenj.CollectorAPI must be 'read' at interface "
											+ i.getName()
											+ " at property "
											+ p.getName());
						}
						if (!"ao".equals(p.getType())) {
							throw new Error(
									"property-type at annotation de.serviceflow.codegenj.CollectorAPI must be 'ao' at interface "
											+ i.getName()
											+ " at property "
											+ p.getName());
						}
						String mname = a.getValue();
						int index = mname.indexOf('#');
						if (index >= 0) {
							throw new Error(
									"Value of annotation de.serviceflow.codegenj.CollectorAPI must be 'interfacename' at interface "
											+ i.getName()
											+ " at property "
											+ p.getName());
						}

						String targettemplate = mname;
						for (Interface i2 : node.getInterfaces()) {
							String templateclassname = i2.getName();
							if (templateclassname.equals(targettemplate)) {
								i.getInterfaceCollectorMap().put(i2,
										i.getName() + "#*get" + p.getName());
								// throw new Error(i2.getName()+" .... " +
								// i.getName()+"#get"+p.getName());
								targettemplate = null;
								break;
							}
						}

						if (targettemplate != null) {
							throw new Error(
									"Value of annotation de.serviceflow.codegenj.CollectorAPI contains a unkown target interface class at interface "
											+ i.getName()
											+ ": "
											+ targettemplate);
						}
					}
				}
			}
		}

		classlist.append("de.serviceflow.codegenj.ObjectManager");

		/*
		 * Create Interface-JNI Layer with .c and .java files
		 */
		for (Interface i : node.getInterfaces()) {
			boolean isSkeleton = false;
			for (Annotation a : i.getAnnotations()) {
				if ("de.serviceflow.codegenj.SkeletonAPI".equals(a.getName())) {
					isSkeleton = true;
				}
			}
			
			initHelper.processInterface(parameters, i);

			InterfaceSourceGenerator itp = new InterfaceSourceGenerator(i,
					parameters, destination);
			// itp.addBlockProccessor(new ForBlock());

			itp.open();
			itp.generate();
			itp.close();

			headerFiles.append(' ');
			headerFiles.append(parameters.get("interface.jname"));
			headerFiles.append(".h");

			// de.serviceflow.codegenj.ObjectManager org.bluez.Adapter1 org.bluez.Device1 org.bluez.GattCharacteristic1 org.bluez.GattDescriptor1 org.bluez.GattService1 org.bluez.Agent1 org.bluez.AgentManager1
			classlist.append(' ');
			classlist.append(parameters.get("interface.name"));
			
			if (isSkeleton) {
				classlist.append(' ');		
				classlist.append(parameters.get("interface.name")+"CB");
				
				objFiles.append(' ');
				objFiles.append(parameters.get("interface.uname"));
				objFiles.append("CB.o");
			}
			else {
				classlist.append(' ');		
				classlist.append(parameters.get("interface.name")+"Proxy");
				classlist.append(' ');
				classlist.append(parameters.get("interface.name")+"Skeleton");
				
				objFiles.append(' ');
				objFiles.append(parameters.get("interface.uname"));
				objFiles.append("Proxy.o");
				objFiles.append(' ');
				objFiles.append(parameters.get("interface.uname"));
				objFiles.append("Skeleton.o");
			}
		}
		parameters.remove("interface.name");

		// recursion with subnodes (optional)
		for (Node c : node.getChilds()) {
			generateCode(c, node);
		}

		parameters.remove("node.name");

		/*
		 * Create Makefile for library loaded by JNI
		 */

		parameters.put("headerfiles", headerFiles.toString());
		parameters.put("classlist", classlist.toString());
		parameters.put("objfiles", objFiles.toString());
		String xml = xmlFile;
		if (xml.lastIndexOf('/')>=0)
			xml = xml.substring(xml.lastIndexOf('/')+1);
		parameters.put("xmlfile", xml);
		
		MakefileGenerator mg = new MakefileGenerator(node, parameters,
				destination);
		mg.open();
		mg.generate();
		mg.close();

		/*
		 * Create Makefile for library loaded by JNI
		 */

		ObjectManagerGenerator omg = new ObjectManagerGenerator(node,
				parameters, destination);
		omg.open();
		omg.generate();
		omg.close();

	}

	public static CodegenJ getInstance() {
		return instance;
	}

	public class DBusValidationEventHandler implements ValidationEventHandler {
		public boolean handleEvent(ValidationEvent event) {
			System.out.println("*** Validation of XML input failed ***");
			System.out.println("MESSAGE:  " + event.getMessage());

			return false;
		}
	}

	public ObjectiveBlockHandler findProcessing(
			String processingObjectiveClassName) {
		return initHelper.getBlockMap().get(processingObjectiveClassName);
	}

}