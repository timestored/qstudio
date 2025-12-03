package com.timestored.sqldash.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.table.TableColumnExt;

import com.timestored.blueprint.BlueprintIcon;
import com.timestored.misc.ColorUtils;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.kdb.KdbType;

import lombok.Data;

/**
 * Contains various table cell renderers for _SD_COLS 
 */
public class RenderProvider {

	// SAMPLE KDB QUERIES TO GENERATE DATA FOR RENDERER TESTING
	//
	//update shade_SD_SHADE:100-count[i]?200.1,sparkbar_SD_SPARKBAR:{10?10} each i,sparkline_SD_SPARKLINE:{10?10} each i from 
	//update v:1,percent_SD_PERCENT2:count[i]?0.123456,databar_SD_DATABAR:count[i]?1.0 from 
	//update tags_SD_TAG:{" " sv string rand[3]?`4} each i, tag_SD_TAG:count[i]?`3,status_SD_STATUS:count[i]?`waiting`cancelled`rejected`resolved`done`complete from 
	//update dollar_SD_CURUSD:100.123,num5_SD_NUMBER5:num9_SD_NUMBER9 from update num9_SD_NUMBER9:count[i]?100.1233453456 from 
	//    update u2:{"<html>test <b>asd</b> asdad</html>"} each i,u:{"http://yahoo.com"} each i,u2:{"[aa](http://yahoo.com)"} each i from 
	//    ([] dt:2013.01.01+til 21; cosineWave:cos a; sineWave:sin a:0.6*til 21)
	//
	//select v,vl_sd_sparkline:v , sparkarea_sd_sparkarea:v , vb_sd_sparkbar:v ,
	//        vd_sd_sparkdiscrete:v , vbl_sd_sparkbullet:v ,
	//  vpie_sd_sparkpie:v , vbox_sd_sparkboxplot:v  from 
	//  ([] a:1 2 3; v:(asc 9?1 2 3 1 9 8 7 3 -10 -28 7 3 -10 -2; 15?4 27 34 52 54 59  -4 -30 -45 52 54 59 61 68 78 82 85 87 91 93 100 ;6 6 6 6 6 -6 2 2 0))
	//  
	//
	//
	//// DEMO QUERY - DELETE AND REPLACE ME
	//
	//// Table display can be configured using column names. See help->charts for details on format.
	//update percbar_SD_DATABAR:percent_SD_PERCENT0 ,bid_SD_FG:((`$("#FF6666";"#66FF66";""))!`$("#222";"#222";"")) bid_SD_BG from  
	//	 ([] time:.z.t-til 50; 
	//		 status:50?`partial`filled; 
	//		 instrument:50?`GBPUSD`USDNZD`USDCAD`CHFJPY`EURUSD;
	//		 symbol_SD_TAG:50?`UBS`C`MS`HSBC`NOMURA`DB;
	//		 price_SD_CURUSD:50?100.0;
	//		 bid:50?20.0;
	//		 bid_SD_BG:50?`$("#FF6666";"";"";"";"";"";"";"";"";"";"";"";"";"#66FF66");
	//		 bid_SD_CODE:50?("0.xXXx";"0.XXx";"0.xxXX");
	//		 percent_SD_PERCENT0:50?1.0 )
	
	
	public static TableCellRenderer getRendererForColumnName(String name, StringValue sv, boolean negRed) {

	    if (name == null) return null;
	    String n = name.toUpperCase();

	    if (n.matches(".*_SD_NUMBER([0-9])$")) {
	        int d = Character.getNumericValue(n.charAt(n.length() - 1));
	        return new DecimalPlacesRenderer(sv, JLabel.RIGHT, negRed, d);
	    } else if (n.matches(".*_SD_CUR([A-Z]{3})$")) {
	        String iso = n.substring(n.length() - 3);
	        return new CurrencyRenderer(sv, JLabel.RIGHT, negRed, iso);
	    } else if (n.endsWith("_SD_STATUS") || n.equals("STATUS")) {
	        return new StatusRenderer(sv, JLabel.LEFT, negRed);
	    } else if (n.endsWith("_SD_TAG") || n.equals("TAG")) {
	        return new TagRenderer(sv, JLabel.LEFT, negRed);
	    } else if (n.matches(".*_SD_PERCENT([0-9])$")) {
	        int d = Character.getNumericValue(n.charAt(n.length() - 1));
	        return new PercentRenderer(sv, JLabel.RIGHT, negRed, d);
	    } else if (n.endsWith("_SD_SPARKLINE") || n.equals("SPARKLINE")) {
	        return new SparkRenderer(SparkRenderer.SparkType.SPARKLINE);
	    } else if (n.endsWith("_SD_SPARKAREA") || n.equals("SPARKAREA")) {
	        return new SparkRenderer(SparkRenderer.SparkType.SPARKAREA);
	    } else if (n.endsWith("_SD_SPARKBAR") || n.equals("SPARKBAR")) {
	        return new SparkRenderer(SparkRenderer.SparkType.SPARKBAR);
	    } else if (n.endsWith("_SD_SPARKDISCRETE") || n.equals("SPARKDISCRETE")) {
	        return new SparkRenderer(SparkRenderer.SparkType.SPARKDISCRETE);
	    } else if (n.endsWith("_SD_SPARKBULLET") || n.equals("SPARKBULLET")) {
	        return new SparkRenderer(SparkRenderer.SparkType.SPARKBULLET);
	    } else if (n.endsWith("_SD_SPARKPIE") || n.equals("SPARKPIE")) {
	        return new SparkRenderer(SparkRenderer.SparkType.SPARKPIE);
	    } else if (n.endsWith("_SD_SPARKBOXPLOT") || n.equals("SPARKBOXPLOT")) {
	        return new SparkRenderer(SparkRenderer.SparkType.SPARKBOXPLOT);
	    } else if (n.endsWith("_SD_ICON")  || n.equals("ICON")) {
	        return new IconRenderer();
        } else if (n.endsWith("_SD_SHADE") || n.equals("SHADE")) {
            return new ShadeRenderer(sv);
	    } else if (n.endsWith("_SD_TICK")) {
	        return new TickBooleanRenderer();
	    }
        return DateTimeRenderer.forColumnName(name);
	}

	
	 
	 static class ConditionalNumberRenderer extends DefaultTableRenderer {
		 	private static final long serialVersionUID = 1L;
			private final boolean negativeShownRed;
			private static final Color REDDY = new Color(150, 50, 50);
			private static final Color GREENY = new Color(50, 130, 50);
			private static final Map<Font, Font> underlineFontCache = new ConcurrentHashMap<>();
			private static final Map<Font, Font> normalFontCache = new ConcurrentHashMap<>();

			public ConditionalNumberRenderer(StringValue stringValer, int align, boolean negativeShownRed) {
		    	super(stringValer, align);
		        this.negativeShownRed = negativeShownRed;
			}

			private static Map<Integer,Color> colorCache = new ConcurrentHashMap<>();
			
			private static Color getColor(int r, int g, int b) {
				int v = ((b & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8);
				Color c = colorCache.get(v);
				if(c != null) {
					return c;
				}
				c = new Color(r,g,b);
				colorCache.put(v, c);
				return c;
			}
			
			private static Font getUnderlineFont(Font baseFont) {
				Font underlineFont = underlineFontCache.get(baseFont);
				if(underlineFont == null) {
					java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>(baseFont.getAttributes());
					attributes.put(java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON);
					underlineFont = baseFont.deriveFont(attributes);
					underlineFontCache.put(baseFont, underlineFont);
				}
				return underlineFont;
			}
			
			private static Font getNormalFont(Font font) {
				if(font.getAttributes().get(java.awt.font.TextAttribute.UNDERLINE) == null) {
					return font;
				}
				Font normalFont = normalFontCache.get(font);
				if(normalFont == null) {
					java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>(font.getAttributes());
					attributes.remove(java.awt.font.TextAttribute.UNDERLINE);
					normalFont = font.deriveFont(attributes);
					normalFontCache.put(font, normalFont);
				}
				return normalFont;
			}
			
			@Override
		    public Component getTableCellRendererComponent(JTable table, Object value,
		            boolean isSelected, boolean hasFocus, int row, int column) {
		        Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		        
		        // Combined Markdown + raw URL hyperlink handling
		        MarkdownLink md = extractMarkdownLink(value);
		        String rawUrl   = extractUrl(value);
		        if(cell instanceof JLabel && (md != null || rawUrl != null)) {
		            JLabel label = (JLabel) cell;
		            if(md != null) { // Use markdown link text if present, otherwise keep existing label text
		                label.setText(md.text);
		            }
		            label.setForeground(getThemeLinkColor());
		            label.setFont(getUnderlineFont(label.getFont()));
		            return cell;
		        }
		        
		        // Reset font to non-underlined if not a hyperlink
		        if(cell instanceof JLabel) {
		        	JLabel label = (JLabel) cell;
		        	label.setFont(getNormalFont(label.getFont()));
		        }
		        
		        // Only apply color if value is a Number and not selected (to preserve selection background)
		        if(negativeShownRed && (KdbType.isPositiveInfinity(value) || KdbType.isNegativeInfinity(value))) {
	            	cell.setForeground(Color.orange);
	            } else if(negativeShownRed && value instanceof Boolean && Boolean.valueOf((Boolean)value)) {
	            	cell.setForeground(GREENY);
	            } else if (value instanceof Number) {
		            double val = ((Number) value).doubleValue();
	            	Color cur = table.getForeground();
		            if (negativeShownRed && val < 0) {
		            	int red = cur.getRed();
		            	int g = cur.getGreen();
		            	int b = cur.getBlue();
		            	if(red < 60 && g<60 && b<60) {
		            		red += 160;
		            	} else if(red < 180) {
		            		red += 44;
		            	} else if(g > 150 && b > 150) {
		            		g -= 55;
		            		b -= 55;
		            	}
		            	Color newC = getColor(red, g, b);
		            	cell.setForeground(newC);
		            } else {
		            	cell.setForeground(cur);
		            }
		        } else {
		        	cell.setForeground(table.getForeground());
		        }

		        return cell;
		    }
		}
	 
	
	 private static class TagRenderer extends ConditionalNumberRenderer {
		    private static final long serialVersionUID = 1L;

		    public TagRenderer(StringValue sv, int align, boolean negativeShownRed) {
		        super(sv, align, negativeShownRed);
		    }


		    @Override
		    public Component getTableCellRendererComponent(
		            JTable table, Object value, boolean isSelected,
		            boolean hasFocus, int row, int col) {

		        JLabel lbl = (JLabel) super.getTableCellRendererComponent(
		                table, value, isSelected, hasFocus, row, col);

		        if(value instanceof String && !((String) value).isEmpty()) {
		            lbl.setText(renderTagsAsHtml(value.toString()));
		        }

		        return lbl;
		    }

		    
		    static String renderTagsAsHtml(String raw) {
		        if (raw == null || raw.isEmpty())
		            return raw;
		        // Split by whitespace or backtick
		        String[] parts = raw.split("[\\s`]+");
		        StringBuilder sb = new StringBuilder("<html>");

		        for (int i = 0; i < parts.length; i++) {
		            String tag = parts[i].trim();
		            if (tag.isEmpty()) continue;
		            sb.append(buildTagSpan(getTagColor(tag), tag));

		            if (i < parts.length - 1) {
		                sb.append(" "); // put normal space back between tags
		            }
		        }
		        sb.append("</html>");
		        return sb.toString();
		    }
		    
		    static Color getTagColor(String str) {
		        if (str == null || str.isEmpty()) {
		            return new Color(128, 128, 128, isDarkTheme() ? 110 : 70);
		        }

		        int hash = fnv1a32(str);
		        float hue = (hash & 0xFFFFFFFFL) % 360;

		        float saturation;
		        float lightness;

		        if (isDarkTheme()) {
		            saturation = 0.40f; // ↓ less saturated = softer
		            lightness = 0.42f;  // ↓ darker = matches browser pastel
		        } else {
		            saturation = 0.55f;
		            lightness = 0.65f;
		        }

		        Color rgb = hslToRgb(hue, saturation, lightness);

		        int alpha = isDarkTheme() ? 110 : 70;
		        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha);
		    }

		    
		    // --------------------
		    // FNV-1a 32-bit hash
		    // --------------------
		    private static int fnv1a32(String s) {
		        final int FNV_PRIME = 0x01000193;
		        int hash = 0x811C9DC5; // offset basis

		        for (int i = 0; i < s.length(); i++) {
		            hash ^= (s.charAt(i) & 0xFF);
		            hash *= FNV_PRIME;
		        }
		        return hash;
		    }

		    // --------------------
		    // HSL → RGB conversion
		    // --------------------
		    private static Color hslToRgb(float h, float s, float l) {
		        h /= 360f;

		        float q = l < 0.5 ? l * (1 + s) : (l + s - l * s);
		        float p = 2 * l - q;

		        float r = hueToRgb(p, q, h + 1f/3f);
		        float g = hueToRgb(p, q, h);
		        float b = hueToRgb(p, q, h - 1f/3f);

		        return new Color(clamp(r), clamp(g), clamp(b));
		    }

		    private static float hueToRgb(float p, float q, float t) {
		        if (t < 0) t += 1;
		        if (t > 1) t -= 1;
		        if (t < 1f/6f) return p + (q - p) * 6 * t;
		        if (t < 1f/2f) return q;
		        if (t < 2f/3f) return p + (q - p) * (2f/3f - t) * 6;
		        return p;
		    }

		    private static int clamp(float v) {
		        return (int)(255 * Math.max(0, Math.min(1, v)));
		    }


		}
	 
	 public static String buildTagSpan(Color c, String text) {
		 	if(c == null) {
		 		return "<span>&nbsp;" + HtmlUtils.escapeHTML(text) + "&nbsp;</span>";
		 	}
		    return "<span style='background-color: rgb("
		            + c.getRed() + ","
		            + c.getGreen() + ","
		            + c.getBlue() + ");'>"
		            + "&nbsp;" + HtmlUtils.escapeHTML(text) + "&nbsp;"
		            + "</span>";
		}

	 
	 private static class DecimalPlacesRenderer extends ConditionalNumberRenderer {
			private static final long serialVersionUID = 1L;
			private final int decimals;

			public DecimalPlacesRenderer(StringValue stringValer, int align, boolean negativeShownRed, int decimals) {
				super(stringValer, align, negativeShownRed);
				this.decimals = decimals;
			}

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

				// Format numbers BEFORE parent logic (links, bold, etc.)
				if (value instanceof Number) {
					double d = ((Number) value).doubleValue();
					value = String.format("%." + decimals + "f", d);
				}

				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}
	 
	 private static class CurrencyRenderer extends ConditionalNumberRenderer {

		    private final Currency currency;
		    private final NumberFormat format;

		    public CurrencyRenderer(StringValue sv, int align, boolean negativeShownRed, String currencyCode) {
		        super(sv, align, negativeShownRed);

		        // Build ISO currency
		        Currency cur = null;
		        try {
		            cur = Currency.getInstance(currencyCode);
		        } catch (Exception e) { }
		        this.currency = cur;

		        // Number formatting with locale
		        if (currency != null) {
		            format = NumberFormat.getCurrencyInstance(Locale.UK); // Or configurable
		            format.setCurrency(currency);
		        } else {
		            // fallback: plain number + 2dp
		            DecimalFormat df = new DecimalFormat("#,##0.00");
		            format = df;
		        }
		    }

		    @Override public Component getTableCellRendererComponent(
		            JTable table, Object value, boolean isSelected,
		            boolean hasFocus, int row, int col) {

		        if (value instanceof Number) {
		        	double d = ((Number) value).doubleValue();
		            String formatted = format.format(d);
		            if (currency == null) {
		                formatted = "??? " + formatted;
		            }
		            value = formatted;
		        }
		        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
		    }
		}
	 
	 private static Color getSColor(String lafName, String fallbackName, Color hardcodedFallback) {
		    Color c = UIManager.getColor(lafName);
		    if (c == null)
		        c = UIManager.getColor(fallbackName);
		    if (c == null)
		        c = hardcodedFallback;

		    if (isDarkTheme()) {
		        // Darken the color for dark themes
		        c = darkenForDarkTheme(c);
		    }

		    return c;
		}
	 
	 private static Color darkenForDarkTheme(Color c) {
		    // Reduce brightness by 40% but never below 20
		    int r = Math.max(20, (int)(c.getRed()   * 0.45));
		    int g = Math.max(20, (int)(c.getGreen() * 0.45));
		    int b = Math.max(20, (int)(c.getBlue()  * 0.45));
		    return new Color(r, g, b);
		}
	
	 
	 	public static Color getStatusRed() { return getSColor("Component.errorColor", "nimbusRed", new Color(220, 80, 80)); }
	 	public static Color getStatusGreen() { return getSColor("Component.successColor", "nimbusGreen", new Color(120, 200, 120)); }
	 	private static Color getStatusAmber() { return getSColor("Component.warningColor", "nimbusOrange", new Color(255, 200, 120)); }
	 	private static Color getStatusBlue() {return getSColor("Component.infoColor", "nimbusBlue", new Color(135, 206, 250)); }
	 	
	 	// Neutral color matching table background
	 	public static Color getStatusNeutral() {
	 	    Color c = UIManager.getColor("Table.background");
	 	    if (c == null) c = UIManager.getColor("Panel.background");
	 	    if (c == null) c = new Color(230, 230, 230);

	 	    if (isDarkTheme())
	 	        c = darkenForDarkTheme(c);

	 	    return c;
	 	}
	 	
	 	private static Color getDefaultIconColor() {
	 	    // Highest priority: FlatLaf or modern LAFs
	 	    Color c = UIManager.getColor("Component.foreground");
	 	    if (c == null) c = UIManager.getColor("Label.foreground");
	 	    if (c == null) c = UIManager.getColor("Button.foreground");

	 	    if (c == null) {
	 	        // Fallback based on theme brightness
	 	        if (isDarkTheme()) {
	 	            c = new Color(220, 220, 220);   // light gray for dark themes
	 	        } else {
	 	            c = Color.BLACK;                // black for light themes
	 	        }
	 	    }

	 	    // Dark-mode adjustment (same as your status color logic)
	 	    if (isDarkTheme()) {
	 	        c = darkenForDarkTheme(c);
	 	    }

	 	    return c;
	 	}
	 	

	 	
		static boolean isDarkTheme() {
		    // FlatLaf supports this directly
		    if (UIManager.getLookAndFeel().getName().toLowerCase().contains("flatlaf")) {
		        Object o = UIManager.get("Component.isLight");
		        if (o instanceof Boolean) {
		            return !((Boolean) o);
		        }
		    }

		    // Fallback heuristic: check window background brightness
		    Color bg = UIManager.getColor("Panel.background");
		    if (bg != null) {
		        int brightness = (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3;
		        return brightness < 100;
		    }

		    return false;
		}
		
		private static class StatusRenderer extends ConditionalNumberRenderer {
		    private static final long serialVersionUID = 1L;

		    public StatusRenderer(StringValue sv, int align, boolean negativeShownRed) {
		        super(sv, align, negativeShownRed);
		    }

		    @Override
		    public Component getTableCellRendererComponent(
		            JTable table, Object value, boolean isSelected,
		            boolean hasFocus, int row, int column) {

		        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		        if (!(c instanceof JLabel) || value == null) {
		            return c;
		        }

		        JLabel label = (JLabel) c;
		        String raw = value.toString();
		        String s = raw.trim().toLowerCase();

		        // If the row is selected, let the selection colors win
		        if (isSelected) {
		            return label;
		        }

		        Color bg = null;
		        if (s.matches("new|open|created|ready|starting")) {
		            bg = getStatusBlue();
		        } else if (s.matches("runnable|waiting|partial|blocked|flagged|suspended|paused|stopping")) {
		            bg = getStatusAmber();
		        } else if (s.matches("removed|cancelled|rejected|stopped")) {
		            bg = getStatusRed();
		        } else if (s.matches("terminated|resolved|closed|done|complete|filled|running")) {
		            bg = getStatusGreen();
		        } else {
		            return label; // no status match, just show normal text
		        }

		        label.setText("<html>" + buildTagSpan(bg, raw) + "</html>");
		        // Important: do NOT setOpaque(true) or setBackground here,
		        // so the row striping is still visible behind the label.
		        return label;
		    }
		}

	 

	private static class PercentRenderer extends ConditionalNumberRenderer {
		    private static final long serialVersionUID = 1L;

		    private final NumberFormat formatter;

		    public PercentRenderer(StringValue sv, int align, boolean negativeShownRed, int decimals) {
		        super(sv, align, negativeShownRed);

		        DecimalFormat df = new DecimalFormat();
		        df.setGroupingUsed(true);          // thousand separators: 1,234,567
		        df.setMinimumFractionDigits(decimals);
		        df.setMaximumFractionDigits(decimals);

		        this.formatter = df;
		    }

		    @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		            boolean hasFocus, int row, int col) {

		        if (value instanceof Number) {
		            double d = ((Number) value).doubleValue();
		            d = d * 100.0;                       // convert fraction → percent
		            value = formatter.format(d) + "%";   // append percent sign
		        }

		        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
		    }
		}
	

	 /** Detects markdown links like: [text](url) */
	static class MarkdownLink {
	     final String text;
	     final String url;
	     MarkdownLink(String t, String u) { text = t; url = u; }
	 }

		private static Color getThemeLinkColor() {
			String[] keys = {
					// 1) FlatLaf (highest priority)
					"Component.linkColor", "Label.linkColor",
					// 2) Darcula / IntelliJ platform
					"Hyperlink.foreground",
					// 3) Nimbus
					"textLinkForeground", "linkForeground",
					// 4) Swing HTML renderer (some LAFs set this)
					"HTML.linkForeground" };
			for (String key : keys) {
				Color c = UIManager.getColor(key);
				if (c != null) {
					return c;
				}
			}
			return new Color(0, 102, 204);
		}


	static MarkdownLink extractMarkdownLink(Object value) {
	     if (value == null) return null;
	     String s = value.toString().trim();

	     // match [text](url)
	     int openBracket = s.indexOf('[');
	     int closeBracket = s.indexOf(']');
	     int openParen = s.indexOf('(');
	     int closeParen = s.indexOf(')');

	     if (openBracket != 0 || closeBracket < 0 || openParen != closeBracket + 1 || closeParen < 0) {
	         return null;
	     }

	     String text = s.substring(openBracket + 1, closeBracket);
	     String url = s.substring(openParen + 1, closeParen);

	     if (url.startsWith("http://") || url.startsWith("https://")) {
	         return new MarkdownLink(text, url);
	     }

	     return null;
	 }

	
	/** Check if value contains a URL starting with http:// or https:// */
	static String extractUrl(Object value) {
		if(value == null) {
			return null;
		}
		String s = value.toString();
		int httpIdx = s.indexOf("http://");
		if(httpIdx == -1) {
			httpIdx = s.indexOf("https://");
		}
		if(httpIdx >= 0) {
			int endIdx = s.length();
			for(int i = httpIdx; i < s.length(); i++) {
				char c = s.charAt(i);
				if(Character.isWhitespace(c) || c == '"' || c == '\'' || c == '>' || c == '<') {
					endIdx = i;
					break;
				}
			}
			return s.substring(httpIdx, endIdx);
		}
		return null;
	}


    @Data
    public static class IconRequest {
        private final BlueprintIcon icon;
        private final Color color;
        
        @Override public String toString() {
            String n = icon == null ? "" : icon.name().toLowerCase().replace('_','-');
            String c = color == null ? "" : String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            return ":" + n + ":" + c;
        }

    }
	
	
    public static class IconRenderer extends DefaultTableRenderer {

        private static final long serialVersionUID = 1L;
        private static final int GAP = 4;

        public IconRenderer() {
            super(StringValues.TO_STRING, JLabel.CENTER);
        }

        /** Paints multiple icons inline with a fixed gap. */
        @Data private static class MultiIcon implements Icon {

            private final List<IconRequest> icons;
            private final int size;
            private final int gap;

            @Override public int getIconWidth() {
                if(icons == null || icons.isEmpty()) {
                    return 0;
                }
                int n = icons.size();
                return n * size + (n - 1) * gap;
            }

            @Override public int getIconHeight() { return size; }

            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                if(!(g instanceof Graphics2D) || icons == null) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    int xPos = x;
                    for(IconRequest r : icons) {
                        Icon base = r.icon.get16(r.color);
                        if(base != null) {
                            int bw = Math.max(1, base.getIconWidth());
                            int bh = Math.max(1, base.getIconHeight());
                            int refMax = Math.max(bw, bh);
                            double scale = (double) size / (double) refMax;
                            int scaledH = (int) Math.round(bh * scale);

                            int yOffset = y + (size - scaledH) / 2;

                            AffineTransform oldTx = g2.getTransform();
                            g2.translate(xPos, yOffset);
                            g2.scale(scale, scale);
                            base.paintIcon(c, g2, 0, 0);
                            g2.setTransform(oldTx);
                        }
                        xPos += size + gap;
                    }
                } finally {
                    g2.dispose();
                }
            }
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            JLabel l = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            List<IconRequest> reqs = parseIconRequests(
                    value == null ? null : value.toString(),
                    table.getForeground()
            );

            if(!reqs.isEmpty()) {
                int rowHeight = table.getRowHeight(row);
                int size = computeIconSize(rowHeight);

                l.setIcon(new MultiIcon(reqs, size, GAP));
                l.setText("");
                l.setHorizontalAlignment(JLabel.CENTER);

                l.setToolTipText(buildTooltip(reqs));
            } else {
                l.setIcon(null);
                l.setHorizontalAlignment(JLabel.LEFT);
                l.setToolTipText(null);
            }

            return l;
        }
        
        private static int computeIconSize(int rowHeight) {
            return rowHeight <= 0 ? 16 : rowHeight <= 22 ? 16 : rowHeight <= 28 ? 20 : rowHeight <= 36 ? 24 :32;
        }

        public static List<IconRequest> parseIconRequests(String raw, Color tableFg) {
            List<IconRequest> out = new ArrayList<>();
            if (raw == null || raw.trim().isEmpty()) {
                return out;
            }
            String[] parts = raw.trim().split("\\s+");
            for (String p : parts) {
                IconRequest ir = parseSingleIcon(p.trim(), tableFg);
                if (ir != null) {
                    out.add(ir);
                }
            }
            return out;
        }


        private static IconRequest parseSingleIcon(String token, Color tableFg) {
            if (token == null) {
                return null;
            }

            token = token.trim();
            if (token.isEmpty() || !token.startsWith(":")) {
                return null;
            }

            while (token.startsWith(":") && token.length() > 1) {
                token = token.substring(1);
            }
            while (token.endsWith(":") && token.length() > 1) {
                token = token.substring(0, token.length() - 1);
            }

            if (token.isEmpty()) {
                return null;
            }

            String[] parts = token.split(":", 2);
            String name = parts[0].trim();
            if (name.isEmpty()) {
                return null;
            }

            BlueprintIcon icon = BlueprintIcon.fromString(name);
            if (icon == null) {
                return null;
            }

            Color color = tableFg;
            if (parts.length == 2) {
                String raw = parts[1].trim();
                if (!raw.isEmpty()) {
                    Color c = ColorUtils.parse(raw);
                    if (c != null) {
                        color = c;
                    }
                }
            }

            return new IconRequest(icon, color);
        }


        private static String buildTooltip(List<IconRequest> reqs) {
            if(reqs == null || reqs.isEmpty()) {
                return null;
            } else if(reqs.size() == 1) {
                return reqs.get(0).icon.name();
            }
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < reqs.size(); i++) {
                if(i > 0) {
                    sb.append(" ");
                }
                sb.append(reqs.get(i).icon.name());
            }
            return sb.toString();
        }
    }

    private static class TickBooleanRenderer implements TableCellRenderer {

        private final IconRenderer iconRenderer = new IconRenderer();

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            String token;
            if(value instanceof Boolean) {
                token = ((Boolean) value) ? ":tick:green" : ":cross:red";
            } else if(value == null) {
                token = "";
            } else {
                token = value.toString();
            }

            return iconRenderer.getTableCellRendererComponent(
                    table, token, isSelected, hasFocus, row, column);
        }
    }

    
	private static String stripSdSuffix(String name) {
	    if (name == null) return null;
	    int idx = name.toUpperCase().lastIndexOf("_SD_");
	    return (idx >= 0) ? name.substring(0, idx) : name;
	}

	static void applySmartDisplayColorColumns(JXTable table) {
	    TableModel model = table.getModel();
	    int mc = model.getColumnCount();

	    Map<String, Integer> fgCols = new HashMap<>();
	    Map<String, Integer> bgCols = new HashMap<>();
	    Map<String, Integer> codeCols = new HashMap<>();

	    // ---- PASS 1: Find the color providers ----
	    for (int mi = 0; mi < mc; mi++) {
	        String name = model.getColumnName(mi);
	        if (name == null) continue;

	        if (name.endsWith("_SD_FG")) {
	            String base = stripSdSuffix(name);   // <-- FIX
	            fgCols.put(base, mi);

	            int view = table.convertColumnIndexToView(mi);
	            if (view >= 0) table.getColumnExt(view).setVisible(false);

	        } else if (name.endsWith("_SD_BG")) {
	            String base = stripSdSuffix(name);   // <-- FIX
	            bgCols.put(base, mi);

	            int view = table.convertColumnIndexToView(mi);
	            if (view >= 0) table.getColumnExt(view).setVisible(false);
	        } else if (name.endsWith("_SD_CODE")) {
	            String base = stripSdSuffix(name);
	            codeCols.put(base, mi);

	            int view = table.convertColumnIndexToView(mi);
	            if (view >= 0) table.getColumnExt(view).setVisible(false);
	        }

	    }

	    // ---- PASS 2: Wrap base column renderers ----
	    for (int mi = 0; mi < mc; mi++) {
	        String colName = model.getColumnName(mi);
	        if (colName == null) continue;

	        String base = stripSdSuffix(colName);     // <-- FIX here

	        Integer fgCol = fgCols.get(base);
	        Integer bgCol = bgCols.get(base);
	        Integer codeCol = codeCols.get(base);

	        if (fgCol == null && bgCol == null)
	            continue;

	        int view = table.convertColumnIndexToView(mi);
	        if (view < 0) continue;

	        TableColumnExt col = table.getColumnExt(view);

	        if (codeCol != null) {
	            TableCellRenderer existing = col.getCellRenderer();
	            col.setCellRenderer(new CodeRenderer(codeCol));
	        }
	        TableCellRenderer existing = col.getCellRenderer();



	        col.setCellRenderer(
	            RenderProvider.wrapWithColorColumns(
	                existing,
	                fgCol != null ? fgCol : -1,
	                bgCol != null ? bgCol : -1
	            )
	        );
	    }
	}

	
	
	
	public static TableCellRenderer wrapWithColorColumns(TableCellRenderer delegate,int fgModelCol,int bgModelCol) {

	    return new TableCellRenderer() {
	    	
	        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
	                boolean hasFocus, int row, int column) {

	            TableCellRenderer base = (delegate != null) ? delegate
	                    : table.getDefaultRenderer(value != null ? value.getClass() : Object.class);

	            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	            if (!(table.getModel() instanceof TableModel)) {
	                return c;
	            }

	            int modelRow = table.convertRowIndexToModel(row);
	            TableModel model = table.getModel();

	            // -------- Foreground color --------
	            if (!isSelected && fgModelCol >= 0 && fgModelCol < model.getColumnCount()) {
	                Object fgVal = model.getValueAt(modelRow, fgModelCol);
	                if (fgVal != null) {
	                    Color fg = ColorUtils.parse(fgVal.toString());
	                    if (fg != null) {
	                        c.setForeground(fg);
	                    }
	                }
	            }

	            // -------- Background color --------
	            if (!isSelected && bgModelCol >= 0 && bgModelCol < model.getColumnCount()) {
	                Object bgVal = model.getValueAt(modelRow, bgModelCol);
	                if (bgVal != null) {
	                    Color bg = ColorUtils.parse(bgVal.toString());
	                    if (bg != null) {
	                        c.setBackground(bg);
	                        if (c instanceof JComponent) {
	                            ((JComponent) c).setOpaque(true);
	                        }
	                    }
	                }
	            }

	            return c;
	        }
	    };
	}
	
	
	
	

	public static class CodeRenderer extends DefaultTableRenderer {

	    private final int codeModelColumnIndex;

	    public CodeRenderer(int codeModelCol) {
	        super(StringValues.TO_STRING, JLabel.RIGHT);
	        this.codeModelColumnIndex = codeModelCol;
	    }

	    @Override
	    public Component getTableCellRendererComponent(
	            JTable table, Object value, boolean isSelected,
	            boolean hasFocus, int row, int column) {

	        JLabel l = (JLabel) super.getTableCellRendererComponent(
	                table, value, isSelected, hasFocus, row, column);

	        if (!(value instanceof Number)) {
	            return l;
	        }

	        int modelRow = table.convertRowIndexToModel(row);
	        TableModel m = table.getModel();

	        if (codeModelColumnIndex < 0 || codeModelColumnIndex >= m.getColumnCount()) {
	            return l;
	        }

	        Object patternObj = m.getValueAt(modelRow, codeModelColumnIndex);
	        if (patternObj == null) return l;

	        String pattern = patternObj.toString().trim();
	        if (pattern.isEmpty()) return l;

	        String html = applyCodePattern((Number) value, pattern);
	        l.setText("<html>" + html + "</html>");

	        return l;
	    }
	    
	    
	    private static String applyCodePattern(Number number, String pattern) {
	        double v = number.doubleValue();

	        // prefix detection (must NOT be '#', '0', or '.')
	        StringBuilder out = new StringBuilder();
	        char first = pattern.charAt(0);
	        int startIndex = 0;

	        if (first != '#' && first != '0' && first != '.') {
	            out.append(first);
	            startIndex = 1;
	        }

	        // find decimal point index in pattern
	        int dot = pattern.indexOf('.', startIndex);
	        int dp = 0;

	        if (dot >= 0) {
	            dp = pattern.length() - (dot + 1);
	        }

	        // format number with 'dp' decimal places
	        NumberFormat nf = NumberFormat.getNumberInstance();
	        nf.setGroupingUsed(false);
	        nf.setMinimumFractionDigits(dp);
	        nf.setMaximumFractionDigits(dp);

	        String formatted = nf.format(v);

	        if (dp == 0) {
	            // integer formatting only
	            out.append(formatted);
	            return out.toString();
	        }

	        // split integer vs decimals
	        int dotIndex = formatted.indexOf(".");
	        String intPart = formatted.substring(0, dotIndex);
	        String fracPart = formatted.substring(dotIndex + 1);

	        out.append(intPart).append('.');

	        // now map frac digits according to pattern
	        for (int i = 0; i < dp && i < fracPart.length(); i++) {
	            char codeChar = pattern.charAt(dot + 1 + i);
	            char digit = fracPart.charAt(i);

	            if (codeChar == 'X') {
	                out.append("<span style='font-size:1.2em; font-weight:bold'>")
	                   .append(digit)
	                   .append("</span>");
	            } else {
	                out.append(digit);
	            }
	        }

	        return out.toString();
	    }
	    
	    
	}
	


}
