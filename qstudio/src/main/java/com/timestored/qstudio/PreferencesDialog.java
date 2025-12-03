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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.timestored.swingxx.SwingUtils;
import com.timestored.theme.Theme;

/**
 * Allow modification of all {@link MyPreferences}.
 */
class PreferencesDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	private final MyPreferences myPreferences;
	private final List<PreferencesPanel> prefPanels = Lists.newArrayList();
	
	
	/**
	 * @param frame Frame that I should be positioned relative to.
	 */
	PreferencesDialog(final MyPreferences myPreferences, JFrame frame) {
		
		this.myPreferences = Preconditions.checkNotNull(myPreferences);
		setTitle("Preferences");
		SwingUtils.setSensibleDimensions(frame, this);
		setIconImage(Theme.CIcon.PREFERENCES.get().getImage());
		setLayout(new BorderLayout());
		setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
		
		// construct appearance
		JTabbedPane tabpane = new JTabbedPane();

		AppearancePreferencesPanel appearancePreferencesPanel = new AppearancePreferencesPanel(myPreferences, frame);
		tabpane.add("Appearance", appearancePreferencesPanel);
		prefPanels.add(appearancePreferencesPanel);
		
		QueryPreferencesPanel queryPanel = new QueryPreferencesPanel(myPreferences, this);
		tabpane.add("Connections", queryPanel);
		prefPanels.add(queryPanel);
		
		PreferencesPanel miscPanel = new MiscPreferencesPanel(myPreferences, this);
		tabpane.add("Misc", miscPanel);
		prefPanels.add(miscPanel);
		
		EditorThemePreferencesPanel editorThemePanel = new EditorThemePreferencesPanel(myPreferences, this);
		tabpane.add("Editor Theme", editorThemePanel);
		prefPanels.add(editorThemePanel);
		
		add(tabpane, BorderLayout.CENTER);
		add(getCloseSaveBox(), BorderLayout.SOUTH);
		setLocationRelativeTo(frame);
		
		setVisible(true);
	}


	private Box getCloseSaveBox() {
		// buttons
		JButton closeButton = new JButton(new AbstractAction("Cancel") {
			@Override public void actionPerformed(ActionEvent e) {
				
				for(PreferencesPanel pp : prefPanels) {
					pp.cancel();
					myPreferences.notifyListeners();
				}
				
				PreferencesDialog.this.setVisible(false);
				PreferencesDialog.this.dispatchEvent(new WindowEvent(
						PreferencesDialog.this, WindowEvent.WINDOW_CLOSING));
            }
		});
		JButton saveButton = new JButton(new AbstractAction("Save") {
			@Override public void actionPerformed(ActionEvent e) {

				for(PreferencesPanel pp : prefPanels) {
					pp.saveSettings();
					myPreferences.notifyListeners();
				}
				
				PreferencesDialog.this.setVisible(false);
				PreferencesDialog.this.dispatchEvent(new WindowEvent(
						PreferencesDialog.this, WindowEvent.WINDOW_CLOSING));
            }
		});
		Box closeBox = Box.createHorizontalBox();
		closeBox.add(Box.createHorizontalGlue());
		closeBox.add(saveButton);
		closeBox.add(Box.createHorizontalGlue());
		closeBox.add(closeButton);
		closeBox.add(Box.createHorizontalGlue());
		return closeBox;
	}

	
	private static class MiscPreferencesPanel extends PreferencesPanel {
		
		private static final long serialVersionUID = 1L;
		private final JCheckBox saveWithWindowsLineEndingsCheckBox;
		private final JTextField hideNsTextField;
		private final JTextField hideFoldersTextField;
		private final JTextField openaiKeyField;

		public MiscPreferencesPanel(MyPreferences myPreferences, Component container) {
			super(myPreferences, container);
			Box panel = Box.createVerticalBox();
			panel.setBorder(Theme.getCentreBorder());
			
			saveWithWindowsLineEndingsCheckBox = new JCheckBox();
			String tooltipText = "<html>Files will be saved with \\r\\n line endings,<br/>" +
					" compared to \\n standard found on linux/mac</html>";
			panel.add(getFormRow(saveWithWindowsLineEndingsCheckBox, "Save with \\r\\n Windows Line Ending:", tooltipText));
			panel.add(Box.createVerticalStrut(10));

			/* hide namespace config */
			hideNsTextField = new JTextField(20);
			tooltipText = "<html>These namespaces will not be displayed in the server tree panel.</html>";
			panel.add(getFormRow(hideNsTextField, "Hidden Namespaces:", null));
			panel.add(Box.createVerticalStrut(10));			

			/* hide namespace config */
			hideFoldersTextField = new JTextField(20);
			tooltipText = "<html>These folders will not be searched when opening files using the Command Bar (Ctrl+P).</html>";
			panel.add(getFormRow(hideFoldersTextField, "Regex Filter To Hide Folders:", tooltipText));
			panel.add(Box.createVerticalStrut(10));
			

			JLabel label = new JLabel("This will send details on your database to OpenAI!!!");
			label.setForeground(Color.RED);
			panel.add(getFormRow(label, "Warning:", null));
			panel.add(getFormRow(new JLabel("Do NOT add a key unless you are happy to send those details."), "Warning:", null));
			openaiKeyField = new JTextField(40);
			tooltipText = "<html>This key will be used to generate AI queries.</html>";
			panel.add(getFormRow(openaiKeyField, "OpenAI Key:", null));
			panel.add(Box.createVerticalStrut(10));	
			
			setLayout(new BorderLayout());
			add(panel, BorderLayout.NORTH);
			
			refresh();
		}

		@Override void refresh() {
			saveWithWindowsLineEndingsCheckBox.setSelected(myPreferences.isSaveWithWindowsLineEndings());

			String hid = Joiner.on(" ").join(myPreferences.getHiddenNamespaces());
			hideNsTextField.setText(hid);
			openaiKeyField.setText(myPreferences.getOpenAIkey());
			
			hideFoldersTextField.setText(myPreferences.getIgnoreFilterRegex());
		}

		@Override void saveSettings() {
			myPreferences.setSaveWithWindowsLineEndings(saveWithWindowsLineEndingsCheckBox.isSelected());
			String[] ns = hideNsTextField.getText().split(" ");
			myPreferences.setHiddenNamespaces(ns);
			myPreferences.setOpenAIkey(openaiKeyField.getText().trim());
			
			myPreferences.setIgnoreFilterRegex(hideFoldersTextField.getText());
		}

		@Override void cancel() { }
		
	}
}
