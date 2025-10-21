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
package axoloti;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;

// import javax.swing.UIManager;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.*;

import axoloti.utils.Constants;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;
import axoloti.utils.StringRef;


/**
 *
 * @author Johannes Taelman
 */
public class TextEditor extends javax.swing.JFrame implements DocumentWindow {

    StringRef s;
    RSyntaxTextArea textArea;
    final DocumentWindow parent;

    /**
     * Creates new form TextEditor
     *
     * @param s initial string
     */
    public TextEditor(StringRef s, DocumentWindow parent) {
        initComponents();
        this.parent = parent;
        this.s = s;
        textArea = new RSyntaxTextArea(20, 60);
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(
                "/resources/rsyntaxtextarea/themes/" + Preferences.getInstance().getCodeSyntaxTheme() + ".xml"));
            theme.apply(textArea);
        } catch (IOException ioe) { // Never happens
            ioe.printStackTrace(System.out);
        }
        textArea.setFont(Constants.FONT_MONO);
        // textArea.setBackground(axoloti.Theme.Object_Default_Background);
        // textArea.setForeground(axoloti.Theme.Object_Default_Foreground);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        textArea.setHighlightCurrentLine(false);
        textArea.setLineWrap(true);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setMarkOccurrences(true);
        textArea.setPaintTabLines(true);
        textArea.setMarkOccurrencesColor(new Color(0x00,0x00,0x00, 0x40));

        RTextScrollPane sp = new RTextScrollPane(textArea);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getHorizontalScrollBar().setUnitIncrement(10);
        sp.getVerticalScrollBar().setUnitIncrement(10);

        cp.setLayout(new BorderLayout());
        cp.add(sp);

        textArea.setVisible(true);
        setContentPane(cp);
        textArea.setText(s.s);
        setIconImage(Constants.APP_ICON.getImage());

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyUtils.CONTROL_OR_CMD_MASK), "closeEditor");

        getRootPane().getActionMap().put("closeEditor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Close();
            }
        });
    }

    public void SetText(String s) {
        textArea.setText(s);
    }

    public String GetText() {
        return textArea.getText();
    }

    public void Close() {
        parent.GetChildDocuments().remove(this);
        dispose();
    }

    public void disableSyntaxHighlighting() {
        textArea.setSyntaxEditingStyle(null);
    }


    private void initComponents() {

        cp = new javax.swing.JPanel();

        setMinimumSize(new java.awt.Dimension(256, 128));
        setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                formComponentHidden(evt);
            }
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                formWindowLostFocus(evt);
            }
        });

        javax.swing.GroupLayout cpLayout = new javax.swing.GroupLayout(cp);
        cp.setLayout(cpLayout);
        cpLayout.setHorizontalGroup(
            cpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 365, Short.MAX_VALUE)
        );
        cpLayout.setVerticalGroup(
            cpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 257, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    private void formWindowLostFocus(java.awt.event.WindowEvent evt) {
//        System.out.println("txt changed (lost focus)");
//        attr.sText = jEditorPane1.getText();
        s.s = textArea.getText();
    }

    private void formComponentHidden(java.awt.event.ComponentEvent evt) {
        parent.GetChildDocuments().remove(this);
    }

    private void formComponentShown(java.awt.event.ComponentEvent evt) {
        parent.GetChildDocuments().add(this);
    }

    private javax.swing.JPanel cp;

    @Override
    public JFrame GetFrame() {
        return this;
    }

    @Override
    public boolean AskClose() {
        Close();
        return false; //TBC
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public ArrayList<DocumentWindow> GetChildDocuments() {
        return null;
    }
}
