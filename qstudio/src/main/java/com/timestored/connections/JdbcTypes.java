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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.timestored.plugins.DatabaseAuthenticationService;

import lombok.Getter;
import lombok.NonNull;


/**
 * THIS FILE IS VERY SIMILAR TO A FILE FROM QSTUDIO. KEEP IN SYNC.
 * THIS FILE IS VERY SIMILAR TO A FILE FROM QSTUDIO. KEEP IN SYNC.
 * THIS FILE IS VERY SIMILAR TO A FILE FROM QSTUDIO. KEEP IN SYNC.
 */


/**
 * Container for supported JDBC databases that allows getting a connection.
 */
public enum JdbcTypes {

	// !!!!!!IMPORTANT!!!!!! New Data sources MUST only be added in order as their number is stored in the database!!
	// If you do not add at the new entry at the end, saved connections will be BROKEN!!! Including even the demo.
	
	KDB("Kdb", "kx.jdbc", 5000, "jdbc:q:{host}:{port}", "http://kx.com", "", "") {
		@Override public String getComment(String commentContent) {
			return "/ " + commentContent;
		}
		@Override public String getComment() { return "/ "; }
		@Override public boolean isKDB() { return true; }
	},
	
	POSTGRES("Postgres", "org.postgresql.Driver", 5432, "jdbc:postgresql://{host}:{port}/{database}?", "http://postgresql.com", "", ""),
	CLICKHOUSE("Clickhouse", "com.clickhouse.jdbc.ClickHouseDriver",8123,"jdbc:ch:{host}:{port}[/{database}]","https://github.com/ClickHouse/clickhouse-java","maven:/com.clickhouse:clickhouse-jdbc:RELEASE[0.8.6]","com/clickhouse/clickhouse-jdbc/0.8.6/clickhouse-jdbc-0.8.6.jar"),	
	
	CUSTOM(getProperty("jdbc.niceName","Custom JDBC Driver"), getProperty("jdbc.driver","DriverNotSpecified"), getProperty("jdbc.port",5000), getProperty("jdbc.dbRequired", getProperty("jdbc.urlFormat","DriverUrlPrefixNotSpecified"))) {

		private volatile DatabaseAuthenticationService dbAuthenticatorService;
		private volatile boolean init = false;
		private volatile boolean isKDB = getProperty("jdbc.isKDB", false);
		private volatile boolean isStreaming = getProperty("jdbc.isStreaming", false);
		
		@Override public DatabaseAuthenticationService getAuthenticator() {
			final String className = getProperty("jdbc.authenticator", null);
			synchronized(this) {
				if(!init) {
					Exception x = null;
					try {
						if(className != null) {
							Class<?> clazz = Class.forName(className);
							if(clazz != null) {
								Constructor<?> ctor = clazz.getConstructor();
								if(ctor != null) {
									Object object = ctor.newInstance();
									if(object instanceof DatabaseAuthenticationService) {
										this.dbAuthenticatorService =  (DatabaseAuthenticationService) object;
									}
								}
							}
						}
					} catch (ClassNotFoundException e) { x = e;
					} catch (NoSuchMethodException e) { x = e;
					} catch (SecurityException e) { x = e;
					} catch (InstantiationException e) { x = e;
					} catch (IllegalAccessException e) { x = e;
					} catch (IllegalArgumentException e) { x = e;
					} catch (InvocationTargetException e) { x = e;
					}
					if(x != null || (className != null && this.dbAuthenticatorService == null)) {
						Logger LOG = Logger.getLogger(JdbcTypes.class.getName());
						LOG.severe("Could not load dbAuthenticatorService for class: " + className + "exception:" + x);
					}
					init = true;
				}
				return this.dbAuthenticatorService;
			}
		}
		
		@Override public String getURL(ServerConfig sc) {
			if(sc.getPort() == 0) {
				return sc.getDatabase(); // If port is zero that means Database contains full JDBC URL
			}
			String s = getSampleURL();
			s = s.replace("@HOST@", sc.getHost());
			s = s.replace("@PORT@", ""+sc.getPort());
			s = s.replace("@DB@", sc.getDatabase());
			// jdbc:postgresql://localhost/test?user=fred&password=secret&ssl=true
			return s;
		}

		@Override public boolean isKDB() { return isKDB; }
		@Override public boolean isStreaming() { return isStreaming; }
	},
	
	
	MSSERVER("Microsoft SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433, "jdbc:sqlserver://{host}[:{port}][;databaseName={database}];trustServerCertificate=true", "http://msdn.microsoft.com/en-us/sqlserver/aa937724", "", ""),	
	H2("H2", "org.h2.Driver", 8082, "jdbc:h2:tcp://{server}[:{port}]") {
		// see http://www.h2database.com/html/features.html
		// jdbc:h2:tcp://<server>[:<port>]/[<path>]<databaseName>[;USER=<username>][;PASSWORD=<value>]
		private final static String URL_PREFIX = "jdbc:h2:";
		
		@Override public String getURL(ServerConfig sc) {
			String s = URL_PREFIX;
			if(sc.getPort() == 0) {
				return sc.getDatabase(); // If port is zero that means Database contains full JDBC URL
			} else if(sc.getPort()!=9000 || !sc.getHost().equals("localhost")) {
				s += "tcp://" + sc.getHost() + ":" + sc.getPort() + "/";
			}
			s += sc.getDatabase();
			s += ";DB_CLOSE_DELAY=-1";
			return s;
		}
	},
	
	MYSQL("MySQL", "com.mysql.jdbc.Driver", 3306, "jdbc:mysql://{host}:{port}/{database}?allowMultiQueries=true", "http://mysql.com", "", ""),	
	REDIS("Redis", "jdbc.RedisDriver", 6379, "jdbc:redis://{host}:{port}[/{database}]", "", "", ""),
	
	DUCKDB("DuckDB", "org.duckdb.DuckDBDriver", 0, "jdbc:duckdb:{file}") {
		private final static String URL_PREFIX = "jdbc:duckdb:";
		@Override public String getURL(ServerConfig sc) {
			if(sc.getPort() == 0) {
				return sc.getDatabase(); // If port is zero that means Database contains full JDBC URL
			}
			return URL_PREFIX;
		}
	},
	
	// Imported from DBeaver settings
	// Convert XML -> excel -> Rearrange excel (see /qstudio/ folder) -> Edit -> Paste into code
	// see /qstudio-all/jardownloader
	//	If adding or modifying, UNCOMMENT and RUN TESTS IN JdbcTypesTest
	DB2_ISERIES("Db2 for IBM i","com.ibm.as400.access.AS400JDBCDriver",446,"jdbc:as400://{host};[libraries={database};]","","maven:/net.sf.jt400:jt400:RELEASE","net/sf/jt400/jt400/20.0.0/jt400-20.0.0.jar"),
	INFORMIX("Informix","com.informix.jdbc.IfxDriver",1533,"jdbc:informix-sqli://{host}:{port}/{database}:INFORMIXSERVER={server}","https://www.developers.net/ibmshowcase/focus/Informix","maven:/com.ibm.informix:jdbc:RELEASE[4.50.4.1]","com/ibm/informix/jdbc/4.50.4.1/jdbc-4.50.4.1.jar"),
	DERBY("Derby Embedded","org.apache.derby.jdbc.EmbeddedDriver",0,"jdbc:derby:{folder}","https://db.apache.org/","maven:/org.apache.derby:derby:RELEASE[10.15.2.0]","org/apache/derby/derby/10.15.2.0/derby-10.15.2.0.jar"),
	DERBY_SERVER("Derby Server","org.apache.derby.client.ClientDriver",1527,"jdbc:derby://{host}:{port}/{database};create=false","http://db.apache.org/","maven:/org.apache.derby:derby:RELEASE[10.15.2.0]","org/apache/derby/derby/10.15.2.0/derby-10.15.2.0.jar"),
	HSQLDB_SERVER("HSQL Server","org.hsqldb.jdbcDriver",9001,"jdbc:hsqldb:hsql://{host}[:{port}]/[{database}]","http://hsqldb.org/","maven:/org.hsqldb:hsqldb:RELEASE","org/hsqldb/hsqldb/2.7.2/hsqldb-2.7.2-jdk8.jar"),
	HSQLDB_EMBEDDED("HSQL Embedded","org.hsqldb.jdbc.JDBCDriver",0,"jdbc:hsqldb:file:{folder}","http://hsqldb.org/","maven:/org.hsqldb:hsqldb:RELEASE","org/hsqldb/hsqldb/2.7.2/hsqldb-2.7.2-jdk8.jar"),
	SQLITE_JDBC("SQLite","org.sqlite.JDBC",0,"jdbc:sqlite:{file}","https://github.com/xerial/sqlite-jdbc","maven:/org.xerial:sqlite-jdbc:RELEASE","org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar"),
	TERADATA("Teradata","com.teradata.jdbc.TeraDriver",1025,"jdbc:teradata://{host}/DATABASE={database},DBS_PORT={port}","https://downloads.teradata.com/download/connectivity/jdbc-driver","maven:/com.teradata.jdbc:terajdbc:RELEASE","com/teradata/jdbc/terajdbc/20.00.00.11/terajdbc-20.00.00.11.jar"),
	CSVJDBC("CSV","org.relique.jdbc.csv.CsvDriver",0,"jdbc:relique:csv:{folder}","http://csvjdbc.sourceforge.net/","maven:/net.sourceforge.csvjdbc:csvjdbc:RELEASE","net/sourceforge/csvjdbc/csvjdbc/1.0.40/csvjdbc-1.0.40.jar"),
	MSACCESS_UCANACCESS("MS Access (UCanAccess)","net.ucanaccess.jdbc.UcanaccessDriver",0,"jdbc:ucanaccess://{file}","http://ucanaccess.sourceforge.net/site.html","maven:/net.sf.ucanaccess:ucanaccess:RELEASE","net/sf/ucanaccess/ucanaccess/5.0.1/ucanaccess-5.0.1.jar"),
	NUODB("NuoDB","com.nuodb.jdbc.Driver",2000,"jdbc:com.nuodb://{host}[:{port}]/[{database}]","http://www.nuodb.com/","maven:/com.nuodb.jdbc:nuodb-jdbc:RELEASE","com/nuodb/jdbc/nuodb-jdbc/24.1.0/nuodb-jdbc-24.1.0.jar"),
	SAP_HANA("HANA (Old)","com.sap.db.jdbc.Driver",30015,"jdbc:sap://{host}[:{port}]","http://scn.sap.com/community/developer-center/hana","maven:/com.sap.cloud.db.jdbc:ngdbc:RELEASE","com/sap/cloud/db/jdbc/ngdbc/2.17.10/ngdbc-2.17.10.jar"),
	GEMFIRE_XD("Gemfire XD","com.pivotal.gemfirexd.jdbc.ClientDriver",1527,"jdbc:gemfirexd://{host}[:{port}]/","http://blog.pivotal.io/tag/gemfire-xd","maven:/io.snappydata:gemfirexd-client:RELEASE","io/snappydata/gemfirexd-client/2.0-BETA/gemfirexd-client-2.0-BETA.jar"),
	SNAPPYDATA("SnappyData","io.snappydata.jdbc.ClientDriver",1528,"jdbc:snappydata://{host}[:{port}]/","https://snappydatainc.github.io/snappydata/","maven:/io.snappydata:snappydata-store-client:RELEASE","io/snappydata/snappydata-store-client/1.6.7/snappydata-store-client-1.6.7.jar"),
	SPARK_HIVE("Apache Spark","org.apache.hive.jdbc.HiveDriver",10000,"jdbc:hive2://{host}[:{port}][/{database}]","https://jaceklaskowski.gitbooks.io/mastering-apache-spark/spark-sql-thrift-server.html","maven:/org.spark-project.hive:hive-jdbc:RELEASE","org/spark-project/hive/hive-jdbc/1.2.1.spark2/hive-jdbc-1.2.1.spark2.jar"),
	KYUUBI_HIVE("Apache Kyuubi","org.apache.kyuubi.jdbc.KyuubiHiveDriver",10009,"jdbc:hive2://{host}[:{port}][/{database}]","https://kyuubi.apache.org/","maven:/org.apache.kyuubi:kyuubi-hive-jdbc-shaded:RELEASE","org/apache/kyuubi/kyuubi-hive-jdbc-shaded/1.7.1/kyuubi-hive-jdbc-shaded-1.7.1.jar"),
	YANDEX_CLICKHOUSE("ClickHouse (Yandex)","ru.yandex.clickhouse.ClickHouseDriver",8123,"jdbc:clickhouse://{host}:{port}[/{database}]","https://github.com/yandex/clickhouse-jdbc","maven:/ru.yandex.clickhouse:clickhouse-jdbc:RELEASE[0.2.6]","ru/yandex/clickhouse/clickhouse-jdbc/0.2.6/clickhouse-jdbc-0.2.6.jar"),
	NEO4J("Neo4j","org.neo4j.jdbc.Driver",7687,"jdbc:neo4j:bolt://{host}[:{port}]/","https://github.com/neo4j-contrib/neo4j-jdbc","maven:/org.neo4j:neo4j-jdbc-driver:RELEASE","org/neo4j/neo4j-jdbc-driver/4.0.9/neo4j-jdbc-driver-4.0.9.jar"),
	PRESTO("PrestoSQL","io.prestosql.jdbc.PrestoDriver",8080,"jdbc:presto://{host}:{port}[/{database}]","https://prestosql.io/","maven:/io.prestosql:presto-jdbc:RELEASE","io/prestosql/presto-jdbc/350/presto-jdbc-350.jar"),
	TRINO("Trino","io.trino.jdbc.TrinoDriver",8080,"jdbc:trino://{host}:{port}[/{database}]","https://trino.io/","maven:/io.trino:trino-jdbc:RELEASE","io/trino/trino-jdbc/422/trino-jdbc-422.jar"),
	APACHE_SOLRJ("Solr","org.apache.solr.client.solrj.io.sql.DriverImpl",9983,"jdbc:solr://{host}:{port}/[?collection={database}]","https://lucene.apache.org/solr/","maven:/org.apache.solr:solr-solrj:RELEASE","org/apache/solr/solr-solrj/9.2.1/solr-solrj-9.2.1.jar"),
	APACHE_IGNITE("Apache Ignite","org.apache.ignite.IgniteJdbcThinDriver",1000,"jdbc:ignite:thin://{host}[:{port}][;schema={database}]","https://apacheignite-sql.readme.io/docs/jdbc-driver","maven:/org.apache.ignite:ignite-core:RELEASE","org/apache/ignite/ignite-core/2.15.0/ignite-core-2.15.0.jar"),
	OMNISCI("OmniSci (formerly MapD)","com.omnisci.jdbc.OmniSciDriver",6274,"jdbc:omnisci:{host}:{port}:{database}","https://docs.omnisci.com/v5.1.1/6_jdbc.html","maven:/com.omnisci:omnisci-jdbc:RELEASE","com/omnisci/omnisci-jdbc/5.10.0/omnisci-jdbc-5.10.0.jar"),
	CRATEDB("CrateDB (Legacy)","io.crate.client.jdbc.CrateDriver",5432,"crate://{host}[:{port}]/","https://crate.io/docs/clients/jdbc/en/latest/","maven:/io.crate:crate-jdbc:RELEASE","io/crate/crate-jdbc/2.7.0/crate-jdbc-2.7.0.jar"),
	SQREAM("SQream DB","com.sqream.jdbc.SQDriver",3108,"jdbc:Sqream://{host}:{port}/{database};cluster=true","https://docs.sqream.com/en/latest/guides/client_drivers/jdbc/index.html","maven:/com.sqream:sqream-jdbc:RELEASE","com/sqream/sqream-jdbc/4.5.9/sqream-jdbc-4.5.9.jar"),
	APACHE_CALCITE_AVATICA("Apache Calcite Avatica","org.apache.calcite.avatica.remote.Driver",8082,"jdbc:avatica:remote:url=http://{host}:{port}/druid/v2/sql/avatica/","https://calcite.apache.org/avatica/docs/client_reference.html","maven:/org.apache.calcite.avatica:avatica-core:RELEASE[1.17.0]","org/apache/calcite/avatica/avatica-core/1.17.0/avatica-core-1.17.0.jar"),
	APACHE_KYLIN("Apache Kylin","org.apache.kylin.jdbc.Driver",443,"jdbc:kylin://{host}:{port}/{database}","https://kylin.apache.org/docs23/howto/howto_jdbc.html","maven:/org.apache.kylin:kylin-jdbc:RELEASE","org/apache/kylin/kylin-jdbc/5.0.0-alpha/kylin-jdbc-5.0.0-alpha.jar"),
	SNOWFLAKE("Snowflake","net.snowflake.client.jdbc.SnowflakeDriver",443,"jdbc:snowflake://{host}[:port]/?[db={database}]","https://docs.snowflake.net/manuals/user-guide/jdbc-configure.html","maven:/net.snowflake:snowflake-jdbc:RELEASE[3.13.6]","net/snowflake/snowflake-jdbc/3.13.6/snowflake-jdbc-3.13.6.jar"),
	CLICKHOUSE_COM("ClickHouse.com","com.clickhouse.jdbc.ClickHouseDriver",8123,"jdbc:ch:{host}:{port}[/{database}]","https://github.com/ClickHouse/clickhouse-java","maven:/com.clickhouse:clickhouse-jdbc:RELEASE[0.8.6]","com/clickhouse/clickhouse-jdbc/0.8.6/clickhouse-jdbc-0.8.6.jar"),
	ELASTICSEARCH("Elasticsearch","org.elasticsearch.xpack.sql.jdbc.EsDriver",9200,"jdbc:es://{host}:{port}/","https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-jdbc.html","maven:/org.elasticsearch.plugin:x-pack-sql-jdbc:7.9.1","org/elasticsearch/plugin/x-pack-sql-jdbc/7.9.1/x-pack-sql-jdbc-7.9.1.jar"),

	DOLPHINDB("DolphinDB","com.dolphindb.jdbc.Driver",9200,"jdbc:dolphindb://{host}:{port}","https://dolphindb.com/","maven:/com.dolphindb:jdbc:3.00.0.1","com/dolphindb/jdbc/3.00.0.1/jdbc-3.00.0.1-jar-with-dependencies.jar"),
	// Special but potentially popular cases 
	MONGODB("MongoDB","com.mongodb.jdbc.MongoDriver",27017,"jdbc:mongodb://{host}[:{port}][/{database}]","https://mongodb.com","https://repo1.maven.org/maven2/org/mongodb/mongodb-jdbc/2.0.2/mongodb-jdbc-2.0.2-all.jar", "org/mongodb/mongodb-jdbc/2.0.2/mongodb-jdbc-2.0.2-all.jar"),
	// InfluxDB v3 using Arrow Flight SQL JDBC Driver
	// InfluxDB v1 and v2 not supported
	// Example URL:
	// jdbc:arrow-flight-sql://localhost:8181?useEncryption=false&disableCertificateVerification=true&database=demo
	// https://arrow.apache.org/docs/12.0/java/flight_sql_jdbc_driver.html
	// The driver is compatible with JDK 8+. On JDK 9+, the following JVM parameter is required:
	// java --add-opens=java.base/java.nio=ALL-UNNAMED ...
	INFLUXDB("InfluxDB v3","org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver",8181,"jdbc:arrow-flight-sql://{host}:{port}?useEncryption=false&disableCertificateVerification=true&database={database}&token={password}","https://docs.influxdata.com/influxdb3/core/reference/client-libraries/flight/","https://repo1.maven.org/maven2/org/apache/arrow/flight-sql-jdbc-driver/12.0.1/flight-sql-jdbc-driver-12.0.1.jar","org/apache/arrow/flight-sql-jdbc-driver/12.0.1/flight-sql-jdbc-driver-12.0.1.jar"){
		// 重载以添加 token 参数
		// 使用 password 字段来保存 token
		//  InfluxDB Arrow Flight SQL 驱动的认证机制要求：
		//   - 必须通过Properties的password字段传递token
		//   - URL中的token参数可能是次要的或被忽略的
		//   - 当Properties的password为空时，无论URL中是否有token，认证都会失败
		//
		// Override to add token parameter
		// use password field to hold token
		// The authentication mechanism of the InfluxDB Arrow Flight SQL driver requires:
		//   - The token must be passed via the password field in Properties
		//   - The token parameter in the URL may be secondary or ignored
		//   - If the password in Properties is empty, authentication will fail regardless of 
		// whether the URL contains a token
		@Override public String getURL(ServerConfig sc) {
			if(sc.getPort() == 0) {
				// URL模式：
				// 从URL中移除token参数，必须通过 password 传递
				//
				// If port is zero, use URL
				// URL mode: remove the token parameter from the URL, 
				// passed via the password in Properties
				String url = sc.getDatabase();
				if(url.contains("token=") && sc.getPassword() != null && !sc.getPassword().isEmpty()) {
					url = url.replaceAll("&?token=[^&]*", "");
					if(url.endsWith("?") || url.endsWith("&")) {
						url = url.substring(0, url.length() - 1);
					}
				}
          		return url;
			}
			String s = getSampleURL();
			s = replaceK(s, "host", sc.getHost());
			s = replaceK(s, "port", ""+sc.getPort());
			s = replaceK(s, "database", sc.getDatabase());
			s = replaceK(s, "password", sc.getPassword());
			return s;
		}
	},

	TDENGINE("TDengine","com.taosdata.jdbc.rs.RestfulDriver",6041,"jdbc:TAOS-RS://{host}:{port}/[{database}]","http://www.tdengine.com","maven:/com.taosdata.jdbc:taos-jdbcdriver:RELEASE[3.2.4]","com/taosdata/jdbc/taos-jdbcdriver/3.2.4/taos-jdbcdriver-3.2.4-dist.jar"),
	ORACLE("Oracle","oracle.jdbc.driver.OracleDriver",1521,"jdbc:oracle:thin:@{host}:{port}/[{database}]","https://www.oracle.com/uk/database/","maven:/com.oracle.database.jdbc:ojdbc8:RELEASE[19.19.0.0]","/com/oracle/database/jdbc/ojdbc8/19.19.0.0/ojdbc8-19.19.0.0.jar"),
	BABELDB("BabelDB", "com.timestored.babeldb.BabelDBJdbcDriver", 80, "jdbc:babeldb:"),
	REDSHIFT("RedShift","com.amazon.redshift.jdbc.Driver",5439,"jdbc:redshift:{host}:{port}/[{database}]","https://aws.amazon.com/redshift/","maven:/com.amazon.redshift:redshift-jdbc42:RELEASE[2.1.0.28]","/com/amazon/redshift/redshift-jdbc42/2.1.0.28/redshift-jdbc42-2.1.0.28.jar")
	;


	
	
	private boolean databaseRequired;
	private int defaultPort;
	/** A formatted text that users / websites would recognise as the database name **/
	private String niceName;



	JdbcTypes(@NonNull String niceName, @NonNull String driver, int defaultPort, String sampleURL) {
		this.niceName = Preconditions.checkNotNull(niceName);
		this.driver = Preconditions.checkNotNull(driver);
		this.defaultPort = defaultPort;
		this.databaseRequired = sampleURL != null && sampleURL.toLowerCase().contains("{database}");
		this.sampleURL = Preconditions.checkNotNull(sampleURL);
		this.webURL = "";
		this.mavenURL = "";
		this.downloadURL = "";
	}	
	
	JdbcTypes(@NonNull String niceName, @NonNull String driver, int defaultPort, String sampleURL, String webURL, String mavenURL, String downloadURL) {
		this.niceName = Preconditions.checkNotNull(niceName);
		this.driver = Preconditions.checkNotNull(driver);
		this.defaultPort = defaultPort;
		this.databaseRequired = sampleURL != null && sampleURL.toLowerCase().contains("{database}");
		this.sampleURL = Preconditions.checkNotNull(sampleURL);
		this.webURL = Preconditions.checkNotNull(webURL);
		this.mavenURL = Preconditions.checkNotNull(mavenURL);
		this.downloadURL = Preconditions.checkNotNull(downloadURL);
	}

	private final String driver;
	@Getter private final String sampleURL;
	@Getter private final String webURL;
	@Getter private final String mavenURL;
	private final String downloadURL;

	private static final String TSU = "https://www.timestored.com/jdbc/drivers/";
	private static final String MVN = "https://repo1.maven.org/maven2/";
	
	public List<String> getDownloadURLs() {
		return (downloadURL == null || downloadURL.trim().isEmpty()) ? Collections.emptyList() :
			Lists.newArrayList(TSU+downloadURL, MVN+downloadURL);
	}

	public String getURL(ServerConfig sc) {
		if(sc.getPort() == 0) {
			return sc.getDatabase(); // If port is zero that means Database contains full JDBC URL
		}
		String s = this.sampleURL;
		s = replaceK(s, "host", sc.getHost());
		s = replaceK(s, "port", ""+sc.getPort());
		s = replaceK(s, "database", sc.getDatabase());
		if(sc.getPort() == 0) {
			s = replaceK(s, "folder", sc.getHost());
			s = replaceK(s, "file", sc.getHost());
		}
		return s;
	}
	
	private static String replaceK(String s, String key, String replacement) {
		String k = "{"+key+"}";
		int p = s.indexOf(k);
		String r = s;
		if(p > -1) {
			String pre = s.substring(0, p);
			String post = s.substring(p+k.length());
			boolean withinOptional = pre.lastIndexOf('[') > pre.lastIndexOf(']');
			// jdbc:hive2://{host}[:{port}][/{database}]
			// replace square brackets with nothing ELSE with inner part.
			if(withinOptional) {
				int b = pre.lastIndexOf('[');
				int a = p+k.length() + post.indexOf(']');
				String rep = replacement.trim().isEmpty() ? "" : s.substring(b+1, a);
				rep = rep.replace(k, replacement);
				r = s.substring(0, b) + rep + s.substring(a+1); 
			} else {
				r = s.replace(k, replacement);	
			}
		}
		return r;
	}

	/**  @return string used at start of line to escape remainder of line as comment. */
	public String getComment() { return "-- ";	}
	
	/** 
	 * Escape single line comment as required for particular database.
	 * @return comment escaped as required by specific database 
	 */
	public String getComment(String singleLineCommentTxt) {
		if(singleLineCommentTxt.contains("\r") || singleLineCommentTxt.contains("\n")) {
			throw new IllegalArgumentException("single lines only permitted");
		}
		return "/* " + singleLineCommentTxt + " */";
	}

	/** @return Formatted text that users / websites would recognise as the database name **/
	public String getNiceName() { return niceName;	}
	
	/** @return true if this type of jdbc database requires a db in its connection url **/
	public boolean isDatabaseRequired() { return databaseRequired; }

	private static String standardURL(ServerConfig sc, String urlprefix) {
		String s = urlprefix + sc.getHost() + ":" + sc.getPort();
		if(sc.getDatabase() != null && sc.getDatabase().trim().length() > 0) {
			 return s+"/" + sc.getDatabase();
		}
		return s;
	}	
	
	/** A string giving the full class name of the jdbc driver **/
	public String getDriver() { return driver; }
	
	/** @return the port used by default for this database **/
	public int getDefaultPort() { return defaultPort; }

	/** @return true if this represents a kdb connection type **/
	public boolean isKDB() { return false; }

	/** @return true if this represents a streaming connection type **/
	public boolean isStreaming() { return false; }

	/** @return true if this JDBC driver is available **/
	public boolean isAvailable() {
		try {
			Class<?> c = Class.forName(driver, false, getClass().getClassLoader());
			return c != null;
		} catch (ClassNotFoundException e) {}
		return false;
	}
	
	/** 
	 * @return If some kind of third party is used to lookup passwords, e.g. LDAP etc
	 * this will return that interface otherwise null.
	 */
	public DatabaseAuthenticationService getAuthenticator() { return null;	}
	
	

	private static String getProperty(String name, String defaultVal) {
		String a = System.getProperty(name);
		return a == null ? defaultVal : a;
	}
	
	private static int getProperty(String name, int defaultVal) {
		String a = System.getProperty(name);
		if(a != null) {
			try {
				return Integer.parseInt(a);
			} catch(NumberFormatException e) { }
		}
		return defaultVal;
	}
	
	private static boolean getProperty(String name, boolean defaultVal) {
		String a = System.getProperty(name);
		if(a != null) {
			try {
				return Boolean.parseBoolean(a);
			} catch(NumberFormatException e) { }
		}
		return defaultVal;
	}
}
