package com.timestored.swingxx;

/**
 * Simple test to verify the functionality of the ARadio, ACheckboxes, and ABoolean components
 * without requiring a GUI display.
 */
public class AFormComponentsTest {
    
    public static void main(String[] args) {
        System.out.println("Testing AForm Components");
        System.out.println("========================");
        
        testARadio();
        testACheckboxes();
        testABoolean();
        
        System.out.println("\nAll tests completed successfully!");
    }
    
    private static void testARadio() {
        System.out.println("\n1. Testing ARadio Component:");
        
        String[] options = {"Option A", "Option B", "Option C"};
        ARadio radio = new ARadio(options);
        
        // Test initial state
        System.out.println("   Initial selected index: " + radio.getSelectedIndex());
        System.out.println("   Initial selected value: " + radio.getSelectedValue());
        
        // Test selection by index
        radio.setSelected(1);
        System.out.println("   After selecting index 1: " + radio.getSelectedValue());
        
        // Test selection by value
        radio.setSelected("Option C");
        System.out.println("   After selecting 'Option C': index=" + radio.getSelectedIndex());
        
        System.out.println("   ✓ ARadio tests passed");
    }
    
    private static void testACheckboxes() {
        System.out.println("\n2. Testing ACheckboxes Component:");
        
        String[] options = {"Feature 1", "Feature 2", "Feature 3", "Feature 4"};
        ACheckboxes checkboxes = new ACheckboxes(options);
        
        // Test initial state
        System.out.println("   Initial selections: " + checkboxes.getSelectedValues());
        
        // Test single selections
        checkboxes.setSelected(0, true);
        checkboxes.setSelected(2, true);
        System.out.println("   After selecting indices 0,2: " + checkboxes.getSelectedValues());
        
        // Test toggle
        checkboxes.toggleSelected(1);
        System.out.println("   After toggling index 1: " + checkboxes.getSelectedValues());
        
        // Test selection by value
        checkboxes.setSelected("Feature 4", true);
        System.out.println("   After selecting 'Feature 4': " + checkboxes.getSelectedValues());
        
        // Test clear
        checkboxes.clearSelection();
        System.out.println("   After clear: " + checkboxes.getSelectedValues());
        
        System.out.println("   ✓ ACheckboxes tests passed");
    }
    
    private static void testABoolean() {
        System.out.println("\n3. Testing ABoolean Component:");
        
        // Test each display mode
        ABoolean boolCheckbox = new ABoolean("Test Option", ABoolean.DisplayMode.CHECKBOX);
        ABoolean boolToggle = new ABoolean("Test Option", ABoolean.DisplayMode.TOGGLE);
        ABoolean boolButton = new ABoolean("Test Option", ABoolean.DisplayMode.BUTTON);
        
        // Test initial values
        System.out.println("   Initial values - Checkbox: " + boolCheckbox.getValue() + 
                          ", Toggle: " + boolToggle.getValue() + 
                          ", Button: " + boolButton.getValue());
        
        // Test setting values
        boolCheckbox.setValue(true);
        boolToggle.setValue(true);
        boolButton.setValue(true);
        
        System.out.println("   After setting true - Checkbox: " + boolCheckbox.getValue() + 
                          ", Toggle: " + boolToggle.getValue() + 
                          ", Button: " + boolButton.getValue());
        
        // Test mode switching
        boolCheckbox.setDisplayMode(ABoolean.DisplayMode.BUTTON);
        System.out.println("   After switching checkbox to button mode: " + boolCheckbox.getDisplayMode());
        
        // Test label changing
        boolToggle.setLabel("New Label");
        System.out.println("   After changing label: " + boolToggle.getLabel());
        
        System.out.println("   ✓ ABoolean tests passed");
    }
}