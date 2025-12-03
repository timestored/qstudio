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
package com.timestored.qstudio.model;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import kx.c.KException;
import kx.jdbc;
import net.jcip.annotations.ThreadSafe;

import com.google.common.base.Preconditions;
import com.timestored.babeldb.PivotProvider;
import com.timestored.command.Command;
import com.timestored.command.CommandProvider;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.JdbcTypes;
import com.timestored.connections.ServerConfig;
import com.timestored.kdb.KdbConnection;
import com.timestored.qstudio.BackgroundExecutor;
import com.timestored.qstudio.PivotFormConfig;
import com.timestored.qstudio.kdb.KdbHelper;
import com.timestored.theme.Theme;


/**
 * Allows querying a currently selected server and notifies it's listeners.
 */
@ThreadSafe public class QueryManager implements CommandProvider,AutoCloseable {
	
	private static final Logger LOG = Logger.getLogger(QueryManager.class.getName());

	private final List<QueryListener> listeners = new CopyOnWriteArrayList<QueryListener>();
	private List<String> serverNames = Collections.emptyList();

	private final ConnectionManager connectionManager;
	private String previousQuery = "";
	private String previousQueryTitle = null;
	private PivotFormConfig previousPivotConfig = null;
	private String selectedServerName;
	private boolean queryWrapped = true;
	private boolean connectionPersisted = false;
	private boolean querying = false;
	private volatile boolean cancelQuery = false;
	private int commercialDBqueries = 0;

	private String queryWrapPrefix = "";
	private String queryWrapPostfix = "";
	
	private long maxReturnedObjectSize;

	private KdbConnection conn;


	/**
	 * @param connectionManager Provides the list of servers.
	 */
	public QueryManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		connectionManager.addListener(new ConnectionManager.Listener() {
			@Override public void serverAdded(ServerConfig sc) {  }	
			@Override public void prefChange() {
				refreshServerList();
				KdbConnection c = conn;
				if(c != null) {
					try {
						c.close();
					} catch (IOException e) {
						// force dropping of connection in case it no longer exists.
					}
				}
			}

			@Override public void statusChange(ServerConfig serverConfig, 
					boolean connected) {}
		});
		refreshServerList();
	}
	
	public void addQueryListener(QueryListener queryListener) {
		listeners.add(Preconditions.checkNotNull(queryListener));
	}
	
	public void removeQueryListener(QueryListener queryListener) {
		listeners.remove(Preconditions.checkNotNull(queryListener));
	}
	
	/**
	 * Send the last query again,   notify listeners.
	 */
	public void resendLastQuery() {
		sendQuery(previousPivotConfig, previousQuery, previousQueryTitle);
	}
	
	/**
	 * Cancelling does not cancel the query on the server (That's no easily possible)
	 * Instead it disconnects and allows the client to go to a different server
	 * send a query there without waiting.
	 */
	public void cancelQuery() {
		
		synchronized (this) {
			if(querying) {
				try {
					KdbConnection c = conn;
					if(c!=null) {
						LOG.warning("Cancelling query");
						cancelQuery = true;
						try {
							c.close();
						} catch (IOException e) {
							// we expect exception
						}
						LOG.warning("conn closed");
					}
				} finally {
					querying = false;
				}
			}
		}
		
	}
	
	/**
	 * Send a query to the currently selected server and notify listeners of the result.
	 * @throws IllegalStateException If no valid server is currently selected
	 */
	public void sendQuery(final String query) {
		sendQuery(query, null);
	}

	public void sendQuery(final PivotFormConfig pivotConfig, final String query, final String queryTitle) {
		sendQuery(pivotConfig, query, selectedServerName, queryTitle);
	}
	
	/**
	 * Send a query to the currently selected server and notify listeners of the result.
	 * @throws IllegalStateException If no valid server is currently selected
	 */
	public void sendQuery(final String query, final String queryTitle) {
		
		final String sName = selectedServerName;
		if(sName == null) {
			throw new IllegalStateException("Select server to send queries to.");
		}
		
		if(query!=null && query.trim().length()>0 && selectedServerName!=null) {

			synchronized (this) {
				if(querying) {
					throw new IllegalStateException("Only one query at a time");
				}
				querying = true;
			}
			
			BackgroundExecutor.EXECUTOR.execute(new Runnable() {
				@Override public void run() {
					try {
						sendQuery(null, query, sName, queryTitle);
					} finally {
						synchronized (this) {
							querying = false;
						}
					}
				}
			});
		}
		
	}
	
	/**
	 * Send a query to the currently selected server and notify listeners of the result.
	 * @param query The actual kdb query that is sent.
	 * @param queryTitle The query reported as being sent, useful for hiding long internal queries.
	 * 	null makes this default to actual query.
	 * @IllegalStateException If no valid server is currently selected
	 */
	private void sendQuery(PivotFormConfig pivotConfig, final String query, final String serverName,  final String queryTitle) {

		synchronized (this) {
			cancelQuery = false;	
		}
		QueryResult qr = null;
		String sqlSent = query;
		ServerConfig sc = connectionManager.getServer(serverName);
		JdbcTypes jtype = sc.getJdbcType();
		// We have to let pivot know that the underlying DB is really duckdb so it uses DuckDB pivot
		if(jtype.equals(JdbcTypes.BABELDB) && sc.getUrl().startsWith("jdbc:babeldb:duckdb:")) {
			jtype = JdbcTypes.DUCKDB;
		}
		if(pivotConfig != null) {
			sqlSent = PivotProvider.pivotSQL(jtype, pivotConfig.getByColsSelected(), pivotConfig.getPivotColsSelected(), 
									pivotConfig.getAggSel(), pivotConfig.getSqlQuery());
		}
		String title = queryTitle==null ? sqlSent : queryTitle;
		LOG.info("run() sendingQuery: " + query);
		for(QueryListener l : listeners) {
			l.sendingQuery(sc, title);
		}
		
		previousQuery = query;
		previousQueryTitle = queryTitle;
		previousPivotConfig = pivotConfig;
		
		
		
		if(conn==null || !conn.isConnected()) {
			long curMillis = System.currentTimeMillis();
			try {
				if(sc.isKDB()) {
					conn = connectionManager.tryKdbConnection(serverName);
				} else {
					ResultSet crs = connectionManager.executeQuery(sc, sqlSent);
					if(pivotConfig != null) {
						crs = PivotProvider.postProcess(jtype, crs, pivotConfig.getByColsSelected(), pivotConfig.getPivotColsSelected());
					}
					long millisTaken = System.currentTimeMillis() - curMillis;
					qr = QueryResult.successfulResult(sc, title, pivotConfig, null, crs, "", millisTaken);
					sendQRtoListeners(sc, qr);
					return;
				}
			} catch (Throwable e) {
				Exception ee = e instanceof Exception ? ((Exception)e) : new IOException(e);
				qr = QueryResult.exceptionResult(sc, title, pivotConfig, ee, System.currentTimeMillis() - curMillis);
				sendQRtoListeners(sc, qr);
				return;
			}
		}

		Object o = null;
		// evaluate the query itself
		long curMillis = System.currentTimeMillis();
		try	{
			String qry = queryWrapPrefix + sqlSent + queryWrapPostfix;
			
			// wrap call to get console display and protect against large downloads and to add debugging information
			// wrapped call has list format with two possible layout results:
			// Note the (1b;`) is in case runResult is also 1b as that would collapse to a boolean[] rather than an Object[]
			// making parsing in java harder.
			// (sizeOk; (runOk=(1b;`); runResult); consoleText)
			// (sizeOk; (runOk=enlist 0b; errorMessage; stackTrace); consoleText)
			if(queryWrapped) {
				String maxSizeString = maxReturnedObjectSize == 0 ? "0Wj" : (maxReturnedObjectSize + "j");
				String callWrapper = "{v:$[`trp in key .Q; .Q.trp[{( (1b;`) ;value x)};x;{((0b;`);x;$[4<count y; .Q.sbt -4 _ y; \"\"])}]; ((1b;`);value x)]; a:" + maxSizeString + ">@[-22!;v;{0}]; (a;$[a;v;0b];.Q.s v 1)} \"";
				qry = callWrapper + KdbHelper.escape(qry) + "\"";
			}

			if(sc.isKDB()) {
				commercialDBqueries++;
			}
			o = conn.query(qry);
			Object k = null;
			String consoleView = null;

			if(queryWrapped) {
				if(!(o instanceof Object[])) {
					throw new KException("replyformat"); // This connects to KError to suggest unwrapping the query.
				}
				Object[] ret = (Object[]) o;
				boolean sizeOK = (Boolean)ret[0];
				consoleView = new String((char[]) ret[2]);
				if(sizeOK && ret[1] instanceof Object[]) {
					Object[] resObject = (Object[]) ret[1];
					boolean runOk = (Boolean) ((Object[]) resObject[0]) [0];
					if(runOk) {
						k = resObject[1];
					} else {
						String errorTitle = new String((char[])resObject[1]);
						String stackMessage = "";
						if(resObject[2] instanceof char[]) {
							stackMessage = new String((char[])resObject[2]);
							String magicString = "{(1b;value x)}";
							if(stackMessage.contains(magicString)) {
								int p = stackMessage.indexOf(magicString);
								if(p > 0) {
									int q = stackMessage.substring(0, p).lastIndexOf("\n");
									if(q > 0) {
										stackMessage = stackMessage.substring(0, q);
									}
								}
							}
						}
						throw new KException(errorTitle, stackMessage);
					}
				}
			} else {
				k = o;
				consoleView = (k == null ? "" : KdbHelper.asLine(k));
			}
			ResultSet rs = null;
			try {
				if(k != null) {
					rs = jdbc.getRS(k, queryTitle);
				}
			} catch(Exception e) {
				LOG.log(Level.INFO, "No RS possible", e);
			}
			long millisTaken = System.currentTimeMillis() - curMillis;
			qr = QueryResult.successfulResult(sc, title, pivotConfig, k, rs, consoleView, millisTaken);
			
		} catch (KException ex) {
			qr = QueryResult.exceptionResult(sc, title, pivotConfig, ex, System.currentTimeMillis() - curMillis);
		} catch (Exception ex) {
			synchronized (this) {
				if(cancelQuery) {
					qr = QueryResult.cancelledResult(sc, title, pivotConfig);
				} else {
					// if our wrapper failed, server could be customized to send anything
					// e.g. secure server could send text warning.
					LOG.log(Level.SEVERE, "Server sent some unwrapped unknown result", ex);
					String s = o!=null ? KdbHelper.asLine(o) : ex.getMessage();
					qr = QueryResult.exceptionResult(sc, title, pivotConfig, new IOException(s), System.currentTimeMillis() - curMillis);
				}
			}
		}
		
	    // must do as connection must still be "open" for rs to work
		sendQRtoListeners(sc, qr);
		
		try {
			if(!connectionPersisted) {
				conn.close();
				conn = null;
			}
		} catch (IOException ioe) {
			if(!cancelQuery) {
				LOG.warning("error refrshing watches /  closing connection");
			}
		}

	}

	@Override public void close() throws Exception {
		if(conn != null) {
			conn.close();
			conn = null;		
		}
	}
	
	public void sendQRtoListeners(ServerConfig sc, QueryResult qr) {
		LOG.info("queryResultReturned: " + qr);
		for(QueryListener l : listeners) {
			try {
				l.queryResultReturned(sc, qr);
			} catch (Exception ex) {
				LOG.log(Level.SEVERE, "query listener: " + l + " exceptioned: ", ex);
			}
		}
	}

	/**
	 * @return true if the query sent to the server is wrapped to 
	 * get nice console output and protect against large sizes.
	 */
	public boolean isQueryWrapped() {
		return queryWrapped;
	}
	
	/**
	 * Set to true to wrap kdb queries to protect against large sizes and
	 * to get nice console output. 
	 */
	public void setQueryWrapped(boolean queryWrapped) {
		this.queryWrapped = queryWrapped;
	}
	
	/**
	 * @return true if queryManager will attempt to keep the kdb connection
	 * open between queries and reuse it again.
	 */
	public boolean isConnectionPersisted() {
		return connectionPersisted;
	}
	
	public boolean isQuerying() {
		return querying;
	}
	
	/**
	 * Set to true to stay connect to the server and reuse the connection
	 * between queries.
	 */
	public void setConnectionPersisted(boolean connectionPersisted) {
		this.connectionPersisted = connectionPersisted;
	}
	
	/**
	 * Set maximum size of object that queries will return to the client.
	 * @param maxSize Maximum size in bytes, 0 means infinite. 
	 * 		0-infinite will be slightly faster than limited calls.
	 */
	public void setMaxReturnSize(long maxSize) {
		Preconditions.checkArgument(maxSize >= 0);
		this.maxReturnedObjectSize = maxSize;
	}
	
	
	/**
	 * Set the serverName that will decide where queries are sent to.
	 * @param serverName name of a server in {@link ConnectionManager}.
	 */
	public synchronized void setSelectedServerName(final String serverName) {

		boolean changeToNull = serverName == null && this.selectedServerName!=null;
		boolean actualChange = serverName!=null && !serverName.equals(this.selectedServerName);
		if(changeToNull || actualChange) {
			conn = null;
			this.selectedServerName = null;
			if(actualChange) {
				ServerConfig sc = connectionManager.getServer(serverName);
				this.selectedServerName = sc==null ? null : serverName;
			}
			for(QueryListener l : listeners) {
				try {
					l.selectedServerChanged(serverName);
				} catch(Exception e) {
					LOG.severe("Error notifying listener:" + l); // Don't break all listeners if one fails.
				}
			}
		} else {
			LOG.fine("Ignoring setSelectedServerName: " + serverName + "as already set");
		}
	}
	
	/**  @return The selected server name or null if none is selected.  */
	public String getSelectedServerName() {
		return selectedServerName;
	}

	private void refreshServerList() {
		serverNames = connectionManager.getServerNames();
		for(QueryListener l : listeners) {
			l.serverListingChanged(serverNames);
		}
		// keep selection if possible, else move to another if possible.
		if(!serverNames.contains(selectedServerName)) {
			if(serverNames.size()>0) {
				String sname = serverNames.get(serverNames.size()-1);
				for(String s : serverNames) {
					if(connectionManager.isConnected(s)) {
						sname = s;
						break;
					}
				}
				setSelectedServerName(sname);
			} else {
				setSelectedServerName(null);
			}
		}
	}

	public List<String> getServerNames() {
		return serverNames;
	}

	public void setQueryWrapPrefix(String queryWrapPre) {
		queryWrapPrefix = queryWrapPre==null ? "" : queryWrapPre;
	}

	public void setQueryWrapPostfix(String queryWrapPost) {
		queryWrapPostfix = queryWrapPost==null ? "" : queryWrapPost;
	}


	/**
	 * Return a list of commands for connecting to servers. 
	 */
	@Override public Collection<Command> getCommands() {
		return getChangeServerCommands(false);
	}
	
	
	/**
	 * Return a list of commands for connecting to servers. 
	 * @param serverNameOnly If true only server names will be shown otherwise 
	 * the command title will include a longer title
	 */
	public List<Command> getChangeServerCommands(boolean serverNameOnly) {
		List<Command> commands = new ArrayList<Command>();
		for(ServerConfig sc : connectionManager.getServerConnections()) {
			commands.add(new ChangeServerCommand(sc, serverNameOnly));
		}
		return commands;
	}
	
	private class ChangeServerCommand implements Command {

		private final ServerConfig sc;
		private final boolean serverNameOnly;
		
		public ChangeServerCommand(ServerConfig serverConfig, boolean serverNameOnly) { 
			this.sc = serverConfig;
			this.serverNameOnly = serverNameOnly;
		}
		
		@Override public javax.swing.Icon getIcon() { return Theme.CIcon.SERVER.get(); }
		@Override public KeyStroke getKeyStroke() { return null; }
		@Override public String toString() { return getTitle(); };
		@Override public void perform() { setSelectedServerName(sc.getName()); }

		@Override public String getTitle() { 
			if(serverNameOnly) {
				return sc.getName();
			}
			return "Connect To Server: " + sc.getName(); 
		}
		@Override public String getTitleAdditional() { 
			String t = sc.getHost() + ":" + sc.getPort();
			if(!t.equals(sc.getName())) {
				return t;
			}
			return ""; 
		}
		
		@Override public String getDetailHtml() { 
			return "<html>host: " + sc.getHost() + 
					"<br/>post:" + sc.getPort() + "</html>"; 
		}
		
	}

	public int getCommercialDBqueries() { return commercialDBqueries; }

	public boolean hasAnyServers() {
		return !getServerNames().isEmpty();
	}
}
