package com.limegroup.gnutella.gui.library;

import java.awt.event.ActionListener;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GUIMediator;

/**
 * This class contains the buttons in the library window, allowing
 * classes in this package to enable or disable buttons at specific
 * indeces in the row.
 */
final class LibraryTableButtons {


	/**
	 * The row of buttons for the download window.
	 */
	private ButtonRow BUTTONS;

	/**
	 * The index of the kill button in the button row.
	 */
	static final int LAUNCH_BUTTON;

	/**
	 * The index of the resume button in the button row.
	 */
	static final int PLAYLIST_BUTTON;

	/**
	 * The index of the launch button in the button row.
	 */
	static final int DELETE_BUTTON;

	/**
	 * The index of the chat button in the button row.
	 */
	static final int ANNOTATE_BUTTON;
	
	/**
	 * The index of the resume button in the button row.
	 */
	static final int RESUME_BUTTON;
	
	static {
	    if (!GUIMediator.isPlaylistVisible()) {
	        LAUNCH_BUTTON   = 0;
	        DELETE_BUTTON   = 1;
	        ANNOTATE_BUTTON = 2;
	        RESUME_BUTTON   = 3;
            PLAYLIST_BUTTON = -1;
	    } else {
	        LAUNCH_BUTTON   = 0;
	        PLAYLIST_BUTTON = 1;
	        DELETE_BUTTON   = 2;
	        ANNOTATE_BUTTON = 3;
	        RESUME_BUTTON   = 4;
	   }
    }
	
	/**
	 * The constructor creates the row of buttons with their associated
	 * listeners.
	 */
	LibraryTableButtons(final LibraryTableMediator ltm) {

        String[] buttonLabelKeys;
        String[] toolTipKeys;
        ActionListener[] listeners;
        String[] iconNames;
        if (!GUIMediator.isPlaylistVisible()) {
            String[] buttonLabelKeysLOCAL = {
                "LIBRARY_LAUNCH_BUTTON_LABEL",
                "LIBRARY_DELETE_BUTTON_LABEL",
                "LIBRARY_ANNOTATE_BUTTON_LABEL",
                "LIBRARY_RESUME_BUTTON_LABEL"
            };
            String[] toolTipKeysLOCAL = {
                "LIBRARY_LAUNCH_BUTTON_TIP",
                "LIBRARY_DELETE_BUTTON_TIP",
                "LIBRARY_ANNOTATE_BUTTON_TIP",
                "LIBRARY_RESUME_BUTTON_TIP"
            };		
            ActionListener[] listenersLOCAL = {
                LibraryTableMediator.LAUNCH_LISTENER, 
                ltm.REMOVE_LISTENER,
                LibraryTableMediator.ANNOTATE_LISTENER,
                LibraryTableMediator.RESUME_LISTENER
            };
    		String[] iconNamesLOCAL =  {
    		    "LIBRARY_LAUNCH",
    		    "LIBRARY_DELETE",
    		    "LIBRARY_ANNOTATE",
    		    "LIBRARY_RESUME"
    		};		            
            buttonLabelKeys = buttonLabelKeysLOCAL;
            toolTipKeys = toolTipKeysLOCAL;
            listeners = listenersLOCAL;
            iconNames = iconNamesLOCAL;
        } else {
            String[] buttonLabelKeysLOCAL = {
                "LIBRARY_LAUNCH_BUTTON_LABEL",
                "LIBRARY_PLAYLIST_BUTTON_LABEL",
                "LIBRARY_DELETE_BUTTON_LABEL",
                "LIBRARY_ANNOTATE_BUTTON_LABEL",
                "LIBRARY_RESUME_BUTTON_LABEL"
            };
            String[] toolTipKeysLOCAL = {
                "LIBRARY_LAUNCH_BUTTON_TIP",
                "LIBRARY_PLAYLIST_BUTTON_TIP",
                "LIBRARY_DELETE_BUTTON_TIP",
                "LIBRARY_ANNOTATE_BUTTON_TIP",
                "LIBRARY_RESUME_BUTTON_TIP"
            };		
            ActionListener[] listenersLOCAL = {
                LibraryTableMediator.LAUNCH_LISTENER, 
                LibraryTableMediator.ADD_PLAY_LIST_LISTENER,
                ltm.REMOVE_LISTENER,
                LibraryTableMediator.ANNOTATE_LISTENER,
                LibraryTableMediator.RESUME_LISTENER
            };
    		String[] iconNamesLOCAL =  {
    		    "LIBRARY_LAUNCH",
    		    "LIBRARY_TO_PLAYLIST",
    		    "LIBRARY_DELETE",
    		    "LIBRARY_ANNOTATE",
    		    "LIBRARY_RESUME"
    		};		       
            buttonLabelKeys = buttonLabelKeysLOCAL;
            toolTipKeys = toolTipKeysLOCAL;
            listeners = listenersLOCAL;
            iconNames = iconNamesLOCAL;
        }
		BUTTONS = new ButtonRow(buttonLabelKeys, toolTipKeys, listeners,
		                        iconNames, ButtonRow.X_AXIS, ButtonRow.NO_GLUE);
	}
	
	ButtonRow getComponent() {
        return BUTTONS;
    }
}
