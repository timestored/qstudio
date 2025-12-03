package com.timestored.sqldash;

import java.awt.BorderLayout;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.base.Preconditions;
import com.timestored.sqldash.chart.ChartAppearanceConfig;
import com.timestored.sqldash.chart.JFreeChartConfigEditor;

/**
 * Thin wrapper panel that integrates the new collapsible JFreeChart configuration editor.
 * It detects numeric series from the ResultSet and feeds them to the unified editor.
 * Changes to the configuration are applied instantly to update the chart.
 */
public class ChartAppearancePanel extends JPanel implements ChartWidget.Listener {

    private static final long serialVersionUID = 1L;

    private final ChartWidget chartWidget;
    private JFreeChartConfigEditor editor;
    private ResultSet lastResultSet;

    public ChartAppearancePanel(ChartWidget chartWidget) {
        this.chartWidget = Preconditions.checkNotNull(chartWidget);
        setLayout(new BorderLayout());
        
        // Register as a listener to get notified of view strategy changes
        chartWidget.addListener(this);
    }

    /**
     * Update the editor UI based on the latest ResultSet.
     * Called whenever new data arrives.
     * Will show/hide the panel based on whether the current ViewStrategy supports configuration.
     */
    public void updateSeriesConfig(ResultSet rs) {
        this.lastResultSet = rs;
        rebuildEditor();
    }
    
    /**
     * Rebuild the editor UI. Called when data changes or view strategy changes.
     */
    private void rebuildEditor() {
        removeAll();
        
        // Check if current view strategy supports appearance config
        boolean supported = chartWidget.getViewStrategy().supportsAppearanceConfig();
        setVisible(supported);
        
        if (!supported) {
            revalidate();
            repaint();
            return;
        }

        if (lastResultSet == null) {
            add(new JLabel("Run a query to configure chart appearance"), BorderLayout.CENTER);
            revalidate(); 
            repaint();
            return;
        }

        try {
            ResultSetMetaData meta = lastResultSet.getMetaData();
            int colCount = meta.getColumnCount();
            List<String> numericColumns = new ArrayList<>();

            // Detect numeric columns → series list
            for (int i = 1; i <= colCount; i++) {
                int type = meta.getColumnType(i);
                if (isNumericType(type)) {
                    numericColumns.add(meta.getColumnLabel(i));
                }
            }

            if (numericColumns.isEmpty()) {
                add(new JLabel("No numeric series found"), BorderLayout.CENTER);
            } else {
                ChartAppearanceConfig config = chartWidget.getAppearanceConfig();
                editor = new JFreeChartConfigEditor(config, numericColumns);
                editor.addConfigChangeListener(() -> {
                    chartWidget.setAppearanceConfig(editor.getConfig());
                });

                add(editor, BorderLayout.CENTER);
            }

        } catch (SQLException e) {
            add(new JLabel("Error reading columns"), BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }
    
    /**
     * Called when the widget configuration changes (e.g., view strategy changed).
     * We only rebuild the editor if the view strategy support status changed,
     * NOT when appearance config changes (to avoid collapsing sections and stealing focus).
     */
    @Override
    public void configChanged(Widget app) {
        // Only rebuild if the support status changed (e.g., switched chart type)
        boolean nowSupported = chartWidget.getViewStrategy().supportsAppearanceConfig();
        boolean wasVisible = isVisible();
        
        if (nowSupported != wasVisible) {
            // View strategy changed between supported/unsupported - rebuild
            SwingUtilities.invokeLater(() -> {
                rebuildEditor();
            });
        }
        // Don't rebuild when just the appearance config changes - that would
        // collapse all sections and steal focus from the user's editing
    }

    /**
     * Push UI changes back to the ChartAppearanceConfig and notify widget.
     */
    public void applyConfiguration() {
        if (editor != null) {
            editor.applyToConfig();
            chartWidget.setAppearanceConfig(editor.getConfig());
        }
    }

    public boolean isConfigurationSupported() {
        return chartWidget.getViewStrategy().supportsAppearanceConfig();
    }

    private boolean isNumericType(int sqlType) {
        return sqlType == java.sql.Types.BIGINT || sqlType == java.sql.Types.DECIMAL
                || sqlType == java.sql.Types.DOUBLE || sqlType == java.sql.Types.FLOAT
                || sqlType == java.sql.Types.INTEGER || sqlType == java.sql.Types.NUMERIC
                || sqlType == java.sql.Types.REAL || sqlType == java.sql.Types.SMALLINT
                || sqlType == java.sql.Types.TINYINT;
    }
}
