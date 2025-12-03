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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import jsyntaxpane.DefaultSyntaxKit;
import kx.c.KException;

import com.timestored.kdb.KdbConnection;
import com.timestored.qstudio.QStudioLauncher;
import com.timestored.qstudio.kdb.KdbHelper;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.theme.Theme;



class FunctionEditingPanel extends JPanel {

	private static final Logger LOG = Logger.getLogger(FunctionEditingPanel.class.getName());
	
	private static final long serialVersionUID = 1L;
	private final String queryName;
	private final JEditorPane codeEditor;
	private final JPanel descContainerPanel;
	private String lastSeenText = "";
	private FunctionValue funcVal;
	private final AdminModel adminModel;

	public FunctionEditingPanel(AdminModel adminModel, 
			String queryName) throws IOException, KException {

		this.queryName = queryName;
		this.adminModel = adminModel;
		setLayout(new BorderLayout(GAP, GAP));

		// create toolbar
		JToolBar toolbar = new JToolBar();
		final JButton saveButton = new JButton("Save", Theme.CIcon.SAVE.get());
		saveButton.setEnabled(false);
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		JButton deleteButton = new JButton("Delete", Theme.CIcon.DELETE.get());
		toolbar.add(saveButton);
		toolbar.add(deleteButton);
		
		// create editor
		DefaultSyntaxKit.initKit();
		codeEditor = new JEditorPane();
        JScrollPane scrPane = new JScrollPane(codeEditor);
        scrPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        codeEditor.setContentType("text/qsql");
		codeEditor.setText("");
		codeEditor.setFont(UIManager.getFont("defaultFont"));
		codeEditor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				saveButton.setEnabled(!codeEditor.getText().equals(lastSeenText));
			}
		});
		

        descContainerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        add(descContainerPanel, BorderLayout.NORTH);
        JPanel p = new JPanel(new BorderLayout());
        p.add(scrPane, BorderLayout.CENTER);
        p.add(toolbar, BorderLayout.SOUTH);
        add(p, BorderLayout.CENTER);
        
		
		refresh();
	}

	private void save() {
		try {
			String saveQuery = "{system \"d ." + funcVal.namespace + "\";"
					+ "set[`" 
					+ adminModel.getSelectedElement().getName() + "; value\"" 
					+ KdbHelper.escape(codeEditor.getText()) + "\"];"
					+ "system \"d .\"}[]";
			LOG.info("saveQuery = " + saveQuery);
			adminModel.getKdbConnection().query(saveQuery);
//			kdbSOModel.refreshTree();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Error saving function");
		} catch (KException e) {
			JOptionPane.showMessageDialog(null, e.toString());
		}
		refresh();
	}

	private void refresh() {

		Object res;
		Exception e = null;
		try {
			KdbConnection conn = adminModel.getKdbConnection();
			
			descContainerPanel.removeAll();
			
			if(conn!=null) {
				res = conn.query(FunctionValue.GET_DEF_Q + queryName);
				
				funcVal = new FunctionValue(res);
				JPanel descriptionPanel = new JPanel(new GridLayout(4, 2, Theme.GAP, Theme.GAP));
				addToGrid(descriptionPanel, "Parameters:", csvSeparate(funcVal.params));
				addToGrid(descriptionPanel, "Locals:", csvSeparate(funcVal.locals));
				addToGrid(descriptionPanel, "Globals:", csvSeparate(funcVal.globals));
				addToGrid(descriptionPanel, "Namespace:", funcVal.namespace);
				descContainerPanel.add(descriptionPanel);
				
				lastSeenText  = funcVal.definition;
				codeEditor.setText(funcVal.definition);
			} else {
				codeEditor.setText("Could not get connection");
			}
		} catch (Exception ex) {
			e = ex;
		}		
		
		if(e != null) {
			String shortDesc = "Error showing selected item, try refreshing the servers object tree.";
			codeEditor.setText(shortDesc);
			LOG.log(Level.WARNING, shortDesc, e);
			add(QStudioLauncher.ERR_REPORTER.getErrorReportLink(e, shortDesc));
		}
		
		revalidate();	
	}
		
	private void addToGrid(JPanel gridPanel, String title,
			String value) {
		JLabel titleLabel = new JLabel(title);
		Font f = titleLabel.getFont();
		f = new Font(f.getName(), Font.BOLD, f.getSize());
		titleLabel.setFont(f);
		gridPanel.add(new JLabel(title));
		gridPanel.add(new JLabel(value));
	}

	private String csvSeparate(String[] stringA) {
		String wholeString = "";
		
		if(stringA!=null && stringA.length > 0) {
			for(int i = 0; i<stringA.length-1; i++) {
				wholeString += stringA[i] + ", ";
			}
			wholeString += stringA[stringA.length-1];
		}
		return wholeString;
	}

	/**
	 * Allows parsing a function to get its meta data, locals,params, etc..
	 */
	private class FunctionValue {
		
		public static final String GET_DEF_Q = "1_value ";
		
		public final String[] params;
		public final String[] locals;
		public final String[] globals;
		public final Object[] definedValues;
		public final String definition;
		public final String namespace;
		
		/**
		 * Can easily throw {@link IndexOutOfBoundsException} etc as it tries to process kdb object
		 * @param kObject
		 */
		public FunctionValue(Object kObject) {
			
			Object[] data = (Object[]) kObject;
			params = (String[]) data[0];
			locals = (String[]) data[1];
			
			String[] temp = (String[]) data[2];
			globals = new String[temp.length-1];
			for(int i=1; i<temp.length; i++) {
				globals[i-1] = temp[i];
			}
			namespace = temp[0];
			
			definedValues = new Object[data.length - 4]; 
			for(int i=0; i<definedValues.length; i++) {
				definedValues[i] = data[2+i];
			}
			definition = new String((char[])data[data.length - 1]);
		}
		
		
	}
}
