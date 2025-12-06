package com.timestored.sqldash.chart;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import com.timestored.connections.JdbcTypes;
import com.timestored.theme.Icon;
import com.timestored.theme.Theme;


public enum NoRedrawViewStrategy implements ViewStrategy {
	
	INSTANCE;

	private static final String DESC = "Do not draw anything.<br /> This selection does nothing and is for use" +
			" by those who do not want a chart, allowing quicker result drawing times.";
		
	@Override public UpdateableView getView(ChartTheme theme) {
		return new UpdateableView() {
			
			@Override public void update(ResultSet rs, ChartResultSet chartResultSet) 
					throws ChartFormatException {
				// intentionally empty.
			}
			
			@Override public Component getComponent() {
				JPanel p = new JPanel(new BorderLayout());
				p.add(Theme.getHeader("No Chart Drawing"), BorderLayout.NORTH);
				p.add(Theme.getHtmlText(DESC), BorderLayout.CENTER);
				p.setPreferredSize(new Dimension(150, 40));
				return p;
			}
		};
	}

	@Override public String getDescription() { return "No Chart"; }

	@Override public String getFormatExplainationHtml() { return DESC; }
	@Override public String getFormatExplaination() { return DESC; }

	@Override public Icon getIcon() { return null; }
	@Override public String getQueryEg(JdbcTypes jdbcType) { return null; }

	@Override public List<ExampleView> getExamples() {
		return Collections.emptyList();
	}

	@Override public String toString() { return getDescription(); }
	
	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int numColumnCount) {
		return true;
	}

	@Override public String getPulseName() { return null; }
}
