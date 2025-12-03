/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2023 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timestored.qstudio;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.UIManager;

import jsyntaxpane.util.Configuration;

/**
 *  A configuration that when applied to a jsyntaxpane styles it.
 */
public class EditorConfigFactory {

	private static final EditorConfig LIGHT = new LightEditorConfig();
	private static final EditorConfig DARK = new DarkEditorConfig();
	private static final EditorConfig INTELLIJ = new IntelliJEditorConfig();
	private static final EditorConfig FLAT_LIGHT = new FlatLightEditorConfig();
	private static final EditorConfig FLAT_DARK = new FlatDarkEditorConfig();
	private static final EditorConfig MAC_FLAT_LIGHT = new MacFlatLightEditorConfig();
	private static final EditorConfig MAC_FLAT_DARK = new MacFlatDarkEditorConfig();
	private static final EditorConfig VSCODE = new VSCodeEditorConfig();
	private static final EditorConfig DARCULA = new DarculaEditorConfig();
	
	private static final Map<String, EditorConfig> THEME_MAP = new LinkedHashMap<>();
	static {
		THEME_MAP.put("Light", LIGHT);
		THEME_MAP.put("Dark", DARK);
		THEME_MAP.put("IntelliJ", INTELLIJ);
		THEME_MAP.put("Flat Light", FLAT_LIGHT);
		THEME_MAP.put("Flat Dark", FLAT_DARK);
		THEME_MAP.put("Mac Flat Light", MAC_FLAT_LIGHT);
		THEME_MAP.put("Mac Flat Dark", MAC_FLAT_DARK);
		THEME_MAP.put("VSCode", VSCODE);
		THEME_MAP.put("Darcula", DARCULA);
	}
	
	public static interface EditorConfig {
		void apply(Configuration configuration);
		/** @return true if this is a dark theme */
		default boolean isDark() { return false; }
	}

	
	public static EditorConfig get() { return LIGHT; }
	
	public static enum TCOLOR { LIGHT, DARK }
	public static EditorConfig get(TCOLOR name) {
		if(name != null && name.equals(TCOLOR.DARK)) {
			return DARK;
		}
		return LIGHT;
	}
	
	/** Get editor config by theme name 
	 * @param isDarkTheme */
	public static EditorConfig getByName(String themeName, boolean isDarkTheme) {
		EditorConfig config = THEME_MAP.get(themeName);
		return config != null ? config : (isDarkTheme ? DARK : LIGHT);
	}
	
	public static String[] getNames() { return THEME_MAP.keySet().toArray(new String[0]); }
	
	/** Helper method to apply common base settings */
	private static void applyBase(Configuration c, boolean isDark) {
		c.put("SingleColorSelect", "false");
		c.put("RightMarginColumn", isDark ? "0" : "80");
		c.put("RightMarginColor", isDark ? "0x222222" : "0xEEEEEE");
	}
	
	private static String getColor(String name, String defColor) {
		Color c = UIManager.getColor(name);
		return c != null ? ("0x"+Integer.toHexString(c.getRGB()).substring(2)) : defColor;
	}
	
	private static class DarkEditorConfig implements EditorConfig {

		final String FG = "ABB2BF";
		
		@Override public void apply(Configuration c) {
//			# These are the various Attributes for each TokenType.
//			# The keys of this map are the TokenType Strings, and the values are:
//			# color (hex, or integer), Font.Style attribute
//			# Style is one of: 0 = plain, 1=bold, 2=italic, 3=bold/italic
			c.put("Style.COMMENT","0x777777, 2");
			c.put("Style.COMMENT2","0x339933, 3");
			c.put("Style.TYPE","0x56B6C2, 2");  // Boolean / byte / short
			c.put("Style.NUMBER","0x999933, 0");  // qSQL
			c.put("Style.REGEX","0xE5A07B, 0");  // ()[]
			c.put("Style.OPERATOR","0xCCCCCC, 0");  // <= > + - 
			c.put("Style.STRING","0x98C379, 0");  // qSQL - strings "blah" - green
			c.put("Style.STRING2","0x689349, 0");
			c.put("Style.DELIMITER","0xC678DD, 0"); // {} - purple
			c.put("Style.TYPE3","0xE5A07B, 0");
			c.put("Style.ERROR","0xCC0000, 3");
			c.put("Style.DEFAULT","0xFFFF00, 0");
			c.put("Style.KEYWORD","0x61AFEF, 0");  // xlog while sums
			c.put("Style.KEYWORD2","0x9F73A0, 0"); // .z.* .Q.*
			c.put("Style.TYPE2","0xC07F54, 0"); // Standard SQL keywords
			c.put("Style.WARNING","0xcc0000, 0"); 
			c.put("Style.IDENTIFIER","0x"+FG+", 0");   // qSQL - lots of text e.g. symbols and colNa


			c.put("SelectionColor","0x99ccff");
			c.put("CaretColor", getColor("TextArea.caretForeground", "0xeeffcc")); // 
			c.put("PairMarker.Color","0xAA0000");
			c.put("TokenMarker.Color","0x214FAF");
			c.put("LineNumbers.CurrentBack","0x222211");
			c.put("LineNumbers.Foreground","0xAAAADD");
			c.put("LineNumbers.Background","0x333333");

			c.put("SelectionColor","0x1659BB");
			c.put("SingleColorSelect", "false"); // true = turns off syntax highlighting when selected
			c.put("RightMarginColumn", "0");
			c.put("RightMarginColor", "0x222222");
		}

		@Override public boolean isDark() { return true; }
	}
	
	private static class LightEditorConfig implements EditorConfig {

//		# Style is one of: 0 = plain, 1=bold, 2=italic, 3=bold/italic
		@Override public void apply(Configuration c) {
			c.put("Style.COMMENT","0x33AA33, 2");
			c.put("Style.COMMENT2","0x33AA33, 3");
			c.put("Style.TYPE","0x56B6C2, 2");  // Boolean / byte / short
			c.put("Style.NUMBER","0x999933, 0");  // qSQL
			c.put("Style.REGEX","0x85502B, 0");  // ()[]
			c.put("Style.OPERATOR","0x222244, 0");  // <= > + - 
			c.put("Style.STRING","0xCC6600, 0");  // qSQL - strings "blah" - green
			c.put("Style.STRING2","0x663300, 0"); 
			c.put("Style.DELIMITER","0xA658BD, 0"); // {} - purple
			c.put("Style.TYPE3","0xE5A07B, 0");
			c.put("Style.ERROR","0xCC0000, 3");
			c.put("Style.DEFAULT","0x000000, 0");
			c.put("Style.KEYWORD","0x3333ee, 0");  // xlog while sums
			c.put("Style.KEYWORD2","0x2222CC, 0"); // .z.* .Q.*
			c.put("Style.TYPE2","0xC07F54, 0"); // Standard SQL keywords
			c.put("Style.WARNING","0xCC0000, 0"); 
			c.put("Style.IDENTIFIER","0x000000, 0");   // qSQL - lots of text e.g. symbols and colNa

			c.put("SelectionColor","0x99ccff");
			c.put("CaretColor","0x000000");
			c.put("PairMarker.Color","0xFF5555");
			c.put("TokenMarker.Color","0xEEAAFF");
			c.put("LineNumbers.CurrentBack","0xcccccc");
			c.put("LineNumbers.Foreground","0x333333");
			c.put("LineNumbers.Background","0xe6e6e6");

			c.put("SingleColorSelect", "false"); // true = turns off syntax highlighting when selected
			c.put("RightMarginColumn", "80");
			c.put("RightMarginColor", "0xEEEEEE");
		}
	}
	
	/** IntelliJ Light theme - matches IntelliJ IDEA's default light theme */
	private static class IntelliJEditorConfig implements EditorConfig {
		@Override public void apply(Configuration c) {
			// Syntax highlighting colors matching IntelliJ IDEA Light
			c.put("Style.COMMENT","0x808080, 2");       // Gray italic
			c.put("Style.COMMENT2","0x808080, 3");      // Gray bold italic
			c.put("Style.TYPE","0x000080, 1");          // Navy bold (types)
			c.put("Style.NUMBER","0x0000FF, 0");        // Blue (numbers)
			c.put("Style.REGEX","0x008000, 0");         // Green (regex)
			c.put("Style.OPERATOR","0x000000, 0");      // Black (operators)
			c.put("Style.STRING","0x008000, 0");        // Green (strings)
			c.put("Style.STRING2","0x008000, 1");       // Green bold
			c.put("Style.DELIMITER","0x000000, 0");     // Black (delimiters)
			c.put("Style.TYPE3","0x20999D, 0");         // Teal
			c.put("Style.ERROR","0xFF0000, 3");         // Red bold italic
			c.put("Style.DEFAULT","0x000000, 0");       // Black
			c.put("Style.KEYWORD","0x000080, 1");       // Navy bold (keywords)
			c.put("Style.KEYWORD2","0x660E7A, 0");      // Purple (secondary keywords)
			c.put("Style.TYPE2","0x000080, 1");         // Navy bold (SQL keywords)
			c.put("Style.WARNING","0xBF8000, 0");       // Orange
			c.put("Style.IDENTIFIER","0x000000, 0");    // Black

			c.put("SelectionColor","0xA6D2FF");
			c.put("CaretColor","0x000000");
			c.put("PairMarker.Color","0x99CCBB");
			c.put("TokenMarker.Color","0xFFE4B5");
			c.put("LineNumbers.CurrentBack","0xFCFAED");
			c.put("LineNumbers.Foreground","0x999999");
			c.put("LineNumbers.Background","0xF0F0F0");

			applyBase(c, false);
		}
	}
	
	/** Flat Light theme - modern flat design light theme */
	private static class FlatLightEditorConfig implements EditorConfig {
		@Override public void apply(Configuration c) {
			c.put("Style.COMMENT","0x8C8C8C, 2");       // Gray italic
			c.put("Style.COMMENT2","0x8C8C8C, 3");      // Gray bold italic
			c.put("Style.TYPE","0x1750EB, 0");          // Bright blue
			c.put("Style.NUMBER","0x1750EB, 0");        // Bright blue
			c.put("Style.REGEX","0xC77DBB, 0");         // Pink
			c.put("Style.OPERATOR","0x000000, 0");      // Black
			c.put("Style.STRING","0x067D17, 0");        // Green
			c.put("Style.STRING2","0x067D17, 1");       // Green bold
			c.put("Style.DELIMITER","0x871094, 0");     // Purple
			c.put("Style.TYPE3","0x00627A, 0");         // Dark cyan
			c.put("Style.ERROR","0xBC0404, 3");         // Dark red bold italic
			c.put("Style.DEFAULT","0x080808, 0");       // Near black
			c.put("Style.KEYWORD","0x0033B3, 1");       // Dark blue bold
			c.put("Style.KEYWORD2","0x9E880D, 0");      // Yellow-brown
			c.put("Style.TYPE2","0x0033B3, 1");         // Dark blue bold
			c.put("Style.WARNING","0xBE8800, 0");       // Orange
			c.put("Style.IDENTIFIER","0x080808, 0");    // Near black

			c.put("SelectionColor","0xA6D2FF");
			c.put("CaretColor","0x000000");
			c.put("PairMarker.Color","0xB4D8FD");
			c.put("TokenMarker.Color","0xFFE4B5");
			c.put("LineNumbers.CurrentBack","0xE8E8E8");
			c.put("LineNumbers.Foreground","0x888888");
			c.put("LineNumbers.Background","0xF2F2F2");

			applyBase(c, false);
		}
	}
	
	/** Flat Dark theme - modern flat design dark theme */
	private static class FlatDarkEditorConfig implements EditorConfig {
		@Override public void apply(Configuration c) {
			c.put("Style.COMMENT","0x7A7E85, 2");       // Gray italic
			c.put("Style.COMMENT2","0x7A7E85, 3");      // Gray bold italic
			c.put("Style.TYPE","0x6897BB, 0");          // Blue
			c.put("Style.NUMBER","0x6897BB, 0");        // Blue
			c.put("Style.REGEX","0xC77DBB, 0");         // Pink
			c.put("Style.OPERATOR","0xA9B7C6, 0");      // Light gray
			c.put("Style.STRING","0x6A8759, 0");        // Green
			c.put("Style.STRING2","0x6A8759, 1");       // Green bold
			c.put("Style.DELIMITER","0xCC7832, 0");     // Orange
			c.put("Style.TYPE3","0x507874, 0");         // Teal
			c.put("Style.ERROR","0xFF5555, 3");         // Red bold italic
			c.put("Style.DEFAULT","0xA9B7C6, 0");       // Light gray
			c.put("Style.KEYWORD","0xCC7832, 1");       // Orange bold
			c.put("Style.KEYWORD2","0xB09D79, 0");      // Tan
			c.put("Style.TYPE2","0xCC7832, 1");         // Orange bold
			c.put("Style.WARNING","0xBBB529, 0");       // Yellow
			c.put("Style.IDENTIFIER","0xA9B7C6, 0");    // Light gray

			c.put("SelectionColor","0x214283");
			c.put("CaretColor", getColor("TextArea.caretForeground", "0xBBBBBB"));
			c.put("PairMarker.Color","0x3B514D");
			c.put("TokenMarker.Color","0x323232");
			c.put("LineNumbers.CurrentBack","0x323232");
			c.put("LineNumbers.Foreground","0x606366");
			c.put("LineNumbers.Background","0x2B2B2B");

			applyBase(c, true);
		}
		@Override public boolean isDark() { return true; }
	}
	
	/** Mac Flat Light theme - Apple-inspired light theme */
	private static class MacFlatLightEditorConfig implements EditorConfig {
		@Override public void apply(Configuration c) {
			c.put("Style.COMMENT","0x8E8E93, 2");       // System gray italic
			c.put("Style.COMMENT2","0x8E8E93, 3");      // System gray bold italic
			c.put("Style.TYPE","0x007AFF, 0");          // System blue
			c.put("Style.NUMBER","0x5856D6, 0");        // System purple
			c.put("Style.REGEX","0xAF52DE, 0");         // System purple
			c.put("Style.OPERATOR","0x1C1C1E, 0");      // System black
			c.put("Style.STRING","0x34C759, 0");        // System green
			c.put("Style.STRING2","0x30D158, 1");       // Green bold
			c.put("Style.DELIMITER","0xFF2D55, 0");     // System pink
			c.put("Style.TYPE3","0x5AC8FA, 0");         // System teal
			c.put("Style.ERROR","0xFF3B30, 3");         // System red bold italic
			c.put("Style.DEFAULT","0x1C1C1E, 0");       // System black
			c.put("Style.KEYWORD","0x007AFF, 1");       // System blue bold
			c.put("Style.KEYWORD2","0xFF9500, 0");      // System orange
			c.put("Style.TYPE2","0x007AFF, 1");         // System blue bold
			c.put("Style.WARNING","0xFF9500, 0");       // System orange
			c.put("Style.IDENTIFIER","0x1C1C1E, 0");    // System black

			c.put("SelectionColor","0xB4D8FD");
			c.put("CaretColor","0x007AFF");
			c.put("PairMarker.Color","0xD1E8FF");
			c.put("TokenMarker.Color","0xFFF0D4");
			c.put("LineNumbers.CurrentBack","0xE5E5EA");
			c.put("LineNumbers.Foreground","0x8E8E93");
			c.put("LineNumbers.Background","0xF2F2F7");

			applyBase(c, false);
		}
	}
	
	/** Mac Flat Dark theme - Apple-inspired dark theme */
	private static class MacFlatDarkEditorConfig implements EditorConfig {
		@Override public void apply(Configuration c) {
			c.put("Style.COMMENT","0x8E8E93, 2");       // System gray italic
			c.put("Style.COMMENT2","0x8E8E93, 3");      // System gray bold italic
			c.put("Style.TYPE","0x0A84FF, 0");          // System blue (dark mode)
			c.put("Style.NUMBER","0x5E5CE6, 0");        // System indigo
			c.put("Style.REGEX","0xBF5AF2, 0");         // System purple (dark mode)
			c.put("Style.OPERATOR","0xF5F5F7, 0");      // System white
			c.put("Style.STRING","0x30D158, 0");        // System green (dark mode)
			c.put("Style.STRING2","0x32D74B, 1");       // Green bold
			c.put("Style.DELIMITER","0xFF375F, 0");     // System pink (dark mode)
			c.put("Style.TYPE3","0x64D2FF, 0");         // System teal (dark mode)
			c.put("Style.ERROR","0xFF453A, 3");         // System red (dark mode) bold italic
			c.put("Style.DEFAULT","0xF5F5F7, 0");       // System white
			c.put("Style.KEYWORD","0x0A84FF, 1");       // System blue (dark mode) bold
			c.put("Style.KEYWORD2","0xFF9F0A, 0");      // System orange (dark mode)
			c.put("Style.TYPE2","0x0A84FF, 1");         // System blue (dark mode) bold
			c.put("Style.WARNING","0xFF9F0A, 0");       // System orange (dark mode)
			c.put("Style.IDENTIFIER","0xF5F5F7, 0");    // System white

			c.put("SelectionColor","0x0A4F7A");
			c.put("CaretColor", getColor("TextArea.caretForeground", "0x0A84FF"));
			c.put("PairMarker.Color","0x1A3A5C");
			c.put("TokenMarker.Color","0x3A3A3C");
			c.put("LineNumbers.CurrentBack","0x3A3A3C");
			c.put("LineNumbers.Foreground","0x636366");
			c.put("LineNumbers.Background","0x1C1C1E");

			applyBase(c, true);
		}
		@Override public boolean isDark() { return true; }
	}
	
	/** VSCode theme - matches Visual Studio Code's default dark+ theme */
	private static class VSCodeEditorConfig implements EditorConfig {
		@Override public void apply(Configuration c) {
			c.put("Style.COMMENT","0x6A9955, 2");       // Green italic
			c.put("Style.COMMENT2","0x6A9955, 3");      // Green bold italic
			c.put("Style.TYPE","0x4EC9B0, 0");          // Teal (types)
			c.put("Style.NUMBER","0xB5CEA8, 0");        // Light green (numbers)
			c.put("Style.REGEX","0xD16969, 0");         // Red (regex)
			c.put("Style.OPERATOR","0xD4D4D4, 0");      // Light gray
			c.put("Style.STRING","0xCE9178, 0");        // Orange (strings)
			c.put("Style.STRING2","0xCE9178, 1");       // Orange bold
			c.put("Style.DELIMITER","0xFFD700, 0");     // Gold (brackets)
			c.put("Style.TYPE3","0x4EC9B0, 0");         // Teal
			c.put("Style.ERROR","0xF44747, 3");         // Red bold italic
			c.put("Style.DEFAULT","0xD4D4D4, 0");       // Light gray
			c.put("Style.KEYWORD","0x569CD6, 1");       // Blue bold (keywords)
			c.put("Style.KEYWORD2","0xC586C0, 0");      // Pink (secondary keywords)
			c.put("Style.TYPE2","0x569CD6, 1");         // Blue bold (SQL keywords)
			c.put("Style.WARNING","0xDCDCAA, 0");       // Yellow
			c.put("Style.IDENTIFIER","0x9CDCFE, 0");    // Light blue (identifiers)

			c.put("SelectionColor","0x264F78");
			c.put("CaretColor", getColor("TextArea.caretForeground", "0xAEAFAD"));
			c.put("PairMarker.Color","0x303030");
			c.put("TokenMarker.Color","0x373737");
			c.put("LineNumbers.CurrentBack","0x282828");
			c.put("LineNumbers.Foreground","0x858585");
			c.put("LineNumbers.Background","0x1E1E1E");

			applyBase(c, true);
		}
		@Override public boolean isDark() { return true; }
	}
	
	/** Darcula theme - classic IntelliJ dark theme */
	private static class DarculaEditorConfig implements EditorConfig {
		@Override public void apply(Configuration c) {
			c.put("Style.COMMENT","0x808080, 2");       // Gray italic
			c.put("Style.COMMENT2","0x629755, 3");      // Green bold italic (doc comments)
			c.put("Style.TYPE","0x6897BB, 0");          // Blue (types)
			c.put("Style.NUMBER","0x6897BB, 0");        // Blue (numbers)
			c.put("Style.REGEX","0x6A8759, 0");         // Green (regex)
			c.put("Style.OPERATOR","0xA9B7C6, 0");      // Light gray
			c.put("Style.STRING","0x6A8759, 0");        // Green (strings)
			c.put("Style.STRING2","0x6A8759, 1");       // Green bold
			c.put("Style.DELIMITER","0xA9B7C6, 0");     // Light gray
			c.put("Style.TYPE3","0x9876AA, 0");         // Purple
			c.put("Style.ERROR","0xBC3F3C, 3");         // Red bold italic
			c.put("Style.DEFAULT","0xA9B7C6, 0");       // Light gray
			c.put("Style.KEYWORD","0xCC7832, 1");       // Orange bold (keywords)
			c.put("Style.KEYWORD2","0xFFC66D, 0");      // Yellow (secondary keywords)
			c.put("Style.TYPE2","0xCC7832, 1");         // Orange bold (SQL keywords)
			c.put("Style.WARNING","0xBBB529, 0");       // Yellow
			c.put("Style.IDENTIFIER","0xA9B7C6, 0");    // Light gray

			c.put("SelectionColor","0x214283");
			c.put("CaretColor", getColor("TextArea.caretForeground", "0xBBBBBB"));
			c.put("PairMarker.Color","0x3B514D");
			c.put("TokenMarker.Color","0x323232");
			c.put("LineNumbers.CurrentBack","0x3A3A3A");
			c.put("LineNumbers.Foreground","0x606366");
			c.put("LineNumbers.Background","0x313335");

			applyBase(c, true);
		}
		@Override public boolean isDark() { return true; }
	}
	
}
