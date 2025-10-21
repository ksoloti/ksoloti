/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */

package axoloti.listener;

import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

/**
 *
 * @author Ksoloti
 */
public class LineLimitListener implements DocumentListener {
    private final static Logger LOGGER = Logger.getLogger(LineLimitListener.class.getName());
    private final JTextPane textPane;
    private final int MAX_LOG_LINES;
    private final int LINES_TO_DELETE = 100;

    public static class StyledLogRecord {
        public String text;
        public AttributeSet style;

        public StyledLogRecord(String text, AttributeSet style) {
            this.text = text;
            this.style = style;
        }
    }

    public LineLimitListener(JTextPane textPane, int maxLines) {
        this.textPane = textPane;
        this.MAX_LOG_LINES = maxLines;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(this::processLimitCheck);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    private void processLimitCheck() {
        try {
            StyledDocument doc = textPane.getStyledDocument();
            Element root = doc.getDefaultRootElement();
            int lineCount = root.getElementCount();

            while (lineCount > MAX_LOG_LINES) {
                
                int linesToRemove = Math.min(lineCount - MAX_LOG_LINES + LINES_TO_DELETE, LINES_TO_DELETE);
                int deletionEndOffset = 0;
                
                if (linesToRemove > 0 && linesToRemove <= root.getElementCount()) {
                    Element lastElementToRemove = root.getElement(linesToRemove - 1);
                    deletionEndOffset = lastElementToRemove.getEndOffset();
                }
                
                if (deletionEndOffset > 0) {
                    doc.remove(0, deletionEndOffset); 
                    
                    lineCount = root.getElementCount();
                } else {
                    break; 
                }
            }
        } 
        catch (BadLocationException ex) {
            LOGGER.log(Level.SEVERE, "LineLimitListener: BadLocationException during line removal: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "LineLimitListener: Unexpected error in processLimitCheck: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }
}