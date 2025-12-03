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
 *  Commercially licensed only. 
 *  contact licensing@timestored.com 
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
 
package com.timestored.babeldb;

 
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

/**
 * Used to construct rough {@link ResultSet}'s for testing / documentation  purposes.
 */
class PivotResultSet extends BaseResultSet {

	private final List<String> colNames = new ArrayList<>(); // column -> row
	private final List<Integer> colTypes = new ArrayList<>(); // column -> row
	private final List<List<Object>> colValues = new ArrayList<>(); // column -> row
	private int idx = -1;
	private final ResultSetMetaData resultSetMetaData;
	private boolean wasNull;
	private final int size;

	public PivotResultSet(ResultSet rs, String byCol, String pivotCol) throws SQLException {
		this(rs, Lists.newArrayList(byCol), Lists.newArrayList(pivotCol));
	}
	/**
	 * Pivot result set. VERY STRICT REQUIREMENTS that table is sorted on byCols
	 * e.g. if byCols aa,bb,cc table MUST be sorted by aa,bb,cc in that order. Else nonsense returned. 
	 */
	public PivotResultSet(ResultSet rs, List<String> byCols, List<String> pivotCols) throws SQLException {

		if(byCols.size() < 1) { throw new IllegalArgumentException("You must specify one by column at least."); }
		if(pivotCols.size() < 1) { throw new IllegalArgumentException("You must specify one pivot column at least."); }
		
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        List<String> originalColNames = new ArrayList<>(numColumns);
        List<Integer> originalColTypes = new ArrayList<>(numColumns);

        for (int i = 0; i < numColumns; i++) {
        	originalColNames.add(rsmd.getColumnLabel(i + 1));
        	originalColTypes.add(rsmd.getColumnType(i + 1));
        }
        
        for(String byc : byCols) {
        	if(!originalColNames.contains(byc)) {
        		throw new IllegalArgumentException("by column not contained in original table: " + byc);
        	}
        	colNames.add(byc); // place by columns first in order passed in
        	colTypes.add(rsmd.getColumnType(1 + originalColNames.indexOf(byc)));
            colValues.add(new ArrayList<>());
        }

        for(String pc : pivotCols) {
        	if(!originalColNames.contains(pc)) {
        		throw new IllegalArgumentException("pivot column not contained in original table: " + pc);
        	}
        }

        // Check original table is sorted on by and pivot cols else the assumptions later on pivoting would create errors
		List<String> bcols = new ArrayList<>();
		bcols.addAll(byCols);
		bcols.addAll(pivotCols);
		checkColsUniquelySorted(rs, bcols);

        rs.beforeFirst();
        int row = 0;
        while (rs.next()) {
        	String prefix = "";
        	for(String pc : pivotCols) {
        		prefix += (rs.getObject(pc) + "_");
        	}

        	// If empty or new BY row, finish prev and add BY values for new.
    		boolean allBysMatch = true;
        	if(row > 0) {
				for(String b : byCols) {
					List<Object> clist = colValues.get(colNames.indexOf(b));
					if(!clist.get(clist.size()-1).equals(rs.getObject(b))) {
						allBysMatch = false;	
						break;
					}
				}
        	}
        	if(row == 0 || !allBysMatch) {
            	makeRowsEqual(byCols.size());
				for(String b : byCols) {
					putt(colTypes.get(colNames.indexOf(b)), b, rs.getObject(b));
				}
        	}
        	
			// Fill in the pivots
        	for(int c=0; c<numColumns; c++) {
        		String oname = originalColNames.get(c);
        		if(!pivotCols.contains(oname) && !byCols.contains(oname)) {
        			String newColName = prefix + oname;
        			if(byCols.contains(newColName)) {
        				throw new IllegalArgumentException("Newly generated pivot column name clashed with by column name");
        			}
        			int otype = originalColTypes.get(c);
            		Object v = gett(otype, rs, c);
        			putt(otype, newColName, v);
        		}
        		
        	}
        	row++;
        }
    	makeRowsEqual(byCols.size());

        // bubble sort - Keep the by columns in the order specified AFTER that column ordering based on  name
        // `j`k`h`l - outer loop moves left to right, then as it's at the current item it brings it to the left while it's not in order.
        for(int i=byCols.size()+1; i<colNames.size(); i++) {
    		for(int j=i; j>byCols.size() && colNames.get(j).compareTo(colNames.get(j-1)) < 0; j--) {
    			Collections.swap(colNames, j, j-1);
    			Collections.swap(colTypes, j, j-1);
    			Collections.swap(colValues, j, j-1);
    		}
        }
        this.size = colValues.size() > 0 ? colValues.get(0).size() : 0;
		this.resultSetMetaData = new SimpleResultSetMetaData(colNames, colTypes);
	}
	
	private static void checkColsUniquelySorted(ResultSet rs, List<String> bcols) throws SQLException {
		List<HashSet<Object>> uniqValsPerCol = new ArrayList<>(); // column -> row
		List<Object> prevValPerCol = new ArrayList<>(); // column -> row
		for(int i=0; i<bcols.size();i++) { uniqValsPerCol.add(new HashSet<>()); prevValPerCol.add(null); };
        rs.beforeFirst();
        while (rs.next()) {
        	for(int c=0; c<bcols.size();c++) {
        		String cname = bcols.get(c);
        		Object obj = rs.getObject(cname);
        		// If leftmost by column does NOT contain this value, reset all uniques at lower levels.
        		if(!uniqValsPerCol.get(c).contains(obj)) {
        			uniqValsPerCol.get(c).add(obj);
        			for(int cc=c+1; cc<bcols.size();cc++) {
        				uniqValsPerCol.get(cc).clear();
        			}
        		} else {
        			boolean matchesPrev = obj == null ? prevValPerCol.get(c) == null : obj.equals(prevValPerCol.get(c));
        			if(!matchesPrev) {
            			throw new IllegalArgumentException("already seen value isn't consecutive val=" + obj + " col = " + cname);
        			}
        			// this value was NOT unique - if we are a leaf this is an error
            		if(c == bcols.size()-1) { // leaf node - must be unique
            			throw new IllegalArgumentException("repeating entry in column " + cname);
            		}
        		}
        		prevValPerCol.set(c, obj);
        	}
        }
	}

	
	private void makeRowsEqual(int numberByCols) {
		// Must use the max as any of the by columns can be the max one
		// e.g. ([a:1 2; b:1 1]...) OR ([a:1 1; b:1 2]...)
		int maxRows = colValues.stream().map(l -> l.size()).mapToInt(Integer::intValue).max().getAsInt();
    	for(int c=0; c<colValues.size(); c++) {
    		List<Object> curCol = colValues.get(c);
    		if(curCol.size() < maxRows) {
    			// IF a by column, fill with previous value ELSE use null for pivot columns
    			Object val = c < numberByCols ? curCol.get(curCol.size()-1) : null;
    			colValues.get(c).add(val);
    		}
    	}
	}


	private void putt(int colType, String cname, Object v) {
		int idx = colNames.indexOf(cname);
		if(idx == -1) {
			colNames.add(cname);
			colTypes.add(colType);
			colValues.add(new ArrayList<>());
			int sz = colValues.size() - 1;
			int maxRows = colValues.stream().map(l -> l.size()).mapToInt(Integer::intValue).max().getAsInt();
			for(int r=0; r<maxRows-1; r++) {
				colValues.get(sz).add(null);
			}
			colValues.get(sz).add(v);
		} else {
			colValues.get(idx).add(v);
		}
	}


	static Object gett(int colType, ResultSet rs, int i) throws SQLException {
		// Copied From ResultSet Serializer
		// Is this even required or can I just call getObject() - depends how "smart" the JDBC driver is
		// Anything that does some smart deserialization may fail.
		Object l = null;
        switch (colType) {

        case Types.INTEGER:
            l = rs.getInt(i + 1);
            if (rs.wasNull()) { l = null; }
            break;
        case Types.BIGINT:
            l = rs.getLong(i + 1);
            if (rs.wasNull()) { l = null; }
            break;
        case Types.DECIMAL:
        case Types.NUMERIC:
            l = rs.getBigDecimal(i + 1);
            break;
        case Types.FLOAT:
        case Types.REAL:
        case Types.DOUBLE:
            l = rs.getDouble(i + 1);
            if (rs.wasNull()) { l = null; }
            break;
        case Types.NVARCHAR:
        case Types.VARCHAR:
        case Types.LONGNVARCHAR:
        case Types.LONGVARCHAR:
            l = rs.getString(i + 1);
            break;
        case Types.BOOLEAN:
        case Types.BIT:
            l = rs.getBoolean(i + 1);
            if (rs.wasNull()) { l = null; }
            break;

        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
            l = rs.getBytes(i + 1);
            break;

        case Types.TINYINT:
        case Types.SMALLINT:
            l = rs.getShort(i + 1);
            if (rs.wasNull()) { l = null; }
            break;
            
        case Types.BLOB:
            l = rs.getBlob(i);
            break;

        case Types.CLOB:
            l = rs.getClob(i);
            break;
        case Types.STRUCT:
            throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type STRUCT");

        case Types.DISTINCT:
            throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type DISTINCT");

        case Types.REF:
            throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type REF");

        case Types.DATE:
        case Types.TIMESTAMP:
        case Types.TIME:     
        case Types.TIME_WITH_TIMEZONE:     
        case Types.TIMESTAMP_WITH_TIMEZONE:   
        case Types.ARRAY:  
        case Types.JAVA_OBJECT:
        default:
        	l = rs.getObject(i + 1);
            break;
        }
		return l;
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		int idx = colNames.indexOf(columnLabel);
		if(idx != -1) { return idx; }
		throw new SQLException();
	}
	
	@Override public boolean absolute(int row) throws SQLException {
		idx = row-1;
		return true;
	}

	@Override public void beforeFirst() throws SQLException {
		idx = -1;
	}

	@Override public void afterLast() throws SQLException {
		idx = size;
	}

	@Override public boolean first() throws SQLException {
		idx = 0;
		return true;
	}

	@Override public int getRow() throws SQLException { return idx+1; }
	@Override public boolean isAfterLast() throws SQLException { return idx >= size; }
	@Override public boolean isBeforeFirst() throws SQLException { return idx < 0; }
	@Override public boolean isFirst() throws SQLException { return idx == 0; }
	@Override public boolean isLast() throws SQLException { return idx == (size - 1); }

	@Override public boolean last() throws SQLException {
		idx = size - 1;
		return size > 0;
	}

	@Override public boolean next() throws SQLException {
		idx++;
		return idx < size;
	}

	@Override public boolean previous() throws SQLException {
		idx--;
		return idx >= 0;
	}
	
	

	@Override public ResultSetMetaData getMetaData() throws SQLException { return resultSetMetaData; }


	@Override public Object getObject(int columnIndex) throws SQLException {
		Object o = colValues.get(columnIndex-1).get(idx);
		wasNull = o == null;
		return o;
	}
	
	@Override public boolean wasNull() throws SQLException { return  wasNull; }

	@Override public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException();
	}

	
}
