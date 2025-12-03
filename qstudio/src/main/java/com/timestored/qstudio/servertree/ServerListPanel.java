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

import static com.timestored.theme.Theme.CENTRE_BORDER;
import static com.timestored.theme.Theme.GAP;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.timestored.connections.JdbcIcons;
import com.timestored.connections.JdbcTypes;
import com.timestored.connections.ServerConfig;
import com.timestored.kdb.KdbConnection;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.qstudio.BackgroundExecutor;
import com.timestored.qstudio.CommonActions;
import com.timestored.qstudio.QStudioModel;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.AdminModel.Category;
import com.timestored.qstudio.model.QEntity;
import com.timestored.qstudio.model.ServerModel;
import com.timestored.qstudio.model.ServerObjectTree;
import com.timestored.qstudio.servertree.ServerListPanel.FolderNode;
import com.timestored.qstudio.servertree.ServerListPanel.ServerObjectTreeNode;
import com.timestored.swingxx.JTreeHelper;
import com.timestored.swingxx.JTreeHelper.IdentifiableNode;
import com.timestored.swingxx.SwingUtils;
import com.timestored.theme.Theme;

import lombok.Setter;

public class ServerListPanel extends JPanel  implements AdminModel.Listener {

	private static final Logger LOG = Logger.getLogger(ServerListPanel.class.getName());
	
	private static final long serialVersionUID = 1L;
	private File savedDocFile;
	private final AdminModel adminModel;
	private JTree tree;
	
	/**
	 * {@link ServerConfig} have folders, but must remember folders here to keep
	 * trackof empty ones.
	 */
	private final Set<String> myFolders = Sets.newHashSet();
	
	private static final String ROOT_TITLE = Msg.get(Key.SERVERS) ;
	
	private final CommonActions commonActions;
	private final Map<String, DefaultMutableTreeNode> nameToNode = Maps.newHashMap();
	private final JFrame parentFrame;
	
	public ServerListPanel(final AdminModel adminModel, 
			final CommonActions commonActions, JFrame parentFrame) {

		this.commonActions = commonActions;
		this.adminModel = adminModel;
		this.parentFrame = parentFrame;
		adminModel.addListener(this);
		setLayout(new BorderLayout(GAP,GAP));

		for(String f : adminModel.getFolders()) {
			myFolders.add(f + "/");
		}
		setPreferredSize(new Dimension(400, 400)); // Required to make splitPane set correct ratio
	}

	private void refreshGui() {

		LOG.info("refreshGui");
		List<ServerModel> serverModels = adminModel.getServerModels();
		Component c = new JPanel();
		
		if(serverModels.size() > 0) {
			tree = generateTree();
			c = new JScrollPane(tree);
			highlightRow(adminModel.getServerModel());
			CustomNode.configure(tree);
		} else {
			c = generateAddButton();
		}

		removeAll();
		add(c, BorderLayout.CENTER);
		revalidate();
	}
	
	private JTree generateTree() {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(new RootFolderNode());
		
		// get a list of expanded folder
		// on first open expand everything
		Set<String> curExpandedFolders = Collections.emptySet();
		if(tree != null) {
			curExpandedFolders = JTreeHelper.getExpandedFolders(tree);
		}
		// get a list of all folders
		for(String f : adminModel.getFolders()) {
			myFolders.add(f + "/");
		}
		// add all subfolder nodes while keeping map from name to node
		nameToNode.clear();
		nameToNode.put("", top);

		TreeSet<String> allFolders = Sets.newTreeSet(myFolders); // make them sorted
		for(String fullPath : allFolders) {
			if(!fullPath.equals("")) {
				String parentPath = "";
				String curPath = "";
				for(String fld : ServerConfig.extractParts(fullPath)) {
					// need the slash after it, in case e.g. folder at root and name of server collide
					curPath += fld + "/";
					DefaultMutableTreeNode pn = nameToNode.get(parentPath);
					if(!nameToNode.containsKey(curPath)) {
						DefaultMutableTreeNode n = new DefaultMutableTreeNode(new SubFolderNode(fld, curPath));
						pn.add(n);
						nameToNode.put(curPath, n);
					}
					parentPath = curPath;
				}
			}
		}
		
		
		// add the servers to their folders
		for (ServerModel sm : adminModel.getServerModels()) {
			String f = sm.getServerConfig().getFolder();
			f = f.equals("") ? "" : f + "/";
			DefaultMutableTreeNode n = new DefaultMutableTreeNode(new ServerObjectTreeNode(sm));
			DefaultMutableTreeNode fNode = nameToNode.get(f);
			if(fNode != null) {
				fNode.add(n);
			} else {
				LOG.warning("No node for " + f);
			}
		}

		JTree tree = new JTree(top);
		JTreeHelper.expandAll(tree, true);
		if(!curExpandedFolders.isEmpty()) {
			JTreeHelper.setFolderExpansions(tree, curExpandedFolders);
		}
		
		tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new ServerMovingTreeTransferHandler(adminModel));
		
		tree.setBorder(CENTRE_BORDER);
		return tree;
	}

	private JPanel generateAddButton() {
		JPanel panel = Theme.getVerticalBoxPanel();
		panel.add(Theme.getSubHeader(Msg.get(Key.ADD_SERVER)));
		JPanel p = new JPanel();
		JButton button = new JButton(commonActions.getAddServerAction());
		p.add(button);
		button.setBackground(Theme.HIGHLIGHT_BUTTON_COLOR);
		button.setForeground(Color.WHITE);
		button.setOpaque(true);
		panel.add(p);
		return panel;
	}


	@Override public void modelChanged() {
		refreshGui();
	}

	@Override public void modelChanged(ServerModel sm) { 
		refreshGui(); // this is required to update icon and show (dis)connected
	}
	
	@Override public void selectionChanged(ServerModel serverModel, Category category,
			String namespace, QEntity element) {	
		highlightRow(serverModel);
	}

	private void highlightRow(ServerModel serverModel) {
		try {
			// highlight the row with the currently selected server
			if(serverModel!=null) {
				String selName = serverModel.getName();
				for(int row = tree.getRowCount(); row>=0; row--) {
					ServerObjectTreeNode sot = getServerObjectTN(tree.getPathForRow(row));
					if(sot!= null && sot.getServerModel().getName().equals(selName)) {
						tree.setSelectionInterval(row, row+1);
						break;
					}
				}
			} else if(!tree.isSelectionEmpty()){
				tree.clearSelection();
			}
		} catch(RuntimeException e) {
			// This was happening when the first ever server is added.
			LOG.warning("Error calling highlightRow for tree");
		}
	}

	private static ServerObjectTreeNode getServerObjectTN(TreePath path) {
		ServerObjectTreeNode sot = null;
		if(path!=null) {
			DefaultMutableTreeNode mtn = (DefaultMutableTreeNode)path.getLastPathComponent();
			Object o = mtn!=null ? mtn.getUserObject() : null;
			if(o!=null && o instanceof ServerObjectTreeNode) {
				sot = ((ServerObjectTreeNode)o);
			}
		}
		return sot;
	}

	
	private void removeMyFolders(String folderName) {
		// if only an empty folder, remove and refresh ELSE remove from model
		Iterator<String> it = myFolders.iterator();
		while(it.hasNext()) {
			if(it.next().startsWith(folderName)) {
				it.remove();
			}
		}
	}
	
	private void renameMyFolder(String from, String to) {
		List<String> newF = new ArrayList<String>();
		Iterator<String> it = myFolders.iterator();
		while(it.hasNext()) {
			String f = it.next();
			if(f.startsWith(from)) {
				newF.add(to + f.substring(from.length()));
				it.remove();
			}
		}
		myFolders.addAll(newF);
	}
	
	private class SubFolderNode extends FolderNode {
		
		private SubFolderNode(String title, String folder) {
			super(title, folder);
		}
		
		@Override public void addMenuItems(JPopupMenu menu) {
	
			JMenuItem deleteMI = new JMenuItem("Delete Folder", Theme.CIcon.FOLDER_DELETE.get());
			deleteMI.setName("miDeleteFolder");
			deleteMI.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					
					String message = "Are you sure you want to delete this folder and all it's connections?";
					int opt = JOptionPane.showConfirmDialog(parentFrame, message , "Confirm Delete", JOptionPane.OK_CANCEL_OPTION);
					if(opt == JOptionPane.OK_OPTION) {
						removeMyFolders(getFolder());
						
						int removed = adminModel.removeFolder(getFolder());
						// if nothing removed, model wont notify listeners of change
						// so refresh ourselves as folder was empty
						if(removed==0) {
							refreshGui();
						}
					}
				}
			});

			JMenuItem renameFolderMI = new JMenuItem("Rename Folder");
			renameFolderMI.setName("renameFolderMI");
			renameFolderMI.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					String newName = JOptionPane.showInputDialog(parentFrame, "Enter new Folder name:");
					if(newName!=null) {
						if(!getFolder().equals("")) {
							List<String> l = ServerConfig.extractParts(getFolder());
							String pf = Joiner.on("/").join(l.subList(0, l.size()-1));
							newName = (pf.length() > 0 ? pf + "/" : "") + newName;
						}
						renameMyFolder(getFolder(), newName + "/");
						int numRenamed = adminModel.renameFolder(getFolder(), newName);
						// must be just one of our empty folders
						if(numRenamed == 0) {
							refreshGui();	
						}
					}
				}
			});

			
			JMenuItem addServerMI = new JMenuItem(commonActions.getAddServerAction(getFolder()));
			addServerMI.setName("addServerMI");

			menu.add(addServerMI);
			menu.add(new AddFolderMenuItem(getFolder()));
			menu.add(renameFolderMI);
			menu.add(deleteMI);
		}
	}
	
	
	private class AddFolderMenuItem extends JMenuItem {
		
		public AddFolderMenuItem(final String parentFolder) {
			super("New Folder...", Theme.CIcon.FOLDER_ADD.get());
			
			setName("miAddFolder");
			addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					String f = JOptionPane.showInputDialog("Enter new folders name:");
					if(f!=null && f.length()>0) {
						myFolders.add(parentFolder + f + "/");
						refreshGui();
					}
				}
			});
		}
	}
	
	private class RootFolderNode extends FolderNode {
		
		public RootFolderNode() { super(ROOT_TITLE, ""); }
		

		@Override public void addMenuItems(JPopupMenu menu) {

			JMenuItem m;
			
//			m = new JMenuItem("Refresh all servers.", Theme.CIcon.ARROW_REFRESH.get());
//			m.setName("miRefreshAll");
//			m.addActionListener(new ActionListener() {
//				@Override public void actionPerformed(ActionEvent e) {
//					BackgroundExecutor.EXECUTOR.execute(new Runnable() {
//						@Override public void run() {
//							adminModel.refresh();
//						}
//					});
//				}
//			});
//			menu.add(m);
//			menu.addSeparator();
			menu.add(new AddFolderMenuItem(""));
//			
			menu.addSeparator();
			for(Action a : commonActions.getServerActions()) {
				menu.add(new JMenuItem(a));
			}
		}
		
		@Override public String getId() {
			return ROOT_TITLE;
		}
	}
			
			
	public abstract class FolderNode extends CustomNode implements IdentifiableNode {

		private final String folder;

		private FolderNode(String title, String folder) {
			super(title, null, Theme.CIcon.FOLDER.get());
			this.folder = Preconditions.checkNotNull(folder);
		}

		@Override public abstract void addMenuItems(JPopupMenu menu);
		
		public String getFolder() { return folder; }
		@Override public String getId() { return folder; }
	}
	
	
	
	/** Shows server details when selected and allows refreshing server tree from popup menu */
	class ServerObjectTreeNode extends CustomNode {
	
		private final ServerModel serverModel;
	
		private ServerObjectTreeNode(ServerModel serverModel) {
			super(serverModel.getServerConfig().getShortName());
			this.serverModel = serverModel;
		}
	
		@Override public String getTooltip() {
			String msg = "<html>" + serverModel.getName() + " " 
					+ (serverModel.isConnected() ? "Connected" : "Disconnected");
			ServerConfig sc = serverModel.getServerConfig();
			if(sc.getPort() != 0) { msg += "<br />Host: <b>" + sc.getHost() + ":" + sc.getPort() + "</b>"; };
			if(sc.getDatabase().length() > 0) {  msg += "<br />DB: <b>" + sc.getDatabase() + "</b>"; };
			if(!sc.isKDB()) {  msg += "<br />Type: <b>" + sc.getJdbcType() + "</b>"; };
			
			if(isTreeProblem()) {
				ServerObjectTree soTree = serverModel.getServerObjectTree();
				String s = soTree==null ? "" : soTree.getErrMsg();
				msg += "<br />Error retrieving tree: " + s;
			}
			
			return msg  + "</html>";
		}
		
		private boolean isTreeProblem() {
			ServerObjectTree soTree = serverModel.getServerObjectTree();
			return soTree==null || soTree.isErrorRetrievingTree();
		}
		
		@Override public ImageIcon getIcon() {
			JdbcTypes jdbcType = serverModel.getServerConfig().getJdbcType();
			if(JdbcTypes.DUCKDB.equals(jdbcType) || JdbcTypes.BABELDB.equals(jdbcType)) {
				return serverModel.isConnected() ? Theme.CIcon.DUCK.get() : Theme.CIcon.DUCK_FADED.get();
			} else if(serverModel.isConnected() && isTreeProblem()) {
				return Theme.CIcon.SERVER_LIGHTNING.get();
			} else if(serverModel.getServerConfig().isKDB() && serverModel.isConnected()) {
				return Theme.CIcon.SERVER.get();
			} else if (!serverModel.getServerConfig().isKDB() && serverModel.isConnected()){
				JdbcIcons jicon = JdbcIcons.getIconFor(jdbcType);
				return jicon.get16();
			} 
			return Theme.CIcon.SERVER_CONNECT.get();
		}
		
		@Override public void doSelectionAction() {
			if(adminModel.getServerModel() != serverModel) {
				if(!serverModel.isConnected()) {
					adminModel.refresh(serverModel.getServerConfig());	
				}
				adminModel.setSelectedServerName(serverModel.getName());
			}
		}
		
		@Override public void addMenuItems(JPopupMenu menu) {
			final ServerConfig sc = serverModel.getServerConfig();
			
			if(sc.getJdbcType().equals(JdbcTypes.BABELDB)) {
				JMenuItem openFolderMI = new JMenuItem("Open Folder");
				openFolderMI.setIcon(Theme.CIcon.FOLDER.get());
				openFolderMI.setName("ssOpenFolder");
				openFolderMI.addActionListener(e -> {
					try {
						Desktop.getDesktop().open(QStudioModel.getLOCALDB_DIR());
					} catch (IOException e1) {				}
				});
				menu.add(openFolderMI);
			}
	
			JMenuItem propertiesMI = new JMenuItem("Properties");
			propertiesMI.setEnabled(sc.isKDB());
			propertiesMI.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					String title = sc.getName() + " Properties";
					adminModel.refresh(sc);
					ServerModel sm = adminModel.getServerModel(sc.getName());
					JPanel contentPanel = SelectedServerObjectPanel.getServerDescriptionPanel(adminModel);
					SwingUtils.showAppDialog(javax.swing.SwingUtilities.getWindowAncestor(ServerListPanel.this), title, contentPanel, 
							Theme.CIcon.SERVER.get().getImage());
				}
			});
	
			JMenuItem refreshMenuItem = new JMenuItem("Refresh " + serverModel.getName());
			refreshMenuItem.setIcon(Theme.CIcon.ARROW_REFRESH.get());
			refreshMenuItem.setName("ssRefreshAll");
			refreshMenuItem.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					BackgroundExecutor.EXECUTOR.execute(new Runnable() {
						@Override public void run() {
							adminModel.refresh(serverModel.getServerConfig());
						}
					});
				}
			});
			menu.add(refreshMenuItem);
			menu.add(commonActions.getCloseConnServerAction(sc));

			if(reportGenerator != null) {
				JMenuItem generateTableDocs = new JMenuItem("Generate Table Docs " + serverModel.getName());
				generateTableDocs.setEnabled(sc.isKDB());
				generateTableDocs.setName("ssgenerateTableDocs");
				generateTableDocs.addActionListener(new ActionListener() {
	
					@Override public void actionPerformed(ActionEvent e) {
						
						File myDocs = new JFileChooser().getFileSystemView().getDefaultDirectory();
						if(savedDocFile == null) {
							savedDocFile = new File(myDocs, "table-docs.html");
						}
						savedDocFile = SwingUtils.askUserSaveLocation(savedDocFile, "html");
						
				        if (savedDocFile != null) {
				            try {
				            	KdbConnection kdbConn = serverModel.getConnection();
				            	if(kdbConn == null) {
				            		throw new IOException("Could not connect to server: " + serverModel.getName());
				            	}
				            	reportGenerator.generate(kdbConn, savedDocFile);
	
								SwingUtils.offerToOpenFile(Msg.get(Key.DOCS_GENERATED), savedDocFile, 
										Msg.get(Key.OPEN_DOCS_NOW), Msg.get(Key.CLOSE));
							} catch (Exception ioe) {
								String msg = Msg.get(Key.ERROR_SAVING) + ": " + savedDocFile + "\r\n" + ioe.toString();
						        LOG.warning(msg);
						        JOptionPane.showMessageDialog(null, msg, Msg.get(Key.ERROR_SAVING), JOptionPane.ERROR_MESSAGE);
							}
				        } else {
				        	LOG.info(Msg.get(Key.SAVE_CANCELLED));
				        }
					}
				});
				menu.add(generateTableDocs);
			}
			
			
			menu.add(commonActions.getEditServerAction(sc));
			menu.add(commonActions.getCloneServerAction(sc));
			menu.add(commonActions.getRemoveServerAction(sc));
			menu.addSeparator();
	
			menu.add(propertiesMI);
		}
		
		public ServerModel getServerModel() {
			return serverModel;
		}
	}

	public static interface ReportGenerator {
		void generate(KdbConnection kdbConnection, File savedDocFile) throws Exception;
	}
	@Setter private static ReportGenerator reportGenerator = null;

}






class ServerMovingTreeTransferHandler extends TransferHandler {

	private static final Logger LOG = Logger.getLogger(ServerMovingTreeTransferHandler.class.getName());
	
	private static final long serialVersionUID = 1L;
    private DataFlavor[] flavors = new DataFlavor[1];
	private final AdminModel adminModel;

    public ServerMovingTreeTransferHandler(AdminModel adminModel) {
    	this.adminModel = adminModel;
        try {
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                              ";class=\"" +
                              ServerObjectTreeNode.class.getName() + "\"";
            flavors[0] = new DataFlavor(mimeType);
        } catch(ClassNotFoundException e) {
        	LOG.warning("ClassNotFound: " + e.getMessage());
        }
    }

    public boolean canImport(TransferHandler.TransferSupport support) {
    	// check dropped item is of correct type
        if(!support.isDrop() || !support.isDataFlavorSupported(flavors[0])) {
            return false;
        }
        support.setShowDropLocation(true);
        // check destination is a folder
        JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode target = (DefaultMutableTreeNode)dest.getLastPathComponent();
        return target.getUserObject() instanceof ServerListPanel.FolderNode;
    }

    protected Transferable createTransferable(JComponent c) {

    	// wrap our Server node as a transferable
        TreePath[] paths = ((JTree)c).getSelectionPaths();
        if(paths != null) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode)paths[0].getLastPathComponent();
            Object o = node.getUserObject();
            if(o instanceof ServerObjectTreeNode) {
                return new NodesTransferable((ServerObjectTreeNode)o);
            }
        }
        return null;
    }

    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    public boolean importData(TransferHandler.TransferSupport support) {
        if(!canImport(support)) {
            return false;
        }
        // Extract transfer data.
        ServerObjectTreeNode serverNode = null;
        try {
            Transferable t = support.getTransferable();
            serverNode = (ServerObjectTreeNode)t.getTransferData(flavors[0]);
        } catch(UnsupportedFlavorException ufe) {
        	LOG.warning("UnsupportedFlavor: " + ufe.getMessage());
        } catch(java.io.IOException ioe) {
        	LOG.warning("I/O error: " + ioe.getMessage());
        }
        // Get drop location info.
        JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)dest.getLastPathComponent();
        Object o = parent.getUserObject();
        if(o instanceof FolderNode) {
        	FolderNode f = (FolderNode) o;
        	adminModel.moveServer(serverNode.getServerModel(), f.getFolder());
        }
        return true;
    }

    public String toString() {
        return getClass().getName();
    }

    private class NodesTransferable implements Transferable {
    	private ServerObjectTreeNode nodes;

        private NodesTransferable(ServerObjectTreeNode node) {
            this.nodes = node;
         }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if(!isDataFlavorSupported(flavor))
                throw new UnsupportedFlavorException(flavor);
            return nodes;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavors[0].equals(flavor);
        }
    }
}