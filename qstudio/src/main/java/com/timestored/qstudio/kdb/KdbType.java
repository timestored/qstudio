package com.timestored.qstudio.kdb;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import kx.c;
import kx.c.Minute;
import kx.c.Month;
import kx.c.Second;
import kx.c.Timespan;

/**
 * Represents Kdb Data types, provides acces to their null/infinty values.
 */
public enum KdbType {
	
	BOOLEAN(-1, Boolean.class, 'b'),
	GUID(-2, UUID.class, 'g', new UUID(0, 0)),
	BYTE(-4, Byte.class, 'x', new Byte((byte) 0)),
	SHORT(-5, Short.class, 'h', new Short(Short.MIN_VALUE), new Short((short)-(1+Short.MIN_VALUE)), new Short((short)(1+Short.MIN_VALUE))), 
	INT(-6, Integer.class, 'i', c.NULL[6], new Integer(Inf.I), new Integer(NegInf.I)),
	LONG(-7, Long.class, 'j', c.NULL[7], new Long(Inf.J), new Long(NegInf.J)),
	REAL(-8, Float.class, 'e', c.NULL[8], new Float(Inf.E), new Float(NegInf.E)),
	FLOAT(-9, Double.class, 'f', c.NULL[9], new Double(Inf.F), new Double(NegInf.F)),
	CHAR(-10, Character.class, 'c', new Character(' ')),
	SYMBOL(-11, String.class, 's', ""),
	TIMESTAMP(-12, Instant.class, 'p', c.NULL[12]),
	MONTH(-13, Month.class, 'm', c.NULL[13], new Month(Inf.I), new Month(NegInf.I)),
	DATE(-14, LocalDate.class, 'd', c.NULL[14]),
	DATETIME(-15, LocalDateTime.class, 'z', c.NULL[15]),
	TIMESPAN(-16, Timespan.class, 'n', c.NULL[16], new Timespan(Inf.J), new Timespan(NegInf.J)),
	MINUTE(-17, Minute.class, 'u', c.NULL[17], new Minute(Inf.I), new Minute(NegInf.I)),
	SECOND(-18, Second.class, 'v', c.NULL[18], new Second(Inf.I), new Second(NegInf.I)),
	TIME(-19, LocalTime.class, 't', c.NULL[19]);

		
	KdbType(int typeNum, @SuppressWarnings("rawtypes") Class clas, char ch) {
		this(typeNum, clas, ch, null, null, null);
	}

	private KdbType(int typeNum, @SuppressWarnings("rawtypes") Class clas, 
			char ch, Object nullVal) {
		this(typeNum, clas, ch, nullVal, null, null);
	}

		private KdbType(int typeNum, @SuppressWarnings("rawtypes") Class clas, char ch,
				Object nullVal, Object posInfinity, Object negInfinity) {
		
		this.typeNum = typeNum;
		this.clas = clas;
		this.characterCode = ch;
		this.nullValue = nullVal;
		this.posInfinity = posInfinity;
		this.negInfinity = negInfinity;
	}


	private final Class<?> clas;
	private final char characterCode;
	private final int typeNum;
	private final Object nullValue;
	private final Object posInfinity;
	private final Object negInfinity;

	private static final Map<Class<?>, KdbType> classLookup = Maps.uniqueIndex(
			ImmutableList.copyOf(KdbType.values()),
			new com.google.common.base.Function<KdbType, Class<?>>() {
				public Class<?> apply(KdbType kt) {return kt.getClas();}
			});

	/** @return true of this type supportsd infinites otherwise false */
	public boolean hasInfinity() {
		return posInfinity!=null;
	}
	
	public char getCharacterCode() {
		return characterCode;
	}
	
	public Class<?> getClas() {
		return clas;
	}
	
	public Object getNullValue() {
		return nullValue;
	}
	
	public Object getNegInfinity() {
		return negInfinity;
	}
	
	public Object getPosInfinity() {
		return posInfinity;
	}
	
	public int getTypeNum() {
		return typeNum;
	}

	/*
	 * Storing a whole bunch of constants to check for infinities
	 */
	private static final long INF_TIME_LONG = 2143883647l;
	private static final int INF_TIMESTAMP_NANOS = 854775807;
	private static final long INF_TIMESTAMP_TIME = 10170053236854l;
	private static final long INF_DATE_LONG = 185543533782000000l;
	private static final long NEG_DATE_LONG = -185541640416000000l;
	private static final long INF_DATE = -9223371090169975809l;
	private static final long NEG_TIMESTAMP_TIME = -8276687236855l;	
	private static final long NEG_TIMESTAMP_NANOS = 145224193l;
	private static final long NEG_TIME_LONG = -2151083647l;
	private static final long NEG_DATE = -9223371090169975808l;

	/** For a given class return the KdbType or null if none apply */
	public static KdbType getType(Class<?> clas) {
		return classLookup.get(clas);
	}
	
	/** @return true if the object o is a null in KDB. */
	public static boolean isNull(Object o) {
		if(o!=null) {
			for(KdbType kt : values()) {
				if(o.equals(kt.nullValue)) {
					return true;
				}
			}
		}
		return c.qn(o); // c.java should be the definitive source of nulls. At one point they changed types they returned for timestamp from Timestamp to Instant.
	}
	
	/** @return true if the object o is a positive infinity in KDB.  */
	public static boolean isPositiveInfinity(Object o) {
		// these classes don't have a proper equals so need checked manually
		if(o == null) {
			return false;
		} else if(o.getClass() == Instant.class) {
			Instant in = (Instant) o;
			return in.getEpochSecond() == 10170056836l && in.getNano() == 854775807l;
		} else if(o.getClass() == LocalDate.class) {
			return ((LocalDate) o).toEpochDay() == 2147494604l;
		} else if(o.getClass() == LocalDateTime.class) {
			return ((LocalDateTime) o).toEpochSecond(ZoneOffset.UTC) == DT_EPOCH && ((LocalDateTime) o).getNano()==191000000;
		} else if(o.getClass() == LocalTime.class) {
			return ((LocalTime) o).toNanoOfDay() == 73883647000000l;
		} else if(o instanceof Date) {
			return ((Date) o).getTime() == INF_DATE_LONG;
		} else if(o instanceof Timestamp) {
			Timestamp ts = (Timestamp) o;
			return ts.getTime() == INF_TIMESTAMP_TIME
					&& ts.getNanos() == INF_TIMESTAMP_NANOS; 
		} else if(o instanceof Time) {
			return ((Time) o).getTime() == INF_TIME_LONG;
		} else if(o instanceof java.util.Date) {
			return ((java.util.Date) o).getTime() == INF_DATE;
		} 
		
		if(o!=null) {
			for(KdbType kt : values()) {
				if(o.equals(kt.posInfinity)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private static final long DT_EPOCH = -9223371090169976l;
	
	/** @return true if the object o is a negative infinity in KDB. */
	public static boolean isNegativeInfinity(Object o) {// these classes don't have a proper equals so need checked manually

		if(o == null) {
			return false;
		} else if(o.getClass() == Instant.class) {
			Instant in = (Instant) o;
			return in.getEpochSecond() == -8276687237l && in.getNano() == 145224193;
		} else if(o.getClass() == LocalDate.class) {
			return ((LocalDate) o).toEpochDay() == -2147472690l;
		} else if(o.getClass() == LocalDateTime.class) {
			return ((LocalDateTime) o).toEpochSecond(ZoneOffset.UTC) == DT_EPOCH && ((LocalDateTime) o).getNano()==192000000;
		} else if(o.getClass() == LocalTime.class) {
			return ((LocalTime) o).toNanoOfDay() == 12516353000000l;
		} else if(o.getClass() == Date.class) {
			return ((Date) o).getTime() == NEG_DATE_LONG;
		} else if(o.getClass() == Timestamp.class) {
			Timestamp ts = (Timestamp) o;
			return ts.getTime() == NEG_TIMESTAMP_TIME
					&& ts.getNanos() == NEG_TIMESTAMP_NANOS; 
		} else if(o instanceof Time) {
			return ((Time) o).getTime() == NEG_TIME_LONG;
		} else if(o instanceof java.util.Date) {
			return ((java.util.Date) o).getTime() == NEG_DATE;
		} 

		if(o!=null) {
			for(KdbType kt : values()) {
				if(o.equals(kt.negInfinity)) {
					return true;
				}
			}
		}
		return false;
	}
}


final class Inf {

	public static final int I = Integer.MAX_VALUE;
	public static final long J = Long.MAX_VALUE;
	public static final float E = Float.POSITIVE_INFINITY;
	public static final double F = Double.POSITIVE_INFINITY;
}

final class NegInf {
	public static final int I = 1+Integer.MIN_VALUE;
	public static final long J = 1+Long.MIN_VALUE;
	public static final float E = -Inf.E;
	public static final double F = -Inf.F;
}
