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

public abstract class ProcessingObjective {
	public void process(Block p, String blockArg, PrintWriter w,
			String blockContent) {
		ObjectiveBlockHandler abp = CodegenJ.getInstance().findProcessing(
				getClass().getName());
		if (abp == null) {
			throw new Error("No block processing found for "
					+ getClass().getName());
		}

		abp.setContent(blockContent);
		System.out.println("***** process eval " + getClass().getName() + " "
				+ blockArg);
		abp.process(p, this, blockArg, w);
	}

	public abstract ProcessingObjective getProcessingObjective(String name2);

	public abstract void startScan();

	public abstract void endScan();

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	private String comment;
	
}
