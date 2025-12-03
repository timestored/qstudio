package com.timestored.pro.kdb;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;
import com.timestored.TimeStored;
import com.timestored.command.Command;
import com.timestored.command.CommandManager;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.JdbcTypes;
import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.jgrowl.Growler;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.misc.HtmlUtils;
import com.timestored.pro.csvloader.CsvLoaderFrame;
import com.timestored.pro.dolphindb.DosDocs;
import com.timestored.pro.notebook.NotebookServer;
import com.timestored.pro.rayforcedb.RflDocs;
import com.timestored.pro.sql.SqlDocSource;
import com.timestored.pro.sql.SqlFunctions;
import com.timestored.qdoc.DocumentedMatcher;
import com.timestored.qdoc.KdbCustomAutocompleter;
import com.timestored.qstudio.CommonActions;
import com.timestored.qstudio.EditorConfigFactory;
import com.timestored.qstudio.EditorConfigFactory.EditorConfig;
import com.timestored.qstudio.Language;
import com.timestored.qstudio.Persistance;
import com.timestored.qstudio.QDocController;
import com.timestored.qstudio.QStudioFrame;
import com.timestored.qstudio.QStudioModel;
import com.timestored.qstudio.ServerDocumentPanel;
import com.timestored.qstudio.UpdateHelper;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.DatabaseDirector;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.qstudio.model.ServerModel;
import com.timestored.qstudio.model.ServerObjectTree;
import com.timestored.qstudio.model.DatabaseDirector.ActionsGenerator;
import com.timestored.qstudio.model.DatabaseDirector.ActionsGeneratorSupplier;
import com.timestored.qstudio.servertree.SelectedServerObjectPanel;
import com.timestored.qstudio.servertree.ServerListPanel;
import com.timestored.swingxx.AAction;
import com.timestored.theme.Theme;

import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.syntaxkits.DosSyntaxKit;
import jsyntaxpane.syntaxkits.PrqlSyntaxKit;
import jsyntaxpane.syntaxkits.QSqlSyntaxKit;
import jsyntaxpane.syntaxkits.RflSyntaxKit;
import lombok.NonNull;

public class PluginInitialiser {
	
	private static final Logger LOG = Logger.getLogger(PluginInitialiser.class.getName());
	private static NotebookServer notebookServer;
	private static JButton markdownServerButton;
	private static boolean mdFileOpened = false;

	public static void init() {
		
		ServerObjectTree.setGET_TREE_QUERY(DatabaseManager.GET_TREE_QUERY);
		
		ServerListPanel.setReportGenerator(DatabaseHtmlReport::generate);
		
		SelectedServerObjectPanel.registerServerDescriptionPanelSupplier(JdbcTypes.KDB, adminModel -> {
			return new com.timestored.pro.kdb.ServerDescriptionPanel(adminModel);
		});
		
		DatabaseDirector.registerActionsGenerator(JdbcTypes.KDB, new ActionsGeneratorSupplier() {
			@Override public ActionsGenerator getActionsGenerator(QueryManager queryManager, AdminModel adminModel, ServerModel sm) {
				return new KdbActionsGenerator(queryManager, adminModel, sm);
			}
		});
	}
	
	
	public static void init(final QStudioModel qsm) {
		
		init();
		UpdateHelper.setQStudioModel(qsm);
		
		List<Command> commands = new ArrayList<>();
		
		ServerDocumentPanel.setEditorConfigUpdater(new Consumer<EditorConfigFactory.EditorConfig>() {
			public void accept(EditorConfig editorConfig) {
				editorConfig.apply(DefaultSyntaxKit.getConfig(QSqlSyntaxKit.class));
				editorConfig.apply(DefaultSyntaxKit.getConfig(DosSyntaxKit.class));
				editorConfig.apply(DefaultSyntaxKit.getConfig(RflSyntaxKit.class));
				editorConfig.apply(DefaultSyntaxKit.getConfig(PrqlSyntaxKit.class));
			}
		});

		QDocController.registerDocMatcherSupplier(() -> {
			return new DocumentedMatcher(Language.Q, new KdbDocSource(qsm), "[","]",";", false, new KdbCustomAutocompleter());
		});
		QDocController.registerDocMatcherSupplier(() -> {
			SqlDocSource ds = new SqlDocSource(qsm.getAdminModel(), SqlFunctions::getKnownFunctions);
			return new DocumentedMatcher(Language.SQL, ds, "(", ")", ",", true, null);
		});
		QDocController.registerDocMatcherSupplier(() -> {
			SqlDocSource ds = new SqlDocSource(qsm.getAdminModel(), DosDocs::getKnownFunctions);
			return new DocumentedMatcher(Language.DOLPHIN,ds, "(", ")", ",", true, null);
		});
		QDocController.registerDocMatcherSupplier(() -> {
			SqlDocSource ds = new SqlDocSource(qsm.getAdminModel(), RflDocs::getKnownFunctions);
			return new DocumentedMatcher(Language.RFL,ds, "(", ")", " ", true, null);
		});
		
		CommonActions.setProActionPlugin((@NonNull CommonActions commonActions, @NonNull Growler growler) -> {
				return KdbActions.getProActions(commonActions, qsm, growler, qsm.getPersistance());
			});
		
		QStudioFrame.setHelpMenuPlugin((QStudioFrame qStudioFrame, Growler growler, CommandManager commandManager) -> {
	        List<Action> helpMenu = new ArrayList<>();
	        ImageIcon codeImage = Theme.CIcon.PAGE_CODE.get16();
	        
	        AAction openExampleAction = new AAction("Open Example kdb-all.md", Theme.CIcon.MARKDOWN_GREEN.get16(), e-> {
				qStudioFrame.openExampleFile(NotebookServer.class, NotebookServer.EXAMPLE_KDB_FILE);
			});
	        helpMenu.add(openExampleAction);
	        commands.add(CommandManager.toCommand(openExampleAction));

	        if(qsm.getConnectionManager().containsKdbServer()) {
	        	AbstractAction openQunitAction = new AbstractAction(Msg.get(Key.OPEN_QUNIT_EXAMPLE), codeImage) {
					private static final long serialVersionUID = 1L;
					
					@Override public void actionPerformed(ActionEvent e) {
						qStudioFrame.openExampleFile(ModuleRunner.class, ModuleRunner.MATH_TEST_Q);
						qStudioFrame.openExampleFile(ModuleRunner.class, ModuleRunner.MATH_Q);
						String html = "<a href='" + TimeStored.Page.QUNIT_HELP.url() + "'>QUnit</a> is a framework for implementing testing in kdb." +
								"<br/><br/>To run the examples you should:" +
								"<br/>1. Select the math.q file and press Ctrl+E to load that onto the server." +
								"<br/>2. Select the testMath.q file and press Ctrl+T to run the tests." +
								"<br/>" +
								"<br/>More Information can be found on the <a href='" + TimeStored.Page.QSTUDIO_HELP_QUNIT.url() + "'>qunit help page</a>";
						JOptionPane.showMessageDialog(qStudioFrame, Theme.getHtmlText(html));
					}
				};
		        helpMenu.add(openQunitAction);
		        commands.add(CommandManager.toCommand(openQunitAction));
	        }
	        return helpMenu;
		});

		QStudioFrame.setToolsMenuPlugin((QStudioFrame qStudioFrame, Growler growler, CommandManager commandManager) -> {
	        OpenDocumentsModel openDocsModel = qsm.getOpenDocumentsModel();
	        openDocsModel.addListener(new OpenDocumentsModel.Adapter() {
	        	@Override public void docAdded(Document document) {
	        		super.docAdded(document);
	        		String p = document.getFilePath();
	        		if(p != null && NotebookServer.hasMarkdownFileEnding(p)) {
	        			startMarkdownServer(qsm);
	        			if(!mdFileOpened) {
	        				if(notebookServer != null) {
	        					String pth = notebookServer.getHttpMarkdown(document.getFilePath());
	        					offerToOpenBrowser(qsm.getPersistance(), pth);
	        				}
	        				mdFileOpened = true;
	        			}
	        		}
	        	}
			});
	        
	        Action startNotebookAction = new AAction(Msg.get(Msg.Key.START_MARKDOWN), Theme.CIcon.MARKDOWN_GREEN.get16(), e-> {
				startMarkdownServer(qsm);
				if(notebookServer != null) {
					String filePath = qsm.getOpenDocumentsModel().getSelectedDocument().getFilePath();
					HtmlUtils.browse(notebookServer.getHttpMarkdown(filePath));
				}
    		});
	        commands.add(CommandManager.toCommand(startNotebookAction));
	        

	    	Action loadCSVaction = new AAction(Msg.get(Key.LOAD_CSV_DATA), Theme.CIcon.CSV.get16(), e-> {
					String server = qsm.getQueryManager().getSelectedServerName();
					if(server == null) {
						JOptionPane.showMessageDialog(qStudioFrame, "You must have a selected server to allow uploading CSVs"	);	
					} else {
						EventQueue.invokeLater(() -> {
				            try {
				            	ConnectionManager conMan = qsm.getConnectionManager();
				            	CsvLoaderFrame.showCsvImporter(qStudioFrame, conMan, conMan.getServer(server));
				            } catch (Exception exc) {
				            	growler.showSevere("Unable to load data file", "IO error");
				                LOG.log(Level.WARNING, "Problem starting CSV loader", exc);
				            }
				        });
					}
				});
	    	boolean hasKDB = qsm.getConnectionManager().containsKdbServer();
	    	loadCSVaction.setEnabled(hasKDB);
	    	if(hasKDB) {
		        commands.add(CommandManager.toCommand(loadCSVaction));	
	    	}
	        commandManager.registerProvider(() -> commands);
	        
	        return Lists.newArrayList(startNotebookAction, loadCSVaction);
		});

		markdownServerButton = makeMarkdownServerButton(qsm);
		QStudioFrame.setToolbarPlugin((QStudioFrame qStudioFrame, Growler growler, CommandManager commandManager) -> {
	        return Lists.newArrayList(markdownServerButton);
		});

	}

	private static JButton makeMarkdownServerButton(QStudioModel qsm) {
		JButton b = new JButton() {
			@Override public Icon getIcon() {
				return notebookServer != null ? Theme.CIcon.MARKDOWN_GREEN.get16() : Theme.CIcon.MARKDOWN_GREY.get16();
			}
		};
		b.setToolTipText(Msg.get(Msg.Key.START_MARKDOWN));
	    b.addActionListener(al -> {
	    	if(notebookServer == null) {
	    		startMarkdownServer(qsm);
	    	}
	    	if(notebookServer != null) {
	    		String filePath = qsm.getOpenDocumentsModel().getSelectedDocument().getFilePath();
	    		HtmlUtils.browse(notebookServer.getHttpMarkdown(filePath));
	    	}
	    });
		b.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (notebookServer != null && SwingUtilities.isRightMouseButton(e)) {
					notebookServer.stop();
					notebookServer = null;
					b.setIcon(Theme.CIcon.MARKDOWN_GREY.get16());
				}
			}
		});
		return b;
	}

	private static void startMarkdownServer(QStudioModel qsm) {
		if(notebookServer == null) {
			try {
				File MARKDOWN_DIR = new File(QStudioModel.APP_HOME, "sqlnotebook");
				notebookServer = new NotebookServer(qsm.getConnectionManager(), MARKDOWN_DIR);
				File exampleFileCreated = notebookServer.initDirWithExamples();
				qsm.putIfAbsentLocalSQL(); // to allow demos to work
				int port = notebookServer.start();
				notebookServer.refreshPageList();
				OpenDocumentsModel openDocsModel = qsm.getOpenDocumentsModel();
				if(openDocsModel.getSelectedFolder() == null) {
					openDocsModel.setSelectedFolder(MARKDOWN_DIR);
				}
				if(exampleFileCreated != null) {
					openDocsModel.openDocument(exampleFileCreated);
				}
				
				// TODO - Create examples relevant to databases eprson has?
				LOG.info("Started Webserver on Port: " + port);
			} catch (Exception e1) {
				LOG.info("Failed to start server.");
			}
		}
		if(markdownServerButton != null) {
			markdownServerButton.setIcon(notebookServer != null ? Theme.CIcon.MARKDOWN_GREEN.get16() : Theme.CIcon.MARKDOWN_GREY.get16());
			markdownServerButton.setToolTipText("Open Notebook Page: " + notebookServer.getHttpMarkdown(null));
		}
	}
	


	public static void offerToOpenBrowser(Persistance persistance, String url) {
		// check license, show warning if necessary then perform action
		final Persistance.Key warningKey = Persistance.Key.SHOW_NOTEBOOK_WARNING;
		String msgHtml = "<a href='" + TimeStored.Page.QSTUDIO_HELP_SQLNOTEBOOKS.url() + "'>Pulse SQL Notebooks</a> enable combining documentation and executable code in the same document.<br/>"
				+ "You can edit and save .md markdown files in QStudio.<br/>"
				+ "Results of queries including tables and charts will be rendered in your browser.<br/>";
		final String title = "Open Notebook in Browser";
		int choice = CommonActions.showDismissibleWarning(persistance, warningKey, msgHtml, title, "Open Notebook", JOptionPane.CANCEL_OPTION);
		if(choice == JOptionPane.OK_OPTION) {
			boolean opened = HtmlUtils.browse(url);
			if(!opened) {
				JOptionPane.showMessageDialog(null, "Problem opening file, try browsing to folder manually and opening there");
			}
		}		
	}
}
