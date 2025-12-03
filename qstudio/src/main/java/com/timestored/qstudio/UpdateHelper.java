package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import com.timestored.TimeStored;
import com.timestored.TimeStored.Page;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.JdbcTypes;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.jgrowl.Growler;
import com.timestored.jgrowl.GrowlerFactory;
import com.timestored.misc.AppLaunchHelper;
import com.timestored.misc.HtmlUtils;
import com.timestored.theme.Theme;

import lombok.Setter;

public class UpdateHelper {

	private static final Logger LOG = Logger.getLogger(UpdateHelper.class.getName());
	private static final Random R = new Random();
	@Setter private static QStudioModel qStudioModel;

	
	
	private static final int GAP = 4;
	public static JPanel getUpdateGrowler(String newVersion) {
		JPanel p = new JPanel(new BorderLayout(GAP,GAP));
		JPanel topRow = new JPanel(new BorderLayout(GAP,GAP));
		JLabel l = GrowlerFactory.getLabelWithFixedWidth("There's an update available: QStudio " + newVersion, 0);
		topRow.add(l, BorderLayout.CENTER);
		topRow.add(new JLabel("X"), BorderLayout.EAST);
		p.add(topRow, BorderLayout.NORTH);
		
		JButton updateButton = new JButton("Install Update");
		updateButton.addActionListener(al -> {
			HtmlUtils.browse(TimeStored.Page.QSTUDIO_DOWNLOAD.url());
			try {Thread.sleep(2000);} catch (InterruptedException e) {}
			System.exit(0);
		});
		updateButton.setBackground(Theme.HIGHLIGHT_BUTTON_COLOR);
		updateButton.setForeground(Color.WHITE);
		updateButton.setOpaque(true);

		JButton changesButton = new JButton("Release Notes");
		changesButton.addActionListener(al -> {
			HtmlUtils.browse(TimeStored.Page.QSTUDIO_CHANGES.url());
		});
		
		JPanel butPanel = new JPanel(new GridLayout(1, 0, GAP, GAP));
		butPanel.add(new JLabel("   "));
		butPanel.add(updateButton);
		butPanel.add(changesButton);
		p.add(butPanel, BorderLayout.SOUTH);
		return p;
	}
	
	
	public static void main(String... args) {
		QStudioModel qsm = new QStudioModel(ConnectionManager.newInstance(), Persistance.INSTANCE, OpenDocumentsModel.newInstance());
		
		JFrame frame = new JFrame();
		frame.setSize(new Dimension(800, 800));
		
		JTabbedPane tabb = new JTabbedPane();

		Growler growler = GrowlerFactory.getGrowler(frame);
		JButton checkBut = new JButton("Check Version");
		checkBut.addActionListener(e -> {
			checkVersion(qsm, 0, GrowlerFactory.getGrowler(frame));
		});
		JButton but = new JButton("Force");
		but.addActionListener(e -> {
			growler.show(Level.INFO, getUpdateGrowler("VERSIONMARKER"), null, true);	
		});
		JPanel p = new JPanel();
		p.add(checkBut);
		p.add(but);
		
		tabb.add(p, "Update Example");
		int i = 1;
		TimeStored.fetchOnlineNews();
		for(String html : TimeStored.getOnlineNews(true)) {
			tabb.add(getNewsPanel(null, html), "Online Dark " + i++);
		}
		for(String html : TimeStored.getOnlineNews(false)) {
			tabb.add(getNewsPanel(null, html), "Online Light " + i++);
		}
		i = 1;
		for(String html : TimeStored.NEWS) {
			tabb.add(getNewsPanel(null, html), "Hardcoded " + i++);
		}
		
		frame.add(tabb);
		frame.setVisible(true);
	}
	
	static void checkVersion(QStudioModel qsm, int queryCount, Growler growler) {
		boolean newVersionAvailable = false;
		String vs = "?";
		String params = "?v=" + QStudioFrame.VERSION;
		try(Scanner sc = new java.util.Scanner(new URL("https://www.timestored.com/qstudio/version3.txt" + params).openStream(), "UTF-8")) {
			try {
				String[] versionTxt = sc.useDelimiter("\\A").next().split(",");
				vs = versionTxt[0];
				newVersionAvailable = !QStudioFrame.VERSION.equals(vs);
				try {
					double vers = Double.parseDouble(vs);
					double cur = Double.parseDouble(QStudioFrame.VERSION);
					newVersionAvailable = cur < vers; // Allows releasing newer version then later changing .txt to recommend updating old
				} catch(NumberFormatException e) {
					LOG.warning("Error parsing versions");
				}
			} catch(RuntimeException e) {
				// any problems then showMsg but dont force
			}
		} catch (IOException e) {}
		if(newVersionAvailable && qsm.getQueryManager().hasAnyServers()) { // Don't ask to update before they add servers
			growler.show(Level.INFO, getUpdateGrowler(vs), null, true);
		}
	}
	


	private static String getPropAsParam(String name) {
		String s = System.getProperty(name);
		return s == null ? "" : URLEncoder.encode(s);
	}

	public static JScrollPane getNewsPanel(JdbcTypes jdbcType) {
		boolean isDarkTheme = AppLaunchHelper.isLafDark(MyPreferences.INSTANCE.getCodeTheme());
		return getNewsPanel(jdbcType, TimeStored.getRandomLongNewsHtml(isDarkTheme));
	}
	
	
	private static JScrollPane getNewsPanel(JdbcTypes jdbcType, String newsHTML) {
		// advert panel initially
		JPanel p = new JPanel(new BorderLayout());
		try {
			if(!TimeStored.isOnlineNewsAvailable()) { // Use builtin images
				p.add(getKdbImage());	
			} else {
				JPanel cp = new JPanel();
				JEditorPane tp = Theme.getHtmlText(newsHTML);
				cp.add(tp);
				p.add(cp, BorderLayout.CENTER);
			}
		} catch(Exception e) {
			// OK to show nothing if error.
		}
		
		return new JScrollPane(p);
	}


	public static JLabel getClickableImageLabel(String imgName, Page page) { 
		ImageIcon imgI = new ImageIcon(TimeStored.class.getResource(imgName));
		JLabel lbl = new JLabel(imgI);
		lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lbl.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
        		HtmlUtils.browse(page.url());
			}
		});
		return lbl;
	}

	
	private static final String[] IMG_NAMES = new String[] { "kdb-cover.jpg", "kdb-tutorials.jpg", "crypto-dark-med.png",
			"excel-export.png", "fxdash-dark-med.png", "price-grid-dark-med.png", "sqlnotebook-start-menu.png", "trade-blotter-dark-med.png" };
	
	
	public static JLabel getKdbImage() { 
		return getClickableImageLabel(IMG_NAMES[R.nextInt(IMG_NAMES.length)], Page.PULSE_TUTORIALS_KDB);
		
	}

	public static boolean possiblyShowTimeStoredWebsite(Persistance persistance) {
		// do a bit of advertising once a month
		Calendar cal = Calendar.getInstance();
		if(cal.get(Calendar.DAY_OF_WEEK)==Calendar.WEDNESDAY 
				&& cal.get(Calendar.DAY_OF_MONTH)<=4) {

			// calc int to represent month and year
	        final int lastOpen = persistance.getInt(Persistance.Key.LAST_AD, 0);
	        int yearOffset = Math.max((cal.get(Calendar.YEAR)-1900), 0)*12;
	        int thisOpen = yearOffset + cal.get(Calendar.MONTH);
	        boolean advertSeenThisMonth = thisOpen <= lastOpen;
	        
	        if(!advertSeenThisMonth) {
	        	// record as seen and open web browser
	        	persistance.putInt(Persistance.Key.LAST_AD, thisOpen);
				HtmlUtils.browse(Page.NEWSPAGE.url());
				return true;
	        }
		}
		return false;
	}

	
}
