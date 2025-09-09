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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.MoreObjects;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ServerConfig;
import com.timestored.kdb.KdbConnection;

/**
 * Represents a single KDB server, allows accessing its {@link ServerObjectTree},
 * {@link ServerReport}, etc. and can be subscribed to, to receive notification
 * of changes / disconnects.
 */
public class ServerModel {

	private static final Logger LOG = Logger.getLogger(ServerModel.class.getName());
	private ServerConfig serverConfig;
	
	private ServerObjectTree serverObjectTree;
	private ServerReport serverReport;
	private ServerSlashConfig serverSlashConfig;
	private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
	private final ConnectionManager connectionManager;
	
	public ServerModel(ConnectionManager connectionManager, ServerConfig serverConfig) {
		this.connectionManager = connectionManager;
		this.serverConfig = serverConfig;
	}

	public static interface Listener {
		public void changeOccurred();
	}

	void refresh() {
		
		serverObjectTree = new ServerObjectTree(connectionManager, serverConfig);
		
		KdbConnection kdbConnection = connectionManager.getKdbConnection(serverConfig);
		if(kdbConnection!= null) {
			
			try {
				try {
					serverReport = new ServerReport(kdbConnection);
					serverSlashConfig = new ServerSlashConfig(kdbConnection);
				} catch(Exception e) {
					LOG.log(Level.WARNING, "Error retrieving Server Properties.", e);
				}
			} finally {
				try {
					kdbConnection.close();
				} catch (IOException e) {
					LOG.log(Level.WARNING, "problem closing kdb connection", e);
				}
			}
			for(Listener l : listeners) {
				l.changeOccurred();
			}
		} 
	}
	
	public KdbConnection getConnection() {
		return connectionManager.getKdbConnection(serverConfig);
	}
	

	/**  @return The last tree obtained from the server otherwise null.  */
	public ServerObjectTree getServerObjectTree() {
		return serverObjectTree;
	}

	/**  @return The last report obtained from the server otherwise null.  */
	public ServerReport getServerReport() {
		return serverReport;
	}


	/**  @return The last config obtained from the server otherwise null.  */
	public ServerSlashConfig getSlashConfig() {
		return serverSlashConfig;
	}


	public ServerConfig getServerConfig() {
		return serverConfig;
	}
	
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public boolean isConnected() {
		return connectionManager.isConnected(serverConfig);
	}
	
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	/** Get the server identifier */
	public String getName() {
		return serverConfig.getName();
	}


	/**
	 * Update the ServerConfig for this ServerModel. This allows preserving the ServerModel 
	 * instance (and any associated permissions) when connection details change.
	 * @param newServerConfig The updated server configuration
	 */
	public void updateServerConfig(ServerConfig newServerConfig) {
		if (!this.serverConfig.getName().equals(newServerConfig.getName())) {
			throw new IllegalArgumentException("Cannot change server name via updateServerConfig");
		}
		this.serverConfig = newServerConfig;
		
		// Clear cached objects that may depend on the old config
		this.serverObjectTree = null;
		this.serverReport = null;
		this.serverSlashConfig = null;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("serverConfig", serverConfig)
			.toString();
	}

	
	
}
