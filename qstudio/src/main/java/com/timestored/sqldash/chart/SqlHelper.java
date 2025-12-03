package com.timestored.sqldash.chart;

import java.sql.Types;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Porivdes helper functions for SQL related tasks.
 */
public class SqlHelper {

	private static final Set<Integer> NUMBERS;
	private static final Set<Integer> TEMPORALS;
	private static final Set<Integer> STRINGYS;
	
	static {
		
		Set<Integer> nums = new HashSet<Integer>();
		nums.add(Types.BIGINT);
		nums.add(Types.DECIMAL);
		nums.add(Types.DOUBLE);
		nums.add(Types.FLOAT);
		nums.add(Types.INTEGER);
		nums.add(Types.INTEGER);
		nums.add(Types.NUMERIC);
		nums.add(Types.REAL);
		nums.add(Types.SMALLINT);
		nums.add(Types.TINYINT);
		NUMBERS = Collections.unmodifiableSet(nums);
		
		Set<Integer> times = new HashSet<Integer>();
		times.add(Types.TIME);
		times.add(Types.TIME_WITH_TIMEZONE);
		times.add(Types.TIMESTAMP);
		times.add(Types.DATE);
		times.add(Types.TIMESTAMP_WITH_TIMEZONE);
		TEMPORALS = Collections.unmodifiableSet(times);
		
		Set<Integer> stringys = new HashSet<Integer>();
		stringys.add(Types.CHAR);
		stringys.add(Types.LONGNVARCHAR);
		stringys.add(Types.LONGVARCHAR);
		stringys.add(Types.NCHAR);
		stringys.add(Types.NVARCHAR);
		stringys.add(Types.VARCHAR);
		STRINGYS = Collections.unmodifiableSet(stringys);
	}
	
	/**
	 * @param ctypeName 
	 * @return true if the sql type specified is numeric.
	 */
	public static boolean isNumeric(int type, String ctypeName) {
		// https://github.com/duckdb/duckdb/issues/9585  
		// DuckDB returns type=2000 generic java object for all pivots. Need this to force conversion.
		return NUMBERS.contains(type) || "HUGEINT".equals(ctypeName); 
	}

	/**
	 * @return true if the sql type specified is a date/time.
	 */
	public static boolean isTemporal(int type, String ctypeName) {
		return TEMPORALS.contains(type) || "TIME".equals(ctypeName) || "TIME WITH TIME ZONE".equals(ctypeName) 
				|| "DT_SECOND".equals(ctypeName)|| "DT_MONTH".equals(ctypeName) || "DT_MINUTE".equals(ctypeName)
				|| "DT_NANOTIME".equals(ctypeName)|| "DT_NANOTIMESTAMP".equals(ctypeName);
	}

	public static boolean isStringy(int type) {
		return STRINGYS.contains(type);
	}
	
}
