/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2024 TimeStored
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
package com.timestored.misc;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import com.google.common.base.Joiner;
import com.timestored.TimeStored;
import com.timestored.swingxx.SwingUtils;
import com.timestored.theme.Theme;

/** Utility methods useful for generating HTML */
public class HtmlUtils {
	
	private static final Logger LOG = Logger.getLogger(HtmlUtils.class.getName());

	public static final String END = "</body></html>";
	public static final String START = "<html><body>";
	private static final boolean browseSupported;

	private static final String HEAD_PRE = "<!DOCTYPE html PUBLIC " +
		"\"-//W3C//DTD XHTML 1.0 Strict//EN\"" +
		"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
		"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" >" +
		"<head><meta http-equiv=\"content-type\" content=\"text/html; " +
		"charset=iso-8859-1\" />";

	private static final String FAVICON_LINK = "<link rel=\"shortcut icon\" type=\"image/png\" href=\"" + TimeStored.URL + "/favicon.png\" />";

	/**
	 * @return HTML for doctype, starting html/body tag
	 */
	public static String getHead(String title, String headContent) {
		return HEAD_PRE + "<title>" + title + "</title>" + headContent + "</head><body>";
	}
	
	public static String getTail() { return END; }

	/** 
	 * Gets to inside "main" div, this function pairs with {@link #getTSPageTail(String)}.
	 */
	public static String getTSPageHead(String title, String subTitleLink, String headContent, boolean withTSFavicon) {
		
		StringBuilder s = new StringBuilder(200);
		
		s.append(HtmlUtils.getHead(title, headContent + FAVICON_LINK));
		s.append("\r\n<div id='wrap'><div id='page'>");
		// HEADER
		s.append("\r\n<div id='header'><h2>" + subTitleLink + "</h2></div>");
		// MAIN
		s.append("\r\n<div id='main'>");
		return s.toString();
	}

	/** 
	 * Gets from inside "main" div to end of html, this function pairs with {@link #getTSPageHead(String, String, String, boolean)} 
	 */
	public static String getTSPageTail(String subTitleLink) {
		StringBuilder s = new StringBuilder(200);
		s.append("\r\n</div>");
		s.append("<div id='footer'> <p>&copy; 2019 ");
		s.append(subTitleLink);
		s.append(" | <a target='a' href='" + TimeStored.URL + "'>TimeStored.com</a>" + 
				"</div>");
		s.append("\r\n</div></div>\r\n"); // end of wrap/page
		s.append(END);
		return s.toString(); 
	}
		
	private static int counter = 13;
	
	static {
		boolean browsey = false;
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				browsey = true;
			}
		}
		browseSupported = browsey;
	}
	
	public static String toList(List<String> items) {
		return "<ul><li>" + Joiner.on("</li><li>").join(items) + "</li></ul>";
	}
	
	public static String toList(String... items) {
		return "<ul><li>" + Joiner.on("</li><li>").join(items) + "</li></ul>";
	}
	
	public static String toList(Map<String, String> smap, boolean hideEmpty) {
		if(smap.isEmpty()) {
			return "";
		}
		return "<ul>" + expandMapToHtml(smap, hideEmpty,"\r\n\t<li><strong>", "</strong> - ", "", "</li>") + "</ul>";
	}

	public static String toDefinitions(Map<String, String> smap, boolean hideEmpty) {
		if(smap.isEmpty()) {
			return "";
		}
		return "<dl>" + expandMapToHtml(smap, hideEmpty,"<dt>", "</dt>", "<dl>", "</dl>") + "</dl>";
	}

	public static String toTable(Map<String, String> smap, boolean hideEmpty) {
		if(smap.isEmpty()) {
			return "";
		}
		return "<table>" + expandMapToHtml(smap, hideEmpty,"\r\n<tr><th>", "</th>", "<td>", "</td></tr>") + "</table>";
	}


	private static String expandMapToHtml(Map<String, String> smap, boolean hideEmpty, 
			String preKey, String postKey, String preVal, String postVal) {
		
		StringBuilder sb = new StringBuilder();
		List<String> keyList = new ArrayList<String>(smap.keySet());
		// @TODO check this gives declared order
		//Collections.sort(keyList);
		
		for(String key : keyList) {
			String val = smap.get(key);
			if(hideEmpty && (val==null || val.trim().isEmpty())) {
				// omit this line
			} else {
				sb.append(preKey).append(key).append(postKey);
				sb.append(preVal).append(val + postVal);	
			}
		}
		return sb.toString();
	}
	
	/** 
	 * Given an HTML document return just the HTML inside the body tags, currently
	 * only supports the most basic format.
	 */
	public static String extractBody(String htmlDoc) {

		if(htmlDoc!=null && !htmlDoc.trim().isEmpty()) {
			String t = getTextInsideTag(htmlDoc, "body");
			if(t == null) { t = getTextInsideTag(htmlDoc, "html"); }
			if(t == null) {	t = htmlDoc; }
			return t;
		}
		return "";
	}

	private static String getTextInsideTag(String htmlDoc, String tag) {
		String b = "<" + tag + ">";
		int st = htmlDoc.indexOf(b);
		int end = htmlDoc.lastIndexOf("</" + tag + ">");
		if(st==-1 || end==-1) {
			return null;
		}
		return htmlDoc.substring(st+b.length(), end);
	}

	/**
	 * Try to open a url in the system associated web browser if possible
	 * @return true if it seemed to launch browser ok, false otherwise.
	 */
	public static boolean browse(String url) {
		if(browseSupported) {
			try {
				Desktop.getDesktop().browse(new URI(url));
				return true;
			} catch (IOException|URISyntaxException e) {
				LOG.log(Level.WARNING, "couldn't open browser", e);
				String msg = "Could not open browser. Here is the URL:\r\n" + url;
				JOptionPane.showMessageDialog(null, Theme.getHtmlText(msg), "Error Opening Browser", JOptionPane.WARNING_MESSAGE);
			}
		}
		return false;
	}
	
	/**
	 * @return true if this java instance supports launching a browser, otherwise false.
	 */
	public static boolean isBrowseSupported() {
		return browseSupported;
	}

	/** get action that launches a web browser */
	public static Action getWWWaction(String title, String url) {
		return new WwwAction(title, url);
	}

	/**  World Wide Web Browsing action */
	private static class WwwAction extends AbstractAction {
		
		private final String url;

		WwwAction(String title, String url) {
			super(title, Theme.CIcon.TEXT_HTML.get16());
			this.url = url;
			setEnabled(HtmlUtils.isBrowseSupported());
		}
		
		@Override public void actionPerformed(ActionEvent e) {
			HtmlUtils.browse(url);
		}
	}
	
	/**
	 * Generate a specially formatted text field for q code, requires including certain JS also 
	 */
	public static void appendQCodeArea(StringBuilder sb, String code) {
		sb.append("\r\n<textarea rows='2' cols='80' class='code' id='code");
		sb.append(counter);
		sb.append("'>");
		sb.append(code);
		sb.append("</textarea> <script type='text/javascript'>" +
				"CodeMirror.fromTextArea(document.getElementById('code");
		sb.append(counter);
		sb.append("'),  {  lineNumbers: true, matchBrackets: true,  mode: \"text/x-plsql\", " +
				"readOnly:true });</script>");
		counter++;
	}

	public static String getXhtmlTop(String title) {
		return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" ><head>" +
				"\r\n<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\" />" +
				"\r\n<title>" + title + "</title></head><body>";
	}

	public static String getXhtmlBottom() {
		return " </body></html>";
	}
	/** 
	 * For the TimeStored.com website get the top part of PHP wrapper 
	 * @see #getTSTemplateBottom()
	 */
	public static String getTSTemplateTop(String title) {
		return "<?php \r\n"
				+ "include 'template.php';\r\n"
				+ "echo $t->getSmallHeadedTop(\"" + title + "\", null, \"codemirror.js\");\r\n"
				+ "echo getHelpTop();	\r\n"
				+ "?>\r\n";
	}


	/** For the TimeStored.com website get the bottom part of PHP wrapper */
	public static String getTSTemplateBottom() {
		return "\r\n"
				+ "<?php \r\n"
				+ "echo getHelpBottom();\r\n"
				+ "echo $t->getSmallHeadedBottom(); \r\n"
				+ "?>";
	}

	/** Escape any ampersands etc in the txt to allow use in html **/
	public static String escapeHTML(String txt) {
	    StringBuilder out = new StringBuilder(Math.max(16, txt.length()));
	    return appendEscapedHtml(out, txt).toString();
	}

	/** Append the txt to sb escaping any ampersands etc in the txt */
	public static StringBuilder appendEscapedHtml(StringBuilder sb, String txt) {
		boolean previousWasASpace = false;
	    for( char c : txt.toCharArray() ) {
	        if( c == ' ' ) {
	            if( previousWasASpace ) {
	                sb.append("&nbsp;");
	                previousWasASpace = false;
	                continue;
	            }
	            previousWasASpace = true;
	        } else {
	            previousWasASpace = false;
	        }
	        switch(c) {
	            case '<': sb.append("&lt;"); break;
	            case '>': sb.append("&gt;"); break;
	            case '&': sb.append("&amp;"); break;
	            case '"': sb.append("&quot;"); break;
	            case '\n': sb.append("\n<br />"); break;
	            // We need Tab support here, because we print StackTraces as HTML
	            case '\t': sb.append("&nbsp; &nbsp; &nbsp;"); break;  
	            default:
	                if( c < 128 ) {
	                    sb.append(c);
	                } else {
	                    sb.append("&#").append((int)c).append(";");
	                }    
	        }
	    }
	    return sb;
	}
	
	/**
	 * Clean up a string for placement inside single quoted html attribute
	 */
	public static String cleanAtt(String name) {
		return name.replace("'", "&lsquo;");
	}
	
	/** clean up a filename string for use in a filepath, ie. remove spaces etc **/
	public static String clean(String s) {
		return s.trim().replace(",", "-").replace(":", "-")
				.replace(" ", "-").replace("'", "-").toLowerCase();
	}
	

	/** append img tag, reading src image to get height width tags **/
	public static void appendImage(StringBuilder sb, String imgFilename, 
			String alt, int height, int width) {
		sb.append("<img src='").append(imgFilename).append("'");
		if(alt!=null && alt.trim().length()>0) {
			sb.append(" alt='").append(cleanAtt(alt)).append("' ");
		}
		sb.append(" height='").append(height)
		.append("' width='").append(width).append("'");
		sb.append(" />");
	}

}
