package com.timestored.qdoc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.timestored.qdoc.parser.*;

/**
 * Extracts documentation from q source files using the Pratt parser AST
 * and COMMENT tokens. Produces the same ParsedQFile/ParsedQEntity output
 * as QFileParser for compatibility.
 */
public class QParserDocExtractor {
    private static final Logger LOG = Logger.getLogger(QParserDocExtractor.class.getName());

    private static class BlockComment {
        final int startLine, endLine, startOffset, endOffset;
        final List<String> lines;
        BlockComment(int sl, int el, int so, int eo, List<String> lines) {
            this.startLine = sl; this.endLine = el; this.startOffset = so; this.endOffset = eo; this.lines = lines;
        }
    }

    public static ParsedQFile parse(String source, String filePath, String fileTitle) {
        source = source.replace("\r\n", "\n").replace("\r", "\n");

        List<BlockComment> blockComments = extractBlockComments(source);
        List<QToken> tokens = QLexer.tokenize(source);
        QParser parser = new QParser(tokens);
        parser.setTimeout(5000);
        QNode ast = parser.parseFile();

        List<QNode> stmts = (ast.type == QNode.Type.STAT_LIST) ? ast.children :
            (ast.type == QNode.Type.EMPTY ? Collections.emptyList() : Collections.singletonList(ast));

        Set<Integer> claimedCommentIndices = new HashSet<>();
        Set<Integer> claimedBlockIndices = new HashSet<>();
        int[] lineOffsets = buildLineOffsets(source);

        // --- Header extraction ---
        List<String> headerComments = new ArrayList<>();

        // Block comment at very start of file = header
        if (!blockComments.isEmpty() && blockComments.get(0).startLine == 1) {
            headerComments.addAll(blockComments.get(0).lines);
            claimedBlockIndices.add(0);
        }

        // Inline comments at start of file
        // Collect them, then decide if they're header or entity comments
        List<Integer> startCommentIndices = new ArrayList<>();
        List<String> startCommentTexts = new ArrayList<>();
        int startCommentSplit = -1;
        int newlineStreak = 0;
        int previousCommentLine = -1;
        for (int i = 0; i < tokens.size(); i++) {
            QToken t = tokens.get(i);
            if (t.type == QToken.Type.COMMENT) {
                if (previousCommentLine > -1 && t.line > previousCommentLine + 1
                        && !startCommentTexts.isEmpty() && startCommentSplit < 0) {
                    startCommentSplit = startCommentTexts.size();
                }
                String commentText = cleanCommentText(t.text);
                startCommentIndices.add(i);
                startCommentTexts.add(commentText);
                previousCommentLine = t.line;
                newlineStreak = 0;
            } else if (t.type == QToken.Type.NEWLINE) {
                newlineStreak++;
                if (newlineStreak >= 2 && !startCommentTexts.isEmpty() && startCommentSplit < 0) {
                    startCommentSplit = startCommentTexts.size();
                }
            } else {
                break;
            }
        }

        // A physical blank line at the top of a file splits file/namespace docs
        // from the first entity docs. Comment paragraph breaks stay inside docs.
        if (startCommentSplit > -1) {
            headerComments.addAll(startCommentTexts.subList(0, startCommentSplit));
            claimedCommentIndices.addAll(startCommentIndices.subList(0, startCommentSplit));
            startCommentIndices = new ArrayList<>(startCommentIndices.subList(startCommentSplit, startCommentIndices.size()));
            startCommentTexts = new ArrayList<>(startCommentTexts.subList(startCommentSplit, startCommentTexts.size()));
        }

        // Determine if start comments are header or entity:
        // If first AST stmt is assignment AND there's no blank line between
        // comments and the assignment, comments belong to the entity.
        // If there IS a blank line, or first stmt is not an assignment,
        // comments are header.
        boolean claimAsHeader = true;
        if (!stmts.isEmpty() && stmts.get(0).type == QNode.Type.ASSIGN
                && blockComments.isEmpty() && !startCommentIndices.isEmpty()) {
            // Check for blank line between last comment and first code
            int lastCI = startCommentIndices.get(startCommentIndices.size() - 1);
            int newlineCount = 0;
            for (int i = lastCI + 1; i < tokens.size(); i++) {
                if (tokens.get(i).type == QToken.Type.NEWLINE) newlineCount++;
                else if (tokens.get(i).type == QToken.Type.COMMENT) { newlineCount = 0; } // more comments reset
                else break;
            }
            claimAsHeader = newlineCount >= 2; // blank line = header
        }

        if (claimAsHeader) {
            headerComments.addAll(startCommentTexts);
            claimedCommentIndices.addAll(startCommentIndices);
        }

        ParsedComments headerDoc = ParsedComments.parse(headerComments);
        ParsedQFile pqf = new ParsedQFile(headerDoc, "", filePath, fileTitle);

        // --- Entity extraction ---
        List<ParsedQEntity> entities = new ArrayList<>();
        String curNamespace = ".";

        for (QNode stmt : stmts) {
            if (stmt.type == QNode.Type.SLASH && stmt.text != null) {
                String ns = parseNamespaceDirective(stmt.text);
                if (ns != null) curNamespace = ns;
                continue;
            }

            if (isSystemNamespaceCall(stmt)) {
                String ns = extractSystemNamespace(stmt);
                if (ns != null) curNamespace = ns;
                continue;
            }

            // Look for assignments — both top-level and inside APPLY (e.g. show b:banana)
            extractAssignments(stmt, curNamespace, pqf, entities, tokens, blockComments,
                    lineOffsets, claimedCommentIndices, claimedBlockIndices);
        }

        combineByFullName(entities);
        pqf.setqEntities(entities);
        return pqf;
    }

    /** Extract ASSIGN nodes from a statement — handles nested assigns inside APPLY */
    private static void extractAssignments(QNode stmt, String curNamespace, ParsedQFile pqf,
            List<ParsedQEntity> entities, List<QToken> tokens, List<BlockComment> blockComments,
            int[] lineOffsets, Set<Integer> claimedCommentIndices, Set<Integer> claimedBlockIndices) {

        if (stmt.type == QNode.Type.ASSIGN && stmt.text != null) {
            addEntity(stmt, curNamespace, pqf, entities, tokens, blockComments,
                    lineOffsets, claimedCommentIndices, claimedBlockIndices);
            // Chained assignment: f:g:{...} — the value is also an ASSIGN
            if (!stmt.children.isEmpty()) {
                QNode value = stmt.children.get(stmt.children.size() - 1);
                if (value.type == QNode.Type.ASSIGN && value.text != null) {
                    extractAssignments(value, curNamespace, pqf, entities, tokens, blockComments,
                            lineOffsets, claimedCommentIndices, claimedBlockIndices);
                }
            }
        }
        // Check inside APPLY for nested assignments: show b:banana
        if (stmt.type == QNode.Type.APPLY) {
            for (QNode child : stmt.children) {
                if (child.type == QNode.Type.ASSIGN && child.text != null) {
                    addEntity(child, curNamespace, pqf, entities, tokens, blockComments,
                            lineOffsets, claimedCommentIndices, claimedBlockIndices);
                }
            }
        }
    }

    private static void addEntity(QNode assignNode, String curNamespace, ParsedQFile pqf,
            List<ParsedQEntity> entities, List<QToken> tokens, List<BlockComment> blockComments,
            int[] lineOffsets, Set<Integer> claimedCommentIndices, Set<Integer> claimedBlockIndices) {

        String name = assignNode.text;
        String ns = curNamespace;

        // Handle fully-qualified dotted names like .ns.name
        if (name.startsWith(".") && name.indexOf('.', 1) > 0) {
            int lastDot = name.lastIndexOf('.');
            ns = name.substring(0, lastDot);
            name = name.substring(lastDot + 1);
        }

        // Find preceding comments: try block comments first, then inline
        List<String> comments = new ArrayList<>();
        int stmtLine = offsetToLine(lineOffsets, assignNode.offset);

        // Check for block comment ending just before this statement (within 3 lines)
        for (int bi = 0; bi < blockComments.size(); bi++) {
            if (claimedBlockIndices.contains(bi)) continue;
            BlockComment bc = blockComments.get(bi);
            if (bc.endLine >= stmtLine - 3 && bc.endLine <= stmtLine) {
                comments.addAll(bc.lines);
                claimedBlockIndices.add(bi);
                break;
            }
        }

        if (comments.isEmpty()) {
            comments = claimPrecedingComments(tokens, assignNode.offset, claimedCommentIndices);
        }

        ParsedComments pc = ParsedComments.parse(comments);
        List<String> paramNames = extractParamNames(assignNode);
        entities.add(ParsedQEntity.get(pqf, name, ns, pc, assignNode.offset, paramNames));
    }

    /** Extract parameter names from an ASSIGN node whose value is a FUNCTION. */
    private static List<String> extractParamNames(QNode assignNode) {
        // Find the FUNCTION child
        QNode funcNode = null;
        for (QNode c : assignNode.children) {
            if (c.type == QNode.Type.FUNCTION) { funcNode = c; break; }
        }
        if (funcNode == null) return null; // not a function assignment

        if (funcNode.text != null && funcNode.text.startsWith("explicit:")) {
            // Explicit params: first N children are param IDs
            int paramCount;
            try { paramCount = Integer.parseInt(funcNode.text.substring("explicit:".length())); }
            catch (NumberFormatException e) { return Collections.emptyList(); }
            List<String> names = new ArrayList<>();
            for (int i = 0; i < Math.min(paramCount, funcNode.children.size()); i++) {
                QNode c = funcNode.children.get(i);
                if (c.type == QNode.Type.ID && c.text != null) {
                    names.add(c.text);
                } else {
                    // Destructuring param (a;b) — use first child's name or placeholder
                    String name = extractFirstName(c);
                    names.add(name != null ? name : "_" + i);
                }
            }
            return names;
        } else {
            // Implicit params: scan body for x, y, z references
            Set<String> found = new LinkedHashSet<>();
            for (QNode c : funcNode.children) collectImplicitParams(c, found);
            // Return in canonical order: x, y, z
            List<String> names = new ArrayList<>();
            if (found.contains("x")) names.add("x");
            if (found.contains("y")) names.add("y");
            if (found.contains("z")) names.add("z");
            // If implicit but no x/y/z found, assume 1 arg (x) — safer than assuming 0
            if (names.isEmpty()) names.add("x");
            return names;
        }
    }

    /** Extract first ID name from a node tree (for destructuring params) */
    private static String extractFirstName(QNode node) {
        if (node.type == QNode.Type.ID && node.text != null) return node.text;
        for (QNode c : node.children) {
            String name = extractFirstName(c);
            if (name != null) return name;
        }
        return null;
    }

    private static void collectImplicitParams(QNode node, Set<String> found) {
        if (node.type == QNode.Type.ID && node.text != null) {
            if ("x".equals(node.text) || "y".equals(node.text) || "z".equals(node.text)) {
                found.add(node.text);
            }
        }
        // Don't recurse into nested functions — they have their own scope
        if (node.type == QNode.Type.FUNCTION) return;
        for (QNode c : node.children) collectImplicitParams(c, found);
    }

    public static ParsedQFile parse(File file) {
        try {
            String s = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return parse(s, file.getAbsolutePath(), file.getName());
        } catch (IOException e) {
            LOG.log(Level.INFO, "Could not read q file", e);
            return parse("", file.getAbsolutePath(), file.getName());
        }
    }

    // ---- Block comment extraction ----

    private static List<BlockComment> extractBlockComments(String source) {
        List<BlockComment> results = new ArrayList<>();
        String[] lines = source.split("\n", -1);
        int offset = 0;
        int i = 0;
        while (i < lines.length) {
            String trimmed = lines[i].trim();
            if (trimmed.equals("/")) {
                int startLine = i + 1;
                int startOffset = offset + lines[i].length() + 1;
                List<String> commentLines = new ArrayList<>();
                i++;
                boolean closed = false;
                while (i < lines.length) {
                    String lt = lines[i].trim();
                    // \ at start of line closes the block (even if code follows)
                    if (lt.startsWith("\\")) {
                        closed = true;
                        i++;
                        break;
                    }
                    if (!lt.isEmpty()) commentLines.add(lt);
                    i++;
                }
                int endLine = i;
                int endOffset = 0;
                for (int j = 0; j < Math.min(i, lines.length); j++) endOffset += lines[j].length() + 1;
                results.add(new BlockComment(startLine, endLine, startOffset, endOffset, commentLines));
                if (!closed) break;
                continue;
            }
            offset += lines[i].length() + 1;
            i++;
        }
        return results;
    }

    // ---- Utilities ----

    private static int[] buildLineOffsets(String source) {
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < source.length(); i++)
            if (source.charAt(i) == '\n') offsets.add(i + 1);
        return offsets.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int offsetToLine(int[] lineOffsets, int offset) {
        for (int i = lineOffsets.length - 1; i >= 0; i--)
            if (offset >= lineOffsets[i]) return i + 1;
        return 1;
    }

    private static String parseNamespaceDirective(String text) {
        String t = text.trim();
        if (t.startsWith("d ")) { String ns = t.substring(2).trim(); if (ns.startsWith(".")) return ns; }
        return null;
    }

    private static boolean isSystemNamespaceCall(QNode node) {
        if (node.type != QNode.Type.APPLY || node.children.size() < 2) return false;
        if (!"system".equals(node.children.get(0).text)) return false;
        QNode arg = findStringArg(node);
        if (arg != null && arg.type == QNode.Type.LITERAL && arg.text != null) {
            String s = stripQuotes(arg.text).trim();
            return s.startsWith("d ") || s.startsWith("d.") || s.equals(".");
        }
        return false;
    }

    private static String extractSystemNamespace(QNode node) {
        QNode arg = findStringArg(node);
        if (arg == null) return null;
        String s = stripQuotes(arg.text).trim();
        if (s.equals(".")) return ".";
        if (s.startsWith("d ") || s.startsWith("d.")) {
            String ns = s.substring(1).trim();
            return ns.startsWith(".") ? ns : ".";
        }
        return null;
    }

    private static QNode findStringArg(QNode apply) {
        for (int i = 1; i < apply.children.size(); i++) {
            QNode c = apply.children.get(i);
            if (c.type == QNode.Type.LITERAL && c.text != null && c.text.startsWith("\"")) return c;
        }
        return null;
    }

    private static String stripQuotes(String s) {
        if (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1);
        return s != null ? s : "";
    }

    private static List<String> claimPrecedingComments(List<QToken> tokens, int targetOffset, Set<Integer> claimed) {
        List<String> comments = new ArrayList<>();
        if (targetOffset < 0) return comments;
        int targetIdx = findTokenAtOrAfter(tokens, targetOffset);
        if (targetIdx <= 0) return comments;
        boolean sawNewline = false;
        for (int i = targetIdx - 1; i >= 0; i--) {
            QToken t = tokens.get(i);
            if (t.type == QToken.Type.COMMENT && !claimed.contains(i)) {
                comments.add(0, cleanCommentText(t.text));
                claimed.add(i);
                sawNewline = false;
            } else if (t.type == QToken.Type.NEWLINE) {
                if (sawNewline) break;
                sawNewline = true;
            } else {
                break;
            }
        }
        return comments;
    }

    private static int findTokenAtOrAfter(List<QToken> tokens, int targetOffset) {
        for (int i = 0; i < tokens.size(); i++) {
            QToken t = tokens.get(i);
            if (t.type == QToken.Type.COMMENT || t.type == QToken.Type.NEWLINE) continue;
            if (t.pos >= targetOffset) return i;
        }
        return -1;
    }

    private static String cleanCommentText(String text) {
        int i = 0;
        while (i < text.length() && (text.charAt(i) == '/' || text.charAt(i) == '#' || text.charAt(i) == ' ')) i++;
        return i > 0 ? text.substring(i) : text;
    }

    private static void combineByFullName(List<ParsedQEntity> entities) {
        Map<String, ParsedQEntity> seen = new LinkedHashMap<>();
        for (ParsedQEntity e : entities) seen.putIfAbsent(e.getFullName(), e);
        entities.clear();
        entities.addAll(seen.values());
    }
}
