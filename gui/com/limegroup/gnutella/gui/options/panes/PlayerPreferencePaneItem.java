package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.settings.PlayerSettings;

/**
 * This class defines the panel in the options window that allows the user
 * to change the default mp3 player used by limewire.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class PlayerPreferencePaneItem extends AbstractPaneItem {

	/**
	 * Constant for the key of the locale-specific <tt>String</tt> for the 
	 * PLAYER enabled check box label in the options window.
	 */
	private final String CHECK_BOX_LABEL = 
		"OPTIONS_PLAYER_ACTIVE_CHECK_BOX_LABEL";

	/**
	 * Constant for the check box that specifies whether or not downloads 
	 * should be automatically cleared.
	 */
	private final JCheckBox CHECK_BOX = new JCheckBox();

	/**
	 * The constructor constructs all of the elements of this 
	 * <tt>AbstractPaneItem</tt>.
	 *
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate locale-specific keys
	 */
	public PlayerPreferencePaneItem(final String key) {
		super(key);
		LabeledComponent comp = new LabeledComponent(CHECK_BOX_LABEL,
													 CHECK_BOX,
													 LabeledComponent.LEFT_GLUE);
		add(comp.getComponent());
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	public void initOptions() {
        CHECK_BOX.setSelected(PlayerSettings.PLAYER_ENABLED.getValue());
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Applies the options currently set in this window, displaying an
	 * error message to the user if a setting could not be applied.
	 *
	 * @throws IOException if the options could not be applied for some reason
	 */
	public boolean applyOptions() throws IOException {
        boolean restartRequired =
            CHECK_BOX.isSelected() != PlayerSettings.PLAYER_ENABLED.getValue();
		PlayerSettings.PLAYER_ENABLED.setValue(CHECK_BOX.isSelected());
        return restartRequired;
	}
	
    public boolean isDirty() {
        return PlayerSettings.PLAYER_ENABLED.getValue() != CHECK_BOX.isSelected();
    }
}
