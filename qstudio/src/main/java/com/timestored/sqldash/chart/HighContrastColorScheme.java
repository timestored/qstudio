package com.timestored.sqldash.chart;

import java.awt.Color;

/**
 * High contrast color scheme for accessibility and data clarity.
 * Uses the Tableau colorblind-safe palette with strong contrast.
 */
class HighContrastColorScheme implements ColorScheme {

	@Override public String toString() {
		return "High Contrast";
	}
	
	private static final Color TXT_COLOR = Color.decode("#000000");
	private static final Color FG_COLOR = Color.decode("#000000");
	private static final Color BG_COLOR = Color.WHITE;
	private static final Color ALT_BG_COLOR = Color.decode("#EEEEEE");
	private static final Color SELECTED_BG_COLOR = Color.decode("#FFFF00");
	private static final Color GRIDLINE_COLOR = Color.decode("#999999");

	// Tableau colorblind-safe palette - highly distinguishable
	private static final Color[] SERIES_COLORS = new Color[] {
			new Color(0, 107, 164),       // Blue
			new Color(255, 128, 14),      // Orange
			new Color(44, 160, 44),       // Green
			new Color(214, 39, 40),       // Red
			new Color(148, 103, 189),     // Purple
			new Color(140, 86, 75),       // Brown
			new Color(227, 119, 194),     // Pink
			new Color(127, 127, 127),     // Gray
			new Color(188, 189, 34),      // Olive
			new Color(23, 190, 207),      // Cyan
			new Color(31, 119, 180),      // Steel Blue
			new Color(255, 187, 120),     // Light Orange
			new Color(152, 223, 138),     // Light Green
			new Color(255, 152, 150),     // Light Red
			new Color(197, 176, 213),     // Light Purple
			new Color(196, 156, 148),     // Light Brown
			new Color(247, 182, 210),     // Light Pink
			new Color(199, 199, 199),     // Light Gray
			new Color(219, 219, 141),     // Light Olive
			new Color(158, 218, 229),     // Light Cyan
			new Color(57, 59, 121),       // Dark Blue
			new Color(230, 85, 13)        // Dark Orange
	};
	
	@Override public Color getBG() { return BG_COLOR; }
	@Override public Color getAltBG() { return ALT_BG_COLOR; }
	@Override public Color getSelectedBG() { return SELECTED_BG_COLOR; }
	@Override public Color getFG() { return FG_COLOR; }
	@Override public Color getText() { return TXT_COLOR; }
	@Override public Color getGridlines() { return GRIDLINE_COLOR; }
	@Override public Color[] getColorArray() { return SERIES_COLORS; }
}
