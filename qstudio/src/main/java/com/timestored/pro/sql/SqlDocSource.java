package com.timestored.pro.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.ImageIcon;

import com.google.common.collect.ImmutableList;
import com.timestored.babeldb.DBHelper;
import com.timestored.qdoc.DocSource;
import com.timestored.qdoc.DocumentedEntity;
import com.timestored.qstudio.QStudioModel;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.QueryResult;
import com.timestored.qstudio.model.ServerObjectTree;
import com.timestored.theme.Theme;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqlDocSource implements DocSource {

	private final QStudioModel qsm;
	private final Supplier<List<? extends DocumentedEntity>> docSupplier;

	@Override public List<? extends DocumentedEntity> getDocs() {
		List<DocumentedEntity> docs = new ArrayList<>(docSupplier.get());
		// Get the entities for just the currently connected server.
		AdminModel adminModel = qsm.getAdminModel();
		ServerObjectTree selectedSeverTree = adminModel.getServerTree(adminModel.getSelectedServerName());
		if(selectedSeverTree != null) {
			docs.addAll(selectedSeverTree.getAllDocumentationEntities(null));
		}
		try {
			QueryResult prevQR = qsm.getQueryManager().getPreviousSuccessfulQR();
			if(prevQR != null && prevQR.getRs() != null) {
				List<String> recentColumnNames = DBHelper.getColumnNames(prevQR.getRs().getMetaData());
				recentColumnNames.forEach(cn -> {
					docs.add(new ColumnDocumentedEntity(cn));
				});
			}
		} catch(Exception e) {} // Ignore
		return ImmutableList.copyOf(docs);
	}

	@Data public static class ColumnDocumentedEntity implements DocumentedEntity {
		private final String fullName;
		private final String source = "Latest Query Result";
		private final SourceType sourceType = DocumentedEntity.SourceType.SERVER;

		@Override public String getDocName() { return fullName; }

		@Override public String getHtmlDoc(boolean shortFormat) {
			return fullName;
		}
		
		@Override public ImageIcon getIcon() { 
			return Theme.CIcon.LAYOUT_ADD.get16(); 
		}

	}
}
