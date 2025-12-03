package com.timestored.sqldash.chart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;

import com.google.common.collect.ImmutableList;
import com.timestored.connections.JdbcTypes;
import com.timestored.theme.Icon;
import com.timestored.theme.Theme.CIcon;

/**
 * Strategy for displaying {@link ResultSet}'s as plain table.
 */
public class DataTableViewStrategy implements ViewStrategy {

	private static final ViewStrategy INSTANCE = new DataTableViewStrategy(false);
	private final boolean debugView;

	private static final List<ExampleView> EXAMPLES;
	private static final TimeStringValuer TIME_STRINGVAL = new TimeStringValuer();
	private static final String FORMAT = "Any format of table is acceptable, " +
				"all rows/columns will be shown as a plain table.";
	
	static {
		ExampleView ev3 = new ExampleView("Many Columned Table", 
				"All rows/columns will be shown as a plain table.",
				ExampleTestCases.COUNTRY_STATS);
		EXAMPLES = ImmutableList.of(ev3);
	}
	
	
	private DataTableViewStrategy(boolean debugView) {
		this.debugView = debugView;
	}
	
	public static ViewStrategy getInstance() { return INSTANCE; }
	
	@Override public UpdateableView getView(ChartTheme theme) {
		return new DataTableUpdateableView(debugView);
	}

	@Override public String getDescription() {
		return (debugView ? "Debug Table" : "Data Table");
	}

	@Override public String getFormatExplainationHtml() { return FORMAT; }
	@Override public String getFormatExplaination() { return FORMAT;	}

	@Override public List<ExampleView> getExamples() { return EXAMPLES; }
	
	@Override public String getQueryEg(JdbcTypes jdbcType) {
		if(jdbcType.equals(JdbcTypes.KDB)) {
			return ExampleTestCases.COUNTRY_STATS.getKdbQuery();
		}
		return null; 
	}
	
	/**
	 * A view that displays {@link ResultSet} data as a fresh {@link JTable} each time new data arrives.
	 */
	private static class DataTableUpdateableView implements UpdateableView {
		private final JPanel p;
		private final boolean debugView;
		
	
		public DataTableUpdateableView(boolean debugView) {
			this.debugView = debugView;
			p = new JPanel(new BorderLayout());
		}
	
		@Override public void update(ResultSet rs, ChartResultSet chartResultSet) 
				throws ChartFormatException {
			try {
				p.removeAll();
				Component tbl = TableFactory.getTable(rs, Integer.MAX_VALUE, true);
				p.add(tbl, BorderLayout.CENTER);
				p.revalidate();
			} catch (SQLException e) {
				throw new ChartFormatException("Could not create ResultSet.");
			}
		}
	
		@Override public Component getComponent() {
			return p;
		}
	

		private static Color getColor(String cVal) {
			Color cl = null;
			try {
				try {
					Field f = Color.class.getField(cVal);
					cl = (Color) f.get(null);
				} catch (SecurityException e) { } 
				catch (NoSuchFieldException e) { } 
				catch (IllegalArgumentException e) { } 
				catch (IllegalAccessException e) { }

				if(cl == null) {
					cl = Color.decode(cVal);
				}
			} catch (NumberFormatException e) {
				// ignore
			}
			return cl;
		}
	
	}

	@Override public Icon getIcon() { return CIcon.TABLE_ELEMENT; }

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (debugView ? 1231 : 1237);
		return result;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataTableViewStrategy other = (DataTableViewStrategy) obj;
		if (debugView != other.debugView)
			return false;
		return true;
	}

	@Override public String toString() {
		return DataTableViewStrategy.class.getSimpleName() + "[" + getDescription() + "]";
	}

	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int numColumnCount) {
		return rowCount < 211_000; // 1 seconds on Ryans PC
	}
	
	@Override public String getPulseName() { return "grid"; }
	 
}