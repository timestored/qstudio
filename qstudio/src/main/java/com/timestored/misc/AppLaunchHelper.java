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
package com.timestored.misc;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes.FlatIJLookAndFeelInfo;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.google.common.io.Files;



/**
 * Static functions that provide commonly used functionality that launching an application requires.
 * Setting Look and Feel, Logging to Users directory etc.
 */
public class AppLaunchHelper {
	
	private static final Logger LOG = Logger.getLogger(AppLaunchHelper.class.getName());

	private static final Map<String,String> nameToLookFeelClassName = new HashMap<>();
	private static final List<String> NAMES = new ArrayList<>();
	public static final String SPACER_PREFIX = " ------ ";
	
	private static void add(String shortName, String classname) {
		nameToLookFeelClassName.put(shortName,classname);
		NAMES.add(shortName);
	}
	static {
		String defaultClassName = UIManager.getSystemLookAndFeelClassName();
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Windows".equals(info.getName())) {
		        	defaultClassName = info.getClassName();
		        	break;
		        }		        
		    }
		} catch (Exception e) {
			LOG.warning("Problem initialising AppLaunchHelper static. " + e.toString());
		}

		NAMES.add(SPACER_PREFIX + " Core Themes " + SPACER_PREFIX);
		add("Light", defaultClassName);
		
		try {
			// These were the very original names qStudio first supported.
			// because preferences are saved, they must continue to be supported
			add("Dark",FlatOneDarkIJTheme.class.getCanonicalName());
			add("IntelliJ", FlatIntelliJLaf.class.getCanonicalName());
			add("Darcula", FlatDarculaLaf.class.getCanonicalName());
			add("Flat Light", FlatLightLaf.class.getCanonicalName());
			add("Flat Dark", FlatDarkLaf.class.getCanonicalName());
			add("Mac Flat Dark", FlatMacDarkLaf.class.getCanonicalName());
			add("Mac Flat Light", FlatMacLightLaf.class.getCanonicalName());
			NAMES.add(SPACER_PREFIX + " System Themes " + SPACER_PREFIX);
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		    	add(info.getName(), info.getClassName());
		    }
			NAMES.add(SPACER_PREFIX + " Intellij Themes " + SPACER_PREFIX);
			for(FlatIJLookAndFeelInfo lfinfo : FlatAllIJThemes.INFOS) {
				if(!lfinfo.getName().toLowerCase().contains("(material)")) {
					add(lfinfo.getName(), lfinfo.getClassName());
				}
			}
			NAMES.add(SPACER_PREFIX + " Material Themes " + SPACER_PREFIX);
			for(FlatIJLookAndFeelInfo lfinfo : FlatAllIJThemes.INFOS) {
				if(lfinfo.getName().toLowerCase().contains("(material)")) {
					add(lfinfo.getName(), lfinfo.getClassName());
				}
			}	  
		} catch (Exception e) {
			LOG.severe("Could not add the themes from flatlaf.");
		}  
	}

	public static List<String> getLafNamesWithSpacerStrings() { return NAMES; }
	
	public static boolean isLafDark(String lafName) {
		if(lafName != null) {
			String ln = lafName.toLowerCase();
			if(ln.contains("dark") || ln.contains("darcula")) {
				return true;
			}
		}
		for(FlatIJLookAndFeelInfo lfinfo : FlatAllIJThemes.INFOS) {
			if(lfinfo.getName().equals(lafName)) {
				return lfinfo.isDark();
			}
		}	    
		return false;
	}
	

	public static String getLFclassname(String lfname) {
		return nameToLookFeelClassName.get(lfname);
	}	
	
	/**
	 * Set the title and menu bar appropriate for the mac and use windows Look and Feel for windows.
	 * For the title setting to work this function must be called before any Swing classes loaded.
	 * @param title The application title to display on the mac.
	 */
	public static void setMacAndWindowsAppearance(String title, BufferedImage image) {
		
		// set mac properties to put menu bar at top etc
		if(System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
			System.setProperty( "apple.awt.application.name", title );
			System.setProperty( "apple.awt.application.appearance", "system" );
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", title);
			if(image != null) {
		    	try {
					setApplicationIconImagePrivate(image);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
						| SecurityException | ClassNotFoundException e) {
					LOG.warning("problem setting mac image: " + e.toString());
				}
			}
		}
	}
	
    private static void setApplicationIconImagePrivate(BufferedImage image) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        Class appClass = Class.forName("com.apple.eawt.Application");
        Object application = appClass.getMethod("getApplication", new Class[0]).invoke(null, new Object[0]);
        if (application != null) {
            try {
                Method setDockIconImage = application.getClass().getMethod("setDockIconImage", Image.class);
                try {
                    setDockIconImage.invoke(application, image);
                } catch (IllegalAccessException | InvocationTargetException e) {}
            } catch (NoSuchMethodException mnfe) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try {
                    ImageIO.write(image, "png", stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Class nsDataClass = Class.forName("com.apple.cocoa.foundation.NSData");
                    Constructor constructor = nsDataClass.getConstructor(new Class[]{new byte[0].getClass()});
                    Object nsData = constructor.newInstance(new Object[]{stream.toByteArray()});
                    Class nsImageClass = Class.forName("com.apple.cocoa.application.NSImage");
                    Object nsImage = nsImageClass.getConstructor(new Class[]{nsDataClass}).newInstance(new Object[]{nsData});

                    Object napplication = getNSApplication();

                    napplication.getClass().getMethod("setApplicationIconImage", new Class[]{nsImageClass}).invoke(application, new Object[]{nsImage});

                } catch (ClassNotFoundException e) {

                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException  | InstantiationException e) {
                    throw new RuntimeException(e);
                }

            }

        }
    }


    private static Object getNSApplication() throws ClassNotFoundException {
        try {
            Class applicationClass = Class.forName("com.apple.cocoa.application.NSApplication");
            return applicationClass.getMethod("sharedApplication", new Class[0]).invoke(null, new Object[0]);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


	public static boolean setTheme(String theme) {
		try {
			String lfCLassname = getLFclassname(theme);
			if(lfCLassname != null) {
				UIManager.setLookAndFeel(lfCLassname);
				return true;
			}
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {}
		return false;
	}


	public static void logToUsersFolder(String folder) {
		try {
			String p = System.getProperty("user.home") + File.separator + folder
					+ File.separator + "a";
			Files.createParentDirs(new File(p));
			FileHandler fh = new FileHandler("%h/" + folder + "/log%g.log", 1024*1024, 1, true);
			fh.setFormatter(new SimpleFormatter());
			Logger.getLogger("").addHandler(fh);

		} catch (IOException ex) {
			LOG.log(Level.SEVERE, ex.getMessage(), ex);
		} catch (SecurityException ex) {
			LOG.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}
}
