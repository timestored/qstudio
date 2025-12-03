package com.timestored.sqldash.chart;

import java.awt.Color;
import java.sql.ResultSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import com.timestored.sqldash.theme.DBIcons;
/**
 * Strategy for displaying {@link ResultSet}'s as area charts. 
 * See {@link #getFormatExplainationHtml()} for details.
 */
class AreaChartViewStrategy extends AbstractCategoryViewStrategy {

	public static final ViewStrategy INSTANCE = new AreaChartViewStrategy();
	
	private AreaChartViewStrategy() { super("Area Chart", DBIcons.CHART_AREA); }
	
	@Override public UpdateableView getView(ChartTheme theme) {
		return getView(theme, null);
	}
	
	@Override public UpdateableView getView(ChartTheme theme, ChartAppearanceConfig appearanceConfig) {
		JFreeChart chart = ChartFactory.createAreaChart("", "", "", null,
				PlotOrientation.VERTICAL, theme.showChartLegend(), true, false);
		chart.setBackgroundPaint(Color.white);
		chart.getPlot().setForegroundAlpha(0.8F);
		return new CategoryDatasetUpdateableView(theme, chart, appearanceConfig);
	}

	@Override public boolean supportsAppearanceConfig() { return true; }

	@Override public String toString() {
		return AreaChartViewStrategy.class.getSimpleName() + "[" + getDescription() + "]";
	}

	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int numColumnCount) {
		return (numColumnCount*rowCount) < 10_000; // 1.5 seconds on Ryans PC
	}
	
	@Override public String getPulseName() { return "area"; }
}