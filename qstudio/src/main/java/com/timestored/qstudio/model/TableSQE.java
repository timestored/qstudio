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

import static com.timestored.qstudio.kdb.CAtomTypes.DICTIONARY;
import static com.timestored.qstudio.kdb.CAtomTypes.TABLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.timestored.connections.JdbcTypes;
import com.timestored.connections.MetaInfo;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.kdb.CAtomTypes;
import com.timestored.theme.Theme.CIcon;

import lombok.Getter;
import lombok.NonNull;


/** {@link ServerQEntity} for tables only */
public class TableSQE extends BaseSQE {

	@Getter private final long count;
	@Getter private final boolean isPartitioned;
	@Getter private final List<String> colNames;
	@NonNull private final JdbcTypes jdbcTypes;

	/**
	 * @param count Count if known or -1 to specify unknown.
	 */
	TableSQE(String serverName, String namespace, String name, CAtomTypes type, 
			long count, boolean isPartitioned, String[] colNames, JdbcTypes jdbcTypes) {
		
		super(serverName, namespace, name, type, jdbcTypes);
		Preconditions.checkArgument(count>=-2);
		Preconditions.checkNotNull(colNames);
		Preconditions.checkArgument(colNames.length>0);
		CAtomTypes t = getType();
		Preconditions.checkArgument(t.equals(TABLE) || t.equals(DICTIONARY));

		this.count = count;
		this.isPartitioned = isPartitioned;
		this.colNames = Arrays.asList(colNames);
		this.jdbcTypes = jdbcTypes == null ? JdbcTypes.KDB : jdbcTypes;
	}

	@Override public boolean isTable() {
		return true;
	}
	
	@Override public ImageIcon getIcon() {
		// The tree query returns count -2 when the count fails
		// This can be to moving from the HDB root directory etc.
		return count == -2 ? CIcon.TABLE_DELETE.get16() : CIcon.TABLE_ELEMENT.get16();
	}

	@Override public String toString() {
		return "TableSQE[" + getName() + " count=" + count 
				+ " cols=" + Joiner.on(",").join(colNames) + "]";
	}
	
	@Override public boolean equals(Object o) {
		if(o instanceof TableSQE) {
			TableSQE that = (TableSQE)o;
			return super.equals(o) && 
				Objects.equal(colNames, that.colNames) && 
				Objects.equal(isPartitioned, that.isPartitioned) && 
				Objects.equal(count, that.count);
		}
		return false;
	}

	@Override public String getHtmlDoc(boolean shortFormat) {
		
		String t = (getType().equals(DICTIONARY) ? "Keyed " : "") + "Table";
		
		// If there are many columns, do comma separated rather than list.
		// Otherwise its too long for the screen
		String colHtml = getColumnHtml(colNames, shortFormat || colNames.size() > 9);
		
		String s = "";
		if(!shortFormat) {
			Map<String,String> namesToDescs = ImmutableMap.of("Name: ", getDocName(), 
					"Type: ", t,
					"Count: ", count==-1 ? "unknown" : ""+count,
					"Partitioned: ", (isPartitioned ? "Yes" : "No"),
					"Columns: ", colHtml);
			s = toHtml(namesToDescs);
		} else {
			s = HtmlUtils.START + " " + t + " ";
			if(count != -1) {
				s += count + " rows ";
			}
			s += (isPartitioned ? "Partitioned" : "") + "<br />Columns: " + colHtml + HtmlUtils.END;
		}
		return s;
	}

	private static String getColumnHtml(List<String> colNames, boolean shortFormat) {
		String colHtml;
		if(shortFormat) {
			StringBuilder sb = new StringBuilder(colNames.size() * 8);
			for(int i=0; i<colNames.size()-1; i++) {
				sb.append(colNames.get(i)).append(", ");
				if(5 == i % 6) {
					sb.append("<br />");
				}
				if(i>23) {
					sb.append(".... ");
					break;
				}
			}
			sb.append(colNames.get(colNames.size()-1));
			colHtml = sb.toString();
		} else {
			colHtml = HtmlUtils.toList(colNames);
		}
		return colHtml;
	}
	
	@Override public List<QQuery> getQQueries() {
		List<QQuery> r = new ArrayList<ServerQEntity.QQuery>();

		String fn = getFullName();
		
		r.add(new QQuery("Select Top 1000", CIcon.TABLE_ELEMENT, MetaInfo.getTop100Query(jdbcTypes, colNames, fn, isPartitioned, false)));
		r.add(new QQuery("Select Col1,Col2... from Top 1000", CIcon.TABLE_ELEMENT, MetaInfo.getTop100Query(jdbcTypes, colNames, fn, isPartitioned, true)));
		r.add(new QQuery("Count", CIcon.TABLE_ELEMENT, MetaInfo.getCountQuery(jdbcTypes, fn)));
		if(jdbcTypes.isKDB()) {
			r.add(new QQuery("Select Bottom 1000", CIcon.TABLE_ELEMENT, MetaInfo.getBottom100query(jdbcTypes, colNames, fn, isPartitioned, false)));
			r.addAll(super.getQQueries());
		}
		
		return r;		
	}
}
