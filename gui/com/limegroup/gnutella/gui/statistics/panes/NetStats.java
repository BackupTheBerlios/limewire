package com.limegroup.gnutella.gui.statistics.panes;

import javax.swing.JLabel;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.LabeledComponent;

/**
 * This class displays information about general network statistics.
 */
public final class NetStats extends AbstractOptionPaneItem {
	
	private final JLabel CURRENT_HOSTS = new JLabel();

	private final JLabel TOTAL_HOSTS = new JLabel();

	private final JLabel TOTAL_FILES = new JLabel();

	private final JLabel TOTAL_FILE_SIZE = new JLabel();

	/**
	 * Creates a new graph that displays general network statistics.
	 * 
	 * @param key the key for obtaining label string resources
	 */
	public NetStats() {
		super("NET_PANE");
		LabeledComponent comp0 = 
		    new LabeledComponent("STATS_NET", CURRENT_HOSTS,
								 LabeledComponent.RIGHT_GLUE);
		LabeledComponent comp1 = 
		    new LabeledComponent("STATS_HOSTS", TOTAL_HOSTS,
								 LabeledComponent.RIGHT_GLUE);
		LabeledComponent comp2 = 
		    new LabeledComponent("STATS_FILES", TOTAL_FILES,
								 LabeledComponent.RIGHT_GLUE);
		LabeledComponent comp3 = 
		    new LabeledComponent("STATS_FILE_SIZE", TOTAL_FILE_SIZE,
								 LabeledComponent.RIGHT_GLUE);

		add(comp0.getComponent());
		add(comp1.getComponent());
		add(comp2.getComponent());
		add(comp3.getComponent());
	}

	// inherit doc comment
	public void refresh() {
		int numConn = RouterService.getNumInitializedConnections();
		int numHosts = (int)RouterService.getNumHosts();
		long numFiles = RouterService.getNumFiles();
		long totSize = RouterService.getTotalFileSize();
		CURRENT_HOSTS.setText(GUIUtils.toLocalizedInteger(numConn));
		TOTAL_HOSTS.setText(GUIUtils.toLocalizedInteger(numHosts));
		TOTAL_FILES.setText(GUIUtils.toLocalizedInteger(numFiles));
		TOTAL_FILE_SIZE.setText(//Note: displayed as MBs, not KBs!
		    GUIUtils.toLocalizedInteger(
			    (totSize > 0 && totSize < 1024) ? 1
				: (totSize + 512) / 1024));		
	}

	
}
