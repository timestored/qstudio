package com.timestored.pro.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.timestored.qdoc.DocSource;
import com.timestored.qdoc.DocumentedEntity;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.ServerObjectTree;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqlDocSource implements DocSource {

	private final AdminModel adminModel;
	private final Supplier<List<? extends DocumentedEntity>> docSupplier;

	@Override public List<? extends DocumentedEntity> getDocs() {
		List<DocumentedEntity> docs = new ArrayList<>(docSupplier.get());
		// Get the entities for just the currently connected server.
		ServerObjectTree selectedSeverTree = adminModel.getServerTree(adminModel.getSelectedServerName());
		if(selectedSeverTree != null) {
			docs.addAll(selectedSeverTree.getAllDocumentationEntities(null));
		}
		return ImmutableList.copyOf(docs).asList();
		
	}

}
