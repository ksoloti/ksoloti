/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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

import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import qcmds.QCmdCreateDirectory;
import qcmds.QCmdDeleteFile;
import qcmds.QCmdGetFileList;
import qcmds.QCmdProcessor;
import qcmds.QCmdStop;
import qcmds.QCmdUploadFile;
import axoloti.SDCardMountStatusListener;
import javax.swing.SwingUtilities;

/**
 *
 * @author Johannes Taelman
 */
public class FileManagerFrame extends javax.swing.JFrame implements ConnectionStatusListener, SDCardMountStatusListener {

    /**
     * Creates new form FileManagerFrame
     */
    public FileManagerFrame() {
        setPreferredSize(new Dimension(800,400));
        initComponents();
        fileMenu1.initComponents();
        USBBulkConnection.GetConnection().addConnectionStatusListener(this);
        USBBulkConnection.GetConnection().addSDCardMountStatusListener(this);
        setIconImage(Constants.APP_ICON.getImage());
        jLabelSDInfo.setText("");

        jFileTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jFileTable.setModel(new AbstractTableModel() {
            private String[] columnNames = {"Name", "Type", "Size", "Modified"};

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public Class getColumnClass(int column) {
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
                        if (f.isDirectory()) {
                            returnValue = f.getFilename();
                        } else {
                            returnValue = f.getFilenameNoExtension();
                        }
                    }
                    break;
                    case 1: {
                        SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);
                        if (f.isDirectory()) {
                            returnValue = "🗀";
                        } else {
                            returnValue = "." + f.getExtension();
                        }
                    }
                    break;
                    case 2: {
                        SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);
                        if (f.isDirectory()) {
                            returnValue = "";
                        } else {
                            int size = f.getSize();
                            if (size < 10240) {
                                returnValue = "" + size + "  bytes";
                            } else if (size < 10240 * 1024) {
                                returnValue = "" + (size / 1024) + " kB";
                            } else {
                                returnValue = "" + (size / (1024 * 1024)) + " MB";
                            }
                        }
                    }
                    break;
                    case 3: {
                        Calendar c = SDCardInfo.getInstance().getFiles().get(rowIndex).getTimestamp();
                        if (c.get(Calendar.YEAR) > 1979) {
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

        jFileTable.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    QCmdProcessor processor = MainFrame.mainframe.getQcmdprocessor();
                    if (USBBulkConnection.GetConnection().isConnected()) {
                        for (File f : droppedFiles) {
                            System.out.println(f.getName());
                            if (!f.canRead()) {
                                Logger.getLogger(FileManagerFrame.class.getName()).log(Level.SEVERE, "Can''t read file");
                            } else {
                                processor.AppendToQueue(new QCmdUploadFile(f, f.getName()));
                            }
                        }
                        RequestRefresh();
                    }
                } catch (UnsupportedFlavorException ex) {
                    Logger.getLogger(FileManagerFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(FileManagerFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        jFileTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                UpdateButtons();
            }
        });
        jFileTable.getTableHeader().setReorderingAllowed(false);

        jScrollPane1.setViewportView(jFileTable);
        if (jFileTable.getColumnModel().getColumnCount() > 0) {
            jFileTable.getColumnModel().getColumn(0).setPreferredWidth(360);
            jFileTable.getColumnModel().getColumn(1).setPreferredWidth(30);
            jFileTable.getColumnModel().getColumn(2).setPreferredWidth(90);
            jFileTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        }
    }
    
    void UpdateButtons(){
        int row = jFileTable.getSelectedRow();
        if (row < 0) {
            jButtonDelete.setEnabled(false);
            ButtonUploadDefaultName();
        } else {
            jButtonDelete.setEnabled(true);
            SDFileInfo f = SDCardInfo.getInstance().getFiles().get(row);
            if (f != null && f.isDirectory()) {
                jButtonUpload.setText("Upload to " + f.getFilename() + " ...");
                jButtonCreateDir.setText("Create Folder in " + f.getFilename() + " ...");
            } else {
                ButtonUploadDefaultName();
            }
        }        
    }

    void ButtonUploadDefaultName() {
        jButtonUpload.setText("Upload...");
        jButtonCreateDir.setText("New Folder...");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
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
        jButtonSDRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSDRefreshActionPerformed(evt);
            }
        });

        jLabelSDInfo.setText("jLabelSDInfo");

        jButtonUpload.setText("Upload...");
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

        jButtonCreateDir.setText("New Folder...");
        jButtonCreateDir.setEnabled(false);
        jButtonCreateDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCreateDirActionPerformed(evt);
            }
        });

        fileMenu1.setText("File");
        jMenuBar1.add(fileMenu1);

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

    void RequestRefresh() {
        if (USBBulkConnection.GetConnection().isConnected() && USBBulkConnection.GetConnection().GetSDCardPresent()) {
            USBBulkConnection.GetConnection().AppendToQueue(new QCmdStop());
            USBBulkConnection.GetConnection().WaitSync();
            USBBulkConnection.GetConnection().AppendToQueue(new QCmdGetFileList());
        }
    }

    private void jButtonSDRefreshActionPerformed(java.awt.event.ActionEvent evt) {
        RequestRefresh();
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
            fc.setDialogTitle("Select File...");
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if (f != null) {
                    prefs.setCurrentFileDirectory(f.getParentFile().toString());
                    if (!f.canRead()) {
                        Logger.getLogger(FileManagerFrame.class.getName()).log(Level.SEVERE, "Can''t read file");
                        return;
                    }
                    processor.AppendToQueue(new QCmdUploadFile(f, dir + f.getName()));
                }
            }
            fc.updateCurrentSize();
        }
    }

    private void formWindowActivated(java.awt.event.WindowEvent evt) {
        RequestRefresh();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        USBBulkConnection.GetConnection().removeConnectionStatusListener(this);
        USBBulkConnection.GetConnection().removeSDCardMountStatusListener(this);
    }

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        int rowIndex = jFileTable.getSelectedRow();

        if (rowIndex >= 0) {

            SDFileInfo f = SDCardInfo.getInstance().getFiles().get(rowIndex);
            String ff = f.getFilename();

            int n = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete \"" + ff + "\"?",
                                                  "Confirm Delete", JOptionPane.YES_NO_OPTION);

            switch (n) {
                case JOptionPane.YES_OPTION: {
                    QCmdProcessor processor = QCmdProcessor.getQCmdProcessor();
                    if (!f.isDirectory()) {
                        processor.AppendToQueue(new QCmdDeleteFile(f.getFilename()));
                    } else {
                        if (ff.endsWith("/")) {
                            ff = ff.substring(0, ff.length() - 1);
                        }
                        processor.AppendToQueue(new QCmdDeleteFile(ff));
                    }
                        break;
                }
                case JOptionPane.NO_OPTION: {
                    break;
                }
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
        String fn = JOptionPane.showInputDialog(this, "Directory name?");
        if (fn != null && !fn.isEmpty()) {
            QCmdProcessor processor = QCmdProcessor.getQCmdProcessor();
            processor.AppendToQueue(new QCmdCreateDirectory(dir + fn));
        }
        UpdateButtons();
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            });
        } else {
            jFileTable.clearSelection();
            jFileTable.revalidate();
            jFileTable.repaint();
        }
            int clusters = SDCardInfo.getInstance().getClusters();
            int clustersize = SDCardInfo.getInstance().getClustersize();
            int sectorsize = SDCardInfo.getInstance().getSectorsize();
            jLabelSDInfo.setText("Free: " + ((long) clusters * (long) clustersize * (long) sectorsize / (1024 * 1024)) + "MB");
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
