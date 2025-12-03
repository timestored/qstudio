package com.timestored.sqldash.chart;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Locale;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;

import com.google.common.base.Preconditions;

/**
 * Modern, clean chart theme with flat visuals and professional styling.
 * Inspired by Excel, Power BI, and other commercial BI tools.
 */
class DefaultTheme implements ChartTheme {

	// Modern thin line stroke for cleaner line charts
	private static final float DEFAULT_LINE_WIDTH = 2.0f;
	private static final Stroke THIN_STROKE = new BasicStroke(DEFAULT_LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	
	// Small, subtle marker for improved readability
	private static final int MARKER_SIZE = 5;
	private static final Shape SMALL_CIRCLE = new Ellipse2D.Double(-MARKER_SIZE/2.0, -MARKER_SIZE/2.0, MARKER_SIZE, MARKER_SIZE);

    private static final StandardXYBarPainter barPainter = new StandardXYBarPainter();
    private static final StandardBarPainter sbarPainter = new StandardBarPainter();
    private static final DefaultTheme INSTANCE = new DefaultTheme(new LightColorScheme(), "Default", "Default");

    private final ColorScheme colorScheme;
	private final StandardChartTheme chartTheme;
	private final DrawingSupplier drawingSupplier;
	private final String description;
	private final String title;
    
    private DefaultTheme(final ColorScheme colorScheme, String title, String description) {

    	this.colorScheme = Preconditions.checkNotNull(colorScheme);
    	this.title = Preconditions.checkNotNull(title);
    	this.description = Preconditions.checkNotNull(description);
    	
    	/*
    	 * create a modified version of the jfree chart theme to suit our needs
    	 */
        chartTheme = (StandardChartTheme)org.jfree.chart.StandardChartTheme.createJFreeTheme();
        chartTheme.setXYBarPainter(barPainter);
        chartTheme.setBarPainter(sbarPainter);
        
        // Flat visuals - no shadows or gradients
        chartTheme.setShadowVisible(false);
        chartTheme.setShadowPaint(new Color(0, 0, 0, 0)); // Transparent shadow
        
        chartTheme.setPlotBackgroundPaint(colorScheme.getBG());
        chartTheme.setDomainGridlinePaint(colorScheme.getGridlines());
        chartTheme.setRangeGridlinePaint(colorScheme.getGridlines());
        chartTheme.setPlotOutlinePaint(colorScheme.getGridlines());
        chartTheme.setChartBackgroundPaint(colorScheme.getBG());
        chartTheme.setTitlePaint(colorScheme.getFG());

        
        chartTheme.setAxisLabelPaint(colorScheme.getText());
        chartTheme.setLabelLinkPaint(colorScheme.getFG());
        
        // legend related colors
        chartTheme.setLegendItemPaint(colorScheme.getText());
        chartTheme.setLegendBackgroundPaint(colorScheme.getBG());
        
        // Improved tick label paint for better readability
        chartTheme.setTickLabelPaint(colorScheme.getText());
        
        // Modern font setup
        setupFonts();

    	drawingSupplier = new DefaultDrawingSupplier() {

			private static final long serialVersionUID = 1L;
			int i = 0;
    		int j = 0;
    		Color[] colors = colorScheme.getColorArray();
			
			@Override public Paint getNextPaint() {
				return colors[(i++) % colors.length];
			}
			
			@Override public Paint getNextFillPaint() {
				return colors[(j++) % colors.length];
			}
			
			// Return consistent small circle markers
			@Override public Shape getNextShape() {
				return SMALL_CIRCLE;
			}
			
			// Modern thin stroke for all lines
			@Override public Stroke getNextStroke() {
				return THIN_STROKE;
			}
		};
		chartTheme.setDrawingSupplier(drawingSupplier);
	}
    
    private void setupFonts() {
        // The default font used by JFreeChart unable to render Chinese properly.
        // We need to provide font which is able to support Chinese rendering.
        if (Locale.getDefault().getLanguage().equals(Locale.SIMPLIFIED_CHINESE.getLanguage())) {
            final Font oldExtraLargeFont = chartTheme.getExtraLargeFont();
            final Font oldLargeFont = chartTheme.getLargeFont();
            final Font oldRegularFont = chartTheme.getRegularFont();
            final Font oldSmallFont = chartTheme.getSmallFont();

            final Font extraLargeFont = new Font("Sans-serif", oldExtraLargeFont.getStyle(), oldExtraLargeFont.getSize());
            final Font largeFont = new Font("Sans-serif", oldLargeFont.getStyle(), oldLargeFont.getSize());
            final Font regularFont = new Font("Sans-serif", oldRegularFont.getStyle(), oldRegularFont.getSize());
            final Font smallFont = new Font("Sans-serif", oldSmallFont.getStyle(), oldSmallFont.getSize());
            
            chartTheme.setExtraLargeFont(extraLargeFont);
            chartTheme.setLargeFont(largeFont);
            chartTheme.setRegularFont(regularFont);
            chartTheme.setSmallFont(smallFont);
        }
    }

    public static ChartTheme getInstance(ColorScheme colorScheme, String title, 
    		String description) {
    	return new DefaultTheme(colorScheme, title, description);
    }
    
    public static ChartTheme getInstance() {
    	return INSTANCE;
    }
    
    
    /**
     * Applying chart theme based on given JFreeChart.
     * @param chart the JFreeChart
     */
    @Override
    public JFreeChart apply(JFreeChart chart) {

        Plot p = chart.getPlot();
		LegendTitle legend = chart.getLegend();
		if(legend != null) {
			legend.setFrame(BlockBorder.NONE);
		}
        p.setDrawingSupplier(drawingSupplier);
        
        // Slightly increased foreground alpha for better visibility
        p.setForegroundAlpha(0.9F);
        
        // High quality rendering
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        
        chartTheme.apply(chart);

        if (chart.getPlot() instanceof CombinedDomainXYPlot) {
            @SuppressWarnings("unchecked")
            List<Plot> plots = ((CombinedDomainXYPlot)chart.getPlot()).getSubplots();
            for (Plot plot : plots) {
                final int domainAxisCount = ((XYPlot)plot).getDomainAxisCount();
                final int rangeAxisCount = ((XYPlot)plot).getRangeAxisCount();
                for (int i = 0; i < domainAxisCount; i++) {
                    setAxisColor(((XYPlot)plot).getDomainAxis(i), colorScheme);
                }
                for (int i = 0; i < rangeAxisCount; i++) {
                    setAxisColor(((XYPlot)plot).getRangeAxis(i), colorScheme);
                }
                // Apply modern line styling to subplots
                applyXYPlotStyling((XYPlot)plot);
            }
            
            
        } else {
            final Plot plot = chart.getPlot();
            if (plot instanceof XYPlot) {          
                final org.jfree.chart.plot.XYPlot xyPlot = (org.jfree.chart.plot.XYPlot)plot;
                final int domainAxisCount = xyPlot.getDomainAxisCount();
                final int rangeAxisCount = xyPlot.getRangeAxisCount();
                for (int i = 0; i < domainAxisCount; i++) {
                    setAxisColor(xyPlot.getDomainAxis(i), colorScheme);
                }
                for (int i = 0; i < rangeAxisCount; i++) {
                    setAxisColor(xyPlot.getRangeAxis(i), colorScheme);
                }
                // Apply modern line styling
                applyXYPlotStyling(xyPlot);
            }
        }

        
        if(chart.getPlot() instanceof CategoryPlot) {
            final CategoryPlot categoryPlot = (CategoryPlot) chart.getPlot();
            categoryPlot.setDomainGridlinesVisible(true);
            CategoryAxis categoryAxis = categoryPlot.getDomainAxis();
            categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
            final double margin = 0.02;
            categoryAxis.setCategoryMargin(margin);
            categoryAxis.setLowerMargin(0);
            categoryAxis.setUpperMargin(0);
            
            categoryAxis.setAxisLinePaint(colorScheme.getGridlines());
            categoryAxis.setTickMarkPaint(colorScheme.getText());
            categoryAxis.setTickLabelPaint(colorScheme.getText());

            setAxisColor(categoryPlot.getRangeAxis(), colorScheme);
            
            // Apply modern category line/shape styling
            applyCategoryPlotStyling(categoryPlot);
            
        } else if(chart.getPlot() instanceof PiePlot) {
            final PiePlot piePlot = (PiePlot) chart.getPlot();
            // Clean pie labels - no heavy outlines or shadows
            piePlot.setLabelOutlinePaint(null);
            piePlot.setLabelShadowPaint(null);
            piePlot.setLabelLinkPaint(colorScheme.getText());
            piePlot.setLabelPaint(colorScheme.getText());
            // Modern flat pie style
            piePlot.setShadowPaint(null);
            piePlot.setOutlinePaint(null);
        }
        return chart;
    }
    
    /**
     * Apply modern styling to XY plots (timeseries, scatter, line charts).
     */
    private void applyXYPlotStyling(XYPlot plot) {
        XYItemRenderer renderer = plot.getRenderer();
        if (renderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer lineRenderer = (XYLineAndShapeRenderer) renderer;
            // Thinner, smoother lines
            lineRenderer.setBaseStroke(THIN_STROKE);
            // Small, consistent markers
            lineRenderer.setBaseShape(SMALL_CIRCLE);
            // Lines visible by default, shapes on hover for less clutter
            lineRenderer.setBaseLinesVisible(true);
            lineRenderer.setBaseShapesVisible(false);
            lineRenderer.setBaseShapesFilled(true);
        }
    }
    
    /**
     * Apply modern styling to category plots (bar, line charts).
     */
    private void applyCategoryPlotStyling(CategoryPlot plot) {
        // Flat bar charts - no gradients
        if (plot.getRenderer() instanceof BarRenderer) {
            BarRenderer barRenderer = (BarRenderer) plot.getRenderer();
            barRenderer.setBarPainter(sbarPainter);
            barRenderer.setShadowVisible(false);
            barRenderer.setDrawBarOutline(false);
        }
        // Modern line charts
        if (plot.getRenderer() instanceof LineAndShapeRenderer) {
            LineAndShapeRenderer lineRenderer = (LineAndShapeRenderer) plot.getRenderer();
            lineRenderer.setBaseStroke(THIN_STROKE);
            lineRenderer.setBaseShape(SMALL_CIRCLE);
            lineRenderer.setBaseLinesVisible(true);
            lineRenderer.setBaseShapesVisible(false);
            lineRenderer.setBaseShapesFilled(true);
        }
    }

	public static void setAxisColor(final ValueAxis valueAxis, ColorScheme colorScheme) {
		// Sharper axis styling - cleaner look
		valueAxis.setAxisLinePaint(colorScheme.getText());
		valueAxis.setTickMarkPaint(colorScheme.getText());
		valueAxis.setTickLabelPaint(colorScheme.getText());
	}

	@Override public boolean showChartLegend() { return true; }

	@Override public String getTitle() { return title; }

	@Override public String getDescription() { return description; }

	@Override public Color getForegroundColor() { return colorScheme.getFG(); }

	@Override public Color getBackgroundColor() { return colorScheme.getBG(); }

	@Override public Color getAltBackgroundColor() { return colorScheme.getAltBG(); }

	@Override public Color getSelectedBackgroundColor() { return colorScheme.getSelectedBG(); }
		
}


