/*******************************************************************************
 *
 *   $$$$$$$\            $$\                     
 *   $$  __$$\           $$ |                     
 *   $$ |  $$ |$$\   $$\ $$ | $$$$$$$\  $$$$$$\   
 *   $$$$$$$  |$$ |  $$ |$$ |$$  _____|$$  __$$\  
 *   $$  ____/ $$ |  $$ |$$ |\$$$$$$\  $$$$$$$$ |  
 *   $$ |      $$ |  $$ |$$ | \____$$\ $$   ____|  
 *   $$ |      \$$$$$$  |$$ |$$$$$$$  |\$$$$$$$\  
 *   \__|       \______/ \__|\_______/  \_______|
 *
 *  Copyright c 2022-2023 TimeStored
 *
 *  Licensed under the Reciprocal Public License RPL-1.5
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/license/rpl-1-5/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
 
package com.timestored.babeldb;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Allows constructing  ResultSetMetaData from defined values.
 */
public class SimpleResultSetMetaData implements ResultSetMetaData {

		private final String[] colNames;
		private final int[] colTypes;

		@Override
		public String toString() {
			return "SimpleResultSetMetaData [colNames=" + Arrays.toString(colNames) + ", colTypes="
					+ Arrays.toString(colTypes) + "]";
		}

		public SimpleResultSetMetaData(List<String> colNames, List<Integer> colTypes) {
	        this(colNames.toArray(new String[] {}), colTypes.stream().mapToInt(i->i).toArray());
		}
		
		/**
		 * Construct a rs meta data with known values
		 */
		public SimpleResultSetMetaData(String[] colNames, int[] colTypes) {
			this.colNames = Preconditions.checkNotNull(colNames);
			this.colTypes = Preconditions.checkNotNull(colTypes);
			Preconditions.checkArgument(colNames.length == colTypes.length, "length of names and types match");
		}

		@Override public int getColumnCount() throws SQLException {
			return colNames.length;
		}

		@Override public int getColumnType(int column) throws SQLException {
			if(column>colNames.length) {
				throw new IllegalArgumentException("column outside data range");
			}
			return colTypes[column-1];
		}
		
		@Override public String getColumnLabel(int column) throws SQLException {
			return getColumnName(column);
		}

		@Override public String getColumnName(int column) throws SQLException {
			return colNames[column-1];
		}

		@Override public String getColumnClassName(int column) throws SQLException {
			throw new UnsupportedOperationException();
//			return getObject(column).getClass().getName();
		}
		
		@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override public <T> T unwrap(Class<T> iface) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override public String getCatalogName(int column) throws SQLException { return ""; }


		@Override public int getColumnDisplaySize(int column) throws SQLException { return 11; }


		@Override public String getColumnTypeName(int column) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override public int getPrecision(int column) throws SQLException { return 11; }

		@Override public int getScale(int column) throws SQLException {
			return 0;
		}

		@Override public String getSchemaName(int column) throws SQLException { return ""; }

		@Override public String getTableName(int column) throws SQLException { return ""; }

		@Override public boolean isAutoIncrement(int column) throws SQLException { return false; }

		@Override public boolean isCaseSensitive(int column) throws SQLException { return true; }

		@Override public boolean isCurrency(int column) throws SQLException { return false; }

		@Override public boolean isDefinitelyWritable(int column) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override public int isNullable(int column) throws SQLException { return 1; }

		@Override public boolean isReadOnly(int column) throws SQLException {
			return true;
		}

		@Override public boolean isSearchable(int column) throws SQLException {
			return false;
		}

		@Override public boolean isSigned(int column) throws SQLException {
			return false;
		}

		@Override public boolean isWritable(int column) throws SQLException {
			return false;
		}
		
	}