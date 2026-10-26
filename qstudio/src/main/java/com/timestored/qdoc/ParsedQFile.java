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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * Represents the parsing of a single Q file, Contained will be
 * any number of {@link ParsedQEntity}. (their full names are not guaranteed to
 * be unique as could be multiple entries in file).  
 */
public class ParsedQFile {

	private final ParsedComments headerDoc;
	private final String author;
	private List<ParsedQEntity> qEntities;
	
	/**
	 * Path that allows source code lookup, optional to allow parsing unsaved documents 
	 */
	private final String srcFileAbsolutePath;
	private final String fileTitle;
	
	ParsedQFile(ParsedComments headerDoc, String author, 
			String srcFileAbsolutePath,
			String fileTitle) {

		this.headerDoc = headerDoc;
		this.author = (author == null ? "" : author);
		this.qEntities = Collections.emptyList();
		this.srcFileAbsolutePath = srcFileAbsolutePath;
		this.fileTitle = Preconditions.checkNotNull(fileTitle);
		
	}
	
	void setqEntities(List<ParsedQEntity> qEntities) {
		List<ParsedQEntity> l;
		l = new ArrayList<ParsedQEntity>(Preconditions.checkNotNull(qEntities));
		Collections.sort(l);
		this.qEntities = Collections.unmodifiableList(l);
	}
	
	/** Get the comments at the top of the file */
	String getHeaderDoc() {
		return headerDoc==null ? "" : headerDoc.docDescription;
	}

	String getAuthor() {
		return author;
	}

	/** 
	 * Get a list of all q entities defined at topmost level within source file.
	 * Order of list will be same as order of occurrence in file. 
	 */
	public List<ParsedQEntity> getQEntities() {
		return qEntities;
	}

	/** 
	 * Get a list of all namespaces defined in this file. 
	 */
	public Collection<String> getNamespaces() {
		if(qEntities.isEmpty()) {
			return Collections.emptySet();
		}
		Set<String> r = Sets.newHashSet();
		for(ParsedQEntity p : qEntities) {
			r.add(p.getNamespace());
		}
		return r;
	}
	
	/** @return File path if known otherwise null.  */
	String getSrcFileAbsolutePath() {
		return srcFileAbsolutePath;
	}
	
	/** @return Title of the file guaranteed not to be null */
	String getFileTitle() {
		return fileTitle;
	}
	
	/** Get q entities that occurred in this file with a given name */
	List<ParsedQEntity> getQEntities(String fullName) {
		List<ParsedQEntity> r = null;
		for(ParsedQEntity pqe : qEntities) {
			if(pqe.getDocName().equals(fullName)) {
				if(r == null) {
					r = new ArrayList<ParsedQEntity>();
				}
				r.add(pqe);
			}
		}
		if(r == null) {
			return Collections.emptyList(); 
		} else {
			return r;
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("srcFileAbsolutePath", srcFileAbsolutePath)
			.add("qEntities", qEntities).toString();
	}

	Map<String,String> getHeaderTags() {
		Map<String, String> e = Collections.emptyMap();
		return headerDoc==null ? e : headerDoc.tags;
	}

	Map<String, Map<String,String>> getHeaderNamedTags() {
		Map<String, Map<String,String>> e = Collections.emptyMap();
		return headerDoc==null ? e : headerDoc.namedTags;
	}

	boolean isIgnored() {
		return getHeaderTags().containsKey(ParsedQEntity.IGNORE_TAG);
	}
	
	
}
