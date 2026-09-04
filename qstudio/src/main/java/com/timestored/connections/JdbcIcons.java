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

package com.timestored.connections;

import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import com.timestored.connections.JdbcTypes;
import com.timestored.theme.Icon;
import com.timestored.theme.IconHelper;


public enum JdbcIcons implements Icon {
	DB_ATHENA("athena_icon.png"),
	DB_AZURE("azure_icon.png"),
	DB_BIGQUERY("bigquery_icon.png"),
	DB_CLICKHOUSE("clickhouse_icon.png"),
	DB_COCKROACH("cockroach_icon.png"),
	DB_DATABRICKS("databricks_icon.png"),
	DB_DREMIO("dremio_icon.png"),
	DB_DUCKDB("duckdb_icon.png"),
	DB_GREENPLPUM("greenplum_icon.png"),
	DB_H2("h2_icon@2x.png"),
	DB_KX("kx_icon.png"),
	DB_MARIADB("mariadb_icon.png"),
	DB_MATERIALIZE("materialize_icon.png"),
	DB_MSSQL("mssql_icon.png"),
	DB_MYSQL("mysql_icon.png"),
	DB_NEO4J("neo4j_icon.png"),
	DB_ORACLE("oracle_icon.png"),
	DB_POSTGRESQL("postgresql_icon.png"),
	DB_PRESTO("presto_icon.png"),
	DB_REDSHIFT("redshift_icon.png"),
	DB_RISINGWAVE("risingwave_icon.png"),
	DB_SNAPPY("snappy_icon.png"),
	DB_SNOWFLAKE("snowflake_icon.png"),
	DB_SPANNER("spanner_icon.png"),
	DB_SQLITE("sqlite_icon.png"),
	DB_STARROCKS("starrocks_icon.png"),
	DB_SYBASE("sybase_icon.png"),
	DB_TDENGINE("tdengine_icon.png"),
	DB_TIDB("tidb_icon.png"),
	DB_TIMESCALE("timescale_icon.png"),
	DB_VERTICA("vertica_icon.png"),
	DB_YUGABYTE("yugabyte_icon.png"),
	DB_APACHE("apache_icon.png"),
	DB_CALCITE("calcite_icon_big.png"),
	DB_CSV("csv_icon.png"),
	DB_DB2("db2_icon.png"),
	DB_DERBY("derby_icon.png"),
	DB_HIVE("hive_icon.png"),
	DB_IGNITE("ignite_icon.png"),
	DB_KYUUBI("kyuubi_hive_icon.png"),
	DB_SOLR("solr_icon.png"),
	DB_TRINO("trino_icon.png"),
	DB_HANA("sap_hana_icon.png"),
	DB_SPARK("spark_hive_icon.png"),
	DB_CRATEDB("cratedb_icon.png"),
	DB_DATABASE_GENERIC("database_icon.png"),
	DB_ELASTIC("elasticsearch_icon.png"),
	DB_GEMFIRE("gemfire_icon.png"),
	DB_HSQL("hsqldb_icon.png"),
	DB_INFORMIX("informix_icon.png"),
	DB_MSACCESS("msaccess_icon.png"),
	DB_NUODB("nuodb_icon.png"),
	DB_SQREAM("sqream_icon.png"),
	DB_TERADATA("teradata_icon.png"),
	DB_MONGODB("mongodb.png"),
	DB_REDIS("redis.png"),
	DB_DOLPHIN("dolphin.png"),
	DB_RAYFORCE("rayforce_icon.png");
	
	public static JdbcIcons getIconFor(JdbcTypes jdbcTypes) {
		switch(jdbcTypes) {
		case CLICKHOUSE:
		case CLICKHOUSE_COM:
		case YANDEX_CLICKHOUSE: return DB_CLICKHOUSE;
		case DUCKDB: return DB_DUCKDB;
		case H2: return DB_H2;
		case KDB: return DB_KX;
		case MSSERVER: return DB_MSSQL;
		case MYSQL: return DB_MYSQL;
		case NEO4J: return DB_NEO4J;
		case ORACLE: return DB_ORACLE;
		case POSTGRES: return DB_POSTGRESQL;
		case PRESTO: return DB_PRESTO;
		case SNAPPYDATA: return DB_SNAPPY;
		case SNOWFLAKE: return DB_SNOWFLAKE;
		case SQLITE_JDBC: return DB_SQLITE;
		case TDENGINE: return DB_TDENGINE;
		case DOLPHINDB: return DB_DOLPHIN;
		case APACHE_CALCITE_AVATICA: return DB_CALCITE;
		case APACHE_KYLIN: return DB_APACHE;
		case APACHE_IGNITE: return DB_IGNITE;
		case APACHE_SOLRJ: return DB_SOLR;
		case SAP_HANA: return DB_HANA;
		case CSVJDBC: return DB_CSV;
		case DB2_ISERIES: return DB_DB2;
		case DERBY:
		case DERBY_SERVER: return DB_DERBY;
		case KYUUBI_HIVE: return DB_KYUUBI;
		case TRINO: return DB_TRINO;
		case SPARK_HIVE: return DB_SPARK;
		case CRATEDB: return DB_CRATEDB;
		case ELASTICSEARCH: return DB_ELASTIC;
		case GEMFIRE_XD: return DB_GEMFIRE;
		case HSQLDB_EMBEDDED:
		case HSQLDB_SERVER: return DB_HSQL;
		case INFORMIX: return DB_INFORMIX;
		case MSACCESS_UCANACCESS: return DB_MSACCESS;
		case NUODB: return DB_NUODB;
		case SQREAM: return DB_SQREAM;
		case TERADATA: return DB_TERADATA;
		case REDIS: return DB_REDIS;
		case MONGODB: return DB_MONGODB;
		case REDSHIFT: return DB_REDSHIFT;
		case RAYFORCE: return DB_RAYFORCE;
		default: 
		}
		return DB_DATABASE_GENERIC;
	}
	
	private final ImageIcon imageIcon;
	private final ImageIcon imageIcon16;
	public final ImageIcon imageIcon32;

	/** @return Default sized imageIcon */
	public ImageIcon get() { return imageIcon; }
	
	/** @return Size 16*16 imageIcon */
	public ImageIcon get16() { return imageIcon16; }
	
	/** @return Size 32*32 imageIcon */
	public ImageIcon get32() { return imageIcon32; }
	
	
	public BufferedImage getBufferedImage() {
		return IconHelper.getBufferedImage(imageIcon);
	}
	
	JdbcIcons(String loc) {
		ImageIcon[] icons = IconHelper.getDiffSizesOfIcon(JdbcIcons.class.getResource(loc));
		imageIcon = icons[0];
		imageIcon16 = icons[1];
		imageIcon32 = icons[2];
	}
	
}