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
package com.timestored.connections;

import static com.timestored.theme.Theme.getFormRow;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.google.common.base.Preconditions;
import com.timestored.swingxx.SwingUtils;
import com.timestored.theme.Theme;


/**
 * Allows editing the login credentials for {@link ConnectionManager}.
 */
public class LoginDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	private static final int PAD = 10;
	private static final int WIDTH = 600;
	private static final int HEIGHT = 400;

	private final JTextField usernameField;
	private final JPasswordField passwordField;
	private final ConnectionManager conMan;


	/**
	 * @param frame Frame that I should be positioned relative to.
	 */
	public LoginDialog(ConnectionManager connectionManager, JFrame frame) {

		this.conMan = Preconditions.checkNotNull(connectionManager);
		setTitle("Default Database Server Login");
		setSize(WIDTH, HEIGHT);
		setIconImage(Theme.CIcon.SET_PASSWORD.get().getImage());
		setLayout(new BorderLayout());
		setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
		SwingUtils.addEscapeCloseListener(this);
		
		// construct appearance
		Box panel = Box.createVerticalBox();
		panel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

		String passTooltip = "Any servers that do not have a username / password specified will use these credentials.";
		usernameField = new JTextField(20);
		passwordField = new JPasswordField(20);
		usernameField.addActionListener(l -> saveAndClose());
		passwordField.addActionListener(l -> saveAndClose());
		panel.add(panel.createVerticalGlue());
		JLabel tooltipLabelA = new JLabel(Theme.CIcon.INFO.get());
		tooltipLabelA.setToolTipText(passTooltip);
		panel.add(getFormRow(usernameField, "Username:", passTooltip, tooltipLabelA));
		panel.add(panel.createVerticalStrut(20));
		panel.add(getFormRow(passwordField, "Password:", passTooltip));
		panel.add(panel.createVerticalGlue());
		
		
		add(panel, BorderLayout.CENTER);
		add(getCloseSaveBox(), BorderLayout.SOUTH);
		setLocationRelativeTo(frame);
		refresh();
		setVisible(true);
	}


	private void refresh() {
		usernameField.setText(conMan.getDefaultLoginUsername());
		passwordField.setText(conMan.getDefaultLoginPassword());
		
	}


	/** Take all changes the user made and persist them.  */
	private void saveSettings() {
		conMan.setDefaultLogin(usernameField.getText(), passwordField.getText());
	}


	private void saveAndClose() {
		saveSettings();
		LoginDialog.this.setVisible(false);
		LoginDialog.this.dispatchEvent(new WindowEvent(
				LoginDialog.this, WindowEvent.WINDOW_CLOSING));
	}

	private Box getCloseSaveBox() {
		// buttons
		JButton closeButton = new JButton(new AbstractAction("Cancel") {
			@Override public void actionPerformed(ActionEvent e) {
				LoginDialog.this.setVisible(false);
				LoginDialog.this.dispatchEvent(new WindowEvent(
						LoginDialog.this, WindowEvent.WINDOW_CLOSING));
            }
		});
		JButton saveButton = new JButton(new AbstractAction("Save") {
			@Override public void actionPerformed(ActionEvent e) {
				saveAndClose();
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
	
}
