package com.timestored.babeldb;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import com.google.common.base.Preconditions;

/** Don't use this class directly unless absolutely necessary, use @link DBHelper#renameColumns instead.   */
final class ColumnRenamingResultSet extends BaseResultSet {

    private final ResultSet delegate;
    private final ResultSetMetaData metaData;
    private final List<String> columnNames;

	public static ResultSet renameColumns(ResultSet rs, List<String> newColumnNames) throws SQLException {

	    Preconditions.checkNotNull(rs, "ResultSet cannot be null");
	    Preconditions.checkNotNull(newColumnNames, "newColumnNames cannot be null");

	    if (newColumnNames.isEmpty()) {
	        return rs;
	    }

	    ResultSetMetaData md = rs.getMetaData();
	    int colCount = md.getColumnCount();

	    Preconditions.checkArgument(newColumnNames.size() <= colCount,
	            "Cannot rename %s columns; result set has only %s",
	            newColumnNames.size(), colCount );

	    Map<String, String> renameMap = new LinkedHashMap<>();
	    for (int i = 1; i <= newColumnNames.size(); i++) {
            renameMap.put(md.getColumnLabel(i), newColumnNames.get(i - 1));
	    }

	    return ColumnRenamingResultSet.renameColumns(rs, renameMap);
	}
    
	public static ResultSet renameColumns(ResultSet rs,Map<String, String> renameMap) throws SQLException {
	    Preconditions.checkNotNull(rs, "ResultSet cannot be null");
	    Preconditions.checkNotNull(renameMap, "renameMap cannot be null");
	    if (renameMap.isEmpty()) {
	        return rs;
	    }
	    return new ColumnRenamingResultSet(rs, renameMap);
	}
    
    ColumnRenamingResultSet(ResultSet rs, Map<String, String> renameMap) throws SQLException {
        this.delegate = rs;

        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();
        
        // Check that all keys in renameMap exist in original column names
        Set<String> originalNames = new HashSet<>();
        for (int i = 1; i <= colCount; i++) {
            originalNames.add(md.getColumnLabel(i));
        }
        for (String key : renameMap.keySet()) {
            if (!originalNames.contains(key)) {
                throw new IllegalArgumentException("Cannot rename unknown column: " + key);
            }
        }

        List<String> names = new ArrayList<>(colCount);
        List<Integer> types = new ArrayList<>(colCount);

        for (int i = 1; i <= colCount; i++) {
            String original = md.getColumnLabel(i);
            String renamed = renameMap.getOrDefault(original, original);
            names.add(renamed);
            types.add(md.getColumnType(i));
        }

        ensureUnique(names);

        this.columnNames = Collections.unmodifiableList(names);
        this.metaData = new SimpleResultSetMetaData(names, types);
    }


    private static void ensureUnique(List<String> names) {
        Set<String> seen = new HashSet<>();
        for (String n : names) {
            if (!seen.add(n)) {
                throw new IllegalArgumentException("Duplicate column name after renaming: " + n);
            }
        }
    }

    // ---------- cursor movement ----------

    @Override public void beforeFirst() throws SQLException { delegate.beforeFirst(); }
    @Override public boolean next() throws SQLException { return delegate.next(); }
    @Override public boolean absolute(int row) throws SQLException { return delegate.absolute(row); }
    @Override public int getRow() throws SQLException { return delegate.getRow(); }

    // ---------- data access ----------

    @Override public Object getObject(int columnIndex) throws SQLException {
        return delegate.getObject(columnIndex);
    }

    @Override public Object getObject(String columnLabel) throws SQLException {
        int idx = columnNames.indexOf(columnLabel);
        if (idx < 0) {
            throw new SQLException("Unknown column: " + columnLabel);
        }
        return delegate.getObject(idx + 1);
    }

    @Override public boolean wasNull() throws SQLException {
        return delegate.wasNull();
    }

    @Override public ResultSetMetaData getMetaData() throws SQLException {
        return metaData;
    }

    @Override public int findColumn(String columnLabel) throws SQLException {
        int idx = columnNames.indexOf(columnLabel);
        if (idx >= 0) return idx + 1;
        throw new SQLException("Unknown column: " + columnLabel);
    }
    @Override public boolean isBeforeFirst() throws SQLException {
    	return delegate.isBeforeFirst();
    }

    @Override public boolean isAfterLast() throws SQLException {
    	return delegate.isAfterLast();
    }

    @Override public boolean isFirst() throws SQLException {
    	return delegate.isFirst();
    }

    @Override public boolean isLast() throws SQLException {
    	return delegate.isLast();
    }

    @Override public void afterLast() throws SQLException {
    	delegate.afterLast();
	}

    @Override public boolean first() throws SQLException {
    	return delegate.first();
    }

    @Override public boolean last() throws SQLException {
    	return delegate.last();
    }

    @Override public boolean previous() throws SQLException {
    	return delegate.previous();
    }

    @Override public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
    	return delegate.getObject(columnIndex, map);
    }

}
