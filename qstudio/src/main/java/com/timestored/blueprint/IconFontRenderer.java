package com.timestored.blueprint;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders icons from a TrueType icon font (TTF) dynamically with
 * arbitrary size and color. Uses internal caching for fast reuse.
 */
public final class IconFontRenderer {

    private static final Font iconFont;
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();

    static {
		 try (InputStream is = IconFontRenderer.class.getResourceAsStream("blueprint-icons-20.ttf")) {
	            iconFont = Font.createFont(Font.TRUETYPE_FONT, is);
		 } catch (IOException | FontFormatException e) {
		     throw new RuntimeException("Failed to load default icon font", e);
		 }
    }
    
    private IconFontRenderer() {}

    
    /**
     * Get a colored, dynamically rendered icon from the glyph character.
     *
     * @param glyph the Unicode glyph in the font (e.g., '\uE13F')
     * @param size  output icon size in pixels (e.g., 16, 24, 32)
     * @param color icon color
     * @return cached ImageIcon
     */
    public static ImageIcon get(char glyph, int size, Color color) {
        if (iconFont == null) {
            throw new IllegalStateException("IconFontRenderer.loadFont() must be called before usage.");
        }

        final String key = glyph + "_" + size + "_" + color.getRGB();

        return CACHE.computeIfAbsent(key, k -> render(glyph, size, color));
    }

    /**
     * Render the glyph using Java2D into a high-quality ARGB image.
     */
    private static ImageIcon render(char glyph, int size, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // High-quality text rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Scale font
        Font f = iconFont.deriveFont((float) size);
        g.setFont(f);
        g.setColor(color);

        // Glyph layout
        FontMetrics fm = g.getFontMetrics();
        int x = (size - fm.charWidth(glyph)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();

        g.drawString(String.valueOf(glyph), x, y);

        g.dispose();
        return new ImageIcon(img);
    }
}
