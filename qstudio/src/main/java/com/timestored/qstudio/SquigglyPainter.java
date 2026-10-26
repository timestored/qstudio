package com.timestored.qstudio;

import java.awt.*;
import javax.swing.text.*;

/**
 * Custom highlight painter that draws a wavy/squiggly underline beneath text.
 * Colors adjust for dark/light themes based on the editor background.
 */
class SquigglyPainter extends DefaultHighlighter.DefaultHighlightPainter {

    static final SquigglyPainter ERROR = new SquigglyPainter(Color.RED, new Color(255, 80, 80));
    static final SquigglyPainter WARNING = new SquigglyPainter(new Color(200, 120, 0), new Color(255, 170, 50));
    static final SquigglyPainter INFO = new SquigglyPainter(new Color(0, 120, 200), new Color(80, 170, 255));

    private final Color lightColor;
    private final Color darkColor;

    SquigglyPainter(Color lightColor, Color darkColor) {
        super(null);
        this.lightColor = lightColor;
        this.darkColor = darkColor;
    }

    @Override
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
        try {
            Rectangle r0 = c.modelToView(offs0);
            Rectangle r1 = c.modelToView(offs1);
            if (r0 == null || r1 == null) return null;

            // Pick color based on editor background brightness
            Color bg = c.getBackground();
            boolean dark = bg != null && (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3 < 128;

            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(dark ? darkColor : lightColor);
            g2.setStroke(new BasicStroke(1.0f));

            int y = r0.y + r0.height - 2;
            int x1 = r0.x;
            int x2 = r1.x + r1.width;
            int wave = 2;
            for (int x = x1; x < x2; x += wave * 2) {
                g2.drawLine(x, y, x + wave, y - wave);
                g2.drawLine(x + wave, y - wave, x + wave * 2, y);
            }
            return r0.union(r1);
        } catch (BadLocationException e) {
            return null;
        }
    }
}
