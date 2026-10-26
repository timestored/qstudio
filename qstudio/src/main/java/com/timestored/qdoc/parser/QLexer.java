package com.timestored.qdoc.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Hand-written lexer for q/kdb+.
 * Context-sensitive: / and \ are adverb or comment/command depending on previous token.
 * Unified atom gobbling: all numeric/temporal literals share one rule, classified at end.
 */
public class QLexer {

    private static final Set<String> MONOPS = Set.of(
        "til","enlist","first","last","distinct","count","key","reverse","null","not",
        "raze","rotate","show","system","desc","asc","get","abs","all","any","avg","avgs",
        "exp","floor","ceiling","cos","sin","tan","acos","asin","atan","log","fills","flip",
        "type","attr","reciprocal","sqrt","svar","sdev","var","dev","differ","getenv","group",
        "iasc","rank","idesc","max","maxs","min","mins","prd","prds","sum","sums","deltas",
        "exit","neg","inv","med","mmu","read0","read1","rand","ratios","signum","value",
        "trim","rtrim","ltrim","upper","lower","string","hcount","hdel","hsym","hopen","hclose",
        "next","prev","gtime","ltime","parse","views","tables","keys",
        "asof","cor","cov","csv","dsave","ema","eval","fkeys","load","lsq","md5","meta",
        "reval","rload","rsave","save","scov","ssr","ungroup","upsert","view",
        "xasc","xdesc","xgroup","xprev","xrank"
    );

    private static final Set<String> BINOPS_WORD = Set.of(
        "and","or","set","mod","sublist","div","except","inter","union","cut","cross",
        "bin","binr","in","within","insert","wsum","wavg","xexp","xbar","setenv",
        "like","ss","sv","vs","mdev","mmax","mmin","msum","mavg","mcount",
        "aj","ej","ij","lj","pj","uj","wj","xlog","xkey","xcols","xcol","cols",
        "each","peach","scan","over","prior",
        "fby"
    );

    private static final Set<String> KEYWORDS = Set.of(
        "select","update","delete","exec","by","from","where","if","do","while"
    );

    /** All q reserved words — union of monadic ops, binary ops, and keywords */
    public static Set<String> getReservedWords() {
        Set<String> all = new java.util.HashSet<>();
        all.addAll(MONOPS);
        all.addAll(BINOPS_WORD);
        all.addAll(KEYWORDS);
        return all;
    }

    private final String src;
    private int pos;
    private int line = 1;
    private int col = 0;
    private QToken.Type prevType = null; // for context-sensitive / and \
    private int bracketDepth = 0; // track {([...])} for multiline continuation

    public QLexer(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static List<QToken> tokenize(String code) {
        // Preprocess: strip multiline comment blocks (/ alone on line ... \ alone on line) and k) lines
        code = preprocess(code);
        QLexer lexer = new QLexer(code);
        List<QToken> tokens = new ArrayList<>();
        while (true) {
            QToken t = lexer.next();
            tokens.add(t);
            if (t.type == QToken.Type.EOF) break;
        }
        return tokens;
    }

    static String preprocess(String code) {
        StringBuilder sb = new StringBuilder(code.length());
        String[] lines = code.split("\n", -1);
        boolean inCommentBlock = false;
        for (int li = 0; li < lines.length; li++) {
            String trimmed = lines[li].trim();
            if (inCommentBlock) {
                if (trimmed.equals("\\")) inCommentBlock = false;
                // Pad to original length with spaces to preserve character offsets
                for (int si = 0; si < lines[li].length(); si++) sb.append(' ');
                if (li > 0) sb.append("\n");
                continue;
            }
            if (trimmed.equals("/")) {
                inCommentBlock = true;
                for (int si = 0; si < lines[li].length(); si++) sb.append(' ');
                if (li > 0) sb.append("\n");
                continue;
            }
            if (trimmed.startsWith("k)")) {
                for (int si = 0; si < lines[li].length(); si++) sb.append(' ');
                if (li > 0) sb.append("\n");
                continue;
            }
            if (li > 0) sb.append("\n");
            sb.append(lines[li]);
        }
        return sb.toString();
    }

    // ---- Character helpers ----

    private char ch() { return pos < src.length() ? src.charAt(pos) : 0; }
    private char ch(int offset) { int p = pos + offset; return p < src.length() && p >= 0 ? src.charAt(p) : 0; }
    private boolean atEnd() { return pos >= src.length(); }

    private void advance() {
        if (pos < src.length()) {
            if (src.charAt(pos) == '\n') { line++; col = 0; }
            else { col++; }
            pos++;
        }
    }

    private void advance(int n) { for (int i = 0; i < n; i++) advance(); }

    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private boolean isIdChar(char c) { return isLetter(c) || isDigit(c) || c == '.' || c == '_'; }
    private boolean isAtomChar(char c) { return isDigit(c) || c == '.' || c == ':' || c == '-' || c == 'D' || c == 'T' || c == 'e' || c == 'E'; }
    private boolean isTypeSuffix(char c) { return "bcghijefmnpstuvdxz".indexOf(c) >= 0; }

    /** Does the previous token indicate an expression result (for / \ context) */
    private boolean afterExpr() {
        return prevType == QToken.Type.ID || prevType == QToken.Type.RPAREN
            || prevType == QToken.Type.RBRACKET || prevType == QToken.Type.RBRACE
            || prevType == QToken.Type.MONOP || prevType == QToken.Type.BINOP
            || prevType == QToken.Type.KEYWORD || prevType == QToken.Type.ADVERB
            || prevType == QToken.Type.COLON
            || isLiteralType(prevType);
    }

    /** Does the previous token end a value expression (for minus/negative disambiguation) */
    private boolean afterValue() {
        return prevType == QToken.Type.ID || prevType == QToken.Type.RPAREN
            || prevType == QToken.Type.RBRACKET || prevType == QToken.Type.RBRACE
            || isLiteralType(prevType);
    }

    private boolean isLiteralType(QToken.Type t) {
        return t != null && (t == QToken.Type.BOOL || t == QToken.Type.SHORT || t == QToken.Type.INT
            || t == QToken.Type.LONG || t == QToken.Type.REAL || t == QToken.Type.FLOAT
            || t == QToken.Type.DATE || t == QToken.Type.MONTH || t == QToken.Type.TIME
            || t == QToken.Type.MINUTE || t == QToken.Type.SECOND || t == QToken.Type.TIMESPAN
            || t == QToken.Type.TIMESTAMP || t == QToken.Type.DATETIME || t == QToken.Type.BYTE
            || t == QToken.Type.SYMBOL || t == QToken.Type.STRING || t == QToken.Type.CHAR);
    }

    /** Is position at start of line (col 0 or only whitespace before on this line) */
    private boolean atLineStart() {
        for (int p = pos - 1; p >= 0; p--) {
            char c = src.charAt(p);
            if (c == '\n') return true;
            if (c != ' ' && c != '\t') return false;
        }
        return true; // start of file
    }

    // ---- Main lexer ----

    public QToken next() {
        skipWhitespace();
        if (atEnd()) return emit(QToken.Type.EOF, "");

        int startLine = line, startCol = col, startPos = pos;
        char c = ch();

        // Newline
        if (c == '\n' || c == '\r') {
            return lexNewline(startLine, startCol, startPos);
        }

        // String
        if (c == '"') return lexString(startLine, startCol, startPos);

        // Symbol
        if (c == '`') return lexSymbol(startLine, startCol, startPos);

        // Byte literal: 0x...
        if (c == '0' && ch(1) == 'x' && !isDigit(ch(2)) || c == '0' && ch(1) == 'x' && isHexDigit(ch(2))) {
            if (ch(1) == 'x') return lexByte(startLine, startCol, startPos);
        }

        // Special atoms: 0N, 0W, 0n, 0w (and -0W, -0w), with optional type suffix (0Ni, 0Wi, etc.)
        if ((c == '0' || (c == '-' && ch(1) == '0')) && !afterValue()) {
            int lookAhead = (c == '-') ? 2 : 1;
            if (pos + lookAhead < src.length()) {
                char sc2 = src.charAt(pos + lookAhead);
                if (sc2 == 'N' || sc2 == 'W' || sc2 == 'n' || sc2 == 'w') {
                    // Check: either end of input, or next is a type suffix (not followed by more letters), or next is not an id char
                    int afterNW = pos + lookAhead + 1;
                    boolean canTake = false;
                    if (afterNW >= src.length()) {
                        canTake = true;
                    } else {
                        char afterChar = src.charAt(afterNW);
                        if (isTypeSuffix(afterChar) && (afterNW + 1 >= src.length() || !isLetter(src.charAt(afterNW + 1)))) {
                            canTake = true; // e.g. 0Ni, 0Wi
                        } else if (!isIdChar(afterChar)) {
                            canTake = true; // e.g. 0N followed by space/operator
                        }
                    }
                    if (canTake) {
                        StringBuilder sb = new StringBuilder();
                        if (c == '-') { sb.append('-'); advance(); }
                        sb.append('0'); advance();
                        sb.append(ch()); advance();
                        // Optional type suffix
                        if (!atEnd() && isTypeSuffix(ch()) && (pos + 1 >= src.length() || !isLetter(ch(1)))) {
                            sb.append(ch()); advance();
                        }
                        return emit(classifyAtom(sb.toString()), sb.toString(), startLine, startCol, startPos);
                    }
                }
            }
        }

        // Atom/AtomList: starts with digit, or - followed by digit/dot
        if (isDigit(c) || (c == '-' && (isDigit(ch(1)) || ch(1) == '.') && !afterValue())) {
            return lexAtom(startLine, startCol, startPos);
        }

        // Dot followed by digit: .5 style float
        if (c == '.' && isDigit(ch(1))) {
            return lexAtom(startLine, startCol, startPos);
        }

        // Two-char operators: ::, <=, >=, <>, +:, *:, 0:, 1:, 2:, /: \: ':
        if (c == ':' && ch(1) == ':') { advance(2); return emit(QToken.Type.MONOP, "::", startLine, startCol, startPos); }
        if (c == '<' && ch(1) == '=') { advance(2); return emit(QToken.Type.BINOP, "<=", startLine, startCol, startPos); }
        if (c == '>' && ch(1) == '=') { advance(2); return emit(QToken.Type.BINOP, ">=", startLine, startCol, startPos); }
        if (c == '<' && ch(1) == '>') { advance(2); return emit(QToken.Type.BINOP, "<>", startLine, startCol, startPos); }
        if (c == '+' && ch(1) == ':') { advance(2); return emit(QToken.Type.BINOP, "+:", startLine, startCol, startPos); }
        if (c == '-' && ch(1) == ':') { advance(2); return emit(QToken.Type.BINOP, "-:", startLine, startCol, startPos); }
        if (c == '*' && ch(1) == ':') { advance(2); return emit(QToken.Type.BINOP, "*:", startLine, startCol, startPos); }
        if (c == '%' && ch(1) == ':') { advance(2); return emit(QToken.Type.BINOP, "%:", startLine, startCol, startPos); }
        if (c == '+' && ch(1) == '+') { advance(2); return emit(QToken.Type.MONOP, "++", startLine, startCol, startPos); }
        if (c == '-' && ch(1) == '-') { advance(2); return emit(QToken.Type.MONOP, "--", startLine, startCol, startPos); }

        // /: \: ': — always adverbs
        if (c == '/' && ch(1) == ':') { advance(2); return emit(QToken.Type.ADVERB, "/:", startLine, startCol, startPos); }
        if (c == '\\' && ch(1) == ':') { advance(2); return emit(QToken.Type.ADVERB, "\\:", startLine, startCol, startPos); }
        if (c == '\'' && ch(1) == ':') { advance(2); return emit(QToken.Type.ADVERB, "':", startLine, startCol, startPos); }

        // / — comment or adverb
        // Rule: space or newline before / = COMMENT, no space = ADVERB (over)
        // Per kx docs: "If a / is meant to denote the left end of a comment
        // then it must be preceded by a blank (or newline)"
        if (c == '/') {
            boolean spaceBefore = startPos == 0 || src.charAt(startPos - 1) == ' '
                || src.charAt(startPos - 1) == '\t' || src.charAt(startPos - 1) == '\n'
                || src.charAt(startPos - 1) == '\r';
            if (!spaceBefore) {
                advance();
                return emit(QToken.Type.ADVERB, "/", startLine, startCol, startPos);
            }
            // Comment: capture text as COMMENT token
            advance(); // skip the /
            StringBuilder commentText = new StringBuilder();
            while (pos < src.length() && ch() != '\n') {
                commentText.append(ch());
                advance();
            }
            return emit(QToken.Type.COMMENT, commentText.toString().trim(), startLine, startCol, startPos);
        }

        // \ — slash command or adverb (same space rule as /)
        if (c == '\\') {
            boolean spaceBefore2 = startPos == 0 || src.charAt(startPos - 1) == ' '
                || src.charAt(startPos - 1) == '\t' || src.charAt(startPos - 1) == '\n'
                || src.charAt(startPos - 1) == '\r';
            if (!spaceBefore2) {
                advance();
                return emit(QToken.Type.ADVERB, "\\", startLine, startCol, startPos);
            }
            // Slash command: gobble rest of line
            return lexSlashCmd(startLine, startCol, startPos);
        }

        // ' — signal or adverb
        if (c == '\'') {
            if (afterExpr()) {
                advance();
                return emit(QToken.Type.ADVERB, "'", startLine, startCol, startPos);
            }
            advance();
            return emit(QToken.Type.ADVERB, "'", startLine, startCol, startPos);
        }

        // // — double-slash comment (only if not after expression)
        // Note: handled by / case above — first / is comment if not after expr,
        // and then the second / is part of the comment text.

        // Single-char structural tokens
        if (c == ':') { advance(); return emit(QToken.Type.COLON, ":", startLine, startCol, startPos); }
        if (c == ';') { advance(); return emit(QToken.Type.SEMI, ";", startLine, startCol, startPos); }
        if (c == ',') { advance(); return emit(QToken.Type.COMMA, ",", startLine, startCol, startPos); }
        if (c == '(') { advance(); bracketDepth++; return emit(QToken.Type.LPAREN, "(", startLine, startCol, startPos); }
        if (c == ')') { advance(); bracketDepth--; return emit(QToken.Type.RPAREN, ")", startLine, startCol, startPos); }
        if (c == '[') { advance(); bracketDepth++; return emit(QToken.Type.LBRACKET, "[", startLine, startCol, startPos); }
        if (c == ']') { advance(); bracketDepth--; return emit(QToken.Type.RBRACKET, "]", startLine, startCol, startPos); }
        if (c == '{') { advance(); bracketDepth++; return emit(QToken.Type.LBRACE, "{", startLine, startCol, startPos); }
        if (c == '}') { advance(); bracketDepth--; return emit(QToken.Type.RBRACE, "}", startLine, startCol, startPos); }

        // Single-char operators
        if (c == '+') { advance(); return emit(QToken.Type.BINOP, "+", startLine, startCol, startPos); }
        if (c == '-') { advance(); return emit(QToken.Type.BINOP, "-", startLine, startCol, startPos); }
        if (c == '*') { advance(); return emit(QToken.Type.BINOP, "*", startLine, startCol, startPos); }
        if (c == '%') { advance(); return emit(QToken.Type.BINOP, "%", startLine, startCol, startPos); }
        if (c == '&') { advance(); return emit(QToken.Type.BINOP, "&", startLine, startCol, startPos); }
        if (c == '|') { advance(); return emit(QToken.Type.BINOP, "|", startLine, startCol, startPos); }
        if (c == '~') { advance(); return emit(QToken.Type.BINOP, "~", startLine, startCol, startPos); }
        if (c == '?') { advance(); return emit(QToken.Type.BINOP, "?", startLine, startCol, startPos); }
        if (c == '=') { advance(); return emit(QToken.Type.BINOP, "=", startLine, startCol, startPos); }
        if (c == '<') { advance(); return emit(QToken.Type.BINOP, "<", startLine, startCol, startPos); }
        if (c == '>') { advance(); return emit(QToken.Type.BINOP, ">", startLine, startCol, startPos); }
        if (c == '^') { advance(); return emit(QToken.Type.BINOP, "^", startLine, startCol, startPos); }
        if (c == '!') { advance(); return emit(QToken.Type.BINOP, "!", startLine, startCol, startPos); }
        if (c == '@') { advance(); return emit(QToken.Type.BINOP, "@", startLine, startCol, startPos); }
        if (c == '$') { advance(); return emit(QToken.Type.BINOP, "$", startLine, startCol, startPos); }
        if (c == '_') { advance(); return emit(QToken.Type.BINOP, "_", startLine, startCol, startPos); }
        if (c == '#') { advance(); return emit(QToken.Type.BINOP, "#", startLine, startCol, startPos); }
        if (c == '.' && !isLetter(ch(1))) { advance(); return emit(QToken.Type.BINOP, ".", startLine, startCol, startPos); }

        // Identifier or keyword (including .namespace.name starting with dot)
        if (isLetter(c) || c == '.') {
            return lexIdentifier(startLine, startCol, startPos);
        }

        // Unknown character — skip
        advance();
        return next();
    }

    // ---- Token emitters ----

    private QToken emit(QToken.Type type, String text) {
        return emit(type, text, line, col, pos);
    }

    private QToken emit(QToken.Type type, String text, int l, int c, int p) {
        prevType = type;
        return new QToken(type, text, l, c, p);
    }

    // ---- Whitespace and comments ----

    private void skipWhitespace() {
        while (pos < src.length()) {
            char c = ch();
            if (c == ' ' || c == '\t') { advance(); }
            else break;
        }
    }

    private void skipToEol() {
        while (pos < src.length() && ch() != '\n') advance();
    }

    // ---- Lexer methods ----

    private QToken lexNewline(int sl, int sc, int sp) {
        // Multiline continuation: indented lines continue the previous statement
        // Only at top level — inside {([...])} the parser handles multiline already
        // Use loop (not recursion) to avoid stack overflow on large inputs
        while (true) {
            while (pos < src.length() && (ch() == '\n' || ch() == '\r')) advance();
            if (bracketDepth <= 0 && pos < src.length() && (ch() == ' ' || ch() == '\t')) {
                skipWhitespace();
                // If we hit another newline, continue the loop
                if (pos < src.length() && (ch() == '\n' || ch() == '\r')) continue;
                // Otherwise return the next real token
                return next();
            }
            break;
        }
        prevType = QToken.Type.NEWLINE;
        return new QToken(QToken.Type.NEWLINE, "\n", sl, sc, sp);
    }

    private QToken lexString(int sl, int sc, int sp) {
        advance(); // skip opening "
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        while (!atEnd() && ch() != '"') {
            if (ch() == '\\') {
                sb.append(ch()); advance();
                if (!atEnd()) { sb.append(ch()); advance(); }
            } else {
                sb.append(ch()); advance();
            }
        }
        if (!atEnd() && ch() == '"') { sb.append('"'); advance(); } // closing "
        else { return emit(QToken.Type.ERROR, sb.toString(), sl, sc, sp); } // unterminated string
        String text = sb.toString();
        QToken.Type type = text.length() == 3 && text.charAt(1) != '\\' ? QToken.Type.CHAR : QToken.Type.STRING;
        return emit(type, text, sl, sc, sp);
    }

    private QToken lexSymbol(int sl, int sc, int sp) {
        StringBuilder sb = new StringBuilder();
        sb.append('`'); advance();
        // `: starts a file handle symbol
        if (ch() == ':') { sb.append(':'); advance(); }
        while (!atEnd() && (isLetter(ch()) || isDigit(ch()) || ch() == '.' || ch() == '_' || ch() == ':' || ch() == '/')) {
            sb.append(ch()); advance();
        }
        return emit(QToken.Type.SYMBOL, sb.toString(), sl, sc, sp);
    }

    private QToken lexByte(int sl, int sc, int sp) {
        StringBuilder sb = new StringBuilder();
        sb.append('0'); advance(); // 0
        sb.append('x'); advance(); // x
        while (!atEnd() && isHexDigit(ch())) { sb.append(ch()); advance(); }
        return emit(QToken.Type.BYTE, sb.toString(), sl, sc, sp);
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Gobble an atom or atom list.
     * An atom is: [-]? [0-9.:DTe+-]+ [type-suffix]?
     * An atom list is: atom (' ' atom)+ [type-suffix]?
     */
    private QToken lexAtom(int sl, int sc, int sp) {
        StringBuilder sb = new StringBuilder();

        // Gobble first atom
        gobbleOneAtom(sb);

        // Check for atom list: space followed by another atom-starting char
        while (!atEnd() && ch() == ' ' && looksLikeAtom(pos + 1)) {
            sb.append(' '); advance(); // consume space
            gobbleOneAtom(sb);
        }

        // Trailing type suffix (applies to whole list)
        String text = sb.toString();
        char lastChar = text.charAt(text.length() - 1);

        return emit(classifyAtom(text), text, sl, sc, sp);
    }

    private void gobbleOneAtom(StringBuilder sb) {
        // Optional leading -
        if (ch() == '-') { sb.append('-'); advance(); }
        // Digits, dots, colons, D, T, e/E with optional +/- after
        while (!atEnd()) {
            char c = ch();
            if (isDigit(c) || c == '.') {
                sb.append(c); advance();
            } else if (c == ':' && isDigit(ch(1))) {
                // Colon in time: 12:30
                sb.append(c); advance();
            } else if ((c == 'D' || c == 'T') && (isDigit(ch(1)) || ch(1) == 0)) {
                // Date-time separator
                sb.append(c); advance();
            } else if ((c == 'e' || c == 'E') && (isDigit(ch(1)) || ch(1) == '+' || ch(1) == '-')) {
                // Exponent
                sb.append(c); advance();
                if (ch() == '+' || ch() == '-') { sb.append(ch()); advance(); }
            } else if (isTypeSuffix(c) && !isLetter(ch(1))) {
                // Type suffix at end of atom (not followed by more letters = not an ID)
                sb.append(c); advance();
                break;
            } else {
                break;
            }
        }
    }

    /** Peek ahead to see if position p starts something that looks like an atom */
    private boolean looksLikeAtom(int p) {
        if (p >= src.length()) return false;
        char c = src.charAt(p);
        if (isDigit(c)) return true;
        if (c == '-' && p + 1 < src.length() && isDigit(src.charAt(p + 1))) return true;
        if (c == '.' && p + 1 < src.length() && isDigit(src.charAt(p + 1))) return true;
        // Special atoms: 0N, 0W, 0n, 0w
        if (c == '0' && p + 1 < src.length() && "NWnw".indexOf(src.charAt(p + 1)) >= 0) return true;
        return false;
    }

    /** Classify an atom/atom-list by its content and suffix */
    private QToken.Type classifyAtom(String text) {
        // Special null/infinity atoms: 0N, 0W (long), 0n, 0w (float), -0W, -0w, 0Ni, 0Wi, etc.
        String base = text.startsWith("-") ? text.substring(1) : text;
        if (base.length() >= 2 && base.charAt(0) == '0' && "NWnw".indexOf(base.charAt(1)) >= 0) {
            if (base.length() == 2) {
                char sc = base.charAt(1);
                if (sc == 'n' || sc == 'w') return QToken.Type.FLOAT;
                return QToken.Type.LONG; // 0N, 0W
            }
            // Has type suffix: 0Ni, 0Wi, etc.
            if (base.length() == 3) {
                char suffix = base.charAt(2);
                switch (suffix) {
                    case 'h': return QToken.Type.SHORT;
                    case 'i': return QToken.Type.INT;
                    case 'j': return QToken.Type.LONG;
                    case 'e': return QToken.Type.REAL;
                    case 'f': return QToken.Type.FLOAT;
                    case 'p': return QToken.Type.TIMESTAMP;
                    case 'n': return QToken.Type.TIMESPAN;
                    case 't': return QToken.Type.TIME;
                    case 'd': return QToken.Type.DATE;
                    case 'm': return QToken.Type.MONTH;
                    case 'u': return QToken.Type.MINUTE;
                    case 'v': return QToken.Type.SECOND;
                    case 'z': return QToken.Type.DATETIME;
                    case 'g': return QToken.Type.SYMBOL; // GUID null/infinity
                    case 'c': return QToken.Type.CHAR;
                    case 'x': return QToken.Type.BYTE;
                    case 'b': return QToken.Type.BOOL;
                }
            }
        }

        // Check for bool: 0b, 1b, 101b etc.
        if (text.endsWith("b") && text.length() >= 2) {
            String body = text.substring(0, text.length() - 1);
            if (body.chars().allMatch(c -> c == '0' || c == '1')) return QToken.Type.BOOL;
        }

        char last = text.charAt(text.length() - 1);
        boolean isList = text.contains(" ");

        // Explicit type suffix
        switch (last) {
            case 'h': return isList ? QToken.Type.SHORT : QToken.Type.SHORT;
            case 'i': return QToken.Type.INT;
            case 'j': return QToken.Type.LONG;
            case 'e': return QToken.Type.REAL;
            case 'f': return QToken.Type.FLOAT;
            case 'm': return QToken.Type.MONTH;
            case 'u': return QToken.Type.MINUTE;
            case 'v': return QToken.Type.SECOND;
            case 'n': return QToken.Type.TIMESPAN;
            case 'p': return QToken.Type.TIMESTAMP;
            case 't': return QToken.Type.TIME;
            case 'd': return QToken.Type.DATE;
            case 'z': return QToken.Type.DATETIME;
        }

        // Infer from content
        String first = isList ? text.split(" ")[0] : text;
        if (first.contains(":")) return QToken.Type.TIME;   // 12:30:00
        // Date pattern: YYYY.MM.DD (4 digits, dot, 2 digits, dot, 2 digits)
        if (first.matches("-?\\d{4}\\.\\d{2}\\.\\d{2}.*")) return QToken.Type.DATE;
        // Month: YYYY.MM
        if (first.matches("-?\\d{4}\\.\\d{2}")) return QToken.Type.MONTH;
        // Float: has decimal point
        if (first.contains(".")) return QToken.Type.FLOAT;
        // Default: long
        return QToken.Type.LONG;
    }

    private QToken lexIdentifier(int sl, int sc, int sp) {
        StringBuilder sb = new StringBuilder();
        // ID can start with letter or dot
        while (!atEnd() && isIdChar(ch())) {
            sb.append(ch()); advance();
        }
        String text = sb.toString();

        // Classify
        if (KEYWORDS.contains(text)) return emit(QToken.Type.KEYWORD, text, sl, sc, sp);
        if (MONOPS.contains(text)) return emit(QToken.Type.MONOP, text, sl, sc, sp);
        if (BINOPS_WORD.contains(text)) return emit(QToken.Type.BINOP, text, sl, sc, sp);
        return emit(QToken.Type.ID, text, sl, sc, sp);
    }

    private QToken lexSlashCmd(int sl, int sc, int sp) {
        advance(); // skip backslash
        StringBuilder sb = new StringBuilder();
        while (!atEnd() && ch() != '\n') { sb.append(ch()); advance(); }
        return emit(QToken.Type.SLASH_CMD, sb.toString().trim(), sl, sc, sp);
    }
}
