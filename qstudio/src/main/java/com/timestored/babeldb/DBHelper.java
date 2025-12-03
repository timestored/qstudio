/*******************************************************************************
 *
 *   $$$$$$$\            $$\                     
 *   $$  __$$\           $$ |                     
 *   $$ |  $$ |$$\   $$\ $$ | $$$$$$$\  $$$$$$\   
 *   $$$$$$$  |$$ |  $$ |$$ |$$  _____|$$  __$$\  
 *   $$  ____/ $$ |  $$ |$$ |\$$$$$$\  $$$$$$$$ |  
 *   $$ |      $$ |  $$ |$$ | \____$$\ $$   ____|  
 *   $$ |      \$$$$$$  |$$ |$$$$$$$  |\$$$$$$$\  
 *   \__|       \______/ \__|\_______/  \_______|
 *
 *  Copyright c 2022-2023 TimeStored
 *
 *  Licensed under the Reciprocal Public License RPL-1.5
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/license/rpl-1-5/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
 
package com.timestored.babeldb;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialJavaObject;

import com.google.common.base.Preconditions;
import com.timestored.qstudio.kdb.KdbHelper;
import com.timestored.sqldash.chart.SqlHelper;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.Getter;


/**
 * DataBase helper methods. 
 */
public class DBHelper {
	
	private static final Logger LOG = Logger.getLogger(DBHelper.class.getName());

	public static int getSize(ResultSet rs) throws SQLException {
//		int row = rs.getRow();
		rs.last();
		int size = rs.getRow();
//		rs.absolute(row);
		rs.beforeFirst();
		return size;
	}

	/** 
	 * Assuming time-series data, usually either appends at one end or the other.
	 * With huge common sequence in "middle".
	 * If the select is moving.. e.g. from trades where time>.z.t-01:00, it could change at both ends. 
	 */
	@Data private static class TblDelta {
		private final ResultSet newHeadRows;
		private final ResultSet newTailRows;
		private final int droppedHeadRows;
		private final int droppedTailRows;
	}
	
	/**
	 * @return True iff the two result sets contain identical columns, types and values.
	 * A return value of false only means they are probably not equal, it is NOT a guarantee.
	 */
	@Nullable public static TblDelta calculateDelta(ResultSet prevRS, ResultSet latestRS) {
		Preconditions.checkNotNull(prevRS);
		Preconditions.checkNotNull(latestRS);

		try {

			if(!isMetaEqual(prevRS, latestRS)) {
				return null;
			}
			int cols = prevRS.getMetaData().getColumnCount();
			
			// Some Scenarios
			// 1. New Data appended at end.     prevRS=DEFGHI   latestRS=FGHI JK
			// 2. New Data appended at start.   prevRS=DEFGHI   latestRS=BC DEFGH
			// Assumption - over half of data won't change otherwise delta is pointless.
			// Take middle value of previous and try to find location in current!
			
			int prevC = getSize(prevRS);
			int mid = (int) Math.floor(prevC/2);
			prevRS.absolute(mid);
			
			// The single ampersand here is essential to make sure both positions move
			// so the later check is true.
			int latestRSmatchRow = -1;
			while(latestRS.next()) {
				int c=1;
				for(; c<=cols; c++) {
					if(!Objects.equals( prevRS.getObject(c),  latestRS.getObject(c))) {
						break;
					}
				}
				if(c == cols+1) {
					latestRSmatchRow = latestRS.getRow();
				}
			}

			int equalsWidthUp = 0;
			while(prevRS.next() & latestRS.next()) {
				int c=1;
				for(; c<=cols; c++) {
					if(!Objects.equals( prevRS.getObject(c),  latestRS.getObject(c))) {
						break;
					}
				}
				if(c != cols+1) {
					break;
				}
				equalsWidthUp++;
			}

			
			prevRS.absolute(mid);
			latestRS.absolute(latestRSmatchRow);
			int equalsWidthDown = 0;
			while(prevRS.previous() & latestRS.previous()) {
				int c=1;
				for(; c<=cols; c++) {
					if(!Objects.equals( prevRS.getObject(c),  latestRS.getObject(c))) {
						break;
					}
				}
				if(c != cols+1) {
					break;
				}
				equalsWidthDown++;
			}

			int droppedHeadRows = mid - equalsWidthDown;
			int droppedTailRows = getSize(prevRS) - (mid + equalsWidthUp);
			
			int newHeadRowCount = latestRSmatchRow - equalsWidthDown;
			int newTailRowCount = getSize(latestRS) - (latestRSmatchRow + equalsWidthUp);
			if(newHeadRowCount > 0) {
				// create RS for head
			}
			
			//                           mid
			//                            \
			// prevRS   [RRR AAAAAAABBBBBBXCCCCCDDDDDDDDD TTTTTT]
			// latestRS [XXXXX  AAAAAAABBBBBBBXCCCCCDDDDDDDDD ZZZZZZZZZ]
			//                                  \
			//                                   latestRSmatchRow
			return new TblDelta(prevRS, latestRS, equalsWidthUp, equalsWidthDown);
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error. Assuming TblDelta not possible", e);
		}
		return null;
	}

	
	/**
	 * @return True iff the two result sets contain identical columns, types and values.
	 * A return value of false only means they are probably not equal, it is NOT a guarantee.
	 */
	public static boolean isEqual(ResultSet rsA, ResultSet rsB) {
		if(rsB == null) {
			return rsA == null;
		}
		if(rsA == null) {
			return rsB == null;
		}
		try {
			if(!isMetaEqual(rsA, rsB)) {
				return false;
			}
			
			int cols = rsA.getMetaData().getColumnCount();
			rsA.beforeFirst();
			rsB.beforeFirst();
			// The single ampersand here is essential to make sure both positions move
			// so the later check is true.
			while(rsA.next() & rsB.next()) {
				for(int c=1; c<=cols; c++) {
					boolean eq = Objects.equals( rsA.getObject(c),  rsB.getObject(c));
					if(!eq) {
						if(LOG.isLoggable(Level.FINER)) {
							LOG.log(Level.FINE, " rsA.getObject(c) = " + rsA.getObject(c));
							LOG.log(Level.FINE, " rsB.getObject(c) = " + rsB.getObject(c));
						}
						return false;
					}
				}
			}
			return rsA.isAfterLast() && rsB.isAfterLast();
			
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error. Assuming isEqualResultSets false", e);
		}
		return false;
	}

	public static List<String> getColumnNames(ResultSetMetaData rsmd) throws SQLException {
		int cols = rsmd.getColumnCount();
		List<String> cns = new ArrayList<>(cols);
		for(int c=1; c<=cols; c++) {
			cns.add(rsmd.getColumnName(c));
		}
		return cns;
	}
	
	private static boolean isMetaEqual(ResultSet rsA, ResultSet rsB) throws SQLException {
		ResultSetMetaData mdA = rsA.getMetaData();
		int cols = mdA.getColumnCount();
		ResultSetMetaData mdB = rsB.getMetaData();
		if(cols != mdB.getColumnCount()) {
			return false;
		}
		for(int c=1; c<=cols; c++) {
			if(!mdA.getColumnName(c).equals(mdB.getColumnName(c))) {
				return false;
			}
			if(mdA.getColumnType(c) != (mdB.getColumnType(c))) {
				return false;
			}
		}
		return true;
	}


	private static Object getStringifiedCell(ResultSet rs, ResultSetMetaData md, int col) throws SQLException {
	    int ct = md.getColumnType(col);
	    Object o;

	    switch (ct) {
	        case java.sql.Types.DATE:
	            o = rs.getObject(col);
	            if (o == null || rs.wasNull()) return "";
	            if (o instanceof java.util.Date)
	                return new SimpleDateFormat("yyyy-MM-dd").format((Date) o);
	            return o.toString();

	        case java.sql.Types.ARRAY:
	            o = rs.getObject(col);
	            return convertArrayToString(o);

	        default:
	            o = rs.getObject(col);
	            return (o == null || rs.wasNull()) ? "" : o.toString();
	    }
	}

	
	
	final public static String toFancyString(ResultSet rs, boolean withTypesInHeader) throws SQLException {

	    if (rs == null) return "<null>";

	    CachedRowSet crs = toCRS(rs);
	    ResultSetMetaData md = crs.getMetaData();
	    int cols = md.getColumnCount();

	    int[] widths = new int[cols];

	    // Compute column widths
	    for (int i = 1; i <= cols; i++) {
	        String label = md.getColumnLabel(i);
	        if (label == null) label = "";
	        widths[i - 1] = label.length();
	        if (withTypesInHeader) {
	            String type = md.getColumnTypeName(i);
	            if (type == null) type = "";
	            widths[i - 1] = Math.max(widths[i - 1], type.length());
	        }
	    }

	    crs.beforeFirst();
	    while (crs.next()) {
	        for (int i = 1; i <= cols; i++) {
	            Object val = getStringifiedCell(crs, md, i);
	            widths[i - 1] = Math.max(widths[i - 1], val.toString().length());
	        }
	    }

	    StringBuilder sb = new StringBuilder();

	    // Build +----+---+ border
	    Runnable topBorder = () -> {
	        sb.append('+');
	        for (int w : widths) {
	            sb.append("-".repeat(w + 2)).append('+');
	        }
	        sb.append('\n');
	    };

	    // Build | v | v |
	    java.util.function.Consumer<String[]> rowPrinter = vals -> {
	        sb.append('|');
	        for (int c = 0; c < cols; c++) {
	            sb.append(' ')
	              .append(pad(vals[c], widths[c]))
	              .append(' ')
	              .append('|');
	        }
	        sb.append('\n');
	    };

	    // Top border
	    topBorder.run();

	    // Types
	    if (withTypesInHeader) {
	        String[] types = new String[cols];
	        for (int i = 1; i <= cols; i++) {
	            String t = md.getColumnTypeName(i);
	            types[i - 1] = (t == null ? "" : t);
	        }
	        rowPrinter.accept(types);
	        topBorder.run();
	    }

	    // Headers
	    String[] headers = new String[cols];
	    for (int i = 1; i <= cols; i++) {
	        String l = md.getColumnLabel(i);
	        headers[i - 1] = (l == null ? "" : l);
	    }
	    rowPrinter.accept(headers);

	    topBorder.run();

	    // Rows
	    crs.beforeFirst();
	    while (crs.next()) {
	        String[] vals = new String[cols];
	        for (int i = 1; i <= cols; i++) {
	            vals[i - 1] = getStringifiedCell(crs, md, i).toString();
	        }
	        rowPrinter.accept(vals);
	    }

	    topBorder.run();

	    return sb.toString();
	}

	private static String pad(String s, int width) {
	    if (s == null) s = "";
	    int missing = width - s.length();
	    return missing <= 0 ? s : s + " ".repeat(missing);
	}

	
	
	
	
	
	
	final public static String toString(ResultSet rs, boolean withTypesInHeader) throws SQLException {
		StringBuilder sb = new StringBuilder();
		ResultSetMetaData rsmd = rs.getMetaData();
		int cn = rsmd.getColumnCount();
		if(withTypesInHeader) {
			for (int i = 1; i <= cn; i++) {
				if (i > 1)
					sb.append(" | ");
				sb.append(rsmd.getColumnType(i));
			}
			sb.append("\r\n");
		}
		for (int i = 1; i <= cn; i++) {
			if (i > 1)
				sb.append(" | ");
			sb.append(rsmd.getColumnLabel(i));
		}
		sb.append("\r\n");
		rs.beforeFirst();
		Object o = null;
		while (rs.next()) {
			for (int i = 1; i <= cn; i++) {
				int ct = rsmd.getColumnType(i);
				if (i > 1)
					sb.append(" | ");
				switch(ct) {
					case java.sql.Types.DATE:
						o = rs.getObject(i);
						if(o == null || rs.wasNull()) {
							sb.append(" ");
						} else if(o instanceof Date) {
							sb.append(new SimpleDateFormat("yyyy-MM-dd").format((Date)o));
						} else {
							sb.append(""+o);
						}
						break;
					case java.sql.Types.ARRAY:
						o = rs.getObject(i); //this must be getObject as H2 dosn't support getArray 
						sb.append(convertArrayToString(o));
						break;
					default:
						o = rs.getObject(i);
						sb.append((o == null || rs.wasNull()) ? " " : (""+o));
				}
			}
			sb.append("\r\n");
		}
		return sb.toString();
	}

    
    public static CachedRowSet toCRS(ResultSet rs) throws SQLException {
    	if(rs == null) {
    		return null;
    	} else if(rs instanceof CachedRowSet) {
    		return (CachedRowSet) rs;
    	}
		CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
    	crs.populate(rs);
    	return crs;
    }


	public static String convertArrayToString(Object o) throws SQLException {
		if(o == null) {
			return "";
		} else if(o instanceof SerialArray) {
			SerialArray sa = (SerialArray)o;
			o = sa.getArray();
		} 
		boolean duckdbArray = o instanceof Object[] && ((Object[])o).length>0 && ((Object[])o)[0] instanceof SerialJavaObject;
		if(duckdbArray) {
			Object[] sa = (Object[])o;
			Object[] oa = new Object[sa.length];
			for(int i=0; i<sa.length; i++) {
				oa[i] = ((SerialJavaObject)sa[i]).getObject();
			}
			o = oa;
		}
		
		if(o instanceof int[]) {
			return Arrays.toString((int[])o);
		} else if(o instanceof long[]) {
			return Arrays.toString((long[])o);
		} else if(o instanceof float[]) {
			return Arrays.toString((float[])o);
		} else if(o instanceof double[]) {
			return Arrays.toString((double[])o);
		} else if(o instanceof String[]) {
			return Arrays.toString((String[])o);
		} else if(o instanceof Object[]) {
			return Arrays.toString((Object[])o);
		}
		return o.toString();
	}

	public static String toKdbStringList(Collection<String> l) {
		if(l.size() == 1) {
			StringBuilder sb = new StringBuilder("(");
			sb.append("enlist ").append(wrapKdbString("" + l.iterator().next()));
			sb.append(")");
			return sb.toString();
		}
		return toStringList("(", ")", ";", l, o -> wrapKdbString("" + o));
	}

	public static String toStringList(String starter, String ender, String sep, Collection<String> l, Function<String,String> wrapper) {
		StringBuilder sb = new StringBuilder(starter);
		boolean firstEntry = true;
		for(Object o : l) {
			if(!firstEntry) {
				sb.append(sep);
			}
			sb.append(wrapper.apply("" + o));
			firstEntry = false;
		}
		sb.append(ender);
		return sb.toString();
	}
	
	/**
	 * Wrap a string for the relevant database.
	 */
	public static String wrapKdbString(String s) {
		String r = "";
		// careful of single chars and escaping quotes
		if(s.length()<2) {
			r = "enlist ";	
		}
		r += "\"" + s.replace("\"", "\\\"") + "\"";
		return r;
	}
	
	public static ColumnInfo[] getColumnInfos(ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int cn = rsmd.getColumnCount();
		ColumnInfo[] ci = new ColumnInfo[cn];
		for (int i = 1; i <= cn; i++) {
			int ct = rsmd.getColumnType(i);
			String ctn = rsmd.getColumnTypeName(i);
			ci[i-1] = SqlHelper.isNumeric(ct, ctn) ? new NumericColumnInfo() : new GenericColumnInfo();
		}
		
		rs.beforeFirst();
		Object o = null;
		while (rs.next()) {
			for (int i = 1; i <= cn; i++) { 
				o = rs.getObject(i);
				boolean wasNull = rs.wasNull();
				ci[i-1].addEntry(o, wasNull);
			}
		}
		return ci;
	}

	public static class GenericColumnInfo extends ColumnInfo {
		private String min = null;
		private String max = null;

		@Override public String getColumnInfo() {
			return super.getColumnInfo() + ((count > 1 && min != null && max != null) ? ("\nmin: " + min + "\nmax: " + max) : ""); 
		}

		@Override public void addEntry(Object o, boolean wasNull) {
			super.addEntry(o, wasNull);
			if(!wasNull && o != null) {
				String s = o.toString();
				min = min == null ?  s : (s.compareTo(min) > 0 ? min : s);
				max = max == null ?  s : (s.compareTo(max) > 0 ? s : max);
			}
		}
		
		@Override public String toString() {
			return getColumnInfo();
		}
	}
	
	public static abstract class ColumnInfo {
		private static final int MAX_SIZE = 10_000;
		private int nullCount;
		protected int count;
		private Map<Object, Integer> distinctCountMap = new HashMap<>(); 

		void addEntry(Object o, boolean wasNull) {
			count++;
			boolean isNull = wasNull || o == null;
			nullCount += isNull ? 1 : 0;
			if(distinctCountMap.size() < MAX_SIZE) {
				distinctCountMap.merge(o, 1, Integer::sum);
			}
		}
		
		public String getColumnInfo() { 
			if(count == 0) {
				return "";
			}
			String s = "nulls: " + nullCount + "\ncount: " + count;
			if(distinctCountMap.size() == count) {
				s += "\ndistincts: Every row unique.";
			} else if(distinctCountMap.size() == count) {
				s += "\ndistincts: All same value.";
			} else if(distinctCountMap.size() < MAX_SIZE) {
				s += "\ndistincts: " + (distinctCountMap.size() < 10 ? distinctCountMap.toString() : distinctCountMap.size());
			}
			return s;
		}
		
		@Override public String toString() {
			return getColumnInfo();
		}
	}
	
	private static class NumericColumnInfo extends ColumnInfo {
		@Getter private double min = Double.POSITIVE_INFINITY;
		@Getter private double max = Double.NEGATIVE_INFINITY;
		@Getter private double sum = 0;

		@Override public String getColumnInfo() {
			return super.getColumnInfo() + "\nmin: " + KdbHelper.formatFloatingPt(min)
				+ "\nmax: " + KdbHelper.formatFloatingPt(max) + "\nsum: " + KdbHelper.formatFloatingPt(sum)
				 + "\naverage: " + KdbHelper.formatFloatingPt(sum/(super.count-super.nullCount)); 
		}

		@Override public void addEntry(Object o, boolean wasNull) {
			super.addEntry(o, wasNull); 
			if(o != null && o instanceof Number) {
				Number n = (Number) o;
				double d = n.doubleValue();
				sum += d;
				min = Math.min(d, min);
				max = Math.max(d, max);
			}
		}
		
		@Override public String toString() {
			return getColumnInfo();
		}
	}
}


