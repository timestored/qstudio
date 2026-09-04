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
package com.timestored.rayforce;

import java.io.IOException;
import java.util.logging.Logger;

import com.timestored.connections.NativeConnection;
import com.timestored.connections.ServerConfig;

import kx.c.KException;

/**
 * A connection to a RayforceDB server - the Rayforce counterpart of
 * {@link com.timestored.kdb.KdbConnection}, including its retry-once-then-give-up
 * behaviour so a server restart doesn't require the user to reconnect by hand.
 */
public class RayConnection implements NativeConnection {

	private static final Logger LOG = Logger.getLogger(RayConnection.class.getName());

	private static final int RETRIES = 1;
	private static final int CONNECT_TIMEOUT_MILLIS = 10000;

	private final String host;
	private final int port;
	private final String username;
	private final String password;

	private RayIpc ipc;
	private String lastConsoleOutput = "";
	private boolean closed = false;
	private long maxResultBytes = 0;

	public RayConnection(String host, int port, String username, String password)
			throws IOException, KException {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		reconnect();
	}

	public RayConnection(ServerConfig sc) throws IOException, KException {
		this(sc.getHost(), sc.getPort(), sc.getUsername(), sc.getPassword());
	}

	@Override public Object query(String query) throws IOException, KException {
		LOG.info("querying -> " + query);
		if (closed) {
			throw new IllegalStateException("we were closed");
		}

		for (int r = 0; ; r++) {
			try {
				RayIpc.Result result = ipc.query(query);
				lastConsoleOutput = result.getConsole();
				return result.getValue();
			} catch (IOException e) {
				// A dropped socket is worth one silent retry; a KException is the
				// server answering, so it propagates untouched.
				lastConsoleOutput = "";
				if (r >= RETRIES) {
					LOG.info("giving up reconnecting");
					throw e;
				}
				try {
					reconnect();
				} catch (IOException io) { /* retry loop reports the original */
				} catch (KException ke) {
					throw ke;
				}
			}
		}
	}

	@Override public String getLastConsoleOutput() { return lastConsoleOutput; }

	/** @see RayIpc#setMaxResultBytes(long) */
	public void setMaxResultBytes(long maxResultBytes) {
		this.maxResultBytes = maxResultBytes;
		ipc.setMaxResultBytes(maxResultBytes);
	}

	@Override public void close() throws IOException {
		LOG.info("close");
		closed = true;
		ipc.close();
	}

	@Override public boolean isConnected() { return !closed && ipc != null && ipc.isConnected(); }

	@Override public String getName() { return host + ":" + port; }

	private void reconnect() throws IOException, KException {
		if (closed) {
			throw new IllegalStateException("we were closed");
		}
		LOG.info("Trying reconnect host:" + host);
		if (ipc != null) {
			try { ipc.close(); } catch (IOException e) { /* the old socket is dead anyway */ }
		}
		ipc = new RayIpc(host, port, username, password, CONNECT_TIMEOUT_MILLIS);
		ipc.setMaxResultBytes(maxResultBytes);
	}
}
