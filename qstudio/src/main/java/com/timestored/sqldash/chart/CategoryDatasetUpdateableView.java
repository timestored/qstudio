package com.timestored.sqldash.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Paint;
import java.sql.ResultSet;
import java.text.DecimalFormat;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;

import com.google.common.base.Preconditions;
import com.timestored.sqldash.chart.ChartAppearanceConfig.AxisPosition;
import com.timestored.sqldash.chart.ChartResultSet.NumericCol;

/**
 * A CategoryDataset based updateable view.
 */
class CategoryDatasetUpdateableView implements UpdateableView {

	private static final Font TINY_FONT = new Font("Times New Roman", Font.PLAIN, 0);
	private final ChartPanel chartPanel;
	private DefaultCategoryDataset dataset;
	private final ChartAppearanceConfig appearanceConfig;
	private final ChartTheme theme;
	/** Stores original renderer class to use same type for right axis */
	private final Class<? extends CategoryItemRenderer> originalRendererClass;

	public CategoryDatasetUpdateableView(ChartTheme theme, JFreeChart chart) {
		this(theme, chart, null);
	}
	
	public CategoryDatasetUpdateableView(ChartTheme theme, JFreeChart chart, ChartAppearanceConfig appearanceConfig) {

		Preconditions.checkNotNull(chart);
		Preconditions.checkNotNull(theme);
		
		this.dataset = new DefaultCategoryDataset();
		this.appearanceConfig = appearanceConfig;
		this.theme = theme;
		
		// Store original renderer class for right axis creation
		CategoryPlot plot = chart.getCategoryPlot();
		this.originalRendererClass = plot.getRenderer().getClass();
		
		plot.setDataset(dataset);
		
		// Apply chart title if configured
		if(appearanceConfig != null && appearanceConfig.getChartTitle() != null 
				&& !appearanceConfig.getChartTitle().isEmpty()) {
			chart.setTitle(appearanceConfig.getChartTitle());
		}
		
		// Apply theme first
		JFreeChart themedChart = theme.apply(chart);
		
		// Then apply chart-level config (background, gridlines, legend, etc.)
		// This must happen AFTER theme.apply() so config overrides theme defaults
		ChartConfigApplier.applyConfig(themedChart, appearanceConfig);
		
		chartPanel = new ChartPanel(themedChart, false, true, true, false, true);
	}

	@Override public void update(ResultSet rs, ChartResultSet chartRS) throws ChartFormatException {
		
		if(chartRS == null) {
			throw new ChartFormatException("Could not construct ResultSet.");
		}
		if(chartRS.getNumericColumns().size()<1) {
			throw new ChartFormatException("Atleast one numeric column is required.");
		}
		
		// name axis using column names etc.
		JFreeChart chart = chartPanel.getChart();
		CategoryPlot cplot = chart.getCategoryPlot();
		CategoryItemRenderer renderer = cplot.getRenderer();
		
		// Apply tooltip
		renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
				Tooltip.LABEL_XY_FORMAT, new DecimalFormat("#,###.##")));
		
		// Apply axis labels from config
		CategoryAxis domainAxis = cplot.getDomainAxis();
		ValueAxis rangeAxis = cplot.getRangeAxis();
		
		// Set domain (X) axis label - from config or data
		if (appearanceConfig != null && appearanceConfig.getDomainAxisLabel() != null 
				&& !appearanceConfig.getDomainAxisLabel().isEmpty()) {
			domainAxis.setLabel(appearanceConfig.getDomainAxisLabel());
		} else {
			domainAxis.setLabel(chartRS.getRowTitle());
		}
		
		// Set range (Y) axis label - from config or data
		if (appearanceConfig != null && appearanceConfig.getRangeAxisLabel() != null 
				&& !appearanceConfig.getRangeAxisLabel().isEmpty()) {
			rangeAxis.setLabel(appearanceConfig.getRangeAxisLabel());
		} else if(chartRS.getNumericColumns().size()==1) {
			rangeAxis.setLabel(chartRS.getNumericColumns().get(0).getLabel());
		} else {
			rangeAxis.setLabel("");
		}

		dataset.clear();
		
		// Populate datasets, potentially splitting between left and right axis
		DefaultCategoryDataset rightDataset = new DefaultCategoryDataset();
		boolean hasRightAxis = addWithAxisConfig(chartRS, dataset, rightDataset, appearanceConfig);
		
		// Set up right axis if needed
		if(hasRightAxis && rightDataset.getRowCount() > 0) {
			NumberAxis rightAxis = new NumberAxis("Value (Right)");
			// Apply theme colors to right axis
			rightAxis.setTickLabelPaint(rangeAxis.getTickLabelPaint());
			rightAxis.setLabelPaint(rangeAxis.getLabelPaint());
			cplot.setRangeAxis(1, rightAxis);
			cplot.setDataset(1, rightDataset);
			cplot.mapDatasetToRangeAxis(1, 1);
			
			// Create renderer for right axis dataset - same type as main renderer
			// This fixes the bug where Area chart right axis becomes a line
			CategoryItemRenderer rightRenderer = createMatchingRenderer(renderer);
			cplot.setRenderer(1, rightRenderer);
			rightRenderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
					Tooltip.LABEL_XY_FORMAT, new DecimalFormat("#,###.##")));
			
			// Apply colors to right axis series
			applySeriesColors(rightDataset, rightRenderer, appearanceConfig, cplot);
			// Apply per-series line/shape settings for right axis
			if (rightRenderer instanceof LineAndShapeRenderer && appearanceConfig != null) {
				ChartConfigApplier.applyCategorySeriesSettings(
						(LineAndShapeRenderer) rightRenderer, rightDataset, appearanceConfig);
			}
		}
		
		// Apply colors to left axis series
		applySeriesColors(dataset, renderer, appearanceConfig, cplot);
		
		// Apply line/shape settings if this is a line chart renderer
		if (renderer instanceof LineAndShapeRenderer && appearanceConfig != null) {
			applyLineShapeSettings((LineAndShapeRenderer) renderer, appearanceConfig);
			// Apply per-series line/shape settings
			ChartConfigApplier.applyCategorySeriesSettings(
					(LineAndShapeRenderer) renderer, dataset, appearanceConfig);
		}

		
		// domain axis labels - too many?
		// if more than 30 categories, hide them and show only 10
		if(dataset.getColumnCount()>30) {
			CategoryAxis dAxis = cplot.getDomainAxis();
			int i = 0;
			int m = dataset.getColumnCount()/7;
			for(Object key : dataset.getColumnKeys()) {
				if(!(i%m==0)) {
					// use tiny font to handle different chart themes and colors better
					dAxis.setTickLabelFont((Comparable<?>) key, TINY_FONT);
				}
				i++;
			}
		}
		
		int totalSeries = dataset.getRowCount() + (hasRightAxis ? rightDataset.getRowCount() : 0);
		boolean legendVisible = (appearanceConfig == null) || appearanceConfig.isLegendVisible();
		chart.getLegend().setVisible(legendVisible && totalSeries > 1 && totalSeries < 60);	
	}

	@Override public Component getComponent() {
		return chartPanel;
	}

	/** Add the new data in {@link ResultSet} to the {@link Dataset} if possible. */
	public static DefaultCategoryDataset add(ChartResultSet colResultSet, DefaultCategoryDataset dataset) {
		for (NumericCol numCol : colResultSet.getNumericColumns()) {
			addSeriesToDataset(numCol, colResultSet, dataset);
		}
		return dataset;
	}
	
	/** Add a single series to a dataset */
	private static void addSeriesToDataset(NumericCol numCol, ChartResultSet chartRS, DefaultCategoryDataset dataset) {
		String seriesName = numCol.getLabel();
		double[] vals = numCol.getDoubles();
		for (int i = 0; i < vals.length; i++) {
			dataset.addValue(vals[i], seriesName, chartRS.getRowLabel(i));
		}
	}
	
	/**
	 * Add data to left and right datasets based on appearance config axis assignments.
	 * Also respects visibility setting.
	 * @return true if any series was assigned to the right axis
	 */
	private static boolean addWithAxisConfig(ChartResultSet chartRS, DefaultCategoryDataset leftDataset, 
			DefaultCategoryDataset rightDataset, ChartAppearanceConfig config) {
		boolean hasRightAxis = false;
		for (NumericCol numCol : chartRS.getNumericColumns()) {
			String seriesName = numCol.getLabel();
			
			// Check visibility
			boolean isVisible = (config == null) || config.isSeriesVisible(seriesName);
			if (!isVisible) {
				continue; // Skip hidden series
			}
			
			AxisPosition axisPos = (config != null) ? config.getSeriesAxisPosition(seriesName) : AxisPosition.LEFT;
			DefaultCategoryDataset targetDataset = (axisPos == AxisPosition.RIGHT) ? rightDataset : leftDataset;
			if(axisPos == AxisPosition.RIGHT) {
				hasRightAxis = true;
			}
			addSeriesToDataset(numCol, chartRS, targetDataset);
		}
		return hasRightAxis;
	}
	
	/** Apply series colors from appearance config to a renderer */
	private static void applySeriesColors(DefaultCategoryDataset dataset, CategoryItemRenderer renderer, 
			ChartAppearanceConfig config, CategoryPlot plot) {
		if(config != null) {
			for(int i = 0; i < dataset.getRowCount(); i++) {
				String seriesName = (String) dataset.getRowKey(i);
				Color color = config.getSeriesColor(seriesName);
				if(color != null) {
					renderer.setSeriesPaint(i, color);
				}
			}
			
			// Apply item labels (values on bars) if configured
			if (config.isItemLabelsVisible()) {
				renderer.setBaseItemLabelsVisible(true);
				renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
				// Use contrasting color based on plot/chart background
				Paint labelPaint = getContrastingLabelPaint(config, plot);
				renderer.setBaseItemLabelPaint(labelPaint);
			}
		}
	}
	
	/**
	 * Get a contrasting paint color for item labels based on background colors.
	 */
	private static Paint getContrastingLabelPaint(ChartAppearanceConfig config, CategoryPlot plot) {
		// Check plot background first
		Paint plotBg = plot.getBackgroundPaint();
		if (plotBg instanceof Color) {
			Color plotBgColor = (Color) plotBg;
			if (isDarkColor(plotBgColor)) {
				return Color.WHITE;
			}
		}
		// Check config plot background
		Color configPlotBg = config.getPlotBackgroundColor();
		if (configPlotBg != null && isDarkColor(configPlotBg)) {
			return Color.WHITE;
		}
		// Check config chart background
		Color configChartBg = config.getChartBackgroundColor();
		if (configChartBg != null && isDarkColor(configChartBg)) {
			return Color.WHITE;
		}
		return Color.BLACK;
	}
	
	/**
	 * Check if a color is dark using luminance calculation.
	 */
	private static boolean isDarkColor(Color color) {
		double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
		return luminance < 0.5;
	}
	
	/**
	 * Create a renderer matching the type of the original renderer.
	 * This ensures AreaChart right axis uses AreaRenderer, not LineAndShapeRenderer.
	 */
	private CategoryItemRenderer createMatchingRenderer(CategoryItemRenderer original) {
		if (original instanceof AreaRenderer) {
			return new AreaRenderer();
		} else if (original instanceof LineAndShapeRenderer) {
			LineAndShapeRenderer lineRenderer = (LineAndShapeRenderer) original;
			return new LineAndShapeRenderer(lineRenderer.getBaseLinesVisible(), lineRenderer.getBaseShapesVisible());
		}
		// Default fallback
		return new LineAndShapeRenderer(true, false);
	}
	
	/**
	 * Apply line/shape settings from config to a LineAndShapeRenderer.
	 */
	private static void applyLineShapeSettings(LineAndShapeRenderer renderer, ChartAppearanceConfig config) {
		if (config == null) return;
		
		// Apply shapes visible
		renderer.setBaseShapesVisible(config.isShapesVisible());
		
		// Apply lines visible
		renderer.setBaseLinesVisible(config.isLinesVisible());
		
		// Apply line stroke (width and style)
		float lineWidth = config.getDefaultLineWidth();
		java.awt.Stroke stroke = ChartConfigApplier.createStroke(lineWidth, config.getDefaultLineStyle());
		renderer.setBaseStroke(stroke);
	}
}
