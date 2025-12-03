package com.timestored.qstudio;

public enum TabLayoutPolicy {
	HORIZONTAL_SCROLL, HORIZONTAL, VERTICAL_SCROLL, VERTICAL;

	boolean isVertical() { return this.equals(VERTICAL) || this.equals(VERTICAL_SCROLL); }
	boolean isScroll() { return this.equals(HORIZONTAL_SCROLL) || this.equals(VERTICAL_SCROLL); }
	
	public static TabLayoutPolicy fromString(String uiTabLayout) {
		switch(uiTabLayout.toUpperCase()) {
		case "HORIZONTAL_SCROLL": return HORIZONTAL_SCROLL;
		case "HORIZONTAL": return HORIZONTAL;
		case "VERTICAL_SCROLL": return VERTICAL_SCROLL;
		case "VERTICAL": return VERTICAL;
		}
		return HORIZONTAL_SCROLL;
	}
}