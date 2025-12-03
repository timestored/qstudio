package com.timestored.sqldash;

import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.timestored.sqldash.chart.ChartAppearanceConfig;
import com.timestored.sqldash.chart.ChartTheme;
import com.timestored.sqldash.chart.ChartViewConfiguration;
import com.timestored.sqldash.chart.DataTableViewStrategy;
import com.timestored.sqldash.chart.JdbcChartPanel;
import com.timestored.sqldash.chart.ViewStrategy;
import com.timestored.sqldash.chart.ViewStrategyFactory;
import com.timestored.theme.Theme;
import com.timestored.theme.Theme.CIcon;

import lombok.Getter;

/**
 * A reusable graphical element that represents a single collection of data.
 * Contains all configuration related to what query is selected
 * and the view strategies appearance etc.
 */
public class ChartWidget extends AbstractWidget {

   	private final ChartViewConfiguration chartViewConfig;
   	
   	private ChartTheme chartTheme;
   	private ViewStrategy viewStrategy;
   	private JdbcChartPanel chartPanel = null;
	private ResultSet prevRS = null;
	@Getter private boolean renderLargeDataSets = false;
	@Getter private ChartAppearanceConfig appearanceConfig = new ChartAppearanceConfig();
	private Queryable q = new Queryable();
	private final List<Queryable> queryable = new ArrayList<Queryable>(1);

	/** stores the last non-tabular view strategy that was set or null if there was none **/
   	private ViewStrategy prevNonTabVS;;
   	
   	
	/** When this is true, the chart will not update when a {@link Widget} config changes */
	private boolean ignoreConfigChanges = false;

	private Widget.Listener updateListener;
	private final Queryable.Listener queryableListener = new Queryable.Listener() {
		@Override public void configChanged(Queryable queryable) {
			ChartWidget.this.configChanged();
		}
	};

	public ChartWidget() {
		super();
		queryable.add(q);
		this.chartViewConfig = new ChartViewConfiguration();
		viewStrategy = ViewStrategyFactory.getStrategies().get(0);
		chartTheme = ViewStrategyFactory.getThemes().get(0);
		q.addListener(queryableListener);
	}
	
	/**
	 * Create a copy of the selected app (not adding same listeners).
	 */
	public ChartWidget(ChartWidget app) {
		super(app);
		this.q = new Queryable(app.getQ());
		queryable.add(q);
		this.viewStrategy = app.viewStrategy;
		this.chartTheme = app.chartTheme;
		this.chartViewConfig = null;
		this.appearanceConfig = new ChartAppearanceConfig(app.appearanceConfig);
		q.addListener(queryableListener);
	}
	
	
	public void setChartTheme(ChartTheme chartTheme) {
		this.chartTheme = chartTheme;
		configChanged();
	}
	
	public ChartTheme getChartTheme() { return chartTheme; }

	
	public void setViewStrategy(ViewStrategy viewStrategy) {
		if(!this.viewStrategy.equals(DataTableViewStrategy.getInstance())) {
			prevNonTabVS = this.viewStrategy;
		}
		this.viewStrategy = viewStrategy;
		configChanged();
	}
	
	public ViewStrategy getViewStrategy() { return viewStrategy; }
	
	public ChartViewConfiguration getChartViewConfig() { return chartViewConfig; }
	
	public void setAppearanceConfig(ChartAppearanceConfig config) {
		this.appearanceConfig = config != null ? config : new ChartAppearanceConfig();
		// Don't call configChanged() for appearance config changes - that triggers
		// a full chart rebuild which is slow and steals focus from the editor.
		// Instead, directly update the chart panel's appearance and refresh.
		if (chartPanel != null) {
			chartPanel.setAppearanceConfig(appearanceConfig);
			chartPanel.update(prevRS);
		}
	}
	
	public void setRenderLargeDataSets(boolean renderLargeDataSets) {
		this.renderLargeDataSets = renderLargeDataSets;
		configChanged();
	}

	@Override public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("viewStrategy", viewStrategy)
			.add("chartTheme", chartTheme)
			.add("chartViewConfig", chartViewConfig)
			.add("queryable", q)
			.toString();
	}

	private JdbcChartPanel generateChart() {
		synchronized (this) {
			chartPanel = ViewStrategyFactory.getJdbcChartpanel();
			chartPanel.setViewStrategy(getViewStrategy());
			chartPanel.setTheme(getChartTheme());
			chartPanel.setAppearanceConfig(appearanceConfig);
	
			updateListener = new Widget.Listener() {
				
				@Override public void configChanged(Widget app) {
					chartPanel.setRenderLargeDataSets(app.isRenderLargeDataSets());
					chartPanel.setViewStrategy(viewStrategy);
					chartPanel.setTheme(chartTheme);
					chartPanel.setAppearanceConfig(appearanceConfig);
					if(!ignoreConfigChanges) {
						chartPanel.update(prevRS);
					}
				}
			};
			addListener(updateListener);
			
		}
		
        return chartPanel;		
	}

	@Override public void invalidatePanelCache() {
		if(chartPanel != null) {
			synchronized (this) {
				if(chartPanel != null) {
					removeListener(updateListener);
					chartPanel = null;
					updateListener = null;
				}
				
			}
		}
	}

	@Override public void tabChanged(Queryable w, ResultSet rs) {
		if(chartPanel!=null && w==q && !ignoreConfigChanges) {
			prevRS = rs;
			chartPanel.update(rs);	
		}
	}

	@Override public void queryError(Queryable w, Exception e) {
		if(chartPanel!=null && w==q && !ignoreConfigChanges) {
			prevRS = null;
			chartPanel.update(e);
		}
	}

	/**
	 * Setting true means all underlying {@link Widget} configChanges will not cause a chart repaint
	 * , allows changing meaning settings without many redraws but make sure to reset to true.
	 */
	public void setIgnoreConfigChanges(boolean ignoreConfigChanges) {
		this.ignoreConfigChanges = ignoreConfigChanges;
	}

	@Override public JPanel getPanel() {
		if(chartPanel == null) {
			synchronized (this) {
				if(chartPanel == null) {
					chartPanel = generateChart();
				}
				
			}
		}
		return chartPanel;
	}

	@Override public Collection<Queryable> getQueryables() { return queryable; }

	/** charts have only a single queryable, this returns it **/
	public Queryable getQ() {
		return q;
	}

	
	public void setQueryable(Queryable q) {
		this.q.removeListener(queryableListener);
		this.q = q;
		queryable.set(0, q);
		this.q.addListener(queryableListener);
		configChanged();
	}

	public com.timestored.theme.Icon getTSIcon() {
		if(viewStrategy != null && viewStrategy.getIcon() != null) {
			return viewStrategy.getIcon();
		}
		return Theme.CIcon.CHART_CURVE;
	}
	
	@Override public Icon getIcon() {
		return getTSIcon().get16();
	}

	@Override public Collection<Action> getActions() {
		Action toggleTabView = new AbstractAction("Toggle Table/Chart View", 
				CIcon.TABLE_ELEMENT.get16()) {
			@Override public void actionPerformed(ActionEvent e) {
				boolean isTab = ChartWidget.this.viewStrategy.equals(DataTableViewStrategy.getInstance());
				if (isTab && prevNonTabVS!=null) {
					setViewStrategy(prevNonTabVS);
				} else {
					setViewStrategy(DataTableViewStrategy.getInstance());
				}
			}
		};
		return Lists.newArrayList(toggleTabView);
	}
}
