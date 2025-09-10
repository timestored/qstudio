package com.timestored.sqldash;

import static com.timestored.theme.Theme.getFormRow;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.timestored.sqldash.theme.DBIcons;
import com.timestored.theme.Theme;

/**
 * Example integration of ANumeric inputs alongside existing form components.
 * This demonstrates how ANumeric can be used in preference panels similar
 * to existing JSpinner usage, but with enhanced functionality.
 */
public class AppearancePreferencesWithANumeric extends JPanel {

	private static final long serialVersionUID = 1L;
	
	// Existing JSpinner controls (from original AppearancePreferencesPanel pattern)
	private final JSpinner maxFractionDigitsSpinner;
	private final JSpinner codeFontSpinner;
	private final JFormattedTextField rowLimitField;
	
	// New ANumeric controls demonstrating enhanced functionality
	private final ANumericInput queryTimeoutInput;
	private final ANumericInput chartRefreshInput;
	private final ANumericInput maxConnectionsInput;
	private final ANumericInput precisionInput;
	
	private final Component container;
	
	public AppearancePreferencesWithANumeric(Component container) {
		this.container = container;
		
		Box panel = Box.createVerticalBox();
		panel.setBorder(Theme.getCentreBorder());
		
		// === Existing JSpinner controls (from original pattern) ===
		
		codeFontSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 50, 1));
		panel.add(getFormRow(codeFontSpinner, "Font Size:", "Font Size of Code Editor"));

		maxFractionDigitsSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 20, 1));
		panel.add(getFormRow(maxFractionDigitsSpinner, "Maximum Decimal Places:",
				"In results tables, floating point values will be trimmed to this precision."));

		rowLimitField = new JFormattedTextField(Integer.valueOf(1000));
		panel.add(getFormRow(rowLimitField, "Maximum Table Rows:",
				"The IDE will only show this many rows at most. 0=unlimited"));
		
		panel.add(Box.createVerticalStrut(10));
		
		// === New ANumeric controls with enhanced features ===
		
		// Add section header
		Box headerBox = Box.createHorizontalBox();
		headerBox.setBorder(BorderFactory.createTitledBorder("Enhanced Numeric Controls (ANumeric)"));
		
		// Query timeout with validation and clamping
		queryTimeoutInput = new ANumericInput(30.0, 0.0, 300.0, 5.0);
		queryTimeoutInput.addNumericChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				notifyChange("Query timeout changed to: " + queryTimeoutInput.getNumericValue() + "s");
			}
		});
		
		JLabel timeoutHelp = new JLabel(Theme.CIcon.INFO.get());
		timeoutHelp.setToolTipText("Values automatically clamped to 0-300 range. Uses 5-second increments.");
		
		panel.add(getFormRow(queryTimeoutInput, "Query Timeout (seconds):", 
				"Maximum time to wait for query results (0-300s, step 5s)", timeoutHelp));
		
		// Chart refresh interval with fine control
		chartRefreshInput = new ANumericInput(1000.0, 100.0, 10000.0, 100.0);
		panel.add(getFormRow(chartRefreshInput, "Chart Refresh Interval (ms):", 
				"How often charts update automatically (100-10000ms, step 100ms)"));
		
		// Maximum connections with integer constraints  
		maxConnectionsInput = new ANumericInput(5, 1, 50, 1);
		panel.add(getFormRow(maxConnectionsInput, "Max Database Connections:",
				"Maximum concurrent database connections (1-50, step 1)"));
				
		// High-precision decimal control
		precisionInput = new ANumericInput(3.141, 0.0, 10.0, 0.001);
		
		JLabel precisionHelp = new JLabel(DBIcons.SPINNER.get16());
		precisionHelp.setToolTipText("Demonstrates high-precision decimal input with 0.001 step size");
		
		panel.add(getFormRow(precisionInput, "Precision Value:", 
				"High-precision decimal (0-10, step 0.001)", precisionHelp));
		
		panel.add(Box.createVerticalStrut(10));
		
		// === Comparison information ===
		
		Box comparisonBox = Box.createVerticalBox();
		comparisonBox.setBorder(BorderFactory.createTitledBorder("ANumeric vs JSpinner Comparison"));
		comparisonBox.add(new JLabel("ANumeric Benefits:"));
		comparisonBox.add(new JLabel("• Automatic value clamping to min/max bounds"));
		comparisonBox.add(new JLabel("• Enhanced integration with Theme system"));
		comparisonBox.add(new JLabel("• Reuses slider variable patterns"));
		comparisonBox.add(new JLabel("• Consistent icon usage (DBIcons.SPINNER)"));
		comparisonBox.add(new JLabel("• Built-in constraint validation"));
		comparisonBox.add(new JLabel("• Simplified API for common use cases"));
		
		panel.add(comparisonBox);
		
		setLayout(new BorderLayout());
		add(panel, BorderLayout.NORTH);
		
		// Initialize values
		refresh();
	}
	
	private void notifyChange(String message) {
		System.out.println("Preference change: " + message);
		SwingUtilities.updateComponentTreeUI(container);
	}
	
	/** Initialize values from settings (simulated) */
	void refresh() {
		// Simulate loading from preferences
		codeFontSpinner.setValue(Integer.valueOf(12));
		maxFractionDigitsSpinner.setValue(Integer.valueOf(2)); 
		rowLimitField.setValue(Integer.valueOf(1000));
		
		// ANumeric inputs retain their constraint-aware values
		queryTimeoutInput.setNumericValue(30.0);
		chartRefreshInput.setNumericValue(1000.0);
		maxConnectionsInput.setNumericValue(5);
		precisionInput.setNumericValue(3.141);
	}
	
	/** Save settings (simulated) */
	void saveSettings() {
		System.out.println("Saving settings:");
		System.out.println("  Font Size: " + codeFontSpinner.getValue());
		System.out.println("  Max Decimal Places: " + maxFractionDigitsSpinner.getValue());
		System.out.println("  Row Limit: " + rowLimitField.getValue());
		System.out.println("  Query Timeout: " + queryTimeoutInput.getNumericValue());
		System.out.println("  Chart Refresh: " + chartRefreshInput.getNumericValue());
		System.out.println("  Max Connections: " + maxConnectionsInput.getNumericValue());
		System.out.println("  Precision Value: " + precisionInput.getNumericValue());
	}
	
	// Getters for the ANumeric values
	public double getQueryTimeout() { return queryTimeoutInput.getNumericValue(); }
	public double getChartRefreshInterval() { return chartRefreshInput.getNumericValue(); }
	public int getMaxConnections() { return (int) maxConnectionsInput.getNumericValue(); }
	public double getPrecisionValue() { return precisionInput.getNumericValue(); }
}