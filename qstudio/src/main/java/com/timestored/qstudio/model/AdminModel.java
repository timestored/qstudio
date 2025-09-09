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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ServerConfig;
import com.timestored.kdb.KdbConnection;
import com.timestored.qdoc.DocSource;
import com.timestored.qdoc.DocumentedEntity;
import com.timestored.qstudio.BackgroundExecutor;

import lombok.Getter;

/**
 * Represents all the KDB servers listed in a given {@link ConnectionManager},
 * Allows browsing their object trees, querying them  and selecting servers/items.
 */
public class AdminModel implements ConnectionManager.Listener,DocSource {
	
	private static final Logger LOG = Logger.getLogger(AdminModel.class.getName());

	@Getter private final ConnectionManager connectionManager;
	private final QueryManager queryManager;
	private final CopyOnWriteArrayList<Listener> listeners;

	/** A cache that holds ServerModels by name, old models are never removed **/
	private Map<String, ServerModel> serverContainer = new HashMap<String, ServerModel>();
	
	private List<ServerModel> serverModels  = new CopyOnWriteArrayList<ServerModel>();
	
	private String selectedNamespace;
	private Category selectedCategory = Category.UNSELECTED;
	private ServerQEntity selectedElement;
	private String selectedServerName;

	protected boolean refreshing;


	public enum Category { TABLES,VIEWS,FUNCTIONS,VARIABLES,ELEMENT, NAMESPACE, UNSELECTED };
	
	/**
	 * Implement this to allow listener for changes in the servers model or
	 * in the selected items.
	 */
	public interface Listener {
		public void modelChanged();
		/** A single ServerModel has changed, not the name, just the contents/configuration **/
		public void modelChanged(ServerModel sm);
		public void selectionChanged(ServerModel serverModel, Category category,
				String namespace, QEntity element);
	}
	
	/**
	 * Constructs our model but will not actually try to connect and refresh 
	 * the model unless {@link #refresh()} is called.
	 */
	public AdminModel(ConnectionManager connectionManager, final QueryManager queryManager) {
		
		this.connectionManager = connectionManager;
		this.queryManager = queryManager;
		connectionManager.addListener(this);
		
		queryManager.addQueryListener(new QueryAdapter() {
			@Override public void selectedServerChanged(String server) {

				String qname = queryManager.getSelectedServerName();
				String aname = getSelectedServerName();
				if(qname!=aname) {
					setSelection(qname, null, Category.UNSELECTED, null);
				}
			}
			
			@Override public void serverListingChanged(List<String> serverNames) {
				// Adding immediately to prevent cache miss later.
				serverNames.forEach(sn -> serverContainer.put(sn, new ServerModel(connectionManager, connectionManager.getServer(sn))));
				BackgroundExecutor.EXECUTOR.execute(new Runnable() {
					@Override public void run() {
						refresh();	
					}
				});
			}
		});
		selectedServerName = queryManager.getSelectedServerName();
		listeners = new CopyOnWriteArrayList<Listener>();
	}

	/**
	 * @return list of trees for all KDB servers
	 */
	public List<ServerModel> getServerModels() {
		return Collections.unmodifiableList(serverModels);
	}
	
	
	public void setSelectedServerName(String serverName) {
		queryManager.setSelectedServerName(serverName);
	}
	
	/**
	 * @return {@link ServerObjectTree} for given server name or null if server not found.
	 */
	public ServerObjectTree getServerTree(String serverName) {
		if(serverName != null) {
			ServerModel sm = serverContainer.get(serverName);
			if(sm != null) {
				return sm.getServerObjectTree();	
			}
		}
		return null;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	/**
	 * Query every server to refresh their connection status's and object trees.
	 */
	public synchronized void refresh() {
			refresh(true);
	}

	public void refresh(final ServerConfig sconf) {
		
		if(!refreshing) {
			BackgroundExecutor.EXECUTOR.execute(new Runnable() {
				@Override public void run() {
					synchronized (AdminModel.this) {
						AdminModel.this.refreshing = true;
						LOG.info("refreshing " + sconf.getName());
						ServerModel sm = null;
						for(int i=0; i<serverModels.size(); i++) {
							if(sconf.getName().equals(serverModels.get(i).getServerConfig().getName())) {
								sm = serverModels.get(i);
								sm.updateServerConfig(sconf);
								sm.refresh();
								serverContainer.put(sconf.getName(), sm);
								// Note: we don't replace the ServerModel in the list, just update its config
		
								for(Listener l : listeners) {
									l.modelChanged(sm);
								}
								
								break;
							}
						}
						AdminModel.this.refreshing = false;
					}
				}
			});

					
		}
		
	}
	
	
	/**
	 * @param hardRefresh if true every server will be requeried to get state, otherwise
	 * 	cached state used.
	 */
	private synchronized void refresh(boolean hardRefresh) {

		LOG.info("AdminModel " + (hardRefresh ? "hard " : "") + "Refresh");
		connectionManager.refreshFromPreferences();
		
		List<ServerModel> modelsNeedingRefreshed = new ArrayList<ServerModel>();

		final Map<String, ServerModel> sContainer = new HashMap<String, ServerModel>();
		final List<ServerModel> sTrees  = new CopyOnWriteArrayList<ServerModel>();
		
		for(ServerConfig sconf : connectionManager.getServerConnections()) {
			ServerModel sm = null;
			
			// if allowed to use cache, try to get previous.
			ServerModel cacheSM = serverContainer.get(sconf.getName());
			if(!hardRefresh && cacheSM!=null && cacheSM.getServerConfig().getName().equals(sconf.getName())) {
				// Reuse the existing ServerModel but update its ServerConfig to preserve permissions
				sm = cacheSM;
				sm.updateServerConfig(sconf);
			} else {
				sm = new ServerModel(connectionManager, sconf);
//				modelsNeedingRefreshed.add(sm);
			}
			
			sContainer.put(sconf.getName(), sm);
			sTrees.add(sm);
		}	

		serverModels = sTrees;
		serverContainer = sContainer;
		// refresh if server newly selected
		String sn = queryManager.getSelectedServerName();
		if(hardRefresh || (sn!=null && !sn.equals(selectedServerName))) {
			ServerModel sm = getServerModel(sn);
			if(sm != null) {
				modelsNeedingRefreshed.add(sm);
			}
		}
		selectedServerName = sn;
		
		// notify listeners that overall server lsiting ahs been updated
		for(Listener l : listeners) {
			l.modelChanged();
		}
		
		// refresh then notify listeners again to show server trees updated
		// done separately as refresh can take a lot of time
		for(ServerModel sm : modelsNeedingRefreshed) {
			sm.refresh();
		}
		for(Listener l : listeners) {
			l.modelChanged();
		}
		
	}
	

	/**
	 * Allows hierarchical selection of serverName -> namespace -> category ->element,
	 * The above level must be specified to allow selecting sub-levels.
	 */
	private void setSelection(String servername, String namespace, Category category, 
			ServerQEntity element) {

		ServerModel serverModel = null;
		// check the parameters and throw exception if invalid
		if(servername != null) {
			serverModel = serverContainer.get(servername);
			if(serverModel == null) {
				throw new IllegalArgumentException("server:" + servername + " not found, try refresh");
			}
			
			ServerObjectTree sTree =  serverModel.getServerObjectTree();
			if(namespace != null) {
				// how did they select namespace unless thay had model, force a refresh nad wait on it!
				if(sTree == null) {
					serverModel.refresh();
					sTree = serverModel.getServerObjectTree();
				}
				
				
				if(!sTree.namespaceExists(namespace)) {
					throw new IllegalArgumentException("server:" + servername 
							+ " namespace not found:" + namespace);
				}

				if(category != null) {
					if(category.equals(Category.ELEMENT) && !sTree.elementExists(namespace, element)) {
						throw new IllegalArgumentException("server:" + servername 
								+ " element not found:" + namespace);
					}
				}
			} 
		}
		
		// no sserver selected clear all
		if(servername!=null && !servername.equals(this.selectedServerName)) {
			ServerConfig sc = connectionManager.getServer(servername);
			if(sc != null) {
				refresh(sc);
			}
		}
		

		this.selectedServerName = servername;
		this.selectedNamespace = namespace;
		this.selectedElement = element;
		this.selectedCategory = category;
			
		for(Listener l : listeners) {
			l.selectionChanged(serverModel,	selectedCategory, selectedNamespace, selectedElement);
		}
		if(selectedElement instanceof TableSQE) {
			TableSQE te = (TableSQE) selectedElement;
			// Slightly unsafe assumption that if count is <1000 we can render the limited select and allow pivot.
			if(te.getQQueries().size() > 0 && te.getCount() <= 1000 && te.getCount() > 0) {
				queryManager.sendQuery(te.getQQueries().get(0).getQuery());
			}
		}
		LOG.info("selectedServerNsCatElem " + selectedServerName
				 + "-> " + selectedNamespace
				 + "-> " + selectedCategory
				 + "-> " + selectedElement);
	}
	
	public void clearSelections() {
		selectedServerName = null;
		selectedNamespace = null;
		selectedCategory = Category.UNSELECTED;
		selectedElement = null;
		for(Listener l : listeners) {
			l.selectionChanged(null, selectedCategory, selectedNamespace, selectedElement);
		}
		LOG.info("clearSelections");
	}
	
	/**
	 * Set the selected namespace. 
	 * @throws IllegalArgumentException if namespace does not exist.
	 */
	public void setSelectedCategory(String servername, String namespace, Category category) {
		setSelection(servername, namespace, category,  null);
	}
	
	/**
	 * @return {@link KdbConnection} if possible otherwise null. 
	 */
	public KdbConnection getKdbConnection() {
		KdbConnection conn = null;
		if(selectedServerName != null) {
			conn = connectionManager.getKdbConnection(selectedServerName);
		}
		return conn;
	}
	
	
	
	/**
	 * Set the selected namespace and element.
	 * @throws IllegalArgumentException if namespace or element does not exist.
	 */
	public void setSelectedElement(String servername, String namespace, ServerQEntity elementName) {
		setSelection(servername, namespace, Category.ELEMENT,  elementName);
	}

	public ServerQEntity getSelectedElement() {
		return selectedElement;
	}
	
	public Category getSelectedCategory() {
		return selectedCategory;
	}
	
	
	public String getSelectedNamespace() {
		return selectedNamespace;
	}
	
	/** 
	 * @return The selected server name or null if none is selected.
	 */
	public String getSelectedServerName() {
		return selectedServerName;
	}

	public Set<String> getFolders() {
		return connectionManager.getFolders();
	}
	
	/**
	 * Set the selected namespace. 
	 * @throws IllegalArgumentException if namespace does not exist.
	 */
	public void setSelectedNamespace(String servername, String namespace) {
		setSelection(servername, namespace, Category.NAMESPACE,  null);
	}

	/**
	 * @return  Model of the server config / memory use etc if possible, otherwise null.
	 */
	public ServerModel getServerModel() {
		return serverContainer.get(selectedServerName);
	}

	/**
	 * @return  Model of the server config / memory use etc if possible, otherwise null.
	 */
	public ServerModel getServerModel(String serverName) {
		return serverContainer.get(serverName);
	}


	@Override public void prefChange() {
		BackgroundExecutor.EXECUTOR.execute(new Runnable() {
			@Override public void run() {
				refresh(false);
			}
		});
	}
	@Override public void serverAdded(ServerConfig sc) {  }	
	

	@Override public void statusChange(ServerConfig serverConfig, 
			boolean connected) {
		refresh(serverConfig);
	}

	/**
	 * @return All known entities on all known servers.
	 */
	public List<ServerQEntity> getAllVariables() {
		List<ServerQEntity> vars = Lists.newArrayList();
		for(ServerModel sm : serverModels) {
			ServerObjectTree stree = sm.getServerObjectTree();
			if(stree != null) {
				vars.addAll(stree.getAll());	
			}
		}
		return vars;
	}

	@Override public List<? extends DocumentedEntity> getDocs() {
		return getAllVariables();
	}

	/** 
	 * Move the server model to a selected folder. 
	 */
	public void moveServer(ServerModel serverModel, String folderName) {
		Preconditions.checkNotNull(serverModel);
		String f = folderName==null ? "" : folderName;
		connectionManager.moveServer(serverModel.getServerConfig(), f);
	}

	/** 
	 * Delete a folder and all connections within those folders 
	 * @return The number of servers removed. 
	 */
	public int removeFolder(String folder) {
		return connectionManager.removeFolder(Preconditions.checkNotNull(folder));
	}

	/**
	 * Rename a folder, if the target folder already exists the folders will be merged.
	 * @return The number of {@link ServerConfig}s actually moved. THis may be 0 if folder did not exist.
	 */
	public int renameFolder(String from, String to) {
		return connectionManager.renameFolder(from, to);
	}

}
