package com.timestored.pro.dolphindb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import com.timestored.babeldb.BabelDBJdbcDriver;
import com.timestored.babeldb.SimpleResultSet;
import com.timestored.misc.IOUtils;
import com.timestored.qdoc.BuiltinDocumentedEntities;
import com.timestored.qdoc.DocSource;
import com.timestored.theme.Theme;

/**
 * DolphinDB Documentation Provider
 * @author ray
 *
 */
public class DosDocs extends BuiltinDocumentedEntities implements DocSource {

	private static List<DosDocs> KNOWN_FUNCS = null;
	private final String params;

	protected DosDocs(String docname, String description, String params, String cal, String eg) {
		super(docname, description, cal, eg);
		this.params = params == null ? "" : params;
	}
	
	@Override public ImageIcon getIcon() { return Theme.CIcon.FUNCTION_ELEMENT.get16(); }

	@Override synchronized public List<DosDocs> getDocs() {
		return getKnownFunctions();
	}

	private static String gt(ResultSet rs, String colName) throws SQLException {
		String s = rs.getString(colName);
		return s == null ? "" : s.replace("\\n", "\n");
	}
	synchronized public static List<DosDocs> getKnownFunctions() {
		if(KNOWN_FUNCS == null) {
			KNOWN_FUNCS = new ArrayList<>();
			try {
				String json = IOUtils.toString(DosDocs.class, "docs.json");
				SimpleResultSet rs = BabelDBJdbcDriver.fromTxt(json);
				while(rs.next()) {
					KNOWN_FUNCS.add(new DosDocs(gt(rs, "title"), gt(rs, "exp"), gt(rs, "params"), gt(rs, "cal"), gt(rs, "eg")));
				}
			} catch(Exception e) {}
		}
		return KNOWN_FUNCS; 
	}
	
	@Override public String getSource() { return "Dolphin"; }

	@Override public String getLink() {
		String page = docname;
		int pos = page.indexOf("(");
		if(pos != -1) {
			page = page.substring(0, pos);
		}
		String e = page.toLowerCase().charAt(0) + "/" + page + ".html";
		// e.g. https://docs.dolphindb.cn/en/help200/FunctionsandCommands/FunctionReferences/c/convertExcelFormula.html
		return "https://docs.dolphindb.cn/en/help200/FunctionsandCommands/FunctionReferences/" + e;
	}
}
