package com.timestored.swingxx;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

import com.timestored.theme.Theme;

/**
 * ABoolean input type widget for Aforms. It can be displayed as a checkbox, 
 * a sliding toggle OR as a single outlined button that highlights on/off like the others.
 */
public class ABoolean extends JPanel {
    private static final long serialVersionUID = 1L;
    
    public enum DisplayMode {
        CHECKBOX,
        TOGGLE,
        BUTTON
    }
    
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final JCheckBox checkBox;
    private final JToggleButton toggleButton;
    private final JButton highlightButton;
    private final List<ActionListener> listeners = new ArrayList<>();
    
    private final Border normalBorder;
    private final Border selectedBorder;
    private final Color normalBackground;
    private final Color selectedBackground;
    private final Color selectedForeground;
    
    private DisplayMode displayMode = DisplayMode.CHECKBOX;
    private boolean value = false;
    private String label = "Option";
    
    /**
     * Creates an ABoolean component with default settings
     */
    public ABoolean() {
        this("Option", DisplayMode.CHECKBOX);
    }
    
    /**
     * Creates an ABoolean component with the specified label
     * @param label The label text for the boolean option
     */
    public ABoolean(String label) {
        this(label, DisplayMode.CHECKBOX);
    }
    
    /**
     * Creates an ABoolean component with the specified label and display mode
     * @param label The label text for the boolean option
     * @param mode The display mode (CHECKBOX, TOGGLE, or BUTTON)
     */
    public ABoolean(String label, DisplayMode mode) {
        this.label = label;
        this.displayMode = mode;
        
        // Initialize styling for button mode
        normalBorder = BorderFactory.createEmptyBorder(6, 15, 6, 15);
        selectedBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.HIGHLIGHT_BUTTON_COLOR, 2),
            BorderFactory.createEmptyBorder(4, 13, 4, 13)
        );
        normalBackground = getBackground();
        selectedBackground = Theme.HIGHLIGHT_BUTTON_COLOR;
        selectedForeground = Color.WHITE;
        
        // Initialize components
        checkBox = new JCheckBox(label);
        checkBox.setName(label + "CheckBox");
        
        toggleButton = new JToggleButton(label);
        toggleButton.setName(label + "ToggleButton");
        
        highlightButton = new JButton(label);
        highlightButton.setBorder(normalBorder);
        highlightButton.setName(label + "HighlightButton");
        highlightButton.setFocusPainted(false);
        
        // Set up card layout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        
        initializeComponent();
    }
    
    private void initializeComponent() {
        setLayout(new BorderLayout());
        
        // Create panels for each mode
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkboxPanel.add(checkBox);
        
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        togglePanel.add(toggleButton);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.add(highlightButton);
        
        // Add to card layout
        contentPanel.add(checkboxPanel, DisplayMode.CHECKBOX.name());
        contentPanel.add(togglePanel, DisplayMode.TOGGLE.name());
        contentPanel.add(buttonPanel, DisplayMode.BUTTON.name());
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Add listeners
        checkBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                value = checkBox.isSelected();
                updateOtherComponents();
                fireValueChanged();
            }
        });
        
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                value = toggleButton.isSelected();
                updateOtherComponents();
                fireValueChanged();
            }
        });
        
        highlightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                value = !value;
                updateOtherComponents();
                updateButtonAppearance();
                fireValueChanged();
            }
        });
        
        // Set initial display mode
        setDisplayMode(displayMode);
    }
    
    /**
     * Sets the display mode of the boolean input
     * @param mode The display mode to use
     */
    public void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        cardLayout.show(contentPanel, mode.name());
        updateAllComponents();
    }
    
    /**
     * Gets the current display mode
     * @return The current display mode
     */
    public DisplayMode getDisplayMode() {
        return displayMode;
    }
    
    /**
     * Sets the boolean value
     * @param value The boolean value to set
     */
    public void setValue(boolean value) {
        this.value = value;
        updateAllComponents();
        updateButtonAppearance();
    }
    
    /**
     * Gets the boolean value
     * @return The current boolean value
     */
    public boolean getValue() {
        return value;
    }
    
    /**
     * Sets the label text
     * @param label The label text to set
     */
    public void setLabel(String label) {
        this.label = label;
        checkBox.setText(label);
        toggleButton.setText(label);
        highlightButton.setText(label);
    }
    
    /**
     * Gets the label text
     * @return The current label text
     */
    public String getLabel() {
        return label;
    }
    
    private void updateAllComponents() {
        checkBox.setSelected(value);
        toggleButton.setSelected(value);
        // highlightButton state is updated in updateButtonAppearance()
    }
    
    private void updateOtherComponents() {
        // Update components other than the one that triggered the change
        if (displayMode != DisplayMode.CHECKBOX) {
            checkBox.setSelected(value);
        }
        if (displayMode != DisplayMode.TOGGLE) {
            toggleButton.setSelected(value);
        }
        if (displayMode == DisplayMode.BUTTON) {
            updateButtonAppearance();
        }
    }
    
    private void updateButtonAppearance() {
        if (value) {
            highlightButton.setBackground(selectedBackground);
            highlightButton.setForeground(selectedForeground);
            highlightButton.setBorder(selectedBorder);
            highlightButton.setOpaque(true);
        } else {
            highlightButton.setBackground(normalBackground);
            highlightButton.setForeground(null); // Use default
            highlightButton.setBorder(normalBorder);
            highlightButton.setOpaque(false);
        }
        highlightButton.repaint();
    }
    
    /**
     * Adds a value change listener
     * @param listener The listener to add
     */
    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a value change listener
     * @param listener The listener to remove
     */
    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }
    
    private void fireValueChanged() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "valueChanged");
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        checkBox.setEnabled(enabled);
        toggleButton.setEnabled(enabled);
        highlightButton.setEnabled(enabled);
    }
}