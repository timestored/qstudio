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

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.RowSet;

import kx.c;
import kx.c.KException;
import lombok.Getter;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.timestored.babeldb.DBHelper;
import com.timestored.connections.ServerConfig;
import com.timestored.kdb.QueryResultI;
import com.timestored.qstudio.PivotFormConfig;

/**
 * Immutable data container to hold result of user ending a KDB query.
 * Four possible results:<ul><li>
 *  query threw an exception (java, cancelled or  kdb {@link c.KException}</li><li>
 *  query was successful and returned a result</li><li>
 *  query was successful but result was too large and query wrap returned only console view.</li></ul>
 *  
 *  A cancel query may or may not contain an actual result.
 * 
 */
public class QueryResult implements QueryResultI {
	
	@Getter private final PivotFormConfig pivotConfig;
	@Getter private final ServerConfig serverConfig;
	public final String query;
	@Getter public final Object k;
	public final ResultSet rs;
	public RowSet rowSet = null;
	@Getter public final Exception e;
	private final String consoleView;
	private final Type type;
	@Getter private final long millisTaken;
	
	enum Type { Exception, Cancel, Success };

	
	public static QueryResult exceptionResult(ServerConfig serverConfig, String query, PivotFormConfig pivotConfig, Exception e, long millisTaken) {
		String cView = (e!=null ? e.getMessage() : "exception");
		return new QueryResult(serverConfig, query, pivotConfig, null, null, cView, e, false, millisTaken);
	}

	public static QueryResult cancelledResult(ServerConfig serverConfig, String query, PivotFormConfig pivotConfig) {
		String cView = "Query Cancelled";
		return new QueryResult(serverConfig, query, pivotConfig, null, null, cView, null, true, 0);
	}

	public static QueryResult successfulResult(ServerConfig serverConfig, String query, PivotFormConfig pivotConfig, Object k, 
			ResultSet rs, String consoleView, long millisTaken) {
		return new QueryResult(serverConfig, query, pivotConfig, k, rs, consoleView, null, false, millisTaken);
	}
	
	public boolean isCancelled() { return type.equals(Type.Cancel);	}
	public boolean isException() { return type.equals(Type.Exception);	}
	
	
	private QueryResult(ServerConfig serverConfig, String query, PivotFormConfig pivotConfig, Object k, ResultSet rs, 
			String consoleView, Exception e, boolean cancelled, long millisTaken) {
		
		if(cancelled) {
			type = Type.Cancel;
		} else if(e != null) {
			type = Type.Exception;
		} else {
			type = Type.Success;
		}
		
		this.serverConfig = Preconditions.checkNotNull(serverConfig);
		this.query = Preconditions.checkNotNull(query);
		this.pivotConfig = pivotConfig;
		this.k = k;
		this.rs = rs;
		this.e = e;
		this.consoleView = (e instanceof KException) ? "'"+e.getMessage() : consoleView;
		this.millisTaken = millisTaken;
	}
	
	Type getType() { return type; }
	
	/** Short text description describing if success, fail, exception **/
	public String getResultType() {
		return type.toString();
	}
	
	/**
	 * The query result may not contain the actual k result if it was over a certain size,
	 * in this case it will just have the console view.
	 * @return true if no k object was returned as too large, instead consoleView is available.
	 */
	public boolean isKResultTooLarge() {
		return k==null && consoleView!=null && consoleView.length()>0;
	}
	
	/**
	 * @return Result as displayed in q console (never null)
	 */
	public String getConsoleView() {
		if(isCancelled()) {
			return "'stop - query cancelled";
		} else if(k==null && consoleView==null || consoleView.isEmpty()) {
			return "::";
		}
		return consoleView;
	}

	public String getQuery() { return query; }
	
	@Override public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("query", query)
				.add("wasResult", k!=null)
				.add("wasException", e!=null)
			.toString();
	}

	@Override public void close() throws Exception {
		if(rowSet != null) {
			rowSet.close();
			rowSet = null;
		}
	}

	/**
	 * Implements QueryResultI and provides cached rowset to allow using Bable in both Pulse and qStudio
	 * babel can't be trusted to go through RS once only.
	 */
	@Override public RowSet getRs() { 
		try {
			if(rowSet == null) {
				rowSet = DBHelper.toCRS(rs); 
			}
			return rowSet;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override public boolean isExceededMax() {
		return false;
	}

}