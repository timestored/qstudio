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

import static com.timestored.theme.Theme.getFormRow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import com.timestored.misc.IOUtils;
import com.timestored.qstudio.EditorConfigFactory.EditorConfig;
import com.timestored.theme.Theme;

import jsyntaxpane.DefaultSyntaxKit;

/**
 * GUI Panel that allows configuring editor theme customization with a live preview.
 */
class EditorThemePreferencesPanel extends PreferencesPanel {

	private static final long serialVersionUID = 1L;
	
	private final JComboBox<String> editorThemeComboBox;
	private final JRadioButton kdbRadioButton;
	private final JRadioButton duckdbRadioButton;
	private final JPanel previewContainer;
	private JEditorPane previewPane;
	private JScrollPane scrollPane;
	private String kdbExampleCode;
	private String duckdbExampleCode;

	public EditorThemePreferencesPanel(final MyPreferences myPreferences, final Component container) {
		super(myPreferences, container);
		
		setLayout(new BorderLayout(10, 10));
		setBorder(Theme.getCentreBorder());
		
		// Top panel with theme selection
		Box topPanel = Box.createVerticalBox();
		
		String[] themeNames = EditorConfigFactory.getNames();
		editorThemeComboBox = new JComboBox<>(themeNames);
		String tooltipText = "<html>Select a syntax highlighting theme for the code editor preview.</html>";
		topPanel.add(getFormRow(editorThemeComboBox, "Editor Syntax Theme:", tooltipText));
		topPanel.add(Box.createVerticalStrut(10));
		
		// Code type toggle
		Box toggleBox = Box.createHorizontalBox();
		JLabel previewLabel = new JLabel("Preview Code:");
		kdbRadioButton = new JRadioButton("KDB/Q");
		duckdbRadioButton = new JRadioButton("DuckDB SQL");
		ButtonGroup group = new ButtonGroup();
		group.add(kdbRadioButton);
		group.add(duckdbRadioButton);
		kdbRadioButton.setSelected(true);
		
		toggleBox.add(previewLabel);
		toggleBox.add(Box.createHorizontalStrut(10));
		toggleBox.add(kdbRadioButton);
		toggleBox.add(Box.createHorizontalStrut(10));
		toggleBox.add(duckdbRadioButton);
		toggleBox.add(Box.createHorizontalGlue());
		topPanel.add(toggleBox);
		topPanel.add(Box.createVerticalStrut(10));
		
		add(topPanel, BorderLayout.NORTH);
		
		// Initialize jsyntaxpane
		DefaultSyntaxKit.initKit();
		
		// Preview container - we'll recreate the editor pane for each theme change
		previewContainer = new JPanel(new BorderLayout());
		previewContainer.setBorder(BorderFactory.createTitledBorder("Preview"));
		previewContainer.setPreferredSize(new Dimension(600, 350));
		
		add(previewContainer, BorderLayout.CENTER);
		
		// Load example code
		loadExampleCode();
		
		// Add listeners
		editorThemeComboBox.addActionListener(e -> {
            myPreferences.setCodeEditorTheme(editorThemeComboBox.getSelectedItem().toString());
            updatePreview();
		});
		kdbRadioButton.addActionListener(e -> updatePreview());
		duckdbRadioButton.addActionListener(e -> updatePreview());
		
		refresh();
	}
	
	private void loadExampleCode() {
		kdbExampleCode = getKdbFallbackCode();
		try {
			String kdbCode = IOUtils.toString(EditorThemePreferencesPanel.class, "kdb-examples.q");
			kdbExampleCode = kdbExampleCode + "\r\n\r\n" + kdbCode;
		} catch (IOException e) {} 
		
		duckdbExampleCode = getDuckdbFallbackCode();
		try {
			String duckdbFileText = IOUtils.toString(EditorThemePreferencesPanel.class, "duckdb.sql");
			duckdbExampleCode = duckdbExampleCode + "\r\n\r\n" + duckdbFileText;
		} catch (IOException e) { }
		
	}
	
	private String getKdbFallbackCode() {
		return "/ KDB Example Code\n" +
			"system \"d .namespace\";\n\n" +
			"/ Time Series Query\n" +
			"([] dt:2013.01.01+til 21; cosineWave:cos a; sineWave:sin a:0.6*til 21)\n\n" +
			"/ Table with data\n" +
			"([Month:2000.01m + til 12]\n" +
			"    Costs:30.0 40.0 45.0 55.0 58.0 63.0 55.0 65.0 78.0 80.0 75.0 90.0;\n" +
			"    Sales:10.0 12.0 14.0 18.0 26.0 42.0 74.0 90.0 110.0 130.0 155.0 167.0)\n\n" +
			"/ Function definition\n" +
			"myFunc:{[x;y]\n" +
			"    result: x + y * 2;\n" +
			"    :result\n" +
			"}\n\n" +
			"/ Boolean and string operations\n" +
			"isValid: 1b\n" +
			"message: \"Hello World\"\n" +
			"symbols: `apple`banana`cherry\n";
	}
	
	private String getDuckdbFallbackCode() {
		return "-- DuckDB SQL Example\n" +
			"-- Time Series Query\n" +
			"WITH b AS (\n" +
			"    SELECT DATE '2013-01-01' + INTERVAL '1 day' * i AS dt, i\n" +
			"    FROM generate_series(0, 20) t(i)\n" +
			")\n" +
			"SELECT dt, COS(i) AS cosineWave, SIN(0.6 * i) AS sineWave FROM b;\n\n" +
			"-- Multi-series Table\n" +
			"SELECT * FROM (\n" +
			"    VALUES\n" +
			"    ('NorthAmerica', 'US', 313847, 15080, 48300, 77.14),\n" +
			"    ('Asia', 'China', 1343239, 11300, 8400, 72.22),\n" +
			"    ('Europe', 'Germany', 81308, 3114, 38100, 78.42)\n" +
			") t(Continent, Country, Population, GDP, GDPperCapita, LifeExpectancy);\n\n" +
			"-- String and numeric operations\n" +
			"SELECT\n" +
			"    id,\n" +
			"    TIMESTAMP '2025-01-01 00:00:00' AS time,\n" +
			"    90 + random() * 20 AS price,\n" +
			"    'http://example.com' AS link\n" +
			"FROM range(10) t(id);\n";
	}
	
	private void updatePreview() {
		String themeName = (String) editorThemeComboBox.getSelectedItem();
		EditorConfig config = EditorConfigFactory.getByName(themeName, true);
		
		boolean isKdb = kdbRadioButton.isSelected();
		String code = isKdb ? kdbExampleCode : duckdbExampleCode;
		String contentType = isKdb ? "text/qsql" : "text/sql";
		ServerDocumentPanel.applyConfigToSyntaxKits(config);
		
		// Remove old preview pane if exists
		previewContainer.removeAll();
		
		// Create a fresh editor pane with the new configuration
		previewPane = new JEditorPane();
		previewPane.setEditable(false);
		
		scrollPane = new JScrollPane(previewPane);
		previewContainer.add(scrollPane, BorderLayout.CENTER);
		
		// Set content type AFTER adding to scroll pane (required for line numbers)
		previewPane.setContentType(contentType);
		previewPane.setText(code);
		previewPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
		
		// Set background color based on theme
		boolean isDark = config.isDark();
		Color bgColor = isDark ? new Color(0x2B2B2B) : Color.WHITE;
		previewPane.setBackground(bgColor);
		scrollPane.getViewport().setBackground(bgColor);
		
		// Move caret to start
		previewPane.setCaretPosition(0);
		
		// Refresh the container
		previewContainer.revalidate();
		previewContainer.repaint();
	}
	
	@Override public void refresh() {
		String selectedThemeName = myPreferences.getCodeEditorTheme();
		editorThemeComboBox.setSelectedItem(selectedThemeName);
		updatePreview();
	}

	@Override void saveSettings() {
	}
		

	/** No live settings changes are made, so nothing needs to be cancelled. */
	@Override 
	void cancel() { }
}
