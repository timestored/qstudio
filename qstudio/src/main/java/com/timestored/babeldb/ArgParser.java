package com.timestored.babeldb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Data;


public class ArgParser {
	private final String s;
	private final char[] ca;
	private int p = 0;
	List<String> l;
	
	@Data
	public static class ParseResult {
		private final List<String> args;
		private final String remainingCode;
	}
	
	private ArgParser(String argsPlusPossibleCode) {
		this.s = argsPlusPossibleCode;
		this.ca = s == null ? new char[0] : s.toCharArray(); 
	}
	
	public static ParseResult parse(String argsPlusPossibleCode) {
		String S = argsPlusPossibleCode.toUpperCase().trim();
		if(!S.startsWith("(") || !S.contains(")")) {
			throw new IllegalArgumentException();
		}
		return new ArgParser(argsPlusPossibleCode).run();
	}
	
	private ParseResult run() {
		if(ca.length == 0) {
			return new ParseResult(Collections.emptyList(),"");
		}

		skipWS();
		swallow('(');
		
		l = new ArrayList<>();
		skipWS();
		while(p < ca.length && ca[p] != ')') {
			acceptOneArg();
			skipWS();
			if(p >= ca.length || ca[p] != ',') {
				break;
			}
			p++;
			skipWS();
		}
		swallow(')');		
		String remainingCode = s.substring(p, s.length());
		return new ParseResult(l,remainingCode);
	}
	
	void swallow(char c) {
		if(ca[p] == c) {
			p++;
			return;
		}
		throw new IllegalStateException("char " + c + " not found");
	}
	
	void acceptOneArg() {
		char c = ca[p];
		int st = p;
		switch(c) {
			case '\'': p++; st=p; skipUntil('\''); l.add(s.substring(st, p)); p++; break;
			case '\"': p++; st=p; skipUntil('"'); l.add(s.substring(st, p)); p++;  break;
			case '(': p++; st=p; skipUntil(')'); l.add(s.substring(st, p)); p++;  break;
			case '{': p++; st=p; skipUntil('}'); l.add(s.substring(st, p)); p++;  break;
			default: st=p; while(p < ca.length && ca[p] != ',' && ca[p] != ')') { p++; } l.add(s.substring(st, p));  break; 
		}
	}
	
	boolean isWS() { return p < ca.length && (ca[p]=='\t' || ca[p]==' ' || ca[p]=='\r' || ca[p]=='\n'); }
	void skipWS() { while(p<ca.length && isWS()) { p++; } }

	void skipUntil(char end) {
		for(;p<ca.length;p++) {
			if(ca[p] == end && ca[p-1] != '\\') {
				return;
			}
		}
		throw new IllegalStateException("Ending not found: " + end);
	}
	
	static boolean isStarter(char[] ca, int p) {
		char prevc = p > 0 ? ca[p] : ' ';
		char c = ca[p];
		return (c == '(' || c == ')' || c == '\'' || c == '"') && prevc != '\\';
	}
}