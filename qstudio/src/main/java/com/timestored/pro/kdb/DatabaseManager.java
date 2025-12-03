package com.timestored.pro.kdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;
import com.timestored.misc.IOUtils;

/**
 * Allow editing in-memory and on disk tables, supports operations like
 * add, delete, rename column. basically a java wrapper around the dbm q library. 
 */
class DatabaseManager {

	public static String GET_TREE_QUERY = "/ qstudio - get server tree \r\n" +
			"{   nsl:\".\",/:string `,key `;    \r\n" +
			"    nsf:{[ns] \r\n        ff:{ [viewset; v; fullname; sname]\r\n" +
			"            findColArgs:{$[.Q.qt x; cols x; 100h~type x; (value x)1; `$()]};\r\n" +
			"            safeCount: {$[.Q.qp x; $[`pn in key `.Q; {$[count x;sum x;-1]} .Q.pn y; -1]; count x]};\r\n" +
			"            (@[type;v;0h]; .[safeCount;(v;fullname);-2]; @[.Q.qt;v;0b]; @[.Q.qp;v;0b]; @[findColArgs;v;()]; .[in;(sname;viewset);0b])};\r\n" +
			"        vws: system \"b \",ns;\r\n        n: asc key[`$ns] except `;\r\n" +
			"        fn: $[ns~enlist \".\"; n; ns,/:\".\",/:string n];\r\n" +
			"        n!.'[ ff[vws;;;]; flip ( @[`$ns; n]; fn; n)]};\r\n" +
			"    (`$nsl)!@[nsf;;()!()] each nsl}[]";
	
	private static volatile String pre = null;
	private static final String ns = ".dbm";
	private static final List<String> ATTRIBS = Arrays.asList(new String[] { "","g","p","u","s" });
	
	private static StringBuilder getDbmLoaded() throws IOException {
		
		// lazy initialise pre field
		if (pre == null) {
			synchronized (DatabaseManager.class) {
				if (pre == null) {
					pre = IOUtils.toString(ModuleRunner.class, "dbm.q")
							+ IOUtils.toString(ModuleRunner.class, "nsfixer.q")
							+ "`.dbm; \r\n";
				}
			}
		}
	
		StringBuilder sb = new StringBuilder(pre.length() + 300);
		sb.append(pre);
		return sb;
	}

	/** Return deletion query if possible otherwise null */
	public static String getDeleteColumnQuery(String table, String column)  {
		return callFunc("deleteCol", table, column);
	}

	/** Return copy query if possible otherwise null */
	public static String getCopyColumnQuery(String table, String oldCol, String newCol)  {
		return callFunc("copyCol", table, oldCol, newCol);
	}

	/** Return rename query if possible otherwise null */
	public static String getRenameColumnQuery(String table, String oldCol, String newCol) {
		return callFunc("renameCol", table, oldCol, newCol);
	}

	/**
	 * @param attrib A single character string representing the attribute to
	 * 		be applied. Must be one of: "g","p","u","s","".
	 */
	public static String getSetAttributeColumnQuery(String table, String col, String attrib)  {
		Preconditions.checkNotNull(attrib);
		String a = attrib.trim().toLowerCase();
		Preconditions.checkArgument(ATTRIBS.contains(a));
		return callFunc("setAttrCol", table, col, a);
	}
	
	/** call a single function from dbm module, return null if not possible  */
	private static String callFunc(String funcName, String... args) {
		try {
			StringBuilder sb = getDbmLoaded();
			sb.append("{r:").append(ns).append(".").append(funcName).append("[");
			for(int i=0; i<args.length; i++) {
				sb.append(i > 0 ? "; `" : " `").append(args[i]);
			}
			sb.append("]").append("; delete from `" + ns + "; r}[]");
			return sb.toString();
		} catch(IOException e) {
			return null;
		}
	}
}
