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

import static com.timestored.theme.Theme.GAP;
import static com.timestored.theme.Theme.CIcon.DOCUMENT_OPEN;
import static java.awt.event.KeyEvent.VK_O;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.timestored.StringUtils;
import com.timestored.TimeStored;
import com.timestored.TimeStored.Page;
import com.timestored.babeldb.DBHelper;
import com.timestored.command.CodeSnippetCommandProvider;
import com.timestored.command.CommandDialog;
import com.timestored.command.CommandManager;
import com.timestored.command.CommandProvider;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ConnectionManagerDialog;
import com.timestored.connections.LoginDialog;
import com.timestored.connections.ServerConfig;
import com.timestored.docs.BackgroundDocumentsSaver;
import com.timestored.docs.Document;
import com.timestored.docs.DocumentActions;
import com.timestored.docs.FileDropDocumentHandler;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.docs.RecentDocumentPersister;
import com.timestored.jgrowl.Growler;
import com.timestored.jgrowl.GrowlerFactory;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.misc.AIFacade;
import com.timestored.misc.AppLaunchHelper;
import com.timestored.misc.HtmlUtils;
import com.timestored.misc.IOUtils;
import com.timestored.qstudio.EditorConfigFactory.EditorConfig;
import com.timestored.qstudio.kdb.KdbHelper;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.AdminModel.Category;
import com.timestored.qstudio.model.QEntity;
import com.timestored.qstudio.model.QueryAdapter;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.qstudio.model.QueryResult;
import com.timestored.qstudio.model.ServerModel;
import com.timestored.qstudio.servertree.PagingTablePanel;
import com.timestored.qstudio.servertree.ServerTreePanel;
import com.timestored.sqldash.chart.ChartTheme;
import com.timestored.sqldash.chart.TableFactory;
import com.timestored.sqldash.chart.ViewStrategyFactory;
import com.timestored.swingxx.AAction;
import com.timestored.swingxx.DockerHelper;
import com.timestored.swingxx.FileOpenCommandProvider;
import com.timestored.swingxx.FileTreePanel;
import com.timestored.swingxx.JToolBarWithBetterTooltips;
import com.timestored.swingxx.QueryStatusBar;
import com.timestored.swingxx.SwingUtils;
import com.timestored.swingxx.TableExporter;
import com.timestored.swingxx.ToggleDockableMenuItem;
import com.timestored.theme.AboutDialog;
import com.timestored.theme.Icon;
import com.timestored.theme.ShortcutAction;
import com.timestored.theme.Theme;
import com.timestored.theme.Theme.CIcon;

import bibliothek.gui.DockController;
import bibliothek.gui.DockFrontend;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.DefaultDockable;
import bibliothek.gui.dock.SplitDockStation;
import bibliothek.gui.dock.action.DefaultDockActionSource;
import bibliothek.gui.dock.action.actions.SimpleButtonAction;
import bibliothek.gui.dock.station.split.DockableSplitDockTree;
import bibliothek.gui.dock.station.split.SplitDockTree;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Contains all panels for browsing servers, editing documents,
 * sending queries and viewing charts.
 */
public class QStudioFrame extends JFrame {

	private static final String KDB_EXAMPLE_FILE = "kdb-examples.q";
	private static final String FIRST_OPEN_FILE = "firstOpen.bat";

	private static final Logger LOG = Logger.getLogger(QStudioFrame.class.getName());

	@Setter private static QStudioFramePlugin<Action> helpMenuPlugin = (a,b,c) -> Collections.emptyList();
	@Setter private static QStudioFramePlugin<JButton> toolbarPlugin = (a,b,c) -> Collections.emptyList();
	@Setter private static QStudioFramePlugin<Action> toolsMenuPlugin = (a,b,c) -> Collections.emptyList();
	private static final int SERVER_NAME_WIDTH = 190;
	private static final long serialVersionUID = 1L;
	private static final String UNIQUE_ID = "UNIQ_ID";
	public static final String VERSION = "5.00";
	
	private final QStudioModel qStudioModel;
	private final ConnectionManager conMan;
	@Getter private final OpenDocumentsModel openDocsModel;
	private final QueryManager queryManager;
	private final Persistance persistance;
	@Getter private final DockFrontend frontend;
	private final AdminModel adminModel;
	private final KDBResultPanel kdbResultPanel;
	private final MyPreferences myPreferences;
	private final ServerTreePanel sTreePanel;
	private final QueryHistoryPanel queryHistorypanel;
	private final FileTreePanel fileTreePanel;
	private final ConsolePanel consolePanel;
	private final SearchAllFilesPanel searchAllFilesPanel;
	private final Growler growler;
	private final CommandManager commandManager;
	
	private final DocumentActions documentActions;
	private final ChartResultPanel chartResultPanel;
	private final ServerDocumentPanel serverDocumentPanel;
	private final CommonActions commonActions;
	
	private final QDocController qDocController;
	private RecentDocumentPersister recentDocumentPersister;
	private BackgroundDocumentsSaver backgroundDocsSaver;
	private JMenu fileMenu;
	/** Stores the default layout of windows at startup, before user config loaded **/
	private String defaultLayoutXml = "";
	private int queryCount = 0;

	private final Color defaultFrameColor;
	private SimpleButtonAction xlsButton;
	private SimpleButtonAction pivotButton;
	private SimpleButtonAction saveToDuckButton;
	private SimpleButtonAction emailButton;
	private QueryResult lastQueryResult;

	private final AbstractAction commandPaletteAction;

	private final ShortcutAction openFilesAction;
	private final AbstractAction searchFilesAction;


	public QStudioFrame(final QStudioModel qStudioModel) {

		this.qStudioModel = qStudioModel;
		LOG.info("Starting QStudioFrame Constructor");
		// used to display loading times of each section in LOG
        long startT = System.nanoTime();
        long prevT = System.nanoTime();
        long nextT;

		// create model
		this.conMan = Preconditions.checkNotNull(qStudioModel.getConnectionManager());
		this.openDocsModel = Preconditions.checkNotNull(qStudioModel.getOpenDocumentsModel());
        this.adminModel = Preconditions.checkNotNull(qStudioModel.getAdminModel());
        this.queryManager = Preconditions.checkNotNull(qStudioModel.getQueryManager());
        this.persistance = qStudioModel.getPersistance();
		this.commandManager = new CommandManager();
        
        // check if this is first ever opening and persist it as having happened.
        final boolean firstEverOpen = persistance.getBoolean(Persistance.Key.FIRST_OPEN, true);
        if(firstEverOpen) {
        	persistance.putBoolean(Persistance.Key.FIRST_OPEN, false);
        }
        
        System.out.println("MODELS = " + ((nextT = System.nanoTime()) - prevT) / 1000000.0); prevT = nextT;

        // create common action containers
        Action docGen = new GenerateDocumentationAction(openDocsModel);

        
        documentActions = new DocumentActions(openDocsModel, () -> qStudioModel.getFileEndings(), docGen);

        FileDropDocumentHandler myTransferHandler = new FileDropDocumentHandler().addListener(files -> handleArgsFiles(files));
		setTransferHandler(myTransferHandler);
        this.growler = GrowlerFactory.getGrowler(this);
        commonActions = new CommonActions(qStudioModel, this, persistance, growler);
        openDocsModel.addListener(new OpenDocumentsModel.Adapter() {
			@Override public void docSelected(Document document) {
				QStudioFrame.this.setFrameTitle();
			}
		});
        
        // set frame appearance
		setTitle(QStudioModel.APP_TITLE);
		setLayout(new BorderLayout(GAP, GAP));
		restoreBounds(persistance);
		queryCount = persistance.getInt(Persistance.Key.QUERY_COUNT, 0);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(Theme.CIcon.QSTUDIO_LOGO.get().getImage());

        /*
         * The windowing framework swallows certain key combinations
         * must override these before framework initialisation
         * to control some document actions
         */
        openDocsModel.forceKeyboardShortcutOverrides();
        
        /*
         * keep admin models selected server and query managers in sync
         */
        myPreferences = MyPreferences.INSTANCE;
        defaultFrameColor = getContentPane().getBackground();
		QStudioFrame.this.setFrameColor();
		setVisible(true);
        queryManager.addQueryListener(new QueryAdapter() {
        	@Override public void selectedServerChanged(String server) {
				QStudioFrame.this.setFrameTitle();
				QStudioFrame.this.setFrameColor();
        	}
        	
        	public void queryResultReturned(ServerConfig sc, QueryResult queryResult) {
        		if(queryResult.e != null && queryResult.e.toString().contains("ClassNotFoundException")) {
        			ConnectionManagerDialog.offerToInstallDriver(sc.getJdbcType(), QStudioFrame.this);
        		}
        		setLastQueryResult(queryResult);
        	}
        });
        adminModel.addListener(new AdminModel.Listener() {
			
			@Override public void selectionChanged(ServerModel serverModel, Category category, String namespace, QEntity element) {
        		setLastQueryResult(null);
			}

			@Override public void modelChanged() { }
			@Override public void modelChanged(ServerModel sm) {}
		});
        
		// @TODO this takes 100ms

		DockController.disableCoreWarning(); // prevent advert
		frontend = new DockFrontend(this);
		SplitDockStation station = new SplitDockStation();
		frontend.addRoot(UNIQUE_ID, station);
		frontend.setShowHideAction(true);
		
		if(myPreferences.getCodeFont().toLowerCase().contains("jetbrains")) {
			// @TODO This .install is taking 400ms!!! https://github.com/JFormDesigner/FlatLaf/issues/901
			FlatJetBrainsMonoFont.install(); 
		}
		
		
		boolean isDarkTheme = AppLaunchHelper.isLafDark(MyPreferences.INSTANCE.getCodeTheme());
		// This is a repeat of later code as the config must ideally be set before editor is ever created.
		EditorConfigFactory.TCOLOR tcolor = isDarkTheme  ? EditorConfigFactory.TCOLOR.DARK : EditorConfigFactory.TCOLOR.LIGHT; 
		ServerDocumentPanel.setEditorConfig(EditorConfigFactory.get(tcolor));

		// Use the document file ending if it's known, else fall back to current selected server type.
		qDocController = new QDocController(qStudioModel);
		serverDocumentPanel = new ServerDocumentPanel(commonActions, documentActions, 
				openDocsModel, this, qDocController, files -> handleArgsFiles(files));
		serverDocumentPanel.setEditorFont(myPreferences.getCodeFontFont());
		serverDocumentPanel.setAssumedFileEnding(conMan.isEmpty() || conMan.containsKdbServer() ? "q" : "sql");

		ActionMap am = getRootPane().getActionMap();
		InputMap im = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		queryManager.addQueryListener(new QueryAdapter() {
			@Override public void selectedServerChanged(String server) {
				String fileEnding = conMan.isEmpty() || conMan.containsKdbServer() ? "q" : "sql";
				if(server != null) {
					ServerConfig sc = conMan.getServer(server);
					if(sc != null) {
//						serverDocumentPanel.setEditorBackground(sc.getColor());
						fileEnding = sc.isKDB() ? "q" : "sql";
					}
				}
				serverDocumentPanel.setAssumedFileEnding(fileEnding);
			}
		});

		DefaultDockable documentsDockable = createDockable(Msg.get(Key.DOCUMENTS), CIcon.PAGE_CODE, serverDocumentPanel, null);

		// customized dock actions
		kdbResultPanel = new KDBResultPanel(adminModel, queryManager);
		kdbResultPanel.setName("kdbResultPanelTop");
		kdbResultPanel.setTransferHandler(myTransferHandler);
		ActionListener refreshResendQueryAL = ae -> queryManager.resendLastQuery();
		
		
		pivotButton = makeButton("Pulse Pivot", Theme.CIcon.TABLE_PIVOT, ae -> kdbResultPanel.togglePivotFormVisible());
		DefaultDockActionSource actions = new DefaultDockActionSource();
		xlsButton = makeButton("Export to Excel", Theme.CIcon.XLSX, (ActionEvent ae) -> {
			if(lastQueryResult != null && lastQueryResult.rs != null) {
				TableExporter.saveToExcelAndOpen(lastQueryResult.rs, lastQueryResult.query, new TableFactory.KdbStringValuer());
			}
		});
		emailButton = makeButton("Send Excel Attachment", Theme.CIcon.EMAIL_ATTACH, (ActionEvent ae) -> {
			if(lastQueryResult != null && lastQueryResult.rs != null) {
				TableExporter.emailExcelForUser(lastQueryResult.rs, lastQueryResult.query, new TableFactory.KdbStringValuer());
			}
		});
		saveToDuckButton = makeButton("Export to QDuckDB Table", Theme.CIcon.DUCK, (ActionEvent ae) -> {
			if(lastQueryResult != null && lastQueryResult.rs != null) {
				try {

					String name = JOptionPane.showInputDialog(QStudioFrame.this, "Enter table name", "tbl");
					if(name != null) {
						File f= qStudioModel.saveToQDuckDB(lastQueryResult.rs, name);
						if(f == null) {
							growler.showSevere("QDuckDB failed to init.", "QDuckDB failed.");
						} else {
							growler.show("Saved to: " + f.getAbsolutePath(), "File Saved");
						}
					}
				} catch (Exception e) {
    				String msg = "Error saving file: ";
    				LOG.log(Level.SEVERE, msg, e);
    				JTextArea errTA = Theme.getTextArea("msg", e.getMessage());
    				JScrollPane sp = new JScrollPane(errTA);
    				sp.setPreferredSize(new Dimension(400, 300));
    		        JOptionPane.showMessageDialog(null, sp, "Error Saving", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		actions.add(pivotButton);
		actions.add(xlsButton);
		actions.add(emailButton);
		actions.add(saveToDuckButton);
		setLastQueryResult(null);
		DefaultDockable kdbResDockable = createDockable(Msg.get(Key.RESULT), CIcon.TAB_GO,
				kdbResultPanel, refreshResendQueryAL, actions);
		
		fileTreePanel = getFileTreePanel(openDocsModel, documentActions);
		fileTreePanel.addListener((File selectedFile) -> {
				if(!selectedFile.isDirectory()) { // Don't open folder as they may just be expanding tree
					FileTreePanel.openFileOrBrowseTo(selectedFile, fl -> handleArgsFiles(fl));
				}
			});
		DefaultDockable fileTreeDockable = createDockable(Msg.get(Key.FILE_TREE), 
				CIcon.DOCUMENT_OPEN, fileTreePanel, ae -> fileTreePanel.refreshGui());
        
        consolePanel = new ConsolePanel();
        consolePanel.setCodeFont(myPreferences.getCodeFontFont());
		queryManager.addQueryListener(consolePanel);
		DefaultDockable consoleDockable = createDockable(Msg.get(Key.CONSOLE), 
				CIcon.TERMINAL, consolePanel, refreshResendQueryAL);

        sTreePanel = new ServerTreePanel(qStudioModel, commonActions, this);
        sTreePanel.setTransferHandler(myTransferHandler);
        ActionListener refreshAL = ae -> BackgroundExecutor.EXECUTOR.execute(() ->  adminModel.refresh());
		DefaultDockable serverTreeDockable = createDockable(Msg.get(Key.SERVER_TREE), 
				CIcon.SERVER, sTreePanel, refreshAL);
	      
		//@TODO this next line takes 40ms
		chartResultPanel = new ChartResultPanel(queryManager, this.growler);
		ChartTheme chartTheme = isDarkTheme ? ViewStrategyFactory.DARK_THEME : ViewStrategyFactory.LIGHT_THEME;
		chartResultPanel.setChartTheme(chartTheme);
		DefaultDockable chartDockable = createDockable(Msg.get(Key.CHART), 
				CIcon.CHART_CURVE,chartResultPanel, refreshResendQueryAL);
        queryHistorypanel = new QueryHistoryPanel(queryManager);
        queryHistorypanel.addListener(queryResult -> chartResultPanel.showQueryResult(queryResult));
        
		DefaultDockable historyDockable = createDockable(Msg.get(Key.HISTORY), 
				CIcon.TABLE_MULTIPLE, queryHistorypanel, refreshResendQueryAL);
		searchAllFilesPanel = new SearchAllFilesPanel(openDocsModel);
		DefaultDockable searchFilesDockable = createDockable(Msg.get(Key.SEARCH_FILES),
				CIcon.EDIT_FIND, searchAllFilesPanel, null);

		/*
		 * Register the various Command Providers so that the Command Palette works
		 */
		commandManager.registerProvider(commonActions);
		commandManager.registerProvider(new FileOpenCommandProvider(documentActions, fileTreePanel, file -> handleArgsFiles(file)));
		commandManager.registerProvider(Language.Q.name(), new CodeSnippetCommandProvider(openDocsModel, queryManager));
		commandManager.registerProvider(queryManager);
		qDocController.registerCommandProviders(commandManager, kdbResultPanel);
		
		// Show an outline of entities in the current document
		commandPaletteAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) {   
				CommandProvider cp = () -> {
					return commandManager.getCommands(qStudioModel.getCurrentSqlLanguage().name());
				};
				// Language specific filter
				CommandDialog cd = new CommandDialog("", cp, BackgroundExecutor.EXECUTOR);
				cd.setPreferredSize(new Dimension(600, 400));
				cd.setMinimumSize(new Dimension(600, 400));
				cd.setLocationRelativeTo(QStudioFrame.this);
				cd.setVisible(true);
			}
		};
		KeyStroke COMMAND_PALETTE_KS = KeyStroke.getKeyStroke(KeyEvent.VK_P, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
		am.put("commandPalette", commandPaletteAction);
		im.put(COMMAND_PALETTE_KS, "commandPalette");
		
		searchFilesAction = new AbstractAction("Find All...", CIcon.EDIT_FIND.get()) {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) {
				frontend.hide(searchFilesDockable);
				frontend.show(searchFilesDockable);
				searchAllFilesPanel.focusSearchField();
			}
		};
		KeyStroke SEARCH_FILES_KS = KeyStroke.getKeyStroke(KeyEvent.VK_F,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK);
		am.put("searchFiles", searchFilesAction);
		im.put(SEARCH_FILES_KS, "searchFiles");

        myPreferences.addListener(() -> pushPreferencesToModels());
		pushPreferencesToModels();

		/*
		 *place them appropriately
		 * @TODO this take 100 ms
		 */
		DockableSplitDockTree tree = new DockableSplitDockTree();

		SplitDockTree<Dockable>.Key docsGroup = tree.put(new Dockable[]{ documentsDockable }, documentsDockable );
		SplitDockTree<Dockable>.Key resultsGroup = tree.put(new Dockable[]{ kdbResDockable, 
				chartDockable, historyDockable,  consoleDockable, searchFilesDockable }, kdbResDockable ); // expWatcherDockable,
		SplitDockTree<Dockable>.Key rightSide = tree.vertical( docsGroup, resultsGroup, 0.55 );
		SplitDockTree<Dockable>.Key leftSide = tree.vertical( serverTreeDockable, fileTreeDockable, 0.6 );
		SplitDockTree<Dockable>.Key root = tree.horizontal( leftSide, rightSide, 1.0/5.0 );
		

		tree.root(root);
		station.dropTree(tree);
        
        /*
         * Menu Bar
         */
		JMenuBar menuBar = getBasicMenuBar(commonActions, documentActions);
		JMenu panelsMenu = getJMenu(Msg.get(Key.WINDOWS), KeyEvent.VK_W);
		panelsMenu.add(new ToggleDockableMenuItem(documentsDockable, frontend, "wDocsMenuItem"));
		panelsMenu.add(new ToggleDockableMenuItem(serverTreeDockable, frontend, "wSTreeMenuItem"));
		panelsMenu.add(new ToggleDockableMenuItem(fileTreeDockable, frontend, "wFileTreeMenuItem"));
//		panelsMenu.add(new ToggleDockableMenuItem(expWatcherDockable, frontend, "wExpWMenuItem"));
		panelsMenu.add(new ToggleDockableMenuItem(consoleDockable, frontend, "wConsoleMenuItem"));
		panelsMenu.add(new ToggleDockableMenuItem(kdbResDockable, frontend, "wKdbResMenuItem"));
		panelsMenu.add(new ToggleDockableMenuItem(chartDockable, frontend, "wChartMenuItem"));
		panelsMenu.add(new ToggleDockableMenuItem(historyDockable, frontend, "wHistoryMenuItem"));
		panelsMenu.add(new ToggleDockableMenuItem(searchFilesDockable, frontend, "wSearchFilesMenuItem"));
		panelsMenu.addSeparator();
		panelsMenu.add(new AAction("Restore Default Layout", ae -> DockerHelper.loadLayout(defaultLayoutXml, frontend)));
		menuBar.add(panelsMenu);

    	// help menu        
        menuBar.add(getHelpMenu());
		menuBar.setName("qStudiomenuBar");
        setJMenuBar(menuBar);

		openFilesAction = new ShortcutAction(Msg.get(Key.OPEN_FILE) + "...", DOCUMENT_OPEN, VK_O) {
			private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent arg0) {
				File lastSelection = documentActions.getLastSelectedOpenFileFolder();
	    		JFileChooser fc = lastSelection!=null ?  new JFileChooser(lastSelection) :new JFileChooser(); 
	    		fc.setMultiSelectionEnabled(true);
	            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
	            	handleArgsFiles(Arrays.asList(fc.getSelectedFiles()));
		        } else {
		        	LOG.info(Msg.get(Key.OPEN_CANCELLED));
		        }
		   }
		};
        rebuildFileMenu(); // need this to force control shortcuts to be registered

        /*
         * Actual displayed panels
         */
        add(getToolbar(commonActions, queryManager, documentActions), BorderLayout.NORTH);
        add(station, BorderLayout.CENTER);
        
		add(getStatusBar(queryManager), BorderLayout.SOUTH);

		// @todo this one line takes 100ms
		defaultLayoutXml = DockerHelper.getLayout(frontend);
		DockerHelper.loadLayout(persistance.get(Persistance.Key.WINDOW_POSITIONS, ""), frontend); 
		
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// save layout on exit
		addWindowListener(new WindowAdapter() {

			@Override public void windowClosing(WindowEvent e) {
				backgroundDocsSaver.shutdownNow();
				backgroundDocsSaver.saveDocumentsScratch();
			  
			    persistance.put(Persistance.Key.WINDOW_POSITIONS, DockerHelper.getLayout(frontend));
			    Rectangle r = getBounds();
			    persistance.putInt(Persistance.Key.FRAME_X, (int) r.getX());
			    persistance.putInt(Persistance.Key.FRAME_Y, (int) r.getY());
			    persistance.putInt(Persistance.Key.FRAME_WIDTH, getWidth());
			    persistance.putInt(Persistance.Key.FRAME_HEIGHT, getHeight());
			    persistance.putInt(Persistance.Key.FRAME_EXTENDEDSTATE, getExtendedState());
			    persistance.putInt(Persistance.Key.QUERY_COUNT, queryCount);
			    try {
					persistance.getPref().flush();
				} catch (BackingStoreException e1) {
					LOG.severe("problem flushing to persistanct");
				}
		        
			    System.exit(0);
		  }

		});
		 
		// show cursor appropriate to query state
		queryManager.addQueryListener(new QueryAdapter() {
			
			private void setC(int c) {
			    QStudioFrame.this.setCursor(Cursor.getPredefinedCursor(c));
			}
			
			@Override public void queryResultReturned(ServerConfig sc, QueryResult queryResult) {
			    setC(Cursor.DEFAULT_CURSOR);
			}
			
			@Override public void sendingQuery(ServerConfig sc, String query) {
			    setC(Cursor.WAIT_CURSOR);
				queryCount++;
			}
		});
		
        System.out.println("ENDER = " + ((nextT = System.nanoTime()) - startT) / 1000000.0);
        
        // work that doesn't need done immediately so run in separate thread.
        BackgroundExecutor.EXECUTOR.execute(() ->  adminModel.refresh());

        BackgroundExecutor.EXECUTOR.execute(new Runnable() {
			@Override public void run() {
				
				recentDocumentPersister = new RecentDocumentPersister(persistance, 
						Persistance.Key.RECENT_DOCS, Persistance.Key.LAST_OPENED_FOLDER);
				backgroundDocsSaver = new BackgroundDocumentsSaver(openDocsModel, QStudioModel.SCRATCH_DIR, 30);
				
		        File folder = recentDocumentPersister.getOpenFolder(persistance);
		        openDocsModel.setSelectedFolder(folder);

				if(firstEverOpen) {
					QStudioFrame.this.openExampleFile(QStudioFrame.class, FIRST_OPEN_FILE);
				} else {
					backgroundDocsSaver.restoreDocuments();
				}
				openDocsModel.addListener(recentDocumentPersister);
		        // Add as listener to save modifications.
				queryManager.addQueryListener(new QueryAdapter() {
					@Override public void sendingQuery(ServerConfig sc, String query) {
						backgroundDocsSaver.requestSave();
					}
				});
				openDocsModel.addListener(backgroundDocsSaver);
				
//				kdbResultPanel.showWebpage(TimeStored.Page.NEWS.url());
				if(queryManager.hasAnyServers()) {
					if(UpdateHelper.possiblyShowTimeStoredWebsite(persistance)) {
						QStudioFrame.this.requestFocus();
					}
				}

				if(conMan.getServerConnections().isEmpty()) {
					WelcomeDialog wd = new WelcomeDialog(QStudioFrame.this, QStudioModel.APP_TITLE, VERSION, commonActions.getAddServerAction());
					wd.setVisible(true);
				}
				
				// Items that can be very slow can go here. i.e. after everything else loaded.
				UpdateHelper.checkVersion(qStudioModel, queryCount, growler);
				
				conMan.addListener(new ConnectionManager.Adapter() {
					@Override public void serverAdded(ServerConfig sc) {
						if(conMan.getServerConnections().size() > 3) {
							return; // no need to help, they are experienced users
						}
						BackgroundExecutor.EXECUTOR.execute(() -> {
							// Allow the add server to close etc.
							try { Thread.sleep(1500); } catch (InterruptedException e) {}
							if(sc.isKDB()) {
								String msg = "You just added a kdb+ server."
										+ "<br>Would you like to see example code for generating charts from kdb+ queries?";
								EventQueue.invokeLater(() -> {
									int choice = CommonActions.showDismissibleWarning(persistance, Persistance.Key.SHOW_KDB_EXAMPLE_WARNING, 
												msg, "Open kdb+ Demo", "Open kdb+ Example Code", JOptionPane.CANCEL_OPTION);
									if(choice == JOptionPane.OK_OPTION) {
										openExampleFile(this.getClass(), KDB_EXAMPLE_FILE);
									}
								});
							}
						});
					}
				});
				try {
					qStudioModel.loadExistingParquet();
				} catch (SQLException e) {
					growler.showWarning("Could not load all files within the QDuckDB folder.", "QDuckDB Load Error");
				}
			}
			
		});

		BackgroundExecutor.EXECUTOR.execute(() -> { TimeStored.fetchOnlineNews(); });
        
		LOG.info("Finished QStudioFrame Constructor");
	}

	
	private static QueryStatusBar getStatusBar(QueryManager queryManager) {
		
		final QueryStatusBar queryStatusBar = new QueryStatusBar();
	    queryManager.addQueryListener(new QueryAdapter() {
	    	@Override public void sendingQuery(ServerConfig sc, final String query) {
	    		queryStatusBar.startQuery(query);
	    	}
	    	
	    	@Override public void queryResultReturned(ServerConfig sc, final QueryResult qr) {
    			int count = -1;
    			String txt = "";
	    		if(qr.rs == null) {
		    		count = (qr.k==null ? -1 : KdbHelper.count(qr.k));
		    		String res = KdbHelper.asLine(qr.k);
		    		txt = qr.getResultType() + ":" + (res == null ? "" : res);
	    		} else {
	    			try {
						count = DBHelper.getSize(qr.rs);
						txt = qr.rs.getMetaData().getColumnCount() + " columns";
					} catch (SQLException e) { }
	    		}
	    		queryStatusBar.endQuery(txt, count);
	    	}
	    });
		return queryStatusBar;
	}
	
	

	/** 
	 * A workaround used to delay before showing a popup on the eventqueue.
	 * Used as the windowing toolkit uses timers to request focus,
	 * which meant popup windows were getting sent to back.
	 */
	public static void showPopup(final Window frame, final Component component, final String title, final Image icon) {
		new Thread(() -> {
			try { Thread.sleep(200); } catch (InterruptedException e) { }
			EventQueue.invokeLater(() -> {
					final JFrame f = SwingUtils.getPopupFrame(frame, title, component, icon);
					f.setVisible(true);						
			});
		}).start();
	}
	
	private void setFrameTitle() {
		String docTitle = openDocsModel.getSelectedDocument().getTitle();
		String srvr = queryManager.getSelectedServerName();
		setTitle(docTitle + (srvr==null ? "" : " #" + srvr) + " - " + QStudioModel.APP_TITLE); 
	}


	private void setFrameColor() {
		String srvr = queryManager.getSelectedServerName();
		 
		HashSet<String> kws = Sets.newHashSet(myPreferences.getCriticalServerKeywords().split(","));
		Color c = defaultFrameColor;
		for(String k : kws) {
			if(srvr!=null && srvr.contains(k)) {
				c = myPreferences.getCriticalServerColor();
				break;
			}
		}
		if(srvr != null) {
			ServerConfig sc = conMan.getServer(srvr);
			if(sc != null) {
				Color tc = sc.getColor();
				if(tc != null && !sc.isDefaultColor()) {
					c = tc;
				}
			}
		}
		getContentPane().setBackground(c);
	}

	@FunctionalInterface
	public interface QStudioFramePlugin<T> {
		List<T> getPlugin(@NonNull QStudioFrame qStudioFrame, @NonNull Growler growler, @NonNull CommandManager commandManager);
	}
	
	/**
	 * @return The help menu containing about / help links etc.
	 * @param openDocsModel Used for opening example documents.
	 * @param parentFrame Used for positioning popup dialogs.
	 * @param growler Used to show non-obtrucive alerts
	 */
	private JMenu getHelpMenu() {
		
		final JMenu helpMenu = getJMenu(Msg.get(Key.HELP), KeyEvent.VK_H);
        Action a = HtmlUtils.getWWWaction(Msg.get(Key.HELP), Page.QSTUDIO_HELP.url());
        a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
        a.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("F1"));
        helpMenu.add(a);
        
        AbstractAction welcomeAction = new AAction(Msg.get(Key.WELCOME), Theme.CIcon.HAND.get16(), ae -> {
				WelcomeDialog wd = new WelcomeDialog(QStudioFrame.this, QStudioModel.APP_TITLE, VERSION, commonActions.getAddServerAction());
				wd.setVisible(true);
			});
		welcomeAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_W);
        helpMenu.add(welcomeAction);
        helpMenu.add(HtmlUtils.getWWWaction("Release Notes", Page.QSTUDIO_CHANGES.url()));
        helpMenu.add(HtmlUtils.getWWWaction("Keyboard Shortcuts", Page.QSTUDIO_HELP_KEYBOARD.url()));
        
        
//        helpMenu.add(HtmlUtils.getWWWaction(Msg.get(Key.REPORT_A_BUG), TimeStored.getContactUrl("qStudio Bug Report")));
	    helpMenu.addSeparator();
        helpMenu.add(HtmlUtils.getWWWaction(Msg.get(Key.REPORT_A_BUG), Page.QDOC_REPORT_ISSUE.url()));
        helpMenu.add(HtmlUtils.getWWWaction("Search Feature Requests", Page.QDOC_FEATURE_REQUEST.url()));
	    helpMenu.addSeparator();


		ImageIcon codeImage = Theme.CIcon.PAGE_CODE.get16();
        helpMenu.add(new AAction("Open QStudio Folder", Theme.CIcon.FOLDER.get16(), e-> {
        		File p = QStudioModel.APP_HOME;
				try {
					Desktop.getDesktop().open(p);
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(null, "Error opening folder: " +  (p == null ? "null" : p.getAbsolutePath()));
				}
			}));
        helpMenu.add(new AAction(Msg.get(Key.OPEN_EXAMPLE_CHARTS), codeImage, e-> {
				QStudioFrame.this.openExampleFile(QStudioFrame.class, KDB_EXAMPLE_FILE);
			}));

        helpMenu.add(new AAction("Open DuckDB Example .sql", codeImage, e-> {
				QStudioFrame.this.openExampleFile(QStudioFrame.class, "duckdb.sql");
			}));

        for(Action pluginAction : helpMenuPlugin.getPlugin(this, growler, commandManager)) {
            helpMenu.add(pluginAction);	
        }
    	
	    helpMenu.addSeparator();
        helpMenu.add(new AAction(Msg.get(Key.ABOUT), e -> showAboutDialog(QStudioFrame.this)));
		return helpMenu;
	}


	public void openExampleFile(Class<?> containerClass, String file) {
		try {
			String welcomeCode = IOUtils.toString(containerClass, file);
			File tempf = Files.createTempDir();
			File f = new File(tempf, file);
			IOUtils.writeStringToFile(welcomeCode, f);
			openDocsModel.openDocument(f);
		} catch(IOException e) {
			String msg = Msg.get(Key.COULD_NOT_LOAD_FILE) + file;
			LOG.severe(msg);
			growler.showSevere(msg, "load error");
		}
	}
	
	/**
	 * @param openDocsModel Will be sent openDocument requests when file clicked.
	 * @param documentActions Used to get getOpenFolderAction.
	 * @return {@link FileTreePanel} that displays the selected folder of {@link OpenDocumentsModel}
	 */
	private static FileTreePanel getFileTreePanel(final OpenDocumentsModel openDocsModel, 
			final DocumentActions documentActions) {
		
		final FileTreePanel fileTreePanel = new FileTreePanel();
		fileTreePanel.setName("DocFolderFileTreePanel");
		openDocsModel.addListener(new OpenDocumentsModel.Adapter() {
			@Override public void folderSelected(File selectedFolder) {
				fileTreePanel.setRoot(selectedFolder);
			}
		});
		
		final Runnable syncSettings = () -> {
			File openFolder = openDocsModel.getSelectedFolder();
			fileTreePanel.setRoot(openFolder);
	        final Pattern pat = Pattern.compile(MyPreferences.INSTANCE.getIgnoreFilterRegex());
	        fileTreePanel.setIgnoredFoldersRegex(pat);
		};
		syncSettings.run();
        openDocsModel.addListener(new OpenDocumentsModel.Adapter() {
			@Override public void ignoredFolderPatternSelected(Pattern ignoredFolderPattern) {
				syncSettings.run();
			}
			@Override public void folderSelected(File selectedFolder) {
				syncSettings.run();
			}
		});
		
		JPanel noRootsComponent = Theme.getVerticalBoxPanel();
		noRootsComponent.add(Theme.getSubHeader(Msg.get(Key.NO_FOLDER_SELECTED)));
		JPanel p = new JPanel();
		p.add(new JButton(documentActions.getOpenFolderAction()));
		noRootsComponent.add(p);
		fileTreePanel.setNoRootsComponent(noRootsComponent);
		return fileTreePanel;
	}
	
	public static boolean isClipped(Rectangle rec) {
	    int recArea = rec.width * rec.height;
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice sd[] = ge.getScreenDevices();
	    int boundsArea = 0;

	    for (GraphicsDevice gd : sd) {
	        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
	        if (bounds.intersects(rec)) {
	            bounds = bounds.intersection(rec);
	            boundsArea = boundsArea + (bounds.width * bounds.height);
	        }
	    }
	    return boundsArea != recArea;
	}
	
	private void restoreBounds(Persistance persistance) {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Insets si = tk.getScreenInsets(getGraphicsConfiguration());
        int defWidth = (tk.getScreenSize().width - si.left - si.right)*4/5;
        int defHeight = (tk.getScreenSize().height - si.top - si.bottom)*4/5;
        int w = persistance.getInt(Persistance.Key.FRAME_WIDTH, defWidth);
        int h = persistance.getInt(Persistance.Key.FRAME_HEIGHT, defHeight);
        int x = persistance.getInt(Persistance.Key.FRAME_X, 0);
        int y = persistance.getInt(Persistance.Key.FRAME_Y, 0);
        Rectangle r = new Rectangle(x, y, w, h);
        if(!isClipped(r)) {
        	setBounds(x, y, w, h);
        } else {
        	setBounds(0, 0, defWidth, defHeight);
        }
        // Not iconified
        int xs = persistance.getInt(Persistance.Key.FRAME_EXTENDEDSTATE, Frame.MAXIMIZED_BOTH) & ~Frame.ICONIFIED;
        setExtendedState(xs);
	}

	private DefaultDockable createDockable(String uniqueTitle,
			final Icon icon, JPanel panel, ActionListener refreshAL, DefaultDockActionSource actions) {
		
		DefaultDockable d = new DefaultDockable(panel, uniqueTitle, icon.get16());
		
		/*  Add to frontend to allow saving etc. */
		frontend.addDockable(uniqueTitle, d);
		frontend.setHideable(d, true);

		/** Set the action offers for this dockable **/
		if(refreshAL != null) {
			actions.add(makeButton(Msg.get(Key.REFRESH), Theme.CIcon.ARROW_REFRESH, refreshAL));
		}
		
		// If it can supply a Grab, then use that to produce popup frames
		if(panel!=null && panel instanceof GrabableContainer) {
			final GrabableContainer gc =  (GrabableContainer) panel;
			SimpleButtonAction button = makeButton("Pop-out"+uniqueTitle, Theme.CIcon.POPUP_WINDOW, new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					GrabItem gi = gc.grab();
					if(gi != null) {
						String title = StringUtils.abbreviate(gi.getTitle(), 70);
						// use grabIt specific icon if possible otherwise dockable
						Icon ic = icon;
						if(gi.getIcon() != null) {
							ic = gi.getIcon(); 
						}
						showPopup(QStudioFrame.this, gi.getComponent(), title, ic.getBufferedImage());	
					}
				}
			});
			actions.add(button);
		}

		d.setActionOffers(actions);
		return d;
	}
	
	private static SimpleButtonAction makeButton(String text, CIcon cIcon, ActionListener actionListener) {
		SimpleButtonAction button = new SimpleButtonAction();
		button.setText(text);
		button.setIcon(cIcon.get16());
		button.addActionListener(actionListener);
		return button;
	}

	/**
	 * @param refreshAL optional parameter that if specified adds a refresh button the the dockable
	 * @param panel to display. If it implelemts {@link GrabableContainer} it will allow popups
	 */
	private DefaultDockable createDockable(String uniqueTitle,
			final Icon icon, JPanel panel, ActionListener refreshAL) {
		return createDockable(uniqueTitle, icon, panel, refreshAL, new DefaultDockActionSource());
	}
	
	/** take user preferences object and push its values to relevant models */
	private void pushPreferencesToModels() {
		queryManager.setMaxReturnSize(myPreferences.getMaxReturnSize());
		queryManager.setQueryWrapped(myPreferences.isQueryWrapped());
		queryManager.setQueryWrapPrefix(myPreferences.getQueryWrapPre());
		queryManager.setQueryWrapPostfix(myPreferences.getQueryWrapPost());
		queryManager.setConnectionPersisted(myPreferences.isConnectionPersistent());
		AIFacade.setOpenAIkey(myPreferences.getOpenAIkey());
		
		sTreePanel.setHiddenNamespaces(myPreferences.getHiddenNamespaces());
		consolePanel.setMaxLength(myPreferences.getMaxConsoleLength());

		Font f = myPreferences.getCodeFontFont();
		if(f != null) {
			serverDocumentPanel.setEditorFont(f);
			consolePanel.setCodeFont(f);
		}
		int uiScale = myPreferences.getUIScale();
		if(uiScale > 0) {
			System.setProperty( "flatlaf.uiScale", ""+(uiScale/100.0));
		}

		String lfname = myPreferences.getCodeTheme();
		boolean isDarkTheme = AppLaunchHelper.isLafDark(lfname);
		EditorConfigFactory.TCOLOR tcolor = isDarkTheme ? EditorConfigFactory.TCOLOR.DARK : EditorConfigFactory.TCOLOR.LIGHT;
		AppLaunchHelper.setTheme(lfname);
		SwingUtilities.updateComponentTreeUI(this);
		ChartTheme chartTheme = isDarkTheme ? ViewStrategyFactory.DARK_THEME : ViewStrategyFactory.LIGHT_THEME;
		chartResultPanel.setChartTheme(chartTheme);		
		
		EditorConfig editorConfig = EditorConfigFactory.getByName(myPreferences.getCodeEditorTheme(), isDarkTheme);
		ServerDocumentPanel.setEditorConfig(editorConfig);
		
		TabLayoutPolicy tabLayout = TabLayoutPolicy.fromString(myPreferences.getUiTabLayout());
		serverDocumentPanel.setTabLayoutPolicy(tabLayout);
		queryHistorypanel.setTabLayoutPolicy(tabLayout);
		
		int mr = myPreferences.getMaxRowsShown();
		mr = mr==0 ? Integer.MAX_VALUE : mr;
		PagingTablePanel.setMaximumRowsShown(mr);
		kdbResultPanel.setMaximumRowsShown(mr);
		kdbResultPanel.setNegativeShownRed(myPreferences.isNegativeShownRed());
		queryHistorypanel.setNegativeShownRed(myPreferences.isNegativeShownRed());
		KdbHelper.setNegativeShownRed(myPreferences.isNegativeShownRed());
		queryHistorypanel.setMaximumRowsShown(mr);
		KdbHelper.setMaximumFractionDigits(myPreferences.getMaximumFractionDigits());
		KdbHelper.setGroupingSize(myPreferences.getNumberGroupingSize());
		

        documentActions.setSaveWithWindowsLineEndings(myPreferences.isSaveWithWindowsLineEndings());
        try {
	        final Pattern pat = Pattern.compile(myPreferences.getIgnoreFilterRegex());
			fileTreePanel.setIgnoredFoldersRegex(pat);
			openDocsModel.setIgnoredFoldersRegex(pat);
        } catch(PatternSyntaxException e) {
        	LOG.warning("Could not compile IgnoredFoldersRegex" + myPreferences.getIgnoreFilterRegex());
        }
	}
	

	private JToolBar getToolbar(CommonActions commonActions, QueryManager queryManager, DocumentActions dh) {

        JToolBar toolbar = new JToolBarWithBetterTooltips("Common Actions");
        
        JComboBox serverNameComboBox = new ServerNameComboBox(queryManager);
        serverNameComboBox.setName("serverNameComboBox");
        serverNameComboBox.setMinimumSize(new Dimension(SERVER_NAME_WIDTH, 1));
        serverNameComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        toolbar.add(dh.getNewFileAction());
        toolbar.add(openFilesAction);
        toolbar.add(dh.getSaveFileAction());
        toolbar.addSeparator();

        commonActions.getQueryActions().forEach(toolbar::add);
        commonActions.getAiActions().forEach(toolbar::add);
        
        boolean isMac = System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
        JTextField searchComboBox = new JTextField("Cmd Search [" + (isMac ? "Cmd" : "Ctrl") + "]+[P]");
        searchComboBox.setName("searchComboBox");
        String searchTooltip = "Search all available commands within qStudio.\r\nServer changing, Docs search etc.";;
		searchComboBox.setToolTipText(searchTooltip);
        searchComboBox.setMinimumSize(new Dimension(SERVER_NAME_WIDTH, 1));
        searchComboBox.setMaximumSize(new Dimension(SERVER_NAME_WIDTH * 2, Integer.MAX_VALUE));
        searchComboBox.addMouseListener(new MouseAdapter() {
        	@Override public void mousePressed(MouseEvent e) {
        		commandPaletteAction.actionPerformed(null);
        	}
		});
        JButton searchButton = new JButton(Theme.CIcon.SEARCH.get16());
        searchButton.addActionListener(ae -> {
    		commandPaletteAction.actionPerformed(null);
        });
        searchButton.setToolTipText(searchTooltip);
        

        toolbar.addSeparator();
        
        for(JButton b : toolbarPlugin.getPlugin(this, growler, commandManager)) {
        	toolbar.add(b);	
        }
        toolbar.addSeparator();
        
        toolbar.add(searchButton);
        toolbar.add(searchComboBox);
        toolbar.addSeparator();
        JLabel serverLabel = new JLabel(Msg.get(Key.SERVER) + ":");
        serverLabel.setLabelFor(serverNameComboBox);
        toolbar.add(serverLabel);
        toolbar.add(commonActions.getServerSelectAction());
        toolbar.add(serverNameComboBox);
        Action copyServer = commonActions.getCopyServerHopenToClipboardAction();
        if(conMan.containsKdbServer()) {
        	toolbar.add(copyServer).setName("copyHopenButton");
        }
        Action editServer = commonActions.getEditCurrentServerAction();
        toolbar.add(editServer).setName("editServerButton");
        Action addServer = commonActions.getAddServerAction();
        toolbar.add(addServer).setName("addServerButton");
        
        return toolbar;
	}

	private JMenuBar getBasicMenuBar(final CommonActions commonActions,  DocumentActions dh) {

    	// server menu
        final JMenu serverMenu = getJMenu(Msg.get(Key.SERVER), KeyEvent.VK_S);
        // must do this now to register shortcut before ever selected
        serverMenu.addMenuListener(new MenuListener() {
        	
        	@Override public void menuSelected(MenuEvent e) {
				serverMenu.removeAll();
				for(Action a : commonActions.getServerActions()) {
					serverMenu.add(new JMenuItem(a));
				}
		        serverMenu.addSeparator();
		        List<ServerConfig> cons = conMan.getServerConnections();
		        final int DISPLAY_LIMIT = 9;
		        for(int i=0; i<cons.size() && i < DISPLAY_LIMIT; i++) {
		        	ServerConfig sc = cons.get(i);
		        	serverMenu.add(commonActions.getEditServerAction(sc));
		        }
		        if(cons.size() > DISPLAY_LIMIT) {
		        	serverMenu.add(new JMenuItem("..."));
		        	serverMenu.add(new JMenuItem("Right Click on Server Tree to Edit Servers"));
		        }
        	}

			@Override public void menuCanceled(MenuEvent e) { }
        	@Override public void menuDeselected(MenuEvent e) { }
		});

    	// Edit menu
        final JMenu editMenu = getJMenu(Msg.get(Key.EDIT), KeyEvent.VK_E);
        MenuListener ml = new MenuListener() {
        	
        	@Override public void menuSelected(MenuEvent e) {
        		editMenu.removeAll();
        		editMenu.add(searchFilesAction);
                for(Action a: documentActions.getEditorActions()) {
                	if(a == null) {
                		editMenu.addSeparator();
                	} else {
                		editMenu.add(new JMenuItem(a));
                	}
                }
        		editMenu.addSeparator();
        		editMenu.add(documentActions.getUndoAction());
        		editMenu.add(documentActions.getRedoAction());
        	}
        	
        	@Override public void menuCanceled(MenuEvent e) { }
        	@Override public void menuDeselected(MenuEvent e) { }
		};
        editMenu.addMenuListener(ml);
        ml.menuSelected(null); // Force Add so that action keyboard shortcuts work.

    	// query menu
        final JMenu queryMenu = getJMenu(Msg.get(Key.QUERY), KeyEvent.VK_Q);
        for(Action a: commonActions.getQueryActions()) {
            queryMenu.add(new JMenuItem(a));
        }
        queryMenu.addSeparator();
        for(Action a: commonActions.getAiActions()) {
            queryMenu.add(new JMenuItem(a));
        }
        if(conMan.containsKdbServer()) {
	        queryMenu.addSeparator();
	        for(Action a: commonActions.getProActions()) {
	            queryMenu.add(new JMenuItem(a));
	        }
        }
        
    	// q menu
        final JMenu toolsMenu = getJMenu(Msg.get(Key.TOOLS), KeyEvent.VK_T);
        if(documentActions.getGenerateDocumentationAction()!=null) {
        	JMenuItem mi = new JMenuItem(documentActions.getGenerateDocumentationAction());
        	mi.setEnabled(conMan.containsKdbServer());
        	if(!conMan.containsKdbServer()) {
        		mi.setToolTipText("Only works for kdb+. Add kdb+ server and restart");
        	}
        	toolsMenu.add(mi);
        }

    	for(Action a : toolsMenuPlugin.getPlugin(this, growler, commandManager)) {
    		toolsMenu.add(a);
    	}

    	// Settings menu
        final JMenu settingsMenu = getJMenu(Msg.get(Key.SETTINGS), KeyEvent.VK_G);
        settingsMenu.add(new JMenuItem(new AAction(Msg.get(Key.PREFERENCES), Theme.CIcon.PREFERENCES.get16(), ae -> {
				new PreferencesDialog(MyPreferences.INSTANCE, QStudioFrame.this);
			})));
        settingsMenu.add(new JMenuItem(new AAction("Set Default Database Login", Theme.CIcon.SET_PASSWORD.get16(), ae -> {
				// Allow user to change default username / password and if he does update preferences
				new LoginDialog(conMan, QStudioFrame.this);
				MyPreferences.INSTANCE.setDefaultLoginUsername(conMan.getDefaultLoginUsername());
				MyPreferences.INSTANCE.setDefaultLoginPassword(conMan.getDefaultLoginPassword());
			})));
        
        settingsMenu.add(new JMenuItem(new AbstractAction(Msg.get(Key.RESET_ALL)) {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) {
				String message = "This will close qStudio and remove:" +
						"\r\n- all saved connections" +
						"\r\n- stored settings" +
						"\r\n- open file history. \r\nAre you sure you want to continue?";
				int choice = JOptionPane.showConfirmDialog(QStudioFrame.this, message, 
						Msg.get(Key.DELETE_ALL_SETTINGS), JOptionPane.YES_NO_OPTION, 
						JOptionPane.WARNING_MESSAGE);
				
				if(choice == JOptionPane.YES_OPTION) {
					try {
						resetDefaults(false);
						System.exit(1);
					} catch (BackingStoreException e1) {
						String errMsg = "Problem accessing registry, please report as bug";
						JOptionPane.showMessageDialog(QStudioFrame.this, errMsg);
					}
				}
			}
		}));
        

    	// File menu
        fileMenu = getJMenu(Msg.get(Key.FILE), KeyEvent.VK_F);
        fileMenu.addMenuListener(new MenuListener() {
        	@Override public void menuSelected(MenuEvent e) {
        		rebuildFileMenu();
        	}

			@Override public void menuCanceled(MenuEvent e) { }
        	@Override public void menuDeselected(MenuEvent e) { }
		});
        
        // overall menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(queryMenu);
        menuBar.add(settingsMenu);
    	menuBar.add(toolsMenu);
        menuBar.add(serverMenu);
        return menuBar;
    }

    private static JMenu getJMenu(String title, int mnemonic) {
    	JMenu m = new JMenu(title);
    	m.setName(title + "Menu");
		m.setMnemonic(mnemonic);
    	return m;
    }

	/**
	 * Given current state of connections and docs rebuild file menu.
	 */
	private void rebuildFileMenu() {

		fileMenu.removeAll();
		fileMenu.add(documentActions.getNewFileAction());
		fileMenu.add(openFilesAction);
		
        for(Action a : documentActions.getFileActions()) {
        	fileMenu.add(new JMenuItem(a));
        }
        
        JMenuItem print = new JMenuItem(Msg.get(Key.PRINT));
        print.setMnemonic('x');
        print.setAction(new AbstractAction(Msg.get(Key.PRINT)) {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent ae) {
				Document doc = openDocsModel.getSelectedDocument();

				MessageFormat footerFormat = null;
				if(doc.getFilePath() != null) {
					footerFormat = new MessageFormat(doc.getFilePath());
				}
				try {
					serverDocumentPanel.print(new MessageFormat(doc.getTitle()), footerFormat);
				} catch (PrinterException e) {
					growler.showSevere("Could not print document", "Printing Failed");
					LOG.log(Level.WARNING, "Printing Failed", e);
				}
			}
		});
        fileMenu.add(print);
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new ShortcutAction("New DuckDB Database", CIcon.DUCK, "") {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) {
				File savedDocFile = SwingUtils.askUserSaveLocation(null, "duckdb");
				if(savedDocFile != null) {
					qStudioModel.addDBfiles(Arrays.asList(savedDocFile));
				}
			}
		}));
        fileMenu.add(new JMenuItem(new ShortcutAction("Open Database (sqlite/duckdb/h2)", CIcon.SERVER_DATABASE, "") {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser();
				jfc.showOpenDialog(null);
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				File selFile = jfc.getSelectedFile();
				if(selFile != null) {
					qStudioModel.addDBfiles(Arrays.asList(selFile));
				}
			}
		}));
        
        // Display recently opened documents
        if(recentDocumentPersister != null) {
        	final List<String> filePaths = recentDocumentPersister.getRecentFilePaths();
	        fileMenu.addSeparator();
	        for(Action a : documentActions.getOpenRecentActions(filePaths)) {
	        	fileMenu.add(a);
	        }
	        fileMenu.addSeparator();
	        fileMenu.add(documentActions.openAllAction(filePaths));
        }

        fileMenu.addSeparator();
        // post window closing to message queue for exit handling
        JMenuItem exit = new JMenuItem(Msg.get(Key.EXIT));
        exit.setMnemonic('x');
        exit.addActionListener(new ActionListener() {
          @Override public void actionPerformed(ActionEvent e) {
        	  WindowEvent wev = new WindowEvent(QStudioFrame.this, 
        			  WindowEvent.WINDOW_CLOSING);
              Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
          }
        });
        fileMenu.add(exit);
	}

	/**
	 * 
	 * @param wipeLicense wipes everything, license and first ever run date.
	 * 	meaning the software would revert to a free trial.
	 */
	public static void resetDefaults(boolean wipeLicense) throws BackingStoreException {
		MyPreferences.INSTANCE.resetDefaults();
		Persistance.INSTANCE.clear(wipeLicense);
	}
	
	public ConnectionManager getConnectionManager() {
		return conMan;
	}

	public static void showAboutDialog(JFrame frame) {
		Icon icon = Theme.CIcon.QSTUDIO_LOGO;
		String htmlTitle = "<h1><font color='#2580A2'>q</font><font color='#25A230'>Studio</font></h1>";
		new AboutDialog(frame, QStudioModel.APP_TITLE, icon, htmlTitle, VERSION).setVisible(true);
	}

	public void handleArgs(List<String> args) {
		// If they drop .csv into qStudio - open it as text file. Only if right click in explorer do query.
		if(args.size() == 2 && args.get(0).toLowerCase().equals("query-csv")) {
			File csvFile = new File(args.get(1));
			qStudioModel.addDataFiles(Lists.newArrayList(csvFile));
			return;
		}
		handleArgsFiles(args.stream().map(s -> new File(s)).collect(Collectors.toList()));
	}

	public void handleArgsFiles(File file) { handleArgsFiles(Arrays.asList(file)); }
	public void handleArgsFiles(List<File> files) {
		Predicate<String> isDB = s -> s.endsWith(".duckdb") || s.endsWith(".db") || s.endsWith(".sqlite");  
		List<File> dbFiles = files.stream().filter(f -> isDB.test(f.getName())).collect(Collectors.toList());
		List<File> parquetFiles = files.stream().filter(f -> f.getName().endsWith(".parquet")).collect(Collectors.toList());
		List<File> docsToOpen = files.stream().filter(f -> !isDB.test(f.getName()) && !f.getName().endsWith(".parquet")).collect(Collectors.toList());
		documentActions.openFiles(docsToOpen);
		qStudioModel.addDBfiles(dbFiles); // Add as connections
		qStudioModel.addDataFiles(parquetFiles);
	}
	
	
	public void setLastQueryResult(QueryResult lastQueryResult) {
		this.lastQueryResult = lastQueryResult;
		boolean hasRS = lastQueryResult != null && lastQueryResult.rs != null;
		xlsButton.setEnabled(hasRS);
		emailButton.setEnabled(hasRS);
		saveToDuckButton.setEnabled(hasRS);
		pivotButton.setEnabled(hasRS);
	}
}
