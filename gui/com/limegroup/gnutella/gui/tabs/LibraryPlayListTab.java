package com.limegroup.gnutella.gui.tabs;

import javax.swing.JComponent;
import javax.swing.JSplitPane;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.library.LibraryMediator;
import com.limegroup.gnutella.gui.playlist.PlaylistMediator;
import com.limegroup.gnutella.gui.util.DividerLocationSettingUpdater;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.UISettings;

/**
 * This class handles access to the tab that contains the library
 * as well as the playlist to the user.
 */
public final class LibraryPlayListTab extends AbstractTab {

	/**
	 * Constant for the <tt>Component</tt> instance containing the 
	 * elements of this tab.
	 */
	private final JComponent COMPONENT;

	/**
	 * Constructs the elements of the tab.
	 *
	 * @param LIBRARY_MEDIATOR the <tt>LibraryMediator</tt> instance 
	 * @param PLAYLIST_VIEW the <tt>PlayListView</tt> instance 
	 */
	public LibraryPlayListTab(final LibraryMediator LIBRARY_MEDIATOR,
							  final PlaylistMediator PLAYLIST_MEDIATOR) {
		super("LIBRARY", GUIMediator.LIBRARY_INDEX, "library_tab");
		if(PLAYLIST_MEDIATOR != null) {
			JSplitPane divider = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
												LIBRARY_MEDIATOR.getComponent(), 
												PLAYLIST_MEDIATOR.getComponent());
			divider.setOneTouchExpandable(true);
			new DividerLocationSettingUpdater(divider, 
					UISettings.UI_LIBRARY_PLAY_LIST_TAB_DIVIDER_LOCATION);
			COMPONENT = divider;
		}	
		
		else {
			COMPONENT = LIBRARY_MEDIATOR.getComponent();
		}
	}

	public void storeState(boolean visible) {
        ApplicationSettings.LIBRARY_VIEW_ENABLED.setValue(visible);
	}

	public JComponent getComponent() {
		return COMPONENT;
	}
}
