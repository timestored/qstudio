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

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.timestored.qstudio.Language;
import com.timestored.qstudio.qdoc.ContextualDocCompleter;

import lombok.Getter;

/**
 * Allows searching {@link DocSource}'s for autocompletion suggestions
 * and documentation. Uses the latest prefix to search through docs
 * and return the full function call while highlighting the first argument
 * where possible.
 */
public class DocumentedMatcher implements DocCompleter {

	private static final Logger LOG = Logger.getLogger(DocumentedMatcher.class.getName());

	@Getter private final Language language;
	private final DocSource docSource;
	private final String funcStart;
	private final String funcEnd;
	private final String argDivider;
	private final boolean ignoreCasing;
	private final CustomAutocompleteHandler filterSorter;
	
	/** cache previous result */
	/** time in millis after which all docs are refetched */
	private static final long STALE_TIME = 1000;
	private List<DocumentedEntity> prevPrefixDocs;
	private java.util.Calendar calendar = Calendar.getInstance();
	private String prevPrefix;
	private long prevMillis = calendar.getTimeInMillis();

	private ContextualDocCompleter contextualDocCompleter;



	public DocumentedMatcher(Language language, DocSource docSource) {	
		this(language, docSource,"[","]",";", false, null);
	}

	/**
	 * Create an entity matcher that gets docs from docSource and provides 
	 * autocomplete suggestions by using funcStart/End etc to highlight
	 * the first argument.
	 * @param docSource Documentation that will be searched.
	 * @param funcStart prefix for containing function arguments e.g. ( in java
	 * @param funcEnd postfix for containing function arguments e.g. ) in java
	 * @param argDivider divider of arguments e.g. , in java
	 * @param completionSorter CUstom sorting to arrange better completions to the top.
	 */
	public DocumentedMatcher(Language language, DocSource docSource, String funcStart, 
			String funcEnd, String argDivider, boolean ignoreCasing, CustomAutocompleteHandler filterSorter) {	
		this.language = language;	
		this.docSource = docSource;
		this.funcStart = funcStart;
		this.funcEnd = funcEnd;
		this.argDivider = argDivider;
		this.ignoreCasing = ignoreCasing;
		this.filterSorter = filterSorter;
	}

	
//	/** 
//	 * For given text and position within text return list of suggestions that would complete
//	 * from our current carat position.  
//	 */
//	public List<Suggestion> findSuggestions(final String txt, int caratPos) {
//
//		Preconditions.checkNotNull(txt);
//		Preconditions.checkArgument(caratPos>=0 && caratPos<=txt.length());
//		
//		List<DocumentedEntity> docs = findByPrefix(getLatestPrefix(txt, caratPos));
//		List<Suggestion> sugs = Lists.newArrayListWithExpectedSize(docs.size());
//		for(DocumentedEntity de : docs) {
//			sugs.add(getSuggestion(de, getLatestPrefix(txt, caratPos)));
//		}
//		return sugs;
//	}
	
	/** 
	 * For given text and position within text return documentation relevant to current text
	 * under the carat.
	 */
	public List<DocumentedEntity> findDocs(final String txt, int caratPos) {

		Preconditions.checkNotNull(txt);
		Preconditions.checkArgument(caratPos>=0 && caratPos<=txt.length());
		return findByFullname(getLatestFullname(txt, caratPos));
	}
	
	/** 
	 * For given text and position within text return the single most documentation 
	 * relevant to current text under the carat, or null if none found.
	 */
	public DocumentedEntity findDoc(final String txt, int caratPos) {
		List<DocumentedEntity> docs = findByFullname(getLatestFullname(txt, caratPos)); 
		if(docs.size()>1) {
			try {
				List<DocumentedEntity> fullnameDocMatches = CombinedDocumentedEntity.filterToShortestFullNameMatch(docs);
				return fullnameDocMatches.size() == 1 ? fullnameDocMatches.get(0) : new CombinedDocumentedEntity(fullnameDocMatches);
			} catch(IllegalArgumentException iae) {
				LOG.log(Level.WARNING, iae.getMessage(), iae);
			}
		} else if(docs.size()==1) {
			return docs.get(0);
		}
		return null;
	}

	/** 
	 * Return all the docs that occur within any given source file
	 */
	public List<ParsedQEntity> findSourceDocs() {
		Predicate<DocumentedEntity> fileFilter = de -> de.getSourceType().equals(DocumentedEntity.SourceType.SOURCE);
		List<ParsedQEntity> p = Lists.newArrayList();
		filter(docSource.getDocs(), fileFilter).forEach(de -> p.add((ParsedQEntity)de));
	    return p;
	}
	
	/** 
	 * Return all the docs that occur within a given file name
	 */
	public List<ParsedQEntity> findSourceDocs(final String sourceFilename) {
		Predicate<DocumentedEntity> fileFilter = (DocumentedEntity de) -> {
	            return de instanceof ParsedQEntity 
	            		&& de.getSourceType().equals(DocumentedEntity.SourceType.SOURCE)
	            		&&  de.getSource().equals(sourceFilename);
	        };
		List<ParsedQEntity> p = Lists.newArrayList();
		filter(docSource.getDocs(), fileFilter).forEach(de -> p.add((ParsedQEntity)de));
	    return p;
	}

	/**
	 * Convert {@link DocumentedEntity} to an actual suggestion. 
	 */
	public Suggestion getSuggestion(DocumentedEntity de, String txt, int caratPos) {
		return getSuggestion(de, getLatestPrefix(txt, caratPos));
	}

	
	List<Suggestion> findSuggestions(final String txt, final int caratPos) {
		List<DocumentedEntity> r = findByPrefix(getLatestPrefix(txt, caratPos));
		Iterable<Suggestion> i =  transform(r,  de ->  getSuggestion(de, getLatestPrefix(txt, caratPos)));
		return Lists.newArrayList(i);
	}
	
	
	/**
	 * Convert {@link DocumentedEntity} to an actual suggestion. 
	 * @param latestPrefix the relevant preceding text
	 */
	private Suggestion getSuggestion(DocumentedEntity de, String latestPrefix) {
		String textInsert = de.getDocName().substring(latestPrefix.length());
		
		int start = textInsert.length();
		int end = textInsert.length();
		
		boolean emptyCall = textInsert.contains(funcStart + funcEnd);
		if(textInsert.contains(funcStart) && !emptyCall) {
			start = textInsert.indexOf(funcStart) + 1;
			String ending = textInsert.substring(start);
			if(ending.contains(argDivider)) {
				end = start + ending.indexOf(argDivider); 
			} else if(ending.contains(funcEnd)) {
				end = start + ending.indexOf(funcEnd);
			}
		}
		
		return new Suggestion(de, textInsert, start, end);
	}

	/** 
	 * Given an exact name return all exact matches.
	 */
	private List<DocumentedEntity> findByFullname(final String fullname) {
		final String cleanName = removeArgs(fullname);
		Predicate<DocumentedEntity> fullMatchFilter = de -> cleanName.equals(removeArgs(de.getDocName()));
	    return Lists.newArrayList(filter(docSource.getDocs(), fullMatchFilter));
	}

	private String removeArgs(String docName) {
		int p = docName.indexOf(funcStart);
		return p!=-1 ? docName.trim().substring(0,p) : docName;
	}

	/** @return All {@link DocumentedEntity}  */
	public List<? extends DocumentedEntity> getDocs() {
		return docSource.getDocs();
	}
	
	/** 
	 * For given prefix return list of docs that contain elements starting with that prefix 
	 */ @Override
	public List<DocumentedEntity> findByPrefix(final String txt, int caratPos) {

		 if(contextualDocCompleter != null) {
			 List<DocumentedEntity> docsToComplete = contextualDocCompleter.findByPrefix(txt, caratPos);
			 // When to only use these results vs also show doc completions?
			 if(docsToComplete != null && docsToComplete.size() > 0) {
				 return docsToComplete;
			 }
		 }
		return findByPrefix(getLatestPrefix(txt, caratPos));
	}
	 
	/** 
	 * For given prefix return list of docs that contain elements starting with that prefix 
	 */
	private List<DocumentedEntity> findByPrefix(final String latestPrefix) {

		List<DocumentedEntity> docs = null;
		Predicate<DocumentedEntity> prefixFilter = new Predicate<DocumentedEntity>() {
	        @Override public boolean apply(DocumentedEntity de) {
	        	if(ignoreCasing) {
		            return de.getDocName().toLowerCase().startsWith(latestPrefix.toLowerCase());
	        	}
	            return de.getDocName().startsWith(latestPrefix);
	        }
	    };

	    long ticksNow = Calendar.getInstance().getTimeInMillis();
	    boolean longTimeBetweenSearch = (ticksNow - prevMillis) > STALE_TIME;
	    boolean extensionToLastSearch = prevPrefix!=null && latestPrefix.startsWith(prevPrefix);
	    if(extensionToLastSearch && !longTimeBetweenSearch) {
		    // just typed an extra letter, filter previous search further
	    		docs = filterSortResults(Lists.newArrayList(filter(prevPrefixDocs, prefixFilter)), latestPrefix);
	    } else { 
			docs = filterSortResults(Lists.newArrayList(filter(docSource.getDocs(), prefixFilter)), latestPrefix);
	    }
		
		prevPrefixDocs = docs;
		prevPrefix = latestPrefix;

		LOG.info("Found " + docs.size() + " docs for prefix: " + latestPrefix);
		return docs;
	}

	@FunctionalInterface
	public static interface CustomAutocompleteHandler {
		List<DocumentedEntity> filterSortResults(List<DocumentedEntity> docs, String prefix);
	}
	
	/** sort the collection in place and also return */
	private List<DocumentedEntity> filterSortResults(List<DocumentedEntity> docs, String prefix) {
		if(this.filterSorter != null) {
			docs = filterSorter.filterSortResults(docs, prefix);
		} else {
			Collections.sort(docs, (de1, de2)  -> de1.getDocName().compareTo(de2.getDocName()));	
		}
		return docs;
	}

	/** Get the closest text before the carat that is relevant */
	static String getLatestPrefix(final String txt, int caratPos) {

		Preconditions.checkNotNull(txt);
		Preconditions.checkArgument(caratPos>=0 && caratPos<=txt.length());
		
		String doc = txt.substring(0, caratPos);
		int startPos = caratPos-1;
		while(startPos>=0) {
			char ch = txt.charAt(startPos); 
			if(!Character.isLetter(ch) && ch!='.' && !Character.isDigit(ch) && ch!='_') {
				startPos++;
				break;
			}
			startPos--;
		}
		
		if(startPos<=0) {
			return doc;
		}
		return doc.substring(startPos);
	}


	/** Get the closest text before and after the carat that is relevant */
	static String getLatestFullname(final String txt, int caratPos) {
		
		int endPos = caratPos;
		while(endPos < txt.length()) {
			char ch = txt.charAt(endPos); 
			if(!(Character.isLetter(ch) || ch=='_') && ch!='.') {
				break;
			}
			endPos++;
		}

		String before = getLatestPrefix(txt, caratPos);
		if(endPos>=txt.length()) {
			return before + txt.substring(caratPos);
		}
		return before + txt.substring(caratPos, endPos);
	}

	public void setContextDocCompleter(ContextualDocCompleter contextualDocCompleter) {
		this.contextualDocCompleter = contextualDocCompleter;
	}

}
