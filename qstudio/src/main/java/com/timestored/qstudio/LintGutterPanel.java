package com.timestored.qstudio;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

import com.timestored.qdoc.parser.ParseError;

/**
 * Gutter panel that draws colored circles next to lines with lint issues.
 */
class LintGutterPanel extends JPanel {

    private final JEditorPane editor;
    private final Map<Integer, Color> lineColors = new TreeMap<>();
    private final Map<Integer, String> lineTooltips = new TreeMap<>();

    LintGutterPanel(JEditorPane editor) {
        this.editor = editor;
        setPreferredSize(new Dimension(14, 0));
        boolean dark = isDark();
        setBackground(dark ? new Color(0x33, 0x33, 0x33) : new Color(0xEE, 0xEE, 0xFF));
        setToolTipText("");
    }

    private boolean isDark() {
        Color bg = editor.getBackground();
        return bg != null && (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3 < 128;
    }

    void setProblems(java.util.List<ParseError> problems) {
        lineColors.clear();
        lineTooltips.clear();
        boolean dark = isDark();
        setBackground(dark ? new Color(0x33, 0x33, 0x33) : new Color(0xEE, 0xEE, 0xFF));
        for (ParseError p : problems) {
            if (p.line < 1) continue;
            String sev = p.getSeverity();
            Color c;
            switch (sev) {
                case "ERROR": c = dark ? new Color(255, 80, 80) : Color.RED; break;
                case "WARNING": c = dark ? new Color(255, 170, 50) : new Color(200, 120, 0); break;
                default: c = dark ? new Color(80, 170, 255) : new Color(0, 120, 200); break;
            }
            Color existing = lineColors.get(p.line);
            if (existing == null || severityRank(sev) > severityRank(severityOf(existing))) {
                lineColors.put(p.line, c);
            }
            String tip = lineTooltips.get(p.line);
            lineTooltips.put(p.line, tip == null ? p.message : tip + "\n" + p.message);
        }
        repaint();
    }

    void clear() {
        lineColors.clear();
        lineTooltips.clear();
        repaint();
    }

    private static int severityRank(String sev) {
        if ("ERROR".equals(sev)) return 3;
        if ("WARNING".equals(sev)) return 2;
        return 1;
    }

    private static String severityOf(Color c) {
        if (Color.RED.equals(c)) return "ERROR";
        if (c.getRed() == 200 && c.getGreen() == 120) return "WARNING";
        return "INFO";
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int line = getLineAtY(e.getY());
        String tip = lineTooltips.get(line);
        if (tip != null && tip.contains("\n")) {
            return "<html>" + tip.replace("\n", "<br>") + "</html>";
        }
        return tip;
    }

    private int getLineAtY(int y) {
        try {
            int offset = editor.viewToModel(new Point(0, y));
            Element root = editor.getDocument().getDefaultRootElement();
            return root.getElementIndex(offset) + 1;
        } catch (Exception e) { return -1; }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Element root = editor.getDocument().getDefaultRootElement();
        for (Map.Entry<Integer, Color> entry : lineColors.entrySet()) {
            int line = entry.getKey();
            if (line < 1 || line > root.getElementCount()) continue;
            try {
                Element elem = root.getElement(line - 1);
                Rectangle r = editor.modelToView(elem.getStartOffset());
                if (r == null) continue;
                g2.setColor(entry.getValue());
                int cy = r.y + r.height / 2;
                g2.fillOval(2, cy - 4, 9, 9);
                g2.setColor(entry.getValue().darker());
                g2.drawOval(2, cy - 4, 9, 9);
            } catch (BadLocationException e) { /* skip */ }
        }
    }
}
