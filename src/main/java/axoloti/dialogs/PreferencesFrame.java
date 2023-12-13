/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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

import axoloti.MainFrame;
import axoloti.utils.AxoFileLibrary;
import axoloti.utils.AxoGitLibrary;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Preferences;

// import static axoloti.MainFrame.mainframe;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Johannes Taelman
 */
public class PreferencesFrame extends javax.swing.JFrame {

    static PreferencesFrame singleton = null;

    public static PreferencesFrame GetPreferencesFrame() {
        if (singleton == null) {
            singleton = new PreferencesFrame();
        }
        return singleton;
    }

    /**
     * Creates new form PreferencesFrame
     *
     */
    private PreferencesFrame() {

        setTitle("Preferences");
        setIconImage(new ImageIcon(getClass().getResource("/resources/ksoloti_icon.png")).getImage());

        setLocation((int)MainFrame.mainframe.getLocation().getX() + 120, (int)MainFrame.mainframe.getLocation().getY() + 60);

        initComponents();

        Preferences prefs = Preferences.LoadPreferences();

        jTextFieldPollInterval.setText(Integer.toString(prefs.getPollInterval()));
        txtFavDir.setText(prefs.getFavouriteDir());
        // txtFirmwareDir.setText(System.getProperty(axoloti.Axoloti.FIRMWARE_DIR));
        // txtRuntimeDir.setText(System.getProperty(axoloti.Axoloti.RUNTIME_DIR));
        jControllerEnabled.setSelected(prefs.isControllerEnabled());
        jTextFieldController.setText(prefs.getControllerObject());
        jTextFieldController.setEnabled(prefs.isControllerEnabled());
        jCheckBoxNoMouseReCenter.setSelected(prefs.getMouseDoNotRecenterWhenAdjustingControls());
        // jCheckBoxKeyboardFrameAlwaysOnTop.setSelected(prefs.getKeyboardFrameAlwaysOnTop());
        if (prefs.getMouseDialAngular()) jComboBoxDialMouseBehaviour.setSelectedItem("Angular"); 
        jComboBoxTheme.setSelectedItem(prefs.getTheme());

        PopulateLibrary();

        setResizable(false);

        /* Double click to edit library */
        jLibraryTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                Point p = me.getPoint();
                int idx = table.rowAtPoint(p);
                if (me.getClickCount() == 2) {
                    if (idx >= 0) {
                        editLibraryRow(idx);
                    }
                }
            }
        });
    }

    void Apply() {

        Preferences prefs = Preferences.LoadPreferences();

        prefs.setPollInterval(Integer.parseInt(jTextFieldPollInterval.getText()));
        prefs.setMouseDialAngular(jComboBoxDialMouseBehaviour.getSelectedItem().equals("Angular"));
        prefs.setFavouriteDir(txtFavDir.getText());
        prefs.setControllerObject(jTextFieldController.getText().trim());
        prefs.setControllerEnabled(jControllerEnabled.isSelected());
        prefs.setTheme(jComboBoxTheme.getSelectedItem().toString());
        prefs.applyTheme();
        // SwingUtilities.updateComponentTreeUI(MainFrame.mainframe);
        // SwingUtilities.updateComponentTreeUI(MainFrame.mainframe.getKeyboard());
        // SwingUtilities.updateComponentTreeUI(MainFrame.mainframe.getFilemanager());
        // for (DocumentWindow p : DocumentWindowList.GetList())
        // {
            // SwingUtilities.updateComponentTreeUI(p.GetFrame());
        // }
        // Theme.loadDefaultTheme();
    }

    final void PopulateLibrary() {

        DefaultTableModel model = (DefaultTableModel) jLibraryTable.getModel();

        model.setRowCount(0);
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

        for (AxolotiLibrary lib : Preferences.LoadPreferences().getLibraries()) {
            model.addRow(new Object[]{lib.getType(), lib.getId(), lib.getLocalLocation(), lib.getEnabled()});
        }

        jLibraryTable.setCellSelectionEnabled(false);
        jLibraryTable.setRowSelectionAllowed(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        jTextFieldPollInterval = new javax.swing.JTextField();
        jLabelLibraries = new javax.swing.JLabel();
        jLabelPollInterval = new javax.swing.JLabel();
        jButtonSave = new javax.swing.JButton();
        jLabelDialMouseBehaviour = new javax.swing.JLabel();
        jComboBoxDialMouseBehaviour = new javax.swing.JComboBox();
        jLabelFavouritesDir = new javax.swing.JLabel();
        // jLabelFirmwareDir = new javax.swing.JLabel();
        // txtFirmwareDir = new javax.swing.JLabel();
        // jLabelRuntimeDir = new javax.swing.JLabel();
        // txtRuntimeDir = new javax.swing.JLabel();
        // btnFirmwareDir = new javax.swing.JButton();
        // btnRuntimeDir = new javax.swing.JButton();
        txtFavDir = new javax.swing.JLabel();
        btnFavDir = new javax.swing.JButton();
        jLabelController = new javax.swing.JLabel();
        jTextFieldController = new javax.swing.JTextField();
        jControllerEnabled = new javax.swing.JCheckBox();
        jScrollPaneLibraryTable = new javax.swing.JScrollPane();
        jLibraryTable = new javax.swing.JTable();
        jAddLibBtn = new javax.swing.JButton();
        jDelLibBtn = new javax.swing.JButton();
        jResetLib = new javax.swing.JButton();
        jEditLib = new javax.swing.JButton();
        jLibStatus = new javax.swing.JButton();
        jLabelTheme = new javax.swing.JLabel();
        jComboBoxTheme = new javax.swing.JComboBox();
        jCheckBoxNoMouseReCenter = new javax.swing.JCheckBox();
        // jCheckBoxKeyboardFrameAlwaysOnTop = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        // addWindowListener(new java.awt.event.WindowAdapter() {
        //     public void windowActivated(java.awt.event.WindowEvent evt) {
        //         formWindowActivated(evt);
        //     }
        // });

        jTextFieldPollInterval.setText("jTextField1");
        jTextFieldPollInterval.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        jLabelLibraries.setText("Libraries");

        jLabelPollInterval.setText("Poll Interval (Milliseconds)");

        jButtonSave.setText("Save All");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jLabelDialMouseBehaviour.setText("Dial Mouse Behaviour");

        jComboBoxDialMouseBehaviour.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Vertical", "Angular" }));
        jComboBoxDialMouseBehaviour.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDialMouseBehaviourActionPerformed(evt);
            }
        });

        jLabelFavouritesDir.setText("Favourites Dir");

        // jLabelFirmwareDir.setText("Firmware Dir");

        // txtFirmwareDir.setText("test");

        // jLabelRuntimeDir.setText("Runtime Dir");

        // txtRuntimeDir.setText("test");

        txtFavDir.setText("test");

        // btnFirmwareDir.setText("Browse...");
        // btnFirmwareDir.addActionListener(new java.awt.event.ActionListener() {
        //     public void actionPerformed(java.awt.event.ActionEvent evt) {
        //         btnFirmwareDirActionPerformed(evt);
        //     }
        // });

        // btnRuntimeDir.setText("Browse...");
        // btnRuntimeDir.addActionListener(new java.awt.event.ActionListener() {
        //     public void actionPerformed(java.awt.event.ActionEvent evt) {
        //         btnRuntimeDirActionPerformed(evt);
        //     }
        // });


        btnFavDir.setText("Browse...");
        btnFavDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFavDirActionPerformed(evt);
            }
        });

        jLabelController.setText("Controller Object");

        jTextFieldController.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        jControllerEnabled.setText("Enabled");
        jControllerEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jControllerEnabledActionPerformed(evt);
            }
        });

        jLibraryTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Type", "Id", "Location", "Enabled"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jLibraryTable.setColumnSelectionAllowed(true);
        jLibraryTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPaneLibraryTable.setViewportView(jLibraryTable);
        jLibraryTable.getTableHeader().setReorderingAllowed(false);
        jLibraryTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        if (jLibraryTable.getColumnModel().getColumnCount() > 0) {
            // jLibraryTable.getColumnModel().getColumn(0).setResizable(false);
            jLibraryTable.getColumnModel().getColumn(0).setPreferredWidth(60);
            // jLibraryTable.getColumnModel().getColumn(1).setResizable(false);
            jLibraryTable.getColumnModel().getColumn(1).setPreferredWidth(140);
            jLibraryTable.getColumnModel().getColumn(2).setPreferredWidth(280);
            // jLibraryTable.getColumnModel().getColumn(3).setResizable(false);
            jLibraryTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        }

        jAddLibBtn.setText("+");
        jAddLibBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddLibBtnActionPerformed(evt);
            }
        });

        jDelLibBtn.setText("-");
        jDelLibBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDelLibBtnActionPerformed(evt);
            }
        });

        jResetLib.setText("Reset All");
        jResetLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jResetLibActionPerformed(evt);
            }
        });

        jEditLib.setText("Edit");
        jEditLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditLibActionPerformed(evt);
            }
        });

        jLibStatus.setText("Status");
        jLibStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLibStatusActionPerformed(evt);
            }
        });

        jLabelTheme.setText("Theme (restart required)");
        jLabelTheme.setEnabled(true);


        for (String i : Preferences.THEMELIST)
        {
            jComboBoxTheme.addItem(i);
        }

        jComboBoxTheme.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxThemeActionPerformed(evt);
            }
        });

        jCheckBoxNoMouseReCenter.setText("Do not re-center cursor (for touchscreens)");
        jCheckBoxNoMouseReCenter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxNoMouseReCenterActionPerformed(evt);
            }
        });

        // jCheckBoxKeyboardFrameAlwaysOnTop.setText("Keyboard window always on top");
        // jCheckBoxKeyboardFrameAlwaysOnTop.addActionListener(new java.awt.event.ActionListener() {
        //     public void actionPerformed(java.awt.event.ActionEvent evt) {
        //         jCheckBoxKeyboardFrameAlwaysOnTopActionPerformed(evt);
        //     }
        // });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(

            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            // .addComponent(jLabelFirmwareDir)
                            // .addComponent(jLabelRuntimeDir)
                            .addComponent(jLabelFavouritesDir)
                            .addComponent(jLabelLibraries)
                        )
                        .addGap(34, 34, 34)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                            // .addGroup(layout.createSequentialGroup()
                            //     .addComponent(txtFirmwareDir, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE)
                            //     // .addGap(288, 288, 288)
                            // )

                            .addGroup(layout.createSequentialGroup()

                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    // .addComponent(txtRuntimeDir, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtFavDir, javax.swing.GroupLayout.PREFERRED_SIZE, 429, javax.swing.GroupLayout.PREFERRED_SIZE)
                                )
                            .addGap(16, 16, 16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            )
                        )

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnFavDir, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(16, 16, 16)
                    )

                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPaneLibraryTable, javax.swing.GroupLayout.PREFERRED_SIZE, 560, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                            // .addGroup(layout.createSequentialGroup()
                                .addComponent(jResetLib, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                                // .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            // )

                            // .addGroup(layout.createSequentialGroup()
                                .addComponent(jLibStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                                // .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            // )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jAddLibBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDelLibBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            )
                            .addComponent(jEditLib, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        )
                        .addContainerGap(14, Short.MAX_VALUE)
                    )

                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelController, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldController, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(28, 28, 28)
                                .addComponent(jControllerEnabled)
                                .addGap(0, 0, Short.MAX_VALUE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelTheme)
                                .addGap(60, 60, 60)
                                .addComponent(jComboBoxTheme, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                                // .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(105, 105, 105)
                                .addComponent(jButtonSave, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                            )
                        )
                        .addContainerGap())

                    .addGroup(layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxNoMouseReCenter, javax.swing.GroupLayout.PREFERRED_SIZE, 453, javax.swing.GroupLayout.PREFERRED_SIZE)

                            // .addComponent(jCheckBoxKeyboardFrameAlwaysOnTop, javax.swing.GroupLayout.PREFERRED_SIZE, 453, javax.swing.GroupLayout.PREFERRED_SIZE)

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelPollInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPollInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelDialMouseBehaviour)
                                .addGap(75, 75, 75)
                                .addComponent(jComboBoxDialMouseBehaviour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            )
                        )
                        .addGap(0, 0, Short.MAX_VALUE)
                    )
                )
            )
        );

        layout.setVerticalGroup(

            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()

                // .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                //     .addComponent(btnRuntimeDir)
                //     .addComponent(txtRuntimeDir)
                //     .addComponent(jLabelRuntimeDir)
                // )
                // .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                // .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                //     .addComponent(jLabelFirmwareDir)
                //     .addComponent(txtFirmwareDir)
                //     .addComponent(btnFirmwareDir)
                // )
                // .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFavDir)
                    .addComponent(jLabelFavouritesDir)
                    .addComponent(btnFavDir)
                )
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelLibraries)
                .addGap(24, 24, 24)

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                    .addGroup(layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jAddLibBtn)
                            .addComponent(jDelLibBtn)
                        )
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                        .addComponent(jEditLib)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                        // .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jResetLib)
                        // )
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        // .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLibStatus)
                        // )
                    )

                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPaneLibraryTable, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelPollInterval)
                            .addComponent(jTextFieldPollInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        )
                    )
                )

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBoxDialMouseBehaviour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelDialMouseBehaviour)
                        )
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxNoMouseReCenter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        // .addComponent(jCheckBoxKeyboardFrameAlwaysOnTop)
                        // .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelController)
                            .addComponent(jTextFieldController, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jControllerEnabled)
                        )
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelTheme)
                            .addComponent(jComboBoxTheme, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonSave)
                        )
                    )

                    // .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    //     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    //     .addContainerGap()
                    // )
                )
                .addGap(14,14,14)
            )
        );

        pack();
    }// </editor-fold>

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {
        Apply();
        Preferences.LoadPreferences().SavePrefs();
        setVisible(false);
    }

    private void jComboBoxDialMouseBehaviourActionPerformed(java.awt.event.ActionEvent evt) {
    }

    // private void btnFirmwareDirActionPerformed(java.awt.event.ActionEvent evt) {
    //     JFileChooser chooser = new JFileChooser(Preferences.LoadPreferences().getCurrentFileDirectory());
    //     chooser.setPreferredSize(new java.awt.Dimension(320, 480));
    //     chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    //     chooser.setAcceptAllFileFilterUsed(false);
    //     if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
    //         String dir;
    //         try {
    //             dir = chooser.getSelectedFile().getCanonicalPath();
    //             Preferences.LoadPreferences().SetFirmwareDir(dir);
    //             txtFirmwareDir.setText(dir);
    //         } catch (IOException ex) {
    //             Logger.getLogger(PreferencesFrame.class.getName()).log(Level.SEVERE, null, ex);
    //         }
    //     }
    // }

    // private void btnRuntimeDirActionPerformed(java.awt.event.ActionEvent evt) {
    //     JFileChooser chooser = new JFileChooser(Preferences.LoadPreferences().getCurrentFileDirectory());
    //     chooser.setPreferredSize(new java.awt.Dimension(320, 480));
    //     chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    //     chooser.setAcceptAllFileFilterUsed(false);
    //     if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
    //         String dir;
    //         try {
    //             dir = chooser.getSelectedFile().getCanonicalPath();
    //             Preferences.LoadPreferences().SetRuntimeDir(dir);
    //             txtRuntimeDir.setText(dir);
    //         } catch (IOException ex) {
    //             Logger.getLogger(PreferencesFrame.class.getName()).log(Level.SEVERE, null, ex);
    //         }
    //     }
    // }

    // private void formWindowActivated(java.awt.event.WindowEvent evt) {
    //     txtFirmwareDir.setText(System.getProperty(axoloti.Axoloti.FIRMWARE_DIR));
    //     txtRuntimeDir.setText(System.getProperty(axoloti.Axoloti.RUNTIME_DIR));
    // }

    private void btnFavDirActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser(Preferences.LoadPreferences().getCurrentFileDirectory());
        chooser.setPreferredSize(new java.awt.Dimension(640, 640));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String dir;
            try {
                dir = chooser.getSelectedFile().getCanonicalPath();
                Preferences.LoadPreferences().setFavouriteDir(dir);
                txtFavDir.setText(dir);
            } catch (IOException ex) {
                Logger.getLogger(PreferencesFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jControllerEnabledActionPerformed(java.awt.event.ActionEvent evt) {
        jTextFieldController.setEnabled(jControllerEnabled.isSelected());
    }

    private void jAddLibBtnActionPerformed(java.awt.event.ActionEvent evt) {

        AxolotiLibraryEditor d = new AxolotiLibraryEditor(this, true);
        AxolotiLibrary lib = d.getLibrary();

        AxolotiLibrary newlib;
        if (AxoGitLibrary.TYPE.equals(lib.getType())) {
            newlib = new AxoGitLibrary();
        } else {
            newlib = new AxoFileLibrary();
        }
        newlib.clone(lib);
        Preferences.LoadPreferences().updateLibrary(lib.getId(), newlib);
        PopulateLibrary();
    }

    private void jDelLibBtnActionPerformed(java.awt.event.ActionEvent evt) {

        DefaultTableModel model = (DefaultTableModel)jLibraryTable.getModel();
        int idx = jLibraryTable.getSelectedRow();
        String id = (String)model.getValueAt(idx, 1);

        int n = JOptionPane.showConfirmDialog(this,
                                            "Are you sure you want to remove the library \"" + id + "\"?",
                                             "Warning",
                                             JOptionPane.YES_NO_OPTION);
        switch (n)
        {
            case JOptionPane.YES_OPTION: {
                if (idx >= 0)
                {
                    Preferences.LoadPreferences().removeLibrary(id);
                }

                PopulateLibrary();
                break;
            }
            case JOptionPane.NO_OPTION:
                break;
        }

    } 

    private void jResetLibActionPerformed(java.awt.event.ActionEvent evt) {
        boolean delete = false;

        int options = JOptionPane.OK_CANCEL_OPTION;
        int res = JOptionPane.showConfirmDialog(this, "Reset will re-download the factory and community directories.\nContinue?", "Warning", options);
        if (res == JOptionPane.CANCEL_OPTION) {
            return;
        }
        delete = (res == JOptionPane.OK_OPTION);

        Preferences.LoadPreferences().ResetLibraries(delete);
        PopulateLibrary();
    }

    private void jEditLibActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) jLibraryTable.getModel();
        int idx = jLibraryTable.getSelectedRow();
        if (idx >= 0) {
            editLibraryRow(idx);
        }
    }

    private void jLibStatusActionPerformed(java.awt.event.ActionEvent evt) {
        for (AxolotiLibrary lib : Preferences.LoadPreferences().getLibraries()) {
            lib.reportStatus();
        }
    }

    private void jComboBoxThemeActionPerformed(java.awt.event.ActionEvent evt) {
        Preferences.LoadPreferences().setTheme(jComboBoxTheme.getSelectedItem().toString());
        Preferences.LoadPreferences().applyTheme();
        SwingUtilities.updateComponentTreeUI(this); /* Preview theme via preferences window */
    }

    private void jCheckBoxNoMouseReCenterActionPerformed(java.awt.event.ActionEvent evt) {
        Preferences.LoadPreferences().setMouseDoNotRecenterWhenAdjustingControls(jCheckBoxNoMouseReCenter.isSelected());
    }

    // private void jCheckBoxKeyboardFrameAlwaysOnTopActionPerformed(java.awt.event.ActionEvent evt) {
    //     Preferences prefs = Preferences.LoadPreferences();
    //     prefs.setKeyboardFrameAlwaysOnTop(jCheckBoxKeyboardFrameAlwaysOnTop.isSelected());
    //     JFrame keyb = MainFrame.mainframe.getKeyboard();
    //     keyb.setAlwaysOnTop(prefs.getKeyboardFrameAlwaysOnTop());
    // }

    private void editLibraryRow(int idx) {
        if (idx >= 0) {
            DefaultTableModel model = (DefaultTableModel) jLibraryTable.getModel();
            String id = (String) model.getValueAt(idx, 1);
            AxolotiLibrary lib = Preferences.LoadPreferences().getLibrary(id);
            if (lib != null) {
                String type = lib.getType();
                AxolotiLibraryEditor d = new AxolotiLibraryEditor(this, true, lib);
                AxolotiLibrary updlib = lib;
                if(!lib.getType().equals(type)) {
                  if (AxoGitLibrary.TYPE.equals(lib.getType())) {
                       updlib = new AxoGitLibrary();
                   } else {
                       updlib = new AxoFileLibrary();
                   }
                  updlib.clone(lib);
                }
                Preferences.LoadPreferences().updateLibrary(lib.getId(), updlib);
                PopulateLibrary();
            }
        }
    }


    // Variables declaration - do not modify
    private javax.swing.JButton btnFavDir;
    private javax.swing.JButton jAddLibBtn;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JCheckBox jCheckBoxNoMouseReCenter;
    // private javax.swing.JCheckBox jCheckBoxKeyboardFrameAlwaysOnTop;
    private javax.swing.JComboBox jComboBoxDialMouseBehaviour;
    private javax.swing.JCheckBox jControllerEnabled;
    private javax.swing.JButton jDelLibBtn;
    private javax.swing.JButton jEditLib;
    private javax.swing.JLabel jLabelLibraries;
    private javax.swing.JLabel jLabelPollInterval;
    private javax.swing.JLabel jLabelDialMouseBehaviour;
    private javax.swing.JLabel jLabelFavouritesDir;
    private javax.swing.JLabel jLabelController;
    private javax.swing.JLabel jLabelTheme;
    private javax.swing.JComboBox jComboBoxTheme;
    private javax.swing.JButton jLibStatus;
    private javax.swing.JTable jLibraryTable;
    private javax.swing.JButton jResetLib;
    private javax.swing.JScrollPane jScrollPaneLibraryTable;
    private javax.swing.JTextField jTextFieldController;
    private javax.swing.JTextField jTextFieldPollInterval;
    // private javax.swing.JButton themeEditButton;
    private javax.swing.JLabel txtFavDir;
    // End of variables declaration
}
