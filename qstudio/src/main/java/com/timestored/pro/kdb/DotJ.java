package com.timestored.pro.kdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;

import com.timestored.qdoc.BuiltinDocumentedEntities;
import com.timestored.qdoc.DocSource;
import com.timestored.qdoc.DocumentedEntity;
import com.timestored.theme.Theme;

/**
 * Lambdas present in the .j namespace (JSON utilities).
 */
class DotJ extends BuiltinDocumentedEntities implements DocumentedEntity,DocSource {

	private static final List<DotJ> knownDotJ;

	private DotJ(String name, String description) {
		super(name, description);
	}


	public static List<DotJ> getKnowndotj() {
		return knownDotJ;
	}

	static {
		List<DotJ> dj = new ArrayList<DotJ>();

		dj.add(new DotJ(".j.j","Serialize q object to JSON string."));
		dj.add(new DotJ(".j.jd","Serialize q object to JSON string with infinity handling."));
		dj.add(new DotJ(".j.k","Deserialize JSON string to q object."));

		knownDotJ = Collections.unmodifiableList(dj);
	}

	@Override public String getLink() {
		String page = docname.replace(".", "dot");
		int pos = page.indexOf("[");
		if(pos != -1) {
			page = page.substring(0, pos);
		}
		return "http://code.kx.com/wiki/Reference/" + page;
	}

	@Override public ImageIcon getIcon() {
		return Theme.CIcon.LAMBDA_ELEMENT.get16();
	}

	@Override public List<DotJ> getDocs() {
		return knownDotJ;
	}

	@Override public String getSource() {
		return ".j";
	}

}
