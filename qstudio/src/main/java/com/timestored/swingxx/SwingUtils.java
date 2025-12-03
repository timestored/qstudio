package com.timestored.swingxx;

import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;

import com.google.common.base.Preconditions;
import com.timestored.StringUtils;
import com.timestored.messages.Msg;
import com.timestored.messages.Msg.Key;
import com.timestored.misc.IOUtils;
import com.timestored.theme.Theme;

/**  A misc collection og Swing Functions. */
public class SwingUtils {

	public static final KeyStroke ESC_KEYSTROKE = getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final Logger LOG = Logger.getLogger(SwingUtils.class.getName());

	/** Add a listener that when the user presses escape will close the dialog. */
	public static void addEscapeCloseListener(final JDialog dialog) {
		putEscapeAction(dialog.getRootPane(), new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) {
			    WindowEvent wev = new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING);
			    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
			}
		});
		
		dialog.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				    WindowEvent wev = new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING);
				    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
				}
				super.keyPressed(e);
			}
		});
	}

	public static Component verticalScrollPane(Component c) {
		return new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}
	
	/** Forcefully bring to front **/
	public static void forceToFront(JFrame frame) {
		frame.setState(Frame.NORMAL); 
		frame.toFront();
		frame.setAlwaysOnTop(true);
		frame.setAlwaysOnTop(false);
		frame.repaint();
	}
	
	public static JFrame getPopupFrame(Component parent, String title, 
			Component content, Image icon) {
		
		JFrame f = new JFrame(StringUtils.abbreviate(title, 80));
		f.setIconImage(icon);
		f.setLayout(new BorderLayout());
		f.add(content);
		setSensibleDimensions(parent, f);
		return f;
	}


	/**
	 * Display a dialog containing the selected panel sized to fit the screen appropriately.
	 * @param parent The component that this should be centered near to.
	 * @param title The title of the dialog.
	 * @param contentPanel The content shown in the dialog.
	 */
	public static void showAppDialog(Component parent, String title, 
			JPanel contentPanel, Image icon) {
		
		JDialog d = new JDialog();

        SwingUtils.addEscapeCloseListener(d);
		
		d.setIconImage(icon);
		d.setTitle(title);
		d.setModalityType(ModalityType.APPLICATION_MODAL);
		d.setLayout(new BorderLayout());
		d.add(contentPanel);
		setSensibleDimensions(parent, d);

//		contentPanel.setPreferredSize(new Dimension(defWidth, defHeight));
		d.setLocationRelativeTo(parent);
//		d.pack();
		d.setVisible(true);
	}

	public static void setSensibleDimensions(Component parent, Window w, int defWidth, int defHeight) {
		Toolkit tk = Toolkit.getDefaultToolkit();
		if(parent != null) {
			Insets si = tk.getScreenInsets(parent.getGraphicsConfiguration());
			defWidth = (int) Math.max(700, (tk.getScreenSize().width - si.left - si.right)*3.0/5);
	        defHeight = (int) ((tk.getScreenSize().height - si.top - si.bottom)*4.0/5);
		}
		w.setSize(defWidth, defHeight);
	}
	public static void setSensibleDimensions(Component parent, Window w) {
		setSensibleDimensions(parent, w, 600, 400);
	}

	/**
	 * Add an action that is added to the inputmap/action map for this component
	 * when the escape key is pressed.
	 */
	private static void putEscapeAction(JComponent com, Action action) {
		ActionMap am = com.getActionMap();
		InputMap im = com.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		am.put("escapeAction", action);
		im.put(ESC_KEYSTROKE, "escapeAction");
	}

	/**
	 * Wraps JOptionPane to make sure very long messages are line wrapped or trimmed
	 */
	public static void showMessageDialog(Component parentComponent,
			String message, String title, int messageType) {

		String[] st = message.split("\r");
		int lineCount = st.length;
		
		int maxLineLength = 0;
		for(String r : st) {
			if(r.length() > maxLineLength) {
				maxLineLength = r.length();
			}
		}
		
		// Use HTML to Wrap lines, if any line is longer than 100 chars
		String style = maxLineLength > 100 ?  " style='width: 600px;'" : "";
		Object msg = "<html><body><p" + style + ">" + message + "</body></html>";
		
		
		// If it's going to create a massive dialog, place message in scoller 
		if (lineCount > 15 || message.length() > 800) {
			// create a JTextArea
			JTextArea textArea = new JTextArea(15, 70);
			textArea.setText(message);
			textArea.setEditable(false);
			msg = new JScrollPane(textArea);
		}

		JOptionPane.showMessageDialog(parentComponent, msg, title, messageType);
	}
	

	/**
	 * Show an option dialog and offer two options that allow opening a file if optionOpen is chosen.
	 */
	public static void offerToOpenFile(String message, File file, String optionOpen, String optionClose) {

		String[] options = new String[] { optionOpen, optionClose };
		int option = JOptionPane.showOptionDialog(null, message, "Open File?", 
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.INFORMATION_MESSAGE, 
				Theme.CIcon.TEXT_HTML.get32(), 
				options, options[0]);
		if(option==JOptionPane.OK_OPTION) {
			try {
				Desktop.getDesktop().open(file);
			} catch (IOException e) { 
				JOptionPane.showMessageDialog(null, "Problem opening output file, " +
						"try browsing to folder manually and opening there");
			}
		}
	}
	

	
	/**
	 * @param filetypeExtension The extension used when saving the file (or null if any allowed).
	 * @param fileOrFolder The location first suggested to the user for saving to or null.
	 * @return The location chosen by the user for saving to or null if cancelled.
	 */
	public static File askUserSaveLocation(File fileOrFolder, String... filetypeExtension) {
		return askUserSaveLocation(fileOrFolder, null, filetypeExtension);
	}
	
	/**
	 * @param filetypeExtension The extension used when saving the file (or null if any allowed).
	 * @param fileOrFolder The location first suggested to the user for saving to or null.
	 * @return The location chosen by the user for saving to or null if cancelled.
	 */
	public static File askUserSaveLocation(File fileOrFolder, JComponent newAccessory, String... filetypeExtension) {
		
		JFileChooser fc = null;
		if(fileOrFolder != null && fileOrFolder.isDirectory()) {
			fc = new JFileChooser(fileOrFolder);
		} else {
			fc = new JFileChooser();
		}
		
		if(fileOrFolder != null && fileOrFolder.isFile()) {
			fc.setSelectedFile(fileOrFolder);
		}
		boolean hasFileTypes = filetypeExtension != null && filetypeExtension.length > 0;
		
		if(hasFileTypes) {
			if(filetypeExtension[0].trim().length() > 0) {
				FileNameExtensionFilter firstFilter = new FileNameExtensionFilter(filetypeExtension[0], filetypeExtension[0]);
				fc.setFileFilter(firstFilter);
				fc.addChoosableFileFilter(firstFilter);
			}
			for(int i=1; i<filetypeExtension.length; i++) {
				fc.addChoosableFileFilter(new FileNameExtensionFilter(filetypeExtension[i], filetypeExtension[i]));
			}
		}
		fc.setApproveButtonText(Msg.get(Key.SAVE));
		fc.setDialogTitle(Msg.get(Key.SAVE_FILE));
		if(newAccessory != null) {
			fc.setAccessory(newAccessory);
		}
		
        if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        	File f = fc.getSelectedFile();
        	if(hasFileTypes && !f.getName().contains(".") && fc.getFileFilter()!=null) {
        		f = new File(f.getAbsolutePath() + "." + fc.getFileFilter().getDescription());
        	}
    		return f;
        } else {
        	LOG.info(Msg.get(Key.SAVE_CANCELLED));
        }
        return null;
	}

	
	

	/**
	 * @param filetypeExtension The extension used when saving the file (or null if any allowed).
	 * @param file The location first suggested to the user for saving to or null.
	 * @param content The content that will be saved to disk
	 * @return The location chosen by the user for saving to or null if cancelled.
	 */
	public static File askUserAndSave(File file, String content, String... filetypeExtension) {
		
		Preconditions.checkNotNull(content);
		File f = askUserSaveLocation(file, filetypeExtension);
        if (f != null) {
            try {
            	IOUtils.writeStringToFile(content, f);
			} catch (IOException e) {
				String msg = Msg.get(Key.ERROR_SAVING) + ": " + f;
				LOG.info(msg);
		        JOptionPane.showMessageDialog(null, msg, Msg.get(Key.ERROR_SAVING), JOptionPane.ERROR_MESSAGE);
			}
        }
        return f;
		
	}
	
	public static void adjustAllFonts(float sizeMultiplier) {
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource) {
				FontUIResource existingFont = (javax.swing.plaf.FontUIResource) value;
				float newFontSize = (float) (existingFont.getSize() * sizeMultiplier);
				Font newFont = existingFont.deriveFont(newFontSize);
				UIManager.put(key, newFont);
			}
		}
	}
}
