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

import de.serviceflow.codegenj.Node;
import de.serviceflow.codegenj.TemplateBlock;
import de.serviceflow.codegenj.TemplateParser;

/**
 * Java Code Generation processor executed fir each interface. Search f�r block
 * tokens like ###for x### .... ###end###
 */
public class ObjectManagerGenerator {
	public static final String OM_INAME = "de.serviceflow.codegenj.ObjectManager";
	
	private final Node nodeDef;
	private final String destination;
	private final Map<String, String> parameters;
	private String fileBaseName = null;

	public ObjectManagerGenerator(Node nodeDef, Map<String, String> parameters, String destination) {
		this.nodeDef = nodeDef;
		this.parameters = parameters;
		this.destination = destination;
	}

	public void open() {
		fileBaseName = "ObjectManager";
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


		/*
		 * ObjectManager.c
		 */
		t = new TemplateParser("template/java/ObjectManager_c.txt");
		t.open();
		try {
			w = new PrintWriter(new FileOutputStream(jnidir + "/" + fileBaseName + ".c"));
		} catch (FileNotFoundException e) {
			throw new Error("Can't create output file: " + e.getLocalizedMessage());
		}

		try {
			new TemplateBlock(nodeDef, parameters, t, w).process();
		} finally {
			w.close();
		}
		t.close();
		
		/*
		 * ObjectManager.java
		 */
		t = new TemplateParser("template/java/ObjectManager_java.txt");
		t.open();
		try {
			w = new PrintWriter(new FileOutputStream(dir + "/" + ppath + fileBaseName + ".java"));
		} catch (FileNotFoundException e) {
			throw new Error("Can't create output file: " + e.getLocalizedMessage());
		}
		parameters.put("classname", "de.serviceflow.codegenj.ObjectManager");
		try {
			new TemplateBlock(nodeDef, parameters, t, w).process();
		} finally {
			w.close();
		}
		t.close();
		parameters.remove("classname");
		
	}
	
	private String packagePath() {
		String pname = OM_INAME.substring(0, OM_INAME.lastIndexOf('.'));
		if (pname==null)
			return "";
		
		StringBuffer packagePath = new StringBuffer();
		int fromIndex = 0;
		for (int index = pname.indexOf('.', fromIndex); index>=0; fromIndex=index+1, index = pname.indexOf('.', fromIndex)) {
			packagePath.append(pname.substring(fromIndex, index));
			packagePath.append('/');
		}
		packagePath.append(pname.substring(fromIndex));
		packagePath.append('/');
		return packagePath.toString();
	}

	

	public void close() {
	}

}
