package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.Box;
import javax.swing.JCheckBox;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.settings.FilterSettings;

/**
 * This class defines the panel in the options window that allows the user
 * to filter out general types of search results, such as search results
 * containing "adult content."
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class IgnoreResultTypesPaneItem extends AbstractPaneItem {

	/**
	 * Handle to the check box for ignoring adult content.
	 */
	private JCheckBox IGNORE_ADULT_CHECK_BOX = new JCheckBox();

	/**
	 * Handle to the check box for ignoring htm/html files.
	 */
	private JCheckBox IGNORE_HTML_CHECK_BOX = new JCheckBox();

	/**
	 * Handle to the check box for ignoring .vbs files.
	 */
	private JCheckBox IGNORE_VBS_CHECK_BOX = new JCheckBox();

	/**
	 * Key for the locale-specifis string for the adult content check box 
	 * label.
	 */
	private String ADULT_BOX_LABEL = "OPTIONS_IGNORE_RESULT_TYPES_ADULT_BOX_LABEL";

	/**
	 * Key for the locale-specifis string for the html file check box 
	 * label.
	 */
	private String HTML_BOX_LABEL = "OPTIONS_IGNORE_RESULT_TYPES_HTML_BOX_LABEL";

	/**
	 * Key for the locale-specifis string for the vbs file check box 
	 * label.
	 */
	private String VBS_BOX_LABEL = "OPTIONS_IGNORE_RESULT_TYPES_VBS_BOX_LABEL";

	/**
	 * The constructor constructs all of the elements of this 
	 * <tt>AbstractPaneItem</tt>.
	 *
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate strings
	 */
	public IgnoreResultTypesPaneItem(final String key) {
		super(key);
		IGNORE_ADULT_CHECK_BOX.setText(GUIMediator.getStringResource(ADULT_BOX_LABEL));
		IGNORE_HTML_CHECK_BOX.setText(GUIMediator.getStringResource(HTML_BOX_LABEL));
		IGNORE_VBS_CHECK_BOX.setText(GUIMediator.getStringResource(VBS_BOX_LABEL));
		BoxPanel checkBoxPanel = new BoxPanel(BoxPanel.X_AXIS);
		BoxPanel checkBoxListPanel = new BoxPanel();
		
		checkBoxListPanel.add(IGNORE_ADULT_CHECK_BOX);
		checkBoxListPanel.add(IGNORE_HTML_CHECK_BOX);
		checkBoxListPanel.add(IGNORE_VBS_CHECK_BOX);
		
		checkBoxPanel.add(Box.createHorizontalGlue());
		checkBoxPanel.add(checkBoxListPanel);
		checkBoxPanel.add(Box.createHorizontalGlue());
		add(checkBoxPanel);
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	public void initOptions() {
		IGNORE_ADULT_CHECK_BOX.setSelected(FilterSettings.FILTER_ADULT.getValue());
		IGNORE_HTML_CHECK_BOX.setSelected(FilterSettings.FILTER_HTML.getValue());
		IGNORE_VBS_CHECK_BOX.setSelected(FilterSettings.FILTER_VBS.getValue());
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
	    boolean adultChanged = false;
	    adultChanged = FilterSettings.FILTER_ADULT.getValue() !=
	                   IGNORE_ADULT_CHECK_BOX.isSelected();
		FilterSettings.FILTER_ADULT.setValue(IGNORE_ADULT_CHECK_BOX.isSelected());
		FilterSettings.FILTER_VBS.setValue(IGNORE_VBS_CHECK_BOX.isSelected());
		FilterSettings.FILTER_HTML.setValue(IGNORE_HTML_CHECK_BOX.isSelected());
		RouterService.adjustSpamFilters();
		if(adultChanged)
		    SearchMediator.rebuildInputPanel();
        return false;
	}
	
    public boolean isDirty() {
        return FilterSettings.FILTER_ADULT.getValue() != IGNORE_ADULT_CHECK_BOX.isSelected() ||
               FilterSettings.FILTER_VBS.getValue() != IGNORE_VBS_CHECK_BOX.isSelected() ||
               FilterSettings.FILTER_HTML.getValue() != IGNORE_HTML_CHECK_BOX.isSelected();
    }	
}
