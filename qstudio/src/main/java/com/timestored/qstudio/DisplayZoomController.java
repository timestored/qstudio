/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2026 TimeStored
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
package com.timestored.qstudio;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.timestored.swingxx.AAction;

/**
 * Handles Ctrl+Plus / Ctrl+Minus / Ctrl+0 / Ctrl+MouseWheel shortcuts to adjust
 * either the code font size (when over a code editor or console panel) or the
 * application UI scale (anywhere else). A single in-place HUD overlay anchored
 * near the cursor (snapped to a grid to reduce jitter) shows the current values
 * and auto-hides shortly after the last change.
 */
final class DisplayZoomController {

	private static final int FONT_MIN = 1;
	private static final int FONT_MAX = 50;
	private static final int FONT_STEP = 1;

	private static final int SCALE_MIN = 25;
	private static final int SCALE_MAX = 400;
	private static final int SCALE_STEP = 10;

	private static final int HIDE_AFTER_MS = 1500;
	private static final int OVERLAY_MARGIN = 16;

	// Cursor drift within a SNAP_PX cell does not reposition the overlay.
	private static final int SNAP_PX = 60;
	private static final int CURSOR_OFFSET_X = 18;
	private static final int CURSOR_OFFSET_Y = 18;

	private enum Target { FONT, SCALE }

	private final JFrame frame;
	private final JLabel overlay;
	private final Timer hideTimer;
	private Target lastChanged;
	// Raw cursor position in layered-pane coords at the moment of the last change.
	// Null → fall back to the top-right corner.
	private Point lastAnchor;

	DisplayZoomController(JFrame frame) {
		this.frame = frame;
		this.overlay = buildOverlay();
		this.hideTimer = new Timer(HIDE_AFTER_MS, e -> overlay.setVisible(false));
		this.hideTimer.setRepeats(false);
		installOverlay();
		registerShortcuts();
		registerWheelListener();
	}

	private static JLabel buildOverlay() {
		// Click-through: contains() always returns false so mouse events pass to components below.
		JLabel lbl = new JLabel() {
			private static final long serialVersionUID = 1L;
			@Override public boolean contains(int x, int y) { return false; }
		};
		lbl.setOpaque(true);
		lbl.setBackground(new Color(40, 40, 40));
		lbl.setForeground(Color.WHITE);
		lbl.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(90, 90, 90)),
				BorderFactory.createEmptyBorder(8, 12, 8, 12)));
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));
		lbl.setVisible(false);
		return lbl;
	}

	private void installOverlay() {
		JLayeredPane lp = frame.getLayeredPane();
		lp.add(overlay, JLayeredPane.POPUP_LAYER);
		lp.addComponentListener(new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent e) {
				if (overlay.isVisible()) reposition();
			}
		});
	}

	private void reposition() {
		JLayeredPane lp = frame.getLayeredPane();
		Dimension pref = overlay.getPreferredSize();
		int w = pref.width;
		int h = pref.height;
		int lpW = lp.getWidth();
		int lpH = lp.getHeight();

		int x, y;
		if (lastAnchor != null && lastAnchor.x >= 0 && lastAnchor.y >= 0
				&& lastAnchor.x <= lpW && lastAnchor.y <= lpH) {
			int ax = Math.floorDiv(lastAnchor.x, SNAP_PX) * SNAP_PX;
			int ay = Math.floorDiv(lastAnchor.y, SNAP_PX) * SNAP_PX;
			// Prefer lower-right of the anchor; flip to the opposite side if we'd clip.
			x = ax + CURSOR_OFFSET_X;
			if (x + w > lpW - OVERLAY_MARGIN) {
				x = ax - CURSOR_OFFSET_X - w;
			}
			y = ay + CURSOR_OFFSET_Y;
			if (y + h > lpH - OVERLAY_MARGIN) {
				y = ay - CURSOR_OFFSET_Y - h;
			}
		} else {
			x = lpW - w - OVERLAY_MARGIN;
			y = OVERLAY_MARGIN;
		}

		int maxX = Math.max(OVERLAY_MARGIN, lpW - w - OVERLAY_MARGIN);
		int maxY = Math.max(OVERLAY_MARGIN, lpH - h - OVERLAY_MARGIN);
		x = clamp(x, OVERLAY_MARGIN, maxX);
		y = clamp(y, OVERLAY_MARGIN, maxY);
		overlay.setBounds(x, y, w, h);
	}

	private void registerShortcuts() {
		int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		ActionMap am = frame.getRootPane().getActionMap();
		InputMap im = frame.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		AbstractAction zoomIn    = new AAction(ae -> { step(+1, focusTarget(), cursorInLayeredPane()); });
		AbstractAction zoomOut   = new AAction(ae -> { step(-1, focusTarget(), cursorInLayeredPane()); });
		AbstractAction zoomReset = new AAction(ae -> { reset(focusTarget(), cursorInLayeredPane()); });

		am.put("displayZoomIn", zoomIn);
		am.put("displayZoomOut", zoomOut);
		am.put("displayZoomReset", zoomReset);

		// Zoom in: cover '=' (shift gives '+'), explicit '+' keys, and numpad add.
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, mask), "displayZoomIn");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, mask | KeyEvent.SHIFT_DOWN_MASK), "displayZoomIn");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, mask), "displayZoomIn");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, mask), "displayZoomIn");

		// Zoom out.
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, mask), "displayZoomOut");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, mask), "displayZoomOut");

		// Reset.
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, mask), "displayZoomReset");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, mask), "displayZoomReset");
	}

	private void registerWheelListener() {
		final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			@Override public void eventDispatched(AWTEvent event) {
				if (!(event instanceof MouseWheelEvent)) {
					return;
				}
				MouseWheelEvent mwe = (MouseWheelEvent) event;
				if ((mwe.getModifiers() & menuMask) == 0) {
					return;
				}
				Component src = mwe.getComponent();
				if (src == null || SwingUtilities.getWindowAncestor(src) != frame) {
					return;
				}
				int rot = mwe.getWheelRotation();
				if (rot == 0) {
					return;
				}
				Point anchor = SwingUtilities.convertPoint(src, mwe.getPoint(), frame.getLayeredPane());
				step(rot < 0 ? +1 : -1, mouseTarget(mwe), anchor);
				mwe.consume();
			}
		}, AWTEvent.MOUSE_WHEEL_EVENT_MASK);
	}

	private Point cursorInLayeredPane() {
		JLayeredPane lp = frame.getLayeredPane();
		if (!lp.isShowing()) return null;
		PointerInfo pi = MouseInfo.getPointerInfo();
		if (pi == null) return null;
		Point p = pi.getLocation();
		SwingUtilities.convertPointFromScreen(p, lp);
		return p;
	}

	private Target focusTarget() {
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return isCodePanel(c) ? Target.FONT : Target.SCALE;
	}

	private Target mouseTarget(MouseWheelEvent mwe) {
		Component src = mwe.getComponent();
		Point pInFrame = SwingUtilities.convertPoint(src, mwe.getPoint(), frame);
		Component deepest = SwingUtilities.getDeepestComponentAt(frame, pInFrame.x, pInFrame.y);
		return isCodePanel(deepest) ? Target.FONT : Target.SCALE;
	}

	/**
	 * A component is considered a "code panel" when it sits inside the
	 * {@link ServerDocumentPanel} (the tabbed code editor area) or the
	 * {@link ConsolePanel} — both of which render text using the code font.
	 */
	private static boolean isCodePanel(Component c) {
		Component cur = c;
		while (cur != null) {
			if (cur instanceof ServerDocumentPanel || cur instanceof ConsolePanel) {
				return true;
			}
			cur = cur.getParent();
		}
		return false;
	}

	private void step(int direction, Target target, Point anchor) {
		MyPreferences p = MyPreferences.INSTANCE;
		if (target == Target.FONT) {
			int current = p.getCodeFontSize();
			int next = clamp(current + direction * FONT_STEP, FONT_MIN, FONT_MAX);
			if (next != current) {
				p.setCodeFontSize(next);
				p.notifyListeners();
			}
		} else {
			int current = p.getUIScale();
			int next = clamp(current + direction * SCALE_STEP, SCALE_MIN, SCALE_MAX);
			if (next != current) {
				p.setUIScale(next);
				p.notifyListeners();
			}
		}
		lastChanged = target;
		lastAnchor = anchor;
		updateOverlay();
	}

	private void reset(Target target, Point anchor) {
		MyPreferences p = MyPreferences.INSTANCE;
		if (target == Target.FONT) {
			p.resetCodeFontSize();
		} else {
			p.resetUIScale();
		}
		p.notifyListeners();
		lastChanged = target;
		lastAnchor = anchor;
		updateOverlay();
	}

	private static int clamp(int v, int lo, int hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	private void updateOverlay() {
		MyPreferences p = MyPreferences.INSTANCE;
		int font = p.getCodeFontSize();
		int scale = p.getUIScale();
		String fontLine = "Font size: " + font + "pt" + capSuffix(font, FONT_MIN, FONT_MAX);
		String scaleLine = "UI scale: " + scale + "%" + capSuffix(scale, SCALE_MIN, SCALE_MAX);
		String arrow = " &larr;";
		if (lastChanged == Target.FONT) {
			fontLine = fontLine + arrow;
		} else if (lastChanged == Target.SCALE) {
			scaleLine = scaleLine + arrow;
		}
		overlay.setText("<html>" + fontLine + "<br>" + scaleLine + "</html>");
		overlay.setVisible(true);
		reposition();
		hideTimer.restart();
	}

	private static String capSuffix(int value, int min, int max) {
		if (value <= min) return " (min)";
		if (value >= max) return " (max)";
		return "";
	}
}
