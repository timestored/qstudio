package com.timestored.sqldash.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.ShapeUtilities;

import com.timestored.sqldash.chart.ChartAppearanceConfig.LegendPosition;
import com.timestored.sqldash.chart.ChartAppearanceConfig.LineStyle;
import com.timestored.sqldash.chart.ChartAppearanceConfig.MarkerShape;

/**
 * Utility class to apply ChartAppearanceConfig settings to JFreeChart objects.
 */
public class ChartConfigApplier {

    /**
     * Apply all applicable settings from the config to the chart.
     * @param chart The JFreeChart to modify
     * @param config The appearance configuration (may be null)
     */
    public static void applyConfig(JFreeChart chart, ChartAppearanceConfig config) {
        if (chart == null || config == null) {
            return;
        }
        
        // Chart title
        String title = config.getChartTitle();
        if (title != null && !title.isEmpty()) {
            chart.setTitle(title);
        }
        
        // Chart background
        Color chartBg = config.getChartBackgroundColor();
        if (chartBg != null) {
            chart.setBackgroundPaint(chartBg);
        }
        
        // Anti-aliasing
        chart.setAntiAlias(config.isAntiAliasing());
        
        // Apply plot settings
        Plot plot = chart.getPlot();
        applyPlotConfig(plot, config);
        
        // Apply legend settings
        applyLegendConfig(chart, config);
    }
    
    /**
     * Apply plot-specific settings.
     */
    private static void applyPlotConfig(Plot plot, ChartAppearanceConfig config) {
        if (plot == null) {
            return;
        }
        
        // Plot background
        Color plotBg = config.getPlotBackgroundColor();
        if (plotBg != null) {
            plot.setBackgroundPaint(plotBg);
        }
        
        // Plot outline
        Color outlineColor = config.getPlotOutlineColor();
        if (outlineColor != null) {
            plot.setOutlinePaint(outlineColor);
        }
        
        // Gridline color
        Color gridColor = config.getGridlineColor();
        
        // XYPlot specific settings
        if (plot instanceof XYPlot) {
            XYPlot xyPlot = (XYPlot) plot;
            xyPlot.setDomainGridlinesVisible(config.isDomainGridlinesVisible());
            xyPlot.setRangeGridlinesVisible(config.isRangeGridlinesVisible());
            
            if (gridColor != null) {
                xyPlot.setDomainGridlinePaint(gridColor);
                xyPlot.setRangeGridlinePaint(gridColor);
            }
            
            // Axis settings
            applyAxisSettings(xyPlot.getDomainAxis(), xyPlot.getRangeAxis(), config);
            
            // Line/Shape renderer settings
            XYItemRenderer renderer = xyPlot.getRenderer();
            if (renderer instanceof XYLineAndShapeRenderer) {
                applyXYRendererSettings((XYLineAndShapeRenderer) renderer, config);
            }
        }
        
        // CategoryPlot specific settings
        if (plot instanceof CategoryPlot) {
            CategoryPlot catPlot = (CategoryPlot) plot;
            catPlot.setDomainGridlinesVisible(config.isDomainGridlinesVisible());
            catPlot.setRangeGridlinesVisible(config.isRangeGridlinesVisible());
            
            if (gridColor != null) {
                catPlot.setDomainGridlinePaint(gridColor);
                catPlot.setRangeGridlinePaint(gridColor);
            }
            
            // Axis settings
            applyAxisSettings(null, catPlot.getRangeAxis(), config);
            
            // Bar renderer settings
            CategoryItemRenderer renderer = catPlot.getRenderer();
            if (renderer instanceof BarRenderer) {
                applyBarRendererSettings((BarRenderer) renderer, config);
            }
            
            // Item labels - set paint to contrast with plot background
            if (config.isItemLabelsVisible()) {
                renderer.setBaseItemLabelsVisible(true);
                renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
                // Use contrasting color based on plot background
                Color itemLabelBg = config.getPlotBackgroundColor();
                if (itemLabelBg != null && isDarkColor(itemLabelBg)) {
                    renderer.setBaseItemLabelPaint(Color.WHITE);
                } else {
                    // Check chart background
                    Color chartBgForLabel = config.getChartBackgroundColor();
                    if (chartBgForLabel != null && isDarkColor(chartBgForLabel)) {
                        renderer.setBaseItemLabelPaint(Color.WHITE);
                    }
                }
            }
        }
    }
    
    /**
     * Apply axis settings.
     */
    private static void applyAxisSettings(ValueAxis domainAxis, ValueAxis rangeAxis, 
            ChartAppearanceConfig config) {
        // Domain axis label
        String domainLabel = config.getDomainAxisLabel();
        if (domainLabel != null && domainAxis != null) {
            domainAxis.setLabel(domainLabel);
        }
        
        // Range axis label
        String rangeLabel = config.getRangeAxisLabel();
        if (rangeLabel != null && rangeAxis != null) {
            rangeAxis.setLabel(rangeLabel);
        }
        
        // Range axis bounds
        if (rangeAxis instanceof NumberAxis) {
            NumberAxis numAxis = (NumberAxis) rangeAxis;
            
            if (!config.isRangeAxisAutoRange()) {
                Double min = config.getRangeAxisMin();
                Double max = config.getRangeAxisMax();
                // Use Double.compare to properly handle edge cases
                if (min != null && max != null && Double.compare(min, max) < 0) {
                    numAxis.setRange(min, max);
                }
            } else {
                numAxis.setAutoRange(true);
            }
        }
    }
    
    /**
     * Apply XY renderer settings (for line, timeseries, step, area charts).
     */
    private static void applyXYRendererSettings(XYLineAndShapeRenderer renderer, 
            ChartAppearanceConfig config) {
        // Shapes visibility
        renderer.setBaseShapesVisible(config.isShapesVisible());
        
        // Lines visibility
        renderer.setBaseLinesVisible(config.isLinesVisible());
        
        // Default line stroke
        float lineWidth = config.getDefaultLineWidth();
        Stroke stroke = createStroke(lineWidth, config.getDefaultLineStyle());
        renderer.setBaseStroke(stroke);
    }
    
    /**
     * Apply bar renderer settings.
     */
    private static void applyBarRendererSettings(BarRenderer renderer, 
            ChartAppearanceConfig config) {
        // Bar margin
        double margin = config.getBarItemMargin();
        renderer.setItemMargin(margin);
    }
    
    /**
     * Create a stroke with the given width and style.
     */
    public static Stroke createStroke(float width, LineStyle style) {
        if (style == null) {
            style = LineStyle.SOLID;
        }
        
        switch (style) {
            case DASHED:
                return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1.0f, new float[] { 10.0f, 6.0f }, 0.0f);
            case DOTTED:
                return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1.0f, new float[] { 2.0f, 6.0f }, 0.0f);
            case DASH_DOT:
                return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1.0f, new float[] { 10.0f, 6.0f, 2.0f, 6.0f }, 0.0f);
            case SOLID:
            default:
                return new BasicStroke(width);
        }
    }
    
    /**
     * Apply legend settings.
     */
    private static void applyLegendConfig(JFreeChart chart, ChartAppearanceConfig config) {
        LegendTitle legend = chart.getLegend();
        
        // Legend visibility
        if (!config.isLegendVisible()) {
            if (legend != null) {
                legend.setVisible(false);
            }
            return;
        }
        
        if (legend != null) {
            legend.setVisible(true);
            
            // Legend position
            LegendPosition pos = config.getLegendPosition();
            if (pos != null) {
                switch (pos) {
                    case TOP:
                        legend.setPosition(RectangleEdge.TOP);
                        break;
                    case BOTTOM:
                        legend.setPosition(RectangleEdge.BOTTOM);
                        break;
                    case LEFT:
                        legend.setPosition(RectangleEdge.LEFT);
                        break;
                    case RIGHT:
                        legend.setPosition(RectangleEdge.RIGHT);
                        break;
                }
            }
            
            // Legend background
            Color legendBg = config.getLegendBackgroundColor();
            if (legendBg != null) {
                legend.setBackgroundPaint(legendBg);
            }
        }
    }
    
    /**
     * Apply per-series line and shape settings to an XY renderer (for line, timeseries charts).
     * This applies per-series line width, line style, and marker settings.
     * @param renderer The XYLineAndShapeRenderer to modify
     * @param dataset The dataset to get series names from
     * @param config The appearance configuration (may be null)
     */
    public static void applyXYSeriesSettings(XYLineAndShapeRenderer renderer, XYDataset dataset, 
            ChartAppearanceConfig config) {
        if (renderer == null || dataset == null || config == null) {
            return;
        }
        
        float defaultWidth = config.getDefaultLineWidth();
        LineStyle defaultStyle = config.getDefaultLineStyle();
        
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            String seriesName = (String) dataset.getSeriesKey(i);
            
            // Apply per-series stroke (line width and style)
            Float seriesWidth = config.getSeriesStrokeWidth().get(seriesName);
            LineStyle seriesStyle = config.getSeriesLineStyle().get(seriesName);
            
            float width = (seriesWidth != null) ? seriesWidth : defaultWidth;
            LineStyle style = (seriesStyle != null) ? seriesStyle : defaultStyle;
            
            // If width is 0 or very small, hide the line for this series
            if (width <= 0.1f) {
                renderer.setSeriesLinesVisible(i, false);
            } else {
                Stroke stroke = createStroke(width, style);
                renderer.setSeriesStroke(i, stroke);
            }
            
            // Apply per-series marker shape and size
            MarkerShape markerShape = config.getSeriesMarkerShape().get(seriesName);
            Integer markerSize = config.getSeriesMarkerSize().get(seriesName);
            
            if (markerSize != null && markerSize <= 0) {
                // Hide markers for this series if size is 0
                renderer.setSeriesShapesVisible(i, false);
            } else if (markerShape != null || markerSize != null) {
                // Show markers with custom shape/size
                renderer.setSeriesShapesVisible(i, true);
                int size = (markerSize != null) ? markerSize : 6; // default size 6
                MarkerShape shape = (markerShape != null) ? markerShape : MarkerShape.CIRCLE;
                renderer.setSeriesShape(i, createMarkerShape(shape, size));
            }
        }
    }
    
    /**
     * Apply per-series line settings to a Category LineAndShapeRenderer (for category line charts).
     * @param renderer The LineAndShapeRenderer to modify
     * @param dataset The dataset to get series names from
     * @param config The appearance configuration (may be null)
     */
    public static void applyCategorySeriesSettings(LineAndShapeRenderer renderer, CategoryDataset dataset, 
            ChartAppearanceConfig config) {
        if (renderer == null || dataset == null || config == null) {
            return;
        }
        
        float defaultWidth = config.getDefaultLineWidth();
        LineStyle defaultStyle = config.getDefaultLineStyle();
        
        for (int i = 0; i < dataset.getRowCount(); i++) {
            String seriesName = (String) dataset.getRowKey(i);
            
            // Apply per-series stroke (line width and style)
            Float seriesWidth = config.getSeriesStrokeWidth().get(seriesName);
            LineStyle seriesStyle = config.getSeriesLineStyle().get(seriesName);
            
            float width = (seriesWidth != null) ? seriesWidth : defaultWidth;
            LineStyle style = (seriesStyle != null) ? seriesStyle : defaultStyle;
            
            // If width is 0 or very small, hide the line for this series
            if (width <= 0.1f) {
                renderer.setSeriesLinesVisible(i, false);
            } else {
                Stroke stroke = createStroke(width, style);
                renderer.setSeriesStroke(i, stroke);
            }
            
            // Apply per-series marker shape and size
            MarkerShape markerShape = config.getSeriesMarkerShape().get(seriesName);
            Integer markerSize = config.getSeriesMarkerSize().get(seriesName);
            
            if (markerSize != null && markerSize <= 0) {
                // Hide markers for this series if size is 0
                renderer.setSeriesShapesVisible(i, false);
            } else if (markerShape != null || markerSize != null) {
                // Show markers with custom shape/size
                renderer.setSeriesShapesVisible(i, true);
                int size = (markerSize != null) ? markerSize : 6; // default size 6
                MarkerShape shape = (markerShape != null) ? markerShape : MarkerShape.CIRCLE;
                renderer.setSeriesShape(i, createMarkerShape(shape, size));
            }
        }
    }
    
    /**
     * Create a Shape for the given marker type and size.
     */
    public static Shape createMarkerShape(MarkerShape shape, int size) {
        if (shape == null || shape == MarkerShape.NONE) {
            return null;
        }
        
        double halfSize = size / 2.0;
        
        switch (shape) {
            case CIRCLE:
                return new Ellipse2D.Double(-halfSize, -halfSize, size, size);
            case SQUARE:
                return new Rectangle2D.Double(-halfSize, -halfSize, size, size);
            case TRIANGLE:
                return ShapeUtilities.createUpTriangle((float) halfSize);
            case DIAMOND:
                return ShapeUtilities.createDiamond((float) halfSize);
            case CROSS:
                return ShapeUtilities.createRegularCross((float) halfSize, (float) (halfSize / 3));
            default:
                return new Ellipse2D.Double(-halfSize, -halfSize, size, size);
        }
    }
    
    /**
     * Check if a color is considered dark (for choosing contrasting text color).
     */
    private static boolean isDarkColor(Color color) {
        // Use relative luminance formula
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return luminance < 0.5;
    }
}
