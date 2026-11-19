package com.timestored.connections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

public class JdbcTypesIoTDBTest {

	@Test
	public void configuresDriverUrlAndBundledDriver() {
		ServerConfig withoutDatabase = new ServerConfig("iotdb.local", 6667, "", "", "iotdb", JdbcTypes.IOTDB);
		ServerConfig withDatabase = new ServerConfig("iotdb.local", 6667, "", "", "iotdb", JdbcTypes.IOTDB, null, "root.sg");

		assertEquals("org.apache.iotdb.jdbc.IoTDBDriver", JdbcTypes.IOTDB.getDriver());
		assertEquals(6667, JdbcTypes.IOTDB.getDefaultPort());
		assertEquals("jdbc:iotdb://iotdb.local:6667", withoutDatabase.getUrl());
		assertEquals("jdbc:iotdb://iotdb.local:6667/root.sg", withDatabase.getUrl());
		assertEquals(JdbcTypes.values().length - 1, JdbcTypes.IOTDB.ordinal());

		List<String> downloadUrls = JdbcTypes.IOTDB.getDownloadURLs();
		assertEquals("https://repo1.maven.org/maven2/org/apache/iotdb/iotdb-jdbc/2.0.11/iotdb-jdbc-2.0.11-jar-with-dependencies.jar",
				downloadUrls.get(1));
	}

	@Test
	public void usesTheApacheDatabaseIcon() {
		assertSame(JdbcIcons.DB_APACHE, JdbcIcons.getIconFor(JdbcTypes.IOTDB));
	}

	@Test
	public void leavesDefaultIoTDBCredentialsToTheDriver() {
		ServerConfig withoutCredentials = new ServerConfig("iotdb.local", 6667, "", "", "iotdb", JdbcTypes.IOTDB);
		Properties defaults = ConnectionManager.getDriverProperties(withoutCredentials);

		assertFalse(defaults.containsKey("user"));
		assertFalse(defaults.containsKey("password"));
	}

	@Test
	public void passesExplicitIoTDBCredentials() {
		ServerConfig configured = new ServerConfig("iotdb.local", 6667, "root", "secret", "iotdb", JdbcTypes.IOTDB);
		Properties properties = ConnectionManager.getDriverProperties(configured);

		assertEquals("root", properties.getProperty("user"));
		assertEquals("secret", properties.getProperty("password"));
	}

	@Test
	public void preservesEmptyCredentialsForOtherDrivers() {
		ServerConfig postgres = new ServerConfig("postgres.local", 5432, "", "", "postgres", JdbcTypes.POSTGRES);
		Properties properties = ConnectionManager.getDriverProperties(postgres);

		assertTrue(properties.containsKey("user"));
		assertTrue(properties.containsKey("password"));
		assertEquals("", properties.getProperty("user"));
		assertEquals("", properties.getProperty("password"));
	}
}
