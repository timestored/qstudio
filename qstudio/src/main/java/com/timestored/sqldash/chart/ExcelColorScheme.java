package com.timestored.sqldash.chart;

import java.awt.Color;

/**
 * Color scheme matching Microsoft Excel's default chart colors.
 * Classic Excel palette on a clean white background.
 */
class ExcelColorScheme implements ColorScheme {

	@Override public String toString() {
		return "Excel Color Scheme";
	}
	
	private static final Color TXT_COLOR = Color.decode("#3B3838");
	private static final Color FG_COLOR = Color.decode("#262626");
	private static final Color BG_COLOR = Color.WHITE;
	private static final Color ALT_BG_COLOR = Color.decode("#F0F0F0");
	private static final Color SELECTED_BG_COLOR = Color.decode("#B4D5FE");
	private static final Color GRIDLINE_COLOR = Color.decode("#D9D9D9");

	// Exact Excel 2019/365 default chart colors
	private static final Color[] SERIES_COLORS = new Color[] {
			new Color(91, 155, 213),     // Blue
			new Color(237, 125, 49),     // Orange
			new Color(165, 165, 165),    // Gray
			new Color(255, 192, 0),      // Gold
			new Color(68, 114, 196),     // Dark Blue
			new Color(112, 173, 71),     // Green
			new Color(37, 94, 145),      // Navy
			new Color(158, 72, 14),      // Brown
			new Color(99, 99, 99),       // Dark Gray
			new Color(153, 115, 0),      // Dark Gold
			new Color(0, 112, 192),      // Ocean Blue
			new Color(67, 104, 43),      // Dark Green
			new Color(124, 175, 221),    // Light Blue
			new Color(244, 177, 131),    // Light Orange
			new Color(199, 199, 199),    // Light Gray
			new Color(255, 217, 102),    // Light Gold
			new Color(158, 193, 228),    // Pale Blue
			new Color(169, 209, 142),    // Pale Green
			new Color(47, 85, 151),      // Steel Blue
			new Color(192, 80, 14),      // Rust
			new Color(128, 128, 128),    // Medium Gray
			new Color(184, 138, 0)       // Mustard
	};
	
	@Override public Color getBG() { return BG_COLOR; }
	@Override public Color getAltBG() { return ALT_BG_COLOR; }
	@Override public Color getSelectedBG() { return SELECTED_BG_COLOR; }
	@Override public Color getFG() { return FG_COLOR; }
	@Override public Color getText() { return TXT_COLOR; }
	@Override public Color getGridlines() { return GRIDLINE_COLOR; }
	@Override public Color[] getColorArray() { return SERIES_COLORS; }
}
