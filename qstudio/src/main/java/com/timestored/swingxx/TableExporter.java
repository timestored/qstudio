package com.timestored.swingxx;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.StringValue;

import ch.rabanti.nanoxlsx4j.Workbook;
import ch.rabanti.nanoxlsx4j.exceptions.IOException;
import ch.rabanti.nanoxlsx4j.styles.BasicStyles;

public class TableExporter {
	private static final Logger LOG = Logger.getLogger(TableExporter.class.getName());
	private static final String NL = "\r\n";

	private static void saveToExcel(ResultSet rs, String query, StringValue stringValue, boolean openFile, boolean openEmail) {
    	if(rs != null && Desktop.isDesktopSupported()) {
    		Executors.newSingleThreadExecutor().execute(() -> {
                try {
        			File f = File.createTempFile("document", ".xlsx");
        			String[][] info = new String[][] {  
	    					new String[] {"QStudio", "https://www.timestored.com/qstudio/" },
	    					new String[] {"Query", ""+query }, 
        					new String[] {"Date", new Date().toString()}};
    				generateWorkbook(rs, f, info, stringValue);
    				if(openFile) {
    					Desktop.getDesktop().open(f);
    				}
    				if(openEmail) {
    		     		 Desktop desktop = Desktop.getDesktop();
    		     		 if (desktop.isSupported(Desktop.Action.MAIL)) {
    		     			 String fp = URLEncoder.encode(f.getAbsolutePath(), StandardCharsets.UTF_8.toString());
    			     		 String body = fp + "%0A%0A";
    			     		 for(int r=0; r<info.length; r++) {
    			     			 String st = info[r][0] + "=" + info[r][1]; 
    			     			 body += "%0A" + URLEncoder.encode(st, StandardCharsets.UTF_8.toString());
    			     		 }
    		     			  URI mailto = new URI("mailto:?subject=qStudio%20XLS&body=" + body);
    		     			  desktop.mail(mailto);
    		     			}
    				}
    			} catch (Exception e) {
    				String msg = "Error saving file: ";
    				LOG.log(Level.SEVERE, msg, e);
    		        JOptionPane.showMessageDialog(null, msg, "Error Saving", JOptionPane.ERROR_MESSAGE);
    			}
    		});
    	}
	}
	
	public static void emailExcelForUser(ResultSet rs, String query, StringValue stringValue) {
		saveToExcel(rs, query, stringValue, false, true);
	}
	
	public static void saveToExcelAndOpen(ResultSet rs, String query, StringValue stringValue) {
		saveToExcel(rs, query, stringValue, true, false);
	}
	
	public static void saveToExcel(ResultSet rs, String query, StringValue stringValue) {
		saveToExcel(rs, query, stringValue, false, false);
	}
	
	private static int generateWorkbook(ResultSet rs, File f, String[][] info, StringValue stringValue) throws SQLException, IOException {
		Workbook workbook = new Workbook(f.getAbsolutePath(), "Sheet1");

		ResultSetMetaData rsmd = rs.getMetaData();
		int cn = rsmd.getColumnCount();

		for (int c = 1; c <= cn; c++) {
			workbook.WS.value(rsmd.getColumnName(c), BasicStyles.Bold());
		}
		workbook.WS.down(); 
		
		int row = 0;
		rs.beforeFirst();
	    for(row = 0; rs.next(); row++) {
			for (int c = 1; c <= cn; c++) {
				String s = "";
				Object o = rs.getObject(c);
				if(o!=null) {
					s = stringValue.getString(o);
				}
				if((o instanceof Number) && !s.trim().isEmpty()) {
					workbook.WS.value(o);
				} else {
					workbook.WS.value(s);	
				}
			}
			workbook.WS.down();
		}
	    if(info != null) {
	    	workbook.addWorksheet("QStudio");
		    for(String[] rowSt : info) {
		    	for(String c : rowSt) {
					workbook.WS.value(c);
		    	}
				workbook.WS.down();
		    }
	    }
	    
		workbook.save();
		return row;
	}
	

	public static void generateWorkbook(JTable table, StringValue stringValue, File f, boolean selectedAreaOnly, boolean includeHeaders)
			throws ch.rabanti.nanoxlsx4j.exceptions.IOException {

		Workbook workbook = new Workbook(f.getAbsolutePath(), "Sheet1");

		int c = 0;
		int r = 0;
		int cEnd = table.getColumnCount();
		int rEnd = table.getRowCount();
		
		// narrow down columns / rows if selected area only
		if(selectedAreaOnly) {
			c = table.getSelectedColumn();
			cEnd = c + table.getSelectedColumnCount();
			r = table.getSelectedRow();
			rEnd = r + table.getSelectedRowCount();
			if(c==-1) {
				return; // no area selected
			}
		}
		// include column headers if whole table
		if(includeHeaders) {
			for (int ci = c; ci < cEnd; ci++) {
				workbook.WS.value(table.getColumnName(ci), BasicStyles.Bold());
			}
			workbook.WS.down();
		}
		
		// loop through rows/cols building up output string.
		int rows = 0;
		for (int ri=r; ri < rEnd; ri++) {
			rows++;
			for (int ci = c; ci < cEnd; ci++) {
				// have to account for column/row order bein changed by user
				int modelCi = table.convertColumnIndexToModel(ci);
				int modelRi = table.convertRowIndexToModel(ri);
				Object o = table.getModel().getValueAt(modelRi, modelCi);
				String s = "";
				if(o!=null) {
					s = stringValue==null ? o.toString() : stringValue.getString(o);
				}
				if((o instanceof Number) && !s.trim().isEmpty()) {
					workbook.WS.value(o);
				} else {
					workbook.WS.value(s);	
				}
			}
			if(!(ri==rEnd-1)) {
				workbook.WS.down();
			}
		}
    	workbook.addWorksheet("QStudio");
    	workbook.WS.value("QStudio");
    	workbook.WS.value("https://www.timestored.com/qstudio");
		workbook.WS.down();
    	workbook.WS.value("Rows");
    	workbook.WS.value(""+rows);
		workbook.WS.down();
		workbook.save();
	}

	public static void saveTable(JXTable table, StringValue stringValue, boolean selectedAreaOnly, boolean includeHeaders, 
						String separator, File f) throws java.io.IOException {
    		LOG.info("writing out to: " + f);
    		FileWriter out = new FileWriter(f);
    		out.write(TableExporter.getTable(table, stringValue, selectedAreaOnly, includeHeaders, ","));
    		out.close();
	}
	

	private static int[] range(int floor, int ceiling) {
		int[] nums = new int[ceiling-floor];
		for(int i=floor; i<ceiling; i++) {
			nums[i]=i;
		}
		return nums;
	}
	
	/**
	 * Convert a table to values separated by separator and new lines.
	 * @param selectedAreaOnly whether to convert the whole table or only user selected area.
	 * @param separator The string to place between columns in the output.
	 */
	public static String getTable(JXTable table, StringValue stringValue, boolean selectedAreaOnly, boolean includeHeaders, final String separator) {
		
		int c = 0;
		int cEnd = table.getColumnCount();
		int rows[] = range(0, table.getRowCount());
		
		// narrow down columns / rows if selected area only
		if(selectedAreaOnly) {
			// user can choose single interval - with varying columns
			if(table.getSelectionMode() == ListSelectionModel.SINGLE_INTERVAL_SELECTION) {
				c = table.getSelectedColumn();
				cEnd = c + table.getSelectedColumnCount();
			}
			rows = table.getSelectedRows();
			if(c==-1) {
				return ""; // no area selected
			}
		}
		
		StringBuffer sb = new StringBuffer();
		
		// include column headers if whole table
		if(includeHeaders) {
			for (int ci = c; ci < cEnd; ci++) {
				sb.append(table.getColumnName(ci));
				if(!(ci==cEnd-1)) {
					sb.append(separator);
				}
			}
			sb.append(NL);
		}
		
		// loop through rows/cols building up output string.
		for (int ri : rows) {
			for (int ci = c; ci < cEnd; ci++) {
				// have to account for column/row order bein changed by user
				int modelCi = table.convertColumnIndexToModel(ci);
				int modelRi = table.convertRowIndexToModel(ri);
				Object o = table.getModel().getValueAt(modelRi, modelCi);
				String s = "";
				if(o!=null) {
					s = stringValue==null ? o.toString() : stringValue.getString(o);
					// If it's a single item. Return just it with proper new lines and all. 
					// Consider people copy-pasting JSON strings within one cell.
					if(c+1==cEnd && rows.length==1 && selectedAreaOnly && !includeHeaders) {
						return unescape(s);
					}
					if(s.contains(",")) {
						s = "\"" + s + "\"";
					}
					// numbers use unformatted to prevent erroneous commas
					// all else take a chance on the stringValuer
					if(o instanceof Number && !s.trim().isEmpty()) {
						s = "" + o;
					}
				}
				sb.append(s);
				if(!(ci==cEnd-1)) { // comma except after last column
					sb.append(separator);
				}
			}
			if(!(ri==rows[rows.length-1])) {
				sb.append(NL);	// new line except after last row
			}
		}
		return sb.toString();
	}

	public static String unescape(String qCode) {
		return qCode.replace("\\\\", "\\").replace("\\t", "\t")
				.replace("\\r", "\r").replace("\\n", "\n")
				.replace("\\\"", "\"");
	}
}
