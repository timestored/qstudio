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
package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseWheelListener;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JPanel;

import com.timestored.StringUtils;
import com.timestored.babeldb.DBHelper;
import com.timestored.connections.ServerConfig;
import com.timestored.qstudio.model.QueryListener;
import com.timestored.qstudio.model.QueryResult;
import com.timestored.swingxx.ScrollingTextArea;

/**
 * Output similar in appearance to the KDB console, simple add as a listener.
 */
class ConsolePanel extends JPanel implements QueryListener,GrabableContainer {

	private static final long serialVersionUID = 1L;
	private static final int FONT_SIZE = 12;
	private static final Color FG_COLOR = new Color(240, 240, 240);

	private final ScrollingTextArea scText;
	private QueryResult latestQR;

	public ConsolePanel() {
		setName("consolePanel");
		setLayout(new BorderLayout());
		
		scText = new ScrollingTextArea(FG_COLOR, Color.BLACK);
		scText.setTextareaFont(new Font("Monospaced", Font.PLAIN, FONT_SIZE));
		add(scText,BorderLayout.CENTER);
	}

	@Override public void queryResultReturned(ServerConfig sc, QueryResult qr) {
		latestQR = qr;

		String txt = "";
		// For KDB always use console view. For non-kdb check if it's just one cell and show that or the number of rows.
		// useful for REDIS for example
		if(sc.isKDB() || qr.getConsoleView().length() > 2) { // console = :: for kdb often
			txt = qr.getConsoleView();
		} else {
			if(qr.rs != null) {
				int count = -1;
				try {
					count = DBHelper.getSize(qr.rs);
					txt = count + " rows returned";
					if(count == 1 && qr.rs.getMetaData().getColumnCount() == 1) {
						qr.rs.beforeFirst();
						qr.rs.next();
						txt = ""+ qr.rs.getObject(1);
					} else if(count <= 30) {
						txt = DBHelper.toFancyString(qr.rs, false);
					}
				} catch (SQLException e) {}	
			}
		}
		app(txt);
	}
	@Override public void sendingQuery(ServerConfig sc, String query) { 
		app((sc.isKDB() ? "q)" : ">") + query); 
	}
	
	@Override public void selectedServerChanged(String server) {
		app("##Server Changed to ->" + server);
	}
	
	private void app(String msg) { scText.appendMessage(msg); }

	@Override public void serverListingChanged(List<String> serverNames) { }
	
	public void setCodeFont(Font f) { scText.setTextareaFont(f); }

	@Override public synchronized void addMouseWheelListener(MouseWheelListener l) {
		scText.addMouseWheelListener(l);
	}

	public void setMaxLength(int maxConsoleLength) { scText.setMaxLength(maxConsoleLength); }

	@Override public GrabItem grab() {
		if(latestQR != null) {
			ScrollingTextArea con = new ScrollingTextArea(FG_COLOR, Color.BLACK);
			con.setTextareaFont(scText.getTextareaFont());
			con.appendMessage(scText.getText());
			return new GrabItem(con, StringUtils.abbreviate(latestQR.getQuery(),50) + " - Console");
		}
		return null;
	}
	        }
