package com.timestored.sqldash.chart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.jfree.data.time.Day;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Decorates a resultSet to provide a data structure easier to access for charting. 
 * <ul>
 * <li>Charts recognise only three types of columns: strings / numbers / dates</li>
 * <li>Number columns are all converted to {@link NumericCol} / double[]'s</li>
 * <li>Non-Number columns are all converted to {@link StringyCol}</li>
 * <li>Only the first date/time column is converted to {@link TimeCol}</li>
 * <li>The order of the columns is maintained within each column type </li>
 * </ul>
 * <p>The first consecutive string columns are considered concatenated into rowTitles.
 * Charts can either interpret data by row or by column, where column names or row names are
 * 		used as labels for series. This supports transposing to easily flip interpretation.</p>
 * 
 */
class ChartResultSet {

	private final List<NumericCol> numericColumns;
	private final List<StringyCol> stringyColumns;
	private final TimeCol timeCol;
	private final List<String> rowLabels;
	private final String rowTitle;
	private final String colTitle;

	
	public static ChartResultSet getInstance(ResultSet rs) throws SQLException {
		return ChartResultSetBuilder.getChartResultSet(rs);
	}

	public static ChartResultSet getTransposedInstance(ResultSet rs) throws SQLException {
		return ChartResultSetBuilder.getChartResultSet(rs);
	}
	
	ChartResultSet(List<NumericCol> numericColumns, List<StringyCol> stringyColumns, 
			List<String> rowTitles, TimeCol timeCol, String rowTitle, String colTitle) {

		this.numericColumns = Collections.unmodifiableList(checkNotNull(numericColumns));
		this.stringyColumns = Collections.unmodifiableList(checkNotNull(stringyColumns));
		this.rowLabels = Collections.unmodifiableList(checkNotNull(rowTitles));
		this.rowTitle = checkNotNull(rowTitle);
		this.colTitle = checkNotNull(colTitle);
		this.timeCol = timeCol;
		
		// checking the row count of all columns are the same.
		int rowCount = rowTitles.size();
		checkArgument(timeCol==null || timeCol.size()==rowCount);
		for(NumericCol c : this.numericColumns) {
			checkArgument(c.getDoubles().length==rowCount);
		}
		for(StringyCol c : this.stringyColumns) {
			checkArgument(c.getVals().size()==rowCount);
		}
	}
	
	/** Find a numerical column by columnLabel (ignoring case) */
	public NumericCol getNumericalColumn(String columnLabel)  {
		for(NumericCol nc : numericColumns) {
			if (nc.getLabel().equalsIgnoreCase(columnLabel)) {
				return nc;
			}
		}
		return null;
	}

	public int getRowCount() {
		return rowLabels.size();
	}

	/**
	 * @return The first time column in the result set or null if there was none.
	 */
	public TimeCol getTimeCol() {
		return timeCol;
	}

	/**
	 * @return List of numerical columns found in this result set ordered same as source table.
	 */
	public List<NumericCol> getNumericColumns() {
		return numericColumns;
	}


	/**
	 * @return List of non-numerical columns found in this result set ordered same as source table.
	 */
	public List<StringyCol> getStringyColumns() {
		return stringyColumns;
	}
	
		
	
	/** Abstract class intended for reuse */
	public static abstract class Col {
		
		private final int type;
		private final String name;
		
		Col(String name, int type) {
			this.name = name;
			this.type = type;
		}
		
		/** The label of this column */
		public String getLabel() {
			return name;
		}

		/** The sql type number of this column */
		public int getType() {
			return type;
		}
	}

	/** Represents a titled column containing numeric data. */
	public static class NumericCol extends Col {
		
		private final double[] vals;
		
		NumericCol(String name, int type, double[] vals) {
			super(name, type);
			this.vals = vals;
		}
		
		public double[] getDoubles() {
			return vals;
		}

		@Override public String toString() {
			return "NumericCol [vals=" + Arrays.toString(vals) + "]";
		}
		
	}

	/** Represents a Stringy column (basically anything non-numeric) */
	public static class StringyCol extends Col {
		
		protected final List<Object> vals;
		
		StringyCol(String name, int type, List<Object> vals) {
			super(name, type);
			this.vals = vals;
		}
		
		public int size() { return vals == null ? 0 : vals.size(); }
		
		public List<Object> getVals() {
			return vals;
		}

		@Override public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((vals == null) ? 0 : vals.hashCode());
			return result;
		}

		@Override public boolean equals(Object obj) {
			
			if (obj!=null && (getClass() == obj.getClass())) {
				StringyCol that = (StringyCol) obj;
				if(vals.size() != that.vals.size()) {
					return false;
				}
				for(int i=0; i<vals.size(); i++) {
					if(!vals.get(i).toString().equals(that.vals.get(i).toString())) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		@Override public String toString() {
			return "StringyCol [vals=" + vals + "]";
		}
		
	}
	
	/** Represents a titled column containing temporal data. */
	public static class TimeCol extends StringyCol {
		
		private volatile Date[] dates = null;
		
		TimeCol(String name, int type, List<Object> vals) {
			super(name, type, vals);
		}
		
		/** @return The time data contained in this {@link Col} as a RegularTimePeriod[] array. */
		public RegularTimePeriod[] getRegularTimePeriods() {
			return convertToJFreeTime(vals);
		}

		private RegularTimePeriod[] convertToJFreeTime(List<Object> timeObjects) {
			
			final int rowCount = timeObjects.size();
			RegularTimePeriod[] res = new RegularTimePeriod[rowCount];
			int unconvertedRows = 0;
			
			for(int row = 0; row < rowCount; row++) {
				Object timeObject = timeObjects.get(row);
		    		RegularTimePeriod timePeriod = null;
		    		if(timeObject instanceof RegularTimePeriod) {
		    			timePeriod = (RegularTimePeriod) timeObject;
		    		} else if(timeObject instanceof java.time.YearMonth) {
		    			java.time.YearMonth t = (java.time.YearMonth)timeObject;
				    	LocalDate ld = t.atDay(1);
			        timePeriod = new Day(ld.getDayOfMonth(), ld.getMonthValue(), ld.getYear());
		    		} else if(timeObject instanceof java.sql.Time) {
		    			java.sql.Time t = (java.sql.Time)timeObject;
			        timePeriod = new Millisecond(new java.util.Date(t.getTime()));	
		    		} else if(timeObject instanceof OffsetTime) {
			        timePeriod = new Millisecond(new java.util.Date(((OffsetTime)timeObject).getLong(ChronoField.MILLI_OF_DAY)));	
		    		} else if(timeObject instanceof OffsetDateTime) {
			        timePeriod = new Day(new java.util.Date(((OffsetDateTime)timeObject).toInstant().toEpochMilli()));	
		    		} else if(timeObject instanceof LocalTime) {
		    			long tick = ((LocalTime)timeObject).atOffset(ZoneOffset.UTC).getLong(ChronoField.MILLI_OF_DAY);
			        timePeriod = new Millisecond(new java.util.Date(tick), TimeZone.getTimeZone("UTC"));	
		    		} else if(timeObject instanceof java.sql.Timestamp) {
			        timePeriod = new Millisecond((java.sql.Timestamp)timeObject, TimeZone.getTimeZone("UTC"));	
			    } else if(timeObject instanceof LocalDate) {
			    	LocalDate ld = (LocalDate) timeObject;
			        timePeriod = new Day(ld.getDayOfMonth(), ld.getMonthValue(), ld.getYear());
			    } else if(timeObject instanceof LocalDateTime) {
			    		long tick = ((LocalDateTime) timeObject).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
			    		timePeriod = new Millisecond(new java.util.Date(tick), TimeZone.getTimeZone("UTC"));
			    } else if(timeObject instanceof java.sql.Timestamp) {
			        timePeriod = new Millisecond((java.sql.Timestamp)timeObject, TimeZone.getTimeZone("UTC"));	
			    } else if(timeObject instanceof Date) {
			        timePeriod = new Day((Date) timeObject, TimeZone.getTimeZone("UTC"));
		    		} else if(timeObject instanceof Instant) {
				    	long tick = ((Instant) timeObject).toEpochMilli();
				    	timePeriod = new Millisecond(new java.util.Date(tick), TimeZone.getTimeZone("UTC"));
		    		} else {
				    	unconvertedRows++;
		    		}
		    		res[row] = timePeriod;
			} 
			if(rowCount>0 && unconvertedRows == rowCount) {
				throw new IllegalArgumentException("Could not convert any rows of the time column");
			}
			return res;
		}

	}

	/**
	 * Similar to columns in standard resultsets, columns have titles in this resultSet.
	 * @return The name of this row (typically concatenation of first consecutive string columns)
	 * 	if there were any otherwise the row number.
	 */
	public String getRowLabel(int row) {
		return rowLabels.get(row);
	}

	private List<String> getColumnNames(List<? extends Col> cols) {
		List<String> n = Lists.newArrayList();
		for(Col c : cols) {
			n.add(c.getLabel());
		}
		return n;
	}
	
	@Override
	public String toString() {
		ToStringHelper tsh = MoreObjects.toStringHelper(this)
			.add("numericColumns", Joiner.on(',').join(getColumnNames(numericColumns)));
		if(timeCol == null) {
			tsh.add("timeCol", "no time col");
		} else {
			tsh.add("timeCol", timeCol.getLabel());	
		}
		if(numericColumns.size() > 0) {
			tsh.add("numeric rows", numericColumns.get(0).getDoubles().length);
		}
		return tsh.toString();
	}

	/**
	 * @return The title of the row labels, e.g. if labels were England,America,Scotland the row
	 * title would be countries. In a standard data layout this will be the column header of
	 * the various title columns concatenated with - between each.
	 */
	public String getRowTitle() {
		return rowTitle;
	}

	/**
	 * @return The title of the column labels, normally there wont be for standard dataset
	 * 		only when transposed the rowTitle becomes colTitle.
	 */
	public String getColTitle() {
		return colTitle;
	}

	public StringyCol getRowLabels() {
		List<Object> objList = new ArrayList<Object>(rowLabels.size());
		for(String s : rowLabels) {
			objList.add(s);
		}
		return new StringyCol(getRowTitle(), Types.VARCHAR, objList);
	}

	
}
