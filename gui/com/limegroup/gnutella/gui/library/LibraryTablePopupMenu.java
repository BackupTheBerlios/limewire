package com.limegroup.gnutella.gui.library;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.limegroup.gnutella.gui.GUIMediator;

/**
 * This class contains the popup menu that is visible on right-click events in
 * the library table window.
 */
final class LibraryTablePopupMenu {
	
	/**
	 * Constant for the <tt>JPopupMenu</tt> that contains all of the menu items.
	 */
	private final JPopupMenu MENU = new JPopupMenu();

	/**
	 * The index of the launch menu item.
	 */
	static final int LAUNCH_INDEX = 0;

	/**
	 * The index of the playlist menu item.
	 */
	static final int PLAYLIST_INDEX = 1;

	/**
	 * The index of the delete menu item.
	 */
	static final int DELETE_INDEX = 2;

	/**
	 * The index of the annotate menu item.
	 */
	static final int ANNOTATE_INDEX = 3;
	
	/**
	 * The index of the resume item.
	 */
	static final int RESUME_INDEX = 4;
	
	/**
	 * Rename index.
	 */
	static final int RENAME_INDEX = 5;
	
	LibraryTablePopupMenu(final LibraryTableMediator ltm) {
    	JMenuItem LAUNCH_ITEM = new JMenuItem(
    	    GUIMediator.getStringResource("LIBRARY_LAUNCH_BUTTON_LABEL"));
    	JMenuItem PLAYLIST_ITEM = new JMenuItem(
    	    GUIMediator.getStringResource("LIBRARY_PLAYLIST_BUTTON_LABEL"));
    	JMenuItem DELETE_ITEM = new JMenuItem(
    	    GUIMediator.getStringResource("LIBRARY_DELETE_BUTTON_LABEL"));
    	JMenuItem ANNOTATE_ITEM = new JMenuItem(LibraryTableMediator.ANNOTATE_LISTENER);
        JMenuItem RESUME_ITEM = new JMenuItem(
            GUIMediator.getStringResource("LIBRARY_RESUME_BUTTON_LABEL"));
    	JMenuItem RENAME_ITEM = new JMenuItem(
    	    GUIMediator.getStringResource("LIBRARY_RENAME_BUTTON_LABEL"));    	    
        
   		LAUNCH_ITEM.addActionListener( LibraryTableMediator.LAUNCH_LISTENER );
		PLAYLIST_ITEM.addActionListener( LibraryTableMediator.ADD_PLAY_LIST_LISTENER );
		DELETE_ITEM.addActionListener( ltm.REMOVE_LISTENER );
		RESUME_ITEM.addActionListener( LibraryTableMediator.RESUME_LISTENER );
		RENAME_ITEM.addActionListener( LibraryTableMediator.RENAME_LISTENER );
		MENU.add(LAUNCH_ITEM);
		// if it's visible, add the playlist item.
        if (GUIMediator.isPlaylistVisible())
        	MENU.add(PLAYLIST_ITEM);
        // otherwise, add a separator so that the indices are still right. :)
        else
        	MENU.addSeparator();
		MENU.add(DELETE_ITEM);
		MENU.add(ANNOTATE_ITEM);
		MENU.add(RESUME_ITEM);
		MENU.add(RENAME_ITEM);
    }
    
    JPopupMenu getComponent() { return MENU; }
    
}
