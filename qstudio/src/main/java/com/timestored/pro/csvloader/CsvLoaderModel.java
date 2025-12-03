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
package com.timestored.pro.csvloader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.Preconditions;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ServerConfig;
import com.timestored.kdb.KdbConnection;

/**
 * State class for holding configurable model from which we can get a CsvLoader
 */
class CsvLoaderModel {
	
	private CsvConfig csvConfig;
	private File csvFile;
	private String tableName;
	/** null if none available or selected otherwise the name of selected server */
	private ServerConfig selectedServer;
	private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
	
	private final ConnectionManager connectionManager;
	
	protected static interface Listener {
		void update(CsvLoaderModel csvLoaderModel);	
	}
	
	public CsvLoaderModel(ConnectionManager connectionManager, File file) throws IOException {
		
		this.connectionManager = Preconditions.checkNotNull(connectionManager);
		setCsvFile(file);
		List<String> names = connectionManager.getServerNames();
		if(!names.isEmpty()) {
			selectedServer = connectionManager.getServer(names.get(0));
		}
		
		csvConfig = CsvConfig.detect(file);
	}
	
	
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
	public File getCsvFile() {
		return csvFile;
	}
	
	public void setCsvFile(File csvFile) {
		String msg = "valid file must be selected.";
		Preconditions.checkNotNull(csvFile, msg);
		boolean sameFile = this.csvFile!=null && csvFile.getAbsolutePath().equals(this.csvFile.getAbsolutePath());
		if(!sameFile) {
			Preconditions.checkArgument(csvFile.isFile() && csvFile.canRead(), msg);
			this.csvFile = csvFile;
			notifyListeners();
		}
	}
	
	/**
	 * @param tableName The destination table on the server, must consist of 
	 * 				characters and numbers only.
	 * @throws IllegalArgumentException If tablename is invalid.
	 */
	public void setTableName(String tableName) {
		Preconditions.checkNotNull(tableName);
		if(!tableName.equals(this.tableName)) {
			String msg =  "Table name must be atleast one letter and all ascii characters only";
			Preconditions.checkArgument(tableName.matches("[a-zA-Z0-9]+"), msg);
			this.tableName = tableName;
			notifyListeners();
		}
	}
	
	public String getTableName() {
		return tableName;
	}
	
	/**
	 * Set a server to upload the csv file to.
	 * @throws IllegalArgumentException If server is non-null but does not exist. 
	 */
	public void setServer(ServerConfig selectedServer) {
		if(selectedServer != null) {
			Preconditions.checkArgument(connectionManager.contains(selectedServer));
		}
		this.selectedServer = selectedServer;
		notifyListeners();
		
	}
	
	public ServerConfig getServer() {
		return selectedServer;
	}
		
	/** 
	 * If you edit the csv config returned by this call you must 
	 * {@link #notifyListeners()} to let listeners see those changes 
	 */
	public CsvConfig getCsvConfig() {
		return csvConfig;
	}

	/** notify listeners that a change occurred */
	void notifyListeners() {
		for(Listener l : listeners) {
			l.update(this);
		}
	}
	
	public boolean addListener(Listener listener) {
		return listeners.add(listener);
	}

	/**
	 * insert the currently selected csv file to the given server.
	 * @throws IOException tablename was invalid or problem sending to server.
	 * @throws IllegalStateException If invalid parameter set.
	 * @return The number of rows sent to the table.
	 */
	public CSVLoader getCsvLoader() throws IOException {
		
		checkStateValid();
		KdbConnection kdbConn = connectionManager.getKdbConnection(selectedServer);
		if(kdbConn == null) {
			throw new IOException("Was not possible to connect.");
		}
		return new CSVLoader(csvFile.getAbsolutePath(), csvConfig, kdbConn, tableName);
	}


	private void checkStateValid() {

		if(tableName==null || tableName.trim().length()<1) {
			throw new IllegalStateException("Table name must be specified");
		}
		if(csvFile==null || !csvFile.canRead() || !csvFile.isFile()) {
			throw new IllegalStateException("Csv file not specified or cannot be read");
		}
	}

}
