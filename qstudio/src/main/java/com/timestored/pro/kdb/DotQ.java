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
class DotQ extends BuiltinDocumentedEntities implements DocumentedEntity,DocSource {

	private static final List<DotQ> knownDotQ;
	
	private DotQ(String name, String description) {
		super(name, description);
	}

	
	public static List<DotQ> getKnowndotq() {
		return knownDotQ;
	}
	
	static {
		List<DotQ> dq = new ArrayList<DotQ>();

		dq.add(new DotQ(".Q.addmonths","Adds y months to x"));
		dq.add(new DotQ(".Q.addr","ip-address as an integer  from a hostname symbol"));
		dq.add(new DotQ(".Q.host","hostname as a symbol for an integer ip-address"));
		dq.add(new DotQ(".Q.chk","fills missing tables"));
		dq.add(new DotQ(".Q.cn","number of rows for partitioned table passed by value"));
		dq.add(new DotQ(".Q.pn","Partition counts cached since the last time .Q.cn was called"));
		dq.add(new DotQ(".Q.D","In segmented dbs, partition vector with each element enlisted"));
		dq.add(new DotQ(".Q.dd","Shorthand for ` sv x,`$string y"));
		dq.add(new DotQ(".Q.dpft[directory;partition;`p#field;tablename]","Saves a table splayed to a specific partition of a database sorted (`p#) on a specified field"));
		dq.add(new DotQ(".Q.dsftg","(loop M&1000000 rows at a time - load/process/save) "));
		dq.add(new DotQ(".Q.en[`:db; table]","Enumerates any character columns in a table to the list sym and appends any new entries to a file in the db directory."));
		dq.add(new DotQ(".Q.fc","parallel on cut"));
		dq.add(new DotQ(".Q.fk","return ` if the column is not an fkey or `tab if the column is a fkey into tab"));
		dq.add(new DotQ(".Q.fmt","Formats a number"));
		dq.add(new DotQ(".Q.fs","Loops over file (in chunks) applying function"));
		dq.add(new DotQ(".Q.ft","creates a new function that also works on keyed"));
		dq.add(new DotQ(".Q.gc","Invokes the garbage collector."));
		dq.add(new DotQ(".Q.hdpf[historicalport;directory;partition;`p#field]","save all tables and notify host "));
		dq.add(new DotQ(".Q.ind","it takes a partitioned table and (long!) indices into the table "));
		dq.add(new DotQ(".Q.P","In segmented dbs, contains the list of segments that have been loaded "));
		dq.add(new DotQ(".Q.par[dir;part;table]","locate a table (sensitive to par.txt) "));
		dq.add(new DotQ(".Q.PD","In partitioned dbs, contains a list of partition locations "));
		dq.add(new DotQ(".Q.pd",".Q.PD as modified by .Q.view. "));
		dq.add(new DotQ(".Q.pf","contains the partition type of a partitioned hdb (only)"));
		dq.add(new DotQ(".Q.PV","In partitioned dbs, contains a list of partition values - conformant to date"));
		dq.add(new DotQ(".Q.pv",".Q.PV as modified by .Q.view. "));
		dq.add(new DotQ(".Q.qp","Returns 1b if given a partitioned table, 0b if splayed table, else 0"));
		dq.add(new DotQ(".Q.qt","Returns 1b if x is a table, 0b otherwise. "));
		dq.add(new DotQ(".Q.s","Format an object to plain text (used by the q console, obeys \\c setting"));
		dq.add(new DotQ(".Q.ty","returns character type code of argument eg \"i\"=.Q.ty 1 2"));
		dq.add(new DotQ(".Q.u","true if each partition is uniquely found in one segment. "));
		dq.add(new DotQ(".Q.v","given file handle sym, returns the splayed table stored there, any other sym, returns global"));
		dq.add(new DotQ(".Q.V","returns a table as a dictionary of column values "));
		dq.add(new DotQ(".Q.view","set a subview eg .Q.view 2#date "));
		dq.add(new DotQ(".Q.def","Allows supplying default values and types for a dicionary."));
		dq.add(new DotQ(".Q.ff","Appends columns to a table with null values. Takes two arguments: x - table to modify, y - table of columns to add to x and set to null. "));
		dq.add(new DotQ(".Q.fsn","Loops over a file and grabs specifically sized lumps of complete records (\n delimited) and allows you to apply a function to each record."));
		dq.add(new DotQ(".Q.fu","Takes a function that can be applied to a list, applies it to only the unique values and maps the result back to the full list. Useful when we have to apply an expensive function f to every element of a vector and the vector has repeats."));
		dq.add(new DotQ(".Q.id",".Q.id can be used to rename columns, as it removes characters that interfere with select/exec/update, and adds '1' to column names which clash with commands in .q namespace. "));
		dq.add(new DotQ(".Q.j10","Encodes a 10 character string to an integer. Supports the alphabet .Q.b6, this is a base64 encoding."));
		dq.add(new DotQ(".Q.x10","Decodes an integer from .Q.j10 back to a string"));
		dq.add(new DotQ(".Q.j12","Encodes a 12 character string to an integer. Supports the alphabet .Q.nA"));
		dq.add(new DotQ(".Q.x12","Decodes an integer from .Q.j12 back to a string"));
		dq.add(new DotQ(".Q.k","is checked against .z.k at startup to ensure that the executable and the copy of q.k being used are compatible "));
		dq.add(new DotQ(".Q.MAP","keeps partitions mapped to avoid the overhead of repeated file system calls during a select. Added in 3.1"));
		dq.add(new DotQ(".Q.opt","Converts command line options to a dictionary."));
		dq.add(new DotQ(".Q.w","Provides memory stats."));
		dq.add(new DotQ(".Q.pt","In partitioned dbs, contains a list of partitioned tables. "));
		dq.add(new DotQ(".Q.bv","In partitioned dbs, construct the dictionary .Q.vp of table schemas for tables with missing partitions. Optionally allow tables to be missing from partitions, by scanning partitions for missing tables and taking the tables' prototypes from the last partition (since v3.0 2012.01.26) "));
		dq.add(new DotQ(".Q.vp","In partitioned dbs, contains a dictionary of table schemas for tables with missing partitions, as populated by .Q.bv. (since v3.0 2012.01.26) "));
		dq.add(new DotQ(".Q.U","In segmented dbs, true if each partition is uniquely found in one segment."));
		dq.add(new DotQ(".Q.atob","Decodes base64 data to a byte vector."));
		dq.add(new DotQ(".Q.b6","Bicameral alphanumerics used for binhex encoding."));
		dq.add(new DotQ(".Q.bt","Dumps the current backtrace to stdout."));
		dq.add(new DotQ(".Q.btoa","Encodes data in base64 format."));
		dq.add(new DotQ(".Q.bvi","Incremental version of .Q.bv scanning only new partitions."));
		dq.add(new DotQ(".Q.Cf","Deprecated. Create empty nested char file (projection of .Q.Xf)."));
		dq.add(new DotQ(".Q.dpfts","Save table with symtable to a specific partition."));
		dq.add(new DotQ(".Q.dpt","Save table unsorted to a partitioned database."));
		dq.add(new DotQ(".Q.dpts","Save table unsorted with symtable."));
		dq.add(new DotQ(".Q.ens","Enumerate table columns against a specific domain file."));
		dq.add(new DotQ(".Q.f","Precision-format a number to fixed decimal places."));
		dq.add(new DotQ(".Q.fpn","Pipe streaming: read z-sized chunks from pipe and apply a function."));
		dq.add(new DotQ(".Q.fps","Pipe streaming with default chunk size (projection of .Q.fpn)."));
		dq.add(new DotQ(".Q.gz","GZip inflate/deflate and checks whether zlib is loaded."));
		dq.add(new DotQ(".Q.hg","HTTP GET request returning response as string."));
		dq.add(new DotQ(".Q.hp","HTTP POST request returning response as string."));
		dq.add(new DotQ(".Q.l","Load directory recursively into default namespace (like \\l)."));
		dq.add(new DotQ(".Q.ld","Load-and-group script lines for evaluation (used by \\l)."));
		dq.add(new DotQ(".Q.li","Load additional partitions into the current HDB."));
		dq.add(new DotQ(".Q.lo","Load database without changing directory or running scripts."));
		dq.add(new DotQ(".Q.M","Chunk size used by .Q.dsftg (default infinite)."));
		dq.add(new DotQ(".Q.n","String of numeric characters (\"0123456789\")."));
		dq.add(new DotQ(".Q.nA","Numeric + uppercase alphabet characters (for base-36)."));
		dq.add(new DotQ(".Q.prf0","Snapshot call stack in another process for profiling."));
		dq.add(new DotQ(".Q.res","List of q keywords and control words."));
		dq.add(new DotQ(".Q.s1","String representation for any q object."));
		dq.add(new DotQ(".Q.sbt","String backtrace formatting."));
		dq.add(new DotQ(".Q.sha1","SHA-1 hash a string, returns byte vector."));
		dq.add(new DotQ(".Q.t","Vector of type letters indexed by datatype numbers."));
		dq.add(new DotQ(".Q.trp","Trap function with backtrace support for unary functions."));
		dq.add(new DotQ(".Q.trpd","Trap function with backtrace support for general-rank f."));
		dq.add(new DotQ(".Q.ts","Measure time/space and apply function (like \\ts)."));
		dq.add(new DotQ(".Q.Xf","Deprecated. Create empty nested-vector file."));
		dq.add(new DotQ(".Q.x","Non-command command-line arguments from .Q.opt/.z.X."));
		dq.add(new DotQ(".Q.st","Timespace statistics helper (documented as ts/st)."));

		
		knownDotQ = Collections.unmodifiableList(dq);
	}

	@Override public String getLink() {
		String page = docname.replace(".", "Dot");
		int pos = page.indexOf("[");
		if(pos != -1) {
			page = page.substring(0, pos);
		}
		return "http://code.kx.com/wiki/DotQ/" + page;
	}
	
	@Override public ImageIcon getIcon() {
		return Theme.CIcon.LAMBDA_ELEMENT.get16();
	}

	@Override public List<DotQ> getDocs() {
		return knownDotQ;
	}

	@Override public String getSource() {
		return ".Q";
	}
	
}
