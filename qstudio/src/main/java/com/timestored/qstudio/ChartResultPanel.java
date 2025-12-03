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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.google.common.base.Preconditions;
import com.timestored.StringUtils;
import com.timestored.TimeStored;
import com.timestored.connections.ServerConfig;
import com.timestored.jgrowl.Growler;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.model.QueryAdapter;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.qstudio.model.QueryResult;
import com.timestored.sqldash.ChartAppearancePanel;
import com.timestored.sqldash.ChartControlPanel;
import com.timestored.sqldash.ChartWidget;
import com.timestored.sqldash.Queryable;
import com.timestored.sqldash.chart.ChartTheme;
import com.timestored.swingxx.SwingUtils;
import com.timestored.theme.Icon;
import com.timestored.theme.Theme;
import com.timestored.theme.Theme.InputLabeller;

/**
 * Display chart and control panel that allows configuring chart.
 */
class ChartResultPanel extends JPanel implements GrabableContainer {
	
	private static final long serialVersionUID = 1L;
	private static final int PADDING = 10;
	private ChartWidget app;
	private final Growler growler;
	private final QueryManager adminModel;
    final Random R = new Random();

	private QueryResult latestQueryResult;
	private ExportPanel exportPanel;
	private ChartAppearancePanel appearancePanel;

	public ChartResultPanel(final QueryManager adminModel, Growler growler) {
		this.growler = Preconditions.checkNotNull(growler);
		this.adminModel = Preconditions.checkNotNull(adminModel);
		
		app = new ChartWidget();
		
		// adapts queryManager events to trigger tabChange events for kdbChartPanel
		adminModel.addQueryListener(new QueryAdapter() {

			@Override public void sendingQuery(ServerConfig sc, String query) {}

			@Override public void queryResultReturned(ServerConfig sc, QueryResult qr) {
				showQueryResult(qr);
			}
			
		});
		
		if(adminModel.hasAnyServers()) {
			add(UpdateHelper.getNewsPanel(null), BorderLayout.CENTER);
		}
	}
	
	public void showQueryResult(QueryResult qr) {
		if(exportPanel == null) {
			resetContent();
		}
		// ignore as we will send tab soon
		app.setIgnoreConfigChanges(true); 
		String qsrv = adminModel.getSelectedServerName();
		Queryable q = new Queryable(qsrv, qr.query);
		app.setQueryable(q);
		app.setIgnoreConfigChanges(false);
		exportPanel.setEnabled(false);
		
		// ok now send new data for redraw
		latestQueryResult = qr;
		if(qr.isException()) {
			app.queryError(q, qr.e);
		} else if (qr.isCancelled()) {
			app.queryError(q, new IOException("Query Cancelled"));
		} else {
			app.tabChanged(q, qr.rs);
			exportPanel.setEnabled(qr.rs != null);
			// Update appearance panel with new result set columns
			if(appearancePanel != null && qr.rs != null) {
				appearancePanel.updateSeriesConfig(qr.rs);
			}
		}
	}
	

	public void setChartTheme(ChartTheme chartTheme) {
		if(app != null) {
			app.setChartTheme(Preconditions.checkNotNull(chartTheme));
		}
	}
	
	private void resetContent() {
		removeAll();
		
		// Use BorderLayout for the config panel to properly position elements
		JPanel configPanel = new JPanel(new BorderLayout(0, 10));
		
		// Top section: chart controls
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.add(new ChartControlPanel(app));
		configPanel.add(topPanel, BorderLayout.NORTH);
		
		// Center section: appearance panel (takes remaining space)
		appearancePanel = new ChartAppearancePanel(app);
		configPanel.add(appearancePanel, BorderLayout.CENTER);
		
		// Bottom section: export buttons
		exportPanel = new ExportPanel(growler);
		exportPanel.setEnabled(false);
		configPanel.add(exportPanel, BorderLayout.SOUTH);
		
		// Set a reasonable preferred width for the config panel
		configPanel.setPreferredSize(new java.awt.Dimension(450, 0));
		
		setLayout(new BorderLayout());
        add(configPanel, BorderLayout.WEST);

		JPanel p = new JPanel(new BorderLayout(PADDING, PADDING));
		p.add(app.getPanel(), BorderLayout.CENTER);
        add(p, BorderLayout.CENTER);
        repaint();
	}

	private class ExportPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		private final JButton popoutButton;
//		private final JButton exportButton;
		private final JButton copyMarkdownButton;
		private final JButton configureButton;

		@Override public void setEnabled(boolean enabled) {
			popoutButton.setEnabled(enabled);
			copyMarkdownButton.setEnabled(enabled);
			configureButton.setEnabled(enabled);
			super.setEnabled(enabled);
		}
		
		public ExportPanel(Growler growler) {
			
			configureButton = new JButton("HTML5", Theme.CIcon.LAYOUT_EDIT.get16());
			configureButton.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					HtmlUtils.browse(TimeStored.Page.QSTUDIO_HELP_CHARTCONFIG.url());
				}
			});
//
//			exportButton = new JButton("Export to Pulse", Theme.CIcon.SQLDASH_LOGO.get16());
//			exportButton.addActionListener(new ActionListener() {
//				
//				@Override public void actionPerformed(ActionEvent e) {
//					try {
//						Queryable q = app.getQ();
//						String chartVS = app.getViewStrategy().getDescription();
//						String chartType =  URLEncoder.encode(chartVS, "UTF-8"); // TODO translate
//						String qry = URLEncoder.encode(q.getQuery(), "UTF-8");
//						String srvr = URLEncoder.encode(q.getServerName(), "UTF-8");
//						String url = "http://localhost:8080/sqleditor?chart=" + chartType + "&qry=" + qry + "&server=" + srvr;
//						java.awt.Desktop.getDesktop().browse(new URI(url));
//					} catch (IOException | URISyntaxException e1) {
//						e1.printStackTrace();
//					}
//				}
//			});

			copyMarkdownButton = new JButton("Copy Markdown", Theme.CIcon.MARKDOWN_GREEN.get16()) {
				@Override public boolean isEnabled() {
					return app.getViewStrategy().getPulseName() != null;
				}
			};
			copyMarkdownButton.addActionListener(new ActionListener() {
				
				@Override public void actionPerformed(ActionEvent e) {
					try {
						Queryable q = app.getQ();
						String chartVS = app.getViewStrategy().getPulseName();
						if(chartVS != null) {
							String mdCommand = "```sql type='" + chartVS + "' server='" + q.getServerName() + "' " 
									+ "\n" + q.getQuery()
									+ "\n```";
							Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
							clpbrd.setContents(new StringSelection(mdCommand ), null);
							growler.showInfo("Copied to clipboard:\r\n" + mdCommand, "Clipboard Set");
						} else {
							growler.showWarning("No export possible. Try a different chart type.", "No export possible.");
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			popoutButton = new JButton("Popout", Theme.CIcon.POPUP_WINDOW.get16());
			popoutButton.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {

					if(latestQueryResult != null && latestQueryResult.rs != null) {
						ChartWidget cw  = new ChartWidget(app);
						Queryable q = cw.getQ();
						String title = q.getQuery() + " - " + q.getServerName();
						JPanel p = cw.getPanel(); // You must get panel then refresh!!
						cw.tabChanged(q, latestQueryResult.rs);
						Icon ic = cw.getViewStrategy().getIcon();
						BufferedImage bi = ic == null ? null : ic.getBufferedImage();
						JFrame f =  SwingUtils.getPopupFrame(ChartResultPanel.this, title, p, bi);
						f.setVisible(true);
					}
				}
			});

			JPanel b = new JPanel(new GridLayout(1, 3, 4, 0)); // 4px gap between buttons
			setLayout(new BorderLayout());

			b.add(popoutButton);
			b.add(configureButton);
			b.add(copyMarkdownButton);

			add(b, BorderLayout.NORTH);

		}
		
	}

	
	
	@Override public GrabItem grab() {
		if(latestQueryResult != null) {
			ChartWidget cw  = new ChartWidget(app);
			Queryable q = cw.getQ();
			String title = q.getQuery() + " - " + q.getServerName();
			JPanel p = cw.getPanel();
			cw.tabChanged(q, latestQueryResult.rs);
			
			return new GrabItem(p, StringUtils.abbreviate(title, 50), cw.getTSIcon());
		}
		return null;
	}

}
