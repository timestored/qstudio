package com.timestored.qdoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.timestored.qdoc.DocumentedEntity.SourceType;
import com.timestored.qdoc.DocumentedMatcher.CustomAutocompleteHandler;

public class KdbCustomAutocompleter implements CustomAutocompleteHandler {

	@Override
	public List<DocumentedEntity> filterSortResults(List<DocumentedEntity> docs, String prefix) {

		Comparator<DocumentedEntity> comparator = (a,b) -> a.getDocName().compareTo(b.getDocName());
		List<DocumentedEntity> r = new ArrayList<>(docs);
		List<DocumentedEntity> deduped = new ArrayList<>();
		Collections.sort(r, comparator);
		for(int i = 0; i < r.size(); i++) {
			DocumentedEntity next = i < r.size()-1 ? r.get(i+1) : null;
			if(next != null && r.get(i).getDocName().equals(next.getDocName())) {
				List<DocumentedEntity> combin = new ArrayList<>(2);
				combin.add(r.get(i));
				while(next != null && r.get(i).getDocName().equals(next.getDocName())) {
					combin.add(next);
					i++;
					next = i < r.size()-1 ? r.get(i+1) : null;
				}
				deduped.add(new CombinedDocumentedEntity(combin));
			} else {
				deduped.add(r.get(i));
			}
		}
		Predicate<DocumentedEntity> isNS = de -> de.getDocName().startsWith(".") && de.getDocName().endsWith(".");
		boolean customHandling = ".".equals(prefix) || prefix.trim().length() == 0 
				|| deduped.stream().filter(isNS).count() > 1; 
		
		// Empty = show globals at topmost level
		// anything else with multiple NS, show namespaces at top.		
		if(prefix.length() == 0) {
			
		}
		if(customHandling) {
			List<DocumentedEntity> namespacesr = new ArrayList<>();
			List<DocumentedEntity> globals = new ArrayList<>();
			List<DocumentedEntity> other = new ArrayList<>();
			for(DocumentedEntity de : deduped) {
				if(de.getDocName().startsWith(".") && de.getDocName().endsWith(".")) {
					namespacesr.add(de);
				} else if(!de.getDocName().contains(".") && de.getSourceType().equals(SourceType.SERVER)){
					globals.add(de);
				} else {
					other.add(de);
				}
			}
			Collections.sort(namespacesr, comparator);
			Collections.sort(other, comparator);
			Collections.sort(globals, comparator);
			
			List<DocumentedEntity> l = null;
			if(prefix.trim().length() == 0) {
				l = globals;
				l.addAll(namespacesr);
			} else {
				l = namespacesr;
				l.addAll(globals);
			}
			l.addAll(other);
			return l; 
		}
		
		return deduped;
	}
	
}