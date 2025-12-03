package com.timestored.sqldash.chart;

import java.awt.Color;

/**
 * Color scheme inspired by Tableau's default "Tableau 10" palette.
 * Professional, colorblind-safe colors widely used in analytics dashboards.
 */
class TableauColorScheme implements ColorScheme {

    @Override public String toString() {
        return "Tableau Color Scheme";
    }

    // Tableau-style light canvas colors
    private static final Color TXT_COLOR          = Color.decode("#1F1F1F");
    private static final Color FG_COLOR           = Color.decode("#000000");
    private static final Color BG_COLOR           = Color.decode("#FFFFFF");
    private static final Color ALT_BG_COLOR       = Color.decode("#F7F7F7");
    private static final Color SELECTED_BG_COLOR  = Color.decode("#D9EAF7");
    private static final Color GRIDLINE_COLOR     = Color.decode("#D0D0D0");

    // Tableau 10 palette
    private static final Color[] SERIES_COLORS = new Color[] {
            Color.decode("#4E79A7"),  // Blue
            Color.decode("#F28E2B"),  // Orange
            Color.decode("#E15759"),  // Red
            Color.decode("#76B7B2"),  // Teal
            Color.decode("#59A14F"),  // Green
            Color.decode("#EDC948"),  // Yellow
            Color.decode("#B07AA1"),  // Purple
            Color.decode("#FF9DA7"),  // Pink
            Color.decode("#9C755F"),  // Brown
            Color.decode("#BAB0AC")   // Gray
    };

    @Override public Color getBG()         { return BG_COLOR; }
    @Override public Color getAltBG()      { return ALT_BG_COLOR; }
    @Override public Color getSelectedBG() { return SELECTED_BG_COLOR; }
    @Override public Color getFG()         { return FG_COLOR; }
    @Override public Color getText()       { return TXT_COLOR; }
    @Override public Color getGridlines()  { return GRIDLINE_COLOR; }
    @Override public Color[] getColorArray() { return SERIES_COLORS; }
}
