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

import axoloti.MainFrame;
import axoloti.USBBulkConnection;

import static axoloti.MainFrame.mainframe;
import static axoloti.MainFrame.prefs;
import static axoloti.usb.Usb.DeviceToPath;
import static axoloti.usb.Usb.PID_AXOLOTI;
import static axoloti.usb.Usb.PID_AXOLOTI_SDCARD;
import static axoloti.usb.Usb.PID_AXOLOTI_USBAUDIO;
import static axoloti.usb.Usb.PID_KSOLOTI;
import static axoloti.usb.Usb.PID_KSOLOTI_SDCARD;
import static axoloti.usb.Usb.PID_KSOLOTI_USBAUDIO;


import static axoloti.usb.Usb.PID_STM_DFU;
import static axoloti.usb.Usb.VID_AXOLOTI;
import static axoloti.usb.Usb.VID_STM;

import axoloti.utils.OSDetect;
import static axoloti.utils.OSDetect.getOS;

import java.util.logging.Logger;

import axoloti.utils.Preferences;
import components.ScrollPaneComponent;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

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

        setSize(560, 200);
        setTitle("Select Device");
        setLocation((int)mainframe.getLocation().getX() + 60, (int)mainframe.getLocation().getY() + 80);

        System.out.println("default cpuid: " + defCPUID);
        this.defCPUID = defCPUID;
        cpuid = defCPUID;

        Populate();

        getRootPane().setDefaultButton(jButtonOK);

        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {

                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                int r = jTable1.getSelectedRow();

                if (r >= 0) {
                    String devName = (String) model.getValueAt(r, 1);

                    if (!USBBulkConnection.GetConnection().isConnected() && prefs.getFirmwareMode().contains("Ksoloti Core") && (devName.equals(sKsolotiCore) || devName.equals(sKsolotiCoreUsbAudio))) {
                        jButtonOK.setEnabled(true);
                        cpuid = (String) model.getValueAt(r, 3);
                    }
                    else if (!USBBulkConnection.GetConnection().isConnected() && prefs.getFirmwareMode().contains("Axoloti Core") && (devName.equals(sAxolotiCore) || devName.equals(sAxolotiCoreUsbAudio))) {
                        jButtonOK.setEnabled(true);
                        cpuid = (String) model.getValueAt(r, 3);
                    }
                    else {
                        jButtonOK.setEnabled(false);
                    }
                }
                else {
                    cpuid = null;
                }
            }
        });
        
        jTable1.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if(column!=0) return;
                
                TableModel model = (TableModel)e.getSource();
                String name = (String) model.getValueAt(row, column);
                String cpuid = (String) ((DefaultTableModel) jTable1.getModel()).getValueAt(row, 3);
                Preferences prefs = MainFrame.prefs;
                prefs.setBoardName(cpuid,name);
                prefs.SavePrefs();
            }
        });

        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(100);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(100);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(20);
            jTable1.getColumnModel().getColumn(3).setPreferredWidth(40);
        }
        
        
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
        DeviceList list = new DeviceList();

        int result = LibUsb.init(null);

        if (result < 0) {
            throw new LibUsbException("Unable to initialize LibUsb context", result);
        }

        result = LibUsb.getDeviceList(null, list);

        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }

        try {
            /* Iterate over all devices and scan for the right one */
            for (Device device : list) {

                DeviceDescriptor descriptor = new DeviceDescriptor();
                
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result == LibUsb.SUCCESS) {

                    if (descriptor.idVendor() == VID_STM) {

                        if (descriptor.idProduct() == PID_STM_DFU) {

                            DeviceHandle handle = new DeviceHandle();

                            result = LibUsb.open(device, handle);
                            if (result < 0) {

                                if (getOS() == OSDetect.OS.WIN) {

                                    if (result == LibUsb.ERROR_NOT_SUPPORTED) {
                                        model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Inaccessible: wrong driver installed"});
                                    }
                                    else if (result == LibUsb.ERROR_ACCESS) {
                                        model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Inaccessible: busy?"});
                                    }
                                    else {
                                        model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Inaccessible: " + result});
                                    }
                                }
                                else {
                                    model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Inaccessible: " + result});
                                }
                            }
                            else {
                                model.addRow(new String[]{"",sDFUBootloader, DeviceToPath(device), "Driver OK"});
                                LibUsb.close(handle);
                            }
                        }
                    }
                    else if (prefs.getFirmwareMode().contains("Ksoloti Core") && descriptor.idVendor() == VID_AXOLOTI && ((descriptor.idProduct() == PID_KSOLOTI) || (descriptor.idProduct() == PID_KSOLOTI_USBAUDIO))) {

                        String sName;

                        if (descriptor.idProduct() == PID_KSOLOTI) {
                            sName = sKsolotiCore;
                        }
                        else {
                            sName = sKsolotiCoreUsbAudio;
                        }

                        DeviceHandle handle = new DeviceHandle();

                        result = LibUsb.open(device, handle);
                        if (result < 0) {
                            model.addRow(new String[]{"", sName, DeviceToPath(device), ErrorString(result)});
                        }
                        else {
                            String serial = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());
                            String name = MainFrame.prefs.getBoardName(serial);
                            if(name==null) name = "";
                            model.addRow(new String[]{name, sName, DeviceToPath(device), serial});
                            LibUsb.close(handle);
                        }
                    }
                    else if (prefs.getFirmwareMode().contains("Ksoloti Core") && descriptor.idVendor() == VID_AXOLOTI && descriptor.idProduct() == PID_KSOLOTI_SDCARD) {
                        model.addRow(new String[]{"",sKsolotiSDCard, DeviceToPath(device), "Unmount disk to connect"});
                    }
                    else if (prefs.getFirmwareMode().contains("Axoloti Core") && descriptor.idVendor() == VID_AXOLOTI && ((descriptor.idProduct() == PID_AXOLOTI) || (descriptor.idProduct() == PID_AXOLOTI_USBAUDIO))) {

                        String sName;

                        if (descriptor.idProduct() == PID_AXOLOTI) {
                            sName = sAxolotiCore;
                        }
                        else {
                            sName = sAxolotiCoreUsbAudio;
                        }

                        DeviceHandle handle = new DeviceHandle();

                        result = LibUsb.open(device, handle);
                        
                        if (result < 0) {
                            model.addRow(new String[]{"", sName, DeviceToPath(device), ErrorString(result)});

                        }
                        else {
                            String serial = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());
                            String name = MainFrame.prefs.getBoardName(serial);
                            if(name==null) name = "";
                            model.addRow(new String[]{name, sName, DeviceToPath(device), serial});
                            LibUsb.close(handle);
                        }
                    }
                    else if (prefs.getFirmwareMode().contains("Axoloti Core") && descriptor.idVendor() == VID_AXOLOTI && descriptor.idProduct() == PID_AXOLOTI_SDCARD) {
                        model.addRow(new String[]{"",sAxolotiSDCard, DeviceToPath(device), "Unmount disk to connect"});
                    }
                }
                else {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }
            }

            for (int r = 0; r < model.getRowCount(); r++) {
                String id = (String) model.getValueAt(r, 3);
                if (id.equals(this.defCPUID)) {
                    jTable1.setRowSelectionInterval(r, r);
                }
            }
        }
        finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, true);
        }
    }

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
                "Board Name", "Device", "USB Port", "Board ID"
            }
        ) {
            Class<?>[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
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
            cpuid = (String) model.getValueAt(selRow, 3);
        }
        setVisible(false);        
    }
    
    
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private ScrollPaneComponent jScrollPane2;
    private javax.swing.JTable jTable1;
}
