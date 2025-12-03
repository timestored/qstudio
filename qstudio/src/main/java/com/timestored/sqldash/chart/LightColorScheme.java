package com.timestored.sqldash.chart;

import java.awt.Color;

/**
 * Modern light color scheme inspired by Excel/PowerBI.
 * Clean, professional colors with good contrast on white backgrounds.
 */
class LightColorScheme implements ColorScheme {

	@Override public String toString() {
		return "Light Color Scheme";
	}
	
	public final static Color[] SERIES_COLORS;
	private final static Color textColor = Color.decode("#323130");
	
	private static final Color FG_COLOR = Color.decode("#201F1E");
	private static final Color BG_COLOR = Color.WHITE;
	private static final Color ALT_BG_COLOR = Color.decode("#F3F2F1");
	private static final Color SELECTED_BG_COLOR = Color.decode("#DEECF9");
	private static final Color GRIDLINE_COLOR = Color.decode("#777777");
	
	static {
		// Modern Excel/PowerBI inspired palette - saturated but professional
		SERIES_COLORS = new Color[] {
				new Color(68, 114, 196),    // Excel Blue
				new Color(237, 125, 49),    // Excel Orange
				new Color(165, 165, 165),   // Excel Gray
				new Color(255, 192, 0),     // Excel Gold
				new Color(91, 155, 213),    // Excel Light Blue
				new Color(112, 173, 71),    // Excel Green
				new Color(38, 68, 120),     // Excel Dark Blue
				new Color(158, 72, 14),     // Excel Dark Orange
				new Color(99, 99, 99),      // Excel Dark Gray
				new Color(153, 115, 0),     // Excel Dark Gold
				new Color(37, 94, 145),     // Excel Steel Blue
				new Color(67, 104, 43),     // Excel Dark Green
				new Color(124, 175, 221),   // Soft Blue
				new Color(244, 177, 131),   // Soft Orange
				new Color(199, 199, 199),   // Light Gray
				new Color(255, 217, 102),   // Light Gold
				new Color(158, 193, 228),   // Pale Blue
				new Color(169, 209, 142),   // Pale Green
				new Color(47, 85, 151),     // Navy Blue
				new Color(192, 80, 14),     // Rust Orange
				new Color(118, 113, 113),   // Medium Gray
				new Color(190, 144, 0)      // Amber
		};
	}
	
	@Override public Color getBG() { return BG_COLOR; }
	@Override public Color getAltBG() { return ALT_BG_COLOR; }
	@Override public Color getSelectedBG() { return SELECTED_BG_COLOR; }
	@Override public Color getFG() { return FG_COLOR; }
	@Override public Color getText() { return textColor; }
	@Override public Color getGridlines() { return GRIDLINE_COLOR; }
	@Override public Color[] getColorArray() { return SERIES_COLORS; }
	
}