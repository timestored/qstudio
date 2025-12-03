package com.timestored.babeldb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Pulse standard intervals to maintain standard namings 1s 1m 1h 1d 1w
@RequiredArgsConstructor
public enum PulseInterval { 
	S1("1s","second",null), M1("1m","minute","1min"), H1("1h","hour","1hour"), D1("1d","day","1day"), W1("1w","week","1week");
	@Getter private final String interval;
	@Getter final String polyName;
	//1min, 3min, 5min, 15min, 30min, 1hour, 2hour, 4hour, 6hour, 8hour, 12hour, 1day, 1week
	@Getter final String kucoin;
	
	private static final PulseInterval lookup(String s) {
		if(s == null) { return null; }
		switch(s.toLowerCase()) {
		case "1s": return S1;
		case "1m": return M1;
		case "1h": return H1;
		case "":
		case "1d": return D1;
		case "1w": return W1;
		}
		return null;
	}

	// Each of those attempts to map from standard naming to particular data source.
	// If the string is not recognised it uses whatever the arg was to allow custom calls 
	//  e.g. BTCUSD_OHLC_7min - Should pass through
	
	public static String toKucoin(String s) {
		String t = s == null ? "" : s;
		PulseInterval pi = lookup(t);
		return pi == null ? t : pi.kucoin == null ? t : pi.kucoin;
	}
	
	public static String toPoly(String s) {
		String t = s == null ? "" : s;
		PulseInterval pi = lookup(t);
		return pi == null ? t : pi.polyName == null ? t : pi.polyName;
	}
	
	public static String toStandard(String s) {
		String t = s == null ? "" : s;
		PulseInterval pi = lookup(t);
		return pi == null ? t : pi.interval == null ? t : pi.interval;
	}
}