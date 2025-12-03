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


import static com.timestored.theme.Theme.makeButton;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.google.common.base.Preconditions;
import com.timestored.TimeStored;
import com.timestored.TimeStored.Page;
import com.timestored.connections.Msg.Key;
import com.timestored.misc.HtmlUtils;
import com.timestored.misc.InfoLink;
import com.timestored.misc.TextWrapper;
import com.timestored.plugins.PluginLoader;
import com.timestored.swingxx.ColorChooserPanel;
import com.timestored.swingxx.SwingUtils;
import com.timestored.theme.Theme;
import com.timestored.theme.Theme.InputLabeller;

import lombok.RequiredArgsConstructor;

/**
 * Allows editing an existing {@link ServerConfig} or adding a new one,
 * in a {@link ConnectionManager}.
 */
public class ConnectionManagerDialog extends JDialog implements ActionListener {
	
	private static final Logger LOG = Logger.getLogger(ConnectionManagerDialog.class.getName());
	
	private static final long serialVersionUID = 1L;
	private static final InputLabeller INPUT_LABELLER = Theme.getInputLabeller(120, 20);
	private static final int DEF_COLUMNS = 50;
	
	private final ConnectionManager conMan;

	private final JTextField hostTextField;
	private final JTextField portTextField;
	private final JPasswordField passwordTextField;
	private final JTextField usernameTextField;
	private final JTextField nameTextField;
	private final JComboBox serverTypeComboBox;
	private final JCheckBox useTLSCheckbox;
	private final Box useTLSBox;
	private final JCheckBox useAsyncCheckbox;
	private final Box useAsyncBox;
	private final List<JdbcTypes> jdbcTypesShown;
	private final List<String> niceDBnames;
	private final ColorChooserPanel colorChooserPanel;
	private final JTextField databaseTextField;
	private JPanel hostPanel;
	private JPanel portPanel;
	private final JPanel  databasePanel;
	private JPanel loginPanel;
	private JComboBox folderComboBox;
	
	/** holds the name of the existing server that was passed in for editing */
	private final String serverName;
	/** If this is an edit, this holds the {@link ServerConfig} details */
	private ServerConfig serverConfig;
	private final HighlightTextField urlTextField;
	private final JRadioButton hostButton;
	private final JRadioButton urlButton;
	
	/**
	 * Add or Edit an existing {@link ServerConfig} details.
	 * @param parent The frame that contains this dialog.
	 * @param serverName name of existing server that will be edited, 
	 * or null if you want to add a new one.
	 */
	public ConnectionManagerDialog(ConnectionManager connectionManager, JFrame parent, String serverName) {
		
		super(parent, "Server Properties");
		// KDB to top then alphabetical
		List<JdbcTypes> allDBs = Arrays.asList(JdbcTypes.values());
		Collections.sort(allDBs, (a,b) -> {
			return a.equals(b) ? 0 : a.equals(JdbcTypes.KDB) ? -1 : b.equals(JdbcTypes.KDB) ? 1 : a.getNiceName().compareTo(b.getNiceName());		
		});
		Map<String, JdbcIcons> jdbcNameToIcon = new HashMap<>();
		niceDBnames = new ArrayList<>();
		allDBs.stream().forEach(j -> { 
			if(j.isAvailable()) { 
				niceDBnames.add(j.getNiceName());
				jdbcNameToIcon.put(j.getNiceName(), JdbcIcons.getIconFor(j));
			}  
		});
		niceDBnames.add("--------------------");
		allDBs.stream().forEach(j -> {  
			if(!j.isAvailable() && !j.equals(JdbcTypes.YANDEX_CLICKHOUSE)) { 
				niceDBnames.add(j.getNiceName());
				jdbcNameToIcon.put(j.getNiceName(), JdbcIcons.getIconFor(j));
			}    
		});
		jdbcTypesShown = allDBs;
		
		setIconImage(Theme.CIcon.SERVER_EDIT.get().getImage());
		this.serverName = serverName;
		this.conMan = Preconditions.checkNotNull(connectionManager);
		
		// if existing one wanted for editing, try to get it
		serverConfig = null;
		if(serverName != null) {
			serverConfig = conMan.getServer(serverName);
			if(serverConfig==null) {
				throw new IllegalArgumentException("Server not found in Connection Manager");
			}
		}
		
		
		// set general appearance
		setResizable(false);
		SwingUtils.setSensibleDimensions(parent, this);
		setLocationRelativeTo(parent);
		setLayout(new BorderLayout());
		setModalityType(ModalityType.APPLICATION_MODAL);
		Box cp = Box.createVerticalBox();
		SwingUtils.addEscapeCloseListener(this);
		
		
		// add components
		JPanel connPanel = new JPanel();
		connPanel.setBorder(new TitledBorder(null, "Connection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		connPanel.setLayout(new BoxLayout(connPanel, BoxLayout.PAGE_AXIS));

		serverTypeComboBox = new JComboBox(niceDBnames.toArray(new String[] {}));
		serverTypeComboBox.setRenderer(new IconListRenderer(jdbcNameToIcon));
		
		connPanel.add(INPUT_LABELLER.get("Server Type:", serverTypeComboBox, "serverTypeDropdown"));		
		
		Component verticalBox = Box.createVerticalStrut(10);
		connPanel.add(verticalBox);
		
		hostButton = new JRadioButton("Host");
		urlButton = new JRadioButton("URL");
		hostButton.setBounds(75,50,100,30);    
		urlButton.setBounds(75,100,100,30);    
		ButtonGroup bg=new ButtonGroup();
		hostButton.setSelected(true);
		bg.add(hostButton);
		bg.add(urlButton);
		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radioPanel.add(hostButton);
		radioPanel.add(urlButton);
		connPanel.add(INPUT_LABELLER.get("Connect By:", radioPanel, "radioPanel"));

		urlTextField = new HighlightTextField("jdbc:");
		urlTextField.setColumns(DEF_COLUMNS);
		urlTextField.addActionListener(this); // save if enter pressed
		connPanel.add(INPUT_LABELLER.get("<html><b>URL</b></html>", urlTextField, "urlTextField"));
		
		hostTextField = new HighlightTextField("localhost");
		hostTextField.setColumns(DEF_COLUMNS);
		hostTextField.addActionListener(this); // save if enter pressed
		String hostLbl = "<html><b>" + Msg.get(Key.HOST) + ":</b></html>";
		hostPanel = INPUT_LABELLER.get(hostLbl, hostTextField, "hostField");
		connPanel.add(hostPanel);
		
		portTextField = new HighlightTextField("5000");
		portTextField.setColumns(10);
		portTextField.addActionListener(this); // save if enter pressed
		portPanel = INPUT_LABELLER.get("<html><b>" + Msg.get(Key.PORT) + ":</b></html>", portTextField, "portField");
		connPanel.add(portPanel);
		
		nameTextField = new HighlightTextField("");
		String nameLbl = "<html><b>Name:</b></html>";
		connPanel.add(INPUT_LABELLER.get(nameLbl, nameTextField, "serverNameField"));
		nameTextField.addActionListener(this); // save if enter pressed

		useTLSCheckbox = new JCheckBox();
		String tlsTooltipText = "<html>Since V3.4t 2016.05.12, kdb+ can use Secure Sockets Layer (SSL)/Transport Layer Security (TLS)"
				+ "<br> to encrypt connections using the OpenSSL libraries.</html>";
		JLabel tlsButton = InfoLink.getLabel("", "See SSL/TLS Docs", TimeStored.Page.QSTUDIO_HELP_SSL, true);
		useTLSBox = Theme.getFormRow(useTLSCheckbox, "Use kdb+ TLS", tlsTooltipText, tlsButton);
		connPanel.add(useTLSBox);
		
		
		useAsyncCheckbox = new JCheckBox();		
		JLabel asyncWarningLabel = new JLabel(Theme.CIcon.WARNING.get16());
		String asyncTooltipText = "<html>Send messages asynchronously then force sync.<br>This setting is experimental, please report any issues.</html>";
		asyncWarningLabel.setToolTipText(asyncTooltipText);
		useAsyncBox = Theme.getFormRow(useAsyncCheckbox, "Use kdb+ Async", asyncTooltipText, asyncWarningLabel);
//		connPanel.add(useAsyncBox);
		
		databasePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		serverTypeComboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				String niceName = (String) serverTypeComboBox.getSelectedItem();
				JdbcTypes t = jdbcTypesShown.stream().filter(j -> j.getNiceName().equals(niceName)).findFirst().get();
				boolean isEmbed = t.getDefaultPort() == 0;
				urlButton.setSelected(isEmbed);
				hostButton.setSelected(!isEmbed);
				enableHostPort(!isEmbed);
				
				useTLSBox.setVisible(t.isKDB());
				useAsyncBox.setVisible(t.isKDB());
				
				databasePanel.setVisible(t.isDatabaseRequired());
				portTextField.setText(""+t.getDefaultPort());
				urlTextField.setText(""+t.getSampleURL());
				
			}
		});		

		// database - optional depending on type of JDBC
		connPanel.add(databasePanel);

		databaseTextField = new HighlightTextField("");
		databasePanel.add(INPUT_LABELLER.get(Msg.get(Key.DATABASE) + ":", databaseTextField, "dbField"));
		
		// LOGIN
		loginPanel = new JPanel();
		loginPanel.setBorder(new TitledBorder(null, "Login", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.PAGE_AXIS));

		usernameTextField = new HighlightTextField("");
		usernameTextField.addActionListener(this); // save if enter pressed
		loginPanel.add(INPUT_LABELLER.get(Msg.get(Key.USERNAME) + ":", usernameTextField, "usernameField"));
		
		passwordTextField = new JPasswordField(DEF_COLUMNS);
		loginPanel.add(INPUT_LABELLER.get(Msg.get(Key.PASSWORD) + ":", passwordTextField, "passwordField"));
		passwordTextField.setColumns(DEF_COLUMNS);
		passwordTextField.addActionListener(this); // save if enter pressed
		
//		JPanel panel_5 = getLeftAlignPanel();
//		loginPanel.add(panel_5);
//		
//		JLabel lblAuthMethod = new JLabel("Auth. Method:");
//		lblAuthMethod.setPreferredSize(new Dimension(LBL_WIDTH, LBL_HEIGHT));
//		panel_5.add(lblAuthMethod);
//		
//		JComboBox authMethodComboBox = new JComboBox();
//		lblAuthMethod.setLabelFor(authMethodComboBox);
//		panel_5.add(authMethodComboBox);
		
		JPanel nameColorPanel = new JPanel();
		nameColorPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		nameColorPanel.setLayout(new BoxLayout(nameColorPanel, BoxLayout.PAGE_AXIS));

		colorChooserPanel = new ColorChooserPanel(ConnectionManagerDialog.this);
		colorChooserPanel.setColor(ServerConfig.DEFAULT_COLOR);
		nameColorPanel.add(INPUT_LABELLER.get("Background:", colorChooserPanel, "colorButton"));
		
		
		folderComboBox = new JComboBox();
		folderComboBox.setEditable(true);
		nameColorPanel.add(INPUT_LABELLER.get("Folder:", folderComboBox, "folderComboBox"));
		
		JPanel buttonPanel = new JPanel(); 
		
		buttonPanel.add(makeButton(serverConfig==null ? Msg.get(Key.ADD) : Msg.get(Key.SAVE), this));
		
		// if this is an edit, show the delete button
		if(serverConfig!=null) {
			ActionListener deleteListener = new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					Component parent = ConnectionManagerDialog.this;
					try {
						int reply = JOptionPane.showConfirmDialog(parent, 
								"Are you sure you want to delete this server?");
						if(reply == JOptionPane.YES_OPTION) {
							conMan.removeServer(serverConfig);
							closeDialog();
						}
					} catch (Exception e) {
						String msg = "Possible problem deleting server.";
						JOptionPane.showMessageDialog(parent, msg, 
								"Delete error", JOptionPane.ERROR_MESSAGE);
						LOG.log(Level.SEVERE, msg, e);
					}
				}
			};
			buttonPanel.add(makeButton(Msg.get(Key.DELETE), deleteListener));
		}
		
		// add a test connection button
		Action dispatchTest = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) {
				Component parent = ConnectionManagerDialog.this;
				ServerConfig newSC = newServerConfig();
				
				try {
					conMan.testConnection(newSC);
					String message = "Connection works";
					JOptionPane.showMessageDialog(parent,	message);
				} catch (IOException ioe) {
					if(ioe.toString().contains("ClassNotFoundException")) {
	        			offerToInstallDriver(newSC.getJdbcType(), ConnectionManagerDialog.this);
					} else {
						String message = "Connection does not work.";
						String fullMsg = TextWrapper.forWidth(80).hard().wrap(message + " " + ioe.toString());
						JOptionPane.showMessageDialog(parent, fullMsg, message, JOptionPane.WARNING_MESSAGE);
						LOG.log(Level.INFO, message, ioe);
					}
				}
			}
		};
		buttonPanel.add(makeButton("Test", dispatchTest));
		

		Action dispatchClose = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) {
				JDialog d = ConnectionManagerDialog.this;
				d.dispatchEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));
			}
		};
		
		// add a cancel button
		buttonPanel.add(makeButton(Msg.get(Key.CANCEL), dispatchClose));
		
		cp.add(connPanel);
		cp.add(loginPanel);
		cp.add(nameColorPanel);
		
//		cp.add(buttonPanel);
		add(SwingUtils.verticalScrollPane(cp), BorderLayout.CENTER);
		buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
		add(buttonPanel, BorderLayout.SOUTH);

		hostButton.addActionListener(e -> {
			enableHostPort(true);
			hostTextField.setText("");
			if(portTextField.getText().equals("0")) {
				String p = serverConfig == null ? "1233" : ""+serverConfig.getJdbcType().getDefaultPort();
				portTextField.setText(p);
			}
		});
		urlButton.addActionListener(e -> {
			enableHostPort(false);
			portTextField.setText("0");
		});
		
		// cancel when escape pressed
		SwingUtils.addEscapeCloseListener(this);
		showConnection(serverConfig);
	}
	
	private void enableHostPort(boolean enableHostPort) {
		hostPanel.setVisible(enableHostPort);
		portPanel.setVisible(enableHostPort);
		
		String niceName = (String) serverTypeComboBox.getSelectedItem();
		JdbcTypes t = jdbcTypesShown.stream().filter(j -> j.getNiceName().equals(niceName)).findFirst().get();
		databasePanel.setVisible(enableHostPort && t.isDatabaseRequired());
		
		urlTextField.setEnabled(!enableHostPort);
		urlTextField.setEditable(!enableHostPort);
	}

	/** JTextfield that highlights its text when focused */
	private static class HighlightTextField  extends JTextField {

		public HighlightTextField(String text) {	
			setText(text);
			setColumns(DEF_COLUMNS);
		}
		
		@Override protected void processFocusEvent(FocusEvent e) {
			super.processFocusEvent(e);
			if (e.getID() == FocusEvent.FOCUS_GAINED)
				selectAll();
		}
	}
	
	protected void closeDialog() {
	    WindowEvent wev = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
	    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
	}

	/** show the details for a given {@link ServerConfig} in the GUI */
	private void showConnection(ServerConfig sc) {
		enableHostPort(true);
		hostButton.setSelected(true);
		urlButton.setSelected(false);
		if(sc!=null) {
			// this must be set early, as when it changes, it's listener
			// sets other fields including the port, which caused a bug.
			serverTypeComboBox.setSelectedItem(sc.getJdbcType().getNiceName());
			
			hostTextField.setText(sc.getHost());
			portTextField.setText(""+sc.getPort());
			usernameTextField.setText(sc.getUsername());
			passwordTextField.setText(sc.getPassword());
			loginPanel.setVisible(!sc.getJdbcType().equals(JdbcTypes.DUCKDB));
			nameTextField.setText(sc.getShortName());
			databaseTextField.setText(sc.getDatabase());
			urlTextField.setText(sc.getJdbcType().getSampleURL());
			colorChooserPanel.setColor(sc.getColor());
			databasePanel.setVisible(sc.getJdbcType().isDatabaseRequired());
			setFolder(sc.getFolder());
			useAsyncCheckbox.setSelected(sc.isUseAsync());
			useTLSCheckbox.setSelected(sc.isUseTLS());
			
			if(sc.getPort() == 0) {
				enableHostPort(false);
				hostButton.setSelected(false);
				urlButton.setSelected(true);
				databaseTextField.setText("");
				urlTextField.setText(sc.getDatabase());
			}
		} else {
			folderComboBox.setSelectedItem("");
			// if adding a server, place defaulter user/pass in GUI box
			if(conMan.isDefaultLoginSet()) {
				usernameTextField.setText(conMan.getDefaultLoginUsername());
				passwordTextField.setText(conMan.getDefaultLoginPassword());
			}
		}
	}


	/** get a server config based on the currently typed in details */
	private ServerConfig newServerConfig() {
		String niceName = (String) serverTypeComboBox.getSelectedItem();
		JdbcTypes t = jdbcTypesShown.stream().filter(j -> j.getNiceName().equals(niceName)).findFirst().get();
		String host = hostTextField.getText().trim();
		int port = Integer.parseInt(portTextField.getText().trim());
		String username = usernameTextField.getText();
		String password = new String(passwordTextField.getPassword());
		
		String folder = folderComboBox!=null ? (String)folderComboBox.getSelectedItem() : "";
		String name = nameTextField.getText();
		
		String database = databaseTextField.getText();
		Color c = colorChooserPanel.getColor();
		if(c == ServerConfig.DEFAULT_COLOR) {
			c = null;
		}
		
		if(urlButton.isSelected()) {
			port = 0;
			database = urlTextField.getText();
		}
		
		boolean useTLS = useTLSCheckbox.isSelected();
		boolean useAsync = useAsyncCheckbox.isSelected();
		return new ServerConfig(host, port, username, password, name, t, c, database, folder, useTLS, useAsync );
	}
	
	@Override public void actionPerformed(ActionEvent e) {
		// called by save button
		
		try {
			if(serverConfig!=null) {
				conMan.updateServer(serverName, newServerConfig());
			} else {
				ServerConfig sc = newServerConfig();
				// if user is adding server with same user/pass as default. blank it out.
				if(!conMan.isDefaultLoginSet()) {
					conMan.setDefaultLogin(sc.getUsername(), sc.getPassword());
				} else {
					if(sc.getUsername().equals(conMan.getDefaultLoginUsername())
						&& sc.getPassword().equals(conMan.getDefaultLoginPassword())) {
						sc = new ServerConfigBuilder(sc).setUsername("").setPassword("").build();
					}
				}
				conMan.addServer(sc);
				try {
					conMan.testConnection(sc);
				} catch (IOException e1) {
					if(e1.toString().contains("ClassNotFoundException")) {
	        			offerToInstallDriver(sc.getJdbcType(), this);
					}
				}

				// only set serverConfig once addServer has succeeded, as all future actions will be updates.
				serverConfig = sc;
			}
			closeDialog();
		} catch(IllegalArgumentException ex) {
			
			String msg = "Error saving server changes. \r\n" + ex.getMessage();
			JOptionPane.showMessageDialog(this, msg, 
					"Save error", JOptionPane.ERROR_MESSAGE);
			LOG.info(msg);
		}
		
	}

	
	
	public void setFolder(String selectedFolder) {
		Set<String> foldSet = new HashSet<String>(conMan.getFolders());
		foldSet.add("");
		foldSet.add(selectedFolder);
		String[] folders = foldSet.toArray(new String[]{});
		
		folderComboBox.setModel(new DefaultComboBoxModel(folders));
		folderComboBox.setSelectedItem(selectedFolder);
	}
	


	public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

	public static void offerToInstallDriver(JdbcTypes jdbcTypes, Component c) {
		String msg = "Would you like to download and install the JDBC driver for " + jdbcTypes.getNiceName();
		int choice = JOptionPane.showConfirmDialog(c, msg, 
				"Install Driver", JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE);
		
		if(choice == JOptionPane.YES_OPTION) {
			EXECUTOR.execute(() -> {
				try {
					// MUST match exactly QStudioFrame.APP_TITLE else directories go strange on linux
					File f = PluginLoader.installDriver(ConnectionManager.APP_TITLE, jdbcTypes);
					JOptionPane.showMessageDialog(c, "Driver installed to: " + f.getAbsolutePath() + " \nPlease try to rerun any previous queries.");
				} catch (Exception e1) {
					List<String> urls = jdbcTypes.getDownloadURLs();
					String html = "<html><body>Failed to download driver.";
					if(urls.size() > 0) {
						String url = urls.get(0);
						html += "<br />Please download at: <a href='" + url + "'>" + url + " </a>";
					}
					html = html + "<br />Then follow these <a href='" + Page.QSTUDIO_HELP_DRIVER_DOWNLOAD_ERROR.url() + "'>data source driver instructions.</a>."
						+ "<br />" + e1.getLocalizedMessage()
									+ "</body></html>";
					JOptionPane.showMessageDialog(c, Theme.getHtmlText(html));
				}
			});
		}
	}
	

	@RequiredArgsConstructor
	class IconListRenderer extends DefaultListCellRenderer{ 
	    private static final long serialVersionUID = 1L;
	    private final Map<String, JdbcIcons> jdbcNameToIcon;
	    
		@Override
	    public Component getListCellRendererComponent(JList list, Object value, int index,boolean isSelected, boolean cellHasFocus) { 
	        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	        if(value instanceof String) {
	        	JdbcIcons jdbcIcon = jdbcNameToIcon.get(value);
		        if(jdbcIcon != null && jdbcIcon.get16() != null) {
			        label.setIcon(jdbcIcon.get16());
		        }
	        }
	        return label; 
	    } 
	}
}
