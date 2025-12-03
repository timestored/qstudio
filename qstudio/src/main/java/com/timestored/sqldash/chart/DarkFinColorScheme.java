package com.timestored.sqldash.chart;

import java.awt.Color;

/**
 * Dark finance-themed color scheme for trading/financial applications.
 * Inspired by Grafana dark theme and professional trading interfaces.
 * Uses high-contrast, saturated colors typical of financial dashboards.
 */
class DarkFinColorScheme implements ColorScheme {

	@Override public String toString() {
		return "Dark Finance Color Scheme";
	}
	
	// Deep black background typical of trading terminals
	private static final Color TXT_COLOR = Color.decode("#C8C8C8");
	private static final Color FG_COLOR = Color.decode("#E0E0E0");
	private static final Color BG_COLOR = Color.decode("#0D0D0D");
	private static final Color ALT_BG_COLOR = Color.decode("#1A1A1A");
	private static final Color SELECTED_BG_COLOR = Color.decode("#1F3A5F");
	private static final Color GRIDLINE_COLOR = Color.decode("#2A2A2A");

	// Finance-oriented colors: greens for profit, reds for loss, blues for neutral
	// Inspired by Bloomberg Terminal, Grafana dark, and trading platforms
	private static final Color[] SERIES_COLORS = new Color[] {
			new Color(0, 200, 83),        // Profit Green (bright)
			new Color(255, 82, 82),       // Loss Red (bright)
			new Color(30, 136, 229),      // Blue (primary)
			new Color(255, 193, 7),       // Gold/Warning Yellow
			new Color(0, 230, 118),       // Bright Green
			new Color(255, 138, 101),     // Soft Red/Coral
			new Color(41, 182, 246),      // Light Blue
			new Color(255, 167, 38),      // Orange
			new Color(102, 187, 106),     // Medium Green
			new Color(239, 83, 80),       // Bright Red
			new Color(66, 165, 245),      // Sky Blue
			new Color(255, 213, 79),      // Light Gold
			new Color(129, 199, 132),     // Soft Green
			new Color(229, 115, 115),     // Soft Red
			new Color(100, 181, 246),     // Pastel Blue
			new Color(255, 224, 130),     // Pale Yellow
			new Color(165, 214, 167),     // Pale Green
			new Color(239, 154, 154),     // Pale Red
			new Color(144, 202, 249),     // Very Light Blue
			new Color(255, 245, 157),     // Very Light Yellow
			new Color(200, 230, 201),     // Very Pale Green
			new Color(255, 205, 210)      // Very Pale Red
	};
	
	@Override public Color getBG() { return BG_COLOR; }
	@Override public Color getAltBG() { return ALT_BG_COLOR; }
	@Override public Color getSelectedBG() { return SELECTED_BG_COLOR; }
	@Override public Color getFG() { return FG_COLOR; }
	@Override public Color getText() { return TXT_COLOR; }
	@Override public Color getGridlines() { return GRIDLINE_COLOR; }
	@Override public Color[] getColorArray() { return SERIES_COLORS; }
	
}
