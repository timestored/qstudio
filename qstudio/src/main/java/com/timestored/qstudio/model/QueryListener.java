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
package com.timestored.qstudio.model;

import java.util.List;

import com.timestored.connections.ServerConfig;

/**
 * Allows listenering for SQL queries being sent / received back.
 * 
 * #sendingQuery fires when a query is sent, it is then guaranteed
 * that queryResultReturned will be called.
 */
public interface QueryListener {
	
	/**
	 * An SQL query is about to be sent.
	 * @param query the SQL query about to be sent.
	 */
	public void sendingQuery(ServerConfig sc, String query);

	/**
	 * A query has returned, the result set is either returned or an exception is.
	 * @param queryResult the result of the SQL query
	 */
	public void queryResultReturned(ServerConfig sc, QueryResult queryResult);

	/** 
	 * The server that queries are ran against was changed
	 * @param server The newly selected server or null if none is selected. 
	 */
	public void selectedServerChanged(String server);
	
	/** 
	 * The list of possible servers changed.
	 * @param serverNames list of serverNames, where those at tail of 
	 * 			list are the most recently updated entries.
	 */
	public void serverListingChanged(List<String> serverNames);
}