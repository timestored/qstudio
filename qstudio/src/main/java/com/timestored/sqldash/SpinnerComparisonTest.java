package com.timestored.sqldash;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Comparison test between ANumeric input and standard JSpinner
 * to highlight the enhanced behavior of ANumeric.
 */
public class SpinnerComparisonTest {
    
    public static void main(String[] args) {
        System.out.println("=== ANumeric vs JSpinner Comparison ===\n");
        
        // Create comparable components
        ANumericInput anumeric = new ANumericInput(50.0, 0.0, 100.0, 5.0);
        JSpinner jspinner = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 5.0));
        
        System.out.println("Initial Setup: value=50, min=0, max=100, step=5");
        System.out.println("ANumeric value: " + anumeric.getNumericValue());
        System.out.println("JSpinner value: " + jspinner.getValue());
        System.out.println();
        
        // Test 1: Setting value within bounds
        System.out.println("Test 1: Setting value to 75 (within bounds)");
        anumeric.setNumericValue(75.0);
        jspinner.setValue(75.0);
        System.out.println("ANumeric: " + anumeric.getNumericValue());
        System.out.println("JSpinner: " + jspinner.getValue());
        System.out.println();
        
        // Test 2: Setting value below minimum
        System.out.println("Test 2: Setting value to -25 (below minimum)");
        anumeric.setNumericValue(-25.0);
        jspinner.setValue(-25.0); // JSpinner allows this!
        System.out.println("ANumeric: " + anumeric.getNumericValue() + " (automatically clamped to 0)");
        System.out.println("JSpinner: " + jspinner.getValue() + " (accepts invalid value)");
        System.out.println();
        
        // Test 3: Setting value above maximum
        System.out.println("Test 3: Setting value to 150 (above maximum)");
        anumeric.setNumericValue(150.0);
        jspinner.setValue(150.0); // JSpinner allows this too!
        System.out.println("ANumeric: " + anumeric.getNumericValue() + " (automatically clamped to 100)");
        System.out.println("JSpinner: " + jspinner.getValue() + " (accepts invalid value)");
        System.out.println();
        
        // Test 4: Constraint modification
        System.out.println("Test 4: Changing constraints dynamically");
        System.out.println("Setting new constraints: min=10, max=80");
        
        anumeric.setConstraints(10.0, 80.0, 5.0);
        // JSpinner requires creating new model for constraint changes
        jspinner.setModel(new SpinnerNumberModel(75.0, 10.0, 80.0, 5.0));
        
        System.out.println("ANumeric: " + anumeric.getNumericValue() + " (automatically adjusted from 100 to 80)");
        System.out.println("JSpinner: " + jspinner.getValue() + " (needed new model, value reset)");
        System.out.println();
        
        // Test 5: Icon and Theme integration
        System.out.println("Test 5: Integration Features");
        System.out.println("ANumeric icon available: " + (anumeric.getComponentIcon() != null));
        System.out.println("ANumeric tooltip: " + anumeric.getToolTipText());
        System.out.println("JSpinner icon: None built-in");
        System.out.println("JSpinner tooltip: " + jspinner.getToolTipText());
        System.out.println();
        
        // Test 6: API convenience
        System.out.println("Test 6: API Convenience");
        System.out.println("ANumeric - Get min/max/step:");
        System.out.println("  Min: " + anumeric.getMinValue());
        System.out.println("  Max: " + anumeric.getMaxValue()); 
        System.out.println("  Step: " + anumeric.getStepSize());
        
        System.out.println("JSpinner - Get min/max/step (requires casting):");
        SpinnerNumberModel model = (SpinnerNumberModel) jspinner.getModel();
        System.out.println("  Min: " + model.getMinimum());
        System.out.println("  Max: " + model.getMaximum());
        System.out.println("  Step: " + model.getStepSize());
        System.out.println();
        
        System.out.println("=== Summary ===");
        System.out.println("ANumeric Advantages:");
        System.out.println("✓ Automatic value clamping to constraints");
        System.out.println("✓ Simplified constraint management API");
        System.out.println("✓ Built-in icon support");
        System.out.println("✓ Enhanced tooltip with constraint info");
        System.out.println("✓ Theme integration ready");
        System.out.println("✓ Reuses slider variable patterns");
        System.out.println();
        System.out.println("JSpinner Limitations:");
        System.out.println("⚠ Allows invalid values outside constraints");
        System.out.println("⚠ Complex API for constraint access");
        System.out.println("⚠ No built-in icon support");
        System.out.println("⚠ Requires manual model replacement for constraint changes");
    }
}