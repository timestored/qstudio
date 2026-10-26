package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.timestored.theme.Theme;

class PivotForm extends JPanel {
		private final JList selList; 
		private final JComboBox pivotColCB;
		private final JComboBox<String> aggColCB;
		private final JComboBox<String> aggOpCB;
		private PivotFormConfig pivotConfig;
		private boolean imChanging = false;
		private String lastQuery = "";
		
		public PivotForm(Consumer<PivotFormConfig> callBack) {
			this.setLayout(new BorderLayout());
			Box form = Box.createHorizontalBox();
			String[] aggOps = PivotFormConfig.AGG_OPS;
			aggOpCB = new JComboBox(aggOps);
			final int ROW_HEIGHT = Math.max(15, MyPreferences.INSTANCE.getCodeFontSize()*2);
			aggOpCB.setMaximumSize(new Dimension(30, ROW_HEIGHT));
//				form.add(new JLabel("Target Column:"));
			aggColCB = new JComboBox<String>();
			aggColCB.setMaximumSize(new Dimension(280, ROW_HEIGHT));

			selList = new JList();
			selList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			selList.setVisibleRowCount(1);
			pivotColCB = new JComboBox();
			pivotColCB.setMaximumSize(new Dimension(280, ROW_HEIGHT));

			
			JButton resetToTableButton = new JButton(new AbstractAction("", Theme.CIcon.TABLE_ELEMENT.get16()) {
				@Override public void actionPerformed(ActionEvent e) {
					pivotColCB.setSelectedIndex(0);
					selList.clearSelection();
				}
			});

			JButton copyButtonSmall = new JButton(createCopyAction(""));
			JButton copyButton = new JButton(createCopyAction("Copy Query"));

			form.add(resetToTableButton);
			form.add(copyButtonSmall);			
			form.add(new JLabel("Select "));
			form.add(aggOpCB);
			form.add(aggColCB);
			form.add(new JLabel(" By: "));
			form.add(selList);
			form.add(new JLabel(" Pivot On: "));
			form.add(pivotColCB);
			form.setPreferredSize(new Dimension(300, 30));
			form.add(copyButton);
			form.add(Box.createHorizontalGlue());
			form.add(Box.createHorizontalStrut(30));
			
			Runnable sendCallback = () -> {
				if(imChanging) {
					return;
				}
				List<String> groupbylist = getSelectedGroupBys();
				List<String> pivotlist = new ArrayList<>();
				if(pivotColCB.getSelectedIndex() > 0) {
					pivotlist.add((String) pivotColCB.getSelectedItem());
				}
				List<String> agglist = new ArrayList<>();
				if(aggColCB.getSelectedIndex() != -1) {
					agglist.add((String) aggColCB.getSelectedItem());
				}
				try {
					if(pivotConfig != null) {
						String aggOp = (String) aggOpCB.getSelectedItem();
						String aggCol = (String) aggColCB.getSelectedItem();
						PivotFormConfig newPC = pivotConfig.changeSelection(groupbylist, pivotlist, aggOp, aggCol);
						if(!newPC.equals(pivotConfig)) {
							callBack.accept(newPC);
						}
					}
				} catch (SQLException e1) {
					JOptionPane.showMessageDialog(null, "Error changing PulsePivot");
				}
			};
			
			selList.addListSelectionListener(lse -> {
				if(!lse.getValueIsAdjusting()) {
					sendCallback.run();		
				}
			});
			pivotColCB.addActionListener(l -> sendCallback.run());
			aggOpCB.addActionListener(l -> sendCallback.run());
			aggColCB.addActionListener(l -> sendCallback.run());
			this.add(form, BorderLayout.CENTER);
		}
		
		private Action createCopyAction(String text) {
			return new AbstractAction(text, Theme.CIcon.EDIT_COPY.get16()) {
				@Override public void actionPerformed(ActionEvent e) {
					Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
					clpbrd.setContents(new StringSelection(lastQuery), null);
			        putValue(SHORT_DESCRIPTION, "Copy the query used to generate the latest result to the clipboard.");
				}
			};
		}

		public void setDisplay(PivotFormConfig pivotConfig, String lastQuery) {
			imChanging = true;
			this.pivotConfig = pivotConfig;
			this.lastQuery = lastQuery;
			
			aggOpCB.setSelectedItem(pivotConfig.getAggOp());
			aggColCB.setSelectedItem(pivotConfig.getAggCol());
			
			List<String> colNamesList = pivotConfig.getByColsShown();
			String[] colNames = pivotConfig.getByColsShown().toArray(new String[] {});

			// Refresh
			aggColCB.setModel(new DefaultComboBoxModel<>(pivotConfig.getAggCols().toArray(new String[] {})));
			selList.setModel(new DefaultComboBoxModel<>(colNames));
			if(colNames.length > 12) {
				selList.setVisibleRowCount(2);
			}
			
			List<String> bySel = pivotConfig.getByColsSelected();
			int[] indices = new int[bySel.size()];
			for(int i=0; i<bySel.size(); i++) {
				indices[i] = pivotConfig.getByColsShown().indexOf(bySel.get(i));
			}
			if(!isEqual(selList.getSelectedIndices(), indices)) {
				selList.setSelectedIndices(indices);	
			}
			
			ArrayList pivotOptions = new ArrayList<>();
			pivotOptions.add("--NONE--");
			pivotColCB.setEnabled(indices.length > 0);
			if(indices.length > 0) {
				pivotOptions.addAll(colNamesList);
				pivotColCB.setModel(new DefaultComboBoxModel<>(pivotOptions.toArray(new String[] {})));
				if(pivotConfig.getPivotColsSelected().size()>0) {
					String selItem = pivotConfig.getPivotColsSelected().get(0);
					for(int i=0; i<pivotOptions.size(); i++) {
						if(selItem.equals(pivotOptions.get(i))) {
							pivotColCB.setSelectedIndex(i);
							break;
						}
					}
				}
			} else {
				pivotColCB.setModel(new DefaultComboBoxModel<>(pivotOptions.toArray(new String[] {})));
			}
			imChanging = false;
		}
		
		private static boolean isEqual(int[] selectedIndices, int[] indices) {
			if(selectedIndices == null && indices == null) {
				return true;
			} else if(selectedIndices == null || indices == null) {
				return false;
			} else if(selectedIndices.length != indices.length) {
				return false;
			}
			for(int i = 0; i < selectedIndices.length; i++) {
				if(selectedIndices[i] != indices[i]) {
					return false;
				}
			}
			return true;
		}

		List<String> getSelectedGroupBys() {
			int[] selIndices = selList.getSelectedIndices();
			List<String> groupbylist = new ArrayList<>();
			for(int i=0; i<selIndices.length; i++) {
				groupbylist.add((String) selList.getModel().getElementAt(selIndices[i]));
			}
			return groupbylist;
		}
	}