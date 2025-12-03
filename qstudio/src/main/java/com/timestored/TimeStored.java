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

package com.timestored;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Container class for email addresses, website addresses etc used throughout. 
 * Allows maintaining tracking and url changes in one java class. 
 */
public class TimeStored {
	
	public static final String TECH_EMAIL_ADDRESS = "tech@timestored.com";
	public static final String URL = "https://www.timestored.com";
//	public static final String URL = "http://timestored.me";
	private static final String NICE_URL = "https://www.TimeStored.com";
	private static final String TRACKER_PARAMS = "utm_source=qstudio&utm_medium=app&utm_campaign=qstudio";

	private static final Random R = new Random();
	private static final List<String> ONLINE_ENTRIES = new CopyOnWriteArrayList<>(); 

	public static boolean isOnlineNewsAvailable() {
		return !ONLINE_ENTRIES.isEmpty();
	}
	
	public static List<String> getOnlineNews(boolean isDarkTheme) {
		final String htmlReplace = isDarkTheme ? DARK_HTML_BODY : LIGHT_HTML_BODY; 
		return ONLINE_ENTRIES.stream().filter(s -> isDarkTheme ? !s.contains("-light") : !s.contains("-dark"))
					.map(s -> s.replace("<html>", htmlReplace)).collect(Collectors.toList());
	}
	
	public static void fetchOnlineNews() {
		if(ONLINE_ENTRIES.size() == 0) {
			String U = TimeStored.URL + "/qstudio/addon/news";
			try(Scanner sc = new java.util.Scanner(new URL(U).openStream(), "UTF-8")) {
				List<String> l = new ArrayList<>();
				String[] entriesTxt = sc.useDelimiter("\\A").next().split("</html>");
				if(entriesTxt.length > 3 && entriesTxt[0].contains("SLASHhtml")) {
					for(int i=1; i<entriesTxt.length; i++) {
						// advert shorter than 20 doesn't make sense. Most likely cause is new line nonsense.
						if(entriesTxt[i] != null && entriesTxt[i].length() > 20) { 
							l.add(entriesTxt[i] + "</html>");
						}
					}
					if(l.size() > 3) {
						ONLINE_ENTRIES.addAll(l);
					}
				}
			} catch (Exception e) {
				// DO nothing. Revert to hardcoded entries elsehwere.
			}
		}
	}
	
	private static final String DARK_HTML_BODY = "<html><body bgcolor='#111111' text='#EEEEEE' link='#EEEEEE'>";
	private static final String LIGHT_HTML_BODY = "<html><body bgcolor='#EEEEEE' text='#111111' link='#111111'>";
	
	/**
	 * Website pages and their location, allows maintaining tracking
	 * and url changes in one java class. 
	 */
	public static enum Page {
		TRAINING("kdb-training"), 
		TUTORIALS("kdb-guides"), 
		PULSE("pulse"), 
		PULSE_TUTORIALS_KDB("/pulse/tutorial?filter=kdb"), 
		TUTE_IPC("kdb-guides/interprocess-communication"), 
		TUTE_PEACH("kdb-guides/parallel-peach"), 
		TUTE_PYTHON("kdb-guides/python-api"), 
		TUTE_DEBUG("kdb-guides/debugging-kdb"),
		TUTE_MEM("kdb-guides/memory-management"),
		QSTUDIO("qstudio"),
		QSTUDIO_CONNECTING("qstudio/help/#connectingServer"),
		QSTUDIO_DATABASES("qstudio/database"),
		QSTUDIO_CHARTING("/qstudio/help/chart-examples"),
		QSTUDIO_RELEASES("/qstudio/help/releases"),
		QSTUDIO_DOWNLOAD("qstudio/download"),
		QSTUDIO_HELP("qstudio/help"),
		QSTUDIO_BUY("qstudio/buy"),
		QSTUDIO_REGISTER("qstudio/register"),
		QSTUDIO_HELP_USER("qstudio/help/user-permissions"),
		QSTUDIO_HELP_JDBC("qstudio/help/add-connections"),
		QSTUDIO_HELP_DRIVER_DOWNLOAD_ERROR("qstudio/help/driver-download-error"),
		QSTUDIO_HELP_DBMANAGE("qstudio/help/database-management"),
		QSTUDIO_HELP_LOADCSV("qstudio/help/load-csv-data-file-into-kdb"),
		QSTUDIO_HELP_QUNIT("qstudio/help/qunit"),
		QSTUDIO_HELP_QDOC("/qstudio/help/qdoc"),
		QSTUDIO_HELP_CHARTCONFIG("/qstudio/help/chart-config"),
		QSTUDIO_HELP_SQLNOTEBOOKS("/qstudio/help/sqlnotebook"),
		SQLDASH("pulse"),	
		SQLDASH_HELP_EG("qstudio/help/chart-examples"), 
		QDOC("qstudio/help/qdoc"),
		COMMAND_BAR_HELP("qstudio/help/keyboard-shortcuts"), 
		CONTACT("contact"),
		NEWSPAGE("news"), 
		QUNIT_HELP("kdb-guides/kdb-regression-unit-tests"), 
		SQLNOTEBOOK_HELP("sqlnotebook/docs"), 
		QSTUDIO_CHANGES("/qstudio/help/releases"),
		QSTUDIO_HELP_KEYBOARD("/qstudio/help/keyboard-shortcuts"),
		QDOC_FEATURE_REQUEST("/r?qdoc=feature-request"),
		QDOC_REPORT_ISSUE("/r?qdoc=report-issue"), 
		QSTUDIO_HELP_SSL("/qstudio/help/ssl-configuration");
		
		final private String loc;
		
		Page(String loc) {
			this.loc = loc;
		}

		/** @return the display url nicely formatted and without tracking code */
		public String niceUrl() {
			return NICE_URL + "/" + loc;
		}
		
		public String url() {
			return URL + "/" + loc + (loc.contains("?") ? '&' : '?') + TRACKER_PARAMS;
		}
		
		public String toAnchor() { return toAnchor(niceUrl()); }
		
		public String toAnchor(String linkTxt) {
			return "<a href='" + url() + "'>" + linkTxt + "</a>";
		}
	}

	private static final String[] LINKS = new String[] {
		"<a href='" + Page.TRAINING.url() + "'>Perhaps some training would help... </a>",
		"<a href='" + Page.PULSE.url() + "'>Pulse - Create and share real-time interactive dashboards with your team</a>",
		"TimeStored.com has <a href='" + Page.TUTORIALS.url() + "'>Free Kdb Video Tutorials.</a>"
	};
	

	public static final String[] NEWS = new String[] {
		"<html>" +
				"<p>Try pressing <b>Ctrl+P</b></p>" +
				"<p>It brings up a <a href='" + Page.COMMAND_BAR_HELP.url() + "'>command bar</a> that allows smart matching on:</p>" +
				"<ul>" +
				"<li>Server name to change your selected server.</li>" +
				"<li>File names in the File Tree to open that document.</li>" +
				"</ul>" +
				"</html>",
		"<html>" +
				"<p>Check out our advanced features for:</p>" +
				"<ul>" +
				"<li><a href='" + Page.QSTUDIO_HELP_LOADCSV.url() + "'>Loading CSV's</a></li>" +
				"<li><a href='" + Page.QSTUDIO_HELP_QUNIT.url() + "'>Unit Testing</a></li>" +
				"</ul>" +
				"</html>",
		"<html>" +
				"<p><a href='" + Page.TUTORIALS.url() + "'>Free Kdb video tutorials</a> are available on:</p>" +
				"<ul>" +
				"<li><a href='" + Page.TUTE_IPC.url() + "'>Inter-Process Communication</a> - client/server, (a)synchronous, message handlers.</li>" +
				"<li><a href='" + Page.TUTE_PEACH.url() + "'>Parallel Processing</a> - peach and .Q.fc</li>" +
				"<li><a href='" + Page.TUTE_PYTHON.url() + "'>Using Kdb from Python.</a></li>" +
				"<li><a href=" + Page.TUTE_DEBUG.url() + "'>Debugging in Kdb</a> - breakpoints, suspending on client errors, debug outputting.</li>" +
				"</ul>" +
				"</html>",
			getPulseHTML("candlestick-light-med.png"),
			getPulseHTML("price-grid-dark-med.png"),
			getPulseHTML("price-grid-light-med.png"),
			getPulseHTML("pulse-laptop.png"),
			getPulseHTML("taq-light-med.png"),
			getPulseHTML("trade-blotter-dark-med.png"),
			getPulseHTML("trade-blotter-light-med.png"),
			getPulseHTML("fxdash-dark-med.png"),
			getPulseHTML("crypto-dark-med.png"),		
			
	};
	
	private static final String getPulseHTML(String imageName) {
		return "<html><center>" +
				"<h1><a href='" + Page.PULSE.url() + "'>Pulse</a> - Create and share interactive dashboards with your team</h1>" +
				"<a href='" + Page.PULSE.url() + "'><img src='https://www.timestored.com/qstudio/addon/"+imageName+"' width=\"720\" height=\"405\" /></a>" +
				"</center></html>";	
	}
	
	/**
	 * @return a link that will take the user to the contact form on the web site.
	 * @param subject The title of the email/contact.
	 */
	public static String getContactUrl(final String subject, final String details) {

		String d = "";
		String s = "";
		
		try {
			s = (subject==null ? "" : "&subject=" + URLEncoder.encode(subject, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {/* ignore */	}
		try {
			d = (details==null ? "" : "&details=" + URLEncoder.encode(details, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {/* ignore */	}

		return URL + "/contact?" + s + d;
	}

	/**
	 * @return a link that will take the user to the contact form on the web site.
	 * @param subject The title of the email/contact.
	 */
	public static String getContactUrl(final String subject) {
		return getContactUrl(subject, null);
	}
	
	/** @return A short 4-10 words, hyperlinked to a page on the web site **/
	public static String getRandomNewsLink() {
		return LINKS[R.nextInt(LINKS.length)];
	}


	/** @return A 3-4 sentence html paragraph with hyperlinks to a page on the web site **/
	public static String getRandomLongNewsHtml(boolean isDarkTheme) {
		List<String> online = getOnlineNews(isDarkTheme);
		if(online.size() > 3) {
			return online.get(R.nextInt(online.size()));
		}
		return NEWS[R.nextInt(NEWS.length)];
	}
	
	/**
	 * When linking to websites other than timestored use this redirector so
	 * that I can track usage and alter it in future if necessary. 
	 */
	public static String getRedirectPage(String url, String purpose) {
		return "http://www.timestored.com/r?url=" + URLEncoder.encode(url)
				+ "&source=qstudio&purpose=" + URLEncoder.encode(purpose);
	}
}
