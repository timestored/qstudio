package com.timestored.babeldb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.timestored.connections.JdbcTypes;

public class PivotProvider {

	public static ResultSet postProcess(JdbcTypes jdbcTypes, ResultSet rs, List<String> byCols, List<String> pivotCols) throws SQLException {
		return rs;
	}
	

	public static String pivotSQL(JdbcTypes jdbcTypes, List<String> groupbylist, List<String> pivotlist, String sel, String translation) {
		return "";
	}
	
}
