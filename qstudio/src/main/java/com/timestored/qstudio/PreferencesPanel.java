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

import java.awt.Component;

import javax.swing.JPanel;

import com.google.common.base.Preconditions;

public abstract class PreferencesPanel extends JPanel {

	protected final MyPreferences myPreferences;
	protected final Component container;

	PreferencesPanel(final MyPreferences myPreferences, Component container) {
		this.myPreferences = Preconditions.checkNotNull(myPreferences);
		this.container = container;
	}
		
	/** Take the persisted values and show them values in the GUI */
	abstract void refresh();
	
	/** Take all changes the user made and persist them.  */
	abstract void saveSettings();
	
	/** undo any live settings changes that were made **/
	abstract void cancel();
}
