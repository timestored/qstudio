package com.timestored.sqldash;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.timestored.theme.Theme;

/**
 * Example showing how ANumeric inputs could be integrated into a preferences panel
 * similar to how JSpinner is used in AppearancePreferencesPanel.
 * 
 * This demonstrates the integration pattern for adding ANumeric inputs to forms.
 */
public class ANumericPreferencesExample extends JPanel {
    
    private static final long serialVersionUID = 1L;
    
    private final ANumericInput queryTimeoutInput;
    private final ANumericInput maxConnectionsInput;
    private final ANumericInput refreshIntervalInput;
    private final ANumericInput precisionInput;
    
    public ANumericPreferencesExample(Component container) {
        
        Box panel = Box.createVerticalBox();
        panel.setBorder(Theme.getCentreBorder());
        
        // Query timeout setting (0 to 300 seconds, step 5)
        queryTimeoutInput = new ANumericInput(30.0, 0.0, 300.0, 5.0);
        queryTimeoutInput.addNumericChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // In real implementation, would update preferences
                System.out.println("Query timeout changed to: " + queryTimeoutInput.getNumericValue());
            }
        });
        
        JLabel timeoutHelpLabel = new JLabel(Theme.CIcon.INFO.get());
        timeoutHelpLabel.setToolTipText("Maximum time to wait for query results. 0 = no timeout.");
        
        panel.add(Theme.getFormRow(queryTimeoutInput, "Query Timeout (seconds):", 
                "Maximum time to wait for query results before timing out", timeoutHelpLabel));
        
        // Maximum connections setting (1 to 50, step 1) 
        maxConnectionsInput = new ANumericInput(10, 1, 50, 1);
        panel.add(Theme.getFormRow(maxConnectionsInput, "Max Database Connections:",
                "Maximum number of concurrent database connections to maintain"));
        
        // Auto-refresh interval (100ms to 10000ms, step 100ms)
        refreshIntervalInput = new ANumericInput(1000, 100, 10000, 100);
        panel.add(Theme.getFormRow(refreshIntervalInput, "Auto-refresh Interval (ms):",
                "How often to refresh charts and data displays automatically"));
        
        // Numeric precision for display (0 to 10 decimal places, step 1)
        precisionInput = new ANumericInput(2, 0, 10, 1);
        panel.add(Theme.getFormRow(precisionInput, "Display Precision (decimal places):",
                "Number of decimal places to show in numeric displays"));
        
        panel.add(Box.createVerticalStrut(20));
        
        // Add examples section
        Box exampleBox = Box.createVerticalBox();
        exampleBox.setBorder(BorderFactory.createTitledBorder("Integration Benefits"));
        exampleBox.add(new JLabel("• Consistent numeric input with validation"));
        exampleBox.add(new JLabel("• Built-in min/max constraints like sliders"));
        exampleBox.add(new JLabel("• Up/down arrows for easy value adjustment"));
        exampleBox.add(new JLabel("• Integrates with existing Theme system"));
        exampleBox.add(new JLabel("• Reuses slider variables and patterns"));
        
        panel.add(exampleBox);
        
        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
    }
    
    /**
     * Get current preference values - would be used in real preference panel
     */
    public double getQueryTimeout() {
        return queryTimeoutInput.getNumericValue();
    }
    
    public int getMaxConnections() {
        return (int) maxConnectionsInput.getNumericValue();
    }
    
    public int getRefreshInterval() {
        return (int) refreshIntervalInput.getNumericValue();
    }
    
    public int getDisplayPrecision() {
        return (int) precisionInput.getNumericValue();
    }
    
    /**
     * Set preference values from stored settings
     */
    public void setQueryTimeout(double timeout) {
        queryTimeoutInput.setNumericValue(timeout);
    }
    
    public void setMaxConnections(int connections) {
        maxConnectionsInput.setNumericValue(connections);
    }
    
    public void setRefreshInterval(int interval) {
        refreshIntervalInput.setNumericValue(interval);
    }
    
    public void setDisplayPrecision(int precision) {
        precisionInput.setNumericValue(precision);
    }
}