package com.timestored.sqldash.chart;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

/**
 * Used to construct rough {@link ResultSet}'s for testing / documentation  purposes.
 */
public class SimpleResultSet extends BaseResultSet {
	
	private final String[] colNames;
	private final Object[] colValues;
	private int idx = 0;
	private final ResultSetMetaData resultSetMetaData;
	private boolean wasNull;

	/** Construct an empty resultSet where only the column names are known. */
	public SimpleResultSet(String[] colNames) {
		this(colNames, new Object[]{});
	}
	
	public SimpleResultSet(String[] colNames, Object[] colValues) {
		
		this.colNames = colNames;
		this.colValues = colValues;
		
		// if no rows, assume type is varchar
		int[] types = null;
		if(colValues.length == 0) {
			types = new int[colNames.length];
			Arrays.fill(types, java.sql.Types.VARCHAR);
		} else {
			types = getTypes(colValues);
		}
		
		
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
		} else if(o instanceof Integer[]) {
			return java.sql.Types.INTEGER;
		} else if(o instanceof long[]) {
			return java.sql.Types.BIGINT;
		} else if(o instanceof float[]) {
			return java.sql.Types.REAL;
		} else if(o instanceof double[]) {
			return java.sql.Types.DOUBLE;
		} else if(o instanceof java.sql.Date[]) {
			return java.sql.Types.DATE;
		} else if(o instanceof java.sql.Time[]) {
			return java.sql.Types.TIME;
		} else if(o instanceof java.sql.Timestamp[]) {
			return java.sql.Types.TIMESTAMP;
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

	@Override
	public boolean absolute(int row) throws SQLException {
		idx = row-1;
		return true;
	}


	@Override
	public void beforeFirst() throws SQLException {
		idx = -1;
	}

	@Override public void afterLast() throws SQLException {
		if(colValues.length > 0) {
			idx = (Array.getLength(colValues[0])) + 1;
		}
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		
		for(int i=0; i<colNames.length; i++) {
			if(colNames[i].equals(columnLabel)) {
				return i+1;
			}
		}
		throw new SQLException();
	}

	@Override
	public boolean first() throws SQLException {
		idx = 0;
		return true;
	}


	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return resultSetMetaData;
	}


	@Override public Object getObject(int columnIndex) throws SQLException {
		Object o =  Array.get(colValues[columnIndex-1], idx);
		wasNull = o == null;
		return o;
	}

	@Override public boolean wasNull() throws SQLException { return wasNull; }
	
	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map)
			throws SQLException {
		throw new UnsupportedOperationException();
	}


	@Override
	public int getRow() throws SQLException {
		return idx;
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		return idx >= colNames.length;
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		return idx < 0;
	}


	@Override
	public boolean isFirst() throws SQLException {
		return idx == 0;
	}

	@Override
	public boolean isLast() throws SQLException {
		return idx == (colNames.length - 1);
	}

	@Override
	public boolean last() throws SQLException {
		if(colValues.length > 0) {
			idx = (Array.getLength(colValues[0]));
			return true;
		}
		return false;
	}


	@Override
	public boolean next() throws SQLException {
		idx++;
		return colValues.length>0 && idx < Array.getLength(colValues[0]);
	}

	@Override
	public boolean previous() throws SQLException {
		idx--;
		return idx >= 0;
	}

	
}
