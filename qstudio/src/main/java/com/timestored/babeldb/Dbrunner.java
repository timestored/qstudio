package com.timestored.babeldb;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.List;

import com.timestored.connections.ServerConfig;


public interface Dbrunner extends AutoCloseable {
	
	public ResultSet executeQry(String serverName, String sql, int millisStalenessPermitted) throws IOException;
	
	public List<String> getServerWithSymbols(); // serverName

	/**
	 * @return Server associated with the serverName, or null if not found.
	 * @param serverName serverName uniquely identifying a given {@link ServerConfig}.
	 */
	public ServerConfig getServer(String serverName);


	/**
	 * @return Server associated with the serverName, or throws exception if not found.
	 * @param serverName serverName uniquely identifying a given {@link ServerConfig}.
	 * @throws IllegalStateException If server cannot be found
	 */
	public default ServerConfig getServerElseThrow(String serverName) {
		if(serverName == null) {
			throw new IllegalStateException("No Server Selected.");
		}
		ServerConfig sc = getServer(serverName);
		if(sc == null) {
			throw new IllegalStateException("Could not find server:" + serverName);
		}
		return sc;
	}

	public boolean isEmpty();

}