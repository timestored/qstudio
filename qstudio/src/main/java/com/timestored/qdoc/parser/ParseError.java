package com.timestored.qdoc.parser;

/**
 * Structured parse error with source position information.
 * toString() produces the same format as the previous string-based errors
 * for backwards compatibility.
 */
public class ParseError {
    public final int offset;
    public final int line;
    public final int col;
    public final String message;
    public final String ruleId; // null for parse errors, e.g. "COND_EVENARGS" for lint

    public ParseError(QToken token, String message) {
        this(token, message, null);
    }

    public ParseError(QToken token, String message, String ruleId) {
        this.offset = token != null ? token.pos : -1;
        this.line = token != null ? token.line : 0;
        this.col = token != null ? token.col : 0;
        this.message = message;
        this.ruleId = ruleId;
    }

    public ParseError(int offset, int line, int col, String message) {
        this(offset, line, col, message, null);
    }

    public ParseError(int offset, int line, int col, String message, String ruleId) {
        this.offset = offset;
        this.line = line;
        this.col = col;
        this.message = message;
        this.ruleId = ruleId;
    }

    /** Get severity: ERROR for parse errors, or the lint rule's severity */
    public String getSeverity() {
        if (ruleId == null) return "ERROR";
        try { return QLintRule.valueOf(ruleId).severity.name(); }
        catch (IllegalArgumentException e) { return "ERROR"; }
    }

    @Override
    public String toString() {
        return "line " + line + ":" + col + " " + message;
    }
}
