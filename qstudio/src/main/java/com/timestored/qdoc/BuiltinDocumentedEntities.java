/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2024 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
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
package com.timestored.qdoc;

import java.net.URLEncoder;

import com.google.common.base.MoreObjects;
import com.timestored.TimeStored;

/**
 * Abstract class that aids creating documentation classes for all the functions builtin to KDB.
 */
public abstract class BuiltinDocumentedEntities implements DocumentedEntity,DocSource {

	protected final String docname;
	protected final String description;
	protected final String syntax;
	protected final String eg;

	/**
	 * @param docname fully identifying name of entity and includes [arguments] for functions.
	 * @param description standard plain text description of this builtin function.
	 */
	protected BuiltinDocumentedEntities(String docname, String description) {
		this.docname = docname;
		this.description = description;
		this.syntax = null;
		this.eg = null;
	}
	
	/**
	 * @param docname fully identifying name of entity and includes [arguments] for functions.
	 * @param description standard plain text description of this builtin function.
	 */
	protected BuiltinDocumentedEntities(String docname, String description, String syntax, String eg) {
		this.docname = docname;
		this.description = description;
		this.syntax = syntax;
		this.eg = eg;
	}

	public String getName() {
		return docname;
	}
	
	public String getDescription() {
		return description;
	}
	
	public abstract String getLink();
	
	@Override public String getDocName() {
		return docname;
	}

	@Override public String getHtmlDoc(boolean shortFormat) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append(description.replaceAll("\n", "<br />"));
		sb.append("<br/>");
		if(syntax != null && syntax.length() > 0) {
			sb.append("<br/><b>syntax:</b> ");
			sb.append(syntax.replace("\n", "<br />"));
		}
		if(eg != null && eg.length() > 0) {
			sb.append("<br/><b>eg:</b> ");
			if(eg.contains("\n")) {
				sb.append("<br />").append(eg.replace("\n", "<br />"));
			} else {
				sb.append(eg);
			}
		}
		String link = getLink();
		if(link != null && link.length() > 0) {
			sb.append("<br/><br/><a href='");
			sb.append(TimeStored.getRedirectPage(link, "qdoc"));
			sb.append("&qdoc=");
			sb.append(URLEncoder.encode(docname, java.nio.charset.StandardCharsets.UTF_8));
			sb.append("'>Open online doc</a>");
		}
		sb.append("</body></html>");
		return sb.toString();
	}
	
	@Override public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("name", docname)
			.add("description", description)
			.toString();
	}

	@Override public SourceType getSourceType() {
		return SourceType.BUILTIN;
	}

	@Override public String getFullName() {
		int p = docname.indexOf('[');
		if(p != -1) {
			return docname.substring(0, p);
		}
		return docname;
	}
}
