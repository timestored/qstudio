package com.timestored.sqldash.chart;

import java.awt.Component;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.TableOrder;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.timestored.connections.JdbcTypes;
import com.timestored.sqldash.theme.DBIcons;
import com.timestored.theme.Icon;

/**
 * Strategy for displaying {@link ResultSet}'s as pie charts. 
 * See {@link #getFormatExplainationHtml()} for details.
 */
public enum PieChartViewStrategy implements ViewStrategy {

	INSTANCE;
	
	private static final String[] FORMATA = 
		{ "Each numerical column represents a pie chart.",
				"The title of each pie chart will be the column title.",
				"Each row will be a section of the pie and will use the row title as a label." };

	@Override public UpdateableView getView(final ChartTheme theme) {
		Preconditions.checkNotNull(theme);
		
		return new HardRefreshUpdateableView(new HardRefreshUpdateableView.ViewGetter() {


			@Override public Component getView(ResultSet resultSet, ChartResultSet colResultSet) 
					throws ChartFormatException {
				
		        if(colResultSet.getNumericColumns().size() < 1) {
		        	throw new ChartFormatException("There must be atleast one number column.");
		        }
		        
		        // If no string column and only one row, transpose it to show something sensible.
		        // 
		        // 
		        // size | price | age
		        // 10   | 1.1   | 20
		        //
		        // Name | Val
		        // size | 10
		        // price| 1.1
		        // age  | 20

				if(colResultSet.getRowCount() == 1 && colResultSet.getNumericColumns().size() > 1) {
			        colResultSet = ChartResultSetBuilder.transpose(colResultSet);
				}
		        
		        DefaultCategoryDataset catData;
		        catData = CategoryDatasetUpdateableView.add(colResultSet, new DefaultCategoryDataset());
		        boolean showLegend = colResultSet.getRowCount() < 100;
				final JFreeChart chart = ChartFactory.createMultiplePieChart("", 
						catData, TableOrder.BY_ROW, showLegend, true, false);

				if(catData.getRowCount()>1) {
					MultiplePiePlot plot = (MultiplePiePlot) chart.getPlot();
					JFreeChart subchart = plot.getPieChart();
					PiePlot p = (PiePlot) subchart.getPlot();
					p.setToolTipGenerator(new StandardPieToolTipGenerator(Tooltip.LABEL_XY_FORMAT, 
							new DecimalFormat("#,###.##"), NumberFormat.getPercentInstance()));
					p.setLabelGenerator(null);
				}
				
				ChartPanel cp = new ChartPanel(theme.apply(chart), false, true, true, false, true);

				return cp;
			}
		});
		
	}

	@Override public String getDescription() { return "Pie Chart"; }

	@Override public Icon getIcon() { return DBIcons.CHART_PIE; }
	
	@Override
	public String toString() {
		return PieChartViewStrategy.class.getSimpleName() + "["
				+ getDescription() + "]";
	}

	@Override public String getFormatExplainationHtml() {
		return "<ol><li>" + Joiner.on("</li><li>").join(FORMATA) + "</li></ol>";
	}
	
	@Override public String getFormatExplaination() {
		return Joiner.on("\r\n").join(FORMATA);
	}

	@Override
	public List<ExampleView> getExamples() {
		
		String name = "Multiple Pie Chart Example";
		String description = "Using multiple numerical columns gives a pie chart for each column.";
		ExampleView MultiPieNQ = new ExampleView(name, description, ExampleTestCases.COUNTRY_STATS);

		name = "Single Pie Chart Example";
		description = "A single numerical columns gives a single pie chart for that column.";
		ExampleView SinglePieNQ = new ExampleView(name, description, ExampleTestCases.COUNTRY_STATS_GDP_ONLY);
		
		return ImmutableList.of(SinglePieNQ, MultiPieNQ);
	}

	@Override public String getQueryEg(JdbcTypes jdbcType) {
		if(jdbcType.equals(JdbcTypes.KDB)) {
			return ExampleTestCases.COUNTRY_STATS_GDP_ONLY.getKdbQuery();
		}
		return null; 
	}
	
	@Override public boolean isQuickToRender(ResultSet rs, int rowCount, int numColumnCount) {
		return rowCount < 700 && numColumnCount <= 160; // 1 seconds on Ryans PC
	}
	
	@Override public String getPulseName() { return "pie"; }
}
