package com.timestored.babeldb;

import lombok.Data;

@Data public class SymPart {
	public final String S;
	public final String SYM;
	public final String sym;
	public final String typ;
	public final String a3;
	
	public SymPart(String fullSymbol) {
		this.S = fullSymbol.toUpperCase().trim();

		String sym = fullSymbol.toLowerCase().trim();
		String typ = "";
		String a3 = "";
		int p = S.indexOf("_");

		if(p > 0) {
			String[] args = S.split("_");
			sym = args[0];
			typ = args.length > 1 ? args[1] : typ;
			a3 = args.length > 2 ? args[2] : a3;
		}
		this.SYM = sym.toUpperCase();
		this.sym = sym.toLowerCase();
		this.typ = typ.toLowerCase();
		this.a3 = a3;
	}
}