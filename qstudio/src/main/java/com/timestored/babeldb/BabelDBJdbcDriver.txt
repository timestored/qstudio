package com.timestored.babeldb;

import java.sql.ResultSet;

public class BabelDBJdbcDriver {
	
	public BabelDBJdbcDriver(){}
	
	public static BabelDBJdbcDriver getDriverIfExists(String jdbcURL) {
		return null;
	}
	
	public boolean run(String sql) {
        return false; 
	};
	
	public void dropCreatePopulate(ResultSet rs, String fullTblName)  {
	}
	
	public static void setDEFAULT_DBRUNNER(Dbrunner dbr){}
}
