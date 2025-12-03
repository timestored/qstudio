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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.google.common.collect.ImmutableList;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ServerConfig;
import com.timestored.qstudio.CommonActions;
import com.timestored.qstudio.QStudioModel;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.AdminModel.Category;
import com.timestored.qstudio.model.DatabaseDirector;
import com.timestored.qstudio.model.QEntity;
import com.timestored.qstudio.model.ServerModel;
import com.timestored.qstudio.model.ServerObjectTree;
import com.timestored.qstudio.model.ServerQEntity;
import com.timestored.qstudio.model.ServerQEntity.QQuery;
import com.timestored.qstudio.model.TableSQE;
import com.timestored.theme.Icon;
import com.timestored.theme.Theme;

/**
 * Displays all servers in a {@link ConnectionManager} and their tables / functions / variables
 * as items in sub trees. The user can select items or right click and take actions on certain
 * elements.
 */
public class ServerTreePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private final ObjectTreePanel objectTreePanel;
	private final ServerListPanel serverListPanel;
	
	public ServerTreePanel(final QStudioModel qStudioModel, final CommonActions commonActions, JFrame parentFrame) {
		
		serverListPanel = new ServerListPanel(qStudioModel.getAdminModel(), commonActions, parentFrame);
		objectTreePanel = new ObjectTreePanel(qStudioModel);

		final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, serverListPanel, objectTreePanel);
		setLayout(new BorderLayout());
		add(splitPane, BorderLayout.CENTER);
		splitPane.setResizeWeight(0.35);
		splitPane.setDividerLocation(0.35);
		splitPane.revalidate();
		
	}
	
	@Override public void setTransferHandler(TransferHandler newHandler) {
		serverListPanel.setTransferHandler(newHandler);
		super.setTransferHandler(newHandler);
	}

	public void setHiddenNamespaces(Set<String> hiddenNS) {
		objectTreePanel.setHiddenNamespaces(hiddenNS);
	}
}

/**
 * Displays the objects that exist on a single {@link ServerModel} to update 
 * the display you must manually call {@link #refreshGui()}.
 */
class ObjectTreePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private JTree tree;
	private Set<String> hiddenNS;

	private final AdminModel adminModel;
	private final QStudioModel qStudioModel;
	// variable used to make expanding default namespace nodes easier
	private final List<DefaultMutableTreeNode> defaultNSnodes = new ArrayList<DefaultMutableTreeNode>();

	
	private ServerModel curServerModel;

	public ObjectTreePanel(QStudioModel qStudioModel) {
		
		this.qStudioModel = qStudioModel;
		this.adminModel = qStudioModel.getAdminModel();
		setLayout(new BorderLayout(GAP,GAP));
		JPanel p = new JPanel();
		p.setPreferredSize(new Dimension(400, 400));
		add(new JScrollPane(p), BorderLayout.CENTER);
		
		adminModel.addListener(new AdminModel.Listener() {
			
			@Override public void selectionChanged(ServerModel serverModel, Category category,
					String namespace, QEntity element) { 
				if(serverModel != curServerModel) { refreshGui(); }
			}
			
			@Override public void modelChanged() { refreshGui();  }
			@Override public void modelChanged(ServerModel sm) {  refreshGui(); }
		});
		refreshGui();
	}
	
	private void refreshGui() {
		
		EventQueue.invokeLater(new Runnable() {
			@Override public void run() {
				DefaultMutableTreeNode top = new DefaultMutableTreeNode();
				defaultNSnodes.clear();
				curServerModel = adminModel.getServerModel();
				if(curServerModel!=null) {
					ServerObjectTree soTree = curServerModel.getServerObjectTree();
					if (soTree != null) {
						addNodes(top, curServerModel, soTree, hiddenNS);
					}
				}
				
				tree = new JTree(top);   
				JScrollPane treeView = new JScrollPane(tree); 
				tree.setBorder(CENTRE_BORDER);
				CustomNode.configure(tree);

				// expand the default node folders
				for (DefaultMutableTreeNode dmtNode : defaultNSnodes) {
					// within default namespace expand functions / variables.
					tree.expandPath(new TreePath(dmtNode.getPath()));
				}
				
				removeAll();
				add(treeView, BorderLayout.CENTER);
				revalidate();
			}
		});
		
	}
	

	
	public void setHiddenNamespaces(Set<String> hiddenNS) {
		this.hiddenNS = hiddenNS;
		refreshGui();
	}


	private void addNodes(DefaultMutableTreeNode top, ServerModel sm,
			ServerObjectTree kdbServerObjects, Set<String> hiddenNS) {

		String serverName = sm.getName();
		String[] namespaces = kdbServerObjects.getNamespaces().toArray(new String[]{});
		Arrays.sort(namespaces);
		DatabaseDirector.ActionsGenerator actionsGenerator = DatabaseDirector.getActionsGenerator(qStudioModel.getQueryManager(), adminModel, sm);
		
		for(String ns : namespaces) {
			
			if(!hiddenNS.contains(ns)) {
			
				NamespaceNode nsNode = new NamespaceNode(ns, sm.getServerConfig());
				DefaultMutableTreeNode nsTree = new DefaultMutableTreeNode(nsNode, true);
				if(ns.equals(".")) {
					defaultNSnodes.add(nsTree);
				} 
				for(TableSQE ed : sorted(kdbServerObjects.getTables(ns))) {

					DefaultMutableTreeNode branch = new DefaultMutableTreeNode(new ServerQEntityNode(serverName, ed));
					int i = ed.isPartitioned() ? 0 : 1;
					for(String cn : ed.getColNames()) {
						ColNode colNode = new ColNode(actionsGenerator, ed, cn, i++==0);
						branch.add(new DefaultMutableTreeNode(colNode));
					}
					nsTree.add(branch);
				}
				for(ServerQEntity ed : kdbServerObjects.getViews(ns)) {
					nsTree.add(new DefaultMutableTreeNode(new ServerQEntityNode(serverName, ed)));
				}
				List<ServerQEntity> funcs = sorted(kdbServerObjects.getFunctions(ns));
				if(funcs.size() > 0) {
					nsTree.add(getBranch(serverName, "Functions", funcs));
				}
				List<ServerQEntity> vars =  sorted(kdbServerObjects.getVariables(ns));
				if(vars.size() > 0) {
					nsTree.add(getBranch(serverName, "Variables",vars));
				}
				top.add(nsTree);
			}
		}
	}
	
	private static <T extends ServerQEntity> List<T> sorted(List<T> sqe) {
		List<T> tables = new ArrayList<>(sqe);
		Collections.sort(tables, (a,b) -> { return a.getName().compareTo(b.getName()); });
		return tables;
	}

	/** Represents KDB namespace */
	private class NamespaceNode extends CustomNode {
		private final String ns;
		private final ServerConfig serverConfig;

		public NamespaceNode(String ns, ServerConfig serverConfig) {
			super(ns);
			this.ns = ns;
			this.serverConfig = serverConfig;
		}

		@Override public void doSelectionAction() {
			adminModel.setSelectedNamespace(serverConfig.getName(), ns);
		}
		
		@Override public void addMenuItems(JPopupMenu menu) {
			QQuery qq = new QQuery("Delete all variables from Namespace", 
					Theme.CIcon.DELETE, "delete from `" + ns);
			addScriptMenu(menu, ImmutableList.of(qq));
		}
	}

	
	/** Tree node for an object that exists on the server */
	private class ServerQEntityNode extends CustomNode {

		private final ServerQEntity sqe;
		private final String serverName;

		public ServerQEntityNode(String serverName, ServerQEntity sqe) {
			super(sqe.getName(), sqe.getHtmlDoc(true), sqe.getIcon());
			this.sqe = sqe;
			this.serverName = serverName;
			if(sqe.getType().isList()) {
				setText("<html><b>" + sqe.getName() + "</b></html>");
			}
		}
		
		@Override public String toString() {
			return sqe.getFullName();
		}

		@Override public void doSelectionAction() {
			adminModel.setSelectedElement(serverName, sqe.getNamespace(), sqe);
		}
		
		@Override public void addMenuItems(JPopupMenu menu) {
			addScriptMenu(menu, sqe.getQQueries());
		}
	}
	

	/**
	 * Add a menu with the qquery actions and a separate sub menu that scripts them qQueries
	 * to the current editor.
	 * @param qQueries The q queries that will each have a menuItem added.
	 */
	private void addScriptMenu(JPopupMenu menu, List<QQuery> qQueries) {
		if(qQueries.size()>0) {
			JMenu scriptToWindowMenu = new JMenu("Script to Editor");
			scriptToWindowMenu.setIcon(Theme.CIcon.PAGE_CODE.get16());
			for(QQuery qquery : qQueries) {
				menu.add(new JMenuItem(new ElementAction(qquery, false)));
				scriptToWindowMenu.add(new JMenuItem(new ElementAction(qquery, true)));
			}
			menu.add(scriptToWindowMenu);
		}
	}

	private DefaultMutableTreeNode getBranch(String serverName, String branchTitle, 
			List<ServerQEntity> elements) {
		
		DefaultMutableTreeNode branch = new DefaultMutableTreeNode(branchTitle);
		for(ServerQEntity ed : elements) {
			branch.add(new DefaultMutableTreeNode(new ServerQEntityNode(serverName, ed)));
		}
		return branch;
	}

	
	private class ElementAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;
		private final String query;
		private final boolean insertText;
		
		public ElementAction(QQuery qquery, boolean insertText) {
			
			super(qquery.getTitle());
			Icon ic = qquery.getIcon();
			if(ic!=null) {
				this.putValue(Action.SMALL_ICON, ic.get16());
			}
			query = qquery.getQuery();
			this.insertText = insertText;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if(insertText) {
				qStudioModel.getOpenDocumentsModel().insertSelectedText(query);
			} else {
				qStudioModel.getQueryManager().sendQuery(query);
			}
		}
	}
}



