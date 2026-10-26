package com.timestored.qdoc.parser;

public class QToken {
    public enum Type {
        // Classified literals
        BOOL, SHORT, INT, LONG, REAL, FLOAT,
        DATE, MONTH, TIME, MINUTE, SECOND, TIMESPAN, TIMESTAMP, DATETIME,
        BYTE, SYMBOL, STRING, CHAR,

        // Identifiers and operators
        ID, MONOP, BINOP, ADVERB,

        // Structural
        COLON, SEMI, COMMA,
        LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,

        // Keywords (also IDs but recognized for highlighting)
        KEYWORD,

        // Other
        NEWLINE, SLASH_CMD, COMMENT, ERROR, EOF
    }

    public final Type type;
    public final String text;
    public final int line;
    public final int col;
    public final int pos;

    public QToken(Type type, String text, int line, int col, int pos) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.col = col;
        this.pos = pos;
    }

    @Override
    public String toString() {
        return type + "(" + text.replace("\n","\\n").replace("\r","\\r") + ")";
    }
}
