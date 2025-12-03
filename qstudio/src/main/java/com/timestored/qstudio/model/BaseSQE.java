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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.timestored.connections.JdbcTypes;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.kdb.CAtomTypes;
import com.timestored.theme.Theme.CIcon;

/** Base class that implements most methods for concreate classes to base themself off. */
abstract class BaseSQE implements ServerQEntity {
	
	private final String namespace;
	private final String name;
	private final CAtomTypes type;
	private final String serverName;
	protected final JdbcTypes jdbcTypes;
	
	protected BaseSQE(String serverName, String namespace, String name, CAtomTypes type, JdbcTypes jdbcTypes) {
		this.namespace = Preconditions.checkNotNull(namespace);
		this.name = Preconditions.checkNotNull(name);
		this.type = type;
		this.serverName = serverName;
		this.jdbcTypes = jdbcTypes;
	}

	@Override public String getName() {
		return name;
	}

	@Override public String getNamespace() {
		return namespace;
	}

	@Override public CAtomTypes getType() {
		return type;
	}

	@Override public String getFullName() {
		 return jdbcTypes != null && jdbcTypes.equals(JdbcTypes.DOLPHINDB) ? 
				 		(namespace.equals(".") ? name : ("loadTable('" + namespace + "', '" + name + "')")) : 
				 (namespace.equals(".") ? "" : namespace + ".") + name;
	}
	
	@Override public String getDocName() {
		 return getFullName();
	}

	@Override public String getHtmlDoc(boolean shortFormat) {
		if(shortFormat) {
			return HtmlUtils.START +  "Type: " + getType().toString().toLowerCase() 
					+ " Count: " + getCount() + HtmlUtils.END;
		}
		return toHtml(ImmutableMap.of("Name: ", getDocName(), 
				"Type: ", getType().toString().toLowerCase(), 
				"Count: ", getCount()==-1 ? "unknown" : ""+ getCount()));
	}

	static String toHtml(Map<String, String> namesToDescs) {
		return HtmlUtils.START + HtmlUtils.toTable(namesToDescs, true) + HtmlUtils.END;
	}

	@Override public ImageIcon getIcon() {
		return type.getIcon().get16();
	}

	@Override public List<QQuery> getQQueries() {
		if(jdbcTypes != null && !jdbcTypes.equals(JdbcTypes.KDB)) {
			return Collections.emptyList();
		}
		List<QQuery> r = Lists.newArrayList();
		r.add(new QQuery("Delete", CIcon.DELETE, "delete "+getName()+" from `"+getNamespace()));
		return r;
	}

	@Override public String toString() {
		long c = getCount();
		return "Element[" + getName() + " type=" + type 
				+ (c!=1 ? " count=" + c : "") 
				+  (isTable() ? " isTable" : " ") 
				+ "]";
	}

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override public boolean equals(Object obj) {
		if (obj instanceof ServerQEntity) {
			ServerQEntity that = (ServerQEntity) obj;
			
			return Objects.equal(name, that.getName()) && 
				Objects.equal(namespace, that.getNamespace()) && 
				Objects.equal(type, that.getType()) && 
				Objects.equal(getCount(), that.getCount()) && 
				Objects.equal(getDocName(), that.getDocName()) && 
				Objects.equal(isTable(), that.isTable()) && 
				Objects.equal(getFullName(), that.getFullName()) && 
				Objects.equal(getQQueries(), that.getQQueries());
		}
		return false;
	}

	@Override public SourceType getSourceType() {
		return SourceType.SERVER;
	}

	@Override public String getSource() {
		return serverName;
	}

}