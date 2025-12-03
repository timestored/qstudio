package com.timestored.sqldash.chart;

import java.awt.BorderLayout;
import java.awt.Component;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import com.timestored.connections.JdbcTypes;
import com.timestored.theme.Icon;
import com.timestored.theme.Theme;


/**
 * AUTO chart selection strategy that automatically picks the best chart type
 * based on the result set column structure.
 */
public enum NoRedrawViewStrategy implements ViewStrategy {
	
	INSTANCE;

	private static final String DESC = "Automatically selects the best chart type based on your data.<br/>" +
			"Analyzes column types (date/time, string, numeric) to pick an appropriate visualization.";
	
	private static final Set<String> CANDLESTICK_COLS = new HashSet<String>();
	static {
		CANDLESTICK_COLS.add("high");
		CANDLESTICK_COLS.add("low");
		CANDLESTICK_COLS.add("h");
		CANDLESTICK_COLS.add("l");
	}
		
	@Override public UpdateableView getView(final ChartTheme theme) {
		return new AutoUpdateableView(theme, null);
	}
	
	/**
	 * Inner class that delegates to the appropriate chart strategy based on data analysis.
	 */
	private static class AutoUpdateableView implements UpdateableView {
		
		private final ChartTheme theme;
		private final ChartAppearanceConfig appearanceConfig;
		private UpdateableView delegateView;
		private JPanel containerPanel;
		
		AutoUpdateableView(ChartTheme theme, ChartAppearanceConfig appearanceConfig) {
			this.theme = theme;
			this.appearanceConfig = appearanceConfig;
			this.containerPanel = new JPanel(new BorderLayout());
			containerPanel.add(Theme.getHtmlText("Waiting for data..."), BorderLayout.CENTER);
		}
		
		@Override public void update(ResultSet rs, ChartResultSet chartResultSet) throws ChartFormatException {
			ViewStrategy selectedStrategy = selectStrategy(rs, chartResultSet);
			// Pass appearance config to the delegate if the strategy supports it
			if(selectedStrategy == null) {
				delegateView =null;
			} else if (selectedStrategy.supportsAppearanceConfig() && appearanceConfig != null) {
				delegateView = selectedStrategy.getView(theme, appearanceConfig);
			} else {
				delegateView = selectedStrategy.getView(theme);
			}

			containerPanel.removeAll();
			if(delegateView == null) {
				containerPanel.add(Theme.getHtmlText("Result is large. Choose specific chart to try charting."), BorderLayout.CENTER);
			} else {
				containerPanel.add(delegateView.getComponent(), BorderLayout.CENTER);
				delegateView.update(rs, chartResultSet);
			}
			
			containerPanel.revalidate();
			containerPanel.repaint();
		}
		
		@Override public Component getComponent() {
			return containerPanel;
		}
	}
	
	/**
	 * Selects the best chart strategy based on the result set structure.
	 * Rules are applied in priority order - first match wins.
	 */
	static ViewStrategy selectStrategy(ResultSet rs, ChartResultSet chartResultSet) {
		if (chartResultSet == null) {
			return DataTableViewStrategy.getInstance();
		}
		if(chartResultSet.getRowCount() > 100_000) {
			// For very large result sets, do not attempt auto charting
			return null;
		}

		
		try {
			// Analyze column structure
			rs.beforeFirst();
			ResultSetMetaData md = rs.getMetaData();
			int colCount = md.getColumnCount();
			
			int firstDateIdx = -1;
			int firstStringIdx = -1;
			int numericCount = 0;
			int stringCount = 0;
			int consecutiveNumericFromFirstString = 0;
			boolean hasHighLow = false;
			
			for (int c = 1; c <= colCount; c++) {
				int ctype = md.getColumnType(c);
				String ctypeName = md.getColumnTypeName(c);
				String colName = md.getColumnName(c).toLowerCase();
				
				boolean isNumeric = SqlHelper.isNumeric(ctype, ctypeName);
				boolean isTemporal = SqlHelper.isTemporal(ctype, ctypeName);
				boolean isStringy = !isNumeric && !isTemporal;
				
				if (isTemporal && firstDateIdx == -1) {
					firstDateIdx = c;
				}
				if (isStringy && firstStringIdx == -1) {
					firstStringIdx = c;
				}
				if (isNumeric) {
					numericCount++;
				}
				if (isStringy) {
					stringCount++;
				}
				
				// Check for candlestick columns
				if (CANDLESTICK_COLS.contains(colName)) {
					hasHighLow = true;
				}
			}
			
			// Count consecutive numerics after first string column ends
			if (firstStringIdx > 0) {
				boolean foundNonString = false;
				for (int c = firstStringIdx; c <= colCount; c++) {
					int ctype = md.getColumnType(c);
					String ctypeName = md.getColumnTypeName(c);
					boolean isNumeric = SqlHelper.isNumeric(ctype, ctypeName);
					boolean isTemporal = SqlHelper.isTemporal(ctype, ctypeName);
					boolean isStringy = !isNumeric && !isTemporal;
					
					if (!isStringy) {
						foundNonString = true;
					}
					if (foundNonString && isNumeric) {
						consecutiveNumericFromFirstString++;
					} else if (foundNonString && !isNumeric) {
						break;
					}
				}
			}
			
			// Rule 1: Candlestick - check for high/low columns (highest priority)
			if (hasHighLow && chartResultSet.getTimeCol() != null) {
				return CandleStickViewStrategy.INSTANCE;
			}
			
			// Rule 2: Time-Based Charts - date/time column appears before any string column
			if (firstDateIdx > 0 && (firstStringIdx == -1 || firstDateIdx < firstStringIdx)) {
				return TimeseriesViewStrategy.INSTANCE;
			}
			
			// Rule 3: Bubble Chart - string columns followed by exactly 3 numeric columns
			if (stringCount > 0 && consecutiveNumericFromFirstString == 3) {
				return BubbleChartViewStrategy.INSTANCE;
			}
			
			// Rule 4: Scatter Plot - no dates, first non-string is numeric, at least 2 numeric columns
			if (firstDateIdx == -1 && numericCount >= 2) {
				// Check if first non-string column is numeric
				for (int c = 1; c <= colCount; c++) {
					int ctype = md.getColumnType(c);
					String ctypeName = md.getColumnTypeName(c);
					boolean isNumeric = SqlHelper.isNumeric(ctype, ctypeName);
					boolean isTemporal = SqlHelper.isTemporal(ctype, ctypeName);
					boolean isStringy = !isNumeric && !isTemporal;
					
					if (!isStringy) {
						if (isNumeric && stringCount == 0) {
							return ScatterPlotViewStrategy.INSTANCE;
						}
						break;
					}
				}
			}
			
			// Rule 5a: Pie Chart - string columns first, exactly 1 numeric column
			if (stringCount > 0 && numericCount == 1 && isStringFirst(md, colCount)) {
				return PieChartViewStrategy.INSTANCE;
			}
			
			// Rule 5b: Stacked Bar Chart - string columns first, 2 or more numeric columns
			if (stringCount > 0 && numericCount >= 2 && isStringFirst(md, colCount)) {
				return StackedBarChartViewStrategy.INSTANCE;
			}
			
			// Rule 6: Histogram - only numeric columns, no strings, no dates
			if (numericCount > 0 && stringCount == 0 && firstDateIdx == -1) {
				return HistogramViewStrategy.INSTANCE;
			}
			
		} catch (SQLException e) {
			// Fall through to default
		}
		
		// Rule 7: Data Table (Fallback)
		return DataTableViewStrategy.getInstance();
	}
	
	/** Checks if the first column is a string type column */
	private static boolean isStringFirst(ResultSetMetaData md, int colCount) throws SQLException {
		if (colCount == 0) return false;
		int ctype = md.getColumnType(1);
		String ctypeName = md.getColumnTypeName(1);
		return !SqlHelper.isNumeric(ctype, ctypeName) && !SqlHelper.isTemporal(ctype, ctypeName);
	}

	@Override public String getDescription() { return "Auto"; }

	@Override public String getFormatExplainationHtml() { return DESC; }
	@Override public String getFormatExplaination() { return DESC; }

	@Override public Icon getIcon() { return Theme.CIcon.CHART_CURVE_ADD; }
	@Override public String getQueryEg(JdbcTypes jdbcType) { return null; }

	@Override public List<ExampleView> getExamples() {
		return Collections.emptyList();
	}

	@Override public String toString() { return getDescription(); }
	
	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int numColumnCount) {
		return true;
	}

	@Override public String getPulseName() { return "auto"; }
	
	/**
	 * AUTO mode supports appearance configuration because it may delegate to 
	 * chart types (like TimeSeries, StackedBar) that support configuration.
	 * The config editor will be shown to allow users to customize the chart.
	 */
	@Override public boolean supportsAppearanceConfig() { return true; }
	
	@Override public UpdateableView getView(ChartTheme theme, ChartAppearanceConfig appearanceConfig) {
		return new AutoUpdateableView(theme, appearanceConfig);
	}
}
