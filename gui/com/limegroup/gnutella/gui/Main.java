package com.limegroup.gnutella.gui;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;

/**
 * This class constructs an <tt>Initializer</tt> instance that constructs
 * all of the necessary classes for the application.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class Main {
	
	/** 
	 * Creates an <tt>Initializer</tt> instance that constructs the 
	 * necessary classes for the application.
	 *
	 * @param args the array of command line arguments
	 */
	public static void main(String args[]) {
	    Frame splash = null;
	    try {
			// show initial splash screen only if there are no arguments
            if (args == null || args.length == 0)
				splash = showInitialSplash();

            // load the GUI through reflection so that we don't reference classes here,
            // which would slow the speed of class-loading, causing the splash to be
            // displayed later.
            Class.forName("com.limegroup.gnutella.gui.GUILoader").
                getMethod("load", new Class[] { String[].class, Frame.class }).
                    invoke(null, new Object[] { args, splash });
        } catch(Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
     }
	
	/**
	 * Shows the initial splash window.
	 */
	private static Frame showInitialSplash() {
	    Frame splashFrame = null;
        Image image = null;
        File themeImage = new File(getUserSettingsDir(), "splash.gif");
        if(themeImage.exists())
            image = Toolkit.getDefaultToolkit().createImage(themeImage.getPath());
        else {
	        URL imageURL = Main.class.getResource("images/default_splash.gif");
	        if(imageURL != null)
	            image = Toolkit.getDefaultToolkit().createImage(imageURL);
	    }

	    if(image != null)
	        splashFrame = AWTSplashWindow.splash(image);
	        
	    return splashFrame;
    }
	
	/**
	 * Gets the settings directory without using CommonUtils.
	 *
	 * Has the side effect of setting the brushed-metal property if necessary.
	 */
    private static File getUserSettingsDir() {
        File dir = new File(System.getProperty("user.home"));
        String os = System.getProperty("os.name").toLowerCase();
        if(os.startsWith("mac os") && os.endsWith("x")) {
			File f = new File(dir, "/Library/Preferences/LimeWire");
			if(new File(f, "useBrushedMetal").exists())
				System.setProperty("apple.awt.brushMetalLook", "true");
			return f;
        } else {
            return new File(dir, ".limewire");
		}
    }
}
