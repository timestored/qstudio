package com.timestored.qdoc;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.qdoc.parser.*;
import com.timestored.qstudio.Language;

/**
 * Centralized code analysis model. Caches parse results, lint errors, metrics,
 * and cross-file index. Incrementally updates on file save/open/close.
 * No Swing dependency — pure model.
 */
public class QAnalysisModel implements OpenDocumentsModel.Listener {

    private static final Logger LOG = Logger.getLogger(QAnalysisModel.class.getName());

    private final OpenDocumentsModel openDocsModel;
    private final Map<String, ParsedQFile> parsedFiles = new ConcurrentHashMap<>();
    private final Map<String, List<ParseError>> lintErrors = new ConcurrentHashMap<>();
    private final Map<String, QMetricsReport> metrics = new ConcurrentHashMap<>();
    private QProjectIndex index = QProjectIndex.build(Collections.emptyList());
    private QLintConfig lintConfig = QLintConfig.defaultConfig();
    private String filterLevel = "NONE";
    private final List<AnalysisListener> listeners = new ArrayList<>();

    /** Listener notified when analysis results change */
    public interface AnalysisListener {
        void analysisComplete(QAnalysisModel model);
    }

    public QAnalysisModel(OpenDocumentsModel openDocsModel) {
        this.openDocsModel = openDocsModel;
        openDocsModel.addListener(this);
        reanalyzeAll();
    }

    public void addListener(AnalysisListener l) { listeners.add(l); }

    // ---- Public accessors ----

    /** Get lint errors filtered by current severity level */
    public Map<String, List<ParseError>> getFilteredLintErrors() {
        if ("ALL".equals(filterLevel)) return Collections.unmodifiableMap(lintErrors);
        if ("NONE".equals(filterLevel)) return Collections.emptyMap();
        Map<String, List<ParseError>> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, List<ParseError>> entry : lintErrors.entrySet()) {
            List<ParseError> fileFiltered = new ArrayList<>();
            for (ParseError e : entry.getValue()) {
                if (severityMatches(e.getSeverity())) fileFiltered.add(e);
            }
            if (!fileFiltered.isEmpty()) filtered.put(entry.getKey(), fileFiltered);
        }
        return filtered;
    }

    /** Get all lint errors unfiltered */
    public Map<String, List<ParseError>> getAllLintErrors() { return Collections.unmodifiableMap(lintErrors); }
    public List<ParseError> getLintErrors(String filename) { return lintErrors.getOrDefault(filename, Collections.emptyList()); }

    public String getFilterLevel() { return filterLevel; }

    public void setFilterLevel(String level) {
        this.filterLevel = level;
        fireListeners();
    }
    public QMetricsReport getMetrics(String filename) { return metrics.get(filename); }
    public Collection<QMetricsReport> getAllMetrics() { return Collections.unmodifiableCollection(metrics.values()); }
    public QProjectIndex getIndex() { return index; }
    public List<ParsedQFile> getParsedFiles() { return new ArrayList<>(parsedFiles.values()); }

    public void setLintConfig(QLintConfig config) {
        this.lintConfig = config;
        relintAll();
        fireListeners();
    }

    // ---- Analysis logic ----

    private void reanalyzeAll() {
        parsedFiles.clear();
        lintErrors.clear();
        metrics.clear();
        for (Document doc : openDocsModel.getDocuments()) {
            analyzeDocument(doc);
        }
        rebuildIndex();
        relintAll();
    }

    private void analyzeDocument(Document doc) {
        String code = doc.getContent();
        if (code == null || code.trim().isEmpty()) return;
        String fileEnding = doc.getFileEnding();
        Language lang = Language.getLanguage(fileEnding);
        if (lang != Language.Q && !fileEnding.isEmpty()) return;
        String filename = doc.getTitle();

        // Parse
        ParsedQFile pqf = QParserDocExtractor.parse(code, doc.getFilePath(), filename);
        parsedFiles.put(filename, pqf);

        // Metrics
        List<QToken> tokens = QLexer.tokenize(code);
        QParser parser = new QParser(tokens);
        parser.setTimeout(5000);
        QNode ast = parser.parseFile();
        metrics.put(filename, QCodeMetrics.analyze(ast, tokens, filename));

        // Parse errors (stored separately from lint)
        List<ParseError> parseErrors = QParser.parseAndCollectErrors(code);
        lintErrors.put(filename, new ArrayList<>(parseErrors));
    }

    private void rebuildIndex() {
        index = QProjectIndex.build(new ArrayList<>(parsedFiles.values()));
    }

    private void relintAll() {
        for (Map.Entry<String, ParsedQFile> entry : parsedFiles.entrySet()) {
            relintFile(entry.getKey());
        }
    }

    private void relintFile(String filename) {
        // Find the document to get its content
        for (Document doc : openDocsModel.getDocuments()) {
            if (doc.getTitle().equals(filename)) {
                String code = doc.getContent();
                if (code == null) return;

                // Start with parse errors
                List<ParseError> errors = new ArrayList<>(QParser.parseAndCollectErrors(code));
                // Add lint errors
                errors.addAll(QLinter.lint(code, filename, lintConfig, index));
                lintErrors.put(filename, errors);
                return;
            }
        }
    }

    private void analyzeAndNotify(Document doc) {
        analyzeDocument(doc);
        rebuildIndex();
        relintFile(doc.getTitle());
        fireListeners();
    }

    /** Cumulative: ERROR=errors only, WARNING=warnings+errors, INFO/ALL=everything, NONE=nothing */
    private boolean severityMatches(String severity) {
        switch (filterLevel) {
            case "ALL": case "INFO": return true;
            case "WARNING": return "WARNING".equals(severity) || "ERROR".equals(severity);
            case "ERROR": return "ERROR".equals(severity);
            case "NONE": return false;
            default: return false;
        }
    }

    private void fireListeners() {
        for (AnalysisListener l : listeners) {
            try { l.analysisComplete(this); }
            catch (Exception e) { LOG.warning("Listener error: " + e.getMessage()); }
        }
    }

    // ---- OpenDocumentsModel.Listener ----

    @Override public void docSaved() {
        Document doc = openDocsModel.getSelectedDocument();
        if (doc != null) analyzeAndNotify(doc);
    }

    @Override public void docAdded(Document document) {
        analyzeAndNotify(document);
    }

    @Override public void docClosed(Document document) {
        String filename = document.getTitle();
        parsedFiles.remove(filename);
        lintErrors.remove(filename);
        metrics.remove(filename);
        rebuildIndex();
        fireListeners();
    }

    @Override public void docSelected(Document document) {
        // No reanalysis needed, but listeners may want to update (e.g. current-file filter)
        fireListeners();
    }

    @Override public void docContentModified() { }
    @Override public void docCaratModified() { }
    @Override public void folderSelected(File selectedFolder) { }
    @Override public void ignoredFolderPatternSelected(java.util.regex.Pattern ignoredFolderPattern) { }
}
