/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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

import static axoloti.MainFrame.fc;

import axoloti.USBBulkConnection;
import axoloti.listener.ConnectionStatusListener;
import axoloti.listener.SDCardMountStatusListener;
import axoloti.sd.AxoSDFileTableModel;
import axoloti.sd.AxoSDFileTreeCellRenderer;
import axoloti.sd.DisplayTreeNode;
import axoloti.sd.SDCardInfo;
import axoloti.sd.SDFileInfo;
import axoloti.utils.Constants;
import axoloti.utils.Preferences;
import components.ScrollPaneComponent;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import qcmds.CommandManager;
import qcmds.QCmdCreateDirectory;
import qcmds.QCmdDeleteFile;
import qcmds.QCmdGetFileList;
import qcmds.QCmdUploadFile;

/**
 *
 * @author Johannes Taelman
 */
public class FileManagerFrame extends javax.swing.JFrame implements ConnectionStatusListener, SDCardMountStatusListener {

    private static final Logger LOGGER = Logger.getLogger(FileManagerFrame.class.getName());

    private Timer refreshTimer;
    private volatile boolean fileListRefreshInProgress = false;

    private static AxoSDFileTableModel fileTableModel;

    private axoloti.menus.FileMenu fileMenu1;
    private javax.swing.JButton jButtonSDRefresh;
    private javax.swing.JButton jButtonCreateDir;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonUpload;
    private javax.swing.JTable jFileTable;
    private javax.swing.JLabel jLabelSDInfo;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private ScrollPaneComponent jScrollPane1;
    private axoloti.menus.WindowMenu windowMenu1;

    public FileManagerFrame() {
        setPreferredSize(new Dimension(640,400));
        initComponents();
        fileMenu1.initComponents();
        USBBulkConnection.GetConnection().addConnectionStatusListener(this);
        USBBulkConnection.GetConnection().addSDCardMountStatusListener(this);
        setIconImage(Constants.APP_ICON.getImage());
        jLabelSDInfo.setText(" ");

        jFileTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

            private static final EmptyBorder paddingBorder = new EmptyBorder(0, 10, 0, 10); /* 10px padding, left and right */

            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                /* Align names left, type center, size and date right. */
                if (column == 0) {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                else if (column == 1) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                }
                else {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } 

                setBorder(paddingBorder);

                return this;
            }
        });

        jFileTable.setRowHeight(24);

        List<DisplayTreeNode> fileTreeData = SDCardInfo.getInstance().getSortedDisplayNodes();
        fileTableModel = new AxoSDFileTableModel(fileTreeData);
        jFileTable.setModel(fileTableModel);

        TableColumn nameColumn = jFileTable.getColumnModel().getColumn(0);
        nameColumn.setCellRenderer(new AxoSDFileTreeCellRenderer());

        jFileTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                UpdateButtons();
            }
        });

        jFileTable.getTableHeader().setReorderingAllowed(false);

        addCopyRightClick(jFileTable);

        jScrollPane1.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    // @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    if (!USBBulkConnection.GetConnection().isConnected()) {
                        LOGGER.log(Level.WARNING, "Cannot upload files: USB connection not active.");
                        return;
                    }

                    if (droppedFiles.size() > 1) {
                        Object[] options = {"Upload", "Cancel"};
                        int n = KeyboardNavigableOptionPane.showOptionDialog(
                            null,
                            "Upload " + droppedFiles.size() + " files to SD card?",
                            "Upload multiple Files",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            options,
                            options[1]
                        );
                        if (n != JOptionPane.YES_OPTION) {
                            return; /* User canceled the upload */
                        }
                    }

                    CommandManager.getInstance().startLongOperation();
                    new SwingWorker<Void, String>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try {
                                for (File f : droppedFiles) {
                                    System.out.println(Instant.now() + " Drag & drop uploading " + f.getName());

                                    if (!USBBulkConnection.GetConnection().isConnected()) {
                                        LOGGER.log(Level.SEVERE, "Upload aborted: USB connection lost during transfer.");
                                        break; /* Exit the loop for remaining files */
                                    }

                                    if (!f.canRead()) {
                                        LOGGER.log(Level.SEVERE, "Cannot read file: " + f.getName());
                                        continue; /* Skip to next file */
                                    }

                                    QCmdUploadFile uploadFileCmd = new QCmdUploadFile(f, f.getName());
                                    uploadFileCmd.Do(USBBulkConnection.GetConnection());
                                    if (!uploadFileCmd.waitForCompletion() || !uploadFileCmd.isSuccessful()) {
                                        LOGGER.log(Level.SEVERE, "Upload failed for " + f.getName());
                                        break; /* Break loop on first failure */
                                    }
                                }
                            } catch (InterruptedException e) {
                                LOGGER.log(Level.SEVERE, "Thread interrupted while uploading file.", e);
                                Thread.currentThread().interrupt();
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            CommandManager.getInstance().endLongOperation();
                            triggerRefresh();
                        }
                    }.execute();
                }
                catch (UnsupportedFlavorException | IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error during drag and drop file upload: " + ex.getMessage(), ex);
                }
            }
        });

        jScrollPane1.setViewportView(jFileTable);
        jScrollPane1.addMouseListener(new MouseAdapter() {

            /* Clear table selection if clicking outside the cell area */
            @Override
            public void mouseClicked(MouseEvent e) {
                /* Check if the click was NOT on the JTable itself */
                if (!SwingUtilities.isDescendingFrom(e.getComponent(), jFileTable)) {
                    jFileTable.clearSelection();
                    jScrollPane1.requestFocusInWindow();
                }
            }
        });

        if (jFileTable.getColumnModel().getColumnCount() > 0) {
            jFileTable.getColumnModel().getColumn(0).setPreferredWidth(320);

            jFileTable.getColumnModel().getColumn(1).setMinWidth(80);
            jFileTable.getColumnModel().getColumn(1).setPreferredWidth(80);
            jFileTable.getColumnModel().getColumn(1).setMaxWidth(80);

            jFileTable.getColumnModel().getColumn(2).setMinWidth(80);
            jFileTable.getColumnModel().getColumn(2).setPreferredWidth(80);
            jFileTable.getColumnModel().getColumn(2).setMaxWidth(80);

            jFileTable.getColumnModel().getColumn(3).setMinWidth(200);
            jFileTable.getColumnModel().getColumn(3).setPreferredWidth(200);
            jFileTable.getColumnModel().getColumn(3).setMaxWidth(200);
        }

        refreshTimer = new Timer(500, e -> {
            if (!fileListRefreshInProgress) { /* Only proceed if no refresh is already active */
                performActualRefreshAsync();
            }
            else {
                System.out.println(Instant.now() + " Timer fired, but refresh already in progress. Skipping.");
            }
        });
        refreshTimer.setRepeats(false); /* Ensure it's a single-shot timer */
    }
    
    void UpdateButtons() {
        int rows[] = jFileTable.getSelectedRows();

        if (rows.length > 1) {
            // jButtonUpload.setEnabled(false);
            // jButtonCreateDir.setEnabled(false);
            // jButtonDelete.setEnabled(true);
            ButtonUploadDefaultName();
        }
        else if (rows.length == 1) {
            // jButtonUpload.setEnabled(true);
            // jButtonCreateDir.setEnabled(true);

            if (rows[0] < 0) {
                // jButtonDelete.setEnabled(false);
                ButtonUploadDefaultName();
            }
            else {
                // jButtonDelete.setEnabled(true);
                AxoSDFileTableModel model = (AxoSDFileTableModel) jFileTable.getModel();
                DisplayTreeNode displayNode = model.getDisplayTreeNode(rows[0]);
                SDFileInfo f = displayNode.fileInfo;

                if (f != null && f.isDirectory()) {
                    jButtonUpload.setText("Upload to Selected...");
                    jButtonCreateDir.setText("New Folder in Selected...");
                }
                else {
                    ButtonUploadDefaultName();
                }
            }        
        }
        else {
            // jButtonUpload.setEnabled(false);
            // jButtonCreateDir.setEnabled(false);
            // jButtonDelete.setEnabled(false);
            ButtonUploadDefaultName();
        }
    }

    void ButtonUploadDefaultName() {
        jButtonUpload.setText("Upload to Root...");
        jButtonCreateDir.setText("New Folder in Root...");
    }

    private void initComponents() {

        jScrollPane1 = new ScrollPaneComponent();
        jButtonSDRefresh = new javax.swing.JButton();
        jLabelSDInfo = new javax.swing.JLabel();
        jButtonUpload = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonCreateDir = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu1 = new axoloti.menus.FileMenu();
        jMenu2 = new javax.swing.JMenu();
        windowMenu1 = new axoloti.menus.WindowMenu();
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        
        jFileTable = new javax.swing.JTable() {

            /* Override getToolTipText to provide cell-specific tooltips */
            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                /* Ensure the mouse is over a valid cell */
                if (rowIndex != -1 && colIndex != -1) {

                    Object cellValue = getModel().getValueAt(rowIndex, colIndex);
                    String columnName = getModel().getColumnName(colIndex);
                    SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);

                    /* Customize tooltip based on column/row/value */
                    if (columnName.equals("Name")) {
                        return f.getFilename();
                    }
                    else if (columnName.equals("Type")) {
                        return "File size: " + cellValue + " KB";
                    }
                    else if (columnName.equals("Size")) {
                        return "File size: " + cellValue + " KB";
                    }
                    else if (columnName.equals("Modified")) {
                        return "Modified on: " + cellValue;
                    }
                    /* Default tooltip if no specific handling */
                    return columnName + ": " + cellValue;
                }
                /* If not over a cell, return null */
                return null;
            }
        };

        jButtonSDRefresh.setText("Refresh");
        jButtonSDRefresh.setEnabled(false);
        jButtonSDRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSDRefreshActionPerformed(evt);
            }
        });

        jButtonUpload.setText("Upload to Root...");
        jButtonUpload.setEnabled(false);
        jButtonUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUploadActionPerformed(evt);
            }
        });

        jButtonCreateDir.setText("New Folder in Root...");
        jButtonCreateDir.setEnabled(false);
        jButtonCreateDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCreateDirActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.setEnabled(false);
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        fileMenu1.setMnemonic('F');
        fileMenu1.setText("File");
        jMenuBar1.add(fileMenu1);

        jMenu2.setMnemonic('E');
        jMenu2.setText("Edit");
        jMenu2.setDelay(300);
        jMenuBar1.add(jMenu2);
        jMenuBar1.add(windowMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonSDRefresh)
                .addGap(29, 29, 29)
                .addComponent(jLabelSDInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap()
            )
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonUpload)
                .addGap(20, 20, 20)
                .addComponent(jButtonCreateDir)
                .addGap(20, 20, 20)
                .addComponent(jButtonDelete)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            )
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonSDRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelSDInfo)
                )
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jButtonUpload)
                .addComponent(jButtonCreateDir)
                .addComponent(jButtonDelete)
                )
                .addGap(5, 5, 5))
        );

        pack();
    }

    private String normalizePathForDeletion(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // The root path "/" should always remain "/".
        if ("/".equals(path)) {
            return path;
        }
        // If it's a directory path that ends with "/", remove the slash for deletion.
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path; // No change for files or already normalized directory paths.
    }

    private void addCopyRightClick(JTable table) {
        table.addMouseListener(new MouseAdapter() {
            private JPopupMenu popupMenu;
            private JMenuItem copyItem;
            private int clickedRow = -1;
            private int clickedColumn = -1;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                /* Determine the cell that was right-clicked */
                Point p = e.getPoint();
                clickedRow = table.rowAtPoint(p);
                clickedColumn = table.columnAtPoint(p);
                
                /* Only offer to copy from the "Name" column (column index 0) */
                if (clickedRow != -1 && clickedColumn == 0) {
                    table.clearSelection();
                    table.requestFocusInWindow();
                    table.setRowSelectionInterval(clickedRow, clickedRow);
                    if (popupMenu == null) {
                        popupMenu = new JPopupMenu();
                        copyItem = new JMenuItem("Copy Path to Clipboard");
                        copyItem.addActionListener(event -> {
                            if (clickedRow != -1 && clickedColumn == 0) {
                                SDFileInfo f = SDCardInfo.getInstance().getFiles().get(clickedRow);
                                String textToCopy = f.getFilename();
                                copyToClipboard(textToCopy);
                            }
                        });
                        popupMenu.add(copyItem);
                    }
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            private void copyToClipboard(String text) {
                StringSelection stringSelection = new StringSelection(text);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                LOGGER.log(Level.INFO, "Copied to clipboard: " + text);
            }
        });
    }

    private SDFileInfo getFileInfoByPath(String path) {
        return SDCardInfo.getInstance().getFiles().stream()
                         .filter(f -> f.getFilename().equals(path))
                         .findFirst()
                         .orElse(null);
    }

    private boolean deleteSdCardEntryRecursive(String sdCardPath) {
        
        SDFileInfo fileInfo = getFileInfoByPath(sdCardPath);
        if (fileInfo == null) {
            System.out.println(Instant.now() + " Entry '" + sdCardPath + "' does not exist in local SD card model. Nothing to delete.");
            return true; /* Already "deleted" */
        }

        if (fileInfo.isDirectory()) {
            System.out.println(Instant.now() + " Identified as directory. Listing contents for recursive deletion: '" + sdCardPath + "'");

            /* Loop for ensuring the directory is empty */
            while (true) {
                List<SDFileInfo> children = SDCardInfo.getInstance().getFiles().stream()
                    .filter(f -> f.getFilename().startsWith(sdCardPath) && !f.getFilename().equals(sdCardPath))
                    .collect(Collectors.toList());

                if (children.isEmpty()) {
                    /* Directory is truly empty in the current model. */
                    break;
                }

                children.sort(Comparator
                    .comparing((SDFileInfo f) -> f.isDirectory() ? 1 : 0)
                    .thenComparing(SDFileInfo::getFilename).reversed()
                );

                SDFileInfo childToProcess = children.get(0);
                String childPath = childToProcess.getFilename();

                System.out.println(Instant.now() + " Processing child: '" + childPath + "'");
                /* Recursive call: this will eventually hit the file deletion block below.
                   This recursive call itself must correctly wait for its command. */
                if (!deleteSdCardEntryRecursive(childPath)) {
                    System.out.println(Instant.now() + " Failed to recursively delete child: '" + childPath + "'. Aborting directory deletion for: '" + sdCardPath + "'");
                    return false;
                }
                else {
                    SDCardInfo.getInstance().Delete(childPath);
                }
            }

            /* Delete the now-empty directory itself */
            String pathForFatFsDelete = normalizePathForDeletion(sdCardPath);
            System.out.println(Instant.now() + " Attempting to delete empty directory: '" + sdCardPath + "' (normalized for FatFs: '" + pathForFatFsDelete + "')");

            QCmdDeleteFile deleteDirCmd = new QCmdDeleteFile(pathForFatFsDelete);
            deleteDirCmd.Do(USBBulkConnection.GetConnection());
            try {
                if (!deleteDirCmd.waitForCompletion()) {
                    System.out.println(Instant.now() + " QCmdDeleteFile timeout waiting for directory deletion ACK: '" + sdCardPath + "'");
                    return false;
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Thread interrupted while deleting directory.", e);
                Thread.currentThread().interrupt();
            }

            boolean success = deleteDirCmd.isSuccessful();
            if (success) {
                SDCardInfo.getInstance().Delete(sdCardPath);
                System.out.println(Instant.now() + " Successfully deleted directory: '" + sdCardPath + "'");
            }
            else {
                System.out.println(Instant.now() + " Failed to delete empty directory: '" + sdCardPath + "'. Check device status/logs.");
            }
            return success;
        }
        else { /* It's a file */
            String pathForFatFsDelete = normalizePathForDeletion(sdCardPath);
            System.out.println(Instant.now() + " Identified as file. Attempting to delete file: '" + sdCardPath + "' (normalized for FatFs: '" + pathForFatFsDelete + "')");

            QCmdDeleteFile deleteFileCmd = new QCmdDeleteFile(pathForFatFsDelete);
            deleteFileCmd.Do(USBBulkConnection.GetConnection());
            try {
                if (!deleteFileCmd.waitForCompletion()) {
                    System.out.println(Instant.now() + " QCmdDeleteFile timeout waiting for directory deletion ACK: '" + sdCardPath + "'");
                    return false;
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Thread interrupted while deleting directory.", e);
                Thread.currentThread().interrupt();
            }

            boolean success = deleteFileCmd.isSuccessful();
            if (success) {
                SDCardInfo.getInstance().Delete(sdCardPath);
                System.out.println(Instant.now() + " Successfully deleted file: '" + sdCardPath + "'");
            }
            else {
                System.out.println(Instant.now() + " Failed to delete file: '" + sdCardPath + "'. Check device status/logs.");
            }
            return success;
        }
    }

    private void setTableData() {
        fileTableModel.setData(SDCardInfo.getInstance().getSortedDisplayNodes());
    }

    public AxoSDFileTableModel getAxoSDFileTableModel() {
        return fileTableModel;
    }

    public void triggerRefresh() {
        jButtonSDRefresh.setEnabled(false);
        jButtonUpload.setEnabled(false);
        jButtonCreateDir.setEnabled(false);
        jButtonDelete.setEnabled(false);
        
        /* Stop any pending timer. If called multiple times quickly, it just resets the timer. */
        refreshTimer.stop();
        /* Start the timer. The actual refresh will happen after the delay. */
        refreshTimer.start();
        // System.out.println(Instant.now() + " [DEBUG] GetFileList triggered. Timer started/reset.");
    }

    private void performActualRefreshAsync() {
        if (!USBBulkConnection.GetConnection().isConnected() || !USBBulkConnection.GetConnection().GetSDCardPresent()) {
            System.out.println(Instant.now() + " Skipping file list refresh request. Not connected or SD card not present.");
            fileListRefreshInProgress = false;
            return;
        }

        if (CommandManager.getInstance().isLongOperationActive()) {
            // System.out.println(Instant.now() + " [DEBUG] FileManagerFrame: Skipping file list refresh request. Long operation in progress.");
            fileListRefreshInProgress = false; // Ensure flag is reset if it was set by a previous, now-skipped attempt
            return; // Do not proceed with refresh
        }

        if (fileListRefreshInProgress) {
            System.out.println(Instant.now() + " File list refresh already in progress. Skipping.");
            return;
        }

        System.out.println(Instant.now() + " Starting asynchronous UI refresh (via timer)...");
        fileListRefreshInProgress = true; // Set the flag immediately when starting a new refresh

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    System.out.println(Instant.now() + " Sending QCmdGetFileList()...");
                    QCmdGetFileList getFileListCommand = new QCmdGetFileList();
                    /* This call blocks until AxoRd or timeout */
                    getFileListCommand.Do(USBBulkConnection.GetConnection());
                    System.out.println(Instant.now() + " File list refresh command completed. SwingWorker's background task finishing.");
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error during background refresh command execution:", e);
                    throw e; /* Re-throw to be caught by done() */
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); /* Re-throws exceptions from doInBackground */
                    System.out.println(Instant.now() + " Asynchronous UI refresh complete. Updating JTable...");
                    setTableData();
                    refreshUI();
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error during UI refresh processing:", e);
                }
                finally {
                    jButtonSDRefresh.setEnabled(true);
                    jButtonUpload.setEnabled(true);
                    jButtonCreateDir.setEnabled(true);
                    jButtonDelete.setEnabled(true);

                    fileListRefreshInProgress = false;
                    System.out.println(Instant.now() + " JTable updated.");
                }
            }
        }.execute();
    }

    private void jButtonSDRefreshActionPerformed(java.awt.event.ActionEvent evt) {
        triggerRefresh();
    }

    private void jButtonUploadActionPerformed(java.awt.event.ActionEvent evt) {

        String dir = "/";
        int rowIndex = jFileTable.getSelectedRow();
        if (rowIndex >= 0) {

            AxoSDFileTableModel model = (AxoSDFileTableModel) jFileTable.getModel();
            DisplayTreeNode displayNode = model.getDisplayTreeNode(rowIndex);
            SDFileInfo f = displayNode.fileInfo;
            if (f != null && f.isDirectory()) {
                dir = f.getFilename();
            }
        }
        if (USBBulkConnection.GetConnection().isConnected()) {
            fc.resetChoosableFileFilters();
            fc.setCurrentDirectory(new File(Preferences.getInstance().getCurrentFileDirectory()));
            fc.restoreCurrentSize();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(true);
            fc.setDialogTitle("Select File...");

            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fc.getSelectedFiles();

                System.out.println(Instant.now() + " FileManagerFrame: Number of files returned by JFileChooser: " + selectedFiles.length);
                for (int i = 0; i < selectedFiles.length; i++) {
                    System.out.println(Instant.now() + " FileManagerFrame: Selected file [" + i + "]: " + selectedFiles[i].getAbsolutePath());
                }

                if (selectedFiles[0] != null) {
                    Preferences.getInstance().setCurrentFileDirectory(selectedFiles[0].getParentFile().toString());
                }

                final String targetDirectory = dir;
                
                CommandManager.getInstance().startLongOperation();
                new SwingWorker<String, String>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        // System.out.println(Instant.now() + " [DEBUG] SwingWorker: Starting batch upload in background (FULL TEST with detailed logs)...");
                        // System.out.println(Instant.now() + " [DEBUG] SwingWorker: Array length of selectedFiles: " + selectedFiles.length);
                        int uploadedCount = 0;
                        int failedCount = 0;
                        String lastError = null;

                        // int fileCount = 0;
                        for (File file : selectedFiles) {
                            // fileCount++;
                            // System.out.println(Instant.now() + " [DEBUG] SwingWorker: --- Starting iteration " + fileCount + " for file: " + (file != null ? file.getName() : "null") + " ---");

                            if (!USBBulkConnection.GetConnection().isConnected()) {
                                // System.err.println(Instant.now() + " [DEBUG] SwingWorker: Connection lost. Breaking loop for remaining files.");
                                lastError = "Batch upload aborted: USB connection lost.";
                                LOGGER.log(Level.SEVERE, lastError);
                                break; // Exit the loop for remaining files
                            }

                            if (file != null) {
                                if (!file.canRead()) {
                                    // System.err.println(Instant.now() + " [DEBUG] SwingWorker: Cannot read file: " + file.getName() + ". Skipping.");
                                    lastError = "Cannot read file: " + file.getName();
                                    LOGGER.log(Level.SEVERE, lastError);
                                    failedCount++;
                                    continue; // Skip to next file if cannot read
                                }
                                try {
                                    QCmdUploadFile uploadFileCmd = new QCmdUploadFile(file, targetDirectory + file.getName());
                                    uploadFileCmd.Do(USBBulkConnection.GetConnection());

                                    if (!uploadFileCmd.waitForCompletion() || !uploadFileCmd.isSuccessful()) {
                                        lastError = "Upload failed for file: " + file.getName();
                                        LOGGER.log(Level.WARNING, lastError + ". Aborting remaining batch.");
                                        failedCount++;
                                        break;
                                    }
                                    uploadedCount++;
                                }
                                catch (Exception e) {
                                    // System.err.println(Instant.now() + " [DEBUG] SwingWorker: Caught Exception for " + file.getName() + ": " + e.getMessage());
                                    e.printStackTrace(System.err);
                                    lastError = "Error uploading file: " + file.getName() + " - " + e.getMessage();
                                    LOGGER.log(Level.SEVERE, lastError, e);
                                    failedCount++;
                                    break; // Abort batch on any exception
                                }
                            }
                            // System.out.println(Instant.now() + " [DEBUG] SwingWorker: --- Finished iteration " + fileCount + " for file: " + (file != null ? file.getName() : "null") + " ---");
                            
                            try {
                                Thread.sleep(100); // Wait for 100 milliseconds
                                // System.out.println(Instant.now() + " [DEBUG] SwingWorker: Paused for 100ms before next file.");
                            }
                            catch (InterruptedException ie) {
                                Thread.currentThread().interrupt(); // Restore interrupt status
                                // System.err.println(Instant.now() + " [DEBUG] SwingWorker: Delay interrupted.");
                                lastError = "Batch upload interrupted.";
                                LOGGER.log(Level.WARNING, lastError);
                                break; // Abort if interrupted
                            }
                        }
                        // System.out.println(Instant.now() + " [DEBUG] SwingWorker: Loop finished. Processed " + fileCount + " files.");
                        if (uploadedCount == selectedFiles.length && failedCount == 0) {
                            return "All " + uploadedCount + " files uploaded successfully.";
                        }
                        else if (uploadedCount > 0 && failedCount > 0) {
                            return uploadedCount + " file(s) uploaded, but " + failedCount + " failed. Last issue: " + lastError;
                        }
                        else if (failedCount == selectedFiles.length) {
                             return "No files were uploaded. Last issue: " + lastError;
                        }
                        else {
                            return "Upload process completed with issues. Last issue: " + lastError;
                        }
                    }

                    @Override
                    protected void done() {
                        String result = "Upload operation completed.";
                        try {
                            result = get();
                            System.out.println(Instant.now() + " Batch upload SwingWorker completed. Result: " + result);
                        }
                        catch (InterruptedException | ExecutionException e) {
                            result = "Batch upload failed unexpectedly: " + e.getMessage();
                            Logger.getLogger(FileManagerFrame.class.getName()).log(Level.SEVERE, result, e);
                        }
                        finally {
                            CommandManager.getInstance().endLongOperation();
                            triggerRefresh();
                        }
                    }
                }.execute();

            }

            fc.setMultiSelectionEnabled(false);
            fc.updateCurrentSize();
        }
    }

    private void formWindowActivated(java.awt.event.WindowEvent evt) {
        triggerRefresh();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        USBBulkConnection.GetConnection().removeConnectionStatusListener(this);
        USBBulkConnection.GetConnection().removeSDCardMountStatusListener(this);
    }

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        int[] selectedRows = jFileTable.getSelectedRows();

        if (selectedRows.length == 0) {
            // System.out.println(Instant.now() + " [DEBUG] No items selected for deletion.");
            return;
        }

        String confirmationText;
        if (selectedRows.length > 1) {
            confirmationText = "Delete " + selectedRows.length + " items?";
        }
        else {

            AxoSDFileTableModel model = (AxoSDFileTableModel) jFileTable.getModel();
            DisplayTreeNode displayNode = model.getDisplayTreeNode(selectedRows[0]);
            SDFileInfo selectedFile = displayNode.fileInfo;
            if (selectedFile.isDirectory()) {
                confirmationText = "Delete \"" + selectedFile.getFilename() + "\" and all its contents?";
            }
            else {
                confirmationText = "Delete \"" + selectedFile.getFilename() + "\"?";
            }
        }

        Object[] options = {"Delete", "Cancel"};
        int confirmResult = KeyboardNavigableOptionPane.showOptionDialog(
            this,
            confirmationText,
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[1]
        );

        if (confirmResult == JOptionPane.YES_OPTION) {

            AxoSDFileTableModel model = (AxoSDFileTableModel) jFileTable.getModel();

            final List<String> filesToDeletePaths = Arrays.stream(selectedRows)
                                                    .boxed()
                                                    .sorted(Comparator.reverseOrder())
                                                    .map(rowIndex -> model.getDisplayTreeNode(rowIndex).fileInfo.getFilename())
                                                    .collect(Collectors.toList());

            if (filesToDeletePaths.isEmpty()) {
                // System.out.println(Instant.now() + " [DEBUG] No valid file paths collected for deletion.");
                return;
            }

            new SwingWorker<Boolean, String>() {

                @Override
                protected Boolean doInBackground() throws Exception {
                    // System.out.println(Instant.now() + " [DEBUG] Starting batch deletion in background...");
                    boolean overallSuccess = true;

                    for (String fullPath : filesToDeletePaths) {
                        try {
                            // System.out.println(Instant.now() + " [DEBUG] Initiating deletion for selected item: " + fullPath + "");
                            // Call the internal recursive deletion helper
                            boolean deleteSuccess = deleteSdCardEntryRecursive(fullPath);
                            if (!deleteSuccess) {
                                overallSuccess = false;
                                System.out.println(Instant.now() + "Failed to delete: " + fullPath + " (See console/logs for details.)");
                            } else {
                                LOGGER.log(Level.INFO, "Deleted " + fullPath);
                            }
                        }
                        catch (Exception e) {
                            overallSuccess = false;
                            String errMsg = "An unexpected error occurred during deletion of " + fullPath + ": " + e.getMessage();
                            LOGGER.log(Level.SEVERE, errMsg, e);
                            publish(errMsg);
                        }
                    }
                    return overallSuccess;
                }

                @Override
                protected void done() {
                    boolean batchOverallSuccess = false;
                    String message;

                    try {
                        batchOverallSuccess = get();
                        if (batchOverallSuccess) {
                            // System.out.println(Instant.now() + " [DEBUG] Batch deletion SwingWorker completed successfully.");
                            message = "All selected items deleted successfully.";
                        }
                        else {
                            // System.out.println(Instant.now() + " [DEBUG] Batch deletion SwingWorker completed with some failures.");
                            message = "Deletion completed with some failures. Check console/logs for details.";
                        }
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        message = "Delete operation was interrupted.";
                        LOGGER.log(Level.WARNING, message, e);
                    }
                    catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        message = "An unexpected error occurred during deletion: " + (cause != null ? cause.getMessage() : e.getMessage());
                        LOGGER.log(Level.SEVERE, "Batch deletion SwingWorker failed unexpectedly:", cause);
                    }
                    finally {
                        triggerRefresh();
                    }
                }
            }.execute();
        }
    }

    private void jButtonCreateDirActionPerformed(java.awt.event.ActionEvent evt) {
        String dir = "/";
        int rowIndex = jFileTable.getSelectedRow();
        if (rowIndex >= 0) {
            AxoSDFileTableModel model = (AxoSDFileTableModel) jFileTable.getModel();
            DisplayTreeNode displayNode = model.getDisplayTreeNode(rowIndex);
            SDFileInfo f = displayNode.fileInfo;
            if (f != null && f.isDirectory()) {
                dir = f.getFilename();
            }
        }
        
        String fn = JOptionPane.showInputDialog(this, "Enter folder name:");
        if (fn != null && !fn.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            QCmdCreateDirectory createDirCmd = new QCmdCreateDirectory(dir + fn, cal);
            createDirCmd.Do(USBBulkConnection.GetConnection());
            try {
                createDirCmd.waitForCompletion();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Thread interrupted while creating directory.", e);
                Thread.currentThread().interrupt();
            }
        }
        UpdateButtons();
        triggerRefresh();
    }

    public void refreshUI() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshUI);
        }
        else {
            if (jFileTable != null) {
                jFileTable.clearSelection();
                jFileTable.revalidate();
                jFileTable.repaint();
            }
        }

        if (SDCardInfo.getInstance() != null && jLabelSDInfo != null) {
            int clusters = SDCardInfo.getInstance().getClusters();
            int clustersize = SDCardInfo.getInstance().getClustersize();
            int sectorsize = SDCardInfo.getInstance().getSectorsize();
            jLabelSDInfo.setText("Free: " + ((long) clusters * (long) clustersize * (long) sectorsize / (1024 * 1024)) + " MB");
            System.out.println(Instant.now() + " " + String.format("SD free: %d MB, Cluster size: %d", ((long) clusters * (long) clustersize * (long) sectorsize / (1024 * 1024)), (clustersize * sectorsize)));
        }
    }

    void ShowConnect(boolean status) {
        jButtonSDRefresh.setEnabled(status);
        jButtonUpload.setEnabled(status);
        jFileTable.setEnabled(status);
        if (!status) jLabelSDInfo.setText(" ");
        jButtonDelete.setEnabled(status);
        jButtonCreateDir.setEnabled(status);
    }

    @Override
    public void ShowConnect() {
        ShowConnect(true);
    }

    @Override
    public void ShowDisconnect() {
        ShowConnect(false);
        SDCardInfo.getInstance().SetInfo(0, 0, 0);
    }

    @Override
    public void ShowSDCardMounted() {
        ShowConnect(true);
    }

    @Override
    public void ShowSDCardUnmounted() {
        ShowConnect(false);
        SDCardInfo.getInstance().SetInfo(0, 0, 0);        
    }
}
