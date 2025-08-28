package org.simplericity.macify.eawt;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements Application by calling the Mac OS X API through reflection.
 * If this class is used on a non-OS X platform the operations will have no effect or they will simulate
 * what the Apple API would do for those who manipulate state. ({@link #setEnabledAboutMenu(boolean)} etc.)
 */
@SuppressWarnings("all")
public class DefaultApplication implements Application {

    private Object application;
    private Class applicationListenerClass;
    private Map<ApplicationListener, Object> listenerMap = Collections.synchronizedMap(new HashMap<>());
    private boolean enabledAboutMenu = true;
    private boolean enabledPreferencesMenu;
    private boolean aboutMenuItemPresent = true;
    private boolean preferencesMenuItemPresent;
    private ClassLoader classLoader;

    private ApplicationPlatform platform;

    public DefaultApplication() {
        this.platform = initializePlatform();
    }

    // Initialize platform to handle Mac or other platforms
    private ApplicationPlatform initializePlatform() {
        try {
            final File file = new File("/System/Library/Java");
            if (file.exists()) {
                ClassLoader scl = ClassLoader.getSystemClassLoader();
                Class clc = scl.getClass();
                if (URLClassLoader.class.isAssignableFrom(clc)) {
                    Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
                    addUrl.setAccessible(true);
                    addUrl.invoke(scl, new Object[]{file.toURL()});
                }
            }

            Class appClass = Class.forName("com.apple.eawt.Application");
            application = appClass.getMethod("getApplication", new Class[0]).invoke(null, new Object[0]);
            applicationListenerClass = Class.forName("com.apple.eawt.ApplicationListener");

            return new MacPlatform(application, applicationListenerClass);
        } catch (Exception e) {
            return new DefaultPlatform();
        }
    }

    @Override
    public boolean isMac() {
        return platform.isMac();
    }

    @Override
    public void addAboutMenuItem() {
        platform.addAboutMenuItem();
    }

    @Override
    public void addApplicationListener(ApplicationListener applicationListener) {
        platform.addApplicationListener(applicationListener);
    }

    @Override
    public void addPreferencesMenuItem() {
        platform.addPreferencesMenuItem();
    }

    @Override
    public boolean getEnabledAboutMenu() {
        return platform.getEnabledAboutMenu();
    }

    @Override
    public boolean getEnabledPreferencesMenu() {
        return platform.getEnabledPreferencesMenu();
    }

    @Override
    public Point getMouseLocationOnScreen() {
        return platform.getMouseLocationOnScreen();
    }

    @Override
    public boolean isAboutMenuItemPresent() {
        return platform.isAboutMenuItemPresent();
    }

    @Override
    public boolean isPreferencesMenuItemPresent() {
        return platform.isPreferencesMenuItemPresent();
    }

    @Override
    public void removeAboutMenuItem() {
        platform.removeAboutMenuItem();
    }

    @Override
    public synchronized void removeApplicationListener(ApplicationListener applicationListener) {
        platform.removeApplicationListener(applicationListener);
    }

    @Override
    public void removePreferencesMenuItem() {
        platform.removePreferencesMenuItem();
    }

    @Override
    public void setEnabledAboutMenu(boolean enabled) {
        platform.setEnabledAboutMenu(enabled);
    }

    @Override
    public void setEnabledPreferencesMenu(boolean enabled) {
        platform.setEnabledPreferencesMenu(enabled);
    }

    @Override
    public int requestUserAttention(int type) {
        return platform.requestUserAttention(type);
    }

    @Override
    public void cancelUserAttentionRequest(int request) {
        platform.cancelUserAttentionRequest(request);
    }

    @Override
    public void setApplicationIconImage(BufferedImage image) {
        platform.setApplicationIconImage(image);
    }

    @Override
    public BufferedImage getApplicationIconImage() {
        return platform.getApplicationIconImage();
    }
}

// Abstract platform class to define the interface for platform-specific behavior
abstract class ApplicationPlatform {

    public abstract boolean isMac();

    public abstract void addAboutMenuItem();

    public abstract void addApplicationListener(ApplicationListener applicationListener);

    public abstract void addPreferencesMenuItem();

    public abstract boolean getEnabledAboutMenu();

    public abstract boolean getEnabledPreferencesMenu();

    public abstract Point getMouseLocationOnScreen();

    public abstract boolean isAboutMenuItemPresent();

    public abstract boolean isPreferencesMenuItemPresent();

    public abstract void removeAboutMenuItem();

    public abstract void removeApplicationListener(ApplicationListener applicationListener);

    public abstract void removePreferencesMenuItem();

    public abstract void setEnabledAboutMenu(boolean enabled);

    public abstract void setEnabledPreferencesMenu(boolean enabled);

    public abstract int requestUserAttention(int type);

    public abstract void cancelUserAttentionRequest(int request);

    public abstract void setApplicationIconImage(BufferedImage image);

    public abstract BufferedImage getApplicationIconImage();
}

// Platform-specific implementation for Mac
class MacPlatform extends ApplicationPlatform {

    private Object application;
    private Class applicationListenerClass;
    private Map<ApplicationListener, Object> listenerMap = Collections.synchronizedMap(new HashMap<>());

    public MacPlatform(Object application, Class applicationListenerClass) {
        this.application = application;
        this.applicationListenerClass = applicationListenerClass;
    }

    @Override
    public boolean isMac() {
        return true;
    }

    @Override
    public void addAboutMenuItem() {
        callMethod("addAboutMenuItem");
    }

    @Override
    public void addApplicationListener(ApplicationListener applicationListener) {
        Object listener = Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{applicationListenerClass},
                new ApplicationListenerInvocationHandler(applicationListener));

        callMethod(application, "addApplicationListener", new Class[]{applicationListenerClass}, new Object[]{listener});
        listenerMap.put(applicationListener, listener);
    }

    @Override
    public void addPreferencesMenuItem() {
        callMethod("addPreferencesMenuItem");
    }

    @Override
    public boolean getEnabledAboutMenu() {
        return callMethod("getEnabledAboutMenu").equals(Boolean.TRUE);
    }

    @Override
    public boolean getEnabledPreferencesMenu() {
        return callMethod("getEnabledPreferencesMenu").equals(Boolean.TRUE);
    }

    @Override
    public Point getMouseLocationOnScreen() {
        try {
            Method method = application.getClass().getMethod("getMouseLocationOnScreen", new Class[0]);
            return (Point) method.invoke(null, new Object[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isAboutMenuItemPresent() {
        return callMethod("isAboutMenuItemPresent").equals(Boolean.TRUE);
    }

    @Override
    public boolean isPreferencesMenuItemPresent() {
        return callMethod("isPreferencesMenuItemPresent").equals(Boolean.TRUE);
    }

    @Override
    public void removeAboutMenuItem() {
        callMethod("removeAboutMenuItem");
    }

    @Override
    public synchronized void removeApplicationListener(ApplicationListener applicationListener) {
        Object listener = listenerMap.get(applicationListener);
        callMethod(application, "removeApplicationListener", new Class[]{applicationListenerClass}, new Object[]{listener});
        listenerMap.remove(applicationListener);
    }

    @Override
    public void removePreferencesMenuItem() {
        callMethod("removePreferencesMenuItem");
    }

    @Override
    public void setEnabledAboutMenu(boolean enabled) {
        callMethod(application, "setEnabledAboutMenu", new Class[]{Boolean.TYPE}, new Object[]{Boolean.valueOf(enabled)});
    }

    @Override
    public void setEnabledPreferencesMenu(boolean enabled) {
        callMethod(application, "setEnabledPreferencesMenu", new Class[]{Boolean.TYPE}, new Object[]{Boolean.valueOf(enabled)});
    }

    @Override
    public int requestUserAttention(int type) {
        try {
            Object application = getNSApplication();
            Field critical = application.getClass().getField("UserAttentionRequestCritical");
            Field informational = application.getClass().getField("UserAttentionRequestInformational");
            Field actual = type == DefaultApplication.REQUEST_USER_ATTENTION_TYPE_CRITICAL ? critical : informational;
            return ((Integer) application.getClass().getMethod("requestUserAttention", new Class[]{Integer.TYPE}).invoke(application, new Object[]{actual.get(null)})).intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void cancelUserAttentionRequest(int request) {
        try {
            Object application = getNSApplication();
            application.getClass().getMethod("cancelUserAttentionRequest", new Class[]{Integer.TYPE}).invoke(application, new Object[]{new Integer(request)});
        } catch (Exception e) {
            // handle the error
        }
    }

    @Override
    public void setApplicationIconImage(BufferedImage image) {
        try {
            callMethod(application, "setApplicationIconImage", new Class[]{BufferedImage.class}, new Object[]{image});
        } catch (Exception e) {
            // handle exception
        }
    }

    @Override
    public BufferedImage getApplicationIconImage() {
        return callMethod("getApplicationIconImage");
    }

    private Object getNSApplication() {
        try {
            Method method = Class.forName("com.apple.eawt.Application").getMethod("getApplication");
            return method.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Object callMethod(String methodName) {
        try {
            Method method = application.getClass().getMethod(methodName);
            return method.invoke(application);
        } catch (Exception e) {
            return null;
        }
    }

    private Object callMethod(Object target, String methodName, Class[] types, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, types);
            return method.invoke(target, args);
        } catch (Exception e) {
            return null;
        }
    }
}

// Default platform behavior for non-Mac platforms
class DefaultPlatform extends ApplicationPlatform {

    @Override
    public boolean isMac() {
        return false;
    }

    @Override
    public void addAboutMenuItem() {
        // No-op for non-Mac
    }

    @Override
    public void addApplicationListener(ApplicationListener applicationListener) {
        // No-op for non-Mac
    }

    @Override
    public void addPreferencesMenuItem() {
        // No-op for non-Mac
    }

    @Override
    public boolean getEnabledAboutMenu() {
        return true; // default to true on non-Mac
    }

    @Override
    public boolean getEnabledPreferencesMenu() {
        return false; // default to false on non-Mac
    }

    @Override
    public Point getMouseLocationOnScreen() {
        return new Point(0, 0); // default value
    }

    @Override
    public boolean isAboutMenuItemPresent() {
        return true; // default to true on non-Mac
    }

    @Override
    public boolean isPreferencesMenuItemPresent() {
        return false; // default to false on non-Mac
    }

    @Override
    public void removeAboutMenuItem() {
        // No-op for non-Mac
    }

    @Override
    public void removeApplicationListener(ApplicationListener applicationListener) {
        // No-op for non-Mac
    }

    @Override
    public void removePreferencesMenuItem() {
        // No-op for non-Mac
    }

    @Override
    public void setEnabledAboutMenu(boolean enabled) {
        // No-op for non-Mac
    }

    @Override
    public void setEnabledPreferencesMenu(boolean enabled) {
        // No-op for non-Mac
    }

    @Override
    public int requestUserAttention(int type) {
        return -1; // No-op for non-Mac
    }

    @Override
    public void cancelUserAttentionRequest(int request) {
        // No-op for non-Mac
    }

    @Override
    public void setApplicationIconImage(BufferedImage image) {
        // No-op for non-Mac
    }

    @Override
    public BufferedImage getApplicationIconImage() {
        return null; // No-op for non-Mac
    }
}
