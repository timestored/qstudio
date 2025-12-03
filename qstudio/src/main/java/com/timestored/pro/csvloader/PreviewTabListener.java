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
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/** simply records the first result **/
class PreviewTabListener implements TabListener {

	private String[] colNames;
	private List<String[]> rowData = Collections.emptyList();

	@Override
	public void tabEvent(String[] colNames, List<String[]> rowData) {
		if (colNames != null) {
			this.colNames = colNames;
			if(rowData!=null && !rowData.isEmpty()) {
				this.rowData = new ArrayList<String[]>(rowData);
			}
		}
	}

	public TableModel getTopAsTableModel() {
		return new AbstractTableModel() {

			@Override public Object getValueAt(int rowIndex, int columnIndex) {
				if(rowIndex < rowData.size()) {
					String[] v = rowData.get(rowIndex);
					if(v!=null && columnIndex<v.length) {
						return v[columnIndex];
					}
				}
				return "";
			}

			@Override public int getRowCount() { return rowData.size(); }
			@Override public int getColumnCount() { return colNames==null ? 0 : colNames.length; }
			@Override public String getColumnName(int column) { return colNames[column]; }
		};
	}
}