package com.timestored.sqldash.chart;

import java.awt.Component;
import java.sql.ResultSet;
import java.util.List;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.DefaultXYZDataset;

import com.timestored.sqldash.chart.ChartResultSet.NumericCol;

/**
 * An XYZ Updateable view that:
 * <ul>
 * <li>Needs 3 numerical columns, which are used for X/Y/Z respectively</li>
 * <li>Each group of 3 numerical columns is used in turn</li>
 * </ul>
 */
class XYZDatasetUpdateableView implements UpdateableView {

	// TODO convert this to tagged rows, where we can group into colours, have individual lables
	// But this is too much time right now.
	
	private final ChartPanel chartPanel;
	private final DefaultXYZDataset dataset;

	public XYZDatasetUpdateableView(ChartPanel chartPanel, DefaultXYZDataset dataset) {
		this.dataset = dataset;
		this.chartPanel = chartPanel;
	}
	
	@Override public void update(ResultSet rs, ChartResultSet chartResultSet) 
			throws ChartFormatException {

		if(chartResultSet==null) {
			throw new ChartFormatException("Could not create chart result set.");
		}
		
		for(int s=0; s<dataset.getSeriesCount(); s++) {
			dataset.removeSeries(dataset.getSeriesKey(s));
		}
		
		add(chartResultSet, dataset);
		
		/*
		 * This code was designed for bubble chart, could be specific to just it 
		 */
		XYPlot xyplot = chartPanel.getChart().getXYPlot();
		if(xyplot != null) {
			// we always want to show origin
			double minY = 0;
			double maxY = 0;
			double minX = 0;
			double maxX = 0;
			
			// getting the range, in order to set the axis
			for(int series = 0; series<dataset.getSeriesCount(); series++) {
				for(int item=0; item<dataset.getItemCount(series); item++) {
					double x = dataset.getX(series, item).doubleValue();
					if(Double.isFinite(x)) {
						if(x < minX) {
							minX = x;
						} else if(x > maxX) {
							maxX = x;
						}
					}

					double y = dataset.getY(series, item).doubleValue();
					if(Double.isFinite(y)) {
						if(y < minY) {
							minY = y;
						} else if(y > maxY) {
							maxY = y;
						}
					}
				}
			}		
			List<NumericCol> numCols = chartResultSet.getNumericColumns();
			NumberAxis numberaxisX = (NumberAxis) xyplot.getDomainAxis();
			numberaxisX.setLabel(numCols.get(0).getLabel());
			numberaxisX.setRange(minX > 0? 0 : minX*1.5, maxX < 0? 0 : maxX*1.15);
			double rangeMinX = minX > 0? 0 : minX*1.5;
			double rangeMaxX = maxX < 0? 0 : maxX*1.15;
			if(Double.isFinite(rangeMinX) && Double.isFinite(rangeMaxX) && rangeMinX < rangeMaxX) {
				numberaxisX.setRange(rangeMinX, rangeMaxX);
			}
			
			NumberAxis numberaxisY = (NumberAxis) xyplot.getRangeAxis();
			numberaxisY.setLabel(numCols.get(1).getLabel());
			double rangeMinY = minY > 0? 0 : minY*1.5;
			double rangeMaxY = maxY < 0? 0 : maxY*1.15;
			if(Double.isFinite(rangeMinY) && Double.isFinite(rangeMaxY) && rangeMinY < rangeMaxY) {
				numberaxisY.setRange(rangeMinY, rangeMaxY);
			}
		}
		
	}

	@Override public Component getComponent() {
		return chartPanel;
	}
	

	/** 
	 * Add the new data in {@link ResultSet} to the {@link Dataset} if possible. 
	 */
	private static DefaultXYZDataset add(ChartResultSet colResultSet, DefaultXYZDataset dataset) 
			throws ChartFormatException {

		// check dataset is usable
		List<NumericCol> numCols = colResultSet.getNumericColumns();
		if(numCols.size() < 3) {
			throw new ChartFormatException("Need atleast three numerical columns.");
		}
		
		double[][] vals = new double[3][];
		for(int i = 0; i < 3; i++) {
				vals[i] = numCols.get(i).getDoubles();
		}
		dataset.addSeries("", vals); // this should be row groups eventually
		

		return dataset;
	}

}
