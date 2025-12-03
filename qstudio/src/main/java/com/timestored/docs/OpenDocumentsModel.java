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
package com.timestored.docs;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.google.common.base.MoreObjects.toStringHelper;
import com.google.common.base.Preconditions;

import lombok.Getter;


/**
 * Represents a group of documents, Only the selected one will be
 * selected/opened/saved, any time there's a significant event listeners are
 * notified. There will always be a document selected, if one is closed and none
 * are left, one is automatically created. Attempting to open documents already
 * opened will select that document but not add another.
 */
public class OpenDocumentsModel {

	private static final Logger LOG = Logger.getLogger(OpenDocumentsModel.class.getName());

	private final List<Document> documents = new CopyOnWriteArrayList<Document>();
	private Document selectedDocument;
	private final CopyOnWriteArrayList<Listener> listeners;
	private Document.Listener selectedDocListener;
	/** directory selected by user */
	private File selectedFolder;
	@Getter private Pattern ignoredFolderPattern;

	/** Navigation history for back/forward functionality */
	private final List<Document> navigationHistory = new ArrayList<Document>();
	private int navigationPosition = -1;
	private boolean isNavigating = false;
	
	
	public static OpenDocumentsModel newInstance() {
		return new OpenDocumentsModel();
	}
	
	/**
	 * For common actions, it listens to the keyboard manager to grab the events and check if its an action
	 * we should handle. Needed if you have previously had other components grab priority.
	 * But does mean you cant have multiple instances of {@link OpenDocumentsModel}.
	 */
	public void forceKeyboardShortcutOverrides() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(new KeyEventDispatcher() {
        	// variable used to limit rate at which actions are taken.
        	boolean down = false;
        	
			@Override public boolean dispatchKeyEvent(KeyEvent e) {
				final int kc = e.getKeyCode();
				final boolean ctrl = e.isControlDown();
				if (!down && ctrl && kc==KeyEvent.VK_F4) {
					closeDocument();
					down = true;
				} else if (!down && ctrl && e.isShiftDown() && kc==KeyEvent.VK_TAB) {
					gotoPrevDocument();
					down = true;
				} else if (!down && ctrl && kc==KeyEvent.VK_TAB) {
					gotoNextDocument();
					down = true;
				} else {
					down = false;	
				}
				if(down) {
					e.consume();
				}
				return false;
			}
		});
	}
	
	private OpenDocumentsModel() {
		
		listeners = new CopyOnWriteArrayList<Listener>();
		
		selectedDocListener = new Document.Listener() {
			@Override public void docContentModified() {
				for(Document.Listener l : listeners) {
					l.docContentModified();
				}
			}
			
			@Override public void docCaratModified() {
				for(Document.Listener l : listeners) {
					l.docCaratModified();
				}
			}

			@Override public void docSaved() {
				for(Document.Listener l : listeners) {
					l.docSaved();
				}
			}
		};
		
		selectedDocument = addDocument();
		selectedDocument.addListener(selectedDocListener);
	}

	/** 
	 * Open a document
	 * @return The document if it was successfully opened otherwise null.
	 * @throws IOException if problem opening document
	 */
	public Document openDocument(String filepath) throws IOException {
		Preconditions.checkNotNull(filepath);
		return openDocument(new File(filepath));
	}

	/**
	 * Open a list of documents, failing silently on Exceptions.
	 */
	public void openDocuments(List<String> filepaths) {
		for(String fp : filepaths) {
			try {
				openDocument(new File(fp));
			} catch (IOException e) {
				LOG.warning("Couldn't open previously opened file location:" + fp);
			}
		}
		if(documents.size()>0) {
			changeSelectedDocTo(documents.get(documents.size() - 1));
		} else {
			changeSelectedDocTo(addDocument());
		}
		selectedDocument.addListener(selectedDocListener);

		for(Listener l : listeners) {
			l.docSelected(selectedDocument);
		}
	}
	
	public void changeSelectedDocTo(Document d) {
		if(selectedDocument!=null) {
			selectedDocument.removeListener(selectedDocListener);
		}
		selectedDocument = d;
		selectedDocument.addListener(selectedDocListener);
	}
	
	/**
	 * Create a new document and make it selected.
	 */
	public Document addDocument() {
		return addDocument(null);
	}

	public Document addDocument(String title) {
		Document d = new Document();
		if(title != null && title.trim().length() > 0) {
			d.setTitle(title);
		}
        LOG.info("addDocument: " + d.getTitle());
		documents.add(d);
		changeSelectedDocTo(d);
		
		// Add to navigation history
		while(navigationHistory.size() > navigationPosition + 1) {
			navigationHistory.remove(navigationHistory.size() - 1);
		}
		navigationHistory.add(d);
		navigationPosition = navigationHistory.size() - 1;
				
		for(Listener l : listeners) {
			l.docAdded(d);
			l.docSelected(d);
		}
		return d;
	}
	
	public boolean isFileOpen(File file) {
		return getDocument(file.getAbsolutePath()) != null;
	}

	public Document getDocument(String filePath) {
        for(Document d : documents) {
        	if(d.getFilePath()!=null && d.getFilePath().equals(filePath)) {
        		return d;
        	}
        }
        return null;
	}
	
	
	/**
	 * Open the selected file as a document and make it selected.
	 * @param file file that you wish to open.
	 * @throws IOException if file could not be opened/read.
	 */
	public Document openDocument(File file) throws IOException {

        LOG.info("openDocument: " + file.getName());

        // if already exists, just select
        for(Document d : documents) {
        	if(d.getFilePath()!=null && d.getFilePath().equals(file.getAbsolutePath())) {
                LOG.info("openDocument: was already open, reselecting->" + file.getName());
                changeSelectedDocTo(d);
        		for(Listener l : listeners) {
        			l.docSelected(d);
        		}
        		return d;
        	}
        }
        
        // else add new doc
        Document d = new Document(file);
		documents.add(d);
		for(Listener l : listeners) {
			l.docAdded(d);
		}
		setSelectedDocument(d);
		return d;
	}

	/**
	 * Close the currently selected document.
	 */
	public void closeDocument() {
		closeDocument(selectedDocument);
	}

	/**
	 * Close the selected file as a document, making sure to always have 
	 * at least one open even if it must be newly created.
	 * @param document that you wish to close.
	 */
	public void closeDocument(Document document) {

        LOG.info("closeDocument: " + document.getTitle());
        boolean removed = documents.remove(document);
        boolean changedSelection = selectedDocument == document;
		if(changedSelection) {
			if(documents.size() > 0) {
				changeSelectedDocTo(documents.get(0));
			} else {
				changeSelectedDocTo(addDocument());
			}
		}

		for(Listener l : listeners) {
			if(removed) { // closeDocument could be called multiple times by UI clock backlog
				l.docClosed(document);
			}
			if(changedSelection) {
				l.docSelected(selectedDocument);
			}
		}
	}
	
	public void saveAs(File file, boolean useWindowsLineEndings) throws IOException {
        LOG.info("saveAs: " + selectedDocument.getTitle() + " as " + file.getAbsolutePath());
        selectedDocument.saveAs(file, useWindowsLineEndings);
	}
	
	
	public void saveDocument(boolean useWindowsLineEndings) throws IOException {
        LOG.info("saveDocument: " + selectedDocument.getTitle());
        selectedDocument.save(useWindowsLineEndings);
	}
	
	/**
	 * Make the selected document, the next document in order.
	 */
	public void gotoNextDocument() {
        LOG.info("gotoNextDocument");
		int i = (documents.indexOf(selectedDocument)+1) % documents.size();
		setSelectedDocument(documents.get(i));
	}


	/**
	 * Make the selected document, the previous document in order.
	 */
	public void gotoPrevDocument() {
        LOG.info("gotoPrevDocument");
		int i = (documents.indexOf(selectedDocument)-1);
		i = i<0 ? documents.size()-1 : i;
		setSelectedDocument(documents.get(i));
	}
	
	public void setSelectedDocument(Document document) {

        LOG.info("setSelectedDocument: " + document.getTitle());

		if(documents.contains(document)) {
			if(!document.equals(selectedDocument)) {
				changeSelectedDocTo(document);
				// Add to navigation history if not navigating via back/forward
				if(!isNavigating) {
					// Remove any forward history when user navigates to a new document
					while(navigationHistory.size() > navigationPosition + 1) {
						navigationHistory.remove(navigationHistory.size() - 1);
					}
					navigationHistory.add(document);
					navigationPosition = navigationHistory.size() - 1;
				}
			}
			for(Listener l : listeners) {
				l.docSelected(document);
			}	
		} else {
			String msg = "I dont have doc: " + document.getTitle();
			LOG.warning(msg);
			throw new IllegalArgumentException(msg);
		}
		
	}

	/** Set the selected folder or clear selection with null.  */
	public void setSelectedFolder(File selectedFolder) {

        LOG.info("setSelectedFolder: " + selectedFolder);

        boolean noChange = (this.selectedFolder == selectedFolder)
				|| (selectedFolder!=null && selectedFolder.equals(this.selectedFolder));
        
        // check its an actual change
		if(!noChange) {
			if(selectedFolder==null || selectedFolder.isDirectory()) {
				this.selectedFolder = selectedFolder;
				for(Listener l : listeners) {
					l.folderSelected(selectedFolder);
				}	
			} else {
				String msg = "not a directory: " + selectedFolder;
				LOG.warning(msg);
				throw new IllegalArgumentException(msg);
			}
		}
		
		this.selectedFolder = selectedFolder;
	}
	
	public void setContent(String content) {
		LOG.info("setContent carat=" + selectedDocument.getCaratPosition());
		selectedDocument.setContent(content);
	}
	
	public List<Document> getDocuments() {
		return documents;
	}

	public Document getSelectedDocument() {
		return selectedDocument;
	}
	
	/** The selected folder or null if there is none */
	public File getSelectedFolder() {
		return selectedFolder;
	}

	/**
	 * @return true if any documents have unsaved changes, otherwise false.
	 */
	public boolean hasAnyUnsavedChanges() {
		for(Document d : documents) {
			if(d.hasUnsavedChanges()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return toStringHelper(this)
			.add("documentsSize", documents.size())
			.add("selectedDocument", selectedDocument)
			.add("listenersSize", listeners.size())
			.toString();
	}

	/**
	 * Insert text into a document at it's current carat position and make it selected.
	 */
	public void insertSelectedText(String text) {
		selectedDocument.insertSelectedText(text);
	}

	/**
	 * Insert text at current carat position, Without moving carat.
	 */
	public void insertText(String text) {
		selectedDocument.insertText(text);
	}
	
	public void closeAll() {
		
		List<Document> docs = new ArrayList<Document>(documents);
        LOG.info("closeAll");
        documents.clear();
        changeSelectedDocTo(addDocument());

		for(Listener l : listeners) {
			l.docSelected(selectedDocument);
		}
		for(Document closedDoc : docs) {
			for(Listener l : listeners) {
				l.docClosed(closedDoc);
			}
		}
	}
	/**
	 * Navigate in document history.
	 * @param direction -1 for back, +1 for forward
	 * @return true if navigation occurred, false otherwise
	 */
	private boolean navigate(int direction) {
	    if (direction == 0) {
	        throw new IllegalArgumentException("direction must be -1 or +1");
	    }
	    boolean canNavigate = direction < 0 ? canNavigateBack() : canNavigateForward();

	    LOG.info("navigate(" + direction + ") - position=" + navigationPosition + " historySize=" + navigationHistory.size());

	    if(!canNavigate) {
	        return false;
	    }

	    isNavigating = true;
	    try {
	        navigationPosition += direction;

	        while (navigationPosition >= 0 && navigationPosition < navigationHistory.size()) {
	            Document doc = navigationHistory.get(navigationPosition);
	            if (documents.contains(doc)) {
	                setSelectedDocument(doc);
	                return true;
	            }

	            // Document closed → remove and adjust index
	            navigationHistory.remove(navigationPosition);

	            if (direction < 0) {
	                // Moving backward: after removal, step backward again if possible
	                if (navigationPosition > 0) {
	                    navigationPosition--;
	                } else {
	                    break;
	                }
	            } else if (navigationPosition >= navigationHistory.size()) {
                    break;
	            }
	        }
	    } finally {
	        isNavigating = false;
	    }

	    return false;
	}
	
	public boolean navigateBack() { return navigate(-1); }

	public boolean navigateForward() { return navigate(+1); }


	/** @return true if there is a previous document in navigation history */
	public boolean canNavigateBack() {
		return navigationPosition > 0;
	}

	/** @return true if there is a next document in navigation history */
	public boolean canNavigateForward() {
		return navigationPosition < navigationHistory.size() - 1;
	}	
	
	
	

	public void setIgnoredFoldersRegex(Pattern ignoredFolderPattern) {
        LOG.info("setIgnoredFoldersRegex: " + ignoredFolderPattern);
        boolean noChange = (this.ignoredFolderPattern == ignoredFolderPattern)
				|| (ignoredFolderPattern!=null && ignoredFolderPattern.equals(this.ignoredFolderPattern));
        
        // check its an actual change
		if(!noChange) {
			this.ignoredFolderPattern = ignoredFolderPattern;
				for(Listener l : listeners) {
					l.ignoredFolderPatternSelected(ignoredFolderPattern);
				}	
		}
	}

	/** 
	 * Allows listening to doc add/close/select events.
	 * Also allows listening to {@link Document.Listener} events for the selected document. 
	 */
	public static interface Listener extends Document.Listener {
		public abstract void docAdded(Document document);
		public abstract void docClosed(Document document);
		public abstract void docSelected(Document document);
		public abstract void folderSelected(File selectedFolder);
		public abstract void ignoredFolderPatternSelected(Pattern ignoredFolderPattern);
	}
	
	/** Convenience abstract class for {@link Listener} */
	public static abstract class Adapter implements Listener {
		@Override public void docAdded(Document document) {}
		@Override public void docClosed(Document document) {}
		@Override public void docSelected(Document document) {}
		@Override public void docContentModified() { }
		@Override public void docCaratModified() { }
		@Override public void docSaved() { }
		@Override public void folderSelected(File selectedFolder) { }
		@Override public void ignoredFolderPatternSelected(Pattern ignoredFolderPattern) { }
	}
	
	/** Subscribe to document add/close/select events */
	public void addListener(Listener listener) {
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}
}
	
