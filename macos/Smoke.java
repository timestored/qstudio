import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * CI smoke test for the macOS app, run with the app's bundled Java runtime:
 *   java -cp qstudio.jar:sqlite-jdbc.jar Smoke.java path/to/prqlc
 * Checks the native libraries that have broken on macOS before (DuckDB, SQLite) load for this CPU,
 * and that the bundled prqlc runs.
 */
public class Smoke {

	public static void main(String[] args) throws Exception {
		System.out.println("java " + System.getProperty("java.version") + " os.arch=" + System.getProperty("os.arch"));
		System.out.println("duckdb: " + query("jdbc:duckdb:", "SELECT 'duckdb ' || version()"));
		System.out.println("sqlite: " + query("jdbc:sqlite::memory:", "SELECT 'sqlite ' || sqlite_version()"));

		Process p = new ProcessBuilder(args[0], "--version").redirectErrorStream(true).start();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			System.out.println("prqlc: " + r.readLine());
		}
		if (p.waitFor() != 0) {
			throw new IllegalStateException("prqlc failed");
		}
		System.out.println("SMOKE OK");
	}

	private static String query(String url, String sql) throws Exception {
		try (Connection conn = DriverManager.getConnection(url);
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			rs.next();
			return rs.getString(1);
		}
	}
}
