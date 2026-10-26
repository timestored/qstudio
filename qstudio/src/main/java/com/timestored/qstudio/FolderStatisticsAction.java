package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.swingx.JXTable;

import com.timestored.docs.OpenDocumentsModel;
import com.timestored.qdoc.parser.*;
import com.timestored.theme.Theme;

/**
 * Action that shows code quality statistics for the currently open folder.
 * Launched from Tools menu.
 */
public class FolderStatisticsAction extends AbstractAction {

    private final OpenDocumentsModel openDocsModel;

    public FolderStatisticsAction(OpenDocumentsModel openDocsModel) {
        super("Folder Statistics...");
        this.openDocsModel = openDocsModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File folder = openDocsModel.getSelectedFolder();
        if (folder == null || !folder.isDirectory()) {
            JOptionPane.showMessageDialog(null, "No folder selected. Open a folder first.");
            return;
        }

        // Run analysis in background
        SwingWorker<StatsResult, Void> worker = new SwingWorker<StatsResult, Void>() {
            @Override protected StatsResult doInBackground() {
                return analyzeFolder(folder);
            }
            @Override protected void done() {
                try {
                    showDialog(get(), folder);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private static StatsResult analyzeFolder(File folder) {
        List<Path> qFiles;
        try {
            qFiles = Files.walk(folder.toPath())
                .filter(p -> p.toString().endsWith(".q"))
                .filter(p -> !p.toString().contains(".git"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            return new StatsResult(Collections.emptyList(), Collections.emptyMap());
        }

        List<QMetricsReport> metrics = new ArrayList<>();
        Map<String, Integer> lintCounts = new LinkedHashMap<>();

        for (Path p : qFiles) {
            try {
                String code = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                String name = p.getFileName().toString();
                metrics.add(QCodeMetrics.analyze(code, name));

                for (ParseError err : QLinter.lint(code, name)) {
                    String rule = err.ruleId != null ? err.ruleId : "PARSE_ERROR";
                    lintCounts.merge(rule, 1, Integer::sum);
                }
            } catch (Exception e) { /* skip */ }
        }

        return new StatsResult(metrics, lintCounts);
    }

    private void showDialog(StatsResult result, File folder) {
        JDialog dialog = new JDialog((JFrame) null, "Folder Statistics: " + folder.getName(), false);
        dialog.setLayout(new BorderLayout());
        dialog.setPreferredSize(new Dimension(800, 500));

        JTabbedPane tabs = new JTabbedPane();

        // Summary tab
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.add(new JLabel(QMetricsReport.toSummaryHtml(result.metrics, folder.getName())), BorderLayout.CENTER);
        tabs.addTab("Summary", summaryPanel);

        // Per-file tab
        String[] cols = {"File", "Lines", "Code", "Comments", "Functions", "Max Lines", "Max Complexity", "Lint Issues"};
        DefaultTableModel fileModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (QMetricsReport m : result.metrics) {
            fileModel.addRow(m.toTableRow());
        }
        JXTable fileTable = Theme.getStripedTable(fileModel);
        tabs.addTab("Per File", new JScrollPane(fileTable));

        // Lint rules tab
        String[] lintCols = {"Rule", "Count"};
        DefaultTableModel lintModel = new DefaultTableModel(lintCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        result.lintCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> lintModel.addRow(new Object[]{e.getKey(), e.getValue()}));
        JXTable lintTable = Theme.getStripedTable(lintModel);
        tabs.addTab("Lint Rules", new JScrollPane(lintTable));

        dialog.add(tabs, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private static class StatsResult {
        final List<QMetricsReport> metrics;
        final Map<String, Integer> lintCounts;
        StatsResult(List<QMetricsReport> metrics, Map<String, Integer> lintCounts) {
            this.metrics = metrics;
            this.lintCounts = lintCounts;
        }
    }
}
