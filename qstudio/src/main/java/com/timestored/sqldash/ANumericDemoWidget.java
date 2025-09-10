package com.timestored.sqldash;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.timestored.sqldash.theme.DBIcons;
import com.timestored.theme.Theme;

/**
 * Demo widget that showcases the ANumeric input component functionality.
 * This demonstrates various configurations and use cases for the ANumeric input.
 */
public class ANumericDemoWidget extends AbstractWidget {
    
    private JPanel panel;
    private final JTextArea resultArea;
    private final ANumericInput basicInput;
    private final ANumericInput constrainedInput;
    private final ANumericInput integerInput;
    private final ANumericInput decimalInput;
    
    public ANumericDemoWidget() {
        super("ANumeric Input Demo");
        
        // Initialize components
        resultArea = new JTextArea(5, 30);
        resultArea.setEditable(false);
        resultArea.setBorder(BorderFactory.createTitledBorder("Current Values"));
        
        // Create different ANumeric input examples
        basicInput = new ANumericInput();
        constrainedInput = new ANumericInput(50.0, 0.0, 100.0, 5.0);
        integerInput = new ANumericInput(10, -100, 100, 1);
        decimalInput = new ANumericInput(3.14, 0.0, 10.0, 0.01);
        
        // Add change listeners
        ChangeListener updateListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateResultDisplay();
            }
        };
        
        basicInput.addNumericChangeListener(updateListener);
        constrainedInput.addNumericChangeListener(updateListener);
        integerInput.addNumericChangeListener(updateListener);
        decimalInput.addNumericChangeListener(updateListener);
        
        // Initial display update
        updateResultDisplay();
    }
    
    @Override
    public JPanel getPanel() {
        if (panel == null) {
            panel = createPanel();
        }
        return panel;
    }
    
    @Override
    public void invalidatePanelCache() {
        panel = null;
    }
    
    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(Theme.getCentreBorder());
        
        // Create the form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("ANumeric Input Examples"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Basic input
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Basic Input:"), gbc);
        gbc.gridx = 1;
        formPanel.add(basicInput, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("(no constraints)"), gbc);
        
        // Constrained input (slider-like)
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Constrained (0-100):"), gbc);
        gbc.gridx = 1;
        formPanel.add(constrainedInput, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("(step: 5.0)"), gbc);
        
        // Integer input
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Integer (-100 to 100):"), gbc);
        gbc.gridx = 1;
        formPanel.add(integerInput, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("(step: 1)"), gbc);
        
        // Decimal input
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Decimal (0-10):"), gbc);
        gbc.gridx = 1;
        formPanel.add(decimalInput, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("(step: 0.01)"), gbc);
        
        // Add form panel to main panel
        mainPanel.add(formPanel, BorderLayout.NORTH);
        
        // Add result area
        mainPanel.add(resultArea, BorderLayout.CENTER);
        
        // Add info panel
        Box infoBox = Box.createVerticalBox();
        infoBox.setBorder(BorderFactory.createTitledBorder("Instructions"));
        infoBox.add(new JLabel("• Use arrow buttons or type values directly"));
        infoBox.add(new JLabel("• Values are constrained to specified ranges"));
        infoBox.add(new JLabel("• Step size determines increment/decrement amount"));
        infoBox.add(new JLabel("• Hover over inputs to see constraint tooltips"));
        
        mainPanel.add(infoBox, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private void updateResultDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Values:\n");
        sb.append("Basic Input: ").append(basicInput.getNumericValue()).append("\n");
        sb.append("Constrained Input: ").append(constrainedInput.getNumericValue()).append("\n");
        sb.append("Integer Input: ").append(integerInput.getNumericValue()).append("\n");
        sb.append("Decimal Input: ").append(decimalInput.getNumericValue()).append("\n\n");
        
        sb.append("Constraint Details:\n");
        sb.append("Basic: min=").append(formatValue(basicInput.getMinValue()))
          .append(", max=").append(formatValue(basicInput.getMaxValue())).append("\n");
        sb.append("Constrained: min=").append(constrainedInput.getMinValue())
          .append(", max=").append(constrainedInput.getMaxValue()).append("\n");
        sb.append("Integer: min=").append(integerInput.getMinValue())
          .append(", max=").append(integerInput.getMaxValue()).append("\n");
        sb.append("Decimal: min=").append(decimalInput.getMinValue())
          .append(", max=").append(decimalInput.getMaxValue()).append("\n");
        
        resultArea.setText(sb.toString());
    }
    
    private String formatValue(double value) {
        if (value == Double.NEGATIVE_INFINITY) {
            return "-∞";
        } else if (value == Double.POSITIVE_INFINITY) {
            return "+∞";
        } else {
            return String.valueOf(value);
        }
    }
    
    @Override
    public Collection<Queryable> getQueryables() {
        return Collections.emptyList();
    }
    
    @Override
    public void tabChanged(Queryable queryable, java.sql.ResultSet rs) {
        // This demo widget doesn't use database queries
    }
    
    @Override
    public void queryError(Queryable queryable, Exception e) {
        // This demo widget doesn't use database queries
    }
    
    @Override
    public Icon getIcon() {
        return DBIcons.SPINNER.get16();
    }
    
    @Override
    public boolean isRenderLargeDataSets() {
        return false; // This demo widget doesn't render large data sets
    }
}