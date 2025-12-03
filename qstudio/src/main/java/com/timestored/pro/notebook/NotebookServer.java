package com.timestored.pro.notebook;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.sql.rowset.CachedRowSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ServerConfig;
import com.timestored.misc.DirWatch;
import com.timestored.misc.IOUtils;
import com.timestored.misc.DirWatch.DirWatchListener;
import com.timestored.qstudio.CommonActions;
import com.timestored.qstudio.QStudioFrame;
import com.timestored.qstudio.model.QueryResult;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotebookServer {

	private static final Logger LOG = Logger.getLogger(NotebookServer.class.getName());
	private static final int PORT_LOW = 8088;
	private int port = -1;
	private final @NonNull ConnectionManager connectionManager;
	private final @NonNull File markdownDir;
	private HttpServer server;
	private ExecutorService httpThreadPool;
	private final FileFilter fileFilter = DirWatch.generateFileFilter(Pattern.compile("^\\..*|^target$"));
	private MyHandler markdownHandler;
	private static final String HARDWIRED_KDBNAME = "localhost:5000";
	public static final String EXAMPLE_KDB_FILE = "kdb-all.md";

	/**
	 * @return Example File if one was created otherwise null.
	 */
	public File initDirWithExamples() throws IOException {
		File PAGES_DIR = new File(markdownDir, "pages");
		File exampleFileCreated = null;
		if (!(PAGES_DIR.isDirectory() && PAGES_DIR.exists())) {
			ServerConfig sc = connectionManager.getServer(HARDWIRED_KDBNAME);
			if(sc == null) {
				sc = connectionManager.getServerConnections().stream().filter(mySC -> mySC.isKDB()).findFirst().orElse(null);
			}

			File duckdbFile = new File(PAGES_DIR, "duckdb-examples.md");
			IOUtils.writeStringToFile(IOUtils.toString(NotebookServer.class, "markdown-examples.md"), new File(PAGES_DIR, "markdown-examples.md"));
			IOUtils.writeStringToFile(IOUtils.toString(NotebookServer.class, "duckdb-examples.md"), duckdbFile);
			exampleFileCreated = duckdbFile; 
			
			if(sc != null) { // Only show KDB examples if they have a KDB+ server
				String kdbAll = IOUtils.toString(NotebookServer.class, EXAMPLE_KDB_FILE);
				kdbAll = (sc != null && !sc.getName().equals("localhost:5000")) ? kdbAll.replace(HARDWIRED_KDBNAME, sc.getName()) : kdbAll;
				IOUtils.writeStringToFile(kdbAll, new File(PAGES_DIR, EXAMPLE_KDB_FILE));
	
				String kdbSimple = IOUtils.toString(NotebookServer.class, "kdb-simple-examples.md");
				kdbSimple = (sc != null && !sc.getName().equals("localhost:5000")) ? kdbSimple.replace(HARDWIRED_KDBNAME, sc.getName()) : kdbSimple;
				File kdbEgFile = new File(PAGES_DIR, "kdb-simple-examples.md");
				IOUtils.writeStringToFile(kdbSimple, kdbEgFile);
				exampleFileCreated = kdbEgFile; 
			}
		}
		return exampleFileCreated;
	}

	/**
	 * @return The port that the web server is running on or -1 if it failed to start.
	 */
	public int start() throws Exception {
		DirWatch dirWatch = new DirWatch(1000, fileFilter, false);
		dirWatch.setRoot(markdownDir);
		dirWatch.addListener(new DirWatchListener() {
			@Override public void changeOccurred() {
				refreshPageList();
			}
		});
		int port = PORT_LOW;
		for(int offset=0; offset < 100; offset++) {
			try {
				port = PORT_LOW+offset;
				InetSocketAddress sockAddr = new InetSocketAddress(port);
				server = HttpServer.create(sockAddr, 0);
				httpThreadPool = Executors.newFixedThreadPool(5);
				server.setExecutor(httpThreadPool);
				this.markdownHandler = new MyHandler(connectionManager, markdownDir);
				server.createContext("/", markdownHandler);
				server.setExecutor(null); // creates a default executor
				server.start();
				this.port = port;
				return port;
			} catch(java.net.BindException e) {
				LOG.fine("Can't get port: " + port + " trying higher");
			}
		}
		refreshPageList();

		LOG.severe("Couldn't get free port for NotebookServer.");
		return -1;
	}

//	private static String getHost() {
//		try { THis caused lock ups when requests came from other IPs
//			return InetAddress.getLocalHost().getHostName();
//		} catch (UnknownHostException e) { }
//		return "localhost";
//	}

	public String getHttpMarkdown(String filePathOrNull) {
		String qry = "";
		if(filePathOrNull != null && hasMarkdownFileEnding(filePathOrNull)) {
			try {
				File f = new File(filePathOrNull);
				if(f.getCanonicalPath().startsWith(markdownDir.getCanonicalPath())) {
					Path parentPath = new File(markdownDir, "pages").toPath();
					qry = "/" + getLinkForFile(parentPath, f) + "?sheet=" + getLinkForFile(parentPath, f);
				}
			} catch (IOException e) {}
		}
		return "http://localhost:" + port + "/markdown" + qry;
	}

	String getHttpAddress() {
		return "http://localhost:" + port;
	}

	public static boolean hasMarkdownFileEnding(String filePath) {
		String e = filePath.toLowerCase();
		return e.endsWith(".markdown") || e.endsWith(".mdown") || e.endsWith(".mkdn") || e.endsWith(".md");
	}

	public synchronized void refreshPageList() {
		LOG.info("refreshPageList");
		File[] pagesToWatch = new File[] { new File(markdownDir, "pages") };
		FileFilter markdownFilter = (File pathname) -> pathname.isDirectory() || hasMarkdownFileEnding(pathname.getName());
		List<File> pages = new ArrayList<File>(DirWatch.generateFileCache(pagesToWatch, markdownFilter));
		try {
			markdownHandler.setAllMarkdownPages(pages);
		} catch (JsonProcessingException e) {
			LOG.severe("Error generating markdown json:" + e.toString());
		}
	}

	public void stop() {
		server.stop(1);
		httpThreadPool.shutdownNow();
	}

	private static String getRequestBody(HttpExchange t) throws IOException {
		StringBuilder postReqSb = new StringBuilder();
		InputStream ios = t.getRequestBody();
		int i;
		while ((i = ios.read()) != -1) {
			postReqSb.append((char) i);
		}
		return postReqSb.toString();
	}

	@Data
	public static class TabbDetails {
		private final String name;
		private final String jsonmodel;
	}

	@Data
	@RequiredArgsConstructor
	public static class DashboardData {

		private final int version;
		private final TabbDetails[] tabbDetails;

		public DashboardData(Path parentPath, List<File> markdownPages, int version) {
			this.version = version;
			List<TabbDetails> l = new ArrayList<>(markdownPages.size());
			for (File mdf : markdownPages) {
				try {
					if (mdf.isFile() && mdf.canRead()) {
						l.add(new TabbDetails(getLinkForFile(parentPath, mdf), IOUtils.toString(mdf)));
					}
				} catch (IOException e) {
					LOG.severe("Could not read all and create full markdown:" + e.toString());
				}
			}
			this.tabbDetails = l.toArray(new TabbDetails[] {});
		}

		public String toJSON() throws JsonProcessingException {
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			return ow.writeValueAsString(this);
		}
	}

	@Data
	public static class QueryWithArgs {
		private String query;
		private String serverCmd;
		private ArgEntry[] argsArray;
	}

	@Data
	public static class ArgEntry {
		private String argKey;
		private String[] argVals;
		private String argType;
	}

	private static <T> T getRequestBodyJson(HttpExchange t, Class<T> c) throws IOException {
		List<String> ct = t.getRequestHeaders().get("Content-Type");
		if (ct == null || ct.size() == 0 || !ct.get(0).equalsIgnoreCase("application/json")) {
			throw new RuntimeException("Expected JSON request");
		}
		String body = getRequestBody(t);
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readValue(body, c);
	}

	static String getLinkForFile(Path pagesDir, File f) {
		Path a = pagesDir.relativize(f.toPath());
		String s = a.toString();
		s = s.endsWith(".md") ? s.substring(0, s.length() - 3) : s;
		s = s.replace(File.separatorChar, '/');
		if(s.toLowerCase().endsWith("/index")) {
			s = s.substring(0, s.length() - 5);
		} else if(s.equalsIgnoreCase("index")) {
			s = "";
		}
		return s;
	}

	@RequiredArgsConstructor
	private static class MyHandler implements HttpHandler {

		private final @NonNull ConnectionManager connectionManager;
		private final @NonNull File markdownDir;
		private final @NonNull Path parentPath;
		private int updateId = -1;
		/**
		 * Folders AND individual .md pages that are thought to exist on last refresh.
		 */
		private List<File> allMarkdownPages = Collections.emptyList();
		private String fullTabbsJson = "";

		public void setAllMarkdownPages(List<File> allMarkdownPages) throws JsonProcessingException {
			this.allMarkdownPages = allMarkdownPages;
			DashboardData dd = new DashboardData(parentPath, allMarkdownPages, ++updateId);
			fullTabbsJson = dd.toJSON();
		}

		public MyHandler(ConnectionManager connectionManager, File markdownDir) {
			this.connectionManager = Preconditions.checkNotNull(connectionManager);
			this.markdownDir = Preconditions.checkNotNull(markdownDir);
			this.parentPath = new File(markdownDir, "pages").toPath();
		}

		private static final String MARKDOWN = "<!DOCTYPE html>\r\n" + "<html lang=\"en\">\r\n" + "  <head>\r\n"
				+ "    <meta charset=\"utf-8\" />\r\n" + "    <base href=\"/\"/>\r\n"
				+ "    <link rel=\"icon\" href=\"/favicon.ico\" />\r\n"
				+ "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\r\n"
				+ "    <meta name=\"theme-color\" content=\"#000000\" />\r\n" + "    <title>qStudio</title>\r\n"
				+ "    <script>window.pulseconfig={}</script>\r\n"
				+ "    <script defer=\"defer\" src=\"/static/js/main.1cdfdc19.js\"></script>\r\n"
				+ "<link href=\"/static/css/main.647099bf.css\" rel=\"stylesheet\"></head>\r\n"
				+ "  <body><noscript>You need to enable JavaScript to run this app.</noscript>\r\n"
				+ "    <div id=\"root\"></div></body></html>\r\n";
		private static final String PRE = "<!DOCTYPE html>\r\n" + "<html lang=\"en\">\r\n" + "  <head>\r\n"
				+ "    <meta charset=\"utf-8\">\r\n" + "    <title>qStudio</title>\r\n"
				+ "    <link rel=\"stylesheet\" href=\"/style.css\">\r\n"
				+ "  </head>\r\n" + "  <body>";
		private static final String POST = "</body></html>";

		@Override
		public void handle(HttpExchange t) throws IOException {
			String requestedURL = "http://" + t.getRequestHeaders().getFirst("Host") + t.getRequestURI();
			URL u = new URL(requestedURL);
			String path = u.getPath();
			if(!path.equals(BUNDLE_PATH)) { // This one getrs pinged a lot.
				LOG.info("Client Request: " + path + " from " + u.getHost() + ":" + u.getPort());
			}
			try {
				if (!handle(t, path)) {
                	writeReply(t, toB("Not Handled"), 200); // TODO THis should be 404-Not Found but it causes the site to fail to load
				}
			} catch (IOException e) {
				writeReply(t, toB(e.toString()), 502); // Server Error
			}
		}

		private static byte[] toB(String s) { return s.getBytes(Charset.forName("UTF-8")); }
		

	    private String toS(String s) { return s == null ? "\"\"" : "\"" + s.replaceAll("\"", "\\\"") + "\""; }
	    
		/*
		 * @return True if a response was sent. I.e. The request was handled.
		 */
		public boolean handle(HttpExchange t, String path) throws IOException {
			String response = null;
			StringBuilder sb = new StringBuilder();

			// This would allow running react on port 3000 and querying qStudio on :8088
			t.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:3000");
			t.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
			t.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
			t.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
			String method = t.getRequestMethod();
			method = method == null ? "" : method.toUpperCase();
			boolean isMyIp = isThisMyIpAddress(t.getRemoteAddress());
			
			if(!isMyIp) {
				writeReply(t, USE_PULSE_HTML.getBytes(Charset.forName("UTF-8")), 403);	
			} else if(method.equals("OPTIONS")) {
				response = ""; // React:3000 sends OPTIONS command to check CORS. By having this we allow using qStudio as SERVER=
			} else if (path.equals("/")) {
				sb.append(PRE);
				sb.append("<a href='/markdown'>markdown</a>");
				sb.append(POST);
				response = sb.toString();
			} else if (path.startsWith("/markdown")) {
				String newConfig = "{\"version\":"+toS("3.04") + ",\"isQStudio\":true" + "}";
				String m = MARKDOWN.replace("window.pulseconfig={}", "window.pulseconfig=" + newConfig);
				response = sb.append(m).toString();
			} else if (path.equals("/api/dbserver/") || path.equals("/api/dbserver/list-private")) {
				t.getResponseHeaders().set("Content-Type", "application/json");
				sb.append("[");
				List<ServerConfig> conns = connectionManager.getServerConnections();
				for (int i = 0; i < conns.size(); i++) {
					ServerConfig sc = conns.get(i);
            		if(i > 0) { sb.append(",\r\n\t"); }
                	sb.append("{\"id\": " + (i+2) + ", \"name\": \"" + sc.getName().replace("\\", "\\\\") + "\", \"jdbcType\": \"" + sc.getJdbcType().toString() + "\" }");
					}
				sb.append("]");
				response = sb.toString();
			} else if (path.startsWith("/api/a.json")) {
				String qry = t.getRequestURI().getQuery();
				// Nasty hardcoded search result box result to mock Pulse return values for now. This should search markdown later.
				if(path.equalsIgnoreCase("/api/a.json") && qry != null && qry.startsWith("server=BABELDB&query=QUERY_SYMBOLS('")) {
					String searchText = qry.substring("server=BABELDB&query=QUERY_SYMBOLS('".length());
					searchText = searchText.length() > 2 ? searchText.substring(0, searchText.length()-2) : searchText;
					response = "{\"tbl\":{\"data\":[{\"symbol\":\"1000BONKUSDC\",\"database\":\"BINANCE\",\"query\":\"1000BONKUSDC\",\"title\":\"\",\"description\":\"\"},{\"symbol\":\"1000BONKUSDC_AGGTRADE\",\"database\":\"BINANCE\",\"query\":\"1000BONKUSDC_AGGTRADE\",\"title\":\"\",\"description\":\"\"}],\"types\":{\"symbol\":\"string\",\"database\":\"string\",\"query\":\"string\",\"title\":\"string\",\"description\":\"string\"}},\"console\":\"\"}";
				} else if("POST".equals(t.getRequestMethod().toUpperCase())) {
					String serverPth = path.substring("/api/a.json/".length());
					serverPth = java.net.URLDecoder.decode(serverPth, StandardCharsets.UTF_8.name());
					QueryWithArgs qryWithArgs = getRequestBodyJson(t, QueryWithArgs.class);
					try {
						response = runQuery(serverPth, qryWithArgs.query);
					} catch(IllegalStateException ise) {
						response = "{\"exception\":\"Could not find server.\"}";
					}
				}
			} else if (path.equals(BUNDLE_PATH)) {
				int reqUpdateId = -2;
				try {
					String body = getRequestBody(t);
					reqUpdateId = Integer.parseInt(body);
				} catch (NumberFormatException nfe) {
					reqUpdateId = -3; // value doesn't really matter as long as it's not valid
				}
				if (reqUpdateId == updateId) {
					writeReply(t, toB("NOCHANGE"));
					return true;
				}
				t.getResponseHeaders().set("Content-Type", "application/json");
				writeReply(t, toB(this.fullTabbsJson));
				return true;
			} else if (path.equals("/style.css") || path.equals("/favicon.ico") || path.equals("/bundle.js")) {
				t.getResponseHeaders().set("Content-Type", getContentType(path));
				byte[] responseRaw = readResourceBytes(path.substring(1));
				if (responseRaw != null) {
					writeReply(t, responseRaw);
					return true;
				}
			} else if (path.startsWith("/static/") || path.startsWith("/api/static/") || path.startsWith("/img/")) {
				// All ajax requests in pulse frontend go via /api/ by default so have to allow for that to enable ajax fetches
				String p = path.startsWith("/api/static/") ? path.substring(4) : path.startsWith("/img/") ? ("/static" + path) : path;
				File f = new File(markdownDir, p.substring(1));
				if (f.exists() && f.isFile()) {
					t.getResponseHeaders().set("Content-Type", getContentType(p));
					writeReply(t, Files.readAllBytes(f.toPath()));
				} else {
					String resPath = p.substring("/static/".length());
					try {
						// Reading resource doesn't seem to work
						byte[] responseRaw = null;
						if(isTextContentType(p)) {
							responseRaw = IOUtils.toString(NotebookServer.class, resPath).getBytes(Charset.forName("UTF-8"));
						} else {
							responseRaw = readResourceBytes(resPath);
						}
						t.getResponseHeaders().set("Content-Type", getContentType(p));
						if (responseRaw != null) {
							writeReply(t, responseRaw);
							return true;
						}
					} catch(IOException ioe) { // file not found as /static/
						writeReply(t, new byte[] {}, 404);	
					}
				}
				return true;
			} 
			if (response != null) {
				writeReply(t, toB(response));
			}
			return response != null;
		}

		private static final String PRQL_PRE = ":prql:";
		private static final Object BUNDLE_PATH = "/api/dashboard/mdbundle";
		private static final String USE_PULSE_HTML = "<html><body><a href='https://www.timestored.com/qstudio/help/issues/sqlnotebook-access-denied?utm_source=sqlnotebook&utm_medium=app&utm_campaign=sqlnotebook'><h1>SQLNotebook Access Denied</h1></a></body></html>";

		private String runQuery(String serverPth, String postedQry) throws IOException,IllegalStateException {
			System.out.println("serverPth ====== " + serverPth + " hm: " + postedQry);
			ServerConfig sc = connectionManager.getServer(serverPth);
			com.timestored.pro.notebook.ResultSetSerializer rss = new ResultSetSerializer(false);
			long startTime = System.currentTimeMillis();
			if (sc != null) {
				try {
					String qry = postedQry;
					if (qry.startsWith(PRQL_PRE)) {
						qry = CommonActions.compilePRQL(qry.substring(PRQL_PRE.length()), sc.getJdbcType());
					}
					// TODO Should use QueryManager with jdbc driver from Pulse to provide wrapping etc.
					// connectionManager = no wrapping.
					// qStudio jdbc = no conversion of times
					CachedRowSet rs = connectionManager.executeQuery(sc, qry);
					QueryResult qr = QueryResult.successfulResult(sc, postedQry, null, null, rs, null, System.currentTimeMillis() - startTime);
					return rss.toString(qr);
				} catch (SQLException e) {
					return rss.toString(QueryResult.exceptionResult(sc, postedQry, null, e, System.currentTimeMillis() - startTime));
				}
			} else {
            	throw new IllegalStateException("Server not found");
			}
		}

		private void writeReply(HttpExchange t, byte[] reply) throws IOException {
			writeReply(t, reply, 200); // Fine
		}

		private void writeReply(HttpExchange t, byte[] reply, int code) throws IOException {
			t.sendResponseHeaders(code, reply.length);
			OutputStream os = t.getResponseBody();
			os.write(reply);
			os.close();
		}

		public static boolean isThisMyIpAddress(InetSocketAddress inetSocketAddress) {
			InetAddress addr = inetSocketAddress.getAddress();
		    if(addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
		        return true; // Was local sub-net.
		    }
	    	// THis was too slow
//	        return NetworkInterface.getByInetAddress(addr) != null;
	        return false;
		}

        
	}

	private static boolean isTextContentType(String path) {
		int dotPos = path.lastIndexOf('.');
		String p = (dotPos >= 0 ? path.substring(dotPos + 1) : "").toLowerCase();
		switch (p) {
        case "css":
        case "js":  
		case "html":
        case "htm": 
        case "json":
        case "xml": 
        case "svg": 
        case "md":
        case "csv":
        	return true;
		}
		return false;
	}
	
	private static String getContentType(String path) {
		int dotPos = path.lastIndexOf('.');
		String p = (dotPos >= 0 ? path.substring(dotPos + 1) : "").toLowerCase();
		switch (p) {
        case "css": return "text/css";
        case "js":  return "application/javascript"; 
		case "html":
        case "htm": return "text/html";
        case "json": return "application/json";
        case "xml": return "application/xml";
        case "png": return "image/png";
		case "jpg":
        case "jpeg": return "image/jpeg";
        case "gif": return "image/gif";
        case "svg": return "image/svg+xml";
        case "bmp": return "image/bmp";
		case "tiff":
        case "tif": return "image/tiff";
        case "mp3": return "audio/mpeg";
        case "wav": return "audio/wav";
        case "ogg": return "audio/ogg";
        case "mp4": return "video/mp4";
        case "webm": return "video/webm";
        case "zip": return "application/zip";
        case "pdf": return "application/pdf";
        case "xls": return "application/vnd.ms-excel";
        case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        case "md": return "text/markdown";
        case "csv": return "text/csv";
        case "ico": return "image/x-icon";
		}
		return "text/plain";
	}

	
    private static byte[] readResourceBytes(String resourcePath) throws IOException {
    	// Files.readAllBytes works perfectly when running in Eclipse NOT when installed. It fails for .js files
		if(isTextContentType(resourcePath)) {
			return IOUtils.toString(NotebookServer.class, resourcePath).getBytes(Charset.forName("UTF-8"));
		} else {
			return Resources.toByteArray(Resources.getResource(NotebookServer.class, resourcePath));
		}
    }

}
