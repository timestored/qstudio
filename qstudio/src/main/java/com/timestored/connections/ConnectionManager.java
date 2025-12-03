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
package com.timestored.connections;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import static com.google.common.base.MoreObjects.toStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import com.timestored.StringUtils;
import com.timestored.kdb.KdbConnection;
import com.timestored.plugins.ConnectionDetails;
import com.timestored.plugins.DatabaseAuthenticationService;
import com.timestored.plugins.PluginLoader;

import lombok.Getter;
import lombok.Setter;
import net.jcip.annotations.ThreadSafe;

/**
 * Allows managing (adding/removing/updating) server connections. All connections 
 * should be created/accessed using this class. A server is uniquely identified by
 * a name, a server name has a serverConfig detailing JDBC details etc.
 * A preference store can be used to save/restore the list of connections
 * before/after any edits, this is however optional.
 */
@ThreadSafe
public class ConnectionManager implements AutoCloseable {	
	
	public static String XML_ROOT = "serverlist";
	private static final Logger LOG = Logger.getLogger(ConnectionManager.class.getName());

	private static final XStream xstream = new XStream(new StaxDriver());
	
	private final List<ServerConfig> serverConns;
	private final Map<ServerConfig, ObjectPool<PoolableConnection>> serverConnPool;
	private final Map<ServerConfig, Boolean> serverConnected = new ConcurrentHashMap<ServerConfig, Boolean>();
	private final List<ServerConfig> readonlyServerConnections;
	private final CopyOnWriteArrayList<Listener> listeners;
	private final Object LOCK = new Object();
	
	@Getter private String defaultLoginUsername = null;
	@Getter private String defaultLoginPassword = null;

	public void close() {
		// copy map, else inner remove causes concurrentModificaitonException
		Map<ServerConfig, ObjectPool<PoolableConnection>> mapCopy = new HashMap<>(serverConnPool);
		for(ServerConfig sc : mapCopy.keySet()) {
			closePool(sc);
		}
		serverConnPool.clear();
		notifyListeners();
	}

	public void closePool(ServerConfig sc) {
		ObjectPool<PoolableConnection> op = serverConnPool.remove(sc);
		if(op != null) {
			try {
				op.clear();
			} catch (Exception e) {}
			try {
				op.close();
			} catch (Exception e) {}
		}
		serverConnected.put(sc, Boolean.FALSE);
	}
	
	private Preferences preferences;
	private String prefKey;

	static {
		xstream.addPermission(AnyTypePermission.ANY);
		xstream.processAnnotations(ServerConfigDTO.class);
	}
	
	public static ConnectionManager newInstance() {
		return new ConnectionManager();
	}

	private ConnectionManager() {
		
		serverConnPool = new HashMap<ServerConfig, ObjectPool<PoolableConnection>>();
		this.serverConns = new CopyOnWriteArrayList<ServerConfig>();
		readonlyServerConnections = Collections.unmodifiableList(serverConns);
		this.listeners = new CopyOnWriteArrayList<Listener>();
	}

	/**
	 * Set a preference store to use for saving and loading connection details,
	 * remove any current servers and load only those from preferences.
	 * If set before each connection add/remove the store will be reloaded,
	 * modified and then saved. 
	 * @param prefKeyPrefix The prefix of keys that will be used, postfixes may be used to allow
	 *  create multiple keys to store more values.
	 */
	public void setPreferenceStore(Preferences preferences, String prefKeyPrefix) {
		this.preferences = preferences;
		this.prefKey = prefKeyPrefix;
		reloadFromPreferences();
	}
	
	/**
	 * @return The list of server connections at a given point in time,
	 * this list is not guaranteed to be 100% up to date.
	 * Connections returned will be alphabetically sorted on name.
	 */
	public List<ServerConfig> getServerConnections() {
		
		List<ServerConfig> r = new ArrayList<ServerConfig>(readonlyServerConnections);
		Comparator<ServerConfig> alphabetOrder = new Comparator<ServerConfig>() {
			@Override public int compare(ServerConfig sc1, ServerConfig sc2) {
				return sc1.getName().compareTo(sc2.getName());
			}
		};
		Collections.sort(r, alphabetOrder);
		return r;
	}
	
	/**
	 * @return list of names of all servers.
	 */
	public List<String> getServerNames() {
		List<String> s = Lists.newArrayList();
		for(ServerConfig sc : getServerConnections()) {
			s.add(sc.getName());
		}
		return Collections.unmodifiableList(s);
	}
	
	/** add server but do not notify listeners */ 
	private void addServerSilently(ServerConfig serverConnection) {
		Preconditions.checkNotNull(serverConnection);
		synchronized (LOCK) {
			String name = serverConnection.getName();
			ServerConfig existingSC = getServer(name);
			if(existingSC!=null) {
				if(existingSC.equals(serverConnection)) {
					return;
				} else {
					throw new IllegalArgumentException("Server name must be unique. " +
							"Cant use this call to update settings.");
				}
			}
			serverConns.add(serverConnection);
			serverConnected.put(serverConnection, Boolean.FALSE);
			LOG.info("added server: " + serverConnection.toString());
		}
	}
	
	/**
	 * Add a {@link ServerConfig}. 
	 * @param serverConnection the connection you want added, the name of the
	 * server must be unique to this manager.
	 * @throws IllegalArgumentException If name is already present and your trying to change its 
	 * 		settings. Note exception not thrown if all details the same.
	 */
	public void addServer(ServerConfig serverConnection) {
		synchronized (LOCK) {
			reloadFromPreferences();
			addServerSilently(serverConnection);
			save();	
		}
		notifyListenersServerAdded(serverConnection);
	}
	

	/**
	 * Add a list of {@link ServerConfig}s where possible. 
	 * @param connections the connections you want added, server names must be unique to this manager.
	 * @return The list of configs that failed to add, one reason could be duplicate name but
	 * 	different ports, {@link #addServer(ServerConfig)}.
	 */
	public List<ServerConfig> addServer(List<ServerConfig> connections) {

		List<ServerConfig> failedConfigs = new ArrayList<ServerConfig>();
		synchronized (LOCK) {
			reloadFromPreferences();
			Preconditions.checkNotNull(connections);
			for(ServerConfig sc : connections) {
				try {
					addServerSilently(sc);
				} catch(IllegalArgumentException iae) {
					LOG.log(Level.WARNING, "Could not add sc: " + sc.toString(), iae);
					failedConfigs.add(sc);
				}
				
			}
			save();
		}
		connections.forEach(sc -> notifyListenersServerAdded(sc));
		return failedConfigs;
	}
	
	/**
	 * Update a {@link ServerConfig}, serverName must already be present. 
	 * @throws IllegalArgumentException If the oldServerName doesn't exist, 
	 * 	or if the newName is already taken.
	 */
	public void updateServer(String oldServerName, ServerConfig serverConnection) {
		
		String newName = serverConnection.getName();
		if(!newName.equals(oldServerName) && getServer(newName)!=null) {
			throw new IllegalArgumentException("That server name is already taken.");
		}
		LOG.info("updateServer(" + oldServerName + " -> " + serverConnection + ")");
		
		ServerConfig existingSC = null;
		synchronized(LOCK) {
			reloadFromPreferences();
			existingSC = getServer(oldServerName);
			if(existingSC != null) {
				serverConns.remove(existingSC);
				closePool(existingSC);
				statusUpdate(existingSC, false);
				serverConns.add(serverConnection);
				statusUpdate(serverConnection, false);
			}
			save();
		}
		LOG.info("updated server: " + serverConnection.toString());
		notifyListeners();
		if(existingSC == null) {
			throw new IllegalArgumentException("server does not exist already, so can't remove");
		}
	}


	/** Move the server model to a selected folder. */
	public void moveServer(ServerConfig serverConfig, String folderName) {
		Preconditions.checkNotNull(serverConfig);
		LOG.info("moveServer(" + serverConfig.getName() + " to " + folderName + ")");
		String f = folderName==null ? "" : folderName;
		if(!serverConfig.getFolder().equals(f)) {
			ServerConfig sc = new ServerConfigBuilder(serverConfig).setFolder(folderName).build();
			updateServer(serverConfig.getName(), sc);
		}
	}
	

	/**
	 * Remove the selected server.
	 * @return true if the {@link ServerConfig} was removed, otherwise false.
	 */
	public boolean removeServer(String name) {
		ServerConfig sc = getServer(name);
		return sc!=null ? removeServer(sc) : false;
	}
	

	/**
	 * Remove the selected {@link ServerConfig} s.
	 */
	private boolean[] removeServers(List<ServerConfig> serverConfigs) {
		if(serverConfigs.size()>0) {
			boolean[] goners = new boolean[serverConfigs.size()];

			synchronized (LOCK) {
				reloadFromPreferences();
				for(int i=0; i<serverConfigs.size(); i++) {
					ServerConfig sc = serverConfigs.get(i);
					ObjectPool<PoolableConnection> objectPool = serverConnPool.remove(sc);
					if(objectPool != null) {
						try { objectPool.clear(); } catch (Exception e) {}
						// For some reason if I close() then reopen a duckdb file, it still finds the objectpool closed even though it was removed.
						// So for now I'm just not closing. I tested and clear() seems to close duckdb file so it's deletable etc. i.e. It works.
//						try { objectPool.close(); } catch (Exception e) {}
					}
					serverConnected.computeIfPresent(sc, (currentSC,present) -> false);
					
					goners[i] = serverConns.remove(sc);
					closePool(sc);
					if(goners[i]) {
						LOG.info("removed server: " + serverConfigs.toString());
					}
				}
				save();
			}
			notifyListeners();
			return goners;
		}
		return new boolean[0];
	}
	/**
	 * Remove the selected {@link ServerConfig}.
	 * @return true if the {@link ServerConfig} was removed, otherwise false.
	 */
	public boolean removeServer(ServerConfig serverConfig) {
		return removeServers(Lists.newArrayList(serverConfig))[0];
	}

	/** Remove all servers. */
	public void removeServers() {
		synchronized (LOCK) {
			serverConns.clear();
			save();
			LOG.info("removed all servers");
		}
		notifyListeners();
	}
	
	
	/**
	 * @return Connection for this serverName, or null no such server exists.
	 * @param serverName serverName uniquely identifying a given {@link ServerConfig}.
	 * @throws IOException if problem connecting to server
	 */
	public Connection getConnection(String serverName) throws IOException  {
		ServerConfig sc = getServer(serverName);
		if(sc != null) {
			return getConnection( getServer(serverName));
		}
		return null;
	}

	public boolean isConnected(String serverName) {
		return isConnected(getServer(serverName));
	}

	/**
	 * @return Connection for this serverName, or null no such server exists.
	 * @throws IOException if problem connecting to server
	 */
	public PoolableConnection getConnection(ServerConfig serverConfig) throws IOException  {
		synchronized (LOCK) {
			if(!serverConns.contains(serverConfig)) {
				return null;
			} 
		}
		return getConn(serverConfig);
	}

	/** Return the connection to the pool */
	public boolean returnConn(ServerConfig serverConfig, PoolableConnection conn, boolean invalidateConnection)  {
		ObjectPool<PoolableConnection> sp = serverConnPool.get(serverConfig);
		if(sp!=null && conn!=null) {
			try {
				boolean isClosedOrErrored = true;
				try {
					isClosedOrErrored = conn.isClosed();
				} catch(Exception e) {}
				if(isClosedOrErrored || invalidateConnection) {
					sp.invalidateObject(conn);
				} else {
					sp.returnObject(conn);
				}
				return true;
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "error returning object to pool", e);
			}
		}
		return false;
	}
	/** get a connection but don't care if we know about it or not */
	private PoolableConnection getConn(ServerConfig serverConfig) throws IOException {
		try {
			ObjectPool<PoolableConnection> connPool = null;
			synchronized (LOCK) {
				connPool = serverConnPool.get(serverConfig);
				if(connPool == null) {
					ServerConfig sc = overrideServerConfig(serverConfig);
					ConnectionFactory connectionFactory = new MyDriverManagerConnectionFactory(sc);
					// I think this may be needed, to pool connections
					@SuppressWarnings("unused")
					PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
					connPool = new GenericObjectPool<>(poolableConnectionFactory);
					  
					serverConnPool.put(serverConfig, connPool);
				}
			}
			
			PoolableConnection c = connPool.borrowObject();
			if(c.isClosed()) {
				connPool.invalidateObject(c);
				c = null;
			} else {
				statusUpdate(serverConfig, true);
			}
			
			return  c;
		} catch ( Exception e) {
			if(serverConnected.containsKey(serverConfig)) {
				statusUpdate(serverConfig, false);
			}
			LOG.info("getConn Exception server: " + serverConfig.toString());
			throw new IOException(e);
		}
	}

	private ServerConfig overrideServerConfig(ServerConfig serverConfig) {
		ServerConfig sc = serverConfig;
		
		// if no login details assigned use default username / password
		if(!serverConfig.hasLogin() && (this.defaultLoginPassword!=null || this.defaultLoginUsername!=null)) {
			sc = new ServerConfigBuilder(serverConfig).setUsername(defaultLoginUsername).setPassword(defaultLoginPassword).build();
		}
		
		// If this JDBC type has custom authenticator use it
		DatabaseAuthenticationService dps = serverConfig.getJdbcType().getAuthenticator();
		if(dps != null) {
			ConnectionDetails connDetails = dps.getonConnectionDetails(sc.getConnectionDetails());
			sc = new ServerConfigBuilder(serverConfig)
					.setHost(connDetails.getHost())
					.setPort(connDetails.getPort())
					.setDatabase(connDetails.getDatabase())
					.setUsername(connDetails.getUsername())
					.setPassword(connDetails.getPassword()).build();
		}
		
		return sc;
	}

	/**
	 * @return Server associated with the serverName, or null if not found.
	 * @param serverName serverName uniquely identifying a given {@link ServerConfig}.
	 */
	public ServerConfig getServer(String serverName) {
		Preconditions.checkNotNull(serverName);

		synchronized(LOCK) {
			for(ServerConfig sc : serverConns) {
				if(sc.getName().equals(serverName)) {
					return sc;
				}
			}
		}
		return null;
	}
	
	
	private void notifyListeners() {
		for(Listener l : listeners) {
			try {
				l.prefChange();
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "problem notifying listener.", e);
			}
		}
	}

	private void notifyListenersServerAdded(ServerConfig sc) {
		for(Listener l : listeners) {
			try {
				l.serverAdded(sc);
				l.prefChange();
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "problem notifying listener.", e);
			}
		}
	}
	
	/**
	 * Interface that allows other classes to listen for changes in preferences.
	 */
	public static interface Listener {
		/** A preference has changed, servers may have been added/removed etc. **/
		public void prefChange();
		public void serverAdded(ServerConfig sc);
		/** The connection status of a single server has changed **/
		public void statusChange(ServerConfig serverConfig, boolean connected);
	}

	public static abstract class Adapter implements Listener {
		public void prefChange() {}
		public void serverAdded(ServerConfig sc) {}
		public void statusChange(ServerConfig serverConfig, boolean connected) {}
	}

	/**
	 * Add the selected listener.
	 */
	public void addListener(Listener prefListener) {
		listeners.add(prefListener);
	}
	
	/**
	 * Remove the selected listener.
	 * @return true if it was removed.
	 */
	public boolean removeListener(Listener prefListener) {
		return listeners.remove(prefListener);
	}

	@Override
	public String toString() {
		return toStringHelper(this)
			.add("serverConns", serverConns)
			.add("listeners", listeners)
			.toString();
	}
	

	/**
	 * Test if the supplied serverConfig is a server that can actually be connected to.
	 * @throws IOException if can't connect
	 */
	public void testConnection(ServerConfig serverConfig) throws IOException {

		boolean connected = false;

		PoolableConnection conn = getConn(serverConfig);
		try {
			connected = !conn.isClosed();
		} catch (SQLException e) {
			connected = false;
		} finally {
			returnConn(serverConfig, conn, !connected);
		}
		
		if(serverConns.contains(serverConfig)) {
			statusUpdate(serverConfig, connected);
		}

		if(!connected) {
			throw new IOException();
		}
	}


	/**
	 * @return kdbConnection for selected serverName else throw an Exception
	 */
	public KdbConnection tryKdbConnection(String serverName) throws Exception {
		ServerConfig serverConfig = getServer(serverName);
		if(serverConfig == null) {
			throw new IllegalStateException("ConnectionManager cant find server named: " + serverName);
		}
		return tryKdbConnection(serverConfig);
	}
	
	/**
	 * @return kdbConnection for selected {@link ServerConfig} else throw an Exception
	 */
	private KdbConnection tryKdbConnection(ServerConfig serverConfig) throws Exception {
		if(serverConfig.isKDB()) {
			try {
				KdbConnection kdbConn = new KdbConnection(overrideServerConfig(serverConfig));
				statusUpdate(serverConfig, true);
				return kdbConn;
			} catch (Exception e) {
				statusUpdate(serverConfig, false);
				String text = "Could not connect to server: " + serverConfig.getHost() + ":" + serverConfig.getPort() 
					+ "\r\n Exception: " + e.toString();
				throw new IOException(text);
			}
		}
		throw new IllegalStateException("tryKdbConnection only works for kdb");
	}

	/**
	 * @return a KDbConnection if possible otherwise null.
	 */
	public KdbConnection getKdbConnection(ServerConfig serverConfig) {
		try {
			return tryKdbConnection(serverConfig);
		} catch (Exception e) { 
			return null;
		}
	}
	
	private void statusUpdate(ServerConfig serverConfig, boolean connected) {
		
		Boolean prevVal = serverConnected.put(serverConfig, connected);
		boolean change = prevVal==null || !prevVal.equals(connected);
		if(change) {
			LOG.info(serverConfig.getName() + " Connected = " + connected);
			for(Listener l : listeners) {
				l.statusChange(serverConfig, connected);
			}
		}
	}
	
	/**
	 * @return a KDbConnection if possible otherwise null. If null is returned
	 * this may either be because the server is not known or that are problems
	 * connecting to it. Try checking it's connection status. 
	 * Note: This method may freeze up if server does not exist or is in debug mode, do not call
	 * it within the GUI Thread.
	 */
	public KdbConnection getKdbConnection(String serverName) {
		ServerConfig sc = getServer(serverName);
		return sc != null ? getKdbConnection(sc) : null;
	}

	public boolean isConnected(ServerConfig sc) {
		if(sc != null) {
			Boolean b = serverConnected.get(sc);
			return b!=null ? b : false;
		}
		return false;
	}

	public boolean contains(ServerConfig serverConfig) {
		synchronized (LOCK) {
			return serverConns.contains(serverConfig);
		}
	}

	public boolean containsKdbServer() {
		synchronized (LOCK) {
			return serverConns.stream().anyMatch(sc -> sc.isKDB());
		}
	}

	/**
	 * @return true if there are no connections defined, otherwise false.
	 */
	public boolean isEmpty() {
		synchronized (LOCK) {
			return serverConns.size()==0;
		}
	}

	/**
	 * Reload connectiong from stored preferences.
	 */
	public void refreshFromPreferences() {
		if(reloadFromPreferences()) {
			notifyListeners();
		}
	}
	
	/** 
	 * Set the default username / password to be used, if a server doesn't already have one 
	 **/
	public void setDefaultLogin(String username, String password) {
		if(!StringUtils.equals(defaultLoginUsername, username) || 
				!StringUtils.equals(defaultLoginPassword, password)) {
			this.defaultLoginUsername = username;
			this.defaultLoginPassword = password;
			// must clear to prevent bad cached conns being returned
			close(); 
			notifyListeners();
		}
	}

	public boolean isDefaultLoginSet() {
		return defaultLoginUsername!=null || defaultLoginPassword!=null;
	}	
	
	private static final int MAX_STORAGE_SLOTS = 20;
	

	public static void wipePreferences(Preferences preferences, String prefKeyPrefix) {
		for(int i=0; i<MAX_STORAGE_SLOTS; i++) {
			preferences.remove(prefKeyPrefix + i);
		}
	}
	
	/**
	 * If a preference store is set, try to get the existing connections stored there.
	 * If existing servers are saved and different than current, use what was saved.
	 * @return true if there was an actual change
	 */
	private boolean reloadFromPreferences() {
		if(preferences != null) {
			synchronized (LOCK) {
				StringBuilder sb = new StringBuilder(preferences.get(prefKey, ""));
				for(int i=0; i<MAX_STORAGE_SLOTS; i++) {
					sb.append(preferences.get(prefKey + i, ""));
				}
				String txt = sb.toString();
				
				try {
			        txt = PreferenceHelper.decode(txt);
					
					List<ServerConfig> sConns = getConnectionsFromXml(txt);
					if(!sConns.equals(serverConns)) {
						LOG.warning("stored conns and current conns disagreed, using stored values");
						LOG.warning("serverConns = " + serverConns.toString());
						LOG.warning("sConns = " + sConns.toString());
						serverConns.clear();
						serverConns.addAll(sConns);
						return true;
					}
				} catch (IOException e) {
					LOG.log(Level.SEVERE, "Could not decrypt connection details txt = " + txt, e);
				}
			}
		}
		return false;
	}

	
	private void save() {
		if(preferences != null) {
			synchronized (LOCK) {
					String txt = getConnectionsXml(serverConns);
				txt = PreferenceHelper.encode(txt);
				if(txt.length() > Preferences.MAX_VALUE_LENGTH * (MAX_STORAGE_SLOTS - 1)) {
					LOG.info("txt.length = " + txt.length() + " maxLength = " + Preferences.MAX_VALUE_LENGTH * 9);
					throw new IllegalArgumentException("Too many connections to save");
				}
				
				
				if(txt.length() <= Preferences.MAX_VALUE_LENGTH) {
					preferences.put(prefKey, txt);
				} else {
					preferences.put(prefKey, txt.substring(0, Preferences.MAX_VALUE_LENGTH));
					txt.substring(Preferences.MAX_VALUE_LENGTH);
					
					for(int i=0; i<MAX_STORAGE_SLOTS; i++) {
						int stPos = (i+1)*Preferences.MAX_VALUE_LENGTH;
						int endPos = Math.min((i+2)*Preferences.MAX_VALUE_LENGTH, txt.length());
						if(stPos < txt.length()) {
							preferences.put(prefKey + i, txt.substring(stPos, endPos));
						} else {
							preferences.put(prefKey + i, "");
						}
					}
				}
			}
		}
	}
	

	public static String getConnectionsXml(List<ServerConfig> serverConns, boolean removeLogins) {
		synchronized (xstream) {
			ArrayList<ServerConfigDTO> l = new ArrayList<ServerConfigDTO>(serverConns.size());
			for(ServerConfig sc : serverConns) {
				l.add(new ServerConfigDTO(sc, removeLogins));
			}
			return  xstream.toXML(l).replaceAll("list>", XML_ROOT + ">");
		}
	}

	/**
	 * @return the list of connections as an xml string. 
	 * 			Outermost tag will be  &lt;serverlist&gt;
	 */
	public static String getConnectionsXml(List<ServerConfig> serverConns) {
		return getConnectionsXml(serverConns, false);
	}

	public String getConnectionsXml() {
		return getConnectionsXml(serverConns);
	}
	
	/**
	 * @throws IOException If it cannot convert the XML to a valid list of servers.
	 */
	@SuppressWarnings("unchecked")
	public static List<ServerConfig> getConnectionsFromXml(String serverListXml) throws IOException {

		try {
			if(serverListXml!=null && serverListXml.length()>0) {
				String s = serverListXml.replaceAll(XML_ROOT + ">", "list>");
				List<ServerConfig> r = new ArrayList<ServerConfig>();
				synchronized (xstream) {
					ArrayList<ServerConfigDTO> a = (ArrayList<ServerConfigDTO>) xstream.fromXML(s);	
					for(ServerConfigDTO scDTO : a) {
						r.add(scDTO.getInstance());
					}
				}
				return r;
			}
		} catch (Exception e) {
			String msg = "Could not convert serverListXml = " + serverListXml;
			LOG.log(Level.SEVERE, msg, e);
			throw new IOException(msg, e);
		}
		return Collections.emptyList();
	}

	/**
	 * @return a list of all folders that exist. Note if all connections
	 * 	are in named folders, the root folder "" will not be in the set.
	 */
	public Set<String> getFolders() {
		Set<String> r = Collections.emptySet();
		if(!serverConns.isEmpty()) {
			r = Sets.newHashSet();
			for(ServerConfig  sc : serverConns) {
				r.add(sc.getFolder());
			}			
		}
		return r;
	}

	/**
	 * Attempt to send an sql query to a selected {@link ServerConfig}.
	 * All exceptions are logged but swallowed.
	 * @return true if the query was sent ok, otherwise false
	 */
	public boolean execute(ServerConfig serverConfig, String sql) {
		boolean executionSucceeded = false;
		PoolableConnection conn = null;
		try {
			conn = getConnection(serverConfig);
			if(conn == null) {
				throw new IOException("cant find server");
			}
			executionSucceeded = execute(sql, conn);
			returnConn(serverConfig, conn, !executionSucceeded);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "error getting connection:\r\n", e);
		} finally {
			returnConn(serverConfig, conn, true);
		}
		
		return executionSucceeded;
	}

	/**
	 * Attempt to send sql queries to a selected {@link ServerConfig}.
	 * All exceptions are logged but swallowed.
	 * @return true if all queries were sent ok, otherwise false
	 */
	public boolean execute(ServerConfig sc, List<String> sqlQueries) {
		boolean success = true;
		for(String sql : sqlQueries) {
			success = success && execute(sc, sql);
		}
		return success;
	}

	/**
	 * Attempt to send an sql query to a selected {@link ServerConfig}.
	 * All exceptions are logged but swallowed.
	 * @return true if the query was sent ok, otherwise false
	 */
	private static boolean execute(String sql, Connection conn) {
		Statement st = null;
		try {
			st = conn.createStatement();
			st.execute(sql);
			return true;
		} catch(SQLException sqe) {
			LOG.log(Level.WARNING, "error running sql:\r\n" + sql, sqe);
		} finally {
			try {
				if(st != null) { st.close();}
			} catch (SQLException e) {}
		}
		return false;
	}

	/**
	 * Attempt to query a selected {@link ServerConfig} and return a cached result.
	 * @return The query result if all successful otherwise null.
	 * @throws SQLException If there was a problem with the sql.
	 * @throws IOException If there was a problem with the connection.
	 */
	public CachedRowSet executeQuery(ServerConfig serverConfig, String sql) throws SQLException, IOException {
		return useConn(serverConfig, (Connection conn) -> executeQuery(serverConfig, sql, conn));
	}
	
	/**
	 * Carefully wrap a call to a Connection so that at all costs the connection is closed.
	 * Previously qStudio had bugs in fetching meta, querying that caused connections NOT to be returned to the pool.
	 * This means the pool runs out and the UI freezes.
	 * @return The query result if all successful otherwise null.
	 * @throws SQLException If there was a problem with the sql.
	 * @throws IOException If there was a problem with the connection.
	 */
	<T> T useConn(ServerConfig serverConfig, CheckedFunction<Connection,T> f) throws IOException,SQLException {
		PoolableConnection conn = getConnection(serverConfig);
		if(conn == null) {
			throw new IOException("Could not find server");
		}
		boolean kdbConnectionClosed = false;
		try {
			return f.apply(conn);
		} catch(SQLException sqe) {
			kdbConnectionClosed = serverConfig.getJdbcType().equals(JdbcTypes.KDB) && 
					(sqe instanceof SQLException) && sqe.toString().contains("recv failed") || sqe.toString().contains("SOCKETERR");
			// basically one level recursion retry as we know recv is remote handle closed. Some KDB systems close handles open for long periods.
			if(kdbConnectionClosed) {
				PoolableConnection connInner = getConnection(serverConfig);
				try {
					return f.apply(connInner);
				} catch(SQLException sqeInner) {
					throw sqeInner;
				} finally {
					returnConn(serverConfig, connInner, false);
				}				
			}
			throw sqe;
		} catch(Exception e) { // What to do if JDBC driver breaks? e.g. bug through NPE. BEst we can do is close it so it will retry?
			kdbConnectionClosed = true;
			throw e;
		} finally {
			returnConn(serverConfig, conn, kdbConnectionClosed);
		}
	}	

	/**
	 * Execute query and return cachedRowset. Uses serverconfig to adapt for kdb queries. 
	 * @return The query result if all successful otherwise null.
	 * @throws SQLException If there was a problem with the sql.
	 */
	private static CachedRowSet executeQuery(ServerConfig serverConfig, String sql, Connection conn) throws SQLException {
		
		Statement st = null;
		try {
			st = conn.createStatement();
			boolean hasRS = st.execute(sql);
			ResultSet rs = null;
			int statementCount = 0;
			int updateCount = 0;
		    CachedRowSet crs = null;
			do {
				ResultSet tempRs = st.getResultSet();
				if(tempRs != null) {
					rs = tempRs;
				    if(rs != null) {
				    	crs = RowSetProvider.newFactory().createCachedRowSet();
				    	crs.populate(rs);
				    }
				}
				updateCount += tempRs == null ? 0 : st.getUpdateCount();
				statementCount++;
			} while((st.getMoreResults() || st.getUpdateCount() != -1) && statementCount < 200_000);
			if(statementCount > 200_000) {
				LOG.warning("Possible error with JDBC driver. BReaking out of query loop as > 200_000 queries?!");
			}
			return crs;
		} catch(SQLException sqe) {
			LOG.warning("Error running sql:\r\n" + sql);
			throw sqe;
		} finally {
			try {
				if(st != null) { st.close();}
			} catch (SQLException e) {}
		}
	}
	
	
	/**
	 * Remove all connections in a given folder.
	 * @return The number of connections removed.
	 */
	public int removeFolder(String folder) {
		List<ServerConfig> removedServers = getServersInFolder(folder);
		boolean[] r = removeServers(removedServers);
		int c = 0;
		for(boolean b : r) {
			if(b) { c++; }
		}
		return c;
	}

	/**
	 * Rename a folder, if the target folder already exists the folders will be merged.
	 * @param from The folder name to move servers from
	 * @param to The folder name to move servers to
	 * @return The number of {@link ServerConfig}s actually moved. THis may be 0 if folder did not exist.
	 */
	public int renameFolder(String from, String to) {
		Preconditions.checkNotNull(from);
		Preconditions.checkNotNull(to);
		to = ServerConfig.cleanFolderName(to);
		from = ServerConfig.cleanFolderName(from);
		
		LOG.info("renameFolder(" + from + " -> " + to + ")");

		synchronized(LOCK) {
			reloadFromPreferences();
			Collection<ServerConfig> fromSCs = getServersInFolder(from);
			if(fromSCs.isEmpty()) {
				return 0;
			}
			for(ServerConfig existingSC : fromSCs) {
				serverConns.remove(existingSC);
				statusUpdate(existingSC, false);
				String newFolder = to + existingSC.getFolder().substring(from.length());
				ServerConfig sc = new ServerConfigBuilder(existingSC).setFolder(newFolder).build();
				serverConns.add(sc);
				statusUpdate(sc, false);
			}
			save();
			notifyListeners();
			return fromSCs.size();
		}
	}

	/** @return The servers that are in a given folder including subfolders.	 */
	public List<ServerConfig> getServersInFolder(String folder) {
		Preconditions.checkNotNull(folder);
		String fn = ServerConfig.cleanFolderName(folder);
		List<ServerConfig> r = null;
		for(ServerConfig sc : serverConns) {
			if(sc.getFolder().startsWith(fn)) {
				if(r == null) {
					r = new ArrayList<ServerConfig>();
				}
				r.add(sc);
			}
		}
		return r==null ? Collections.<ServerConfig>emptyList() : r;
	}

	/**
	 * @return true if the first server config connection works.
	 */
	public boolean doesLoginWork() {
		for(ServerConfig sc : serverConns) {
			try {
				testConnection(sc);
			} catch (IOException e) {
				return false;
			}
			return true;
		}
		return true;
	}


	/** This is only exposed to allow testing **/

	public static final String APP_TITLE = "QStudio";
	@Setter private static String appname = APP_TITLE;

	private static class MyDriverManagerConnectionFactory implements ConnectionFactory {
		private ServerConfig sc;
		public MyDriverManagerConnectionFactory(ServerConfig sc) {
			this.sc = sc;
		}
		@Override public Connection createConnection() throws SQLException {
			try {
				String driverName = sc.getJdbcType().getDriver();
				Class<?> driver = PluginLoader.getCClass(appname, driverName);
				Properties p = new Properties();
				p.setProperty("user", sc.getUsername());
				p.setProperty("password", sc.getPassword());
				return ((Driver) driver.newInstance()).connect(sc.getUrl(), p);
			} catch (InstantiationException | IllegalAccessException | SQLException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new SQLException(e);
			}
		}
		
	}

	@FunctionalInterface
	public interface CheckedFunction<T, R> {
	   R apply(T t) throws SQLException;
	}

}

