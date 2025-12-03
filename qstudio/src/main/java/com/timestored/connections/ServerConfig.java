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
package com.timestored.connections;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import static com.google.common.base.MoreObjects.toStringHelper;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.timestored.misc.IOUtils;
import com.timestored.plugins.ConnectionDetails;

import lombok.Getter;
import net.jcip.annotations.Immutable;


/** Details relevant to a single database server connection. */
@Immutable 
public class ServerConfig {

	private final String name;
	private final String username;
	private final String password;
	private final String host;
	private final int port;
	private final String database;
	private final JdbcTypes jdbcType;
	@Getter private final boolean useTLS;
	@Getter private final boolean useAsync;
   	
	/** The color GUI elements should be shown for this server */
	private final Color color;
	public static final Color DEFAULT_COLOR = Color.WHITE;
	public static final boolean DEFAULT_USE_TLS = false;
	public static final boolean DEFAULT_USE_ASYNC = false;
	

	public ServerConfig(String host, int port, String username, 
			String password, String name, JdbcTypes jdbcType) {
		this(host, port, username, password, name, jdbcType, DEFAULT_COLOR, null, null, DEFAULT_USE_TLS, DEFAULT_USE_ASYNC);
	}
	public ServerConfig(String host, int port, String username, 
			String password, String name, JdbcTypes jdbcType, Color color, String database) {
		this(host, port, username, password, name, jdbcType, color == null ? DEFAULT_COLOR : color, database, null, 
				DEFAULT_USE_TLS, DEFAULT_USE_ASYNC);
	}
	
	public static ServerConfig forFile(String filePath) throws IOException {
		String path = filePath.replace('\\', '/');
		File f = new File(filePath);
		String header = "";
		if(f.length() > 5) {
			byte[] buffer = new byte[4];
			InputStream is = new FileInputStream(f);
			if (is.read(buffer) != buffer.length) { 
			    // do something 
			}
			is.close();
			header = new String(buffer);
		}
		
		String cleanName = f.getAbsolutePath().replace('/', '\\'); 
		if(header.equalsIgnoreCase("H:2,")) {
			String url = "jdbc:h2:file:" + (path.endsWith(".mv.db") ? path.substring(0, path.length() - 6) : path);
			return new ServerConfig("localhost", 0, "sa", "", cleanName, JdbcTypes.H2, null, url);
		} else if(header.equalsIgnoreCase("SQLi") || path.endsWith(".sqlite")) {
			String url = "jdbc:sqlite:" + filePath;
			return new ServerConfig("localhost", 0, "", "", cleanName, JdbcTypes.SQLITE_JDBC, null, url);
		} else { // if(filePath.endsWith(".duckdb")) {
			File hsqlProps = new File(filePath + ".properties");
			if(hsqlProps.exists() && IOUtils.toString(hsqlProps).toUpperCase().contains("#HSQL")) {
				String url = "jdbc:hsqldb:" + filePath;
				return new ServerConfig("localhost", 0, "", "", cleanName, JdbcTypes.HSQLDB_EMBEDDED, null, url);
			} else { // assume duckdb
				String url = "jdbc:duckdb:" + filePath;
				return new ServerConfig("localhost", 0, "", "", cleanName, JdbcTypes.DUCKDB, null, url);
			}
		}
		
		//throw new IOException("Unrecognised file format. I only recognise H2 and duckdb databases.");
	}
	
	/**
	 * @param folder The folder(s) separated by forward slashes that this server goes into. If null it could be supplied
	 * as part of the name itself.
	 */
	public ServerConfig(String host, int port, String username, 
			String password, String name, JdbcTypes jdbcType, Color color, String database, String folder, 
			boolean useTLS, boolean useAsync) {

		if(port<0) {
			throw new IllegalArgumentException("Must specify positive port");
		}
		if(name.endsWith("/")) {
			throw new IllegalArgumentException("Name cannot end with a /");
		}
		
		this.database = database;
		this.color = color == null ? DEFAULT_COLOR : color;
		this.host = Preconditions.checkNotNull(host);
		this.port = port;
		this.username = username;
		this.password = password;
		this.useTLS = useTLS;
		this.useAsync = useAsync;
		
		// clean any folders, remove multiple empty /s
		String n = name;
		if(n==null || n.trim().isEmpty()) {
			if(port == 0) {
				n = database == null || database.trim().isEmpty() ? jdbcType.name() : database.replace("jdbc:", "");
				int p = n.indexOf("/");
				if(p >= 0) {
					n = n.substring(0,p);
				}
			} else {
				// Clickhouse specifies hostname including https
				n = host.replaceAll("https://", "").replaceAll("http://", "") + ":" + port;  
			}
		}

		// tricky part is either folder or name can specify folder but NOT both
		if(folder != null) {
			if(n.contains("/")) {
				throw new IllegalArgumentException("Cant specify name with path and separate folder");
			} else {
				String cf = cleanFolderName(folder);
				n =  (cf.length()>0 ? cf + "/" : "") + n;
			}
		} else {
			n = Joiner.on("/").join(extractParts(n));
		}
		this.name = n;
		
		
		this.jdbcType = Preconditions.checkNotNull(jdbcType);
	}

	public ServerConfig(String host, int port, 
			String username, String password, String name) {
		this(host, port, username, password, name, JdbcTypes.KDB, null, null);
	}
	public ServerConfig(String host, int port, 
			String username, String password) {
		this(host, port, username, password, host + ":" + port, JdbcTypes.KDB, null, null);
	}

	public ServerConfig(String host, int port) {
		this(host, port, "", "", host + ":" + port, JdbcTypes.KDB, null, null);
	}

	/** @return A short user assigned name representing this server **/
	public String getName() { return name; }

	/** @return username for logging into database if one is set else empty string  **/
	public String getUsername() { return username == null ? "" : username; }

	/** @return password for logging into database if one is set else empty string  **/
	public String getPassword() { return password == null ? "" : password; }

	/** @return hostname that the database server is running on **/
	public String getHost() { return host; }

	/** @return tcp port that the database server is running on **/
	public int getPort() { return port; }

	/** @return database that will be connected to if one is set else empty string  **/
	public String getDatabase() { return database == null ? "" : database; }
	
	ConnectionDetails getConnectionDetails() {
		return new ConnectionDetails(host, port, database, username, password);
	}
	
	/**
	 * @return The folder that this {@link ServerConfig} is in or "" if in the root.
	 * Note the returned folder will NOT have a trailing slash.
	 */
	public String getFolder() {
		int p = name.lastIndexOf("/");
		String s = p > -1 ? name.substring(0, p) : "";
		return s;
	}

	public List<String> getFolders() {
		List<String> l = extractParts(name);
		return l.subList(0, l.size() -1);
	}

	/**
	 * @return List of strings that had been separated by folder-separators
	 */
	public static List<String> extractParts(String name) {
		if(!name.contains("/")) {
			List<String> r = new ArrayList<String>(1);
			r.add(name);
			return r;
		}
		String[] a = name.split("/");
		List<String> r = new ArrayList<String>(a.length);
		for(String s : a) {
			if(s.length()>0) {
				r.add(s);
			}
		}
		return r;
	}
	

	/**
	 * @return cleaned folder name i.e. collapse reoccurring // and only return one slash and no start/end slash.
	 *  eg cleanFolderName("///aa///bb/////c//") -&gt; a/b/c
	 */
	public static String cleanFolderName(String folder) {
		List<String> folds = extractParts(folder);
		return Joiner.on("/").join(folds);
	}
	
	/**
	 * @return The name of this {@link ServerConfig} excluding the path.
	 */
	public String getShortName() {
		int p = name.lastIndexOf("/");
		return p > -1 ? name.substring(p + 1) : name;
	}
	
	public JdbcTypes getJdbcType() { return jdbcType; }

	
	@Override public String toString() {
		return toStringHelper(this)
			.add("name", name)
			.add("username", username)
			.add("host", host)
			.add("port", port)
			.add("cstoreType", jdbcType)
			.add("database", database)
			.toString();
	}
	
	/**
	 * @return JDBC URL for this connection
	 */
	public String getUrl() {
		return jdbcType.getURL(this);
	}

	/** 
	 * The color some GUI elements should be shown for this server, 
	 * allows red to warn of production machine for example */
	public Color getColor() { return color==null ? DEFAULT_COLOR : color; }
	
	public boolean isDefaultColor() { return DEFAULT_COLOR.equals(color); }


	@Override
	public int hashCode(){
		return Objects.hashCode(name, username, password, host, port, database, jdbcType, color);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof ServerConfig) {
			ServerConfig that = (ServerConfig) object;
			return Objects.equal(this.name, that.name)
				&& Objects.equal(this.username, that.username)
				&& Objects.equal(this.password, that.password)
				&& Objects.equal(this.host, that.host)
				&& Objects.equal(this.port, that.port)
				&& Objects.equal(this.getDatabase(), that.getDatabase())
				&& Objects.equal(this.getFolder(), that.getFolder())
				&& Objects.equal(this.jdbcType, that.jdbcType)
				&& Objects.equal(this.color, that.color);
		}
		return false;
	}

	/** @return true iff this is a kdb server **/
	public boolean isKDB() { return jdbcType.isKDB(); }
	
	/** @return true iff this is a streaming server **/
	public boolean isStreaming() { return jdbcType.isStreaming(); }

	public boolean hasLogin() {
		return username!=null && username.length()>0 || password!=null && password.length()>0;
	}
	
}
