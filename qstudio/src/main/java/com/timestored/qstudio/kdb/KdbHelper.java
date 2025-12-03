package com.timestored.qstudio.kdb;

import java.awt.Component;
import java.awt.Font;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import kx.jdbc;
import kx.c.Dict;
import kx.c.Flip;
import lombok.Setter;

import com.timestored.sqldash.chart.TableFactory;
import com.timestored.sqldash.chart.TimeStringValuer;
import com.timestored.swingxx.JTreeHelper;
import com.timestored.theme.Theme;

import jsyntaxpane.DefaultSyntaxKit;

/**
 * Contains functions useful for handling KDb data structures and presenting them.
 */
public class KdbHelper {

	private static final Logger LOG = Logger.getLogger(KdbHelper.class.getName());
	private static final NumberFormat NUM_FORMAT;
	public static final int DEFAULT_MAX_FRACTION_DIGITS = 7;
	public static final int DEFAULT_GROUPING_SIZE= 0;
	private static int decimalPlaces = DEFAULT_MAX_FRACTION_DIGITS;
	private static String formatString = "%." + decimalPlaces + "f";
	@Setter private static boolean negativeShownRed = true;
	private static final TimeStringValuer TABLE_STRINGER = new TimeStringValuer(l -> format(l, true), null);
	private static final TimeStringValuer VAL_STRINGER = new TimeStringValuer(l -> format(l, false), "yyyy.MM.dd");
	
	static {
		NUM_FORMAT = NumberFormat.getInstance();
		setMaximumFractionDigits(decimalPlaces);
		setGroupingSize(DEFAULT_GROUPING_SIZE);
		setNegativeShownRed(true);
	}

	public static void setGroupingSize(int groupingSize) {
		 NUM_FORMAT.setGroupingUsed(groupingSize > 0);
		 if (NUM_FORMAT instanceof DecimalFormat) {
			 ((DecimalFormat) NUM_FORMAT).setGroupingSize(groupingSize);
		 }
	}
	/**
	 * @param decimalPlaces the maximum number of fraction digits to be shown; 
	 * if less than zero, then zero is used.
	 */
	public static void setMaximumFractionDigits(int decimalPlaces) {
		if(decimalPlaces < 0) {
			decimalPlaces = 0;
		}
		 if (NUM_FORMAT instanceof DecimalFormat) {
			 NUM_FORMAT.setMaximumFractionDigits(decimalPlaces);
		 }
		 KdbHelper.decimalPlaces = decimalPlaces;
		 formatString = "%." + decimalPlaces + "f";
	}
	
	/**
	 * Flatten any KDB object into a single line, output similar to -3! in KDB. 
	 * But types will always be shown and tables will be shown differently.
	 * @param k a KDB result object
	 * @return String representation of the array or null if it could not convert.
	 */
	public static String asLine(Object k) {
		return asText(k, false, true);
	}
	

	/**
	 * Convert a kdb object to a string for display in a console etc.
	 * @param k a KDB result object
	 * @return String representation of the array or null if it could not convert.
	 */
	public static String asText(Object k) {
		return asText(k, false, false);
	}
	
	/**
	 * Convert a kdb object to a string for display in a console etc.
	 * @param k a KDB result object
	 * @return String representation of the array or null if it could not convert.
	 * @param singleLine Controls whether we spread over multiple lines and try to imitate kdb exactly.
	 */
	public static String asText(Object k, boolean forTable, boolean singleLine) {
		String s = null;
		try {
			if(k != null) {
				if(k instanceof Flip || k instanceof Dict) { // unkeyed table
					s = k.toString();
				} else {
					s = vs(k, forTable, singleLine);	
				}
			} else {
				s = forTable ? "" : "::";
			}
		} catch(ClassCastException | IllegalArgumentException e) {
			// fall through
		}
		if(s == null) {
			return s;
		}
		return s + (singleLine ? "" : "\r\n");
	}	
	
	/**
	 * Flatten any KDB object into a single line, output similar to -3! in KDB. 
	 * But types will always be shown and tables will be shown differently.
	 * @param k a KDB result object
	 * @param forTable when displaying values in a table, nulls are shown as
	 * 		blanks and single items do not have their type shown.
	 * @return String representation of the array or null if it could not convert.
	 */
	public static String asLine(Object k, boolean forTable) {
		return asText(k, forTable, true);
	}

	
	private static String flatten(Object[] a, final String emptySt, final String postfix) {
		if(a.length==0) {
			return emptySt;
		} else if(a.length==1) {
			return TimeStringValuer.SINGLE_ITEM_LIST_PREFIX +vs(a[0]);
		}
		StringBuilder s = new StringBuilder(a.length*3);
		s.append("(" + vs(a[0]));
		for (int i = 1; i < Math.min(a.length, TimeStringValuer.MAX_ARRAY_ITEMS_SHOWN); i++) {
			s.append(";").append(vs(a[i]));
		}
		if(a.length>TimeStringValuer.MAX_ARRAY_ITEMS_SHOWN) {
			s.append("...");
		}
		return s.append(")").toString();
	}


	/**
	 * Convert a (possibly nested) list to a string. 
	 */
	private static String vs(Object k) {
		return vs(k, false, true);
	}

	/**
	 * Convert a (possibly nested) list to a string. 
	 * @param forTable when displaying values in a table, nulls are shown as
	 * 		blanks and single items do not have their type shown.
	 * @param singleLine Controls whether we spread over multiple lines and try to imitate kdb exactly.
	 */
	private static String vs(Object k, boolean forTable, boolean singleLine) {
		// recursively flatten each nested object by callingourself
		String li = "";
		TimeStringValuer conv = forTable ? TABLE_STRINGER : VAL_STRINGER;
		
		if(k == null && !forTable) {
			li = "::";
		} else if (k instanceof String[]) {
			li = conv.flatten((String[]) k, "`symbol$()", "", "`");
		} else if (k instanceof Object[]) {
			li = flatten((Object[]) k, "", "");
		} else if(k instanceof int[]) {
			li = conv.flatten((int[]) k, "`int$()", "i");
		} else if(k instanceof long[]) {
			li = conv.flatten((long[]) k, "`long$()", "");
		} else if(k instanceof double[]) {
			li = conv.flatten((double[]) k, "`float$()", "");
		} else if(k instanceof float[]) {
			li = conv.flatten((float[]) k, "`real$()", "e");
		} else if(k instanceof short[]) {
			li = conv.flatten((short[]) k, "`short$()", "h");
		} else if(k instanceof boolean[]) {
			li = conv.flatten((boolean[]) k, "`boolean$()", "b");
		} else if(k instanceof Boolean) {
			li = ((boolean)k) ? "1b" : "0b";
		} else if(k instanceof byte[]) {
			li = conv.flatten((byte[]) k, "`byte$()", "", "0x");
		} else if(k instanceof Character) {
			li = forTable ? k.toString() : "\"" + k.toString() + "\"";
		} else if(k instanceof char[]) {
			li = new String((char[])k);
			if(singleLine) {
				li = li.replace("\r", "\\r").replace("\n", "\\n");
			}
			if(!forTable) {
				li = "\"" + li + "\"";
			}
		} else if(k instanceof String){
			li = forTable ? k.toString() : "`" + k;	
		} else if(k instanceof Dict || k instanceof Flip){
			li = k.toString(); 
		} else if(k!=null) {
			KdbType kt = KdbType.getType(k.getClass());
			if(kt != null) {
				char cc = kt.getCharacterCode();
				li = conv.getString(k);
				boolean hideTypeChar = forTable || cc == 'j' || cc == 'x' || (cc=='f' && (li.contains(".") || li.contains("n") || li.contains("w")));
				li += (hideTypeChar ? "" : kt.getCharacterCode());
			} else if(k!= null) {
				li = conv.getString(k);
			}
		}
		
		return li;
	}

	/** 
	 * @return a topmost count of a KDB object, or -1 if unknown
	 */
	public static int count(Object k) {
		if(k == null) {
			return 0;
		}
		try {
			if(k.getClass().isArray()) {
				return Array.getLength(k);
			} else if(k instanceof Flip) {
				Object vals = ((Flip)k).y[0];
				if(vals.getClass().isArray()) {
					return Array.getLength(vals);
				} else {
					 return ((Flip)k).x.length;
				}
			} else if(k instanceof Dict) {
				Dict d = (Dict) k;
				if (d.x instanceof Flip) {
					Object vals = ((Flip) d.x).y[0];
					if (vals.getClass().isArray()) {
						return Array.getLength(vals);
					} else {
						return ((Flip) k).x.length;
					}
				} else {
					if(d.x.getClass().isArray()) {
						return Array.getLength(d.x);
					}
				}
			}
		} catch(Exception e) {
			// fall through to return -1
		}
		return 0;
	}
	

	
	/** For a KDB type number return the character for that type */
	public static char getTypeChar(int type) {
		final String typec = " b gxhijefcspmdznuvt";
		int t = Math.abs(type);
		if(t<typec.length()) {
			return typec.charAt(t);
		}
		return '?';
	}
	
	/**
	 * Take qCode and escape it so that it could be value'd.
	 * new lines are replaced with \r\n, tabs with \t etc.
	 */
	public static String escape(String qCode) {
		return qCode.replace("\\", "\\\\").replace("\t", "\\t")
				.replace("\r", "\\r").replace("\n", "\\n")
				.replace("\"", "\\\"");
	}

	private static String format(Object o, boolean forTable) {
		
		// null symbol ` return nothing
		if(o == null || ((o instanceof String) && o.equals(""))) {
			return "";
		}
		boolean isFloat = (o instanceof Float) || (o instanceof Double);
		if(KdbType.isNull(o)) {
			if(forTable) {
				return "";
			}
			return isFloat ? "0n" : "0N";
		} else if(KdbType.isPositiveInfinity(o)) {
			return isFloat ? "0w" : "0W";
		} else if(KdbType.isNegativeInfinity(o)) {
			return isFloat ? "-0w" : "-0W";
		} else if(o instanceof Double) {
			return formatFloatingPt((Double) o, forTable);
		} else if(o instanceof Float) {
			return formatFloatingPt((Float) o, forTable);
		} else if(o instanceof Byte) {
			return "0x" + String.format("%02x", (Byte)o).toLowerCase();
		}

		return null;
	}

	public static String formatFloatingPt(double d) {
		return formatFloatingPt(d, false);
	}
	
	private static String formatFloatingPt(double d, boolean forTable) {
		if(forTable) {
			return NUM_FORMAT.format(d);
		} else {
			String tmp = String.format(formatString , d);
			return TimeStringValuer.trimTrailingPointZeroes(tmp);
		}
		
	}
	
	
	/**
	 * Given a kdb k object display it in the best way possible.
	 * @param k A k result object
	 * @return A panel displaying the result
	 */
	public static Component getComponent(Object k) {
		return getComponent(k, Integer.MAX_VALUE);
}
	
	/**
	 * Given a kdb k object display it in the best way possible.
	 * @param k A k result object
	 * @param maxRowsShown maximum number of rows to show if returning a table,
	 * 			0 means show all possible.
	 * @return A panel displaying the result
	 */
	public static Component getComponent(Object k, int maxRowsShown) {
		Component res = new JPanel();
		if(k == null) {
			return res;
		}
		
		// fall through, try drawing as table->tree->string line
		if(maxRowsShown > 0) {
			res =  getJXTableFromKdb(k, maxRowsShown);
		} else {
			res =  getJXTable(k);
		}
		
		// no table? IS it a function, else just show text.
		if(res == null) {
			char[] ck = k instanceof char[] ? (char[])k : new char[] {};
			// identify functions and show in a code editor.
			if(ck.length>=2 && ck[0] == '{' && (ck[ck.length-1]=='}' || ck[ck.length-1]==']')) {
				String code = new String(ck);
				DefaultSyntaxKit.initKit();
				JEditorPane codeEditor = new JEditorPane();
		        JScrollPane scrPane = new JScrollPane(codeEditor);
//			        scrPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		        codeEditor.setContentType("text/qsql");
				codeEditor.setText(code);
				codeEditor.setEditable(false);
				codeEditor.setFont(UIManager.getFont("defaultFont"));
				return scrPane;
			} else {
				String txt = KdbHelper.asText(k);
				if(txt != null) {
					return new JScrollPane(Theme.getTextArea("flat-kdb-res", txt));
				}
			}
		}

		if(k.getClass().isArray()) { // enchance by also showing list.
			JTabbedPane tabbedPane = new JTabbedPane();
			if(res != null) {
				tabbedPane.add("Table", res);	
			}
			if(isMixedList(k)) {
				tabbedPane.add("Tree", getTree(k, maxRowsShown));
			}
			JScrollPane txtPanel = new JScrollPane(Theme.getTextArea("flat-kdb-res", KdbHelper.asText(k)));
			tabbedPane.add("Text", txtPanel);
			res = tabbedPane;
		}
		return res;
	}

	public static Component getJXTable(Object kObject) {
		return getJXTableFromKdb(kObject, Integer.MAX_VALUE);
	}
	
	public static Component getJXTableFromKdb(Object kObject, int maxRowsShown) {
		try {
			return TableFactory.getTable(jdbc.getRS(kObject), maxRowsShown, negativeShownRed);
		} catch(Exception e) {
			LOG.log(Level.WARNING, "problem creating table", e);
		}
		return null;
	}
	
	/**
	 * Represent the kdb object k in a jtree if possible/sensible.
	 * @param k A k result object
	 * @param maxRowsShown maximum number of rows to show if returning a table,
	 * 			0 means show all possible.
	 * @return JTree representation of kdb object is sensible otherwise null.
	 */
	private static Component getTree(Object k, int maxRowsShown) {

		Component c = null;

		try {
			JTree tree = new JTree(getBranch(k, maxRowsShown, -1));
			final Font currentFont = tree.getFont();
			final Font bigFont = new Font(currentFont.getName(), currentFont.getStyle(), currentFont.getSize() + 1);
			tree.setFont(bigFont);
			JTreeHelper.expandAll(tree, true);
	        tree.setCellRenderer(new CustomNodeRenderer());   
	        tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			c = new JScrollPane(tree);
		} catch (IllegalArgumentException iae) {
			LOG.log(Level.FINE, "getTree - wasn't an array", iae);
		} catch(Exception e) {
			LOG.log(Level.WARNING, "getTree - bigger problem", e);
		}

		return c;
	}

	/**
	 * @param maxRowsShown The maximum number of items to show in any one branch.
	 * @param myIndex -1 if this is a root, otherwise the index label to show for this node.
	 * @return A TreeNode that represents a particular KDB object.
	 */
	private static DefaultMutableTreeNode getBranch(Object k, int maxRowsShown, int myIndex) {
		
		DefaultMutableTreeNode root;
		if(k instanceof String[] || k instanceof Object[]) {
			int len = Array.getLength(k); 
			int l = Math.min(maxRowsShown, len); 
			
			if(myIndex >= 0) {
				root = new DefaultMutableTreeNode("[" + myIndex + "]");
			} else {
				root = new DefaultMutableTreeNode(len + " items");
			}
			for (int i = 0; i < l; i++) {
				Object nk = Array.get(k, i);
				if(nk instanceof String[] || nk instanceof Object[]) {
					root.add(getBranch(nk, maxRowsShown, i));
				} else {
					root.add(new CustomNode(Array.get(k, i), i));
				}
			}
			if(len > l) {
				root.add(new DefaultMutableTreeNode("..."));
			}
		} else {
			root = new CustomNode(k);
		}
		return root;
		
	}

	/** Represents one part of a kdb object in a jtree */
	private static class CustomNode extends DefaultMutableTreeNode {

		public final Object k;
		public final int i;
		private ImageIcon icon;
		
		public CustomNode(Object k, int i) {
			this.k = k;
			this.i = i;
			CAtomTypes type = CAtomTypes.getTypeOfJavaObject(k);
			if(type!=null) {
				icon = type.getIcon().get();
			}
		}

		public CustomNode(Object k) {
			this(k, -1);
		}

		public String getText() {
			return (i>=0 ? "[" + i + "] = " : "") + KdbHelper.asLine(k);
		}

		public String getTooltip() {
			return ""+KdbHelper.count(k);
		}

		public Icon getIcon() {
			return icon;
		}
	}

	
	/** Checks if node is actual a {@link CustomNode} and if so displays appropriately  */
	private static class CustomNodeRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = 1L;

		public CustomNodeRenderer() { }

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

			super.getTreeCellRendererComponent(tree, value, sel, expanded,
					leaf, row, hasFocus);
			
			setToolTipText(null); // no tool tip
			if (value instanceof CustomNode) {
				CustomNode cn = (CustomNode) value;
				setText(cn.getText());
				setToolTipText(cn.getTooltip());
				if(cn.getIcon()!=null) {
					setIcon(cn.getIcon());
				}
			}

			return this;
		}
	}

		
	private static boolean isMixedList(Object k) {
		if(k instanceof Object[]) {
			return k.getClass().getComponentType().equals(java.lang.Object.class);
		}
		return false;
	}
}

