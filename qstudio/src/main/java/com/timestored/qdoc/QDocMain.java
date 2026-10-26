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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.google.common.collect.Lists;
import com.timestored.docs.Document;
import com.timestored.misc.HtmlUtils;
import com.timestored.qdoc.parser.*;

/**
 * Allows running qDoc on files / directories from the command line. 
 * Command line arguments are: 
 * 1-targetFolder- created if necessary 
 * 2-qSrcFolder searched recursively for .q files
 * 3-(optional) baseWeblink A http web address specifying a kdb server against which example call links should be generated.
 */
public class QDocMain {

	public static void main(String... args) {
		
		String baseWeblink = null;
		/* Check args  */
		if(args.length < 2) {
			printErr("", 1);
		}
		if(args.length >= 3) {
			baseWeblink = args[2];
		}
		File target = new File(args[0]);
		boolean suc = target.mkdirs();
		if(!target.isDirectory()) {
			printErr("target folder must be writeable directory: " + args[0], 1);
		}
		
		File qSrc = new File(args[1]);
		if(!qSrc.isDirectory()) {
			printErr("source folder must be directory:" + args[1], 2);
		}
		
		/* Get all .q docs */
		Collection<File> qs = listFileTree(qSrc, ".q");
		List<Document> docs = Lists.newArrayList();
		for(File qFile : qs) {
			try {
				docs.add(new Document(qFile));
				System.out.println("Processed: " + qFile);
			} catch (IOException e) {
				System.err.println("Problem with file:" + qFile);
			}
		}
		
		/* Generate docs and output any errors */
		List<String> errors = HtmlPqfOutputter.output(docs, target, baseWeblink);
		for(String e : errors) {
			System.err.println("Error: " + e);
		}

		/* Generate metrics and lint reports */
		generateReports(docs, target);
	}

	public static void generateReports(List<Document> docs, File target) {
		List<QMetricsReport> allMetrics = new ArrayList<>();
		StringBuilder lintCsv = new StringBuilder("file,line,col,severity,rule,message\n");

		for (Document doc : docs) {
			String code = doc.getContent();
			String name = doc.getTitle();
			if (code == null || code.trim().isEmpty()) continue;

			allMetrics.add(QCodeMetrics.analyze(code, name));

			for (ParseError e : QLinter.lint(code, name)) {
				String severity = "WARNING";
				if (e.ruleId != null) {
					try { severity = QLintRule.valueOf(e.ruleId).severity.name(); }
					catch (IllegalArgumentException ex) {}
				}
				lintCsv.append(String.format("%s,%d,%d,%s,%s,\"%s\"\n",
					name, e.line, e.col, severity,
					e.ruleId != null ? e.ruleId : "PARSE_ERROR",
					e.message.replace("\"", "\"\"")));
			}
		}

		try {
			writeFile(QMetricsReport.toCsv(allMetrics), new File(target, "metrics.csv"));
			writeFile(lintCsv.toString(), new File(target, "lint.csv"));
			System.out.println("Generated metrics.csv and lint.csv");
		} catch (IOException e) {
			System.err.println("Error writing CSV: " + e.getMessage());
		}

		try {
			String metricsHtml = HtmlUtils.getHead("Code Metrics", "") + QMetricsReport.toHtmlTable(allMetrics) + HtmlUtils.getTail();
			writeFile(metricsHtml, new File(target, "metrics.html"));
			writeFile(generateLintHtml(docs), new File(target, "lint.html"));
			System.out.println("Generated metrics.html and lint.html");
		} catch (IOException e) {
			System.err.println("Error writing HTML: " + e.getMessage());
		}
	}

	private static String generateLintHtml(List<Document> docs) {
		StringBuilder sb = new StringBuilder();
		sb.append(HtmlUtils.getHead("Lint Report", ""));
		sb.append("<h1>Lint Report</h1>\n");

		Map<String, Integer> ruleCounts = new LinkedHashMap<>();
		int totalErrors = 0, totalWarnings = 0;
		List<ParseError> allErrors = new ArrayList<>();

		for (Document doc : docs) {
			String code = doc.getContent();
			if (code == null) continue;
			for (ParseError e : QLinter.lint(code, doc.getTitle())) {
				allErrors.add(e);
				String rule = e.ruleId != null ? e.ruleId : "PARSE_ERROR";
				ruleCounts.merge(rule, 1, Integer::sum);
				if (e.ruleId != null) {
					try {
						if (QLintRule.valueOf(e.ruleId).severity == QLintRule.Severity.ERROR) totalErrors++;
						else totalWarnings++;
					} catch (IllegalArgumentException ex) { totalWarnings++; }
				} else {
					totalErrors++;
				}
			}
		}

		sb.append("<p>Total: ").append(allErrors.size()).append(" (")
			.append(totalErrors).append(" errors, ")
			.append(totalWarnings).append(" warnings)</p>\n");

		sb.append("<h2>By Rule</h2>\n<table border='1' cellpadding='4' cellspacing='0'>\n");
		sb.append("<tr><th>Rule</th><th>Count</th></tr>\n");
		for (Map.Entry<String, Integer> e : ruleCounts.entrySet()) {
			sb.append(String.format("<tr><td>%s</td><td>%d</td></tr>\n", e.getKey(), e.getValue()));
		}
		sb.append("</table>\n");
		sb.append(HtmlUtils.getTail());
		return sb.toString();
	}

	private static void writeFile(String content, File file) throws IOException {
		try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			w.write(content);
		}
	}

	private static void printErr(String msg, int num) {
		System.err.println("Must specify 2 args. 1-targetFolder, 2-qSrcFolder searched recursively");
		if(msg.length()>0) {
			System.err.println(msg);
		}
		System.exit(num);
	}
	
	private static Collection<File> listFileTree(File dir, String suffix) {
		if(dir.isDirectory()) {
		    Set<File> fileTree = new HashSet<File>();
		    for (File entry : dir.listFiles()) {
		    	if(entry != null) {
			        if (entry.isFile() && (suffix==null || entry.getName().endsWith(suffix))) { 
			        	fileTree.add(entry);
			        } else { 
			        	fileTree.addAll(listFileTree(entry, suffix));
			        }
		    	}
		    }
		    return fileTree;
		}
		return Collections.emptySet();
	}
}
