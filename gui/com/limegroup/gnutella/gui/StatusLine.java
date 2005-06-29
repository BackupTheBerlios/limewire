package com.limegroup.gnutella.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.limegroup.gnutella.gui.mp3.MediaPlayerComponent;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * The component for the space at the bottom of the main application
 * window, including the connected status and the media player.
 */
public final class StatusLine implements ThemeObserver {

    /**
     * Disconnected status.
     */
    public static final int STATUS_DISCONNECTED = 0;

    /**
     * Connecting status.
     */
    public static final int STATUS_CONNECTING = 1;

    /**
     * Poor status.
     */
    public static final int STATUS_POOR = 2;

    /**
     * Fair status.
     */
    public static final int STATUS_FAIR = 3;

    /**
     * Good status.
     */
    public static final int STATUS_GOOD = 4;

    /**
     * Excellent status.
     */
    public static final int STATUS_EXCELLENT = 5;

    /**
     * Turbocharged status.
     */
    public static final int STATUS_TURBOCHARGED = 6;

    /**
     * Idle status.
     */
    public static final int STATUS_IDLE = 7;
    
    /**
     * Waking up status.
     */
    public static final int STATUS_WAKING_UP = 8;

    /**
     * The left most panel containing the connection quality.
     * The switcher changes the actual image on this panel.
     */
    private JPanel connectionQuality;

    /**
     * The images of the connection quality.
     */
    private JPanel[] qualityPanels = new JPanel[9];

    /**
     * <tt>CardLayout</tt> for switching between connected and
     * disconnected views.
     */
    private CardLayout switcher;

    /**
     * The label with number of files being shared statistics.
     */
    private JLabel sharingLabel;

    /**
     * The panel that holds the text & status.
     */
    private JPanel connectionQualityText;

    /**
     * The label with the connection quality status.
     */
    private JLabel connectionStatus;


    /**
     * Takes care of all the MediaPlayer stuff.
     */
    private MediaPlayerComponent _mediaPlayer;

    /**
     * Update panel
     */
    private UpdatePanel _updatePanel;

    /**
     * Constant for the <tt>JPanel</tt> that displays the status line.
     */
    private final JPanel PANEL = new BoxPanel(BoxPanel.X_AXIS);

    /**
     * Constant for the center panel, containing either the updated
     * status text or the donation text.
     */
    private final JPanel CENTER = new BoxPanel(BoxPanel.X_AXIS);

    /**
     * The status text.
     */
    private final StatusComponent STATUS_LABEL =
        new StatusComponent(StatusComponent.CENTER);

    /**
     * Creates a new status line in the disconnected state.
     */
    public StatusLine() {
        GUIMediator.setSplashScreenString(
            GUIMediator.getStringResource("SPLASH_STATUS_STATUS_WINDOW"));

        //Make components
        createConnectPanel();

        // add the 'Sharing X files' info
        sharingLabel = new JLabel("           ");
        sharingLabel.setHorizontalAlignment(SwingConstants.LEFT);
        String toolTip =
            GUIMediator.getStringResource("STATISTICS_SHARING_TOOLTIP");
        sharingLabel.setToolTipText(toolTip);

        // add the 'Connection Quality: <status>' labels.
        connectionQualityText = new BoxPanel(BoxPanel.X_AXIS);
        JLabel connectionText = new JLabel(
            GUIMediator.getStringResource("STATISTICS_CONNECTION_QUALITY") + " ");
        connectionText.setAlignmentX(0.0f);
        connectionStatus = new JLabel();
        connectionStatus.setAlignmentX(0.0f);
        connectionQualityText.add(connectionText);
        connectionQualityText.add(connectionStatus);

        // don't allow easy clipping
        Dimension minimum = new Dimension(20, 3);
        sharingLabel.setMinimumSize(minimum);
        connectionStatus.setMinimumSize(minimum);
        connectionQualityText.setMinimumSize(minimum);

        // align far left.
        connectionQualityText.setAlignmentX(0.0f);
        sharingLabel.setAlignmentX(0.0f);

        // Set the bars to not be connected.
        setConnectionQuality(0);

        // Tweak appearance
        PANEL.setBorder(BorderFactory.createLoweredBevelBorder());

        // Create the panel with the text descriptions.
        JPanel textInfo = new BoxPanel(BoxPanel.Y_AXIS);
        textInfo.add(connectionQualityText);
        textInfo.add(sharingLabel);

        // Put them together.
        JPanel leftPanel = new BoxPanel(BoxPanel.X_AXIS);
        // add connect icon and text panel
        leftPanel.add(connectionQuality);
        leftPanel.add(textInfo);
        PANEL.add(leftPanel);

        JPanel centerPanel = new BoxPanel(BoxPanel.Y_AXIS);

        JPanel topCenter = new BoxPanel(BoxPanel.X_AXIS);
        //add the panel to notify about updates
        _updatePanel = new UpdatePanel();
        topCenter.add(_updatePanel);

        STATUS_LABEL.setProgressPreferredSize(new Dimension(250, 20));
        CENTER.add(STATUS_LABEL);

        centerPanel.add(topCenter);
        centerPanel.add(CENTER);

        PANEL.add(Box.createHorizontalGlue());
        PANEL.add(centerPanel);
        PANEL.add(Box.createHorizontalGlue());

        Dimension prefer = new Dimension(PANEL.getWidth(), 45);
        PANEL.setMinimumSize(prefer);

        if (GUIMediator.isPlaylistVisible()) {
            // now show the mp3 player...
            _mediaPlayer = MediaPlayerComponent.instance();
            JPanel mediaPanel = _mediaPlayer.getMediaPanel();
            PANEL.add(mediaPanel);
        }

	    ThemeMediator.addThemeObserver(this);
    }

    /**
     * Updates the status text.
     */
    void setStatusText(final String text) {
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                STATUS_LABEL.setText(text);
            }
        });
    }

    /**
     * Notification that loading has finished.
     *
     * The loading label is removed and the donation component
     * is added if necessary.
     */
    void loadFinished() {
        CENTER.removeAll();
        if(!GUIMediator.hasDonated()) {
            CENTER.add(new StatusLinkHandler().getComponent());
        }
        PANEL.repaint();
    }

    /**
     * Sets up switchedPanel and switcher so you have
     * connect/disconnect icons.
     *
     * @modifies switchedPanel, switcher
     */
    private void createConnectPanel() {
		updateTheme();
        connectionQuality = new JPanel();
        switcher = new CardLayout();
        connectionQuality.setLayout(switcher);

        // add the quality panels to the connectionQuality panel.
        for(int i = 0; i < qualityPanels.length; i++)
            connectionQuality.add(qualityPanels[i], Integer.toString(i));

        connectionQuality.setMaximumSize(new Dimension(90, 30));
    }

	// inherit doc commment
	public void updateTheme() {
        for(int i = 0; i < qualityPanels.length; i++) {
            Icon image = GUIMediator.getThemeImage("connect_" + i);
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel();
            label.setIcon(image);
            panel.add(label);
            qualityPanels[i] = panel;
        }
		if(_mediaPlayer != null)
			_mediaPlayer.updateTheme();
	}

    /**
     * Alters the displayed connection quality.
     *
     * @modifies this
     */
    public void setConnectionQuality(int quality) {
        // make sure we don't go over our bounds.
        if(quality >= qualityPanels.length)
            quality = qualityPanels.length - 1;

        switcher.show(connectionQuality, Integer.toString(quality));

        String status = null;
        String tip = null;
        switch(quality) {
            case STATUS_DISCONNECTED:
                	status = STATISTICS_CONNECTION_DISCONNECTED;
                    tip = TIP_CONNECTION_DISCONNECTED;
                    break;
            case STATUS_CONNECTING:
                    status = STATISTICS_CONNECTION_CONNECTING;
                    tip = TIP_CONNECTION_CONNECTING;
                    break;
            case STATUS_POOR:
                    status = STATISTICS_CONNECTION_POOR;
                    tip = TIP_CONNECTION_POOR;
                    break;
            case STATUS_FAIR:
                    status = STATISTICS_CONNECTION_FAIR;
                    tip = TIP_CONNECTION_FAIR;
                    break;
            case STATUS_GOOD:
                    status = STATISTICS_CONNECTION_GOOD;
                    tip = TIP_CONNECTION_GOOD;
                    break;
            case STATUS_IDLE:
            case STATUS_EXCELLENT:
                    status = STATISTICS_CONNECTION_EXCELLENT;
                    tip = TIP_CONNECTION_EXCELLENT;
                    break;
            case STATUS_TURBOCHARGED:
                    status = STATISTICS_CONNECTION_TURBO_CHARGED;
                    tip = TIP_CONNECTION_TURBO_CHARGED;
                    break;
            //case STATUS_IDLE:
                    //status = STATISTICS_CONNECTION_IDLE;
                    //tip = null; // impossible to see this
                    //break;
            case STATUS_WAKING_UP:
                    status = STATISTICS_CONNECTION_WAKING_UP;
                    tip = TIP_CONNECTION_WAKING_UP;
                    break;
        }
        connectionStatus.setText(status);
        connectionQuality.setToolTipText(tip);
        connectionQualityText.setToolTipText(tip);
        if(quality == 0) {
            connectionStatus.setForeground(
                ThemeFileHandler.SEARCH_PRIVATE_IP_COLOR.getValue());
        } else if(quality >= 1 && quality <= 3) {
            connectionStatus.setForeground(
                ThemeFileHandler.WINDOW8_COLOR.getValue());
        } else {
            connectionStatus.setForeground(
                ThemeFileHandler.SEARCH_RESULT_SPEED_COLOR.getValue());
        }
    }

    /**
     * Sets the horizon statistics for this.
     * @modifies this
     * @return A displayable Horizon string.
     */
    public String setStatistics(long hosts, long files, long kbytes,
                                int share,  int pending) {
        //Shorten with KB/MB/GB/TB as needed.
        String txt;
        if (hosts == 0)
            txt = STATS_DISCONNECTED_STRING;
        else
            txt = GUIUtils.toUnitnumber(files, false) +
                  " " + STATS_FILE_STRING + " / " +
                  GUIUtils.toUnitbytes(kbytes * 1024) +
                  " " + STATS_AVAILABLE_STRING;

        // if nothing is pending shared, display as normal
        if( pending == 0 )
            sharingLabel.setText(STATS_SHARING_STRING + " " +
                                 String.valueOf(share) +
                                 " " + STATS_FILE_STRING);
        //otherwise display as  'shared / total'
        else
            sharingLabel.setText(STATS_SHARING_STRING + " " +
                                String.valueOf(share) + " / " +
                                String.valueOf(pending + share) +
                                " " + STATS_FILE_STRING);

        if (share == 0) {
			Color notSharingColor =
                ThemeFileHandler.NOT_SHARING_LABEL_COLOR.getValue();
			sharingLabel.setForeground(notSharingColor);
		} else
            sharingLabel.setForeground(UIManager.getColor("Label.foreground"));

        sharingLabel.setPreferredSize(
            new Dimension(GUIUtils.width(sharingLabel), 20));
        return txt;
    }


    /**
     * The String for part of the stats string, ie "available".
     */
    private static final String STATS_AVAILABLE_STRING =
        GUIMediator.getStringResource("STATISTICS_AVAILABLE");

    /**
     * The String for part of the stats string, ie "files".
     */
    private static final String STATS_FILE_STRING =
        GUIMediator.getStringResource("STATISTICS_FILES");

    /**
     * The String for part of the stats string, ie "no files...".
     */
    private static final String STATS_DISCONNECTED_STRING =
        GUIMediator.getStringResource("STATISTICS_DISCONNECTED");

    /**
     * The String for part of the stats string, ie "no files...".
     */
    private static final String STATS_SHARING_STRING =
        GUIMediator.getStringResource("STATISTICS_SHARING");

	/**
     * Disconnected tip
     */
    private static final String TIP_CONNECTION_DISCONNECTED =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_DISCONNECTED_TIP");

	/**
     * Disconnected string.
     */
    private static final String STATISTICS_CONNECTION_DISCONNECTED =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_DISCONNECTED");

    /**
     * Connecting tip
     */
    private static final String TIP_CONNECTION_CONNECTING =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_CONNECTING_TIP");

    /**
     * Connecting string
     */
    private static final String STATISTICS_CONNECTION_CONNECTING =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_CONNECTING");

    /**
     * Poor tip.
     */
    private static final String TIP_CONNECTION_POOR =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_POOR_TIP");

    /**
     * Poor string.
     */
    private static final String STATISTICS_CONNECTION_POOR =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_POOR");

	/**
     * Mediocre tip
     */
    private static final String TIP_CONNECTION_FAIR =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_FAIR_TIP");

	/**
     * Mediocre string
     */
    private static final String STATISTICS_CONNECTION_FAIR =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_FAIR");

    /**
     * Good tip
     */
    private static final String TIP_CONNECTION_GOOD =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_GOOD_TIP");

    /**
     * Good string
     */
    private static final String STATISTICS_CONNECTION_GOOD =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_GOOD");

    /**
     * Excellent tip
     */
    private static final String TIP_CONNECTION_EXCELLENT =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_EXCELLENT_TIP");

    /**
     * Excellent string
     */
    private static final String STATISTICS_CONNECTION_EXCELLENT =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_EXCELLENT");

	/**
     * Turbo charged tip
     */
    private static final String TIP_CONNECTION_TURBO_CHARGED =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_TURBO_CHARGED_TIP_" +
            (CommonUtils.isPro() ? "PRO" : "FREE") );

	/**
     * Turbo charged string
     */
    private static final String STATISTICS_CONNECTION_TURBO_CHARGED =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_TURBO_CHARGED");

    /**
     * Idle string.
     */
    private static final String STATISTICS_CONNECTION_IDLE =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_IDLE");
        
    /**
     * Waking up string.
     */
    private static final String STATISTICS_CONNECTION_WAKING_UP =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_WAKING_UP");
    
    /**
     * Waking up tip.
     */
    private static final String TIP_CONNECTION_WAKING_UP =
        GUIMediator.getStringResource("STATISTICS_CONNECTION_WAKING_UP_TIP");

    public void launchAudio(File toPlay) {
        MediaPlayerComponent.launchAudio(toPlay);
    }

    public UpdatePanel getUpdatePanel() {
        return _updatePanel;
    }

    /**
      * Accessor for the <tt>JComponent</tt> instance that contains all
      * of the panels for the status line.
      *
      * @return the <tt>JComponent</tt> instance that contains all
      *  of the panels for the status line
      */
    public JComponent getComponent() {
        return PANEL;
    }
}

