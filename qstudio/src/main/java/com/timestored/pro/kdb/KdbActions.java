package com.timestored.pro.kdb;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JOptionPane;

import com.timestored.TimeStored;
import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.jgrowl.Growler;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.misc.IOUtils;
import com.timestored.qstudio.CommonActions;
import com.timestored.qstudio.Persistance;
import com.timestored.qstudio.QStudioModel;
import com.timestored.qstudio.UpdateHelper;
import com.timestored.theme.ShortcutAction;
import com.timestored.theme.Theme;

import lombok.NonNull;

class KdbActions {
	private static final Logger LOG = Logger.getLogger(KdbActions.class.getName());
	private static List<Action> proActions = null;

	private final @NonNull CommonActions commonActions;
	private final @NonNull OpenDocumentsModel openDocumentsModel;
	private final @NonNull Growler growler;
	private final @NonNull Persistance persistance;

	private KdbActions(@NonNull CommonActions commonActions, @NonNull QStudioModel qStudioModel,
			@NonNull Growler growler, @NonNull Persistance persistance) {
		this.commonActions = commonActions;
		this.openDocumentsModel = qStudioModel.getOpenDocumentsModel();
		this.growler = growler;
		this.persistance = persistance;
	}
	
	public static List<Action> getProActions(@NonNull CommonActions commonActions, @NonNull QStudioModel qStudioModel,
						@NonNull Growler growler, Persistance persistance) {
		return new KdbActions(commonActions, qStudioModel, growler, persistance).getProActions();
	}
	
	private List<Action> getProActions() {
		if(proActions == null) {
			
			ShortcutAction unitTestAction = new ShortcutAction("Unit Test Current Script",
	        		Theme.CIcon.SCRIPT_GO, "Unit test all namespaces within the current script.",
	        		KeyEvent.VK_T, KeyEvent.VK_T) {
						private static final long serialVersionUID = 1L;

				@Override public void actionPerformed(ActionEvent ae) {
					runQUnitTests();
				}
			};

			 
			ShortcutAction loadScriptAction = new ShortcutAction(Msg.get(Key.LOAD_SCRIPT_MODULE),
	        		Theme.CIcon.SERVER_GO, "Load this q script onto current server.",
	        		KeyEvent.VK_L, KeyEvent.VK_L) {
						private static final long serialVersionUID = 1L;

				@Override public void actionPerformed(ActionEvent e) {
					runScriptModuleLoad();
				}
			};

			ShortcutAction profileAction = new ShortcutAction(Msg.get(Key.PROFILE_SELECTION),
	        		Theme.CIcon.CLOCK_GO, "Profile highlighted Q query on current server.",
	        		KeyEvent.VK_J, KeyEvent.VK_J) {
						private static final long serialVersionUID = 1L;

				@Override public void actionPerformed(ActionEvent ae) {
					String qry = openDocumentsModel.getSelectedDocument().getSelectedText();
					if(qry.length()<=0) {
						qry = openDocumentsModel.getSelectedDocument().getContent();
					}
					if(qry.length() > 0) {
						try {
							String prof = IOUtils.toString(CommonActions.class, "profile.q");
							String fullQuery = prof + "@[;1] .prof.profile \"" +
									qry.replace("\"", "\\\"") + "\""; 
							commonActions.sendQuery(fullQuery, "PROFILE -> " + qry);
						} catch (IOException e) {
							LOG.log(Level.SEVERE, "Problem loading profile.q", e);
						}
					}
				}
			};

			
			List<Action> l = new ArrayList<>();
			l.add(unitTestAction);
			l.add(loadScriptAction);
			l.add(profileAction);
			proActions = l;
		}
		return proActions;
	}

	/** 
	 * Execute the entire text of the current document, then fix the namespace
	 * of all functions. Bug in Kdb that remotely executed code leaves
	 * functions defined in the global namespace. 
	 */
	private void runScriptModuleLoad() {
		String qry = null;
		try {
			Document doc = openDocumentsModel.getSelectedDocument();
			qry = ModuleRunner.getRunScriptModuleLoad(doc.getContent());
			commonActions.sendQuery(qry, "Loading -> " + doc.getTitle());
		} catch(IOException io) {
			growler.showWarning("Could not load script loader module", "Module Load Fail"); 
		}
	}

	private void runQUnitTests() {
		// check license, show warning if necessary then perform action
		final Persistance.Key warningKey = Persistance.Key.SHOW_QDOC_WARNING;
		String msgHtml = "<a href='" + TimeStored.Page.QUNIT_HELP.url() + "'>QUnit</a> is a framework for implementing testing in kdb. <br/> <br/>" + 
				"Running qunit tests involves: <br/>" +
				"1. Loading the qunit module into the .qunit namespace. <br/>" +
				"2. Loading all test functions within the currently selected document. <br/>" +
				"3. Running the tests, checking all assertions are met. <br/>" +
				"4. Reporting a table of results. <br/>" +
				"<br/>" +
				"Tests must be properly structured and more help can be found on the " +
				"<a href='" + TimeStored.Page.QSTUDIO_HELP_QUNIT.url() + "'>qunit help page</a><br/><br/>" + 
						"<b>Run the qunit tests?</b>";
		final String title = "Load and Run .qunit module";
		
		int choice = CommonActions.showDismissibleWarning(persistance, warningKey, msgHtml, title, "Run Tests", JOptionPane.OK_OPTION);
		
		if(choice == JOptionPane.OK_OPTION) {
			String qry = null;
			try {
				String testq = openDocumentsModel.getSelectedDocument().getContent();
				try {
					qry = ModuleRunner.getRunQUnitQuery(testq);
				} catch(IllegalArgumentException iae) {
					String message = "no namespace found in this file";
					JOptionPane.showMessageDialog(null, message);
				}
			} catch(IOException io) {
				growler.showWarning("Could not load testing module", "Module Load Fail"); 
			}
			if(qry != null) {
				String t = openDocumentsModel.getSelectedDocument().getTitle();
				commonActions.sendQuery(qry, "TEST file -> " + t);
			}
		}
	}

}
