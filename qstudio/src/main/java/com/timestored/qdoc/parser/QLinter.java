package com.timestored.qdoc.parser;

import java.util.*;

/**
 * Static analysis linter for q/kdb+ code. Walks the Pratt parser AST
 * and token stream to detect common issues. Rule IDs are compatible
 * with kx qlint naming. Supports CSV config via QLintConfig.
 */
public class QLinter {

    private static final Set<String> RESERVED_WORDS = QLexer.getReservedWords();

    public static List<ParseError> lint(String code) { return lint(code, null); }
    public static List<ParseError> lint(String code, String filename) { return lint(code, filename, QLintConfig.defaultConfig()); }
    public static List<ParseError> lint(String code, String filename, QLintConfig config) { return lint(code, filename, config, null); }

    // Thread-local token list for addResult to find line/col from offset
    private static ThreadLocal<List<QToken>> currentTokens = new ThreadLocal<>();

    public static List<ParseError> lint(String code, String filename, QLintConfig config, QProjectIndex index) {
        List<QToken> tokens = QLexer.tokenize(code);
        currentTokens.set(tokens);
        QParser parser = new QParser(tokens);
        parser.setTimeout(5000);
        QNode ast = parser.parseFile();
        List<ParseError> results = new ArrayList<>();

        // Token-level checks
        if (config.isEnabled(QLintRule.INVALID_ESCAPE)) checkInvalidEscape(tokens, results);
        if (config.isEnabled(QLintRule.DEPRECATED_DATETIME)) checkDeprecatedDatetime(tokens, results);
        if (config.isEnabled(QLintRule.LINE_TOO_LONG)) checkLineTooLong(tokens, results, config);
        if (config.isEnabled(QLintRule.TRAILING_SEMICOLON)) checkTrailingSemicolon(tokens, results);

        // AST-level checks
        walkAst(ast, results, false, config);

        // Collect file-level names (top-level assignments) for UNDECLARED_VAR
        Set<String> fileLevelNames = new HashSet<>();
        collectFileLevelNames(ast, fileLevelNames);

        // Scope analysis
        analyzeScopesRecursive(ast, results, config, fileLevelNames);

        // Multiple statements per line
        if (config.isEnabled(QLintRule.MULTIPLE_STATEMENTS_PER_LINE)) checkMultipleStatementsPerLine(ast, tokens, results);

        // Cross-file reference checking
        if (index != null) {
            if (config.isEnabled(QLintRule.UNDEFINED_REFERENCE))
                checkUndefinedReferences(ast, results, index);
            if (config.isEnabled(QLintRule.WRONG_ARG_COUNT) || config.isEnabled(QLintRule.PARAM_TYPE_MISMATCH))
                checkArgCountAndTypeWalk(ast, results, index, config);
        }

        // Filename check
        if (filename != null && config.isEnabled(QLintRule.RESERVED_NAME)) checkReservedName(filename, results);

        // Single-file rules (batch 4)
        if (config.isEnabled(QLintRule.TODO_MARKER)) checkTodoMarkers(tokens, results);
        if (config.isEnabled(QLintRule.DEPRECATED_INTERNAL)) checkDeprecatedInternal(ast, results);
        if (config.isEnabled(QLintRule.UNARY_FORM)) checkUnaryForm(tokens, results);

        // @qlintsuppress
        Set<String> suppressed = new HashSet<>();
        for (QToken t : tokens) {
            if (t.type == QToken.Type.COMMENT && t.text.trim().startsWith("@qlintsuppress "))
                suppressed.add(t.text.trim().substring("@qlintsuppress ".length()).trim());
        }
        if (!suppressed.isEmpty()) results.removeIf(e -> e.ruleId != null && suppressed.contains(e.ruleId));

        currentTokens.remove(); // clean up ThreadLocal
        return results;
    }

    // ---- AST walking ----

    private static void walkAst(QNode node, List<ParseError> results, boolean inExpr, QLintConfig config) {
        if (node == null) return;
        switch (node.type) {
            case ASSIGN:
                if (config.isEnabled(QLintRule.ASSIGN_RESERVED_WORD)) checkAssignReservedWord(node, results);
                if (config.isEnabled(QLintRule.INVALID_ASSIGN)) checkInvalidAssign(node, results);
                if (config.isEnabled(QLintRule.UNDERSCORE_IN_NAME)) checkUnderscoreInName(node, results);
                if (config.isEnabled(QLintRule.NAME_COLLISION)) checkNameCollision(node, results);
                if (config.isEnabled(QLintRule.XYZ_AS_LOCAL)) checkXyzAsLocal(node, results);
                for (int i = 1; i < node.children.size(); i++) walkAst(node.child(i), results, false, config);
                break;
            case COND:
                if (config.isEnabled(QLintRule.COND_EVENARGS)) checkCondEvenArgs(node, results);
                for (QNode c : node.children) walkAst(c, results, true, config);
                break;
            case CONTROL:
                if (config.isEnabled(QLintRule.EMPTY_IF)) checkEmptyIf(node, results);
                for (QNode c : node.children) walkAst(c, results, true, config);
                break;
            case BINARY:
                if (config.isEnabled(QLintRule.CAST_TYPE_NUMERICAL)) checkCastTypeNumerical(node, results);
                for (QNode c : node.children) walkAst(c, results, true, config);
                break;
            case FUNCTION:
                if (config.isEnabled(QLintRule.UNUSED_PARAM)) checkUnusedParam(node, results);
                if (config.isEnabled(QLintRule.UNREACHABLE_CODE)) checkUnreachableCode(node, results);
                if (config.isEnabled(QLintRule.NEED_EXPLICIT_RETURN)) checkNeedExplicitReturn(node, results);
                if (config.isEnabled(QLintRule.FUNCTION_TOO_LONG)) checkFunctionTooLong(node, results, config);
                if (config.isEnabled(QLintRule.TOO_MANY_PARAMS)) checkTooManyParams(node, results, config);
                if (config.isEnabled(QLintRule.IMPLICIT_PARAMS)) checkImplicitParams(node, results);
                for (QNode c : node.children) walkAst(c, results, false, config);
                break;
            case APPLY:
                if (config.isEnabled(QLintRule.DEBUG_FUNCTION)) checkDebugFunction(node, results);
                for (QNode c : node.children) walkAst(c, results, true, config);
                break;
            default:
                for (QNode c : node.children) walkAst(c, results, inExpr, config);
                break;
        }
    }

    // ---- Original rules (batch 1 & 2) ----

    private static void checkAssignReservedWord(QNode n, List<ParseError> r) {
        if (n.text != null && RESERVED_WORDS.contains(n.text))
            addResult(r, n, QLintRule.ASSIGN_RESERVED_WORD, "Assignment to reserved word '" + n.text + "'");
    }

    private static void checkCondEvenArgs(QNode n, List<ParseError> r) {
        int argCount = n.children.size() - 1;
        if (argCount > 0 && argCount % 2 == 0)
            addResult(r, n, QLintRule.COND_EVENARGS, "Conditional $ has " + argCount + " arguments (expected odd number)");
    }

    private static void checkInvalidEscape(List<QToken> tokens, List<ParseError> r) {
        for (QToken t : tokens) {
            if (t.type != QToken.Type.STRING && t.type != QToken.Type.CHAR) continue;
            for (int i = 0; i < t.text.length() - 1; i++) {
                if (t.text.charAt(i) == '\\') {
                    char next = t.text.charAt(i + 1);
                    if (next != 'n' && next != 'r' && next != 't' && next != '\\' && next != '/' && next != '"' && !(next >= '0' && next <= '7'))
                        r.add(new ParseError(new QToken(t.type, t.text, t.line, t.col + i, t.pos + i), "Invalid escape sequence '\\" + next + "'", QLintRule.INVALID_ESCAPE.name()));
                    i++;
                }
            }
        }
    }

    private static void checkDeprecatedDatetime(List<QToken> tokens, List<ParseError> r) {
        for (QToken t : tokens)
            if (t.type == QToken.Type.DATETIME) r.add(new ParseError(t, "Datetime type has been deprecated", QLintRule.DEPRECATED_DATETIME.name()));
    }

    private static void checkEmptyIf(QNode n, List<ParseError> r) {
        if ("if".equals(n.text) && n.children.size() - 2 <= 0) addResult(r, n, QLintRule.EMPTY_IF, "If statement lacks code to execute");
    }

    private static void checkCastTypeNumerical(QNode n, List<ParseError> r) {
        if (!"$".equals(n.text) || n.children.size() < 2) return;
        QNode left = n.child(0);
        if (left.type == QNode.Type.LITERAL && left.text != null && left.text.length() == 3 && left.text.charAt(0) == '"' && left.text.charAt(2) == '"') {
            char c = left.text.charAt(1);
            if (c >= 'a' && c <= 'z' && "bijhefcspmdznuvt".indexOf(c) >= 0)
                addResult(r, n, QLintRule.CAST_TYPE_NUMERICAL, "Casting using short type '" + c + "'");
        }
    }

    // STATEMENT_IN_EXPR removed — low practical value
    private static void checkInvalidAssign(QNode n, List<ParseError> r) { if (!n.children.isEmpty() && n.child(0).type == QNode.Type.LITERAL) addResult(r, n, QLintRule.INVALID_ASSIGN, "Attempt to assign to " + n.child(0).text); }

    private static void checkUnusedParam(QNode func, List<ParseError> r) {
        if (!func.text.startsWith("explicit")) return;
        int paramCount = getExplicitParamCount(func);
        List<String> params = new ArrayList<>();
        for (int i = 0; i < Math.min(paramCount, func.children.size()); i++) params.add(func.children.get(i).text);
        List<QNode> body = func.children.subList(Math.min(paramCount, func.children.size()), func.children.size());
        for (String p : params) if (!containsReference(body, p)) addResult(r, func, QLintRule.UNUSED_PARAM, "Parameter '" + p + "' is declared but never used");
    }

    /** Get the explicit param count from a FUNCTION node's text ("explicit:N") */
    private static int getExplicitParamCount(QNode func) {
        if (func.text == null || !func.text.startsWith("explicit:")) return 0;
        try { return Integer.parseInt(func.text.substring("explicit:".length())); }
        catch (NumberFormatException e) { return 0; }
    }

    private static void checkUnreachableCode(QNode func, List<ParseError> r) {
        List<QNode> body = getFunctionBody(func); boolean afterReturn = false;
        for (QNode s : body) { if (afterReturn) addResult(r, s, QLintRule.UNREACHABLE_CODE, "Code after return statement is unreachable");
            if (s.type == QNode.Type.OPERATOR && ":".equals(s.text) && !s.children.isEmpty()) afterReturn = true; }
    }

    private static void checkNeedExplicitReturn(QNode func, List<ParseError> r) {
        List<QNode> body = getFunctionBody(func); if (body.isEmpty()) return;
        QNode last = body.get(body.size() - 1);
        if (last.type == QNode.Type.ASSIGN) addResult(r, last, QLintRule.NEED_EXPLICIT_RETURN, "Function ends with assignment - returns generic null");
    }

    private static void checkDebugFunction(QNode n, List<ParseError> r) {
        if (n.children.isEmpty()) return; QNode fn = n.child(0);
        if ("show".equals(fn.text)) { addResult(r, n, QLintRule.DEBUG_FUNCTION, "Call to debug function 'show'"); return; }
        if (fn.type == QNode.Type.LITERAL && ("-1".equals(fn.text) || "-2".equals(fn.text))) addResult(r, n, QLintRule.DEBUG_FUNCTION, "Call to debug function '" + fn.text + "'");
    }

    private static void checkReservedName(String filename, List<ParseError> r) {
        String name = filename; int sep = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')); if (sep >= 0) name = name.substring(sep + 1);
        int dot = name.lastIndexOf('.'); if (dot >= 0) name = name.substring(0, dot);
        if (RESERVED_WORDS.contains(name)) r.add(new ParseError(-1, 0, 0, "File has reserved name '" + name + "'", QLintRule.RESERVED_NAME.name()));
    }

    // ---- Batch 3: Style rules ----

    private static void checkLineTooLong(List<QToken> tokens, List<ParseError> r, QLintConfig config) {
        int maxLen = config.getThreshold(QLintRule.LINE_TOO_LONG, QLintConfig.DEFAULT_MAX_LINE_LENGTH);
        Set<Integer> flaggedLines = new HashSet<>();
        for (QToken t : tokens) {
            if (t.type == QToken.Type.NEWLINE || t.type == QToken.Type.EOF || t.type == QToken.Type.COMMENT) continue;
            int endCol = t.col + t.text.length();
            if (endCol > maxLen && !flaggedLines.contains(t.line)) {
                flaggedLines.add(t.line);
                r.add(new ParseError(t, "Line " + t.line + " exceeds " + maxLen + " characters", QLintRule.LINE_TOO_LONG.name()));
            }
        }
    }

    private static void checkFunctionTooLong(QNode func, List<ParseError> r, QLintConfig config) {
        int maxLines = config.getThreshold(QLintRule.FUNCTION_TOO_LONG, QLintConfig.DEFAULT_MAX_FUNCTION_LINES);
        List<QNode> body = getFunctionBody(func);
        if (body.isEmpty()) return;
        // Estimate line count from first to last body node offsets
        int firstOffset = func.offset;
        int lastOffset = body.get(body.size() - 1).offset;
        // Rough line count — each statement is roughly one line
        int lineCount = body.size();
        if (lineCount > maxLines)
            addResult(r, func, QLintRule.FUNCTION_TOO_LONG, "Function has " + lineCount + " statements (max " + maxLines + ")");
    }

    private static void checkTooManyParams(QNode func, List<ParseError> r, QLintConfig config) {
        if (!func.text.startsWith("explicit")) return;
        int maxParams = config.getThreshold(QLintRule.TOO_MANY_PARAMS, QLintConfig.DEFAULT_MAX_PARAMS);
        int count = 0;
        for (QNode c : func.children) { if (c.type == QNode.Type.ID) count++; else break; }
        if (count > maxParams) addResult(r, func, QLintRule.TOO_MANY_PARAMS, "Function has " + count + " parameters (max " + maxParams + ")");
    }

    private static void checkUnderscoreInName(QNode n, List<ParseError> r) {
        if (n.text != null && n.text.contains("_") && !n.text.startsWith("."))
            addResult(r, n, QLintRule.UNDERSCORE_IN_NAME, "Name '" + n.text + "' contains underscore - conflicts with drop operator");
    }

    private static void checkImplicitParams(QNode func, List<ParseError> r) {
        if ("implicit".equals(func.text)) addResult(r, func, QLintRule.IMPLICIT_PARAMS, "Function uses implicit parameters - consider naming them");
    }

    private static void checkXyzAsLocal(QNode n, List<ParseError> r) {
        if (n.text != null && (n.text.equals("x") || n.text.equals("y") || n.text.equals("z"))) {
            // Check if we're inside a function (walk up would be ideal, but we check during FUNCTION walk)
            addResult(r, n, QLintRule.XYZ_AS_LOCAL, "'" + n.text + "' used as local variable - reserved for implicit parameters");
        }
    }

    private static void checkTrailingSemicolon(List<QToken> tokens, List<ParseError> r) {
        // Detect ;} pattern — semicolon before closing brace means empty last
        // statement in function body, which silently returns generic null.
        // {x+2;};  is bad — the ; before } means empty last statement
        // {x+2};   is fine — ; after } is just a statement separator
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (tokens.get(i).type == QToken.Type.SEMI) {
                // Look ahead past newlines/comments for RBRACE
                for (int j = i + 1; j < tokens.size(); j++) {
                    QToken.Type nt = tokens.get(j).type;
                    if (nt == QToken.Type.NEWLINE || nt == QToken.Type.COMMENT) continue;
                    if (nt == QToken.Type.RBRACE) {
                        r.add(new ParseError(tokens.get(i),
                            "Trailing semicolon before } - function returns generic null",
                            QLintRule.TRAILING_SEMICOLON.name()));
                    }
                    break;
                }
            }
        }
    }

    private static void checkMultipleStatementsPerLine(QNode ast, List<QToken> tokens, List<ParseError> r) {
        List<QNode> stmts = (ast.type == QNode.Type.STAT_LIST) ? ast.children : Collections.singletonList(ast);
        // Build offset-to-line map
        Map<Integer, Integer> offsetLine = new HashMap<>();
        for (QToken t : tokens) if (t.type != QToken.Type.NEWLINE && t.type != QToken.Type.COMMENT) offsetLine.put(t.pos, t.line);
        int prevLine = -1;
        for (QNode s : stmts) {
            Integer line = offsetLine.get(s.offset);
            if (line != null && line == prevLine)
                addResult(r, s, QLintRule.MULTIPLE_STATEMENTS_PER_LINE, "Multiple statements on line " + line);
            if (line != null) prevLine = line;
        }
    }

    // ---- Cross-file reference checking ----

    private static void checkUndefinedReferences(QNode node, List<ParseError> r, QProjectIndex index) {
        if (node == null) return;

        // Pattern 1: ID starting with . (e.g., .trade.getPrice as single token)
        if (node.type == QNode.Type.ID && node.text != null && node.text.startsWith(".")
                && node.text.indexOf('.', 1) > 0) {
            String ref = node.text;
            // Never flag builtins — even if user defined .z.po, we don't know all of .z
            if (!QProjectIndex.isBuiltinNamespace(ref) && !index.isDefined(ref)) {
                int secondDot = ref.indexOf('.', 1);
                String ns = secondDot > 0 ? ref.substring(0, secondDot) : ref;
                if (index.hasNamespace(ns)) {
                    addResult(r, node, QLintRule.UNDEFINED_REFERENCE, "Reference to undefined symbol '" + ref + "'");
                }
            }
        }

        // Pattern 2: ID like "trade.getPrice" (without leading dot) — check ".trade.getPrice"
        if (node.type == QNode.Type.ID && node.text != null && !node.text.startsWith(".") && node.text.contains(".")) {
            String ref = "." + node.text;
            if (!QProjectIndex.isBuiltinNamespace(ref) && !index.isDefined(ref) && !index.isDefined(node.text)) {
                int secondDot = ref.indexOf('.', 1);
                String ns = secondDot > 0 ? ref.substring(0, secondDot) : ref;
                if (index.hasNamespace(ns)) {
                    addResult(r, node, QLintRule.UNDEFINED_REFERENCE, "Reference to undefined symbol '" + ref + "'");
                }
            }
        }

        // Don't recurse into ASSIGN targets (first child) — those are definitions, not references
        if (node.type == QNode.Type.ASSIGN) {
            for (int i = 1; i < node.children.size(); i++) checkUndefinedReferences(node.child(i), r, index);
            return;
        }
        for (QNode c : node.children) checkUndefinedReferences(c, r, index);
    }

    // ---- Arg count and type checking ----

    private static void checkArgCountAndTypeWalk(QNode node, List<ParseError> r, QProjectIndex index, QLintConfig config) {
        if (node == null) return;
        if (node.type == QNode.Type.APPLY && node.children.size() >= 2) {
            checkArgCountAndType(node, r, index, config);
        }
        if (node.type == QNode.Type.ASSIGN) {
            for (int i = 1; i < node.children.size(); i++) checkArgCountAndTypeWalk(node.child(i), r, index, config);
            return;
        }
        for (QNode c : node.children) checkArgCountAndTypeWalk(c, r, index, config);
    }

    private static final Set<String> SYM_PARAMS = new HashSet<>(Arrays.asList("sym", "symbol", "symbols"));
    private static final Set<String> DATE_PARAMS = new HashSet<>(Arrays.asList(
        "date", "dates", "dt", "startDate", "start_date", "endDate", "end_date", "asofDate"));
    private static final Set<String> DICT_PARAMS = new HashSet<>(Arrays.asList("dict"));
    private static final Set<String> SPID_PARAMS = new HashSet<>(Arrays.asList("spid", "spids"));

    private static void checkArgCountAndType(QNode apply, List<ParseError> r, QProjectIndex index, QLintConfig config) {
        QNode funcNode = apply.child(0);
        if (funcNode == null || funcNode.text == null) return;

        // Resolve full name
        String funcName = funcNode.text;
        if (!funcName.contains(".")) return; // only namespace functions
        if (!funcName.startsWith(".")) funcName = "." + funcName;

        // Skip builtins and unknown namespaces
        if (QProjectIndex.isBuiltinNamespace(funcName)) return;
        int secondDot = funcName.indexOf('.', 1);
        if (secondDot < 0) return;
        String ns = funcName.substring(0, secondDot);
        if (!index.hasNamespace(ns)) return;

        QProjectIndex.FunctionInfo fi = index.getFunctionInfo(funcName);
        if (fi == null || !fi.explicitParams) return;

        // Count arguments (children after the function node)
        int argCount = apply.children.size() - 1;

        // Check for projection (empty/null children = intentional projection)
        boolean hasEmptyArgs = false;
        for (int i = 1; i < apply.children.size(); i++) {
            QNode arg = apply.child(i);
            if (arg == null || arg.type == QNode.Type.EMPTY) { hasEmptyArgs = true; break; }
        }

        // WRONG_ARG_COUNT
        if (config.isEnabled(QLintRule.WRONG_ARG_COUNT)) {
            if (argCount > fi.arity) {
                addResult(r, apply, QLintRule.WRONG_ARG_COUNT,
                    "'" + funcName + "' expects " + fi.arity + " args, got " + argCount);
            } else if (argCount < fi.arity && !hasEmptyArgs) {
                addResult(r, apply, QLintRule.WRONG_ARG_COUNT,
                    "'" + funcName + "' expects " + fi.arity + " args, got " + argCount + " (use semicolons for projection)");
            }
        }

        // PARAM_TYPE_MISMATCH — check each literal arg against param name
        if (!config.isEnabled(QLintRule.PARAM_TYPE_MISMATCH)) return;
        for (int i = 1; i < apply.children.size() && (i - 1) < fi.paramNames.size(); i++) {
            QNode arg = apply.child(i);
            if (arg == null || arg.type != QNode.Type.LITERAL || arg.text == null) continue;
            String paramName = fi.paramNames.get(i - 1);
            checkParamType(apply, arg, paramName, r);
        }
    }

    private static void checkParamType(QNode apply, QNode literal, String paramName, List<ParseError> r) {
        String text = literal.text;
        boolean isSymbol = text.startsWith("`");
        boolean isString = text.startsWith("\"");
        boolean isDate = isDateTimeLiteral(text);
        boolean isNumeric = isNumericLiteral(text);

        if (SYM_PARAMS.contains(paramName)) {
            if (isString) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected symbol for '" + paramName + "', got string");
            else if (isDate) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected symbol for '" + paramName + "', got date/time");
            else if (isNumeric) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected symbol for '" + paramName + "', got number");
        } else if (DATE_PARAMS.contains(paramName)) {
            if (isSymbol) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected date/time for '" + paramName + "', got symbol");
            else if (isString) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected date/time for '" + paramName + "', got string");
            else if (isNumeric && !isDate) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected date/time for '" + paramName + "', got number");
        } else if (DICT_PARAMS.contains(paramName)) {
            if (isSymbol) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected dictionary for '" + paramName + "', got symbol");
            else if (isString) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected dictionary for '" + paramName + "', got string");
            else if (isDate) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected dictionary for '" + paramName + "', got date/time");
            else if (isNumeric) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected dictionary for '" + paramName + "', got number");
        } else if (SPID_PARAMS.contains(paramName)) {
            if (!isNumeric || isFloatLiteral(text)) addResult(r, apply, QLintRule.PARAM_TYPE_MISMATCH,
                "Expected long/int for '" + paramName + "', got " + (isSymbol ? "symbol" : isString ? "string" : isDate ? "date/time" : "non-integer"));
        }
    }

    private static boolean isDateTimeLiteral(String text) {
        // Date literals contain . or : and digits, e.g. 2024.01.01, 12:00:00, etc.
        return text.matches("\\d{4}\\.\\d{2}\\.\\d{2}.*") || text.matches("\\d{2}:\\d{2}.*")
            || text.matches("\\d{4}\\.\\d{2}[mM]?") || text.endsWith("D") || text.contains("D");
    }

    private static boolean isNumericLiteral(String text) {
        if (text.isEmpty()) return false;
        char c = text.charAt(0);
        return (c >= '0' && c <= '9') || c == '-';
    }

    private static boolean isFloatLiteral(String text) {
        return text.contains(".") && !text.contains("-") || text.contains("e") || text.contains("E")
            || text.endsWith("f") || text.endsWith("e");
    }

    // ---- Batch 3: kx qlint rules ----

    private static void checkNameCollision(QNode n, List<ParseError> r) {
        // Top-level assignments that shadow built-in names (broader than ASSIGN_RESERVED_WORD)
        if (n.text != null && RESERVED_WORDS.contains(n.text))
            return; // Already caught by ASSIGN_RESERVED_WORD
        // Check against built-in namespace functions
        if (n.text != null && !n.text.startsWith(".") && RESERVED_WORDS.contains(n.text))
            addResult(r, n, QLintRule.NAME_COLLISION, "Assignment to '" + n.text + "' could shadow a built-in");
    }

    // ---- Scope analysis ----

    /** Collect all top-level assignment names (short names) from the file AST */
    private static void collectFileLevelNames(QNode ast, Set<String> names) {
        List<QNode> stmts = (ast.type == QNode.Type.STAT_LIST) ? ast.children : Collections.singletonList(ast);
        for (QNode s : stmts) {
            if (s.type == QNode.Type.ASSIGN && s.text != null) {
                String name = s.text;
                // Add both the full name and the short name
                names.add(name);
                if (name.contains(".")) {
                    names.add(name.substring(name.lastIndexOf('.') + 1));
                }
            }
        }
    }

    private static void analyzeScopesRecursive(QNode node, List<ParseError> r, QLintConfig config, Set<String> fileLevelNames) {
        if (node == null) return;
        if (node.type == QNode.Type.FUNCTION) analyzeFunction(node, r, config, fileLevelNames);
        for (QNode c : node.children) analyzeScopesRecursive(c, r, config, fileLevelNames);
    }

    private static void analyzeFunction(QNode func, List<ParseError> r, QLintConfig config, Set<String> fileLevelNames) {
        Set<String> params = new LinkedHashSet<>();
        int paramCount = getExplicitParamCount(func);
        List<QNode> bodyNodes;
        if (func.text.startsWith("explicit") && paramCount > 0) {
            for (int i = 0; i < Math.min(paramCount, func.children.size()); i++) params.add(func.children.get(i).text);
            bodyNodes = new ArrayList<>(func.children.subList(Math.min(paramCount, func.children.size()), func.children.size()));
        } else {
            bodyNodes = new ArrayList<>(); boolean inBody = false;
            for (QNode c : func.children) { if (!inBody && c.type == QNode.Type.ID) params.add(c.text); else { inBody = true; bodyNodes.add(c); } }
        }
        if (!func.text.startsWith("explicit")) params.addAll(Arrays.asList("x", "y", "z"));

        Map<String, QNode> localAssignments = new LinkedHashMap<>();
        for (QNode s : bodyNodes) collectAssignments(s, localAssignments);
        Set<String> references = new LinkedHashSet<>();
        for (QNode s : bodyNodes) collectReferences(s, references);
        Map<String, Integer> firstUseOffset = new LinkedHashMap<>();
        for (QNode s : bodyNodes) collectFirstUse(s, firstUseOffset);

        // TOO_MANY_LOCALS
        if (config.isEnabled(QLintRule.TOO_MANY_LOCALS)) {
            int maxLocals = config.getThreshold(QLintRule.TOO_MANY_LOCALS, QLintConfig.DEFAULT_MAX_LOCALS);
            if (localAssignments.size() > maxLocals)
                addResult(r, func, QLintRule.TOO_MANY_LOCALS, "Function has " + localAssignments.size() + " locals (max " + maxLocals + ")");
        }

        // TOO_MANY_GLOBALS
        if (config.isEnabled(QLintRule.TOO_MANY_GLOBALS)) {
            int maxGlobals = config.getThreshold(QLintRule.TOO_MANY_GLOBALS, QLintConfig.DEFAULT_MAX_GLOBALS);
            int globalCount = 0;
            for (String name : localAssignments.keySet()) if (name.startsWith(".")) globalCount++;
            if (globalCount > maxGlobals) addResult(r, func, QLintRule.TOO_MANY_GLOBALS, "Function modifies " + globalCount + " globals (max " + maxGlobals + ")");
        }

        // TOO_MANY_CONSTANTS
        if (config.isEnabled(QLintRule.TOO_MANY_CONSTANTS)) {
            int maxConst = config.getThreshold(QLintRule.TOO_MANY_CONSTANTS, QLintConfig.DEFAULT_MAX_CONSTANTS);
            int constCount = countLiterals(bodyNodes);
            if (constCount > maxConst) addResult(r, func, QLintRule.TOO_MANY_CONSTANTS, "Function has " + constCount + " constants (max " + maxConst + ")");
        }

        // GLOBAL_PEACH
        if (config.isEnabled(QLintRule.GLOBAL_PEACH)) checkGlobalPeach(func, r);

        // UNUSED_VAR
        if (config.isEnabled(QLintRule.UNUSED_VAR)) {
            for (Map.Entry<String, QNode> e : localAssignments.entrySet()) {
                String name = e.getKey();
                if (references.contains(name) || params.contains(name)) continue;
                // Skip builtin namespace assignments (.z.ts, .z.pw, etc.) — these are event handlers
                if (name.startsWith(".") && QProjectIndex.isBuiltinNamespace(name)) continue;
                addResult(r, e.getValue(), QLintRule.UNUSED_VAR, "Variable '" + name + "' is assigned but never used");
            }
        }

        // DECLARED_AFTER_USE
        if (config.isEnabled(QLintRule.DECLARED_AFTER_USE)) {
            for (Map.Entry<String, QNode> e : localAssignments.entrySet()) {
                Integer useOff = firstUseOffset.get(e.getKey());
                if (useOff != null && useOff < e.getValue().offset && !params.contains(e.getKey()))
                    addResult(r, e.getValue(), QLintRule.DECLARED_AFTER_USE, "Variable '" + e.getKey() + "' used before assignment");
            }
        }

        // UNDECLARED_VAR
        if (config.isEnabled(QLintRule.UNDECLARED_VAR) && func.text.startsWith("explicit")) {
            Set<String> allDeclared = new HashSet<>(params);
            allDeclared.addAll(localAssignments.keySet());
            allDeclared.addAll(RESERVED_WORDS);
            allDeclared.addAll(fileLevelNames); // file-level names are accessible by short name within \d namespace
            for (String ref : references) {
                if (allDeclared.contains(ref) || ref.isEmpty()) continue;
                // Skip built-in namespace references (.Q.*, .q.*, .z.*, .h.*, .j.*, .o.*, .m.*)
                if (QProjectIndex.isBuiltinNamespace(ref)) continue;
                if (ref.contains(".") && QProjectIndex.isBuiltinNamespace("." + ref)) continue;
                // Skip fully-qualified namespace refs — handled by UNDEFINED_REFERENCE rule instead
                if (ref.startsWith(".") && ref.indexOf('.', 1) > 0) continue;
                addResult(r, func, QLintRule.UNDECLARED_VAR, "Undeclared variable '" + ref + "'");
            }
        }

        // CONDITIONALLY_DECLARED
        if (config.isEnabled(QLintRule.CONDITIONALLY_DECLARED)) checkConditionallyDeclared(bodyNodes, references, params, r);
    }

    private static void checkGlobalPeach(QNode func, List<ParseError> r) {
        // Walk looking for peach adverb applied to functions that modify globals
        for (QNode c : func.children) {
            if (c.type == QNode.Type.ADVERB && "peach".equals(c.text)) {
                // Check if the adverbed function modifies globals
                for (QNode sub : c.children) {
                    if (sub.type == QNode.Type.FUNCTION && hasGlobalAssignment(sub))
                        addResult(r, c, QLintRule.GLOBAL_PEACH, "Modifying globals inside peach");
                }
            }
            if (c.type == QNode.Type.APPLY && c.children.size() >= 2) {
                QNode fn = c.child(0);
                // Check keyword peach: expr peach func
                if (fn.type == QNode.Type.ID && "peach".equals(fn.text)) {
                    for (int i = 1; i < c.children.size(); i++)
                        if (c.child(i).type == QNode.Type.FUNCTION && hasGlobalAssignment(c.child(i)))
                            addResult(r, c, QLintRule.GLOBAL_PEACH, "Modifying globals inside peach");
                }
            }
        }
    }

    private static boolean hasGlobalAssignment(QNode node) {
        if (node.type == QNode.Type.ASSIGN && node.text != null && node.text.startsWith(".")) return true;
        if (node.type == QNode.Type.FUNCTION) return false; // don't recurse into nested functions
        for (QNode c : node.children) if (hasGlobalAssignment(c)) return true;
        return false;
    }

    private static void checkConditionallyDeclared(List<QNode> bodyNodes, Set<String> references, Set<String> params, List<ParseError> r) {
        // Track unconditional assignments seen so far
        Set<String> unconditionallyDeclared = new HashSet<>(params);
        for (QNode stmt : bodyNodes) {
            if (stmt.type == QNode.Type.COND || (stmt.type == QNode.Type.CONTROL && "if".equals(stmt.text))) {
                Set<String> branchAssigns = new HashSet<>();
                for (QNode child : stmt.children) collectAssignmentNames(child, branchAssigns);
                for (String name : branchAssigns) {
                    // Only flag if not already declared unconditionally before this point
                    if (!unconditionallyDeclared.contains(name) && references.contains(name))
                        addResult(r, stmt, QLintRule.CONDITIONALLY_DECLARED, "Variable '" + name + "' may be undefined - declared only conditionally");
                }
            } else {
                // Collect top-level (unconditional) assignments
                collectAssignmentNames(stmt, unconditionallyDeclared);
            }
        }
    }

    private static void collectAssignmentNames(QNode node, Set<String> names) {
        if (node.type == QNode.Type.ASSIGN && node.text != null) names.add(node.text);
        if (node.type == QNode.Type.FUNCTION) return;
        for (QNode c : node.children) collectAssignmentNames(c, names);
    }

    private static int countLiterals(List<QNode> nodes) {
        int count = 0;
        for (QNode n : nodes) {
            if (n.type == QNode.Type.LITERAL) count++;
            if (n.type != QNode.Type.FUNCTION) count += countLiterals(n.children);
        }
        return count;
    }

    // ---- Batch 4: advanced rules ----

    private static void checkTodoMarkers(List<QToken> tokens, List<ParseError> r) {
        for (QToken t : tokens) {
            if (t.type != QToken.Type.COMMENT) continue;
            String upper = t.text.toUpperCase();
            if (upper.contains("TODO") || upper.contains("FIXME") || upper.contains("HACK") || upper.contains("XXX")) {
                r.add(new ParseError(t, t.text.trim(), QLintRule.TODO_MARKER.name()));
            }
        }
    }

    private static final java.util.Map<Integer, String> DEPRECATED_INTERNALS = new java.util.HashMap<>();
    static {
        DEPRECATED_INTERNALS.put(-1, "hsym"); DEPRECATED_INTERNALS.put(-2, "attr");
        DEPRECATED_INTERNALS.put(-3, ".Q.s1"); DEPRECATED_INTERNALS.put(-5, "parse");
        DEPRECATED_INTERNALS.put(-6, "eval"); DEPRECATED_INTERNALS.put(-7, "hcount");
        DEPRECATED_INTERNALS.put(-12, ".Q.host"); DEPRECATED_INTERNALS.put(-13, ".Q.addr");
        DEPRECATED_INTERNALS.put(-15, "md5"); DEPRECATED_INTERNALS.put(-19, "set");
        DEPRECATED_INTERNALS.put(-20, ".Q.gc"); DEPRECATED_INTERNALS.put(-24, "reval");
        DEPRECATED_INTERNALS.put(-29, ".j.k"); DEPRECATED_INTERNALS.put(-31, ".j.jd");
        DEPRECATED_INTERNALS.put(-32, ".Q.btoa"); DEPRECATED_INTERNALS.put(-34, ".Q.ts");
        DEPRECATED_INTERNALS.put(-35, ".Q.gz"); DEPRECATED_INTERNALS.put(-37, ".Q.prf0");
    }

    private static void checkDeprecatedInternal(QNode node, List<ParseError> r) {
        if (node == null) return;
        // Pattern: APPLY(!, LITERAL(-N), arg) — internal function call
        if (node.type == QNode.Type.APPLY && node.children.size() >= 2) {
            QNode func = node.child(0);
            QNode arg = node.child(1);
            if (func.type == QNode.Type.OPERATOR && "!".equals(func.text)
                    && arg.type == QNode.Type.LITERAL && arg.text != null) {
                try {
                    int val = Integer.parseInt(arg.text);
                    String replacement = DEPRECATED_INTERNALS.get(val);
                    if (replacement != null) {
                        addResult(r, node, QLintRule.DEPRECATED_INTERNAL,
                            "Use '" + replacement + "' instead of '" + val + "!'");
                    }
                } catch (NumberFormatException e) { /* not an integer literal */ }
            }
        }
        // Also check BINARY: -N ! x parses as BINARY(!, -N, x) in some cases
        if (node.type == QNode.Type.BINARY && "!".equals(node.text) && node.children.size() >= 2) {
            QNode left = node.child(0);
            if (left.type == QNode.Type.LITERAL && left.text != null) {
                try {
                    int val = Integer.parseInt(left.text);
                    String replacement = DEPRECATED_INTERNALS.get(val);
                    if (replacement != null) {
                        addResult(r, node, QLintRule.DEPRECATED_INTERNAL,
                            "Use '" + replacement + "' instead of '" + val + "!'");
                    }
                } catch (NumberFormatException e) { /* not an integer literal */ }
            }
        }
        for (QNode c : node.children) checkDeprecatedInternal(c, r);
    }

    private static final java.util.Map<String, String> UNARY_FORMS = new java.util.HashMap<>();
    static {
        UNARY_FORMS.put("!:", "key/til"); UNARY_FORMS.put("#:", "count");
        UNARY_FORMS.put("$:", "string"); UNARY_FORMS.put("%:", "reciprocal");
        UNARY_FORMS.put("&:", "where"); UNARY_FORMS.put("*:", "first");
        UNARY_FORMS.put("+:", "flip"); UNARY_FORMS.put(",:", "enlist");
        UNARY_FORMS.put("-:", "neg"); UNARY_FORMS.put(".:", "get");
        UNARY_FORMS.put("0::", "read0"); UNARY_FORMS.put("1::", "read1");
        UNARY_FORMS.put("<:", "iasc"); UNARY_FORMS.put("=:", "group");
        UNARY_FORMS.put(">:", "idesc"); UNARY_FORMS.put("?:", "distinct");
        UNARY_FORMS.put("@:", "type"); UNARY_FORMS.put("^:", "null");
        UNARY_FORMS.put("_:", "floor"); UNARY_FORMS.put("|:", "reverse");
        UNARY_FORMS.put("~:", "not");
    }

    private static void checkUnaryForm(List<QToken> tokens, List<ParseError> r) {
        // Unary forms: operator followed by : token, e.g. #: tokenizes as BINOP(#) COLON(:)
        // Must NOT be preceded by an ID (that would be modified assignment: a+:2)
        for (int i = 0; i < tokens.size() - 1; i++) {
            QToken t = tokens.get(i);
            QToken next = tokens.get(i + 1);
            if ((t.type == QToken.Type.BINOP || t.type == QToken.Type.MONOP)
                    && next.type == QToken.Type.COLON
                    && next.pos == t.pos + t.text.length()) { // adjacent
                // Check if preceded by an ID — that's modified assignment, not unary form
                boolean precededById = false;
                for (int j = i - 1; j >= 0; j--) {
                    QToken prev = tokens.get(j);
                    if (prev.type == QToken.Type.NEWLINE || prev.type == QToken.Type.COMMENT) continue;
                    if (prev.type == QToken.Type.ID) precededById = true;
                    break;
                }
                if (precededById) continue;

                String key = t.text + ":";
                String replacement = UNARY_FORMS.get(key);
                if (replacement != null) {
                    r.add(new ParseError(t, "Use '" + replacement + "' instead of '" + key + "'",
                        QLintRule.UNARY_FORM.name()));
                }
            }
        }
    }

    // ---- Helpers ----

    private static boolean containsReference(List<QNode> nodes, String name) {
        for (QNode n : nodes) { if (n.type == QNode.Type.ID && name.equals(n.text)) return true; if (containsReference(n.children, name)) return true; } return false;
    }

    private static List<QNode> getFunctionBody(QNode func) {
        int paramCount = func.text != null && func.text.startsWith("explicit:") ? getExplicitParamCount(func) : 0;
        if (paramCount > 0 && paramCount < func.children.size()) {
            return func.children.subList(paramCount, func.children.size());
        }
        // Fallback for implicit: skip leading IDs
        List<QNode> body = new ArrayList<>(); boolean inBody = false;
        for (QNode c : func.children) { if (!inBody && c.type == QNode.Type.ID) continue; inBody = true; body.add(c); } return body;
    }

    private static void collectAssignments(QNode node, Map<String, QNode> a) {
        if (node.type == QNode.Type.ASSIGN && node.text != null) a.putIfAbsent(node.text, node);
        if (node.type == QNode.Type.FUNCTION || node.type == QNode.Type.QUERY || node.type == QNode.Type.TABLE) return;
        for (QNode c : node.children) collectAssignments(c, a);
    }

    private static void collectReferences(QNode node, Set<String> refs) {
        if (node.type == QNode.Type.ID && node.text != null) refs.add(node.text);
        if (node.type == QNode.Type.ASSIGN) { for (int i = 1; i < node.children.size(); i++) collectReferences(node.child(i), refs); return; }
        if (node.type == QNode.Type.FUNCTION || node.type == QNode.Type.QUERY || node.type == QNode.Type.TABLE) return;
        for (QNode c : node.children) collectReferences(c, refs);
    }

    private static void collectFirstUse(QNode node, Map<String, Integer> firstUse) {
        if (node.type == QNode.Type.ID && node.text != null && node.offset >= 0) firstUse.putIfAbsent(node.text, node.offset);
        if (node.type == QNode.Type.FUNCTION || node.type == QNode.Type.QUERY || node.type == QNode.Type.TABLE) return;
        for (QNode c : node.children) collectFirstUse(c, firstUse);
    }

    private static void addResult(List<ParseError> r, QNode node, QLintRule rule, String message) {
        // Resolve offset — if node has -1, try first child
        int offset = node.offset;
        if (offset < 0 && !node.children.isEmpty()) {
            for (QNode c : node.children) {
                if (c.offset >= 0) { offset = c.offset; break; }
            }
        }
        if (offset >= 0) {
            List<QToken> tokens = currentTokens.get();
            QToken found = null;
            if (tokens != null) {
                for (QToken t : tokens) {
                    if (t.pos <= offset && t.type != QToken.Type.NEWLINE && t.type != QToken.Type.COMMENT) {
                        found = t;
                    }
                    if (t.pos > offset) break;
                }
            }
            if (found != null) {
                r.add(new ParseError(found, message, rule.name()));
            } else {
                r.add(new ParseError(new QToken(QToken.Type.ERROR, "", 1, 0, offset), message, rule.name()));
            }
        } else {
            r.add(new ParseError(new QToken(QToken.Type.ERROR, "", 1, 0, 0), message, rule.name()));
        }
    }
}
