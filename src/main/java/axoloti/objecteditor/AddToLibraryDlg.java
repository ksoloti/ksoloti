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
import axoloti.object.AxoObject;
import axoloti.object.AxoObjects;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Preferences;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public AddToLibraryDlg(AxoObjectEditor parent, boolean modal, AxoObject obj) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(parent);
        // for later use
        jAxoFile.setVisible(false);
        jAxoFileLabel.setVisible(false);
        this.obj = new AxoObject();
        try {
            this.obj.copy(obj);
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(AddToLibraryDlg.class.getName()).log(Level.SEVERE, null, ex);
        }
        populateFields();
        jObjectName.getDocument().addDocumentListener(new DocumentListener() {
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
        ((AbstractDocument) jObjectName.getDocument()).setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(DocumentFilter.FilterBypass fb, int offset,
                    String string, AttributeSet attr)
                    throws BadLocationException {
                string = string.replaceAll("\\\\", "/");
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

        jLabel1 = new javax.swing.JLabel();
        jObjectName = new javax.swing.JTextField();
        jAxoFile = new javax.swing.JTextField();
        jAxoFileLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLibrary = new javax.swing.JComboBox<String>();
        jLabel9 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jOK = new javax.swing.JButton();
        jCancel = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        jFileTxt = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Name");

        jObjectName.setText("jTextField1");
        jObjectName.setPreferredSize(new java.awt.Dimension(120, 28));
        jObjectName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jObjectNameFocusLost(evt);
            }
        });

        jAxoFile.setText("jTextField1");
        jAxoFile.setPreferredSize(new java.awt.Dimension(150, 28));
        jAxoFile.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jAxoFileFocusLost(evt);
            }
        });

        jAxoFileLabel.setText("AxoFile");

        jLabel3.setText("Library");

        jLibrary.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jLibraryFocusLost(evt);
            }
        });

        jLabel9.setText("File");

        jLabel4.setText("Information");

        jOK.setText("OK");
        jOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jOKActionPerformed(evt);
            }
        });

        jCancel.setText("Cancel");
        jCancel.setDefaultCapable(false);
        jCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCancelActionPerformed(evt);
            }
        });

        jFileTxt.setEditable(false);
        jFileTxt.setText("jTextField1");
        jFileTxt.setEnabled(false);
        jFileTxt.setFocusTraversalKeysEnabled(false);
        jFileTxt.setRequestFocusEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addGap(51, 51, 51)
                        .addComponent(jFileTxt)
                        .addGap(20, 20, 20))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 406, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(130, Short.MAX_VALUE))))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jAxoFileLabel, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING))
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLibrary, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jObjectName, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(344, Short.MAX_VALUE)
                .addComponent(jCancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jOK, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
            .addGroup(layout.createSequentialGroup()
                .addGap(79, 79, 79)
                .addComponent(jAxoFile, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(266, Short.MAX_VALUE)
                    .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(266, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLibrary, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jObjectName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAxoFileLabel)
                    .addComponent(jAxoFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jFileTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCancel)
                    .addComponent(jOK))
                .addGap(15, 15, 15))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(128, Short.MAX_VALUE)
                    .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(129, Short.MAX_VALUE)))
        );

        pack();
    }

    private void jOKActionPerformed(java.awt.event.ActionEvent evt) {
        modifiedData();

        String objname = jObjectName.getText().trim();
        int ididx = objname.lastIndexOf('/');
        if (ididx > 0) {
            obj.shortId = objname.substring(ididx + 1);
        } else {
            obj.shortId = objname;
        }
        obj.id = jObjectName.getText();
        obj.sObjFilePath = jFileTxt.getText();
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

    private void jCancelActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
        dispose();
    }

    private void jAxoFileFocusLost(java.awt.event.FocusEvent evt) {
        modifiedData();
    }

    private void jObjectNameFocusLost(java.awt.event.FocusEvent evt) {
        modifiedData();
    }

    private void jLibraryFocusLost(java.awt.event.FocusEvent evt) {
        modifiedData();
    }


    private javax.swing.Box.Filler filler1;
    private javax.swing.JTextField jAxoFile;
    private javax.swing.JLabel jAxoFileLabel;
    private javax.swing.JButton jCancel;
    private javax.swing.JTextField jFileTxt;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JComboBox<String> jLibrary;
    private javax.swing.JButton jOK;
    private javax.swing.JTextField jObjectName;
    private javax.swing.JSeparator jSeparator1;

    private void populateFields() {
       jObjectName.setText(obj.id);

        AxolotiLibrary sellib = null;
        for (AxolotiLibrary lib : Preferences.getInstance().getLibraries()) {
            if (!lib.isReadOnly()) {
                jLibrary.addItem(lib.getId());
            }
            if (obj.sObjFilePath != null && obj.sObjFilePath.startsWith(lib.getLocalLocation())) {
               if (sellib == null || sellib.getLocalLocation().length() < lib.getLocalLocation().length()) {
                    sellib = lib;
                }
            }
        }

        if (sellib == null) {
            jLibrary.setSelectedItem(AxolotiLibrary.AXOLOTI_CONTRIB_ID);
        } else {
            if(sellib.isReadOnly()) {
                 jLibrary.setSelectedItem(AxolotiLibrary.AXOLOTI_CONTRIB_ID);
            } else {
                jLibrary.setSelectedItem(sellib.getId());
            }
            if(sellib.getContributorPrefix()!=null) {
                String cp = sellib.getContributorPrefix();
                if(cp.length()>0) {
                    if(obj.id.startsWith(cp)) {
                        jObjectName.setText(obj.id.substring(cp.length()+1));
                    }
                }
            }
        }
        modifiedData();
    }

    String GetDestinationPath() {
        if (jLibrary.getSelectedIndex() >= 0) {
            AxolotiLibrary lib = Preferences.getInstance().getLibrary((String) jLibrary.getSelectedObjects()[0]);
            StringBuilder file = new StringBuilder();
 
            file.append(lib.getLocalLocation());
            file.append("objects").append(File.separator);
            String cp = lib.getContributorPrefix();
            if (cp != null && cp.length() > 0) {
                file.append(cp).append(File.separator);
            }
 
            String objname = jObjectName.getText().trim();
            String objid = objname;
            String objpath = "";
            int ididx = objname.lastIndexOf('/');
            if (ididx > 0) {
                objid = objname.substring(ididx + 1);
                objpath = objname.substring(0, ididx);
            }
            objid = AxoObjects.ConvertToLegalFilename(objid);
            objpath = objpath.replace('/', File.separatorChar);

            
            file.append(objpath).append(File.separator);
            file.append(objid);
            return file.toString() + ".axo";
        } else return "";
    }

    private void modifiedData() {
        jFileTxt.setText(GetDestinationPath());
    }
}
