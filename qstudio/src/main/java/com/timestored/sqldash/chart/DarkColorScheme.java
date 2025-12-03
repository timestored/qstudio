package com.timestored.sqldash.chart;

import java.awt.Color;

/**
 * Modern dark color scheme inspired by PowerBI Dark Mode and VS Code.
 * Professional colors with excellent visibility on dark backgrounds.
 */
public class DarkColorScheme implements ColorScheme {

	@Override public String toString() {
		return "Dark Color Scheme";
	}
	
	private static final Color TXT_COLOR = Color.decode("#E1E1E1");
	private static final Color FG_COLOR = Color.decode("#F3F3F3");
	private static final Color BG_COLOR = Color.decode("#1E1E1E");
	private static final Color ALT_BG_COLOR = Color.decode("#2D2D2D");
	private static final Color SELECTED_BG_COLOR = Color.decode("#264F78");
	private static final Color GRIDLINE_COLOR = Color.decode("#3C3C3C");

	// Bright, saturated colors that work well on dark backgrounds
	private static final Color[] DARK_SERIES_COLORS = new Color[] {
			new Color(86, 156, 214),     // VS Code Blue
			new Color(220, 140, 84),     // Soft Orange
			new Color(181, 206, 168),    // Soft Green
			new Color(214, 157, 133),    // Salmon Pink
			new Color(156, 220, 254),    // Light Cyan
			new Color(197, 134, 192),    // Purple/Lavender
			new Color(78, 201, 176),     // Teal
			new Color(241, 195, 80),     // Gold/Yellow
			new Color(215, 186, 125),    // Tan
			new Color(186, 162, 206),    // Light Purple
			new Color(134, 198, 190),    // Seafoam
			new Color(209, 122, 115),    // Coral
			new Color(100, 178, 237),    // Sky Blue
			new Color(249, 174, 88),     // Bright Orange
			new Color(162, 217, 145),    // Light Green
			new Color(249, 156, 139),    // Light Coral
			new Color(138, 190, 245),    // Pastel Blue
			new Color(185, 142, 184),    // Mauve
			new Color(68, 182, 159),     // Medium Teal
			new Color(230, 180, 88),     // Amber
			new Color(200, 170, 140),    // Warm Gray
			new Color(175, 155, 200)     // Periwinkle
	};
	
	@Override public Color getBG() { return BG_COLOR; }
	@Override public Color getAltBG() { return ALT_BG_COLOR; }
	@Override public Color getSelectedBG() { return SELECTED_BG_COLOR; }
	@Override public Color getFG() { return FG_COLOR; }
	@Override public Color getText() { return TXT_COLOR; }
	@Override public Color getGridlines() { return GRIDLINE_COLOR; }
	@Override public Color[] getColorArray() { return DARK_SERIES_COLORS; }
	
}