package com.timestored.babeldb;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import com.google.common.base.Preconditions;

/**
 * ResultSet wrapper that deletes (drops) columns by name.
 * 
 * Do not use directly unless necessary — prefer DBHelper.deleteColumns.
 */
final class DeletingColumnResultSet extends BaseResultSet {

    private final ResultSet delegate;
    private final ResultSetMetaData metaData;
    private final List<String> columnNames;
    private final int[] indexMap; // logical -> physical

    static ResultSet deleteColumns(ResultSet rs, Collection<String> columnsToDrop)
            throws SQLException {

        Preconditions.checkNotNull(rs, "ResultSet cannot be null");
        Preconditions.checkNotNull(columnsToDrop, "columnsToDrop cannot be null");

        if (columnsToDrop.isEmpty()) {
            return rs;
        }
        return new DeletingColumnResultSet(rs, columnsToDrop);
    }

    DeletingColumnResultSet(ResultSet rs, Collection<String> columnsToDrop)
            throws SQLException {

        this.delegate = rs;

        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();

        // Collect original column names
        Map<String, Integer> originalIndex = new LinkedHashMap<>();
        for (int i = 1; i <= colCount; i++) {
            originalIndex.put(md.getColumnLabel(i), i);
        }

        // Validate drop set
        for (String col : columnsToDrop) {
            if (!originalIndex.containsKey(col)) {
                throw new IllegalArgumentException("Cannot drop unknown column: " + col);
            }
        }

        List<Integer> idxs = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Integer> types = new ArrayList<>();

        for (Map.Entry<String, Integer> e : originalIndex.entrySet()) {
            if (!columnsToDrop.contains(e.getKey())) {
                idxs.add(e.getValue());
                names.add(e.getKey());
                types.add(md.getColumnType(e.getValue()));
            }
        }

        if (idxs.isEmpty()) {
            throw new IllegalArgumentException("Cannot drop all columns");
        }

        this.indexMap = idxs.stream().mapToInt(i -> i).toArray();
        this.columnNames = Collections.unmodifiableList(names);
        this.metaData = new SimpleResultSetMetaData(names, types);
    }

    // ---------- cursor movement ----------

    @Override public void beforeFirst() throws SQLException { delegate.beforeFirst(); }
    @Override public boolean next() throws SQLException { return delegate.next(); }
    @Override public boolean absolute(int row) throws SQLException { return delegate.absolute(row); }
    @Override public int getRow() throws SQLException { return delegate.getRow(); }

    @Override public boolean isBeforeFirst() throws SQLException { return delegate.isBeforeFirst(); }
    @Override public boolean isAfterLast() throws SQLException { return delegate.isAfterLast(); }
    @Override public boolean isFirst() throws SQLException { return delegate.isFirst(); }
    @Override public boolean isLast() throws SQLException { return delegate.isLast(); }
    @Override public void afterLast() throws SQLException { delegate.afterLast(); }
    @Override public boolean first() throws SQLException { return delegate.first(); }
    @Override public boolean last() throws SQLException { return delegate.last(); }
    @Override public boolean previous() throws SQLException { return delegate.previous(); }

    // ---------- data access ----------

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        if (columnIndex < 1 || columnIndex > indexMap.length) {
            throw new SQLException("Invalid column index: " + columnIndex);
        }
        return delegate.getObject(indexMap[columnIndex - 1]);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        int idx = columnNames.indexOf(columnLabel);
        if (idx < 0) {
            throw new SQLException("Unknown column: " + columnLabel);
        }
        return delegate.getObject(indexMap[idx]);
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map)
            throws SQLException {
        if (columnIndex < 1 || columnIndex > indexMap.length) {
            throw new SQLException("Invalid column index: " + columnIndex);
        }
        return delegate.getObject(indexMap[columnIndex - 1], map);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        int idx = columnNames.indexOf(columnLabel);
        if (idx >= 0) {
            return idx + 1;
        }
        throw new SQLException("Unknown column: " + columnLabel);
    }

    @Override
    public boolean wasNull() throws SQLException {
        return delegate.wasNull();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return metaData;
    }
}
