package com.timestored.swingxx;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

import javax.swing.JComponent;

/**
 * Allows blocking right clicks from changing tabbed panes.
 */
public class TabbedPaneRightClickBlocker {
	private static final Logger LOG = Logger.getLogger(TabbedPaneRightClickBlocker.class.getName());

	/** wrap the native mouse handler to block popup calls */
	public static void install(JComponent tabbedPane) {
	    MouseListener handler = findUIMouseListener(tabbedPane);
	    if(handler!=null) {
		    tabbedPane.removeMouseListener(handler);
		    tabbedPane.addMouseListener(new MouseListenerWrapper(handler));
	    }
	}

	/** Find the built in handler */
	private static MouseListener findUIMouseListener(JComponent tabbedPane) {
	    MouseListener[] listeners = tabbedPane.getMouseListeners();
	    for (MouseListener l : listeners) {
	        if (l.getClass().getName().contains("$Handler")) {
	            return l;
	        }
	    }
	    return null;
	}

	/**
	 * Passes everything but isPopupTrigger clicks onto delegated listener.
	 */
	private static class MouseListenerWrapper implements MouseListener {

	    private final MouseListener delegate;

	    public MouseListenerWrapper(MouseListener delegate) {
	        this.delegate = delegate;
	    }

	    @Override public void mouseClicked(MouseEvent e) {
	    	if(e.isPopupTrigger()) { return; };
	    	try {
		        delegate.mouseClicked(e);
	    	} catch (NullPointerException npe) {
	    		LOG.fine("NullPointerException from RightClickBlocker delegate" + npe.getLocalizedMessage());
	    		// this was happening but probably ok to ignore.
	    	}
	    }

	    @Override public void mousePressed(MouseEvent e) {
	    	if(e.isPopupTrigger()) { return; };
	    	try {
	    		delegate.mousePressed(e);
	    	} catch (NullPointerException npe) {
	    		LOG.fine("NullPointerException from RightClickBlocker delegate" + npe.getLocalizedMessage());
	    		// this was happening but probably ok to ignore.
	    	}
	    }

	    @Override public void mouseReleased(MouseEvent e) {
	    	if(e.isPopupTrigger()) { return; };
	    	try {
	    		delegate.mouseReleased(e);
	    	} catch (NullPointerException npe) {
	    		LOG.fine("NullPointerException from RightClickBlocker delegate" + npe.getLocalizedMessage());
	    		// this was happening but probably ok to ignore.
	    	}
	    }

	    @Override public void mouseEntered(MouseEvent e) {
	    	if(e.isPopupTrigger()) { return; };
	    	try {
	    		delegate.mouseEntered(e);
	    	} catch (NullPointerException npe) {
	    		LOG.fine("NullPointerException from RightClickBlocker delegate" + npe.getLocalizedMessage());
	    		// this was happening but probably ok to ignore.
	    	}
	    }

	    @Override public void mouseExited(MouseEvent e) {
	    	if(e.isPopupTrigger()) { return; };
	    	try {
		        delegate.mouseExited(e);
	    	} catch (NullPointerException npe) {
	    		LOG.fine("NullPointerException from RightClickBlocker delegate" + npe.getLocalizedMessage());
	    		// this was happening but probably ok to ignore.
	    	}
	    }

	}
}
