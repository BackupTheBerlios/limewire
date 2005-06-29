package com.limegroup.gnutella.gui;

import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.bugs.BugManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.gui.notify.NotifyUserProxy;
import com.limegroup.gnutella.settings.SettingsHandler;

/**
 * This class provides the "shutdown" method that should be
 * the only method for closing the application for production
 * (non_testing) code.  This method makes sure that all of
 * the necessary classes are notified that the virtual machine
 * is about to be exited.
 */
final class Finalizer {
    
    /** Stores the connection status before a shutdown
     * operation is initiated.
     */    
    private static boolean _wasConnected;
    
    /** Stores whether a shutdown operation has been
     * initiated.
     */    
    private static boolean _shutdownImminent;
    
    /** Indicates whether file uploads are complete.
     */    
    private static boolean _uploadsComplete;
    
    /** Indicates whether file downloads are complete.
     */    
    private static boolean _downloadsComplete;    

	/**
	 * Suppress the default constructor to ensure that this class can never
	 * be constructed.
	 */
	private Finalizer() {}
    
    /** Indicates whether the application is waiting to
     * shutdown.
     * @return true if the application is waiting to
     * shutdown, false otherwise
     */    
    static boolean isShutdownImminent() {
        return _shutdownImminent;
    }
    
    /**
     * Exits the virtual machine, making calls to save
     * any necessary settings and to perform any
     * necessary cleanups.
     * @param toExecute a string to try to execute after shutting down.
     */
    static void shutdown(final String toExecute) {
        GUIMediator.applyWindowSettings();
        
        GUIMediator.setAppVisible(false);
        new ShutdownWindow().setVisible(true);
        
        // remove any user notification icons
        NotifyUserProxy.instance().removeNotify();
        
        // Do shutdown stuff in another thread.
        // We don't want to lockup the event thread
        // (which this was called on).
        Thread shutdown = new Thread("Shutdown Thread") {
            public void run() {
                try {
                    BugManager.instance().shutdown();

                    RouterService router = GUIMediator.instance().getRouter();
                    if (router != null)
                        RouterService.shutdown(toExecute);
                    // if the router does not exist, we still need
                    // to write out the properties.
                    else
                        SettingsHandler.save();
                    
                    System.exit(0);
                } catch(Throwable t) {
                    t.printStackTrace();
                    System.exit(0);
                }
            }
        };
        shutdown.start();
    }
    
    static void shutdown() {
        shutdown(null);
    }
    
    /** Exits the virtual machine, making calls to save
     * any necessary settings and to perform any
     * necessary cleanups, after all incoming and
     * outgoing transfers are complete.
     */    
    static void shutdownAfterTransfers() {
        if (isShutdownImminent())
            return;
        
        _shutdownImminent = true;
        
        _wasConnected = RouterService.isConnected();
	
		RouterService.setIsShuttingDown(true);
	
        if (_wasConnected)
            RouterService.disconnect();
        
        if (transfersComplete())
            GUIMediator.shutdown();
    }
    
    /** Cancels a pending shutdown operation.
     */    
    public static void cancelShutdown() {
        _shutdownImminent = false;
        _uploadsComplete = false;
        _downloadsComplete = false;
        
        if (_wasConnected) {
            RouterService.connect();
		}
		
		RouterService.setIsShuttingDown(false);

    }
    
    /** Notifies the <tt>Finalizer</tt> that all
     * downloads have been completed.
     */    
    static void setDownloadsComplete() {
        _downloadsComplete = true;
        checkForShutdown();
    }
    
    /** Notifies the <tt>Finalizer</tt> that all uploads
     * have been completed.
     */    
    static void setUploadsComplete() {
        _uploadsComplete = true;
        checkForShutdown();
    }
    
    /** Indicates whether all incoming and outgoing
     * transfers have completed at the time this method
     * is called.
     * @return true if all transfers have been
     * completed, false otherwise.
     */    
    private static boolean transfersComplete() {
		RouterService router = GUIMediator.instance().getRouter();
        if (router == null)
            return true;
        
        if (RouterService.getNumDownloads() == 0)
            _downloadsComplete = true;
        if (RouterService.getNumUploads() == 0)
            _uploadsComplete = true;
        
        return _uploadsComplete & _downloadsComplete;
    }
    
    /** Attempts to shutdown the application.  This
     * method does nothing if all file transfers are
     * not yet complete.
     */    
    private static void checkForShutdown() {
        if(_shutdownImminent && _uploadsComplete && _downloadsComplete) {
            GUIMediator.shutdown();
        }
    }
    
    /**
     * Adds the specified <tt>Finalizable</tt> instance to the list of
     * classes to notify prior to shutdown.
     * 
     * @param fin the <tt>Finalizable</tt> instance to register
     */
    static void addFinalizeListener(final FinalizeListener fin) {
        Thread t = new Thread("FinalizeItem") {
            public void run() {
                fin.doFinalize();
            }
        };
        RouterService.addShutdownItem(t);
    }
    
}
