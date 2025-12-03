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
package com.timestored.pro.csvloader;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 * A textarea input, that accepts a folder location, with a browse button
 * on the RHS allowing the user to browse graphically for a file. 
 */
class JFilePicker extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JLabel label;
	private final JTextField textField;
	private final JButton button;

	private final JFileChooser fileChooser;

	static enum Mode {
		OPEN, SAVE
	};

	private Mode mode;

	JFilePicker(String textFieldLabel, String buttonLabel) {

		fileChooser = new JFileChooser();

		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		// creates the GUI
		label = new JLabel(textFieldLabel);

		textField = new JTextField(30);
		textField.setEditable(false);
		button = new JButton(buttonLabel);

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				buttonActionPerformed(evt);
			}
		});

		add(label);
		add(textField);
		add(button);

	}

	private void buttonActionPerformed(ActionEvent evt) {
		if (mode == Mode.OPEN) {
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				textField.setText(fileChooser.getSelectedFile()
						.getAbsolutePath());
			}
		} else if (mode == Mode.SAVE) {
			if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				textField.setText(fileChooser.getSelectedFile()
						.getAbsolutePath());
			}
		}
	}

	public void addFileFilter(FileFilter fileFilter) {
		fileChooser.addChoosableFileFilter(fileFilter);
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public String getSelectedFilePath() {
		return textField.getText();
	}

	public File getSelectedFile() {
		return fileChooser.getSelectedFile();
	}

	public void setSelectedFile(File file) {
		if (file != null && file.exists()) {
			if (!file.getAbsolutePath().equals(textField.getText())) {
				textField.setText(file.getAbsolutePath());
			}
		} else {
			textField.setText("");
		}
	}
	
	public void addActionListener(ActionListener actionListener) {
		fileChooser.addActionListener(actionListener);
	}
}