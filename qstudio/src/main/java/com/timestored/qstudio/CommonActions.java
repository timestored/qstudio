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
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.timestored.command.Command;
import com.timestored.command.CommandDialog;
import com.timestored.command.CommandManager;
import com.timestored.command.CommandProvider;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ConnectionManagerDialog;
import com.timestored.connections.ConnectionShortFormat;
import com.timestored.connections.JdbcTypes;
import com.timestored.connections.ServerConfig;
import com.timestored.connections.ServerConfigBuilder;
import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.jgrowl.Growler;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.misc.AIFacade;
import com.timestored.misc.HtmlUtils;
import com.timestored.misc.AIFacade.AIresult;
import com.timestored.qstudio.kdb.CAtomTypes;
import com.timestored.qstudio.model.QueryAdapter;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.qstudio.model.QueryResult;
import com.timestored.qstudio.model.ServerQEntity;
import com.timestored.qstudio.model.TableSQE;
import com.timestored.swingxx.AAction;
import com.timestored.theme.ShortcutAction;
import com.timestored.theme.Theme;
import com.timestored.theme.Theme.CIcon;

/**
 * Contains common actions for querying, editing servers etc that can be 
 * activated from various parts of qStudio.
 */
public class CommonActions implements CommandProvider {

	private static final Logger LOG = Logger.getLogger(CommonActions.class.getName());

	@Setter private static ActionPlugin proActionPlugin = null;
	private final Action copyServerHopenToClipboardAction;
	private final Action editCurrentServerAction;
	
	private final Action qAction;
	private final Action qLineAction;
	private final Action qFileAction;
	private final Action generateAIQueryAction;
	private final Action aIExplainAction;
	private final Action generateAIAction;
	private final Action googleAction;
	private final Action qCurrentStatementAction;
	private final Action disconnectAllAction;
	private final Action qLineAndMoveAction;
	private final Action qCancelQueryAction;
	private final Action sendClipboardQuery;
	private final QueryManager queryManager;
	private final Growler growler;

	private final Action addServerAction = new AddServerAction(null);
	private final Action serverSelectAction;
	
	private final Action addServerListAction;
	private final Action removeServerAction;
	private final AbstractAction copyServerListAction;
	private final List<Action> serverActions;
	
	@Getter private final List<Action> queryActions;
	@Getter private final List<Action> aiActions;
	@Getter private final List<Action> proActions;

	private final QStudioModel qStudioModel;
	private final OpenDocumentsModel openDocumentsModel;
	private final ConnectionManager connectionManager;
	private JFrame parent;
//	@Getter private final Action watchDocExpressionAction;
	
	public static final KeyStroke AUTO_COMPLETE_KS = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_MASK);
	
	@FunctionalInterface
	public interface ActionPlugin {
		List<Action> getActions(@NonNull CommonActions commonActions, @NonNull Growler growler);
	}
	
	/**
	 * @param parent the frame that popup dialogs will be centred to.
	 */
	CommonActions(final QStudioModel qStudioModel, final JFrame parent, final Persistance persistance, final Growler growler) {
		this.qStudioModel = Preconditions.checkNotNull(qStudioModel);
		this.openDocumentsModel = qStudioModel.getOpenDocumentsModel();
		this.queryManager = qStudioModel.getQueryManager();
		this.connectionManager = qStudioModel.getConnectionManager();
		this.parent = parent;
		this.growler = growler;


		this.proActions = proActionPlugin == null ? Collections.emptyList() : proActionPlugin.getActions(this, growler);
		/*
		 * Construct all of our actions
		 */        
		qAction = new ShortcutAction(Msg.get(Key.QUERY_SELECTION),
        		Theme.CIcon.GREEN_NEXT, "Send highlighted query to current server.",
        		KeyEvent.VK_E, KeyEvent.VK_E) {
					private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				String qry = openDocumentsModel.getSelectedDocument().getSelectedText();
				if(qry.length()>0) {
					sendQuery(openDocumentsModel.getSelectedDocument().getSelectedText());	
				} else {
					sendQuery(openDocumentsModel.getSelectedDocument().getContent());
				}
			}
		};
		
		editCurrentServerAction = new ShortcutAction("Edit current server",
        		Theme.CIcon.SERVER_EDIT, "Edit the currently selected server.") {
			@Override public void actionPerformed(ActionEvent e) {
				String serverName = queryManager.getSelectedServerName();
				if(serverName != null) {
					new ConnectionManagerDialog(connectionManager, parent, serverName).setVisible(true);
				}
			}
			
			@Override public boolean isEnabled() {
				return queryManager.getSelectedServerName() != null;
			}
		};
		
		copyServerHopenToClipboardAction = new ShortcutAction("Copy hopen `:Server",
        		Theme.CIcon.EDIT_COPY, "Copy an hopen command for the current server to the clipboard") {
			@Override public void actionPerformed(ActionEvent e) {
				String serverName = queryManager.getSelectedServerName();
				if(serverName != null) {
					ServerConfig server = connectionManager.getServer(serverName);
					if(server != null) {
						Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
						String hopenCommand = "hopen `$\":" + server.getHost() + ":" + server.getPort() + ":" + server.getUsername() + ":\"";
						clpbrd.setContents(new StringSelection(hopenCommand ), null);
						growler.showInfo("Copied to clipboard:\r\n" + hopenCommand, "Clipboard Set");
					}
				}
				
			}
		};
		
		
		generateAIQueryAction = new GenerateAIQueryAction("AI Text to SQL");
		aIExplainAction = new AIExplainAction("AI Explain SQL");
		generateAIAction = new GenerateAIAction("Ask OpenAI");
		googleAction = new GoogleAction("Google");
		
        qLineAction = new ShortcutAction(Msg.get(Key.QUERY_LINE),
        		Theme.CIcon.GREEN_PLAY, "Send current line as query",
        		KeyEvent.VK_ENTER, KeyEvent.VK_ENTER) {
					private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				sendQuery(openDocumentsModel.getSelectedDocument().getCurrentLine());
			}
		};
		
		qFileAction = new AAction("Query Full File", Theme.CIcon.SCRIPT_GO.get16(), al -> {
			sendQuery(openDocumentsModel.getSelectedDocument().getContent());
		});
        KeyStroke shortcut = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        qFileAction.putValue(Action.ACCELERATOR_KEY, shortcut);
        qFileAction.putValue(Action.SHORT_DESCRIPTION, "Run the full file.");
        
		// On Mac Cmd+Q = Quit application.
        boolean isMac = System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        if(isMac) {
            ks = KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        }
		qCurrentStatementAction = new ShortcutAction("Query Current Statement",
        		Theme.CIcon.BLUE_PLAY, "Send current statement as query", KeyEvent.VK_Q, ks) {
					private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				ServerConfig sc = connectionManager.getServer(queryManager.getSelectedServerName());
				boolean isKdb = sc == null || sc.isKDB();
				if(isKdb) {
					sendQuery(openDocumentsModel.getSelectedDocument().getCurrentLine());
				} else {
					sendQuery(openDocumentsModel.getSelectedDocument().getCurrentStatement());
				}
			}
		};
		
		disconnectAllAction = new ShortcutAction("Disconnect All Databases",
        		Theme.CIcon.DISCONNECT, "Disconnect from all database servers.") {
					private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				connectionManager.close();
				try {
					queryManager.close();
				} catch (Exception e1) {
					LOG.warning("problem closing QM" + e1.toString());
					growler.show("problem closing QM");
				}
				growler.show("All connections closed.");
			}
		};
		disconnectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
		
		String qlDesc = "Query Line and Move";
		qLineAndMoveAction = new AbstractAction(qlDesc, Theme.CIcon.GREEN_FORWARD.get16()) {
			@Override public void actionPerformed(ActionEvent e) {
				Document doc = openDocumentsModel.getSelectedDocument();
				String qry = doc.getCurrentLine();
				sendQuery("{x}"+qry, qry);
				doc.gotoNextLine();
			}
		};
		qLineAndMoveAction.putValue(AbstractAction.SHORT_DESCRIPTION, qlDesc);
		int modifier = InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK;
		KeyStroke kEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifier);
//		qLineAndMoveAction.putValue(AbstractAction.MNEMONIC_KEY, mnemonic);	
		qLineAndMoveAction.putValue(AbstractAction.ACCELERATOR_KEY, kEnter);

        qCancelQueryAction = new ShortcutAction(Msg.get(Key.CANCEL_QUERY),
        		Theme.CIcon.CANCEL, "Cancel the latest query",
        		KeyEvent.VK_C, KeyEvent.VK_K) {
					private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				queryManager.cancelQuery();
			}
		};

        sendClipboardQuery = new ShortcutAction("Send clipboard as query",
        		Theme.CIcon.EDIT_PASTE, "Send clipboard as query",
        		KeyEvent.VK_B, KeyEvent.VK_B) {
					private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				String qry;
				try {
					qry = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
					if(qry != null && qry.length() > 0) {
						sendQuery(qry);
					}
				} catch (IOException e1) {
					LOG.log(Level.WARNING, "Send Query Error", e1);
					growler.showSevere(e1.getMessage(), Msg.get(Key.ERROR));
				} catch (HeadlessException e1) {
					LOG.log(Level.WARNING, "Send Query Error", e1);
					growler.showSevere(e1.getMessage(), Msg.get(Key.ERROR));
				} catch (UnsupportedFlavorException e1) {
					LOG.log(Level.WARNING, "Send Query Error", e1);
					growler.showSevere(e1.getMessage(), Msg.get(Key.ERROR));
				}
			}
		};
				
		queryActions = Collections.unmodifiableList(Arrays.asList(disconnectAllAction, qAction,  
				qLineAction, qFileAction, qCurrentStatementAction, qLineAndMoveAction, sendClipboardQuery, qCancelQueryAction));
		
		aiActions = Collections.unmodifiableList(Arrays.asList(generateAIAction, aIExplainAction, generateAIQueryAction, googleAction));
		
		queryManager.addQueryListener(new QueryAdapter() {
			@Override public void selectedServerChanged(String server) {
				refreshQButtonEnabledFlags();
			}
			
			@Override public void sendingQuery(ServerConfig sc, String query) {
				qCancelQueryAction.setEnabled(true);
			}
			
			@Override public void queryResultReturned(ServerConfig sc, QueryResult queryResult) {
				qCancelQueryAction.setEnabled(false);
			}
		});
		
		/******************************************************************************
		 * Server Actions
		 */
		addServerListAction = new AddServerListAction(parent, connectionManager);

		serverSelectAction = new ShortcutAction(Msg.get(Key.FIND_SERVER),
        		Theme.CIcon.EDIT_FIND, "Change the selected server by text searching.",
        		KeyEvent.VK_R, KeyEvent.VK_R) {
					private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				CommandDialog cd = new CommandDialog("Select Server:", queryManager.getChangeServerCommands(true), BackgroundExecutor.EXECUTOR);
				cd.setLocationRelativeTo(null);
				cd.setName("ServerNameDialog");
				cd.setVisible(true);
			}
		};
		 
		removeServerAction = new AAction(Msg.get(Key.REMOVE_ALL_SERVERS), Theme.CIcon.DELETE.get16(), e -> {
				int choice = JOptionPane.showConfirmDialog(parent, "Delete all Server Connections?",
						Msg.get(Key.WARNING), JOptionPane.WARNING_MESSAGE);
				if(choice == JOptionPane.YES_OPTION) {
					connectionManager.removeServers();
				}
			});
		
		copyServerListAction = new AAction(Msg.get(Key.COPY_SERVER_LIST_TO_CLIPBOARD), CIcon.EDIT_COPY.get16(), e-> {
				String s = ConnectionShortFormat.compose(connectionManager.getServerConnections(), 
						JdbcTypes.KDB);
				StringSelection sel = new StringSelection(s);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
			});

		serverActions = Collections.unmodifiableList(Arrays.asList(addServerAction, serverSelectAction, 
				addServerListAction, copyServerListAction, removeServerAction, copyServerHopenToClipboardAction, editCurrentServerAction));
		
		refreshQButtonEnabledFlags();
	}



	/**
	 * Show the user a warning about carrying out this particular action.
	 * A checkbox is present to not show the warning again by storing a key
	 * in the persistance.
	 * @return JOptionPane constant as returned by { @link {@link JOptionPane#showOptionDialog}
	 * 		e.g. JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
	 */
	public static int showDismissibleWarning(final Persistance persistance, 
			final Persistance.Key warningKey, String msgHtml, final String title, 
			String confirmation, int defaultChoice) {
		
		int choice = defaultChoice;
		if(persistance.getBoolean(warningKey, true)) {
		
			final JCheckBox showWarningCheckBox = new JCheckBox("Do not show this information again");
			showWarningCheckBox.setName("checky");
			showWarningCheckBox.addActionListener(new ActionListener() {
				
				@Override public void actionPerformed(ActionEvent e) {
					persistance.putBoolean(warningKey, !showWarningCheckBox.isSelected());
				}
			});
			JPanel msg = new JPanel(new BorderLayout());
			msg.add(Theme.getHtmlText(msgHtml), BorderLayout.CENTER);
			msg.add(showWarningCheckBox, BorderLayout.SOUTH);
			String[] buttons = new String[] {confirmation == null ? "Yes" : confirmation,"Cancel"};

			choice = JOptionPane.showOptionDialog(null, msg, title,
				JOptionPane.OK_CANCEL_OPTION,
			   JOptionPane.INFORMATION_MESSAGE,
			   null,
			   buttons,
			   buttons[0]);
		}
		return choice;
	}
	
	private void refreshQButtonEnabledFlags() {
		
		boolean serverAvailable = queryManager.getSelectedServerName()!=null;
		qAction.setEnabled(serverAvailable);
		qLineAction.setEnabled(serverAvailable);
		qCurrentStatementAction.setEnabled(serverAvailable);
		qCancelQueryAction.setEnabled(serverAvailable);
//		watchDocExpressionAction.setEnabled(serverAvailable);
		copyServerHopenToClipboardAction.setEnabled(serverAvailable);
		editCurrentServerAction.setEnabled(serverAvailable);
		
		if(serverAvailable) {
			ServerConfig sc = connectionManager.getServer(queryManager.getSelectedServerName());
			boolean enableKdb = sc == null || sc.isKDB();
			for(Action a : proActions) {
				a.setEnabled(enableKdb);
			}
			copyServerHopenToClipboardAction.setEnabled(enableKdb);
		}
	}
	
	/** attempt to send query, if it fails, report to user */
	private void sendQuery(String qry) {
		sendQuery(qry, null);
	}


	/**
	 * Send a query to the currently selected server and notify listeners of the result.
	 * @param qry The actual kdb query that is sent.
	 * @param queryTitle The query reported as being sent, useful for hiding long internal queries.
	 * 	null makes this default to actual query.
	 */
	public synchronized void sendQuery(final String qry, final String queryTitle) {
		if(connectionManager.isEmpty()) {
			JOptionPane.showMessageDialog(parent, Msg.get(Key.NO_CONNECTIONS_DEFINED) +
					Msg.get(Key.PLEASE_ADD_SERVER_CONNECTION),
					Msg.get(Key.NO_CONNECTIONS),
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(queryManager.isQuerying()) {
			String msg = "A query is currently in progress. \r\nYou must cancel that query before sending another."
					+ "\r\n\r\nIf you want to query multiple servers simultaneously, open multiple qStudio instances.";
			String title = "Query in progress";
			JOptionPane.showMessageDialog(parent, msg,
					title, JOptionPane.WARNING_MESSAGE);
			return;
		}

		ServerConfig sc = connectionManager.getServer(queryManager.getSelectedServerName());
		String qryToRun = qry;
		try {
			if(openDocumentsModel.getSelectedDocument().getFileEnding().toLowerCase().equals("prql")) {
				qryToRun = compilePRQL(qry, sc.getJdbcType());
			}
			LOG.info("Send Query->" + qryToRun);
			queryManager.sendQuery(qryToRun, queryTitle);
		} catch(IllegalStateException ia) {
			LOG.log(Level.WARNING, "Send Query Error", ia);
			growler.showSevere(ia.getMessage(), Msg.get(Key.ERROR));
		} catch(Exception ex) {
			LOG.log(Level.WARNING, "Send Query Error", ex);
			growler.showSevere("Problem sending query to server", Msg.get(Key.ERROR)); 
			queryManager.sendQRtoListeners(sc, QueryResult.exceptionResult(sc, qryToRun, null, ex, 0));
		}
	}
	
	public static String compilePRQL(String qry, JdbcTypes jdbcTypes) throws IOException {
		
		String target = getPrqlTarget(jdbcTypes);
		String[] commands = new String[] { "prqlc","compile", "--hide-signature-comment", "--color", "never" };
		if(target != null && !qry.contains("prql target:")) {
			commands = new String[] { "prqlc","compile", "--hide-signature-comment", "--color", "never", "--target", target };
		}
		
		Process process = new ProcessBuilder(commands).redirectErrorStream(true).start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader bre = new BufferedReader (new InputStreamReader(process.getErrorStream()));
		OutputStream stdin = process.getOutputStream(); // write to this
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
        writer.write(qry);
        writer.close();

		StringBuilder sb = new StringBuilder();
		StringBuilder errSb = new StringBuilder();
		String line = null;
		while ( (line = reader.readLine()) != null) {
		   sb.append(line);
		   sb.append(System.getProperty("line.separator"));
		}
		reader.close();
		while ((line = bre.readLine()) != null) { 
			System.err.println(line); 
			errSb.append(line);
			errSb.append(System.getProperty("line.separator"));
		}
	    bre.close();
	    try {
			process.waitFor();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	    if(process.exitValue()!=0) {
	    	throw new IOException("Non-zero exit value for runArgs:" + sb + System.getProperty("line.separator") + errSb);
	    }

		return sb.toString();
	}


	private static String getPrqlTarget(JdbcTypes jdbcTypes) {
		if(jdbcTypes == null) {
			return null;
		}
		String s = jdbcTypes.equals(JdbcTypes.CLICKHOUSE) ? "clickhouse" :
				jdbcTypes.equals(JdbcTypes.DUCKDB) ? "duckdb" :
					jdbcTypes.equals(JdbcTypes.BABELDB) ? "duckdb" :
					jdbcTypes.equals(JdbcTypes.MSSERVER) ? "mssql" :
						jdbcTypes.equals(JdbcTypes.MYSQL) ? "mysql" :
							jdbcTypes.equals(JdbcTypes.POSTGRES) ? "postgres" :
								jdbcTypes.equals(JdbcTypes.SQLITE_JDBC) ? "sqlite" :
									jdbcTypes.equals(JdbcTypes.SNOWFLAKE) ? "snowflake" : null;
		return s == null ? null : "sql." + s;
	}
	
	/**
	 * @return action that sends the currently highlighted text to q server.
	 */
	Action getqAction() { return qAction; }

	/**
	 * @return action that shows dialogue to allow editing a selected {@link ServerConfig}.
	 */
	public Action getEditServerAction(ServerConfig sc) {
		return new EditServerAction(sc);
	}
	
	/**
	 * @return action that shows dialogue to allow adding a {@link ServerConfig}.
	 */
	public Action getAddServerAction() { return addServerAction; }
	
	public Action getServerSelectAction() { return serverSelectAction; }
	
	public Action getCloseConnServerAction(ServerConfig sc) { return new CloseConnServerAction(sc); }

	/**
	 * @return action that shows dialogue to allow adding a {@link ServerConfig}
	 * 	that is a copy of an existing sc. (name will be postfixed with ' copy')
	 */
	public Action getCloneServerAction(ServerConfig sc) {
		return new CloneServerAction(sc);
	}

	/**
	 * @return action that shows dialogue to allow adding a {@link ServerConfig}.
	 */
	public Action getAddServerAction(String folder) {
		Preconditions.checkNotNull(folder);
		return new AddServerAction(folder);
	}

	/** @return action that copies hopen command for current server to clipboard */
	public Action getCopyServerHopenToClipboardAction() {
		return copyServerHopenToClipboardAction;
	}

	/** @return action that edits the currently selected server */
	public Action getEditCurrentServerAction() {
		return editCurrentServerAction;
	}
	
	
	/**
	 * @return all actions related to modifying server connections.
	 */
	public List<Action> getServerActions() {
		return serverActions;
	}
	
	/** add a server to the {@link ConnectionManager} */
	private class EditServerAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		private final ServerConfig sc;

		public EditServerAction(ServerConfig sc) {
			super(Msg.get(Key.EDIT) + sc.getName() + Msg.get(Key.CONNECTION)); 
			if(!connectionManager.isConnected(sc)) {
				ImageIcon ii = CIcon.SERVER_ERROR.get16();
				this.putValue(SMALL_ICON, ii);
				this.putValue(LARGE_ICON_KEY, ii);
			}
			
			this.sc = sc;
			
			putValue(SHORT_DESCRIPTION,	"Edit the server connection " + sc.getName());
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			new ConnectionManagerDialog(connectionManager, parent, sc.getName()).setVisible(true);
		}
	}


	/** add a server to the {@link ConnectionManager} */
	private class AddServerAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;
		private final String folder;

		public AddServerAction(String folder) {
			super(Msg.get(Key.ADD_SERVER), Theme.CIcon.SERVER_ADD.get());
			putValue(SHORT_DESCRIPTION, "Add a server to the list of possible connections");
			putValue(MNEMONIC_KEY, KeyEvent.VK_A);
			this.folder = folder;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			ConnectionManagerDialog cmd = new ConnectionManagerDialog(connectionManager, parent, null);
			if(folder!=null) {
				cmd.setFolder(folder);
			}
			cmd.setVisible(true);
		}
	}
	
	/** Close all server connections */
	private class CloseConnServerAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;
		private final ServerConfig sc;

		public CloseConnServerAction(ServerConfig sc) {
			super(Msg.get(Key.CLOSE_CONNECTION), Theme.CIcon.SERVER_CONNECT.get());
			putValue(SHORT_DESCRIPTION, "Close all connections to this database.");
			this.sc = sc;
		}
		
		@Override public void actionPerformed(ActionEvent arg0) {
			connectionManager.closePool(sc);
			try {
				queryManager.close();
			} catch (Exception e) {
				LOG.warning("Problem closing QM");
			}
		}
	}
	

	/** Clone a server to the {@link ConnectionManager} */
	private class CloneServerAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;
		private final ServerConfig sc;

		public CloneServerAction(ServerConfig sc) {
			super(Msg.get(Key.CLONE_SERVER), Theme.CIcon.COPY.get());
			putValue(SHORT_DESCRIPTION,
					"Create a new connection with the same settings as this one.");
			putValue(MNEMONIC_KEY, KeyEvent.VK_C);
			this.sc = sc;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			String newName = sc.getName() + " " + Msg.get(Key.COPY);
			ServerConfig sc2 = new ServerConfigBuilder(sc).setName(newName).build();
			connectionManager.addServer(sc2);
			ConnectionManagerDialog cmd = new ConnectionManagerDialog(connectionManager, parent, newName);
			cmd.setVisible(true);
		}
	}
	
	
	/**
	 * @param serverConfig The server that should be removed
	 * @return Action to remove a selected server from the connectionManager
	 */
	public Action getRemoveServerAction(final ServerConfig serverConfig) {
		return new AbstractAction(Msg.get(Key.DELETE_CONNECTION),
				CIcon.DELETE.get16()) {
					private static final long serialVersionUID = 1L;

			@Override public void actionPerformed(ActionEvent e) {
				connectionManager.removeServer(serverConfig);
			}

			@Override public boolean isEnabled() {
				return connectionManager.contains(serverConfig);
			}
		};
	}


	/**
	 * @return Common commands including querying, tools, server actions.
	 */
	@Override public Collection<Command> getCommands() {
		List<Command> a = new ArrayList<Command>();
		a.addAll(CommandManager.toCommands(queryActions));
		a.addAll(CommandManager.toCommands(proActions));
		a.addAll(CommandManager.toCommands(serverActions));
		return a;
	}

	private class GenerateAIAction extends TextAction {

		public GenerateAIAction(String name) {
			super(name);
	        putValue(Action.SMALL_ICON, Theme.CIcon.AI.get16());
	        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
	        putValue(SHORT_DESCRIPTION, "Send your question to OpenAI.");
		}

		@Override public void actionPerformed(ActionEvent e) {
	    	JTextComponent txtc = getTextComponent(e);
	    	Function<String,String> queryReplace = (String selectedText) -> {
				try {
					return AIFacade.queryOpenAIstructured(selectedText).getFirstContent();
				} catch (IOException e1) {
					LOG.warning("Error AI : " + e1);
					return "Error AI : " + e1;
				}
	    	};
	    	performOpenAIQueryReplace(txtc, queryReplace, true);
		}
	}

	private class GoogleAction extends TextAction {
		public GoogleAction(String name) {
			super(name);
	        putValue(Action.SMALL_ICON, Theme.CIcon.GOOGLE.get16());
	        putValue(MNEMONIC_KEY, KeyEvent.VK_G);
	        putValue(SHORT_DESCRIPTION, "Google the selected text or line.");
		}

		@Override public void actionPerformed(ActionEvent e) {
	    	String selectedTxt = openDocumentsModel.getSelectedDocument().getSelectedText();
	    	if(selectedTxt.trim().length() == 0) {
	    		selectedTxt = openDocumentsModel.getSelectedDocument().getCurrentLine();
	    	}
			try {
				String qry = URLEncoder.encode(selectedTxt, "UTF-8");
	    		HtmlUtils.browse("https://www.google.com?q=" + qry);
			} catch (UnsupportedEncodingException e1) {
				LOG.warning("Problem googling:" + e1);
			}
		}
	}
	
	public static boolean checkForOpenAIkey() {
		final String OPENAI_INSTRUCT = "You must first configure an OpenAI key in Settings->Preferences...->Misc";
		boolean hasKey = AIFacade.getOpenAIkey() != null && AIFacade.getOpenAIkey().trim().length() > 1;
		if(!hasKey) {
			JOptionPane.showMessageDialog(null, OPENAI_INSTRUCT);
			new PreferencesDialog(MyPreferences.INSTANCE, null);
		}
		return hasKey;
	}
	
	/**
	 * @param atEnd If true, place the replacement text at the end of the current selection. Otherwise place it before.
	 */
	public void performOpenAIQueryReplace(JTextComponent txtc, Function<String,String> performQuery, boolean atEnd) {
		if(!checkForOpenAIkey()) {
			return;
		}
    	String selectedTxt = openDocumentsModel.getSelectedDocument().getSelectedText();
    	// was possible for caret to be outside bounds when at very end of text
		String fullTxt = txtc.getText().replace("\r","");
		int end = txtc.getSelectionEnd();
		int start = txtc.getSelectionStart(); 
    	if(selectedTxt.trim().length() == 0) {
    		selectedTxt = openDocumentsModel.getSelectedDocument().getCurrentLine();
    		for(;end >= 0 && end < fullTxt.length(); end++) {
    			if(fullTxt.charAt(end) == '\n') {
    				break;
    			}
    		}
    		for(; start >= 0 && start < fullTxt.length(); start--) {
    			if(fullTxt.charAt(start) == '\n') {
    				break;
    			}
    		}
    	}
		try {
			String queryRes = performQuery.apply(selectedTxt);
			LOG.info("queryOpenAIstructured:" + selectedTxt);
			if(atEnd) {
				String newTxt = "\n" + queryRes;
				txtc.setCaretPosition(end);
				txtc.replaceSelection(newTxt);
				txtc.setSelectionStart(end);
				txtc.setSelectionEnd(end + newTxt.length());
			} else { // at start - before selection
				String newTxt = queryRes + "\n";
				txtc.setCaretPosition(start);
				txtc.replaceSelection(newTxt);
				txtc.setSelectionStart(start);
				txtc.setSelectionEnd(start + newTxt.length());
			}
		} catch (Exception e1) {
			LOG.warning("Problem queryOpenAIstructured: " + e1);
		}
	}
	
	private JdbcTypes getJdbcType() {
    	String serverName = queryManager.getSelectedServerName();
    	ServerConfig sc = serverName == null ? null : connectionManager.getServer(serverName);
    	return sc != null ? sc.getJdbcType() : JdbcTypes.KDB;
	}

	private class GenerateAIQueryAction extends TextAction {
		public GenerateAIQueryAction(String name) {
			super(name);
	        putValue(Action.SMALL_ICON, Theme.CIcon.ROBOT_GO.get16());
	        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F7,0));
	        putValue(MNEMONIC_KEY, KeyEvent.VK_A);
	        putValue(SHORT_DESCRIPTION, "Send your question to OpenAI to generate the SQL.");
			
		}

		@Override public void actionPerformed(ActionEvent e) {
        	
	    	JTextComponent txtc = getTextComponent(e);
	    	Function<String,String> queryReplace = (String selectedText) -> {
	        	// Try to get table info for query
	        	String tblInfo = "";
	        	List<ServerQEntity> st = qStudioModel.getAdminModel().getServerModel().getServerObjectTree().getAll();
				if (st != null && st.size() > 0) {
					try {
					List<TableSQE> tables = st.stream().filter(se -> se.getType().equals(CAtomTypes.TABLE)).map(se -> (TableSQE)se).collect(Collectors.toList());
							
					tblInfo = tables.stream()
								.map(se -> {
									String s = "";
									if(se.getQQueries().size()>0) {
										s += se.getQQueries().get(0).getQuery() + "\n";	
									}
									s += se.getFullName() + " Columns: " + Joiner.on(",").join(se.getColNames()) + "\n";
									return s;
								})
								.collect(Collectors.joining());
					} catch(Exception e2) {
						LOG.warning("Could not populate tblInfo for AI query:" + e2);
					}
				}
				LOG.info("queryOpenAIstructured:" + selectedText);
				try {
					AIresult air = AIFacade.queryOpenAIstructured(getJdbcType(), tblInfo, selectedText);
					return air.getFirstCode().trim();
				} catch (IOException e1) {
					LOG.warning("Error AI : " + e1);
					return "Error AI : " + e1;
				}
	    	};
	    	
	    	performOpenAIQueryReplace(txtc, queryReplace, true);
		}
	}

	private class AIExplainAction extends TextAction {
		public AIExplainAction(String name) {
			super(name);
	        putValue(Action.SMALL_ICON, Theme.CIcon.ROBOT_COMMENT.get16());
	        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F6,0));
	        putValue(MNEMONIC_KEY, KeyEvent.VK_E);
	        putValue(SHORT_DESCRIPTION, "Ask OpenAI to explain the SQL.");
			
		}

		@Override public void actionPerformed(ActionEvent e) {
	    	JTextComponent txtc = getTextComponent(e);
			String com = getJdbcType().getComment();
	    	Function<String,String> queryReplace = (String selectedText) -> {
				String aiQry = "For the " + getJdbcType() + " database ";
				aiQry += "explain this SQL code:\n```\n";
				aiQry += selectedText + "\n```\n";
				
				LOG.info("queryOpenAIstructured:" + aiQry);
				try {
					AIresult air = AIFacade.queryOpenAIstructured(aiQry);
					return "\n" + com + air.getFirstContent().trim().replace("\n", ("\n" + com));
				} catch (IOException | IllegalStateException e1) {
					LOG.warning("Error AI : " + e1);
					return "\n" + com + e1.toString().replace("\n", ("\n" + com));
				}
	    	};
	    	
	    	performOpenAIQueryReplace(txtc, queryReplace, false);
		}
	}
}
