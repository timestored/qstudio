package com.timestored.swingxx;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import bibliothek.gui.DockFrontend;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.event.DockFrontendAdapter;

/**
 * MenuItem that allows toggling whether a dockable is visible. 
 */
public class ToggleDockableMenuItem extends JCheckBoxMenuItem {
	
	private static final long serialVersionUID = 1L;

	public ToggleDockableMenuItem(final Dockable observed,
			final DockFrontend frontend, final String name) {
		
		super(observed.getTitleText(), observed.getTitleIcon());
		setName(name);

		/*
		 * We add a DockFrontendListener to "frontend" to be informed whenever a
		 * Dockable is opened or closed (shown and hidden in the terminology of
		 * DockFrontend)
		 */
		frontend.addFrontendListener(new DockFrontendAdapter() {
			@Override public void shown(DockFrontend f, Dockable dockable) {
				if (dockable == observed) {
					ToggleDockableMenuItem.this.setSelected(true);
				}
			}

			@Override public void hidden(DockFrontend f, Dockable dockable) {
				if (dockable == observed) {
					ToggleDockableMenuItem.this.setSelected(false);
				}
			}
		});
		/*
		 * And an ActionListener added to "item" will tell us when the user
		 * clicks on the menu item.
		 */
		addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				if (isSelected()) {
					frontend.show(observed);
				} else {
					frontend.hide(observed);
				}
			}
		});

		/* Be sure the initial state of "item" is the correct one */
		setSelected(frontend.isShown(observed));
	}
}