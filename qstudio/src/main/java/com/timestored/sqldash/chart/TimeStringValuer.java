package com.timestored.sqldash.chart;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.jdesktop.swingx.renderer.StringValue;

import kx.jdbc.KdbJdbcType;


/**
 * Converts common date/time formats to a more presentable form,
 * including showing nano seconds etc which generally are not supported elsewhere.
 * Most the logic for showing data in tables or as strings for both sqlDashboards and qStudio goes here,
 * so that the logic is shared.
 */
public class TimeStringValuer implements StringValue {
	
	private static final long serialVersionUID = 1L;
	
	/** If an array is larger than this, it's string output is truncated and ... */
	public static final long MAX_ARRAY_ITEMS_SHOWN = 50000;
	public static final String SINGLE_ITEM_LIST_PREFIX = ",";
	private static final String SPACER = " ";
	private static final String DEFAULT_EMPTY_ARRAY = "()";
	private static final String DEFAULT_POSTFIX = "";
	
	private final SimpleDateFormat nanosTimeOnlyFormat = new SimpleDateFormat("HH:mm:ss.SSSSSSSSS");
	private final SimpleDateFormat millisTimeOnlyFormat = new SimpleDateFormat("HH:mm:ss.SSS");
	private final DateTimeFormatter millisTimeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));
	// TODO increase dtformatter to nanos?
	private final DateTimeFormatter dtformatter = DateTimeFormatter.ofPattern("yyyy.MM.dd'T'HH:mm:ss.SSSSSS").withZone(ZoneId.of("UTC"));
	private final DateTimeFormatter dateOnlyFormatter;
	private final SimpleDateFormat dateOnlyFormat;
	private final SimpleDateFormat dtWithMillisFormat;
	private final DateTimeFormatter dtWithMillisFormatter;
	private final DecimalFormat nanosEndFormat = new DecimalFormat("000");

	private final StringValue overidingStringFormatter;

	/**
	 * @param overidingStringFormatter A function that if it returns non-null will be used
	 * first to return the value of a single item. Overiding nested structures is not possible.
	 */
	public TimeStringValuer(StringValue overidingStringFormatter, String overidingDateFormat) {
		this.overidingStringFormatter = overidingStringFormatter;
		if(overidingDateFormat != null) {
			dateOnlyFormat = new SimpleDateFormat(overidingDateFormat);
			dateOnlyFormatter =  DateTimeFormatter.ofPattern(overidingDateFormat).withZone(ZoneId.of("UTC")); 
			dtWithMillisFormatter =  DateTimeFormatter.ofPattern(overidingDateFormat + "'T'HH:mm:ss.SSS").withZone(ZoneId.of("UTC")); 
			dtWithMillisFormat = new SimpleDateFormat(overidingDateFormat + "'T'HH:mm:ss.SSS");
		} else {
			dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateOnlyFormatter =  DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
			dtWithMillisFormatter =  DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneId.of("UTC")); 
			dtWithMillisFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}
	}
	
	public TimeStringValuer() { this(null, null); }
	
	@Override public String getString(Object o) {
		if(o == null) {
			return "";
		}
		if(o instanceof java.sql.Timestamp[] || o instanceof java.sql.Time[]
				|| o instanceof java.sql.Date[] || o instanceof java.util.Date[]) {
			return flatten((Object[]) o, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
		}else if(o instanceof int[]) {
			return flatten((int[]) o, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
		} else if(o instanceof long[]) {
			return flatten((long[]) o, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
		} else if(o instanceof double[]) {
			return flatten((double[]) o, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
		} else if(o instanceof float[]) {
			return flatten((float[]) o, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
		} else if(o instanceof short[]) {
			return flatten((short[]) o, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
		} else if(o instanceof boolean[]) {
			return flatten((boolean[]) o, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
		} else if(o instanceof byte[]) {
			return flatten((byte[]) o, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX, "0x");
		} else if(o instanceof Integer[]) {
			return flatten((Integer[]) o);
		} else if(o instanceof Long[]) {
			return flatten((Long[]) o);
		} else if(o instanceof Double[]) {
			return flatten((Double[]) o);
		} else if(o instanceof Float[]) {
			return flatten((Float[]) o);
		} else if(o instanceof Short[]) {
			return flatten((Short[]) o);
		} else if(o instanceof Boolean[]) {
			return flatten((Boolean[]) o);
		} else if(o instanceof Byte[]) {
			return flatten((Byte[]) o);
		}
		return format(o);
	}
	 

	public static String trimTrailingPointZeroes(String numString) {
		int dotPos = numString.lastIndexOf(".");
		if(dotPos==-1) {
			return numString;
		} else {
			int lastIdx = numString.length();
			while(numString.charAt(lastIdx-1)=='0') {
				lastIdx--;
			}
			if(numString.charAt(lastIdx-1)=='.') {
				lastIdx--;
			}
			return numString.substring(0, lastIdx);
		}
	}


	public String flatten(Boolean[] a) {
		boolean[] r = new boolean[a.length];
		for(int i=0; i<a.length; i++) { r[i] = a[i]; };
		return flatten(r, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
	}

	public String flatten(Byte[] a) {
		byte[] r = new byte[a.length];
		for(int i=0; i<a.length; i++) { r[i] = a[i]; };
		return flatten(r, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX, "0x");
	}
	
	public String flatten(Short[] a) {
		short[] r = new short[a.length];
		for(int i=0; i<a.length; i++) { r[i] = a[i]; };
		return flatten(r, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
	}
	
	public String flatten(Integer[] a) {
		int[] r = new int[a.length];
		for(int i=0; i<a.length; i++) { r[i] = a[i]; };
		return flatten(r, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
	}
	
	public String flatten(Long[] a) {
		long[] r = new long[a.length];
		for(int i=0; i<a.length; i++) { r[i] = a[i]; };
		return flatten(r, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
	}

	public String flatten(Float[] a) {
		float[] r = new float[a.length];
		for(int i=0; i<a.length; i++) { r[i] = a[i]; };
		return flatten(r, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
	}

	public String flatten(Double[] a) {
		double[] r = new double[a.length];
		for(int i=0; i<a.length; i++) { r[i] = a[i]; };
		return flatten(r, DEFAULT_EMPTY_ARRAY, DEFAULT_POSTFIX);
	}
	


	public String flatten(int[] a, final String emptySt, final String postfix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX +format(a[0]) + postfix;
		}
		StringBuilder s = new StringBuilder(a.length*3);
		s.append(format(a[0]));
		for (int i = 1; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append(SPACER).append(format(a[i]));
		}
		return doEnding(a.length, s.append(postfix));
	}

	/**
	 * @return This function MUST always return a string
	 */
	public String format(Object o) {
		String s = null;
		if(this.overidingStringFormatter != null) {
			s = overidingStringFormatter.getString(o);
		}
		if(s == null) {
			if(o instanceof KdbJdbcType) {
				s = o.toString();
			} else if(o instanceof java.sql.Timestamp) {
				Timestamp ts = (Timestamp) o;
				s =  dtWithMillisFormat.format(ts) + nanosEndFormat.format((ts.getNanos()%1000000)/1000);
			} else if(o instanceof Instant) {
				s = dtformatter.format((Instant) o);
			} else if(o instanceof LocalDate) {
				s = dateOnlyFormatter.format((LocalDate) o);
			} else if(o instanceof LocalTime) {
				s = millisTimeOnlyFormatter.format((LocalTime) o);
			} else if(o instanceof LocalDateTime) {
				s = dtWithMillisFormatter.format((LocalDateTime) o);
			} else if(o instanceof java.sql.Time) {
				s = millisTimeOnlyFormat.format((java.sql.Time)o);
			} else if(o instanceof java.sql.Date) {
				s = dateOnlyFormat.format((java.sql.Date)o);
			} else if(o instanceof java.util.Date) {
				s = dtWithMillisFormat.format((java.util.Date)o);
			} 
		}
		// This function MUST always return a string, as it's converter of last resort.
		return s != null ? s : o.toString();
	}

	private static String doEnding(int arrayLength, StringBuilder s) {
		if(arrayLength>MAX_ARRAY_ITEMS_SHOWN) {
			s.append("...");
		}
		return s.toString();
	}


	public String flatten(short[] a, final String emptySt, final String postfix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX +format(a[0]) + postfix;
		}
		StringBuilder s = new StringBuilder(a.length*3);
		s.append(format(a[0]));
		for (int i = 1; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append(SPACER).append(format(a[i]));
		}
		return doEnding(a.length, s.append(postfix));
	}


	public String flatten(boolean[] a, final String emptySt, final String postfix) {
		if(a.length==0) {
			return "`boolean$()";
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX +format(a[0]) + postfix;
		}
		StringBuilder s = new StringBuilder(a.length+1);
		for (int i = 0; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append(((boolean)a[i]) ? "1" : "0");
		}
		return doEnding(a.length, s.append(postfix));
	}

	public String flatten(byte[] a, final String emptySt, final String postfix, final String prefix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX + prefix + format(a[0]) + postfix;
		}
		StringBuilder s = new StringBuilder(a.length*2+2);
		s.append(prefix);
		for (int i = 0; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
	        s.append(String.format("%02X", a[i]).toLowerCase());
		}
		return doEnding(a.length, s.append(postfix));
	}

	public String flatten(long[] a, final String emptySt, final String postfix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX + format(a[0]) + postfix;
		}
		StringBuilder s = new StringBuilder(a.length*3);
		s.append(format(a[0]));
		for (int i = 1; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append(SPACER).append(format(a[i]));
		}
		return doEnding(a.length, s.append(postfix));
	}


	public String flatten(float[] a, final String emptySt, final String postfix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX + format(a[0]) + postfix;
		}
		StringBuilder s = new StringBuilder(a.length*3);
		s.append(format(a[0]));
		for (int i = 1; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append(SPACER).append(format(a[i]));
		}
		return doEnding(a.length, s.append(postfix));
	}

	public String flatten(double[] a, final String emptySt, final String postfix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX +format(a[0]);
		}
		StringBuilder s = new StringBuilder(a.length*3);
		s.append(format(a[0]));
		for (int i = 1; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append(SPACER).append(format(a[i]));
		}
		if(!s.toString().contains(".")) {
			s.append("f");
		}
		return doEnding(a.length, s);
	}
	
	
	public String flatten(String[] a, final String emptySt, final String postfix, final String prefix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX + prefix + format(a[0]);
		}
		StringBuilder s = new StringBuilder(a.length*3);
		s.append("`" + format(a[0]));
		for (int i = 1; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append("`").append(format(a[i]));
		}
		return doEnding(a.length, s);
	}

	public String flatten(Object[] a, final String emptySt, final String postfix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return SINGLE_ITEM_LIST_PREFIX + format(a[0]) + postfix;
		}
		StringBuilder s = new StringBuilder(a.length*3);
		s.append(format(a[0]));
		for (int i = 1; i < Math.min(a.length, MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append(SPACER).append(format(a[i]));
		}
		return doEnding(a.length, s.append(postfix));
	}
 }