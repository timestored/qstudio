# ANumeric Input Component

The `ANumericInput` class provides a numeric input component with up/down arrows and configurable min/max constraints, reusing the slider variable pattern from the existing codebase.

## Features

- **Up/Down Arrows**: Built on `JSpinner` with `SpinnerNumberModel` for intuitive value adjustment
- **Min/Max Constraints**: Similar to slider variables, supports configurable bounds
- **Step Size Control**: Configurable increment/decrement amount
- **Value Clamping**: Automatically constrains values to specified bounds
- **Theme Integration**: Compatible with existing `Theme.getFormRow()` and `InputLabeller` systems
- **Icon Support**: Uses existing `DBIcons.SPINNER` for consistency

## Basic Usage

```java
// Basic numeric input (no constraints)
ANumericInput basicInput = new ANumericInput();

// Input with initial value
ANumericInput valueInput = new ANumericInput(25.0);

// Constrained input (slider-like behavior)
ANumericInput constrainedInput = new ANumericInput(50.0, 0.0, 100.0, 5.0);
//                                                 value  min   max   step
```

## Integration with Forms

### Using Theme.getFormRow() (Legacy Pattern)
```java
ANumericInput timeoutInput = new ANumericInput(30.0, 0.0, 300.0, 5.0);
panel.add(Theme.getFormRow(timeoutInput, "Timeout (seconds):", 
    "Maximum wait time for operations"));
```

### Using InputLabeller (Modern Pattern)
```java
Theme.InputLabeller labeller = Theme.getInputLabeller();
ANumericInput precisionInput = new ANumericInput(2, 0, 10, 1);
JPanel labelledPanel = labeller.get("Decimal Places:", precisionInput, 
    "precisionInput", "Number of decimal places to display");
panel.add(labelledPanel);
```

## Configuration Methods

### Value Management
```java
double value = input.getNumericValue();
input.setNumericValue(42.0);
```

### Constraint Management
```java
// Individual constraint setting
input.setMinValue(0.0);
input.setMaxValue(100.0);
input.setStepSize(0.5);

// Set all constraints at once
input.setConstraints(0.0, 100.0, 0.5);

// Query current constraints
double min = input.getMinValue();
double max = input.getMaxValue();
double step = input.getStepSize();
```

### Event Handling
```java
input.addNumericChangeListener(new ChangeListener() {
    @Override
    public void stateChanged(ChangeEvent e) {
        System.out.println("New value: " + input.getNumericValue());
    }
});
```

## Examples

### 1. Preference Panel Integration
```java
public class MyPreferencesPanel extends JPanel {
    private final ANumericInput timeoutInput;
    
    public MyPreferencesPanel() {
        // Query timeout: 0-300 seconds, step 5
        timeoutInput = new ANumericInput(30.0, 0.0, 300.0, 5.0);
        
        add(Theme.getFormRow(timeoutInput, "Query Timeout:", 
            "Maximum time to wait for query results"));
    }
    
    public double getQueryTimeout() {
        return timeoutInput.getNumericValue();
    }
}
```

### 2. Chart Configuration
```java
public class ChartConfigPanel extends JPanel {
    private final ANumericInput refreshInterval;
    private final ANumericInput maxDataPoints;
    
    public ChartConfigPanel() {
        // Refresh interval: 100ms to 10s, step 100ms
        refreshInterval = new ANumericInput(1000, 100, 10000, 100);
        
        // Max data points: 10 to 10000, step 10
        maxDataPoints = new ANumericInput(1000, 10, 10000, 10);
        
        Theme.InputLabeller labeller = Theme.getInputLabeller();
        add(labeller.get("Refresh Interval (ms):", refreshInterval, "refreshInterval"));
        add(labeller.get("Max Data Points:", maxDataPoints, "maxDataPoints"));
    }
}
```

### 3. Slider Replacement
```java
// Instead of using JSlider with separate value display:
// JSlider slider = new JSlider(0, 100, 50);
// JLabel valueLabel = new JLabel("50");

// Use ANumeric input directly:
ANumericInput input = new ANumericInput(50, 0, 100, 1);
// Value is always visible and editable
```

## Error Handling

The component validates constraints and throws `IllegalArgumentException` for invalid configurations:

```java
try {
    // This will throw an exception
    ANumericInput invalid = new ANumericInput(50, 100, 0, 1); // min > max
} catch (IllegalArgumentException e) {
    // Handle invalid constraint configuration
}
```

## Comparison with Existing Components

| Feature | ANumericInput | JSpinner | JSlider + JTextField |
|---------|---------------|----------|---------------------|
| Up/Down Arrows | ✅ | ✅ | ❌ |
| Direct Text Input | ✅ | ✅ | ✅ (separate field) |
| Min/Max Constraints | ✅ | ✅ | ✅ |
| Value Clamping | ✅ | ❌ | ❌ |
| Theme Integration | ✅ | ❌ | ❌ |
| Icon Support | ✅ | ❌ | ❌ |
| Slider Variables Pattern | ✅ | ❌ | ✅ |

## Implementation Details

- **Base Class**: Extends `javax.swing.JSpinner`
- **Model**: Uses `javax.swing.SpinnerNumberModel`
- **Icon**: Reuses `DBIcons.SPINNER`
- **Size**: Default preferred size of 100x25 pixels
- **Constraints**: Supports `Double.NEGATIVE_INFINITY` and `Double.POSITIVE_INFINITY` for unbounded values

## Testing

Run the included test classes to verify functionality:

```bash
# Console test (no GUI required)
java -cp "lib/*:src/main/java" com.timestored.sqldash.ANumericConsoleTest

# GUI demo (requires display)
java -cp "lib/*:src/main/java" com.timestored.sqldash.ANumericTestApp
```