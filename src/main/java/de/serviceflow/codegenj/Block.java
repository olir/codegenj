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

/**
 * Common Block Processing.
 */
public abstract class Block {
	private final String blockname;
	private final ProcessingObjective context;
	private final Map<String, String> parameters;
	private TemplateParser t;
	private PrintWriter w;

	protected Block(String blockname, ProcessingObjective context,
			Map<String, String> parameters, TemplateParser t, PrintWriter w) {
		this.blockname = blockname;
		this.context = context;
		this.parameters = parameters;
		this.t = t;
		this.w = w;
	}

	/**
	 * 
	 * @return true if end token, false of EOF.
	 */
	public boolean process() {
		return process(false);
	}

	/**
	 * 
	 * @return true if end token, false of EOF.
	 */
	public String process(String content) {
		System.out.println("**** process start "+content.substring(0, Math.min(15, content.length()))+"...");

		StringWriter sw = new StringWriter();
		PrintWriter w2 = new PrintWriter(sw);
		PrintWriter parentWriter = w;
		setWriter(w2);

		TemplateParser parentParser = t;
		t = new TemplateBlockParser(parentParser, content);
		t.open();
		boolean valid = process(false);
		t.close();
		
		w2.close();
		w = parentWriter;
		t = parentParser;
		
		return parameterSubstitution(sw.toString());
	}
	
	/**
	 * 
	 * @return true if end token, false if EOF.
	 */
	public boolean process(boolean cachemode) {
		System.out.println("*** process start "+blockname+" "+cachemode);
		boolean inBlockSyntax = false;
		String token;
		int cachedBlockLevel = 0;

		while ((token = t.readNext()) != TemplateParser.EOF_TOKEN) {
			if (token == TemplateParser.EOL_TOKEN) {
				w.println("");
			} else if (token == TemplateParser.BLOCK_TOKEN) {
				inBlockSyntax = !inBlockSyntax;
			} else if (inBlockSyntax) {
				inBlockSyntax = !inBlockSyntax;
				if ("end".equals(token)) {
					cachedBlockLevel--;
					if (cachedBlockLevel<0)
						break; // end processing, return to parent
					w.print(TemplateParser.BLOCK_TOKEN);
					w.print(token);
					w.print(TemplateParser.BLOCK_TOKEN);
				} else if (cachemode) {
					cachedBlockLevel++;
					w.print(TemplateParser.BLOCK_TOKEN);
					w.print(token);
					w.print(TemplateParser.BLOCK_TOKEN);
				} else {
					String[] args = token.split("\\s+");
					if (args.length < 1)
						throw new Error("Block Command with no args at "
								+ t.location() + ": '" + token + "'");
					String blockType = args[0];

					if ("for".equals(blockType)) {
						if (args.length != 2) {
							throw new Error("for block argument invalid: '"
									+ token + "' at " + t.location());
						}
						if (!new ForBlock(getContext(), parameters, t, w, args[1])
								.process(true)) {
							throw new Error("Unexpected EOF in child block,"
									+ t.location());
						}
					} else if ("case".equals(blockType)) {
						if (args.length != 2) {
							throw new Error("case block argument invalid: '"
									+ token + "' at " + t.location());
						}
						if (!new CaseBlock(getContext(), parameters, t, w, args[1])
								.process(true)) {
							throw new Error("Unexpected EOF in child block,"
									+ t.location());
						}
					} else {
						throw new Error("Block type unknown: '" + blockType
								+ "' at " + t.location());
					}					
				}
			} else {
				printResult(token);
			}
		}

		System.out.println("*** process end "+blockname+" "+cachemode);
		
		return token != TemplateParser.EOF_TOKEN;
	}

	protected void printResult(String token) {
		w.print(token);
	}

	public String getBlockname() {
		return blockname;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public TemplateParser getTemplate() {
		return t;
	}

	public PrintWriter getWriter() {
		return w;
	}

	protected void setWriter(PrintWriter w) {
		this.w = w;
	}

	protected String parameterSubstitution(String subject) {
		StringBuffer buffer = new StringBuffer();

		String[] parts = subject.split("\\$\\$\\$");
		boolean inParameterSyntax = false;
		if (parts != null) {
			for (String p : parts) {
				if (inParameterSyntax) {
					String value = parameters.get(p);
					if (value == null) {
						buffer.append("$$$" + p + "$$$");
					} else {
						buffer.append(value);
					}
				} else {
					buffer.append(p);
				}
				inParameterSyntax = !inParameterSyntax;
			}
		}
		return buffer.toString();
	}

	public ProcessingObjective getContext() {
		return context;
	}

}
