/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2024 TimeStored
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

package com.timestored.qdoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Data container that takes comment lines and converts them into structured data, with param
 * details, exception descriptions, etc.
 */
class ParsedComments {

	public final String docDescription;
	public final String returnDescription;
	
	public final Map<String, Map<String,String>> namedTags;
	public final Map<String,String> tags;
	
	private ParsedComments(List<String> comments) {
		
		tags = new HashMap<String, String>();
		namedTags = new HashMap<String, Map<String,String>>();

		Multimap<String, String> t = ArrayListMultimap.create();
		
		String doc = "";
		String ret = "";

		// if no tag found on current line, use previous tag. and append string.
		String prevTag = "";
		
		for(String c : comments) {
			
			if(c.startsWith("@")) {
				int p = firstWhitespace(c);
				String tag = p>-1 ? c.substring(1, p).trim() : c.substring(1).trim();
				String value = p>-1 ? c.substring(p + 1) : "";
				if(!isExampleTag(tag)) {
					value = value.trim();
				}
				if(!tag.isEmpty() && (!value.isEmpty() || isMarkerTag(tag) || isExampleTag(tag))) {
					t.put(tag, value);
					prevTag = tag;
				}
				
			} else if(prevTag.isEmpty()) { 
				// still in doc description
				if(isParagraphBreak(c)) {
					if(doc.length()>0 && !doc.endsWith("\n\n")) {
						doc += "\n\n";
					}
				} else if(doc.length()==0 || doc.endsWith("\n\n")) {
					doc += c;
				} else {
					doc = doc + " " + c;
				}
			} else {
				// append current line to previous tag
				assert(!prevTag.isEmpty());
				if(isExampleTag(prevTag) && isParagraphBreak(c)) {
					prevTag = "";
					if(doc.length()>0 && !doc.endsWith("\n\n")) {
						doc += "\n\n";
					}
					continue;
				}
				ArrayList<String> curVal = new ArrayList<String>(t.get(prevTag));
				int li = curVal.size()-1; // last index
				String separator = isExampleTag(prevTag) ? "\n" : " ";
				String current = curVal.get(li);
				curVal.set(li, current.isEmpty() ? c : current + separator + c);
				t.replaceValues(prevTag, curVal);
			}
		}
		
		// special case @param / @exception which we always want to force as named tags.
		addIfExists(t, ParsedQEntity.PARAM_TAG);
		addIfExists(t, ParsedQEntity.EXCEPTION_TAG);
		
		ret = toDescription(t.get(ParsedQEntity.RETURN_TAG));
		if(!ret.isEmpty()) {
			tags.put(ParsedQEntity.RETURN_TAG, ret);	
		}

		t.removeAll(ParsedQEntity.PARAM_TAG);
		t.removeAll(ParsedQEntity.EXCEPTION_TAG);
		t.removeAll(ParsedQEntity.RETURN_TAG);

		addExamples(t, ParsedQEntity.EXAMPLE_TAG);
		addExamples(t, "eg");
		
		for(Entry<String, Collection<String>> e : t.asMap().entrySet()) {
			if(e.getValue().size() > 1) {
				namedTags.put(e.getKey(), toMap(e.getValue()));
			} else {
				tags.put(e.getKey(), toDescription(e.getValue()));
			}
		}
		
		this.docDescription = doc;
		this.returnDescription = ret;
	}

	private boolean isParagraphBreak(String commentText) {
		String t = commentText == null ? "" : commentText.trim();
		return t.isEmpty() || t.equals(".");
	}

	private boolean isExampleTag(String tag) {
		return ParsedQEntity.EXAMPLE_TAG.equals(tag) || "eg".equalsIgnoreCase(tag);
	}

	private boolean isMarkerTag(String tag) {
		return ParsedQEntity.IGNORE_TAG.equals(tag);
	}

	private int firstWhitespace(String s) {
		for(int i=0; i<s.length(); i++) {
			if(Character.isWhitespace(s.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	private void addExamples(Multimap<String, String> t, String tag) {
		Collection<String> examples = t.get(tag);
		if(!examples.isEmpty()) {
			Map<String, String> m = Maps.newLinkedHashMap();
			for(String example : examples) {
				m.put(example, "");
			}
			namedTags.put(tag, m);
			t.removeAll(tag);
		}
	}

	private void addIfExists(Multimap<String, String> t, String tag) {
		Map<String, String> m = toMap(t.get(tag));
		if(m.size() > 0) {
			namedTags.put(tag, m);
		}
	}
	
	private String toDescription(Collection<String> lines) {
		return Joiner.on(" ").join(lines);
	}

	/**
	 * Convert strings to a map by taking the first whole word as its key. Ignore empty lines.
	 */
	private Map<String, String> toMap(Collection<String> lines) {
		
		Map<String, String> r = Maps.newHashMap();
		for(String s : lines) {
			if(!s.isEmpty()) {
				int p = s.indexOf(' ');
				String k = p>-1 ? s.substring(0, p) : s;
				String v = p>-1 ? s.substring(p).trim() : "";
				r.put(k, v);
			}
		}
		return r;
	}

	public static ParsedComments parse(List<String> comments) {
		return new ParsedComments(comments);
	}
}
