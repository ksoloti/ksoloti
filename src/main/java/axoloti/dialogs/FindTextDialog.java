/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2024 by Ksoloti
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

package axoloti.dialogs;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import axoloti.PatchGUI;

/**
 * 
 * @author Ksoloti
 */
public class FindTextDialog extends JDialog implements ActionListener, DocumentListener {
    private JTextField searchField;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel resultLabel;
    private PatchGUI patchGUI;
    private Timer searchTimer;

    public FindTextDialog(PatchGUI patchGUI) {
        super(patchGUI.getPatchframe(), "Find in " + patchGUI.getPatchframe().getTitle(), false);
        this.patchGUI = patchGUI;
        setLayout(new FlowLayout());

        searchField = new JTextField(20);
        prevButton = new JButton("<");
        nextButton = new JButton(">");
        resultLabel = new JLabel("99/99"); /* Set largest width until pack() call below */

        searchField.getDocument().addDocumentListener(this);
        prevButton.addActionListener(this);
        nextButton.addActionListener(this);
        searchField.addActionListener(this);

        searchTimer = new Timer(250, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                FindTextDialog.this.runLiveSearch(); 
            }
        });
        searchTimer.setRepeats(false);

        add(new JLabel("Find text:"));
        add(searchField);
        add(prevButton);
        add(nextButton);
        add(resultLabel);

        pack();
        setResizable(false);

        resultLabel.setText("0/0");

        AbstractAction closeAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                FindTextDialog.this.setVisible(false); 
            }
        };
        this.getRootPane().getActionMap().put("CLOSE_DIALOG", closeAction);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_DIALOG");

        AbstractAction nextAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                patchGUI.findAndHighlight(searchField.getText(), 1, FindTextDialog.this);
            }
        };
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "NEXT_RESULT");
        this.getRootPane().getActionMap().put("NEXT_RESULT", nextAction);

        AbstractAction prevAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                patchGUI.findAndHighlight(searchField.getText(), -1, FindTextDialog.this);
            }
        };
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "PREV_RESULT");
        this.getRootPane().getActionMap().put("PREV_RESULT", prevAction);
    }

    public void updateResults(int currentIndex, int totalCount) {
        if (totalCount > 0) {
            resultLabel.setText((currentIndex + 1) + "/" + totalCount);
        } else {
            resultLabel.setText("0/0");
        }
    }

    public void showAndFocus() {
        this.setVisible(true);
        this.toFront();
        searchField.requestFocusInWindow(); 
        searchField.selectAll(); 
    }

    private void runLiveSearch() {
        String text = searchField.getText();
        
        /* Only trigger the search if the text length is 2 or more */
        if (text.length() > 1) { 
            patchGUI.findAndHighlight(text, 0, this); 
        } else if (text.isEmpty()) {
            patchGUI.findAndHighlight("", 0, this);
        }
    }

    private void triggerSearch() {
        searchTimer.restart(); 
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        triggerSearch();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        triggerSearch();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        triggerSearch();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == searchField || e.getSource() == nextButton) {
            patchGUI.findAndHighlight(searchField.getText(), 1, this);
        } else if (e.getSource() == prevButton) {
            patchGUI.findAndHighlight(searchField.getText(), -1, this);
        }
    }
}
