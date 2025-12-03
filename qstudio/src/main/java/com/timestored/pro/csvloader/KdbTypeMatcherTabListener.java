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
package com.timestored.pro.csvloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;



/**
 * Listening to all tab data keeps a record of what kdb column type
 * conversion would be most specifically suitable. 
 */
class KdbTypeMatcherTabListener implements TabListener {
	
	private static enum RECOGNISED_TYPES implements Comparable<RECOGNISED_TYPES> { 
		// IMPORTANT, order of declaration gives the priority order.
		// use LONG where possible before FLOAT
		EMPTY('*',"") { 
			@Override public boolean recognises(String s) {
				return s.trim().isEmpty(); 
			}
		},
		DATE('D', "") {
			@Override public boolean recognises(String s) {
				boolean match = false;
				String[] r = s.split("[\\/\\-\\.]");
				if(r.length == 3) {
					try {
						int a = Integer.parseInt(r[0]);
						int b = Integer.parseInt(r[1]);
						int c = Integer.parseInt(r[2]);
						match = (a>0 && a<=31 && b>0 && b<=31 && c>0 && c<=9999)
							|| (c>0 && c<=31 && b>0 && b<=31 && a>0 && a<=9999);
						
					} catch (NumberFormatException nfe) {
						// fall through
					}
				}
				return match;
			}
		},
		MINUTE('U', "^(?:[0-2])?[0-9]:[0-5][0-9]$"),
		SECOND('V', "^(?:[0-2])?[0-9]:[0-5][0-9](:[0-5][0-9])?$"),
		TIME('T', "^(?:[0-2])?[0-9]:[0-5][0-9](:[0-5][0-9](\\.?[0-9]*))??$"),
		LONG('J', "^-?\\d{1,19}$"),
		DOUBLE('F', "[-+]?[0-9]*\\.?[0-9]+(e[+-]?\\d+)?");

		private final String regex;
		private final char kdbTypeLetter; 

		private RECOGNISED_TYPES(char kdbTypeLetter, String regex) {
			this.kdbTypeLetter = kdbTypeLetter;
			this.regex = regex;
		}
		
		public boolean recognises(String s) {
			if(this.equals(DOUBLE) && !s.matches(regex)) {
				System.out.println(s);
			}
			return s.matches(regex);
		}
	};
	
	private HashMap<String, Set<RECOGNISED_TYPES>> colnameToTypeMatches = new HashMap<String, Set<RECOGNISED_TYPES>>();

	@Override public void tabEvent(String[] colNames, List<String[]> rowData) {
		for(int c=0; c<colNames.length; c++) {
			String cn = colNames[c];
			// first time seeing this column, add all possible matches
			if(!colnameToTypeMatches.containsKey(cn)) {
				EnumSet<RECOGNISED_TYPES> allPossible = EnumSet.allOf(KdbTypeMatcherTabListener.RECOGNISED_TYPES.class);
				colnameToTypeMatches.put(cn, allPossible);
			}
			
			Set<RECOGNISED_TYPES> possible = colnameToTypeMatches.get(cn);
			// if a single row not recognized, remove that type as its not possible
			for(RECOGNISED_TYPES rtype : possible) {
				for(int r=0; r<rowData.size(); r++) {
					String s = rowData.get(r)[c].trim();
					if(!s.isEmpty() && !rtype.recognises(s)) {
						possible.remove(rtype);
						break;
					}
				}
			}
			
		}
	}
	
	/**
	 * @return map from column names seen to the most specific kdb letter
	 * that that column could be safely converted to. Strings/* are not returned.
	 */
	public Map<String,Character> getRecognisedKdbTypes() {
		HashMap<String, Character> r = new HashMap<String,Character>();
		for(Entry<String, Set<RECOGNISED_TYPES>> e : colnameToTypeMatches.entrySet()) {
			if (!e.getValue().isEmpty()) {
				ArrayList<RECOGNISED_TYPES> types = new ArrayList<RECOGNISED_TYPES>(e.getValue());
				Collections.sort(types); // order gives priority
				if(types.get(0).kdbTypeLetter != '*') {
					r.put(e.getKey(), types.get(0).kdbTypeLetter);
				}
			}
		}
		return r;
	}
}