package com.timestored.sqldash.chart;

import java.awt.Color;
import java.awt.Component;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.jcip.annotations.Immutable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.Log;

import com.google.common.base.Preconditions;
import com.timestored.connections.JdbcTypes;
import com.timestored.sqldash.chart.ChartAppearanceConfig.AxisPosition;
import com.timestored.sqldash.theme.DBIcons;
import com.timestored.theme.Icon;



/**
 * Strategy for displaying {@link ResultSet}'s as bar charts. 
 * See {@link #getFormatExplainationHtml()} for details.
 */
@Immutable public class StepChartViewStrategy implements ViewStrategy {

	public static final ViewStrategy INSTANCE = new StepChartViewStrategy();

	@Override public UpdateableView getView(final ChartTheme theme) {
		return getView(theme, null);
	}

	@Override public UpdateableView getView(final ChartTheme theme, final ChartAppearanceConfig appearanceConfig) {
		Preconditions.checkNotNull(theme);
		
		return new HardRefreshUpdateableView(new HardRefreshUpdateableView.ViewGetter() {

			@Override public Component getView(ResultSet resultSet, ChartResultSet colResultSet) 
					throws ChartFormatException {

		        if(colResultSet == null) {
		        	throw new ChartFormatException("Could not create Result Set.");
		        }

		        XYDataset dataset = null;
		        TimeSeriesCollection leftDataset = new TimeSeriesCollection();
		        TimeSeriesCollection rightDataset = new TimeSeriesCollection();
		        List<String> seriesNames = new ArrayList<String>();
		        boolean isTimeSeries = false;
		        
		        try {
		        	TimeseriesViewStrategy.generateTimeSeriesWithConfig(colResultSet, appearanceConfig, leftDataset, rightDataset, seriesNames);
		        	dataset = leftDataset;
		        	isTimeSeries = true;
		        } catch(ChartFormatException cfe) {
		        	dataset = ScatterPlotViewStrategy.createXYDataset(colResultSet, appearanceConfig);
		        }
		        
		        String chartTitle = (appearanceConfig != null && appearanceConfig.getChartTitle() != null) 
						? appearanceConfig.getChartTitle() : "";
		        
				JFreeChart chart = ChartFactory.createXYStepChart(chartTitle, "", "", dataset,
						PlotOrientation.VERTICAL, theme.showChartLegend(), true, false);
				
				XYPlot plot = chart.getXYPlot();
				
				// Handle right axis if there are series assigned to it
				if(isTimeSeries && rightDataset.getSeriesCount() > 0) {
					NumberAxis rightAxis = new NumberAxis("Value (Right)");
					rightAxis.setAutoRangeIncludesZero(false);
					plot.setRangeAxis(1, rightAxis);
					plot.setDataset(1, rightDataset);
					plot.mapDatasetToRangeAxis(1, 1);
					
					// Create renderer for right axis dataset
					XYLineAndShapeRenderer rightRenderer = new XYLineAndShapeRenderer(true, false);
					plot.setRenderer(1, rightRenderer);
					TimeseriesViewStrategy.setTimeTooltipRenderer(colResultSet, rightRenderer);
					
					// Apply colors to right axis series
					if(appearanceConfig != null) {
						for(int i = 0; i < rightDataset.getSeriesCount(); i++) {
							String seriesName = (String) rightDataset.getSeriesKey(i);
							Color color = appearanceConfig.getSeriesColor(seriesName);
							if(color != null) {
								rightRenderer.setSeriesPaint(i, color);
							}
						}
					}
				}
				
				// StepChart by default starts at 0. Set it to auto range.
				try {
					if(chart.getPlot() instanceof XYPlot) {
						if(chart.getPlot() instanceof XYPlot) {
							XYPlot xyPlot = (XYPlot) chart.getPlot();
							if(xyPlot.getRangeAxis() instanceof NumberAxis) {
								NumberAxis na = ((NumberAxis) xyPlot.getRangeAxis());
								na.setAxisLineVisible(false);
								na.setAutoRangeIncludesZero(false);
								// Plotting of very small numbers (common in FX trading) wasn't showing any axis labels.
								// Workaround for small numbers (common in FX trading) not showing axis labels.
								// Clear user feedback on a number of ranges will be required.
								// Based on workaround from jfree forum: https://www.jfree.org/forum/viewtopic.php?t=26056
								if(na.getRange().getLength() < 0.01) {
									na.setStandardTickUnits(new StandardTickUnitSource());
									DecimalFormat df=new DecimalFormat();
									df.applyPattern("##0.#######");
									na.setNumberFormatOverride(df);
								}
							}
						}
					}
				} catch(RuntimeException e) {
					Log.debug(e);
				}
				
				// Check tooltip setting
				boolean showTooltips = (appearanceConfig == null) || appearanceConfig.isTooltipEnabled();
				
				// Apply theme first, then override with appearance config.
				// IMPORTANT: theme.apply() resets series colors to theme defaults,
				// so user-configured colors must be re-applied AFTER theming.
				JFreeChart themedChart = theme.apply(chart);
				
				// Re-apply user-configured colors after theme has been applied
				if(appearanceConfig != null && isTimeSeries) {
					XYPlot themedPlot = themedChart.getXYPlot();
					XYItemRenderer themedLeftRenderer = themedPlot.getRenderer();
					for(int i = 0; i < leftDataset.getSeriesCount(); i++) {
						String seriesName = (String) leftDataset.getSeriesKey(i);
						Color color = appearanceConfig.getSeriesColor(seriesName);
						if(color != null) {
							themedLeftRenderer.setSeriesPaint(i, color);
						}
					}
					
					// Apply per-series line and shape settings
					if (themedLeftRenderer instanceof XYLineAndShapeRenderer) {
						ChartConfigApplier.applyXYSeriesSettings(
								(XYLineAndShapeRenderer) themedLeftRenderer, leftDataset, appearanceConfig);
					}
					
					// Re-apply right axis colors if present
					if(rightDataset.getSeriesCount() > 0) {
						XYItemRenderer themedRightRenderer = themedPlot.getRenderer(1);
						if(themedRightRenderer != null) {
							for(int i = 0; i < rightDataset.getSeriesCount(); i++) {
								String seriesName = (String) rightDataset.getSeriesKey(i);
								Color color = appearanceConfig.getSeriesColor(seriesName);
								if(color != null) {
									themedRightRenderer.setSeriesPaint(i, color);
								}
							}
							// Apply per-series line and shape settings to right axis
							if (themedRightRenderer instanceof XYLineAndShapeRenderer) {
								ChartConfigApplier.applyXYSeriesSettings(
										(XYLineAndShapeRenderer) themedRightRenderer, rightDataset, appearanceConfig);
							}
						}
					}
				}
				
				// Apply all chart-level config settings
				ChartConfigApplier.applyConfig(themedChart, appearanceConfig);
				
				if(colResultSet.getTimeCol() != null && showTooltips) {
					XYItemRenderer renderer = themedChart.getXYPlot().getRenderer();
					TimeseriesViewStrategy.setTimeTooltipRenderer(colResultSet, renderer);
				}
				
				return new ChartPanel(themedChart, false, showTooltips, true, false, true);
			}
		});
		
	}
	
	@Override public boolean supportsAppearanceConfig() { return true; }

	@Override public String getDescription() { return "Step Plot"; }

	@Override public Icon getIcon() { return DBIcons.CHART_LINE; }
	
	@Override public String getQueryEg(JdbcTypes jdbcType) {
		return TimeseriesViewStrategy.INSTANCE.getQueryEg(jdbcType); 
	}
	

	@Override
	public String toString() {
		return ScatterPlotViewStrategy.class.getSimpleName() 
				+ "[" + getDescription() + "]";
	}

	@Override
	public List<ExampleView> getExamples() {
		return TimeseriesViewStrategy.INSTANCE.getExamples();	
	}

	@Override public String getFormatExplainationHtml() {
		return TimeseriesViewStrategy.INSTANCE.getFormatExplainationHtml();
	}
	
	@Override public String getFormatExplaination() {
		return TimeseriesViewStrategy.INSTANCE.getFormatExplaination();
	}

	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int columnCount) {
		return rowCount < 211_000; // 2 seconds on Ryans PC
	}
	
	@Override public String getPulseName() { return "timeseries"; }
}

