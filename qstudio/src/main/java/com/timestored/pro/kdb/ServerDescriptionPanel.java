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
package com.timestored.pro.kdb;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import com.timestored.TimeStored;
import com.timestored.kdb.SysCommand;
import com.timestored.misc.InfoLink;
import com.timestored.qstudio.QStudioLauncher;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.ServerModel;
import com.timestored.qstudio.model.ServerObjectTree;
import com.timestored.qstudio.model.ServerReport;
import com.timestored.qstudio.model.ServerSlashConfig;
import com.timestored.theme.Theme;

/**
 * Displays server properties, configuration, event handlers, open connections.
 * Also allows editing of the same.
 */
class ServerDescriptionPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final ServerReportpanel serverReportPanel;
	private final ServerConfigPanel serverConfigPanel;
	private JTabbedPane tabpane;

	private final ServerModel serverModel;
	private final AdminModel adminModel;

	/**
	 * Display the {@link ServerModel} as it is. If you want this to display the latest, you should
	 * send it a refresh request after creating this panel.
	 * @param serverModel
	 */
	public ServerDescriptionPanel(final AdminModel adminModel) {
		
		this.adminModel = adminModel;
		this.serverModel = adminModel.getServerModel();
		setLayout(new BorderLayout());
		tabpane = new JTabbedPane();
		
		serverReportPanel = new ServerReportpanel();
		serverConfigPanel = new ServerConfigPanel();
		
		tabpane.addTab("Info", scrollWrap(serverReportPanel));
		tabpane.addTab("Configuration", scrollWrap(serverConfigPanel));
		// If you change this order make sure to change setComponentAt in code below
		tabpane.addTab("Server", scrollWrap(getServerListing(adminModel)));
		
		add(tabpane, BorderLayout.CENTER);
		
		serverModel.addListener(new ServerModel.Listener() {
			@Override public void changeOccurred() {
				refreshGui();
			}
		});

		
		// show what we had before but in background ask for refresh model.
		refreshGui();
	}

	private static JPanel getServerListing(AdminModel adminModel) {
		JPanel bp = new JPanel();
//		bp.add(Theme.getSubHeader(adminModel.getSelectedNamespace()));
		ServerModel serverModel = adminModel.getServerModel();
		if(serverModel != null && serverModel.getServerObjectTree() != null) {
			String serverName = adminModel.getSelectedServerName();
			ServerObjectTree soTree = serverModel.getServerObjectTree();
			for(String ns : soTree.getNamespaces()) {
				JButton b = new JButton(ns);
				b.addActionListener(e -> {
					adminModel.setSelectedNamespace(serverName, ns);
				});
				bp.add(b);
			}
		}
		
		return bp;
	}
	
	private void refreshGui() {
		EventQueue.invokeLater(new Runnable() {

			@Override public void run() {
				removeAll();
				if(serverModel.getServerConfig().isKDB() && serverModel.isConnected()) {
					serverReportPanel.display(serverModel.getServerReport());
					serverConfigPanel.display(serverModel.getSlashConfig());
					tabpane.setComponentAt(2, getServerListing(adminModel));
					add(tabpane, BorderLayout.CENTER);
				} else {
					String msg = "Server: " + serverModel.getName() + (serverModel.getServerConfig().isKDB() ? " not connected" : "");
					JPanel p = new JPanel(new BorderLayout());
					p.add(Theme.getHeader(msg));
					add(p, BorderLayout.CENTER);
				}
			}
		});
	}
	
	private static JScrollPane scrollWrap(JPanel panel) {
		return new JScrollPane(panel, 
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

	}
	
	/** Panel displaying memory / server info */
	private static class ServerReportpanel extends Theme.BoxyPanel {
		
		private static final long serialVersionUID = 1L;

		private static final String MEM_DESCRIPTION = "<html><b>used</b> - number of bytes allocated. \r\n"
				+ "<br><b>heap</b> - bytes available in heap. \r\n"
				+ "<br><b>peak</b> - maximum heap size so far. \r\n"
				+ "<br><b>wmax</b> - maximum bytes available, given in -w command line parameter. \r\n"
				+ "<br><b>wmap</b> - mapped bytes. \r\n"
				+ "<br><b>mphy</b> - physical memory.";
		
		public ServerReportpanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			
		}
		
		public void display(ServerReport serverReport) {
			removeAll();
			if(serverReport != null) {
				JPanel tabPanel = Theme.getPlainReadonlyTable(serverReport.getGeneralInfoTable());
		    	add(Theme.wrap("Server Report", tabPanel));

				JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				infoPanel.add(InfoLink.getButton("How Memory Management Works", MEM_DESCRIPTION, 
						TimeStored.Page.TUTE_MEM));
				add(Theme.wrap("Memory Usage", serverReport.getMemTab(), infoPanel));
				add(Theme.wrap("Table Storage", serverReport.getDiskTab()));
				add(Box.createGlue());
			} else {
				String msg = "Could not retrieve Server Info, check server security settings.";
				add(QStudioLauncher.ERR_REPORTER.getErrorReportLink(msg, msg));
			}
		}
	}


	private static class ServerConfigPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private final JPanel conPanel;
		
		public ServerConfigPanel() {
			setLayout(new BorderLayout());
			JPanel headerPanel = Theme.getSubHeader("System Commands");
			add(headerPanel, BorderLayout.NORTH);
			
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
			conPanel = new JPanel(new GridLayout(12, 2));
			p.add(conPanel);
			add(p, BorderLayout.CENTER);
		}
		
		public void display(ServerSlashConfig serverConfig) {
			conPanel.removeAll();
			
			if(serverConfig != null) {

				conPanel.removeAll();
				List<String> sysStr = Arrays.asList(new String[] {"c","C","e","g","o","p","P","s","t","T","W","z" });
				
				for(String sysCmd : sysStr) {
					addRow(conPanel, SysCommand.get(sysCmd), serverConfig);
				}
				
			} else {
				String msg = "Could not retrieve Server Config, check server security settings.";
				conPanel.add(QStudioLauncher.ERR_REPORTER.getErrorReportLink(msg, msg));
			}
		}
		
		private static void addRow(JPanel conPanel, final SysCommand scmd, 
				final ServerSlashConfig servCfg) {
			
			if(scmd != null) {

				JLabel descLabel = new JLabel("\\" + scmd.getCommand() 
						+ "    " + scmd.getShortDesc());
				conPanel.add(descLabel);
				
				JPanel panel = new JPanel(new BorderLayout());
				String curVal = servCfg.getVal(scmd);
				final JTextField curValTextField = new JTextField(curVal == null ? "" : curVal);
				curValTextField.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						servCfg.setVal(scmd, curValTextField.getText());
					}
				});
				curValTextField.setEditable(scmd.isWritable() && curVal!=null);
				panel.add(curValTextField, BorderLayout.CENTER);
				
				String title = "\\" + scmd.getCommand() + " " + scmd.getArgs();
				Component infoLink = InfoLink.getLabel(title, scmd.getLongDesc(), scmd.getUrl(), false);
				
				
				panel.add(infoLink, BorderLayout.EAST);
				panel.setAlignmentX(Component.LEFT_ALIGNMENT);
				conPanel.add(panel);
			}
		}
	}




	

}
