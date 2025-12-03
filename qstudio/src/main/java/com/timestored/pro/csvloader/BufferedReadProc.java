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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import au.com.bytecode.opencsv.CSVReadProc;



/**
 * Buffered readProc that processes lines and every n rows sends listeners
 * an event with a list of strings and column titles. Must flush after use
 * to ensure all messages pushed to listeners.
 */
class BufferedReadProc implements CSVReadProc {

	private static final Logger LOG = Logger.getLogger(CSVLoader.class.getName());
	private final int bufferSize;

	private boolean firstRow = true;
	private String[] colNames = null;
	private final List<TabListener> tabListeners = new CopyOnWriteArrayList<TabListener>();
	private final CsvConfig csvConfig;
	private final List<String[]> rowData;

	public boolean addTabListener(TabListener tabListener) {
		return tabListeners.add(tabListener);
	}

	public void flush() {
		LOG.info("BufferedReadProc flushing " + rowData.size() + " rows.");
		if(!rowData.isEmpty()) {
			for (TabListener tl : tabListeners) {
				tl.tabEvent(colNames, rowData);
			}
			rowData.clear();
		}
	}

	/**
	 * Construct loader that flushes bufferSize chunk of records to listeners.
	 */
	public BufferedReadProc(CsvConfig csvConfig, int bufferSize) {

		this.csvConfig = csvConfig;
		this.bufferSize = bufferSize;
		rowData = new ArrayList<String[]>(bufferSize);
	}

	public void procRow(int rowIndex, String... values) {
		// get column headers
		if (firstRow) {
			colNames = getColumnTitles(csvConfig, values);
		}

		if (!(firstRow && csvConfig.containsHeader())) {
			// pad all rows to have same number of columns as header row.
			if(values.length < colNames.length) {
				String[] v = new String[colNames.length];
				for(int i=0; i<v.length; i++) {
					if(i<values.length) {
						v[i] = values[i];
					} else {
						v[i] = "";
					}
				}
				values = v;
			}
			rowData.add(values);
			if (rowData.size() == bufferSize) {
				for (TabListener tl : tabListeners) {
					tl.tabEvent(colNames, rowData);
					LOG.info("tabEvent " + rowData.size() + " rows.");
				}
				rowData.clear();
			}
		}
		firstRow = false;
	}

	
	private static String[] getColumnTitles(final CsvConfig csvConfig,
			String[] values) {
		String[] cNames = values;
		if (!csvConfig.containsHeader()) {
			cNames = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				cNames[i] = "c" + i;
			}
		}
		return cNames;
	}
}
