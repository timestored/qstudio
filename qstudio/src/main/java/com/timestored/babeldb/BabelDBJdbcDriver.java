package com.timestored.babeldb;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvException;
import com.timestored.babeldb.ArgParser.ParseResult;
import com.timestored.babeldb.JsonResultSetBuilder.SqlTypeMerger;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * H2 Database based driver, that detects what tables are being requested and refreshes them before running the sql.
 * Contains concept of SYMBOLS that underlying queries can contain, then when user query_sym - performs actual SQL
 */
public class BabelDBJdbcDriver extends BaseJdbcDriver implements AutoCloseable {

    private static Logger log = LoggerFactory.getLogger(BabelDBJdbcDriver.class);
	public static CopyOnWriteArrayList<SymbolDetails> SYMBOL_DETAILS = new CopyOnWriteArrayList<>();
	public static final String BABEL_SYMBOL_qry = "SELECT * FROM symbols;"; 
	public static final String BABEL_SYMBOL_QRY = "SELECT * FROM SYMBOLS;"; 
	public static final String QUERY_SYM = "QUERY_SYM";

	private final String jdbcURL;
	private final Connection memConn;
	private final Dbrunner dbrunner;
	private final SqlHandler sqlHandler;
	@Setter private static Dbrunner DEFAULT_DBRUNNER;
	public static final String BABEL_SYMBOL_JDBC_URL = "jdbc:babeldb:";
	public static final ConcurrentHashMap<String, BabelDBJdbcDriver> existingDrivers = new ConcurrentHashMap<>();
	
	static {
		 try {
			java.sql.DriverManager.registerDriver(new BabelDBJdbcDriver());
		} catch (SQLException e) {}
	}

	/** This constructor is only for registerDriver - It only need to implement acceptUrl **/
	public BabelDBJdbcDriver() throws SQLException {
		this(BABEL_SYMBOL_JDBC_URL, null); 
	}
	
	/**
	 * @return The driver if it already exists otherwise create it and return it.
	 */
	public static BabelDBJdbcDriver getDriverIfExists(String jdbcURL) {
		BabelDBJdbcDriver bab = existingDrivers.computeIfAbsent(jdbcURL, url -> {
			try {
				return new BabelDBJdbcDriver(url, null);
			} catch (SQLException e) {
				return null;
			}
		});
		return bab;
	}
	
	@Override public Connection connect(String jdbcURL, Properties p) throws SQLException {
		if(!acceptsURL(jdbcURL)) {
			return null;
		}
		// This is extremely weird as one babel instance may be asked for a connection for another.
		if(jdbcURL != null && jdbcURL.equals(this.jdbcURL)) {
			return super.connect(jdbcURL, p);
		}
		BabelDBJdbcDriver bab = getDriverIfExists(jdbcURL);
		if(bab == null) {
			throw new IllegalStateException("Problem connecting to babel");
		}
		return bab.connect(jdbcURL, p);
	}
	
	/**
	 * Very specifically create a new isntance of BabelDB. Rather than via generic JDBC URL.
	 * Useful to allow greater control and specify a very specific DBrunner rather than chancing the generic static provider.
	 */
	public BabelDBJdbcDriver(String jdbcURL, Dbrunner dbrunner) throws SQLException {
		super(BABEL_SYMBOL_JDBC_URL);
		this.jdbcURL = jdbcURL;
		if(jdbcURL != null && !jdbcURL.equals(BABEL_SYMBOL_JDBC_URL)) {
			String underlyingURL = jdbcURL.replace(BABEL_SYMBOL_JDBC_URL, "jdbc:");
			// Watch out for null case, when static init happens and calls DriverManager.registerDriver(new BabelDBJdbcDriver());
			memConn = DriverManager.getConnection(underlyingURL);
		} else {
			memConn = null;
		}
		this.sqlHandler = jdbcURL.contains(":duckdb:") ? DuckDbSqlHandler.INSTANCE : DefaultSqlHandler.INSTANCE;
		this.dbrunner = dbrunner == null ? DEFAULT_DBRUNNER : dbrunner;
	}

	public static String H2_PARAMS = ";CASE_INSENSITIVE_IDENTIFIERS=TRUE;TIME ZONE=UTC";
	
	public static BabelDBJdbcDriver standardH2(String uniqueName, Dbrunner dbrunner) throws SQLException {
		return new BabelDBJdbcDriver("jdbc:babeldb:h2:mem:" + uniqueName + H2_PARAMS, dbrunner);
	}

	public static BabelDBJdbcDriver standardQDuckdb(String uniqueName, Dbrunner dbrunner, File qduckdb) throws SQLException {
		BabelDBJdbcDriver babelDB =  new BabelDBJdbcDriver("jdbc:babeldb:duckdb:", dbrunner);
		if(qduckdb != null) {
			babelDB.loadExistingParquet(qduckdb);
		}
		return babelDB;
	}

	@Override public void close() throws IOException {
		if(memConn != null) { 
			try { memConn.close(); } catch (SQLException e) {} 
		}
		
	}

	public static void main(String... args) throws ClassNotFoundException, SQLException {
		try (Connection conn = DriverManager.getConnection(BABEL_SYMBOL_JDBC_URL, "bob", "pass")) {
			System.out.println("Connecting to database...");
			try(Statement stmt = conn.createStatement()) {
				try(ResultSet rs = stmt.executeQuery("https://api.github.com/search/repositories?q=more+useful+keyboard")) {
					while (rs.next()) {
						System.out.print(" a: " + rs.getString("name"));
						System.out.println(" s: " + rs.getString("fullName"));
					}
				}
				try(ResultSet rs = stmt.executeQuery("SELECT * FROM binance.bookTicker")) {
					System.out.println(DBHelper.toString(rs, true));
				}
			}
			
		}
	}

	private static abstract class SqlHandler {
		public abstract String getTime(ResultSet rs, int columnPosition) throws SQLException;
		public abstract String getArray(Object o) throws SQLException;

		String arrItemToString(Object obj) {
			if(obj == null) {
				return "NULL";
			}
			if(obj instanceof Boolean) {
				return ((Boolean)obj).toString();
			}
			
			String s =  obj instanceof String ? ((String)obj) : 
					(obj instanceof char[] ? new String((char[])obj) : obj.toString());
			return "'" + s.replace("'", "''") + "'";
		}
		
		String toArray(Object o, String leftSt, String rightSt) throws SQLException {
			StringBuilder sb = new StringBuilder(leftSt);
			if(o == null) {
				return null;
			} else if(o != null && o.getClass().isArray()) {
				int n = Array.getLength(o);
				if(n > 0) {
					sb.append(arrItemToString(Array.get(o, 0)));
				}
				for(int i=1; i < n; i++) {
					sb.append(",").append(arrItemToString(Array.get(o, i)));
				}
				sb.append(rightSt);
				return sb.toString();
			}
			throw new UnsupportedOperationException("Array not recognised: " + o.toString());
		}
		abstract String map(String ct, Object firstVal);
		String adaptTimestamp(String timestampString) {
			return timestampString.replace("Z", "");
		}
		static JDBCType javaObjectToSqlTypeName(Object o) {
			if(o instanceof int[]) {
				return JDBCType.INTEGER;
			} else if(o instanceof long[]) {
				return JDBCType.BIGINT;
			} else if(o instanceof float[]) {
				return JDBCType.FLOAT;
			} else if(o instanceof double[]) {
				return JDBCType.DOUBLE;
			} else if(o instanceof boolean[]) {
				return JDBCType.BIT;
			} else if(o instanceof java.sql.Date[] || o instanceof java.util.Date[]) {
				return JDBCType.DATE;
			}
			return JDBCType.VARCHAR;
		}
	}

	private static class DefaultSqlHandler extends SqlHandler {
		public static final SqlHandler INSTANCE = new DefaultSqlHandler();
		@Override public String getTime(ResultSet rs, int columnPosition) throws SQLException {
			Object o = rs.getObject(columnPosition);
			return (o == null || rs.wasNull()) ? "NULL" : "'" + o.toString().replace("Z", "") + "'"; 
		}
		@Override public String getArray(Object o) throws SQLException {
			return toArray(o, "array[", "]");
		}
		@Override public String map(String ct, Object firstVal) {
			return ct != null && ct.equals("ARRAY") ? (javaObjectToSqlTypeName(firstVal).getName()+" ARRAY") : 
				firstVal instanceof UUID ? "UUID" : ct;
		}
	}
	
	private static class DuckDbSqlHandler extends SqlHandler {
		public static final SqlHandler INSTANCE = new DuckDbSqlHandler();
		@Override public String getTime(ResultSet rs, int columnPosition) throws SQLException {
			Object o = rs.getObject(columnPosition);
			if(o == null || rs.wasNull()) {
				return "NULL";
			}
			String os = o.toString().trim();
			if(os.length()==5 && os.charAt(2) == ':') {
				os = os + ":00";
			}
			return "'" + os + "'"; 
		}
		@Override public String getArray(Object o) throws SQLException {
			return toArray(o, "list_value(", ")");
		}
		@Override public String map(String ct, Object firstVal) {
			return ct != null && ct.equals("ARRAY") ? (javaObjectToSqlTypeName(firstVal).getName()+"[]") : 
				firstVal instanceof UUID ? "UUID" : ct;
		}
		public String adaptTimestamp(String timestampString) {
			String s = timestampString.replace("T", " ");
			if(s.length() == "2000-01-01T12:00".length() && s.charAt(10) == ' ' && s.charAt(13) == ':') {
				return s + ":00"; // DuckDB needs seconds every time 
			}
			return s.replace("Z", "");
		}
		
		@Override String arrItemToString(Object obj) {
			String s = super.arrItemToString(obj);
			if(obj instanceof Float) {
				return s + "::FLOAT";
			} else if(obj instanceof Double) {
				return s + "::DOUBLE";
			}
			return s;
		}
	}
	
	@Data
	public 
	static class QueryTranslation {
		private final String originalQuery;
		private final String translatedQuery;
		private final List<List<String>> cmdWithArgs;
		
		public static QueryTranslation withOneCommand(String originalQuery, List<String> cmdWithArgs) {
			List<List<String>> ll = Lists.newArrayList();
			ll.add(cmdWithArgs);
			return new QueryTranslation(originalQuery, null, ll);
		}
	}


	private final static String TMPNAME = "temptbl";
	private static final String QRU = "QUERY_";
	private static final String WEBERROR = "Unable to fetch data from WWW. Have you lost internet or been firewalled.";
	
	private static QueryTranslation translateFromsToSym(QueryTranslation qt) throws IOException, SQLException {
		final String sql = qt.getTranslatedQuery();
		final String SQL = sql.toUpperCase();
		String FR = " FROM ";
		if(!SQL.contains(FR)) {
			return qt;
		}
		List<List<String>> commands = new ArrayList<>(qt.getCmdWithArgs());

		StringBuilder sb = new StringBuilder();
		char[] ca = sql.toCharArray();
		for(int i=0; i<ca.length; i++) {
			if(U(ca[i])=='F' && U(ca[i+1])=='R' && U(ca[i+2])=='O' && U(ca[i+3])=='M' && ca[i+4]==' ') {
				sb.append(ca[i++]); sb.append(ca[i++]); sb.append(ca[i++]); sb.append(ca[i++]); sb.append(ca[i++]);
				while(i<ca.length && ca[i]==' ') { sb.append(ca[i++]); } // swallow spaces
				StringBuilder idSB = new StringBuilder(10);
				while(i<ca.length && isID(ca[i])) { idSB.append(ca[i++]); } // swallow ID
				String id = idSB.toString();
				// can only recognise symbols with a prefix as otherwise this logic gets confused by 'QUERY_DB("","SELECT * FROM actual_table")
				if(id.length()>2 && id.toUpperCase().startsWith("S_")) {
					id = id.substring(2);
					sb.append(id);
					commands.add(Lists.newArrayList(id, "sym", id));
				} else {
					sb.append(id);
				}
				if(i<ca.length) { sb.append(ca[i]); } 
			} else {
				sb.append(ca[i]);
			}
		}
		return new QueryTranslation(sql, sb.toString(), commands);
	}
	private static char U(char u) { return Character.toUpperCase(u); }
	private static boolean isID(char u) { return Character.isAlphabetic(u) || Character.isDigit(u) || u == '_' || u == ':' || u == '.'; }

	private static QueryTranslation translateQueryUnderscore(QueryTranslation qt) throws IOException, SQLException {
		final String sql = qt.getTranslatedQuery();
		final String SQL = sql.toUpperCase();
		if(!SQL.contains(QRU)) {
			return qt;
		}
		List<List<String>> commands = new ArrayList<>(qt.getCmdWithArgs());
		StringBuilder sb = new StringBuilder();
		String[] queries = SQL.split(QRU);
		int start = 0;
		for(int i=0; i<queries.length; i++) {
			String S = queries[i];
			String s = sql.substring(start, start + S.length());
			start += (S.length() + QRU.length());
			if(i == 0) {
				sb.append(sql.substring(0, S.length()));
			} else {
				int p = s.indexOf("(");
				if(p < 0) { throw new IllegalStateException("( missing from query_"); }
				ParseResult parseRes = ArgParser.parse(s.substring(p));
				String cmd = s.substring(0,p);
				String tname = TMPNAME + commands.size();
				if(cmd.equals("sym")) {
					List<String> symArgs = parseRes.getArgs();
					for(int j=0; j<symArgs.size(); j++) {
						tname = symArgs.get(j).replace(":","_").replace("-","_").replace(".","_");
						commands.add(Lists.newArrayList(tname, cmd, symArgs.get(j)));
						sb.append(tname + parseRes.getRemainingCode());
					}
				} else {
					List<String> cmdArgs = new ArrayList<>(5);
					cmdArgs.add(tname);
					cmdArgs.add(cmd);
					cmdArgs.addAll(parseRes.getArgs());
					commands.add(cmdArgs);
					sb.append(tname + parseRes.getRemainingCode());
				}
			}
		}
		return new QueryTranslation(sql, sb.toString(), commands);
	}
	
	public static QueryTranslation translateQry(String sqlToRun) throws IOException, SQLException {
		final String q = sqlToRun.trim();
		final String SQL = q.toUpperCase().trim();
		if(SQL.length() == 0) {
			return new QueryTranslation(q, null, Collections.emptyList());
		}
		if(SQL.startsWith("HTTP:") || SQL.startsWith("HTTPS:")) {
			return QueryTranslation.withOneCommand(q, Lists.newArrayList("", "web", q));
		}
		if(!SQL.contains(" ") && SQL.matches("^[a-zA-Z0-9_:\\.\\-]*$")) {
			return QueryTranslation.withOneCommand(q, Lists.newArrayList("", "sym", q));
		}
		QueryTranslation qt = new QueryTranslation(sqlToRun, q, Collections.emptyList());
		qt = translateFromsToSym(qt);
		qt = translateQueryUnderscore(qt);
		return qt;
	}

	private static <T> List<T> dropFirst(int n, List<T> list) {
		List<T> r = new ArrayList<>(list.size() - n);
		for(int i=n; i<list.size(); i++) {
			r.add(list.get(i));
		}
		return r;
	}
	
	public ResultSet executeQryForTestsOnly(String sqlToRun) throws IOException {
		try {
			ResultSet rs = executeQry(sqlToRun, 0);
			return DBHelper.toCRS(rs);
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	
	@Override public ResultSet executeQry(String sqlToRun, int millisStalenessPermitted) throws IOException, SQLException {
		QueryTranslation qt = translateQry(sqlToRun);
		List<List<String>> commands = qt.getCmdWithArgs();
		
		if(sqlToRun.equals("SYMBOLS") || sqlToRun.equalsIgnoreCase(BABEL_SYMBOL_QRY)) {
			return getSymbolDetailsRS();
		}
		// No need to create temporary table for single select.
		if(commands.size() == 1) {
			List<String> cmdArg = commands.get(0);
			Preconditions.checkArgument(cmdArg.size() >= 3);
			// no translation or translation is exact table name
			if(qt.getTranslatedQuery() == null || qt.getTranslatedQuery().equals(cmdArg.get(0))) {
				return run(cmdArg.get(1), dropFirst(2,cmdArg), millisStalenessPermitted);
			}
		}
		// TODO check for basic select *
		//  String starQ = "SELECT * FROM "+TMPNAME.toUpperCase();
		
		for(int i=0; i<commands.size(); i++) {
			List<String> cmdArg = commands.get(i);
			ResultSet rs = run(cmdArg.get(1), dropFirst(2,cmdArg), millisStalenessPermitted);
			dropCreatePopulate(rs, cmdArg.get(0));
		}
		return underlyingQuery(qt.getTranslatedQuery());
	}

	private static ResultSet getSymbolDetailsRS() {
		String[] symbols = new String[SYMBOL_DETAILS.size()];
		String[] dbs = new String[SYMBOL_DETAILS.size()];
		String[] query = new String[SYMBOL_DETAILS.size()];
		String[] title = new String[SYMBOL_DETAILS.size()];
		String[] description = new String[SYMBOL_DETAILS.size()];
		for(int i=0; i < SYMBOL_DETAILS.size(); i++) {
			SymbolDetails sd = SYMBOL_DETAILS.get(i);
			symbols[i] = sd.symbol;
			dbs[i] = sd.database;
			query[i] = sd.query;
			title[i] = sd.title;
			description[i] = sd.description;
		}
		String[] cns = new String[] { "symbol","database","query","title","description" };
		return new SimpleResultSet(cns, new Object[] { symbols, dbs, query, title, description });
	}
	
	private ResultSet run(String cmd, List<String> cmdArgs, int millisStalenessPermitted) throws IOException {
		switch(cmd.toLowerCase()) {
		case "web": return fetch(Joiner.on("#").join(cmdArgs));
		case "data": 
			if(cmdArgs.size() == 1) { return fromTxt(cmdArgs.get(0)); };
			if(cmdArgs.size() == 2) { return fromTxt(cmdArgs.get(0),cmdArgs.get(1), "", false); };
			if(cmdArgs.size() == 3) { return fromTxt(cmdArgs.get(0),cmdArgs.get(1),cmdArgs.get(2), false); };
			throw new UnsupportedOperationException("query_data takes 1-3 arg");
		case "sym": 
			String arg0 = cmdArgs.get(0);
			if(cmdArgs.size() != 1) { throw new UnsupportedOperationException("query_sym takes 1 arg"); };
			SymbolDetails sq = getSymbolQuery(arg0);
			if(sq.getDatabase() != null) {
				return dbrunner.executeQry(sq.getDatabase(), sq.getQueryToRun(), millisStalenessPermitted);
			}
			int p = arg0.indexOf(':');
			if(p > -1) {
				String inferredDb = arg0.substring(p+1).toUpperCase();
				return dbrunner.executeQry(inferredDb, arg0.substring(0,p), millisStalenessPermitted);
			}
			throw new UnsupportedOperationException("Couldn't find symbol = " + arg0); 
		case "symbols": 
			if(cmdArgs.size() != 1) { throw new UnsupportedOperationException("query_symbols takes 1 arg"); };
			List<SymbolDetails> sl = searchSymbols(cmdArgs.get(0));
			return toRS(sl);
		case "db": 
			if(cmdArgs.size() != 2 && cmdArgs.size() != 3) { throw new UnsupportedOperationException("query_db takes 2 or 3 args"); };
			if(dbrunner == null) { throw new UnsupportedOperationException("dbrunner not configued"); };
			String targetDB = cmdArgs.get(0);
			
			if(dbrunner.getServer(targetDB) == null) {
				throw new UnsupportedOperationException("Database '" + targetDB + "' doesn't exist");
			}
			int millis = millisStalenessPermitted;
			if(cmdArgs.size() >= 3) {
				millis = Integer.parseInt(cmdArgs.get(2)); 
			}
			return dbrunner.executeQry(targetDB, cmdArgs.get(1), millis);
		default: throw new UnsupportedOperationException(QRU + cmd);
		}
	}
	

	public void dropCreatePopulate(ResultSet rs, String fullTblName) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("BEGIN TRANSACTION;\r\n");
		sb.append("DROP TABLE IF EXISTS " + fullTblName + ";\r\n");
		sb.append("CREATE TABLE " + fullTblName + getCreate(rs, sqlHandler));
		appendTableInserts(sb, fullTblName, rs, sqlHandler);
		sb.append("\r\nCOMMIT;");
		run(sb.toString());
	}
	
	public boolean run(String sql) throws SQLException {
        log.debug(sql);
		return memConn.createStatement().execute(sql); 
	};
	
	ResultSet underlyingQuery(String sql) throws SQLException {
		Statement st = memConn.createStatement();
        log.info(sql);
		st.execute(sql);
		ResultSet rs = st.getResultSet();
		if(rs == null) {
			rs = new SimpleResultSet(new String[] {"res"}, new Object[] {new String[] { "Ran " + sql}});
		}
		return rs; 
	};

	@Override public int getMajorVersion() { return 0; }
	@Override public int getMinorVersion() { return 0; }

	@Override public DatabaseMetaData getDatabaseMetaData(Connection conn) {
		return new SimpleDatabaseMetaData(conn, getMajorVersion(), getMinorVersion()) {
			public ResultSet getColumns(String cat,String schema,String table,String colPattern)throws SQLException{ 
				return memConn.getMetaData().getColumns(cat, schema, table, colPattern);
			}
		};
	}
	
	

	
	private static int appendTableInserts(StringBuilder sb, String tableName, ResultSet rs, SqlHandler sqlHandler) throws SQLException {
		int rows = 0;
		ResultSetMetaData rsmd = rs.getMetaData();
		int cn = rsmd.getColumnCount();
		sb.append("INSERT INTO " + tableName + " VALUES");
		while (rs.next()) {
			if(rows > 0) {
				sb.append(",");
			}
			sb.append("(");
		    for (int i = 1; i <= cn; i++) {
		        int ct = rsmd.getColumnType(i);
		        Object o = null;
		        switch(ct) {
                    case java.sql.Types.CHAR:
                    case java.sql.Types.VARCHAR:
                    case java.sql.Types.NCHAR:
                    case java.sql.Types.NVARCHAR:
                    case java.sql.Types.LONGNVARCHAR:
                    	String d = rs.getString(i);
	                    if(d == null || rs.wasNull()) {
	                    	sb.append("NULL");
	                    } else {
	                    	sb.append("'" + d.replace("'", "''") + "'");
	                    }
                    	break;
                    case java.sql.Types.BIGINT: 
                    case java.sql.Types.BOOLEAN: 
                    	o = rs.getObject(i);
                    	sb.append(rs.wasNull() ? "NULL" : o); break;
//	                    case(java.sql.Types.CHAR): appender.append((char[]) rs.getObject(i)); break;
	                case java.sql.Types.DATE: 
			        	o = rs.getObject(i);
	                	if(o == null || rs.wasNull()) {
		                	sb.append("NULL"); 
	                	} else {
		                	String ds = null;
		                	if(o instanceof java.sql.Date) {
			                	ds = new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date) o);
				        	} else if(o instanceof java.util.Date) {
			                	ds = new SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) o);
				        	} else if(o instanceof LocalDate) {
				        		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			                	ds = formatter.format((LocalDate) o);
				        	} else {
	                        	log.warn("Warning unsupported type = " + ct + " -> " + o);
	                        	// Should I just pass the string rather than null?
				        	}
		                	sb.append(ds == null ? "NULL" : "'"+ds+"'"); 
	                	}
//			        	else if(o instanceof LocalTime) {
//			        		epoch = toEpochSecond((LocalTime) o);
//			        	} else if(o instanceof LocalDateTime) {
//			        		epoch = ((LocalDateTime)o).toInstant(ZoneOffset.UTC).toEpochMilli();
//			        	} else if(o instanceof Instant) {
//			        		epoch = ((Instant)o).toEpochMilli();
//			        	}
	                	break;
	                	
                    case Types.DOUBLE: 
                    case Types.FLOAT: // some dbs say float/double and return float or double mismatched.  
                    	o = rs.getObject(i);
                    	if(rs.wasNull() || o == null) {
                    		sb.append("NULL");
                    	} else if(o instanceof Double) {
                    		sb.append(Double.isNaN((Double)o) ? "NULL" : (double)o);
                    	} else if(o instanceof Float) {
                    		sb.append(Float.isNaN((Float)o) ? "NULL" : (float)o);
                    	}
                    	break;
                    case Types.INTEGER: 
//                  case Types.LONGVARBINARY: appender.append((byte[]) rs.getObject(i)); break;
                    case Types.REAL:
                    case Types.SMALLINT:
                    case Types.TINYINT:
                    case Types.NUMERIC:
                    case Types.DECIMAL:
            		case Types.BINARY:
                    	o = rs.getObject(i); 
                    	sb.append((rs.wasNull() || o == null) ? "NULL" : o); 
                    	break;
                    case java.sql.Types.BIT: sb.append((boolean) rs.getObject(i)); break;

            		case Types.TIME:
            			sb.append(sqlHandler.getTime(rs, i));
            			break;
	                case Types.TIMESTAMP: 
            		case Types.TIME_WITH_TIMEZONE:     
            		case Types.TIMESTAMP_WITH_TIMEZONE:
            			o = rs.getObject(i);
            			if(rs.wasNull() || o == null) {
            				sb.append("NULL");
            			} else {
            				String os = o.toString();
            				sb.append("'").append(sqlHandler.adaptTimestamp(os)).append("'"); 
            			}
            			break;
            		case Types.ARRAY:
            			o = rs.getObject(i);
            			if(o == null || rs.wasNull()) {
            				sb.append("NULL");
            			} else if(o instanceof String) {
            				sb.append("'" + ((String)o).replace("'", "''") + "'"); // kdb char array :(
            			} else {
            				sb.append(sqlHandler.getArray(o));
            			}
            			break;
                    default:
                    	o = rs.getObject(i);
                    	if(!(o instanceof UUID)) {
                        	log.warn("Warning unsupported type = " + ct + " -> " + o);	
                    	}
            			sb.append("'").append(o).append("'"); 
                    	break;
                    	
		        }
		        if(i != cn) {
		        	sb.append(", ");
		        }
		    }
			rows++;
			sb.append(")");
        }
		sb.append(";");
		return rows;
	}	

	public static String getCreate(String[] colNames, int[] colSqlTypes, Object[] firstVals, SqlHandler sqlHandler) throws SQLException {
		int columnCount = colNames.length;
		StringBuilder sb = new StringBuilder(columnCount * 30);
		if ( columnCount > 0 ) { 
		    sb.append( " ( " );
		}
		for(int i=0; i < columnCount; i++ ) {
		    if ( i > 0 ) sb.append( ", " );
		    String columnName = colNames[i];
		    int columnType = colSqlTypes[i];
		    String ct = "VARCHAR";
		    ct = JDBCType.valueOf(columnType).getName();
		    if(ct.equals("ARRAY")) {
		    	if(firstVals[i] instanceof String) {
		    		ct = "VARCHAR"; // kdb sends char arrays, we convert to strings
		    	}
		    }
		    String typ = sqlHandler.map(ct, firstVals[i]);
			typ = typ == null ? "VARCHAR" : typ;
		    sb.append("\"").append( columnName ).append("\"").append( " " ).append(typ);
		}
		sb.append( " );" );
		return sb.toString();
	}
	
	/** Get CREATE assuming you have a kdb jdbc table */
	public static String getCreate(ResultSet rs, SqlHandler sqlHandler) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		String[] colNames = new String[columnCount];
		int[] colSqlTypes = new int[columnCount];
		Object[] firstVals = new Object[columnCount];
		rs.beforeFirst();
		if(rs.next()) {
			for ( int i = 1; i <= columnCount; i++ ) {
				firstVals[i-1] = rs.getObject(i);
			}
		}
		rs.beforeFirst();
		
		for ( int i = 1; i <= columnCount; i++ ) {
			colNames[i-1] = rsmd.getColumnLabel(i);
			colSqlTypes[i-1] = rsmd.getColumnType(i);
	    }
		return getCreate(colNames, colSqlTypes, firstVals, sqlHandler);
	}

	/**
	 * @return Mapping from column name (mixed case) to SQL type integer.
	 */
	public static Map<String, Integer> getColTypeMap(String columns) throws JsonMappingException, JsonProcessingException {
		ObjectMapper objectMapper = JsonResultSetBuilder.getObjectMapper();
		if(!columns.isEmpty()) {
			TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
			Map<String, String> colTypeMap = objectMapper.readValue(columns, typeRef);

			final Map<String,Integer> cTypes = new HashMap<>();
			colTypeMap.forEach((String typKey, String typVal) -> {
				// User specified overrides of column types.
				String t = typVal.toUpperCase();
				int typ = t.equals("DOUBLE") ? java.sql.Types.DOUBLE : 
							t.equals("BOOLEAN") ? java.sql.Types.BOOLEAN :
								t.equals("INTEGER") ? java.sql.Types.INTEGER :
								t.equals("TIMESTAMP") ? java.sql.Types.TIMESTAMP :
							java.sql.Types.VARCHAR;
				cTypes.put(typKey, typ);
			});
			return cTypes;
		}
		return Collections.emptyMap();
	}
	
	private static int countOccurrences(String s, char c) {
		if(s == null) { return 0; }
		int count = 0;
        for (int i = 0; i < s.length(); i++) {
        	if(s.charAt(i) == c) {
        		count++;
        	}
        }
        return count;
	}

	private static int countOccurrences(String text, String find) {
        int index = 0, count = 0, length = find.length();
        while( (index = text.indexOf(find, index)) != -1 ) {                
                index += length; count++;
        }
        return count;
	}
	
	/**
	 * @param urlWithPathOrTypes e.g.  https://.../bookTicker#.results#{symbol: 'VARCHAR', price: 'DOUBLE'}
	 */
	public static SimpleResultSet fetch(String urlWithPathOrTypes) throws IOException {
//		if(IexWebFetcher.isOptimizedToHandle(urlWithPathOrTypes)) {
//			return IexWebFetcher.executeQry(urlWithPathOrTypes);
//		}
		log.info("fetch:" + urlWithPathOrTypes);
		String[] args = urlWithPathOrTypes.split("\\#");
		String url = args[0].trim();
		String path = args.length > 1 ? args[1] : "";
		String colTypeTxt = args.length > 2 ? args[2] : "";
		String urlTxt = Curler.fetchURL(url, "GET", null);
		if(urlTxt == null) {
			throw new IOException(WEBERROR);
		}
		boolean forceCSV = url.toLowerCase().contains(".csv");
		// both CSV and json should return String[][] and then we cast to simpleresultSet to make conversion exact same.
		SimpleResultSet srs = fromTxt(urlTxt, path, colTypeTxt, forceCSV);
		return srs;
	}

	public static JsonNode toJSON(String fullUrl, String method) throws IOException {
		String urlTxt = Curler.fetchURL(fullUrl, method, null);
		if(urlTxt == null) {
			throw new IOException(WEBERROR);
		}
    	JsonMapper jsonMapper = JsonResultSetBuilder.getObjectMapper();
		return jsonMapper.readTree(urlTxt);
	}

	public static JsonNode toJSON(String fullUrl) throws IOException {
		return toJSON(fullUrl, "GET");
	}

	public static SimpleResultSet fromTxt(String txt) throws IOException {
		try {
			return fromTxt(txt, "", "", false);
		} catch(JsonProcessingException e) {
			throw new IOException(e);
		}
	}
			
	
	public static SimpleResultSet fromTxt(String txtArg, String path, String colTypeTxt, boolean forceCSV) throws JsonMappingException, JsonProcessingException, IOException {
		String txt = txtArg.trim() + "\r\n"; // csv/json can have any amount of spacing before or after, we standardise it here.
		SimpleResultSet srs = null;
		Map<String, Integer> colTypes = getColTypeMap(colTypeTxt);
		int curlies = countOccurrences(txt, '}');
		boolean manyCommas = countOccurrences(txt, ',') > 2 * curlies;
		int TRcount = countOccurrences(txt, "<tr");
		if(txt.startsWith("{") || txt.startsWith("[")) {
			srs = JsonResultSetBuilder.fromJSON(txt, path);	
		} else if(TRcount > 3 && txt.contains("<table")) {
			List<String[]> sa = HtmlToCsvConverter.toStringArray(txt);
			try {
				srs = fromStringArray(sa, Collections.emptyMap(), true);
			} catch (IOException | CsvException e) {
				throw new IOException(e);
			}
		} else if (forceCSV  || manyCommas || curlies == 0) {
			try {
				srs = fromCSV(txt, colTypes);
			} catch (CsvException e) {
				throw new IOException(e);
			}
		} else {
			srs = JsonResultSetBuilder.fromJSON(txt, path);	
		}
		srs = srs.castTypes(colTypes, true);
		return srs;
	}

	private static int countChar(String s, IntFunction<Boolean> f) {
		char[] ca = s.toCharArray();
		int c = 0;
		for(int i=0; i<ca.length; i++) {
			if(f.apply(ca[i])) {
				c++;
			}
		}
		return c;
	}


	private static char findMostPopularSeparator(String s) {
		char[] chars = { ',', ';', '\t', '|' };
		int[] count = new int[4];
		char[] ca = s.toCharArray();
		int c = 0;
		for(int i=0; i<ca.length; i++) {
			switch(ca[i]) {
			case ',': count[0]++; break;
			case ';': count[1]++; break;
			case '\t':count[2]++; break;
			case '|': count[3]++; break;
			}
		}
		int maxPos = 0;
		for(int i=1; i<count.length; i++) {
			if(count[i] > count[maxPos]) {
				maxPos = i;
			}
		}
		return chars[maxPos];
	}
	
	private static SimpleResultSet fromCSV(String urlTxt, Map<String, Integer> colTypes) throws IOException, CsvException {
		int p = urlTxt.indexOf("\n");
		char sep = findMostPopularSeparator(urlTxt);
		if(p > 0) {
			String headerTxt = urlTxt.substring(0, p);
			boolean header = true; //!headerTxt.contains(".");
			int q = urlTxt.indexOf("\n",p+1);
			if(q > 0) {
				String secondTxt = urlTxt.substring(p, q);
				double headerRatio = countChar(headerTxt,Character::isAlphabetic) / (1.0+countChar(headerTxt,i -> i == '.'));
				double secondRatio = countChar(secondTxt,Character::isAlphabetic) / (1.0+countChar(secondTxt,i -> i == '.'));
				int headerDigs  = countChar(headerTxt,Character::isDigit);
				int secondDigs  = countChar(secondTxt,Character::isDigit);
				if(Math.abs(headerRatio - secondRatio) < 0.05 && Math.abs(headerDigs - secondDigs) < 10) {
					header = false; // Only if very similar assume there are no headers
				}
			}
			ICSVParser csvParser = new CSVParserBuilder().withSeparator(sep).build();
			CSVReader reader  = new CSVReaderBuilder(new StringReader(urlTxt)).withCSVParser(csvParser ).build();
			List<String[]> csvData = reader.readAll();
			return fromStringArray(csvData, colTypes, header);
		}
		throw new IllegalArgumentException("No newline found.");
	}

	static SimpleResultSet fromStringArray(List<String[]> csvData, Map<String, Integer> colTypes, boolean header)
			throws IOException, CsvException {
		
		Map<String, Integer> cTypes = new HashMap<>();
		// Now convert types - see similarity to {@link JsonResultSetBuilder#fromArrayOfObjectNodes}
		List<String> cNames = new ArrayList<>();
		for(int i=0; i<csvData.get(0).length; i++) {
			cNames.add(header ? csvData.get(0)[i].trim() : ("c"+i));
		}
		
		// detect > override > defaultsVarcharIfNothingElse
		int r = 0;
		Iterator<String[]> it = csvData.iterator();
		if(header) { it.next(); } // skip first row
		while(it.hasNext()) {
			String[] on = it.next();
			for(int c=0; c<cNames.size() && c<on.length; c++) {
				cTypes.merge(cNames.get(c), toSQLtype(on[c]), new SqlTypeMerger());
			}
			r++;
		}
		cTypes.putAll(colTypes);
		for(String cName : cNames) {
			cTypes.putIfAbsent(cName, java.sql.Types.VARCHAR);
		}
		// Null was lowest precendence when we were not sure of type. If still not sure use varchar.
		SqlTypeMerger.replaceNulls(cTypes);
		
		// Create array then set vals forced to types
		int sz = csvData.size() - (header ? 1 : 0);
		Object[] colVals = SimpleResultSet.getArray(cNames.stream().map(cTypes::get).collect(Collectors.toList()), sz);

		r = 0;
		it = csvData.iterator();
		if(header) { it.next(); } // skip first row
		while(it.hasNext()) {
			String[] on = it.next();
			for(int c=0; c<cNames.size() && c<on.length; c++) {
				JsonResultSetBuilder.arraySetUnlessNull(colVals[c], r, getValue(on[c], cTypes.get(cNames.get(c))) );					
			}
			r++;
		}
		return new SimpleResultSet(cNames.toArray(new String[] {}), colVals);
	}


	static Integer toSQLtype(String s) {
		if(s == null || s.trim().length()==0) {
			return java.sql.Types.NULL; // THis special case is used for empty and is replaced by anything else as it is lowest priority
		}
		if(s.equals("-") || s.equals("--") || s.equals("–") || s.equals("––")) {
			return java.sql.Types.NULL; // THis special case is used for empty and is replaced by anything else as it is lowest priority
		}
		if(isNumeric(s) || s.equals(".")) { // FRED data source returns just "." for some number entries
			return s.contains(".") ? java.sql.Types.DOUBLE : java.sql.Types.BIGINT;
		}
		String S = s.toUpperCase().trim();
		if(S.equals("TRUE") || S.equals("FALSE")) {
			return java.sql.Types.BOOLEAN;
		}
		if(isISOorUkDate(S)) {
			return S.length() > 10 ? java.sql.Types.TIMESTAMP : java.sql.Types.DATE;
		}
		return java.sql.Types.VARCHAR;
	}
	
	private static boolean isNNsNNsNN(String S) {
		return S.length() >= 8 && isN(S,0) && isN(S,1) &&  S.charAt(2) == '/' && isN(S,3) && isN(S,4) &&  S.charAt(5) == '/' && isN(S,6) && isN(S,7);
	}

	private static boolean in(char needle, String listOfChars) {
		for(char c: listOfChars.toCharArray()) {
			if(c == needle) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isUSDate(String S) {
		try {
			if(isNNsNNsNN(S)) { // MM/dd/YY
				return Integer.parseInt(S.substring(0, 1)) <= 12 && Integer.parseInt(S.substring(3, 4)) <= 31;   
			}
		} catch(NumberFormatException e) {}
		return false;
	}

	private static boolean isUKDate(String S) {
		try {
			if(isNNsNNsNN(S)) { // MM/dd/YY
				return Integer.parseInt(S.substring(0, 2)) <= 31 && Integer.parseInt(S.substring(3, 5)) <= 12;   
			}
		} catch(NumberFormatException e) {}
		return false;
	}
	
	private static boolean isISOorUkDate(String S) {
		boolean isNNNN = S.length() >= 4 && isN(S,0) && isN(S,1) && isN(S,2) && isN(S,3);
		boolean isNNNNhNNhNN = S.length() >= 10 && isNNNN && S.charAt(4) == '-' && isN(S,5) && isN(S,6) && S.charAt(7) == '-' && isN(S,8) && isN(S,9);
		boolean isYYYYhMs = false;
		boolean isDDslashYYYY = isUSDate(S) && (S.length() == 8 || (S.length() == 10 && isN(S,8)  && isN(S,9)));
		if(S.length() == 7 && isNNNN && S.charAt(4) == '-') {
			isYYYYhMs = (isN(S,5) && isN(S,6)) || (Character.toUpperCase(S.charAt(5))=='Q' && in(S.charAt(6),"1234"));
		} 
		// 2023-09-12T20:14:02Z
		boolean isYYYY = isNNNN && S.length() == 4 && S.charAt(0) == '1' || S.charAt(0) == '2';
		return (S.length() >= 10 && isNNNNhNNhNN) || isYYYYhMs || isYYYY || isDDslashYYYY;
	}

	private static boolean isN(String s, int i) { return Character.isDigit(s.charAt(i)); }
	
	
	private static boolean isNumeric(String strNum) {
	    if (strNum == null) { return false; }
		String s = strNum.trim().toLowerCase();
		if(s.endsWith("million")) {
			// purposefully reassigning and flowing through
		    s = s.substring(0, s.length()-7);
		}

	    try {
	        Double.parseDouble(s.replace(",", ""));
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	    return true;
	}
	
	private static <T> T castElse(Function<String,T> caster, String s, T fallbackVal) {
		try {
			return caster.apply(s);
		} catch(NumberFormatException e) {}
		return fallbackVal;
	}
	
	private static DateTimeFormatter getDateTimeFormatterBuilder() {
		final DateTimeFormatterBuilder dtfb = new DateTimeFormatterBuilder();
		dtfb.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"))
		.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSS"))
		.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS"))
		.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"))
		.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSS"))
		.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS"))
		    .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
		    .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS"))
		    .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S"))
		    .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
		    .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
		    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
		    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
		    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0);
		return dtfb.toFormatter();
	}
	
	private static String cleanNum(String num) {
		String s = num.trim().toLowerCase();
		if(s.endsWith("million") && s.length() > 7) {
			s = s.substring(0,s.length()-7).trim() + "000000";
		}
		return s.replace(",", "");
	}
	
	static Object getValue(String val, Integer sqlType) {
		String s= val.trim();
		switch(sqlType) {
			case java.sql.Types.DOUBLE: return castElse(Double::parseDouble, cleanNum(s),Double.NaN);
			case java.sql.Types.INTEGER: return castElse(Integer::parseInt, cleanNum(s), null);
			case java.sql.Types.BIGINT: return castElse(Long::parseLong, cleanNum(s), null);
			case java.sql.Types.BOOLEAN: return castElse(Boolean::parseBoolean,s, null);
			case java.sql.Types.TIMESTAMP:
				try {
					if(s.length() > 10 && (s.charAt(10)=='T' || s.charAt(10)==' ')) {
						DateTimeFormatter dtf = getDateTimeFormatterBuilder();
						// formatter expects T but we can be leniant - twelvedata.com has space instead of T
						String t = s.charAt(10)==' ' ? (s.substring(0, 10)+"T"+s.substring(11)) : s; 
						t = t.endsWith("Z") ? t.substring(0, s.length()-1) : t;
						TemporalAccessor accessor = dtf.parse(t);
						return Timestamp.valueOf(LocalDateTime.from(accessor));
					}	
				} catch (DateTimeParseException e) {
					return null;
				}
				return null;
			case java.sql.Types.DATE:
				try {
					DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
					if(s.length() == 4) {
						df1 = new SimpleDateFormat("yyyy");
					} else if(s.length() == 7 && s.charAt(4)=='-') {
						df1 = new SimpleDateFormat("yyyy-MM");
						String yyyyh = s.substring(0, 5);
						if(Character.toUpperCase(s.charAt(5))=='Q') {
							switch(s.charAt(6)) {
								case '1': case '2': case '3': case '4': 
								return df1.parse(yyyyh + "0" + s.charAt(6));
								default:
							}
						}
					} else if(s.length() == 8 && isUSDate(s)) {
						df1 = new SimpleDateFormat("MM/dd/yy");
					} else if(s.length() == 10 && s.charAt(2)=='/' && s.charAt(5)=='/') {
						df1 = new SimpleDateFormat("MM/dd/yyyy");
					}
					return df1.parse(s);
				} catch (ParseException e) {
					return null;
				}
			case java.sql.Types.VARCHAR: return s;
			default:
		}
		return s;
	}
	
	@Data @RequiredArgsConstructor public static class SymbolDetails {
		@Nullable private final String database;
		@NonNull private final String symbol;
		@NonNull private final String query;
		@NonNull private final String title;
		@NonNull private final String description;
		
		public SymbolDetails(String database, String symbol, String query) {
			this(database, symbol, query, "", "");
		}
		
		@Override public String toString() { return "SymD[" + symbol + ":" + database + "=" + query + "]"; }
		
		public String getQueryToRun() { 
			SymPart sp = new SymPart(getSymbol());
			return getQuery().replace("{{XXX}}", getSymbol()).replace("XXX", getSymbol()).replace("{{SYM}}", sp.getSYM()).replace("{{TYP}}", sp.getTyp()); 
		}
	}

	private ResultSet toRS(List<SymbolDetails> sl) {
		List<Integer> colTypes = Lists.newArrayList(java.sql.Types.VARCHAR,java.sql.Types.VARCHAR,java.sql.Types.VARCHAR,java.sql.Types.VARCHAR,java.sql.Types.VARCHAR);
		Object[] colVals = SimpleResultSet.getArray(colTypes, sl.size());
		for(int row=0; row<sl.size(); row++) {
			SymbolDetails sd = sl.get(row);
			Array.set(colVals[0], row, sd.getSymbol());
			Array.set(colVals[1], row, sd.getDatabase());
			Array.set(colVals[2], row, sd.getQueryToRun());
			Array.set(colVals[3], row, sd.getTitle());
			Array.set(colVals[4], row, sd.getDescription());
		}
		String[] colNames = new String[] {"symbol", "database", "query", "title", "description"};
		return new SimpleResultSet(colNames, colVals);
	}

	public List<SymbolDetails> searchSymbols(@Nullable String qry) {
		List<SymbolDetails> l = new ArrayList<>(100);
		final String Q = qry.toUpperCase();
		if(qry.length() == 0) {
			for(SymbolDetails sd : SYMBOL_DETAILS) {
				if(sd.getSymbol().length()==1) { l.add(sd); }
			}			
		}
		for(SymbolDetails sd : SYMBOL_DETAILS) {
			if(sd.getSymbol().equals(Q)) { l.add(sd); }
		}
		for(SymbolDetails sd : SYMBOL_DETAILS) {
			if(l.size() > 99) { break; }
			if(sd.getSymbol().startsWith(Q) && !sd.getSymbol().equals(Q)) { l.add(sd); }
		}	
		for(SymbolDetails sd : SYMBOL_DETAILS) {
			if(l.size() > 99) { break; }
			if(Q.length() >= 4 && sd.getTitle().toUpperCase().contains(Q) && !sd.getSymbol().equals(Q)) { 
				l.add(sd); 
			}
		}	
		return l;
	}
	
	public static void generateSymbolsCache() {
	    Executors.newSingleThreadScheduledExecutor().execute(() -> {
		    try(BabelDBJdbcDriver babel = BabelDBJdbcDriver.standardH2("", DEFAULT_DBRUNNER)) {
		    	List<SymbolDetails> l = new ArrayList<>(babel.generateSymbols());
		    	Collections.sort(l, (a,b) -> a.getSymbol().compareTo(b.getSymbol()));
		    	SYMBOL_DETAILS = new CopyOnWriteArrayList<>(l);
			} catch (SQLException | IOException e) {
				log.error("No symbols cached." + e.getLocalizedMessage());
			}
	    });
	}
	
	public List<SymbolDetails> generateSymbols()  {
		List<SymbolDetails> s = new ArrayList<>();
		if(this.dbrunner != null) {
			for(String serverName : this.dbrunner.getServerWithSymbols()) {
				String db = serverName.toUpperCase();
				int added = 0;
				try {
					// This resultset should really be closed
					// BUT closing CachedRowSets sets ALL shared ones to null objects.
					// Need to replace CachedRowSets
					ResultSet rs = dbrunner.executeQry(db, BABEL_SYMBOL_QRY, 0);
					List<String> colNames = DBHelper.getColumnNames(rs.getMetaData());
					
					while(rs.next()) {
						String title = colNames.contains("title") ? rs.getString("title") : "";
						String description = colNames.contains("description") ? rs.getString("description") : "";
						String t = title == null ? "" : title;
						String d = description == null ? "" : description;
						s.add(new SymbolDetails(db, rs.getString("symbol").toUpperCase(), rs.getString("query"), t, d));
						added++;
					}
				} catch (Exception e) {
					log.warn("Could not fetch symbols for:" + db + " " + e.getLocalizedMessage());
				}
				log.info("generateSymbols added " + added + " symbols from " + db);
			}
		}
		return s;
	}

	public static ResultSet toSymbolRS(List<String> symbols) {
		List<Object> colValues = new ArrayList<>(2);
		colValues.add(symbols.stream().map(s -> s.toUpperCase()).collect(Collectors.toList()).toArray(new String[] {}));
		colValues.add(symbols.stream().map(s -> "XXX").collect(Collectors.toList()).toArray(new String[] {}));
		return new SimpleResultSet(Lists.newArrayList("symbol","query"), colValues);
	}
	
	private static SymbolDetails getSymbolQuery(String symbol) {
		String S = symbol.toUpperCase();
		String end = S.contains(":") ? S.substring(S.lastIndexOf(':')) : "";
		for(SymbolDetails sd : SYMBOL_DETAILS) {
			if(sd.getSymbol().equals(S)) { 
				return sd; 
			} else if(end.length()>0 && S.startsWith(sd.getSymbol()) && end.equalsIgnoreCase(sd.getDatabase())) {
				String fullSym = sd.getSymbol() + ":" + sd.getDatabase().toUpperCase();
				if(fullSym.equals(symbol)) {
					return sd; 
				}
			}
		}
		return new SymbolDetails(null, symbol, symbol); 
	}
	
	public static @Nullable SymbolDetails checkForSymbolQuery(String query) {
		final String Q = query.toUpperCase().trim();
		if(Q.startsWith(BabelDBJdbcDriver.QUERY_SYM + "(") && Q.endsWith(")")) {
			int p = Q.indexOf("(");
			List<String> symbols = ArgParser.parse(Q.substring(p)).getArgs();
			if(symbols.size()>1) { throw new UnsupportedOperationException("Only 1 arg supported now"); }
			try {
				return getSymbolQuery(symbols.get(0));
			} catch(UnsupportedOperationException e) {}
		}
		return null;
	}
	
	public static void addToSymbols_ONLY_FOR_TESTING(List<SymbolDetails> sds) {
		SYMBOL_DETAILS.addAll(sds);
	}

	public void loadExistingParquet(File folder) throws SQLException {
		File[] files = folder.listFiles();
		List<File> dataFiles = new ArrayList<>();
		if(files != null) {
			for(File f : files) {
				String n = f.getName().toLowerCase();
				if(n.endsWith(".parquet") || n.endsWith(".csv")) {
					dataFiles.add(f);
				}
			}
		}
		if(dataFiles.size() > 0) {
			run(getReplaceView(dataFiles));
		}
	}
	public static String getTblName(File dfile) {
		String df = dfile.getAbsolutePath();
		int p = df.lastIndexOf(File.separator)+1;
		if(p < 0) {
			p = 0;
		}
		int e = df.lastIndexOf('.');
		if(e < p) {
			e = df.length();
		}
		return df.substring(p, e).replace(" ", "_");
	}
	
	public static String getReplaceView(List<File> dataFiles) {
		StringBuilder sb = new StringBuilder();
		for(File dfile : dataFiles) {
			String df = dfile.getAbsolutePath();
			String name = getTblName(dfile);
			if(df.toLowerCase().endsWith(".csv")) {
				sb.append("CREATE OR REPLACE VIEW " + name + " AS SELECT * FROM read_csv('" + df + "');\n");	
			} else if(df.toLowerCase().endsWith(".parquet")) {
				sb.append("CREATE OR REPLACE VIEW " + name + " AS SELECT * FROM read_parquet('" + df + "');\n");	
			}
		}
		return sb.toString();
	}
}
