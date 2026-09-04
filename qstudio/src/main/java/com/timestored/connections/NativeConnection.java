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

import java.io.IOException;

import kx.c.KException;

/**
 * A connection to a database that speaks its own protocol rather than JDBC, and
 * answers with a native object graph rather than a {@link java.sql.ResultSet}.
 *
 * kdb and Rayforce both work this way: the query is text in the server's own
 * language, and the reply is a value - an atom, a vector, a dictionary, a table -
 * that qStudio renders directly. Implementations return the shapes {@link kx.c}
 * produces, so the grid, charts and export paths are shared.
 */
public interface NativeConnection {

	/** Evaluate {@code query} on the server and return its result. */
	Object query(String query) throws IOException, KException;

	/**
	 * Whatever the query printed on the server, for the console pane. Valid only
	 * for the most recent {@link #query} call; empty when the server said nothing.
	 */
	String getLastConsoleOutput();

	void close() throws IOException;

	boolean isConnected();

	/** host:port, for logging and error messages. */
	String getName();
}
