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

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.qstudio.BackgroundExecutor;
import com.timestored.qstudio.Language;

/**
 * Tracks the open documents and provides a {@link DocSource} for all open files
 * that is updated on each open/close/save.
 */
public class OpenDocumentsDocSource implements DocSource,OpenDocumentsModel.Listener {

	private static final Logger LOG = Logger.getLogger(OpenDocumentsDocSource.class.getName());
	
	private final OpenDocumentsModel openDocumentsModel;
	/** Documents that have not changed since last parse */
	private Map<Document,ParsedQFile> docParseCache = Maps.newHashMap();
	private Map<String,ParsedQFile> folderParseCache = new ConcurrentHashMap<String, ParsedQFile>();
	
	private Future<Void> future;
	private Pattern ignoredFoldersRegex = Pattern.compile("");
	private File selectedFolder = null;

	/** maximum depth of folders to search for .q files whos docs will be read */
	private static final int MAX_FOLDER_SEARCH_DEPTH = 8;
	/** maximum number of .q files whos docs will be parsed */
	private static final int MAX_FILE_PARSE = 500;

	protected static final String FILE_SUFFIX = ".q";
	
	

	/** 
	 * Files opened in openDocumentsModel will  be documented when opened/saved. 
	 */
	public OpenDocumentsDocSource(OpenDocumentsModel openDocumentsModel) {
		this.openDocumentsModel = openDocumentsModel;
		openDocumentsModel.addListener(this);
		startFolderParse(openDocumentsModel.getSelectedFolder());
	}

	/**
	 * @return The latest parse result for the given document
	 */
	private ParsedQFile parseAndCache(Document document) {
		Language lang = Language.getLanguage(document.getFileEnding());
		if(Language.Q.equals(lang) || document.getFileEnding().length()==0) { // new files won't have suffix
			String fp = document.getFilePath();
			String fileId = document.getTitle();
			ParsedQFile pqf = QParserDocExtractor.parse(document.getContent(), fp, fileId);
			docParseCache.put(document, pqf);
			
			if(fp!=null && folderParseCache.containsKey(fp)) {
				folderParseCache.put(document.getFilePath(), pqf);
			}
			
			return pqf;
		}
		return null;
	}
	
	@Override public void docClosed(Document document) {
		docParseCache.remove(document);
	}
	
	@Override public void docSaved() {
		Document d = openDocumentsModel.getSelectedDocument();
		if(d != null) {
			parseAndCache(d);
		}
	}

	@Override public void docAdded(Document document) {}
	@Override public void docContentModified() {} // may be invalid code
	@Override public void docCaratModified() {} // no point parsing
	@Override public void docSelected(Document document) {} // no point parsing
	
	@Override public void folderSelected(final File selectedFolder) {
		startFolderParse(selectedFolder);
	}

	@Override public void ignoredFolderPatternSelected(Pattern ignoredFolderPattern) { 
		setIgnoredFoldersRegex(ignoredFolderPattern);
	}

	private void startFolderParse(final File folder) {

		synchronized (this) {
			
			// cancel previous task
			if(future != null) {
				future.cancel(true);
				future = null;
			}
			
			final Map<String,ParsedQFile> fParseCache = new ConcurrentHashMap<String, ParsedQFile>();
			folderParseCache = fParseCache;
			
			// if new folder selected start it parsing
			if(folder != null && folder.isDirectory()) {
				future = BackgroundExecutor.EXECUTOR.submit(new Callable<Void>() {
		
					@Override public Void call() throws Exception {
						List<File> qFiles = findFiles(folder, FILE_SUFFIX, ignoredFoldersRegex);
						int i = 0;
						
						for(File f : qFiles) {
							 if (Thread.currentThread().isInterrupted()) {
							        throw new RuntimeException(); 
						    }
							if(i++ > MAX_FILE_PARSE) {
								LOG.log(Level.WARNING, "stopping file parsing as hit MAX_FILE_PARSE");
								break;
							}
							fParseCache.put(f.getAbsolutePath(), QParserDocExtractor.parse(f));
						}
						return null;
					}
					
				});
			}
		}
	}

	/** Get all parsed files from both open documents and folder cache. */
	public List<ParsedQFile> getParsedFiles() {
		List<ParsedQFile> result = new ArrayList<>();
		Set<String> seen = Sets.newHashSet();
		for (Document doc : openDocumentsModel.getDocuments()) {
			ParsedQFile pqf = docParseCache.get(doc);
			if (pqf == null) pqf = parseAndCache(doc);
			if (pqf != null && doc.getFilePath() != null) {
				result.add(pqf);
				seen.add(doc.getFilePath());
			}
		}
		for (Entry<String, ParsedQFile> e : folderParseCache.entrySet()) {
			if (!seen.contains(e.getKey())) result.add(e.getValue());
		}
		return result;
	}

	@Override public List<DocumentedEntity> getDocs() {

		List<DocumentedEntity> documentedEntities = Lists.newArrayList();
		Set<String> openDocFilePaths = Sets.newHashSet();
		
		for(Document doc : openDocumentsModel.getDocuments()) {
			ParsedQFile pqf = docParseCache.get(doc);
			if(pqf == null) {
				pqf = parseAndCache(doc);
			}
			if(pqf != null) {
				openDocFilePaths.add(doc.getFilePath());
				documentedEntities.addAll(pqf.getQEntities());
				documentedEntities.addAll(createNamespaceDocumentedEntity(pqf));
			}
		}
		
		for(Entry<String, ParsedQFile> e : folderParseCache.entrySet()) {
			// only if not already included in open docs
			if(!openDocFilePaths.contains(e.getKey())) {
				documentedEntities.addAll(e.getValue().getQEntities());
				documentedEntities.addAll(createNamespaceDocumentedEntity(e.getValue()));
			}
		}
		
		return documentedEntities;
	}



	private Collection<? extends DocumentedEntity> createNamespaceDocumentedEntity(ParsedQFile parsedQFile) {
		List<DocumentedEntity> nsDocs = Lists.newArrayList();
		Set<String> namespaces = Sets.newHashSet();
		for(ParsedQEntity pq : parsedQFile.getQEntities()) {
			String ns = pq.getNamespace();
			if(ns.length()>1 && !namespaces.contains(ns)) {
				ParsedQEntity nsPQE = ParsedQEntity.get(parsedQFile, "", 
						ns, parsedQFile.getHeaderDoc(), null, null, "", pq.getOffset());
				nsDocs.add(nsPQE);	
				namespaces.add(ns);
			}
		}
		return nsDocs;
	}

	/**
	 * @return Files within any depth within directory which have a given suffix.
	 */
	static List<File> findFiles(File directory, String suffix, Pattern ignoredFoldersRegex) {
		List<File> result = Collections.synchronizedList(new ArrayList<File>());
		return findFiles(result, directory, suffix, ignoredFoldersRegex);
	}
	
	/**
	 * @param matches The collection that matches are added to.
	 * @return Files within any depth of a directory which have a given suffix.
	 */
	private static List<File> findFiles(List<File> result, File directory, final String suffix, final Pattern ignoredFoldersRegex) {

		Preconditions.checkArgument(directory.isDirectory());
		
		// breadth first search for suffixed files
		// allows stopping early and result will contain what we have so far.
		List<File> directoriesToProcess = new ArrayList<File>();
		directoriesToProcess.add(directory);
		
		int i = 0;
		int depth = 0;
		while(i < directoriesToProcess.size() && depth<MAX_FOLDER_SEARCH_DEPTH) {
			
			int dirSize = directoriesToProcess.size();
			while(i < dirSize) {
				File f = directoriesToProcess.get(i);
				LOG.finer("searching " + f.getAbsolutePath());
				
				// add matches at this level
				File[] filesFound = f.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(suffix);
					}
				});
				if(filesFound != null) {
					result.addAll(Arrays.asList(filesFound));
				}

				// add subfolders to toProcess list
				File[] directories = f.listFiles(new FileFilter() {
					@Override public boolean accept(File pathname) {
						if(pathname.isDirectory()) {
							return !ignoredFoldersRegex.matcher(pathname.getName()).matches();
						}
						return false;
					}
				});
				if(directories != null) {
					directoriesToProcess.addAll(Arrays.asList(directories));
				}
				
				i++;
			}
			
			depth++;
			LOG.fine(depth + " levels deep in seach");
		}
		LOG.info("Searched " + directoriesToProcess.size() + " directories and found " 
				+ result.size() + " matches for " + suffix);
		
		return result;
	}
	
	private void setIgnoredFoldersRegex(Pattern ignoredFoldersRegex) {
		Preconditions.checkNotNull(ignoredFoldersRegex);
		// if pattern changed, better re-parse folder.
		if(!ignoredFoldersRegex.pattern().equals(this.ignoredFoldersRegex.pattern())) {
			this.ignoredFoldersRegex = ignoredFoldersRegex;
			startFolderParse(selectedFolder);
		}
	}
}
