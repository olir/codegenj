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
package de.serviceflow.codegenj.java;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.serviceflow.codegenj.Block;
import de.serviceflow.codegenj.CaseBlock;
import de.serviceflow.codegenj.ForBlock;
import de.serviceflow.codegenj.Naming;
import de.serviceflow.codegenj.Node;
import de.serviceflow.codegenj.ObjectiveBlockHandler;
import de.serviceflow.codegenj.ProcessingObjective;
import de.serviceflow.codegenj.Node.Interface;
import de.serviceflow.codegenj.Node.Interface.Member;
import de.serviceflow.codegenj.Node.Interface.Method;
import de.serviceflow.codegenj.Node.Interface.Property;
import de.serviceflow.codegenj.Node.Interface.Member.Annotation;
import de.serviceflow.codegenj.Node.Interface.Member.Arg;

public class CodeGenerationProcessing {
	private final Map<String, ObjectiveBlockHandler> blockMap = new HashMap<String, ObjectiveBlockHandler>();
	private final Map<String, String> wireToJavaTypeMap = new HashMap<String, String>();
	private final Map<String, String> wireToJniTypeMap = new HashMap<String, String>();
	private final Map<String, String> wireToCTypeMap = new HashMap<String, String>();
	private final Map<String, String> wireBasedJava2CAssignFormatMap = new HashMap<String, String>();
	private final Map<String, String> wireBasedJava2CCleanupFormatMap = new HashMap<String, String>();
	private final Map<String, String> wireBasedC2JavaAssignFormatMap = new HashMap<String, String>();

	private final Set<String> forbiddenIdentifiers = new HashSet<String>(
			Arrays.asList("abstract", "continue", "for", "new", "switch",
					"assert", "default", "goto", "package", "synchronized",
					"boolean", "do", "if", "private", "this", "break",
					"double", "implements", "protected", "throw", "byte",
					"else", "import", "public", "throws", "case", "enum",
					"instanceof", "return", "transient", "catch", "extends",
					"int", "short", "try", "char", "final", "interface",
					"static", "void", "class", "finally", "long", "strictfp",
					"volatile", "const", "float", "native", "super", "while"));

	public void initializeProcessing() {
		initializeWireToJavaTypeMapping();
		initializeWireToJniTypeMapping();
		initializeWireToCTypeMapping();
		initializeWireBasedJava2CAssignFormatMapping();
		initializeWireBasedJava2CCleanupFormatMapping();
		initializeWireBasedC2JavaAssignFormatMapping();

		initializeTemplateProcessingEvaluators();
	}

	/**
	 * @param parameters
	 * @param i
	 */
	public void processInterface(Map<String, String> parameters, Interface i) {
		parameters.put("interface.name", i.getName());
		parameters.put("interface.uname", Naming.javaUName(i.getName()));
		parameters.put("classname", i.getName());
		String pname = Naming.javaPName(i.getName());
		if (pname == null) {
			parameters.put("interface.pcode", "");
		} else {
			parameters.put("interface.pname", pname);
			parameters.put("interface.pcode", "package " + pname + ";\n");
		}

		parameters.put("interface.cname",
				Naming.cName(Naming.javaUName(i.getName())));
		parameters.put("interface.jname", Naming.jName(i.getName()));
	}

	private void initializeTemplateProcessingEvaluators() {
		/*
		 * Interface template parameterization
		 */
		blockMap.put(Interface.class.getName(), new ObjectiveBlockHandler() {
			@Override
			public void process(Block p, ProcessingObjective o,
					String blockArg, PrintWriter w) {
				Interface objective = (Interface) o;
				if (p instanceof ForBlock) {
					if ("method".equals(blockArg)) {
						boolean supported = true;
						for (Method m : objective.getMethods()) {
							((ForBlock) p).changeContext(m);
							p.getParameters().put("method.name", m.getName());
							p.getParameters().put("method.jname",
									Naming.javaMemberName(m.getName()));
							p.getParameters().put("method.cname",
									Naming.cName(m.getName()));
							p.getParameters()
									.put("method.sname",
											Naming.cName(m.getName()).replace(
													'_', '-'));
							String comment = m.getComment();
							if (comment == null)
								comment = "";
							p.getParameters().put("method.comment", comment);

							String javareturntype = "void";
							String jnireturntype = "void";
							StringBuffer javaargs = new StringBuffer();
							StringBuffer jniargs = new StringBuffer();
							StringBuffer cparams = new StringBuffer();
							StringBuffer cargmapcode = new StringBuffer();
							StringBuffer cleanupcode = new StringBuffer();
							StringBuffer creturncode = new StringBuffer();

							for (Arg a : m.getArgs()) {
								if ("out".equals(a.getDirection())) {
									String wireType = a.getType();
									int aDims = 0;
									while (wireType.startsWith("a")) {
										aDims++;
										wireType = wireType.substring(1);
									}
									javareturntype = wireToJavaTypeMap
											.get(wireType);
									String javareturnbasetype = javareturntype;
									jnireturntype = wireToJniTypeMap
											.get(wireType);
									String ctype = wireToCTypeMap.get(wireType);
									String cbasetype = ctype;
									for (int d = 0; d < aDims; d++) {
										javareturntype = javareturntype + "[]";
									}
									if (javareturntype == null
											|| jnireturntype == null || ctype==null) {
										supported = false;
										javareturntype = "?";
										jnireturntype = "?";
										ctype = "?";
										// throw new Error(
										// "Return wire type unsupperted: '"
										// + a.getType()
										// + "' at "
										// + objective.getName()
										// + "->"
										// + javaMemberName(m
										// .getName()));
									}
									if (aDims >= 2) {
										jnireturntype = "jobject";
										supported = false;
									} else if (aDims == 1) {
										if ("jstring".equals(jnireturntype)) {
											jnireturntype = "jobjectArray";
											supported = false;
										} else if ("jobject"
												.equals(jnireturntype)) {
											jnireturntype = "jobjectArray";
											supported = false;

										} else {
											jnireturntype = jnireturntype
													+ "Array";
											ctype = ctype + " *";
										}
									}
									cargmapcode.append(ctype);
									if (ctype.charAt(ctype.length() - 1) != '*')
										cargmapcode.append(' ');
									cargmapcode.append("_result;\n    ");

									cparams.append("&_result,\n        ");

									creturncode.append("    ");
									creturncode.append(jnireturntype);
									creturncode.append(" _jniresult;\n");

									if (aDims == 0) {
										creturncode
												.append("    _jniresult = _result;\n");
									} else if (aDims == 1) {
										creturncode
												.append("    gint size = sizeof(_result) / sizeof(");
										creturncode.append(cbasetype);
										creturncode.append(");\n");
										creturncode
												.append("    _jniresult = (*env)->New");
										creturncode.append(Character
												.toUpperCase(javareturnbasetype
														.charAt(0)));
										creturncode.append(javareturnbasetype
												.substring(1));
										creturncode
												.append("Array(env, size);\n");

										creturncode
												.append("    if (_jniresult == NULL) {\n");
										creturncode
												.append("        return NULL; // out of memory error thrown\n");
										creturncode.append("    }\n");
										creturncode.append("    (*env)->Set");
										creturncode.append(Character
												.toUpperCase(javareturnbasetype
														.charAt(0)));
										creturncode.append(javareturnbasetype
												.substring(1));
										creturncode
												.append("ArrayRegion(env, _jniresult, 0, size, _result);\n");
									}

									creturncode
											.append("    return _jniresult;\n");
								} else if ("in".equals(a.getDirection())) {
									String wireType = a.getType();
									int aDims = 0;
									while (wireType.startsWith("a")) {
										aDims++;
										wireType = wireType.substring(1);
									}
									String javaargbasetype = wireToJavaTypeMap
											.get(wireType);
									String javaargtype = javaargbasetype;
									String jniargtype = wireToJniTypeMap
											.get(wireType);
									String ctype = wireToCTypeMap.get(wireType);
									if (javaargbasetype == null
											|| ctype == null) {
										supported = false;
										javaargbasetype = "?";
										// throw new Error(
										// "Wire type unsupperted for arg : "
										// + a.getName()
										// + " '"
										// + a.getType()
										// + "' at "
										// + objective.getName()
										// + "->"
										// + javaMemberName(m
										// .getName()));
									}
									if (aDims >= 2
											|| (aDims == 1 && "jstring"
													.equals(jniargtype))) {
										jniargtype = "jobject";
										supported = false;
									} else if (aDims == 1) {
										javaargtype = javaargbasetype + " []";
										jniargtype = jniargtype + "Array";
									}

									if (javaargs.length() > 0)
										javaargs.append(", ");
									javaargs.append(javaargtype);
									javaargs.append(' ');
									javaargs.append(a.getName());

									jniargs.append(", ");
									jniargs.append(jniargtype);
									jniargs.append(' ');
									jniargs.append(a.getName());

									cargmapcode.append(ctype);
									if (ctype != null
											&& ctype.charAt(ctype.length() - 1) != '*')
										cargmapcode.append(' ');
									if (aDims == 1) {
										cargmapcode.append("*");
									}
									cargmapcode.append("c_arg_");
									cargmapcode.append(a.getName());
									cargmapcode.append(" = ");
									if (aDims == 0) {
										String code = wireBasedJava2CAssignFormatMap
												.get(wireType);
										if (code != null) {
											if ("?".equals(code)) {
												supported = false;
											}
											MessageFormat form = new MessageFormat(
													code);
											Object[] fArgs = { a.getName() };
											cargmapcode.append(form
													.format(fArgs));
										} else {
											// types compatible: direct
											// assignment
											cargmapcode.append(a.getName());
											cargmapcode.append(';');
										}
									} else if (aDims == 1) {
										cargmapcode.append("(*env)->Get");
										cargmapcode.append(Character
												.toUpperCase(javaargbasetype
														.charAt(0)));
										cargmapcode.append(javaargbasetype
												.substring(1));
										cargmapcode
												.append("ArrayElements(env, ");
										cargmapcode.append(a.getName());
										cargmapcode.append(", 0);");
									} else {
										supported = false;
									}
									cargmapcode.append("\n    ");

									if (isPrimitiveJniType(jniargtype))
										cparams.append("&");
									cparams.append("c_arg_");
									cparams.append(a.getName());
									cparams.append(",\n        ");

									if (aDims == 0) {
										String code = wireBasedJava2CAssignFormatMap
												.get(wireType);
										if (code != null) {
											if ("?".equals(code)) {
												supported = false;
											}
											MessageFormat form = new MessageFormat(
													code);
											Object[] fArgs = { a.getName() };
											cleanupcode.append("    ");
											cleanupcode.append(form
													.format(fArgs));
											cleanupcode.append("\n");
										} else {
											// types compatible: direct
											// assignment
											cleanupcode.append(a.getName());
											cleanupcode.append(";\n");
										}
									} else if (aDims == 1) {
										cleanupcode
												.append("    (*env)->Release");
										cleanupcode.append(Character
												.toUpperCase(javaargbasetype
														.charAt(0)));
										cleanupcode.append(javaargbasetype
												.substring(1));
										cleanupcode
												.append("ArrayElements(env, ");
										cleanupcode.append(a.getName());
										cleanupcode.append(", c_arg_");
										cleanupcode.append(a.getName());
										cleanupcode.append(", 0);\n");
									} else {
										supported = false;
									}
								}
							}
							p.getParameters().put("method.javareturntype",
									javareturntype);
							p.getParameters().put("method.javaargs",
									javaargs.toString());
							p.getParameters().put("method.jnireturntype",
									jnireturntype);
							p.getParameters().put("method.jniargs",
									jniargs.toString());
							p.getParameters().put("method.cparams",
									cparams.toString());
							p.getParameters().put("method.cargmapcode",
									cargmapcode.toString());
							p.getParameters().put("method.cleanupcode",
									cleanupcode.toString());
							p.getParameters().put("method.creturncode",
									creturncode.toString());
							if (supported)
								w.print(p.process(getContent()));
							else
								w.print("// Method signature unsupported: "
										+ m.getName());
						}
					} else if ("de.serviceflow.codegenj.CollectorAPI"
							.equals(blockArg)) {
						StringBuffer initCode = new StringBuffer();
						StringBuffer addCode = new StringBuffer();
						StringBuffer removeCode = new StringBuffer();
						String existingCode = p.getParameters().get("api.init");
						if (existingCode != null) {
							initCode.append(existingCode);
						}
						existingCode = p.getParameters().get("api.add");
						if (existingCode != null) {
							addCode.append(existingCode);
						}
						existingCode = p.getParameters().get("api.remove");
						if (existingCode != null) {
							removeCode.append(existingCode);
						}
						for (Entry<Interface, String> entry : objective
								.getInterfaceCollectorMap().entrySet()) {
							Interface i = entry.getKey();
							String mname = entry.getValue();
							int index = mname.indexOf('#');
							if (index < 0) {
								throw new Error(
										"Value of annotation de.serviceflow.codegenj.CollectorAPI must be 'class#method' or 'class#*method' for "
												+ p.getBlockname()
												+ " ("
												+ p.getClass().getName() + ")");
							}
							String templateclassname = p.getParameters().get(
									"classname");
							if (!templateclassname.equals(mname.substring(0,
									index))) {
								continue; // annotation not for current template
											// instance
							}
							mname = mname.substring(index + 1);
							p.getParameters().put("opmode",
									String.valueOf(!mname.startsWith("*")));
							if (mname.startsWith("*")) {
								mname = mname.substring(1);
							}
							String name = i.getName();
							p.getParameters().put("cinterfacename", name);
							index = name.lastIndexOf('.');
							if (index > 0)
								name = name.substring(index + 1);
							p.getParameters().put("cname", name);
							p.getParameters().put("vname", name.toLowerCase());
							p.getParameters().put("mname", mname);
							w.print(p.process(getContent()));

							initCode.append("    	");
							initCode.append(p.getParameters().get(
									"interface.name"));
							initCode.append("Proxy.initialize");
							initCode.append(name);
							initCode.append("Mapping();\n");

							addCode.append("    	if (\"");
							addCode.append(i.getName());
							addCode.append("\".equals(interfaceName)) { ");
							addCode.append(p.getParameters().get(
									"interface.name"));
							addCode.append("Proxy.add");
							addCode.append(name);
							addCode.append("Object(objectpath,  proxy); }\n");

							removeCode.append("    	if (\"");
							removeCode.append(i.getName());
							removeCode.append("\".equals(interfaceName)) { ");
							removeCode.append(p.getParameters().get(
									"interface.name"));
							removeCode.append("Proxy.remove");
							removeCode.append(name);
							removeCode
									.append("Object(objectpath,  proxy); }\n");
						}
						p.getParameters().put("api.init", initCode.toString());
						p.getParameters().put("api.add", addCode.toString());
						p.getParameters().put("api.remove",
								removeCode.toString());
					} else if ("properties".equals(blockArg)) {
						for (Property pr : objective.getProperties()) {
							((ForBlock) p).changeContext(pr);
							w.print(p.process(getContent()));
						}
						p.getParameters().remove("property.access");
					} else {
						throw new Error(
								"No processing objective for block argument '"
										+ blockArg + "' at " + p.getBlockname()
										+ " (" + p.getClass().getName() + ")");
					}
				} else {
					throw new Error("No processing objective for "
							+ p.getBlockname() + " (" + p.getClass().getName()
							+ ")");
				}
			}

		});

		/*
		 * ObjectHandler template parameterization
		 */
		blockMap.put(Node.class.getName(), new ObjectiveBlockHandler() {
			@Override
			public void process(Block p, ProcessingObjective o,
					String blockArg, PrintWriter w) {
				Node objective = (Node) o;
				if (p instanceof ForBlock) {
					if ("de.serviceflow.codegenj.CollectorAPI".equals(blockArg)) {
						StringBuffer initCode = new StringBuffer();
						StringBuffer addCode = new StringBuffer();
						StringBuffer removeCode = new StringBuffer();
						String existingCode = p.getParameters().get("api.init");
						if (existingCode != null) {
							initCode.append(existingCode);
						}
						existingCode = p.getParameters().get("api.add");
						if (existingCode != null) {
							addCode.append(existingCode);
						}
						existingCode = p.getParameters().get("api.remove");
						if (existingCode != null) {
							removeCode.append(existingCode);
						}
						for (Entry<Interface, String> entry : objective
								.getInterfaceCollectorMap().entrySet()) {
							Interface i = entry.getKey();
							String mname = entry.getValue();
							int index = mname.indexOf('#');
							if (index < 0) {
								throw new Error(
										"Value of annotation de.serviceflow.codegenj.CollectorAPI must be 'class#method' or 'class#*method' for "
												+ p.getBlockname()
												+ " ("
												+ p.getClass().getName() + ")");
							}
							String templateclassname = p.getParameters().get(
									"classname");
							if (!templateclassname.equals(mname.substring(0,
									index))) {
								continue; // annotation not for current template
											// instance
							}
							mname = mname.substring(index + 1);
							String name = i.getName();
							p.getParameters().put("interfacename", name);
							index = name.lastIndexOf('.');
							if (index > 0)
								name = name.substring(index + 1);
							p.getParameters().put("opmode",
									String.valueOf(!name.startsWith("*")));
							if (name.startsWith("*")) {
								name = name.substring(1);
							}
							p.getParameters().put("cname", name);
							p.getParameters().put("vname", name.toLowerCase());
							p.getParameters().put("mname", mname);
							w.print(p.process(getContent()));

							initCode.append("    	de.serviceflow.codegenj.ObjectManager.initialize");
							initCode.append(name);
							initCode.append("Mapping();\n");

							addCode.append("    	if (\"");
							addCode.append(i.getName());
							addCode.append("\".equals(interfaceName)) { de.serviceflow.codegenj.ObjectManager.add");
							addCode.append(name);
							addCode.append("Object(objectpath,  proxy); }");

							removeCode.append("    	if (\"");
							removeCode.append(i.getName());
							removeCode
									.append("\".equals(interfaceName)) { de.serviceflow.codegenj.ObjectManager.remove");
							removeCode.append(name);
							removeCode.append("Object(objectpath,  proxy); }");
						}
						p.getParameters().put("api.init", initCode.toString());
						p.getParameters().put("api.add", addCode.toString());
						p.getParameters().put("api.remove",
								removeCode.toString());
					}
				} else {
					throw new Error("No processing objective for "
							+ p.getBlockname() + " (" + p.getClass().getName()
							+ ")");
				}
			}
		});

		/*
		 * Property Sub-Block parameterization
		 */
		blockMap.put(Property.class.getName(), new ObjectiveBlockHandler() {
			@Override
			public void process(Block p, ProcessingObjective o,
					String blockArg, PrintWriter w) {
				Property objective = (Property) o;
				if ("ao".equals(objective.getType())) {
					for (Annotation a : objective.getAnnotations()) {
						if ("de.serviceflow.codegenj.CollectorAPI".equals(a
								.getName())) {
							return; // handled by annotation - skip property
						}
					}

				}
				if (p instanceof CaseBlock) {
					boolean supported = true;
					String casename = ((CaseBlock) p).getCaseName();
					String accessvalue = objective.getAccess();
					System.out.println("... property '" + objective.getName()
							+ "': casename : " + casename + "   accessvalue: "
							+ accessvalue);
					String suffix = "";
					if (forbiddenIdentifiers.contains(objective.getName()
							.toLowerCase())) {
						suffix = "Property";
					}
					String comment = objective.getComment();
					if (comment == null)
						comment = "";
					p.getParameters().put("method.comment", comment);
					if (("read".equals(casename) && ("read".equals(accessvalue) || "readwrite"
							.equals(accessvalue)))
							|| ("write".equals(casename) && ("write"
									.equals(accessvalue) || "readwrite"
									.equals(accessvalue)))) {
						p.getParameters().put("property.name",
								objective.getName());
						p.getParameters().put("property.type",
								objective.getType());
						p.getParameters().put("property.access", accessvalue);
						String javareturntype = "void";
						String jnireturntype = "void";
						StringBuffer javaargs = new StringBuffer();
						StringBuffer jniargs = new StringBuffer();
						StringBuffer cargmapcode = new StringBuffer();
						StringBuffer cleanupcode = new StringBuffer();
						StringBuffer creturncode = new StringBuffer();
						p.getParameters().put("method.cname",
								Naming.cName(objective.getName()));

						if ("read".equals(casename)) {
							p.getParameters().put("method.jname",
									"get" + objective.getName() + suffix);

							// ------------- GETTER -------------

							String wireType = objective.getType();
							int aDims = 0;
							while (wireType.startsWith("a")) {
								aDims++;
								wireType = wireType.substring(1);
							}
							javareturntype = wireToJavaTypeMap.get(wireType);
							if (javareturntype == null)
								supported = false;
							String javareturnbasetype = javareturntype;
							jnireturntype = wireToJniTypeMap.get(wireType);
							String ctype = wireToCTypeMap.get(wireType);
							String cbasetype = ctype;
							for (int d = 0; d < aDims; d++) {
								javareturntype = javareturntype + "[]";
							}
							if (javareturntype == null || jnireturntype == null) {
								supported = false;
								javareturntype = "?";
								jnireturntype = "?";
							}
							if (aDims >= 2) {
								jnireturntype = "jobject";
								supported = false;
							} else if (aDims == 1) {
								if ("jstring".equals(jnireturntype)) {
									jnireturntype = "jobjectArray";
									ctype = "char *const *";
									// supported = false;
								} else if ("jobject".equals(jnireturntype)) {
										jnireturntype = "jobjectArray";
										ctype = "void *";
										supported = false;
								} else {
									jnireturntype = jnireturntype + "Array";
									ctype = ctype + " *";
								}
							}

							if (supported) {
								cargmapcode.append(ctype);
								if (ctype.charAt(ctype.length() - 1) != '*')
									cargmapcode.append(' ');
								cargmapcode.append("_result = (");
								cargmapcode.append(ctype);
								cargmapcode.append(")");

								if (aDims == 0
										&& wireBasedC2JavaAssignFormatMap
												.get(wireType) != null) {
									creturncode
											.append("    if (_result==NULL)\n");
									creturncode
											.append("        return NULL;\n");
								}
								creturncode.append("    ");
								creturncode.append(jnireturntype);
								creturncode.append(" _jniresult = ");

								if (aDims == 0) {
									String code = wireBasedC2JavaAssignFormatMap
											.get(wireType);
									if (code != null) {
										if ("?".equals(code)) {
											supported = false;
										}
										MessageFormat form = new MessageFormat(
												code);
										Object[] fArgs = { "_result" };
										creturncode.append(form.format(fArgs));
									} else {
										// types compatible: direct
										// assignment
										creturncode.append("_result");
										creturncode.append(';');
									}
								} else if (aDims == 1) {
									if ("s".equals(wireType)) {
										creturncode.append("NULL;\n");

										creturncode
												.append("    int asize = 0;\n");
										creturncode.append("    int i;\n");

										creturncode
												.append("    for (i=0; _result[i] != NULL; i++)  asize++;\n");

										creturncode.append("    ");
										creturncode.append("_jniresult = ");
										creturncode
												.append("(jobjectArray) (*env) -> NewObjectArray(env, asize, (*env) -> FindClass(env, \"java/lang/String\"), NULL);\n");
										creturncode
												.append("    for(i=0;i<asize;i++) { (*env)->SetObjectArrayElement(env, _jniresult, i, (*env)->NewStringUTF(env, _result[i])); }");
									} else {
										creturncode.append("NULL;\n");

										creturncode
												.append("    int asize = 0;\n");
										creturncode.append("    int i;\n");

										creturncode
												.append("    for (i=0; _result[i] != (");
										creturncode.append(cbasetype);
										creturncode
												.append(")0; i++)  asize++;\n");

										creturncode.append("    ");
										creturncode.append("_jniresult = ");
										creturncode
												.append("(jobjectArray) (*env) -> New");
										creturncode.append(Character
												.toUpperCase(javareturnbasetype
														.charAt(0)));
										creturncode.append(javareturnbasetype
												.substring(1));
										creturncode
												.append("Array(env, asize);\n");

										creturncode.append("    (*env)->Set");
										creturncode.append(Character
												.toUpperCase(javareturnbasetype
														.charAt(0)));
										creturncode.append(javareturnbasetype
												.substring(1));
										creturncode
												.append("ArrayRegion(env, _jniresult, 0, asize, _result);");
									}

								} else {
									supported = false;
								}
								creturncode.append("\n");

								creturncode.append("    return _jniresult;\n");
							}

						} else {

							// ------------- SETTER -------------

							p.getParameters().put("method.jname",
									"set" + objective.getName() + suffix);

							String wireType = objective.getType();
							int aDims = 0;
							while (wireType.startsWith("a")) {
								aDims++;
								wireType = wireType.substring(1);
							}
							String javaargbasetype = wireToJavaTypeMap
									.get(wireType);
							String javaargtype = javaargbasetype;
							String jniargtype = wireToJniTypeMap.get(wireType);
							String ctype = wireToCTypeMap.get(wireType);
							if (javaargbasetype == null || ctype == null) {
								supported = false;
								javaargbasetype = "?";
								ctype = "?";
								// throw new Error(
								// "Wire type unsupperted for arg : "
								// + a.getName()
								// + " '"
								// + a.getType()
								// + "' at "
								// + objective.getName()
								// + "->"
								// + javaMemberName(m
								// .getName()));
							}
							if (aDims >= 2
									|| (aDims == 1 && "jstring"
											.equals(jniargtype))) {
								jniargtype = "jobject";
								supported = false;
							} else if (aDims == 1) {
								javaargtype = javaargbasetype + " []";
								jniargtype = jniargtype + "Array";
							}

							if (javaargs.length() > 0)
								javaargs.append(", ");
							javaargs.append(javaargtype);
							javaargs.append(' ');
							javaargs.append(objective.getName());

							jniargs.append(", ");
							jniargs.append(jniargtype);
							jniargs.append(' ');
							jniargs.append("value");

							cargmapcode.append(ctype);
							if (ctype.charAt(ctype.length() - 1) != '*')
								cargmapcode.append(' ');
							if (aDims == 1) {
								cargmapcode.append("*");
							}
							cargmapcode.append("c_arg_");
							cargmapcode.append("value");
							cargmapcode.append(" = ");
							if (aDims == 0) {
								String code = wireBasedJava2CAssignFormatMap
										.get(wireType);
								if (code != null) {
									if ("?".equals(code)) {
										supported = false;
									}
									MessageFormat form = new MessageFormat(code);
									Object[] fArgs = { "value" };
									cargmapcode.append(form.format(fArgs));
								} else {
									// types compatible: direct
									// assignment
									cargmapcode.append("value");
									cargmapcode.append(';');
								}
							} else if (aDims == 1) {
								cargmapcode.append("(*env)->Get");
								cargmapcode.append(Character
										.toUpperCase(javaargbasetype.charAt(0)));
								cargmapcode.append(javaargbasetype.substring(1));
								cargmapcode.append("ArrayElements(env, ");
								cargmapcode.append("value");
								cargmapcode.append(", 0);");
							} else {
								supported = false;
							}
							cargmapcode.append("\n    ");

							if (aDims == 0) {
								String code = wireBasedJava2CAssignFormatMap
										.get(wireType);
								if (code != null) {
									if ("?".equals(code)) {
										supported = false;
									}
									MessageFormat form = new MessageFormat(code);
									Object[] fArgs = { objective.getName() };
									cleanupcode.append("    ");
									cleanupcode.append(form.format(fArgs));
									cleanupcode.append("\n");
								} else {
									// types compatible: direct
									// assignment
									cleanupcode.append(objective.getName());
									cleanupcode.append(";\n");
								}
							} else if (aDims == 1) {
								cleanupcode.append("    (*env)->Release");
								cleanupcode.append(Character
										.toUpperCase(javaargbasetype.charAt(0)));
								cleanupcode.append(javaargbasetype.substring(1));
								cleanupcode.append("ArrayElements(env, ");
								cleanupcode.append(objective.getName());
								cleanupcode.append(", c_arg_");
								cleanupcode.append(objective.getName());
								cleanupcode.append(", 0);\n");
							} else {
								supported = false;
							}
						}

						p.getParameters().put("method.javareturntype",
								javareturntype);
						p.getParameters().put("method.javaargs",
								javaargs.toString());
						p.getParameters().put("method.jnireturntype",
								jnireturntype);
						p.getParameters().put("method.jniargs",
								jniargs.toString());
						p.getParameters().put("method.cargmapcode",
								cargmapcode.toString());
						p.getParameters().put("method.cleanupcode",
								cleanupcode.toString());
						p.getParameters().put("method.creturncode",
								creturncode.toString());
						if (supported)
							w.print(p.process(getContent()));
						else
							w.print("// property signature unsupported: "
									+ objective.getName());
					}
				} else {
					throw new Error("No processing objective for "
							+ p.getBlockname() + " (" + p.getClass().getName()
							+ ")");
				}
			}
		});

	}

	public Map<String, ObjectiveBlockHandler> getBlockMap() {
		return blockMap;
	}

	// https://dbus.freedesktop.org/doc/dbus-specification.html#type-system
	// y BYTE
	// b BOOLEAN
	// n INT16
	// q UINT16
	// i INT32
	// u UINT32
	// x INT64
	// t UINT64
	// d DOUBLE
	// h UNIX_FD
	// s String
	// o OBJECT_PATH
	// g SIGNATURE
	// a Array

	private void initializeWireToJavaTypeMapping() {
		wireToJavaTypeMap.put("y", "byte");
		wireToJavaTypeMap.put("b", "boolean");
		wireToJavaTypeMap.put("n", "short");
		wireToJavaTypeMap.put("q", "short");
		wireToJavaTypeMap.put("i", "int");
		wireToJavaTypeMap.put("u", "int");
		wireToJavaTypeMap.put("x", "long");
		wireToJavaTypeMap.put("t", "long");
		wireToJavaTypeMap.put("d", "double");
		wireToJavaTypeMap.put("h", "int");
		wireToJavaTypeMap.put("s", "String");
		wireToJavaTypeMap.put("o", "Object");
		// wireToJavaTypeMap.put("g", "Object");
		// wireToJavaTypeMap.put("e{}", "Object");
	}

	private void initializeWireToJniTypeMapping() {
		wireToJniTypeMap.put("y", "jbyte");
		wireToJniTypeMap.put("b", "jboolean");
		wireToJniTypeMap.put("n", "jshort");
		wireToJniTypeMap.put("q", "jshort");
		wireToJniTypeMap.put("i", "jint");
		wireToJniTypeMap.put("u", "jint");
		wireToJniTypeMap.put("x", "jlong");
		wireToJniTypeMap.put("t", "jlong");
		wireToJniTypeMap.put("d", "jdouble");
		wireToJniTypeMap.put("h", "jint");
		wireToJniTypeMap.put("s", "jstring");
		wireToJniTypeMap.put("o", "jobject");
		// wireToJniTypeMap.put("g", "jobject");
	}

	private void initializeWireToCTypeMapping() {
		wireToCTypeMap.put("y", "gchar");
		wireToCTypeMap.put("b", "gboolean");
		wireToCTypeMap.put("n", "gint16");
		wireToCTypeMap.put("q", "guint16");
		wireToCTypeMap.put("i", "gint32");
		wireToCTypeMap.put("u", "guint32");
		wireToCTypeMap.put("x", "gint64");
		wireToCTypeMap.put("t", "guint64");
		wireToCTypeMap.put("d", "gdouble");
		wireToCTypeMap.put("h", "guint32");
		wireToCTypeMap.put("s", "const gchar *");
		wireToCTypeMap.put("o", "const gchar *");
		wireToCTypeMap.put("g", "?");
	}

	private void initializeWireBasedJava2CAssignFormatMapping() {
		wireBasedJava2CAssignFormatMap.put("s",
				"(*env)->GetStringUTFChars(env, {0}, NULL);");
		wireBasedJava2CAssignFormatMap.put("o",
				"(*env)->GetStringUTFChars(env, {0}, NULL);"); // g_variant_new_object_path((*env)->GetStringUTFChars(env,
																// {0}, NULL));
																// g_print(\"g_variant_is_object_path
																// : %s\",
																// (*env)->GetStringUTFChars(env,
																// {0}, NULL));
		wireBasedJava2CAssignFormatMap.put("g", "?");
	}

	private void initializeWireBasedJava2CCleanupFormatMapping() {
		wireBasedJava2CCleanupFormatMap.put("s",
				"(*env)->ReleaseStringUTFChars(env, {0}, c_arg_{0});");
		wireBasedJava2CCleanupFormatMap.put("g", "; // ?");
	}

	private void initializeWireBasedC2JavaAssignFormatMapping() {
		wireBasedC2JavaAssignFormatMap.put("s",
				"(*env)->NewStringUTF(env, {0});");
		wireBasedC2JavaAssignFormatMap.put("o", "?");
		wireBasedC2JavaAssignFormatMap.put("g", "?");
	}

	/**
	 * @param jniargtype
	 * @return
	 */
	private boolean isPrimitiveJniType(String jniargtype) {
		return !("jobject".equals(jniargtype) || "jstring".equals(jniargtype));
	}

}