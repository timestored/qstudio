package com.timestored.sqldash.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.table.TableColumnExt;

import com.timestored.StringUtils;
import com.timestored.babeldb.DBHelper;
import com.timestored.babeldb.DBHelper.ColumnInfo;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.QStudioFrame;
import com.timestored.qstudio.kdb.DataComparator;
import com.timestored.qstudio.kdb.KdbHelper;
import com.timestored.sqldash.chart.RenderProvider.*;
import com.timestored.swingxx.SaveTableMouseAdapter;
import com.timestored.theme.Theme;

import kx.jdbc.ExtendedResultSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class TableFactory {

	private static final Logger LOG = Logger.getLogger(TableFactory.class.getName());

	private static final KdbStringValuer KDB_STRING_VALER = new KdbStringValuer();
	public static final Color KEY_COL_COLOR = new Color(222,188,255);
	public static final Color KEY_COL_HEADER_COLOR = new Color(111,88,133);
	private static final Highlighter KEY_COL_HIGHLIGHTER = new ColorHighlighter(KEY_COL_COLOR, Color.BLACK);

	/** Once a column is over this width, restrict it to cutoff some text **/
	private static final int MAX_COL_WIDTH = 700;	

	private static final Comparator MANY_COMPARATOR = new DataComparator();

	public static JScrollPane getTable(final TableModel tableModel, ColumnInfo[] columnInfos, boolean negativeShownRed) {
		final JXTable table = Theme.getStripedTable(tableModel);
		table.setCellSelectionEnabled(true);
		int keyColCount = 0;
		if(tableModel instanceof EnrichedTableModel) {
			keyColCount = ((EnrichedTableModel) tableModel).getKeyedColumnCount(); 
		}		
		
		for(int i=0; i<keyColCount; i++) {
			table.getColumnExt(i).setHighlighters(KEY_COL_HIGHLIGHTER );
			table.getColumnExt(i).setHeaderRenderer(new DefaultTableRenderer() {
				@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column) {
					Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					setBackground(KEY_COL_HEADER_COLOR);
			        return cell;
				}
			});
		}
		for (int i = 0; i < table.getColumnCount(); i++) {
		    TableColumnExt col = table.getColumnExt(i);
		    // MUST use model column name because title was cleaned
		    String name = table.getModel().getColumnName(table.convertColumnIndexToModel(i)).toUpperCase();

		    TableCellRenderer renderer = RenderProvider.getRendererForColumnName(name, KDB_STRING_VALER, negativeShownRed);
		    if(renderer != null) {
		        col.setCellRenderer(renderer);
		    }


		}

		RenderProvider.applySmartDisplayColorColumns(table);
		
		for (int ci = 0; ci < table.getColumnCount(); ci++) {
		    TableColumnExt tce = table.getColumnExt(ci);
		    // Remove any _SD_XXXX suffix
		    String displayName = tce.getTitle().replaceAll("(?i)_sd_[a-z0-9]+$", "");
		    tce.setTitle(displayName);
		}

		
		SaveTableMouseAdapter saveTableMouseAdapter = setPresentation(table, negativeShownRed);
		for(int ci=0; ci<table.getColumnCount(); ci++) {
			TableColumnExt tce = table.getColumnExt(ci);
			if(columnInfos != null && ci < columnInfos.length) {
				String s = StringUtils.abbreviateMultiline(columnInfos[ci].toString());
				tce.setToolTipText(s);
			}
			tce.setComparator(MANY_COMPARATOR);
			if(tce.getPreferredWidth() > MAX_COL_WIDTH) {
				tce.setPreferredWidth(MAX_COL_WIDTH);
			}
		}
		
		JScrollPane scrollPane = new JScrollPane(table,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		JXTable rowTable = getRowTable(tableModel, table);
		rowTable.addMouseListener(saveTableMouseAdapter);
		
		scrollPane.setRowHeaderView(rowTable);
		scrollPane.setColumnHeaderView(table.getTableHeader());
		scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
		return scrollPane;
	}

	private static JXTable getRowTable(final TableModel tableModel, final JXTable table) {
		// Add a frozen first column that contains the row number
		TableModel rowTableModel = new AbstractTableModel() {
			private static final long serialVersionUID = 1L;
			@Override public Object getValueAt(int rowIndex, int columnIndex) {
				return rowIndex;
			}
			@Override public int getRowCount() { return tableModel.getRowCount(); }
			@Override public int getColumnCount() { return 1; }
			@Override public String getColumnName(int column) {return "";	}
			@Override public Class<?> getColumnClass(int columnIndex) {return Number.class;}
		};
		JXTable rowTable = new JXTable(rowTableModel);
		rowTable.getColumnExt(0).setComparator(MANY_COMPARATOR);
		rowTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		rowTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
			
			@Override public void sorterChanged(RowSorterEvent e) {
				RowSorter v = e.getSource();
				List l = v.getSortKeys();
				table.getRowSorter().setSortKeys(l);
				System.out.println(Arrays.toString(l.toArray()));
				System.out.println(l.toString());
			}
		});
		rowTable.packAll();
		rowTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		MouseAdapter mouseAdapter = new ExcelStyleSelectorMouseListener(rowTable, table);
		rowTable.addMouseMotionListener(mouseAdapter);
		rowTable.addMouseListener(mouseAdapter);

		rowTable.getColumnExt(0).setCellRenderer(new DefaultTableRenderer() {
			private static final long serialVersionUID = 1L;
			@Override public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel l = new JLabel("" + value.toString());
				l.addMouseListener(new MouseAdapter() {
					@Override public void mouseClicked(MouseEvent e) {
						System.out.println("" + row);
						table.setRowSelectionInterval(row, row+1);
					}
					
					@Override
					public void mouseEntered(MouseEvent e) {
						System.out.println("" + row);
					}
				});
				l.setBorder(BorderFactory.createRaisedBevelBorder());
				return l; 
			}
		});
		return rowTable;
	}
	
	@RequiredArgsConstructor
	private static class ExcelStyleSelectorMouseListener extends MouseAdapter {

		int mouseRowDown = -1;
		int mouseRowUp = -1;
		private final JXTable rowTable;
		private final JXTable table;
		
		@Override public void mousePressed(MouseEvent e) {
			if(mouseRowDown == -1 && e.getButton() == MouseEvent.BUTTON1) {
				int rowDown = rowTable.rowAtPoint(e.getPoint());
				if(rowDown != mouseRowDown) {
					mouseRowDown = rowDown;
					System.out.println("mouseDragged mouseRowDown = " + mouseRowDown);
				}
			}
			super.mousePressed(e);
		}
		
		@Override public void mouseDragged(MouseEvent e) {
			 if(mouseRowDown != -1) {
				int mouseUp = rowTable.rowAtPoint(e.getPoint());
				if(table.getSelectionMode() != ListSelectionModel.SINGLE_INTERVAL_SELECTION) {
					table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				}
				if(mouseRowUp != mouseUp) {
					mouseRowUp = mouseUp;
					int rowCount = table.getRowCount();
					if(mouseRowDown >= 0 && mouseRowDown < rowCount && mouseRowUp >= 0 && mouseRowUp < rowCount) {
			        	int lowRow = Math.min(mouseRowDown, mouseRowUp);
			        	int hiRow = Math.max(mouseRowDown, mouseRowUp);
						table.setColumnSelectionInterval(0, table.getColumnCount() - 1);
						table.setRowSelectionInterval(lowRow, hiRow);
						LOG.fine("lowRow = " + lowRow + " hiRow =" + hiRow);
					}
				}
			}
			super.mouseDragged(e);
		}
		
		@Override public void mouseReleased(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1) {
				mouseRowDown = -1;
				mouseRowUp = -1;
			}
			super.mouseReleased(e);
		}
		
		@Override public void mouseClicked(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1) {
	        	if(table.getSelectionMode() != ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
	        		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	        		table.clearSelection();
	        	}
		        int row = rowTable.rowAtPoint(e.getPoint());
		        // If shift held and already row selected, highlight between the rows.
		        if(row >= 0) {
					table.setColumnSelectionInterval(0, table.getColumnCount() - 1);
		        	if(e.isShiftDown() && table.getSelectedRow() >= 0) {
			        	int lowRow = Math.min(row, table.getSelectedRow());
			        	int hiRow = Math.max(row, table.getSelectedRow());
						table.setRowSelectionInterval(lowRow, hiRow);	
			        } else if(e.isControlDown()) {
			        	if(table.getSelectedRowCount() > 0) {
			        		table.addRowSelectionInterval(row, row);
			        	} else {
			        		table.setRowSelectionInterval(row, row);
			        	}
			        } else {
						table.setRowSelectionInterval(row, row);
			        }
		        }
			}
			super.mouseClicked(e);
		}
	}
	 
	/** Set how values are rendered and pack the columns */
	public static SaveTableMouseAdapter setPresentation(final JXTable table, boolean negativeShownRed) {
		
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		SaveTableMouseAdapter rightClickListener = new SaveTableMouseAdapter(table, Theme.CIcon.CSV.get(), KDB_STRING_VALER, negativeShownRed);
		
		table.addMouseListener(rightClickListener);
		table.addMouseListener(new MouseAdapter() {			
		    public void mousePressed(MouseEvent mouseEvent) {
		    	if(mouseEvent.getButton() == MouseEvent.BUTTON1) {
			        if(table.getSelectionMode() != ListSelectionModel.SINGLE_INTERVAL_SELECTION) {
			        	table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			        }
			        Point point = mouseEvent.getPoint();
			        int row = table.rowAtPoint(point);
			        int col = table.columnAtPoint(point);
			        if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1 && row != -1) {
			            // so we know there is a doubleclick
			            // a row has been selected before
			            // the click was inside the JTable filled with data
	
			            // the row number is the visual row number
			            // when filtering or sorting it is not the model's row number
			            // this line takes care of that
			            int modelRow = table.convertRowIndexToModel(row);
			            int modelCol = table.convertColumnIndexToModel(col);
			            TableModel model = table.getModel();
			            Object val = model.getValueAt(modelRow, modelCol);
			            System.out.println(val);
			            if(model instanceof EnrichedTableModel) {
			            	ExtendedResultSet nestedRS = ((EnrichedTableModel)model).getNestedResultSetAt(modelRow, modelCol);
							if(nestedRS != null) {
			            		try {
									Component nestedTable = getTable(nestedRS, Integer.MAX_VALUE, negativeShownRed);
									String title = nestedRS.getName().length() == 0 ? "nested" : nestedRS.getName();
						            Window w = SwingUtilities.getWindowAncestor(table);
									QStudioFrame.showPopup(w, nestedTable, title, Theme.CIcon.TABLE_ROW_DELETE.get16().getImage());
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			            	}
			            }
			            // your valueChanged overridden method
			        }
		    	}
		    }
		});
		
		// Handle single click on hyperlinks
		table.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
					Point point = e.getPoint();
					int row = table.rowAtPoint(point);
					int col = table.columnAtPoint(point);
					if(row >= 0 && col >= 0) {
						int modelRow = table.convertRowIndexToModel(row);
						int modelCol = table.convertColumnIndexToModel(col);
						Object val = table.getModel().getValueAt(modelRow, modelCol);
						
						// Prefer markdown links
						MarkdownLink md = RenderProvider.extractMarkdownLink(val);
						if (md != null) {
						    HtmlUtils.browse(md.url);
						    return;
						}

						// Fallback: raw URL detection
						String url = RenderProvider.extractUrl(val);
						if (url != null) {
						    HtmlUtils.browse(url);
						}
					}
				}
			}
		});
		
		// Change cursor to hand when hovering over hyperlink cells
		table.addMouseMotionListener(new MouseMotionAdapter() {
			private int lastRow = -1;
			private int lastCol = -1;
			private boolean lastWasHyperlink = false;
			
			@Override public void mouseMoved(MouseEvent e) {
				Point point = e.getPoint();
				int row = table.rowAtPoint(point);
				int col = table.columnAtPoint(point);
				if(row >= 0 && col >= 0) {
					// Only recalculate if cell changed
					if(row != lastRow || col != lastCol) {
						lastRow = row;
						lastCol = col;
						int modelRow = table.convertRowIndexToModel(row);
						int modelCol = table.convertColumnIndexToModel(col);
						Object val = table.getModel().getValueAt(modelRow, modelCol);
						lastWasHyperlink = RenderProvider.extractMarkdownLink(val) != null || RenderProvider.extractUrl(val) != null;
					}
					if(lastWasHyperlink) {
						table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					} else {
						table.setCursor(Cursor.getDefaultCursor());
					}
				} else {
					lastRow = -1;
					lastCol = -1;
					lastWasHyperlink = false;
					table.setCursor(Cursor.getDefaultCursor());
				}
			}
		});

//		DefaultTableRenderer defaultTabRenderer = new DefaultTableRenderer(KDB_STRING_VALER, JLabel.RIGHT);
		DefaultTableRenderer defaultTabRenderer = new ConditionalNumberRenderer(KDB_STRING_VALER, JLabel.RIGHT, negativeShownRed);
		table.setDefaultRenderer(Object.class, defaultTabRenderer);
		table.setDefaultRenderer(Number.class, defaultTabRenderer);

		table.packAll();
		table.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
	    return rightClickListener;
	}
	

	public static DefaultTableModel buildTableModel(ResultSet rs, int maxRowsShown) throws SQLException {

	    ResultSetMetaData metaData = rs.getMetaData();

	    // names of columns
	    Vector<String> columnNames = new Vector<String>();
	    int columnCount = metaData.getColumnCount();
	    for (int column = 1; column <= columnCount; column++) {
	        columnNames.add(metaData.getColumnName(column));
	    }
	    
	    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	    for(int row = 0;rs.next() && row < maxRowsShown;row++) {
	        Vector<Object> vector = new Vector<Object>();
	        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
        		vector.add(rs.getObject(columnIndex));
	        }
	        data.add(vector);
	    }
	    if(rs instanceof ExtendedResultSet) {
	    	return new EnrichedTableModel(data, columnNames, (ExtendedResultSet)rs);	
	    }
	    return new DefaultTableModel(data, columnNames);

	}

	public static class TransposedTableModel extends EnrichedTableModel {
		private static final long serialVersionUID = 1L;

		public TransposedTableModel(JXTable table, int[] rows) {
	        super(transpose(table, rows), toVector(rows), 1);
		}
		
		private static Vector<String> toVector(int[] rows) {
			Vector<String> result = new Vector<>(rows.length);
			result.add("ColumnName");
			for(int r : rows) {
				result.add(""+r);
			}
			return result;
		}

		private static Vector<Vector<Object>> transpose(JXTable table, int[] rows) {
		    Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		    for(int r=0; r<table.getColumnCount(); r++) {
		        Vector<Object> vector = new Vector<Object>();
		        vector.add(table.getColumnName(r));
			    for(int c=0; c<rows.length; c++) {
			    	vector.add(table.getValueAt(rows[c], r));
			    }
		        data.add(vector);
		    }
	        return data;
		}
	}
	
	public static class EnrichedTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;
		private final ExtendedResultSet rs;
		@Getter private final int keyedColumnCount;

		public EnrichedTableModel(Vector<? extends Vector> dataVector, Vector<?> columnIdentifiers, int keyedColumnCount) {
			super(dataVector, columnIdentifiers);
			this.rs = null;
			this.keyedColumnCount = keyedColumnCount;
		}
		
		public EnrichedTableModel(Vector<? extends Vector> dataVector, Vector<?> columnIdentifiers, ExtendedResultSet rs) {
			super(dataVector, columnIdentifiers);
			this.rs = rs;
			this.keyedColumnCount = rs == null ? 0 : rs.getKeyedColumnCount();
		}
		
		public ExtendedResultSet getNestedResultSetAt(int modelRow, int modelCol) {
			return rs==null ? null : rs.getNestedResultSetAt(modelRow,modelCol);
		}

	}
	
	public static Component getTable(ResultSet rs, int maxRowsShown, boolean negativeShownRed) throws SQLException {
		if(rs != null) {
			DefaultTableModel tableModel = buildTableModel(rs, maxRowsShown);
			ColumnInfo[] columnInfos = DBHelper.getColumnInfos(rs);
			JScrollPane scrollPane = getTable(tableModel, columnInfos, negativeShownRed);

			if(DBHelper.getSize(rs) > maxRowsShown) {
				Box b = Box.createVerticalBox();
				b.add(new JLabel("<html><b>Warning: some rows not shown " +
						"as over max display limit: " + maxRowsShown + "</b></html>"));
				b.add(scrollPane);
				return b;
			}
			return scrollPane;
		}
		return null;
	}

	public static class KdbStringValuer implements StringValue {
		private static final long serialVersionUID = 1L;

		@Override public String getString(Object value) {
			String s = getStringSpecialized(value);
			if(s != null) {
				return s;
			}
			Object o = KdbHelper.asLine(value, true);
			if(o!=null) {
				return o.toString();
			}
			return StringValues.TO_STRING.getString(value);
		}
		
		public String getStringSpecialized(Object value) {
			if(value instanceof SerialArray) {
				try {
					Object array = ((SerialArray)value).getArray();
					if(array != null && array.getClass().isArray()) {
						StringBuilder sb = new StringBuilder("[");
						int n = Array.getLength(array);
						for(int mi=0;mi<n;mi++) {
							if(mi>0) {
								sb.append(", ");
							}
							Object o = Array.get(array, mi);
							sb.append(formatSpecialized(o));
						}
						sb.append("]");
						return sb.toString();
					}
				} catch (SerialException e) {
					LOG.warning(e.toString());
					return "";
				}
			} else if(value instanceof Boolean) {
				return ((Boolean)value) ? "TRUE" : "FALSE"; // Tick and cross
			}
			return null;
		}

		public String formatSpecialized(Object o) {
			return o == null ? "NULL" : o instanceof String ? ("'" + o.toString() + "'") : o.toString();
		}
		
	 };
	 

}
