package com.timestored.kdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.dbcp2.PoolableConnection;

import com.google.common.io.Files;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ServerConfig;
import com.timestored.misc.CmdRunner;

import kx.c.KException;

/**
 * Provides functions to start / stop a kdb server to allow testing. 
 */
public class KdbTestHelper {

	private static final Logger LOG = Logger.getLogger(KdbTestHelper.class.getName());
	
	private static final String QHOST = "localhost";
	public static final int QPORT = 15000;
	public static final String TEST_SERVER_NAME = "testServer";
	public static final ServerConfig SERVER_CONFIG = new ServerConfig(QHOST, QPORT, "", "", TEST_SERVER_NAME);
	public static final ServerConfig SERVER_CONFIG_B = new ServerConfig(QHOST, QPORT+1, "", "");
	public static final ServerConfig DISCONNECTED_SERVER_CONFIG = 
			new ServerConfig(QHOST, 10201, "", "", "DISCONNECTED_SERVER");

	private static Process proc;
	private static Process proc_b;
	private static ConnectionManager connMan = ConnectionManager.newInstance();
	private static KdbConnection kdbConn;
	private static PoolableConnection conn;

	private static File latestDir;
	
	/**
	 * Start a q process and return a connection to it, make sure to later call
	 * {@link #killAnyOpenProcesses()} else the process will be left open.
	 * Note any existing process on QPORT will be closed.
	 */
	public static KdbConnection getNewKdbConnection() throws KException, IOException, InterruptedException {
		killAnyOpenProcesses();
		startQ();
		kdbConn = new KdbConnection(QHOST, QPORT);
		return kdbConn;
	}

	private static void startQ() throws IOException, InterruptedException {

		if(proc!=null || proc_b!=null) {
			throw new IllegalStateException("Q Proc already started");
		}
		latestDir = Files.createTempDir();
		// Close when last connection is gone.
//		File tempFile = File.createTempFile(".z.pc:{if[0=count .z.W; exit 0]}", ".q");
//		IOUtils.writeStringToFile(QHOST, tempFile);
		
		proc = CmdRunner.startProc("q -p " + QPORT, latestDir);
		proc_b = CmdRunner.startProc("q -p " + (QPORT+1), latestDir);
		Thread.sleep(200); // give it time to start
		
	}


	private static void l(Exception e) {
		LOG.log(Level.WARNING, "error", e);
	}
	
	/**
	 * Any kdb processes and connections that I started, close them.
	 * Very thorough and uses multiple methods to avoid process leaks. 
	 */
	public static void killAnyOpenProcesses() throws IOException {

		// try using any existing connections to exit / close first.
		if(kdbConn!=null) {
			closeConn(kdbConn);
		} else if(conn!=null) {
			try { conn.createStatement().execute("q)exit 0"); 
				} catch (Exception e) { l(e);	}
			try { 	conn.close(); } catch (Exception e) {l(e);}
			connMan.returnConn(SERVER_CONFIG, conn, true);
		} else {
			// check not left open from previous run
			try {
				closeConn(new KdbConnection(QHOST, QPORT));
			} catch (IOException e1) {
				// hopefully we can't connect so this error should occur
			} catch (KException e) {
				throw new IOException("q proc already open and passworded");
			}
		}

		if(proc!=null) {
			try { proc.destroy(); } catch (Exception e) {l(e);}
		}
		if(proc_b!=null) {
			try { proc_b.destroy(); } catch (Exception e) {l(e);}
		}
		
		proc = null;
		proc_b = null;
		conn = null;
		kdbConn = null;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void closeConn(KdbConnection kc) {
		LOG.info("kdbConn.close()");
		try { kc.send("exit 0"); } catch (Exception e) { l(e);	}
		try { kc.close(); } catch (Exception e) { l(e);	}
	}

	public static String getServerName() { return SERVER_CONFIG.getName(); }

	public static String getServerNameB() { return SERVER_CONFIG_B.getName(); }
	
	
	/**
	 * @return a connection to a default example database, an existing
	 * 			kdb process will be used if possible otherwise
	 * 			a new one will be started.
	 */
	public static Connection getNewConn() throws IOException {
		killAnyOpenProcesses();
		
		try {
			startQ();
			connMan = ConnectionManager.newInstance();
			try {
				connMan.addServer(SERVER_CONFIG);
			} catch(IllegalArgumentException e) { /* do nothing */ }
			try {
				connMan.addServer(SERVER_CONFIG_B);
			} catch(IllegalArgumentException e) { /* do nothing */ }
			
			conn = connMan.getConnection(SERVER_CONFIG);
			
			// this together with retry is essential
			// connection pool may hand out conns that have been closed
			// but don't realise it till you query.
			Statement st = conn.createStatement();
		    st.execute("q)static:([] sym:1000?`4; price:1000?100.0)");
		    
		} catch (SQLException se) {
			try {
				conn = connMan.getConnection(SERVER_CONFIG);
			} catch (IOException e) {
				throw new IOException(se);	
			}
		} catch(InterruptedException ie) {
			throw new IOException(ie);
		}
		
		return conn;
	}

	/**
	 * @return a connection manager with a known {@link ServerConfig} that
	 * 		is a newly created kdb process.
	 */
	public static ConnectionManager getNewConnectedMangager() throws IOException, 
		InterruptedException {
		ConnectionManager cMan = ConnectionManager.newInstance();
		killAnyOpenProcesses();
		startQ();
		try {
			cMan.addServer(SERVER_CONFIG);
			cMan.addServer(SERVER_CONFIG_B);
			cMan.addServer(DISCONNECTED_SERVER_CONFIG);
		} catch(IllegalArgumentException e) {
			// do nothing
		}
		
		return cMan;
	}
	
	/** @return current working directory of most recently launched q process. */
	public static File getLatestDir() {
		return latestDir;
	}


	public static <T> T waitForType(Supplier<T> supplier) {
		for(int i=0; i<100; i++) {
			T o = supplier.get();
			if(o != null) {
				return o;
			}
			try {
				Thread.sleep(80);
			} catch (InterruptedException e) {}
		}
		fail("Failed waiting for condition");
		return null;
	}
	

	public static void assertContains(String haystack, String needle) {
		if(!haystack.contains(needle)) {
			assertEquals(haystack, needle);
		}
	}

	public static void assertContains(String haystack, String needle, String comment) {
		if(!haystack.contains(needle)) {
			assertEquals(haystack, needle, comment);
		}
	}


	public static boolean assertStartsWith(String s, String start) {
		if(!s.startsWith(start)) {
			System.out.println(s);
		}
		assertEquals(start, s.length() >= start.length() ? s.substring(0, start.length()) : s);
		return s.startsWith(start);
	}

	public static boolean assertEndsWith(String s, String end) {
		if(!s.endsWith(end)) {
			System.out.println(s);
		}
		assertEquals(end, s.length() >= end.length() ? s.substring(s.length() - end.length()) : s);
		return s.endsWith(end);
	}

	private static Connection CACHED_CONN = null;
	public static ResultSet queryKdb(String sqlQuery) throws SQLException, IOException {
		if(CACHED_CONN == null) {
			CACHED_CONN = getNewConn();
		}
		Statement stmt = CACHED_CONN.createStatement();
		return stmt.executeQuery(sqlQuery); // This leaks resources but it's only for tests.
	}
}
