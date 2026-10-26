package com.timestored.babeldb;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 
 * Don't use this class directly unless absolutely necessary, use @link {@link DBHelper#sort(ResultSet, List) 
 * Read-only ResultSet view that presents rows sorted by selected columns.
 *
 * Designed to sit on top of a CachedRowSet and enforce ordering
 * required by PivotResultSet without copying or retyping data.
 */
final class SortedResultSet extends BaseResultSet {

	private final ResultSet delegate;
	private final List<RowKey> order;
	private int idx = -1;

	@Data private static final class RowKey {
		final int physicalRow;      // 0-based row index in delegate
		final Object[] sortKeys;
	}

	public SortedResultSet(ResultSet rs, List<String> sortColumns) throws SQLException {
		this.delegate = rs;
		this.order = new ArrayList<>();

		// Resolve sort column indices once
		int[] sortIdx = new int[sortColumns.size()];
		for (int i = 0; i < sortColumns.size(); i++) {
			sortIdx[i] = rs.findColumn(sortColumns.get(i));
		}

		// Capture sort keys only
		rs.beforeFirst();
		int row = 0;
		while (rs.next()) {
			Object[] keys = new Object[sortIdx.length];
			for (int i = 0; i < sortIdx.length; i++) {
				keys[i] = rs.getObject(sortIdx[i]);
			}
			order.add(new RowKey(row++, keys));
		}

		// Stable lexicographic sort
		Collections.sort(order, rowComparator());

		rs.beforeFirst();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Comparator<RowKey> rowComparator() {
		return (a, b) -> {
			for (int i = 0; i < a.sortKeys.length; i++) {
				Object x = a.sortKeys[i];
				Object y = b.sortKeys[i];

				if (x == y) continue;
				if (x == null) return -1;
				if (y == null) return 1;

				if (!(x instanceof Comparable)) {
					throw new IllegalArgumentException(
						"Sort column values must be Comparable: " + x.getClass()
					);
				}

				int c = ((Comparable) x).compareTo(y);
				if (c != 0) return c;
			}
			return 0;
		};
	}

	// ---------------- cursor movement ----------------

	@Override public void beforeFirst() { idx = -1; }

	@Override public boolean next() {
		idx++;
		return idx < order.size();
	}

	@Override public boolean absolute(int row) {
		idx = row - 1;
		return idx >= 0 && idx < order.size();
	}

	@Override public int getRow() {
		return idx + 1;
	}

	@Override public boolean isBeforeFirst() {
		return idx < 0;
	}

	@Override public boolean isAfterLast() {
		return idx >= order.size();
	}

	@Override public boolean isFirst() {
		return idx == 0;
	}

	@Override public boolean isLast() {
		return idx == order.size() - 1;
	}

	// ---------------- delegation ----------------

	private void positionDelegate() throws SQLException {
		int physical = order.get(idx).physicalRow + 1; // JDBC is 1-based
		delegate.absolute(physical);
	}

	@Override public Object getObject(int columnIndex) throws SQLException {
		positionDelegate();
		return delegate.getObject(columnIndex);
	}

	@Override public Object getObject(String columnLabel) throws SQLException {
		positionDelegate();
		return delegate.getObject(columnLabel);
	}

	@Override public boolean wasNull() throws SQLException {
		return delegate.wasNull();
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return delegate.getMetaData();
	}
	
	@Override
	public int findColumn(String columnLabel) throws SQLException {
		return delegate.findColumn(columnLabel);
	}

	@Override
	public void afterLast() throws SQLException {
		idx = order.size();
	}

	@Override
	public boolean first() throws SQLException {
		if (order.isEmpty()) {
			return false;
		}
		idx = 0;
		return true;
	}

	@Override
	public boolean last() throws SQLException {
		if (order.isEmpty()) {
			return false;
		}
		idx = order.size() - 1;
		return true;
	}

	@Override
	public boolean previous() throws SQLException {
		idx--;
		return idx >= 0;
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		positionDelegate();
		return delegate.getObject(columnIndex, map);
	}

}
