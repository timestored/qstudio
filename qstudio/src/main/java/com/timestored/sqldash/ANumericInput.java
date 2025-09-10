package com.timestored.sqldash;

import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.timestored.sqldash.theme.DBIcons;

/**
 * ANumeric input component that provides a numeric input with up/down arrows
 * and support for min/max values reusing slider variables.
 * 
 * This component extends JSpinner to provide a consistent numeric input 
 * experience with configurable constraints.
 */
public class ANumericInput extends JSpinner {
    
    private static final long serialVersionUID = 1L;
    
    // Default values similar to slider constraints
    private static final double DEFAULT_MIN = Double.NEGATIVE_INFINITY;
    private static final double DEFAULT_MAX = Double.POSITIVE_INFINITY;
    private static final double DEFAULT_STEP = 1.0;
    private static final double DEFAULT_VALUE = 0.0;
    
    private final SpinnerNumberModel numberModel;
    private ChangeListener userChangeListener;
    
    /**
     * Create an ANumeric input with default settings
     */
    public ANumericInput() {
        this(DEFAULT_VALUE, DEFAULT_MIN, DEFAULT_MAX, DEFAULT_STEP);
    }
    
    /**
     * Create an ANumeric input with specified value
     * @param value Initial value
     */
    public ANumericInput(double value) {
        this(value, DEFAULT_MIN, DEFAULT_MAX, DEFAULT_STEP);
    }
    
    /**
     * Create an ANumeric input with specified constraints
     * @param value Initial value
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @param step Step size for increment/decrement
     */
    public ANumericInput(double value, double min, double max, double step) {
        super();
        
        // Validate parameters
        if (min > max) {
            throw new IllegalArgumentException("Min value cannot be greater than max value");
        }
        if (value < min || value > max) {
            value = Math.max(min, Math.min(max, value));
        }
        
        // Create and set the number model
        numberModel = new SpinnerNumberModel(
                Double.valueOf(value), 
                min == Double.NEGATIVE_INFINITY ? null : Double.valueOf(min),
                max == Double.POSITIVE_INFINITY ? null : Double.valueOf(max),
                Double.valueOf(step));
        setModel(numberModel);
        
        // Set preferred size to match other form components
        setPreferredSize(new Dimension(100, 25));
        
        // Set tooltip
        setToolTipText(String.format("Numeric input (min: %s, max: %s, step: %s)", 
                formatValue(min), formatValue(max), formatValue(step)));
    }
    
    /**
     * Get the current numeric value
     * @return Current value as double
     */
    public double getNumericValue() {
        Object value = getValue();
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return DEFAULT_VALUE;
    }
    
    /**
     * Set the numeric value
     * @param value New value
     */
    public void setNumericValue(double value) {
        // Clamp value to constraints
        double min = getMinValue();
        double max = getMaxValue();
        
        if (min != Double.NEGATIVE_INFINITY && value < min) {
            value = min;
        }
        if (max != Double.POSITIVE_INFINITY && value > max) {
            value = max;
        }
        
        setValue(value);
    }
    
    /**
     * Get the minimum allowed value
     * @return Minimum value or Double.NEGATIVE_INFINITY if no minimum
     */
    public double getMinValue() {
        Comparable<?> min = numberModel.getMinimum();
        return min != null ? ((Number) min).doubleValue() : Double.NEGATIVE_INFINITY;
    }
    
    /**
     * Set the minimum allowed value
     * @param min New minimum value
     */
    public void setMinValue(double min) {
        double currentMax = getMaxValue();
        if (min > currentMax && currentMax != Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("Min value cannot be greater than current max value");
        }
        
        numberModel.setMinimum(min == Double.NEGATIVE_INFINITY ? null : Double.valueOf(min));
        
        // Ensure current value is within new bounds
        double currentValue = getNumericValue();
        if (currentValue < min) {
            setNumericValue(min);
        }
        
        updateTooltip();
    }
    
    /**
     * Get the maximum allowed value
     * @return Maximum value or Double.POSITIVE_INFINITY if no maximum
     */
    public double getMaxValue() {
        Comparable<?> max = numberModel.getMaximum();
        return max != null ? ((Number) max).doubleValue() : Double.POSITIVE_INFINITY;
    }
    
    /**
     * Set the maximum allowed value
     * @param max New maximum value
     */
    public void setMaxValue(double max) {
        double currentMin = getMinValue();
        if (max < currentMin && currentMin != Double.NEGATIVE_INFINITY) {
            throw new IllegalArgumentException("Max value cannot be less than current min value");
        }
        
        numberModel.setMaximum(max == Double.POSITIVE_INFINITY ? null : Double.valueOf(max));
        
        // Ensure current value is within new bounds
        double currentValue = getNumericValue();
        if (currentValue > max) {
            setNumericValue(max);
        }
        
        updateTooltip();
    }
    
    /**
     * Get the step size
     * @return Step size for increment/decrement operations
     */
    public double getStepSize() {
        Object step = numberModel.getStepSize();
        return step instanceof Number ? ((Number) step).doubleValue() : DEFAULT_STEP;
    }
    
    /**
     * Set the step size
     * @param step New step size
     */
    public void setStepSize(double step) {
        if (step <= 0) {
            throw new IllegalArgumentException("Step size must be positive");
        }
        numberModel.setStepSize(Double.valueOf(step));
        updateTooltip();
    }
    
    /**
     * Set all constraints at once
     * @param min Minimum value
     * @param max Maximum value  
     * @param step Step size
     */
    public void setConstraints(double min, double max, double step) {
        if (min > max) {
            throw new IllegalArgumentException("Min value cannot be greater than max value");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("Step size must be positive");
        }
        
        numberModel.setMinimum(min == Double.NEGATIVE_INFINITY ? null : Double.valueOf(min));
        numberModel.setMaximum(max == Double.POSITIVE_INFINITY ? null : Double.valueOf(max));
        numberModel.setStepSize(Double.valueOf(step));
        
        // Ensure current value is within new bounds
        double currentValue = getNumericValue();
        if (currentValue < min) {
            setNumericValue(min);
        } else if (currentValue > max) {
            setNumericValue(max);
        }
        
        updateTooltip();
    }
    
    /**
     * Add a change listener to be notified when the value changes
     * @param listener Change listener
     */
    public void addNumericChangeListener(ChangeListener listener) {
        this.userChangeListener = listener;
        addChangeListener(listener);
    }
    
    /**
     * Remove the change listener
     * @param listener Change listener to remove
     */
    public void removeNumericChangeListener(ChangeListener listener) {
        removeChangeListener(listener);
        if (this.userChangeListener == listener) {
            this.userChangeListener = null;
        }
    }
    
    /**
     * Get the icon associated with this component type
     * @return Icon for ANumeric input (spinner icon)
     */
    public Icon getComponentIcon() {
        return DBIcons.SPINNER.get16();
    }
    
    /**
     * Format a value for display, handling infinity cases
     */
    private String formatValue(double value) {
        if (value == Double.NEGATIVE_INFINITY) {
            return "∞";
        } else if (value == Double.POSITIVE_INFINITY) {
            return "∞";
        } else {
            return String.valueOf(value);
        }
    }
    
    /**
     * Update the tooltip to reflect current constraints
     */
    private void updateTooltip() {
        String tooltip = String.format("Numeric input (min: %s, max: %s, step: %s)", 
                formatValue(getMinValue()), formatValue(getMaxValue()), formatValue(getStepSize()));
        setToolTipText(tooltip);
    }
}