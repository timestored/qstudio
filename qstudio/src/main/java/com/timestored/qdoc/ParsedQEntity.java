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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.model.QEntity;
import com.timestored.theme.Theme;

import net.thisptr.jackson.jq.internal.misc.Lists;


/**
 * A single q entity whos existence has been found through a text file.
 */
public class ParsedQEntity implements QEntity, Comparable<ParsedQEntity> {

	public static final String PARAM_TAG = "param";
	public static final String EXCEPTION_TAG = "exception";
	public static final String COLUMN_TAG = "col";
	public static final String EXAMPLE_TAG = "example";
	public static final String RETURN_TAG = "return";
	private static final Map<String,String> EMPTY_MAP = Collections.emptyMap();
	
	private final String name;
	private final String namespace;
	private final String docDescription;
	private final String returnDescription;
	
	/** named tags are like @param ie they have multiple named entries per tag */
	private final Map<String,Map<String,String>> namedTags;
	/** tags like @return where it has only one assigned string */
	private final Map<String,String> tags;
	
	private final ParsedQFile parentFile;
	private final int offset;


	/**
	 * 
	 * @param parentFile specifies the parent file to this entity. null is permitted, note
	 * 	entities are considered equal regardless of what file they are in.
	 */
	static ParsedQEntity get(ParsedQFile parentFile, String name, String namespace, 
			ParsedComments pec, int offset) {
		
		return new ParsedQEntity(parentFile, name, namespace, pec.docDescription, 
				pec.namedTags, pec.tags, 
				pec.returnDescription, offset);
	}
	
	/**
	 * @param parentFile specifies the parent file to this entity. null is NOT permitted, note
	 * 	entities are considered equal regardless of what file they are in.
	 */
	private ParsedQEntity(ParsedQFile parentFile, String name, 
			String namespace, String docDescription, 
			Map<String, Map<String, String>> namedTags,
			Map<String, String> tags,
			String returnDescription,
			int offset) {
		
		this.name = Preconditions.checkNotNull(name);
		this.namespace = Preconditions.checkNotNull(namespace);
		this.docDescription = (docDescription == null ? "" : docDescription);
		this.returnDescription = (returnDescription == null ? "" : returnDescription);

		this.namedTags = Preconditions.checkNotNull(namedTags);
		this.tags = Preconditions.checkNotNull(tags);
		this.parentFile = Preconditions.checkNotNull(parentFile);
		this.offset = offset;
	}
	
	
	/**
	 * @param parentFile specifies the parent file to this entity. null is NOT permitted, note
	 * 	entities are considered equal regardless of what file they are in.
	 */
	public static ParsedQEntity get(ParsedQFile parentFile, String name, 
			String namespace, String docDescription, 
			Map<String, String> paramDescriptions,
			Map<String, String> exceptionDescriptions,
			String returnDescription,
			int offset) {

		HashMap<String, Map<String,String>> namedTags = new HashMap<String, Map<String,String>>();
		if(paramDescriptions != null) {
			namedTags.put(PARAM_TAG, Collections.unmodifiableMap(paramDescriptions));
		}
		if(exceptionDescriptions != null) {
			namedTags.put(EXCEPTION_TAG, Collections.unmodifiableMap(exceptionDescriptions));
		}
		return new ParsedQEntity(parentFile, name, namespace, docDescription, 
				namedTags, EMPTY_MAP, returnDescription, offset);
	}

	/** {@inheritDoc} */ @Override 
	public String getNamespace() {
		return namespace;
	}

	/** {@inheritDoc} */
	@Override
	public String getHtmlDoc(boolean shortFormat) {
		return getHtmlDoc(shortFormat, null);
	}

	public String getHtmlDoc(boolean shortFormat, String baseWeblink) {
		
		boolean hasDocs = !docDescription.trim().isEmpty() || !tags.isEmpty() || !namedTags.isEmpty();
		String s = "";
		
		if(hasDocs) {
			StringBuilder sb = new StringBuilder();
			sb.append(HtmlUtils.START);
			sb.append("<p>" + docDescription + "</p>");	
			
			Map<String,String> namesToDescs = Maps.newHashMap();
			
			for(Entry<String, String> e : tags.entrySet()) {
				String k = e.getKey();
				if(k.equals(RETURN_TAG)) {
					k = "Return";
				}
				namesToDescs.put(k + ": ", e.getValue());
			}
			
			for(Entry<String, Map<String, String>> e : namedTags.entrySet()) {
				String k = e.getKey();
				if(k.equals(PARAM_TAG)) {
					k = "Parameters";
				} else if(k.equals(EXCEPTION_TAG)) {
					k = "Exceptions";
				} else if(k.equals(COLUMN_TAG)) {
					k = "Columns";
				}
				
				if(k.equals(EXAMPLE_TAG) || k.toLowerCase().equals("eg")) {
					k = "Examples";
					List<String> examples = new ArrayList<>();
					e.getValue().forEach((String u, String vv) -> {
						String txt = u;
						if(baseWeblink!=null && baseWeblink.length()>0) {
							try {
								String args = URLEncoder.encode(u, "UTF-8");
								txt = "<a href='" + baseWeblink + args + "' target='a'>"+u+"</a>";
							} catch (UnsupportedEncodingException e1) {}
						}
						examples.add("<code>" + txt + "</code>");
					});
					namesToDescs.put(k + ": ", HtmlUtils.toList(examples));
				} else {
					namesToDescs.put(k + ": ", HtmlUtils.toList(e.getValue(), true));
				}
			}
			
			sb.append(HtmlUtils.toTable(namesToDescs, true));
			sb.append(HtmlUtils.END);
			s = sb.toString();
		}
		
		return s;
	}

	/** {@inheritDoc} */ @Override 
	public String getDocName() {
		 return getFullName();
	}

	@Override public String getFullName() {
		 return (namespace.equals(".") ? "" : namespace + ".") + name;
	}
	
	String getDocDescription() {
		return docDescription;
	}

	/** 
	 * Named tags are like @param ie they have multiple named entries per tag.
	 * @return a map from names to entries, e.g. "param"->("key"->"a unique identifier")
	 */
	Map<String, Map<String, String>> getNamedTags() {
		return Collections.unmodifiableMap(namedTags);
	}
	

	/** 
	 * Named tags are like @param ie they have multiple named entries per tag
	 * @return A map of names to descriptions if tagName exists otherwise an empty map.  
	 */
	Map<String, String> getNamedTags(String tagName) {
		Map<String, String> r = namedTags.get(tagName);
		return r == null ? EMPTY_MAP : r;
	}


	/** 
	 * Tags like @return where it has only one assigned string
	 * @return a map from tags to their description. 
	 */
	public Map<String, String> getTags() {
		return tags;
	}


	/** 
	 * Tags like @return where it has only one assigned string
	 * Get the description of a specific tag, or null if it does not exist. 
	 */
	public String getTag(String tag) {
		return tags.get(tag);
	}
	
	Map<String, String> getParamTags() {
		return getNamedTags(PARAM_TAG);
	}

	
	Map<String, String> getExceptionTags() {
		return getNamedTags(EXCEPTION_TAG);
	}
	
	String getReturnDescription() {
		return returnDescription;
	}
	
	/**
	 * @return The parent file if it is known, otherwise null
	 */
	private String getSrcFileAbsolutePath() {
		return parentFile==null ? null : parentFile.getSrcFileAbsolutePath();
	}
	
	/**
	 * @return position from start of file in characters that entity occurs.
	 * NOTE! \r\n are treated as one character only!! In order that {@link Document} and parsing 
	 * offsets match up. All due to javas editor pane handling new lines.
	 */
	public int getOffset() {
		return offset;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("name", name)
			.add("namespace", namespace)
			.add("srcFilePath", getSrcFileAbsolutePath())
			.add("offset", offset)
			.toString();
	}

	
	
	@Override
	public int hashCode(){
		return Objects.hashCode(super.hashCode(), name, namespace, 
				docDescription, namedTags, getSrcFileAbsolutePath(), offset);
	}
	
	public String toFullString() {
		return MoreObjects.toStringHelper(this)
			.add("name", name)
			.add("namespace", namespace)
			.add("docDescription", docDescription)
			.add("returnDescription", returnDescription)
			.add("paramDescriptions", namedTags.get(PARAM_TAG))
			.add("exceptionDescriptions", namedTags.get(EXCEPTION_TAG))
			.add("srcFilePath", getSrcFileAbsolutePath())
			.add("offset", offset)
			.toString();
	}


	@Override
	public boolean equals(Object object){
		if (object instanceof ParsedQEntity) {
			ParsedQEntity that = (ParsedQEntity) object;
			return Objects.equal(this.name, that.name)
				&& Objects.equal(this.namespace, that.namespace)
				&& Objects.equal(this.docDescription, that.docDescription)
				&& Objects.equal(this.namedTags, that.namedTags)
//				&& Objects.equal(this.parentFile, that.parentFile)
			&& Objects.equal(this.returnDescription, that.returnDescription);
//				&& Objects.equal(this.offset, that.offset);
		}
		return false;
	}

	/** @return Description up till the first full stop. */
	public String getShortDescription() {
		String s = getDocDescription();
		int p = s.indexOf('.');
		return p==-1 ? s : s.substring(0, p+1);
	}

	public void gotoDefinition(OpenDocumentsModel openDocumentsModel) throws IOException {
		String pth = getSrcFileAbsolutePath();
		if(pth!=null) {
			Document d = openDocumentsModel.openDocument(pth);
			d.setCaratPosition(getOffset());
		} 
	}

	@Override public ImageIcon getIcon() {
		return Theme.CIcon.PAGE_CODE.get16();
	}

	@Override public SourceType getSourceType() {
		return SourceType.SOURCE;
	}

	@Override public String getSource() {
		return parentFile.getFileTitle();
	}

	ParsedQFile getParentFile() {
		return parentFile;
	}
	
	/** The name of this entity (not including namesapce */
	String getName() {
		return name;
	}
	
	@Override public int compareTo(ParsedQEntity o) {
		return this.getFullName().compareTo(o.getFullName());
	}
}
