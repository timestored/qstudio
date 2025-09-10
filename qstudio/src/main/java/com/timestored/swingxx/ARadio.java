package com.timestored.swingxx;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;

import com.timestored.theme.Theme;

/**
 * ARadio supports an inline form that allows highlighting one of many button-like items.
 * Similar to radio buttons but rendered as highlighted buttons in a horizontal layout.
 */
public class ARadio extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private final List<JButton> buttons = new ArrayList<>();
    private final List<ActionListener> listeners = new ArrayList<>();
    private final Border normalBorder;
    private final Border selectedBorder;
    private final Color normalBackground;
    private final Color selectedBackground;
    private final Color selectedForeground;
    
    private int selectedIndex = -1;
    private String[] options;
    
    /**
     * Creates an ARadio component with the specified options
     * @param options Array of string options to display as buttons
     */
    public ARadio(String[] options) {
        this.options = options.clone();
        
        // Initialize styling
        normalBorder = BorderFactory.createEmptyBorder(6, 15, 6, 15);
        selectedBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.HIGHLIGHT_BUTTON_COLOR, 2),
            BorderFactory.createEmptyBorder(4, 13, 4, 13)
        );
        normalBackground = getBackground();
        selectedBackground = Theme.HIGHLIGHT_BUTTON_COLOR;
        selectedForeground = Color.WHITE;
        
        initializeComponent();
    }
    
    private void initializeComponent() {
        setLayout(new FlowLayout(FlowLayout.LEFT, Theme.getGap(), 0));
        
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            JButton button = new JButton(options[i]);
            button.setBorder(normalBorder);
            button.setName(options[i] + "RadioButton");
            button.setFocusPainted(false);
            
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setSelected(index);
                    fireSelectionChanged();
                }
            });
            
            buttons.add(button);
            add(button);
        }
    }
    
    /**
     * Sets the selected index
     * @param index The index to select, or -1 for no selection
     */
    public void setSelected(int index) {
        if (index < -1 || index >= options.length) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        
        // Update visual state
        for (int i = 0; i < buttons.size(); i++) {
            JButton button = buttons.get(i);
            if (i == index) {
                button.setBackground(selectedBackground);
                button.setForeground(selectedForeground);
                button.setBorder(selectedBorder);
                button.setOpaque(true);
            } else {
                button.setBackground(normalBackground);
                button.setForeground(null); // Use default
                button.setBorder(normalBorder);
                button.setOpaque(false);
            }
        }
        
        selectedIndex = index;
        repaint();
    }
    
    /**
     * Sets the selected option by value
     * @param value The option value to select
     */
    public void setSelected(String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                setSelected(i);
                return;
            }
        }
    }
    
    /**
     * Gets the currently selected index
     * @return The selected index, or -1 if no selection
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Gets the currently selected value
     * @return The selected value, or null if no selection
     */
    public String getSelectedValue() {
        return selectedIndex >= 0 ? options[selectedIndex] : null;
    }
    
    /**
     * Adds a selection change listener
     * @param listener The listener to add
     */
    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a selection change listener
     * @param listener The listener to remove
     */
    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }
    
    private void fireSelectionChanged() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selectionChanged");
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }
}