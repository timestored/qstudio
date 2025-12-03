package com.timestored.sqldash.chart;

import java.sql.ResultSet;
import java.util.List;

import com.timestored.connections.JdbcTypes;
import com.timestored.theme.Icon;


/**
 * A strategy of looking at {@link ResultSet} data in a JPanel e.g. chart,table.
 * You get an inital {@link UpdateableView} and then update it with each new {@link ResultSet}, 
 * some strategies may choose to entirely redraw the component others may append data.
 */
public interface ViewStrategy {
	    
		/**
	     * For the given data, give us a panel with a view of that data if possible.
	     * @return a panel showing qtab if possible otherwise false.
	     */
		UpdateableView getView(ChartTheme theme);
		
		/**
	     * For the given data and appearance config, give us a panel with a view of that data if possible.
	     * @param theme The chart theme to apply
	     * @param appearanceConfig Optional appearance configuration (title, colors, axis assignments)
	     * @return a panel showing qtab if possible otherwise false.
	     */
		default UpdateableView getView(ChartTheme theme, ChartAppearanceConfig appearanceConfig) {
			return getView(theme);
		}
		
		/** @return true if this view strategy supports appearance configuration */
		default boolean supportsAppearanceConfig() { return false; }
		
		/** a textual description of this chart type */
		String getDescription();

		/**
		 *  An explanation of the format of QTable format best used and how it affects 
		 *  what is displayed. May contain HTML markup but will not be wrapped
		 *  in an html tag.
		 */
		public String getFormatExplainationHtml();

		/**
		 *  An explanation of the format of QTable format best used and how it affects 
		 *  what is displayed. Will use line breaks to spearate items, No HTML markup.
		 */
		public String getFormatExplaination();
		
		/**  @return Examples of queries.   */
		List<ExampleView>getExamples();

		/**  @return Examples of queries.   */
		String getQueryEg(JdbcTypes jdbcType);

		Icon getIcon();

		boolean isQuickToRender(ResultSet rs, int rowCount, int numColCount);

		String getPulseName();
	}