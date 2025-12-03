package com.timestored.qstudio.kdb;

import java.util.Comparator;

/**
 *  Comparator that tries to use number or sensible kdb data types ordering
 *  where possible but falls back to alphabetical.
 */
public class DataComparator implements Comparator {

	@Override
	public int compare(Object o1, Object o2)  {
    	if((o1 instanceof Number) && (o2 instanceof Number)) {
    		Double d1 = ((Number)o1).doubleValue();
            return d1.compareTo(((Number)o2).doubleValue());
    	} else if((o1 instanceof Comparable) && (o2 instanceof Comparable)) {
    		return ((Comparable)o1).compareTo((Comparable)o2);
    	} else if((o1 instanceof char[]) && (o2 instanceof char[])) {
    		String s1 = new String((char[])o1);
    		String s2 = new String((char[])o2);
    		return  s1.compareTo(s2);
    	} 
    	// backup to strings
        if(o1!=null) {
        	return o2!=null ? o1.toString().compareTo(o2.toString()) : 1;
        } else if(o2!=null) {
        	return -1;
        }
        return 0;
    }
	
}