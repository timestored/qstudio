package com.timestored.sqldash;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;
import com.timestored.theme.Theme;

/**
 * Test application to demonstrate the ANumeric input component.
 * This creates a standalone window showing different ANumeric input configurations.
 */
public class ANumericTestApp {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set a modern look and feel
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception e) {
                // Fall back to system look and feel
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e2) {
                    // Use default
                }
            }
            
            createAndShowGUI();
        });
    }
    
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("ANumeric Input Component Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Create the main panel with the demo widget
        ANumericDemoWidget demoWidget = new ANumericDemoWidget();
        JPanel mainPanel = demoWidget.getPanel();
        
        // Add a header
        JLabel headerLabel = new JLabel("ANumeric Input Component - Demonstration");
        headerLabel.setFont(headerLabel.getFont().deriveFont(16f));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        frame.add(headerLabel, BorderLayout.NORTH);
        frame.add(mainPanel, BorderLayout.CENTER);
        
        // Create an additional panel showing Theme integration
        JPanel themePanel = createThemeIntegrationPanel();
        frame.add(themePanel, BorderLayout.SOUTH);
        
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private static JPanel createThemeIntegrationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Theme Integration Examples"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Example using Theme.getFormRow (legacy method)
        ANumericInput themeInput1 = new ANumericInput(25.0, 0.0, 50.0, 2.5);
        panel.add(Theme.getFormRow(themeInput1, "Using Theme.getFormRow:", 
                "This shows integration with the legacy Theme.getFormRow method"));
        
        // Example using InputLabeller (modern method) 
        Theme.InputLabeller labeller = Theme.getInputLabeller();
        ANumericInput themeInput2 = new ANumericInput(100, 0, 1000, 10);
        JPanel labelledPanel = labeller.get("Using InputLabeller:", themeInput2, "themeInput2", 
                "This shows integration with the modern Theme.InputLabeller");
        panel.add(labelledPanel);
        
        return panel;
    }
}