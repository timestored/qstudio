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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.TableModel;

import kx.c;
import kx.c.KException;
import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.Preconditions;
import com.timestored.kdb.KdbConnection;

/**
 * Allows loading CSV format files as TableModels or into a kdbConnection.
 * For now a non-existent empty table must be specified as the destination
 * 
 */
class CSVLoader {

	private static final Logger LOG = Logger.getLogger(CSVLoader.class.getName());
	private static final int KDB_BUFFER_ROWS = 100;

	private final KdbConnection kdbConn;
	private final String tableName; 
	
	private final KdbTypeMatcherTabListener kdbTypeListener;
	private final KdbSendTabListener kdbSendTabListener;
	private final BufferedReadProc bufferedReader;
	private final CSVReader reader;
	private boolean readerClosed = false;
	
	private int rowsLoaded = 0;

	/**
	 * Streaming read a csv file using the given config and upload its
	 * data to the selected kdbConnection.
	 * @param csvFile A csv file.
	 * @param csvConfig Configuration for reading csv file.
	 */
	public CSVLoader(String csvFile, CsvConfig csvConfig,
			KdbConnection kdbConn, String tableName) throws UnsupportedEncodingException, FileNotFoundException {
		
		File f = getFile(csvFile);
		this.kdbConn = Preconditions.checkNotNull(kdbConn);
		this.tableName = Preconditions.checkNotNull(tableName);
		
		bufferedReader = new BufferedReadProc(csvConfig, KDB_BUFFER_ROWS);
		kdbTypeListener = new KdbTypeMatcherTabListener();
		bufferedReader.addTabListener(kdbTypeListener);
		kdbSendTabListener = new KdbSendTabListener(kdbConn, tableName);
		bufferedReader.addTabListener(kdbSendTabListener);

		reader = csvConfig.getCsvReader(f);
	}

	/**
	 * Try to read all lines in the CSV file and load them onto the kdb server.
	 * @return number of lines loaded to server.
	 */
	public int processAll() throws IOException {
		while(processRow());
		return rowsLoaded;
	}
	
	/**
	 * @return true of an actual row was found and processed or false if end of file reached.
	 */
	boolean processRow() throws IOException {
		
		if(readerClosed) {
			throw new IllegalStateException("reader closed at EOF, cant process more");
		}
		
		// if first row check that we don't overwrite existing table
		if(rowsLoaded == 0) {
			if(!checkTableNameFree()) {
				throw new IllegalArgumentException("Tablename already in use on server.");
			}
		}
		
		String[] values = reader.readNext();

		if(values != null) {
			bufferedReader.procRow(rowsLoaded++, values);
		} else {
		// reached end of file, close reader and send type converter to database server
			close();

			if(rowsLoaded > 0) {
				// convert columns to preferred types
				Map<String, Character> types = kdbTypeListener.getRecognisedKdbTypes();
				if(!types.isEmpty()) {
					String castQuery = getUpdateCast(types, tableName) + ";";	
					kdbConn.send(castQuery);
				}
				
				// cast columns with repeating strings to symbols
				String castCommonSymbols = "{@[`" + tableName + ";x;\"S\"$]} each where 0.5>{{(count distinct x)%count x } each flip $[count strCols:exec c from meta x where t=\"C\";strCols#x;()]} " + tableName + ";";
				kdbConn.send(castCommonSymbols);
			}
		}
		
		return values!=null;
	}

	/**
	 * @return true if table name free otherwise false.
	 * @throws IOException
	 */
	boolean checkTableNameFree() throws IOException {
		boolean variableExists = true;
		try {
			variableExists = (Boolean) kdbConn.query("`" + tableName + " in key `.");
		} catch (KException e) {
			throw new IOException(e);
		} catch (ClassCastException cce) {
			variableExists = true;
		}
		return !variableExists;
	}

	private void close() throws IOException {
		readerClosed = true;
		reader.close();
		bufferedReader.flush();
		rowsLoaded = kdbSendTabListener.getRowsSent();
	}

	public int getRowsLoaded() {
		return rowsLoaded;
	}
	
	private static String getUpdateCast(Map<String, Character> types,
			String tableName) {
		StringBuilder sb = new StringBuilder();
		for(Entry<String, Character> e : types.entrySet()) {
			sb.append("@[`" + tableName + ";`$\"" + e.getKey() + "\";\"" + e.getValue() + "\"$];");
		}
		return sb.toString();
	}

	
	/**
	 * Read the first rows of a csvFile using the given configuration and return
	 * as a {@link TableModel} if possible otherwise null.
	 */
	static TableModel getTopTable(String csvFile, final CsvConfig csvConfig, final int maxRowsShown) {

		File f = getFile(csvFile);
		Charset cs = Charset.forName(csvConfig.getCharset());
		Preconditions.checkNotNull(cs);

	    BufferedReadProc bufferedReader = new BufferedReadProc(csvConfig, maxRowsShown);

	    PreviewTabListener pTabListener = new PreviewTabListener();
	    bufferedReader.addTabListener(pTabListener);

		int rowsToRead = maxRowsShown + (csvConfig.containsHeader() ? 1 : 0);
	    TableModel tm = null;
		try {
			int row = 0;
			CSVReader reader = csvConfig.getCsvReader(f);
			try {
				String[] values = reader.readNext();
				while (values != null && row<rowsToRead ) {
					bufferedReader.procRow(row++, values);
					values = reader.readNext();
				}
				bufferedReader.flush();
				tm = pTabListener.getTopAsTableModel();
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "getTopTable", e);
		}
		return tm;
	}


	private static File getFile(String fileName) {
		Preconditions.checkNotNull(fileName);
		File f = new File(fileName);
		Preconditions.checkArgument(f.exists() && f.canRead());
		return f;
	}

	
	/**
	 * Listen to tab data and send to server as inserts to given tableName.
	 */
	private static class KdbSendTabListener implements TabListener {

		private final KdbConnection kdbConn;
		private final String tableName;
		private int rowsSent = 0;

		public KdbSendTabListener(KdbConnection kdbConn, String tableName) {
			this.kdbConn = Preconditions.checkNotNull(kdbConn);
			this.tableName = Preconditions.checkNotNull(tableName);
		}
		
		@Override public void tabEvent(String[] colNames, List<String[]> rowData) {
			c.Flip tab = getTable(rowData, colNames);
			Object[] updStatement = new Object[] { "insert".toCharArray(),
					tableName, tab };
			try {
				kdbConn.send(updStatement);
				rowsSent += rowData.size();
			} catch (IOException e) {
				LOG.log(Level.WARNING, "loadToServer", e);
			}
		}
		
		public int getRowsSent() {
			return rowsSent;
		}
	}
	
	
	/** convert row data and colNames into kdb table **/
	private static c.Flip getTable(List<String[]> rowData, String[] colNames) {
		Object[] data = new Object[colNames.length];
		for (int c = 0; c < colNames.length; c++) {
			char[][] colVals = new char[rowData.size()][];
			for (int r = 0; r < rowData.size(); r++) {
				colVals[r] = rowData.get(r)[c].toCharArray();
			}
			data[c] = colVals;
		}
		// create the command to insert the table of data into the named table.
		return new c.Flip(new c.Dict(colNames, data));
	}
}
