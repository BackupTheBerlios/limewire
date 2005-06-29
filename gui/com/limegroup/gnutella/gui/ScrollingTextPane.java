package com.limegroup.gnutella.gui;

import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Timer;
import javax.swing.event.HyperlinkListener;

/**
 * Extend <tt>JScrollPane</tt> so that a scrolled html file is shown
 */
public final class ScrollingTextPane extends JScrollPane {

    /**
     * <tt>JEditorPane</tt> to show the text
     */
    private final JEditorPane EDITOR_PANE;

    /**
     * Timer to control scrolling
     */
    protected Timer _timer;

    /**
     * Constructs the elements of the about window.
     *
     * @param aboutUrl the url to load into the scrolling pane
     * @throws <tt>NullPointerException</tt> if the attempt to load the
     *  file fails
     */
    public ScrollingTextPane(URL aboutUrl) {
	if(aboutUrl == null) {
	    throw new NullPointerException("null url: "+aboutUrl);
	}

	try {
	    EDITOR_PANE = new JEditorPane(aboutUrl);
	} catch(IOException ioe) {
            throw new NullPointerException("could not load url: "+aboutUrl);
	}
	EDITOR_PANE.setMargin(new Insets(5, 5, 5, 5));
		
	// don't allow edit of editor pane - use it just as a viewer
	EDITOR_PANE.setEditable(false);
		
	// add it to the scrollpane
	JViewport vp = getViewport();
	vp.add(EDITOR_PANE);

	// enable double buffering for smooth scroll effect
	this.setDoubleBuffered(true);

        // create timer
        Action scrollText = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    scroll();
		}
	    };

        _timer = new Timer(40, scrollText);
    }

    /**
     * Shows the file of the specified file name, throwing a 
     * <tt>NullPointerExeption</tt> if the file could not be found
     * in the com/limegroup/gnutella/gui/resources directory.
     *
     * @param FILE_NAME the name of the file to load into the scrolling pane
     * @throws <tt>NullPointerExeption</tt> if the attempt to load the
     *  file fails
     */
    public void showFile(String FILE_NAME) {
	URL url = GUIMediator.getURLResource(FILE_NAME);
	if(url == null) {
	    throw new NullPointerException("FILE COULD NOT BE LOADED");			
	}
	try {
	    EDITOR_PANE.setPage(url);
	} catch(IOException ioe) {
	    throw new NullPointerException("FILE COULD NOT BE LOADED");			
	}
    }

    /**
     * Start scrolling.
     */
    public void startScroll() {
	// start the timer
	_timer.start();
    }

    /**
     * Stop scrolling.
     */
    public void stopScroll() {
	// deactivate timer
	_timer.stop();
    }

    /**
     * Scroll the content of the JEditorPane
     */
    protected void scroll() {
	// calculate visible rectangle
	Rectangle rect = EDITOR_PANE.getVisibleRect();

	// get x / y values
	int x = rect.x;

	int y = this.getVerticalScrollBar().getValue(); 

	if((y+rect.height) >= EDITOR_PANE.getHeight()) {
	    return;
	}
	else {
	    y += 1;
	}

	Rectangle rectNew = 
	    new Rectangle(x, y,(x + rect.width), 
			  (y + rect.height));


	// scroll to current position
	EDITOR_PANE.scrollRectToVisible(rectNew);
    }

    /**
     * Adds a <tt>HyperlinkListener</tt> instance to the underlying
     * <tt>JEditorPane</tt> instance.
     *
     * @param listener the listener for hyperlinks
     */
    public void addHyperlinkListener(HyperlinkListener listener) {
	EDITOR_PANE.addHyperlinkListener(listener);
    }
}
