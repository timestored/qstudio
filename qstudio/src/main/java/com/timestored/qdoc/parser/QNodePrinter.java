package com.timestored.qdoc.parser;

/**
 * Formats a QNode AST in kdb's parse output format.
 * E.g. "2+11" parses to an AST that formats as "(+;2;11)".
 */
public class QNodePrinter {

    public static String format(QNode node) {
        if (node == null) return "";
        switch (node.type) {
            case LITERAL:
                return node.text != null ? node.text : "";
            case ID:
                return "`" + (node.text != null ? node.text : "");
            case OPERATOR:
                // Monadic operator applied to an arg: e.g. (:;val)
                if (!node.children.isEmpty()) {
                    StringBuilder sb = new StringBuilder("(");
                    sb.append(node.text != null ? node.text : "");
                    for (QNode child : node.children) {
                        sb.append(";").append(format(child));
                    }
                    sb.append(")");
                    return sb.toString();
                }
                return node.text != null ? node.text : "";
            case EMPTY:
            case PROJECTION:
                return "::";
            case BINARY: {
                StringBuilder sb = new StringBuilder("(");
                sb.append(node.text != null ? node.text : "");
                for (QNode child : node.children) {
                    sb.append(";").append(format(child));
                }
                sb.append(")");
                return sb.toString();
            }
            case ASSIGN: {
                // children[0] = target (ID or indexed), children[1..] = value
                // For simple: (:;`name;value)
                // For indexed assignment: children = [target, idx1, ..., idxN, value]
                StringBuilder sb = new StringBuilder("(:;");
                for (int i = 0; i < node.children.size(); i++) {
                    if (i > 0) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append(")");
                return sb.toString();
            }
            case APPLY: {
                // children[0] = function, children[1..] = args
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < node.children.size(); i++) {
                    if (i > 0) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append(")");
                return sb.toString();
            }
            case ADVERB: {
                // children[0] = the verb/function being adverbed
                // text = the adverb symbol (/, \, ', etc.)
                StringBuilder sb = new StringBuilder("(");
                sb.append(node.text != null ? node.text : "");
                for (QNode child : node.children) {
                    sb.append(";").append(format(child));
                }
                sb.append(")");
                return sb.toString();
            }
            case STAT_LIST: {
                StringBuilder sb = new StringBuilder("(;");
                for (int i = 0; i < node.children.size(); i++) {
                    if (i > 0) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append(")");
                return sb.toString();
            }
            case FUNCTION: {
                // Reconstruct function source from children
                // For simplicity, show {params|body} format
                StringBuilder sb = new StringBuilder("{");
                boolean isExplicit = node.text != null && node.text.startsWith("explicit");
                int bodyStart = 0;
                if (isExplicit) {
                    sb.append("[");
                    int paramCount = getExplicitParamCount(node);
                    for (int i = 0; i < paramCount && i < node.children.size(); i++) {
                        if (i > 0) sb.append(";");
                        sb.append(node.children.get(i).text);
                        bodyStart = i + 1;
                    }
                    sb.append("] ");
                }
                for (int i = bodyStart; i < node.children.size(); i++) {
                    if (i > bodyStart) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append("}");
                return sb.toString();
            }
            case PARENS: {
                // Single expression in parens, or semicolon-separated
                if (node.children.size() == 1) {
                    return format(node.children.get(0));
                }
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < node.children.size(); i++) {
                    if (i > 0) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append(")");
                return sb.toString();
            }
            case COND: {
                // $[cond;true;false] — children[0] is $, rest are args
                StringBuilder sb = new StringBuilder("($;");
                for (int i = 1; i < node.children.size(); i++) {
                    if (i > 1) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append(")");
                return sb.toString();
            }
            case CONTROL: {
                // if[cond;body] etc — children[0] is the keyword ID
                StringBuilder sb = new StringBuilder("(");
                sb.append(node.text != null ? node.text : "");
                for (int i = 1; i < node.children.size(); i++) {
                    sb.append(";").append(format(node.children.get(i)));
                }
                sb.append(")");
                return sb.toString();
            }
            case QUERY: {
                // q-SQL: (keyword;cols;table;where...)
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < node.children.size(); i++) {
                    if (i > 0) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append(")");
                return sb.toString();
            }
            case TABLE: {
                StringBuilder sb = new StringBuilder("(+;");
                for (int i = 0; i < node.children.size(); i++) {
                    if (i > 0) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append(")");
                return sb.toString();
            }
            case SLASH: {
                return "\\" + (node.text != null ? node.text : "");
            }
            default:
                return node.text != null ? node.text : "";
        }
    }

    private static int getExplicitParamCount(QNode func) {
        if (func.text == null || !func.text.startsWith("explicit:")) return 0;
        try { return Integer.parseInt(func.text.substring("explicit:".length())); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Heuristic: in a FUNCTION node with "explicit" text, leading ID children
     * are parameters until we hit a non-ID or an assignment/expression.
     */
    private static boolean isParamLike(QNode func, int idx) {
        QNode child = func.children.get(idx);
        if (child.type != QNode.Type.ID) return false;
        // If next child is also a plain ID, both could be params
        // If next child is something else, this is the last param
        // We rely on the parser putting params first
        // Simple check: if all children from 0..idx are IDs with no children, they're params
        for (int i = 0; i <= idx; i++) {
            if (func.children.get(i).type != QNode.Type.ID) return false;
            if (!func.children.get(i).children.isEmpty()) return false;
        }
        // Check that the next child is NOT a plain leaf ID (which would also be a param)
        // Actually, we need to count params. The parser groups them, so consecutive
        // leaf IDs at the start are params when kind is "explicit"
        return true;
    }

    /** Pretty-print with each statement on a new line, indented by depth */
    public static String formatPretty(QNode node) {
        StringBuilder sb = new StringBuilder();
        formatPretty(node, sb, 0);
        return sb.toString();
    }

    private static void formatPretty(QNode node, StringBuilder sb, int depth) {
        if (node == null) return;
        String indent = "\t".repeat(depth);

        switch (node.type) {
            case STAT_LIST:
                for (int i = 0; i < node.children.size(); i++) {
                    if (i > 0) sb.append("\n");
                    formatPretty(node.children.get(i), sb, depth);
                }
                break;

            case ASSIGN: {
                // Check if the last child (the value) is a FUNCTION — if so, pretty-print it
                QNode lastChild = node.children.isEmpty() ? null : node.children.get(node.children.size() - 1);
                if (lastChild != null && lastChild.type == QNode.Type.FUNCTION) {
                    sb.append(indent).append("(:;");
                    for (int i = 0; i < node.children.size() - 1; i++) {
                        if (i > 0) sb.append(";");
                        sb.append(format(node.children.get(i)));
                    }
                    sb.append(";");
                    formatFunctionPretty(lastChild, sb, depth);
                    sb.append(")");
                } else {
                    sb.append(indent).append(format(node));
                }
                break;
            }

            case FUNCTION:
                sb.append(indent);
                formatFunctionPretty(node, sb, depth);
                break;

            case COND:
                sb.append(indent).append("$[");
                for (int i = 1; i < node.children.size(); i++) {
                    if (i > 1) sb.append(";");
                    sb.append("\n").append(indent).append("\t");
                    sb.append(format(node.children.get(i)));
                }
                sb.append("]");
                break;

            case CONTROL:
                sb.append(indent).append(node.text != null ? node.text : "").append("[");
                for (int i = 1; i < node.children.size(); i++) {
                    if (i > 1) sb.append(";");
                    sb.append(format(node.children.get(i)));
                }
                sb.append("]");
                break;

            case QUERY:
                sb.append(indent).append(format(node));
                break;

            case SLASH:
                sb.append(indent).append("\\").append(node.text != null ? node.text : "");
                break;

            default:
                sb.append(indent).append(format(node));
                break;
        }
    }

    /** Render a FUNCTION node starting from the '{'. Does NOT prepend indent. */
    private static void formatFunctionPretty(QNode node, StringBuilder sb, int depth) {
        String indent = "\t".repeat(depth);
        sb.append("{");
        boolean isExplicit = node.text != null && node.text.startsWith("explicit");
        int bodyStart = 0;
        if (isExplicit) {
            sb.append("[");
            int paramCount = getExplicitParamCount(node);
            for (int i = 0; i < paramCount && i < node.children.size(); i++) {
                if (i > 0) sb.append(";");
                sb.append(node.children.get(i).text);
                bodyStart = i + 1;
            }
            sb.append("]");
        }
        int bodyCount = node.children.size() - bodyStart;
        if (bodyCount <= 1) {
            // Single-line function body
            sb.append(" ");
            for (int i = bodyStart; i < node.children.size(); i++) {
                sb.append(format(node.children.get(i)));
            }
            sb.append("}");
        } else {
            sb.append("\n");
            for (int i = bodyStart; i < node.children.size(); i++) {
                if (i > bodyStart) sb.append("\n");
                formatPretty(node.children.get(i), sb, depth + 1);
            }
            sb.append("\n").append(indent).append("}");
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java QNodePrinter <q-expression>");
            System.exit(1);
        }
        String code = args[0];
        QNode ast = QParser.parse(code);
        System.out.println(formatPretty(ast));
    }
}
