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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import axoloti.PatchGUI;
import axoloti.utils.KeyUtils;

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
    private JCheckBox searchNameBox;
    private JCheckBox searchLabelBox;
    private JCheckBox searchAttributesBox;
    private JCheckBox searchParametersBox;
    private JCheckBox searchIoletsBox;

    public static final int FIND_TARGET_NAME        = 1;    /* Bit 0: Object TypeName/ID */
    public static final int FIND_TARGET_LABEL       = 2;    /* Bit 1: Instance Name (Label) */
    public static final int FIND_TARGET_ATTRIBUTES  = 4;    /* Bit 2: AttributeInstance Names */
    public static final int FIND_TARGET_PARAMETERS  = 8;    /* Bit 3: ParameterInstance Names */
    public static final int FIND_TARGET_IOLETS      = 16;   /* Bit 4: Inlet/Outlet Names */

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

        searchTimer = new Timer(250, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FindTextDialog.this.runLiveSearch(); 
            }
        });
        searchTimer.setRepeats(false);

        add(new JLabel("Find text:"));
        add(searchField);
        add(prevButton);
        add(nextButton);
        add(resultLabel);

        searchNameBox = new JCheckBox("Name");
        searchLabelBox = new JCheckBox("Label");
        searchAttributesBox = new JCheckBox("Attributes");
        searchParametersBox = new JCheckBox("Parameters");
        searchIoletsBox = new JCheckBox("Iolets");

        ActionListener checkboxListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FindTextDialog.this.runLiveSearch();
            }
        };
        searchNameBox.addActionListener(checkboxListener);
        searchLabelBox.addActionListener(checkboxListener);
        searchAttributesBox.addActionListener(checkboxListener);
        searchParametersBox.addActionListener(checkboxListener);
        searchIoletsBox.addActionListener(checkboxListener);

        JPanel targetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetsPanel.add(searchNameBox);
        targetsPanel.add(searchLabelBox);
        targetsPanel.add(searchAttributesBox);
        targetsPanel.add(searchParametersBox);
        targetsPanel.add(searchIoletsBox);
        add(targetsPanel); 

        searchNameBox.setSelected(true);
        searchLabelBox.setSelected(true);
        searchAttributesBox.setSelected(true);
        searchParametersBox.setSelected(true);
        searchIoletsBox.setSelected(true);

        searchNameBox.setToolTipText("Search in Object TypeName/ID fields ");
        searchLabelBox.setToolTipText("Search in Object Instance Label fields");
        searchAttributesBox.setToolTipText("Search in Object Attribute name fields");
        searchParametersBox.setToolTipText("Search in Object Parameter name fields");
        searchIoletsBox.setToolTipText("Search in Object Inlet/Outlet name fields");

        pack();
        setResizable(false);

        resultLabel.setText("0/0");

        AbstractAction selectAllAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow(); 
                searchField.selectAll();
            }
        };
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyUtils.CONTROL_OR_CMD_MASK), "SELECT_ALL_SEARCH_TEXT");
        this.getRootPane().getActionMap().put("SELECT_ALL_SEARCH_TEXT", selectAllAction);

        AbstractAction closeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FindTextDialog.this.setVisible(false); 
            }
        };
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_DIALOG");
        this.getRootPane().getActionMap().put("CLOSE_DIALOG", closeAction);

        AbstractAction nextAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                patchGUI.findAndHighlight(searchField.getText(), 1, FindTextDialog.this, getSearchCheckmask());
            }
        };
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "NEXT_RESULT");
        this.getRootPane().getActionMap().put("NEXT_RESULT", nextAction);

        AbstractAction prevAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                patchGUI.findAndHighlight(searchField.getText(), -1, FindTextDialog.this, getSearchCheckmask());
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
        int checkmask = getSearchCheckmask();
        patchGUI.findAndHighlight(text, 0, this, checkmask);
    }

    private void triggerSearch() {
        if (searchField.getText().isEmpty()) {
            searchTimer.stop();
            FindTextDialog.this.runLiveSearch(); /* trigger search immediately, clearing the highlights */
        } else {
            searchTimer.restart(); 
        }
    }

    private int getSearchCheckmask() {
        int mask = 0;
        if (searchNameBox.isSelected()) {
            mask |= FIND_TARGET_NAME;
        }
        if (searchLabelBox.isSelected()) {
            mask |= FIND_TARGET_LABEL;
        }
        if (searchAttributesBox.isSelected()) {
            mask |= FIND_TARGET_ATTRIBUTES;
        }
        if (searchParametersBox.isSelected()) {
            mask |= FIND_TARGET_PARAMETERS;
        }
        if (searchIoletsBox.isSelected()) {
            mask |= FIND_TARGET_IOLETS;
        }
        return mask;
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
            patchGUI.findAndHighlight(searchField.getText(), 1, this, getSearchCheckmask());
        } else if (e.getSource() == prevButton) {
            patchGUI.findAndHighlight(searchField.getText(), -1, this, getSearchCheckmask());
        }
    }
}
