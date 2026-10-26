package com.timestored.qdoc.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Pratt parser for q/kdb+. Uses hand-written QLexer for tokenization.
 * No backtracking — O(n) parsing. Handles context-sensitive q syntax explicitly.
 */
public class QParser {
    private final List<QToken> tokens;
    private final List<ParseError> errors = new ArrayList<>();
    private int pos = 0;
    private int nestDepth = 0;
    private int exprDepth = 0;
    private static final int MAX_EXPR_DEPTH = 200;
    private boolean debug = false;
    private boolean inQSql = false; // when true, from/by/where stop expressions

    public void setDebug(boolean d) { this.debug = d; }

    public QParser(List<QToken> tokens) {
        this.tokens = tokens;
    }

    // ---- Public API ----

    public static QNode parse(String code) {
        List<QToken> tokens = QLexer.tokenize(code);
        QParser parser = new QParser(tokens);
        return parser.parseFile();
    }

    public static List<ParseError> parseAndCollectErrors(String code) {
        return parseAndCollectErrors(code, 5000);
    }

    public static List<ParseError> parseAndCollectErrors(String code, long timeoutMs) {
        try {
            List<QToken> tokens = QLexer.tokenize(code);
            QParser parser = new QParser(tokens);
            parser.setTimeout(timeoutMs);
            parser.parseFile();
            if (parser.timedOut()) parser.errors.add(new ParseError(parser.peek(), "parse timeout"));
            return parser.errors;
        } catch (StackOverflowError e) {
            List<ParseError> errors = new ArrayList<>();
            errors.add(new ParseError(-1, 0, 0, "stack overflow during parse"));
            return errors;
        }
    }

    public List<ParseError> getErrors() { return errors; }

    // ---- Error reporting ----

    private void error(String msg) {
        QToken t = peek();
        errors.add(new ParseError(t, msg));
    }

    // ---- Token navigation ----

    private QToken peek() { return pos < tokens.size() ? tokens.get(pos) : tokens.get(tokens.size() - 1); }
    private QToken.Type peekType() { return peek().type; }
    private String peekText() { return peek().text; }

    private QToken advance() {
        QToken t = peek();
        if (pos < tokens.size() - 1) pos++;
        return t;
    }

    private boolean check(QToken.Type type) { return peekType() == type; }
    private boolean checkText(String text) { return text.equals(peekText()); }
    private boolean atEnd() { return peekType() == QToken.Type.EOF; }

    private QToken expectText(String text) {
        if (checkText(text)) return advance();
        error("expected '" + text + "' but found '" + peekText() + "'");
        return peek();
    }

    private boolean matchText(String text) {
        if (checkText(text)) { advance(); return true; }
        return false;
    }

    // ---- Boundary detection ----

    private boolean atStatEnd() {
        if (atEnd()) return true;
        if (check(QToken.Type.NEWLINE) && nestDepth == 0) return true;
        if (check(QToken.Type.NEWLINE)) return false;
        QToken.Type t = peekType();
        return t == QToken.Type.SEMI || t == QToken.Type.RBRACE
            || t == QToken.Type.RBRACKET || t == QToken.Type.RPAREN;
    }

    // ---- Binding powers ----

    private static final int BP_NONE = 0;
    private static final int BP_EXPR = 10;
    private static final int BP_ADVERB = 20;
    private static final int BP_BRACKET = 30;

    private int getLbp() {
        if (atEnd()) return BP_NONE;
        QToken.Type type = peekType();
        if (type == QToken.Type.NEWLINE || type == QToken.Type.COMMENT) return BP_NONE;
        if (type == QToken.Type.SEMI || type == QToken.Type.RBRACE
            || type == QToken.Type.RPAREN || type == QToken.Type.RBRACKET) return BP_NONE;
        if (type == QToken.Type.LBRACKET) return BP_BRACKET;
        if (type == QToken.Type.ADVERB) return BP_ADVERB;
        if (inQSql && nestDepth == 0 && type == QToken.Type.COMMA) return BP_NONE; // comma separates q-SQL columns
        if (type == QToken.Type.BINOP || type == QToken.Type.COLON || type == QToken.Type.COMMA) return BP_EXPR;
        if (canStartExpr()) return BP_EXPR;
        return BP_NONE;
    }

    private boolean canStartExpr() {
        if (atEnd()) return false;
        QToken.Type t = peekType();
        if (inQSql && t == QToken.Type.KEYWORD && isQSqlClause(peekText())) return false;
        return isLiteral(t) || t == QToken.Type.ID || t == QToken.Type.MONOP
            || t == QToken.Type.BINOP || t == QToken.Type.ADVERB || t == QToken.Type.COMMA
            || t == QToken.Type.LPAREN || t == QToken.Type.LBRACE || t == QToken.Type.COLON
            || t == QToken.Type.KEYWORD || t == QToken.Type.SYMBOL;
    }

    private boolean isLiteral(QToken.Type t) {
        return t == QToken.Type.LONG || t == QToken.Type.INT || t == QToken.Type.SHORT
            || t == QToken.Type.FLOAT || t == QToken.Type.REAL
            || t == QToken.Type.BOOL || t == QToken.Type.DATE || t == QToken.Type.TIME
            || t == QToken.Type.MONTH || t == QToken.Type.MINUTE || t == QToken.Type.SECOND
            || t == QToken.Type.TIMESPAN || t == QToken.Type.TIMESTAMP || t == QToken.Type.DATETIME
            || t == QToken.Type.BYTE || t == QToken.Type.STRING || t == QToken.Type.CHAR;
    }

    private boolean isQSqlKeyword(String text) {
        return "select".equals(text) || "update".equals(text)
            || "delete".equals(text) || "exec".equals(text);
    }

    private boolean isQSqlClause(String text) {
        return "from".equals(text) || "by".equals(text) || "where".equals(text);
    }

    /** In q-SQL, comma separates columns. Don't let it be a binary op. */
    private boolean isQSqlCommaStop() {
        return inQSql && check(QToken.Type.COMMA);
    }

    private boolean isControlKeyword(String text) {
        return "$".equals(text) || "if".equals(text) || "do".equals(text) || "while".equals(text);
    }

    // ---- Timeout ----

    private long parseDeadline = Long.MAX_VALUE;
    public void setTimeout(long ms) { this.parseDeadline = System.currentTimeMillis() + ms; }
    private boolean timedOut() { return System.currentTimeMillis() > parseDeadline; }

    /** Skip tokens until a safe sync point for error recovery.
     *  Tracks bracket depth so we don't stop inside nested brackets. */
    private void skipToSync() {
        int depth = 0;
        while (!atEnd()) {
            QToken.Type t = peekType();
            if (t == QToken.Type.LPAREN || t == QToken.Type.LBRACKET || t == QToken.Type.LBRACE) {
                depth++;
                advance();
                continue;
            }
            if (t == QToken.Type.RPAREN || t == QToken.Type.RBRACKET || t == QToken.Type.RBRACE) {
                if (depth > 0) { depth--; advance(); continue; }
                return; // don't consume — let caller's expectText() handle it
            }
            if (depth == 0) {
                if (t == QToken.Type.SEMI) { advance(); return; }
                if (t == QToken.Type.NEWLINE && nestDepth == 0) { advance(); return; }
            }
            if (t == QToken.Type.EOF) return;
            advance();
        }
    }

    // ---- Top-level parsing ----

    public QNode parseFile() {
        List<QNode> stmts = new ArrayList<>();
        int lastPos = -1;
        while (!atEnd() && !timedOut()) {
            if (pos == lastPos) { advance(); continue; }
            lastPos = pos;
            skipSeparators();
            if (atEnd()) break;
            if (check(QToken.Type.RPAREN) || check(QToken.Type.RBRACKET) || check(QToken.Type.RBRACE)) {
                error("unexpected '" + peekText() + "'");
                advance();
                continue;
            }
            if (check(QToken.Type.SLASH_CMD)) {
                stmts.add(QNode.leaf(QNode.Type.SLASH, peek(), peekText()));
                advance();
                continue;
            }
            try {
                QNode expr = parseExpr(BP_NONE);
                if (expr != null && expr.type != QNode.Type.EMPTY) stmts.add(expr);
            } catch (Exception e) {
                error("parse error: " + e.getMessage());
                skipToSync();
            }
            skipSeparators();
        }
        if (stmts.size() == 1) return stmts.get(0);
        return QNode.node(QNode.Type.STAT_LIST, tokens.isEmpty() ? null : tokens.get(0), null, stmts);
    }

    private void skipSeparators() {
        while (!atEnd() && (check(QToken.Type.NEWLINE) || check(QToken.Type.SEMI) || check(QToken.Type.COMMENT))) advance();
    }

    // ---- Pratt expression parser ----

    private QNode parseExpr(int rbp) {
        if (++exprDepth > MAX_EXPR_DEPTH) {
            exprDepth--;
            error("expression nesting too deep");
            return QNode.leaf(QNode.Type.EMPTY, null, "");
        }
        try {
        if (debug && exprDepth <= 20) {
            String indent = "  ".repeat(exprDepth);
            System.out.println(indent + "parseExpr(rbp=" + rbp + " depth=" + exprDepth + " pos=" + pos + " tok=" + peekType() + ":" + peekText().substring(0, Math.min(20, peekText().length())) + ")");
        }
        while (nestDepth > 0 && (check(QToken.Type.NEWLINE) || check(QToken.Type.COMMENT))) advance();
        while (check(QToken.Type.COMMENT)) advance(); // skip comments at any nesting level
        if (atEnd() || atStatEnd()) return QNode.leaf(QNode.Type.EMPTY, null, "");

        QToken t = advance();
        QNode left = nud(t);

        int guard = 0;
        while (!atEnd() && !timedOut()) {
            if (nestDepth > 0 && (check(QToken.Type.NEWLINE) || check(QToken.Type.COMMENT))) { advance(); continue; }
            if (check(QToken.Type.COMMENT)) { advance(); continue; }
            if (rbp >= getLbp()) break;
            int beforePos = pos;
            left = led(left);
            if (pos == beforePos) { error("stuck at '" + peekText() + "'"); advance(); break; }
            if (++guard > 10000) { error("expression too complex"); break; }
        }
        return left;
        } finally { exprDepth--; }
    }

    // ---- nud: prefix / atom ----

    private QNode nud(QToken t) {
        QToken.Type type = t.type;
        String text = t.text;

        // Literals
        if (isLiteral(type)) return QNode.leaf(QNode.Type.LITERAL, t, text);

        // Symbol — check for symbol list: `a`b`c
        if (type == QToken.Type.SYMBOL) {
            List<String> syms = new ArrayList<>();
            syms.add(text);
            while (check(QToken.Type.SYMBOL)) { syms.add(peekText()); advance(); }
            return QNode.leaf(QNode.Type.LITERAL, t, String.join("", syms));
        }

        // Comma (enlist)
        if (type == QToken.Type.COMMA) return QNode.leaf(QNode.Type.OPERATOR, t, ",");

        // ID
        if (type == QToken.Type.ID) {
            if (check(QToken.Type.COLON)) return tryAssignment(t);
            return QNode.leaf(QNode.Type.ID, t, text);
        }

        // Keyword used as expression (select/update/delete/exec)
        if (type == QToken.Type.KEYWORD) {
            if (isQSqlKeyword(text)) return parseQuery(t);
            if (check(QToken.Type.COLON)) return tryAssignment(t);
            return QNode.leaf(QNode.Type.ID, t, text);
        }

        // Monadic operator
        if (type == QToken.Type.MONOP) {
            if (check(QToken.Type.COLON)) return tryAssignment(t);
            return QNode.leaf(QNode.Type.OPERATOR, t, text);
        }

        // Binary operator as value
        if (type == QToken.Type.BINOP) {
            if (check(QToken.Type.COLON)) return tryAssignment(t);
            return QNode.leaf(QNode.Type.OPERATOR, t, text);
        }

        // Colon: return operator
        if (type == QToken.Type.COLON) {
            if (!atStatEnd() && canStartExpr()) {
                QNode val = parseExpr(BP_NONE);
                return QNode.node(QNode.Type.OPERATOR, t, ":", listOf(val));
            }
            return QNode.leaf(QNode.Type.OPERATOR, t, ":");
        }

        // Adverb as value (' for signal, etc.)
        if (type == QToken.Type.ADVERB) return QNode.leaf(QNode.Type.OPERATOR, t, text);

        // ( — parens, table, or empty list
        if (type == QToken.Type.LPAREN) return parseParensOrTable(t);

        // { — function
        if (type == QToken.Type.LBRACE) return parseFunction(t);

        // [ — block expression
        if (type == QToken.Type.LBRACKET) {
            nestDepth++;
            try {
                List<QNode> exprs = parseMultiExpr();
                expectText("]");
                return QNode.node(QNode.Type.PARENS, t, null, exprs);
            } finally { nestDepth--; }
        }

        // Slash command
        if (type == QToken.Type.SLASH_CMD) return QNode.leaf(QNode.Type.SLASH, t, text);

        error("unexpected token '" + text + "'");
        return QNode.leaf(QNode.Type.LITERAL, t, text);
    }

    // ---- led: infix / postfix ----

    private QNode led(QNode left) {
        QToken t = peek();
        QToken.Type type = t.type;
        String text = t.text;

        // [ — bracket apply or control structure
        if (type == QToken.Type.LBRACKET) {
            advance();
            return parseBracketApply(left, t);
        }

        // Adverb — postfix
        if (type == QToken.Type.ADVERB) {
            advance();
            return QNode.node(QNode.Type.ADVERB, t, text, listOf(left));
        }

        // Binary operator — collect chain and fold right-to-left (avoids deep recursion)
        if (type == QToken.Type.BINOP) {
            return parseBinaryChain(left);
        }

        // : as assignment or binary
        if (type == QToken.Type.COLON) {
            if (left.type == QNode.Type.ID || left.type == QNode.Type.OPERATOR) {
                advance();
                QNode val = parseExpr(BP_NONE);
                return QNode.node(QNode.Type.ASSIGN, t, left.text, listOf(left, val));
            }
            return parseBinaryChain(left);
        }

        // Comma as binary
        if (type == QToken.Type.COMMA) {
            return parseBinaryChain(left);
        }

        // Juxtaposition — function application (non-recursive to avoid stack overflow)
        if (canStartExpr()) {
            QToken rt = advance();
            QNode right = nud(rt);
            // After nud, consume any adverbs/brackets that bind tighter than juxtaposition
            while (!atEnd() && !timedOut()) {
                if (nestDepth > 0 && (check(QToken.Type.NEWLINE) || check(QToken.Type.COMMENT))) { advance(); continue; }
                int lbp = getLbp();
                if (lbp <= BP_EXPR) break; // only consume adverbs (BP_ADVERB) and brackets (BP_BRACKET)
                int bp = pos;
                right = led(right);
                if (pos == bp) break;
            }
            return QNode.node(QNode.Type.APPLY, rt, null, listOf(left, right));
        }

        error("unexpected token in expression: '" + text + "'");
        advance();
        return left;
    }

    /**
     * Parse a chain of binary operations iteratively, then fold right-to-left.
     * a+b*c-d → collect [a,+,b,*,c,-,d] → fold → (+ a (* b (- c d)))
     * This avoids deep recursion for long expression chains.
     */
    private QNode parseBinaryChain(QNode firstOperand) {
        List<QNode> operands = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        operands.add(firstOperand);

        while (!atEnd() && !timedOut()) {
            QToken t = peek();
            QToken.Type type = t.type;
            if (type != QToken.Type.BINOP && type != QToken.Type.COLON && type != QToken.Type.COMMA) break;
            if (inQSql && nestDepth == 0 && type == QToken.Type.COMMA) break; // comma separates q-SQL columns/conditions
            // Don't consume : if it would be assignment (left is ID/OPERATOR)
            if (type == QToken.Type.COLON && operands.size() == 1
                && (firstOperand.type == QNode.Type.ID || firstOperand.type == QNode.Type.OPERATOR)) break;

            operators.add(t.text);
            advance(); // consume operator

            // Parse right operand — just the immediate atom/prefix, not a full expr
            while (nestDepth > 0 && (check(QToken.Type.NEWLINE) || check(QToken.Type.COMMENT))) advance();
            while (check(QToken.Type.COMMENT)) advance();
            if (atEnd() || atStatEnd()) {
                // Operator at end of expression — monadic use
                operands.add(QNode.leaf(QNode.Type.EMPTY, null, ""));
                break;
            }
            QToken rt = advance();
            QNode right = nud(rt);

            // After nud, check for adverbs/brackets that bind tighter
            while (!atEnd() && !timedOut()) {
                if (nestDepth > 0 && (check(QToken.Type.NEWLINE) || check(QToken.Type.COMMENT))) { advance(); continue; }
                int lbp = getLbp();
                if (lbp <= BP_EXPR) break; // only consume adverbs and brackets
                int beforePos = pos;
                right = led(right);
                if (pos == beforePos) break;
            }

            operands.add(right);
        }

        // Fold right-to-left: [a,+,b,*,c] → (+ a (* b c))
        QNode result = operands.get(operands.size() - 1);
        for (int i = operators.size() - 1; i >= 0; i--) {
            result = QNode.node(QNode.Type.BINARY, null, operators.get(i),
                listOf(operands.get(i), result));
        }
        return result;
    }

    // ---- Bracket apply / control structures ----

    private QNode parseBracketApply(QNode left, QToken bracket) {
        nestDepth++;
        boolean savedInQSql = inQSql;
        inQSql = false; // bracket contents are not q-SQL context
        try {
            String leftText = left.text;
            if (left.type == QNode.Type.OPERATOR && "$".equals(leftText)) {
                List<QNode> args = parseMultiExpr();
                expectText("]");
                List<QNode> all = new ArrayList<>(); all.add(left); all.addAll(args);
                return QNode.node(QNode.Type.COND, bracket, "$", all);
            }
            if (left.type == QNode.Type.ID && isControlKeyword(leftText)) {
                List<QNode> args = parseMultiExpr();
                expectText("]");
                List<QNode> all = new ArrayList<>(); all.add(left); all.addAll(args);
                return QNode.node(QNode.Type.CONTROL, bracket, leftText, all);
            }
            List<QNode> args = parseMultiExpr();
            expectText("]");
            if (check(QToken.Type.COLON)) {
                advance();
                QNode val = parseExpr(BP_NONE);
                List<QNode> children = new ArrayList<>();
                children.add(left); children.addAll(args); children.add(val);
                return QNode.node(QNode.Type.ASSIGN, bracket, null, children);
            }
            List<QNode> children = new ArrayList<>();
            children.add(left); children.addAll(args);
            return QNode.node(QNode.Type.APPLY, bracket, null, children);
        } finally { nestDepth--; inQSql = savedInQSql; }
    }

    // ---- Assignment ----

    private QNode tryAssignment(QToken name) {
        if (check(QToken.Type.COLON)) {
            advance();
            QNode val = parseExpr(BP_NONE);
            return QNode.node(QNode.Type.ASSIGN, name, name.text,
                listOf(QNode.leaf(QNode.Type.ID, name, name.text), val));
        }
        return QNode.leaf(QNode.Type.ID, name, name.text);
    }

    // ---- Parens and Tables ----

    private QNode parseParensOrTable(QToken openParen) {
        nestDepth++;
        boolean savedInQSql = inQSql;
        inQSql = false; // nested parens/tables are not q-SQL context
        try {
            if (check(QToken.Type.RPAREN)) { advance(); return QNode.leaf(QNode.Type.LITERAL, openParen, "()"); }
            if (check(QToken.Type.LBRACKET)) {
                advance();
                List<QNode> keycols = parseMultiExpr();
                expectText("]");
                if (check(QToken.Type.RPAREN)) {
                    advance();
                    return QNode.node(QNode.Type.TABLE, openParen, null, keycols);
                }
                List<QNode> tcols = parseMultiExpr();
                expectText(")");
                List<QNode> children = new ArrayList<>();
                children.addAll(keycols); children.addAll(tcols);
                return QNode.node(QNode.Type.TABLE, openParen, null, children);
            }
            List<QNode> exprs = parseMultiExpr();
            expectText(")");
            return QNode.node(QNode.Type.PARENS, openParen, null, exprs);
        } finally { nestDepth--; inQSql = savedInQSql; }
    }

    // ---- Function ----

    private QNode parseFunction(QToken openBrace) {
        boolean savedInQSql = inQSql;
        inQSql = false; // lambda bodies are not q-SQL context
        List<QNode> params = new ArrayList<>();
        boolean hasExplicitParams = false;
        if (check(QToken.Type.LBRACKET)) {
            hasExplicitParams = true;
            advance();
            int lastParamPos = -1;
            while (!checkText("]") && !atEnd() && !timedOut()) {
                if (pos == lastParamPos) { advance(); continue; }
                lastParamPos = pos;
                if (check(QToken.Type.NEWLINE) || check(QToken.Type.COMMENT)) { advance(); continue; }
                if (check(QToken.Type.ID)) {
                    QToken paramToken = peek();
                    String paramName = peekText();
                    advance();
                    // Type annotation: x:`j — consume the : and symbol, keep only the param name
                    if (check(QToken.Type.COLON) && pos + 1 < tokens.size()
                            && tokens.get(pos + 1).type == QToken.Type.SYMBOL) {
                        advance(); // skip :
                        advance(); // skip `j
                    }
                    params.add(QNode.leaf(QNode.Type.ID, paramToken, paramName));
                } else if (check(QToken.Type.LPAREN)) {
                    // Destructuring param: (a;b) — parse as paren expression, count as one param
                    QNode parenParam = parseExpr(BP_NONE);
                    if (parenParam != null) params.add(parenParam);
                }
                if (check(QToken.Type.SEMI)) advance();
            }
            expectText("]");
        }
        List<QNode> body = new ArrayList<>();
        int lastBodyPos = -1;
        while (!check(QToken.Type.RBRACE) && !atEnd() && !timedOut()) {
            if (pos == lastBodyPos) { advance(); continue; } // guarantee progress
            lastBodyPos = pos;
            skipSeparators();
            if (check(QToken.Type.RBRACE) || atEnd()) break;
            if (check(QToken.Type.SLASH_CMD)) {
                body.add(QNode.leaf(QNode.Type.SLASH, peek(), peekText()));
                advance(); continue;
            }
            try {
                QNode stmt = parseExpr(BP_NONE);
                if (stmt != null && stmt.type != QNode.Type.EMPTY) body.add(stmt);
            } catch (Exception e) { error("error in function body: " + e.getMessage()); skipToSync(); }
            skipSeparators();
        }
        expectText("}");
        inQSql = savedInQSql; // restore q-SQL context
        List<QNode> children = new ArrayList<>();
        children.addAll(params); children.addAll(body);
        return QNode.node(QNode.Type.FUNCTION, openBrace, hasExplicitParams ? "explicit:" + params.size() : "implicit", children);
    }

    // ---- q-SQL query ----

    /**
     * Parse q-SQL: select/exec/update/delete
     * Output format matches kdb functional form:
     *   select → (?;t;where;by;cols)
     *   exec   → (?;t;where;by;cols)  where by=() means exec
     *   update → (!;t;where;0b;cols)
     *   delete → (!;t;where;0b;cols)
     */
    private QNode parseQuery(QToken keyword) {
        boolean savedInQSql = inQSql;
        inQSql = true;
        try {
            String kw = keyword.text;
            boolean isSelectOrExec = kw.equals("select") || kw.equals("exec");
            String op = isSelectOrExec ? "?" : "!";

            // Optional limit [n] or [n;<col] (select only)
            List<QNode> limitParts = new ArrayList<>();
            if (kw.equals("select") && check(QToken.Type.LBRACKET)) {
                advance();
                nestDepth++;
                try {
                    limitParts.add(parseExpr(BP_NONE));
                    while (check(QToken.Type.SEMI)) {
                        advance();
                        limitParts.add(parseExpr(BP_NONE));
                    }
                } finally { nestDepth--; }
                expectText("]");
            }

            // Optional "distinct" (select or exec)
            boolean distinct = false;
            if ((kw.equals("exec") || kw.equals("select")) && checkText("distinct")) {
                advance();
                distinct = true;
            }

            // Parse select/update columns (comma-separated before 'by' or 'from')
            List<QNode> selectCols = new ArrayList<>();
            int lastSelPos = -1;
            while (!atEnd() && !timedOut() && !checkText("from") && !checkText("by")
                   && !check(QToken.Type.NEWLINE) && !check(QToken.Type.SEMI)) {
                if (pos == lastSelPos) { advance(); break; }
                lastSelPos = pos;
                selectCols.add(parseExpr(BP_NONE));
                if (check(QToken.Type.COMMA)) advance();
                else break;
            }

            // Optional 'by' clause
            List<QNode> byCols = new ArrayList<>();
            if (checkText("by")) {
                advance();
                int lastByPos = -1;
                while (!atEnd() && !timedOut() && !checkText("from")
                       && !check(QToken.Type.NEWLINE) && !check(QToken.Type.SEMI)) {
                    if (pos == lastByPos) { advance(); break; }
                    lastByPos = pos;
                    byCols.add(parseExpr(BP_NONE));
                    if (check(QToken.Type.COMMA)) advance();
                    else break;
                }
            }

            // Required 'from' clause
            QNode table = QNode.leaf(QNode.Type.EMPTY, null, "");
            if (matchText("from")) {
                table = parseExpr(BP_NONE);
            }

            // Optional 'where' clause
            List<QNode> whereCols = new ArrayList<>();
            if (checkText("where")) {
                advance();
                int lastWherePos = -1;
                while (!atStatEnd() && !timedOut()) {
                    if (pos == lastWherePos) { advance(); break; }
                    lastWherePos = pos;
                    whereCols.add(parseExpr(BP_NONE));
                    if (check(QToken.Type.COMMA)) advance();
                    else break;
                }
            }

            // Build functional form: (op; table; where; by; cols)
            List<QNode> result = new ArrayList<>();
            result.add(QNode.leaf(QNode.Type.OPERATOR, null, op));
            result.add(table);

            // Where: () or conditions
            if (whereCols.isEmpty()) {
                result.add(QNode.leaf(QNode.Type.LITERAL, null, "()"));
            } else {
                result.add(QNode.node(QNode.Type.PARENS, null, null, whereCols));
            }

            // By: 0b (select/update/delete), () (exec), 1b (distinct), or expressions
            if (distinct) {
                result.add(QNode.leaf(QNode.Type.LITERAL, null, "1b"));
            } else if (byCols.isEmpty()) {
                result.add(QNode.leaf(QNode.Type.LITERAL, null, kw.equals("exec") ? "()" : "0b"));
            } else {
                result.add(QNode.node(QNode.Type.PARENS, null, null, byCols));
            }

            // Cols: () for select-all, or list of col expressions
            if (selectCols.isEmpty()) {
                result.add(QNode.leaf(QNode.Type.LITERAL, null, "()"));
            } else {
                result.add(QNode.node(QNode.Type.PARENS, null, null, selectCols));
            }

            // Optional limit parts
            for (QNode lp : limitParts) {
                result.add(lp);
            }

            return QNode.node(QNode.Type.QUERY, keyword, kw, result);
        } finally {
            inQSql = savedInQSql;
        }
    }

    // ---- Multi-expression (semicolon-separated) ----

    private List<QNode> parseMultiExpr() {
        List<QNode> exprs = new ArrayList<>();
        if (check(QToken.Type.RBRACKET) || check(QToken.Type.RPAREN)) return exprs;
        if (check(QToken.Type.SEMI)) {
            exprs.add(QNode.leaf(QNode.Type.PROJECTION, null, ""));
        } else {
            try {
                exprs.add(parseExpr(BP_NONE));
            } catch (Exception e) {
                error("parse error: " + e.getMessage());
                skipToSync();
                exprs.add(QNode.leaf(QNode.Type.EMPTY, null, ""));
            }
        }
        int guard = 0;
        while (check(QToken.Type.SEMI) || check(QToken.Type.NEWLINE) || check(QToken.Type.COMMENT)) {
            if (++guard > 10000) break;
            int beforePos = pos;
            boolean wasSemi = check(QToken.Type.SEMI);
            advance();
            while (check(QToken.Type.NEWLINE) || check(QToken.Type.COMMENT)) advance();
            if (check(QToken.Type.RBRACKET) || check(QToken.Type.RPAREN) || atEnd()) {
                // Trailing semicolon: f[1;] — add empty projection slot
                if (wasSemi) exprs.add(QNode.leaf(QNode.Type.PROJECTION, null, ""));
                break;
            }
            if (wasSemi && check(QToken.Type.SEMI)) {
                exprs.add(QNode.leaf(QNode.Type.PROJECTION, null, ""));
            } else if (wasSemi) {
                try {
                    exprs.add(parseExpr(BP_NONE));
                } catch (Exception e) {
                    error("parse error: " + e.getMessage());
                    skipToSync();
                    exprs.add(QNode.leaf(QNode.Type.EMPTY, null, ""));
                }
            }
            if (pos == beforePos) { advance(); break; }
        }
        return exprs;
    }

    // ---- Helpers ----

    private List<QNode> listOf(QNode... nodes) {
        List<QNode> list = new ArrayList<>();
        for (QNode n : nodes) if (n != null) list.add(n);
        return list;
    }
}
