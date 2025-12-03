package com.timestored.sqldash.chart;

import static com.timestored.sqldash.chart.KdbFunctions.cos;
import static com.timestored.sqldash.chart.KdbFunctions.mul;
import static com.timestored.sqldash.chart.KdbFunctions.sin;
import static com.timestored.sqldash.chart.KdbFunctions.til;

import java.awt.Color;
import java.awt.Component;
import java.sql.Date;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.timestored.connections.JdbcTypes;
import com.timestored.sqldash.chart.ChartAppearanceConfig.AxisPosition;
import com.timestored.sqldash.chart.ChartResultSet.NumericCol;
import com.timestored.sqldash.chart.ChartResultSet.TimeCol;
import com.timestored.theme.Icon;
import com.timestored.theme.Theme.CIcon;


/**
 * Strategy for displaying {@link ResultSet}'s as a time series.
 * See {@link #getFormatExplainationHtml()} for details.
 */
public enum TimeseriesViewStrategy implements ViewStrategy {

	INSTANCE;

	private static final String KDB_QUERY = "([] dt:2013.01.01+til 21; cosineWave:cos a; \r\n\t sineWave:sin a:0.6*til 21)";
	private static final String DUCKDB_QUERY = "WITH b AS (SELECT DATE '2013-01-01'+INTERVAL '1 day'*i dt,i FROM generate_series(0,20)t(i))\r\nSELECT dt,COS(i) cosineWave,SIN(0.6*i) sineWave FROM b";
	private static final String TOOLTIP_FORMAT = "<html><b>{0}:</b><br>{1}<br>{2}</html>";
	private static final DecimalFormat DEC_FORMAT = new DecimalFormat("#,###.##");

	
	@Override
	public 	UpdateableView getView(final ChartTheme theme) {
		return getView(theme, null);
	}
	
	@Override
	public 	UpdateableView getView(final ChartTheme theme, final ChartAppearanceConfig appearanceConfig) {

		return new HardRefreshUpdateableView(new HardRefreshUpdateableView.ViewGetter() {

			@Override public Component getView(ResultSet rs, ChartResultSet colResultSet) 
					throws ChartFormatException {

				TimeSeriesCollection leftDataset = new TimeSeriesCollection();
				TimeSeriesCollection rightDataset = new TimeSeriesCollection();
				List<String> seriesNames = new ArrayList<String>();
				
				generateTimeSeriesWithConfig(colResultSet, appearanceConfig, leftDataset, rightDataset, seriesNames);
				
				String chartTitle = (appearanceConfig != null && appearanceConfig.getChartTitle() != null) 
						? appearanceConfig.getChartTitle() : "";
				
				// Check tooltip setting
				boolean showTooltips = (appearanceConfig == null) || appearanceConfig.isTooltipEnabled();

				JFreeChart chart = ChartFactory.createTimeSeriesChart(
						chartTitle, "Time", "Value", leftDataset, true, showTooltips, false);
				
				XYPlot plot = chart.getXYPlot();
				
				// Handle right axis if there are series assigned to it
				if(rightDataset.getSeriesCount() > 0) {
					NumberAxis rightAxis = new NumberAxis("Value (Right)");
					rightAxis.setAutoRangeIncludesZero(false);
					plot.setRangeAxis(1, rightAxis);
					plot.setDataset(1, rightDataset);
					plot.mapDatasetToRangeAxis(1, 1);
					
					// Create renderer for right axis dataset
					XYLineAndShapeRenderer rightRenderer = new XYLineAndShapeRenderer(true, false);
					plot.setRenderer(1, rightRenderer);
					if (showTooltips) {
						setTimeTooltipRenderer(colResultSet, rightRenderer);
					}
					
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
				
				// Apply colors to left axis series
				XYItemRenderer leftRenderer = plot.getRenderer();
				if(appearanceConfig != null) {
					for(int i = 0; i < leftDataset.getSeriesCount(); i++) {
						String seriesName = (String) leftDataset.getSeriesKey(i);
						Color color = appearanceConfig.getSeriesColor(seriesName);
						if(color != null) {
							leftRenderer.setSeriesPaint(i, color);
						}
					}
				}
				
				// Apply theme first, then override with appearance config.
				// IMPORTANT: theme.apply() resets series colors to theme defaults,
				// so user-configured colors must be re-applied AFTER theming.
				JFreeChart themedChart = theme.apply(chart);
				
				// Re-apply user-configured colors after theme has been applied
				XYPlot themedPlot = themedChart.getXYPlot();
				XYItemRenderer themedLeftRenderer = themedPlot.getRenderer();
				if(appearanceConfig != null) {
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
				
				// Apply all chart-level config settings (background, gridlines, legend, etc.)
				ChartConfigApplier.applyConfig(themedChart, appearanceConfig);
				
				ChartPanel cp = new ChartPanel(themedChart, false, showTooltips, true, false, true);
				if (showTooltips) {
					setTimeTooltipRenderer(colResultSet, themedLeftRenderer);
				}
				
				return cp;
			}
				
		});
			
	}
	
	@Override public boolean supportsAppearanceConfig() { return true; }


	static void setTimeTooltipRenderer(ChartResultSet colResultSet, XYItemRenderer renderer) {
		// once we have data set the tooltip appropriately
		// set tooltip specific to date type
		TimeCol timeCol = colResultSet.getTimeCol();
		if(timeCol != null) {
			SimpleDateFormat dateFormat = getDateFormat(timeCol.getType());
			if(dateFormat != null) {
				StandardXYToolTipGenerator ttg;
				ttg = new StandardXYToolTipGenerator(TOOLTIP_FORMAT, dateFormat, DEC_FORMAT);
				renderer.setBaseToolTipGenerator(ttg);
			}
		}
	}
	
	static TimeSeriesCollection generateTimeSeries(ChartResultSet colResultSet) throws ChartFormatException {
		TimeSeriesCollection leftDataset = new TimeSeriesCollection();
		TimeSeriesCollection rightDataset = new TimeSeriesCollection();
		List<String> seriesNames = new ArrayList<String>();
		generateTimeSeriesWithConfig(colResultSet, null, leftDataset, rightDataset, seriesNames);
		return leftDataset;
	}
	
	/**
	 * Generate time series data and split into left/right datasets based on configuration.
	 * @param colResultSet The chart result set
	 * @param config Optional appearance configuration
	 * @param leftDataset Dataset for left axis (will be populated)
	 * @param rightDataset Dataset for right axis (will be populated)
	 * @param seriesNames List to be populated with series names in order
	 */
	static void generateTimeSeriesWithConfig(ChartResultSet colResultSet, ChartAppearanceConfig config, 
			TimeSeriesCollection leftDataset, TimeSeriesCollection rightDataset, List<String> seriesNames) throws ChartFormatException {

		if(colResultSet==null) {
			throw new ChartFormatException("Could not create chart result set.");
		}
		
		TimeCol timeCol = colResultSet.getTimeCol();
		if(timeCol==null) {
			throw new ChartFormatException("No Time Column Found.");
		}
			
		// time series chart
		if(timeCol != null) {
			RegularTimePeriod[] timePeriods = null;
			try {
				timePeriods = timeCol.getRegularTimePeriods();
			} catch(IllegalArgumentException iae) {
				throw new ChartFormatException(iae.toString());
			}
	    	// create time series for each column
	    	for(NumericCol nc : colResultSet.getNumericColumns()) {
	    		String seriesName = nc.getLabel();
	    		
	    		// Check if this series should be visible
	    		boolean isVisible = (config == null) || config.isSeriesVisible(seriesName);
	    		if(!isVisible) {
	    			continue; // Skip hidden series
	    		}
	    		
    	    	TimeSeries tSeries = new TimeSeries(seriesName);
    	    	int row = 0;
		        for(double d : nc.getDoubles()) {
		        	if(!Double.isNaN(d)) {
		        		tSeries.addOrUpdate(timePeriods[row], d);
		        	}
		        	row++;
		        }
	    		if(!tSeries.isEmpty()) {
	    			seriesNames.add(seriesName);
	    			// Check if this series should go on the right axis
	    			AxisPosition axisPos = (config != null) ? config.getSeriesAxisPosition(seriesName) : AxisPosition.LEFT;
	    			if(axisPos == AxisPosition.RIGHT) {
	    				rightDataset.addSeries(tSeries);
	    			} else {
	    				leftDataset.addSeries(tSeries);
	    			}
	    		}
	    	}
		}
	}
	
	public static SimpleDateFormat getDateFormat(int timeType) {
		SimpleDateFormat dateFormat = null;
		if (timeType == java.sql.Types.DATE) {
			dateFormat = new SimpleDateFormat("d-MMM-yyyy hh:mm:ss");
		} else if (timeType == java.sql.Types.TIME) {
			dateFormat = new SimpleDateFormat("hh:mm:ss");
		}
		return dateFormat;
	}
	
	@Override public String getDescription() { return "Time Series"; }

	@Override public Icon getIcon() { return CIcon.CHART_CURVE; }
	
	@Override public String getQueryEg(JdbcTypes jdbcType) {
		return jdbcType.equals(JdbcTypes.KDB) ? KDB_QUERY : null; 
	}
	
	@Override
	public String toString() {
		return TimeseriesViewStrategy.class.getSimpleName() 
				+ "[" + getDescription() + "]";
	}

	@Override public List<ExampleView> getExamples() {
		return ImmutableList.of(getSineWave());
	}


	public static ExampleView getSineWave() {
		String description = "A sine/cosine wave over a period of days.";
		String name = "Day Sines";
		String[] colNames = new String[] { "dt", "cosineWave", "sineWave" };
		double[] a = mul(til(21), 0.6);
		Date[] dt = ExampleTestCases.getDays(2013, 1, 1, 21);
		Object[] colValues = new Object[] { dt, cos(a), sin(a) };
		ResultSet resultSet = new SimpleResultSet(colNames, colValues);
		ExampleView sineEV = new ExampleView(name, description, new TestCase(name, resultSet, KDB_QUERY, DUCKDB_QUERY));
		return sineEV;
	}

	private static final String[] FORMATA = 
		{ "The first date/time column found will be used for the x-axis.",
				"Each numerical column represents one time series line on the chart." };


	@Override public String getFormatExplainationHtml() {
		return "<ol><li>" + Joiner.on("</li><li>").join(FORMATA) + "</li></ol>";
	}
	
	@Override public String getFormatExplaination() {
		return Joiner.on("\r\n").join(FORMATA);
	}
	
	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int numColumnCount) {
		return rowCount < 211_000; // 2 seconds on Ryans PC
	}
	@Override public String getPulseName() { return "timeseries"; }
}
