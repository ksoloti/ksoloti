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
import axoloti.utils.Constants;
import axoloti.utils.Preferences;
import components.ScrollPaneComponent;

import static axoloti.MainFrame.fc;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    Preferences prefs = Preferences.LoadPreferences();

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
        setIconImage(Constants.APP_ICON.getImage());

        setLocation((int)MainFrame.mainframe.getLocation().getX() + 120, (int)MainFrame.mainframe.getLocation().getY() + 60);

        initComponents();


        jTextFieldPollInterval.setText(Integer.toString(prefs.getPollInterval()));

        jTextFieldCodeFontSize.setText(Integer.toString(prefs.getCodeFontSize()));

        jTextFieldFavDir.setText(prefs.getFavouriteDir());

        jTextFieldUserShortcut1.setText(prefs.getUserShortcut(0));
        jTextFieldUserShortcut2.setText(prefs.getUserShortcut(1));
        jTextFieldUserShortcut3.setText(prefs.getUserShortcut(2));
        jTextFieldUserShortcut4.setText(prefs.getUserShortcut(3));

        jControllerEnabled.setSelected(prefs.isControllerEnabled());
        jTextFieldController.setText(prefs.getControllerObject());
        jTextFieldController.setEnabled(prefs.isControllerEnabled());

        jCheckBoxNoMouseReCenter.setSelected(prefs.getMouseDoNotRecenterWhenAdjustingControls());

        if (prefs.getMouseDialAngular()) jComboBoxDialMouseBehaviour.setSelectedItem("Angular"); 

        if (prefs.getAxolotiLegacyMode()) jComboBoxAxolotiLegacyMode.setSelectedItem("Axoloti (Legacy)"); 

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

        // Preferences prefs = Preferences.LoadPreferences();

        prefs.setPollInterval(Integer.parseInt(jTextFieldPollInterval.getText()));
        prefs.setCodeFontSize(Integer.parseInt(jTextFieldCodeFontSize.getText()));
        Constants.FONT_MONO = Constants.FONT_MONO.deriveFont((float)prefs.getCodeFontSize());
        MainFrame.mainframe.updateConsoleFont();
        prefs.setMouseDialAngular(jComboBoxDialMouseBehaviour.getSelectedItem().equals("Angular"));
        prefs.setAxolotiLegacyMode(jComboBoxAxolotiLegacyMode.getSelectedItem().equals("Axoloti (Legacy)"));

        prefs.setUserShortcut(0, jTextFieldUserShortcut1.getText());
        prefs.setUserShortcut(1, jTextFieldUserShortcut2.getText());
        prefs.setUserShortcut(2, jTextFieldUserShortcut3.getText());
        prefs.setUserShortcut(3, jTextFieldUserShortcut4.getText());

        prefs.setFavouriteDir(jTextFieldFavDir.getText());
        prefs.setControllerObject(jTextFieldController.getText().trim());
        prefs.setControllerEnabled(jControllerEnabled.isSelected());
        prefs.setTheme(jComboBoxTheme.getSelectedItem().toString());
        prefs.applyTheme();
    }

    final void PopulateLibrary() {

        DefaultTableModel model = (DefaultTableModel) jLibraryTable.getModel();

        model.setRowCount(0);
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

        for (AxolotiLibrary lib : prefs.getLibraries()) {
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
    private void initComponents() {

        jTextFieldPollInterval = new javax.swing.JTextField();
        jTextFieldCodeFontSize = new javax.swing.JTextField();
        jLabelLibraries = new javax.swing.JLabel();
        jLabelPollInterval = new javax.swing.JLabel();
        jLabelCodeFontSize = new javax.swing.JLabel();
        jButtonSave = new javax.swing.JButton();
        jLabelDialMouseBehaviour = new javax.swing.JLabel();
        jLabelAxolotiLegacyMode = new javax.swing.JLabel();
        jComboBoxDialMouseBehaviour = new javax.swing.JComboBox<String>();
        jComboBoxAxolotiLegacyMode = new javax.swing.JComboBox<String>();
        jLabelFavouritesDir = new javax.swing.JLabel();
        jLabelUserShortcut1 = new javax.swing.JLabel();
        jLabelUserShortcut2 = new javax.swing.JLabel();
        jLabelUserShortcut3 = new javax.swing.JLabel();
        jLabelUserShortcut4 = new javax.swing.JLabel();
        jTextFieldFavDir = new javax.swing.JTextField();

        jTextFieldUserShortcut1 = new javax.swing.JTextField();
        jTextFieldUserShortcut2 = new javax.swing.JTextField();
        jTextFieldUserShortcut3 = new javax.swing.JTextField();
        jTextFieldUserShortcut4 = new javax.swing.JTextField();

        btnFavDir = new javax.swing.JButton();
        jLabelController = new javax.swing.JLabel();
        jTextFieldController = new javax.swing.JTextField();
        jControllerEnabled = new javax.swing.JCheckBox();
        jScrollPaneLibraryTable = new ScrollPaneComponent();
        jLibraryTable = new javax.swing.JTable();
        jAddLibBtn = new javax.swing.JButton();
        jDelLibBtn = new javax.swing.JButton();
        jResetLib = new javax.swing.JButton();
        jEditLib = new javax.swing.JButton();
        jLibStatus = new javax.swing.JButton();
        jLabelTheme = new javax.swing.JLabel();
        jComboBoxTheme = new javax.swing.JComboBox<String>();
        jCheckBoxNoMouseReCenter = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jTextFieldPollInterval.setText("jTextField1");
        jTextFieldPollInterval.setToolTipText("Interval at which the Patcher displays the newest parameter data coming from the Core.");
        jTextFieldPollInterval.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        jLabelPollInterval.setText("Poll Interval (milliseconds)");
        jLabelPollInterval.setToolTipText(jTextFieldPollInterval.getToolTipText());

        jTextFieldCodeFontSize.setText("jTextField2");
        jTextFieldCodeFontSize.setToolTipText("Changes font size for all code text windows (object editor) and the main window console.");
        jTextFieldCodeFontSize.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        jLabelCodeFontSize.setText("Code Font Size");
        jLabelCodeFontSize.setToolTipText(jTextFieldCodeFontSize.getToolTipText());

        jLabelLibraries.setText("Libraries");


        jButtonSave.setText("Save All");
        jButtonSave.setToolTipText("Save all settings.");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jLabelDialMouseBehaviour.setText("Dial Mouse Behaviour");

        jLabelAxolotiLegacyMode.setText("Axoloti Legacy Mode (restart required)");
        jLabelAxolotiLegacyMode.setToolTipText("<html>Legacy support of Axoloti Core.<p>Select \"Axoloti (Legacy)\" if you want this copy of the patcher to connect to Axoloti Cores instead of Ksoloti Cores.<p>FOR WHATEVER OPTION IS ACTIVE, THE PATCHER WILL ONLY DETECT THAT PARTICULAR BOARD TYPE.<p>You can run another copy of the patcher in \"Ksoloti (Default)\" mode to connect to both types of boards simultaneously.");

        jComboBoxDialMouseBehaviour.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Vertical", "Angular" }));
        jComboBoxDialMouseBehaviour.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDialMouseBehaviourActionPerformed(evt);
            }
        });

        jComboBoxAxolotiLegacyMode.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Ksoloti (Default)", "Axoloti (Legacy)" }));
        jComboBoxAxolotiLegacyMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxAxolotiLegacyModeActionPerformed(evt);
            }
        });
        jComboBoxAxolotiLegacyMode.setToolTipText(jLabelAxolotiLegacyMode.getToolTipText());

        jLabelFavouritesDir.setText("Favourites Dir");

        jLabelUserShortcut1.setText("Object Finder Shortcut 1");
        jLabelUserShortcut2.setText("Object Finder Shortcut 2");
        jLabelUserShortcut3.setText("Object Finder Shortcut 3");
        jLabelUserShortcut4.setText("Object Finder Shortcut 4");

        jTextFieldFavDir.setText("test");
        jTextFieldFavDir.setToolTipText("Select a folder/subfolder with patch files to conveniently access them via the file menu.");
        jTextFieldFavDir.setEditable(false);
        jTextFieldFavDir.setCaretColor(new Color(0,0,0,0));

        jTextFieldUserShortcut1.setText("test");
        jTextFieldUserShortcut2.setText("test");
        jTextFieldUserShortcut3.setText("test");
        jTextFieldUserShortcut4.setText("test");

        jTextFieldUserShortcut1.setToolTipText("Enter a string here to create a custom Object Finder shortcut. Useful if you have certain objects or a personal library you use a lot. In the patch window, press SHIFT+1 to open shortcut 1, SHIFT+2 for shortcut 2, etc.\nHint: The Object Finder also supports the \"*\" wildcard and regex!");
        jTextFieldUserShortcut2.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        jTextFieldUserShortcut3.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        jTextFieldUserShortcut4.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        jLabelUserShortcut1.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        jLabelUserShortcut2.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        jLabelUserShortcut3.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        jLabelUserShortcut4.setToolTipText(jTextFieldUserShortcut1.getToolTipText());

        btnFavDir.setText("Browse...");
        btnFavDir.setToolTipText(jTextFieldFavDir.getToolTipText());
        btnFavDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFavDirActionPerformed(evt);
            }
        });

        jLabelFavouritesDir.setToolTipText(jTextFieldFavDir.getToolTipText());

        jLabelController.setText("Controller Object");
        jLabelController.setToolTipText("A controller object runs invisibly in the background on the Core, regardless of which patch is currently loaded. This is useful for implementing a patch change system.");

        jTextFieldController.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        jTextFieldController.setToolTipText(jLabelController.getToolTipText());

        jControllerEnabled.setText("Enabled");
        jControllerEnabled.setToolTipText(jLabelController.getToolTipText());
        jControllerEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jControllerEnabledActionPerformed(evt);
            }
        });

        jLibraryTable.getTableHeader().setReorderingAllowed(false);
        jLibraryTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Type", "Id", "Location", "Enabled"
            }
        ) {
            Class<?>[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class<?> getColumnClass(int columnIndex) {
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
            jLibraryTable.getColumnModel().getColumn(0).setPreferredWidth(60);
            jLibraryTable.getColumnModel().getColumn(1).setPreferredWidth(140);
            jLibraryTable.getColumnModel().getColumn(2).setPreferredWidth(280);
            jLibraryTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        }

        jAddLibBtn.setText("+");
        jAddLibBtn.setToolTipText("Add a library.");
        jAddLibBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddLibBtnActionPerformed(evt);
            }
        });

        jDelLibBtn.setText("🗑");
        jDelLibBtn.setToolTipText("Delete the selected library.");
        jDelLibBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDelLibBtnActionPerformed(evt);
            }
        });

        jResetLib.setText("Reset All");
        jResetLib.setToolTipText("Reset the library table to factory settings.");
        jResetLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jResetLibActionPerformed(evt);
            }
        });

        jEditLib.setText("Edit");
        jEditLib.setToolTipText("Edit the selected library.");
        jEditLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditLibActionPerformed(evt);
            }
        });

        jLibStatus.setText("Status");
        jLibStatus.setToolTipText("Show in the console if libraries are up-to-date, have unsaved changes etc.");
        jLibStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLibStatusActionPerformed(evt);
            }
        });

        jLabelTheme.setText("Theme (restart required)");
        jLabelTheme.setToolTipText("Change the color theme. The preferences window will reflect a preview of the selected theme but a restart is required for all windows to change.");
        jLabelTheme.setEnabled(true);


        for (String i : Preferences.THEMELIST) {
            jComboBoxTheme.addItem(i);
        }

        jComboBoxTheme.setToolTipText(jLabelTheme.getToolTipText());
        jComboBoxTheme.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxThemeActionPerformed(evt);
            }
        });

        jCheckBoxNoMouseReCenter.setText("Touchscreen Mode");
        jCheckBoxNoMouseReCenter.setToolTipText("Makes the patcher usable with touchscreens. Also fixes abnormal mouse behaviour on some systems when turning knobs.");
        jCheckBoxNoMouseReCenter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxNoMouseReCenterActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(

            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelFavouritesDir)
                            .addComponent(jLabelUserShortcut1)
                            .addComponent(jLabelUserShortcut2)
                            .addComponent(jLabelUserShortcut3)
                            .addComponent(jLabelUserShortcut4)
                            .addComponent(jLabelLibraries)
                        )
                        .addGap(15, 15, 15)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                            .addGroup(layout.createSequentialGroup()

                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextFieldFavDir, javax.swing.GroupLayout.PREFERRED_SIZE, 403, javax.swing.GroupLayout.PREFERRED_SIZE)
                                )
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            )
                            .addGroup(layout.createSequentialGroup()

                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextFieldUserShortcut1, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldUserShortcut2, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldUserShortcut3, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldUserShortcut4, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                                )
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            )
                        )
                        .addGap(12, 12, 12)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnFavDir, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(16, 16, 16)
                    )

                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPaneLibraryTable, javax.swing.GroupLayout.PREFERRED_SIZE, 560, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                                .addComponent(jResetLib, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)

                                .addComponent(jLibStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)

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
                                .addComponent(jLabelController, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldController, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jControllerEnabled)
                                .addGap(0, 0, Short.MAX_VALUE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelTheme, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBoxTheme, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelAxolotiLegacyMode)
                                .addGap(5, 5, 5)
                                .addComponent(jComboBoxAxolotiLegacyMode, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(166, 166, 166)
                                .addComponent(jButtonSave, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                            )
                        )
                        .addContainerGap())

                    .addGroup(layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelPollInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPollInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelCodeFontSize, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldCodeFontSize, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelDialMouseBehaviour)
                                .addGap(101, 101, 101)
                                .addComponent(jComboBoxDialMouseBehaviour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jCheckBoxNoMouseReCenter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldFavDir)
                    .addComponent(jLabelFavouritesDir)
                    .addComponent(btnFavDir)
                )
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldUserShortcut1)
                    .addComponent(jLabelUserShortcut1)
                )
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldUserShortcut2)
                    .addComponent(jLabelUserShortcut2)
                )
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldUserShortcut3)
                    .addComponent(jLabelUserShortcut3)
                )
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldUserShortcut4)
                    .addComponent(jLabelUserShortcut4)
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

                            .addComponent(jResetLib)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLibStatus)
                    )

                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPaneLibraryTable, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelPollInterval)
                            .addComponent(jTextFieldPollInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        )

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelCodeFontSize)
                            .addComponent(jTextFieldCodeFontSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        )
                    )
                )

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBoxDialMouseBehaviour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelDialMouseBehaviour)
                            .addComponent(jCheckBoxNoMouseReCenter)
                        )

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelController)
                            .addComponent(jTextFieldController, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jControllerEnabled)
                        )
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelTheme)
                            .addComponent(jComboBoxTheme, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        )

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelAxolotiLegacyMode)
                            .addComponent(jComboBoxAxolotiLegacyMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonSave)
                        )
                    )

                )
                .addGap(14,14,14)
            )
        );

        pack();
    }

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {
        Apply();
        prefs.SavePrefs();
        setVisible(false);
    }

    private void jComboBoxDialMouseBehaviourActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jComboBoxAxolotiLegacyModeActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void btnFavDirActionPerformed(java.awt.event.ActionEvent evt) {
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(prefs.getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setDialogTitle("Open...");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String dir;
            try {
                dir = fc.getSelectedFile().getCanonicalPath();
                prefs.setFavouriteDir(dir);
                jTextFieldFavDir.setText(dir);
            } catch (IOException ex) {
                Logger.getLogger(PreferencesFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        fc.updateCurrentSize();
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
        prefs.updateLibrary(lib.getId(), newlib);
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
        switch (n) {
            case JOptionPane.YES_OPTION: {
                if (idx >= 0) {
                    prefs.removeLibrary(id);
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

        prefs.ResetLibraries(delete);
        PopulateLibrary();
    }

    private void jEditLibActionPerformed(java.awt.event.ActionEvent evt) {
        // DefaultTableModel model = (DefaultTableModel) jLibraryTable.getModel();
        int idx = jLibraryTable.getSelectedRow();
        if (idx >= 0) {
            editLibraryRow(idx);
        }
    }

    private void jLibStatusActionPerformed(java.awt.event.ActionEvent evt) {
        for (AxolotiLibrary lib : prefs.getLibraries()) {
            lib.reportStatus();
        }
    }

    private void jComboBoxThemeActionPerformed(java.awt.event.ActionEvent evt) {
        prefs.setTheme(jComboBoxTheme.getSelectedItem().toString());
        prefs.applyTheme();
        SwingUtilities.updateComponentTreeUI(this); /* Preview theme via preferences window */
    }

    private void jCheckBoxNoMouseReCenterActionPerformed(java.awt.event.ActionEvent evt) {
        prefs.setMouseDoNotRecenterWhenAdjustingControls(jCheckBoxNoMouseReCenter.isSelected());
    }

    private void editLibraryRow(int idx) {
        if (idx >= 0) {
            DefaultTableModel model = (DefaultTableModel) jLibraryTable.getModel();
            String id = (String) model.getValueAt(idx, 1);
            AxolotiLibrary lib = prefs.getLibrary(id);
            if (lib != null) {
                String type = lib.getType();
                // AxolotiLibraryEditor d = new AxolotiLibraryEditor(this, true, lib);
                AxolotiLibrary updlib = lib;
                if(!lib.getType().equals(type)) {
                  if (AxoGitLibrary.TYPE.equals(lib.getType())) {
                       updlib = new AxoGitLibrary();
                   } else {
                       updlib = new AxoFileLibrary();
                   }
                  updlib.clone(lib);
                }
                prefs.updateLibrary(lib.getId(), updlib);
                PopulateLibrary();
            }
        }
    }


    private javax.swing.JButton btnFavDir;
    private javax.swing.JButton jAddLibBtn;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JCheckBox jCheckBoxNoMouseReCenter;
    private javax.swing.JComboBox<String> jComboBoxDialMouseBehaviour;
    private javax.swing.JComboBox<String> jComboBoxAxolotiLegacyMode;
    private javax.swing.JCheckBox jControllerEnabled;
    private javax.swing.JButton jDelLibBtn;
    private javax.swing.JButton jEditLib;
    private javax.swing.JLabel jLabelLibraries;
    private javax.swing.JLabel jLabelPollInterval;
    private javax.swing.JLabel jLabelCodeFontSize;
    private javax.swing.JLabel jLabelDialMouseBehaviour;
    private javax.swing.JLabel jLabelAxolotiLegacyMode;
    private javax.swing.JLabel jLabelFavouritesDir;
    
    private javax.swing.JLabel jLabelUserShortcut1;
    private javax.swing.JLabel jLabelUserShortcut2;
    private javax.swing.JLabel jLabelUserShortcut3;
    private javax.swing.JLabel jLabelUserShortcut4;

    private javax.swing.JLabel jLabelController;
    private javax.swing.JLabel jLabelTheme;
    private javax.swing.JComboBox<String> jComboBoxTheme;
    private javax.swing.JButton jLibStatus;
    private javax.swing.JTable jLibraryTable;
    private javax.swing.JButton jResetLib;
    private ScrollPaneComponent jScrollPaneLibraryTable;
    private javax.swing.JTextField jTextFieldController;
    private javax.swing.JTextField jTextFieldPollInterval;
    private javax.swing.JTextField jTextFieldCodeFontSize;
    private javax.swing.JTextField jTextFieldFavDir;

    private javax.swing.JTextField jTextFieldUserShortcut1;
    private javax.swing.JTextField jTextFieldUserShortcut2;
    private javax.swing.JTextField jTextFieldUserShortcut3;
    private javax.swing.JTextField jTextFieldUserShortcut4;
}
