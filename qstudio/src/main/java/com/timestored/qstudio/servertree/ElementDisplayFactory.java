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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.rowset.CachedRowSet;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kx.c.KException;

import com.timestored.connections.JdbcTypes;
import com.timestored.connections.ServerConfig;
import com.timestored.qstudio.kdb.CAtomTypes;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.qstudio.model.ServerQEntity;
import com.timestored.qstudio.model.ServerQEntity.QQuery;
import com.timestored.sqldash.chart.ChartTheme;
import com.timestored.sqldash.chart.TableFactory;
import com.timestored.theme.Theme;

/**
 * Build for displaying details about known types. It's important that the type
 * and namespace / name is known as this allows advanced handling of functions
 * and partitioned tables etc.
 */
class ElementDisplayFactory {
	
	private static final Logger LOG = Logger.getLogger(ElementDisplayFactory.class.getName());

	public static Box getActionButtons(QueryManager queryManager, List<QQuery> qQueryies) {
		Box b = Box.createHorizontalBox();
		for(QQuery qQuery : qQueryies) {
			if(!qQuery.getTitle().toLowerCase().contains("delete")) {
				JButton but = new JButton(qQuery.getTitle(), qQuery.getIcon().get16());
				but.setToolTipText("Run query:\r\n" + qQuery.getQuery());
				JButton copyBut = new JButton("", Theme.CIcon.EDIT_COPY.get16());
				copyBut.setToolTipText("Copy query to Clipboard:\r\n" + qQuery.getQuery());
				but.addActionListener(e -> {
					queryManager.sendQuery(qQuery.getQuery());
				});
				copyBut.addActionListener(e -> {
					Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
					clpbrd.setContents(new StringSelection(qQuery.getQuery()), null);
				});
				b.add(copyBut);
				b.add(but);
			}
		}
		return b;
	}

	public static Component getPanel(AdminModel adminModel, QueryManager queryManager, ChartTheme chartTheme, boolean negativeShownRed) {
		
		Component retComponent = null;
		try {
			retComponent  = getCustomizedEditor(adminModel, queryManager, chartTheme, negativeShownRed);
		} catch (KException e) {
			retComponent = handleException(adminModel, queryManager, chartTheme, negativeShownRed);
		} catch (IOException e) {
			retComponent = handleException(adminModel, queryManager, chartTheme, negativeShownRed);
		}
	
		if(retComponent == null) {
			retComponent = DefaultElementDisplayStrategy.INSTANCE.getPanel(adminModel);
		}
		return retComponent;
	}

	private static Component handleException(AdminModel adminModel, QueryManager queryManager, ChartTheme chartTheme, boolean negativeShownRed) {

		Component retComponent = null;
		// problems = refresh tree and try once more
		adminModel.refresh();
		try {
			retComponent = getCustomizedEditor(adminModel, queryManager, chartTheme, negativeShownRed);
		} catch (KException e) {
			LOG.log(Level.WARNING, "error using proposed Element Display Strategy", e);
			// fall through
		} catch (IOException e) {
			LOG.log(Level.WARNING, "error using proposed Element Display Strategy", e);
			//fall through
		}
		if(retComponent == null) {
			JPanel panel = new JPanel();
			JLabel label = new JLabel("Problem viewing this element, likely that the" +
					"wrong type was assumed. Try refreshing the tree and then selecting this" +
					"element again.");
			panel.add(label);
			retComponent = panel;
		}
		return retComponent;
	}

	private static Component getCustomizedEditor(AdminModel adminModel, QueryManager queryManager, ChartTheme chartTheme, boolean negativeShownRed) throws IOException,
			KException {
		ServerQEntity elementDetails = adminModel.getSelectedElement();
		String queryName = elementDetails.getFullName();
		
		if(adminModel.getServerModel().getServerConfig().isKDB()) {
			if (elementDetails.isTable()) {
				JPanel p = new JPanel(new BorderLayout());
				p.add(getActionButtons(queryManager, elementDetails.getQQueries()), BorderLayout.NORTH);
				p.add(new PagingTablePanel(adminModel, queryName), BorderLayout.CENTER);
				return p;
			
			} else if(elementDetails.getType().equals(CAtomTypes.LAMBDA)) {
				return new FunctionEditingPanel(adminModel, queryName);
			}
		} else if (elementDetails.isTable() || adminModel.getServerModel().getServerConfig().getJdbcType().equals(JdbcTypes.DOLPHINDB)) {
			return new NonkdbTablePanel(adminModel, queryManager, elementDetails, chartTheme, negativeShownRed);
		}
		return null;
	}

	private static class NonkdbTablePanel extends JPanel {
		private static final long serialVersionUID = 1L;

		public NonkdbTablePanel(AdminModel adminModel, QueryManager queryManager, ServerQEntity serverEntity, ChartTheme chartTheme, boolean negativeShownRed) {
			setLayout(new BorderLayout());

			try {
				if(serverEntity.getQQueries().size() > 0) {
					ServerConfig sc = adminModel.getServerModel().getServerConfig();
					List<QQuery> qQueryies = serverEntity.getQQueries();
					if(qQueryies.size() > 0) {
						add(getActionButtons(queryManager, qQueryies), BorderLayout.NORTH);
						QQuery qQuery = serverEntity.getQQueries().get(0);
						String sqlQuery = qQuery.getQuery();
						CachedRowSet r = adminModel.getConnectionManager().executeQuery(sc, sqlQuery );
						add(TableFactory.getTable(r, 10000, negativeShownRed), BorderLayout.CENTER);
						revalidate();
					}
				}
			} catch (SQLException | IOException e) {
			}
		}
		
	}

}

