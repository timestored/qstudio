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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.google.common.base.Preconditions;
import com.timestored.connections.ServerConfig;
import com.timestored.qstudio.model.QueryAdapter;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.qstudio.model.QueryResult;
import com.timestored.swingxx.AAction;
import com.timestored.swingxx.TabbedPaneRightClickBlocker;
import com.timestored.theme.Theme;
import com.timestored.theme.Theme.CIcon;

import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

/**
 * Display the last {@link #FIXED_SIZE} queries sent 
 * and allow seeing old results and resending. 
 */
class QueryHistoryPanel extends JPanel implements GrabableContainer {

	private static final long serialVersionUID = 1L;
	private static final int FIXED_SIZE = 3;
	private static final List<QueryHistoryPanelListener> listeners = new LinkedList<>();
	
	private static final String HIST_PREFIX = "History-";
	private static final CIcon PIN_ICON = Theme.CIcon.PIN_GREEN;
	private static int historyCount = 1;
	
	
	void addListener(QueryHistoryPanelListener listener) {
		listeners.add(listener);
	}
	
	void removeListener(QueryHistoryPanelListener listener) {
		listeners.remove(listener);
	}
	
	@FunctionalInterface
	public static interface QueryHistoryPanelListener {
		public void resultSelected(@NonNull QueryResult queryResult);
	}
	
	@Data private static class TabDetails {
		final QueryResult queryResult;
		String title;
		boolean pinned;
		
		public TabDetails(QueryResult queryResult, String title, boolean pinned) {
			this.queryResult = queryResult;
			this.title = title;
			this.pinned = pinned;
		}
		
		public CIcon getIcon() {
			return (queryResult.e != null ? Theme.CIcon.ERROR 
					: queryResult.getRs() != null ? Theme.CIcon.TABLE_ELEMENT 
					: queryResult.getK() != null ? Theme.CIcon.LAMBDA_ELEMENT	: Theme.CIcon.FUNCTION_ELEMENT);
		}
		public String getTooltipText() {
			return queryResult.getResultType() + " " + queryResult.getMillisTaken() + " ms\nquery: " + queryResult.getQuery();
		}
	}
	
	private final QueryManager queryManager;
	private final List<TabDetails> tabs = new LinkedList<>();
	@Setter private boolean negativeShownRed = true;
	
	private TabLayoutPolicy tabLayoutPolicy;
	private final JTabbedPane tabbedPane;
	
	private int maxRowsShown = Integer.MAX_VALUE;

	QueryHistoryPanel(QueryManager queryManager) {
		
		this.queryManager = queryManager;
		setLayout(new BorderLayout());

        // create the tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setName("historyTabbedPane");
        tabbedPane.setMinimumSize(new Dimension(200*2, 1));
        tabbedPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // convulated wrapping to prevent right-clicks changing tabs.
		TabbedPaneRightClickBlocker.install(tabbedPane);
        // this pops up between gaps in the tabs.
		tabbedPane.addMouseListener(new MouseAdapter() {

			@Override public void mouseClicked(MouseEvent e) {
			    if (e.getClickCount() == 2) {
			        int idx = tabbedPane.indexAtLocation(e.getX(), e.getY());
			        if (idx >= 0 && idx < tabs.size()) {
			            handleRenameAndPin(idx);
			        }
			        return; // do not process popup after double-click
			    }


			    // 1. Middle click (button 2) toggles pin state
			    if (e.getButton() == MouseEvent.BUTTON2) {
			        int idx = tabbedPane.indexAtLocation(e.getX(), e.getY());
			        if (idx >= 0 && idx < tabs.size()) {
			            togglePin(idx);
			        }
			        return;
			    }

			    
				maybeHandleClick(e); 
			}

			@Override public void mouseReleased(MouseEvent e) { maybeHandleClick(e); }

			private void maybeHandleClick(MouseEvent e) {
				if (e.isPopupTrigger()) {
					// Find the closest doc.
					if(e.getSource() instanceof JTabbedPane) {
					     JTabbedPane tabbedPane = (JTabbedPane)e.getSource();
				    	 int idx = tabbedPane.indexAtLocation(e.getX(), e.getY());
			    	 	if(idx >= 0 && idx < tabs.size()) {
			    	 		TabDetails tabDetails = tabs.get(idx);
			    	 		if(tabDetails != null) {
			    	 			new TabsPopupMenu(tabDetails).show(e.getComponent(), e.getX(), e.getY());
			    	 		}
			    	 	}
					}
				}
			}
		});
		tabbedPane.addChangeListener(e -> {
			int idx = tabbedPane.getSelectedIndex();
			if(idx >= 0 && idx < tabs.size()) {
				TabDetails td = tabs.get(idx);
				if(td != null) {
					for(QueryHistoryPanelListener listener : listeners) {
						listener.resultSelected(td.getQueryResult());
					}
				}
			}
		});

		add(tabbedPane, BorderLayout.CENTER);
		
		
		queryManager.addQueryListener(new QueryAdapter() {

			@Override public void queryResultReturned(ServerConfig sc, QueryResult queryResult) {
				addToHistory(queryResult);
//				display(history.size()-1);
			}
		});
	}
	
	private void handleRenameAndPin(int idx) {
	    TabDetails td = tabs.get(idx);
	    if (td == null) {
	        return;
	    }

	    String currentTitle = td.getTitle();
	    String newTitle = javax.swing.JOptionPane.showInputDialog(this,"Enter new title:",currentTitle);
	    if (newTitle == null || newTitle.trim().isEmpty()) {
	        return; // cancelled or blank
	    }
	    td.setTitle(newTitle);
	    td.setPinned(true);

	    // Update the tab’s visible title & icon
	    tabbedPane.setTitleAt(idx, newTitle);
	    tabbedPane.setIconAt(idx, PIN_ICON.get16());
	}

	private void togglePin(int idx) {
	    TabDetails td = tabs.get(idx);
	    if (td == null) return;

	    boolean nowPinned = !td.isPinned();
	    td.setPinned(nowPinned);

	    if (nowPinned) {
	        tabbedPane.setIconAt(idx, PIN_ICON.get16());
	    } else {
	        tabs.remove(idx);
	        tabbedPane.removeTabAt(idx);
	    }
	}
	
	class TabsPopupMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;
		
		public TabsPopupMenu(final TabDetails tabDetails) {
	        setName("TabsPopupMenu");
	        int idx = tabs.indexOf(tabDetails);
	        if(idx < 0) {
	        	return; // should not happen
	        }
	        
	        String pinTitle = tabDetails.pinned ? "Close Tab" : "Pin Tab";
			Action pinTab = new AAction(pinTitle, PIN_ICON.get16(), ae -> togglePin(idx));
			add(pinTab);
			add(new AAction("Rename Tab", ae -> handleRenameAndPin(idx)));
		}

	}
	
	
	private void addToHistory(QueryResult qr) {
		// scroll to bottom
		EventQueue.invokeLater(() -> {
			
			int c = tabbedPane.getTabCount();
			List<Integer> histTabIndices = new ArrayList<>();
			
			for(int i=0; i<c; i++) {
				String n = tabbedPane.getTitleAt(i);
				if(n != null && n.startsWith(HIST_PREFIX) && !tabs.get(i).pinned) {
					histTabIndices.add(i);
				}
			}
				
			if(histTabIndices.size() > FIXED_SIZE) {
				for(int i=histTabIndices.size()-1; i>=FIXED_SIZE; i--) {
					int tabIdx = histTabIndices.get(i);
					tabbedPane.removeTabAt(tabIdx);
					tabs.remove(tabIdx);
				}
			}

			// restore title before moving
			if(tabs.size() > 0) {
				tabbedPane.setTitleAt(0, tabs.get(0).getTitle());
			}
			TabDetails td = new TabDetails(qr, HIST_PREFIX+historyCount++, false);
			tabs.add(0, td);
			tabbedPane.insertTab(td.getTitle(), Theme.CIcon.PIN.get16(), getPanel(td), td.getTooltipText(), 0);
			tabbedPane.setSelectedIndex(0);
			tabbedPane.setTitleAt(0, "Latest");
		});
	}

	private Component getPanel(TabDetails td) {
		return KDBResultPanel.getDisplay(td.queryResult, maxRowsShown, queryManager, false, negativeShownRed);
	}

	void setMaximumRowsShown(int maxRowsShown) {
		Preconditions.checkArgument(maxRowsShown > 0);
		this.maxRowsShown = maxRowsShown;
	}

	public void setTabLayoutPolicy(TabLayoutPolicy tabLayoutPolicy) {
		if(tabLayoutPolicy != null && !tabLayoutPolicy.equals(this.tabLayoutPolicy)) {
			this.tabLayoutPolicy = tabLayoutPolicy;
    		tabbedPane.setTabLayoutPolicy(tabLayoutPolicy.isScroll() ? 
    					JTabbedPane.SCROLL_TAB_LAYOUT : JTabbedPane.WRAP_TAB_LAYOUT);
    		tabbedPane.setTabPlacement(tabLayoutPolicy.isVertical() ? JTabbedPane.LEFT : JTabbedPane.NORTH);
    		UIManager.put("TabbedPane.tabRotation", tabLayoutPolicy.isVertical() ? "auto" : "none");	
		}
    }
	
	@Override public GrabItem grab() {
		int idx = tabbedPane.getSelectedIndex();
		if(idx >= 0 && idx < tabs.size()) {
			TabDetails td = tabs.get(idx);
			if(td != null) {
				return new GrabItem(getPanel(td), td.getTitle(), td.getIcon());
			}
		}
		return null;
	}

}
