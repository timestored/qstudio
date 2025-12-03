package com.timestored.pro.rayforcedb;

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
public class RflDocs extends BuiltinDocumentedEntities implements DocSource {

	private static List<RflDocs> KNOWN_FUNCS = null;
	private final String location;

	protected RflDocs(String docname, String location, String description) {
		super(docname, description);
		this.location = location == null ? "" : location;
	}
	
	@Override public ImageIcon getIcon() { return Theme.CIcon.FUNCTION_ELEMENT.get16(); }

	@Override synchronized public List<RflDocs> getDocs() {
		return getKnownFunctions();
	}

	private static String gt(ResultSet rs, String colName) throws SQLException {
		String s = rs.getString(colName);
		return s == null ? "" : s.replace("\\n", "\n");
	}
	synchronized public static List<RflDocs> getKnownFunctions() {
		if(KNOWN_FUNCS == null) {
			KNOWN_FUNCS = new ArrayList<>();
			try {
				String json = IOUtils.toString(RflDocs.class, "rayforcedocs.json");
				SimpleResultSet rs = BabelDBJdbcDriver.fromTxt(json);
				while(rs.next()) {
					String location = gt(rs, "location");
					String title = gt(rs, "title");
					String text = gt(rs, "text");
					String docname = location.substring(location.lastIndexOf("/") + 1, location.lastIndexOf(".")).replace(")", "-");
					KNOWN_FUNCS.add(new RflDocs(docname, location, text));
				}
			} catch(Exception e) {}
		}
		return KNOWN_FUNCS; 
	}
	
	@Override public String getSource() { return "Rayforce"; }

	@Override public String getLink() {
		return "https://rayforcedb.com/" + location;
	}
}
