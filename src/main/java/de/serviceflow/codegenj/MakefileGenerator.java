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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Java Code Generation processor executed fir each interface. Search f�r block
 * tokens like ###for x### .... ###end###
 */
public class MakefileGenerator {
	private final Node nodeDef;
	private final String destination;
	private final Map<String, String> parameters;
	private String fileBaseName = null;

	public MakefileGenerator(Node nodeDef, Map<String, String> parameters, String destination) {
		this.nodeDef = nodeDef;
		this.parameters = parameters;
		this.destination = destination;
	}

	public void open() {
		fileBaseName = "Makefile";
	}

	public void generate() {
		TemplateParser t;
		PrintWriter w;

		String jnidir = destination + "/jni";
		new File(jnidir).mkdirs();

		t = new TemplateParser("template/Makefile.txt");
		t.open();
		try {
			w = new PrintWriter(new FileOutputStream(jnidir + "/" + fileBaseName));
		} catch (FileNotFoundException e) {
			throw new Error("Can't create output file: " + e.getLocalizedMessage());
		}

		try {
			new TemplateBlock(nodeDef, parameters, t, w).process();
		} finally {
			w.close();
		}
		t.close();
	}

	public void close() {
	}

}
