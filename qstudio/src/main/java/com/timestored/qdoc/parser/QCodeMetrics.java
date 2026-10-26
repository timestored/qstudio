package com.timestored.qdoc.parser;

import java.util.*;

/**
 * Analyzes q source code and produces quality metrics.
 */
public class QCodeMetrics {

    public static QMetricsReport analyze(String code) {
        return analyze(code, "unknown");
    }

    public static QMetricsReport analyze(String code, String fileName) {
        List<QToken> tokens = QLexer.tokenize(code);
        QParser parser = new QParser(tokens);
        parser.setTimeout(5000);
        QNode ast = parser.parseFile();
        return analyze(ast, tokens, fileName);
    }

    public static QMetricsReport analyze(QNode ast, List<QToken> tokens, String fileName) {
        // Line counts
        int totalLines = 1;
        int commentLines = 0;
        int blankLines = 0;
        Set<Integer> commentLineNums = new HashSet<>();
        Set<Integer> codeLineNums = new HashSet<>();

        for (QToken t : tokens) {
            if (t.type == QToken.Type.NEWLINE) totalLines++;
            else if (t.type == QToken.Type.COMMENT) commentLineNums.add(t.line);
            else if (t.type != QToken.Type.EOF) codeLineNums.add(t.line);
        }
        commentLines = commentLineNums.size();
        int codeLines = codeLineNums.size();

        // Function metrics
        List<int[]> funcStats = new ArrayList<>(); // [lineCount, paramCount, localCount, complexity]
        collectFunctionMetrics(ast, funcStats);

        int functionCount = funcStats.size();
        int minFL = 0, maxFL = 0, minP = 0, maxP = 0, minL = 0, maxL = 0, maxCC = 0;
        float avgFL = 0, avgP = 0, avgL = 0, avgCC = 0;

        if (functionCount > 0) {
            int sumFL = 0, sumP = 0, sumL = 0, sumCC = 0;
            minFL = Integer.MAX_VALUE; minP = Integer.MAX_VALUE; minL = Integer.MAX_VALUE;
            for (int[] fs : funcStats) {
                sumFL += fs[0]; sumP += fs[1]; sumL += fs[2]; sumCC += fs[3];
                if (fs[0] < minFL) minFL = fs[0]; if (fs[0] > maxFL) maxFL = fs[0];
                if (fs[1] < minP) minP = fs[1]; if (fs[1] > maxP) maxP = fs[1];
                if (fs[2] < minL) minL = fs[2]; if (fs[2] > maxL) maxL = fs[2];
                if (fs[3] > maxCC) maxCC = fs[3];
            }
            avgFL = (float) sumFL / functionCount;
            avgP = (float) sumP / functionCount;
            avgL = (float) sumL / functionCount;
            avgCC = (float) sumCC / functionCount;
        }

        // Nesting depth
        int maxNesting = computeMaxNesting(tokens);

        // Namespace count
        int namespaceCount = countNamespaces(ast);

        return new QMetricsReport(fileName, totalLines, codeLines, commentLines,
            functionCount, minFL, maxFL, avgFL, minP, maxP, avgP, minL, maxL, avgL,
            maxNesting, namespaceCount, maxCC, avgCC);
    }

    private static void collectFunctionMetrics(QNode node, List<int[]> stats) {
        if (node == null) return;
        if (node.type == QNode.Type.FUNCTION) {
            int paramCount = 0;
            int localCount = 0;
            int lineCount = 0;
            int complexity = 1; // base complexity
            boolean inBody = false;
            Set<String> locals = new HashSet<>();

            for (QNode c : node.children) {
                if (!inBody && c.type == QNode.Type.ID) {
                    paramCount++;
                } else {
                    inBody = true;
                    lineCount++; // each body statement ≈ one line
                    countLocalsAndComplexity(c, locals, new int[]{complexity});
                }
            }
            // Recount complexity properly
            int[] cc = {1};
            for (QNode c : node.children) {
                if (c.type != QNode.Type.ID) countComplexity(c, cc);
            }

            localCount = locals.size();
            stats.add(new int[]{lineCount, paramCount, localCount, cc[0]});
        }
        // Recurse into children (but collectFunctionMetrics handles nested functions too)
        for (QNode c : node.children) collectFunctionMetrics(c, stats);
    }

    private static void countLocalsAndComplexity(QNode node, Set<String> locals, int[] cc) {
        if (node.type == QNode.Type.ASSIGN && node.text != null && !node.text.startsWith("."))
            locals.add(node.text);
        if (node.type == QNode.Type.COND || node.type == QNode.Type.CONTROL) cc[0]++;
        if (node.type == QNode.Type.FUNCTION) return; // don't recurse into nested functions
        for (QNode c : node.children) countLocalsAndComplexity(c, locals, cc);
    }

    private static void countComplexity(QNode node, int[] cc) {
        if (node.type == QNode.Type.COND) cc[0]++;
        if (node.type == QNode.Type.CONTROL) cc[0]++;
        if (node.type == QNode.Type.FUNCTION) return;
        for (QNode c : node.children) countComplexity(c, cc);
    }

    private static int computeMaxNesting(List<QToken> tokens) {
        int depth = 0, maxDepth = 0;
        for (QToken t : tokens) {
            if (t.type == QToken.Type.LPAREN || t.type == QToken.Type.LBRACKET || t.type == QToken.Type.LBRACE) depth++;
            if (t.type == QToken.Type.RPAREN || t.type == QToken.Type.RBRACKET || t.type == QToken.Type.RBRACE) depth--;
            if (depth > maxDepth) maxDepth = depth;
        }
        return maxDepth;
    }

    private static int countNamespaces(QNode ast) {
        Set<String> namespaces = new HashSet<>();
        List<QNode> stmts = (ast.type == QNode.Type.STAT_LIST) ? ast.children : Collections.singletonList(ast);
        for (QNode s : stmts) {
            if (s.type == QNode.Type.SLASH && s.text != null && s.text.trim().startsWith("d ."))
                namespaces.add(s.text.trim().substring(2).trim());
        }
        return Math.max(1, namespaces.size()); // at least root namespace
    }
}
