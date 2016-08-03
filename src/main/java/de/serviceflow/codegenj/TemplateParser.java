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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Encapsulate TemplateParser Access. Decouples read from Processing instance. 
 */
public class TemplateParser {
	public static final String BLOCK_TOKEN = "###";
	public static final String EOF_TOKEN = null;
	public static final String EOL_TOKEN = "\n";

	private final String path;
	private BufferedReader r;
	private int lineno = 0;
	private boolean returnBlockToken = false;
	private String[] bufferedTokens = null; 
	private int bufferedTokensPointer = 0;

	TemplateParser(String path) {
		this.path = path;
	}

	public void open() {
		InputStream tis = getClass().getResourceAsStream("/" + path);
		r = new BufferedReader(new InputStreamReader(tis));
	}

	public String readNext() {
		try {
			if (bufferedTokens == null || bufferedTokensPointer > bufferedTokens.length) {
				String strLine;
				if ((strLine = r.readLine()) != null) {
					lineno++;
					returnBlockToken = false;
					bufferedTokens = strLine.split("###");
					bufferedTokensPointer = 0;
					// System.out.println(">"+lineno+" "+bufferedTokens.length+"
					// "+strLine);
				} else {
					return EOF_TOKEN;
				}
			}

			if (bufferedTokensPointer < bufferedTokens.length) {
				if (returnBlockToken) {
					returnBlockToken = !returnBlockToken;
					return BLOCK_TOKEN;
				} else {
					if (bufferedTokensPointer + 1 < bufferedTokens.length)
						returnBlockToken = !returnBlockToken;
					return bufferedTokens[bufferedTokensPointer++];
				}
			} else {
				if (returnBlockToken) {
					returnBlockToken = !returnBlockToken;
					return BLOCK_TOKEN;
				} else {
					bufferedTokensPointer++;
					return EOL_TOKEN;
				}
			}
		} catch (IOException e) {
			throw new Error("Internal error", e);
		}
	}

	public void close() {
		try {
			r.close();
		} catch (IOException e) {
			throw new Error("Internal error", e);
		}
	}

	public String location() {
		return path + ":" + lineno;
	}

	public String getTemplatePath() {
		return path;
	}

}
