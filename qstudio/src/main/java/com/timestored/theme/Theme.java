package com.timestored.theme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.sql.SQLException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import com.timestored.StringUtils;
import com.timestored.babeldb.DBHelper;
import com.timestored.misc.HtmlUtils;
import com.timestored.qstudio.kdb.KdbHelper;
import com.timestored.sqldash.chart.TableFactory;
import com.timestored.sqldash.chart.TableFactory.EnrichedTableModel;

import kx.c.Flip;
import kx.jdbc.ExtendedResultSet;


/**
 * Contains functions making it easy to construct forms and display tables
 * while also dictating the appearance of the application.
 */
public class Theme {

	// TODO convert this from a static instance to a class and interface
	// so themes can be interchangeable. 
	// keep in mind kdbTrader has such a facility. These should be combined.
	public static final Color SUB_HEADER_FG_COLOR = new Color(242,242,242);
	public static final Color SUB_HEADER_BG_COLOR = new Color(174,185,210);
	public static final Color HIGHLIGHT_BUTTON_COLOR = new Color(14, 99, 156);
	public static final int GAP = 4;
	public static final Border CENTRE_BORDER = BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP);
	
	private static final Color HEADER_FG_COLOR = new Color(252,252,252);
	private static final Color HEADER_BG_COLOR = new Color(154,165,190);
	private static final Border OFFSET_BORDER = BorderFactory.createEmptyBorder(0, 0, GAP, GAP);
	private static final ListCellRenderer LIST_RENDERER = new ActionListCellRenderer();

	private static final InputLabeller INPUT_LABELLER_INSTANCE = new InputLabeller();

	public static enum CIcon implements Icon {
		SERVER_ADD("server_add.png"), 
		SERVER_CHART("server_chart.png"), 
		SERVER_CONNECT("server_connect.png"), 
		SERVER_DELETE("server_delete.png"), 
		SERVER_EDIT("server_edit.png"), 
		SERVER_ERROR("server_error.png"), 
		SERVER_GO("server_go.png"), 
		SERVER_KEY("server_key.png"), 
		SERVER_DATABASE("server_database.png"), 
		SERVER_LIGHTNING("server_lightning.png"), 
		SERVER("server.png"),
		DELETE("delete.png"), 
		DUCK("duck.png"), 
		DUCK_FADED("duck_faded.png"), 
		SAVE("disk.png"), 
		SAVE_AS("disk_multiple.png"),
		ADD_SERVER("red-dot.png"), 
		TREE_ELEMENT("red-dot.png"),
		TABLE_ELEMENT("tbl.gif"),
		VIEW_ELEMENT("view.png"),
		DATE_ELEMENT("typdate.png"),
		CHAR_ELEMENT("typstring.gif"),
		NUMBER_ELEMENT("var.png"),
		LAMBDA_ELEMENT("typlambda.png"),
		FUNCTION_ELEMENT("function.png"),
		JPAD("jpad.png"),
		INFO("info.png"), 
		INFO32("info32.png"), 
		SQLDASH_LOGO("logo.png"),
		EYE("eye.png"),
		QSTUDIO_LOGO("qstudio.png"),
		DOCUMENT_NEW("page_white_add.png"),
		DOCUMENT_OPEN("document-open.png"),
		DOCUMENT_SAVE("document-save.png"), 
		DOCUMENT_SAVE_AS("document-save-as.png"),
		TABLE_ADD("table_add.png"),
		TABLE_DELETE("table_delete.png"),
		CHART_CURVE("chart_curve.png"),
		TAB_GO("tab_go.png"),
		PAGE("page.png"),
		TABLE_MULTIPLE("table_multiple.png"),
		PAGE_EDIT("page_edit.png"),
		PAGE_CODE("page_code.png"),
		PAGE_RED("page_red.png"),
		PAGE_WHITE_ZIP("page_white_zip.png"),
		TABLE_ROW_DELETE("table_row_delete.png"),
		ARROW_REFRESH("arrow_refresh.png"),
		CSV("csv.png"),
		XLSX("xlsx.png"),
		EMAIL_ATTACH("email_attach.png"),
		EDIT_CUT("edit-cut.png"),
		EDIT_COPY("edit-copy.png"),
		EDIT_PASTE("edit-paste.png"),
		EDIT_COMMENT("comment.png"),
		EDIT_FIND("find.png"),
		EDIT_FIND_NEXT("find-next.png"),
		EDIT_GOTO_LINE("goto-line.png"),
		EDIT_UNDO("edit-undo.png"),
		EDIT_REDO("edit-redo.png"),
		TEXT_HTML("text-html.png"),
		COPY("copy.png"),
		FUNC_COL("fncol.png"),
		RENAME("rename.png"),
		ADD("add.png"),
		ACCEPT("accept.png"),
		CANCEL("cancel.png"),
		TERMINAL("utilities-terminal.png"),
		INFORMATION("dialog-information.png"), 
		WARNING("dialog-warning.png"), 
		ERROR("dialog-error.png"),
		SCRIPT_GO("script-go.png"),
		CLOCK_GO("clock-go.png"),
		FOLDER_ADD("folder-add.png"),
		FOLDER_DELETE("folder-delete.png"),
		LAYOUT_DELETE("layout-delete.png"),
		LAYOUT_ADD("layout-add.png"),
		LAYOUT_EDIT("layout-edit.png"),
		DAS_FILE("das-file.png"),
		CHART_CURVE_DELETE("chart-curve-delete.png"),
		CHART_CURVE_ADD("chart-curve-add.png"),
		UP_CLOUD("upcloud.png"),
		POPUP_WINDOW("application-double.png"),
		GREEN_FORWARD("forward_green.png"),
		GREEN_BACK("back_green.png"),
		GREEN_NEXT("next_green.png"),
		GREEN_PLAY("play_green.png"),
		FOLDER("folder.png"),
		DISCONNECT("disconnect.png"),
		USER_WISE_GO("user_wise_go.png"),
		USER_WISE("user_wise.png"),
		AI("ai.png"),
		HAND("hand.png"),
		CONNECT("connect.png"),
		PREFERENCES("interface_preferences_32.png"),
		SET_PASSWORD("set_password.png"),
		GOOGLE("google.png"),
		ROBOT("robot.png"),
		ROBOT_COMMENT("robot_comment.png"),
		ROBOT_GO("robot_go.png"),
		TABLE_PIVOT("summary_table.png"),
		SEARCH("google_custom_search.png"),
		BLUE_PLAY("play_blue.png"),
		MARKDOWN_GREEN("pulse64.png"),
		MARKDOWN_GREY("pulse64red.png"),
		PIN("pin.png"),
		PIN_GREEN("pin-green.png");
		
		private final ImageIcon imageIcon;
		private final ImageIcon imageIcon16;
		private final ImageIcon imageIcon32;

		@Override public ImageIcon get() { return imageIcon; }
		
		
		/* (non-Javadoc)
		 * @see com.timestored.theme.IIcon#getBufferedImage()
		 */
		@Override public BufferedImage getBufferedImage() {
			return IconHelper.getBufferedImage(imageIcon);
		}
		
		@Override public ImageIcon get16() { return imageIcon16; }
		
		@Override public ImageIcon get32() { return imageIcon32; }
		
		CIcon(String loc) {
			ImageIcon[] icons = IconHelper.getDiffSizesOfIcon(CIcon.class.getResource(loc));
			imageIcon = icons[0];
			imageIcon16 = icons[1];
			imageIcon32 = icons[2];
		}
	}
	
	private static Font getHeaderFont(Font curFont) {
		return new Font(curFont.getName(), Font.BOLD, 
				curFont.getSize() + 7);
	}

	private static Font getSubHeaderFont(Font curFont) {
		return new Font(curFont.getName(), Font.BOLD, 
				curFont.getSize() + 3);
	}

	public static JPanel getHeader(final String title) {
		
		JPanel outPanel = new JPanel(new BorderLayout());
		outPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, (int)(GAP*1.5), 0));
		JPanel headerPanel = new JPanel(new BorderLayout());
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(Theme.getHeaderFont(titleLabel.getFont()));
		titleLabel.setForeground(HEADER_FG_COLOR);
		titleLabel.setBorder(CENTRE_BORDER);
		headerPanel.add(titleLabel, BorderLayout.CENTER);
		headerPanel.setBackground(HEADER_BG_COLOR);
		outPanel.add(headerPanel, BorderLayout.NORTH);
		return outPanel;
	}
	
	public static JPanel getPlainReadonlyTable(TableModel tableModel) {
		JXTable table = new JXTable(tableModel);
		table.getTableHeader().setReorderingAllowed(false);
		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
		tablePanel.add(table, BorderLayout.CENTER);
		tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		tablePanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

		JPanel tContainerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		tContainerPanel.add(tablePanel);
		TableFactory.setPresentation(table, false);
		table.packAll();
		return tContainerPanel;
	}

	public static JXTable getStripedTable(TableModel tableModel) {
		final JXTable table = new JXTable(tableModel) {

	        @Override public String getToolTipText(MouseEvent e) {
	        	 Point p = e.getPoint();
	             int viewRow = rowAtPoint(p);
	             int viewCol = columnAtPoint(p);
	             if (viewRow >= 0 && viewCol >= 0) {
	                 int modelRow = convertRowIndexToModel(viewRow);
	                 int modelCol = convertColumnIndexToModel(viewCol);
	                 TableModel model = getModel();
					 Object value = model .getValueAt(modelRow, modelCol);
	                 if(value != null) {
	                	 String vs = value.toString();
	                	 String s = null;

	                	 if(model instanceof EnrichedTableModel) {
							 EnrichedTableModel etm = (EnrichedTableModel) model;
							 ExtendedResultSet rs = etm.getNestedResultSetAt(modelRow, modelCol);
							 if(rs != null) {
								 try {
									 s = DBHelper.toString(rs, false);
									 s = "Double-click to dive into:\n" + s;
								 } catch (SQLException e1) {}
                		 	}
	                	 } 

	                	 if(s == null && vs.length() > 100) {
	                		 try {
                				 s = vs instanceof String ? vs : KdbHelper.asText(value);
	                		 } catch(RuntimeException re) {
	                		 }
	                	 }
	                	 if(s != null) {
	                		return StringUtils.abbreviateMultiline(s);
	                	 }
	                 }
	                 return null;
	             }
	             return super.getToolTipText(e);
	        }
		};
		JTableHeader anHeader = table.getTableHeader();
		anHeader.setForeground(Color.BLACK);
		anHeader.setBackground(Color.GRAY);
		table.setHighlighters(HighlighterFactory.createSimpleStriping());
		table.setEditable(false);
		table.setName("stripedTable");
		return table;
	}

	public static JPanel getSubHeader(final String title) {
		return getSubHeader(title, SUB_HEADER_FG_COLOR, SUB_HEADER_BG_COLOR);
	}

	public static JPanel getHorizontalRule() {
		JPanel outPanel = new JPanel(new BorderLayout());
		outPanel.setBorder(BorderFactory.createEmptyBorder(GAP/3, 0, GAP/3, 0));
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.add(new JLabel(" "));
		headerPanel.setBackground(SUB_HEADER_BG_COLOR);
		outPanel.add(headerPanel, BorderLayout.NORTH);
		return outPanel;
	}

	public static JPanel getSubHeader(final String title, final Color foregroundColor, 
			final Color backgroundColor) {
		
		JPanel outPanel = new JPanel(new BorderLayout());
		outPanel.setBorder(BorderFactory.createEmptyBorder(GAP, 0, (int)(GAP*1.5), 0));
		JPanel headerPanel = new JPanel(new BorderLayout());
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(Theme.getSubHeaderFont(titleLabel.getFont()));
		titleLabel.setForeground(foregroundColor);
		titleLabel.setBorder(CENTRE_BORDER);
		headerPanel.add(titleLabel, BorderLayout.CENTER);
		headerPanel.setBackground(backgroundColor);
		outPanel.add(headerPanel, BorderLayout.NORTH);
		return outPanel;
	}
	
	public static int getGap() {
		return GAP;
	}
	
	public static Border getCentreBorder() {
		return CENTRE_BORDER;
	}
	
	public static Border getOffsetBorder() {
		return OFFSET_BORDER;
	}

	public static JPanel getVerticalBoxPanel() {
		return new BoxyPanel();
	}
	
	/**
	 * Wrap a centerPanel and display as a table with title in a standard way. 
	 */
	public static JPanel wrap(String title, Component centerPanel, String text) {
		JTextArea description = null;
		if(text!=null && text.length()>0) {
			description = new JTextArea(text);
			description.setLineWrap(true);
		}
		return wrap(title, centerPanel, description);
	}

	/**
	 * Wrap a centerPanel and display as a table with title in a standard way. 
	 */
	public static JPanel wrap(String title, Component centerPanel) {
		return wrap(title, centerPanel, (Component) null);
	}
	
	/**
	 * Wrap a centerPanel and display as a table with title in a standard way. 
	 * @param description A component that describes the content.
	 */
	public static JPanel wrap(String title, Component centerPanel, Component description) {
		JPanel containerPanel = new JPanel(new BorderLayout());
		JPanel headerPanel = Theme.getSubHeader(title);
		containerPanel.add(headerPanel, BorderLayout.NORTH);
		if(centerPanel != null) {
			containerPanel.add(centerPanel, BorderLayout.CENTER);
		}

		if(description!=null) {
			containerPanel.add(description, BorderLayout.SOUTH);
		}
		return containerPanel;
	}

	public static class BoxyPanel extends JPanel {
		
		public BoxyPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		}
		
		@Override public Component add(Component c) {
			super.add(c).setMaximumSize(new Dimension(Integer.MAX_VALUE, 
					(int) c.getPreferredSize().getHeight()));
			super.add(Box.createVerticalStrut(GAP*4));
	    	return c;
		}
	}

	/**
	 * @param html Te html you want shown (can include hyperlinks).
	 * @return html text area with hyperlinks that actually open the systems browser on click.
	 */
	public static JEditorPane getHtmlText(String html) {
		JEditorPane ep = new JEditorPane("text/html", html);
	    // handle link events
	    ep.addHyperlinkListener(new HyperlinkListener()
	    {
	        @Override public void hyperlinkUpdate(HyperlinkEvent e) {
	            if (e != null && e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
	            	if(e.getURL() != null) {
	            		HtmlUtils.browse(e.getURL().toString());
	            	}
	            }
	        }
	    });
	    ep.setEditable(false);
	    ep.setBackground(new JLabel().getBackground());
		return ep;
	}

	/** @return uneditable transparent line-wrapped text area. */
	public static JTextArea getTextArea(String name, String txt) {
		JTextArea ta = new JTextArea(txt);
		ta.setWrapStyleWord(true);
		ta.setLineWrap(true);
		ta.setOpaque(false);
		ta.setEditable(false);
		return ta;
	}

	/** Get an InputLabeller that simplifies correctly labelling input components */
	public static InputLabeller getInputLabeller(int labelWidth, int labelHeight) {
		return new InputLabeller(labelWidth, labelHeight);
	}

	/** Get an InputLabeller that simplifies correctly labelling input components */
	public static InputLabeller getInputLabeller() {
		return INPUT_LABELLER_INSTANCE;
	}
	

	public static Box getFormRow(JComponent c, String label, String tooltip) {
		return getFormRow(c, label, tooltip, null);
	}
	
	public static Box getFormRow(JComponent c, String label, String tooltip, JComponent rowEnd) {
		JLabel l = new JLabel(label);
		if(tooltip!=null) {
			c.setToolTipText(tooltip);
			l.setToolTipText(tooltip);
		}
		l.setLabelFor(c);
		
		Box b = Box.createHorizontalBox();
		b.add(l);
		b.add(c);
		if(rowEnd!=null) {
			b.add(rowEnd);	
		}
		b.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
		return b;
	}
	
	/**
	 * Standardises good practices of labelling and naming components.
	 * Optionally displays help or making labels all fixed widths.
	 */
	public static class InputLabeller {
		
		private final Dimension labelDimension;
		
		private InputLabeller(int labelWidth, int labelHeight) {
			labelDimension = new Dimension(labelWidth, labelHeight);
		};

		private InputLabeller() {
			labelDimension = null;
		}

		
		
		public JPanel get(String labelText, Component inputComp, String inputName) {
			return get(labelText, inputComp, inputName, null, null);
		}

		/**
		 * @param inputName The name set for the inputComponent useul for testing.
		 * @return panel containing a left-hand label, for the given named input component
		 * and optionally to the right show a help component.
		 */
		public JPanel get(String labelText, Component inputComp, 
				String inputName, Component helpComponent, String lblTooltip) {
			
			JPanel p = new JPanel();
			FlowLayout flowLayout = (FlowLayout) p.getLayout();
			flowLayout.setAlignment(FlowLayout.LEFT);
			
			JLabel label = new JLabel(labelText);
			if(lblTooltip != null) {
				label.setToolTipText(lblTooltip);
			}
			if(labelDimension!=null) {
				label.setPreferredSize(labelDimension);
			}
			label.setLabelFor(inputComp);
			inputComp.setName(inputName);
			
			p.add(label);
			p.add(inputComp);
			if(helpComponent != null) {
				p.add(helpComponent);
			}
			
			return p;
		}


		public JPanel get(String labelText, Component inputComp, 
				String inputName, Component helpComponent) {
			return get(labelText, inputComp, inputName, helpComponent, null);
		}
		public JPanel get(String labelText, Component inputComp, 
				String inputName, String toolltip) {
			return get(labelText, inputComp, inputName, null, toolltip);
		}
	}

	/**
	 * @return renderer that uses action name and icon to display it in a list.
	 */
	public static ListCellRenderer getActionListCellRenderer() {
		return LIST_RENDERER;
	}
	

	private static class ActionListCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override public Component getListCellRendererComponent(JList list, Object value, 
				int index, boolean isSelected, boolean cellHasFocus) {

			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, 
					isSelected, cellHasFocus);

			if (value instanceof Action) {
				Action a = (Action) value;
				label.setText((String) a.getValue(Action.NAME));
				label.setIcon((javax.swing.Icon) a.getValue(Action.SMALL_ICON));
				return label;
			}
			return label;
		}

	}	
	
	public static Box getErrorBox(String header, Component... components) {
		Box box = Box.createVerticalBox();
		box.add(Theme.getSubHeader(header, Color.RED, Color.PINK));
		for(Component c : components) {
			box.add(c);
		}
		return box;
	}


	public static JButton makeButton(String text, ActionListener actionListener) {
		JButton btn = new JButton(text);
		btn.setBorder(new EmptyBorder(6, 15, 6, 15));
		btn.setName(text + "Button");
		btn.addActionListener(actionListener);
		return btn;
	}

	/**
	 * Show a textArea entry dialog and if the user selects ok return the text within the textArea otherwise return null.
	 * @param helpMsg Text displayed above the text area to inform the user.
	 * @param defaultText The default option, that also controls whether null is returned
	 */
	public static String getTextFromDialog(Component parent, String title, 
			final String defaultText, String helpMsg) {
		
		JPanel p = new JPanel(new BorderLayout());
		final JTextArea textArea = new JTextArea(defaultText);
		textArea.selectAll();
		JScrollPane scrPane = new JScrollPane(textArea);
		scrPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		p.add(Theme.getHtmlText(helpMsg), BorderLayout.NORTH);
		
		
		p.add(scrPane, BorderLayout.CENTER);
		String[] options = new String[] {"ok","cancel" };
		JOptionPane optPane = new JOptionPane(p, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.YES_NO_OPTION,  null, options, options[0] );
		JDialog d = optPane.createDialog(parent, title );
		d.setSize(new Dimension(560, 400));
		d.setPreferredSize(new Dimension(560, 400));
		d.pack();
		
		// workaround to ensure textarea gets focus
		d.addWindowListener(new WindowAdapter() {
			@Override public void windowActivated(WindowEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
						textArea.requestFocus();
						textArea.requestFocusInWindow();
					}
				});
			}
		});
		d.setVisible(true);
		
		String txt = null;
		if(options[0].equals(optPane.getValue())) {
			txt = textArea.getText();
		}
		return txt;
	}
}
