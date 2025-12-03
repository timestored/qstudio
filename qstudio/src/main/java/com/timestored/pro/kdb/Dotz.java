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
 * Lambdas present in the .Q namespace.
 */
class Dotz extends BuiltinDocumentedEntities implements DocumentedEntity,DocSource {

	private static final List<Dotz> knownDotz;
	
	private Dotz(String name, String description) {
		super(name, description);
	}

	
	public static List<Dotz> getKnowndotz() {
		return knownDotz;
	}
	
	static {
		List<Dotz> dz = new ArrayList<Dotz>();
		dz.add(new Dotz(".z.a","ip-address ie. \".\" sv string `int$0x00 vs .z.a"));
		dz.add(new Dotz(".z.ac","Http authenticate from cookie"));
		dz.add(new Dotz(".z.b","dependencies (more information than \b)"));
		dz.add(new Dotz(".z.d","gmt date"));
		dz.add(new Dotz(".z.D","local date"));
		dz.add(new Dotz(".z.f","startup file"));
		dz.add(new Dotz(".z.h","hostname"));
		dz.add(new Dotz(".z.i","pid"));
		dz.add(new Dotz(".z.k","kdb+ releasedate "));
		dz.add(new Dotz(".z.K","kdb+ major version"));
		dz.add(new Dotz(".z.l","license information (;expirydate;updatedate;;;)"));
		dz.add(new Dotz(".z.n","Get gmt timespan (nanoseconds). e.g. 0D15:49:07.295301000"));
		dz.add(new Dotz(".z.N","Get local timespan (nanoseconds). e.g. 0D15:49:07.295301000"));
		dz.add(new Dotz(".z.o","OS "));
		dz.add(new Dotz(".z.p","Get gmt timestamp (nanoseconds). e.g. 2011.11.06D15:48:38.446651000"));
		dz.add(new Dotz(".z.P","Get local timestamp (nanoseconds). e.g. 2011.11.06D15:48:38.446651000"));
		dz.add(new Dotz(".z.pc[h]","close, h handle (already closed)"));
		dz.add(new Dotz(".z.pg[x]","get"));
		dz.add(new Dotz(".z.ph[x]","http get"));
		dz.add(new Dotz(".z.pi[x]","input (qcon)"));
		dz.add(new Dotz(".z.po[h]","open, h handle "));
		dz.add(new Dotz(".z.pp[x]","http post"));
		dz.add(new Dotz(".z.ps[x]","set"));
		dz.add(new Dotz(".z.pw[u;p]","validate user and password"));
		dz.add(new Dotz(".z.1","quiet mode"));
		dz.add(new Dotz(".z.s","self, current function definition"));
		dz.add(new Dotz(".z.t","gmt time"));
		dz.add(new Dotz(".z.T","local time"));
		dz.add(new Dotz(".z.ts[x]","timer expression (called every \t)"));
		dz.add(new Dotz(".z.u","userid "));
		dz.add(new Dotz(".z.vs[v;i]","value set"));
		dz.add(new Dotz(".z.w","handle (0 for console, handle to remote for KIPC)"));
		dz.add(new Dotz(".z.x","command line parameters (argc..)"));
		dz.add(new Dotz(".z.z","gmt timestamp. e.g. 2013.11.06T15:49:26.559"));
		dz.add(new Dotz(".z.Z","local timestamp. e.g. 2013.11.06T15:49:26.559"));
		dz.add(new Dotz(".z.c","Physical core count of the machine."));
		dz.add(new Dotz(".z.exit","Function called on exit."));
		dz.add(new Dotz(".z.pd","Peach handles"));
		dz.add(new Dotz(".z.q","Quiet Mode"));
		dz.add(new Dotz(".z.W","Returns a dictionary of ipc handles each to a list containing the number of bytes waiting in their output queues. "));
		dz.add(new Dotz(".z.zd","Zip Defaults. If defined, is an integer list of default parameters for logical block size, compression algorithm and compression level that apply when saving to files with no file extension. "));
		dz.add(new Dotz(".z.ws","Called when a message on a websocket arrives, as .z.ws[char vector|byte vector] "));
		dz.add(new Dotz(".z.bm","Called when a badly formed message is sent ot kdb."));
		dz.add(new Dotz(".z.e","TLS connection status for a connection handle."));
		dz.add(new Dotz(".z.ex","Failed primitive in a debugger session."));
		dz.add(new Dotz(".z.ey","Argument to failed primitive in a debugger session."));
		dz.add(new Dotz(".z.H","Active sockets as a low-cost list."));
		dz.add(new Dotz(".z.pm","HTTP methods callback (OPTIONS, PATCH, PUT, DELETE)."));
		dz.add(new Dotz(".z.pq","Callback for qcon protocol connections."));
		dz.add(new Dotz(".z.r","Indicates whether updates in this context are blocked."));

		knownDotz = Collections.unmodifiableList(dz);
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

	@Override public List<Dotz> getDocs() {
		return knownDotz;
	}

	@Override public String getSource() {
		return ".z";
	}
	
}
