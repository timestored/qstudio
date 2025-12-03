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
package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.google.common.base.Preconditions;
import com.timestored.StringUtils;
import com.timestored.connections.JdbcTypes;
import com.timestored.connections.ServerConfig;
import com.timestored.kdb.KError;
import com.timestored.misc.AIFacade;
import com.timestored.misc.AIFacade.AIresult;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.kdb.KdbHelper;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.AdminModel.Category;
import com.timestored.qstudio.model.QEntity;
import com.timestored.qstudio.model.QueryAdapter;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.qstudio.model.QueryResult;
import com.timestored.qstudio.model.ServerModel;
import com.timestored.qstudio.servertree.SelectedServerObjectPanel;
import com.timestored.sqldash.chart.TableFactory;
import com.timestored.theme.Theme;

import kx.c.KException;

/**
 * Show the result of a KDB query as either a table otherwise as kdb console
 * like format.
 */
class KDBResultPanel extends JPanel implements GrabableContainer {

	private static final long serialVersionUID = 1L;

	private final SelectedServerObjectPanel selectedServerObjectPanel;
	private QueryResult lastQueryResult;
	private QueryManager queryManager;

	private int maxRowsShown = Integer.MAX_VALUE;

	private boolean pivotFormVisible = false;
	private boolean negativeShownRed = true;

	KDBResultPanel(AdminModel adminModel, QueryManager queryManager) {

		setLayout(new BorderLayout());
		// add as listener last to make sure constructed
		selectedServerObjectPanel = new SelectedServerObjectPanel(adminModel, queryManager);
		selectedServerObjectPanel.setNegativeShownRed(this.negativeShownRed);
		this.queryManager = queryManager;
		
		// When queries are sent/received display results
		queryManager.addQueryListener(new QueryAdapter() {
			@Override public void queryResultReturned(ServerConfig sc, QueryResult qr) {
				// Need lastQueryResult to allow generating new popout window. 
				lastQueryResult = qr;
				regenerateDisplay(qr);
			}

			@Override public void sendingQuery(ServerConfig sc, String query) {
				lastQueryResult = null;
				String txt = sc.getName() + "<- " + query;
				clearAndSetContent(Theme.getTextArea("querySent", txt));
			}
		});
		
		// when user changes selection in tree, show that
		adminModel.addListener(new AdminModel.Listener() {
			@Override public void modelChanged() { }
			@Override public void modelChanged(ServerModel sm) { }
			@Override public void selectionChanged(ServerModel serverModel, 
					Category category, String namespace, QEntity element) {
				lastQueryResult = null;
				clearAndSetContent(selectedServerObjectPanel);
			}

		});
		
		BackgroundExecutor.EXECUTOR.execute(() -> {
			JPanel p = new JPanel(new BorderLayout());
	        String licTxt = "Version: <b>" + QStudioFrame.VERSION + "</b>";
			p.add(Theme.getHtmlText(licTxt), BorderLayout.NORTH);
			if(queryManager.hasAnyServers()) {
				JdbcTypes jdbcTypes = adminModel.getConnectionManager().containsKdbServer() ? JdbcTypes.KDB : null;
				p.add(UpdateHelper.getNewsPanel(jdbcTypes), BorderLayout.CENTER);
			}
			clearAndSetContent(p);
		});
	}


	public static Component getDisplay(QueryResult qr, int maxRowsShown, QueryManager queryManager, 
			boolean pivotFormVisible, boolean negativeShownRed) {
		boolean isNonNullKdbResult = qr.k != null || (qr.getConsoleView().length()>0 && !qr.getConsoleView().equals("::"));
		if(isNonNullKdbResult || qr.rs != null) {
			return getComponent(qr, maxRowsShown, queryManager, pivotFormVisible, negativeShownRed);
		} else {
			if(qr.getConsoleView().length()>0) {
				String txt = qr.getConsoleView();
				return new JScrollPane(Theme.getTextArea("consoleView", txt));
			}
		}
		return new JScrollPane(new JLabel("No Result"));
	}
	
	private void regenerateDisplay(QueryResult qr) {
		clearAndSetContent(getDisplay(qr, maxRowsShown, queryManager, pivotFormVisible, negativeShownRed));
	}


	public void setNegativeShownRed(boolean negativeShownRed) {
		this.negativeShownRed = negativeShownRed;
		selectedServerObjectPanel.setNegativeShownRed(this.negativeShownRed);
	}

	
	
	/**
	 * get the result of a KDB query viewed as a component.
	 */
	static Component getComponent(QueryResult qr, int maxRowsShown, QueryManager queryManager, 
			boolean pivotFormVisible, boolean negativeShownRed) {
		Component p = new JLabel("Result returned for query: " + qr.query);
		JdbcTypes jdbcType = qr.getServerConfig() != null ? qr.getServerConfig().getJdbcType() : JdbcTypes.KDB;
		
		if(qr.isCancelled() || qr.isException()) {
			String errMsg = qr.getE() == null ? null : qr.getE().toString();
			Box searchBox = getSearchBoxes(qr.getQuery(), errMsg, jdbcType);
			if (qr.e instanceof KException) {
				// Not currently adding AI search to KX as it's useless.
				p = KError.getDescriptionComponent((KException) qr.e);	
			} else {
				// Non-KDB show an advert on errors
				JScrollPane newsPanel = UpdateHelper.getNewsPanel(jdbcType);
				JTextArea errDetails = Theme.getTextArea("qryErr", errMsg);
				int sz = MyPreferences.INSTANCE.getCodeFontSize();
				Font f = new Font(MyPreferences.INSTANCE.getCodeFont(), Font.PLAIN, sz);
				errDetails.setFont(f);
				p = Theme.getErrorBox(qr.getConsoleView(), searchBox, errDetails, newsPanel);
			}
		} else if (qr.k != null || qr.rs != null) { // try to show simple KDB objects similar to console
				Box b = Box.createVerticalBox();
				p = b;
				try {
					if(pivotFormVisible && qr.rs != null) {
						PivotFormConfig pc = qr.getPivotConfig() == null ? new PivotFormConfig(qr.query, qr.rs.getMetaData()) : qr.getPivotConfig();
						if(pc.getByColsShown().size() >= 1) { // Only show if something to group or pivot on
							PivotForm form = new PivotForm(pivotConfig -> {
								queryManager.sendQuery(pivotConfig, qr.query, null);
							});
							form.setDisplay(pc, qr.getQuery());
							form.setPreferredSize(new Dimension(Integer.MAX_VALUE, 30));
							form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
							b.add(form);
						}
					}
				} catch (SQLException e1) {
					b.add(new JLabel("Problem adding PulsePivot Form"));
				}
				if(qr.k != null) { // make sure to prioritize kdb view first
					b.add(KdbHelper.getComponent(qr.k, maxRowsShown));
				} else {
					try {
						b.add(TableFactory.getTable(qr.rs, maxRowsShown, negativeShownRed));
					} catch (SQLException e) {
						p = Theme.getErrorBox("Error rendering Table Result Set");
					}
				}
		} else  {
			String txt = qr.getConsoleView();
			if(qr.isKResultTooLarge()) {
				txt = "Data was too large to return, adjust maximum allowed size in settings if desired." +
						"\r\nconsole view:\r\n\r\n" + qr.getConsoleView();
			}
			p = new JScrollPane(Theme.getTextArea("consoleView", txt));
		}

		JPanel panel = new JPanel(new BorderLayout());

		if (p != null) {
			panel.add(p);
			p.setName("kdbResultPanel");
		}
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		return panel;
	}


	private static Box getSearchBoxes(String qry, String errMsg, JdbcTypes jdbcType) {
		String db = jdbcType == null ? "" : (jdbcType.getNiceName() + " ");
		Box box = Box.createHorizontalBox();
		JButton googButton = new JButton("Google", Theme.CIcon.GOOGLE.get16());
		try {
			String googQry = URLEncoder.encode(db + errMsg, "UTF-8");
			googButton.addActionListener(e -> { HtmlUtils.browse("https://www.google.com?q=" + googQry); });
		} catch (UnsupportedEncodingException e1) {
			googButton.setEnabled(false);
			googButton.setToolTipText("Error translating query.");
		}
		JButton openAiButton = new JButton("AI", Theme.CIcon.AI.get16());
		openAiButton.addActionListener(e -> {
			boolean hasKey = CommonActions.checkForOpenAIkey();

			if(hasKey) {
				openAiButton.setEnabled(false);
				BackgroundExecutor.EXECUTOR.execute(() -> {
					try {
						String aiQry = "I ran the SQL query: \n```\n" + qry + "\n```\n" + "which causes this SQL database error in " 
					+ db + ": " + errMsg + "\nWhat is causing the error and how do I fix it?"; 
						AIresult r = AIFacade.queryOpenAIstructured(aiQry);
						JOptionPane.showMessageDialog(null, r.getFirstContent());
						openAiButton.setEnabled(true);
					} catch (IOException e1) {
						openAiButton.setEnabled(false);
						openAiButton.setToolTipText("Error translating query.");
					}
				});
			}
		});
		JButton copyToClipboardButton = new JButton("Copy to Clipboard", Theme.CIcon.EDIT_COPY.get16());
		copyToClipboardButton.addActionListener(e -> {
			Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
			clpbrd.setContents(new StringSelection(errMsg), null);
		});
		
		box.add(googButton);
		box.add(openAiButton);
		box.add(copyToClipboardButton);
		return box;
	}


	public void clearAndSetContent(final Component comp) {
		EventQueue.invokeLater(new Runnable() {
			@Override public void run() {
				removeAll();
				add(comp, BorderLayout.CENTER);
				revalidate();
			}
		});
	}

	public void setMaximumRowsShown(int maxRowsShown) {
		Preconditions.checkArgument(maxRowsShown > 0);
		this.maxRowsShown = maxRowsShown;
	}

	@Override public GrabItem grab() {
		if(lastQueryResult != null) {
			String title = StringUtils.abbreviate(lastQueryResult.query, 50) + " - Result";
			return new GrabItem(getComponent(lastQueryResult, maxRowsShown, queryManager, false, negativeShownRed), title);	
		}
		return null;
	}


	public void togglePivotFormVisible() {
		this.pivotFormVisible = !this.pivotFormVisible;
		if(lastQueryResult != null) {
			regenerateDisplay(lastQueryResult);
		}
	}

}
