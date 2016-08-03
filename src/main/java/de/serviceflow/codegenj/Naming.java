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

public final class Naming {
	public static String javaMemberName(String name) {
		if (name.length() > 1) {
			return name.substring(0, 1).toLowerCase() + name.substring(1);
		} else {
			return name.substring(0, 1).toLowerCase();
		}
	}

	public static String javaClassName(String name) {
		if (name.length() > 1) {
			return name.substring(0, 1).toUpperCase() + name.substring(1);
		} else {
			return name.substring(0, 1).toUpperCase();
		}
	}

	public static String javaUName(String name) {
		int lastDot = name.lastIndexOf('.');
		return lastDot >= 0 ? name.substring(lastDot + 1) : name;
	}

	public static String javaPName(String name) {
		int lastDot = name.lastIndexOf('.');
		return lastDot >= 0 ? name.substring(0, lastDot) : null;
	}

	public static String cName(String name) {
		StringBuffer result = new StringBuffer();
		StringBuffer word = new StringBuffer();
		int n = name.length();

		boolean wordStart = false;
		for (int i = 0; i < n; i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c) && !wordStart) {
				wordStart = true;
				if (i > 0) {
					result.append(word.toString().toLowerCase());
					word.setLength(0);
					word.append('_');
				}
				word.append(c);
			} else {
				word.append(c);
				if (!Character.isUpperCase(c))
					wordStart = false;
			}
		}
		result.append(word.toString().toLowerCase());
		return result.toString();
	}

	public static String jName(String name) {
		return name.replace('.', '_');
	}
}
