package com.timestored.connections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import org.junit.Test;

import com.timestored.plugins.PluginLoader;

public class IoTDBRemoteSmokeTest {

	@Test
	public void loadsDriverConnectsAndReadsTreeMetadata() throws Exception {
		String portText = System.getenv("IOTDB_TEST_PORT");
		assumeTrue("Set IOTDB_TEST_PORT to a local IoTDB RPC tunnel to opt in", portText != null && !portText.trim().isEmpty());
		int port = Integer.parseInt(portText);

		String previousHome = System.getProperty("user.home");
		String previousDirectory = System.getProperty("user.dir");
		Path pluginHome = Files.createTempDirectory("qstudio-iotdb-smoke-");
		String database = "root.dbeco_qstudio_" + UUID.randomUUID().toString().replace("-", "");
		String device = database + ".sensor";
		boolean created = false;
		try {
			System.setProperty("user.home", pluginHome.toString());
			System.setProperty("user.dir", pluginHome.resolve("work").toString());
			Files.createDirectories(pluginHome.resolve("work"));
			PluginLoader.installDriver(ConnectionManager.APP_TITLE, JdbcTypes.IOTDB);

			Class<?> driverClass = PluginLoader.getCClass(ConnectionManager.APP_TITLE.toLowerCase(), JdbcTypes.IOTDB.getDriver());
			Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
			ServerConfig config = new ServerConfig("127.0.0.1", port, "root", "root", "iotdb-smoke", JdbcTypes.IOTDB);
			Properties properties = ConnectionManager.getDriverProperties(config);
			try (Connection connection = driver.connect(config.getUrl(), properties); Statement statement = connection.createStatement()) {
				assertTrue("QStudio's configured driver should accept its IoTDB URL", driver.acceptsURL(config.getUrl()));
				statement.execute("CREATE DATABASE " + database);
				created = true;
				statement.execute("CREATE TIMESERIES " + device + ".temperature WITH DATATYPE=DOUBLE, ENCODING=PLAIN, COMPRESSOR=LZ4");
				statement.execute("CREATE TIMESERIES " + device + ".status WITH DATATYPE=TEXT, ENCODING=PLAIN, COMPRESSOR=LZ4");
				long timestamp = System.currentTimeMillis();
				statement.execute("INSERT INTO " + device + "(time,temperature,status) VALUES (" + timestamp + ",18.75,'ready')");

				boolean foundTemperature = false;
				try (ResultSet rows = statement.executeQuery("SHOW TIMESERIES " + database + ".**")) {
					assertTrue("IoTDB Tree metadata should expose at least four columns", rows.getMetaData().getColumnCount() >= 4);
					while (rows.next()) {
						String series = rows.getString(1);
						String dataType = rows.getString(4);
						MetaInfo.ColumnInfo column = MetaInfo.fromIoTDBTreeSeries(series, dataType);
						if (column != null && column.getColumnName().equals("temperature")) {
							assertEquals(device, column.getTableName());
							assertEquals("DOUBLE", column.getTypeName());
							foundTemperature = true;
						}
					}
				}
				assertTrue("QStudio should map the live temperature timeseries into a device column", foundTemperature);
			}
		} finally {
			try {
				if (created) dropDatabase(pluginHome, port, database);
			} finally {
				try {
					closePluginClassLoader();
				} finally {
					restoreProperty("user.home", previousHome);
					restoreProperty("user.dir", previousDirectory);
					deleteTree(pluginHome);
				}
			}
		}
	}

	private static void dropDatabase(Path pluginHome, int port, String database) throws Exception {
		System.setProperty("user.home", pluginHome.toString());
		System.setProperty("user.dir", pluginHome.resolve("work").toString());
		Class<?> driverClass = PluginLoader.getCClass(ConnectionManager.APP_TITLE.toLowerCase(), JdbcTypes.IOTDB.getDriver());
		Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
		ServerConfig config = new ServerConfig("127.0.0.1", port, "root", "root", "iotdb-smoke", JdbcTypes.IOTDB);
		try (Connection connection = driver.connect(config.getUrl(), ConnectionManager.getDriverProperties(config));
				Statement statement = connection.createStatement()) {
			statement.execute("DROP DATABASE " + database);
		}
	}

	private static void closePluginClassLoader() throws Exception {
		Field latestLoader = PluginLoader.class.getDeclaredField("latestLoader");
		latestLoader.setAccessible(true);
		Object loader = latestLoader.get(null);
		if (loader instanceof URLClassLoader) ((URLClassLoader) loader).close();
		latestLoader.set(null, null);
		Field loaders = PluginLoader.class.getDeclaredField("urlsToClassLoaders");
		loaders.setAccessible(true);
		((java.util.Map<?, ?>) loaders.get(null)).clear();
	}

	private static void restoreProperty(String name, String value) {
		if (value == null) System.clearProperty(name);
		else System.setProperty(name, value);
	}

	private static void deleteTree(Path root) throws IOException {
		if (root == null || !Files.exists(root)) return;
		try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
			paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
				try { Files.deleteIfExists(path); }
				catch (IOException e) { throw new java.io.UncheckedIOException(e); }
			});
		} catch (java.io.UncheckedIOException e) {
			throw e.getCause();
		}
	}
}
