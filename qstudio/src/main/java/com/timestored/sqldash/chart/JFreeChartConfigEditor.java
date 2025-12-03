package com.timestored.sqldash.chart;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import com.timestored.blueprint.BlueprintIcon;
import com.timestored.sqldash.chart.ChartAppearanceConfig.AxisPosition;
import com.timestored.sqldash.chart.ChartAppearanceConfig.LegendPosition;
import com.timestored.sqldash.chart.ChartAppearanceConfig.LineStyle;
import com.timestored.sqldash.chart.ChartAppearanceConfig.MarkerShape;

/**
 * Chart config editor with:
 *  - Series Appearance section (colors, axis, visibility)
 *  - Chart Settings (title, background, anti-aliasing)
 *  - Plot Settings (background, gridlines)
 *  - Legend Settings (visible, position, background)
 *  - Tooltip Settings (enabled)
 *
 * This panel is UI-only; it reads/writes a ChartAppearanceConfig
 * and assumes the outer code applies it to a JFreeChart / ChartWidget.
 */
public class JFreeChartConfigEditor extends JPanel {

    /** Minimum row height for series rows */
    private static final int SERIES_ROW_HEIGHT = 32;

    /** Listener interface for config change notifications */
    public interface ConfigChangeListener {
        void configChanged();
    }

    private final ChartAppearanceConfig config;
    private final List<String> seriesNames;
    private final List<ConfigChangeListener> changeListeners = new ArrayList<>();

    // Chart settings fields
    private JTextField chartTitleField;
    private JButton chartBackgroundBtn;
    private JCheckBox antiAliasingCheckbox;

    // Plot settings fields
    private JButton plotBackgroundBtn;
    private JButton plotOutlineBtn;
    private JCheckBox domainGridlinesCheckbox;
    private JCheckBox rangeGridlinesCheckbox;
    private JButton gridlineColorBtn;

    // Legend settings fields
    private JCheckBox legendVisibleCheckbox;
    private JComboBox<LegendPosition> legendPositionCombo;
    private JButton legendBackgroundBtn;

    // Tooltip settings fields
    private JCheckBox tooltipEnabledCheckbox;

    // Line/Shape settings fields
    private JCheckBox shapesVisibleCheckbox;
    private JCheckBox linesVisibleCheckbox;
    private JSpinner lineWidthSpinner;
    private JComboBox<LineStyle> lineStyleCombo;

    // Bar chart settings fields
    private JSpinner barMarginSpinner;
    private JCheckBox itemLabelsCheckbox;

    // Axis settings fields
    private JTextField domainAxisLabelField;
    private JTextField rangeAxisLabelField;
    private JCheckBox autoRangeCheckbox;
    private JSpinner rangeMinSpinner;
    private JSpinner rangeMaxSpinner;

    // series rows
    private final List<SeriesRow> seriesRows = new ArrayList<>();

    public JFreeChartConfigEditor(ChartAppearanceConfig config, List<String> seriesNames) {
        super(new BorderLayout());
        this.config = (config != null) ? config : new ChartAppearanceConfig();
        this.seriesNames = (seriesNames != null) ? seriesNames : new ArrayList<>();

        // TABS
        JTabbedPane tabs = new JTabbedPane();

        // --- SERIES TAB ---
        JPanel seriesContent = new JPanel();
        seriesContent.setLayout(new BoxLayout(seriesContent, BoxLayout.Y_AXIS));

        seriesContent.add(buildSection("Series Appearance", seriesAppearancePanel(), true));
        seriesContent.add(buildSection("Line/Shape Settings", lineShapeSettingsPanel(), true));
        seriesContent.add(buildSection("Bar Chart Settings", barChartSettingsPanel(), true));

        JScrollPane seriesScroll = new JScrollPane(seriesContent);
        tabs.addTab("Series", seriesScroll);

        // --- CHART TAB ---
        JPanel chartContent = new JPanel();
        chartContent.setLayout(new BoxLayout(chartContent, BoxLayout.Y_AXIS));

        chartContent.add(buildSection("Axis Settings", axisSettingsPanel(), true));
        chartContent.add(buildSection("Chart Settings", chartSettingsPanel(), true));
        chartContent.add(buildSection("Plot Settings", plotSettingsPanel(), true));
        chartContent.add(buildSection("Legend Settings", legendSettingsPanel(), true));
        chartContent.add(buildSection("Tooltip Settings", tooltipSettingsPanel(), true));

        JScrollPane chartScroll = new JScrollPane(chartContent);
        tabs.addTab("Chart", chartScroll);

        add(tabs, BorderLayout.CENTER);

        // Make fonts slightly smaller and more compact
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new JLabel().getFont();
        Font small = base.deriveFont(Math.max(10f, base.getSize2D() - 1f));
        setFontRecursively(this, small);

        loadFromConfig();
    }

    // ---------------------------------------------------------------------
    // ConfigChangeListener API
    // ---------------------------------------------------------------------

    /** Add a listener that will be notified when config changes */
    public void addConfigChangeListener(ConfigChangeListener listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    /** Remove a config change listener */
    public void removeConfigChangeListener(ConfigChangeListener listener) {
        changeListeners.remove(listener);
    }

    /** Notify all listeners that the config has changed */
    private void fireConfigChanged() {
        applyToConfig();
        for (ConfigChangeListener l : changeListeners) {
            l.configChanged();
        }
    }

    // Recursive font helper
    private static void setFontRecursively(Component c, Font f) {
        c.setFont(f);
        if (c instanceof Container)  {
            for (Component child : ((Container)c).getComponents()) {
                setFontRecursively(child, f);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Section panel (always expanded, no collapse toggle)
    // ---------------------------------------------------------------------
    private JPanel buildSection(String title, JPanel body, boolean startExpanded) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new CompoundBorder(
                new EmptyBorder(3, 0, 3, 0),
                new LineBorder(UIManager.getColor("Component.borderColor") != null ?
                        UIManager.getColor("Component.borderColor") : Color.GRAY)
        ));
        // Ensure section expands to fill available width in BoxLayout
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Simple header label - no toggle, always expanded
        JLabel headerLabel = new JLabel("  " + title);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        headerLabel.setBorder(new EmptyBorder(2, 3, 2, 3));
        headerLabel.setForeground(UIManager.getColor("Label.foreground"));

        JPanel header = new JPanel(new BorderLayout());
        Color headerBg = UIManager.getColor("TableHeader.background");
        if (headerBg == null) {
            headerBg = UIManager.getColor("Panel.background");
        }
        header.setBackground(headerBg);
        header.add(headerLabel, BorderLayout.CENTER);

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(body, BorderLayout.CENTER);
        return wrapper;
    }

    // ---------------------------------------------------------------------
    // Row utilities
    // ---------------------------------------------------------------------
    private JPanel buildRow(String label, JComponent editor) {
        JPanel row = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0; c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(2, 2, 2, 6); // tighter
        c.anchor = GridBagConstraints.WEST;

        JLabel lbl = new JLabel(label);
        row.add(lbl, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);
        row.add(editor, c);

        return row;
    }
    
    

    private JPanel sectionPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(1, 4, 1, 4)); // tighter
        return p;
    }

    // ---------------------------------------------------------------------
    // Color button helpers + reset icon
    // ---------------------------------------------------------------------

    /**
     * Small curved-arrow icon used for reset buttons.
     */
    private static class ResetIcon implements Icon {
        private final int size;

        ResetIcon(int size) {
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color col = c.isEnabled() ? c.getForeground() : UIManager.getColor("Label.disabledForeground");
            if (col == null) col = Color.GRAY;
            g2.setColor(col);

            int s = size - 4;
            int cx = x + size / 2;
            int cy = y + size / 2;

            g2.setStroke(new BasicStroke(1.3f));
            g2.drawArc(cx - s / 2, cy - s / 2, s, s, 45, 270);

            int ax = cx + s / 2 - 1;
            int ay = cy - s / 2 + 2;
            g2.drawLine(ax, ay, ax - 3, ay);
            g2.drawLine(ax, ay, ax, ay + 3);

            g2.dispose();
        }
    }

    private JButton createResetButton(Runnable onReset) {
        JButton b = new JButton(new ResetIcon(10));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusable(false);
        b.setBorderPainted(true);
        b.setPreferredSize(new Dimension(18, 18));
        b.setContentAreaFilled(false);
        b.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        b.addActionListener(e -> onReset.run());
        return b;
    }

    private void resetColorButton(JButton btn) {
        // clear explicit color flag
        btn.putClientProperty("colorSet", Boolean.FALSE);
        Color panelBg = UIManager.getColor("Panel.background");
        if (panelBg == null) panelBg = Color.WHITE;
        btn.setBackground(panelBg);
        btn.setText(""); // display just flat panel color
        fireConfigChanged();
    }

    /**
     * Wrap a color button and a tiny reset button beside it.
     */
    private JComponent wrapColorWithReset(JButton colorBtn) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p.setOpaque(false);
        JButton reset = createResetButton(() -> resetColorButton(colorBtn));
        p.add(colorBtn);
        p.add(reset);
        return p;
    }

    /**
     * Create and setup a color button with action listener.
     *
     * Uses JButton.putClientProperty("colorSet", Boolean) to track whether a color
     * was explicitly set by the user. This is necessary because:
     * 1. We display some default color when no color is configured
     * 2. That default is also a valid user selection
     * 3. We need to distinguish "no color set" (return null) from "user chose color"
     *
     * When colorSet=false/null, getButtonColor() returns null (use theme default).
     * When colorSet=true, getButtonColor() returns the actual background color.
     */
    private JButton createColorButton(Color initial) {
        JButton btn = new JButton();
        Color panelBg = UIManager.getColor("Panel.background");
        if (panelBg == null) panelBg = Color.WHITE;
        btn.setBackground(initial != null ? initial : panelBg);
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setText("");
        // Track whether color was explicitly set (vs. showing default)
        btn.putClientProperty("colorSet", initial != null);
        btn.addActionListener(e -> {
            showLiveColorChooser(btn, "Choose Color");
        });
        return btn;
    }

    /**
     * Show a color chooser dialog that updates the chart live as colors are selected.
     * The chart updates immediately when a color is picked, without needing to close the dialog.
     */
    private void showLiveColorChooser(JButton btn, String title) {
        Color originalColor = btn.getBackground();
        // Store original colorSet state before dialog opens
        Boolean originalColorSet = (Boolean) btn.getClientProperty("colorSet");

        JColorChooser chooser = new JColorChooser(originalColor);

        // Add change listener for live preview - updates chart as user selects colors
        chooser.getSelectionModel().addChangeListener(e -> {
            Color newColor = chooser.getColor();
            btn.setBackground(newColor);
            btn.putClientProperty("colorSet", Boolean.TRUE);
            fireConfigChanged();
        });

        // Create dialog with OK/Cancel buttons
        JDialog dialog = JColorChooser.createDialog(
                this,
                title,
                true, // modal
                chooser,
                // OK action - color is already applied via change listener
                okEvent -> {
                    // nothing extra
                },
                // Cancel action - restore original color and colorSet state
                cancelEvent -> {
                    btn.setBackground(originalColor);
                    btn.putClientProperty("colorSet", originalColorSet);
                    fireConfigChanged();
                }
        );

        dialog.setVisible(true);
    }

    /**
     * Get color from button, returning null if no color was explicitly set.
     * This allows distinguishing between "use default" and "user chose specific color".
     */
    private Color getButtonColor(JButton btn) {
        Boolean colorSet = (Boolean) btn.getClientProperty("colorSet");
        if (colorSet == null || !colorSet) {
            return null;
        }
        return btn.getBackground();
    }

    // ---------------------------------------------------------------------
    // Line/Shape Settings (for line, timeseries, step, area charts)
    // ---------------------------------------------------------------------
    private JPanel lineShapeSettingsPanel() {
        JPanel p = sectionPanel();

        shapesVisibleCheckbox = new JCheckBox("Show Markers", config.isShapesVisible());
        shapesVisibleCheckbox.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Data Point Markers:", shapesVisibleCheckbox));

        linesVisibleCheckbox = new JCheckBox("Show Lines", config.isLinesVisible());
        linesVisibleCheckbox.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Connecting Lines:", linesVisibleCheckbox));

        SpinnerNumberModel lineWidthModel = new SpinnerNumberModel(
                (double) config.getDefaultLineWidth(), 0.5, 10.0, 0.5);
        lineWidthSpinner = new JSpinner(lineWidthModel);
        lineWidthSpinner.addChangeListener(e -> fireConfigChanged());
        p.add(buildRow("Line Width:", lineWidthSpinner));

        lineStyleCombo = new JComboBox<>(LineStyle.values());
        lineStyleCombo.setSelectedItem(config.getDefaultLineStyle());
        lineStyleCombo.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Line Style:", lineStyleCombo));

        return p;
    }

    // ---------------------------------------------------------------------
    // Bar Chart Settings
    // ---------------------------------------------------------------------
    private JPanel barChartSettingsPanel() {
        JPanel p = sectionPanel();

        SpinnerNumberModel marginModel = new SpinnerNumberModel(
                config.getBarItemMargin(), 0.0, 0.5, 0.05);
        barMarginSpinner = new JSpinner(marginModel);
        barMarginSpinner.addChangeListener(e -> fireConfigChanged());
        p.add(buildRow("Bar Spacing:", barMarginSpinner));

        itemLabelsCheckbox = new JCheckBox("Show Values", config.isItemLabelsVisible());
        itemLabelsCheckbox.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Item Labels:", itemLabelsCheckbox));

        return p;
    }

    // ---------------------------------------------------------------------
    // Axis Settings
    // ---------------------------------------------------------------------
    private JPanel axisSettingsPanel() {
        JPanel p = sectionPanel();

        domainAxisLabelField = new JTextField(config.getDomainAxisLabel() != null ? config.getDomainAxisLabel() : "");
        domainAxisLabelField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { fireConfigChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { fireConfigChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { fireConfigChanged(); }
        });
        p.add(buildRow("X-Axis Label:", domainAxisLabelField));

        rangeAxisLabelField = new JTextField(config.getRangeAxisLabel() != null ? config.getRangeAxisLabel() : "");
        rangeAxisLabelField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { fireConfigChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { fireConfigChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { fireConfigChanged(); }
        });
        p.add(buildRow("Y-Axis Label:", rangeAxisLabelField));

        autoRangeCheckbox = new JCheckBox("Auto", config.isRangeAxisAutoRange());
        autoRangeCheckbox.addActionListener(e -> {
            boolean auto = autoRangeCheckbox.isSelected();
            rangeMinSpinner.setEnabled(!auto);
            rangeMaxSpinner.setEnabled(!auto);
            fireConfigChanged();
        });
        p.add(buildRow("Y-Axis Auto Range:", autoRangeCheckbox));

        Double min = config.getRangeAxisMin();
        SpinnerNumberModel minModel = new SpinnerNumberModel(
                min != null ? min : 0.0, -1000000.0, 1000000.0, 1.0);
        rangeMinSpinner = new JSpinner(minModel);
        rangeMinSpinner.setEnabled(!config.isRangeAxisAutoRange());
        rangeMinSpinner.addChangeListener(e -> fireConfigChanged());
        p.add(buildRow("Y-Axis Min:", rangeMinSpinner));

        Double max = config.getRangeAxisMax();
        SpinnerNumberModel maxModel = new SpinnerNumberModel(
                max != null ? max : 100.0, -1000000.0, 1000000.0, 1.0);
        rangeMaxSpinner = new JSpinner(maxModel);
        rangeMaxSpinner.setEnabled(!config.isRangeAxisAutoRange());
        rangeMaxSpinner.addChangeListener(e -> fireConfigChanged());
        p.add(buildRow("Y-Axis Max:", rangeMaxSpinner));

        return p;
    }

    // ---------------------------------------------------------------------
    // Chart Settings - title, background, anti-aliasing
    // ---------------------------------------------------------------------
    private JPanel chartSettingsPanel() {
        JPanel p = sectionPanel();

        chartTitleField = new JTextField("");
        chartTitleField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { fireConfigChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { fireConfigChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { fireConfigChanged(); }
        });
        p.add(buildRow("Title Text:", chartTitleField));

        chartBackgroundBtn = createColorButton(config.getChartBackgroundColor());
        JComponent chartBgRow = wrapColorWithReset(chartBackgroundBtn);
        p.add(buildRow("Background:", chartBgRow));

        antiAliasingCheckbox = new JCheckBox("Enabled", config.isAntiAliasing());
        antiAliasingCheckbox.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Anti-Aliasing:", antiAliasingCheckbox));

        return p;
    }

    // ---------------------------------------------------------------------
    // Plot Settings - background, outline, gridlines
    // ---------------------------------------------------------------------
    private JPanel plotSettingsPanel() {
        JPanel p = sectionPanel();

        plotBackgroundBtn = createColorButton(config.getPlotBackgroundColor());
        p.add(buildRow("Plot Background:", wrapColorWithReset(plotBackgroundBtn)));

        plotOutlineBtn = createColorButton(config.getPlotOutlineColor());
        p.add(buildRow("Plot Outline:", wrapColorWithReset(plotOutlineBtn)));

        domainGridlinesCheckbox = new JCheckBox("Visible", config.isDomainGridlinesVisible());
        domainGridlinesCheckbox.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Domain Gridlines:", domainGridlinesCheckbox));

        rangeGridlinesCheckbox = new JCheckBox("Visible", config.isRangeGridlinesVisible());
        rangeGridlinesCheckbox.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Range Gridlines:", rangeGridlinesCheckbox));

        gridlineColorBtn = createColorButton(config.getGridlineColor());
        p.add(buildRow("Gridline Color:", wrapColorWithReset(gridlineColorBtn)));

        return p;
    }

    // ---------------------------------------------------------------------
    // Legend Settings - visible, position, background
    // ---------------------------------------------------------------------
    private JPanel legendSettingsPanel() {
        JPanel p = sectionPanel();

        legendVisibleCheckbox = new JCheckBox("Visible", config.isLegendVisible());
        legendVisibleCheckbox.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Legend Visible:", legendVisibleCheckbox));

        legendPositionCombo = new JComboBox<>(LegendPosition.values());
        legendPositionCombo.setSelectedItem(config.getLegendPosition());
        legendPositionCombo.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Legend Position:", legendPositionCombo));

        legendBackgroundBtn = createColorButton(config.getLegendBackgroundColor());
        p.add(buildRow("Legend Background:", wrapColorWithReset(legendBackgroundBtn)));

        return p;
    }

    // ---------------------------------------------------------------------
    // Tooltip Settings - enabled
    // ---------------------------------------------------------------------
    private JPanel tooltipSettingsPanel() {
        JPanel p = sectionPanel();

        tooltipEnabledCheckbox = new JCheckBox("Enabled", config.isTooltipEnabled());
        tooltipEnabledCheckbox.addActionListener(e -> fireConfigChanged());
        p.add(buildRow("Tooltips Enabled:", tooltipEnabledCheckbox));

        return p;
    }

    // ---------------------------------------------------------------------
    // Series appearance section (the main functionality)
    // ---------------------------------------------------------------------
    private JPanel seriesAppearancePanel() {
        JPanel p = sectionPanel();

        if (seriesNames.isEmpty()) {
            p.add(new JLabel("No series configured."));
            return p;
        }

        // header row with better sizing
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        header.setOpaque(false);
        header.add(labelFixed("Series", 100));
        header.add(labelFixed("Visible", 50));
        header.add(labelFixed("Color", 100));
        header.add(labelFixed("Axis", 70));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        p.add(header);
        p.add(Box.createVerticalStrut(3));

        for (String s : seriesNames) {
            SeriesRow row = new SeriesRow(s, config);
            seriesRows.add(row);
            p.add(row);
            p.add(Box.createVerticalStrut(3));
        }

        return p;
    }

    private JLabel labelFixed(String text, int w) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    // ---------------------------------------------------------------------
    // SeriesRow: compact inline editor for a single series
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // SeriesRow: compact inline editor for a single series
    // ---------------------------------------------------------------------
    private class SeriesRow extends JPanel {
        private final String seriesName;
        private final JButton visibilityBtn;
        private boolean isVisible;
        private final JButton colorBtn;
        private final JComboBox<AxisPosition> axisBox;
        private Color selectedColor;
        private boolean colorWasSet; // Track if user explicitly set a color

        SeriesRow(String seriesName, ChartAppearanceConfig cfg) {
            this.seriesName = seriesName;
            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 2));
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, SERIES_ROW_HEIGHT + 6));

            // name
            JLabel nameLbl = new JLabel(seriesName);
            nameLbl.setPreferredSize(new Dimension(100, SERIES_ROW_HEIGHT));
            add(nameLbl);

            // visibility toggle button with eye icon
            isVisible = cfg.isSeriesVisible(seriesName);
            visibilityBtn = new JButton();
            visibilityBtn.setPreferredSize(new Dimension(32, 32));
            visibilityBtn.setFocusable(false);
            visibilityBtn.setBorderPainted(true);
            visibilityBtn.setContentAreaFilled(false);
            updateVisibilityIcon();
            visibilityBtn.addActionListener(e -> {
                isVisible = !isVisible;
                updateVisibilityIcon();
                fireConfigChanged();
            });
            visibilityBtn.setToolTipText(isVisible ? "Click to hide series" : "Click to show series");
            add(visibilityBtn);

            // color button - show default if no color set
            selectedColor = cfg.getSeriesColor(seriesName);
            colorWasSet = (selectedColor != null);
            colorBtn = new JButton();
            Color panelBg = UIManager.getColor("Panel.background");
            if (panelBg == null) panelBg = Color.LIGHT_GRAY;
            colorBtn.setBackground(selectedColor != null ? selectedColor : panelBg);
            colorBtn.setOpaque(true);
            colorBtn.setPreferredSize(new Dimension(30, 30));
            colorBtn.setBorderPainted(true);
            colorBtn.setText(selectedColor == null ? "" : "");
            colorBtn.addActionListener(e -> showSeriesColorChooser());

            // reset button
            JButton resetBtn = createResetButton(() -> {
                selectedColor = null;
                colorWasSet = false;
                Color bg = UIManager.getColor("Panel.background");
                if (bg == null) bg = Color.LIGHT_GRAY;
                colorBtn.setBackground(bg);
                colorBtn.setText("");
                fireConfigChanged();
            });
            resetBtn.setToolTipText("Reset to default color");
            resetBtn.setPreferredSize(new Dimension(18, 18));

            JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            colorPanel.setOpaque(false);
            colorPanel.add(colorBtn);
            colorPanel.add(resetBtn);
            add(colorPanel);

            // axis (LEFT/RIGHT)
            axisBox = new JComboBox<>(AxisPosition.values());
            axisBox.setSelectedItem(cfg.getSeriesAxisPosition(seriesName));
            axisBox.addActionListener(e -> fireConfigChanged());
            add(axisBox);
        }
        
        private void updateVisibilityIcon() {
            Color iconColor = UIManager.getColor("Label.foreground");
            if (iconColor == null) iconColor = Color.BLACK;
            if (isVisible) {
                visibilityBtn.setIcon(BlueprintIcon.EYE_ON.get16(iconColor));
                visibilityBtn.setToolTipText("Click to hide series");
            } else {
                visibilityBtn.setIcon(BlueprintIcon.EYE_OFF.get16(iconColor));
                visibilityBtn.setToolTipText("Click to show series");
            }
        }


        /**
         * Show live color chooser for series color - updates chart in real-time.
         */
        private void showSeriesColorChooser() {
            Color originalColor = selectedColor;
            boolean originalWasSet = colorWasSet;

            Color startColor;
            if (selectedColor != null) {
                startColor = selectedColor;
            } else {
                Color panelBg = UIManager.getColor("Panel.background");
                startColor = panelBg != null ? panelBg : Color.GRAY;
            }

            JColorChooser chooser = new JColorChooser(startColor);

            // Live preview - update chart as user selects colors
            chooser.getSelectionModel().addChangeListener(e -> {
                selectedColor = chooser.getColor();
                colorWasSet = true;
                colorBtn.setBackground(selectedColor);
                colorBtn.setText("");
                fireConfigChanged();
            });

            JDialog dialog = JColorChooser.createDialog(
                    JFreeChartConfigEditor.this,
                    "Color for " + seriesName,
                    true,
                    chooser,
                    // OK - color already applied
                    okEvent -> { },
                    // Cancel - restore original
                    cancelEvent -> {
                        selectedColor = originalColor;
                        colorWasSet = originalWasSet;
                        Color bg = selectedColor != null
                                ? selectedColor
                                : (UIManager.getColor("Panel.background") != null
                                        ? UIManager.getColor("Panel.background")
                                        : Color.LIGHT_GRAY);
                        colorBtn.setBackground(bg);
                        colorBtn.setText("");
                        fireConfigChanged();
                    }
            );

            dialog.setVisible(true);
        }

        void applyToConfig(ChartAppearanceConfig cfg) {
            // visibility
            cfg.setSeriesVisible(seriesName, isVisible);

            // color - only set if user explicitly chose a color
            if (colorWasSet) {
                cfg.setSeriesColor(seriesName, selectedColor);
            } else {
                cfg.setSeriesColor(seriesName, null);
            }

            // axis
            AxisPosition pos = (AxisPosition) axisBox.getSelectedItem();
            cfg.setSeriesAxisPosition(seriesName, pos);
        }
    }

    // ---------------------------------------------------------------------
    // Public API: load/apply
    // ---------------------------------------------------------------------

    /** Load config values into UI */
    public void loadFromConfig() {
        if (chartTitleField != null) {
            chartTitleField.setText(config.getChartTitle());
        }
        // series rows are already initialized with config in constructor
    }


    /** Push UI values back into the given config instance */
    public void applyToConfig() {

        // Chart settings
        config.setChartTitle(chartTitleField.getText());
        config.setChartBackgroundColor(getButtonColor(chartBackgroundBtn));
        config.setAntiAliasing(antiAliasingCheckbox.isSelected());

        // Plot settings
        config.setPlotBackgroundColor(getButtonColor(plotBackgroundBtn));
        config.setPlotOutlineColor(getButtonColor(plotOutlineBtn));
        config.setDomainGridlinesVisible(domainGridlinesCheckbox.isSelected());
        config.setRangeGridlinesVisible(rangeGridlinesCheckbox.isSelected());
        config.setGridlineColor(getButtonColor(gridlineColorBtn));

        // Legend settings
        config.setLegendVisible(legendVisibleCheckbox.isSelected());
        config.setLegendPosition((LegendPosition) legendPositionCombo.getSelectedItem());
        config.setLegendBackgroundColor(getButtonColor(legendBackgroundBtn));

        // Tooltip settings
        config.setTooltipEnabled(tooltipEnabledCheckbox.isSelected());

        // Line/Shape settings
        config.setShapesVisible(shapesVisibleCheckbox.isSelected());
        config.setLinesVisible(linesVisibleCheckbox.isSelected());
        config.setDefaultLineWidth(((Number) lineWidthSpinner.getValue()).floatValue());
        config.setDefaultLineStyle((LineStyle) lineStyleCombo.getSelectedItem());

        // Bar chart settings
        config.setBarItemMargin(((Number) barMarginSpinner.getValue()).doubleValue());
        config.setItemLabelsVisible(itemLabelsCheckbox.isSelected());

        // Axis settings
        String domainLabel = domainAxisLabelField.getText();
        config.setDomainAxisLabel(domainLabel.isEmpty() ? null : domainLabel);

        String rangeLabel = rangeAxisLabelField.getText();
        config.setRangeAxisLabel(rangeLabel.isEmpty() ? null : rangeLabel);

        config.setRangeAxisAutoRange(autoRangeCheckbox.isSelected());
        config.setRangeAxisMin(((Number) rangeMinSpinner.getValue()).doubleValue());
        config.setRangeAxisMax(((Number) rangeMaxSpinner.getValue()).doubleValue());

        // Series settings
        for (SeriesRow row : seriesRows) {
            row.applyToConfig(config);
        }
    }

    

    public ChartAppearanceConfig getConfig() {
        return config;
    }
}
