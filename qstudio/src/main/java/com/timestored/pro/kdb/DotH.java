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
 * Lambdas present in the .h namespace (HTML/HTTP utilities).
 */
class DotH extends BuiltinDocumentedEntities implements DocumentedEntity,DocSource {

	private static final List<DotH> knownDotH;

	private DotH(String name, String description) {
		super(name, description);
	}


	public static List<DotH> getKnowndoth() {
		return knownDotH;
	}

	static {
		List<DotH> dh = new ArrayList<DotH>();

		dh.add(new DotH(".h.HOME","Home directory URL."));
		dh.add(new DotH(".h.br","HTML line break."));
		dh.add(new DotH(".h.c0","Web color — Loss (default: \"ff0000\")."));
		dh.add(new DotH(".h.c1","Web color — Gain (default: \"009900\")."));
		dh.add(new DotH(".h.cd","CSV from data."));
		dh.add(new DotH(".h.code","Code block format."));
		dh.add(new DotH(".h.d","Delimiter."));
		dh.add(new DotH(".h.ed","Excel from data."));
		dh.add(new DotH(".h.edsn","Excel from data with sheet name."));
		dh.add(new DotH(".h.fram","HTML frameset."));
		dh.add(new DotH(".h.ha","Anchor tag."));
		dh.add(new DotH(".h.hb","HTML body."));
		dh.add(new DotH(".h.hc","HTML escape."));
		dh.add(new DotH(".h.he","HTTP 400 error."));
		dh.add(new DotH(".h.hn","HTTP response header."));
		dh.add(new DotH(".h.hp","HTTP response with body."));
		dh.add(new DotH(".h.hr","Horizontal rule."));
		dh.add(new DotH(".h.ht","Marque-up."));
		dh.add(new DotH(".h.hta","HTML start tag with attributes."));
		dh.add(new DotH(".h.htac","HTML element with attributes and content."));
		dh.add(new DotH(".h.htc","HTML element with content."));
		dh.add(new DotH(".h.html","HTML page."));
		dh.add(new DotH(".h.http","HTTP response."));
		dh.add(new DotH(".h.hu","URI encode."));
		dh.add(new DotH(".h.hug","URI map."));
		dh.add(new DotH(".h.hy","HTTP response body."));
		dh.add(new DotH(".h.iso8601","ISO 8601 timestamp."));
		dh.add(new DotH(".h.jx","Table to JSON."));
		dh.add(new DotH(".h.ka","Coerce to atom."));
		dh.add(new DotH(".h.logo","KX logo."));
		dh.add(new DotH(".h.nbr","Number format."));
		dh.add(new DotH(".h.pre","Preformatted text."));
		dh.add(new DotH(".h.sa","Anchor style."));
		dh.add(new DotH(".h.sb","Body style."));
		dh.add(new DotH(".h.sc","Center style."));
		dh.add(new DotH(".h.td","Table data."));
		dh.add(new DotH(".h.text","Plain text HTTP response."));
		dh.add(new DotH(".h.tx","Type format."));
		dh.add(new DotH(".h.ty","MIME types."));
		dh.add(new DotH(".h.uh","URI decode."));
		dh.add(new DotH(".h.val","Value."));
		dh.add(new DotH(".h.xd","XML."));
		dh.add(new DotH(".h.xmp","XMP element."));
		dh.add(new DotH(".h.xs","XML escape."));
		dh.add(new DotH(".h.xt","JSON types."));

		knownDotH = Collections.unmodifiableList(dh);
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

	@Override public List<DotH> getDocs() {
		return knownDotH;
	}

	@Override public String getSource() {
		return ".h";
	}

}
