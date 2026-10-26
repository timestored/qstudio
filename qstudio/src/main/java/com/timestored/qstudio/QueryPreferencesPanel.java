/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2023 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timestored.qstudio;

import static com.timestored.theme.Theme.getFormRow;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.timestored.theme.Theme;

/**
 * GUI Panel that allows configuring {@link MyPreferences} related to Querying/Connections. 
 */
class QueryPreferencesPanel extends PreferencesPanel {

	private static final long serialVersionUID = 1L;
	
	private final JFormattedTextField sizeLimitField;
	private final JCheckBox queryWrappedCheckBox;
	private final JTextField queryWrapPreTextField;
	private final JTextField queryWrapPostTextField;
	private final JCheckBox connectionPersistentCheckBox;
	
	
	public QueryPreferencesPanel(final MyPreferences myPreferences, Component Container) {
		super(myPreferences, Container);
		
		/*
		 *  wrap queries sent to server
		 */
		Box panel = Box.createVerticalBox();
		panel.setBorder(Theme.getCentreBorder());
		connectionPersistentCheckBox = new JCheckBox();
		String tooltipText = "<html>qStudio will keep an open connection to the current server, <br/>" +
				" rather than opening a new connection with each query.</html>";
		panel.add(getFormRow(connectionPersistentCheckBox, "Keep Server Connection Open:", tooltipText));
		
		queryWrappedCheckBox = new JCheckBox();
		tooltipText = "<html>Wrapping a query allows protecting against large results freezing the GUI <br/>" +
		" and displaying exact text in the console panel. However protected <br/>" +
		"servers that restrict function calls may require this to be turned off.</html>";
		JLabel b = new JLabel(Theme.CIcon.INFO.get());
		b.setToolTipText(tooltipText);
		panel.add(getFormRow(queryWrappedCheckBox, "Wrap query sent to server:", tooltipText, b));
		queryWrappedCheckBox.setSelected(myPreferences.isQueryWrapped());
		queryWrappedCheckBox.addChangeListener(new ChangeListener() {
			
			@Override public void stateChanged(ChangeEvent e) {
				sizeLimitField.setEnabled(queryWrappedCheckBox.isSelected());
			}
		});
		
		// query object size limit
		sizeLimitField = new JFormattedTextField(Integer.valueOf(0));
		String sizeTT = "Queries will never return objects over this size." +
				" Useful to prevent lockups due to slow transfers.";
		panel.add(getFormRow(sizeLimitField, "Query Maximum Size Limit MB:",sizeTT));
		panel.add(Box.createVerticalStrut(10));
		
		
		Box queryWrapPanel = Box.createVerticalBox();
		queryWrapPanel.setBorder(BorderFactory.createTitledBorder("Query Wrapping"));
		String wrapTooltip = "Any queries sent to a kdb server will be wrapped with these pre/postfixes.";
		queryWrapPreTextField = new JTextField(20);
		queryWrapPostTextField = new JTextField(20);
		queryWrapPanel.add(getFormRow(queryWrapPreTextField, "Prefix:", wrapTooltip));
		queryWrapPanel.add(getFormRow(queryWrapPostTextField, "Postfix:", wrapTooltip));
		panel.add(queryWrapPanel);
		setLayout(new BorderLayout());
		add(panel, BorderLayout.NORTH);
		refresh();
	}
	

	/** Take all changes the user made and persist them.  */
	@Override void saveSettings() {
		
		int max = (Integer) sizeLimitField.getValue();
		myPreferences.setMaxReturnSizeMB(max);

		myPreferences.setQueryWrapped(queryWrappedCheckBox.isSelected());
		myPreferences.setQueryWrapPre(queryWrapPreTextField.getText());
		myPreferences.setQueryWrapPost(queryWrapPostTextField.getText());
		
		myPreferences.setConnectionPersistent(connectionPersistentCheckBox.isSelected());
	}
	

	/** Take the persisted values and show them values in the GUI */
	@Override void refresh() {
		
		int max = myPreferences.getMaxReturnSizeMB();
		sizeLimitField.setValue(Integer.valueOf(max));
		
		boolean wrapped = myPreferences.isQueryWrapped();
		queryWrappedCheckBox.setSelected(wrapped);
		sizeLimitField.setEnabled(wrapped);

		queryWrapPreTextField.setText(myPreferences.getQueryWrapPre());
		queryWrapPostTextField.setText(myPreferences.getQueryWrapPost());
		
		connectionPersistentCheckBox.setSelected(myPreferences.isConnectionPersistent());
	}

	/** undo any live settings changes that were made **/
	@Override void cancel() { }
}
