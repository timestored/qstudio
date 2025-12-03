package com.timestored.sqldash.chart;

import java.awt.Color;

/**
 * Original qStudio dark color scheme - the classic dark theme.
 * Uses the original series colors from LightColorScheme on a dark background.
 */
class DarkOriginalColorScheme implements ColorScheme {

	
	@Override public String toString() {
		return "Dark Original Color Scheme";
	}
	private static final Color TXT_COLOR = Color.decode("#DDDDDD");
	private static final Color FG_COLOR = Color.decode("#EEEEEE");
	private static final Color BG_COLOR = Color.decode("#111217");
	private static final Color ALT_BG_COLOR = Color.decode("#333333");
	private static final Color SELECTED_BG_COLOR = Color.decode("#630802");
	
	// Original qStudio series colors - matches the original LightColorScheme SERIES_COLORS
	// These were generated with http://tools.medialab.sciences-po.fr/iwanthue/
	private static final Color[] SERIES_COLORS = new Color[] {
			new Color(220, 61, 50),
			new Color(61, 140, 167),
			new Color(205, 81, 217),
			new Color(81, 172, 50),
			new Color(168, 90, 132),
			new Color(191, 139, 44),
			new Color(53, 116, 67),
			new Color(110, 109, 215),
			new Color(100, 99, 150),
			new Color(214, 62, 109),
			new Color(164, 77, 46),
			new Color(151, 158, 47),
			new Color(163, 93, 178),
			new Color(210, 70, 154),
			new Color(47, 151, 131),
			new Color(221, 111, 45),
			new Color(95, 110, 35),
			new Color(115, 147, 212),
			new Color(141, 95, 34),
			new Color(76, 169, 105),
			new Color(191, 85, 87),
			new Color(80, 139, 48)
	};
	
	@Override public Color getBG() { return BG_COLOR; }
	@Override public Color getAltBG() { return ALT_BG_COLOR; }
	@Override public Color getSelectedBG() { return SELECTED_BG_COLOR; }
	@Override public Color getFG() { return FG_COLOR; }
	@Override public Color getText() { return TXT_COLOR; }
	@Override public Color getGridlines() { return Color.LIGHT_GRAY; }
	@Override public Color[] getColorArray() { return SERIES_COLORS; }
	
}
