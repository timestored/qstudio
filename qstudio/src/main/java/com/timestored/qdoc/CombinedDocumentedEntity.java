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

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;

import com.google.common.base.Preconditions;
import com.timestored.misc.HtmlUtils;
import com.timestored.theme.Theme;

import lombok.Getter;

/**
 * Allows combining  documented entities with the same fullname but difference sources into
 * one documentation. Mostly for qdoc popup to show just one entry.
 */
class CombinedDocumentedEntity implements DocumentedEntity {

	private static final Logger LOG = Logger.getLogger(CombinedDocumentedEntity.class.getName());
	
	private List<DocumentedEntity> docs;
	private String source = "";

	@Getter private ImageIcon icon;

	/**
	 * @param docs must contain more than one item.
	 */
	public CombinedDocumentedEntity(List<DocumentedEntity> docs) {
		Preconditions.checkArgument(docs.size()>1);
		for(DocumentedEntity de : docs) {
			boolean namesEqual = de.getFullName().equals(docs.get(0).getFullName());
			Preconditions.checkArgument(namesEqual);
			source += de.getSource() + " ";
		}
		Optional<DocumentedEntity> srverE = docs.stream().filter(de -> de.getSourceType().equals(SourceType.SERVER)).findFirst();
		this.icon = srverE.isPresent() ? srverE.get().getIcon() : Theme.CIcon.TEXT_HTML.get16();
				
		this.docs = docs;
	}

	@Override public String getDocName() {
		String longestDocName = "";
		for(DocumentedEntity docE : docs) {
			if(docE.getDocName().length() > longestDocName.length()) {
				longestDocName = docE.getDocName();
			}
		}
		return longestDocName;
	}

	@Override public String getHtmlDoc(boolean shortFormat) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(HtmlUtils.START);
		
		for(int i=0; i<docs.size(); i++) {
			DocumentedEntity docE = docs.get(i);	sb.append(docs.size()>6 ? "<br /><br /><b>" : docs.size()>3 ? "<h3>" : "<h2>");
			sb.append(docE.getSourceType()).append(" ").append(docE.getSource());
			sb.append(docs.size()>6 ? "</b>: " : docs.size()>3 ? "</h3>" : "</h2>");
			sb.append(HtmlUtils.extractBody(docE.getHtmlDoc(docs.size()>1)));
			// too many to show in short tooltip format, limit to 6 and ...
			if(shortFormat && i>6) {
				sb.append("...");
				break;
			}
		}
		sb.append(HtmlUtils.END);

		// if short/tooltip format, check if there are more than 10 lines, if so trim it
		String s =  sb.toString();
		if(shortFormat) {
			s = s.replace("<br />", "<br/>");
			int lastIndex = 0;
			int count = 0;
			while ((lastIndex = s.indexOf("<br/>", lastIndex)) != -1) {
				count++;
				lastIndex += "<br/>".length() - 1;
				if(count > 10) {
					return s.substring(0, lastIndex+1) + "..." + HtmlUtils.END;
				}
			}
		}
		
		return s;
	}
	

	@Override public SourceType getSourceType() {
		return SourceType.MIXED;
	}

	@Override public String getFullName() {
		return docs.get(0).getFullName();
	}

	@Override public String getSource() {
		return source;
	}

	public static List<DocumentedEntity> filterToShortestFullNameMatch(List<DocumentedEntity> docs) {
		// happens if fullname clashes e.g. "select" and "select [-1000] from t". Confuses them as same
		// But they can't really create a combined entity.
		int shortestDocLength = (int)docs.stream().map(de -> de.getFullName().length()).min(Integer::compare).get();
		List<DocumentedEntity> shortestMatches = docs.stream().filter(de -> de.getFullName().length() == shortestDocLength).collect(Collectors.toList());
		return shortestMatches.stream().filter(de -> de.getFullName().equals(shortestMatches.get(0).getFullName())).collect(Collectors.toList());
	}

}
