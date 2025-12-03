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

import static com.timestored.theme.Theme.CIcon.DOCUMENT_NEW;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_F3;
import static java.awt.event.KeyEvent.VK_F4;
import static java.awt.event.KeyEvent.VK_F;
import static java.awt.event.KeyEvent.VK_G;
import static java.awt.event.KeyEvent.VK_H;
import static java.awt.event.KeyEvent.VK_N;
import static java.awt.event.KeyEvent.VK_S;
import static java.awt.event.KeyEvent.VK_SLASH;
import static java.awt.event.KeyEvent.VK_PERIOD;
import static java.awt.event.KeyEvent.VK_V;
import static java.awt.event.KeyEvent.VK_X;
import static java.awt.event.KeyEvent.VK_Y;
import static java.awt.event.KeyEvent.VK_Z;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;


import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

import jsyntaxpane.actions.FindNextAction;
import jsyntaxpane.actions.FindReplaceAction;
import jsyntaxpane.actions.GotoLineAction;
import jsyntaxpane.actions.RedoAction;
import jsyntaxpane.actions.ToggleCommentsAction;
import jsyntaxpane.actions.UndoAction;
import lombok.Getter;
import lombok.Setter;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.qstudio.UpdateHelper;
import com.timestored.swingxx.AAction;
import com.timestored.swingxx.SwingUtils;
import com.timestored.theme.Icon;
import com.timestored.theme.ShortcutAction;
import com.timestored.theme.Theme;
import com.timestored.theme.Theme.CIcon;

/**
 * Wraps an {@link OpenDocumentsModel} and provides common Document/Editor actions.
 * Document actions will remember what directory they last opened/saved and if the user
 * goes to repeat the action will begin in the previous directory. 
 */
public class DocumentActions {

	private static final Logger LOG = Logger.getLogger(DocumentActions.class.getName());
	/** Pop up dialog confirming user wants to open if file larger than this **/
	private static final long FILE_WARNING_SIZE_MB = 1;
	
	private final OpenDocumentsModel openDocumentsModel;
	/** remember the last folder we were in for any action to start there again */
	@Getter private File lastSelectedOpenFileFolder;
	private File lastSelectedSaveFolder;

	private final Action openFolderAction;
	private final Action closeFolderAction;
//	private final Action openFilesAction;
	private final Action saveFileAction;
	private final Action closeFileAction;
	private final Action closeAllFileAction = new CloseAllFileAction();
	private final Action saveAsFileAction = new SaveAsFileAction();
	private final Action newFileAction;
	private final List<Action> fileActions;
	private final Action nextDocumentAction;
	private final Action prevDocumentAction;	
	private final Action navigateBackAction;
	private final Action navigateForwardAction;
	
	private final Action cutAction;
	private final Action copyAction;
	private final Action selectCurrentStatementAction;
	private final Action upperAction;
	private final Action lowerAction;
	private final Action formatAction;
	
	private final List<Action> editorActions;

	private final Action generateDocumentationAction;
	private final Supplier<String[]> defaultFiletypeExtension;
	
	@Setter @Getter private boolean saveWithWindowsLineEndings = true;

	private static final int shortModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	private static final KeyStroke OUTLINE_DOC_KS = KeyStroke.getKeyStroke(KeyEvent.VK_I, shortModifier);

	
	/**
	 * @param defaultFiletypeExtension File extension used as default when saving.
	 */
	public DocumentActions(final OpenDocumentsModel openDocumentsModel, Supplier<String[]> defaultFiletypeExtension) {
		this(openDocumentsModel, defaultFiletypeExtension, null);
	}
	

	/**
	 * @param defaultFiletypeExtension File extension used as default when saving.
	 */
	public DocumentActions(final OpenDocumentsModel openDocumentsModel, 
			Supplier<String[]> defaultFiletypeExtension, Action generateDocumentationAction) {
		
		this.openDocumentsModel = Preconditions.checkNotNull(openDocumentsModel);
		this.defaultFiletypeExtension = defaultFiletypeExtension;
		
		this.generateDocumentationAction = generateDocumentationAction;
		if(generateDocumentationAction!=null) {
			generateDocumentationAction.putValue(Action.MNEMONIC_KEY, VK_D);
		}

		// NOT used as opening file in qStudio is more complicated, it may be a duckdb/parquet file.
//		openFilesAction = new ShortcutAction(Msg.get(Key.OPEN_FILE) + "...", 
//				DOCUMENT_OPEN, VK_O) {
//			
//			private static final long serialVersionUID = 1L;
//
//			@Override public void actionPerformed(ActionEvent arg0) {
//
//	    		JFileChooser fc = getFileChooser(lastSelectedOpenFileFolder);
//	    		fc.setMultiSelectionEnabled(true);
//	            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//					openFiles(Arrays.asList(fc.getSelectedFiles()));
//		        } else {
//		        	LOG.info(Msg.get(Key.OPEN_CANCELLED));
//		        }
//		   }
//		};
		
		final String oFolder = Msg.get(Key.OPEN_FOLDER);
		openFolderAction = new AbstractAction(oFolder + "...") {
			
			private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = getFileChooser(lastSelectedOpenFileFolder);
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.setMultiSelectionEnabled(false);
				fc.setDialogTitle(Msg.get(Key.BROWSE_FOLDER));
				fc.setApproveButtonText(oFolder);
				
		        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
	            	File f = fc.getSelectedFile();
	            	if(f.isDirectory()) {
		            	lastSelectedOpenFileFolder = f;
		            	openDocumentsModel.setSelectedFolder(f);
	            	} else {
	            		String message = Msg.get(Key.INVALID_DIRECTORY);
	            		JOptionPane.showMessageDialog(null, message);
	            	}
		        } else {
		        	LOG.info(Msg.get(Key.OPEN_CANCELLED));
		        }
		   }
		};
		
		closeFolderAction = new AAction(Msg.get(Key.CLOSE_FOLDER), e -> openDocumentsModel.setSelectedFolder(null));
		
		newFileAction = new ShortcutAction(Msg.get(Key.NEW_FILE), 
				DOCUMENT_NEW, VK_N) {
			
			private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent arg0) {
				openDocumentsModel.addDocument();
		   }
		};
			
		closeFileAction = new ShortcutAction(Msg.get(Key.CLOSE), 
				null, Msg.get(Key.CLOSE), VK_C, VK_F4) {
			
			private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent arg0) {
	            closeWithConfirmation(openDocumentsModel.getSelectedDocument());
		   }
		};

//		super("Save file", Theme.Icon.DOCUMENT_SAVE.get16());
//        putValue(SHORT_DESCRIPTION, "Save file");
//        putValue(MNEMONIC_KEY, KeyEvent.VK_S);

		saveFileAction = new ShortcutAction(Msg.get(Key.SAVE_FILE), CIcon.SAVE, VK_S) {

			private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent ae) {
				try {
					Document selDoc = openDocumentsModel.getSelectedDocument();
					if (selDoc.getFilePath() != null) {
						openDocumentsModel.saveDocument(saveWithWindowsLineEndings);
					} else {
						letUserChooseFileAndSave();
					}
				} catch (IOException e) {
					String msg = Msg.get(Key.SAVE_FILE_ERROR);
					LOG.info(msg);
					JOptionPane.showMessageDialog(null, msg, Msg.get(Key.SAVE_ERROR), JOptionPane.ERROR_MESSAGE);
				}
			}
		};


		nextDocumentAction = new ShortcutAction(Msg.get(Key.NEXT_DOCUMENT), null, KeyEvent.VK_PAGE_DOWN) {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent ae) {
				openDocumentsModel.gotoNextDocument();
			}
		};

		prevDocumentAction = new ShortcutAction(Msg.get(Key.PREV_DOCUMENT), null, KeyEvent.VK_PAGE_UP) {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent ae) {
				openDocumentsModel.gotoPrevDocument();
			}
		};
		
		// Navigation back/forward actions (like browser back/forward)
		navigateBackAction = new AbstractAction("Navigate Back", Theme.CIcon.GREEN_BACK.get16()) {
			private static final long serialVersionUID = 1L;
			{
				putValue(Action.SHORT_DESCRIPTION, "Navigate Back (Alt+Left)");
				putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(VK_LEFT, java.awt.event.InputEvent.ALT_DOWN_MASK));
			}
			@Override public void actionPerformed(ActionEvent ae) {
				openDocumentsModel.navigateBack();
			}
		};

		navigateForwardAction = new AbstractAction("Navigate Forward", Theme.CIcon.GREEN_FORWARD.get16()) {
			private static final long serialVersionUID = 1L;
			{
				putValue(Action.SHORT_DESCRIPTION, "Navigate Forward (Alt+Right)");
				putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(VK_RIGHT, java.awt.event.InputEvent.ALT_DOWN_MASK));
			}
			@Override public void actionPerformed(ActionEvent ae) {
				openDocumentsModel.navigateForward();
			}
		};
		
		
		fileActions = Collections.unmodifiableList(Arrays.asList(openFolderAction, closeFileAction, 
				closeAllFileAction, closeFolderAction, saveFileAction, saveAsFileAction));
		
		
		/*
		 * Editor Actions - related to actions inside one selected document
		 */
		selectCurrentStatementAction = new SelectCurrentStatementAction();
		upperAction = new UpperAction();
		lowerAction = new LowerAction();
		formatAction = new FormatAction();
		cutAction = getAction(Msg.get(Key.CUT), CIcon.EDIT_CUT, new DefaultEditorKit.CutAction(), VK_X);
		copyAction = getAction(Msg.get(Key.COPY), CIcon.EDIT_COPY, new DefaultEditorKit.CopyAction(), VK_C);
		Action findNextAction = getAction(Msg.get(Key.FIND_NEXT), CIcon.EDIT_FIND_NEXT, new FindNextAction(), VK_F3);
		Action toggleCommentAction = getAction(Msg.get(Key.TOGGLE_COMMENTS), 
				CIcon.EDIT_COMMENT, new ToggleCommentsAction(), VK_SLASH);
		
		// customise
		toggleCommentAction.putValue("LineComments", "/ "); // comment format
		findNextAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("F3"));
		
		List<Action> acts = new ArrayList<Action>();
		acts.add(cutAction);
		acts.add(copyAction);
		acts.add(getAction(Msg.get(Key.PASTE), CIcon.EDIT_PASTE, new DefaultEditorKit.PasteAction(), VK_V));
		acts.add(toggleCommentAction);
		acts.add(getAction(Msg.get(Key.FIND) + "...", CIcon.EDIT_FIND, new FindReplaceAction(), VK_H));
		acts.add(findNextAction);
		acts.add(getAction(Msg.get(Key.GOTO_LINE) + "...", CIcon.EDIT_GOTO_LINE, new GotoLineAction(), VK_G));
		acts.add(navigateBackAction);
		acts.add(navigateForwardAction);
		acts.add(null);
		acts.add(selectCurrentStatementAction);
		acts.add(formatAction);
		acts.add(upperAction);
		acts.add(lowerAction);
		editorActions = Collections.unmodifiableList(acts);
		
		// update on any selected doc change
		openDocumentsModel.addListener(new OpenDocumentsModel.Adapter() {
			@Override public void docSelected(Document document) {
				refresh();
			}
			
			@Override public void docSaved() {
				refresh();
			}
			
			@Override public void docContentModified() {
				refresh();
			}
			
			@Override public void docCaratModified() {
				refresh();
			}
		});
		
		refresh();
	}
	
    public class SelectCurrentStatementAction extends TextAction {
        public SelectCurrentStatementAction() { 
        	super("Select Current Statement");
    		Toolkit tk = Toolkit.getDefaultToolkit();
    		KeyStroke k = KeyStroke.getKeyStroke(VK_PERIOD, tk.getMenuShortcutKeyMask() | java.awt.Event.SHIFT_MASK);
    		putValue(Action.ACCELERATOR_KEY, k);
        }
        public void actionPerformed(ActionEvent e) {
        	int[] startEnd = openDocumentsModel.getSelectedDocument().getCurrentStatementBounds();
        	JTextComponent txtc = getTextComponent(e);
        	int[] bounds = Document.getStatementBounds(txtc.getText(), txtc.getCaretPosition());
        	txtc.setSelectionStart(bounds[0]);
        	txtc.setSelectionEnd(bounds[1]);
        }
    }
	
    public static class UpperAction extends TextAction {
        public UpperAction() { super("UPPERCASE"); }
        public void actionPerformed(ActionEvent e) {
            replaceSelected(getTextComponent(e), String::toUpperCase);
        }
    }
	
    public static class FormatAction extends TextAction {
        public FormatAction() { 
        	super("Format SQL");
    		Toolkit tk = Toolkit.getDefaultToolkit();
    		KeyStroke k = KeyStroke.getKeyStroke(VK_F, tk.getMenuShortcutKeyMask() | java.awt.Event.ALT_MASK);
    		putValue(Action.ACCELERATOR_KEY, k);
        }
        public void actionPerformed(ActionEvent e) {
        	FormatConfig cfg = FormatConfig.builder().maxColumnLength(120).indent("\t").build();
        	JTextComponent tc = getTextComponent(e);
        	if(tc.getSelectionStart() == tc.getSelectionEnd()) {
        		tc.setSelectionStart(0);
        		tc.setSelectionEnd(tc.getText().length()-1);
        	}
            replaceSelected(getTextComponent(e), s -> SqlFormatter.format(s, cfg));	
        }
    }
    
    static void replaceSelected(JTextComponent target, Function<String,String> transform) {
        if (target != null) {
        	int start = target.getSelectionStart();
        	int end = target.getSelectionEnd();
        	if(end > start) {
        		target.replaceSelection(transform.apply(target.getSelectedText()));
	        	target.setSelectionStart(start);
	        	target.setSelectionEnd(end);
        	}
        }
    }

    public static class LowerAction extends TextAction {
        public LowerAction() { super("lowercase"); }
        public void actionPerformed(ActionEvent e) {
            replaceSelected(getTextComponent(e), String::toLowerCase);
        }
    }
    
	public Action configureAction(Action action, KeyStroke keyStroke, String shortDescription) {
		action.putValue(Action.SHORT_DESCRIPTION, shortDescription);
		action.putValue(Action.ACCELERATOR_KEY, keyStroke);
		return action;
	}
	
	/** Open file notify the user with a dialog if it couldn't be opened. */
	public void openFile(File file) {
		openFiles(Arrays.asList(new File[] { file }));
	}
	
	/**
	 * Open the list of files, notify the user with a dialog  
	 * of any files which couldn't be opened.
	 */
	public void openFiles(List<File> files) {
		String errMsg = "";
    	for(File f : files) {
    		// if file is over a certain size, double check with user before proceeding
    		boolean proceed = true; 
    		double fileSizeMb = (f.length()/(1024.0*1024));
    		if(fileSizeMb > FILE_WARNING_SIZE_MB) {
    	        DecimalFormat df = new DecimalFormat("##.00"); 
    			String msg = f.getName() + " file is " + df.format(fileSizeMb) + " MB and may take some time to open. Proceed?";
    			proceed = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, msg);
    		}
    		
            try {
            	if(proceed) {
	            	openDocumentsModel.openDocument(f);	
	            	lastSelectedOpenFileFolder = f.getParentFile();
            	}
			} catch (IOException e) {
				errMsg += "\r\n" + f.getAbsolutePath();
		        LOG.log(Level.WARNING, "openFiles exception", e);
			}
    	}
    	if(errMsg.length()>0) {
    		String msg = Msg.get(Key.EROR_OPENING_FILES) + errMsg;
	        JOptionPane.showMessageDialog(null, msg, Msg.get(Key.ERROR_OPENING), JOptionPane.ERROR_MESSAGE);
    	}
	}
	
	/**
	 * @return All actions related to editing a single document,
	 * copy,paste,cut,toggle-comments etc. Buttons are setEnabled 
	 * when this function is called so do not store menu and reuse.
	 */
	public List<Action> getEditorActions() {
		refresh();
		return editorActions;
	}
	
	private static Action getAction(String title, Icon icon, Action action, 
			int acceleratorKey) {
        action.putValue(Action.NAME, title);
    	action.putValue(Action.SMALL_ICON, icon.get16());
		KeyStroke k;
		k = KeyStroke.getKeyStroke(acceleratorKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		action.putValue(Action.ACCELERATOR_KEY, k);
        return action;
	}
	
	/** Lets user choose a folder to open. */
	public Action getOpenFolderAction() { return openFolderAction; }

	/** Lets user choose file, opens a new document, or shows error why it filed. */
	public Action getNewFileAction() 	{ return newFileAction; }

	/** Save selected document as pre-known location if known, else prompt. */
	public Action getSaveFileAction() 	{ return saveFileAction; }
	
	public Action getSaveAsFileAction() { return saveAsFileAction; }

	/** Close the currently selected document. */
	public Action getCloseFileAction() 	{ return closeFileAction; }
	
	/**
	 * @param document Usually a document in our {@link OpenDocumentsModel}.
	 * @return close action for a particular document, if it is not contained or
	 * is null action will be disabled.
	 */
	public Action getCloseFileAction(final Document document) {
		String t = document==null ? "" : document.getTitle();
		Action a = new AAction(Msg.get(Key.CLOSE) + " " + t, e-> closeWithConfirmation(document));
		a.setEnabled(document!=null && 
			openDocumentsModel.getDocuments().contains(document));
		
		return a;
	}

	/**
	 * @param document Usually a document in our {@link OpenDocumentsModel}.
	 * @return close action for a particular document, if it is not contained or
	 * is null action will be disabled.
	 */
	public Action getCloseOtherFilesAction(final Document document) {
	    Action closeOthersAction = new AAction("Close Other Tabs", e -> {
				for(Document d : openDocumentsModel.getDocuments()) {
					if(!d.equals(document)) {
						getCloseFileAction(d).actionPerformed(null);
					}
				}
	    	});
		closeOthersAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
		return closeOthersAction;
	}



	
	public Action getCloseAllFileAction() {
		return closeAllFileAction;
	}
	
	private class CloseAllFileAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public CloseAllFileAction() {
			super(Msg.get(Key.CLOSE_ALL));
	        putValue(SHORT_DESCRIPTION, Msg.get(Key.CLOSE_ALL));
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		}
		
		@Override public void actionPerformed(ActionEvent arg0) {
			for(Document d : openDocumentsModel.getDocuments()) {
				closeWithConfirmation(d);
			}
	   }
	}

	private class CloseAllToRightFileAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final Document document;

		public CloseAllToRightFileAction(Document document) {
			super(Msg.get(Key.CLOSE_ALL) + " to the Right");
	        putValue(SHORT_DESCRIPTION, Msg.get(Key.CLOSE_ALL) + " to the Right");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
			this.document = Preconditions.checkNotNull(document);
		}
		
		@Override public void actionPerformed(ActionEvent arg0) {
			boolean foundMe = false;
			for(Document d : openDocumentsModel.getDocuments()) {
				if(foundMe) {
					closeWithConfirmation(d);
				} else {
					foundMe = foundMe || d.equals(document);
				}
			}
	   }
	}


	private void closeWithConfirmation(Document document) {
		LOG.info("closeWithConfirmation: " + document);
		try {
			if(document.hasUnsavedChanges()) {
				String message = Msg.get(Key.DOCUMENT) + document.getTitle()
						+ Msg.get(Key.UNSAVED_CHANGES_CONFIRM);
				int choice = JOptionPane.showConfirmDialog(null, message);
				if(choice == JOptionPane.YES_OPTION) {
					if(document.getFilePath() != null) {
						openDocumentsModel.saveDocument(saveWithWindowsLineEndings);
					} else {
						letUserChooseFileAndSave();
					}
				} else if(choice == JOptionPane.NO_OPTION){
					openDocumentsModel.closeDocument(document);
				}
			} else {
				openDocumentsModel.closeDocument(document);
			}
        } catch(IOException e) {
			String msg = Msg.get(Key.ERROR_SAVING);
	        LOG.info(msg);
	        JOptionPane.showMessageDialog(null, msg, msg, JOptionPane.ERROR_MESSAGE);
        }
	}
	
	/**
	 * Lets user choose file, opens a new document, or shows error why it filed.
	 */
	private class SaveAsFileAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;

		public SaveAsFileAction() {
			super(Msg.get(Key.SAVE_AS) + "...", Theme.CIcon.SAVE_AS.get16());
	        putValue(SHORT_DESCRIPTION, Msg.get(Key.SAVE_AS) + "...");
	        putValue(MNEMONIC_KEY, KeyEvent.VK_A);
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			letUserChooseFileAndSave();
	   }
	}


	private void letUserChooseFileAndSave() {
		
		File file = SwingUtils.askUserSaveLocation(lastSelectedSaveFolder, defaultFiletypeExtension.get());
        if (file != null) {
            try {
            	openDocumentsModel.saveAs(file, saveWithWindowsLineEndings);
            	lastSelectedSaveFolder = file.getParentFile();
			} catch (IOException e) {
				String msg = Msg.get(Key.ERROR_SAVING) + ": " + file;
		        LOG.info(msg);
		        JOptionPane.showMessageDialog(null, msg, Msg.get(Key.ERROR_SAVING), JOptionPane.ERROR_MESSAGE);
			}
        } else {
        	LOG.info(Msg.get(Key.SAVE_CANCELLED));
        }
	}
	

	private void refresh() {
		Document d = openDocumentsModel.getSelectedDocument();
		// document actions
		saveFileAction.setEnabled(d.hasUnsavedChanges());
		// editor actions
		cutAction.setEnabled(d.isTextSelected());
		copyAction.setEnabled(d.isTextSelected());
		lowerAction.setEnabled(d.isTextSelected());
		upperAction.setEnabled(d.isTextSelected());
		formatAction.setEnabled(true);
		navigateBackAction.setEnabled(openDocumentsModel.canNavigateBack());
		navigateForwardAction.setEnabled(openDocumentsModel.canNavigateForward());
	}

	/** Let user undo action in latest editor */
	public Action getUndoAction() {
		return getAction(Msg.get(Key.UNDO_TYPING), CIcon.EDIT_UNDO, new UndoAction(), VK_Z);
	}

	/** Let user redo action in latest editor */
	public Action getRedoAction() {
		return getAction(Msg.get(Key.REDO), CIcon.EDIT_REDO, new RedoAction(), VK_Y);
	}
	
	/**
	 * @return All user actions available for {@link OpenDocumentsModel} related to files.
	 */
	public List<Action> getFileActions() {
		return fileActions;
	}
	
	/*
	 * MISC ACTIONS
	 */
	private static JFileChooser getFileChooser(File lastSelection) {
		if(lastSelection!=null) {
			return new JFileChooser(lastSelection);
		}
		return new JFileChooser();
	}
	
	public Action getGenerateDocumentationAction() {
		return generateDocumentationAction;
	}

	public List<Action> getOpenRecentActions(List<String> filePaths) {
		List<Action> r = Lists.newArrayListWithExpectedSize(filePaths.size());
        int i=1;
        for(final String fp : filePaths) {
        	r.add(new AAction(i + " " + fp, e ->  openFile(new File(fp))));
        	i++;
        }
        return r;
	}

	public Action openAllAction(final List<String> filePaths) {
		return new AAction(Msg.get(Key.OPEN_ALL_RECENT), e -> {
				List<File> files = Lists.newArrayList();
		        for(final String fp : filePaths) {
		        	files.add(new File(fp));	
		        }
		        openFiles(files);
			});
	}

	/**
	 * Internally the last folder accessed is tracked and is the starting point when the
	 * user next goes to open or close a file. By setting the default, that becomes the starting
	 * point for the next user open/save.
	 */
	public void setDefaultFolder(File folder) {
		if(folder != null && folder.isDirectory()) {
			this.lastSelectedOpenFileFolder = folder;
			this.lastSelectedSaveFolder = folder;
		}
	}
	
	public void addActionsToEditor(JComponent component, Document document) {

		addAction(component, nextDocumentAction);
		addAction(component, prevDocumentAction);
	}
	

	private void addAction(JComponent component, Action action) {
		String mapkey = (String) action.getValue(Action.NAME);
		component.getActionMap().put(mapkey, action);
		KeyStroke keyStroke = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
		component.getInputMap().put(keyStroke, mapkey);
	}


	public Action getCloseAllToRightFileAction(Document document) {
		return new CloseAllToRightFileAction(document);
	}
}
