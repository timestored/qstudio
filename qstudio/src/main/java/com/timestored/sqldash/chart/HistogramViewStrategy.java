package com.timestored.sqldash.chart;

import static com.timestored.sqldash.chart.KdbFunctions.cos;
import static com.timestored.sqldash.chart.KdbFunctions.mul;
import static com.timestored.sqldash.chart.KdbFunctions.til;

import java.awt.Color;
import java.awt.Component;
import java.sql.ResultSet;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.statistics.HistogramDataset;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.timestored.connections.JdbcTypes;
import com.timestored.sqldash.chart.ChartResultSet.NumericCol;
import com.timestored.sqldash.theme.DBIcons;
import com.timestored.theme.Icon;


/**
 * Strategy for displaying {@link ResultSet}'s as a Histogram. 
 * See {@link #getFormatExplainationHtml()} for details.
 */
public enum HistogramViewStrategy implements ViewStrategy {

	INSTANCE;
	
	public static final int NUMBER_BINS = 100;
	private static final String KDB_QUERY = "([] Returns:cos 0.0015*til 500; Losses:cos 0.002*til 500)";
	private static final String DUCKDB_QUERY = "WITH b AS (\r\nSELECT COS(0.0015*i) AS Returns,COS(0.002*i) AS Losses\r\nFROM generate_series(0,499)t(i))\r\nSELECT * FROM b";
	private static final String FORMAT = "Each Numeric column represents a separate series in the histogram." +
	" The series values are placed into buckets and their frquency tallied.";

	@Override public UpdateableView getView(ChartTheme theme) {
		return getView(theme, null);
	}
	
	@Override public UpdateableView getView(ChartTheme theme, ChartAppearanceConfig appearanceConfig) {
	    return new HistogramUpdateableView(theme, appearanceConfig);
	}
	
	@Override public boolean supportsAppearanceConfig() { return true; }

	@Override public String getDescription() { return "Histogram"; }

	@Override
	public List<ExampleView> getExamples() {
		String description = "Distribution of Returns and Losses";
		String name = "Profit Distribution";
		
		String[] colNames = new String[] { "Returns", "Losses"};
		double[] returns = cos(mul(til(500), 0.0015));
		double[] losses = cos(mul(til(500), 0.002));
		Object[] colValues = new Object[] { returns, losses };
		ResultSet resultSet = new SimpleResultSet(colNames, colValues);
		
		TestCase testCase = new TestCase(name, resultSet, KDB_QUERY, DUCKDB_QUERY);
		return ImmutableList.of(new ExampleView(name , description, testCase));
	}

	@Override public String getFormatExplainationHtml() { return FORMAT; }
	@Override public String getFormatExplaination() { return FORMAT;	}

	@Override public String getQueryEg(JdbcTypes jdbcType) { 
		return jdbcType.equals(JdbcTypes.KDB) ? KDB_QUERY : null; 
	}

	@Override public Icon getIcon() { return DBIcons.CHART_HISTOGRAM; }
	
	private static class HistogramUpdateableView implements UpdateableView {

		private final ChartPanel chartPanel;
		private final ChartTheme theme;
		private final ChartAppearanceConfig appearanceConfig;
		private final JFreeChart chart;

		public HistogramUpdateableView(ChartTheme theme, ChartAppearanceConfig appearanceConfig) {

			Preconditions.checkNotNull(theme);
			this.theme = theme;
			this.appearanceConfig = appearanceConfig;
			
			String chartTitle = (appearanceConfig != null && appearanceConfig.getChartTitle() != null) 
					? appearanceConfig.getChartTitle() : "";
			boolean showTooltips = (appearanceConfig == null) || appearanceConfig.isTooltipEnabled();
			
			chart = ChartFactory.createHistogram(chartTitle, 
					null, "Frequency", null, PlotOrientation.VERTICAL, true, showTooltips, false);
			
			if (showTooltips) {
				chart.getXYPlot().getRenderer().setBaseToolTipGenerator(Tooltip.getXYNumbersGenerator());
			}
			
			// Apply theme first
			JFreeChart themedChart = theme.apply(chart);
			
			// Then apply chart-level config
			ChartConfigApplier.applyConfig(themedChart, appearanceConfig);
			
			chartPanel = new ChartPanel(themedChart, false, showTooltips, true, false, true);
		}
		
		@Override public void update(ResultSet rs, ChartResultSet chartRS) throws ChartFormatException {

	        if(chartRS.getNumericColumns().size() < 1) {
	        	throw new ChartFormatException("There must be atleast one number column.");
	        }
			
			HistogramDataset dataset = new HistogramDataset();
			for (NumericCol numCol : chartRS.getNumericColumns()) {
				String seriesName = numCol.getLabel();
				
				// Check visibility
				boolean isVisible = (appearanceConfig == null) || appearanceConfig.isSeriesVisible(seriesName);
				if (!isVisible) {
					continue; // Skip hidden series
				}
				
				dataset.addSeries(seriesName, numCol.getDoubles(), NUMBER_BINS);
			}
			
			XYPlot xyplot = ((XYPlot) chartPanel.getChart().getPlot());
			xyplot.setDataset(dataset);
			
			// Apply domain axis label from config or data
			if (appearanceConfig != null && appearanceConfig.getDomainAxisLabel() != null 
					&& !appearanceConfig.getDomainAxisLabel().isEmpty()) {
				xyplot.getDomainAxis().setLabel(appearanceConfig.getDomainAxisLabel());
			} else {
				xyplot.getDomainAxis().setLabel(chartRS.getRowTitle());
			}
			
			// Apply range axis label from config
			if (appearanceConfig != null && appearanceConfig.getRangeAxisLabel() != null 
					&& !appearanceConfig.getRangeAxisLabel().isEmpty()) {
				xyplot.getRangeAxis().setLabel(appearanceConfig.getRangeAxisLabel());
			}
			
			// Apply series colors
			XYItemRenderer renderer = xyplot.getRenderer();
			if(appearanceConfig != null) {
				for(int i = 0; i < dataset.getSeriesCount(); i++) {
					String seriesName = (String) dataset.getSeriesKey(i);
					Color color = appearanceConfig.getSeriesColor(seriesName);
					if(color != null) {
						renderer.setSeriesPaint(i, color);
					}
				}
			}
		}

		@Override public Component getComponent() {
			return chartPanel;
		}

		
	}

	@Override public String toString() {
		return HistogramViewStrategy.class.getSimpleName() + "[" + getDescription() + "]";
	}
	
	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int numColumnCount) {
		return rowCount < 211_000; // 1 seconds on Ryans PC
	}
	@Override public String getPulseName() { return "histogram"; }
}
