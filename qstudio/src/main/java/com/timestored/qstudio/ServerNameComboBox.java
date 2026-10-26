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

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.timestored.connections.ServerConfig;
import com.timestored.qstudio.model.QueryAdapter;
import com.timestored.qstudio.model.QueryManager;

/**
 * Display name of currently selected server and allow changing selection.
 */
class ServerNameComboBox extends JComboBox {
	
	private static final long serialVersionUID = 1L;
	private final QueryManager queryManager;
	
	ServerNameComboBox(final QueryManager queryManager) {
    	
		this.queryManager = queryManager;
        addActionListener(this);
        
        queryManager.addQueryListener(new QueryAdapter() {
			@Override public void selectedServerChanged(String server) {
				refresh();
			}

			@Override
			public void serverListingChanged(List<String> serverNames) {
		        String[] names = serverNames.toArray(new String[]{});
				setModel(new DefaultComboBoxModel(names));
				refresh();
			}
			
		});
        
		setModel(new DefaultComboBoxModel(queryManager.getServerNames().toArray(new String[]{})));
		refresh();
	}

	private void refresh() {
		String selServer = queryManager.getSelectedServerName();
		String mySel = (String) getSelectedItem();
		if(selServer != null && !selServer.equals(mySel)) {
			setSelectedItem(selServer);
		}
		refreshTooltip();
	}

	@Override public void actionPerformed(ActionEvent e) {
		queryManager.setSelectedServerName((String) getSelectedItem());
		refreshTooltip();
	}

	/** Update the combo's tooltip to show host:port / DB / Type for the currently-selected server. */
	private void refreshTooltip() {
		String name = (String) getSelectedItem();
		ServerConfig sc = name == null ? null : queryManager.getServer(name);
		if(sc == null) { setToolTipText(null); return; }
		StringBuilder msg = new StringBuilder("<html>");
		if(sc.getPort() != 0) {
			msg.append("<b>").append(sc.getHost()).append(':').append(sc.getPort()).append("</b>");
		}
		if(sc.getDatabase() != null && !sc.getDatabase().isEmpty()) {
			if(msg.length() > 6) msg.append("<br />");
			msg.append("DB: <b>").append(sc.getDatabase()).append("</b>");
		}
		if(msg.length() > 6) msg.append("<br />");
		msg.append("Type: <b>").append(sc.getJdbcType()).append("</b>");
		msg.append("</html>");
		setToolTipText(msg.toString());
	}
}