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

import axoloti.utils.OSDetect;
import axoloti.utils.Preferences;

import java.awt.Dialog;
import java.util.List;
// import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import components.ScrollPaneComponent;
import qcmds.CommandManager;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
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

    private static final Logger LOGGER = Logger.getLogger(USBBulkConnection.class.getName());

    private String cpuid;
    private final String defCPUID;

    public static final String sDFUBootloader = "STM DFU Bootloader";
    public static final String sAxolotiCore = "Axoloti Core";
    public static final String sAxolotiSDCard = "Axoloti SD Card Reader";
    public static final String sAxolotiCoreUsbAudio = "Axoloti Core USB Audio";
    public static final String sKsolotiCore = "Ksoloti Core";
    public static final String sKsolotiSDCard = "Ksoloti SD Card Reader";
    public static final String sKsolotiCoreUsbAudio = "Ksoloti Core USB Audio";

    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonSelect;
    private ScrollPaneComponent jScrollPaneBoardsList;
    private javax.swing.JTable jTableBoardsList;

    private JDialog connectingDialog;
    private JLabel connectingLabel;

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

        setSize(640, 200);
        setTitle("Select Device");
        setLocationRelativeTo(MainFrame.mainframe);

        System.out.println("default cpuid: " + defCPUID);
        this.defCPUID = defCPUID;
        cpuid = defCPUID;

        Populate();

        getRootPane().setDefaultButton(jButtonSelect);

        jTableBoardsList.getTableHeader().setReorderingAllowed(false);

        jTableBoardsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                DefaultTableModel model = (DefaultTableModel) jTableBoardsList.getModel();
                int r = jTableBoardsList.getSelectedRow();

                if (r >= 0) {
                    String devName = (String) model.getValueAt(r, 1);

                    if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core") && (devName.equals(sKsolotiCore) || devName.equals(sKsolotiCoreUsbAudio))) {
                        jButtonSelect.setEnabled(true);
                        cpuid = (String) model.getValueAt(r, 3);
                    }
                    else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core") && (devName.equals(sAxolotiCore) || devName.equals(sAxolotiCoreUsbAudio))) {
                        jButtonSelect.setEnabled(true);
                        cpuid = (String) model.getValueAt(r, 3);
                    }
                    else {
                        jButtonSelect.setEnabled(false);
                    }
                }
                else {
                    cpuid = null;
                }
            }
        });

        jTableBoardsList.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (column != 0) return;

                TableModel model = (TableModel) e.getSource();
                String name = (String) model.getValueAt(row, column);
                String cpuid = (String) ((DefaultTableModel) jTableBoardsList.getModel()).getValueAt(row, 3);
                Preferences.getInstance().setBoardName(cpuid, name);
                Preferences.getInstance().SavePrefs();
                String currentlyConnectedCpuId = USBBulkConnection.getInstance().getDetectedCpuId();
                if (currentlyConnectedCpuId != null && cpuid.equals(currentlyConnectedCpuId)) {
                    USBBulkConnection.getInstance().ShowBoardIDName(cpuid, name);
                }
            }
        });

        if (jTableBoardsList.getColumnModel().getColumnCount() > 0) {
            jTableBoardsList.getColumnModel().getColumn(0).setPreferredWidth(100);
            jTableBoardsList.getColumnModel().getColumn(1).setPreferredWidth(100);
            jTableBoardsList.getColumnModel().getColumn(2).setPreferredWidth(20);
            jTableBoardsList.getColumnModel().getColumn(3).setPreferredWidth(100);
        }

        initConnectingDialog(parent);
    }

    private void initConnectingDialog(java.awt.Frame parent) {
        /* Temporary popup window during the re-connection process */
        connectingDialog = new JDialog(parent, "", Dialog.ModalityType.MODELESS);
        connectingDialog.setUndecorated(true);
        connectingDialog.setSize(360, 60);
        connectingDialog.setResizable(false);

        connectingLabel = new JLabel("Connecting...");
        connectingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        connectingLabel.setVerticalAlignment(SwingConstants.CENTER);
        connectingDialog.add(connectingLabel);
        
        connectingDialog.setLocationRelativeTo(parent);
    }

    private void showConnectingDialog(String boardName) {
        connectingLabel.setText("Connecting to " + boardName + "...");
        connectingDialog.setLocationRelativeTo(this);
        connectingDialog.setVisible(true);
    }

    private void hideConnectingDialog() {
        connectingDialog.setVisible(false);
    }

    public static String ErrorString(int result) {
        if (result < 0) {

            if (OSDetect.getOS() == OSDetect.OS.WIN) {

                if (result == LibUsb.ERROR_NOT_FOUND) {
                    LOGGER.log(Level.WARNING, "You may need to install a compatible driver using Zadig. More info at https://ksoloti.github.io/3-4-rescue_mode.html#zadig_bootloader");
                    return "Inaccessible: driver not installed";
                }
                else if (result == LibUsb.ERROR_ACCESS) {
                    return "Inaccessible: busy?";
                }
            }
            else if (OSDetect.getOS() == OSDetect.OS.LINUX) {

                if (result == LibUsb.ERROR_ACCESS) {
                    LOGGER.log(Level.WARNING, "You may need to add permissions by running platform_linux_*/add_udev_rules.sh. More info at https://ksoloti.github.io/3-install.html#linux_permissions");
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
        DefaultTableModel model = (DefaultTableModel) jTableBoardsList.getModel();
        model.setRowCount(0);
        
        List<String[]> devices = USBBulkConnection.getInstance().getDeviceList();
        for (String[] row : devices) {
            model.addRow(row);
        }

        for (int r = 0; r < model.getRowCount(); r++) {
            String id = (String) model.getValueAt(r, 3);
            if (id.equals(this.defCPUID)) {
                jTableBoardsList.setRowSelectionInterval(r, r);
            }
        }
    }

    public String getCPUID() {
        return cpuid;
    }

    private void initComponents() {

        jButtonSelect = new javax.swing.JButton();
        jButtonClose = new javax.swing.JButton();
        jButtonRefresh = new javax.swing.JButton();
        jScrollPaneBoardsList = new ScrollPaneComponent();
        jTableBoardsList = new javax.swing.JTable() {
            /* Add tooltip functionality */
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                /* Ensure the mouse is over a valid cell */
                if (rowIndex == -1 || colIndex == -1) {
                    return null;
                }

                int modelColumnIndex = convertColumnIndexToModel(colIndex);
                switch (modelColumnIndex) {
                    case 0: /* "Board Name" column */
                        return "Board Name: Double-click to add a human-readable name for your board.\nMake sure you press 'Enter' to confirm your edit.";
                    case 1: /* "Device" column */
                        return "Device: The detected type of the USB device (e.g.,\nKsoloti Core, Axoloti Core, STM DFU Bootloader).\nDouble-click on a row or click \"Select\" below to connect to this device.";
                    case 2: /* "USB Port" column */
                        return "USB Port: The hardware USB port the device is connected to, as defined by your OS.\nDouble-click on a row or click \"Select\" below to connect to this device.";
                    case 3: /* "Board ID" column */
                        return "Board ID: The unique serial number (CPU ID) of the Axoloti/Ksoloti Core.\nDouble-click on a row or click \"Select\" below to connect to this device.";
                    default:
                        return null;
                }
            }
        };


        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setName("Serial port selection"); // NOI18N

        jButtonSelect.setText("Select");
        jButtonSelect.setEnabled(false);
        jButtonSelect.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSelectActionPerformed(evt);
            }
        });

        jButtonClose.setText("Close");
        jButtonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseActionPerformed(evt);
            }
        });

        jButtonRefresh.setText("Refresh");
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jTableBoardsList.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Board Name", "Device", "USB Port", "Board ID"}
        ) {
            Class<?>[] types = new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                    true, false, false, false
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jTableBoardsList.getTableHeader().setReorderingAllowed(false);
        jTableBoardsList.setRowHeight(24);
        jTableBoardsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPaneBoardsList.setViewportView(jTableBoardsList);

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
                        .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(jButtonSelect, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPaneBoardsList, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(146, 146, 146)
                        .addComponent(jButtonRefresh)
                        .addGap(0, 88, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneBoardsList, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonRefresh)
                    .addComponent(jButtonClose)
                    .addComponent(jButtonSelect))
                .addContainerGap())
        );

        pack();
    }

    private void jButtonSelectActionPerformed(java.awt.event.ActionEvent evt) {
        onSelect();
    }

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {
        //port = null;
        setVisible(false);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        Populate();
    }

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {
        if (jButtonSelect.isEnabled()) { /* Lazy way of checking if selected entry is valid for connection */
            if (evt.getClickCount() == 2) {
                onSelect();
            }
        }
    }

    private void onSelect() {
        DefaultTableModel model = (DefaultTableModel) jTableBoardsList.getModel();
        int selRow = jTableBoardsList.getSelectedRow();

        if (selRow < 0) { /* No row selected, should not happen if button is enabled only on selection */
            LOGGER.log(Level.SEVERE, "No board selected. Please select a board to connect.");
            return;
        }

        /* Get the CPU ID from the selected row (column 3 as per TableModel definition) */
        String selectedCpuid = (String) model.getValueAt(selRow, 3);
        if (selectedCpuid == null || selectedCpuid.isEmpty()) {
            LOGGER.log(Level.WARNING, "Could not retrieve Board ID for selected row. Please select a valid board.", "Error");
            return;
        }

        String selectedBoardName = Preferences.getInstance().getBoardName(selectedCpuid); /* ShowBoardIDName below will sort out which to use */
        String displayedName = (selectedBoardName == null || selectedBoardName.trim().isEmpty()) ? selectedCpuid : selectedBoardName;

        /* Show the connecting popup before starting the connection process */
        showConnectingDialog(displayedName);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                boolean disconnected = true;
                if (USBBulkConnection.getInstance().isConnected()) {
                    try {
                        /* Disconnect if a board is currently connected */
                        LOGGER.log(Level.INFO, "Disconnecting board: " + displayedName);
                        USBBulkConnection.getInstance().disconnect();
                        Thread.sleep(500); /* Delay to ensure the disconnect completes before new connection attempt */
                    }
                    catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Error during disconnect of current board: " + displayedName + ", " + ex.getMessage());
                        ex.printStackTrace(System.out);
                        disconnected = false;
                    }
                }

                if (disconnected) {
                    LOGGER.log(Level.INFO, "Attempting to connect to board: " + displayedName);
                    return USBBulkConnection.getInstance().connect();
                }
                return false;
            }

            @Override
            protected void done() {
                /* Hide the connecting dialog after the connection attempt is complete */
                hideConnectingDialog();

                try {
                    boolean connected = get(); /* Get the result from doInBackground() */
                    if (connected) {
                        USBBulkConnection.getInstance().ShowBoardIDName(selectedCpuid, selectedBoardName);
                        setVisible(false); /* Close the dialog on successful connection */
                    }
                    else {
                        LOGGER.log(Level.SEVERE, "Failed to re-connect to board: " + displayedName);
                    }
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error during connection SwingWorker task: " + ex.getMessage());
                    ex.printStackTrace(System.out);
                }
            }
        }.execute();
    }
}
