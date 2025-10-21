/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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

package axoloti.dialogs;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        searchField = new JTextField(20);
        searchField.getDocument().addDocumentListener(this);
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none"
        );

        prevButton = new JButton("<");
        prevButton.addActionListener(this);

        nextButton = new JButton(">");
        nextButton.addActionListener(this);

        resultLabel = new JLabel("99/99"); /* Set largest width until pack() call below */

        JPanel searchRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRowPanel.add(new JLabel("Find text:"));
        searchRowPanel.add(searchField);
        searchRowPanel.add(prevButton);
        searchRowPanel.add(nextButton);
        searchRowPanel.add(resultLabel);

        ActionListener checkboxListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FindTextDialog.this.runLiveSearch();
            }
        };

        searchNameBox = new JCheckBox("Name");
        searchNameBox.setMnemonic(KeyEvent.VK_N);
        searchNameBox.setSelected(true);
        searchNameBox.addActionListener(checkboxListener);
        searchNameBox.setToolTipText("Search in object TypeName/ID fields.\nPress ALT+N to toggle.");

        searchLabelBox = new JCheckBox("Label");
        searchLabelBox.setMnemonic(KeyEvent.VK_L);
        searchLabelBox.setSelected(true);
        searchLabelBox.addActionListener(checkboxListener);
        searchLabelBox.setToolTipText("Search in object Instance Label fields.\nPress ALT+L to toggle.");

        searchAttributesBox = new JCheckBox("Attributes");
        searchAttributesBox.setMnemonic(KeyEvent.VK_A);
        searchAttributesBox.setSelected(true);
        searchAttributesBox.addActionListener(checkboxListener);
        searchAttributesBox.setToolTipText("Search in bject Attribute name fields.\nPress ALT+A to toggle.");

        searchParametersBox = new JCheckBox("Parameters");
        searchParametersBox.setMnemonic(KeyEvent.VK_P);
        searchParametersBox.setSelected(true);
        searchParametersBox.addActionListener(checkboxListener);
        searchParametersBox.setToolTipText("Search in object Parameter name fields.\nPress ALT+P to toggle.");

        searchIoletsBox = new JCheckBox("Iolets");
        searchIoletsBox.setMnemonic(KeyEvent.VK_I);
        searchIoletsBox.setSelected(true);
        searchIoletsBox.addActionListener(checkboxListener);
        searchIoletsBox.setToolTipText("Search in object Inlet/Outlet name fields.\nPress ALT+I to toggle.");

        JPanel targetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetsPanel.add(searchNameBox);
        targetsPanel.add(searchLabelBox);
        targetsPanel.add(searchAttributesBox);
        targetsPanel.add(searchParametersBox);
        targetsPanel.add(searchIoletsBox);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 0, 5);
        gbc.weightx = 1.0;
        add(searchRowPanel, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        gbc.weightx = 1.0; 
        add(targetsPanel, gbc);

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
        runLiveSearch();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        runLiveSearch();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        runLiveSearch();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == nextButton) { 
            patchGUI.findAndHighlight(searchField.getText(), 1, this, getSearchCheckmask());
        } else if (e.getSource() == prevButton) {
            patchGUI.findAndHighlight(searchField.getText(), -1, this, getSearchCheckmask());
        }
    }
}
