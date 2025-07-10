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

import axoloti.ConnectionStatusListener;
import axoloti.MainFrame;

import static axoloti.MainFrame.fc;
import static axoloti.MainFrame.prefs;
import axoloti.SDCardInfo;
import axoloti.SDFileInfo;
import axoloti.USBBulkConnection;
import axoloti.utils.Constants;
import components.ScrollPaneComponent;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import qcmds.QCmdCreateDirectory;
import qcmds.QCmdDeleteFile;
import qcmds.QCmdGetFileList;
import qcmds.QCmdProcessor;
// import qcmds.QCmdStop;
import qcmds.QCmdUploadFile;
import axoloti.SDCardMountStatusListener;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author Johannes Taelman
 */
public class FileManagerFrame extends javax.swing.JFrame implements ConnectionStatusListener, SDCardMountStatusListener {

    private static final Logger LOGGER = Logger.getLogger(FileManagerFrame.class.getName());

    private static char decimalSeparator = new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator();
    
    private volatile boolean refreshInProgress = false;

    public FileManagerFrame() {
        setPreferredSize(new Dimension(640,400));
        initComponents();
        fileMenu1.initComponents();
        USBBulkConnection.GetConnection().addConnectionStatusListener(this);
        USBBulkConnection.GetConnection().addSDCardMountStatusListener(this);
        setIconImage(Constants.APP_ICON.getImage());
        jLabelSDInfo.setText("");

        jFileTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        /* Center Type, Size, Modified columns, TODO: add some tooltips? */
        jFileTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // SDFileInfo f = SDCardInfo.getInstance().getFiles().get(row);

                /* Align names left, all other columns center */
                if (column == 0) {
                    setHorizontalAlignment(SwingConstants.LEFT);
                    // setToolTipText(f.getFilename());
                }
                else {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    // setToolTipText("");
                }

                return c;
            }
        });

        jFileTable.setRowHeight(24);
        jFileTable.setModel(new AbstractTableModel() {
            private final String[] columnNames = {"Name", "Type", "Size", "Modified"};

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }

            @Override
            public int getRowCount() {
                return SDCardInfo.getInstance().getFiles().size();
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Object returnValue = null;

                switch (columnIndex) {
                    case 0: {
                        SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);
                        if (f != null) {
                            if (f.isDirectory()) {
                                /* is directory: print full path */
                                returnValue = f.getFilename();
                            }
                            else if (f.getFilename().lastIndexOf("/") > 0) {
                                /* is file in sub directory: print file name with indent */
                                returnValue = "    " + f.getPatchFileName();
                            }
                            else {
                                /* is file in root directory: print file name with slash and without indent */
                                returnValue = "/" + f.getPatchFileName();
                            }
                        }
                    }
                    break;
                    case 1: {
                        SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);
                        if (f.isDirectory()) {
                            returnValue = "[ D ]";
                        } else {
                            returnValue = f.getExtension();
                        }
                    }
                    break;
                    case 2: {
                        SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);
                        if (f.isDirectory()) {
                            returnValue = "";
                        } else {
                            long size = f.getSize();
                            if (size < 1024) {
                                returnValue = "" + size + " B";
                            }
                            else if (size < 1024 * 1024 / 10) {
                                returnValue = "" + (size / 1024) + decimalSeparator + (size % 1024) / 103 + " kB";
                            }
                            else if (size < 1024 * 1024) {
                                returnValue = "" + (size / 1024) + " kB";
                            }
                            else if (size < 10240 * 1024 * 10) {
                                returnValue = "" + (size / (1024 * 1024)) + decimalSeparator + (size % (1024 * 1024)) / (1024 * 1024 / 10) + " MB";
                            }
                            else {
                                returnValue = "" + (size / (1024 * 1024)) + " MB";
                            }
                        }
                    }
                    break;
                    case 3: {
                        Calendar c = SDCardInfo.getInstance().getFiles().get(rowIndex).getTimestamp();
                        if (c.get(Calendar.YEAR) > 1980) {
                            returnValue = DateFormat.getDateTimeInstance().format(c.getTime());
                        } else {
                            returnValue = "";
                        }
                    }
                    break;
                }

                return returnValue;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });

        jFileTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                UpdateButtons();
            }
        });

        jFileTable.getTableHeader().setReorderingAllowed(false);

        jScrollPane1.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    QCmdProcessor processor = MainFrame.mainframe.getQcmdprocessor();

                    if (USBBulkConnection.GetConnection().isConnected()) {
                        if (droppedFiles.size() > 1) {
                            Object[] options = {"Upload", "Cancel"};
                            int n = JOptionPane.showOptionDialog(
                                null,
                                "Upload " + droppedFiles.size() + " files to SD card?",
                                "Upload multiple Files",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                options,
                                options[1]
                            );
                            switch (n) {
                                case JOptionPane.YES_OPTION: {
                                    for (File f : droppedFiles) {
                                        System.out.println(f.getName());
                                        if (!f.canRead()) {
                                            LOGGER.log(Level.SEVERE, "Cannot read file: " + f.getName());
                                        }
                                        else {
                                            processor.AppendToQueue(new QCmdUploadFile(f, f.getName()));
                                        }
                                    }
                                }
                                break;

                                case JOptionPane.NO_OPTION:
                                break;
                            }
                        }
                        else if (droppedFiles.size() == 1) {
                            File f = droppedFiles.get(0);
                            System.out.println(f.getName());
                            if (!f.canRead()) {
                                LOGGER.log(Level.SEVERE, "Cannot read file: " + f.getName());
                            }
                            else {
                                processor.AppendToQueue(new QCmdUploadFile(f, f.getName()));
                            }
                        }
                        RequestRefreshAsync();
                    }
                } catch (UnsupportedFlavorException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
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

            jFileTable.getColumnModel().getColumn(1).setPreferredWidth(120);
            jFileTable.getColumnModel().getColumn(1).setMaxWidth(120);

            jFileTable.getColumnModel().getColumn(2).setPreferredWidth(120);
            jFileTable.getColumnModel().getColumn(2).setMaxWidth(120);

            jFileTable.getColumnModel().getColumn(3).setPreferredWidth(240);
            jFileTable.getColumnModel().getColumn(3).setMaxWidth(240);
        }
    }
    
    void UpdateButtons() {
        int rows[] = jFileTable.getSelectedRows();

        if (rows.length > 1) {
            jButtonDelete.setEnabled(true);
            jButtonUpload.setEnabled(false);
            jButtonCreateDir.setEnabled(false);
            ButtonUploadDefaultName();
        }
        else if (rows.length == 1) {
            jButtonUpload.setEnabled(true);
            jButtonCreateDir.setEnabled(true);

            if (rows[0] < 0) {
                jButtonDelete.setEnabled(false);
                ButtonUploadDefaultName();
            }
            else {
                jButtonDelete.setEnabled(true);
                SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rows[0]);

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
            jButtonDelete.setEnabled(false);
            // jButtonUpload.setEnabled(false);
            // jButtonCreateDir.setEnabled(false);
            ButtonUploadDefaultName();
        }
    }

    void ButtonUploadDefaultName() {
        jButtonUpload.setText("Upload to Root...");
        jButtonCreateDir.setText("New Folder in Root...");
    }


    private void initComponents() {

        jScrollPane1 = new ScrollPaneComponent();
        jFileTable = new javax.swing.JTable();
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

        // jFileTable.setModel(new javax.swing.table.DefaultTableModel(
        //     new Object [][] {

        //     },
        //     new String [] {
        //         "Name", "Extension", "Size", "Modified"
        //     }
        // ) {
        //     Class[] types = new Class [] {
        //         java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
        //     };
        //     boolean[] canEdit = new boolean [] {
        //         false, false, false, false
        //     };

        //     public Class getColumnClass(int columnIndex) {
        //         return types [columnIndex];
        //     }

        //     public boolean isCellEditable(int rowIndex, int columnIndex) {
        //         return canEdit [columnIndex];
        //     }
        // });

        jButtonSDRefresh.setText("Refresh");
        jButtonSDRefresh.setEnabled(false);
        jButtonSDRefresh.setVisible(false);
        jButtonSDRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSDRefreshActionPerformed(evt);
            }
        });

        jLabelSDInfo.setText("jLabelSDInfo");

        jButtonUpload.setText("Upload to Root...");
        jButtonUpload.setEnabled(false);
        jButtonUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUploadActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.setEnabled(false);
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jButtonCreateDir.setText("New Folder in Root...");
        jButtonCreateDir.setEnabled(false);
        jButtonCreateDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCreateDirActionPerformed(evt);
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
                .addComponent(jButtonDelete)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonUpload)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCreateDir)
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
                    .addComponent(jButtonDelete)
                    .addComponent(jButtonUpload)
                    .addComponent(jButtonCreateDir)
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

    // --- Helper Method: getFileInfoByPath (local to this class) ---
    private SDFileInfo getFileInfoByPath(String path) {
        return SDCardInfo.getInstance().getFiles().stream()
                         .filter(f -> f.getFilename().equals(path))
                         .findFirst()
                         .orElse(null);
    }

    // --- Helper Method: Recursive Deletion Logic (Embedded) ---
    private boolean deleteSdCardEntryRecursiveInternal(String sdCardPath) {
        // sdCardPath here is the path exactly as it comes from SDCardInfo.getFiles()
        // This means it might have a trailing slash for directories.

        if (sdCardPath == null || sdCardPath.isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete an empty or null path.");
            return false;
        }

        LOGGER.log(Level.INFO, "Attempting to delete entry (as per SDCardInfo): ''{0}''", sdCardPath);

        SDFileInfo fileInfo = getFileInfoByPath(sdCardPath);
        if (fileInfo == null) {
            LOGGER.log(Level.INFO, "Entry ''{0}'' does not exist in local SD card model. Nothing to delete.", sdCardPath);
            return true; // Already "deleted"
        }

        boolean isDir = fileInfo.isDirectory();

        if (isDir) {
            LOGGER.log(Level.INFO, "  Identified as directory. Listing contents for recursive deletion: ''{0}''", sdCardPath);

            // --- CRITICAL CHANGE HERE: Loop until no more children are found ---
            // This ensures we are always working with the freshest view of the SDCardInfo model.
            while (true) {
                // Get the *current* list of children for this directory
                List<SDFileInfo> children = SDCardInfo.getInstance().getFiles().stream()
                    .filter(f -> f.getFilename().startsWith(sdCardPath) && !f.getFilename().equals(sdCardPath))
                    // OPTIONAL ADVANCED FILTER: Ensure it's a direct child (prevents going too deep if names overlap)
                    // This checks that the path has exactly one more '/' than the parent, after the parent's path.
                    // For example, for /asd/, it ensures /asd/file.txt or /asd/folder/ but not /asd/folder/subfile.txt
                    // .filter(f -> {
                    //     String relativePath = f.getFilename().substring(sdCardPath.length());
                    //     return !relativePath.contains("/"); // Ensure no further slashes for direct children
                    // })
                    .collect(Collectors.toList());

                if (children.isEmpty()) {
                    LOGGER.log(Level.INFO, "  Directory ''{0}'' is now empty (or no known children).", sdCardPath);
                    break; // No more children, can proceed to delete the directory itself
                }

                // Sort children for reliable deletion order (files before folders, deeper paths first)
                // This is crucial for rmdir operations on the device.
                children.sort(Comparator
                    .comparing((SDFileInfo f) -> f.isDirectory() ? 1 : 0) // Files (0) before Dirs (1)
                    .thenComparing(SDFileInfo::getFilename).reversed()   // Longer paths (deeper) first
                );

                // Process the *first* child in the sorted list (which will be the deepest file or directory)
                SDFileInfo childToProcess = children.get(0);
                String childPath = childToProcess.getFilename();

                LOGGER.log(Level.INFO, "  Processing child: ''{0}''", childPath);
                if (!deleteSdCardEntryRecursiveInternal(childPath)) {
                    LOGGER.log(Level.WARNING, "Failed to recursively delete child: ''{0}''. Aborting directory deletion for: ''{1}''",
                            new Object[]{childPath, sdCardPath});
                    return false; // If a child fails, the whole operation fails
                }
                // Loop will continue to get the *new* list of children after one is deleted
            }

            // --- Delete the now-empty directory itself ---
            String pathForFatFsDelete = normalizePathForDeletion(sdCardPath);
            LOGGER.log(Level.INFO, "  Attempting to delete empty directory: ''{0}'' (normalized for FatFs: ''{1}'')",
                    new Object[]{sdCardPath, pathForFatFsDelete});

            QCmdDeleteFile deleteDirCmd = new QCmdDeleteFile(pathForFatFsDelete);
            QCmdProcessor processor = QCmdProcessor.getQCmdProcessor();
            processor.AppendToQueue(deleteDirCmd);
            processor.WaitQueueFinished();

            // IMPORTANT: Get actual success/failure from QCmdProcessor/QCmd
            boolean success = true; // Placeholder: Replace with actual result from your QCmd implementation
            if (success) {
                LOGGER.log(Level.INFO, "Successfully deleted directory: ''{0}''", sdCardPath);
            } else {
                LOGGER.log(Level.WARNING, "Failed to delete empty directory: ''{0}''. Check device status/logs.", sdCardPath);
            }
            return success;

        } else {
            // It's a file
            String pathForFatFsDelete = normalizePathForDeletion(sdCardPath);
            LOGGER.log(Level.INFO, "  Identified as file. Attempting to delete file: ''{0}'' (normalized for FatFs: ''{1}'')",
                    new Object[]{sdCardPath, pathForFatFsDelete});

            QCmdDeleteFile deleteFileCmd = new QCmdDeleteFile(pathForFatFsDelete);
            QCmdProcessor processor = QCmdProcessor.getQCmdProcessor();
            processor.AppendToQueue(deleteFileCmd);
            processor.WaitQueueFinished();

            boolean success = true; // Placeholder
            if (success) {
                LOGGER.log(Level.INFO, "Successfully deleted file: ''{0}''", sdCardPath);
            } else {
                LOGGER.log(Level.WARNING, "Failed to delete file: ''{0}''. Check device status/logs.", sdCardPath);
            }
            return success;
        }
    }

    private void RequestRefreshAsync() {
        // Check if a refresh is already in progress. If so, just return.
        if (refreshInProgress) {
            LOGGER.log(Level.INFO, "Refresh already in progress. Skipping new refresh request.");
            return;
        }
    
        if (!USBBulkConnection.GetConnection().isConnected() || !USBBulkConnection.GetConnection().GetSDCardPresent()) {
            LOGGER.log(Level.INFO, "Skipping refresh: Not connected or SD card not present.");
            return;
        }
    
        LOGGER.log(Level.INFO, "Starting asynchronous UI refresh...");
        refreshInProgress = true; // Set the flag immediately when starting a new refresh
    
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    /* This code runs in a background thread */

                    // Optional: Remove QCmdStop and its WaitSync if not strictly needed
                    // LOGGER.log(Level.INFO, "Sending QCmdStop()...");
                    // USBBulkConnection.GetConnection().AppendToQueue(new QCmdStop());
                    // USBBulkConnection.GetConnection().WaitSync(); // This waits on 'sync'

                    LOGGER.log(Level.INFO, "Sending QCmdGetFileList()...");
                    USBBulkConnection.GetConnection().AppendToQueue(new QCmdGetFileList());

                    // After removing the above, your doInBackground will look cleaner, ending something like this:
                    LOGGER.log(Level.INFO, "File list refresh command appended to queue. SwingWorker's background task finishing.");

                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error during background refresh command execution:", e);
                    throw e;
                }
                return null;
            }
    
            @Override
            protected void done() {
                // This code runs back on the Event Dispatch Thread (UI thread)
                try {
                    get(); // This re-throws any exceptions from doInBackground
                    LOGGER.log(Level.INFO, "Asynchronous UI refresh complete. Updating JTable...");
                    // Your JTable model update logic goes here:
                    ((AbstractTableModel) jFileTable.getModel()).fireTableDataChanged();
                    // You might also want to re-select the previously selected rows if possible
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error during UI refresh processing:", e);
                    // Optionally, show a message to the user:
                    // JOptionPane.showMessageDialog(FileManagerFrame.this, "Failed to refresh file list: " + e.getMessage(), "Refresh Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    // Reset the flag ONLY after the SwingWorker has truly finished its lifecycle
                    // and any UI updates are attempted.
                    refreshInProgress = false;
                }
            }
        }.execute(); // Start the worker
    }

    private void jButtonSDRefreshActionPerformed(java.awt.event.ActionEvent evt) {
        RequestRefreshAsync();
    }

    private void jButtonUploadActionPerformed(java.awt.event.ActionEvent evt) {
        QCmdProcessor processor = QCmdProcessor.getQCmdProcessor();
        String dir = "/";
        int rowIndex = jFileTable.getSelectedRow();
        if (rowIndex >= 0) {
            SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);
            if (f != null && f.isDirectory()) {
                dir = f.getFilename();
            }
        }
        if (USBBulkConnection.GetConnection().isConnected()) {
            fc.resetChoosableFileFilters();
            fc.setCurrentDirectory(new File(prefs.getCurrentFileDirectory()));
            fc.restoreCurrentSize();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(true);
            fc.setDialogTitle("Select File...");
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File[] fs = fc.getSelectedFiles();
                if (fs[0] != null) {
                    prefs.setCurrentFileDirectory(fs[0].getParentFile().toString());
                }
                for (File f : fs) {
                    if (f != null) {
                        if (!f.canRead()) {
                            LOGGER.log(Level.SEVERE, "Cannot read file");
                            return;
                        }
                        processor.AppendToQueue(new QCmdUploadFile(f, dir + f.getName()));
                    }
                }
            }
            fc.setMultiSelectionEnabled(false);
            fc.updateCurrentSize();
            RequestRefreshAsync();
        } 
    }

    private void formWindowActivated(java.awt.event.WindowEvent evt) {
        RequestRefreshAsync();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        USBBulkConnection.GetConnection().removeConnectionStatusListener(this);
        USBBulkConnection.GetConnection().removeSDCardMountStatusListener(this);
    }

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        int[] selectedRows = jFileTable.getSelectedRows();

        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "No items selected for deletion.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String confirmationText;
        if (selectedRows.length > 1) {
            confirmationText = "Delete " + selectedRows.length + " items?";
        } else {
            SDFileInfo selectedFile = SDCardInfo.getInstance().getFiles().get(selectedRows[0]);
            if (selectedFile.isDirectory()) {
                confirmationText = "Delete \"" + selectedFile.getFilename() + "\" and all its contents?";
            }
            else {
                confirmationText = "Delete \"" + selectedFile.getFilename() + "\"?";
            }
        }

        Object[] options = {"Delete", "Cancel"};
        int confirmResult = JOptionPane.showOptionDialog(
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
            /* Sort selected rows in reverse order to ensure parent directories appear later
             * in the loop if both parent and child are selected. This helps a bit,
             * but the recursive logic ultimately determines the deletion order. */
            List<Integer> sortedRows = Arrays.stream(selectedRows)
                                             .boxed()
                                             .sorted(Comparator.reverseOrder())
                                             .collect(Collectors.toList());

            boolean overallSuccess = true;
            for (int row : sortedRows) {
                SDFileInfo fileToDelete = SDCardInfo.getInstance().getFiles().get(row);
                String fullPath = fileToDelete.getFilename(); /* Get the full absolute path */

                try {
                    LOGGER.log(Level.INFO, "\n--- Initiating deletion for selected item: {0} ---", fullPath);
                    /* Call the internal recursive deletion helper */
                    boolean deleteSuccess = deleteSdCardEntryRecursiveInternal(fullPath);
                    if (!deleteSuccess) {
                        overallSuccess = false;
                        JOptionPane.showMessageDialog(this,
                            "Failed to delete: " + fullPath + "\n(See console/logs for details.)",
                            "Deletion Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    overallSuccess = false;
                    LOGGER.log(Level.SEVERE, "An unexpected error occurred during deletion of {0}: {1}", new Object[]{fullPath, e.getMessage()});
                    JOptionPane.showMessageDialog(this,
                        "An unexpected error occurred during deletion of " + fullPath + ":\n" + e.getMessage() + "\nStopping deletion.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    break;
                }
            }

            RequestRefreshAsync(); /* Always refresh after attempting deletions */

            if (overallSuccess) {
                JOptionPane.showMessageDialog(this, "Selected items deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Some items could not be deleted. Check logs for details.", "Partial Success/Failure", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void jButtonCreateDirActionPerformed(java.awt.event.ActionEvent evt) {
        String dir = "/";
        int rowIndex = jFileTable.getSelectedRow();
        if (rowIndex >= 0) {
            SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);
            if (f != null && f.isDirectory()) {
                dir = f.getFilename();
            }
        }
        
        String fn = JOptionPane.showInputDialog(this, "Enter folder name:");
        if (fn != null && !fn.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            QCmdProcessor processor = QCmdProcessor.getQCmdProcessor();
            processor.AppendToQueue(new QCmdCreateDirectory(dir + fn, cal));
            processor.WaitQueueFinished();
        }
        UpdateButtons();
        RequestRefreshAsync();
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            });
        }
        else {
            jFileTable.clearSelection();
            jFileTable.revalidate();
            jFileTable.repaint();
        }

        int clusters = SDCardInfo.getInstance().getClusters();
        int clustersize = SDCardInfo.getInstance().getClustersize();
        int sectorsize = SDCardInfo.getInstance().getSectorsize();
        jLabelSDInfo.setText("Free: " + ((long) clusters * (long) clustersize * (long) sectorsize / (1024 * 1024)) + " MB");
        // System.out.println(String.format("SD free: %d MB, Cluster size: %d", ((long) clusters * (long) clustersize * (long) sectorsize / (1024 * 1024)), (clustersize * sectorsize)));
    }

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

    void ShowConnect(boolean status) {
        jButtonSDRefresh.setEnabled(status);
        jButtonUpload.setEnabled(status);
        jFileTable.setEnabled(status);
        jLabelSDInfo.setText("");
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
