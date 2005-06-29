package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * Contains the <tt>JDialog</tt> instance that shows "about" information
 * for the application.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class AboutWindow {
	/**
	 * Constant handle to the <tt>JDialog</tt> that contains about
	 * information.
	 */
	private final JDialog DIALOG;

	/**
	 * Constant for the scolling pane of credits.
	 */
	private final ScrollingTextPane SCROLLING_PANE;

	/**
	 * Check box to specify whether to scroll or not.
	 */
	private final JCheckBox SCROLL_CHECK_BOX = 
		new JCheckBox(GUIMediator.getStringResource(
            "ABOUT_SCROLL_CHECK_BOX_LABEL"));

	/**
	 * Constructs the elements of the about window.
	 */
	AboutWindow() {
	    DIALOG = new JDialog(GUIMediator.getAppFrame());
	    
        if (!(CommonUtils.isMacOSX() && CommonUtils.isJava14OrLater()))
            DIALOG.setModal(true);

		DIALOG.setSize(new Dimension(450, 400));            
		DIALOG.setResizable(false);
		DIALOG.setTitle(GUIMediator.getStringResource("ABOUT_TITLE"));
		DIALOG.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		DIALOG.addWindowListener(new WindowAdapter() {
		    public void windowClosed(WindowEvent we) {
		        SCROLLING_PANE.stopScroll();
		    }
		    public void windowClosing(WindowEvent we) {
		        SCROLLING_PANE.stopScroll();
		    }
		});		

		String aboutName = "about_" +
		                   ResourceManager.getLocale().toString() + ".html";
        URL aboutUrl = GUIMediator.getURLResource(aboutName);
        // if nothing by that name, default to the english file.
        if(aboutUrl == null)
            aboutUrl = GUIMediator.getURLResource("about.html");            
        SCROLLING_PANE = new ScrollingTextPane(aboutUrl);
		SCROLLING_PANE.addHyperlinkListener(GUIUtils.getHyperlinkListener());

		SCROLL_CHECK_BOX.setSelected(true);
		SCROLL_CHECK_BOX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(SCROLL_CHECK_BOX.isSelected()) {
					SCROLLING_PANE.startScroll();
				} else {
					SCROLLING_PANE.stopScroll();
                }
			}
		});
		
		JComponent pane = (JComponent)DIALOG.getContentPane();
		GUIUtils.addHideAction(pane);
		
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 0, 17, 0);
		
		LogoPanel logo = new LogoPanel();
		logo.setSearching(true);
		pane.add(logo, c);

		JLabel client = new JLabel(GUIMediator.getStringResource("ABOUT_LABEL_START") +
		                          " " + CommonUtils.getLimeWireVersion());
        client.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel java = new JLabel("Java " + CommonUtils.getJavaVersion());
        java.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel url = new URLLabel("http://www.limewire.com");
        url.setHorizontalAlignment(SwingConstants.CENTER);
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 0, 0);
		pane.add(client, c);
		pane.add(java, c);
		pane.add(url, c);
		
		c.weighty = 1;
		c.weightx = 1;
		c.insets = new Insets(3, 3, 3, 3);
		pane.add(SCROLLING_PANE, c);

		c.gridwidth = GridBagConstraints.RELATIVE;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.weighty = 0;
		pane.add(SCROLL_CHECK_BOX, c);

		JButton button = new JButton(GUIMediator.getStringResource("GENERAL_CLOSE_BUTTON_LABEL"));
		DIALOG.getRootPane().setDefaultButton(button);
		button.setToolTipText(GUIMediator.getStringResource("ABOUT_BUTTON_TIP"));
		button.addActionListener(GUIUtils.getDisposeAction());
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.EAST;
		pane.add(button, c);
		
	}

	/**
	 * Displays the "About" dialog window to the user.
	 */
	void showDialog() {
		if(GUIMediator.isAppVisible())
			DIALOG.setLocationRelativeTo(GUIMediator.getAppFrame());
		else {
			DIALOG.setLocation(GUIMediator.getScreenCenterPoint(DIALOG));
		}

		if(SCROLL_CHECK_BOX.isSelected()) {
			ActionListener startTimerListener = new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
				    //need to check isSelected() again,
				    //it might have changed in the past 10 seconds.
				    if(SCROLL_CHECK_BOX.isSelected()) {
				        //activate scroll timer
					    SCROLLING_PANE.startScroll();
					}
				}
			};
			
			Timer startTimer = new Timer(10000, startTimerListener);
			startTimer.setRepeats(false);			
			startTimer.start();
		}
		DIALOG.setVisible(true);
	}

	/*
	public static void main(String[] args) {
	    AboutWindow aw = new AboutWindow();
	    aw.showDialog();
	}
	*/
}
