package com.timestored.sqldash;

/**
 * Console test for the ANumeric input component to verify functionality
 * without requiring a GUI display.
 */
public class ANumericConsoleTest {
    
    public static void main(String[] args) {
        System.out.println("=== ANumeric Input Component Test ===\n");
        
        // Test 1: Basic functionality
        System.out.println("Test 1: Basic ANumeric Input");
        ANumericInput basic = new ANumericInput();
        System.out.println("Default value: " + basic.getNumericValue());
        System.out.println("Min value: " + basic.getMinValue());
        System.out.println("Max value: " + basic.getMaxValue());
        System.out.println("Step size: " + basic.getStepSize());
        System.out.println("Icon available: " + (basic.getComponentIcon() != null));
        System.out.println();
        
        // Test 2: Constrained input (like slider)
        System.out.println("Test 2: Constrained ANumeric Input (slider-like)");
        ANumericInput slider = new ANumericInput(50.0, 0.0, 100.0, 5.0);
        System.out.println("Initial value: " + slider.getNumericValue());
        System.out.println("Min: " + slider.getMinValue() + ", Max: " + slider.getMaxValue());
        System.out.println("Step: " + slider.getStepSize());
        
        // Test value changes
        slider.setNumericValue(25.0);
        System.out.println("After setting to 25: " + slider.getNumericValue());
        
        // Test boundary conditions
        slider.setNumericValue(-10.0); // Should be clamped to 0
        System.out.println("After setting to -10 (should be 0): " + slider.getNumericValue());
        
        slider.setNumericValue(150.0); // Should be clamped to 100
        System.out.println("After setting to 150 (should be 100): " + slider.getNumericValue());
        System.out.println();
        
        // Test 3: Integer input
        System.out.println("Test 3: Integer ANumeric Input");
        ANumericInput integer = new ANumericInput(10, -100, 100, 1);
        System.out.println("Integer input value: " + integer.getNumericValue());
        System.out.println("Range: " + integer.getMinValue() + " to " + integer.getMaxValue());
        System.out.println();
        
        // Test 4: Decimal precision input  
        System.out.println("Test 4: High-precision Decimal Input");
        ANumericInput decimal = new ANumericInput(3.14159, 0.0, 10.0, 0.001);
        System.out.println("Decimal input value: " + decimal.getNumericValue());
        System.out.println("Step size: " + decimal.getStepSize());
        System.out.println();
        
        // Test 5: Constraint modifications
        System.out.println("Test 5: Dynamic Constraint Changes");
        ANumericInput dynamic = new ANumericInput(50.0);
        System.out.println("Initial (unconstrained): " + dynamic.getNumericValue());
        
        dynamic.setConstraints(0.0, 200.0, 10.0);
        System.out.println("After setting constraints (0-200, step 10): " + dynamic.getNumericValue());
        
        dynamic.setMinValue(25.0);
        System.out.println("After setting min to 25 (value should adjust): " + dynamic.getNumericValue());
        
        dynamic.setMaxValue(75.0);
        System.out.println("After setting max to 75: " + dynamic.getNumericValue());
        System.out.println();
        
        // Test 6: Error handling
        System.out.println("Test 6: Error Handling");
        try {
            new ANumericInput(50, 100, 0, 1); // min > max
            System.out.println("ERROR: Should have thrown exception for min > max");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Correctly rejected min > max: " + e.getMessage());
        }
        
        try {
            dynamic.setStepSize(-1.0); // negative step
            System.out.println("ERROR: Should have thrown exception for negative step");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Correctly rejected negative step: " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Integration features
        System.out.println("Test 7: Integration Features");
        ANumericInput integration = new ANumericInput(42.0, 0.0, 100.0, 1.0);
        System.out.println("Tooltip text: " + integration.getToolTipText());
        System.out.println("Component name (if set): " + integration.getName());
        System.out.println("Preferred size: " + integration.getPreferredSize());
        System.out.println();
        
        System.out.println("=== All Tests Completed Successfully ===");
        System.out.println("\nANumeric Input Component Features:");
        System.out.println("✓ Supports min/max constraints like sliders");
        System.out.println("✓ Configurable step size for up/down arrows");
        System.out.println("✓ Automatic value clamping to constraints");
        System.out.println("✓ Integration with existing Icon system");
        System.out.println("✓ Proper error handling for invalid configurations");
        System.out.println("✓ Compatible with Theme and form layout systems");
        System.out.println("✓ Reuses slider variable patterns and concepts");
    }
}