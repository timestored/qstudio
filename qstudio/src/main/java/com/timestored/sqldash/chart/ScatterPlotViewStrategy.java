package com.timestored.sqldash.chart;

import java.awt.Color;
import java.awt.Component;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.timestored.connections.JdbcTypes;
import com.timestored.sqldash.chart.ChartResultSet.NumericCol;
import com.timestored.sqldash.theme.DBIcons;
import com.timestored.theme.Icon;


/**
 * Strategy for displaying {@link ResultSet}'s as a scatter plot.
 * See {@link #getFormatExplainationHtml()} for details.
 */
public enum ScatterPlotViewStrategy implements ViewStrategy {

	INSTANCE;

	private static final String TOOLTIP_FORMAT = "<html><b>{0}:</b><br>{1}<br>{2}</html>";
	

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
		        
		        final boolean isTS = colResultSet.getTimeCol() != null;
		        XYDataset dataset = null;
		        if(isTS) {
					dataset = TimeseriesViewStrategy.generateTimeSeries(colResultSet);		        	
		        } else {
		        	dataset = createXYDataset(colResultSet, appearanceConfig);		        	
		        }
		        
		        String chartTitle = (appearanceConfig != null && appearanceConfig.getChartTitle() != null) 
						? appearanceConfig.getChartTitle() : "";
				String xAxisLabel = isTS ? colResultSet.getTimeCol().getLabel() : colResultSet.getNumericColumns().get(0).getLabel();
				
				// Check tooltip setting
				boolean showTooltips = (appearanceConfig == null) || appearanceConfig.isTooltipEnabled();
				
				JFreeChart chart = ChartFactory.createScatterPlot(chartTitle, xAxisLabel, "", 
						dataset, PlotOrientation.VERTICAL, true, showTooltips, false);

				XYPlot plot = chart.getXYPlot();
				XYItemRenderer renderer = plot.getRenderer();
				if(isTS) {
			        DateAxis xAxis = new DateAxis(xAxisLabel);
			        plot.setDomainAxis(xAxis);
			        if (showTooltips) {
			        	TimeseriesViewStrategy.setTimeTooltipRenderer(colResultSet, renderer);
			        }
				} else {
					if (showTooltips) {
						StandardXYToolTipGenerator toolTipGenie = new StandardXYToolTipGenerator(TOOLTIP_FORMAT, 
								NumberFormat.getInstance(), NumberFormat.getInstance());
						renderer.setBaseToolTipGenerator(toolTipGenie);
					}
				}
				
				// Apply theme first
				JFreeChart themedChart = theme.apply(chart);
				
				// Apply series colors after theme
				XYPlot themedPlot = themedChart.getXYPlot();
				XYItemRenderer themedRenderer = themedPlot.getRenderer();
				if(appearanceConfig != null) {
					for(int i = 0; i < dataset.getSeriesCount(); i++) {
						String seriesName = (String) dataset.getSeriesKey(i);
						Color color = appearanceConfig.getSeriesColor(seriesName);
						if(color != null) {
							themedRenderer.setSeriesPaint(i, color);
						}
					}
				}
				
				// Apply chart-level config (background, gridlines, legend, etc.)
				// BUT ignore line/shape visibility settings - scatter plots always show shapes, never lines
				ChartConfigApplier.applyConfig(themedChart, appearanceConfig);
				
				// Scatter plots: Always show markers, never show lines
				// This overrides any config settings for line/shape visibility
				if (themedRenderer instanceof XYLineAndShapeRenderer) {
					XYLineAndShapeRenderer xyRenderer = (XYLineAndShapeRenderer) themedRenderer;
					xyRenderer.setBaseLinesVisible(false);  // Never show lines in scatter plot
					xyRenderer.setBaseShapesVisible(true);  // Always show markers
				}
				
				return new ChartPanel(themedChart, false, showTooltips, true, false, true);
			}
		});
		
	}
	
	@Override public boolean supportsAppearanceConfig() { return true; }
	

	/**
	 * First column becomes the Y-axis values, all other columns become graphs
	 * with points ( firstCol[i], selecteedCol[i])
	 * @return the transformed data set.
	 */
	static XYDataset createXYDataset(ChartResultSet chartResultSet, ChartAppearanceConfig config) 
			throws ChartFormatException {

        List<NumericCol> numCols = chartResultSet.getNumericColumns();
        if(numCols.size() < 2) {
        	throw new ChartFormatException("There must be atleast two numeric columns.");
        }
			
    	// convert the RS to array of doubles
		DefaultXYDataset dataset = new DefaultXYDataset();
    	double[] xAxis = numCols.get(0).getDoubles();
		for (int i=1; i < numCols.size(); i++) {
    		String sTitle = numCols.get(i).getLabel();
    		
    		// Check visibility
    		boolean isVisible = (config == null) || config.isSeriesVisible(sTitle);
    		if (!isVisible) {
    			continue; // Skip hidden series
    		}
    		
            dataset.addSeries(sTitle, new double[][] { xAxis, numCols.get(i).getDoubles()});
		}
    	
        return dataset;
	}
	

	@Override public String getDescription() { return "Scatter Plot"; }

	@Override public Icon getIcon() { return DBIcons.CHART_SCATTER_PLOT; }
	
	@Override public String getQueryEg(JdbcTypes jdbcType) {
		if(jdbcType.equals(JdbcTypes.KDB)) {
			return ExampleTestCases.COUNTRY_STATS_ADJUSTED_POP.getKdbQuery();
		}
		return null; 
	}
	

	@Override
	public String toString() {
		return ScatterPlotViewStrategy.class.getSimpleName() 
				+ "[" + getDescription() + "]";
	}

	@Override
	public List<ExampleView> getExamples() {
		ExampleView ev = new ExampleView("Country Population and GDP", 
				"The first column GDP is used for the x-axis. " +
				"The subsequent columns are then plotted against that x " +
				"axis as separate colored series.",
				ExampleTestCases.COUNTRY_STATS_ADJUSTED_POP);
		return ImmutableList.of(ev, TimeseriesViewStrategy.getSineWave());
	}

	private static final String[] FORMATA = 
		{ "Two or more numeric columns are required. ",
				"</li><li>The values in the first column are used for the X-axis. ",
				"</li><li>The values in following columns are used for the Y-axis. ",
				"</li><li>Each column is displayed with a separate color." };


	@Override public String getFormatExplainationHtml() {
		return "<ol><li>" + Joiner.on("</li><li>").join(FORMATA) + "</li></ol>";
	}
	
	@Override public String getFormatExplaination() {
		return Joiner.on("\r\n").join(FORMATA);
	}

	/** {@inheritDoc} **/
	public Component getControlPanel() {
		return null;
	}
	
	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int columnCount) {
		return rowCount < 41_000; // 1.5 seconds on Ryans PC
	}

	@Override public String getPulseName() { return "scatter"; }
}