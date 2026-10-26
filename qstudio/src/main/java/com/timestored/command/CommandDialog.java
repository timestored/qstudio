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
package com.timestored.command;

import static com.timestored.swingxx.SwingUtils.ESC_KEYSTROKE;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.timestored.swingxx.AAction;

/**
 * Popup dialog that allows choosing commands. 
 * Destroyed once a choice is made. 
 */
public class CommandDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	private static final Dimension PREF_DIMENSION = new Dimension(500,500);
	private static final int RESULT_LIMIT = 100;
	private final CommandPanel commandPanel;
	private String prevSearch = "";
	private Collection<Command> commands = Collections.emptyList();
	private final ExecutorService executorService;
	

	/**
	 * Create a titled dialog displaying the commands.
	 * @param commands The list of commands from which all or some may be shown.
	 */
	public CommandDialog(String title, final List<Command> commands, ExecutorService executorService) {
		this(title, new CommandProvider() {
			@Override public Collection<Command> getCommands() {
				return commands;
			}
		}, executorService);
	}
	
	
	public CommandDialog(String title, final CommandProvider commandProvider, ExecutorService executorService) {
		
		this.executorService = Preconditions.checkNotNull(executorService);
		setTitle(title);
		setName("CodeOutlineDialog");
		setMinimumSize(PREF_DIMENSION);
		setPreferredSize(PREF_DIMENSION);

		commandPanel = new CommandPanel();
		final JTextField searchTextField = new JTextField();
		
		/*
		 * Override the up/down/escape within the textfield to move within
		 * the entities
		 */
		ActionMap am = searchTextField.getActionMap();
		InputMap im = searchTextField.getInputMap();
		am.put("upAction", new AAction(e -> commandPanel.moveUp()));
		im.put(getKeyStroke("UP"), "upAction");

		am.put("downAction", new AAction(e -> commandPanel.moveDown()));
		im.put(getKeyStroke("DOWN"), "downAction");
		
		am.put("escapeAction", new AAction(e ->  dispose()));
		im.put(ESC_KEYSTROKE, "escapeAction");
		
		searchTextField.addActionListener(new AAction(e -> {
				Command com = commandPanel.getSelectedCommand();
				if(com!=null) {
					com.perform();
				}
				dispose();
			}));
		
		Utils.addEscapeCloseListener(this);
		Utils.putEscapeAction(searchTextField, new AAction(e -> dispose()));
		
		commandPanel.setSelectAction(new AAction(e -> {
				Command com = commandPanel.getSelectedCommand();
				if(com!=null) {
					com.perform();
				}
				dispose();
			}));
		
		this.addWindowFocusListener(new WindowFocusListener() {
			@Override public void windowLostFocus(WindowEvent e) { dispose(); }
			@Override public void windowGainedFocus(WindowEvent e) { }
		});
		
		searchTextField.addKeyListener(new KeyAdapter() {

			@Override public void keyReleased(KeyEvent e) {
				KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
				if(!ks.equals(getKeyStroke("UP")) && !ks.equals(getKeyStroke("DOWN"))) {
					
					final String txt = searchTextField.getText();
					if(!txt.equals(prevSearch)) {
			        	showDocsForSearch(txt);
					    prevSearch = txt;
					}
				}
			}
		});

		setLayout(new BorderLayout());
		add(searchTextField, BorderLayout.NORTH);
		add(commandPanel, BorderLayout.CENTER);
		searchTextField.requestFocus();
		
		
		pack();
		
		this.addFocusListener(new FocusListener() {
			@Override public void focusLost(FocusEvent e) {
				setVisible(false);	
				dispose();
			}
			@Override public void focusGained(FocusEvent e) {}
		});
		
		executorService.execute(new Runnable() {
			@Override public void run() {
				commands = commandProvider.getCommands();
				EventQueue.invokeLater(() -> showDocsForSearch(prevSearch));
			}
		});
		
	}

	

	/** Only show commands that match this search query **/
	private void showDocsForSearch(final String txt) {
		if(txt.trim().length() > 0) {
			String[] t = txt.trim().split(" ");
			List<Command> r = new ArrayList<Command>();
			for(Command c : commands) {
				boolean match = true;
				if(t.length>0) {
			    	for(String s : t) {
			    		if(!c.getTitle().toUpperCase().contains(s.toUpperCase())) {
			    			match = false;
			    			break;
			    		}
			    	}
				}
				if(match) {
					r.add(c);
				}
				if(r.size() > RESULT_LIMIT) {
					break; // show no more than 100 rows
				}
			}
			commandPanel.setCommands(r);
		} else {
			List<Command> l = Lists.newArrayList(commands);
			if(l.size()>100) {
				l = l.subList(0, RESULT_LIMIT);
			}
			commandPanel.setCommands(l);
		}
	}
	
	public void setSelectedCommand(Command command) {
		commandPanel.setSelectedCommand(command);
	}
}

