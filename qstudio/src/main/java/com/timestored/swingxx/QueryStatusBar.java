package com.timestored.swingxx;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.JXStatusBar;

/**
 * Displays sending/receiving events in a {@link JXStatusBar}.
 */
public class QueryStatusBar extends JXStatusBar {
	
	private static final long serialVersionUID = 1L;

	private long startTick;
	private final JProgressBar pbar;
	private final JLabel statusLabel;
	private final JLabel rowCountLabel;
	private final JLabel timingLabel;
	private final JLabel remoteStatusLabel;

	public QueryStatusBar() {

		statusLabel = new JLabel("Ready");
	    Constraint fillConstraint = new Constraint(Constraint.ResizeBehavior.FILL);
	    Constraint fixedWidthConstraint = new Constraint();
	    fixedWidthConstraint.setFixedWidth(100);
	    pbar = new JProgressBar();
		rowCountLabel = new JLabel("Count = 0  ");
		timingLabel = new JLabel("Time = 0 ms   ");

	    add(statusLabel, fillConstraint);
	    add(rowCountLabel);
	    add(timingLabel);
	    add(pbar, fixedWidthConstraint);

	    remoteStatusLabel = new JLabel("");
	    Constraint remoteConstraint = new Constraint();
	    remoteConstraint.setFixedWidth(200);
	    add(remoteStatusLabel, remoteConstraint);

	}
	
	public void startQuery(String query) {
		startTick = new Date().getTime();
		display("sent query: "+query, true, -1, -1);
	}
	
	
	/**
	 * @param count The number of results that were returned, setting to a negative means unknown.
	 */
	public void endQuery(String statusText, int count) {
		long millisTaken = (new Date().getTime()) - startTick;
		startTick = new Date().getTime();
		display(statusText, false, millisTaken, count);
	}

	
	public void setRemoteStatus(String text) {
		EventQueue.invokeLater(() -> {
			remoteStatusLabel.setText(text);
			remoteStatusLabel.setForeground(null); // default color
		});
	}

	public void setRemoteStatusError(String text) {
		EventQueue.invokeLater(() -> {
			remoteStatusLabel.setText(text);
			remoteStatusLabel.setForeground(java.awt.Color.RED);
		});
	}

	public void setRemoteStatusTooltip(String tooltip) {
		EventQueue.invokeLater(() -> remoteStatusLabel.setToolTipText(tooltip));
	}

	public void clearRemoteStatus() {
		EventQueue.invokeLater(() -> {
			remoteStatusLabel.setText("");
			remoteStatusLabel.setForeground(null);
			remoteStatusLabel.setToolTipText(null);
		});
	}

	/**
	 * Update the appearance of the status bar
	 * @param count The number of results that were returned, setting to a negative means unknown.
	 */
	private void display(final String statusText, final boolean waiting, 
			final long millisTaken, final int count) {
		EventQueue.invokeLater(new Runnable() {
			
			@Override public void run() {
				int c = waiting ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR;
			    setCursor(Cursor.getPredefinedCursor(c));
			    String s = statusText;
			    if(s.length() > 60) {
			    	s = statusText.substring(0, 55) + "...";
			    }
				statusLabel.setText(s);
				pbar.setIndeterminate(waiting);

				String countTxt = "Count = ?";
				if(count >= 0) {
					countTxt = "Count = " + count;
				}
				String timerTxt = "Time = ? ms";
				if(millisTaken >= 0) {
					timerTxt = "Time = " + millisTaken + " ms";
				}
				timingLabel.setText(timerTxt);
				rowCountLabel.setText(countTxt);
			}
		});
	}
	
}
