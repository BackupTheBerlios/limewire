package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.metal.MetalLabelUI;

import com.limegroup.gnutella.version.UpdateInformation;

final class UpdatePanel extends JPanel implements RefreshListener {

    private final String labelString = 
        GUIMediator.getStringResource("UPDATE_MESSAGE_SMALL");

    private JLabel LABEL;
    
    private boolean visible;

    private boolean blink;
    
    /**
     * The most recent UpdateInformation we know about.
     */
    private UpdateInformation info;

    UpdatePanel() {
         LABEL = new JLabel(labelString, SwingConstants.CENTER);
        //make the font so that it looks like a link
        LABEL.setUI(new LinkLabelUI());
		FontMetrics fm = LABEL.getFontMetrics(LABEL.getFont());
  		int width = fm.stringWidth(labelString);
  		Dimension dim = new Dimension(width, fm.getHeight());
        //link color, could grab system attribute as well
		LABEL.setForeground(Color.red); 
  		LABEL.setPreferredSize(dim);
  		LABEL.setMaximumSize(dim);

        //add a mouse listener
        LABEL.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                UpdatePanel.this.handleClick();
            }
            //change cursor, we are on a link
            public void mouseEntered(MouseEvent e) { 
                if(visible) 
                    e.getComponent().setCursor
                    (Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
            //change back to normal
			public void mouseExited(MouseEvent e) {
                if(visible)
                    e.getComponent().setCursor(Cursor.getDefaultCursor()); 
			}
        });

        //add the link
        add(LABEL);
        GUIMediator.addRefreshListener(this);
        //keep it invisible
        setVisible(false);
        visible = false;
        blink = false;
    }
    
    public void makeVisible(boolean blink, UpdateInformation info) {
        visible = true;
        this.blink = blink;
        this.info = info;
        super.setVisible(true);
    }
    

    private void handleClick() {
        if(!visible) //not visible? no update yet
            return;
            
        if(info != null) {
            if(blink)
                new UpdateDialog(info).setVisible(true);
            else
                GUIMediator.openURL(info.getUpdateURL());
        }
    }
    
    public void refresh() {
        if(!visible || !blink)
            return;
        Color currCol = LABEL.getForeground();
        if(currCol.equals(Color.red))
           LABEL.setForeground(Color.black);
        if(currCol.equals(Color.black))
           LABEL.setForeground(Color.red);           
    }


    private class LinkLabelUI extends MetalLabelUI {
        /**
         * Paint clippedText at textX, textY with the labels foreground color.
         * 
         * @see #paint
         * @see #paintDisabledText
         */
        protected void paintEnabledText(JLabel l, Graphics g, String s, 
                                        int textX, int textY) {
            super.paintEnabledText(l, g, s, textX, textY);
			if (LABEL.getText() == null)  return;
			
			FontMetrics fm = g.getFontMetrics();
			g.fillRect(textX, fm.getAscent()+2, 
                       fm.stringWidth(LABEL.getText()) - 
					   LABEL.getInsets().right, 1); //X,Y,WIDTH,HEIGHT
            
        }        
    }
}


