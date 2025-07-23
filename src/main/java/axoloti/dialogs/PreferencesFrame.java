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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Johannes Taelman
 */
public class PreferencesFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(PreferencesFrame.class.getName());

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
        jBackupPatchesOnSDEnabled.setSelected(prefs.isBackupPatchesOnSDEnabled());
        jTextFieldController.setText(prefs.getControllerObject());
        jTextFieldController.setEnabled(prefs.isControllerEnabled());

        jCheckBoxNoMouseReCenter.setSelected(prefs.getMouseDoNotRecenterWhenAdjustingControls());

        if (prefs.getMouseDialAngular()) jComboBoxDialMouseBehaviour.setSelectedItem("Angular"); 

        jComboBoxFirmwareMode.setSelectedItem(prefs.getFirmwareMode()); 

        jComboBoxTheme.setSelectedItem(prefs.getTheme());

        jComboBoxDspSafetyLimit.setSelectedIndex(prefs.getDspSafetyLimit());

        PopulateLibrary();

        setResizable(false);

        /* Double click to edit library */
        jLibraryTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                table.setRowHeight(24);
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

        prefs.setPollInterval(Integer.parseInt(jTextFieldPollInterval.getText()));

        prefs.setCodeFontSize(Integer.parseInt(jTextFieldCodeFontSize.getText()));
        Constants.FONT_MONO = Constants.FONT_MONO.deriveFont((float)prefs.getCodeFontSize());
        MainFrame.mainframe.updateConsoleFont();

        prefs.setMouseDialAngular(jComboBoxDialMouseBehaviour.getSelectedItem().equals("Angular"));

        prefs.setMouseDoNotRecenterWhenAdjustingControls(jCheckBoxNoMouseReCenter.isSelected());

        if (!jComboBoxFirmwareMode.getSelectedItem().toString().equals(prefs.getFirmwareMode())) {

            prefs.setFirmwareMode(jComboBoxFirmwareMode.getSelectedItem().toString());

            /* Flush old .h.gch file (Will be recompiled for new firmware mode the next time a patch goes live) */
            axoloti.Axoloti.deletePrecompiledHeaderFile();

            MainFrame.mainframe.updateLinkFirmwareID();
            
        }

        prefs.setUserShortcut(0, jTextFieldUserShortcut1.getText());
        prefs.setUserShortcut(1, jTextFieldUserShortcut2.getText());
        prefs.setUserShortcut(2, jTextFieldUserShortcut3.getText());
        prefs.setUserShortcut(3, jTextFieldUserShortcut4.getText());

        prefs.setFavouriteDir(jTextFieldFavDir.getText());

        prefs.setControllerObject(jTextFieldController.getText().trim());
        prefs.setControllerEnabled(jControllerEnabled.isSelected());
        prefs.setBackupPatchesOnSDEnabled(jBackupPatchesOnSDEnabled.isSelected());

        prefs.setTheme(jComboBoxTheme.getSelectedItem().toString());
        prefs.applyTheme();
        
        prefs.setDspSafetyLimit(jComboBoxDspSafetyLimit.getSelectedIndex());
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


    private void initComponents() {

        jTextFieldPollInterval = new JTextField();
    
        jTextFieldCodeFontSize = new JTextField();
        jLabelLibraries = new JLabel();
        jLabelPollInterval = new JLabel();
            
        jLabelCodeFontSize = new JLabel();
        jButtonSave = new JButton();
        jLabelDialMouseBehaviour = new JLabel();
        jLabelFirmwareMode = new JLabel();
        jComboBoxDialMouseBehaviour = new JComboBox<String>();
        jComboBoxFirmwareMode = new JComboBox<String>();
        jLabelFavouritesDir = new JLabel();
        jLabelUserShortcutTitle = new JLabel();
        jLabelUserShortcut1 = new JLabel();
        jLabelUserShortcut2 = new JLabel();
        jLabelUserShortcut3 = new JLabel();
        jLabelUserShortcut4 = new JLabel();
        jTextFieldFavDir = new JTextField();

        jTextFieldUserShortcut1 = new JTextField();
        jTextFieldUserShortcut2 = new JTextField();
        jTextFieldUserShortcut3 = new JTextField();
        jTextFieldUserShortcut4 = new JTextField();

        btnFavDir = new JButton();
        jLabelController = new JLabel();
        jTextFieldController = new JTextField();
        jControllerEnabled = new JCheckBox();
        jBackupPatchesOnSDEnabled = new JCheckBox();
        jLabelBackupPatchesOnSD = new JLabel();
        jScrollPaneLibraryTable = new ScrollPaneComponent();
        jLibraryTable = new JTable();
        jAddLibBtn = new JButton();
        jDelLibBtn = new JButton();
        jResetLib = new JButton();
        jEditLib = new JButton();
        jLibStatus = new JButton();
        jLabelTheme = new JLabel();
        jComboBoxTheme = new JComboBox<String>();

        jLabelDspSafetyLimit = new JLabel();
        jComboBoxDspSafetyLimit = new JComboBox<String>();

        jCheckBoxNoMouseReCenter = new JCheckBox();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        jTextFieldPollInterval.setText("jTextField1");
        jTextFieldPollInterval.setToolTipText("Interval at which the Patcher displays the newest parameter data coming from the Core.");
        jTextFieldPollInterval.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        jLabelPollInterval.setText("Poll Interval (milliseconds)");
        jLabelPollInterval.setToolTipText(jTextFieldPollInterval.getToolTipText());

        jTextFieldCodeFontSize.setText("jTextField2");
        jTextFieldCodeFontSize.setToolTipText("Changes font size for all code text windows (object editor), patch notes, and the main window console.");
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
        jLabelDialMouseBehaviour.setToolTipText("Vertical: Drag up and down to change values.\n" +
                                                "Angular: Circle the cursor around the control to change its value.");

        jLabelFirmwareMode.setText("Firmware Mode (restart required)");
        jLabelFirmwareMode.setToolTipText("<html><div width=\"480px\">Several firmware modes are available, each supporting different boards and/or adding certain firmware features.<p/><p/><b>Ksoloti Core</b>: The default mode. The Patcher will (only!) detect and connect to Ksoloti Core boards.<p/><p/><b>Axoloti Core</b>: \"Legacy mode\". The Patcher will (only!) detect and connect to Axoloti Core boards.<p/><p/><b>[BOARD] + SPILink</b>: Activates a link between two Cores which lets them sync up and exchange audio and data channels. Make the necessary hardware connections as described in the SPILink help patch, and flash this firmware mode on both Cores to be synced together. Follow the instructions in the SPILink help patch.<p/><p/><b>[BOARD] + USBAudio</b>: [Currently experimental] Activates USB audio functionality while connected to a host. Stream up to 4 channels of audio from and to your computer (DAW). Follow the instructions in the USBAudio help patch. <b>Note:</b> On Windows, you may have to right-click->\"Uninstall\" the drivers both for \"Ksoloti Core\" and \"Ksoloti Bulk Interface\" in the Device Manager to force a driver update. On Linux, you will have to run 'platform_linux_*&#47;add_udev_rules.sh' to update the udev rules.<p/><p/><b>[BOARD] + I2SCodec</b>: [Currently experimental] Activates an I2S stream via SPI3 (PA15, PB3, PB4, PD6). Connect a suitable I2S ADC, DAC, or codec to get 2 additional audio inputs, outputs, or both. Follow the instructions in the I2SCodec help patch.<p/><p/><b>After you've changed modes, click \"Update\" in the resulting \"Firmware Mismatch\" popup to update the Core's firmware automatically to the selected mode.</b></div></html>");

        jComboBoxDialMouseBehaviour.setToolTipText(jLabelDialMouseBehaviour.getToolTipText());
        jComboBoxDialMouseBehaviour.setModel(new DefaultComboBoxModel<String>(new String[] { "Vertical", "Angular" }));
        jComboBoxDialMouseBehaviour.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDialMouseBehaviourActionPerformed(evt);
            }
        });

        jComboBoxFirmwareMode.setModel(new DefaultComboBoxModel<String>(new String[] {
            "Ksoloti Core",
            "Ksoloti Core + SPILink",
            "Ksoloti Core + USBAudio",
            "Ksoloti Core + I2SCodec",
            "Axoloti Core",
            "Axoloti Core + SPILink",
            "Axoloti Core + USBAudio",
            "Axoloti Core + I2SCodec"
        }));
        jComboBoxFirmwareMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxFirmwareModeActionPerformed(evt);
            }
        });
        jComboBoxFirmwareMode.setToolTipText(jLabelFirmwareMode.getToolTipText());

        jLabelFavouritesDir.setText("Favourites Dir");

        jLabelUserShortcutTitle.setText("Object Finder Shortcuts");
        jLabelUserShortcut1.setText("Shift + 1");
        jLabelUserShortcut2.setText("Shift + 2");
        jLabelUserShortcut3.setText("Shift + 3");
        jLabelUserShortcut4.setText("Shift + 4");

        jTextFieldFavDir.setText("test");
        jTextFieldFavDir.setToolTipText("Select a folder/subfolder with patch files to conveniently access them via \'File > Favourites\'.");
        jTextFieldFavDir.setEditable(false);
        jTextFieldFavDir.setCaretColor(new Color(0,0,0,0));

        jTextFieldUserShortcut1.setText("test");
        jTextFieldUserShortcut2.setText("test");
        jTextFieldUserShortcut3.setText("test");
        jTextFieldUserShortcut4.setText("test");

        jTextFieldUserShortcut1.setToolTipText("Enter a string here to create a custom Object Finder shortcut.\nUseful if you have certain objects or a personal library you use a lot.\nIn the patch window, press SHIFT+1 to open shortcut 1, SHIFT+2 for shortcut 2, etc.\nHint: The Object Finder also supports the \"*\" wildcard and regex!");
        jTextFieldUserShortcut2.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        jTextFieldUserShortcut3.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        jTextFieldUserShortcut4.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
        
        jLabelUserShortcutTitle.setToolTipText(jTextFieldUserShortcut1.getToolTipText());
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
        jLabelController.setToolTipText("A controller object runs invisibly in the background on the Core,\nregardless of which patch is currently loaded.\nThis is useful for implementing a patch change system.");

        jTextFieldController.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        jTextFieldController.setToolTipText(jLabelController.getToolTipText());

        jControllerEnabled.setText("Enabled");
        jControllerEnabled.setToolTipText(jLabelController.getToolTipText());
        jControllerEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jControllerEnabledActionPerformed(evt);
            }
        });

        jLabelBackupPatchesOnSD.setText("Backup Patches to SD Card");
        jLabelBackupPatchesOnSD.setToolTipText("Whenever a patch is \'uploaded to SD card\' or \'uploaded to SD card as startup\',\na backup patch file with timestamp is created in the respective folder on SD card.\nExample: \'myCoolPatch.axp.backup_2025-01-31_09-21-58.axp\'\nIf you ever lose a patch file you have previously uploaded to SD card,\nyou can recover a working version from the backup history.\nBackup files in the root directory can hint at the startup patch currently set up on SD card.\n\nRun \'Board > Enter Card Reader Mode\' to get access to the backup files on SD card.");

        jBackupPatchesOnSDEnabled.setText("Enabled");
        jBackupPatchesOnSDEnabled.setToolTipText(jLabelBackupPatchesOnSD.getToolTipText());
        jBackupPatchesOnSDEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBackupPatchesOnSDEnabledActionPerformed(evt);
            }
        });

        jLibraryTable.getTableHeader().setReorderingAllowed(false);
        jLibraryTable.setRowHeight(24);
        jLibraryTable.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Type", "ID", "Location", "Enabled"
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
        jLibraryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPaneLibraryTable.setViewportView(jLibraryTable);
        jLibraryTable.getTableHeader().setReorderingAllowed(false);
        jLibraryTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jLibraryTable.setRowHeight(24);
        if (jLibraryTable.getColumnModel().getColumnCount() > 0) {
            jLibraryTable.getColumnModel().getColumn(0).setPreferredWidth(60);
            jLibraryTable.getColumnModel().getColumn(1).setPreferredWidth(140);
            jLibraryTable.getColumnModel().getColumn(2).setPreferredWidth(280);
            jLibraryTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        }

        jAddLibBtn.setText("➕");
        jAddLibBtn.setToolTipText("Add a library...");
        jAddLibBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddLibBtnActionPerformed(evt);
            }
        });

        jDelLibBtn.setText("❌");
        jDelLibBtn.setToolTipText("Delete the selected library.");
        jDelLibBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDelLibBtnActionPerformed(evt);
            }
        });

        jResetLib.setText("Reset");
        jResetLib.setToolTipText("Reset and re-download the factory and community libraries.");
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


        for (String i : prefs.getThemeList()) {
            jComboBoxTheme.addItem(i);
        }

        jComboBoxTheme.setToolTipText(jLabelTheme.getToolTipText());
        jComboBoxTheme.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxThemeActionPerformed(evt);
            }
        });

        jComboBoxDspSafetyLimit.setModel(new DefaultComboBoxModel<String>(new String[] {
            "Legacy",
            "Very Safe",
            "Safe",
            "Normal",
            "Risky",
            "Very Risky"
        }));
        jLabelDspSafetyLimit.setText("DSP Safety Limit");
        jLabelDspSafetyLimit.setToolTipText("Changes the DSP safety limits.\nThe \'very safe\' and \'safe\' settings ensure a stricter DSP overload threshold and make sure all features in your patches work stable.\nIf you encounter frequent USB disconnects from your computer, try a safer setting.\nThe \'risky\' and \'very risky\' settings set the DSP overload threshold a bit higher,\nallowing you to push your patch load further, but with higher risk of glitches and USB disconnects.\nThe \'Legacy\' setting replicates the original Axoloti Patcher behaviour.");
        jLabelDspSafetyLimit.setEnabled(true);


        jComboBoxDspSafetyLimit.setToolTipText(jLabelDspSafetyLimit.getToolTipText());
        jComboBoxDspSafetyLimit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDspSafetyLimitPerformed(evt);
            }
        });


        jCheckBoxNoMouseReCenter.setText("Touchscreen Mode");
        jCheckBoxNoMouseReCenter.setToolTipText("Makes the Patcher usable with touchscreens.\nAlso fixes abnormal mouse behaviour on some systems when turning knobs.");
        jCheckBoxNoMouseReCenter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxNoMouseReCenterActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(

            layout.createParallelGroup(Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()

                .addGroup(layout.createParallelGroup(Alignment.LEADING)

                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jLabelFavouritesDir)
                            .addComponent(jLabelUserShortcutTitle)
                            .addComponent(jLabelUserShortcut1)
                            .addComponent(jLabelUserShortcut2)
                            .addComponent(jLabelUserShortcut3)
                            .addComponent(jLabelUserShortcut4)
                            .addComponent(jLabelLibraries)
                        )
                        .addGap(15, 15, 15)

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)

                            .addGroup(layout.createSequentialGroup()

                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(jTextFieldFavDir, GroupLayout.PREFERRED_SIZE, 390, GroupLayout.PREFERRED_SIZE)
                                )
                            )
                            .addGroup(layout.createSequentialGroup()

                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(jTextFieldUserShortcut1, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldUserShortcut2, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldUserShortcut3, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldUserShortcut4, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            )
                        )
                        .addGap(12, 12, 12)

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(btnFavDir, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(15, 15, 15)
                    )

                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPaneLibraryTable, GroupLayout.PREFERRED_SIZE, 560, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)

                                .addComponent(jResetLib, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)

                                .addComponent(jLibStatus, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jAddLibBtn, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jDelLibBtn, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                            )
                            .addComponent(jEditLib, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
                        )
                        .addContainerGap(14, Short.MAX_VALUE)
                    )

                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelController, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jControllerEnabled, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextFieldController, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelBackupPatchesOnSD, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jBackupPatchesOnSDEnabled, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelFirmwareMode, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jComboBoxFirmwareMode, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelDspSafetyLimit, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jComboBoxDspSafetyLimit, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelTheme, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jComboBoxTheme, GroupLayout.PREFERRED_SIZE, 240, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED, 106, 106)
                                .addComponent(jButtonSave, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                            )

                        )
                        .addContainerGap())

                    .addGroup(layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelPollInterval, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPollInterval, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                            )
                            
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelCodeFontSize, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jTextFieldCodeFontSize, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                            )

                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelDialMouseBehaviour, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jComboBoxDialMouseBehaviour, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jCheckBoxNoMouseReCenter, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            )

                        )
                        .addGap(0, 0, Short.MAX_VALUE)
                    )
                )
            )
        );

        layout.setVerticalGroup(

            layout.createParallelGroup(Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jTextFieldFavDir)
                    .addComponent(jLabelFavouritesDir)
                    .addComponent(btnFavDir)
                )
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelUserShortcutTitle)
                )
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelUserShortcut1)
                    .addComponent(jTextFieldUserShortcut1)
                )
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelUserShortcut2)
                    .addComponent(jTextFieldUserShortcut2)
                )
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelUserShortcut3)
                    .addComponent(jTextFieldUserShortcut3)
                )
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelUserShortcut4)
                    .addComponent(jTextFieldUserShortcut4)
                )
                .addGap(15, 15, 15)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(jLabelLibraries)
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)

                    .addGroup(layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jAddLibBtn)
                            .addComponent(jDelLibBtn)
                        )
                        .addPreferredGap(ComponentPlacement.RELATED)

                        .addComponent(jEditLib)
                        .addPreferredGap(ComponentPlacement.RELATED)

                            .addComponent(jResetLib)
                        .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(jLibStatus)
                    )

                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPaneLibraryTable, GroupLayout.PREFERRED_SIZE, 114, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jLabelPollInterval)
                            .addComponent(jTextFieldPollInterval, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(5, 5, 5)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jLabelCodeFontSize)
                            .addComponent(jTextFieldCodeFontSize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                    )
                )

                .addGroup(layout.createParallelGroup(Alignment.LEADING)

                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jComboBoxDialMouseBehaviour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelDialMouseBehaviour)
                            .addComponent(jCheckBoxNoMouseReCenter)
                        )
                        .addGap(15, 15, 15)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jLabelController)
                            .addComponent(jControllerEnabled)
                            .addComponent(jTextFieldController, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(15, 15, 15)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jLabelBackupPatchesOnSD)
                            .addComponent(jBackupPatchesOnSDEnabled, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(15, 15, 15)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jLabelFirmwareMode)
                            .addComponent(jComboBoxFirmwareMode, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(5, 5, 5)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jLabelDspSafetyLimit)
                            .addComponent(jComboBoxDspSafetyLimit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(5, 5, 5)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jLabelTheme)
                            .addComponent(jComboBoxTheme, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonSave)
                        )
                    )

                )
                .addGap(15,15,15)
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

    private void jComboBoxFirmwareModeActionPerformed(java.awt.event.ActionEvent evt) {
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

    private void jBackupPatchesOnSDEnabledActionPerformed(java.awt.event.ActionEvent evt) {
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
        if (idx < 0) { /* Return if nothing selected */
            return;
        }

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
        int res = JOptionPane.showConfirmDialog(this, "Reset will re-download the factory and community libraries.\n" +
                                                      "Your custom library entries will be preserved.\n" +
                                                      "Continue?", "Warning", options);
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
        LOGGER.log(Level.INFO, "Checking library status...");
        for (AxolotiLibrary lib : prefs.getLibraries()) {
            lib.reportStatus();
        }
        // LOGGER.log(Level.INFO, "Done checking library status.\n");
    }

    private void jComboBoxThemeActionPerformed(java.awt.event.ActionEvent evt) {
        prefs.setTheme(jComboBoxTheme.getSelectedItem().toString());
        prefs.applyTheme();
        SwingUtilities.updateComponentTreeUI(this); /* Preview theme via preferences window */
    }

    private void jComboBoxDspSafetyLimitPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jCheckBoxNoMouseReCenterActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void editLibraryRow(int idx) {
        if (idx >= 0) {
            DefaultTableModel model = (DefaultTableModel) jLibraryTable.getModel();
            String id = (String) model.getValueAt(idx, 1);
            AxolotiLibrary lib = prefs.getLibrary(id);
            if (lib != null) {
                String type = lib.getType();
                new AxolotiLibraryEditor(this, true, lib);
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


    private JButton btnFavDir;
    private JButton jAddLibBtn;
    private JButton jButtonSave;
    private JCheckBox jCheckBoxNoMouseReCenter;
    private JComboBox<String> jComboBoxDialMouseBehaviour;
    private JComboBox<String> jComboBoxFirmwareMode;
    private JCheckBox jControllerEnabled;
    private JCheckBox jBackupPatchesOnSDEnabled;
    private JButton jDelLibBtn;
    private JButton jEditLib;
    private JLabel jLabelLibraries;
    private JLabel jLabelPollInterval;
    private JLabel jLabelDspSafetyLimit;
    private JComboBox<String> jComboBoxDspSafetyLimit;
    private JLabel jLabelCodeFontSize;
    private JLabel jLabelDialMouseBehaviour;
    private JLabel jLabelFirmwareMode;
    private JLabel jLabelFavouritesDir;
    
    private JLabel jLabelUserShortcutTitle;
    private JLabel jLabelUserShortcut1;
    private JLabel jLabelUserShortcut2;
    private JLabel jLabelUserShortcut3;
    private JLabel jLabelUserShortcut4;

    private JLabel jLabelController;
    private JLabel jLabelBackupPatchesOnSD;
    private JLabel jLabelTheme;
    private JComboBox<String> jComboBoxTheme;
    private JButton jLibStatus;
    private JTable jLibraryTable;
    private JButton jResetLib;
    private ScrollPaneComponent jScrollPaneLibraryTable;
    private JTextField jTextFieldController;
    private JTextField jTextFieldPollInterval;
    private JTextField jTextFieldCodeFontSize;
    private JTextField jTextFieldFavDir;

    private JTextField jTextFieldUserShortcut1;
    private JTextField jTextFieldUserShortcut2;
    private JTextField jTextFieldUserShortcut3;
    private JTextField jTextFieldUserShortcut4;
}
