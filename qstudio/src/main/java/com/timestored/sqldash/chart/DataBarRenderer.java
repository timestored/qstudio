package com.timestored.sqldash.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

public class DataBarRenderer implements TableCellRenderer {
	  private final DataBarComponent comp = new DataBarComponent();

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        double v = 0.0;
        if (value instanceof Number) {
            v = ((Number) value).doubleValue();
        }

        comp.value = v;
        comp.tooltip = String.format("%.0f%%", v * 100);

        // selection + striping friendly
        if (isSelected) {
            comp.setForeground(table.getSelectionForeground());
            comp.setBackground(table.getSelectionBackground());
        } else {
            comp.setForeground(table.getForeground());
            comp.setBackground(table.getBackground());
        }

        return comp;
    }

    static class DataBarComponent extends JComponent {
        double value = 0.0;
        String tooltip = "";

        @Override public String getToolTipText() { return tooltip; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            double pct = Math.min(1.0, Math.max(0.0, value));
            int w = getWidth();
            int h = getHeight();

            Graphics2D g2 = (Graphics2D) g.create();

            // Background (very light to allow striping behind it)
            Color bg = UIManager.getColor("Table.background");
            Color fillBg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 90);
            g2.setColor(fillBg);
            g2.fillRect(2, 2, w - 4, h - 4);

            // Bar color: green ≤ 1, red > 1
            Color green = new Color(60, 170, 60);
            Color red   = new Color(200, 50, 50);
            Color bar   = (value > 1.0) ? red : green;

            int fillW = (int) ((w - 4) * pct);
            g2.setColor(bar);
            g2.fillRect(2, 2, fillW, h - 4);

            // Border
            g2.setColor(getForeground());
            g2.drawRect(2, 2, w - 4, h - 4);

            g2.dispose();
        }
    }


}

