package com.timestored.babeldb;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Preconditions;

import lombok.extern.java.Log;

@Log
class HtmlToCsvConverter {
	
	public static List<String[]> toStringArray(String htmlTxt) {

		Document doc = Jsoup.parse(htmlTxt);

		boolean isWiki = htmlTxt.contains("- Wikipedia</title>");
		if(isWiki) { // Ignore [3] citations
			doc.select("sup").remove();
		}
		
		Elements tables = doc.select("table"); //select the first table.
		List<String[]> all = new ArrayList<>();
		List<String> headers = new ArrayList<>();
		boolean raggedArray = false;
		all.add(new String[] {}); // fake row that we will replace with all headers later
		
		
		for(Element tbl : tables) {
			Set<String> cns = tbl.classNames();
			if(cns.contains("sidebar") || cns.contains("navbox-inner") || cns.contains("infobox") || cns.contains("vcard") || cns.contains("navbox-subgroup")) {
				continue; // skip - based on wikipedia naming but sidebar makes no sense!
			}
			List<String[]> cur = toStringArray(tbl);
			if(cur.size() < 2) {  continue; } // header row + data row - nothing here so skip.
			// headers wasn't set, just set it to whatever has appeared.
			if(headers.size() == 0) {
				for(String s : cur.get(0)) {
					headers.add(s);	
				}
			}
			// some sites with multiple tables, label the headers similar but different on caps.
			if(equalsIgnoreCase(cur.get(0), headers.toArray(new String[] {}))) {
				for(int r=1; r<cur.size(); r++) {
					all.add(cur.get(r));
				}	
			} else {
				raggedArray = true;
				// Add all the headers and create lookup array
				int[] newIndices = new int[cur.get(0).length];
				for(int c=0; c < cur.get(0).length; c++) {
					String e = cur.get(0)[c];
					int p = headers.indexOf(e);
					if(p == -1) {
						headers.add(e);
						newIndices[c] = headers.size() - 1;
					} else {
						newIndices[c] = p;
					}
				}
				// Place from cur Idx to newRow which is expanded to accommodate old and new columns.
				for(int r=1; r<cur.size(); r++) {
					String[] cols = cur.get(r);
					String[] newRow = new String[headers.size()];
					for(int c=0; c<cols.length; c++) {
						if(c < newIndices.length) { // index is within a header. Without a header data is missed. 
							newRow[newIndices[c]] = cols[c];
						}
					}
				}	
			}
		}
		all.set(0, headers.toArray(new String[] {}));
		
		autoConvertRecognisedTypes(all);
		
//		if(raggedArray) {
//			// probably works but needs test case added. 
//			throw new IllegalArgumentException("Need to stitch different tables together");
//		}
		return all;
	}

	private static void autoConvertRecognisedTypes(List<String[]> all) {
		// The approach , logic and function naming of this is intended to be similar to BabelDBJdbcDriver
		// i.e. look over everthing, guess the type, then convert.
		// makes it esy to add more later.
		// This date logic wasn't placed in babel as it's country specific so was reluctant.
		for(int c=0; c<all.get(0).length; c++) {
			int type = java.sql.Types.NULL;
			for(int r=1; r < all.size(); r++) {
				if(c < all.get(r).length) {
					String s = all.get(r)[c];
					type = JsonResultSetBuilder.SQL_TYPE_MERGER.apply(type, toSQLtype(s));
				}
			}

			if(type != java.sql.Types.NULL) {
				for(int r=1; r < all.size(); r++) {
					if(c < all.get(r).length) {
						String s = all.get(r)[c];
						all.get(r)[c] = getValue(s,type);
					}
				}
			}
		}
		
	}

	static String getValue(String s, Integer sqlType) {
		if(sqlType == java.sql.Types.DATE) {
			try {
				DateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
				Date d = fmt.parse(s);
				SimpleDateFormat oFormat = new SimpleDateFormat("yyyy-MM-dd");
				return oFormat.format(d);
			} catch (ParseException e) {
				log.fine("Error parsing " + s + " " + e.getLocalizedMessage());
			}
		}
		return s;
	}

	static Integer toSQLtype(String s) {
		if(s.contains(",") && "JFMASOND".contains(""+s.charAt(0))) {
			try {
				DateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
				Date d = fmt.parse(s);
				return java.sql.Types.DATE;
			} catch (ParseException e) {}
		}
		return java.sql.Types.NULL;
	}
	private static boolean equalsIgnoreCase(String[] a, String[] b) {
		Preconditions.checkNotNull(a);
		if(b == null || a.length != b.length) {
			return false;
		}
		for(int i=0; i<a.length; i++) {
			if(a[i] == null && b[i] == null) {
				return true;
			} else {
				if(a[i] == null || b[i] == null) {
					return false;
				}
				return a[i].equalsIgnoreCase(b[i]);
			}
		}
		return false;
	}

	public static List<String[]> toStringArray(Element table) {
		// We create a ragged array with differing number of "columns" to make sure we capture all data.
		// More tables on wikipedia have nested headers than have colSpan
		
		Element tbl = table;
		Element head = tbl.selectFirst("thead");
		Elements body = tbl.select("tbody");
		boolean hasBody = body.size()==1 && head != null;
		tbl = hasBody ? table : body.get(0);

		Elements rows = tbl.select("tr:not(:first-child)");
		Elements headerRows = new Elements(tbl.selectFirst("tr"));
		
		// If small reasonable number of headers, use those. Careful as some tables have th in every row.
		if(tbl.select("tr:has(> th)").size() < 0.5*rows.size()) {
			headerRows = tbl.select("tr:has(> th)");
			rows = tbl.select("tr:not(:has(> th))");
		}
		
		// wikipedia specific behaviour but NOT all tables have it.
		if(tbl.selectFirst("tr").classNames().contains("static-row-header")) {  
			rows = tbl.select("tr:not(.static-row-header)");
			headerRows = tbl.select("tr.static-row-header");
		}
		
		if(rows.size() == 0) {
			return Collections.emptyList();
		}
		List<String[]> tList = new ArrayList<>(rows.size());
		tList.add(new String[] {}); // Reserve first row for whatever we find in thead
		for (int r = 0; r < rows.size(); r++) {;
			tList.add(toArray(rows.get(r)));
		}
		
		List<String[]> headers = new ArrayList<>(); 
		for(Element row : headerRows) {
			headers.add(toArray(row));
		}
		// use largest header row if multiples - probably should really merge text from above sensibly but very difficult. 
		for(String[] h : headers) { // String length check is to prevent fully empty rows
			if(h.length > tList.get(0).length && (Arrays.toString(h).length() > Arrays.toString(tList.get(0)).length() && h.length > 1)) { 
				tList.set(0, h);
			}
		}
		// expand header if one of the data rows had more values
		int maxLength = tList.stream().mapToInt(e -> e.length).max().getAsInt();
		String[] curh = tList.get(0);
		if(curh.length < maxLength) {
			String[] sa = new String[maxLength];
			for(int i=0; i<maxLength; i++) {
				sa[i] = i < curh.length ? curh[i] : ("c" + i); 
			}
		}
		return tList;
	}

	private static String[] toArray(Element row) {
		Elements colVals = row.select("td,th");
		String[] colStrings = new String[colVals.size()];
		int c = 0;
		for(Element e : colVals) {
			colStrings[c++] = e.text(); 
		}
		return colStrings;
	}
}
