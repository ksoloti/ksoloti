/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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

package axoloti.objecteditor;

import axoloti.MainFrame;
import static axoloti.MainFrame.axoObjects;
import static axoloti.utils.FileUtils.toUnixPath;

import axoloti.object.AxoObject;
import axoloti.object.AxoObjects;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Preferences;

import java.awt.Color;
import java.io.File;

import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 *
 * @author kodiak
 */
public class AddToLibraryDlg extends javax.swing.JDialog {

    private final AxoObject obj;

    private Box.Filler filler1;
    private JButton jButtonCancel;
    private JButton jButtonOK;
    private JComboBox<String> jComboBoxLibrary;
    private JLabel jLabelFullPath;
    private JLabel jLabelLibrary;
    private JLabel jLabelName;
    private JSeparator jSeparator1;
    private JTextField jTextFieldFullPath;
    private JTextField jTextFieldObjectName;

    public AddToLibraryDlg(AxoObjectEditor parent, boolean modal, AxoObject obj) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(parent);

        this.obj = new AxoObject();
        this.obj.copy(obj);
        populateFields();

        jTextFieldObjectName.getDocument().addDocumentListener(new DocumentListener() {
            void Update() {
                modifiedData();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                Update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                Update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                Update();
            }
        });

        ((AbstractDocument) jTextFieldObjectName.getDocument()).setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(DocumentFilter.FilterBypass fb, int offset,
                    String string, AttributeSet attr)
                    throws BadLocationException {
                string = toUnixPath(string);
                if (offset == 0) {
                    while (string.length() > 0) {
                        if (string.charAt(0) <= 0x20) {
                            string = string.substring(1);
                        } else {
                            break;
                        }
                    }
                }
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void replace(DocumentFilter.FilterBypass fb,
                    int offset, int length, String string, AttributeSet attr) throws BadLocationException {
                if (length > 0) {
                    fb.remove(offset, length);
                }
                insertString(fb, offset, string, attr);
            }
        });
    }

    private void initComponents() {
        filler1 = new Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        jButtonCancel = new JButton();
        jButtonOK = new JButton();
        jComboBoxLibrary = new JComboBox<String>();
        jLabelFullPath = new JLabel();
        jLabelLibrary = new JLabel();
        jLabelName = new JLabel();
        jSeparator1 = new JSeparator();
        jTextFieldFullPath = new JTextField();
        jTextFieldObjectName = new JTextField();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        jLabelName.setText("Name");

        jTextFieldObjectName.setText("jTextField");
        jTextFieldObjectName.setPreferredSize(new java.awt.Dimension(120, 28));
        jTextFieldObjectName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jObjectNameFocusLost(evt);
            }
        });

        jLabelLibrary.setText("Library");

        jComboBoxLibrary.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jComboBoxLibraryFocusLost(evt);
            }
        });

        jLabelFullPath.setText("Full Path");

        jButtonOK.setText("OK");
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.setDefaultCapable(false);
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jTextFieldFullPath.setEditable(false);
        jTextFieldFullPath.setText("jTextField");
        jTextFieldFullPath.setCaretColor(new Color(0,0,0,0));
        // jTextFieldFullPath.setFocusTraversalKeysEnabled(false);
        // jTextFieldFullPath.setRequestFocusEnabled(false);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabelFullPath)
                        .addGap(51, 51, 51)
                        .addComponent(jTextFieldFullPath)
                        .addGap(20, 20, 20))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, 406, GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(130, Short.MAX_VALUE)
                    )
                )
            )
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(jLabelName, Alignment.LEADING)
                    .addComponent(jLabelLibrary, Alignment.LEADING)
                )
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jComboBoxLibrary, GroupLayout.PREFERRED_SIZE, 171, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldObjectName, GroupLayout.PREFERRED_SIZE, 247, GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE)
            )
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(344, Short.MAX_VALUE)
                .addComponent(jButtonCancel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jButtonOK, GroupLayout.PREFERRED_SIZE, 86, GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
            )
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(266, Short.MAX_VALUE)
                    .addComponent(filler1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(266, Short.MAX_VALUE)
                )
            )
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelLibrary)
                    .addComponent(jComboBoxLibrary, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelName)
                    .addComponent(jTextFieldObjectName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelFullPath)
                    .addComponent(jTextFieldFullPath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOK)
                )
                .addGap(15, 15, 15)
            )
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(128, Short.MAX_VALUE)
                    .addComponent(filler1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(129, Short.MAX_VALUE)
                )
            )
        );

        pack();
    }

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {
        modifiedData();

        String objname = jTextFieldObjectName.getText().trim();
        int ididx = objname.lastIndexOf('/');
        if (ididx > 0) {
            obj.shortId = objname.substring(ididx + 1);
        } else {
            obj.shortId = objname;
        }
        obj.id = jTextFieldObjectName.getText();
        obj.sObjFilePath = jTextFieldFullPath.getText();
        obj.setUUID(obj.GenerateUUID());
        File f = new File(obj.sObjFilePath);
        if (!f.exists()) {
            File dir = f.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        MainFrame.axoObjects.WriteAxoObject(obj.sObjFilePath, obj);
        axoObjects.LoadAxoObjects();

        setVisible(false);
        dispose();
    }

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
        dispose();
    }

    private void jObjectNameFocusLost(java.awt.event.FocusEvent evt) {
        modifiedData();
    }

    private void jComboBoxLibraryFocusLost(java.awt.event.FocusEvent evt) {
        modifiedData();
    }

    private void populateFields() {
       jTextFieldObjectName.setText(obj.id);

        AxolotiLibrary sellib = null;
        for (AxolotiLibrary lib : Preferences.getInstance().getLibraries()) {
            if (!lib.isReadOnly()) {
                jComboBoxLibrary.addItem(lib.getId());
            }
            if (obj.sObjFilePath != null && obj.sObjFilePath.startsWith(lib.getLocalLocation())) {
               if (sellib == null || sellib.getLocalLocation().length() < lib.getLocalLocation().length()) {
                    sellib = lib;
                }
            }
        }

        if (sellib == null) {
            jComboBoxLibrary.setSelectedItem(AxolotiLibrary.KSOLOTI_CONTRIB_ID);
        } else {
            if(sellib.isReadOnly()) {
                jComboBoxLibrary.setSelectedItem(AxolotiLibrary.KSOLOTI_CONTRIB_ID);
            } else {
                jComboBoxLibrary.setSelectedItem(sellib.getId());
            }
            if(sellib.getContributorPrefix()!=null) {
                String cp = sellib.getContributorPrefix();
                if(cp.length()>0) {
                    if(obj.id.startsWith(cp)) {
                        jTextFieldObjectName.setText(obj.id.substring(cp.length()+1));
                    }
                }
            }
        }
        modifiedData();
    }

    String GetDestinationPath() {
        if (jComboBoxLibrary.getSelectedIndex() >= 0) {
            AxolotiLibrary lib = Preferences.getInstance().getLibrary((String) jComboBoxLibrary.getSelectedObjects()[0]);
            StringBuilder file = new StringBuilder();
 
            file.append(lib.getLocalLocation()).append(File.separator);
            file.append("objects").append(File.separator);
            String cp = lib.getContributorPrefix();
            if (cp != null && cp.length() > 0) {
                file.append(cp).append(File.separator);
            }
 
            String objname = jTextFieldObjectName.getText().trim();
            String objid = objname;
            String objpath = "";
            int ididx = objname.lastIndexOf('/');
            if (ididx > 0) {
                objid = objname.substring(ididx + 1);
                objpath = objname.substring(0, ididx);
            }
            objid = AxoObjects.ConvertToLegalFilename(objid);
            objpath = objpath.replace('/', File.separatorChar);
            if (!objpath.isEmpty()) {
                file.append(objpath).append(File.separator);
            }
            file.append(objid);
            return file.toString() + ".axo";
        } else {
            return "";
        }
    }

    private void modifiedData() {
        jTextFieldFullPath.setText(GetDestinationPath());
    }
}
