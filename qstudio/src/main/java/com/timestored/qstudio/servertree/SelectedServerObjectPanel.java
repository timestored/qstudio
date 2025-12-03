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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.timestored.connections.JdbcTypes;
import com.timestored.connections.ServerConfig;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.AdminModel.Category;
import com.timestored.qstudio.model.QEntity;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.qstudio.model.ServerModel;
import com.timestored.qstudio.model.ServerObjectTree;
import com.timestored.qstudio.model.ServerQEntity;
import com.timestored.sqldash.chart.ChartTheme;
import com.timestored.theme.Theme;

import lombok.Setter;

/**
 * Displays the selected component for a given {@link ServerObjectTree}.
 * in most appropriate way possible e.g. table for tables, text area for functions.
 * May also allow editing.
 */
public class SelectedServerObjectPanel extends JPanel implements AdminModel.Listener {
	
	private static final long serialVersionUID = 1L;
	private final AdminModel adminModel;
	private final QueryManager queryManager;
	private ChartTheme chartTheme;
	private static Map<JdbcTypes,Function<AdminModel,JPanel>> jdbcTypeToDescriptionPanel = new HashMap<>();
	@Setter private boolean negativeShownRed = true;
	
	public static void registerServerDescriptionPanelSupplier(JdbcTypes jdbcType, Function<AdminModel,JPanel> provider) {
		Preconditions.checkNotNull(jdbcType);
		jdbcTypeToDescriptionPanel.put(jdbcType, provider);
	}
	
	public static JPanel getServerDescriptionPanel(AdminModel adminModel) {
		if(adminModel != null && adminModel.getServerModel() != null) {
			ServerModel serverModel = adminModel.getServerModel();
			ServerConfig sc = serverModel.getServerConfig();
			Function<AdminModel, JPanel> panelSupplier = jdbcTypeToDescriptionPanel.get(sc.getJdbcType());
			if(panelSupplier != null) {
				return panelSupplier.apply(adminModel);
			}
		}
		return new JPanel(new BorderLayout());
	}
	
	public SelectedServerObjectPanel(AdminModel adminModel, QueryManager queryManager) {
		this.adminModel = adminModel;
		this.queryManager = queryManager;
		setLayout(new BorderLayout());
		adminModel.addListener(this);
		refreshGUI(false);
	}

	/**]
	 * @param forceModelUpdate if true, when server is selected the {@link ServerModel} will be 
	 * forcefully refreshed to ensure accurate data is displayed.
	 */
	private void refreshGUI(boolean forceModelUpdate) {
		
		Component p = new JLabel("nothing to see here");
		Category cat = adminModel.getSelectedCategory();
		String title = adminModel.getSelectedServerName();

		if(adminModel.getSelectedServerName() != null) {
			if(cat.equals(Category.ELEMENT)) {
				p = ElementDisplayFactory.getPanel(adminModel, queryManager, chartTheme, negativeShownRed);
				title = adminModel.getSelectedElement().getName();
			} else if(cat.equals(Category.NAMESPACE)) {
				p = getNamespaceListing();
				title = adminModel.getSelectedNamespace();
			} else {
				p = getServerDescriptionPanel(adminModel);
			}
		}
		
		if(p == null) {
			JPanel panel = new JPanel();
			panel.add(new JLabel("Problem querying server. is it connected?"));
			p = panel;
		}
		
		// wrap up what we want to show
		final JPanel wrapPanel = new JPanel(new BorderLayout());
		wrapPanel.add(Theme.getHeader(title), BorderLayout.NORTH);
		wrapPanel.add(p, BorderLayout.CENTER);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				removeAll();
				add(wrapPanel, BorderLayout.CENTER);
				revalidate();
			}
		});
	}


	private Component getNamespaceListing() {
		Box bp = Box.createVerticalBox();
//		bp.add(Theme.getSubHeader(adminModel.getSelectedNamespace()));
		ServerModel serverModel = adminModel.getServerModel();
		String namespace = adminModel.getSelectedNamespace();
		if(serverModel != null && namespace != null) {
			ServerObjectTree soTree = serverModel.getServerObjectTree();
			if(soTree != null) {
				addElements(bp, namespace, "Functions",soTree.getFunctions(namespace));
				addElements(bp, namespace, "Tables", soTree.getTables(namespace));
				addElements(bp, namespace, "Variables", soTree.getVariables(namespace));
			}
		}
		JScrollPane sp = new JScrollPane(bp, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JScrollBar sb = sp.getVerticalScrollBar();
		sb.setUnitIncrement(20);
		return sp;
	}

	private void addElements(Box bp, String namespace, String title, List<? extends ServerQEntity> functions) {
		if(functions.size() > 0) {
			bp.add(Theme.getSubHeader(title));
			String serverName = adminModel.getSelectedServerName();
			for(ServerQEntity sqe : functions) {
				Box row = Box.createHorizontalBox();
				JButton b = new JButton(sqe.getFullName());
				b.addActionListener(e -> {
					adminModel.setSelectedElement(serverName, namespace, sqe);
				});
				b.setIcon(sqe.getIcon());
				b.setToolTipText(sqe.getHtmlDoc(true));
				JButton cpButton = new JButton("",Theme.CIcon.EDIT_COPY.get16());
//				row.add(cpButton);
				row.add(b);
//				row.add(new JButton("[]",Theme.CIcon.GREEN_PLAY.get()));
				row.add(Box.createVerticalStrut(2));
				row.add(ElementDisplayFactory.getActionButtons(queryManager, sqe.getQQueries()));
				row.add(Box.createVerticalStrut(20));
				row.add(Box.createVerticalGlue());
				row.setAlignmentX(Component.LEFT_ALIGNMENT);
				bp.add(row);
			}
		}
	}

	@Override
	public void modelChanged() {
		// ignore, we only care when the servers contents change
	}

	@Override
	public void selectionChanged(ServerModel serverModel, Category category, 
			String namespace, QEntity element) {
		refreshGUI(true);
	}

	@Override public void modelChanged(ServerModel sm) {
		refreshGUI(false);
	}



	public void setChartTheme(ChartTheme chartTheme) {
		this.chartTheme = Preconditions.checkNotNull(chartTheme);
		refreshGUI(false);
	}

	
}
