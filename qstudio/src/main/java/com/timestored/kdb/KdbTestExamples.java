package com.timestored.kdb;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.runners.Parameterized;

/**
 * Example queryies for generating tables containing different time types in KDB. 
 */
public class KdbTestExamples {

	// queries with space between to allow multiplying values.

	private static final String Q = ";a:m*0 1 2 5 6 10; ([] t:x$a; v:0 1 2 3 0N 5)} \"";	
	
	private static final String MQ = ";a:til 6; b:0 1 2 5 6 10; ([] t1:x$m*b; t2:2011.06.06+b;" +
			" u:div[a;2]; v:`real$0.33*a; x:neg b; w:`float$a)} \"";


    @Parameterized.Parameters
    public static ArrayList<Object[]> getCandleStickInstancesToTest() {
		ArrayList<Object[]> r = new ArrayList<Object[]>();
		
		// The simpler time types
		for(char t : "mduv".toCharArray()) { addCandle(r, "{m:1", t);	}
		/* 
		 * multiplier used for data type like timespan where must multiply
		 * to make increments between values large enough to be reasonable.
		 */
		for(char t : "tz".toCharArray()) { addCandle(r, "{m:1000", t);	}
		for(char t : "np".toCharArray()) { addCandle(r, "{m:1000000", t);	}
		return r;
    }

	private static void addCandle(ArrayList<Object[]> r, String prefix, char t) {	
		String cndl = ";a:m*til 22; ([] t:x$a; high:c+30; low:c-20; open:60+til 22; close:c:55+2*til 22)} \"";
		r.add( new Object[]{ prefix + cndl + t + "\"", "candle-"+t });
	}
	
    @Parameterized.Parameters
    public static ArrayList<Object[]> getTimeSeriesInstancesToTest() {
    	ArrayList<Object[]> r = new ArrayList<Object[]>();
    	
    	// The simpler time types
		for(char t : "mduv".toCharArray()) { addExamples(r, "{m:1", t);	}
        /* 
         * multiplier used for data type like timespan where must multiply
         * to make increments between values large enough to be reasonable.
         */
		for(char t : "tz".toCharArray()) { addExamples(r, "{m:1000", t);	}
		for(char t : "np".toCharArray()) { addExamples(r, "{m:1000000", t);	}
        return r;

    }
	
    @Parameterized.Parameters
    public static Collection<Object[]> instancesToTest() {
    	ArrayList<Object[]> r = getTimeSeriesInstancesToTest();
		r.add( new Object[]{ KDB_CONVERT_TO_LISTS+KDB_TYPES_DATE_QRY, "cc-nest-kdb_types_date_qry" });
		r.add( new Object[]{ KDB_CONVERT_TO_LISTS+KDB_TYPES_QRY, "cc-nest-kdb_types_qry" });
		r.add( new Object[]{ KDB_CONVERT_TO_LISTS+KDB_DATETIME_QRY, "cc-nest-kdb_datetime_qry" });
		r.add( new Object[]{ KDB_TYPES_DATE_QRY, "cc-kdb_types_date_qry" });
		r.add( new Object[]{ KDB_TYPES_QRY, "cc-kdb_types_qry" });
		r.add( new Object[]{ KDB_DATETIME_QRY, "cc-kdb_datetime_qry" });
		r.add( new Object[]{ KDB_NESTED_NUMS, "cc-kdb_nested_nums" });
		r.add( new Object[]{ KDB_NESTED_CHARS, "cc-kdb_nested_chars" });
		r.add( new Object[]{ "`pq`oi`uy!3 1 2", "cc-kdb_dict" });
		r.add( new Object[]{ "3 1 2", "cc-kdb_simplelist" });
		r.add( new Object[]{ "(1 2;`pq`k`j)", "cc-kdb_nestedlist" });

		final String NT = "([] v:1 2; m:(([] nv:1 2);([] nv:1 2)))";
		r.add( new Object[]{ NT, "cc-kdb_nestedTable" });
		r.add( new Object[]{ "([] v:1 2; m:(1 2;til 100))", "cc-kdb_nestedTable-list" });
		r.add( new Object[]{ "([] v:1 2; m:(`p`o!1 2;`p`o!(3 4 5;6 7 8)))", "cc-kdb_nestedTable-Dict" });
		r.add( new Object[]{ "([] v:1 2 3; m:(`p`o!1 2; ([] ta:`p`o`i; tv:10 20 30) ; 1))", "cc-kdb_nestedTable-mixed" });
		r.add( new Object[]{ "("+NT + ";" + NT + ")", "cc-kdb_nestedTable-nestedNested" });
		return r;
    }
    
	private static void addExamples(ArrayList<Object[]> r, String prefix, char t) {
		r.add( new Object[]{ prefix + Q + t + "\"", "simple-"+t });
		r.add( new Object[]{ prefix + MQ + t + "\"", "multi-"+t });
	}
	
	
	// taken from Pulse ConferenceController
	public static String KDB_TYPES_DATE_QRY = "([] p:0N 2023.02.10D15:36:36.576232000p; m:0N 2012.01m; d:0N 2012.01.20d; z:0N 2023.02.10T15:38:41.256z; n:0N 0D15:36:07.429070000n; u:0N 01:30u; v:0N 08:00:01v; t:0N 08:00:01.123t)";
	public static String KDB_TYPES_QRY = "([] l:(1 2;3 4 5); b:01b; g:2#\"G\"$\"8c680a01-5a49-5aab-5a65-d4bfddb6a661\"; x:0x0004; h:0 5h; i:0N 6i; j:0N 7j; e:0n 8.8e; f:0n 9.9f; c:\" A\"; s:``11)";
	public static String KDB_DATETIME_QRY = "{a:til 2; update ts:`timestamp$dt from update dt:t+2023.07.05 from ([] t:00:00u + a; v:a; d:2023.07.05+a; sec:00:00:00+a; tim:00:00t+a)}[]";
	public static String KDB_NESTED_NUMS = "([] b:(01b;010b); h:(5 0Nh;1 2 3h); i:(6 0Ni;4 5 7i); j:(7 0Nj;8 9 1j); e:(8.8 0ne; 1 2 3e); f:(9.9 0nf; 1 2 3f))";
	public static String KDB_NESTED_CHARS =  "([] c:(\" A\";\"DEF\"); s:(`11`;`p`o`i); st:((\"\";\"AA\";\"BB\");(\"CC\";\"DD\")))";
	public static String KDB_CONVERT_TO_LISTS = "({2#x}'')";
}
