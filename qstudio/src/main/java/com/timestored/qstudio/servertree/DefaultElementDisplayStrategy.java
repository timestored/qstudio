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
package com.timestored.qstudio.servertree;

import static com.timestored.theme.Theme.GAP;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.activation.UnsupportedDataTypeException;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import jsyntaxpane.DefaultSyntaxKit;
import kx.c.KException;

import com.timestored.kdb.KdbConnection;
import com.timestored.qstudio.QStudioLauncher;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.theme.Theme;

/**
 * The fallback handler for showing a KDB entity, shows console representation.
 */
enum DefaultElementDisplayStrategy 
	implements ElementDisplayStrategy {
	
	INSTANCE;

	private static final String REFRESH_SERVER = "Unable to display object, <br />try refreshing" +
			" the server tree and viewing it again.";
	
	@Override
	public JPanel getPanel(AdminModel adminModel) {

		JPanel panel = new JPanel(new BorderLayout(GAP, GAP));
		Exception e = null;
		String errMsg = REFRESH_SERVER;
		
		if(adminModel.getSelectedElement() != null) {
			Object o;
			KdbConnection conn = null;
			try {
				String q = "-2_.Q.s " + adminModel.getSelectedElement().getFullName();
				conn = adminModel.getKdbConnection();
				if(conn==null) {
					panel.add(new JLabel("Could not get connection to server."));
				} else {
					o = conn.query(q);
					if(!(o instanceof char[])) {
						String msg = "DefaultElementDisplayStrategy expected char[] got: " 
								+ (o==null ? "null" : o.toString());
						throw new UnsupportedDataTypeException(msg);
					}
					String text = new String((char[]) o);
					DefaultSyntaxKit.initKit();
					JEditorPane codeEditor = new JEditorPane();
			        JScrollPane scrPane = new JScrollPane(codeEditor);
			        scrPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			        panel.add(scrPane, BorderLayout.CENTER);
			        codeEditor.setContentType("text/qsql");
					codeEditor.setText(text);
					codeEditor.setFont(UIManager.getFont("defaultFont"));
					codeEditor.setEditable(false);
	
				}
			} catch (KException ke) {
				e = ke;
				errMsg = "kdb exception:" + ke.getMessage() + " Check server settings and refresh.";
			} catch(Exception ee) {
				e = ee;
			} finally {
				if(conn!=null) {
					try {
						conn.close();
					} catch (IOException ioe) {
						// ignore
					}
				}
			}
			if(e != null) {
				panel.add(QStudioLauncher.ERR_REPORTER.getErrorReportLink(e, errMsg));
			}
		}
		return panel;
	}
}
