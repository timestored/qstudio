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
package com.timestored.pro.csvloader;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;

import com.google.common.base.Preconditions;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.ServerConfig;
import com.timestored.connections.ServerNameComboBox;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.qstudio.BackgroundExecutor;
import com.timestored.qstudio.QStudioFrame;
import com.timestored.qstudio.QStudioLauncher;
import com.timestored.swingxx.SwingUtils;
import com.timestored.theme.Theme;
import com.timestored.theme.Theme.InputLabeller;

/**
 * Allows selecting a csv for import to KDB, configuring what separators it
 * uses etc and previewing how it will look. 
 */
public class CsvLoaderFrame extends JFrame implements CsvLoaderModel.Listener {

	private static final Logger LOG = Logger.getLogger(CsvLoaderFrame.class.getName());
	/** counter to choose a likely free name for a table */
	private static int count = 1;

	private static final int PREF_WIDTH = 1000;
	private static final int PREF_HEIGHT = 800;
	private static final int MIN_HEIGHT = 600;
	private static final int MIN_WIDTH = 700;
	private static final long serialVersionUID = 1L;
	private static final InputLabeller LABELLER = Theme.getInputLabeller(80, 20);
	
	private final JFilePicker filePicker;
	private final JComboBox charSetComboBox;
	private final JSpinner rowSpinner;
	private final JButton loadButton;
	private final JButton cancelButton;
	private final JTextField separatorTF;
	private final JTextField quoteTF;
	private SortedMap<String, Charset> charsets;
	private final JPanel previewPanel;
	private final JCheckBox headerCheckBox;
	private final ServerNameComboBox serverComboBox;
	private final JPanel container;
	private final JTextField tableNameTextField;
	
	private JDialog progressCancelDialog;

	private final CsvLoaderModel csvLoaderModel;
	private LoadWorker loadWorker;

	private JTextField rowLoadTextField;
	
	
	public static void showCsvImporter(QStudioFrame qStudioFrame, ConnectionManager connectionManager, ServerConfig selectedServer) throws IOException {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setDialogTitle(Msg.get(Key.SELECT_FILE_TO_IMPORT));
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		fc.setMultiSelectionEnabled(false);
		fc.setApproveButtonText(Msg.get(Key.IMPORT));
		
		if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			CsvLoaderModel csvLoaderModel;
			File file = fc.getSelectedFile();
			if(file != null) {
				csvLoaderModel = new CsvLoaderModel(connectionManager, file);
				csvLoaderModel.setServer(selectedServer);
		    	CsvLoaderFrame window = new CsvLoaderFrame(csvLoaderModel);
				SwingUtils.setSensibleDimensions(qStudioFrame, window);
		    	window.setLocationRelativeTo(qStudioFrame);
		        window.setVisible(true);
		        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			}
		}
	}
	
	CsvLoaderFrame(final CsvLoaderModel csvLoaderModel) {
		
		this.csvLoaderModel = Preconditions.checkNotNull(csvLoaderModel);
		String tName = csvLoaderModel.getTableName();
		if(tName==null || tName.trim().length()==0) {
			csvLoaderModel.setTableName("tab" + count++);
		}
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(Theme.CIcon.CSV.get16().getImage());
		setTitle("CSV Importer");
		
		/*
		 * Create the inputs
		 */
		filePicker = new JFilePicker("Pick a file", "Browse..."); 
		filePicker.setMode(JFilePicker.Mode.OPEN);
		filePicker.setSelectedFile(csvLoaderModel.getCsvFile());
		filePicker.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				if(filePicker.getSelectedFile() != null) {
					csvLoaderModel.setCsvFile(filePicker.getSelectedFile());
				}
			}
		});
		
		serverComboBox = new ServerNameComboBox(csvLoaderModel.getConnectionManager());
		serverComboBox.setPreferredSize(new Dimension(MIN_WIDTH-100, (int) serverComboBox.getPreferredSize().getHeight()));
		serverComboBox.setSelectedItem(csvLoaderModel.getServer().getName());
		serverComboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				csvLoaderModel.setServer(serverComboBox.getSelectedServer());
			}
		});
//		serverComboBox.setPreferredSize(new Dimension(101, (int) serverComboBox.getPreferredSize().getHeight()));
		
		tableNameTextField = new JTextField();
		tableNameTextField.setColumns(20);
		tableNameTextField.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				csvLoaderModel.setTableName(tableNameTextField.getText());
			}
		});
		tableNameTextField.addFocusListener(new FocusAdapter() {
			@Override public void focusLost(FocusEvent e) {
				try {
					csvLoaderModel.setTableName(tableNameTextField.getText());
				} catch(IllegalArgumentException iae) {
					JOptionPane.showMessageDialog(CsvLoaderFrame.this, iae.getMessage());
				}
			}
		});
		
		charsets = Charset.availableCharsets();
		charSetComboBox = new JComboBox(charsets.keySet().toArray());
		charSetComboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				String cs = (String) charSetComboBox.getSelectedItem();
				csvLoaderModel.getCsvConfig().setCharset(cs);
				csvLoaderModel.notifyListeners();
			}
		});
		
		rowSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
		rowSpinner.addChangeListener(new ChangeListener() {
			
			@Override public void stateChanged(ChangeEvent e) {
				try {
					Integer row = (Integer) rowSpinner.getValue();
					csvLoaderModel.getCsvConfig().setSkipLines(row);
					csvLoaderModel.notifyListeners();
				} catch(ClassCastException cce) { /* do nothing */ } 
				catch(IllegalArgumentException iae) { /* do nothing */ }
			}
		});

		headerCheckBox = new JCheckBox("Contains header row");
		headerCheckBox.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				csvLoaderModel.getCsvConfig().setContainsHeader(headerCheckBox.isSelected());
				csvLoaderModel.notifyListeners();
			}
		});
		
		separatorTF = new JTextField(1);
		separatorTF.addKeyListener(new KeyAdapter() {
			@Override public void keyTyped(KeyEvent e) {
				csvLoaderModel.getCsvConfig().setSeparator(e.getKeyChar());
				csvLoaderModel.notifyListeners();
			}
		});
		separatorTF.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) { separatorTF.selectAll();  }
		});
		quoteTF = new JTextField(1);
		quoteTF.addKeyListener(new KeyAdapter() {
			@Override public void keyTyped(KeyEvent e) {
				csvLoaderModel.getCsvConfig().setQuote(e.getKeyChar());
				csvLoaderModel.notifyListeners();
			}
		});
		quoteTF.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) { quoteTF.selectAll();  }
		});


		previewPanel = new JPanel(new BorderLayout());
		previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
		

		loadButton = new JButton("Load", Theme.CIcon.SERVER_GO.get16());
		loadButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				executeLoad(CsvLoaderFrame.this, csvLoaderModel);
			}
		});
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				CsvLoaderFrame.this.dispatchEvent(new WindowEvent(CsvLoaderFrame.this, WindowEvent.WINDOW_CLOSING));
			}
		});
		

		/* Add separate panels to container and display	 */
		Box b = Box.createVerticalBox();
		b.add(getImportPanel());
		b.add(getSeparatorPanel());
		b.add(getDestinationPanel());
		container = new JPanel(new BorderLayout());
		container.add(b, BorderLayout.NORTH);
		container.add(previewPanel, BorderLayout.CENTER);
		container.add(getSaveCancelPanel(), BorderLayout.SOUTH);
		setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(container, BorderLayout.CENTER);
		
		csvLoaderModel.addListener(this);
		refreshUI();
	}

	private JPanel getSaveCancelPanel() {
		JPanel saveCancelPanel = new JPanel();
		JPanel p = getGrid(loadButton, cancelButton);
		p.setPreferredSize(new Dimension(300, 40));
		p.setMinimumSize(new Dimension(300, 40));
		saveCancelPanel.add(p);
		return saveCancelPanel;
	}

	private JPanel getSeparatorPanel() {
		JPanel sepPanel = new TitlePanel("Separator Options");
		sepPanel.add(getGrid(LABELLER.get("Separator:", separatorTF, "separator"),
				LABELLER.get("Quote:", quoteTF, "quote")));
		return sepPanel;
	}

	private JPanel getImportPanel() {
		JPanel p = new TitlePanel("Import");
		p.add(filePicker);
		p.add(LABELLER.get("Character Set:", charSetComboBox, "charSet"));
		p.add(getGrid(LABELLER.get("From Row:", rowSpinner, "row"), headerCheckBox));
		return p;
	}
	


	private synchronized void executeLoad(final JFrame parent, final CsvLoaderModel csvLoaderModel) {

		if(loadWorker!=null) {
			showMsg("Load already in progress, must finish current load operation before starting another");
			return;
		}
		
		try {
			CSVLoader csvLoader = csvLoaderModel.getCsvLoader();
			if(csvLoader.checkTableNameFree()) {
				
				loadWorker = new LoadWorker(csvLoader);
				// show loading screen
				progressCancelDialog = new JDialog(parent, "Loading...");
				JProgressBar pbar = new JProgressBar();
				pbar.setIndeterminate(true);
				rowLoadTextField = new JTextField(40);
				rowLoadTextField.setText("0 rows loaded");
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					@Override public void actionPerformed(ActionEvent e) {
						synchronized (CsvLoaderFrame.this) {
							if(loadWorker!=null) {
								loadWorker.cancel(true);
							}
						}
					}
				});
				
				progressCancelDialog.setLayout(new BorderLayout());
				progressCancelDialog.add(pbar, BorderLayout.NORTH);
				progressCancelDialog.add(rowLoadTextField, BorderLayout.CENTER);
				progressCancelDialog.add(cancelButton, BorderLayout.SOUTH);
				
				progressCancelDialog.setLocationRelativeTo(null);
				progressCancelDialog.setSize(200, 100);
				progressCancelDialog.setVisible(true);
				BackgroundExecutor.EXECUTOR.execute(loadWorker);
			} else {
				showMsg("That table name is already in use please choose another");
			}
		} catch (IOException e) {
			showMsg("IO error communicating with server", e);
		} catch(IllegalStateException ise) {
			showMsg(ise.getMessage(), ise);
		}
		
	}

	private void showMsg(Object message) { showMsg(message, null); }
	private void showMsg(Object message, Exception e) {
		if(message instanceof String) {
			LOG.log(Level.INFO, (String)message, e);
		}
		JOptionPane.showMessageDialog(CsvLoaderFrame.this, message);
	}
	
	private class LoadWorker extends SwingWorker<Integer, Integer> {

		private final CSVLoader csvLoader;
		int rows = 0;
		
		public LoadWorker(CSVLoader csvLoader) {
			this.csvLoader = csvLoader;
		}
		
		@Override public Integer doInBackground() {

			final CsvLoaderFrame frame = CsvLoaderFrame.this;
			String errMessage = null;
			
			try {
				EventQueue.invokeLater(new Runnable() {
					@Override public void run() {
						frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}
				});
				while(csvLoader.processRow() && !isCancelled()) {
					rows = csvLoader.getRowsLoaded();
					if(rows%100==0) {
						publish(rows);
					}
				}
			} catch(IOException ioe) {
				errMessage = "Problem sending data to server, not all data may have been sent";
			} catch(IllegalArgumentException iae) {
				errMessage = iae.getMessage();
			} catch(IllegalStateException ise) {
				errMessage = ise.getMessage();
			}
			EventQueue.invokeLater(new Runnable() {
				@Override public void run() {
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			});
			if(errMessage != null) {
				JOptionPane.showMessageDialog(frame, 
						errMessage, "Error loading file", JOptionPane.WARNING_MESSAGE);
			}
			return rows;
	    }

		@Override protected void process(List<Integer> rowReports) {
			Integer r = rowReports.get(rowReports.size()-1);
			rowLoadTextField.setText(r.toString() + " rows loaded");
		}
		
	    @Override public void done() {

	    	Integer bReport = null;
			Exception e = null;
	    	synchronized (CsvLoaderFrame.this) {
				progressCancelDialog.setVisible(false);
				progressCancelDialog.dispose();
				progressCancelDialog = null;
				loadWorker = null;
			}
			
			try {
				bReport = get();
			} catch (InterruptedException ie) {
				e = ie;
			} catch (ExecutionException ee) {
				e = ee;
			} catch(CancellationException ce) {
				e = ce;
			}
			if(e!=null) {
				LOG.log(Level.WARNING, "csv loader problem", e);
			}
			
			if(e instanceof CancellationException) {
				showMsg(rows + " rows loaded then user cancelled.");
			} else if(bReport != null) {
				showMsg(rows + " rows sent");
			} else {
				String txt = "Problem Running CSV Load";
				showMsg(QStudioLauncher.ERR_REPORTER.getErrorReportLink(e, txt));
			}
	    }
	}
	
	private class TitlePanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JPanel b;

		public TitlePanel(String title) {
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createTitledBorder(title));
			b = Theme.getVerticalBoxPanel();
			add(b, BorderLayout.CENTER);
		}
		
		@Override public Component add(Component c) {
			return b.add(c);
		}
	}
	

	
	private static JPanel getGrid(Component left, Component right) {
		JPanel g = new JPanel(new BorderLayout());
		g.add(left, BorderLayout.WEST);
		g.add(right, BorderLayout.CENTER);
		return g;
	}
	
	private JPanel getDestinationPanel() {
		TitlePanel p = new TitlePanel("Destination");
		p.add(LABELLER.get(Msg.get(Key.SERVER) + ":", serverComboBox, Msg.get(Key.SERVER))); 
		p.add(LABELLER.get("Table:", tableNameTextField, "tableName"));		
		return p;
	}
	
	private void refreshUI() {
		
		CsvConfig csvConfig = csvLoaderModel.getCsvConfig();
		Charset ch = charsets.get(csvConfig.getCharset());
		if(ch != null) {
			if(!charSetComboBox.getSelectedItem().equals(ch.name())) {
				charSetComboBox.setSelectedItem(ch.name());	
			}
		}
		if(!rowSpinner.getValue().equals(csvConfig.getSkipLines())) {
			rowSpinner.setValue(csvConfig.getSkipLines());
		}
		if(!separatorTF.getText().equals(csvConfig.getSeparator())) {
			separatorTF.setText(""+csvConfig.getSeparator());
		}
		if(!quoteTF.getText().equals(csvConfig.getQuote())) {
			quoteTF.setText(""+csvConfig.getQuote());
		}
		if(!headerCheckBox.isSelected()==csvConfig.containsHeader()) {
			headerCheckBox.setSelected(csvConfig.containsHeader());
		}
		String tName = csvLoaderModel.getTableName();
		if(!tableNameTextField.getText().equals(tName)) {
			tableNameTextField.setText(tName);
		}
		
		ServerConfig sc = csvLoaderModel.getServer();
		if(sc!=null) {
			serverComboBox.setSelectedItem(sc.getName());	
		}
		
		File pfile = csvLoaderModel.getCsvFile();
		filePicker.setSelectedFile(csvLoaderModel.getCsvFile());


		boolean validFile = pfile!=null && pfile.isFile() && pfile.canRead();
		loadButton.setEnabled(validFile);
		// preview
		previewPanel.removeAll();
		if(validFile) {
			String t = "CSV Importer - ";
			t += pfile!=null ? pfile.getName() : "";
			setTitle(t);
			TableModel tm = null;
			try {
				tm = CSVLoader.getTopTable(pfile.getAbsolutePath(), csvConfig, 100);
			} catch(UnsupportedOperationException uoe)  {
				// fall through
			}
			if(tm != null) {
				JXTable tab = Theme.getStripedTable(tm);
				tab.setSortable(false);
				previewPanel.add(new JScrollPane(tab), BorderLayout.CENTER);
			} else {
				previewPanel.add(Theme.getHtmlText("Cannot parse file"), BorderLayout.CENTER);
			}
		} else {
			previewPanel.add(Theme.getHtmlText("Please select a valid file"), BorderLayout.CENTER);
		}
		previewPanel.revalidate();
	}
	
	@Override public void update(CsvLoaderModel csvLoaderModel) {
		refreshUI();
	}
}
