package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;

import com.google.common.base.Preconditions;
import com.timestored.TimeStored;
import com.timestored.misc.HtmlUtils;
import com.timestored.theme.Icon;
import com.timestored.theme.Theme;

/**
 * Displays version and homepage information.
 */
public class WelcomeDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	private static final String ACT = "https://www.timestored.com/pulse?action=";
	private static final String ADD_CONN = "add-connection";
	private static final String PREFERENCES = "PREFERENCES";
	private final Action addServerAction;
	private final JFrame parentFrame;

	public WelcomeDialog(JFrame parentFrame, String title, String version, Action addServerAction) {
		
		super(parentFrame, title);
		Icon icon = Theme.CIcon.QSTUDIO_LOGO;
		String htmlTitle = "<h1><font color='#2580A2'>q</font><font color='#25A230'>Studio</font></h1>";
		this.addServerAction = Preconditions.checkNotNull(addServerAction);
		this.parentFrame = Preconditions.checkNotNull(parentFrame);
        setIconImage(icon.get().getImage());
		setPreferredSize(new Dimension(700, 700));

        JPanel topRow = new JPanel(new BorderLayout());
		JPanel logoPanel = new JPanel();
		logoPanel.add(new JLabel(icon.get()));
		logoPanel.add(Theme.getHtmlText(htmlTitle));
		logoPanel.setAlignmentX(CENTER_ALIGNMENT);
		topRow.add(logoPanel, BorderLayout.CENTER);

		JLabel label = new JLabel("<html><h4>Version: " + version + "</h4></html>");
		label.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
		label.setHorizontalAlignment(SwingConstants.CENTER);
		topRow.add(label, BorderLayout.EAST);
		

        JPanel p = Theme.getVerticalBoxPanel();
		p.add(topRow);
		
		String html = "Welcome. To get started <a href='" + ACT + ADD_CONN + "'>Add a Database Connection</a><br />"
				+ "For more details, see our online guide: " + TimeStored.Page.QSTUDIO_CONNECTING.toAnchor("Connecting to Databases.") 
				+ "<br /><br />"
				+ "<h3>Customize qStudio</h3>"
				+ "To customize qStudio appearance, see <a href='" + ACT + PREFERENCES + "'>Settings->Preferences</a>."
				+ "<br />This allows you to set dark mode and customize the fonts used."
				+ "<br /><h3>Online Guides:</h3>"
				+ "<ul>"
				+ "<li>" + TimeStored.Page.QSTUDIO_HELP.toAnchor("Online Help") + "</li>"
				+ "<li>" + TimeStored.Page.QSTUDIO_DATABASES.toAnchor("Supported Databases") + "</li>"
				+ "<li>" + TimeStored.Page.QSTUDIO_CHARTING.toAnchor("Charting") + "</li>"
				+ "<h3>kdb+ Specific Guides:</h3>"
				+ "<li>" + TimeStored.Page.QSTUDIO_HELP_QUNIT.toAnchor("kdb+ QUnit Testing") + "</li>"
				+ "<li>" + TimeStored.Page.QSTUDIO_HELP_QDOC.toAnchor("kdb+ Database Documentation") + "</li>"
						+ "<li>" + TimeStored.Page.QSTUDIO_HELP_LOADCSV.toAnchor("Loading CSVs") + "</li>"
				
				+ "</ul>"; 
		JScrollPane sp = new JScrollPane(new MyHtmlPane(html));
		sp.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
		p.add(sp);
		p.setAlignmentX(CENTER_ALIGNMENT);
		
		Container cp = this.getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(topRow, BorderLayout.NORTH);
		cp.add(p, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(parentFrame);
	}

	private class MyHtmlPane extends JEditorPane {
		public MyHtmlPane(String html) {
			super("text/html", html);
			this.addHyperlinkListener((HyperlinkEvent e) -> {
	            if (e != null && e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
	            	if(e.getURL() != null) {
		            	String urlS = e.getURL().toString();
	            		if(urlS.startsWith(ACT)) {
	            			switch(urlS.substring(ACT.length())) {
	            			case ADD_CONN:
	            				addServerAction.actionPerformed(null);
	            				break;
	            			case PREFERENCES:
	            				new PreferencesDialog(MyPreferences.INSTANCE, parentFrame);
	            				break;
	            			default:
		            			HtmlUtils.browse(urlS);
	            			}
	            		} else {
	            			HtmlUtils.browse(urlS);
	            		}	            
	            	}
            	}
		    });
		    setEditable(false);
		    setBackground(new JLabel().getBackground());
		}
	}

}
