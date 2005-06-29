package com.limegroup.gnutella.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.limegroup.gnutella.settings.StartupSettings;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.util.CommonUtils;


public final class TipOfTheDayMediator implements ThemeObserver {
    
    /**
     * The instance of this class.
     */
    private static TipOfTheDayMediator instance;
    
    /**
     * The title for the TOTD window.
     */
    private static final String TOTD_TITLE =
        GUIMediator.getStringResource("TOTD_TITLE");
        
    /**
     * The 'Did You Know' intro.
     */
    private static final String TOTD_INTRO =
        GUIMediator.getStringResource("TOTD_INTRODUCTION");
        
    /**
     * The 'Show Tips At Startup' string
     */
    private static final String TOTD_STARTUP =
        GUIMediator.getStringResource("TOTD_SHOW_AT_STARTUP");
        
    /**
     * 'Next'.
     */
    private static final String TOTD_NEXT =
        GUIMediator.getStringResource("TOTD_NEXT");
        
    /**
     * 'Previous'.
     */
    private static final String TOTD_PREVIOUS =
        GUIMediator.getStringResource("TOTD_PREVIOUS");
        
    /**
     * 'Close'.
     */
    private static final String TOTD_CLOSE =
        GUIMediator.getStringResource("TOTD_CLOSE");
    
    /**
     * The actual TOTD JDialog.
     */
    private final JDialog TOTD = new JDialog();
    
    /**
     * The JTextComponent that displays the tip.
     */
    private final JEditorPane TIP = new JEditorPane();
    
    /**
     * The 'Previous' JButton.  Global so it can be
     * enabled/disabled.
     */
    private final JButton PREVIOUS;
    
    /**
     * The prefix to use for general tips.
     */
    private static final String GENERAL = "GENERAL_";
    
    /**
     * The prefix to use for OSX tips.
     */
    private static final String OSX = "OSX_";
    
    /**
     * The prefix to use for Windows tips.
     */
    private static final String WINDOWS = "WINDOWS_";
    
    /**
     * The prefix to use for Linux tips.
     */
    private static final String LINUX = "LINUX_";
    
    /**
     * The prefix to use for other all other OS' tips.
     */
    private static final String OTHER = "OTHER_";
    
    /**
     * The prefix to use for non OSX tips.
     */
    private static final String NOT_OSX = "NOT_OSX_";
    
    /**
     * The prefix to use for Pro tips.
     */
    private static final String PRO = "PRO_";
    
    /**
     * The prefix to use for Free tips.
     */
    private static final String FREE = "FREE_";
    
    /**
     * The list of keys that are valid in the resource bundle.
     */
    private static final List KEYS = new ArrayList();
    
    /**
     * The index of the current tip.
     */
    private static int _currentTip;
    
    /**
     * The foreground color to use for text.
     */
    private static Color foreground;
    
    /**
     * Whether or not we can display the TOTD dialog.
     */
    private boolean _canDisplay = true;    
    
        
    /**
     * Private constructor that initiates the appropriate things for the TOTD.
     */
    private TipOfTheDayMediator() {
        retrieveKeys();
        
        TOTD.setModal(false);
        TOTD.setResizable(false);
        TOTD.setTitle(TOTD_TITLE);
        GUIUtils.addHideAction((JComponent)TOTD.getContentPane());
        
        // Previous' listener must be added here instead of
        // in constructDialog because otherwise multiple
        // listeners will be added when the theme changes.
        PREVIOUS = new JButton(TOTD_PREVIOUS);
        PREVIOUS.addActionListener(new PreviousTipListener());        
        constructDialog();
        TIP.setText("...loading tips");
        ThemeMediator.addThemeObserver(this);
    }
        
    /**
     * Returns the sole instance of this class.
     */
    public static TipOfTheDayMediator instance() {
        if( instance == null )
            instance = new TipOfTheDayMediator();
        return instance;
    }
    
    /**
     * Retrieves the TOTD Dialog.
     */
    public JDialog getDialog() {
        return TOTD;
    }
    
    /**
     * Causes the TOTD window to become visible.
     */
    public void displayTipWindow() {
        if(!_canDisplay)
            return;
        
        if(TOTD.isShowing()) {
            TOTD.hide();
            TOTD.show();
            TOTD.toFront();
            return;
        }
        
        if(GUIMediator.isAppVisible())
            TOTD.setLocationRelativeTo(GUIMediator.getAppFrame());
        else
            TOTD.setLocation(GUIMediator.getScreenCenterPoint(TOTD));
    		
        TOTD.show();
        if(!"text/html".equals(TIP.getContentType())) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    TIP.setContentType("text/html");
                    setText(getRandomTip());
                }
            });
        }
        
        TOTD.toFront();
    }
    
    /**
     * Hides the TOTD dialogue window.
     */
    public void hide() {
        TOTD.hide();
    }
    
    /**
     * Sets the text of the tip to a new tip.
     */
    private void setText(String tip) {
        int r = foreground.getRed();
        int g = foreground.getGreen();
        int b = foreground.getBlue();
        String foreHex = toHex(r) + toHex(g) + toHex(b);
        TIP.setText("<html><body text='#" + foreHex + "'>" + tip + "</html>");
        TIP.setCaretPosition(0);
    }
    
    /**
     * Returns the int as a hex string.
     */
    private String toHex(int i) {
        String hex = Integer.toHexString(i).toUpperCase();
        if(hex.length() == 1)
            return "0" + hex;
        else
            return hex;
    }
    
    /**
     * Iterates through all the tips' keys and stores the ones
     * that are valid for this OS.
     */
    private void retrieveKeys() {
        ResourceBundle bundle = ResourceManager.getTOTDResourceBundle();
        Enumeration e = bundle.getKeys();
        while(e.hasMoreElements()) {
            final String k = (String)e.nextElement();
            if(k.startsWith(GENERAL))
                KEYS.add(k);
            else if(CommonUtils.isWindows() && k.startsWith(WINDOWS))
                KEYS.add(k);
            else if(CommonUtils.isMacOSX() && k.startsWith(OSX))
                KEYS.add(k);
            else if(CommonUtils.isLinux() && k.startsWith(LINUX))
                KEYS.add(k);
            else if(!CommonUtils.isWindows() &&
                    !CommonUtils.isMacOSX() &&
                    !CommonUtils.isLinux() &&
                    k.startsWith(OTHER))
                KEYS.add(k);
            else if(!CommonUtils.isMacOSX() && k.startsWith(NOT_OSX))
                KEYS.add(k);
            else if(CommonUtils.isPro() && k.startsWith(PRO))
                KEYS.add(k);
            else if(!CommonUtils.isPro() && k.startsWith(FREE))
                KEYS.add(k);
        }
        
        // randomize the list.
        Collections.shuffle(KEYS);
        _currentTip = -1;
    }
    
    /**
     * Retrieves a random tip and updates the _currentTip
     * index to that tip.
     */
    private String getRandomTip() {
        // If this is our last key, reshuffle them.
        if(_currentTip == KEYS.size() - 1) {
            Collections.shuffle(KEYS);
            _currentTip = -1;
        } else if(_currentTip < -1)
            _currentTip = -1;

        String k = (String)KEYS.get(++_currentTip);
        
        if(_currentTip == 0)
            PREVIOUS.setEnabled(false);
        else
            PREVIOUS.setEnabled(true);
        
        ResourceBundle bundle = ResourceManager.getTOTDResourceBundle();
        return bundle.getString(k);
    }
    
    /**
     * Recreates the dialog box to update the theme.
     */
    public void updateTheme() {
        boolean wasShowing = TOTD.isShowing();
        
        TOTD.hide();
        TOTD.getContentPane().removeAll();
        // Lower the size of the font in the TIP because
        // it's going to get larger again.
        Font tipFont = new Font(
            TIP.getFont().getName(),
            TIP.getFont().getStyle(),
            TIP.getFont().getSize()-2);
        TIP.setFont(tipFont);
        constructDialog();
        TIP.setContentType("text/html");
        setText(getRandomTip());
        
        if(wasShowing) {
            TOTD.show();
            TOTD.toFront();
        }
    }
    
    /**
     * Builds the TOTD dialog.
     */
    private void constructDialog() {
        JPanel imagePanel = new JPanel();
        imagePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, 
                         ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue()));
        JLabel img = new JLabel(GUIMediator.getThemeImage("question"));
        imagePanel.add(img);
        
        JPanel didYouKnowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel didYouKnow = new JLabel(TOTD_INTRO);
        Font didYouKnowFont = new Font(
            "Dialog",
            didYouKnow.getFont().getStyle(),
            didYouKnow.getFont().getSize()+5);
        didYouKnow.setFont(didYouKnowFont);
        didYouKnowPanel.add(Box.createHorizontalStrut(3));
        didYouKnowPanel.add(didYouKnow);
        
        JPanel tipPanel = new JPanel();
        foreground = didYouKnow.getForeground();
        tipPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                       ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue()));
        // THE HTML ENGINE TAKES TOO LONG TO LOAD, SO WE MUST LOAD AS TEXT.
        TIP.setContentType("text");
        TIP.setEditable(false);
        TIP.setBackground(tipPanel.getBackground());
        Font tipFont = new Font(
            "Dialog",
            TIP.getFont().getStyle(),
            TIP.getFont().getSize()+2);
        TIP.setFont(tipFont);
        TIP.addHyperlinkListener(GUIUtils.getHyperlinkListener());
        JScrollPane tipScroller = new JScrollPane(TIP);
        tipScroller.setPreferredSize(new Dimension(400, 100));
        tipScroller.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tipScroller.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tipScroller.setBorder(null);
        tipPanel.add(tipScroller);
        
        BoxPanel rightTip = new BoxPanel();
        rightTip.add(Box.createVerticalStrut(10));
        rightTip.add(didYouKnowPanel);
        rightTip.add(tipPanel);
        
        JPanel wholeTip = new JPanel(new BorderLayout());
        BoxPanel innerTip = new BoxPanel(BoxPanel.X_AXIS);
        innerTip.add(imagePanel);
        innerTip.add(rightTip);
        innerTip.setBorder(BorderFactory.createLoweredBevelBorder());
        wholeTip.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
        wholeTip.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
        wholeTip.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
        wholeTip.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);
        wholeTip.add(innerTip, BorderLayout.CENTER);
        
        JPanel startupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox showTips = new JCheckBox(TOTD_STARTUP);
        showTips.setSelected(StartupSettings.SHOW_TOTD.getValue());
        startupPanel.add(showTips);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(PREVIOUS);
        JButton next = new JButton(TOTD_NEXT);
        buttonPanel.add(next);
        JButton close = new JButton(TOTD_CLOSE);
        buttonPanel.add(close);
        
        JPanel navigation = new JPanel(new BorderLayout());
        navigation.add(startupPanel, BorderLayout.WEST);
        navigation.add(buttonPanel, BorderLayout.EAST);
        
        showTips.addActionListener(new ShowTipListener());
        next.addActionListener(new NextTipListener());
        close.addActionListener(GUIUtils.getDisposeAction());
        
        TOTD.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        Container pane = TOTD.getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(wholeTip);
        pane.add(Box.createVerticalStrut(5));
        pane.add(navigation);
        try {
            TOTD.pack();
        } catch(OutOfMemoryError oome) {
            // who knows why it happens, but it's an internal error.
            _canDisplay = false;
        }
    }
    
    /**
     * A listener for changing the state of the 'Show Tips on Startup'.
     */
    private class ShowTipListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JCheckBox source = (JCheckBox)e.getSource();
            StartupSettings.SHOW_TOTD.setValue(source.isSelected());
        }
    }
    
    /**
     * A listener for showing the next tip.
     */
    private class NextTipListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            setText(getRandomTip());
        }
    }
    
    /**
     * A listener for showing the previous tip.
     */
    private class PreviousTipListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            _currentTip = _currentTip - 2;
            setText(getRandomTip());
        }
    }
}

