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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetWarning;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;

import com.google.common.base.Preconditions;

/**
 * Used to construct rough {@link ResultSet}'s for testing / documentation  purposes.
 */
public class SimpleResultSet extends BaseResultSet implements CachedRowSet {
	
	private final String[] colNames;
	private final Object[] colValues;
	private final int size;
	private int idx = -1;
	private final ResultSetMetaData resultSetMetaData;
	private boolean wasNull;
	
	public static SimpleResultSet til(int n) {
		int[] a = new int[n];
		for(int i=0; i<a.length; i++) {
			a[i] = i;
		}
		return new SimpleResultSet(new String[] {"v"}, new Object[] { a });
	}

	/** Construct an empty resultSet where only the column names are known. */
	public SimpleResultSet(String[] colNames) {
		this(colNames, getEmptyVals(colNames.length));
	}

	private static Object[] getEmptyVals(int numberColumns) {
		Object[] val = new Object[numberColumns];
		for(int i=0; i < numberColumns; i++) {
			val[i] = new Object[] {};
		}
		return val;
	}
	
	public SimpleResultSet(List<String> colNames, List<Object> colValues) {
		this(colNames.toArray(new String[colNames.size()]), colValues.toArray(new Object[colValues.size()]));
	}
	
	public String[] getColNames() { return colNames; }

	public SimpleResultSet getColumnSubset(List<String> originalColumnNames, List<String> optionalRenaming) throws SQLException {
		if(originalColumnNames == null || originalColumnNames.size() < 1) {
			throw new IllegalArgumentException("Must specify columns");
		}
		List<String> newNames = optionalRenaming != null ? optionalRenaming : originalColumnNames;
		List<Object> newColValues = new ArrayList<>(originalColumnNames.size());
		for(String colName : originalColumnNames) {
			newColValues.add(this.colValues[findColumn(colName)-1]);
		}
		return new SimpleResultSet(newNames, newColValues); 
	}


	public SimpleResultSet renameColumns(ArrayList<String> newColumnNames) {
		if(newColumnNames.size() != this.colNames.length) {
			throw new IllegalArgumentException("Must reame all columns");
		}
		return new SimpleResultSet(newColumnNames.toArray(new String[] {}), this.colValues); 
	}
	
	public SimpleResultSet(String[] colNamesArg, Object[] colValues) {
		int dupeCount = 1;
		Preconditions.checkArgument(colNamesArg.length == colValues.length);
		
		List<String> newColNames = new ArrayList<>();
		for(String cn : colNamesArg) {
			boolean isEmpty = cn.trim().equals("");
			if(isEmpty || newColNames.contains(cn)) {
				while(newColNames.contains((isEmpty ? "c" : cn)+dupeCount)) {
					dupeCount++;
				}
				newColNames.add((isEmpty ? "c" : cn)+dupeCount);
			} else {
				newColNames.add(cn);
			}
		}
		this.colNames = newColNames.toArray(new String[] {});
		this.colValues = colValues;
		
		// if no rows, assume type is varchar
		int[] types = null;
		if(colValues.length == 0) {
			types = new int[colNames.length];
			Arrays.fill(types, java.sql.Types.VARCHAR);
		} else {
			types = getTypes(colValues);
		}
		size = this.colValues.length > 0 ? Array.getLength(colValues[0]) : 0;
		
		this.resultSetMetaData = new SimpleResultSetMetaData(colNames, types);
	}

	private static int getType(Object o) {
		if(o instanceof String[]) {
			return java.sql.Types.VARCHAR;
		} else if(o instanceof boolean[]) {
			return java.sql.Types.BIT;
		} else if(o instanceof short[]) {
			return java.sql.Types.SMALLINT;
		} else if(o instanceof int[]) {
			return java.sql.Types.INTEGER;
		} else if(o instanceof long[]) {
			return java.sql.Types.BIGINT;
		} else if(o instanceof float[]) {
			return java.sql.Types.REAL;
		} else if(o instanceof double[]) {
			return java.sql.Types.DOUBLE;			
		} else if(o instanceof java.sql.Timestamp[]) {
			return java.sql.Types.TIMESTAMP;
		} else if(o instanceof java.sql.Date[]) {
			return java.sql.Types.DATE;
		} else if(o instanceof java.util.Date[]) {
			return java.sql.Types.DATE;
		} else if(o instanceof java.sql.Time[]) {
			return java.sql.Types.TIME;
		} else if(o instanceof Instant[]) {
			return java.sql.Types.TIMESTAMP;
		} else if(o instanceof LocalDate[]) {
			return java.sql.Types.DATE;
		} else if(o instanceof LocalTime[]) {
			return java.sql.Types.TIME;
		}
		return java.sql.Types.VARCHAR;
	}
	
	private static int[] getTypes(Object[] colValues) {
		int[] r = new int[colValues.length];
		for(int i=0; i<r.length; i++) {
			r[i] = getType(colValues[i]);
		}
		return r;
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		for(int i=0; i<colNames.length; i++) {
			if(colNames[i].equals(columnLabel)) {
				return i+1;
			}
		}
		throw new SQLException();
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException { return resultSetMetaData; }

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		Object o = Array.get(colValues[columnIndex-1], idx);
		wasNull = o == null || (o instanceof Double && Double.isNaN((Double)o)) || (o instanceof Float && Float.isNaN((Float)o));
		return wasNull ? null : o;
	}
	
	@Override public boolean wasNull() throws SQLException { return wasNull; }

	@Override public Object getObject(int columnIndex, Map<String, Class<?>> map)
			throws SQLException {
		throw new UnsupportedOperationException();
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
	
	

	private static final Set<String> DTCOLS = new HashSet<>();
	static {
		DTCOLS.add("d");
		DTCOLS.add("t");
		DTCOLS.add("date");
		DTCOLS.add("time");
	}
	
	public SimpleResultSet castTypes(Map<String, Integer> colTypes, boolean autoconvertOtherCols) {
		Object[] colVals = new Object[this.colValues.length];
		for(int c=0; c<colVals.length; c++) {
			colVals[c] = this.colValues[c];
			Integer newT = colTypes != null ? colTypes.get(this.colNames[c]) : null;
			if(newT != null) {
				colVals[c] = castArray(newT, colVals[c]);
			} else if(autoconvertOtherCols) {
				if(isStringAnd(colVals[c], s -> s.matches("-?\\d+")) ) {
					colVals[c] = castArray(java.sql.Types.BIGINT, colVals[c]);
				}
				// automatic castings
				String cn = this.colNames[c].toLowerCase();
				if(DTCOLS.contains(cn) || cn.endsWith("date") || cn.endsWith("time")) {
					if(isAll(colVals[c], cv -> cv instanceof Long) ) {
						// 20101227
						if(isLongAnd(colVals[c], n -> n > 19000101 && n <= 21001231)) {
							colVals[c] = castArray(java.sql.Types.DATE, colVals[c]);
						} else {
							colVals[c] = castArray(java.sql.Types.TIMESTAMP, colVals[c]);
						}
					}
				} else if(cn.trim().endsWith("year")) {
					if(isLongAnd(colVals[c], n -> n > 1700 && n <= 2200)) {
						colVals[c] = castArray(java.sql.Types.DATE, colVals[c]);
					}
				}
			}
		}
		return new SimpleResultSet(this.colNames, colVals);
	}

	boolean isStringAnd(Object col, Predicate<String> predicate) {
		return isAll(col, v -> v != null && v instanceof String && predicate.test((String)v));
	}

	boolean isLongAnd(Object col, Predicate<Long> predicate) {
		return isAll(col, v -> v != null && v instanceof Long && predicate.test((Long)v));
	}

	boolean isAll(Object col, Predicate<Object> predicate) {
		int sz = Array.getLength(col);
		for(int i=0; i<sz; i++) {
			if(!predicate.test(Array.get(col, i))) {
				return false;
			}
		}
		return true;
	}

	public static Object[] getArray(List<Integer> sqlTypes, int sz) {
		Object[] r = new Object[sqlTypes.size()];
		for(int c=0; c<r.length; c++) {
			r[c] = getArrayFromSqlType(sqlTypes.get(c), sz);
		}
		return r;
	}
	
	@Override public String toString() {
		int curIdx = this.idx;
		try {
			return DBHelper.toString(this, true);
		} catch (SQLException e) {
			return "SimpleResultSet [colNames=" + Arrays.toString(colNames) + ", colValues=" + Arrays.toString(colValues)
			+ ", size=" + size + ", idx=" + idx + "]";
		} finally {
			this.idx = curIdx;
		}
	}

	/**
	 * One SQL type may map to multiple Java types depending on the driver.
	 * Better to decide type on what is about to be stored.
	 */
	public static Object getArray(Object o, int size) {
		if(o instanceof Instant) { return new Instant[size]; }
		if(o instanceof java.sql.Date) { return new java.sql.Date[size]; }
		if(o instanceof java.util.Date) { return new java.util.Date[size]; }
		if(o instanceof LocalDate) { return new LocalDate[size]; }
		if(o instanceof LocalTime) { return new LocalTime[size]; }
		if(o instanceof LocalDateTime) { return new LocalDateTime[size]; }
		if(o instanceof String) { return new String[size]; }
		if(o instanceof Boolean) { return new boolean[size]; }
		if(o instanceof Short) { return new short[size]; }
		if(o instanceof Integer) { return new int[size]; }
		if(o instanceof Long) { return new long[size]; }
		if(o instanceof Float) { return new float[size]; }
		if(o instanceof Double) { return new double[size]; }
		if(o instanceof java.sql.Timestamp) { return new java.sql.Timestamp[size]; }
		if(o instanceof java.sql.Time) { return new java.sql.Time[size]; }
		throw new IllegalArgumentException("Unrecognised Object for SRS = " + o);
	}

	/**
	 * If converting an existing RS do NOT use this function. Use getArray(Object o, int size)
	 * One SQL type may map to multiple Java types depending on the driver.
	 */
	private static Object getArrayFromSqlType(int sqlType, int sz) {
		switch(sqlType) {
			case java.sql.Types.DOUBLE: return new double[sz];
			case java.sql.Types.INTEGER: return new int[sz];
			case java.sql.Types.BIGINT: return new long[sz];
			case java.sql.Types.BOOLEAN: return new boolean[sz];
			case java.sql.Types.VARCHAR: return new String[sz];
			case java.sql.Types.DATE: return new Date[sz];
			case java.sql.Types.TIMESTAMP: return new Timestamp[sz];
			default:
		}
		return new Object[sz];
	}
	
	private Object castArray(int newT, Object array) {
		int sz = Array.getLength(array);
		Object newArr = getArrayFromSqlType(newT, sz);
		for(int r=0; r < sz; r++) {
			Object val = Array.get(array, r);
			JsonResultSetBuilder.arraySetUnlessNull(newArr, r, cast(newT, val) );
		}
		return newArr;
	}

	private Object castFromString(int sqlType, String val) {
		switch(sqlType) {
		case java.sql.Types.DOUBLE: return Double.parseDouble(val);
		case java.sql.Types.INTEGER: return Integer.parseInt(val);
		case java.sql.Types.BIGINT: return Long.parseLong(val);
		case java.sql.Types.BOOLEAN: return Boolean.parseBoolean(val);
		case java.sql.Types.TIMESTAMP:
			try {
				return new Timestamp(Long.parseLong(val));
			} catch(NumberFormatException nfe) {
				throw new UnsupportedOperationException();	
			}
		case java.sql.Types.VARCHAR:
		default:
		}
		return val;
	}
	private static final SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
	private static final SimpleDateFormat yyyy = new SimpleDateFormat("yyyy", Locale.ENGLISH);
	
	private Object cast(int sqlType, Object val) {
		if(val instanceof String) {
			return castFromString(sqlType, (String)val);
		}
		switch(sqlType) {
		case java.sql.Types.DOUBLE: return val instanceof Double ? val : Double.NaN;
		case java.sql.Types.INTEGER: return val instanceof Integer ? val : 0;
		case java.sql.Types.BIGINT: return val instanceof Long ? val : 0;
		case java.sql.Types.BOOLEAN: return val instanceof Boolean ? val : false;
		case java.sql.Types.TIMESTAMP: 
			// Note: this conversion may lose some precision when converting Double -> Long
			if(val instanceof Long) {
				long v = (Long) val;
				if(v > 1002857600 && v < 3002857600l) { // between this range is seconds
					return new Timestamp(v * 1000);
				} else if(v > 1000*1002857600 && v < 1000*3002857600l) { // between this range is milliseconds
					return new Timestamp(v);
				}
			}
			return val instanceof Timestamp ? val : val instanceof Number ? new Timestamp(((Number)val).longValue()) : null;
		case java.sql.Types.DATE:
			if(val instanceof Timestamp) {
				return new Date(((Timestamp)val).getTime());
			} else if(val instanceof Date) {
				return val;
			} else if(val instanceof Long) {
				long v = (Long) val;
				if(v > 1002857600 && v < 3002857600l) { // between this range is seconds
					return new Date(v * 1000);
				} else if(v > 1000*1002857600 && v < 1000*3002857600l) { // between this range is milliseconds
					return new Date(v);
				}
				try {
					return v<2200 && v>1700 ? yyyy.parse(""+v) : yyyyMMdd.parse(""+v);
				} catch (ParseException e) {}
			}
			return new Date(0);
		case java.sql.Types.VARCHAR:
		default:
		}
		return (String) val;
	}

	/**
	 * CachedRowSetImplementation below here
	 * CachedRowSetImplementation below here
	 * CachedRowSetImplementation below here
	 * CachedRowSetImplementation below here
	 */
	@Override public int size() { return size;  };
	@Override public RowSet createShared() throws SQLException {return new SimpleResultSet(this.colNames,this. colValues); }
	
	@Override public String getUrl() throws SQLException { return null; }
	@Override public void setUrl(String url) throws SQLException {  }
	@Override public String getDataSourceName() { return null; }
	@Override public void setDataSourceName(String name) throws SQLException { }
	@Override public String getUsername() { return null; }
	@Override public void setUsername(String name) throws SQLException { }
	@Override public String getPassword() { return null; }
	@Override public void setPassword(String password) throws SQLException { }
	@Override public int getTransactionIsolation() { return 0; }
	@Override public void setTransactionIsolation(int level) throws SQLException {}
	@Override public Map<String, Class<?>> getTypeMap() throws SQLException {return null; }
	@Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException {}
	@Override public String getCommand() {return null; }
	@Override public void setCommand(String cmd) throws SQLException {}
	@Override public boolean isReadOnly() { return false; }
	@Override public void setReadOnly(boolean value) throws SQLException {}
	@Override public int getMaxFieldSize() throws SQLException { return 0; }
	@Override public void setMaxFieldSize(int max) throws SQLException {}
	@Override public int getMaxRows() throws SQLException { return Integer.MAX_VALUE; }
	@Override public void setMaxRows(int max) throws SQLException {}
	@Override public boolean getEscapeProcessing() throws SQLException { return false; }
	@Override public void setEscapeProcessing(boolean enable) throws SQLException {}
	@Override public int getQueryTimeout() throws SQLException { return 0; }
	@Override public void setQueryTimeout(int seconds) throws SQLException {}
	@Override public void setType(int type) throws SQLException {}
	@Override public void setConcurrency(int concurrency) throws SQLException {}
	@Override public void setNull(int parameterIndex, int sqlType) throws SQLException {}
	@Override public void setNull(String parameterName, int sqlType) throws SQLException {}
	@Override public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {}
	@Override public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {}
	@Override public void setBoolean(int parameterIndex, boolean x) throws SQLException {}
	@Override public void setBoolean(String parameterName, boolean x) throws SQLException {}
	@Override public void setByte(int parameterIndex, byte x) throws SQLException {}
	@Override public void setByte(String parameterName, byte x) throws SQLException {}
	@Override public void setShort(int parameterIndex, short x) throws SQLException {}
	@Override public void setShort(String parameterName, short x) throws SQLException {}
	@Override public void setInt(int parameterIndex, int x) throws SQLException {}
	@Override public void setInt(String parameterName, int x) throws SQLException {}
	@Override public void setLong(int parameterIndex, long x) throws SQLException {}
	@Override public void setLong(String parameterName, long x) throws SQLException {}
	@Override public void setFloat(int parameterIndex, float x) throws SQLException {}
	@Override public void setFloat(String parameterName, float x) throws SQLException {}
	@Override public void setDouble(int parameterIndex, double x) throws SQLException {}
	@Override public void setDouble(String parameterName, double x) throws SQLException {}
	@Override public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {}
	@Override public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {}
	@Override public void setString(int parameterIndex, String x) throws SQLException {}
	@Override public void setString(String parameterName, String x) throws SQLException {}
	@Override public void setBytes(int parameterIndex, byte[] x) throws SQLException {}
	@Override public void setBytes(String parameterName, byte[] x) throws SQLException {}
	@Override public void setDate(int parameterIndex, java.sql.Date x) throws SQLException {}
	@Override public void setTime(int parameterIndex, Time x) throws SQLException {}
	@Override public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {}
	@Override public void setTimestamp(String parameterName, Timestamp x) throws SQLException {}
	@Override public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {}
	@Override public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {}
	@Override public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {}
	@Override public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {}
	@Override public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {}
	@Override public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {}
	@Override public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {}
	@Override public void setAsciiStream(String parameterName, InputStream x) throws SQLException {}
	@Override public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {}
	@Override public void setBinaryStream(String parameterName, InputStream x) throws SQLException {}
	@Override public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {}
	@Override public void setCharacterStream(String parameterName, Reader reader) throws SQLException {}
	@Override public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {}
	@Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {}
	@Override public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {}
	@Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {}
	@Override public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {}
	@Override public void setObject(String parameterName, Object x) throws SQLException {}
	@Override public void setObject(int parameterIndex, Object x) throws SQLException {}
	@Override public void setRef(int i, Ref x) throws SQLException {}
	@Override public void setBlob(int i, Blob x) throws SQLException {}
	@Override public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {}
	@Override public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {}
	@Override public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {}
	@Override public void setBlob(String parameterName, Blob x) throws SQLException {}
	@Override public void setBlob(String parameterName, InputStream inputStream) throws SQLException {}
	@Override public void setClob(int i, Clob x) throws SQLException {}
	@Override public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {}
	@Override public void setClob(int parameterIndex, Reader reader) throws SQLException {}
	@Override public void setClob(String parameterName, Reader reader, long length) throws SQLException {}
	@Override public void setClob(String parameterName, Clob x) throws SQLException {}
	@Override public void setClob(String parameterName, Reader reader) throws SQLException {}
	@Override public void setArray(int i, java.sql.Array x) throws SQLException {}
	@Override public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException {}
	@Override public void setDate(String parameterName, java.sql.Date x) throws SQLException {}
	@Override public void setDate(String parameterName, java.sql.Date x, Calendar cal) throws SQLException {}
	@Override public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {}
	@Override public void setTime(String parameterName, Time x) throws SQLException {}
	@Override public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {}
	@Override public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {}
	@Override public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {}
	@Override public void clearParameters() throws SQLException {}
	@Override public void execute() throws SQLException {}
	@Override public void addRowSetListener(RowSetListener listener) {}
	@Override public void removeRowSetListener(RowSetListener listener) {}
	@Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {}
	@Override public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {}
	@Override public void setRowId(int parameterIndex, RowId x) throws SQLException {}
	@Override public void setRowId(String parameterName, RowId x) throws SQLException {}
	@Override public void setNString(int parameterIndex, String value) throws SQLException {}
	@Override public void setNString(String parameterName, String value) throws SQLException {}
	@Override public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {}
	@Override public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {}
	@Override public void setNCharacterStream(String parameterName, Reader value) throws SQLException {}
	@Override public void setNClob(String parameterName, NClob value) throws SQLException {}
	@Override public void setNClob(String parameterName, Reader reader, long length) throws SQLException {}
	@Override public void setNClob(String parameterName, Reader reader) throws SQLException {}
	@Override public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {}
	@Override public void setNClob(int parameterIndex, NClob value) throws SQLException {}
	@Override public void setNClob(int parameterIndex, Reader reader) throws SQLException {}
	@Override public void setURL(int parameterIndex, URL x) throws SQLException {}
	@Override public void setMatchColumn(int columnIdx) throws SQLException {}
	@Override public void setMatchColumn(int[] columnIdxes) throws SQLException {}
	@Override public void setMatchColumn(String columnName) throws SQLException {}
	@Override public void setMatchColumn(String[] columnNames) throws SQLException {}
	@Override public int[] getMatchColumnIndexes() throws SQLException {return null; }
	@Override public String[] getMatchColumnNames() throws SQLException {return null; }
	@Override public void unsetMatchColumn(int columnIdx) throws SQLException {}
	@Override public void unsetMatchColumn(int[] columnIdxes) throws SQLException {}
	@Override public void unsetMatchColumn(String columnName) throws SQLException {}
	@Override public void unsetMatchColumn(String[] columnName) throws SQLException {}
	@Override public void execute(Connection conn) throws SQLException {}
	@Override public void acceptChanges() throws SyncProviderException {}
	@Override public void acceptChanges(Connection con) throws SyncProviderException {}
	@Override public void restoreOriginal() throws SQLException {}
	@Override public void release() throws SQLException {}
	@Override public void undoDelete() throws SQLException {}
	@Override public void undoInsert() throws SQLException {}
	@Override public void undoUpdate() throws SQLException {}
	@Override public boolean columnUpdated(int idx) throws SQLException { return false; }
	@Override public boolean columnUpdated(String columnName) throws SQLException { return false; }
	@Override public Collection<?> toCollection() throws SQLException {return null; }
	@Override public Collection<?> toCollection(int column) throws SQLException {return null; }
	@Override public Collection<?> toCollection(String column) throws SQLException {return null; }
	@Override public SyncProvider getSyncProvider() throws SQLException {return null; }
	@Override public void setSyncProvider(String provider) throws SQLException {}
	@Override public void setMetaData(RowSetMetaData md) throws SQLException {}
	@Override public void populate(ResultSet data) throws SQLException {}
	@Override public ResultSet getOriginal() throws SQLException {return null; }
	@Override public ResultSet getOriginalRow() throws SQLException {return null; }
	@Override public void setOriginalRow() throws SQLException {}
	@Override public String getTableName() throws SQLException {return null; }
	@Override public void setTableName(String tabName) throws SQLException {}
	@Override public int[] getKeyColumns() throws SQLException {return null; }
	@Override public void setKeyColumns(int[] keys) throws SQLException {}
	@Override public CachedRowSet createCopy() throws SQLException {return null; }
	@Override public CachedRowSet createCopySchema() throws SQLException {return null; }
	@Override public CachedRowSet createCopyNoConstraints() throws SQLException {return null; }
	@Override public RowSetWarning getRowSetWarnings() throws SQLException {return null; }
	@Override public boolean getShowDeleted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override public void setShowDeleted(boolean b) throws SQLException {}
	@Override public void commit() throws SQLException {}
	@Override public void rollback() throws SQLException {}
	@Override public void rollback(Savepoint s) throws SQLException {}
	@Override public void rowSetPopulated(RowSetEvent event, int numRows) throws SQLException {}
	@Override public void populate(ResultSet rs, int startRow) throws SQLException {}
	@Override public void setPageSize(int size) throws SQLException {}
	@Override public int getPageSize() { return 0; }
	@Override public boolean nextPage() throws SQLException { return false; }
	@Override public boolean previousPage() throws SQLException { return false; }

	
	
}
