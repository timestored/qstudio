package com.timestored.qdoc.parser;

import java.util.*;

/**
 * Code quality metrics for a single q source file.
 * Fields are package-private — use static formatting methods for output.
 */
public class QMetricsReport {
    private final String fileName;
    private final int totalLines;
    private final int codeLines;
    private final int commentLines;
    private final float commentRatio;

    private final int functionCount;
    private final int maxFunctionLines;
    private final float avgFunctionLines;
    private final int maxParams;
    private final float avgParams;

    private final int namespaceCount;
    private final int maxCyclomaticComplexity;

    QMetricsReport(String fileName, int totalLines, int codeLines, int commentLines,
            int functionCount, int minFunctionLines, int maxFunctionLines, float avgFunctionLines,
            int minParams, int maxParams, float avgParams,
            int minLocals, int maxLocals, float avgLocals,
            int maxNestingDepth, int namespaceCount,
            int maxCyclomaticComplexity, float avgCyclomaticComplexity) {
        this.fileName = fileName;
        this.totalLines = totalLines;
        this.codeLines = codeLines;
        this.commentLines = commentLines;
        this.commentRatio = totalLines > 0 ? (float) commentLines / totalLines : 0;
        this.functionCount = functionCount;
        this.maxFunctionLines = maxFunctionLines;
        this.avgFunctionLines = avgFunctionLines;
        this.maxParams = maxParams;
        this.avgParams = avgParams;
        this.namespaceCount = namespaceCount;
        this.maxCyclomaticComplexity = maxCyclomaticComplexity;
    }

    // ---- Static formatting methods ----

    public static String csvHeader() {
        return "file,totalLines,codeLines,commentLines,commentRatio,functionCount,maxFunctionLines,avgFunctionLines,maxParams,maxCyclomaticComplexity";
    }

    public String toCsvRow() {
        return String.format("%s,%d,%d,%d,%d,%d,%d,%.1f,%d,%d",
            fileName, totalLines, codeLines, commentLines,
            Math.round(commentRatio * 100), functionCount,
            maxFunctionLines, avgFunctionLines, maxParams,
            maxCyclomaticComplexity);
    }

    public static String toCsv(Collection<QMetricsReport> reports) {
        StringBuilder sb = new StringBuilder(csvHeader()).append("\n");
        for (QMetricsReport r : reports) sb.append(r.toCsvRow()).append("\n");
        return sb.toString();
    }

    public static String toHtmlTable(Collection<QMetricsReport> reports) {
        int totalFiles = reports.size(), totalLines = 0, totalFunctions = 0;
        for (QMetricsReport r : reports) { totalLines += r.totalLines; totalFunctions += r.functionCount; }

        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Code Metrics</h1>\n");
        sb.append("<p>Files: ").append(totalFiles).append(" | Lines: ").append(totalLines)
            .append(" | Functions: ").append(totalFunctions).append("</p>\n");
        sb.append("<table border='1' cellpadding='4' cellspacing='0'>\n");
        sb.append("<tr><th>File</th><th>Lines</th><th>Code</th><th>Comments</th><th>Functions</th><th>Max Lines</th><th>Max Complexity</th></tr>\n");
        for (QMetricsReport r : reports) {
            sb.append(String.format("<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td></tr>\n",
                r.fileName, r.totalLines, r.codeLines, r.commentLines, r.functionCount, r.maxFunctionLines, r.maxCyclomaticComplexity));
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    /** Summary row data for table display: {fileName, totalLines, codeLines, commentLines, functionCount, maxFunctionLines, maxCyclomaticComplexity} */
    public Object[] toTableRow() {
        return new Object[]{fileName, totalLines, codeLines, commentLines, functionCount, maxFunctionLines, maxCyclomaticComplexity};
    }

    /** Aggregate summary stats for a collection of reports */
    public static String toSummaryHtml(Collection<QMetricsReport> reports, String title) {
        int totalFiles = reports.size();
        int totalLines = 0, totalFunctions = 0, totalComments = 0, maxFL = 0, maxCC = 0;
        for (QMetricsReport r : reports) {
            totalLines += r.totalLines;
            totalFunctions += r.functionCount;
            totalComments += r.commentLines;
            if (r.maxFunctionLines > maxFL) maxFL = r.maxFunctionLines;
            if (r.maxCyclomaticComplexity > maxCC) maxCC = r.maxCyclomaticComplexity;
        }
        StringBuilder sb = new StringBuilder("<html><body style='padding:10px'>");
        sb.append("<h2>").append(title).append("</h2>");
        sb.append("<table cellpadding='4'>");
        sb.append("<tr><td><b>Files:</b></td><td>").append(totalFiles).append("</td></tr>");
        sb.append("<tr><td><b>Total lines:</b></td><td>").append(totalLines).append("</td></tr>");
        sb.append("<tr><td><b>Functions:</b></td><td>").append(totalFunctions).append("</td></tr>");
        sb.append("<tr><td><b>Comment lines:</b></td><td>").append(totalComments).append("</td></tr>");
        sb.append("<tr><td><b>Comment ratio:</b></td><td>").append(totalLines > 0 ? totalComments * 100 / totalLines : 0).append("%</td></tr>");
        sb.append("<tr><td><b>Max function length:</b></td><td>").append(maxFL).append(" lines</td></tr>");
        sb.append("<tr><td><b>Max complexity:</b></td><td>").append(maxCC).append("</td></tr>");
        sb.append("</table></body></html>");
        return sb.toString();
    }

    @Override
    public String toString() {
        int blankLines = totalLines - codeLines - commentLines;
        return String.format("=== Code Metrics: %s ===\n" +
            "Lines: %d (code: %d, comments: %d, blank: %d)\n" +
            "Comment ratio: %d%%\n" +
            "Functions: %d (avg %.0f lines, max %d lines)\n" +
            "Parameters: avg %.1f, max %d\n" +
            "Namespaces: %d\n" +
            "Max cyclomatic complexity: %d",
            fileName, totalLines, codeLines, commentLines, blankLines,
            Math.round(commentRatio * 100),
            functionCount, avgFunctionLines, maxFunctionLines,
            avgParams, maxParams,
            namespaceCount, maxCyclomaticComplexity);
    }
}
