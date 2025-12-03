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
package com.timestored.qdoc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.timestored.docs.Document;
import com.timestored.misc.HtmlUtils;
import com.timestored.misc.IOUtils;

/**
 * Converts a {@link ParsedQFile} to formatted HTML output similar to javadoc.
 */
public class HtmlPqfOutputter {
	
	private static final String FILE_SUFFIX = ".html";

	private static final Logger LOG = Logger.getLogger(HtmlPqfOutputter.class.getName());
	private static final String C = ";";
	
	/** 
	 * The maximum string length of the short entity description in the 
	 * overview table. 
	 */
	private static final int SD_LENGTH = 60;

	private static final String MANLISTING_HEADER = "// Generated qdoc tables to allow programmatic use of documentation.\r\n"
			+ ".man.funcs:([] fullname:(); ns:(); description:(); name:(); other:());\r\n"
			+ ".man.registerFunc:{`.man.funcs insert `fullname`ns`description`name`other!(),/:x};\r\n"
			+ ".man.args:([] fullname:(); tag:(); param:(); description:());\r\n"
			+ ".man.registerArg:{`.man.args insert `fullname`tag`param`description!(),/:x};\r\n"
			+ ".man.files:([] title:(); author:(); namespaces:(); header:());\r\n"
			+ ".man.registerFile:{`.man.files insert `title`author`namespaces`header!(),/:x};\r\n"
			+ ".man.filetags:([] title:(); tag:(); val:());\r\n"
			+ ".man.registerFileTag:{`.man.filetags insert `title`tag`val!(),/:x};\r\n"
			+ "\r\n"
			+ "/ @eg .man.getDocs[]\r\n"
			+ "/ @return table of format ([] fullname; tag; param; description)\r\n"
			+ ".man.getDocs:{[]\r\n"
			+ "    ft:select fullname,description from .man.funcs;\r\n"
			+ "    at:select fullname,tag,param,description from .man.args;\r\n"
			+ "    headerTbl:0!select description:\"\\n\" sv header by title,fullname:{first \"|\" vs x} each namespaces from .man.files where 1<count each trim namespaces,1<count each trim header;\r\n"
			+ "    filenameToNSDict:exec first fullname by title from headerTbl;\r\n"
			+ "    tagTbl:select fullname:filenameToNSDict title, tag,description:val from .man.filetags;\r\n"
			+ "    t:at uj ft uj tagTbl uj (``title _ headerTbl);\r\n"
			+ "    t:asc select from t where (0<count each tag) or (0<count each param) or (0<count each description);\r\n"
			+ "    t };    \r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "/ @eg .man.getTS[`GOOG]\r\n"
			+ ".man.getTS:{[symbol]  // random walk with set seed to mimic incoming data\r\n"
			+ "    seed:prd `int$string symbol;\r\n"
			+ "    {  walk:{ [seed;n]\r\n"
			+ "    	 r:{{ abs ((1664525*x)+1013904223) mod 4294967296}\\[y-1;x]};\r\n"
			+ "    	 prds (100+((r[seed;n]) mod 11)-5)%100};\r\n"
			+ "    	 c:{x mod `long$00:20:00.0t}x;   st:x-c;   cn:`long$c%1000;\r\n"
			+ "    	 ([] time:.z.d+st+1000*til cn; gold:walk[y;cn])  }[.z.t;seed]\r\n"
			+ "    };\r\n"
			+ "    \r\n"
			+ "/ @eg .man.getOHLC[`MSFT]\r\n"
			+ ".man.getOHLC:{[symbol]\r\n"
			+ "    seed:prd `int$string symbol;\r\n"
			+ "    {  r:{{ abs ((1664525*x)+1013904223) mod 4294967296}\\[y-1;x]};\r\n"
			+ "	walk:{ [r;seed;n] prds (100+((r[seed;n]) mod 11)-5)%100}[r;;];\r\n"
			+ "	c:{x mod `long$00:05:00.0t}x;   st:x-c;   cn:100+`long$c%1000;\r\n"
			+ "	t:([] time:`second$.z.d+st+1000*til cn; open:walk[y+4;cn]; close:walk[y+3;cn]);\r\n"
			+ "	-100 sublist update low:?[open > close;close;open]-(r[11;cn] mod 11)*0.02,high:?[open < close;close;open]+(r[44;cn] mod 11)*0.02,volume:(r[44;cn] mod 110) from t}[.z.t;seed]\r\n"
			+ "    };\r\n"
			+ "\r\n"
			+ "/ @eg .man.getSymbols[]\r\n"
			+ ".man.getSymbols:{[]\r\n"
			+ "    sym:`MSFT`AAPL`NVDA`AMZN`GOOGL`META`BRK.B`AVGO`TSLA`TSM`WMT`JPM`V`LLY`MA`NFLX`XOM`COST`ORCL`JNJ`PG`HD`UNH`SAP`ABBV`BAC`KO`NVO;\r\n"
			+ "    des:(\"Microsoft Corporation\";\"Apple Inc.\";\"NVIDIA Corporation\";\"Amazon.com, Inc.\";\"Alphabet Inc.\";\"Meta Platforms, Inc.\";\"Berkshire Hathaway Inc.\";\"Broadcom Inc.\";\"Tesla, Inc.\";\"Taiwan Semiconductor Manufacturing Company Limited\";\"Walmart Inc.\";\"JPMorgan Chase & Co.\";\"Visa Inc.\";\"Eli Lilly and Company\";\"Mastercard Incorporated\";\"Netflix, Inc.\";\"Exxon Mobil Corporation\";\"Costco Wholesale Corporation\";\"Oracle Corporation\";\"Johnson & Johnson\";\"The Procter & Gamble Company\";\"The Home Depot, Inc.\";\"UnitedHealth Group Incorporated\";\"SAP SE\";\"AbbVie Inc.\";\"Bank of America Corporation\";\"The Coca-Cola Company\";\"Novo Nordisk A/S\");\r\n"
			+ "    t:update query:(`$\".man.getTS[`XXX]\") from ([] symbol:sym; title:`$des);\r\n"
			+ "    t,:select query:(`$\".man.getOHLC[`XXX]\"),symbol:(`$string[symbol],\\:\"_OHLC\"),title from t;\r\n"
			+ "    t};\r\n"
			+ "\r\n"
			+ "\r\n";


	private HtmlPqfOutputter() {	}

	public static void writeFile(String s, File tgtFile) throws IOException {
		System.out.println("writing: " + tgtFile.getAbsolutePath());
		IOUtils.writeStringToFile(s, tgtFile);
	}

	/**
	 * Save documentation as HTMl to selected directory.
	 * @param documents The documents to output documentation for.
	 * @param outdir The directory to write to, any existing files will be overwritten
	 * @param baseWeblink The beginning of a URL that is added to to allow sending a webQuery showing example function calls. Can be null.
	 * @return list of error descriptions for end user, no errors = empty
	 */
	public static List<String> output(List<Document> documents, File outdir, String baseWeblink) {

		final List<String> errors = Lists.newArrayList();
		
		if(!outdir.isDirectory()) {	
			addLogError(errors, "Could not output documentation to " + outdir.getAbsolutePath()
					+ "\r\nIt is not a valid directory.");
			return errors;
		}
		
		if(outdir.list().length > 0) {
			LOG.warning("directory is not empty!");
		}
		List<ParsedQFile> parsedDocs = Lists.newArrayList();
		for(Document d : documents) {
			ParsedQFile pqf = QFileParser.parse(d.getContent(), d.getFilePath(), d.getTitle());
			parsedDocs.add(pqf);
		}
		String[] nsFileHtmls = generateIndexListing(parsedDocs);
		
		// output each HTML file
		for(ParsedQFile pqf : parsedDocs) {
			try {
				String filename = pqf.getFileTitle() + FILE_SUFFIX;
				File tgtFile = new File(outdir, filename);
				String html = generateHTML(pqf, baseWeblink);
				html = getTemplate(pqf.getFileTitle(), html, nsFileHtmls);
				writeFile(html, tgtFile);
			} catch (IOException e) {
				addLogError(errors, "Could not output documentation for " + pqf.getFileTitle());
			}
		}

		try {
			File tgtFile = new File(outdir, "man.q");
			System.out.println("writing: " + tgtFile.getAbsolutePath());
			FileWriter fw = new FileWriter(tgtFile);
			fw.write(MANLISTING_HEADER);
			generateQhelpTable(parsedDocs, fw);
			fw.close();
		} catch (IOException e) {
			addLogError(errors, "man.q");
		}
		
		// generate package-summary.html
		try {
			String fileSummaryHTML = generateFileSummaryHtml(parsedDocs);
			fileSummaryHTML = getTemplate("package-summary", fileSummaryHTML, nsFileHtmls);
			writeFile(fileSummaryHTML, new File(outdir, "index.html"));
		} catch (IOException e) {
			addLogError(errors, "Could not output package-summary.html");
		}

		try {
			saveQdocCssTo(outdir);
		} catch (IOException e) {
			addLogError(errors, "saveQdocCssTo fail");
		}
		
		return errors;
	}

	private static final String replace(String haystack, String start, String end, String replacement) {
		int p = haystack.indexOf(start);
		int q = haystack.indexOf(end);
		return haystack.substring(0, p) + replacement + haystack.substring(q, haystack.length());
	}

	public static void saveQdocCssTo(File outdir) throws IOException {
		String css = IOUtils.toString(HtmlPqfOutputter.class, "qdoc2.css");
		writeFile(css, new File(outdir, "qdoc2.css"));
	}
	
	private static String getTemplate(String title, String main, String[] namespaceFiles) throws IOException {
		String s = IOUtils.toString(HtmlPqfOutputter.class, "qdoc-template.html");
		s = s.replace("Foobar", title);
		s = replace(s, "<!--MAIN_START-->", "<!--MAIN_END-->", main);
		// workaround for naming convention dependency to avoid regenerating each time
		String t = (title.endsWith(".q") ? title.substring(0, title.length()-2) : title).replace(".", "");
		String ns = namespaceFiles[0].replace("cls-"+t+"-cls", "current");
		String fils = namespaceFiles[1].replace("cls-"+t+"-cls", "current");
		s = replace(s, "<!--NAMESPACE_START-->", "<!--NAMESPACE_END-->", ns);
		s = replace(s, "<!--FILES_START-->", "<!--FILES_END-->", fils);
		return s;
	}
	
	/**
	 * @return File summary that is on right hand side of qdoc frameset usually. Giving
	 * 		a summary of each file.
	 */
	private static String generateFileSummaryHtml(List<ParsedQFile> pqfiles) {

		// construct output HTML from parts
		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Files</h1>");

		Collections.sort(pqfiles, new Comparator<ParsedQFile>() {
			@Override public int compare(ParsedQFile o1, ParsedQFile o2) {
				return o1.getFileTitle().compareTo(o2.getFileTitle());
		}});
		
		// file links
		sb.append("<table  class='overviewSummary'>");
		sb.append("<tr><th>File</th><th>Description</th>");
		
		for(ParsedQFile pqf : pqfiles) {
			String fn = pqf.getFileTitle() + FILE_SUFFIX;
			sb.append("<tr><td>");
			makeLink(fn, pqf.getFileTitle());
			sb.append(makeLink(fn, pqf.getFileTitle()));
			sb.append("</td><td>");
			sb.append(pqf.getHeaderDoc());
			sb.append("</td></tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}
	
	/** ml for make link */
	private static String makeLI(String href, String txt) {
		String t = (txt.endsWith(".q") ? txt.substring(0, txt.length()-2) : txt).replace(".", "");
		String cls = "cls-" + t + "-cls";
		return "<li class=\"toctree-l1 " + cls + "\"><a class=\"reference internal " + cls + "\" href='" + href + "' >" + txt + "</a></li>";
	}
	
	private static String makeLink(String href, String txt) {
		return "<a href='" + href + "' >" + txt + "</a>";
	}
	
	
	private static String esc(String s) {
		if(s == null) {
			return "";
		}
		return "\"" + s.replace("\"", "\\\"") + "\"";
	}

	private static void generateQhelpTable(List<ParsedQFile> pqfiles, FileWriter w) throws IOException {
		for(ParsedQFile pqFile : pqfiles) {
			w.write(".man.registerFile (");
			w.write(esc(pqFile.getFileTitle()));
			w.write(C);
			w.write(esc(pqFile.getAuthor()));
			w.write(C);
			w.write(esc(Joiner.on("|").join(pqFile.getNamespaces())));
			w.write(C);
			w.write(esc(pqFile.getHeaderDoc()));
			w.write(");\r\n");
			for(Entry<String, String> tagEntry : pqFile.getHeaderTags().entrySet()) {
				w.write(".man.registerFileTag (");
				w.write(esc(pqFile.getFileTitle()));
				w.write(C);
				w.write(esc(tagEntry.getKey()));
				w.write(C);
				w.write(esc(tagEntry.getValue()));
				w.write(");\r\n");
			}
			for(ParsedQEntity parsedQentity : pqFile.getQEntities()) {
				w.write(".man.registerFunc (");
				w.write(esc(parsedQentity.getFullName()));
				w.write(C);
				w.write(esc(parsedQentity.getNamespace()));
				w.write(C);
				w.write(esc(parsedQentity.getDocDescription()));
				w.write(C);
				w.write(esc(parsedQentity.getDocName()));
				w.write(C);
				String eg = parsedQentity.getTag("eg");
				if(eg == null) {
					eg = parsedQentity.getTag("example");
				}
				w.write(esc(eg == null ? "" : eg));
				w.write(");\r\n");
				
				for(Entry<String, Map<String, String>> namedTags : parsedQentity.getNamedTags().entrySet()) {
					for(Entry<String, String> paramDescriptionPair : namedTags.getValue().entrySet()) {
						writeArg(w, parsedQentity.getFullName(), namedTags.getKey(), 
								paramDescriptionPair.getKey(), paramDescriptionPair.getValue());
					}
				}
				for(Entry<String, String> tags : parsedQentity.getTags().entrySet()) {
					writeArg(w, parsedQentity.getFullName(), tags.getKey(), "", tags.getValue());
				}
			}
		}
	}


	private static void writeArg(FileWriter w, String name, String tag, String subTag,
			String description) throws IOException {
		w.write(".man.registerArg (");
		w.write(esc(name));
		w.write(C);
		w.write(esc(tag));
		w.write(C);
		w.write(esc(subTag));
		w.write(C);
		w.write(esc(description));
		w.write(");\r\n");
	}
	
	/**
	 * Generate HTML page that lists files and namespaces for all files in our list.
	 * This page is usually found in left side of qdocs and is used for navigation.
	 * @param pqfiles
	 * @return HTML page that lists files and namespaces for all files in our list.
	 */
	private static String[] generateIndexListing(List<ParsedQFile> pqfiles) {
		
		// file links
		List<String> fileLinks = Lists.newArrayList();
		for(ParsedQFile pqf : pqfiles) {
			String fn = pqf.getFileTitle() + FILE_SUFFIX;
			fileLinks.add(makeLI(fn, pqf.getFileTitle()));
			Collections.sort(fileLinks);
		}
		
		// build map from namespace's to distinct files
		Multimap<String, String> namespaceToFiles = ArrayListMultimap.create();
		for(ParsedQFile pqf : pqfiles) {
			for(ParsedQEntity pqe : pqf.getQEntities()) {
				Collection<String> files = namespaceToFiles.get(pqe.getNamespace());
				if(files==null || !files.contains(pqf.getFileTitle())) {
					namespaceToFiles.put(pqe.getNamespace(), pqf.getFileTitle());
				} 
			}
		}
		
		// generate html of links
		// have to be careful of namespaces that may occur in more than one file
		List<String> nsLinks = Lists.newArrayList();
		List<String> nsListing = Lists.newArrayList(namespaceToFiles.asMap().keySet());
		Collections.sort(nsListing);
		for(String ns : nsListing) {
			List<String> files = new ArrayList<String>(namespaceToFiles.get(ns));
			if(files.size()>1) {
				List<String> fileListing = Lists.newArrayList();
				for(String fileTitle : files) {
					fileListing.add(makeLI(fileTitle + FILE_SUFFIX, fileTitle));
					nsLinks.add(makeLI(fileTitle + FILE_SUFFIX, ns + " (" + fileTitle + ")"));
				}
//				nsLinks.add("<li>"+ ns + ": " + "\n\t<ul>" + Joiner.on("\n\t").join(fileListing) + "</ul></li>");
			} else {
				String fn = files.get(0) + FILE_SUFFIX;
				nsLinks.add(makeLI(fn, ns));
			}
		}

		String nsHTML =  "\n<ul>" + Joiner.on("\n\t").join(nsLinks) + "\n</ul>";
		String fileHTML =  "\n<ul>" + Joiner.on("\n\t").join(fileLinks) + "\n</ul>";
		return new String[] { nsHTML, fileHTML };
	}
	
	private static void addLogError(final List<String> errors, String msg) {
		LOG.severe(msg);
		errors.add(msg);
	}

	/**
	 * @return Full HTML page for one {@link ParsedQFile} showing all docs/entities.
	 */
	static String generateHTML(ParsedQFile pqf) throws IOException {
		String [] nsFiles = new String[] { makeLI("NAMESPACER","namespacer"),
				makeLI("FILES","files")};
		return getTemplate(pqf.getFileTitle(), generateHTML(pqf, "http://www.pulseui.net?qry="), nsFiles);
	}
	
	/**
	 * @param baseWeblink The beginning of a URL that is added to to allow sending a webQuery showing example function calls. Can be null.
	 * @return Full HTML page for one {@link ParsedQFile} showing all docs/entities. 
	 */
	static String generateHTML(ParsedQFile pqf, String baseWeblink) {
		StringBuilder s = new StringBuilder();
			s.append("\r\n\t<div id='headerDoc'>");
			s.append(pqf.getHeaderDoc());
			s.append(HtmlUtils.toList(pqf.getHeaderTags(), true));
			
			s.append("</div>");
			
			s.append("\r\n\t<div id='summary'>");
			s.append("<h2>Entity Summary</h2>");
			appendTableOverview(s, pqf);
			s.append("</div>");
			
			s.append("\r\n\t<div class='details'>");
			s.append("\r\n\t<h2>Entity Details</h2>");
			appendEntityDetails(s, pqf, baseWeblink);
			s.append("</div>");
		return s.toString();
	}

	/** @return true iff the documented entity is for internal use only */
	private static boolean isInternal(ParsedQEntity e) {
		return e.getDocName().contains(".i.");
	}
	
	/** @return true if more than just name/namespace is known about this entity. */
	private static boolean hasDetails(ParsedQEntity e) {
		return !e.getDocDescription().equals(e.getShortDescription()) 
				|| e.getTags().size()>0
				|| e.getNamedTags().size()>0;
	}

	/**
	 * For a given entity return a unique HTML id.
	 */
	private static String getHtmlId(ParsedQEntity e) {
		return e.getDocName().replace('.', '-');
	}
	

	private static void appendEntityDetails(StringBuilder s, ParsedQFile pqf, String baseWeblink) {
		for(ParsedQEntity e : pqf.getQEntities()) {
			if(hasDetails(e) && !isInternal(e)) {
				s.append("\r\n\t\t<div class='entity' id='"+getHtmlId(e)+"'>");
				s.append("<h2>");
				s.append(e.getDocName());
				s.append("</h2>");
				s.append(HtmlUtils.extractBody(e.getHtmlDoc(false, baseWeblink)));
				s.append("</div>");
			}
		}
	}

	private static void appendTableOverview(StringBuilder s, ParsedQFile pqf) {

		s.append("<table class='overviewSummary'><tbody>");
		s.append("\n<tr><th>Entities</th><th>Short Description</th></tr>");
		
		for(ParsedQEntity e : pqf.getQEntities()) {
			// Ignore internal functions, we don't want to generate HTML for them
			if(!isInternal(e)) {
				s.append("\r\n\t\t<tr><td>");
				if(hasDetails(e)) {
					s.append("<a href='#"+getHtmlId(e)+"'>");
					s.append(e.getDocName());
					s.append("</a>");
				} else {
					s.append(e.getDocName());
				}
				s.append("</td><td>");
				String sd = e.getShortDescription();
				s.append(sd.length()<=SD_LENGTH ? sd : sd.substring(0, SD_LENGTH));
				s.append("</td></tr>");
			}
		}
		s.append("\r\n</tbody></table>");
	}


}
