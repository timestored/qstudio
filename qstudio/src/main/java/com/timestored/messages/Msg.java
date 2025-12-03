/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2024 TimeStored
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
package com.timestored.messages;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/** Provides locale specific i18n test. Allows translated GUIs. */
public class Msg {

	private static final String BUNDLE_NAME = "com.timestored.messages.messages";
	private static ResourceBundle rb;

	static { setLocale(Locale.getDefault()); }
	
	public static enum Key {
		BYTE_CODE,
		
		ABOUT, CHART, CONFIRM, CONGRATS_VALID_LICENSE, CONSOLE, 
		COULD_NOT_LOAD_FILE, DELETE_ALL_SETTINGS, DOCUMENTS, 
		ENTER_LICENSE_KEY, EXIT, EXPRESSIONS, FILE, FILE_TREE, 
		GOTO_DEFINITION, HELP, HISTORY, IMPORT, LAUNCH_SQL_DASHBOARDS, 
		LICENSE_KEY, LOAD_CSV_DATA, NO_FOLDER_SELECTED, OPEN_ALL_RECENT, 
		OPEN_EXAMPLE_CHARTS, OPEN_QUNIT_EXAMPLE, PLEASE_ENTER_LICENSE_KEY, 
		PREFERENCES, QUERY, REFRESH, REPORT_A_BUG, RESET_ALL, RESULT, 
		SELECT_FILE_TO_IMPORT, SERVER, SERVER_TREE, SETTINGS, SORRY_INVALID_LICENSE, 
		TOOLS, UNSAVED_CHANGES, WELCOME, WINDOWS, 
		
		BROWSE_FOLDER, CLOSE, CLOSE_ALL, CLOSE_FOLDER, COPY, CUT, PRINT, DOCS_GENERATED, 
		DOCUMENT, EROR_OPENING_FILES, ERROR_GENERATING_DOCS, ERROR_OPENING, 
		ERROR_SAVING, FIND, FIND_NEXT, GENERATE, GENERATE2, GOTO_LINE, 
		INVALID_DIRECTORY, NEW_FILE, OPEN_CANCELLED, OPEN_DOCS_NOW, 
		OPEN_FILE, OPEN_FOLDER, PASTE, REDO, SAVE, SAVE_AS, 
		SAVE_CANCELLED, SAVE_ERROR, SAVE_FILE, SAVE_FILE_ERROR, SELECT_DOC_DIR, 
		TOGGLE_COMMENTS, UNDO_TYPING, UNSAVED_CHANGES_CONFIRM,
		
		FIND_SERVER, ADD_SERVER, CANCEL_QUERY, CLONE_SERVER, CONNECTION,
		COPY_SERVER_LIST_TO_CLIPBOARD, CLOSE_CONNECTION, DELETE_CONNECTION, EDIT, 
		ERROR, LOAD_SCRIPT_MODULE, NO_CONNECTIONS, 
		NO_CONNECTIONS_DEFINED, PLEASE_ADD_SERVER_CONNECTION, 
		PROFILE_SELECTION, QUERY_LINE, QUERY_SELECTION, REMOVE_ALL_SERVERS, 
		WARNING, WATCH_EXPRESSION,
		
		ROWS, TABLE, STOP, EXPORT, DATABASE, OPEN_IN_EXCEL, USERNAME, PASSWORD, 
		SERVERS, NEXT_DOCUMENT, PREV_DOCUMENT, EXAMPLE_SCRIPTS, MY_SCRIPTS, CREATE_NEW_FOLDER, CREATE_NEW_FILE, 
		START_MARKDOWN, SEARCH_FILES;
		
		private static final Map<String, Key> lookup = Maps.newHashMap();
		
		 static {
			for(Key k : Key.values()) { lookup.put(k.toString(), k); }
	     }
		 
		 public static Key get(String k) { return lookup.get(k); }
	}
	
	public static String get(Key key) { return rb.getString(key.toString()); }
	
    public static void setLocale(Locale locale){
        rb = ResourceBundle.getBundle(BUNDLE_NAME,locale);
    }
    
    /**
     * @return True if all possible keys are specified in the current resource bundle
     */
    static boolean checkAllKeysSpecified() {
    	for(Key k : Key.values()) {
    		if(get(k)==null || get(k).trim().length()<1) {
    			return false;
    		}
    	}
    	return true;
    }
    
    /**
     * @return Any keys only defined in the resource bundle but not the enumeration
     */
    static Set<String> getSuperfluosResourceBundleEntries() {
    	
    	Set<String> extraKeys = Sets.newHashSet();
    	Enumeration<String> keys = rb.getKeys();
    	while(keys.hasMoreElements()) {
    		String k = keys.nextElement();
    		if(Key.get(k)==null) {
    			extraKeys.add(k);
    		}
    	}
    	return extraKeys;
    }
}
