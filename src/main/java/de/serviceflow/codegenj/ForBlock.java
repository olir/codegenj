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
import java.io.StringWriter;
import java.util.Map;

public class ForBlock extends Block {

	private StringWriter sw = new StringWriter();
	private PrintWriter w2 = new PrintWriter(sw);
	private String iterableName;

	private final PrintWriter parentWriter;

	private ProcessingObjective currentContext;
	
	public ForBlock(ProcessingObjective context,
			Map<String, String> parameters, TemplateParser t, PrintWriter w,
			String iterableName) {
		super("for", context, parameters, t, w);

		sw = new StringWriter();
		w2 = new PrintWriter(sw);
		this.iterableName = iterableName;

		parentWriter = w;
		setWriter(w2);
		
		currentContext = context;
	}

	public boolean process(boolean cachemode) {
		boolean valid = super.process(cachemode);
		w2.close();

		String blockContent = sw.toString();

		if (valid) {
			getContext()
					.process(this, iterableName, parentWriter, blockContent); 
		}
		return valid;
	}
	
	public void changeContext(ProcessingObjective newContext) {
		currentContext = newContext;
	}
	
	public ProcessingObjective getContext() {
		return currentContext;
	}
	
}
