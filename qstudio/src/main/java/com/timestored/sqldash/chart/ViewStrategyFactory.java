package com.timestored.sqldash.chart;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides access to all charting strategies, themes and {@link UpdateableView} 's.
 */
public class ViewStrategyFactory {
	
	private static final List<ViewStrategy> STRATEGIES
		= Collections.unmodifiableList(Arrays.asList(new ViewStrategy[] {
				AutoRedrawViewStrategy.INSTANCE,
				TimeseriesViewStrategy.INSTANCE,
				StepChartViewStrategy.INSTANCE,
				AreaChartViewStrategy.INSTANCE,
				BarChartViewStrategy.INSTANCE,
				StackedBarChartViewStrategy.INSTANCE,
				BubbleChartViewStrategy.INSTANCE,
				CandleStickViewStrategy.INSTANCE,
				HeatMapViewStrategy.INSTANCE,
				HistogramViewStrategy.INSTANCE,
				LineChartViewStrategy.INSTANCE,
				PieChartViewStrategy.INSTANCE,
				ScatterPlotViewStrategy.INSTANCE,
				DataTableViewStrategy.getInstance(),
				NoRedrawViewStrategy.INSTANCE,
		}));


	public static final ChartTheme LIGHT_THEME = DefaultTheme.getInstance(new LightColorScheme(), "Light", "Modern Excel-inspired colors on white background");
	public static final ChartTheme DARK_THEME = DefaultTheme.getInstance(new DarkColorScheme(), "Dark", "VS Code-inspired colors on dark background");
	public static final ChartTheme DARK_ORIGINAL_THEME = DefaultTheme.getInstance(new DarkOriginalColorScheme(), "Dark Original", "Original qStudio dark theme");
	public static final ChartTheme DARK_FIN_THEME = DefaultTheme.getInstance(new DarkFinColorScheme(), "Dark Finance", "Dark theme for trading/financial dashboards");
	public static final ChartTheme EXCEL_THEME = DefaultTheme.getInstance(new ExcelColorScheme(), "Excel", "Classic Microsoft Excel chart colors");
	public static final ChartTheme POWERBI_THEME = DefaultTheme.getInstance(new PowerBIColorScheme(), "PowerBI", "Microsoft Power BI dashboard colors");
	public static final ChartTheme HIGH_CONTRAST_THEME = DefaultTheme.getInstance(new HighContrastColorScheme(), "High Contrast", "Colorblind-safe high contrast palette");
	public static final ChartTheme TABULAR_THEME = DefaultTheme.getInstance(new TableauColorScheme(), "Tabular", "Tableau-inspired color palette");

	private static final List<ChartTheme> THEMES
		= Collections.unmodifiableList(Arrays.asList(new ChartTheme[] {
				LIGHT_THEME, DARK_THEME, DARK_ORIGINAL_THEME, DARK_FIN_THEME, EXCEL_THEME, POWERBI_THEME, HIGH_CONTRAST_THEME, TABULAR_THEME }));
	
	
	public static List<ViewStrategy> getStrategies() {
		return STRATEGIES;
	}

	public static List<ChartTheme> getThemes() {
		return THEMES;
	}
		
	public static JdbcChartPanel getJdbcChartpanel() {
		return new JdbcChartPanel(TimeseriesViewStrategy.INSTANCE, THEMES.get(0));
	}

	
	public static JdbcChartPanel getJdbcChartpanel(ViewStrategy vs) {
		return new JdbcChartPanel(vs, THEMES.get(0));
	}
	
}
