package com.timestored.sqldash.chart;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.StringValues;

import lombok.Getter;

public class ColumnFormatManager {

    public enum ColType { NUMBER, ARRAY, DATE, TIME, STRING, BOOLEAN, UNKNOWN }

    @Getter private final Map<String,String> formats = new HashMap<>();
    private final JXTable table;
    private final boolean negativeShownRed;

    public ColumnFormatManager(JXTable table, boolean negativeShownRed) {
        this.table = table;
        this.negativeShownRed = negativeShownRed;
    }
    
    private void resetRenderer(String colName) {
        TableModel m = table.getModel();
        int mc = m.getColumnCount();
        for(int i=0; i<mc; i++) {
            String name = m.getColumnName(i);
            if(!colName.equals(name)) {
                continue;
            }
            int vc = table.convertColumnIndexToView(i);
            if(vc < 0) {
                continue;
            }
            table.getColumnExt(vc).setCellRenderer(
                RenderProvider.getRendererForColumnName(name, StringValues.TO_STRING, negativeShownRed)
            );
        }
        table.repaint();
    }
    
    private void setFormat(String colName, String fmt) {
        if(fmt == null || fmt.trim().isEmpty()) {
            formats.remove(colName);
            resetRenderer(colName);
        } else {
            formats.put(colName, fmt.trim());
            apply();
        }
        
        try {
	        TableModel m = table.getModel();
	        int mc = m.getColumnCount();
	        for(int i=0; i<mc; i++) {
				if(colName.equals(m.getColumnName(i))) {
				    int vc = table.getColumnModel().getColumnIndex(colName);  // <-- THE FIX
					if(vc >= 0) {
						autoSizeSingleColumn(vc);
					}
					break;
				}
			}
        } catch(Exception ex) {
			// ignore - kept getting index errors here sometimes
		}
	}

    public ColType detectType(JXTable t, int modelCol) {
        TableModel m = t.getModel();
        int rows = m.getRowCount();

        for(int r=0;r<rows;r++) {
            Object v = m.getValueAt(r, modelCol);
            if(v == null) { continue; }

            if(v instanceof Number) { return ColType.NUMBER; }
            if(v instanceof Boolean) { return ColType.BOOLEAN; }
            if(v instanceof java.util.Date) { return ColType.DATE; }
            if(v instanceof java.time.temporal.TemporalAccessor) { return ColType.DATE; }

            if(v instanceof List || v.getClass().isArray()) {
                return ColType.ARRAY;
            }

            String s = v.toString().trim();

            if(s.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) { return ColType.DATE; }
            if(s.matches("\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?")) { return ColType.TIME; }

            return ColType.STRING;
        }

        return ColType.UNKNOWN;
    }

    public JMenu  buildFlatMenu(String colName, ColType type) {
        JMenu m = new JMenu("Format…");

        switch(type) {
            case NUMBER:
                addNumberItems(m, colName);
                break;
            case ARRAY:
                addArrayItems(m, colName);
                break;
            case DATE:
            case TIME:
                addDateTimeItems(m, colName);
                break;
            case BOOLEAN:
                addBooleanItems(m, colName);
                break;
            case STRING:
            default:
                addStringItems(m, colName);
                break;
        }

        m.addSeparator();
        m.add(item(colName, "Clear Formatting", ""));
        return m;
    }

    private JMenuItem item(String col, String name, String fmt) {
        JMenuItem mi = new JMenuItem(name);
        mi.addActionListener(e -> setFormat(col, fmt));
        return mi;
    }
    
    private void addBooleanItems(JMenu m, String col) {
        m.add(item(col, "Tick/Cross", "TICK"));
    }
    
    private void addNumberItems(JMenu m, String col) {
        m.add(item(col, "Shade by Value", "SHADE"));
        for(int dp=0; dp<=6; dp++) {
            m.add(item(col, dp + " Decimal Places", "NUMBER" + dp));
        }
        m.add(item(col, "Percent (0dp)", "PERCENT0"));
        m.add(item(col, "Percent (2dp)", "PERCENT2"));
        m.add(item(col, "Data Bar", "DATABAR"));
        m.add(item(col, "Currency USD", "CURUSD"));
        m.add(item(col, "Currency EUR", "CUREUR"));
        m.add(item(col, "Currency GBP", "CURGBP"));
    }

    private void addArrayItems(JMenu m, String col) {
        m.add(item(col, "Spark Line", "SPARKLINE"));
        m.add(item(col, "Spark Bar", "SPARKBAR"));
        m.add(item(col, "Spark Discrete", "SPARKDISCRETE"));
        m.add(item(col, "Spark Bullet", "SPARKBULLET"));
        m.add(item(col, "Spark Pie", "SPARKPIE"));
        m.add(item(col, "Spark Boxplot", "SPARKBOXPLOT"));
    }

    private void addDateTimeItems(JMenu m, String col) {
        m.add(item(col, "YYYY-MM-DD", "DATE"));
        m.add(item(col, "DD/MM/YYYY", "DATEDD"));
        m.add(item(col, "MM/DD/YYYY", "DATEMM"));
        m.add(item(col, "17 Feb 2023", "DATEMONTH"));
        m.add(item(col, "17-Feb-23", "DATEMON"));
        m.add(item(col, "13:01", "TIMEMM"));
        m.add(item(col, "13:01:59", "TIMESS"));
        m.add(item(col, "13:01:59.123", "TIME"));
    }

    private void addStringItems(JMenu m, String col) {
        m.add(item(col, "Tags", "TAG"));
        m.add(item(col, "Icon / Multi-icon", "ICON"));
    }
    private void apply() {
        TableModel m = table.getModel();
        int mc = m.getColumnCount();

        for(int i=0; i<mc; i++) {
            String name = m.getColumnName(i);
            String f = formats.get(name);
            int vc = table.convertColumnIndexToView(i);
            if(vc < 0) {
                continue;
            }

            if(f == null) {
                continue;
            }

            // Apply new formatted renderer
            String decorated = name + "_SD_" + f;
            table.getColumnExt(vc).setCellRenderer(
                RenderProvider.getRendererForColumnName(
                    decorated, 
                    StringValues.TO_STRING, 
                    true
                )
            );

            table.packColumn(vc, 4);
        }

        table.repaint();
    }


    private void autoSizeSingleColumn(int viewCol) {

        int viewCount = table.getColumnModel().getColumnCount();
        if(viewCol < 0 || viewCol >= viewCount) {
            return;
        }
        String headerName = table.getColumnName(viewCol);
        int max = 0;
        int margin = 6;
        TableModel model = table.getModel();
        int rows = table.getRowCount();

        // header width
        TableCellRenderer hr = table.getTableHeader().getDefaultRenderer();
        Component hc = hr.getTableCellRendererComponent(
                table,
                headerName,
                false, false, -1, viewCol
        );
        max = Math.max(max, hc.getPreferredSize().width);

        // cell widths
        for(int row = 0; row < rows; row++) {
            TableCellRenderer r = table.getCellRenderer(row, viewCol);
            Component c = table.prepareRenderer(r, row, viewCol);
            max = Math.max(max, c.getPreferredSize().width);
        }

        table.getColumnExt(viewCol).setPreferredWidth(max + margin);
    }


}
