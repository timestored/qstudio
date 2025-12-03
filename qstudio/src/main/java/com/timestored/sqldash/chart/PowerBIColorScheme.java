package com.timestored.sqldash.chart;

import java.awt.Color;

/**
 * Color scheme inspired by Microsoft Power BI default visuals.
 * Bold, data-ink-focused colors optimized for business dashboards.
 */
class PowerBIColorScheme implements ColorScheme {

	@Override public String toString() {
		return "Power BI Color Scheme";
	}
	private static final Color TXT_COLOR = Color.decode("#252423");
	private static final Color FG_COLOR = Color.decode("#000000");
	private static final Color BG_COLOR = Color.decode("#F2F2F2");
	private static final Color ALT_BG_COLOR = Color.decode("#E6E6E6");
	private static final Color SELECTED_BG_COLOR = Color.decode("#CCE5FF");
	private static final Color GRIDLINE_COLOR = Color.decode("#C8C8C8");

	// Power BI default theme colors
	private static final Color[] SERIES_COLORS = new Color[] {
			new Color(1, 184, 170),      // Teal (PowerBI primary)
			new Color(253, 98, 94),      // Coral Red
			new Color(51, 77, 178),      // Blue
			new Color(254, 192, 7),      // Yellow/Gold
			new Color(128, 128, 128),    // Gray
			new Color(55, 141, 189),     // Ocean Blue
			new Color(138, 194, 74),     // Green
			new Color(195, 77, 136),     // Magenta/Pink
			new Color(95, 95, 95),       // Dark Gray
			new Color(231, 136, 56),     // Orange
			new Color(70, 130, 180),     // Steel Blue
			new Color(107, 163, 61),     // Olive Green
			new Color(63, 150, 168),     // Cyan
			new Color(226, 107, 98),     // Light Red
			new Color(88, 108, 188),     // Slate Blue
			new Color(223, 180, 52),     // Amber
			new Color(112, 112, 112),    // Medium Gray
			new Color(80, 159, 203),     // Sky Blue
			new Color(165, 207, 116),    // Light Green
			new Color(214, 121, 159),    // Light Pink
			new Color(140, 140, 140),    // Light Gray
			new Color(250, 168, 79)      // Light Orange
	};
	
	@Override public Color getBG() { return BG_COLOR; }
	@Override public Color getAltBG() { return ALT_BG_COLOR; }
	@Override public Color getSelectedBG() { return SELECTED_BG_COLOR; }
	@Override public Color getFG() { return FG_COLOR; }
	@Override public Color getText() { return TXT_COLOR; }
	@Override public Color getGridlines() { return GRIDLINE_COLOR; }
	@Override public Color[] getColorArray() { return SERIES_COLORS; }
}
