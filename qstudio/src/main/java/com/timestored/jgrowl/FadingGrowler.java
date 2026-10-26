/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2024 TimeStored
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
package com.timestored.jgrowl;

import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;

/**
 * Growler where {@link Growl}'s start on the bottom right of the parent,
 * move up to the top and fade out after a period. Hovering over an item
 * will cause it and all those after it to cease expiring.
 */
class FadingGrowler {

	private static final Logger LOG = Logger.getLogger(FadingGrowler.class.getName());

	private final Theme theme;

	private final JFrame parentFrame;
	private int lastSeenY;
	private final List<DisplayedItem> displayedItems = new ArrayList<DisplayedItem>();
	private static ExecutorService executor;

	
	/** Record for one message shown */
	private static class DisplayedItem {
		
		private final Growl message;
		private final Window frame;
		/** ratio between 0-1 for life remaining */
		private float lifeLeft;
		
		public DisplayedItem(Window frame, Growl message) {
			this.frame = frame;
			this.message = message;
			this.lifeLeft = 1.0f;
		}
	}
	
	/**
	 * If we havn't already done so start a thread that fades out the shown items.
	 */
	private void startFadingThread() {
		
		// lazy initialisation to prevent uneeded thread creation
		
		if (executor == null) {
			synchronized (this) {
				if(executor == null) {
					executor = Executors.newCachedThreadPool();
					executor.execute(new Runnable() {
						@Override public void run() {
							while (true) {
								try {
									Thread.sleep(theme.getFadeTimerDelay());
									EventQueue.invokeLater(new Runnable() {
										@Override
										public void run() {
											synchronized (displayedItems) {
												updateDisplayItems();
											}

										}
									});
								} catch (InterruptedException ex) {
									LOG.log(Level.SEVERE, null, ex);
								}
							}
						}
					});
					
				}
			}
		}
	}
	
	public FadingGrowler(final JFrame parentFrame, final Theme theme) {
		this.parentFrame = parentFrame;
		lastSeenY = parentFrame.getY();
		this.theme = theme;
			
		// keep messages in front of main parent, but not of all windows
		parentFrame.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				synchronized (displayedItems) {
					for(DisplayedItem d : displayedItems) {
						d.frame.setAlwaysOnTop(false);
					}
				}
			}
			
			@Override
			public void windowGainedFocus(WindowEvent e) {
				synchronized (displayedItems) {
					for(DisplayedItem d : displayedItems) {
						d.frame.setAlwaysOnTop(true);
					}
				}
			}
		});
		
	}
	
	public synchronized void show(String message, String title, Icon icon, boolean sticky, Level logLevel) {
		// start thread that calls fade() ever X ms
		startFadingThread();
		LOG.log(logLevel, title + ": " + message);
		addItem(new Growl(message, title, icon, sticky, logLevel));
	}

	public void show(Level logLevel, JPanel messagePanel, String title, boolean sticky) {
		// start thread that calls fade() ever X ms
		startFadingThread();
		LOG.log(logLevel, title + ": Custom JPanel");
		addItem(new Growl(messagePanel, title, null, sticky, logLevel));
	}

	private void addItem(final Growl message) {
		
		EventQueue.invokeLater(new Runnable() {
			
			@Override public void run() {
				Window frame = theme.getWindow(message, parentFrame);
				synchronized (displayedItems) {
					int top = parentFrame.getY() + (parentFrame.getHeight() - 100);
					if(displayedItems.size() > 0) {
						Window lastFrame = displayedItems.get(displayedItems.size() - 1).frame;
						top = lastFrame.getY() + lastFrame.getHeight()	+ theme.getSpaceBetweenItems();
					}
					frame.setLocation(theme.getLeftRuler(parentFrame), top);
					DisplayedItem di = new DisplayedItem(frame, message);
					frame.addMouseListener(new MouseAdapter() {
						@Override public void mousePressed(MouseEvent e) {
							di.lifeLeft = 0;
							super.mousePressed(e);
						}
					});
					displayedItems.add(di);
				}
			}
		});
	}

	
	/**
	 * Updates the lifeLeft and the appearance of all displayedItems
	 * must be called within a list and swing lock
	 */
	private void updateDisplayItems() {
		int parentTop = parentFrame.getY() +  ((2*parentFrame.getHeight())/3);
		int parentYmove = 0;
		// window moved
		if(parentFrame.getY() != lastSeenY) {
			parentYmove = parentFrame.getY() - lastSeenY;
			lastSeenY = parentFrame.getY();
		}
		
		// cycle through list adjusting alpha / position
		Iterator<DisplayedItem> it = displayedItems.iterator();
		int prevTop = parentTop;
		while (it.hasNext()) {
			final DisplayedItem dispItem = it.next();
			final Window fm = dispItem.frame;
			int y = fm.getY();
			
			Point p = fm.getMousePosition();
			if (p!=null && fm.contains(p) && dispItem.lifeLeft>0) {
				dispItem.lifeLeft = 1.0f;
				setAlpha((dispItem.lifeLeft > 1.0 ? 1.0f : dispItem.lifeLeft), fm);
				break;
			} else {
				// fade
				if(!dispItem.message.isSticky()) {
					int fadeLevel = parentTop + Math.min(theme.getFadeRangeMinimum(), 
							parentFrame.getHeight()/2);
					if(y < fadeLevel) { // don't fade ones far out
						dispItem.lifeLeft -= theme.getFadeRate();
					}
				}
				// move
				if(y > prevTop) {
					y -= theme.getMoveSpeed();
				}
				prevTop = y + theme.getSpaceBetweenItems() + fm.getHeight();
			}
			if (dispItem.lifeLeft > 0.0f) {
				setAlpha((dispItem.lifeLeft > 1.0 ? 1.0f : dispItem.lifeLeft), fm);
				fm.setLocation(theme.getLeftRuler(parentFrame), y + parentYmove);
			} else {
				it.remove();
				fm.setVisible(false);
			}
		}
	}
	

	private static void setAlpha(float alpha, Window win) {
		try {
			// Java 6 compatible method
			// invoke AWTUtilities.setWindowOpacity(win, 0.0f);
			@SuppressWarnings("rawtypes")
			Class awtutil = Class.forName("com.sun.awt.AWTUtilities");
			@SuppressWarnings("unchecked")
			Method setWindowOpaque = awtutil.getMethod("setWindowOpacity", Window.class, float.class);
			setWindowOpaque.invoke(null, win, alpha);
//			win.setOpacity(alpha);
		} catch (Exception ex) {
			if(win instanceof JWindow) {
				((JWindow)win).getRootPane().putClientProperty("Window.alpha", Float.valueOf(alpha));	
			}else if(win instanceof JFrame) {
				((JFrame)win).getRootPane().putClientProperty("Window.alpha", Float.valueOf(alpha));	
			}
		}
	}

}
