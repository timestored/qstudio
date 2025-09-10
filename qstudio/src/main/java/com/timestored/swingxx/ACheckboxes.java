package com.timestored.swingxx;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;

import com.timestored.theme.Theme;

/**
 * ACheckboxes supports an inline form that allows highlighting multiple button-like items.
 * Similar to checkboxes but rendered as highlighted buttons in a horizontal layout with multi-selection support.
 */
public class ACheckboxes extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private final List<JButton> buttons = new ArrayList<>();
    private final List<ActionListener> listeners = new ArrayList<>();
    private final Border normalBorder;
    private final Border selectedBorder;
    private final Color normalBackground;
    private final Color selectedBackground;
    private final Color selectedForeground;
    
    private final Set<Integer> selectedIndices = new HashSet<>();
    private String[] options;
    
    /**
     * Creates an ACheckboxes component with the specified options
     * @param options Array of string options to display as buttons
     */
    public ACheckboxes(String[] options) {
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
            button.setName(options[i] + "CheckboxButton");
            button.setFocusPainted(false);
            
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleSelected(index);
                    fireSelectionChanged();
                }
            });
            
            buttons.add(button);
            add(button);
        }
    }
    
    /**
     * Toggles the selection state of the specified index
     * @param index The index to toggle
     */
    public void toggleSelected(int index) {
        if (index < 0 || index >= options.length) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index);
        } else {
            selectedIndices.add(index);
        }
        
        updateButtonAppearance(index);
    }
    
    /**
     * Sets the selection state of the specified index
     * @param index The index to set
     * @param selected Whether the index should be selected
     */
    public void setSelected(int index, boolean selected) {
        if (index < 0 || index >= options.length) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        
        if (selected) {
            selectedIndices.add(index);
        } else {
            selectedIndices.remove(index);
        }
        
        updateButtonAppearance(index);
    }
    
    /**
     * Sets the selection state of the specified value
     * @param value The option value to set
     * @param selected Whether the value should be selected
     */
    public void setSelected(String value, boolean selected) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                setSelected(i, selected);
                return;
            }
        }
    }
    
    /**
     * Clears all selections
     */
    public void clearSelection() {
        selectedIndices.clear();
        for (int i = 0; i < buttons.size(); i++) {
            updateButtonAppearance(i);
        }
    }
    
    /**
     * Selects all options
     */
    public void selectAll() {
        for (int i = 0; i < options.length; i++) {
            selectedIndices.add(i);
            updateButtonAppearance(i);
        }
    }
    
    private void updateButtonAppearance(int index) {
        JButton button = buttons.get(index);
        if (selectedIndices.contains(index)) {
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
        button.repaint();
    }
    
    /**
     * Gets the set of selected indices
     * @return A copy of the selected indices set
     */
    public Set<Integer> getSelectedIndices() {
        return new HashSet<>(selectedIndices);
    }
    
    /**
     * Gets the list of selected values
     * @return A list of selected values
     */
    public List<String> getSelectedValues() {
        List<String> values = new ArrayList<>();
        for (Integer index : selectedIndices) {
            values.add(options[index]);
        }
        return values;
    }
    
    /**
     * Checks if the specified index is selected
     * @param index The index to check
     * @return True if selected, false otherwise
     */
    public boolean isSelected(int index) {
        return selectedIndices.contains(index);
    }
    
    /**
     * Checks if the specified value is selected
     * @param value The value to check
     * @return True if selected, false otherwise
     */
    public boolean isSelected(String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value) && selectedIndices.contains(i)) {
                return true;
            }
        }
        return false;
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