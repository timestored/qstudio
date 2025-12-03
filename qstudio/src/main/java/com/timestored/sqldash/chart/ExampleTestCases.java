package com.timestored.sqldash.chart;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.sql.Date;

import com.google.common.base.Preconditions;



/**
 * Container class for example {@link ExampleView}'s to be reused throughout {@link ViewStrategy}'s.
 */
public class ExampleTestCases {
	
	/*
	 * Public Test cases
	 */
	
	public static final TestCase COUNTRY_STATS; 
	public static final TestCase COUNTRY_STATS_WITHOUT_CONTINENT; 
	public static final TestCase COUNTRY_STATS_ADJUSTED_POP;
	public static final TestCase COUNTRY_STATS_GDP_ONLY; 
	public static final TestCase MONTHLY_COSTS_SALES; 
	public static final TestCase MONTHLY_COSTS_SALES_OVER_MANY_YEARS; 

	
	/*
	 * Component columns
	 */

	private static final String[] COUNTRIES = { "US", "China", "japan", "Germany", "UK", 
		"Zimbabwe", "Bangladesh", "Nigeria", "Vietnam"};
	private static final String[] CONTINENT = { "NorthAmerica", "Asia", "Asia", "Europe", "Europe", 
		"Africa", "Asia", "Africa", "Asia", };
	public static final double[] GDP = { 15080, 11300, 4444, 3114, 2228, 9.9, 113, 196, 104 };
	private static final double[] GDP_PER_CAPITA = { 48300, 8400, 34700, 38100, 36500, 
		413, 1788, 732, 3359 };
	private static final double[] POPULATION = { 313847, 1343239, 127938, 81308, 63047,
		13010, 152518, 166629, 87840};
	private static final double[] LIFE_EXP = { 77.14, 72.22, 80.93, 78.42, 78.16, 39.01, 61.33, 51.01, 70.05};

	private static final String[] MONTHS = getMonths(2000, 0, 12);
	private static final double[] COSTS = { 30, 40, 45, 55, 58, 63, 55, 65, 78, 80, 75, 90 };
	private static final double[] SALES = { 10, 12, 14, 18, 26, 42, 74, 90, 110, 130, 155, 167 };

	
	/*
	 * DuckDB SQL queries for common test cases
	 */
	private static final String DUCKDB_COUNTRY_STATS = 
			"SELECT * FROM (\r\nVALUES\r\n" +
			"('NorthAmerica','US',313847,15080,48300,77.14),\r\n" +
			"('Asia','China',1343239,11300,8400,72.22),\r\n" +
			"('Asia','Japan',127938,4444,34700,80.93),\r\n" +
			"('Europe','Germany',81308,3114,38100,78.42),\r\n" +
			"('Europe','UK',63047,2228,36500,78.16),\r\n" +
			"('Africa','Zimbabwe',13010,9.9,413,39.01),\r\n" +
			"('Asia','Bangladesh',152518,113,1788,61.33),\r\n" +
			"('Africa','Nigeria',166629,196,732,51.01),\r\n" +
			"('Asia','Vietnam',87840,104,3359,70.05)\r\n" +
			")t(Continent,Country,Population,GDP,GDPperCapita,LifeExpectancy)";
	
	private static final String DUCKDB_COUNTRY_STATS_ADJUSTED = 
			"WITH b AS (\r\n" + DUCKDB_COUNTRY_STATS + ")\r\n" +
			"SELECT Continent,Country,Population,GDP,GDPperCapita/20 AS GDPperCapita,LifeExpectancy FROM b";
	
	private static final String DUCKDB_COUNTRY_STATS_WITHOUT_CONTINENT = 
			"SELECT * FROM (\r\nVALUES\r\n" +
			"('US',313847,15080,48300),\r\n" +
			"('China',1343239,11300,8400),\r\n" +
			"('Japan',127938,4444,34700),\r\n" +
			"('Germany',81308,3114,38100),\r\n" +
			"('UK',63047,2228,36500),\r\n" +
			"('Zimbabwe',13010,9.9,413),\r\n" +
			"('Bangladesh',152518,113,1788),\r\n" +
			"('Nigeria',166629,196,732),\r\n" +
			"('Vietnam',87840,104,3359)\r\n" +
			")t(Country,Population,GDP,GDPperCapita)";
	
	private static final String DUCKDB_COUNTRY_STATS_GDP_ONLY = 
			"SELECT * FROM (\r\nVALUES\r\n" +
			"('US',15080),('China',11300),('Japan',4444),\r\n" +
			"('Germany',3114),('UK',2228),('Zimbabwe',9.9),\r\n" +
			"('Bangladesh',113),('Nigeria',196),('Vietnam',104)\r\n" +
			")t(Country,GDP)";
	
	private static final String DUCKDB_MONTHLY_COSTS_SALES = 
			"WITH b AS (\r\nSELECT \r\n" +
			"    DATE '2000-01-01' + INTERVAL '1 month' * i AS Month,\r\n" +
			"    (ARRAY[30,40,45,55,58,63,55,65,78,80,75,90])[i+1] AS Costs,\r\n" +
			"    (ARRAY[10,12,14,18,26,42,74,90,110,130,155,167])[i+1] AS Sales\r\n" +
			"FROM generate_series(0,11) t(i))\r\nSELECT * FROM b";
	
	private static final String DUCKDB_MONTHLY_COSTS_SALES_MANY_YEARS = 
			"WITH b AS (\r\nSELECT \r\n" +
			"    DATE '2000-01-01' + INTERVAL '1 month' * i AS Month,\r\n" +
			"    (ARRAY[30,40,45,55,58,63,55,65,78,80,75,90])[(i%12)+1] AS Costs,\r\n" +
			"    (ARRAY[10,12,14,18,26,42,74,90,110,130,155,167])[(i%12)+1] + (i/12)*10 AS Sales\r\n" +
			"FROM generate_series(0,35) t(i))\r\nSELECT * FROM b";
	
	static {
		
		String query;
		String duckdbQuery;
		String[] colTitles;
		ResultSet rs;
		/*
		 * Assemble test cases from components
		 */
		String countryCol =  " Country:" + toQ(COUNTRIES) + "; ";
		String numCols = "\r\n\t Population:" + toQ(POPULATION) + 
				";\r\n\t GDP:" + toQ(GDP) +  
				"; \r\n\tGDPperCapita:" + toQ(GDP_PER_CAPITA) + 
				";  \r\n\tLifeExpectancy:" + toQ(LIFE_EXP) + ")";

		String countryQuery = "([] Continent:" + toQ(CONTINENT) + ";\r\n\t" + countryCol + numCols;
		colTitles = new String[] { "Continent", "Country", "Population", "GDP", "GDPperCapita", "LifeExpectancy" };
		Object[] colValues = new Object[] {CONTINENT, COUNTRIES, POPULATION, GDP, GDP_PER_CAPITA, LIFE_EXP};
		rs = new SimpleResultSet(colTitles, colValues);
		COUNTRY_STATS = new TestCase("COUNTRY_STATS", rs, countryQuery, DUCKDB_COUNTRY_STATS);

		// adjusted GDPperCapita to make XYZ / bubble chart have sensible z/bubble size
		query = "update GDPperCapita%20 from " + countryQuery;
		colValues = new Object[] {CONTINENT, COUNTRIES, POPULATION, 
				GDP, KdbFunctions.mul(GDP_PER_CAPITA, 0.05), LIFE_EXP};
		rs = new SimpleResultSet(colTitles, colValues);
		COUNTRY_STATS_ADJUSTED_POP = new TestCase("COUNTRY_STATS", rs, query, DUCKDB_COUNTRY_STATS_ADJUSTED);

		
		query = "([] " + countryCol + numCols;
		colTitles = new String[] { "Country", "Population", "GDP", "GDPperCapita" };
		rs = new SimpleResultSet(colTitles, new Object[] {COUNTRIES, POPULATION, GDP, GDP_PER_CAPITA});
		COUNTRY_STATS_WITHOUT_CONTINENT = new TestCase("COUNTRY_STATS_WITHOUT_CONTINENT", rs, query, DUCKDB_COUNTRY_STATS_WITHOUT_CONTINENT);
		
		
		query = "([] Country:" + toQ(COUNTRIES) + "; \r\n\t GDP:" + toQ(GDP) + ")";
		colTitles = new String[] { "Country", "GDP" };
		rs = new SimpleResultSet(colTitles, new Object[] { COUNTRIES, GDP});
		COUNTRY_STATS_GDP_ONLY = new TestCase("COUNTRY_STATS_GDP_ONLY", rs, query, DUCKDB_COUNTRY_STATS_GDP_ONLY);
		
		
		query = "([Month:2000.01m + til 12]  \r\n\t Costs:" + toQ(COSTS) +
				"; \r\n\t Sales:" + toQ(SALES) + ")";
		colTitles = new String[] { "Month", "Costs", "Sales" };
		rs = new SimpleResultSet(colTitles, new Object[] { MONTHS, COSTS, SALES});
		MONTHLY_COSTS_SALES = new TestCase("MONTHLY_COSTS_SALES", rs, query, DUCKDB_MONTHLY_COSTS_SALES);

		query = "([Month:2000.01m + til 36]  \r\n\t Costs:36#" + toQ(COSTS) +
				"; \r\n\t Sales:raze 0 10 20+\\:" + toQ(SALES) + ")";
		colTitles = new String[] { "Month", "Costs", "Sales" };
		
		
		
		double[] threeYearCosts = new double[COSTS.length*3];
		for(int i=0; i<threeYearCosts.length; i++) {
			threeYearCosts[i] = COSTS[i%COSTS.length];
		}
		double[] increasingSales = new double[COSTS.length*3];
		for(int i=0; i<3; i++) {
			for(int j=0; j<COSTS.length; j++) {
				increasingSales[(i*COSTS.length)+j] = SALES[j]+(i*10);
			}
		}
		
		rs = new SimpleResultSet(colTitles, new Object[] { getMonths(2000, 0, 36), threeYearCosts, increasingSales});
		MONTHLY_COSTS_SALES_OVER_MANY_YEARS = new TestCase("MONTHLY_COSTS_SALES_OVER_MANY_YEARS", rs, query, DUCKDB_MONTHLY_COSTS_SALES_MANY_YEARS);
	}

	
	
	/** @return Date array starting at date specified and going up by one month for count months. */
	private static String[] getMonths(int year, int month, int count) {
		String[] r = new String[count];
		for(int i=0; i<count; i++) {
			int m = (month + i) % 12;
			r[i] = (year + i/12) + "." + (m<9 ? "0" : "") + (m+1);
		}
		return r;
	}

	/** 
	 * @return String array starting at date specified and going up by one day for count days. 
	 * 	outputs each date in format yyyy-mm-dd
	 * @param month starts at 1 and goes to 12 inclusive
	 */
	static java.sql.Date[] getDays(int year, int month, int day, int count) {
		Date[] r = new Date[count];
		for(int i=0; i<count; i++) {
			r[i] = new Date(year-1900, month-1, day + i);
		}
		return r;
	}
	
	/** 
	 * Starting from the supplied MONDAY date, return an array with count weekdays
	 * @param month starts at 1 and goes to 12 inclusive
	 */
	static Date[] getWeekDays(int year, int month, int day, int count) {
		Preconditions.checkArgument(count>=0);
		Calendar cal = Calendar.getInstance();
		Date[] r = new Date[count];
		int i = 0;
		Date dt = new Date(year-1900, month-1, day + i - 1);
		cal.setTime(dt);
		while(i < count) {
			for(int j=0; j<5 && i<count; j++,i++) {
				cal.add(Calendar.DATE, 1);
				r[i] = new java.sql.Date(cal.getTimeInMillis());
			}
			cal.add(Calendar.DATE, 2);
		}
		return r;
	}

	/** 
	 * @return String array starting at time specified and going up by one day for count days. 
	 * 		outputs each time in format hh:mm:ss
	 */
	static String[] getTimes(int hour, int min, int minStep, int count) {
		String[] r = new String[count];
		for(int i=0; i<r.length; i++) {
			int m = (min + i*minStep)%60;
			int h = hour + (min + i*minStep)/60;
			r[i] = (h<10 ? "0" : "") + h + ":"+ (m<10 ? "0" : "") + m + ":00";  
		}
		return r;
	}

	/** Convert double array to format that can be parsed by q */
	private static String toQ(double[] nums) {
		StringBuilder sb = new StringBuilder();
		for(double d : nums) {
			sb.append(d);
			sb.append(' ');
		}
		return sb.toString();
	}

	/** Convert string array to format that can be parsed by as symbols */
	private static String toQ(String[] symSafeStrings) {
		StringBuilder sb = new StringBuilder();
		for(String s : symSafeStrings) {
			sb.append('`');
			sb.append(s);
		}
		return sb.toString();
	}	
}
