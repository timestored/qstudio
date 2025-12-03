/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2023 TimeStored
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
package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.syntaxkits.BashSyntaxKit;
import jsyntaxpane.syntaxkits.DOSBatchSyntaxKit;
import jsyntaxpane.syntaxkits.JavaScriptSyntaxKit;
import jsyntaxpane.syntaxkits.JavaSyntaxKit;
import jsyntaxpane.syntaxkits.PlainSyntaxKit;
import jsyntaxpane.syntaxkits.QSqlSyntaxKit;
import jsyntaxpane.syntaxkits.RflSyntaxKit;
import jsyntaxpane.syntaxkits.SqlSyntaxKit;
import jsyntaxpane.syntaxkits.PrqlSyntaxKit;
import jsyntaxpane.syntaxkits.DosSyntaxKit;
import jsyntaxpane.util.Configuration;
import lombok.Setter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.timestored.docs.Document;
import com.timestored.docs.DocumentActions;
import com.timestored.docs.DocumentsPopupMenu;
import com.timestored.docs.FileDropDocumentHandler;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.qstudio.EditorConfigFactory.EditorConfig;
import com.timestored.swingxx.TabbedPaneRightClickBlocker;

/**
 * Tabbed interface for displaying/editing of documents in {@link OpenDocumentsModel}.
 * NOT isntance safe as it relies on a static configuration {@link JavaSyntaxKit} related.
 */
public class ServerDocumentPanel  extends JPanel implements GrabableContainer {
	private static final Logger LOG = Logger.getLogger(ServerDocumentPanel.class.getName());
	
	private static final long serialVersionUID = 1L;
	private static final int SPLITPANE_WIDTH = 200;
	private boolean ignoredOnce = false;
	
	/** tab of open documents, maintains same order as {@link OpenDocumentsModel#getDocuments()} */
	private final JTabbedPane tabbedPane;
	private final OpenDocumentsModel openDocModel;
	private final CommonActions commonActions;
	private final DocumentActions documentActions;
	private final QDocController qDocController;
	private final FileDropDocumentHandler.Listener fileDropListener;
	private final JFrame parentFrame;
	@Setter private String assumedFileEnding = "q";
	
	/** flag to prevent changes we make coming back from model and causing infinite loop */
	private boolean iAmChangingSelection = false;
	private Font editorFont = null;

	private TabLayoutPolicy tabLayoutPolicy;


	static {
		try {
			DefaultSyntaxKit.initKit(); // to make it jsysntaxpane
		} catch(Exception e) {
			LOG.severe("Could not init code highlighter: " + e);
		}
	}
	
	ServerDocumentPanel(CommonActions commonActions, 
			final DocumentActions documentActions, 
			final OpenDocumentsModel openDocModel,
			final JFrame parentFrame, 
			final QDocController qDocController,
			FileDropDocumentHandler.Listener fileDropListener) {

        /*
         * Construct our model
         */
        this.openDocModel = openDocModel;
        this.commonActions = commonActions;
        this.documentActions = documentActions;
        this.parentFrame = parentFrame;
        this.qDocController = Preconditions.checkNotNull(qDocController);
        this.fileDropListener = Preconditions.checkNotNull(fileDropListener);


        // create the tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setName("serverDocsTabbedPane");
        tabbedPane.setMinimumSize(new Dimension(SPLITPANE_WIDTH*2, 1));
        tabbedPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		for(Document document : openDocModel.getDocuments()) {
	        addDocToTabs(document);
		}
		
		// convulated wrapping to prevent right-clicks changing tabs.
		TabbedPaneRightClickBlocker.install(tabbedPane);
        // this pops up between gaps in the tabs.
		tabbedPane.addMouseListener(new MouseAdapter() {

			@Override public void mouseClicked(MouseEvent e) { maybeHandleClick(e); }

			@Override public void mouseReleased(MouseEvent e) { maybeHandleClick(e); }

			private void maybeHandleClick(MouseEvent e) {
				if (e.isPopupTrigger()) {
					// Find the closest doc.
					if(e.getSource() instanceof JTabbedPane) {
					     JTabbedPane tabs = (JTabbedPane)e.getSource();
				    	 int idx = tabs.indexAtLocation(e.getX(), e.getY());
			    	 	if(idx >= 0 && idx < openDocModel.getDocuments().size()) {
			    	 		Document doc = openDocModel.getDocuments().get(idx);
			    	 		if(doc != null) {
								openDocModel.setSelectedDocument(doc);
			    	 			new DocumentsPopupMenu(documentActions, doc).show(e.getComponent(), e.getX(), e.getY());
			    	 		}
			    	 	}
					}
				}
			}
		});

        // set appearance
		setLayout(new BorderLayout());
        add(tabbedPane);
        refreshTabAppearance();
        openDocModel.addListener(new UpdateTabsListener());

        tabbedPane.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				if(!iAmChangingSelection) {
					Component c = tabbedPane.getSelectedComponent();
					if(c!=null && c instanceof QCodeEditorPanel) {
						QCodeEditorPanel docEditor = ((QCodeEditorPanel) c);
						Document doc = docEditor.getDoc();
						if(doc!=null && openDocModel.getDocuments().contains(doc) 
								&& !openDocModel.getSelectedDocument().equals(doc)) {
							openDocModel.setSelectedDocument(doc);
						}
					}
				}
			}
		});
	}

	@Setter private static Consumer<EditorConfig> editorConfigUpdater = ec -> {};
	
	
	public void setTabLayoutPolicy(TabLayoutPolicy tabLayoutPolicy) {
    		if(tabLayoutPolicy != null && !tabLayoutPolicy.equals(this.tabLayoutPolicy)) {
    			this.tabLayoutPolicy = tabLayoutPolicy;
        		tabbedPane.setTabLayoutPolicy(tabLayoutPolicy.isScroll() ? 
        					JTabbedPane.SCROLL_TAB_LAYOUT : JTabbedPane.WRAP_TAB_LAYOUT);
        		tabbedPane.setTabPlacement(tabLayoutPolicy.isVertical() ? JTabbedPane.LEFT : JTabbedPane.NORTH);
        		UIManager.put("TabbedPane.tabRotation", tabLayoutPolicy.isVertical() ? "auto" : "none");	
    		}
    }
	
	/**
	 * Apply settings to the jsyntaxpane editor, this must be called before any are constructed.
	 * If called after, you must then call applyEditorConfig on any instances to refresh them.
	 */
	public static void setEditorConfig(EditorConfig editorConfig) {
		editorConfigUpdater.accept(editorConfig);
		applyConfigToSyntaxKits(editorConfig);
	}
	
	public static void applyConfigToSyntaxKits(EditorConfig editorConfig) {
		editorConfig.apply(DefaultSyntaxKit.getConfig(DefaultSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(SqlSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(JavaScriptSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(BashSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(DOSBatchSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(PlainSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(QSqlSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(DosSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(RflSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(PrqlSyntaxKit.class));
		
		editorConfig.apply(DefaultSyntaxKit.getConfig(SqlSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(JavaScriptSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(BashSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(DOSBatchSyntaxKit.class));
		editorConfig.apply(DefaultSyntaxKit.getConfig(PlainSyntaxKit.class));
	}

	/**
     * A convenience print method that displays a print dialog, and then
     * prints this {@code JTextComponent} in <i>interactive</i> mode with 
     * the specified header and footer text. Note: this method
     * blocks until printing is done.
     * <p>
     * Note: In <i>headless</i> mode, no dialogs will be shown.
     * 
     * @param headerFormat the text, in {@code MessageFormat}, to be
     *        used as the header, or {@code null} for no header
     * @param footerFormat the text, in {@code MessageFormat}, to be
     *        used as the footer, or {@code null} for no footer
     * @return {@code true}, unless printing is canceled by the user
     * @throws PrinterException if an error in the print system causes the job
     *         to be aborted
     * @throws SecurityException if this thread is not allowed to
     *                           initiate a print job request
     *         
     */
	public boolean print(MessageFormat headerFormat, MessageFormat footerFormat) throws PrinterException {
		Component c = tabbedPane.getSelectedComponent();
		if(c!=null && c instanceof QCodeEditorPanel) {
			QCodeEditorPanel docEditor = ((QCodeEditorPanel) c);
			return docEditor.print(headerFormat, footerFormat);
		}
		return false;
	}
	
	/**
	 * Set the font used in all editors
	 */
	public void setEditorFont(Font font) {
		editorFont  = Preconditions.checkNotNull(font);
		synchronized(tabbedPane.getTreeLock()) {
			for(Component c : tabbedPane.getComponents()) {
				if(c instanceof QCodeEditorPanel) {
					((QCodeEditorPanel) c).setEditorFont(font);
				}
			}
		}
	}
	
	private void addDocToTabs(final Document document) {
		
		final QCodeEditorPanel codep = new QCodeEditorPanel(document);
		if(editorFont != null) {
			codep.setEditorFont(editorFont);
		}

        // tab forward backward shortcuts
		documentActions.addActionsToEditor(codep, document);
		if(fileDropListener != null) {
			FileDropDocumentHandler fileDropHandler = new FileDropDocumentHandler(codep.getTransferHandler());
			fileDropHandler.addListener(fileDropListener);
			codep.setTransferHandler(fileDropHandler);
		}
		tabbedPane.add(codep);
		tabbedPane.setSelectedComponent(codep);
	}


	private QCodeEditorPanel getComponent(Document document) {
		synchronized(tabbedPane.getTreeLock()) {
			for(Component c : tabbedPane.getComponents()) {
				if(c instanceof QCodeEditorPanel) {
					QCodeEditorPanel codeEditorPanel = (QCodeEditorPanel) c;
					if(codeEditorPanel.getDoc().equals(document)) {
						return codeEditorPanel;
					}
				}
			}
			return null;
		}
	}

	@Override public boolean requestFocusInWindow() {
		Component c = tabbedPane.getSelectedComponent();
		if(c!=null) {
			return c.requestFocusInWindow();
		}
		return false;
	}

	@Override public void requestFocus() {
		Component c = tabbedPane.getSelectedComponent();
		if(c!=null) {
			c.requestFocus(); 
		}
	}

	
	public int getNumberOfDocumentsOpen() { return openDocModel.getDocuments().size(); }
	
	private void refreshTabAppearance() {
		
		for(final Document doc : openDocModel.getDocuments()) {
			final QCodeEditorPanel codeEditorPanel = getComponent(doc);
			if(codeEditorPanel != null) {
				int idx = tabbedPane.indexOfComponent(codeEditorPanel);
				if(idx != -1) {
					tabbedPane.setTabComponentAt(idx, getTabComponent(doc));
				}
			}
		}
		requestFocusInWindow();
	}
// UNSAFE AS point may not be visible
//	public Point getPopupPoint() {
//		QCodeEditorPanel qCodeEd = getComponent(openDocModel.getSelectedDocument());
//		return qCodeEd.getPopupPoint();
//	}

	private JPanel getTabComponent(final Document doc) {
		
		boolean horiz = this.tabLayoutPolicy == null || !this.tabLayoutPolicy.isVertical();
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,  horiz ? 2 : 0, horiz ? 0 : 2));
		p.setName("tab-" + doc.getTitle());
		
		JLabel label = new JLabel(doc.getTitle(), doc.getIcon().get16(), SwingConstants.LEFT);
		label.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				if(SwingUtilities.isLeftMouseButton(e)) {
					openDocModel.setSelectedDocument(doc);
				} else if(SwingUtilities.isMiddleMouseButton(e)) {
					documentActions.getCloseFileAction(doc).actionPerformed(null);
				}
				super.mouseClicked(e);
			}
			
			@Override public void mouseReleased(MouseEvent e) {
				if(SwingUtilities.isRightMouseButton(e)) {
					openDocModel.setSelectedDocument(doc);
		            new DocumentsPopupMenu(documentActions, doc).show(e.getComponent(), e.getX(), e.getY());
				} 
				super.mouseReleased(e);
			}
		});
		
		label.setToolTipText(doc.getFilePath());
//		label.setOpaque(false);
		p.add(label);
		// This logic was to color tabs based on filename to allow easy finding.
		// WHen I added better dark theme support I realised it just wasn't that useful
//		p.setOpaque(true);
//		int h = Math.abs(doc.getTitle().hashCode());
//		int r = 192 + h%64;
//		int g = 192 + (h/64)%64;
//		int b = 192 + (h/64*64)%64;
//		p.setBackground(new Color(r, g, b));
		
		JButton closeButton = new JButton("x");
		closeButton.setMaximumSize(new Dimension(16, 16));
		closeButton.setPreferredSize(new Dimension(16, 16));
		closeButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
		closeButton.setBorder(BorderFactory.createEmptyBorder());
	    final Color originalBg = closeButton.getBackground();

	    closeButton.addMouseListener(new MouseAdapter() {
	        @Override
	        public void mouseEntered(MouseEvent e) {
	            closeButton.setBackground(Color.RED);
	            // if you want the red to actually fill, you may need to turn content area filled on:
	            closeButton.setContentAreaFilled(true);
	        }
	        @Override
	        public void mouseExited(MouseEvent e) {
	            closeButton.setBackground(originalBg);
	            closeButton.setContentAreaFilled(false);
	        }
	    });
	    
		closeButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				documentActions.getCloseFileAction(doc).actionPerformed(null);
			}
		});

		p.add(closeButton);
		return p;
	}
	
	
	private class UpdateTabsListener implements OpenDocumentsModel.Listener {

		@Override public void docClosed(final Document document) {
			EventQueue.invokeLater(new Runnable() {
				@Override public void run() {
					iAmChangingSelection = true;
					QCodeEditorPanel qCodeEditorPanel = getComponent(document);
					if(qCodeEditorPanel != null) { // quickly sending multiple closes in UI may mean it's already gone.
						qCodeEditorPanel.setTransferHandler(null);
						tabbedPane.remove(qCodeEditorPanel);
						iAmChangingSelection = false;
					}
				}
			});
		}
		@Override public void docSelected(final Document document) {
			// show it if we are not already
		        EventQueue.invokeLater(new Runnable() {
					@Override public void run() {
						synchronized(tabbedPane.getTreeLock()) {
							QCodeEditorPanel cep;
							cep = (QCodeEditorPanel)tabbedPane.getSelectedComponent();
							if(!cep.getDoc().equals(document)) {
								refreshTabAppearance();
								iAmChangingSelection = true;
								QCodeEditorPanel newDocEditor = getComponent(document);
								try {
									tabbedPane.setSelectedComponent(newDocEditor);
								} catch(IllegalArgumentException e) {
									// Workaround for occasional boot-time issue on Windows 10 
									if(!ignoredOnce) {
										ignoredOnce = true;
										LOG.warning("Ignoring setSelectedComponent");
									} else {
										throw e;
									}
								}
								iAmChangingSelection = false;
							}
							requestFocusInWindow();
						}
					}
				});
		}
		
		@Override public void docAdded(final Document document) {
	        EventQueue.invokeLater(new Runnable() {
				@Override public void run() {
			        addDocToTabs(document);
			        refreshTabAppearance();
				}
			});
		}

		@Override public void docSaved() {
			final QCodeEditorPanel codeEditorPanel = getComponent(openDocModel.getSelectedDocument());
			codeEditorPanel.setContentType();
	        refresh();
		}
		
		@Override public void docContentModified() {
	        refresh();
		}

		private void refresh() {
			EventQueue.invokeLater(new Runnable() {
				@Override public void run() {
					refreshTabAppearance();
				}
			});
		}
		
		@Override public void docCaratModified() {	}
		@Override public void folderSelected(File selectedFolder) { }
		@Override public void ignoredFolderPatternSelected(Pattern ignoredFolderPattern) { }
	}


	/**
	 * Popup menu shown when user right clicks on edit area.
	 */
	 private class EditorPopupMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;

		private EditorPopupMenu(List<Action> appendedActions) {
	        setName("EditorPopupMenu");

	        for(Action ca : commonActions.getQueryActions()) {
	        	add(ca).setName(ca.getValue(Action.NAME) + "-action");
	        }
	        addSeparator();
	        for(Action ca : commonActions.getAiActions()) {
	        	add(ca).setName(ca.getValue(Action.NAME) + "-action");
	        }

	        addSeparator();
	        for(Action ea : documentActions.getEditorActions()) {
            	if(ea == null) {
            		addSeparator();
            	} else {
    		        add(ea).setName(ea.getValue(Action.NAME) + "-action");
            	}
	        }
	        if(!appendedActions.isEmpty()) {
	        	addSeparator();
		        for(Action aa : appendedActions) {
			        add(aa).setName(aa.getValue(Action.NAME) + "-action");
		        }
	        }
		}

	}
	 
	 
	/** 
	 * Code editor that presents autocompletion/docs for certain key combos 
	 */
	 private class QCodeEditorPanel extends JPanel {

		private final DocEditorPane editorPane;
		private final Document document;
		 
		public QCodeEditorPanel(final Document document) {

			this.document = document;

			DefaultSyntaxKit.initKit(); // to make it jsysntaxpane
			Configuration config = DefaultSyntaxKit.getConfig(DefaultSyntaxKit.class);

	        // save positions now as setting text/type moves it 
	        final int selectionStart = document.getSelectionStart();
	        final int selectionEnd = document.getSelectionEnd();
	        final int caretPosition = document.getCaratPosition();
	        
			editorPane = new DocEditorPane(document);
			
			setActions(document);

			// set GUI layout etc
			setLayout(new BorderLayout());
			editorPane.setName("codeEditor-" + document.getTitle());
	        JScrollPane scrPane = new JScrollPane(editorPane);
	        // need to re-set text after setting content type
	        
	        /*
	         * Workaround for editor pane content type handling
	         */
	        
	        String txt = editorPane.getText();
	        // must add to scrollpane before setting content type to allow line numbers.
	        setContentType();
	        editorPane.setText(txt);
	        
	        // tooltips = autocomplete
	        editorPane.setTooltipProvider(qDocController.getTooltipProvider(document));
	        
	        add(scrPane, BorderLayout.CENTER);
	        
	        // restore positions, post to EventThread to make scrollpane scroll to position
	        EventQueue.invokeLater(new Runnable() {
				@Override public void run() {
			        document.setSelection(selectionStart, selectionEnd, caretPosition);
				}
			});
		}

		void setContentType() {
			String end = document.getFileEnding();
			if(end.length() == 0) {
				end = assumedFileEnding;
			}
			String newContentType = "";
	        switch(end) {
	        	case "sql":
	        	case "c":
	        	case "cpp":
	        	case "js":
	        	case "json":
	        	case "xml":
	        	case "properties":
	        	case "lua":
	        	case "java":
	        	case "prql":
	        	case "dos":
	        	case "rfl":
	        	case "jflex":
	        	case "javascript":
	        	case "ruby":
	        	case "scala":
	        	case "xpath":
	        		newContentType = "text/" + end;
		        	break;
	        	case "cjc":
	        		newContentType = "text/clojure";
		        	break;
	        	case "py":
	        		newContentType = "text/python";
		        	break;
	        	default:
	        		if(end.equals("bat")) {
	        			newContentType = "text/dosbatch";
	        		} else if(end.equals("sh")) {
	        			newContentType = "text/bash";
	        		} else if(end.equals("html") || end.equals("htm") || end.equals("xhtml")) {
	        			newContentType = "text/xhtml";
	        		} else if(end.equals("sh")) {
	        			newContentType = "text/bash";	        			
//					Purposefully not using plain. As it's just so plain.	        		
//	        		} else if(end.equals("txt") || end.equals("md")) {
//	        			editorPane.setContentType("text/plain");
	        		} else {
	        			newContentType = "text/qsql";
	        		}
	        }
	        if(!editorPane.getContentType().equals(newContentType)) {
				// Odd but setting contentType gets rid of text. Need to save and restore.
				String content = document.getContent();
				editorPane.setContentType(newContentType);
				editorPane.setText(content);
				if(editorFont != null) {
					editorPane.setFont(editorFont);
				}
	        }
		}
		
		
		/**
	     * A convenience print method that displays a print dialog, and then
	     * prints this {@code JTextComponent} in <i>interactive</i> mode with 
	     * the specified header and footer text. Note: this method
	     * blocks until printing is done.
	     * <p>
	     * Note: In <i>headless</i> mode, no dialogs will be shown.
	     * 
	     * <p> This method calls the full featured 
	     * {@link #print(MessageFormat, MessageFormat, boolean, PrintService, PrintRequestAttributeSet, boolean)
	     * print} method to perform printing.
	     * @param headerFormat the text, in {@code MessageFormat}, to be
	     *        used as the header, or {@code null} for no header
	     * @param footerFormat the text, in {@code MessageFormat}, to be
	     *        used as the footer, or {@code null} for no footer
	     * @return {@code true}, unless printing is canceled by the user
	     * @throws PrinterException if an error in the print system causes the job
	     *         to be aborted
	     * @throws SecurityException if this thread is not allowed to
	     *                           initiate a print job request
	     *         
	     * @see #print(MessageFormat, MessageFormat, boolean, PrintService)
	     * @see java.text.MessageFormat     
	     */
		public boolean print(MessageFormat headerFormat, MessageFormat footerFormat) throws PrinterException {
			// If user set large font for large screen, reduce so it prints sensibly
			Font originalFont = editorPane.getFont();
			int sz = originalFont.getSize();
			Font newFont = originalFont.deriveFont(Font.PLAIN, sz > 11 ? sz - 8 : sz);
			editorPane.setFont(newFont);
			try {
				return editorPane.print(headerFormat, footerFormat);
			} finally {
				editorPane.setFont(originalFont);
			}
		}

		/**
		 * Set the keyboard and mouse shorcuts and actions 
		 */
		private void setActions(final Document document) {

			// workaround to make control-e shortcut work as jsyntaxpane key listener otherwise
			// consumes control-e and does not pass to actionMap etc.
			editorPane.addKeyListener(new KeyAdapter() {
				@Override public void keyPressed(KeyEvent e) { 
					if(e.isControlDown() && e.getKeyCode()==KeyEvent.VK_E) {
						commonActions.getqAction().actionPerformed(null);
						e.consume();
					}
				}
			});
			// workaround to make control-e shortcut work as jsyntaxpane key listener otherwise
			// consumes control-e and does not pass to actionMap etc.
			editorPane.addKeyListener(new KeyAdapter() {
				@Override public void keyPressed(KeyEvent e) { 
					if(e.isControlDown() && e.getKeyCode()==KeyEvent.VK_R) {
						commonActions.getServerSelectAction().actionPerformed(null);
						e.consume();
					}
				}
			});

			add(qDocController.getOutlineFileAction(editorPane));
			add(qDocController.getOutlineAllFilesAction(editorPane));
			add(qDocController.getAutoCompleteAction(editorPane, parentFrame));
			final Action gotoDefinitionAction = qDocController.getGotoDefinitionAction(document);
			add(gotoDefinitionAction);
			final Action docLookupAction = qDocController.getDocLookupAction(editorPane);
			add(docLookupAction);
			
			
			// show dynamically generated menu when mouse right clicked
			editorPane.addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(final MouseEvent me) {
			        if (SwingUtilities.isRightMouseButton(me)) {
						ArrayList<Action> acts = Lists.newArrayList(docLookupAction, gotoDefinitionAction);
			            JPopupMenu popMenu = new EditorPopupMenu(acts);
			            popMenu.show(me.getComponent(),me.getX(), me.getY());
			        }
			    }
			});
		}
		
		

		private void add(Action action) {
			String mapkey = (String) action.getValue(Action.NAME);
			editorPane.getActionMap().put(mapkey, action);
			KeyStroke keyStroke = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
			editorPane.getInputMap().put(keyStroke, mapkey);
		}
		

		public Document getDoc() {
			return document;
		}
		
	    /**
	     * Add a control-combo shortcut to this components input/action Map
	     * @param key unique name for this action
	     * @param keyEvent The key combined with control that is the shortcut
	     * @param act The actual action to perform
	     */
		public void addCtrlActionShortcut(String key, int keyEvent, AbstractAction act) {
	        KeyStroke ks = KeyStroke.getKeyStroke(keyEvent, 
	        		Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
	        editorPane.getInputMap().put(ks, key);
	        editorPane.getActionMap().put(key, act);
		}

		
		@Override public void setTransferHandler(TransferHandler newHandler) {
			 editorPane.setTransferHandler(newHandler);
		}
		 
		 @Override public TransferHandler getTransferHandler() {
			return editorPane.getTransferHandler();
		}

		@Override public boolean requestFocusInWindow() { return editorPane.requestFocusInWindow();	}

		@Override public void requestFocus() { editorPane.requestFocus(); }

		public void setEditorFont(Font font) { editorPane.setFont(font); }
		public void setEditorEditable(boolean editable) { editorPane.setEditable(editable); }

	 }
	 
	 
	 @Override public GrabItem grab() {
		 /**
		  * For a grab, provide a read-only content/title copy
		  */
		Document d = new Document();
		d.setContent(openDocModel.getSelectedDocument().getContent());
		d.setTitle(d.getTitle());
		final QCodeEditorPanel codep = new QCodeEditorPanel(d);
		if(editorFont != null) {
			codep.setEditorFont(editorFont);
		}
		codep.setEditorEditable(false);
		return new GrabItem(codep, d.getTitle());
	}

}
