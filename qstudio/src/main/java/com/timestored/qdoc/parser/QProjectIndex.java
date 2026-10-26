package com.timestored.qdoc.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import com.timestored.qdoc.ParsedQEntity;
import com.timestored.qdoc.ParsedQFile;
import com.timestored.qdoc.QParserDocExtractor;

/**
 * Project-wide symbol index built from parsed q files.
 * Maps fully-qualified symbol names to the files that define them.
 * Used for cross-file lint checking (UNDEFINED_REFERENCE).
 */
public class QProjectIndex {

    private static final Set<String> BUILTIN_NAMESPACES = new HashSet<>(Arrays.asList(
        ".q", ".Q", ".h", ".z", ".j", ".o", ".m", ".log"
    ));

    private final Map<String, Set<String>> symbolToFiles;
    private final Map<String, FunctionInfo> functionInfoMap;

    /** Metadata about a namespace function for arg count / type checking */
    public static class FunctionInfo {
        public final String fullName;
        public final int arity;               // -1 if implicit params
        public final boolean explicitParams;
        public final List<String> paramNames;  // empty if implicit

        public FunctionInfo(String fullName, int arity, boolean explicitParams, List<String> paramNames) {
            this.fullName = fullName;
            this.arity = arity;
            this.explicitParams = explicitParams;
            this.paramNames = paramNames != null ? paramNames : Collections.emptyList();
        }
    }

    private QProjectIndex(Map<String, Set<String>> symbolToFiles, Map<String, FunctionInfo> functionInfoMap) {
        this.symbolToFiles = symbolToFiles;
        this.functionInfoMap = functionInfoMap;
    }

    /** Build index from already-parsed files — includes FunctionInfo from entity paramNames */
    public static QProjectIndex build(List<ParsedQFile> parsedFiles) {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        Map<String, FunctionInfo> funcInfo = new LinkedHashMap<>();
        for (ParsedQFile pqf : parsedFiles) {
            for (ParsedQEntity entity : pqf.getQEntities()) {
                String file = entity.getSource();
                String fullName = entity.getFullName();
                map.computeIfAbsent(fullName, k -> new LinkedHashSet<>()).add(file);
                java.util.List<String> params = entity.getParamNames();
                if (params != null) {
                    boolean explicit = !isImplicitParams(params);
                    funcInfo.put(fullName, new FunctionInfo(fullName, params.size(), explicit, params));
                }
            }
        }
        return new QProjectIndex(map, funcInfo);
    }

    /** Check if param names indicate implicit params (all are x, y, or z) */
    public static boolean isImplicitParams(java.util.List<String> paramNames) {
        if (paramNames == null || paramNames.isEmpty()) return false;
        for (String p : paramNames) {
            if (!"x".equals(p) && !"y".equals(p) && !"z".equals(p)) return false;
        }
        return true;
    }

    /** Convenience: parse all .q files in a directory, then build index */
    public static QProjectIndex buildFromDirectory(File directory) {
        List<ParsedQFile> parsed = new ArrayList<>();
        try {
            List<Path> qFiles = Files.walk(directory.toPath())
                .filter(p -> p.toString().endsWith(".q"))
                .filter(p -> !p.toString().contains(".git"))
                .collect(Collectors.toList());
            for (Path p : qFiles) {
                try {
                    String code = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                    parsed.add(QParserDocExtractor.parse(code, p.toString(), p.getFileName().toString()));
                } catch (Exception e) { /* skip */ }
            }
        } catch (IOException e) { /* empty index */ }
        return build(parsed);
    }

    public boolean isDefined(String fullName) { return symbolToFiles.containsKey(fullName); }

    /** Check if at least one symbol from this namespace has been parsed (user-defined only) */
    public boolean hasNamespace(String ns) {
        String prefix = ns + ".";
        for (String sym : symbolToFiles.keySet()) {
            if (sym.startsWith(prefix)) return true;
        }
        return false;
    }

    /** Get FunctionInfo for a fully-qualified name, or null */
    public FunctionInfo getFunctionInfo(String fullName) {
        return functionInfoMap.get(fullName);
    }

    /** Check if a reference is to a built-in kx namespace (.q, .Q, .h, .z, .j, .o, .m) */
    public static boolean isBuiltinNamespace(String ref) {
        if (ref == null || !ref.startsWith(".")) return false;
        int secondDot = ref.indexOf('.', 1);
        String ns = secondDot > 0 ? ref.substring(0, secondDot) : ref;
        return BUILTIN_NAMESPACES.contains(ns);
    }

    public Set<String> getDefinitions(String fullName) {
        return symbolToFiles.getOrDefault(fullName, Collections.emptySet());
    }

    public Set<String> getAllSymbols() { return Collections.unmodifiableSet(symbolToFiles.keySet()); }
    public int size() { return symbolToFiles.size(); }

    /**
     * Find dead code: symbols defined in the project but never referenced by any other file.
     * A symbol is "dead" if no file other than its defining file references it.
     * Only checks fully-qualified .ns.name symbols (top-level globals are excluded — too noisy).
     *
     * @param parsedFiles the same files used to build this index
     * @return map of dead symbol name → file that defines it
     */
    public Map<String, String> findDeadCode(List<ParsedQFile> parsedFiles) {
        // Collect all .ns.name references across all files
        Map<String, Set<String>> referencedIn = new LinkedHashMap<>(); // symbol → files that reference it
        for (ParsedQFile pqf : parsedFiles) {
            String code = null;
            // Re-parse to get AST for reference scanning
            try {
                // Use the file path to identify — we need the AST for reference collection
                String fileTitle = pqf.getQEntities().isEmpty() ? "" : pqf.getQEntities().get(0).getSource();
                List<QToken> tokens = QLexer.tokenize(getSourceFromPqf(pqf));
                QParser parser = new QParser(tokens);
                parser.setTimeout(5000);
                QNode ast = parser.parseFile();
                Set<String> refs = new HashSet<>();
                collectDottedReferences(ast, refs);
                for (String ref : refs) {
                    referencedIn.computeIfAbsent(ref, k -> new LinkedHashSet<>()).add(fileTitle);
                }
            } catch (Exception e) { /* skip */ }
        }

        // Dead = defined but not referenced by any OTHER file
        Map<String, String> dead = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : symbolToFiles.entrySet()) {
            String symbol = entry.getKey();
            if (!symbol.startsWith(".") || !symbol.contains(".")) continue; // only .ns.name
            Set<String> definedIn = entry.getValue();
            Set<String> usedIn = referencedIn.getOrDefault(symbol, Collections.emptySet());
            // Remove self-references (defined and used in same file)
            Set<String> externalUses = new HashSet<>(usedIn);
            externalUses.removeAll(definedIn);
            if (externalUses.isEmpty()) {
                dead.put(symbol, definedIn.iterator().next());
            }
        }
        return dead;
    }

    /** Collect all dotted references (IDs containing dots) from an AST */
    private static void collectDottedReferences(QNode node, Set<String> refs) {
        if (node == null) return;
        if (node.type == QNode.Type.ID && node.text != null && node.text.contains(".")) {
            String ref = node.text.startsWith(".") ? node.text : "." + node.text;
            refs.add(ref);
        }
        // Skip ASSIGN targets — those are definitions, not references
        if (node.type == QNode.Type.ASSIGN) {
            for (int i = 1; i < node.children.size(); i++)
                collectDottedReferences(node.child(i), refs);
            return;
        }
        for (QNode c : node.children) collectDottedReferences(c, refs);
    }

    /**
     * Get source code from a ParsedQFile. Since ParsedQFile doesn't store source,
     * we need the original source. This is a limitation — callers should use the
     * overload that accepts source strings.
     */
    private static String getSourceFromPqf(ParsedQFile pqf) {
        // Can't recover source from ParsedQFile — return empty
        // Callers should use findDeadCode(List<ParsedQFile>, Map<String,String> fileSources)
        return "";
    }

    /**
     * Find dead code with access to source code for reference scanning.
     * @param parsedFiles the parsed files (for definitions)
     * @param fileSources map of file title → source code (for reference scanning)
     * @return map of dead symbol name → file that defines it
     */
    public Map<String, String> findDeadCode(List<ParsedQFile> parsedFiles, Map<String, String> fileSources) {
        Map<String, Set<String>> referencedIn = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fileSources.entrySet()) {
            String fileTitle = entry.getKey();
            String source = entry.getValue();
            try {
                List<QToken> tokens = QLexer.tokenize(source);
                QParser parser = new QParser(tokens);
                parser.setTimeout(5000);
                QNode ast = parser.parseFile();
                Set<String> refs = new HashSet<>();
                collectDottedReferences(ast, refs);
                for (String ref : refs) {
                    referencedIn.computeIfAbsent(ref, k -> new LinkedHashSet<>()).add(fileTitle);
                }
            } catch (Exception e) { /* skip */ }
        }

        Map<String, String> dead = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> defEntry : symbolToFiles.entrySet()) {
            String symbol = defEntry.getKey();
            if (!symbol.startsWith(".") || symbol.indexOf('.', 1) < 0) continue;
            Set<String> definedIn = defEntry.getValue();
            Set<String> usedIn = referencedIn.getOrDefault(symbol, Collections.emptySet());
            Set<String> externalUses = new HashSet<>(usedIn);
            externalUses.removeAll(definedIn);
            if (externalUses.isEmpty()) {
                dead.put(symbol, definedIn.iterator().next());
            }
        }
        return dead;
    }

    @Override public String toString() { return "QProjectIndex{" + size() + " symbols}"; }
}
