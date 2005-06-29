
package com.limegroup.gnutella.gui;

import javax.swing.SwingUtilities;

import com.limegroup.gnutella.browser.ExternalControl;

/**
JNI based GetURL AppleEvent handler for Mac OS X
*/
public final class GURLHandler {
    
    static {
        System.loadLibrary("GURL");
    }

    private static final GURLHandler INSTANCE = new GURLHandler();

    private boolean isRegistered = false;
    
    private GURLHandler() 
        throws UnsatisfiedLinkError {
    }
	
	public static GURLHandler getInstance() {
		return INSTANCE;
	}
    
    /** Called by the native code */
    private void callback(final String url) {
		if ( ExternalControl.isInitialized() ) {
			Runnable runner = new Runnable() {
				public void run() {
                    try {
					   ExternalControl.handleMagnetRequest(url);
                    } catch(Throwable t) {
                        // Make sure we catch any errors.
                        GUIMediator.showInternalError(t);
                    }
				} 
			};
			SwingUtilities.invokeLater(runner);
		} else {
		    ExternalControl.enqueueMagnetRequest(url);
		}
    }
    
    /** Registers the GetURL AppleEvent handler. */
    public void register() {
		if (!isRegistered) {
            if (InstallEventHandler() == 0) {
                isRegistered = true;
            }
        }
    }
    
    /** We're nice guys and remove the GetURL AppleEvent handler although
    this never happens */
    protected void finalize() throws Throwable {
        if (isRegistered) {
            RemoveEventHandler();
        }
    }
    
    private synchronized final native int InstallEventHandler();
    private synchronized final native int RemoveEventHandler();
}
