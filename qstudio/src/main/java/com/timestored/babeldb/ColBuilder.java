package com.timestored.babeldb;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.timestored.babeldb.SimpleResultSet;

public class ColBuilder {
	List<Object> colValues = new ArrayList<>();
	List<String> colNames = new ArrayList<>();

	public void add(String colName, double val) {
		colNames.add(colName);
		colValues.add(new double[] {val});
	}
	public void add(String colName, String val) {
		colNames.add(colName);
		colValues.add(new String[] {val});
	}
	public void addDate(String colName, long l) {
		colNames.add(colName);
		colValues.add(new Date[] {new Date(l)});
	}
	public void addTimestamp(String colName, long l) {
		colNames.add(colName);
		colValues.add(new Timestamp[] {new Timestamp(l)});
	}
	
	public SimpleResultSet toSimpleResultSet() {
		return new SimpleResultSet(colNames, colValues);
	}
}