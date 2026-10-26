package com.timestored.sqldash.chart;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;

/**
 * Detects whether a ResultSet is a candidate for automatic time-series pivoting.
 * Only pivots when the data structure deterministically indicates a series dimension exists.
 */
class AutoPivotDetector {

	private static final int MAX_CARDINALITY = 50;

	/** Result of pivot detection analysis. */
	
	@Data static class PivotCandidate {
		private final String timeColumnName;
		private final String pivotColumnName;
		private final boolean shouldPivot;
		private final String reason;
		private final boolean wereManyPotentialPivotColumns;

		static PivotCandidate noPivot(String reason) { return new PivotCandidate(null, null, false, reason, false); }
		static PivotCandidate pivot(String timeColumnName, String pivotColumnName, String reason, boolean wereManyPotentialPivotCOlumns) { 
			return new PivotCandidate(timeColumnName, pivotColumnName, true, reason, wereManyPotentialPivotCOlumns); 
			}

		public boolean shouldPivot() { return shouldPivot; }
	}

	/**
	 * Analyze a ResultSet to determine if it should be auto-pivoted for time-series charting.
	 * 
	 * Rules:
	 * 1. Must have exactly one time column
	 * 2. Must have exactly one numeric column
	 * 3. A non-time, non-numeric column qualifies as a pivot candidate if:
	 *    - Its cardinality <= MAX_CARDINALITY
	 *    - Grouping by (time, column) produces no duplicate rows
	 * 4. If time column is totally unique (no overlaps), DON'T pivot
	 * 5. If exactly one column qualifies -> auto-select it
	 * 6. If zero or multiple columns qualify -> no auto-pivot
	 */
	static PivotCandidate detectPivotColumn(ResultSet rs) throws SQLException {
		rs.beforeFirst();
		ResultSetMetaData md = rs.getMetaData();
		int colCount = md.getColumnCount();

		// Identify column types
		List<String> timeColumns = new ArrayList<String>();
		List<String> numericColumns = new ArrayList<String>();
		List<String> otherColumns = new ArrayList<String>();

		for(int c=1; c<=colCount; c++) {
			int ctype = md.getColumnType(c);
			String ctypeName = md.getColumnTypeName(c);
			String colName = md.getColumnName(c);

			if(SqlHelper.isTemporal(ctype, ctypeName)) {
				timeColumns.add(colName);
			} else if(SqlHelper.isNumeric(ctype, ctypeName)) {
				numericColumns.add(colName);
			} else {
				otherColumns.add(colName);
			}
		}

		// Rule 1: Must have exactly one time column
		if(timeColumns.size() != 1) {
			return PivotCandidate.noPivot("Must have exactly one time column (found " + timeColumns.size() + ")");
		}

		// Rule 2: Must have exactly one numeric column
		if(numericColumns.size() != 1) {
			return PivotCandidate.noPivot("Must have exactly one numeric column (found " + numericColumns.size() + ")");
		}

		String timeCol = timeColumns.get(0);

		// Rule 4: If time column is totally unique (no overlaps on pivot), DON'T pivot
		if(isTimeColumnUnique(rs, timeCol)) {
			return PivotCandidate.noPivot("Time column is unique - no pivot needed");
		}

		// Rule 3: Find qualifying pivot candidates
		List<String> qualifyingColumns = new ArrayList<String>();
		for(String col : otherColumns) {
			if(isQualifyingPivotColumn(rs, timeCol, col)) {
				qualifyingColumns.add(col);
			}
		}

		// Rule 5 & 6: Auto-select if exactly one, otherwise no pivot
		if(qualifyingColumns.size() == 0) {
			return PivotCandidate.noPivot("No qualifying pivot columns found");
		}
		boolean manyColOptions = qualifyingColumns.size() > 1;
		String pvtCol = choosePivotColumn(qualifyingColumns);
		return PivotCandidate.pivot(timeCol, pvtCol, "Auto-selected pivot column: " + pvtCol, manyColOptions);
	}
	
	// Common finance / time-series series identifiers, in priority order
	private static final List<String> PREFERRED_PIVOT_NAMES = List.of(
		"sym", "symbol", "ticker", "ric", "isin", "instrument", "asset", "product",
		"currency", "ccy", "exchange", "mic", "market",
		"side", "bidask", "direction", "metric", "measure", "field", "venue", 
		"portfolio", "book", "account");

	private static String choosePivotColumn(List<String> qualifyingColumns) {
		for (String preferred : PREFERRED_PIVOT_NAMES) {
			for (String col : qualifyingColumns) {
				if (col.equalsIgnoreCase(preferred)) {
					return col;
				}
			}
		}
		return qualifyingColumns.get(0);
	}


	/**
	 * Check if a column qualifies as a pivot candidate:
	 * - Cardinality <= MAX_CARDINALITY
	 * - (time, column) combinations are unique (no time overlaps)
	 */
	private static boolean isQualifyingPivotColumn(ResultSet rs, String timeCol, String pivotCol) throws SQLException {
		rs.beforeFirst();

		Set<Object> uniqueValues = new HashSet<Object>();

		while(rs.next()) {
			Object timeVal = rs.getObject(timeCol);
			Object pivotVal = rs.getObject(pivotCol);

			uniqueValues.add(pivotVal);

			// Check cardinality constraint
			if(uniqueValues.size() > MAX_CARDINALITY) {
				return false;
			}
		}

		return uniqueValues.size() > 1; // Must have at least 2 distinct values to be useful
	}

	/**
	 * Check if the time column contains only unique values.
	 */
	private static boolean isTimeColumnUnique(ResultSet rs, String timeCol) throws SQLException {
		rs.beforeFirst();
		Set<Object> timeValues = new HashSet<Object>();

		while(rs.next()) {
			Object timeVal = rs.getObject(timeCol);
			if(timeValues.contains(timeVal)) {
				return false; // Found duplicate
			}
			timeValues.add(timeVal);
		}

		return true;
	}
}