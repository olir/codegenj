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

import java.io.PrintWriter;
import java.util.Map;

import de.serviceflow.codegenj.Node.Interface;

public class TemplateBlock extends Block {

	public TemplateBlock(Interface interfaceDef, Map<String, String> parameters, TemplateParser t, PrintWriter w) {
		super(null, interfaceDef, parameters, t, w);
	}

	public TemplateBlock(Node nodeDef, Map<String, String> parameters, TemplateParser t, PrintWriter w) {
		super(null, nodeDef, parameters, t, w);
	}

	public final boolean process() {
		System.out.println("----- "+super.getTemplate().location());
		return process(false);
	}

	protected void printResult(String token) {
		getWriter().print(parameterSubstitution(token)); 
	}
}
