/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2023 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timestored.qstudio.kdb;

import java.lang.reflect.Array;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import kx.c.Minute;
import kx.c.Month;
import kx.c.Second;
import kx.c.Timespan;

import com.timestored.theme.Theme;
import com.timestored.theme.Theme.CIcon;

/**
 * Types of the simplest atoms in a {@link CTable}
 */
public enum CAtomTypes {

	BOOLEAN_LIST(1, Boolean.class),
	GUID_LIST(2),
	BYTE_LIST(4, Byte.class),
	SHORT_LIST(5, Short.class),
	INT_LIST(6, Integer.class),
	LONG_LIST(7, Long.class),
	REAL_LIST(8, Float.class),
	FLOAT_LIST(9, Double.class),
	CHAR_LIST(10, Character.class),
	SYMBOL_LIST(11, String.class),
	TIMESTAMP_LIST(12, Timestamp.class, true, true),
	MONTH_LIST(13, Month.class, true, false),
	DATE_LIST(14, Date.class, true, false),
	DATETIME_LIST(15, java.util.Date.class, true, true),
	TIMESPAN_LIST(16, null, false, true),
	MINUTE_LIST(17, Minute.class, false, true),
	SECOND_LIST(18, Second.class, false, true),
	TIME_LIST(19, Time.class, false, true),
	MIXED_LIST(0),
	BOOLEAN(-1, Boolean.class),
	GUID(-2),
	BYTE(-4, Byte.class),
	SHORT(-5, Short.class),
	INT(-6, Integer.class),
	LONG(-7, Long.class),
	REAL(-8, Float.class),
	FLOAT(-9, Double.class),
	CHAR(-10, Character.class),
	SYMBOL(-11, String.class),
	TIMESTAMP(-12, Timestamp.class, true, true),
	MONTH(-13, Month.class, true, false),
	DATE(-14, Date.class, true, false),
	DATETIME(-15, java.util.Date.class, true, true),
	TIMESPAN(-16, Timespan.class, false, true),
	MINUTE(-17, Minute.class, false, true),
	SECOND(-18, Second.class, false, true),
	TIME(-19, Time.class, false, true),
	TABLE(98),
	DICTIONARY(99),
	LAMBDA(100),
	UNARY_PRIMITIVE(101),
	BINARY_PRIMITIVE(102),
	TERNARY_OPERATOR(103),
	PROJECTION(104),
	COMPOSITION(105),
	F_EACH(106),
	F_OVER(107),
	F_SCAN(108),
	F_EACH_BOTH(109),
	F_EACH_RIGHT(110),
	F_EACH_LEFT(111),
	DYNAMIC_LOAD(112),
	VIEW(3000); // my own creation not in KDB

	private static final Map<Integer,CAtomTypes> typeMap = new HashMap<Integer,CAtomTypes>();

	public static final EnumSet<CAtomTypes> NUMBERS 
		= EnumSet.of(SHORT, INT, LONG, REAL, FLOAT,
				SHORT_LIST, INT_LIST, LONG_LIST, REAL_LIST, FLOAT_LIST);

	public static final EnumSet<CAtomTypes> TEMPORALS 
		= EnumSet.of(TIMESTAMP, MONTH, DATE, DATETIME, TIMESPAN, MINUTE, SECOND, TIME,
				TIMESTAMP_LIST, MONTH_LIST, DATE_LIST, DATETIME_LIST, 
				TIMESPAN_LIST, MINUTE_LIST, SECOND_LIST, TIME_LIST);

	public static final EnumSet<CAtomTypes> LISTS 
		= EnumSet.of(BOOLEAN_LIST, GUID_LIST, BYTE_LIST, SHORT_LIST, INT_LIST,
				LONG_LIST, REAL_LIST, FLOAT_LIST, CHAR_LIST, SYMBOL_LIST,
				TIMESTAMP_LIST, MONTH_LIST, DATE_LIST, DATETIME_LIST, TIMESPAN_LIST, 
				MINUTE_LIST, SECOND_LIST, TIME_LIST, MIXED_LIST);

	public static final EnumSet<CAtomTypes> FUNCTIONS 
		= EnumSet.of(LAMBDA, UNARY_PRIMITIVE, BINARY_PRIMITIVE,
				TERNARY_OPERATOR, PROJECTION, COMPOSITION,
				F_EACH, F_OVER, F_SCAN, F_EACH_BOTH, F_EACH_RIGHT,
				F_EACH_LEFT, DYNAMIC_LOAD, VIEW);

	public static final EnumSet<CAtomTypes> CHARACTERS 
		= EnumSet.of(CHAR, CHAR_LIST, SYMBOL_LIST, SYMBOL);
	
	private final int typeNum;
	private final Class<?> clas;
	private final boolean hasDateComponent;
	private final boolean hasTimeComponent;
	

	static {
	    for(CAtomTypes qt : EnumSet.allOf(CAtomTypes.class)) {
	    	typeMap.put(qt.typeNum, qt);
	    }
	}

	private CAtomTypes(int typeNum, @SuppressWarnings("rawtypes") Class clas, 
			boolean hasDateComponent, boolean hasTimeComponent) {
		this.typeNum = typeNum;
		this.clas = clas;
		this.hasDateComponent = hasDateComponent;
		this.hasTimeComponent = hasTimeComponent;
	}

	private CAtomTypes(int typeNum, @SuppressWarnings("rawtypes") Class clas) {
		this(typeNum, clas, false, false);
	}

	private CAtomTypes(int typeNum) {
		this(typeNum, null);
	}
	
	public int getTypeNum() {
		return typeNum;
	}
	
	public static CAtomTypes getType(int typeNum) {
		return typeMap.get(typeNum);
	}
	
	/**
	 * Return what {@link CAtomTypes} o is if possible, otherwise null. 
	 * @param o The object who's type you want.
	 * @return  what {@link CAtomTypes} o is if possible, otherwise null.
	 */
	public static CAtomTypes getTypeOfJavaObject(Object o) {	

		if(o==null) {
			return null;
		}
		
		if(o.getClass().isArray()) {
			if(Array.getLength(o)>0) {
				Class<? extends Object> c = Array.get(o, 0).getClass();
			    for(CAtomTypes at : EnumSet.allOf(CAtomTypes.class)) {
			    	if(at.clas!=null && at.clas.equals(c)) {
	    				return at;
			    	}
			    }
			}
	    	return MIXED_LIST;
		} else {
		    for(CAtomTypes at : EnumSet.allOf(CAtomTypes.class)) {
		    	if(at.isAtom() && at.clas!=null && at.clas.equals(o.getClass())) {
    				return at;
		    	}
		    }
		}
	    return null;
	}

	public Class<?> getClas() {
		return clas;
	}
	
	public boolean hasDateComponent() {
		return hasDateComponent;
	}
	
	public boolean hasTimeComponent() {
		return hasTimeComponent;
	}
	
	public boolean isNumber() {
		return NUMBERS.contains(this);
	}
	
	public boolean isTemporal() {
		return TEMPORALS.contains(this);
	}
	
	public boolean isList() {
		return LISTS.contains(this);
	}
	
	public boolean isAtom() {
		return  isFunction() || !(LISTS.contains(this) 
				|| this.equals(TABLE)  || this.equals(DICTIONARY));
	}
	
	public boolean isFunction() {
		return FUNCTIONS.contains(this);
	}
	
	public boolean isCharacter() {
		return CHARACTERS.contains(this);
	}

	/** @return an icon always */
	public com.timestored.theme.Icon getIcon() {
		if(isNumber()) {
			return CIcon.NUMBER_ELEMENT;
		} else if(isTemporal()) {
			return CIcon.DATE_ELEMENT;
		} else if(equals(VIEW)) {
			return CIcon.VIEW_ELEMENT;
		} else if(equals(CAtomTypes.LAMBDA)) {
			return CIcon.LAMBDA_ELEMENT;
		} else if(isFunction()) {
			return CIcon.FUNCTION_ELEMENT;
		} else if(isCharacter()) {
			return CIcon.CHAR_ELEMENT;
		}
		return Theme.CIcon.FUNCTION_ELEMENT;
	}
}
