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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Map;

import de.serviceflow.codegenj.Naming;
import de.serviceflow.codegenj.Node;
import de.serviceflow.codegenj.TemplateBlock;
import de.serviceflow.codegenj.TemplateParser;
import de.serviceflow.codegenj.Node.Interface;
import de.serviceflow.codegenj.Node.Interface.Member;
import de.serviceflow.codegenj.Node.Interface.Member.Annotation;

/**
 * Java Code Generation processor executed fir each interface. Search f�r block
 * tokens like ###for x### .... ###end###
 */
public class InterfaceSourceGenerator {
	private final Interface interfaceDef;
	private final String destination;
	private final Map<String, String> parameters;
	private String fileBaseName = null;
	private boolean isSkeleton = false;

	public InterfaceSourceGenerator(Interface interfaceDef,
			Map<String, String> parameters, String destination) {
		this.interfaceDef = interfaceDef;
		this.parameters = parameters;
		this.destination = destination;
	}

	public void open() {
		String uname = parameters.get("interface.uname");
		fileBaseName = Naming.javaClassName(uname);

		for (Annotation a : interfaceDef.getAnnotations()) {
			if ("de.serviceflow.codegenj.SkeletonAPI".equals(a.getName())) {
				isSkeleton = true;
			}
		}
	}

	public void generate() {
		TemplateParser t;
		PrintWriter w;

		String ppath = packagePath();
		String dir = destination + "/generated";
		new File(dir + '/' + ppath).mkdirs();
		String jnidir = destination + "/jni";
		new File(jnidir).mkdirs();
		String jnihdir = jnidir + "/include";
		new File(jnihdir).mkdirs();

		/**
		 * Create Java Interface file
		 */

		t = new TemplateParser("template/java/interface_java.txt");
		t.open();
		try {
			w = new PrintWriter(new FileOutputStream(dir + "/" + ppath
					+ fileBaseName + ".java"));
		} catch (FileNotFoundException e) {
			throw new Error("Can't create output file: "
					+ e.getLocalizedMessage());
		}

		try {
			new TemplateBlock(interfaceDef, parameters, t, w).process();
		} finally {
			w.close();
		}
		t.close();

		/**
		 * Create Java Proxy/Server/Skeleton file
		 */
		if (isSkeleton) {
			t = new TemplateParser("template/java/skeleton_java.txt");
			t.open();
			try {
				w = new PrintWriter(new FileOutputStream(dir + "/" + ppath
						+ fileBaseName + "CB.java"));
			} catch (FileNotFoundException e) {
				throw new Error("Can't create output file: "
						+ e.getLocalizedMessage());
			}

			try {
				new TemplateBlock(interfaceDef, parameters, t, w).process();
			} finally {
				w.close();
			}
			t.close();

			/**
			 * Create .c file. The .h file can be generated from compiled class
			 * file by javah tool.
			 */

			t = new TemplateParser("template/java/skeleton_c.txt");
			t.open();
			try {
				w = new PrintWriter(new FileOutputStream(jnidir + "/"
						+ fileBaseName + "CB.c"));
			} catch (FileNotFoundException e) {
				throw new Error("Can't create output file: "
						+ e.getLocalizedMessage());
			}
			w.println("// " + interfaceDef.getName() + "CB.c");
			try {
				new TemplateBlock(interfaceDef, parameters, t, w).process();
			} finally {
				w.close();
			}
			t.close();
		} else {
			t = new TemplateParser("template/java/interfaceproxy_java.txt");
			t.open();
			try {
				w = new PrintWriter(new FileOutputStream(dir + "/" + ppath
						+ fileBaseName + "Proxy.java"));
			} catch (FileNotFoundException e) {
				throw new Error("Can't create output file: "
						+ e.getLocalizedMessage());
			}
			try {
				new TemplateBlock(interfaceDef, parameters, t, w).process();
			} finally {
				w.close();
			}
			t.close();

			
			t = new TemplateParser("template/java/interfaceskeleton_java.txt");
			t.open();
			try {
				w = new PrintWriter(new FileOutputStream(dir + "/" + ppath
						+ fileBaseName + "Skeleton.java"));
			} catch (FileNotFoundException e) {
				throw new Error("Can't create output file: "
						+ e.getLocalizedMessage());
			}
			try {
				new TemplateBlock(interfaceDef, parameters, t, w).process();
			} finally {
				w.close();
			}
			t.close();


			/**
			 * Create .c file. The .h file can be generated from compiled class
			 * file by javah tool.
			 */

			t = new TemplateParser("template/java/interfaceproxy_c.txt");
			t.open();
			try {
				w = new PrintWriter(new FileOutputStream(jnidir + "/"
						+ fileBaseName + "Proxy.c"));
			} catch (FileNotFoundException e) {
				throw new Error("Can't create output file: "
						+ e.getLocalizedMessage());
			}
			w.println("// " + interfaceDef.getName() + "Proxy.c");
			try {
				new TemplateBlock(interfaceDef, parameters, t, w).process();
			} finally {
				w.close();
			}
			t.close();
			
			t = new TemplateParser("template/java/interfaceskeleton_c.txt");
			t.open();
			try {
				w = new PrintWriter(new FileOutputStream(jnidir + "/"
						+ fileBaseName + "Skeleton.c"));
			} catch (FileNotFoundException e) {
				throw new Error("Can't create output file: "
						+ e.getLocalizedMessage());
			}
			w.println("// " + interfaceDef.getName() + "Skeleton.c");
			try {
				new TemplateBlock(interfaceDef, parameters, t, w).process();
			} finally {
				w.close();
			}
			t.close();
			
		}

	}

	private String packagePath() {
		String pname = parameters.get("busname");
		if (pname == null)
			return "";

		StringBuffer packagePath = new StringBuffer();
		int fromIndex = 0;
		for (int index = pname.indexOf('.', fromIndex); index >= 0; fromIndex = index + 1, index = pname
				.indexOf('.', fromIndex)) {
			packagePath.append(pname.substring(fromIndex, index));
			packagePath.append('/');
		}
		packagePath.append(pname.substring(fromIndex));
		packagePath.append('/');
		return packagePath.toString();
	}

	public void close() {
		// String objectfilter = parameters.get("objectmanager.objectfilter");

		for (Annotation a : interfaceDef.getAnnotations()) {
			if ("de.serviceflow.codegenj.CollectorAPI".equals(a.getName())) {
				String apiValue = a.getValue();
				int sep = apiValue.indexOf('#');
				if (sep < 0) {
					throw new Error(
							"Syntax Error at annotation de.serviceflow.codegenj.CollectorAPI for interface "
									+ interfaceDef.getName());
				}
				String apiClass = apiValue.substring(0, sep);
				String apiMethod = apiValue.substring(sep + 1);
				if (ObjectManagerGenerator.OM_INAME.equals(apiClass)) {
					// if (objectfilter.length()>0)
					// objectfilter = objectfilter + ",";
					// objectfilter = objectfilter + interfaceDef.getName();
				}
			}
		}

	}

}
