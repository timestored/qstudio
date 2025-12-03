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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.google.common.base.Joiner;
import com.timestored.misc.HtmlUtils;

/** * Allows converting a table model to html */
public class TableModelHtmlConverter {
	
	private IndentingAppender sb = new IndentingAppender();
	
	private TableModelHtmlConverter() { }

	private IndentingAppender wrap(String tag, String inner) { 
		return sb.b("<" + tag + ">").b(HtmlUtils.escapeHTML(inner)).b("</" + tag + ">");
	}
	
	private String convertTable(ResultSet rs) throws SQLException {

		sb.start("<table>");
		sb.a("<tr>");
		ResultSetMetaData tm = rs.getMetaData();
		for(int c=1; c<=tm.getColumnCount(); c++) {
			wrap("th", tm.getColumnName(c));
		}
		sb.b("</tr>");
		while(rs.next()) {
			sb.start("<tr>");
			for(int c=1; c <= tm.getColumnCount(); c++) {
				sb.a();
				Object o = rs.getObject(c);
				String s = o == null ? rs.getString(c) : "" + o.toString();
				if(o == null) {
					s = "";
				} else if(o instanceof String) {
					s = (String) o;
				} else if(o instanceof char[]) {
					s = new String((char[])o);
				} else if(o instanceof String[]) {
					String[] v = (String[])o;
					s = "`" + Joiner.on('`').join(v);
				}
				wrap("td", s);
			}
			sb.end("</tr>");
		}
		sb.end("</table>");
		return sb.toString();
	}
	
	/** convert table model to html 
	 * @throws SQLException **/
	public static String convert(ResultSet rs) throws SQLException {
		return new TableModelHtmlConverter().convertTable(rs);
	}
	
	
	private static class IndentingAppender {
		private String indent = "";
		private StringBuilder sb = new StringBuilder();

		IndentingAppender a() { return a("");}
		IndentingAppender a(String s) {
			return b("\r\n").b(indent).b(s);
		}
		IndentingAppender b(String s) {
			sb.append(s);
			return this;
		}

		IndentingAppender start(String s) { return a(s).increaseIndent(); };
		IndentingAppender end(String s) { return decreaseIndent().a(s); };
		private IndentingAppender increaseIndent() { indent += "\t"; return this;}
		private IndentingAppender decreaseIndent() { indent = indent.substring(1); return this; }
		@Override public String toString() { return sb.toString(); }
	}
}
