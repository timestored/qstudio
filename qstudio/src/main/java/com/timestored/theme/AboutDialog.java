package com.timestored.theme;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.timestored.TimeStored;
import com.timestored.misc.InfoLink;
import com.timestored.qstudio.QStudioFrame;

/**
 * Displays version and homepage information.
 */
public class AboutDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;

	public AboutDialog(JFrame parentFrame, String title,
			Icon icon, String htmlTitle, String version) {
		
		super(parentFrame, title);
        setIconImage(icon.get().getImage());
        setMinimumSize(new Dimension(500, 400));
		
        
		JPanel logoPanel = new JPanel();
		logoPanel.add(new JLabel(icon.get()));
		logoPanel.add(Theme.getHtmlText(htmlTitle));
		logoPanel.setAlignmentX(CENTER_ALIGNMENT);

		JLabel label = new JLabel("<html><h4>Version: " + version + "</h4></html>");
		label.setHorizontalAlignment(SwingConstants.CENTER);
		String txt = "QStudio Release Changes";		
		
        JPanel timestoredLinkPanel = Theme.getVerticalBoxPanel();
		timestoredLinkPanel.add(label);
		timestoredLinkPanel.add(InfoLink.getLabel(txt, txt, TimeStored.Page.QSTUDIO_CHANGES.url(), true));
		timestoredLinkPanel.setAlignmentX(CENTER_ALIGNMENT);
		
		Container cp = this.getContentPane();
		cp.setLayout(new BoxLayout(cp, BoxLayout.PAGE_AXIS));
		cp.add(logoPanel);
		cp.add(timestoredLinkPanel);
		cp.add(Theme.getSubHeader("Configuration Details:"));
		ScrollPane sp = new ScrollPane();
		JTextArea textArea = Theme.getTextArea("name", getDetails());
        textArea.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                // Only select all if it doesn't already have focus
                if (!textArea.hasFocus()) {
                    textArea.requestFocusInWindow();
                    textArea.selectAll();
                }
            }
        });

		sp.add(textArea);
		cp.add(sp);

		pack();
		setLocationRelativeTo(parentFrame);
	}
	
	private String getDetails() {
		StringBuilder sb = new StringBuilder();
		sb.append("version = " + QStudioFrame.VERSION);
		sb.append("\ncurrent dir = " + new File(".").getAbsolutePath());
		sb.append("\nJAVA_HOME = " + System.getenv("JAVA_HOME"));
		sb.append("\njava.version =" + System.getProperty("java.version"));
		sb.append("\nos.name =" + System.getProperty("os.name"));
		sb.append("\nuser.home =" + System.getProperty("user.home"));
		sb.append("\nuser.dir =" + System.getProperty("user.dir"));
		sb.append("\nPATH = " + System.getenv("PATH"));
		return sb.toString();
	}
}
