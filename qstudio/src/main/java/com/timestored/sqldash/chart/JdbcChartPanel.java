package com.timestored.sqldash.chart;

import com.timestored.TimeStored;
import com.timestored.babeldb.DBHelper;
import com.timestored.kdb.KError;
import com.timestored.misc.InfoLink;
import com.timestored.theme.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.util.Log;

import kx.c.KException;
import lombok.Getter;
import lombok.Setter;

import com.google.common.base.Preconditions;

/**
 * A generic chart component that can have the particular type of chart displayed changed, 
 * it's configuration changed, and receive {@link ResultSet} data updates.
 */
public class JdbcChartPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public static boolean TEST_MODE = true;  // Tests need forced instant render. End user UI not really.
	private static final Logger LOG = Logger.getLogger(JdbcChartPanel.class.getName());

	private ViewStrategy viewCreator;
	private ChartTheme theme;
	@Setter @Getter private boolean renderLargeDataSets = false;
	@Getter @Setter private ChartAppearanceConfig appearanceConfig = null;
	
	private UpdateableView updateableView;
	
	/** cache previous RS so that if chart type selected changed we can fill some data */
	private ResultSet prevRS = null;
	private Exception e = null;
	/** contains resulting exception (if any) of latest redraw attempt. **/
	private ChartFormatException lastChartFormatException = null;
	private ChartResultSet prevCRS = null;

	/** Construct a chart panel using inital {@link ViewStrategy} and {@link ViewTheme} */
	JdbcChartPanel(ViewStrategy viewCreator, ChartTheme theme) {

		this.viewCreator = Preconditions.checkNotNull(viewCreator);
		this.theme = Preconditions.checkNotNull(theme);
		updateableView = viewCreator.getView(theme);
		
		setLayout(new GridLayout(1, 0));
		add(updateableView.getComponent());
	}

	
	
	/** Set the theme that controls the colors/font appearance. */
	public void setTheme(ChartTheme theme) {
		if(!this.theme.equals(theme)) {
			this.theme = Preconditions.checkNotNull(theme);
			refreshGUI();
		}
	}

	/** Set the {@link ViewStrategy} that controls what type of chart is shown */
	public void setViewStrategy(ViewStrategy viewCreator) {
		if(!this.viewCreator.equals(viewCreator)) {
			this.viewCreator = Preconditions.checkNotNull(viewCreator);
			refreshGUI();
		}
	}

	private void refreshGUI() {
		LOG.fine("JdbcChartPanel refreshGUI()");

		//TODO with invokeLater all the unit test screenshots break, fix this!?
		final ResultSet rs = prevRS;
		final Exception err = e;
		runInUIthread(() ->runn(rs,err), TEST_MODE);
	}
	
	public static void runInUIthread(Runnable r, boolean invokeAndWait) {
		if(EventQueue.isDispatchThread()) {
			r.run();
		} else if(invokeAndWait) {
			try {
				EventQueue.invokeAndWait(r);
			} catch (InterruptedException | InvocationTargetException e) { 
				LOG.severe("Severe Error Updating Chart");
			}
		} else {
			EventQueue.invokeLater(r);
		}
	}
	
	private boolean isNumType(int sqlType) {
		return sqlType == java.sql.Types.BIGINT || sqlType == java.sql.Types.DECIMAL
			 || sqlType == java.sql.Types.DOUBLE || sqlType == java.sql.Types.FLOAT
			 || sqlType == java.sql.Types.INTEGER  || sqlType == java.sql.Types.NUMERIC
			 || sqlType == java.sql.Types.REAL || sqlType == java.sql.Types.SMALLINT
			 || sqlType == java.sql.Types.TINYINT;
	}

	private void runn(ResultSet rs, Exception err) {
		Component c = null;
		try {
			updateableView = viewCreator.getView(theme, appearanceConfig);
			if(rs!=null) {
				// TODO this line threw a null pointer exception from JfreeChary DefaulHighLowDataset
				// should i let these spiral up or show an error screen?
				boolean isVerySafeToRender = true;
				try {
					int rowCount = DBHelper.getSize(rs);
					int numColCount = 0;
					ResultSetMetaData rsmd = prevRS.getMetaData();
					for(int col=1; col<=rsmd.getColumnCount(); col++) {
						if(isNumType(rsmd.getColumnType(col))) {
							numColCount++;
						}
					}
					isVerySafeToRender = viewCreator.isQuickToRender(rs, rowCount, numColCount);
				} catch (SQLException e) {
					Log.warn("Problem assessing how safe it is to render chart:" + err);
				}
				lastChartFormatException = null;
				if(isVerySafeToRender || renderLargeDataSets) {
					updateableView.update(prevRS, prevCRS);
					c = updateableView.getComponent();
				} else {
        			String html = "<html>The data is large, it has many rows and/or columns."
        					+ "<br /><b>To continue and draw the chart click the checkbox on the left hand side.</b>"
        					+ "<br />This may take some time and consume significant memory.</html>";
        	        c = Theme.getErrorBox("Dataset very large", Theme.getHtmlText(html));
				}
				
			} else {
				if(e instanceof KException) {
					c = new JScrollPane(KError.getDescriptionComponent((KException)e));
				} else if(e != null) {
					String msg = "Error retrieving query";
					if(e != null) {
						msg = e.getMessage();
					}
					Component errDetails = Theme.getTextArea("qryErr", msg);
					c = new JScrollPane(Theme.getErrorBox("Query Error", errDetails));
				} else {
					c = new JScrollPane(Theme.getTextArea("noRes", "No table returned."));
				}
			}
		} catch(ChartFormatException cfe) {
			lastChartFormatException = cfe;
			c = getChartFormatExplaination(viewCreator, cfe);
		} catch(Exception npe) {
			String txt = "Problem updating view from RecordSet";
			LOG.log(Level.SEVERE, txt, npe);
			c =  getChartFormatExplaination(viewCreator, null);
		}
		
		removeAll();
		add(c);
		revalidate();
	}

	private static Component getChartFormatExplaination(ViewStrategy viewStrategy, ChartFormatException cfe) {
		
		String text = viewStrategy.getFormatExplainationHtml();
		JEditorPane editPane = new JEditorPane("text/html", 
				"<html>" + text + "</html>");
		
		final JPanel wrapPanel = Theme.getVerticalBoxPanel();
		wrapPanel.add(Theme.getHeader(viewStrategy.getDescription()));
		if(cfe != null) {
			wrapPanel.add(new JLabel(cfe.getDetails()));
		}
		wrapPanel.add(Theme.getSubHeader("Data Format Expected"));

		editPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		wrapPanel.add(editPane);
		wrapPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		JPanel p = new JPanel(new BorderLayout());
		p.add(InfoLink.getButton("See Example Charts", "Click here to see chart examples", 
				TimeStored.Page.SQLDASH_HELP_EG), BorderLayout.WEST);
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		wrapPanel.add(p);
		
		return new JScrollPane(wrapPanel);
	}
	
	
	/**
	 * Update this chart panel to show the new {@link ResultSet}.
	 * @param resultSet A {@link ResultSet} or null, null means no data was retrieved from DB
	 * 	for an unknown reason.
	 */
	public void update(ResultSet resultSet) {
		prevRS = resultSet;
		e = null;
		prevCRS = null;
		if(resultSet != null) {
			try {
				prevCRS = ChartResultSet.getInstance(resultSet);
			} catch (SQLException e) {
				LOG.log(Level.INFO, "could not create chartResultSet ", e);
			} catch (IllegalArgumentException e) {
				LOG.log(Level.WARNING, "could not create chartResultSet ", e);
			} catch (NullPointerException e) {
				LOG.log(Level.WARNING, "could not create chartResultSet ", e);
			}
		}
		refreshGUI();
	}
	

	/**
	 * Update this chart panel to show that an exception occurred and no result set was returned.
	 */
	public void update(Exception e) {
		prevRS = null;
		this.e = Preconditions.checkNotNull(e);
		prevCRS = null;
		refreshGUI();
	}
	
	public ChartFormatException getLastChartFormatException() {
		return lastChartFormatException;
	}


}
