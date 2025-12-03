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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.timestored.connections.JdbcTypes;
import com.timestored.connections.MetaInfo;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.kdb.CAtomTypes;
import com.timestored.qstudio.model.ServerQEntity.QQuery;
import com.timestored.theme.Theme.CIcon;

import lombok.Getter;

/**
 * Factory to aid construction of {@link ServerQEntity}'s.
 */
class ServerQEntityFactory {

	
	/**
	 * @return {@link ServerQEntity} that fits the passed params if possible otherwise null.
	 * @param partitioned true if it's partitioned (only applies to tables)
	 * @param isView true if its a view (views in KDB are type 100, count 1 etc)
	 * @param colArgNames If the type is table, this is the column names
	 * 		if it's specified as lambda, this is the arguments
	 * @param count Count if known or -1 to specify unknown.
	 */
	static ServerQEntity get(String serverName, String namespace, String name, Short typeNum, long count,
			boolean isTable, boolean partitioned, boolean isView, String[] colArgNames, JdbcTypes jdbcTypes) {
		CAtomTypes t = CAtomTypes.getType(typeNum);
		if(t==null) {
			return null; // can't recognise type, return
		}
		if(isView) {
			return getView(serverName, namespace, name);
		} else if(isTable) {
			return getTable(serverName, namespace, name, typeNum, count, partitioned, colArgNames, jdbcTypes);
		} else if(t.isList()) {
			return new ListSQE(serverName, namespace, name, t, count, jdbcTypes);
		} else if (t.equals(CAtomTypes.DICTIONARY)){
			return new DictSQE(serverName, namespace, name, count);
		} else if (t.equals(CAtomTypes.LAMBDA)){
			return new LambdaSQE(serverName, namespace, name, colArgNames);
		} else {
			return new AtomSQE(serverName, namespace, name, t, jdbcTypes);
		}
	}

	static ServerQEntity getTable(String serverName, String namespace, String name, int typeNum, long count, 
			boolean isPartitioned, String[] colNames, JdbcTypes jdbcTypes) {
		return new TableSQE(serverName, namespace, name, CAtomTypes.getType(typeNum), 
				count, isPartitioned,  colNames, jdbcTypes);
	}

	static ServerQEntity getTable(String serverName, String namespace, String name, int typeNum, long count, 
			boolean isPartitioned, String[] colNames) {
		return new TableSQE(serverName, namespace, name, CAtomTypes.getType(typeNum), 
				count, isPartitioned,  colNames, null);
	}

	static ServerQEntity getAtom(String serverName, String namespace, String name, int typeNum, JdbcTypes jdbcTypes) {
		return new AtomSQE(serverName, namespace, name, CAtomTypes.getType(typeNum), jdbcTypes);
	}

	static ServerQEntity getDict(String serverName, String namespace, String name, int count) {
		return new DictSQE(serverName, namespace, name, count);
	}

	static ServerQEntity getLambda(String serverName, String namespace, String name, String[] argNames) {
		return new LambdaSQE(serverName, namespace, name, argNames);
	}
	
	static ServerQEntity getView(String serverName, String namespace, String name) {
		return new ViewSQE(serverName, namespace, name);
	}
	


	/** {@link ServerQEntity} for lists only */
	private static class ListSQE extends BaseSQE {

		@Getter private final long count;
		
		public ListSQE(String serverName, String namespace, String name, CAtomTypes type, long count, JdbcTypes jdbcTypes) {
			super(serverName, namespace, name, type, jdbcTypes);
			Preconditions.checkArgument(count>=0);
			Preconditions.checkArgument(type.isList());
			this.count = count;
		}

		@Override public boolean isTable() {
			return false;
		}

		@Override public List<QQuery> getQQueries() {
			String fn = getFullName();
			if(jdbcTypes != null && !jdbcTypes.isKDB()) {
				List<QQuery> r = new ArrayList<ServerQEntity.QQuery>();
				r.add(new QQuery(fn, CIcon.TABLE_ELEMENT, fn));
				r.addAll(super.getQQueries());
				return r;
			}
			return super.getQQueries();		
		}
	}

	/** {@link ServerQEntity} for dicts only */
	private static class DictSQE extends BaseSQE {

		@Getter private final long count;
		
		public DictSQE(String serverName, String namespace, String name, long count) {
			super(serverName, namespace, name, CAtomTypes.DICTIONARY, null);
			Preconditions.checkArgument(count>=0);
			this.count = count;
		}

		@Override public boolean isTable() {
			return false;
		}
	}

	/** {@link ServerQEntity} for atoms only */
	private static class AtomSQE extends BaseSQE {
		
		public AtomSQE(String serverName, String namespace, String name, CAtomTypes type, JdbcTypes jdbcTypes) {
			super(serverName, namespace, name, type, jdbcTypes);
			Preconditions.checkArgument(type.isAtom());
		}

		@Override public long getCount() {
			return 1;
		}

		@Override public boolean isTable() {
			return false;
		}

		@Override public String getHtmlDoc(boolean shortFormat) {
			if(shortFormat) {
				return HtmlUtils.START + "Type: " + getType().toString().toLowerCase()
						+ HtmlUtils.END;
			}
			return toHtml(ImmutableMap.of("Name: ", getDocName(), 
					"Type: ", getType().toString().toLowerCase()));
		}

		@Override public List<QQuery> getQQueries() {
			String fn = getFullName();
			if(jdbcTypes != null && !jdbcTypes.isKDB()) {
				List<QQuery> r = new ArrayList<ServerQEntity.QQuery>();
				r.add(new QQuery(fn, CIcon.TABLE_ELEMENT, fn));
				r.addAll(super.getQQueries());
				return r;
			}
			return super.getQQueries();		
		}
	}

	/** {@link ServerQEntity} for atoms only */
	private static class ViewSQE extends BaseSQE {
		
		public ViewSQE(String serverName, String namespace, String name) {
			super(serverName, namespace, name, CAtomTypes.VIEW, null);
		}

		@Override public long getCount() {
			return 1;
		}

		@Override public boolean isTable() {
			return false;
		}

		@Override public String getHtmlDoc(boolean shortFormat) {
			return toHtml(ImmutableMap.of("Name: ", getDocName(), 
					"Type: ", getType().toString().toLowerCase()));
		}
	}


	/** {@link ServerQEntity} for atoms only */
	private static class LambdaSQE extends BaseSQE {
		
		final List<String> argNames;
		
		public LambdaSQE(String serverName, String namespace, String name, String[] argNames) {
			super(serverName, namespace, name, CAtomTypes.LAMBDA, null);
			this.argNames = Arrays.asList(argNames);
		}

		@Override public long getCount() {
			return 1;
		}

		@Override public boolean isTable() {
			return false;
		}
		
		@Override public boolean equals(Object o) {
			if(o instanceof LambdaSQE) {
				LambdaSQE that = (LambdaSQE)o;
				return super.equals(o) && Objects.equal(argNames, that.argNames);
			}
			return false;
		}
		
		@Override public String toString() {
			return "LambdaSQE[" + getName() + " args=" + Joiner.on(",").join(argNames) + "]";
		}
		
		@Override public String getDocName() {
			if(argNames.size()>2 || !argNames.contains("x")) {
				return getFullName() + "[" + Joiner.on(";").join(argNames) + "]";
			}
			return getFullName();
		}
		
		@Override public String getHtmlDoc(boolean shortFormat) {
			return toHtml(ImmutableMap.of("Name: ", getDocName(), 
					"Arguments: ", HtmlUtils.toList(argNames)));
		}
	}

	
}