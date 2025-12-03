package com.timestored.qstudio;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.google.common.base.Preconditions;
import com.timestored.babeldb.BabelDBJdbcDriver;
import com.timestored.babeldb.Dbrunner;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.JdbcTypes;
import com.timestored.connections.ServerConfig;
import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.qstudio.model.AdminModel;
import com.timestored.qstudio.model.QueryManager;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
public class QStudioModel {

	public static final String APP_TITLE = ConnectionManager.APP_TITLE;
	public static final String LEGACY_FOLDER_NAME = "qStudio";
	// For legacy reasons this MUST be qStudio - with that casing so as not to lose work
	public static final File APP_HOME = new File(System.getProperty("user.home") + File.separator + LEGACY_FOLDER_NAME);
	static final File SCRATCH_DIR = new File(APP_HOME, "scratch");
	
	private final ConnectionManager connectionManager;
	private final OpenDocumentsModel openDocumentsModel; 
	private final AdminModel adminModel; 
	private final QueryManager queryManager; 
	private final Persistance persistance;
	@Getter private static final File LOCALDB_DIR = new File(APP_HOME, "qduckdb");
	private static final Logger LOG = Logger.getLogger(QStudioModel.class.getName());
	
	private int queryCount = 0;
	
	public QStudioModel(ConnectionManager connectionManager, Persistance persistance, OpenDocumentsModel openDocumentsModel) {
		this.connectionManager = Preconditions.checkNotNull(connectionManager);
		this.persistance = Preconditions.checkNotNull(persistance);
		this.openDocumentsModel = Preconditions.checkNotNull(openDocumentsModel);
        this.queryManager = new QueryManager(connectionManager);
        
		SCRATCH_DIR.mkdirs();

        // Babel needs some static init so that any new conns created are of specific types
        try { // make sure it's reged before Babel
			DriverManager.registerDriver(new org.h2.Driver());
		} catch (SQLException e) {
			LOG.warning("Couldn't reg H2");
		} 
		BabelDBJdbcDriver.setDEFAULT_DBRUNNER(new MyDbRunner(connectionManager));
		
        this.adminModel = new AdminModel(connectionManager, queryManager);
		queryCount = persistance.getInt(Persistance.Key.QUERY_COUNT, 0);
	}

	Language getCurrentSqlLanguage() {
		String serverName = queryManager.getSelectedServerName();
		ServerConfig sc = serverName == null ? null : connectionManager.getServer(serverName);
		Language fallbackLang = sc != null && sc.isKDB() ? Language.Q : Language.SQL;
		Language l = Language.getLanguage(openDocumentsModel.getSelectedDocument().getFileEnding());
		return (l.equals(Language.MARKDOWN) || l.equals(Language.OTHER)) ? fallbackLang : l;
	}

	String[] getFileEndings() {
    	String[] qFirst = new String[] {"q","sql","prql"};
    	String[] rflFirst = new String[] {"rfl","sql","prql"};
    	String[] sqlFirst = new String[] {"sql","q","prql"};
    	String[] prqlFirst = new String[] {"prql","sql","q"};
    	String[] anyFirst = new String[] {"","prql","sql","q"};
    	String[] dosFirst = new String[] {"dos","prql","sql","q"};
    	String title = openDocumentsModel.getSelectedDocument().getTitle();
    	if(title.endsWith(".q")) {
    		return qFirst;
    	} else if(title.endsWith(".sql")) {
    		return sqlFirst;
    	} else if(title.endsWith(".prql")) {
    		return prqlFirst;
    	} else if(title.endsWith(".rfl")) {
    		return rflFirst;
    	} else if(title.contains(".")) {
    		return anyFirst;
    	}
    	if(this.connectionManager.getServerConnections().isEmpty()) {
    		return qFirst;
    	}
    	ServerConfig sc = connectionManager.getServer(queryManager.getSelectedServerName());
    	return sc != null ? (JdbcTypes.DOLPHINDB.equals(sc.getJdbcType()) ? dosFirst : sc.isKDB() ? qFirst : sqlFirst) : qFirst;
	}

	private static final String MEMNAME = "QDUCKDB";
	private static final ServerConfig MEMDUCK = new ServerConfig("", 0, "", "", MEMNAME, JdbcTypes.BABELDB, null, "jdbc:babeldb:duckdb:");

	public BabelDBJdbcDriver putIfAbsentLocalSQL() {
		LOCALDB_DIR.mkdirs();
		if(connectionManager.getServer(MEMNAME) == null) {
			connectionManager.addServer(MEMDUCK);
		}
		return BabelDBJdbcDriver.getDriverIfExists(MEMDUCK.getUrl());
	}


	public void loadExistingParquet() throws SQLException {
		if(connectionManager.getServer(MEMNAME) == null) {
			return; // Do nothing
		}
		BabelDBJdbcDriver babelDuck = putIfAbsentLocalSQL();
		babelDuck.loadExistingParquet(LOCALDB_DIR);
	}
	
	
	void addDataFiles(List<File> dataFiles) {
		if(dataFiles.size() == 0) {
			return;
		}
		putIfAbsentLocalSQL();
		StringBuilder pubQry = new StringBuilder();
		pubQry.append("\n");
		String name = null;
		for(File dfile : dataFiles) {
			name = BabelDBJdbcDriver.getTblName(dfile);
			pubQry.append("SELECT * FROM " + name + " LIMIT 111000;\n");
		}
		pubQry.append("\n\n-- Press F7 on the comment line below to use AI to generate queries");
		pubQry.append("\n-- Generate 5 example queries for the table " + name + ". Include a simple select, a count and 3 more advanced queries.");
		
		queryManager.setSelectedServerName(MEMNAME);
		String docTitle = dataFiles.size() == 1 && name != null ? (name+".sql") : null; 
		Document selDoc = openDocumentsModel.addDocument(docTitle);
		openDocumentsModel.setSelectedDocument(selDoc);
		String code = BabelDBJdbcDriver.getReplaceView(dataFiles) + pubQry.toString();
		openDocumentsModel.insertSelectedText(code);
		queryManager.sendQuery(code);
		BackgroundExecutor.EXECUTOR.execute(() -> adminModel.refresh(connectionManager.getServer(MEMNAME)));
	}

	
	void addDBfiles(List<File> dbFiles) {
		if(dbFiles.size() == 0) {
			return;
		}
		ServerConfig sc = null;
		for(File dbFile : dbFiles) {
			String db = dbFile.getAbsolutePath();
			try {
				sc = ServerConfig.forFile(db);
				connectionManager.addServer(sc);
			} catch (IOException ioe) {
				String msg = Msg.get(Key.ERROR_SAVING) + ": " + db + "\r\n" + ioe.toString();
		        LOG.warning(msg);
		        JOptionPane.showMessageDialog(null, msg, "Error Adding Database from File " + db, JOptionPane.ERROR_MESSAGE);
			}
		}
		if(sc != null) {
			queryManager.setSelectedServerName(sc.getName());
			Document selDoc = openDocumentsModel.addDocument();
			openDocumentsModel.setSelectedDocument(selDoc);
			StringBuilder sb = new StringBuilder();
			sb.append(sc.getJdbcType().getComment() + " Database:" + sc.getShortName());
			sb.append("\n");
			sb.append("\n-- Press F7 on the comment line below to use AI to generate queries");
			sb.append("\n-- Generate 5 example queries. Include a simple select, a count and 3 more advanced queries.");
			String code = sb.toString();
			openDocumentsModel.insertSelectedText(code);
		}
	}
	
	
	/**
	 * Babel runs it's query_db through connectionManager as we don't ant each individual one logged to UI etc.
	 */
	@RequiredArgsConstructor
	private static class MyDbRunner implements Dbrunner {
		private final ConnectionManager connectionManager;

		@Override public ResultSet executeQry(String serverName, String sql, int millisStalenessPermitted) throws IOException {
			try {
				return connectionManager.executeQuery(connectionManager.getServer(serverName), sql);
			} catch (SQLException | IOException e) {
				throw new IOException(e);
			}
		}

		@Override public List<String> getServerWithSymbols() { return Collections.emptyList(); }
		@Override public ServerConfig getServer(String serverName) { return connectionManager.getServer(serverName); }
		@Override public boolean isEmpty() { return connectionManager.isEmpty(); }
		@Override public void close() { }
		
	}

	public File saveToQDuckDB(ResultSet rs, String name) throws SQLException {
		BabelDBJdbcDriver babelDuck = putIfAbsentLocalSQL();
		if(babelDuck != null) {
			babelDuck.dropCreatePopulate(rs, "exportTbl");
			File f = new File(LOCALDB_DIR, name+".parquet");
			babelDuck.run("COPY exportTbl TO '" + f.getAbsolutePath() + "' (FORMAT PARQUET);");
			babelDuck.run("CREATE OR REPLACE VIEW " + name + " AS SELECT * FROM read_parquet('" + f.getAbsolutePath() + "');");
			return f;
		}
		return null;
	}
}
