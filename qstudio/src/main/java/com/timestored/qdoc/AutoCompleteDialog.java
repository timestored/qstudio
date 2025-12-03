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
package com.timestored.qdoc;

import static com.timestored.swingxx.SwingUtils.ESC_KEYSTROKE;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.google.common.collect.Maps;
import com.timestored.command.Command;
import com.timestored.command.CommandSplitPane;
import com.timestored.qstudio.CommonActions;
import com.timestored.swingxx.AAction;
import com.timestored.theme.Theme;

/**
 * Dialog similar to eclipses autocomplete that takes the nearest text 
 * in a {@link JEditorPane} and presents relevant autocompletes
 * and docs.
 */
public class AutoCompleteDialog extends JDialog {

	private static final Logger LOG = Logger.getLogger(AutoCompleteDialog.class.getName());

	private static final long serialVersionUID = 1L;

	private final JEditorPane editorPane;
	private final CommandSplitPane docSplitPane;
	private final CaretListener caretListener;
	
	/* store overridden actions to later restore */
	private final Map<KeyStroke, Replacement> replacementActions;
	private final Map<KeyStroke, Object> savedActions;

	private List<DocumentedEntity> docsShown = Collections.emptyList();
	private int prevDot;

	private final DocumentedMatcher documentedEntityMatcher;
	private MouseAdapter mouseListener;

	
	public AutoCompleteDialog(final JEditorPane editorPane, JFrame parentFrame, 
			final DocumentedMatcher documentedEntityMatcher) {

		super(parentFrame);
		// problem is if undecorated have to handle resizing myself
		// setUndecorated(true); 
		this.editorPane = editorPane;
		this.documentedEntityMatcher = documentedEntityMatcher;
		prevDot = editorPane.getCaretPosition();
		
		/*
		 * SAve current key actions (to later restore) and override with ours
		 */
		replacementActions = getReplacements();
		savedActions = getActions(replacementActions.keySet(), editorPane.getInputMap());
		overrideKeyActions(editorPane, replacementActions);

		caretListener = new CaretListener() {
			@Override public void caretUpdate(CaretEvent e) {
				int newDot = e.getDot();
				if(Math.abs(newDot-prevDot)>1) {
					close();
				} else {
					prevDot = newDot;
					refreshSelection(true);
				}
			}
			
		};
		
		editorPane.addCaretListener(caretListener);
		mouseListener = new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				close();
			}
		};
		editorPane.addMouseListener(mouseListener);
		
		
		/*
		 * Create GUI layout
		 */
		docSplitPane = new CommandSplitPane();
		docSplitPane.setFgColor(Theme.SUB_HEADER_FG_COLOR);
		docSplitPane.setBgColor(Theme.SUB_HEADER_BG_COLOR);
		setLayout(new BorderLayout());
		add(docSplitPane, BorderLayout.CENTER);
		
		
		setName("autoCompleteDialog");
		setMinimumSize(DocumentationDialog.PREF_DIMENSION);
		setPreferredSize(DocumentationDialog.PREF_DIMENSION);
		refreshSelection(false);

		/*
		 * Override the various actions provided by doc selection panel
		 */
		docSplitPane.setCloseAction(new AAction(e -> close()));
		

		docSplitPane.setSelectAction(new AAction(e -> {
				Command cmd = docSplitPane.getSelectedCommand();
				if(cmd!=null) {
					cmd.perform();
				}
				dispose();
			}));
	}


	/** @return map to actions for given keyStrokes */
	private Map<KeyStroke, Object> getActions(Set<KeyStroke> ksSet, InputMap inputMap) {
		Map<KeyStroke, Object> m = Maps.newHashMap();
		for(KeyStroke ks : ksSet) {
			m.put(ks, inputMap.get(ks));
		}
		return m;
	}

	/** override editorPanes action/inputMap with our replacements */
	private static void overrideKeyActions(final JEditorPane editorPane, 
			Map<KeyStroke, Replacement> replacementActions) {
		
		InputMap iMap = editorPane.getInputMap();
		ActionMap aMap = editorPane.getActionMap();

		Iterator<Entry<KeyStroke, Replacement>> repActions;
		repActions = replacementActions.entrySet().iterator();
		
		while(repActions.hasNext()) {
			Replacement r = repActions.next().getValue();
			aMap.put(r.uniqueKey, r.action);
			iMap.put(r.ks, r.uniqueKey);
		}
	}
	


	private void doAutocomplete(DocumentedEntity documentedEntity) {

		Document d = editorPane.getDocument();
		Suggestion suggestion = null;
		try {
			String txt = d.getText(0, d.getLength());
			suggestion = documentedEntityMatcher.getSuggestion(documentedEntity, txt, prevDot);
		} catch (BadLocationException e) {
			LOG.warning("illegal doc access: " + e);
		}
		
		if(suggestion!=null) {

			Document doc = editorPane.getDocument();
			try {
				String newText = doc.getText(0, prevDot) + suggestion.getTextInsert() 
						+ doc.getText(prevDot, doc.getLength()-prevDot);
				editorPane.setText(newText);
				editorPane.setSelectionStart(prevDot+suggestion.getSelectionStart());
				editorPane.setSelectionEnd(prevDot+suggestion.getSelectionEnd());
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		close();
	}
	
	/** remove all listeners, close/dispose dialog */
	private void close() {
		LOG.info("close");
		setVisible(false);
		editorPane.removeCaretListener(caretListener);
		editorPane.removeMouseListener(mouseListener);

		// Restore previous actions
		InputMap iMap = editorPane.getInputMap();
		for(Entry<KeyStroke, Object> savedAct : savedActions.entrySet()) {
			iMap.put(savedAct.getKey(), savedAct.getValue());
		}

		dispose();
	}

	/**
	 * Refresh the choice of {@link DocumentedEntity}'s shown.
	 * @param closeIfNoMatches If true and there are no matches, close dialog.
	 * @throws BadLocationException 
	 */
	private void refreshSelection(boolean closeIfNoMatches)  {

		LOG.info("refreshSelection");
		docsShown = null;
		try {
			Document d = editorPane.getDocument();
			String txt = d.getText(0, d.getLength());
			docsShown = documentedEntityMatcher.findByPrefix(txt, prevDot);
			
		} catch (BadLocationException e) {
			LOG.fine("BadLocationException" + e.toString());
		}

		if(docsShown != null) {
			if(docsShown.size()==0 && closeIfNoMatches) {
				close();
			}
			List<Command> commands = new ArrayList<Command>();
			for (final DocumentedEntity de : docsShown) {
				DocumentedEntityDocCommand docCommand;
				docCommand = new DocumentedEntityDocCommand(de, true, "") {
					@Override public void perform() {
						doAutocomplete(de);
					}
				};
				commands.add(docCommand);
			}
			docSplitPane.setDocsShown(commands);
		}
	}
	

	/** stores keyboard overrides needed for autocomplete */
	private class Replacement {
		
		public final String uniqueKey;
		public final KeyStroke ks;
		public final Action action;

		private Replacement(String uniqueKey, KeyStroke ks, Action action) {
			super();
			this.uniqueKey = uniqueKey;
			this.ks = ks;
			this.action = action;
		}
	}
	
	/**
	 * @return The list of keys that should be overriden in the parent text 
	 * editor and instead handled by {@link AutoCompleteDialog}.
	 */
	private Map<KeyStroke, Replacement> getReplacements() {
		
		Map<KeyStroke, Replacement> r = Maps.newHashMap();
		
		// change selected doc element on key up
		Action upAction = new AAction(e ->docSplitPane.moveUp());
		KeyStroke upKey = getKeyStroke("UP");
		r.put(upKey, new Replacement("upAction", upKey, upAction));

		// change selected doc element on key down
		Action downAction = new AAction(e -> docSplitPane.moveDown());
		KeyStroke downKey = getKeyStroke("DOWN");
		r.put(downKey, new Replacement("downAction", downKey, downAction));
		
		// close dialog on escape
		Action escapeAction = new AAction(e -> close());
		r.put(ESC_KEYSTROKE, new Replacement("escapeAction", ESC_KEYSTROKE, escapeAction));

		// autcomplete does nothing
		//TODO some smarts to filter autocomplete suggestions like eclipse
		Action autocompleteAction = new AAction(e -> {});
		KeyStroke autocompleteKey = CommonActions.AUTO_COMPLETE_KS;
		r.put(autocompleteKey, new Replacement("autocompleteAction", autocompleteKey, autocompleteAction));
		
		// perform completion on enter
		Action enterAction = new AAction(e -> docSplitPane.getSelectedCommand().perform());
		KeyStroke enterKey = getKeyStroke(KeyEvent.VK_ENTER, 0);
		r.put(enterKey, new Replacement("enterAction", enterKey, enterAction));
		
		return r;
	}
	

}
