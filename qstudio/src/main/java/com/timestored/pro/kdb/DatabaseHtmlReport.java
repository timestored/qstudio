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
package com.timestored.pro.kdb;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

import kx.c;
import kx.c.KException;
import kx.jdbc;

import com.google.common.base.Preconditions;
import com.timestored.TimeStored.Page;
import com.timestored.kdb.KdbConnection;
import com.timestored.misc.HtmlUtils;
import com.timestored.misc.IOUtils;
import com.timestored.qdoc.HtmlPqfOutputter;
import com.timestored.qstudio.model.TableModelHtmlConverter;


/**
 * Allows generating an HTML report on the database structure.
 */
class DatabaseHtmlReport {

	private static final String QSTUDIO_LINK = "<a class='qlogo' href='" + Page.QSTUDIO.url() + "' target='a'>q<span>Studio</span></a>";
	

	private static final String DATABASE_REPORT_Q = "dbreport.q"; 
	public static final String CSS_LINK = "<link rel=\"stylesheet\" href=\"qdoc.css\" type=\"text/css\" media=\"screen\" />";

	
	/**
	 * For the given kdb connection generate a report detailing it's table structure to an html file.
	 * @throws IOException If there's a problem communicating with kdb.
	 * @throws SQLException 
	 */
	public static void generate(KdbConnection kdbConn, File destFile) throws IOException, SQLException {
		
		destFile.createNewFile();
		Preconditions.checkArgument(destFile.isFile() && destFile.canWrite(), 
				"Must be able to write to destination file");
		Preconditions.checkNotNull(kdbConn);
		Preconditions.checkArgument(kdbConn.isConnected());	

		Object k = null;
		boolean hasTables = false;
		try {
			hasTables = (Boolean)kdbConn.query("0<count system \"a\"");
			if(!hasTables) {
				throw new IOException("no tables in kdb process " + kdbConn.getName());
			}
			String dbreportq = IOUtils.toString(DatabaseHtmlReport.class, DATABASE_REPORT_Q);
			k = kdbConn.query(dbreportq);
		} catch (KException e) {
			throw new IOException("Kdb Exception:", e);
		}
		Object[] resArray = (Object[]) k;
		ResultSet rs = jdbc.getRS(resArray[0], "Database Report");
		
		PrintWriter pw = new PrintWriter(destFile);
		String title = kdbConn.getName() + " database structure";
		String header = "<style type=\"text/css\">html table {width:auto;} table td,table th { padding:2px; }</style>";
		
		pw.println(HtmlUtils.getTSPageHead(title, QSTUDIO_LINK, CSS_LINK + header, true));
		pw.println("<h1>" + title + "</h1>");
		
		if(hasTables) {
			try {
				pw.println(TableModelHtmlConverter.convert(rs));
				c.Dict details = (c.Dict) resArray[1];
				printTableDetails(pw, details);
			} catch(ClassCastException cce) {
				pw.println("error parsing table details.");
			}
		} else {
			pw.println("No tables");
		}
		pw.println(HtmlUtils.getTSPageTail(QSTUDIO_LINK));
		pw.close();
		HtmlPqfOutputter.saveQdocCssTo(destFile.getParentFile());
	}

	private static void printTableDetails(PrintWriter pw, c.Dict details) throws SQLException {
		Object[] tableMetas = (Object[]) details.y;

		pw.println("<ul>");
		for(int i=0; i<tableMetas.length; i++) {
			String tableName = ((String[]) details.x)[i];
			pw.println("<li><a href='#tab-" + tableName + "'>" + tableName + "</a></li>");
		}
		pw.println("</ul>");
		
		for(int i=0; i<tableMetas.length; i++) {
			String tableName = ((String[]) details.x)[i];
			pw.println("<h3 id='tab-" + tableName + "'>" + tableName + "</h3>");
			ResultSet metaTM = jdbc.getRS(tableMetas[i], "TableDetails");
			if(metaTM != null) {
				pw.println(TableModelHtmlConverter.convert(metaTM));
			} else {
				pw.println("Could not understand kdb result.");
			}
		}
	}
	
}
