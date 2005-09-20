package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.settings.StatusBarSettings;

/**
 * This class defines the panel in the options window that allows the user
 * to change whether the connection quality is visible in the status bar.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class StatusBarConnectionQualityPaneItem extends AbstractPaneItem {

    /**
     * Constant for the key of the locale-specific <tt>String</tt> for whether 
     * the connection quality status should be displayed in the status bar.
     */
    private final String CHECK_BOX_LABEL = 
        "OPTIONS_STATUS_BAR_CONNECTION_QUALITY_CHECK_BOX_LABEL";

    private final JCheckBox CHECK_BOX = new JCheckBox();

    /**
     * The constructor constructs all of the elements of this 
     * <tt>AbstractPaneItem</tt>.
     *
     * @param key the key for this <tt>AbstractPaneItem</tt> that the
     *            superclass uses to generate locale-specific keys
     */
    public StatusBarConnectionQualityPaneItem(final String key) {
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
        CHECK_BOX.setSelected(StatusBarSettings.CONNECTION_QUALITY_DISPLAY_ENABLED.getValue());
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
        if (!isDirty())
            return false;
        
        StatusBarSettings.CONNECTION_QUALITY_DISPLAY_ENABLED.setValue(CHECK_BOX.isSelected());
        GUIMediator.instance().getStatusLine().refresh();
        return false;
    }
    
    public boolean isDirty() {
        return StatusBarSettings.CONNECTION_QUALITY_DISPLAY_ENABLED.getValue() != CHECK_BOX.isSelected();
    }
}
