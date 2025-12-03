package com.timestored.sqldash.chart;

import java.sql.ResultSet;

import net.jcip.annotations.Immutable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import com.timestored.sqldash.theme.DBIcons;



/**
 * Strategy for displaying {@link ResultSet}'s as bar charts. 
 * See {@link #getFormatExplainationHtml()} for details.
 */
@Immutable public class BarChartViewStrategy extends AbstractCategoryViewStrategy {

	public static final ViewStrategy INSTANCE = new BarChartViewStrategy();
	
	private BarChartViewStrategy() { super("Bar Chart", DBIcons.CHART_BAR); }
	
	@Override public UpdateableView getView(ChartTheme theme) {
		return getView(theme, null);
	}
	
	@Override public UpdateableView getView(ChartTheme theme, ChartAppearanceConfig appearanceConfig) {
		final JFreeChart chart = ChartFactory.createBarChart("", "", 
				"values", null, PlotOrientation.VERTICAL, theme.showChartLegend(), 
				true, false);
		return new CategoryDatasetUpdateableView(theme, chart, appearanceConfig);
	}
	
	@Override public boolean supportsAppearanceConfig() { return true; }
	
	@Override public String toString() {
		return BarChartViewStrategy.class.getSimpleName() + "[" + getDescription() + "]";
	}

	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int numColumnCount) {
		return (numColumnCount*rowCount) < 10_000; // 1.5 seconds on Ryans PC
	}
	@Override public String getPulseName() { return "bar"; }
}

