package com.timestored.sqldash;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.sql.rowset.CachedRowSet;

import joptsimple.OptionException;
import joptsimple.OptionSet;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ServerConfig;
import com.timestored.misc.CmdRunner;
import com.timestored.sqldash.chart.ChartFormatException;
import com.timestored.sqldash.chart.ChartUtils;

/**
 * Facade that allows generating kdb charts or dashboard actions from the
 * command line.
 */
public class SqlChart {

	private static final Logger LOG = Logger.getLogger(SqlChart.class.getName());
	
	public static void main(String... args) {

		// dont print any logs to the console
		Logger globalLogger = Logger.getLogger("");
		Handler[] handlers = globalLogger.getHandlers();
		for(Handler handler : handlers) {
		    globalLogger.removeHandler(handler);
		}
		
		System.exit(run(args));
	}

	/**
	 * Static run method to allow for use within a test, where system.exit makes things difficult.
	 * @return The exit status for reporting to the OS.
	 */
	public static int run(String... args) {
		
		int exitCode = 0;
//		args = "-?".split(" ");
//		args = " --host localhost --port 5000 --user bob --pass jim --servertype kdb --execute \"([] a:til 10)\"".split(" ");
		
		if(args.length > 0) {
			// generate chart, catching any exceptions.
			Exception ex = null;
		    try {
				LOG.info("Generating Chart for args: " + Joiner.on(" ").join(args));
		    	OptionSet o = ChartParams.parse(args);
				if(o.has("?")) {
					try {
						ChartParams.printHelpOn(System.out);
					} catch (IOException e) {
						throw new IOException("Error displaying help.");
					}
				} else {
					generate(ChartParams.getChartParams(o));
				}
			} catch (IOException e) {
				ex = e;
			} catch (SQLException e) {
				ex = new SQLException("SQL Error: " + e.getMessage());
			} catch (OptionException e) {
				ex = e;
			} catch (IllegalArgumentException e) {
				ex = e;
			}
		    
		    // display simplified error message with help
		    if(ex != null) {
		    	System.err.println(ex.getMessage());
				exitCode = 1;
		    }
		} else {
			//p.formatHelpWith(new HtmlHelpFormatter());
			try {
				ChartParams.printHelpOn(System.out);
			} catch (IOException e) {
				System.err.println("Error printing help.");
			}
		}
		
		return exitCode;
	}
	/** 
	 * Generate the chart for a given string of command line arguments.
	 * Usually arg splitting is OS dependent so how this function works is a
	 * rough approximation of windows, use ONLY for testing purposes.
	 */
	public static int testGenerate(String arg) {
		return run(CmdRunner.parseCommand(arg));
	}

	/**
	 * Given a set of arguments generate a chart saved to relevant .png file.
	 */
	public static void  generate(ChartParams chartParams) throws IOException, SQLException {

			ServerConfig sc = chartParams.serverConfig;
			try {
				Class.forName(sc.getJdbcType().getDriver());
			} catch (ClassNotFoundException e) {
				throw new IOException("Database driver could not be loaded.");
			}
			try {
				ConnectionManager connMan = ConnectionManager.newInstance();
				connMan.addServer(sc);
				Connection conn = DriverManager.getConnection(sc.getUrl(), sc.getUsername(), sc.getPassword());
				CachedRowSet rs = connMan.executeQuery(sc, chartParams.query);

			    File file = chartParams.file;
			    try {
					Files.createParentDirs(file);
					ChartUtils.save(chartParams.viewStrategy, rs, file, 
							chartParams.width, chartParams.height, chartParams.chartTheme);
				    System.out.println(file.getAbsolutePath());
				} catch (ChartFormatException e) {
					String msg = "Error: " + e.getMessage() + "\r\n\r\nFormat Expected:\r\n";
					msg += chartParams.viewStrategy.getFormatExplaination();
					throw new IOException(msg);
				} catch (IOException e) {
					throw new IOException("Error creating necessary output folders/files: " + file.getPath());
				}
				
			} catch (SQLException e) {
				throw new IOException("Error getting database connection: " + e.getMessage());
			}
	}
	

}
