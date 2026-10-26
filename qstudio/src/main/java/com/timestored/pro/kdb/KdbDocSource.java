package com.timestored.pro.kdb;


import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.timestored.babeldb.DBHelper;
import com.timestored.pro.sql.SqlDocSource;
import com.timestored.qdoc.DocSource;
import com.timestored.qdoc.DocumentedEntity;
import com.timestored.qdoc.OpenDocumentsDocSource;
import com.timestored.qstudio.QStudioModel;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.QueryResult;
import com.timestored.qstudio.model.ServerObjectTree;

/**
 * Pulls together the various {@link DocSource}'s to provide all relevant
 * KDB related Docs. Uses both builtin function docs and those pulled
 * dynamically from servers where possible.
 */
class KdbDocSource implements DocSource {

	private final List<DocumentedEntity> staticDocs;
	private final QStudioModel qsm;
	private final AdminModel adminModel;
	private static Predicate<DocumentedEntity> excludeBuiltins;
	private final OpenDocumentsDocSource openDocsDS;
	
	static {
		excludeBuiltins = (DocumentedEntity de) -> {
		        return !de.getDocName().startsWith(".q.")
		        		&& !de.getDocName().startsWith(".Q.")
		        		&& !de.getDocName().startsWith(".h.")
		        		&& !de.getDocName().startsWith(".j.")
		        		&& !de.getDocName().startsWith(".o.");
			};
	}
	
	public KdbDocSource(QStudioModel qStudioModel) {
		this.qsm = qStudioModel;
		this.adminModel = qStudioModel.getAdminModel();
		this.openDocsDS = new OpenDocumentsDocSource(qStudioModel.getOpenDocumentsModel());
		
		List<DocumentedEntity> docs;
		docs = new ArrayList<DocumentedEntity>(Function.getKnownfunctions());
		docs.addAll(DotQ.getKnowndotq());
		docs.addAll(Dotz.getKnowndotz());
		docs.addAll(DotH.getKnowndoth());
		docs.addAll(DotJ.getKnowndotj());
		staticDocs = docs;
	}
	
	@Override public List<? extends DocumentedEntity> getDocs() {
		List<DocumentedEntity> docs = new ArrayList<>(staticDocs);
		// Get the entities for just the currently connected server.
		ServerObjectTree selectedSeverTree = adminModel.getServerTree(adminModel.getSelectedServerName());
		if(selectedSeverTree != null) {
			docs.addAll(selectedSeverTree.getAllDocumentationEntities(excludeBuiltins));
		}
		try {
			QueryResult prevQR = qsm.getQueryManager().getPreviousSuccessfulQR();
			if(prevQR != null && prevQR.getRs() != null) {
				List<String> recentColumnNames = DBHelper.getColumnNames(prevQR.getRs().getMetaData());
				recentColumnNames.forEach(cn -> {
					docs.add(new SqlDocSource.ColumnDocumentedEntity(cn));
				});
			}
		} catch(Exception e) {} // Ignore
		
		docs.addAll(openDocsDS.getDocs());
		return ImmutableList.copyOf(docs);
	}

}
