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
package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.JXTable;

import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.theme.Theme;

import lombok.Data;

/**
 * Panel that provides cross-file search functionality.
 * Allows searching across all open files and opened folders with case-sensitive and regex options.
 */
class SearchAllFilesPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int MAX_HISTORY = 20;
	
	private final JTextField searchField;
	private final JCheckBox caseSensitiveCheckbox;
	private final JCheckBox regexCheckbox;
	private final JCheckBox searchInFolderCheckbox;
	private final JXTable resultsTable;
	private final DefaultTableModel tableModel;
	private final JLabel statusLabel;
	private final OpenDocumentsModel openDocsModel;
	
	private final String[] columnNames = new String[] { "File", "Line", "Content" };
	private final List<SearchResult> searchResults = new ArrayList<>();
	private final LinkedList<String> searchHistory = new LinkedList<>();
	private int historyIndex = -1;
	private SwingWorker<Void, Void> currentSearchWorker = null;
	private String lastSearchText = "";
	
	SearchAllFilesPanel(OpenDocumentsModel openDocsModel) {
		this.openDocsModel = openDocsModel;
		setLayout(new BorderLayout());
		
		// Search controls panel
		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel searchLabel = new JLabel("Search:");
		searchField = new JTextField(30);
		searchField.putClientProperty("JTextField.placeholderText", "Press Up / Down for history");
		caseSensitiveCheckbox = new JCheckBox("Case Sensitive");
		regexCheckbox = new JCheckBox("Regex");
		searchInFolderCheckbox = new JCheckBox("Search in open folder");
		searchInFolderCheckbox.setSelected(false);
		
		searchPanel.add(searchLabel);
		searchPanel.add(searchField);
		searchPanel.add(caseSensitiveCheckbox);
		searchPanel.add(regexCheckbox);
		searchPanel.add(searchInFolderCheckbox);
		
		add(searchPanel, BorderLayout.NORTH);
		
		// Results table
		tableModel = new DefaultTableModel(columnNames, 0) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		
		resultsTable = Theme.getStripedTable(tableModel);
		resultsTable.setAutoResizeMode(JXTable.AUTO_RESIZE_LAST_COLUMN);
		TableColumnModel colModel = resultsTable.getColumnModel();
		colModel.getColumn(0).setPreferredWidth(150); // File
		colModel.getColumn(1).setPreferredWidth(60);  // Line
		colModel.getColumn(2).setPreferredWidth(600); // Content - will stretch
		
		// Make filename column bold
		JScrollPane scrollPane = new JScrollPane(resultsTable);
		add(scrollPane, BorderLayout.CENTER);
		
		// Status bar
		statusLabel = new JLabel("Ready");
		statusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(statusLabel, BorderLayout.SOUTH);
		
		// Add listeners
		// Search as user types
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) {
				performSearchDelayed();
			}
			@Override public void removeUpdate(DocumentEvent e) {
				performSearchDelayed();
			}
			@Override public void changedUpdate(DocumentEvent e) {
				performSearchDelayed();
			}
		});
		
		// Handle Enter key to add to history
		searchField.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				addToHistory(searchField.getText());
			}
		});
		
		// Handle up/down arrows for history navigation
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					navigateHistory(-1);
					e.consume();
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					navigateHistory(1);
					e.consume();
				}
			}
		});
		
		caseSensitiveCheckbox.addActionListener(e -> performSearch());
		regexCheckbox.addActionListener(e -> performSearch());
		searchInFolderCheckbox.addActionListener(e -> performSearch());
		
		// Single-click to jump to match
		resultsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1 && !e.isPopupTrigger()) {
					jumpToSelectedMatch();
				}
			}
			
			@Override public void mouseReleased(MouseEvent e) {
				handlePopup(e);
			}
			
			@Override public void mousePressed(MouseEvent e) {
				handlePopup(e);
			}
			
			private void handlePopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int row = resultsTable.rowAtPoint(e.getPoint());
					if (row >= 0) {
						resultsTable.setRowSelectionInterval(row, row);
						showPopupMenu(e);
					}
				}
			}
		});
		
		// Enter key also navigates
		resultsTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					jumpToSelectedMatch();
					e.consume();
				}
			}
		});
	}
	
	/**
	 * Focus the search field when panel is shown and select all text
	 */
	public void focusSearchField() {
		SwingUtilities.invokeLater(() -> {
			searchField.requestFocusInWindow();
			searchField.selectAll();
		});
	}
	
	/**
	 * Show popup menu for copying results
	 */
	private void showPopupMenu(MouseEvent e) {
		int row = resultsTable.getSelectedRow();
		if (row < 0 || row >= searchResults.size()) {
			return;
		}
		
		JPopupMenu popup = new JPopupMenu();
		SearchResult result = searchResults.get(row);
		
		JMenuItem copyLine = new JMenuItem("Copy Line Content", Theme.CIcon.EDIT_COPY.get16());
		copyLine.addActionListener(ev -> {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(result.getLine()), null);
		});
		
		String fp = result.getFilePath();
		JMenuItem copyFilePath = new JMenuItem("Copy File Path", Theme.CIcon.EDIT_COPY.get16());
		copyFilePath.setEnabled(fp != null && !fp.isEmpty());
		copyFilePath.addActionListener(ev -> {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(result.getFilePath()), null);
		});
		
		JMenuItem copyAllResults = new JMenuItem("Copy All Results", Theme.CIcon.EDIT_COPY.get16());
		copyAllResults.addActionListener(ev -> {
			StringBuilder sb = new StringBuilder();
			for (SearchResult r : searchResults) {
				sb.append(r.getName()).append(":").append(r.getLineNumber())
				  .append(": ").append(r.getLine()).append("\n");
			}
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(sb.toString()), null);
		});
		
		popup.add(copyLine);
		popup.add(copyFilePath);
		popup.addSeparator();
		popup.add(copyAllResults);
		popup.show(e.getComponent(), e.getX(), e.getY());
	}
	
	/**
	 * Navigate through search history
	 */
	private void navigateHistory(int direction) {
		if (searchHistory.isEmpty()) {
			return;
		}
		
		// Add current search to history when first pressing up
		if (direction < 0 && historyIndex == -1) {
			String currentText = searchField.getText();
			if (!currentText.trim().isEmpty() && !searchHistory.contains(currentText)) {
				addToHistory(currentText);
			}
		}
		
		historyIndex += direction;
		if (historyIndex < 0) {
			historyIndex = 0;
		} else if (historyIndex >= searchHistory.size()) {
			historyIndex = searchHistory.size() - 1;
		}
		
		if (historyIndex >= 0 && historyIndex < searchHistory.size()) {
			String historyItem = searchHistory.get(historyIndex);
			searchField.setText(historyItem);
		}
	}
	
	/**
	 * Add search text to history
	 */
	private void addToHistory(String text) {
		if (text == null || text.trim().isEmpty()) {
			return;
		}
		searchHistory.remove(text); // Remove if already exists
		searchHistory.addFirst(text); // Add to front
		
		while (searchHistory.size() > MAX_HISTORY) {
			searchHistory.removeLast();
		}
		
		historyIndex = -1;
	}
	
	/**
	 * Perform search with a small delay to avoid searching on every keystroke
	 */
	private void performSearchDelayed() {
		String currentText = searchField.getText();
		
		// Cancel previous search if still running
		if (currentSearchWorker != null && !currentSearchWorker.isDone()) {
			currentSearchWorker.cancel(true);
		}
		
		// Only search if text has changed
		if (!currentText.equals(lastSearchText)) {
			lastSearchText = currentText;
			performSearch();
		}
	}
	
	private void performSearch() {
		final String searchText = searchField.getText();
		if (searchText.isEmpty()) {
			clearResults();
			statusLabel.setText("Ready");
			return;
		}
		
		statusLabel.setText("Searching...");
		clearResults();
		
		// Perform search in background
		currentSearchWorker = new SwingWorker<Void, Void>() {
			private int filesSearched = 0;
			private int matchCount = 0;
			
			@Override
			protected Void doInBackground() throws Exception {
				Pattern pattern = createSearchPattern(searchText);
				if (pattern == null) {
					return null;
				}
				
				// Search in open documents
				List<Document> docs = openDocsModel.getDocuments();
				for (Document doc : docs) {
					if (isCancelled()) {
						return null;
					}
					filesSearched++;
					matchCount += searchInDocument(doc, pattern, searchText);
				}
				
				// Search in selected folder if checkbox is enabled
				if (searchInFolderCheckbox.isSelected()) {
					File selectedFolder = openDocsModel.getSelectedFolder();
					if (selectedFolder != null && selectedFolder.isDirectory()) {
						searchInFolder(selectedFolder, pattern, searchText);
					}
				}
				
				return null;
			}
			
			@Override
			protected void done() {
				if (!isCancelled()) {
					updateStatusLabel();
				}
			}
			
			private void searchInFolder(File folder, Pattern pattern, String searchText) {
				if (isCancelled()) {
					return;
				}
				
				File[] files = folder.listFiles();
				if (files == null) {
					return;
				}
				
				for (File file : files) {
					if (isCancelled()) {
						return;
					}
					
					if (file.isDirectory()) {
						// Check if folder should be ignored
						if (openDocsModel.getIgnoredFolderPattern() != null &&
							openDocsModel.getIgnoredFolderPattern().matcher(file.getName()).matches()) {
							continue;
						}
						searchInFolder(file, pattern, searchText);
					} else if (file.isFile() && isTextFile(file) && !openDocsModel.isFileOpen(file)) {
						filesSearched++;
						matchCount += searchInFile(file, pattern, searchText);
					}
				}
			}
			
			private int searchInDocument(Document doc, Pattern pattern, String searchText) {
				int matches = 0;
				String content = doc.getContent();
				String[] lines = content.split("\n");
				
				for (int i = 0; i < lines.length; i++) {
					if (isCancelled()) {
						return matches;
					}
					
					String line = lines[i];
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						matches++;
						int lineNumber = i + 1;
						String highlightedContent = highlightMatch(line.trim(), searchText, pattern);
						addResult(new DocumentSearchResult(doc, lineNumber, highlightedContent));
					}
				}
				return matches;
			}
			private static final long MAX_FILE_SIZE = 11L * 1024 * 1024; // 11 MB

			private int searchInFile(File file, Pattern pattern, String searchText) {
			    // Skip large files
			    if (file.length() > MAX_FILE_SIZE) {
			        return 0;
			    }

			    int matches = 0;

			    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			        String line;
			        int lineNumber = 0;

			        while ((line = reader.readLine()) != null) {
			            if (isCancelled()) {
			                return matches;
			            }

			            lineNumber++;

			            Matcher matcher = pattern.matcher(line);
			            if (matcher.find()) {
			                matches++;
			                String highlightedContent = highlightMatch(line.trim(), searchText, pattern);
			                addResult(new FileSearchResult(file, lineNumber, highlightedContent));
			            }
			        }

			    } catch (IOException e) {
			        // Skip unreadable files
			    }

			    return matches;
			}

			
			private boolean isTextFile(File file) {
				String name = file.getName().toLowerCase();
				return name.endsWith(".q") || name.endsWith(".sql") || name.endsWith(".txt") ||
					   name.endsWith(".java") || name.endsWith(".js") || name.endsWith(".py") ||
					   name.endsWith(".c") || name.endsWith(".cpp") || name.endsWith(".h") ||
					   name.endsWith(".cs") || name.endsWith(".xml") || name.endsWith(".json") ||
					   name.endsWith(".prql") || name.endsWith(".k") || name.endsWith(".dos") ||
					   name.endsWith(".md") || name.endsWith(".rfl") || name.endsWith(".markdown") ||
					   name.endsWith(".html") || name.endsWith(".css") || name.endsWith(".md");
			}
		};
		
		currentSearchWorker.execute();
	}
	
	/**
	 * Highlight the matched text in the content line by surrounding it with ** markers
	 */
	private String highlightMatch(String content, String searchText, Pattern pattern) {
		if (regexCheckbox.isSelected()) {
			// For regex, use the pattern
			Matcher matcher = pattern.matcher(content);
			if (matcher.find()) {
				String before = content.substring(0, matcher.start());
				String match = content.substring(matcher.start(), matcher.end());
				String after = content.substring(matcher.end());
				return before + "**" + match + "**" + after;
			}
		} else {
			// For plain text, find and highlight
			int flags = caseSensitiveCheckbox.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
			Pattern p = Pattern.compile(Pattern.quote(searchText), flags);
			Matcher matcher = p.matcher(content);
			if (matcher.find()) {
				String before = content.substring(0, matcher.start());
				String match = content.substring(matcher.start(), matcher.end());
				String after = content.substring(matcher.end());
				return before + "**" + match + "**" + after;
			}
		}
		return content;
	}
	
	private Pattern createSearchPattern(String searchText) {
		try {
			int flags = caseSensitiveCheckbox.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
			String patternText = regexCheckbox.isSelected() ? searchText : Pattern.quote(searchText);
			return Pattern.compile(patternText, flags);
		} catch (PatternSyntaxException e) {
			SwingUtilities.invokeLater(() -> statusLabel.setText("Invalid regex pattern: " + e.getMessage()));
			return null;
		}
	}
	
	private void addResult(SearchResult result) {
		searchResults.add(result);
		SwingUtilities.invokeLater(() -> {
			tableModel.addRow(new Object[] { result.getName(), result.getLineNumber(), result.getLine() });
		});
	}
	
	private void clearResults() {
		searchResults.clear();
		tableModel.setRowCount(0);
	}
	
	private void updateStatusLabel() {
		int matchCount = searchResults.size();
		long fileCount = searchResults.stream().map(sr -> sr.getName()+sr.getFilePath()).distinct().count();
		statusLabel.setText(fileCount + " file" + (fileCount != 1 ? "s" : "") + " , " + matchCount + " match" + (matchCount != 1 ? "es" : "") + " found");
	}
	
	private void jumpToSelectedMatch() {
		int selectedRow = resultsTable.getSelectedRow();
		if (selectedRow >= 0 && selectedRow < searchResults.size()) {
			// Add current search to history when clicking a result
			String currentSearch = searchField.getText();
			if (!currentSearch.trim().isEmpty()) {
				addToHistory(currentSearch);
			}
			SearchResult result = searchResults.get(selectedRow);
			try {
				result.open(openDocsModel);
			} catch (IOException e) {
				statusLabel.setText("Error opening file: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Calculate the character position for a given line number
	 */
	private static int getPositionForLine(String content, int lineNumber) {
		String[] lines = content.split("\n", lineNumber);
		if (lines.length < lineNumber) {
			return content.length();
		}
		int position = 0;
		for (int i = 0; i < lineNumber - 1 && i < lines.length; i++) {
			position += lines[i].length() + 1; // +1 for newline
		}
		return position;
	}

	
	@Data private static class FileSearchResult implements SearchResult {
		final File file;
		final int lineNumber;
		final String line;
		
		@Override public String getFilePath() { return file.getAbsolutePath(); }
		@Override public String getName() { return file.getName(); }
		
		@Override public void open(OpenDocumentsModel openDocsModel) throws IOException {
			Document doc = openDocsModel.openDocument(file.getAbsolutePath());
			openDocsModel.setSelectedDocument(doc);
			int position = getPositionForLine(doc.getContent(), getLineNumber());
			doc.setCaratPosition(position);
		}
	}

	
	@Data private static class DocumentSearchResult implements SearchResult {
		final Document document;
		final int lineNumber;
		final String line;
		
		@Override public String getFilePath() { return document.getFilePath(); }
		@Override public String getName() { return document.getTitle(); }

		@Override public void open(OpenDocumentsModel openDocsModel) throws IOException {
			openDocsModel.setSelectedDocument(document);
			int position = getPositionForLine(document.getContent(), getLineNumber());
			document.setCaratPosition(position);
		}
	}
	
	
	private static interface SearchResult {
		void open(OpenDocumentsModel openDocsModel) throws IOException;
		int getLineNumber();
		String getName();
		String getFilePath();
		String getLine();
	}
	
	
}
