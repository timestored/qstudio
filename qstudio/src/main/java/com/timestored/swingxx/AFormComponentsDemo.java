package com.timestored.swingxx;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;
import com.timestored.theme.Theme;

/**
 * Demo application to test the ARadio, ACheckboxes, and ABoolean components
 */
public class AFormComponentsDemo {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                createAndShowGUI();
            }
        });
    }
    
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("AForm Components Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(Theme.getCentreBorder());
        
        // ARadio Demo
        mainPanel.add(new JLabel("ARadio Component (Single Selection):"));
        mainPanel.add(Box.createVerticalStrut(Theme.getGap()));
        
        String[] radioOptions = {"Option A", "Option B", "Option C", "Option D"};
        ARadio radioComponent = new ARadio(radioOptions);
        radioComponent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("ARadio selected: " + radioComponent.getSelectedValue() + 
                                 " (index: " + radioComponent.getSelectedIndex() + ")");
            }
        });
        radioComponent.setSelected(0); // Select first option by default
        mainPanel.add(radioComponent);
        
        mainPanel.add(Box.createVerticalStrut(Theme.getGap() * 3));
        
        // ACheckboxes Demo
        mainPanel.add(new JLabel("ACheckboxes Component (Multi Selection):"));
        mainPanel.add(Box.createVerticalStrut(Theme.getGap()));
        
        String[] checkboxOptions = {"Feature 1", "Feature 2", "Feature 3", "Feature 4"};
        ACheckboxes checkboxComponent = new ACheckboxes(checkboxOptions);
        checkboxComponent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("ACheckboxes selected: " + checkboxComponent.getSelectedValues() + 
                                 " (indices: " + checkboxComponent.getSelectedIndices() + ")");
            }
        });
        checkboxComponent.setSelected(0, true);
        checkboxComponent.setSelected(2, true);
        mainPanel.add(checkboxComponent);
        
        mainPanel.add(Box.createVerticalStrut(Theme.getGap() * 3));
        
        // ABoolean Demo - Checkbox Mode
        mainPanel.add(new JLabel("ABoolean Component - Checkbox Mode:"));
        mainPanel.add(Box.createVerticalStrut(Theme.getGap()));
        
        ABoolean booleanCheckbox = new ABoolean("Enable Feature X", ABoolean.DisplayMode.CHECKBOX);
        booleanCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("ABoolean (Checkbox) value: " + booleanCheckbox.getValue());
            }
        });
        booleanCheckbox.setValue(true);
        mainPanel.add(booleanCheckbox);
        
        mainPanel.add(Box.createVerticalStrut(Theme.getGap() * 2));
        
        // ABoolean Demo - Toggle Mode
        mainPanel.add(new JLabel("ABoolean Component - Toggle Mode:"));
        mainPanel.add(Box.createVerticalStrut(Theme.getGap()));
        
        ABoolean booleanToggle = new ABoolean("Enable Feature Y", ABoolean.DisplayMode.TOGGLE);
        booleanToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("ABoolean (Toggle) value: " + booleanToggle.getValue());
            }
        });
        mainPanel.add(booleanToggle);
        
        mainPanel.add(Box.createVerticalStrut(Theme.getGap() * 2));
        
        // ABoolean Demo - Button Mode
        mainPanel.add(new JLabel("ABoolean Component - Button Mode:"));
        mainPanel.add(Box.createVerticalStrut(Theme.getGap()));
        
        ABoolean booleanButton = new ABoolean("Enable Feature Z", ABoolean.DisplayMode.BUTTON);
        booleanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("ABoolean (Button) value: " + booleanButton.getValue());
            }
        });
        mainPanel.add(booleanButton);
        
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}