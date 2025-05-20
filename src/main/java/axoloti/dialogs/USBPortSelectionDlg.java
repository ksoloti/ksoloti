/**
 * Copyright (C) 2015 Johannes Taelman
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

import axoloti.Boards;
import axoloti.Boards.BoardDetail;
import axoloti.Boards.BoardMode;
import axoloti.Boards.BoardType;
import axoloti.Boards.FirmwareType;
import axoloti.Boards.MemoryLayoutType;
import axoloti.MainFrame;

import static axoloti.MainFrame.mainframe;
import static axoloti.MainFrame.prefs;

import axoloti.utils.OSDetect;
import static axoloti.utils.OSDetect.getOS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import components.ScrollPaneComponent;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.usb4java.LibUsb;

/**
 *
 * @author Johannes Taelman
 */
public class USBPortSelectionDlg extends javax.swing.JDialog {

    private String cpuid;
    private final String defCPUID;

    private final String sDFUBootloader = "STM DFU Bootloader";

    private final String sAxolotiCore = "Axoloti Core";
    private final String sAxolotiSDCard = "Axoloti SD Card Reader";
    private final String sAxolotiCoreUsbAudio = "Axoloti Core USB Audio";

    private final String sKsolotiCore = "Ksoloti Core";
    private final String sKsolotiSDCard = "Ksoloti SD Card Reader";
    private final String sKsolotiCoreUsbAudio = "Ksoloti Core USB Audio";

    private final String sKsolotiGekoCore = "Ksoloti Geko Core";
    private final String sKsolotiGekoSDCard = "Ksoloti Geko SD Card Reader";
    private final String sKsolotiGekoCoreUsbAudio = "Ksoloti Geko Core USB Audio";

    private String[] dspSafetyNames = new String[]{"Legacy", "Very Safe", "Safe", "Normal", "Risky", "Very Risky"};

    private enum Columns
    {
        Status      (0),
        Name        (1),
        Device      (2),
        Port        (3),
        Serial      (4),
        Firmware    (5),
        DSP         (6),
        Memory      (7);

        private final int value;
        private Columns(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    /**
     * Creates new form USBPortSelectionDlg
     *
     * @param parent parent frame
     * @param modal is modal
     * @param defCPUID default port name
     */
    public USBPortSelectionDlg(java.awt.Frame parent, boolean modal, String defCPUID) {

        super(parent, modal);

        initComponents();

        setSize(1020, 200);
        setTitle("Select Device");
        setLocation((int)mainframe.getLocation().getX() + 60, (int)mainframe.getLocation().getY() + 80);

        System.out.println("default cpuid: " + defCPUID);
        this.defCPUID = defCPUID;
        cpuid = defCPUID;

        Populate();

        getRootPane().setDefaultButton(jButtonOK);

        // probably a better way to do this but I don't know it!
        FirmwareType[] firmwareTypes = FirmwareType.values();
        String[] firmwareTypeNames = new String[firmwareTypes.length];
        for (int i = 0; i < firmwareTypes.length; i++) {
            firmwareTypeNames[i] = firmwareTypes[i].toString();
        }
        jComboBoxFirmwareMode.setModel(new DefaultComboBoxModel<String>(firmwareTypeNames));

        jComboBoxFirmwareMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxFirmwareModeActionPerformed(evt);
            }
        });
        jComboBoxFirmwareMode.setToolTipText("<html><div width=\"480px\">Several firmware modes are available, each supporting different boards and/or adding certain firmware features.<p/><p/><b>SPI Link</b>: Activates a link between two Cores which lets them sync up and exchange audio and data channels. Make the necessary hardware connections as described in the SPILink help patch, and flash this firmware mode on both Cores to be synced together. Follow the instructions in the SPILink help patch.<p/><p/><b>USB Audio</b>: [Currently experimental] Activates USB audio functionality while connected to a host. Stream up to 4 channels of audio from and to your computer (DAW). Follow the instructions in the USBAudio help patch. <b>Note:</b> On Windows, you may have to right-click->\"Uninstall\" the drivers both for \"Ksoloti Core\" and \"Ksoloti Bulk Interface\" in the Device Manager to force a driver update. On Linux, you will have to run 'platform_linux&#47;add_udev_rules.sh' to update the udev rules.<p/><p/><b>I2S Codec</b>: [Currently experimental] Activates an I2S stream via SPI3 (PA15, PB3, PB4, PD6). Connect a suitable I2S ADC, DAC, or codec to get 2 additional audio inputs, outputs, or both. Follow the instructions in the I2SCodec help patch.<p/><p/><b>After you've changed modes, click \"Update\" in the resulting \"Firmware Mismatch\" popup to update the Core's firmware automatically to the selected mode.</b></div></html>");


        // probably a better way to do this but I don't know it!
        MemoryLayoutType[] memorylayoutTypes = MemoryLayoutType.values();
        String[] memoryLayoutTypeNames = new String[memorylayoutTypes.length];
        for (int i = 0; i < memorylayoutTypes.length; i++) {
            memoryLayoutTypeNames[i] = memorylayoutTypes[i].toString();
        }
        jComboBoxMemoryLayout.setModel(new DefaultComboBoxModel<String>(memoryLayoutTypeNames));

        jComboBoxMemoryLayout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxMemoryLayoutTypeActionPerformed(evt);
            }
        });
        jComboBoxMemoryLayout.setToolTipText("Choose the memory layout for the Geko (TODO better text needed)");
        jComboBoxMemoryLayout.setEnabled(prefs.boards.getBoardType() == BoardType.KsolotiGeko);

        jComboBoxDspSafetyLimit.setModel(new DefaultComboBoxModel<String>(dspSafetyNames));

        jComboBoxDspSafetyLimit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDspSafetyLimitPerformed(evt);
            }
        });

        jComboBoxMemoryLayout.setToolTipText("Changes the DSP safety limits.\nThe \'very safe\' and \'safe\' settings ensure a stricter DSP overload threshold and make sure all features in your patches work stable.\nIf you encounter frequent USB disconnects from your computer, try a safer setting.\nThe \'risky\' and \'very risky\' settings set the DSP overload threshold a bit higher,\nallowing you to push your patch load further, but with higher risk of glitches and USB disconnects.\nThe \'Legacy\' setting replicates the original Axoloti Patcher behaviour.");


        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {

                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                int r = jTable1.getSelectedRow();

                if (r >= 0) {
                    cpuid = (String) model.getValueAt(r, Columns.Serial.value);
                    if(!prefs.boards.getSelectedBoardSerialNumber().equals(cpuid)) {
                        jButtonOK.setEnabled(true);
                    } else {
                        jButtonOK.setEnabled(false);
                        cpuid = null;
                    }
                }
            }
        });
        
        jTable1.getColumnModel().getColumn(Columns.Firmware.getValue()).setCellEditor(new DefaultCellEditor(jComboBoxFirmwareMode));
        jTable1.getColumnModel().getColumn(Columns.DSP.getValue()).setCellEditor(new DefaultCellEditor(jComboBoxDspSafetyLimit));
        jTable1.getColumnModel().getColumn(Columns.Memory.getValue()).setCellEditor(new DefaultCellEditor(jComboBoxMemoryLayout));

        jTable1.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();

                TableModel model = (TableModel)e.getSource();
                if(model.getRowCount() > 0)
                {
                    String cpuid = (String) model.getValueAt(row, Columns.Serial.value);

                    if(column == Columns.Name.getValue()) {
                        String name = (String) model.getValueAt(row, column);
                        prefs.boards.setBoardName(cpuid, name);
                    } else if(column == Columns.Firmware.getValue()) {
                        FirmwareType firmwareType = FirmwareType.fromString((String)model.getValueAt(row, column));
                        prefs.boards.setFirmwareType(cpuid, firmwareType);
                    } else if(column == Columns.DSP.getValue()) {
                        Integer dspSafety = Arrays.asList(dspSafetyNames).indexOf((String)model.getValueAt(row, column));
                        prefs.boards.setDspSafetyLimit(cpuid, dspSafety);
                    } else if(column == Columns.Memory.getValue()) {
                        MemoryLayoutType memoryLayout = MemoryLayoutType.fromString((String)model.getValueAt(row, column));
                        prefs.boards.setMemoryLayout(cpuid, memoryLayout);
                    } else {
                        return;
                    }

                    jButtonOK.setEnabled(true);
                }
            }
        });

        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            jTable1.getColumnModel().getColumn(Columns.Name.getValue()).setPreferredWidth(150);
            jTable1.getColumnModel().getColumn(Columns.Status.getValue()).setPreferredWidth(100);
            jTable1.getColumnModel().getColumn(Columns.Device.getValue()).setPreferredWidth(120);
            jTable1.getColumnModel().getColumn(Columns.Port.getValue()).setPreferredWidth(80);
            jTable1.getColumnModel().getColumn(Columns.Serial.getValue()).setPreferredWidth(190);
            jTable1.getColumnModel().getColumn(Columns.Firmware.getValue()).setPreferredWidth(80);
            jTable1.getColumnModel().getColumn(Columns.DSP.getValue()).setPreferredWidth(80);
            jTable1.getColumnModel().getColumn(Columns.Memory.getValue()).setPreferredWidth(200);
        }
        
        jTable1.setSize(1000, 200);
        jTable1.doLayout();
    }

    public static String ErrorString(int result) {
        if (result < 0) {

            if (getOS() == OSDetect.OS.WIN) {

                if (result == LibUsb.ERROR_NOT_FOUND) {
                    Logger.getLogger(MainFrame.class.getName(), "You may need to install a compatible driver using Zadig. More info at https://ksoloti.github.io/3-4-rescue_mode.html#zadig_bootloader");
                    return "Inaccessible: driver not installed";
                }
                else if (result == LibUsb.ERROR_ACCESS) {
                    return "Inaccessible: busy?";
                }
            }
            else if (getOS() == OSDetect.OS.LINUX) {

                if (result == LibUsb.ERROR_ACCESS) {
                    Logger.getLogger(MainFrame.class.getName(), "You may need to add permissions by running platform_linux/add_udev_rules.sh. More info at https://ksoloti.github.io/3-install.html#linux_permissions");
                    return "Insufficient permissions";
                }
            }

            return "Inaccessible: " + result; /* Mac OS default, fallthrough for Windows and Linux */
        }
        else {
            return null;
        }
    }

    final void Populate() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        
        prefs.getBoards().scanBoards();
        Boards boards = prefs.getBoards();

        HashMap<String, BoardDetail> boardDetails = boards.getBoardDetails();
        
        int r =0;
        for(BoardDetail board : boardDetails.values()) {
            String memoryLayoutString;
            if(board.memoryLayout != null) {
                memoryLayoutString = board.memoryLayout.toString();
            } else {
                memoryLayoutString = "N/A";
            }
            String status;
            if(board.boardMode == BoardMode.SDCard) {
                status = "SDCard";
            } else if(board.boardMode == BoardMode.DFU) {
                status = "DFU";
            } else {
                if (board.isAttached) {
                    status = "Available";
                } else {
                    status = "Not Available";
                }
            }
            
            model.addRow(new String[]{status, board.name, board.boardType.toString(), board.path, board.serialNumber, board.firmwareType.toString(), dspSafetyNames[board.dspSafetyLimit], memoryLayoutString});
            if(boards.getSelectedBoardSerialNumber().equals(board.serialNumber))
                jTable1.setRowSelectionInterval(r,r);
            r++;
        }
    }

        // DeviceList list = new DeviceList();

        // int result = LibUsb.init(null);

        // if (result < 0) {
        //     throw new LibUsbException("Unable to initialize LibUsb context", result);
        // }

        // result = LibUsb.getDeviceList(null, list);

        // if (result < 0) {
        //     throw new LibUsbException("Unable to get device list", result);
        // }

        // try {
        //     /* Iterate over all devices and scan for the right one */
        //     for (Device device : list) {

        //         DeviceDescriptor descriptor = new DeviceDescriptor();
                
        //         result = LibUsb.getDeviceDescriptor(device, descriptor);
        //         if (result == LibUsb.SUCCESS) {

        //             if (descriptor.idVendor() == VID_STM) {

        //                 if (descriptor.idProduct() == PID_STM_DFU) {

        //                     DeviceHandle handle = new DeviceHandle();

        //                     result = LibUsb.open(device, handle);
        //                     if (result < 0) {

        //                         if (getOS() == OSDetect.OS.WIN) {

        //                             if (result == LibUsb.ERROR_NOT_SUPPORTED) {
        //                                 model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Inaccessible: wrong driver installed"});
        //                             }
        //                             else if (result == LibUsb.ERROR_ACCESS) {
        //                                 model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Inaccessible: busy?"});
        //                             }
        //                             else {
        //                                 model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Inaccessible: " + result});
        //                             }
        //                         }
        //                         else {
        //                             model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Inaccessible: " + result});
        //                         }
        //                     }
        //                     else {
        //                         model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Driver OK"});
        //                         LibUsb.close(handle);
        //                     }
        //                 }
        //             }
        //             else if (prefs.isKsolotiDerivative() && descriptor.idVendor() == VID_AXOLOTI && ((descriptor.idProduct() == PID_KSOLOTI) || (descriptor.idProduct() == PID_KSOLOTI_USBAUDIO))) {

        //                 String sName;

        //                 if (descriptor.idProduct() == PID_KSOLOTI) {
        //                     sName = sKsolotiCore;
        //                 }
        //                 else {
        //                     sName = sKsolotiCoreUsbAudio;
        //                 }

        //                 DeviceHandle handle = new DeviceHandle();

        //                 result = LibUsb.open(device, handle);
        //                 if (result < 0) {
        //                     model.addRow(new String[]{"", sName, DeviceToPath(device), ErrorString(result)});
        //                 }
        //                 else {
        //                     String serial = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());
        //                     String name = MainFrame.prefs.getBoardName(serial);
        //                     if(name==null) name = "";
        //                     model.addRow(new String[]{name, sName, DeviceToPath(device), serial});
        //                     LibUsb.close(handle);
        //                 }
        //             }
        //             else if (prefs.isKsolotiDerivative() && descriptor.idVendor() == VID_AXOLOTI && descriptor.idProduct() == PID_KSOLOTI_SDCARD) {
        //                 model.addRow(new String[]{"",sKsolotiSDCard, DeviceToPath(device), "Unmount disk to connect"});
        //             }
        //             else if (prefs.isAxolotiDerivative() && descriptor.idVendor() == VID_AXOLOTI && ((descriptor.idProduct() == PID_AXOLOTI) || (descriptor.idProduct() == PID_AXOLOTI_USBAUDIO))) {

        //                 String sName;

        //                 if (descriptor.idProduct() == PID_AXOLOTI) {
        //                     sName = sAxolotiCore;
        //                 }
        //                 else {
        //                     sName = sAxolotiCoreUsbAudio;
        //                 }

        //                 DeviceHandle handle = new DeviceHandle();

        //                 result = LibUsb.open(device, handle);
                        
        //                 if (result < 0) {
        //                     model.addRow(new String[]{"", sName, DeviceToPath(device), ErrorString(result)});

        //                 }
        //                 else {
        //                     String serial = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());
        //                     String name = MainFrame.prefs.getBoardName(serial);
        //                     if(name==null) name = "";
        //                     model.addRow(new String[]{name, sName, DeviceToPath(device), serial});
        //                     LibUsb.close(handle);
        //                 }
        //             }
        //             else if (prefs.isAxolotiDerivative() && descriptor.idVendor() == VID_AXOLOTI && descriptor.idProduct() == PID_AXOLOTI_SDCARD) {
        //                 model.addRow(new String[]{"",sAxolotiSDCard, DeviceToPath(device), "Unmount disk to connect"});
        //             }
        //         }
        //         else {
        //             throw new LibUsbException("Unable to read device descriptor", result);
        //         }
        //     }

        //     for (int r = 0; r < model.getRowCount(); r++) {
        //         String id = (String) model.getValueAt(r, 3);
        //         if (id != null && id.equals(this.defCPUID)) {
        //             jTable1.setRowSelectionInterval(r, r);
        //         }
        //     }
        // }
        // finally {
        //     // Ensure the allocated device list is freed
        //     LibUsb.freeDeviceList(list, true);
        // }
//    }

    public String getCPUID() {
        return cpuid;
    }


    private void initComponents() {

        jButtonOK = new javax.swing.JButton();
        // jLabel1 = new javax.swing.JLabel();
        jButtonCancel = new javax.swing.JButton();
        jButtonRefresh = new javax.swing.JButton();
        jScrollPane2 = new ScrollPaneComponent();
        jTable1 = new javax.swing.JTable();

        jComboBoxFirmwareMode = new JComboBox<String>();
        jComboBoxMemoryLayout = new JComboBox<String>();
        jComboBoxDspSafetyLimit = new JComboBox<String>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setName("Serial port selection"); // NOI18N

        jButtonOK.setText("OK");
        jButtonOK.setEnabled(false);
        jButtonOK.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jButtonRefresh.setText("Refresh");
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Status", "Board Name", "Device", "USB Port", "Board ID", "Firmware", "DSP Limit", "Memory Layout"
            }
        ) {
            Class<?>[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, false, false, true, true, true
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if(columnIndex == Columns.Memory.value) {
                    jTable1.getModel().getValueAt(rowIndex, Columns.Device.value);
                    BoardType boardType = BoardType.fromString((String)jTable1.getModel().getValueAt(rowIndex, Columns.Device.value));
                    return (boardType == BoardType.KsolotiGeko);
                } else {
                    return canEdit [columnIndex];
                }
            }
        });
        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.setRowHeight(24);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(jButtonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        // .addComponent(jLabel1)
                        .addGap(146, 146, 146)
                        .addComponent(jButtonRefresh)
                        .addGap(0, 88, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(1, 1, 1)
                // .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    // .addComponent(jLabel1)
                    // .addComponent(jButtonRefresh))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonRefresh)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOK))
                .addContainerGap())
        );

        pack();
    }

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {
        onSelect();
    }

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {
        //port = null;
        setVisible(false);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        Populate();
    }

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {
      if (evt.getClickCount() == 2) {
          onSelect();
      }
    }

    private void onSelect() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int selRow = 0;
        if (jTable1.getSelectedRowCount() > 0 ) {
            selRow = jTable1.getSelectedRow();
            cpuid = (String) model.getValueAt(selRow, Columns.Serial.value);

            BoardDetail boardDetail = prefs.boards.getBoardDetail(cpuid);

           
            if(boardDetail != null) {
                prefs.boards.setSelectedBoard(boardDetail);
                prefs.SavePrefs(false);

                axoloti.Axoloti.deletePrecompiledHeaderFile();
                MainFrame.mainframe.updateLinkFirmwareID();
            }
        }
        setVisible(false);        
    }
    
    private void jComboBoxFirmwareModeActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jComboBoxMemoryLayoutTypeActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jComboBoxDspSafetyLimitPerformed(java.awt.event.ActionEvent evt) {
    }

    
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private ScrollPaneComponent jScrollPane2;
    private javax.swing.JTable jTable1;
    private JComboBox<String> jComboBoxDspSafetyLimit;
    private JComboBox<String> jComboBoxMemoryLayout;
    private JComboBox<String> jComboBoxFirmwareMode;
}
