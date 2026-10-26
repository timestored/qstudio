package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.JXTable;

import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.qdoc.QAnalysisModel;
import com.timestored.qdoc.parser.QLintConfig;
import com.timestored.qdoc.parser.QLintRule;
import com.timestored.qdoc.parser.ParseError;
import com.timestored.theme.Theme;

/**
 * Thin UI panel showing lint warnings and parse errors.
 * Reads from QAnalysisModel, filters by severity, fires highlight listeners.
 */
public class ProblemsPanel extends JPanel implements QAnalysisModel.AnalysisListener {

    private static final Logger LOG = Logger.getLogger(ProblemsPanel.class.getName());
    private static final String[] COLUMNS = {"Severity", "File", "Line", "Rule", "Message"};

    private final OpenDocumentsModel openDocsModel;
    private final QAnalysisModel model;
    private final JXTable table;
    private final DefaultTableModel tableModel;
    private final JLabel titleLabel;
    private final JButton filterBtn;
    private final JButton scopeBtn;
    private final Map<String, Boolean> disabledRules = new HashMap<>();
    private final List<LintHighlightListener> highlightListeners = new ArrayList<>();

    private boolean allFiles = true;

    /** Listener for editor highlighting */
    public interface LintHighlightListener {
        void onLintResultsChanged(Map<String, List<ParseError>> problemsByFile);
    }

    public ProblemsPanel(OpenDocumentsModel openDocsModel, QAnalysisModel model) {
        this.openDocsModel = openDocsModel;
        this.model = model;
        model.addListener(this);
        setLayout(new BorderLayout());

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

        titleLabel = new JLabel("Problems (0)");
        toolbar.add(titleLabel);

        filterBtn = new JButton("NONE");
        filterBtn.setToolTipText("Click to toggle severity filter");
        filterBtn.addActionListener(e -> {
            String level = model.getFilterLevel();
            switch (level) {
                case "ALL": level = "ERROR"; break;
                case "ERROR": level = "WARNING"; break;
                case "WARNING": level = "INFO"; break;
                case "INFO": level = "NONE"; break;
                default: level = "ALL"; break;
            }
            filterBtn.setText(level);
            model.setFilterLevel(level); // fires analysisComplete → applyFilter
        });
        toolbar.add(filterBtn);

        scopeBtn = new JButton("All Files");
        scopeBtn.setToolTipText("Click to toggle between current file and all files");
        scopeBtn.addActionListener(e -> {
            allFiles = !allFiles;
            scopeBtn.setText(allFiles ? "All Files" : "Current File");
            applyFilter();
        });
        toolbar.add(scopeBtn);

        JButton configBtn = new JButton(Theme.CIcon.SERVER_EDIT.get16());
        configBtn.setToolTipText("Configure lint rules");
        configBtn.addActionListener(e -> showConfigDialog());
        toolbar.add(configBtn);

        add(toolbar, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = Theme.getStripedTable(tableModel);
        table.setAutoResizeMode(JXTable.AUTO_RESIZE_LAST_COLUMN);

        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(60);
        colModel.getColumn(1).setPreferredWidth(120);
        colModel.getColumn(2).setPreferredWidth(50);
        colModel.getColumn(3).setPreferredWidth(150);
        colModel.getColumn(4).setPreferredWidth(500);

        // Single-click to navigate
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    int modelRow = table.convertRowIndexToModel(row);
                    if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                        String filename = (String) tableModel.getValueAt(modelRow, 1);
                        int line = (Integer) tableModel.getValueAt(modelRow, 2);
                        navigateTo(filename, line);
                    }
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        applyFilter();
    }

    // ---- QAnalysisModel.AnalysisListener ----

    @Override public void analysisComplete(QAnalysisModel m) {
        SwingUtilities.invokeLater(this::applyFilter);
    }

    // ---- Filter and display ----

    private void applyFilter() {
        tableModel.setRowCount(0);
        Map<String, List<ParseError>> filtered = model.getFilteredLintErrors();

        String currentFile = null;
        if (!allFiles) {
            Document sel = openDocsModel.getSelectedDocument();
            if (sel != null) currentFile = sel.getTitle();
        }

        // Apply file scope filter (UI-only concern) and populate table
        Map<String, List<ParseError>> display = new LinkedHashMap<>();
        int count = 0;

        for (Map.Entry<String, List<ParseError>> entry : filtered.entrySet()) {
            String filename = entry.getKey();
            if (!allFiles && (currentFile == null || !currentFile.equals(filename))) continue;
            display.put(filename, entry.getValue());
            for (ParseError e : entry.getValue()) {
                tableModel.addRow(new Object[]{
                    e.getSeverity(), filename, e.line,
                    e.ruleId != null ? e.ruleId : "",
                    e.message
                });
                count++;
            }
        }

        titleLabel.setText("Problems (" + count + ")");

        // Notify highlight listeners with severity-filtered results
        for (LintHighlightListener l : highlightListeners) {
            l.onLintResultsChanged(filtered); // full filtered set, not file-scoped
        }
    }

    void addLintHighlightListener(LintHighlightListener l) { highlightListeners.add(l); }

    private void navigateTo(String filename, int line) {
        try {
            for (Document doc : openDocsModel.getDocuments()) {
                if (doc.getTitle().equals(filename)) {
                    openDocsModel.setSelectedDocument(doc);
                    String content = doc.getContent();
                    if (content != null) {
                        doc.setCaratPosition(getPositionForLine(content, line));
                    }
                    return;
                }
            }
        } catch (Exception e) {
            LOG.warning("Failed to navigate: " + e.getMessage());
        }
    }

    private void showConfigDialog() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAll = new JButton("Select All");
        JButton selectNone = new JButton("Select None");
        btnPanel.add(selectAll);
        btnPanel.add(selectNone);
        panel.add(btnPanel, BorderLayout.NORTH);

        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        Map<String, JCheckBox> checkboxes = new LinkedHashMap<>();

        for (QLintRule rule : QLintRule.values()) {
            boolean enabled = !disabledRules.containsKey(rule.name()) || disabledRules.get(rule.name());
            JCheckBox cb = new JCheckBox(rule.name() + " (" + rule.severity + ") — " + rule.description, enabled);
            checkboxes.put(rule.name(), cb);
            checkPanel.add(cb);
        }

        selectAll.addActionListener(e -> checkboxes.values().forEach(cb -> cb.setSelected(true)));
        selectNone.addActionListener(e -> checkboxes.values().forEach(cb -> cb.setSelected(false)));

        JScrollPane scroll = new JScrollPane(checkPanel);
        scroll.setPreferredSize(new java.awt.Dimension(900, 600));
        panel.add(scroll, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, "Configure Lint Rules",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            disabledRules.clear();
            for (Map.Entry<String, JCheckBox> e : checkboxes.entrySet()) {
                disabledRules.put(e.getKey(), e.getValue().isSelected());
            }
            // Update model's lint config
            Map<String, String> cfgMap = new HashMap<>();
            for (Map.Entry<String, Boolean> e : disabledRules.entrySet()) {
                if (!e.getValue()) cfgMap.put(e.getKey(), "false");
            }
            model.setLintConfig(new QLintConfig(cfgMap.isEmpty() ? null : cfgMap));
        }
    }

    private static int getPositionForLine(String content, int line) {
        int currentLine = 1;
        for (int i = 0; i < content.length(); i++) {
            if (currentLine == line) return i;
            if (content.charAt(i) == '\n') currentLine++;
        }
        return content.length();
    }
}
