package com.timestored.sqldash.chart;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValue;

import com.timestored.sqldash.chart.TableFactory.KdbStringValuer;

public class ShadeRenderer extends DefaultTableRenderer {

    private static final long serialVersionUID = 1L;
    private double min = 0, max = 0, mean = 0;
    private boolean rangeComputed = false;

    public ShadeRenderer(StringValue sv) {
        super(sv, JLabel.CENTER);
    }

    private void computeRange(JTable table, int column) {
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        int count = 0;

        for (int r = 0; r < table.getRowCount(); r++) {
            Object v = table.getValueAt(r, column);
            if (v instanceof Number) {
                double d = ((Number)v).doubleValue();
                min = Math.min(min, d);
                max = Math.max(max, d);
                sum += d;
                count++;
            }
        }
        if (min == Double.POSITIVE_INFINITY) min = 0;
        if (max == Double.NEGATIVE_INFINITY) max = 0;

        mean = count > 0 ? sum / count : 0;
        rangeComputed = true;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!(value instanceof Number)) return l;

        if (!rangeComputed)
            computeRange(table, column);

        double v = ((Number)value).doubleValue();
        boolean neutral = isNeutral(v, mean, min, max);
        Color shade = neutral ? null : shadeByValueThemeAware(v, min, max, mean);

        String formatted = new KdbStringValuer().getString(value);
        l.setText("<html>" + RenderProvider.buildTagSpan(shade, formatted) + "</html>");
        l.setHorizontalAlignment(JLabel.CENTER);
        l.setToolTipText("Value: " + v);

        return l;
    }

    private static boolean isNeutral(double v, double mean, double min, double max) {
        double range = max - min;
        return Math.abs(v - mean) < range * 0.001;
    }

    private static Color shadeByValueThemeAware(double v, double min, double max, double mean) {

        final double INTENSITY = RenderProvider.isDarkTheme() ? 1.2 : 1.0;

        Color neutral = RenderProvider.getStatusNeutral();
        if (max == min) return neutral;

        Color shade;
        if (v >= mean) {
            double t = (v - mean) / (max - mean);     // 0 at mean → 1 at max
            shade = interpolate(neutral, RenderProvider.getStatusGreen(), t);
        } else {
            double t = (mean - v) / (mean - min);     // 0 at mean → 1 at min
            shade = interpolate(neutral, RenderProvider.getStatusRed(), t);
        }

        if (INTENSITY != 1.0)
            shade = interpolate(neutral, shade, INTENSITY);

        return shade;
    }

    private static Color interpolate(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int b2 = (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, b2);
    }
}
