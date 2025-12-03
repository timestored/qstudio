package com.timestored.sqldash.chart;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class ChartAppearanceConfig {

    public enum AxisPosition { LEFT, RIGHT }
    public enum MarkerShape { NONE, CIRCLE, SQUARE, TRIANGLE, DIAMOND, CROSS }
    public enum LegendPosition { TOP, BOTTOM, LEFT, RIGHT }
    public enum LineStyle { SOLID, DASHED, DOTTED, DASH_DOT }

    private String chartTitle = "";

    // Chart-level
    private Color chartBackgroundColor;
    private Color plotBackgroundColor;
    private Color plotOutlineColor;
    private boolean domainGridlinesVisible = true;
    private boolean rangeGridlinesVisible = true;
    private boolean legendVisible = true;
    private LegendPosition legendPosition = LegendPosition.BOTTOM;
    private Color legendBackgroundColor;
    private boolean tooltipEnabled = true;
    private boolean antiAliasing = true;

    // Line + shape defaults
    private boolean shapesVisible = false;
    private boolean linesVisible = true;
    private float defaultLineWidth = 1.0f;
    private LineStyle defaultLineStyle = LineStyle.SOLID;

    // Bar chart
    private double barItemMargin = 0.0;
    private boolean itemLabelsVisible = false;

    // Axis
    private String domainAxisLabel;
    private String rangeAxisLabel;
    private Double rangeAxisMin;
    private Double rangeAxisMax;
    private boolean rangeAxisAutoRange = true;
    private Color gridlineColor;

    // Series settings
    private final Map<String, Color> seriesColors = new HashMap<>();
    private final Map<String, AxisPosition> seriesAxisPositions = new HashMap<>();
    private final Map<String, Boolean> seriesVisible = new HashMap<>();
    private final Map<String, Float> seriesStrokeWidth = new HashMap<>();
    private final Map<String, MarkerShape> seriesMarkerShape = new HashMap<>();
    private final Map<String, Integer> seriesMarkerSize = new HashMap<>();
    private final Map<String, LineStyle> seriesLineStyle = new HashMap<>();

    /** Copy constructor */
    public ChartAppearanceConfig(ChartAppearanceConfig o) {
        if (o == null) return;

        this.chartTitle = o.chartTitle;
        this.chartBackgroundColor = o.chartBackgroundColor;
        this.plotBackgroundColor = o.plotBackgroundColor;
        this.plotOutlineColor = o.plotOutlineColor;
        this.domainGridlinesVisible = o.domainGridlinesVisible;
        this.rangeGridlinesVisible = o.rangeGridlinesVisible;
        this.legendVisible = o.legendVisible;
        this.legendPosition = o.legendPosition;
        this.legendBackgroundColor = o.legendBackgroundColor;
        this.tooltipEnabled = o.tooltipEnabled;
        this.antiAliasing = o.antiAliasing;

        this.shapesVisible = o.shapesVisible;
        this.linesVisible = o.linesVisible;
        this.defaultLineWidth = o.defaultLineWidth;
        this.defaultLineStyle = o.defaultLineStyle;

        this.barItemMargin = o.barItemMargin;
        this.itemLabelsVisible = o.itemLabelsVisible;

        this.domainAxisLabel = o.domainAxisLabel;
        this.rangeAxisLabel = o.rangeAxisLabel;
        this.rangeAxisMin = o.rangeAxisMin;
        this.rangeAxisMax = o.rangeAxisMax;
        this.rangeAxisAutoRange = o.rangeAxisAutoRange;
        this.gridlineColor = o.gridlineColor;

        this.seriesColors.putAll(o.seriesColors);
        this.seriesAxisPositions.putAll(o.seriesAxisPositions);
        this.seriesVisible.putAll(o.seriesVisible);
        this.seriesStrokeWidth.putAll(o.seriesStrokeWidth);
        this.seriesMarkerShape.putAll(o.seriesMarkerShape);
        this.seriesMarkerSize.putAll(o.seriesMarkerSize);
        this.seriesLineStyle.putAll(o.seriesLineStyle);
    }

    // ---- Custom validation setters that Lombok should NOT override ---- //

    public void setChartTitle(String chartTitle) {
        this.chartTitle = chartTitle != null ? chartTitle : "";
    }

    public void setDefaultLineWidth(float width) {
        this.defaultLineWidth = Math.max(0.5f, width);
    }

    public void setBarItemMargin(double margin) {
        this.barItemMargin = Math.max(0.0, Math.min(1.0, margin));
    }

    // ---- Series helpers (kept as-is) ---- //

    public Color getSeriesColor(String s) {
        return seriesColors.get(s);
    }

    public void setSeriesColor(String s, Color c) {
        if (s == null) return;
        if (c == null) seriesColors.remove(s);
        else seriesColors.put(s, c);
    }

    public AxisPosition getSeriesAxisPosition(String s) {
        return seriesAxisPositions.getOrDefault(s, AxisPosition.LEFT);
    }

    public void setSeriesAxisPosition(String s, AxisPosition pos) {
        if (s == null) return;
        if (pos == null || pos == AxisPosition.LEFT) seriesAxisPositions.remove(s);
        else seriesAxisPositions.put(s, pos);
    }

    public boolean hasRightAxisSeries() {
        return seriesAxisPositions.containsValue(AxisPosition.RIGHT);
    }

    public boolean isSeriesVisible(String s) {
        return seriesVisible.getOrDefault(s, true);
    }

    public void setSeriesVisible(String s, Boolean vis) {
        if (s == null) return;
        if (vis == null) seriesVisible.remove(s);
        else seriesVisible.put(s, vis);
    }

    public void setSeriesStrokeWidth(String s, Float w) {
        if (s == null) return;
        // Allow width=0 to explicitly hide lines, remove null or negative values
        if (w == null || w < 0) seriesStrokeWidth.remove(s);
        else seriesStrokeWidth.put(s, w);
    }

    public void setSeriesMarkerShape(String s, MarkerShape m) {
        if (s == null) return;
        if (m == null || m == MarkerShape.NONE) seriesMarkerShape.remove(s);
        else seriesMarkerShape.put(s, m);
    }

    public void setSeriesMarkerSize(String s, Integer size) {
        if (s == null) return;
        // Allow size=0 to explicitly hide markers, remove null or negative values
        if (size == null || size < 0) seriesMarkerSize.remove(s);
        else seriesMarkerSize.put(s, size);
    }

    public void setSeriesLineStyle(String s, LineStyle ls) {
        if (s == null) return;
        if (ls == null) seriesLineStyle.remove(s);
        else seriesLineStyle.put(s, ls);
    }

    // ---- Utility ---- //

    public void clearSeriesConfig() {
        seriesColors.clear();
        seriesAxisPositions.clear();
        seriesVisible.clear();
        seriesStrokeWidth.clear();
        seriesMarkerShape.clear();
        seriesMarkerSize.clear();
        seriesLineStyle.clear();
    }

    public boolean hasConfiguration() {
        return (chartTitle != null && !chartTitle.isEmpty())
                || chartBackgroundColor != null
                || plotBackgroundColor != null
                || !seriesColors.isEmpty()
                || !seriesAxisPositions.isEmpty()
                || !seriesVisible.isEmpty()
                || !seriesStrokeWidth.isEmpty()
                || !seriesMarkerShape.isEmpty()
                || !seriesMarkerSize.isEmpty();
    }
}
