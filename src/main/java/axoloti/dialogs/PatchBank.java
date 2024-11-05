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
package axoloti.dialogs;

import axoloti.ConnectionStatusListener;
import axoloti.DocumentWindow;
import axoloti.DocumentWindowList;
import static axoloti.FileUtils.axpFileFilter;
import axoloti.MainFrame;

import static axoloti.MainFrame.fc;
import static axoloti.MainFrame.prefs;
import axoloti.PatchFrame;
import axoloti.PatchGUI;
import axoloti.SDCardInfo;
import axoloti.SDCardMountStatusListener;
import axoloti.SDFileInfo;
import axoloti.USBBulkConnection;
import components.ScrollPaneComponent;

import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import qcmds.QCmdProcessor;
import qcmds.QCmdUploadFile;

/**
 *
 * @author jtaelman
 */
public class PatchBank extends javax.swing.JFrame implements DocumentWindow, ConnectionStatusListener, SDCardMountStatusListener {

    private static final Logger LOGGER = Logger.getLogger(PatchBank.class.getName());

    String FilenamePath = null;

    final String fileExtension = ".axb";

    boolean dirty = false;

    ArrayList<File> files;

    /**
     * Creates new form PatchBank
     */
    public PatchBank() {
        setPreferredSize(new Dimension(800, 600));
        initComponents();
        fileMenu1.initComponents();
        files = new ArrayList<File>();
        DocumentWindowList.RegisterWindow(this);
        USBBulkConnection.GetConnection().addConnectionStatusListener(this);
        setIconImage(new ImageIcon(getClass().getResource("/resources/ksoloti_icon_axb.png")).getImage());

        jTable1.setModel(new AbstractTableModel() {
            private final String[] columnNames = {"Index", "File", "Found locally", "Found on SD"};

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
                switch (column) {
                    case 0:
                        return Integer.class;
                    case 1:
                        return String.class;
                    case 2:
                        return String.class;
                    case 3:
                        return String.class;
                }
                return null;
            }

            @Override
            public int getRowCount() {
                return files.size();
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {

                switch (columnIndex) {
                    case 0:
                        break;
                    case 1:
                        String svalue = (String) value;
                        if (svalue != null && !svalue.isEmpty()) {
                            File f = new File(svalue);
                            if (f.exists() && f.isFile() && f.canRead()) {
                                files.set(rowIndex, f);
                                setDirty();
                                refresh();
                            }
                        }
                        break;
                    case 2:
                    case 3:
                        break;
                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Object returnValue = null;

                switch (columnIndex) {
                    case 0:
                        returnValue = Integer.toString(rowIndex);
                    break;
                    case 1: {
                        File f = files.get(rowIndex);
                        if (f != null) {
                            returnValue = toRelative(f);
                        } else {
                            returnValue = "";
                        }
                    }
                    break;
                    case 2: {
                        File f = files.get(rowIndex);
                        if (f != null) {
                            boolean en = f.exists();
                            if (en) {
                                returnValue = "Found locally";
                            } else {
                                returnValue = "NOT found locally";
                            }
                        }
                    }
                    break;
                    case 3: {
                        File f = files.get(rowIndex);
                        if (f != null) {
                            String fn = f.getName();
                            int i = fn.lastIndexOf('.');
                            if (i > 0) {
                                fn = fn.substring(0, i);
                            }
                            SDFileInfo sdfi = SDCardInfo.getInstance().find("/" + fn + "/patch.bin");
                            // LOGGER.log(Level.INFO, "/" + fn + "/patch.bin");
                            if (sdfi != null) {
                                returnValue = "Found on SD card";
                            }
                            else {
                                returnValue = "NOT on SD card";
                            }
                        }
                    }
                    break;
                }

                return returnValue;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return (columnIndex == 1);
            }
        });
        jTable1.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);


        jScrollPane1.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : droppedFiles) {
                        System.out.println(f.getName());
                        if (!f.canRead()) {
                            LOGGER.log(Level.SEVERE, "Cannot read file");
                        }
                        else {
                            files.add(f);
                        }
                    }

                    setDirty();
                    refresh();

                } catch (UnsupportedFlavorException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        });

        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(20);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(300);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(60);
            jTable1.getColumnModel().getColumn(3).setPreferredWidth(60);
        }

        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                    if (jTable1.getSelectedRowCount() > 0) {
                        reflectSelection(jTable1.getSelectedRows());
                    }
            }
        });
        int[] s = {1};
        reflectSelection(s);
    }

    final void reflectSelection(int[] rows) {
        Arrays.sort(rows);
        if (rows[0] < 0 || files.size() < 1) {
            jButtonUp.setEnabled(false);
            jButtonDown.setEnabled(false);
            jButtonOpen.setEnabled(false);
            jButtonRemove.setEnabled(false);
            jButtonUpload.setEnabled(false);
        } else {
            jButtonUp.setEnabled(rows[0] > 0);
            jButtonDown.setEnabled(rows[rows.length-1] < files.size() - 1);
            File f = files.get(rows[0]);
            boolean en = (f != null) && (f.exists());
            jButtonOpen.setEnabled(en);
            jButtonUpload.setEnabled(en);
            jButtonRemove.setEnabled(true);
        }
    }

    public void refresh() {
        jTable1.revalidate();
        jTable1.repaint();
    }

    String toRelative(File f) {
        if (FilenamePath != null && !FilenamePath.isEmpty()) {
            Path path = Paths.get(f.getPath());
            Path pathBase = Paths.get(new File(FilenamePath).getParent());
            if (path.isAbsolute()) {
                Path pathRelative = pathBase.relativize(path);
                return pathRelative.toString();
            } else {
                return path.toString();
            }
        } else {
            return f.getAbsolutePath();
        }
    }

    File fromRelative(String s) {
        Path basePath = FileSystems.getDefault().getPath(FilenamePath);
        Path resolvedPath = basePath.getParent().resolve(s);
        return resolvedPath.toFile();
    }

    public byte[] GetContents() {
        ByteBuffer data = ByteBuffer.allocateDirect(128 * 256);
        for (File file : files) {
            String fn = (String) file.getName();
            for (char c : fn.toCharArray()) {
                data.put((byte) c);
            }
            data.put((byte) '\n');
        }
        data.limit(data.position());
        data.rewind();
        byte[] b = new byte[data.limit()];
        data.get(b);
        return b;
    }

    void Open(File f) throws IOException {
        FilenamePath = f.getPath();
        InputStream fs = new FileInputStream(f);
        BufferedReader fbs = new BufferedReader(new InputStreamReader(fs));
        String s;
        files = new ArrayList<File>();
        while ((s = fbs.readLine())
                != null) {
            File ff = fromRelative(s);
            if (ff != null) {
                files.add(ff);
            }
        }
        fs.close();
        refresh();
        setTitle(FilenamePath);
    }

    void Save(File f) {
        FilenamePath = f.getPath();
        try {
            PrintWriter pw = new PrintWriter(f);
            for (File file : files) {
                String fn = toRelative(file);
                pw.println(fn);
            }
            pw.close();
            clearDirty();
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        //  catch (IOException ex) {
        //     LOGGER.log(Level.SEVERE, null, ex);
        // }
    }

    void Save() {
        if (FilenamePath == null) {
            SaveAs();
        } else {
            File f = new File(FilenamePath);
            if (!f.canWrite()) {
                SaveAs();
            } else {
                Save(f);
            }
        }
        refresh();
    }

    void SaveAs() {
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(prefs.getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Save As...");
        fc.setAcceptAllFileFilterUsed(false);
        FileFilter axb = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.getName().endsWith("axb")) {
                    return true;
                } else if (file.isDirectory()) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Axoloti Patchbank";
            }
        };
        fc.addChoosableFileFilter(axb);
        String fn = FilenamePath;
        if (fn == null) {
            fn = "untitled";
        }
        File f = new File(fn);
        fc.setSelectedFile(f);

        String ext = "";
        int dot = fn.lastIndexOf('.');
        if (dot > 0 && fn.length() > dot + 3) {
            ext = fn.substring(dot);
        }
        if (ext.equalsIgnoreCase(fileExtension)) {
            fc.setFileFilter(axb);
        }

        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String filterext = fileExtension;
            if (fc.getFileFilter() == axb) {
                filterext = fileExtension;
            }

            File fileToBeSaved = fc.getSelectedFile();
            ext = "";
            String fname = fileToBeSaved.getAbsolutePath();
            dot = fname.lastIndexOf('.');
            if (dot > 0 && fname.length() > dot + 3) {
                ext = fname.substring(dot);
            }

            if (!ext.equalsIgnoreCase(fileExtension)) {
                fileToBeSaved = new File(fc.getSelectedFile() + filterext);

            } else if (!ext.equals(filterext)) {
                Object[] options = {"Yes",
                    "No"};
                int n = JOptionPane.showOptionDialog(this,
                        "File does not match filter. Change extension to " + filterext + "?",
                        "File Extension",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                switch (n) {
                    case JOptionPane.YES_OPTION:
                        fileToBeSaved = new File(fname.substring(0, fname.length() - 4) + filterext);
                        break;
                    case JOptionPane.NO_OPTION:
                        return;
                }
            }

            if (fileToBeSaved.exists()) {
                Object[] options = {"Yes",
                    "No"};
                int n = JOptionPane.showOptionDialog(this,
                        "File exists! Overwrite?",
                        "File Exists",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                switch (n) {
                    case JOptionPane.YES_OPTION:
                        break;
                    case JOptionPane.NO_OPTION:
                        return;
                }
            }

            FilenamePath = fileToBeSaved.getPath();
            setTitle(FilenamePath);
            MainFrame.prefs.setCurrentFileDirectory(fileToBeSaved.getPath());
            Save(fileToBeSaved);
        }
        fc.updateCurrentSize();
    }

    boolean isDirty() {
        return dirty;
    }

    void setDirty() {
        dirty = true;
    }

    void clearDirty() {
        dirty = false;
    }

    public void Close() {
        DocumentWindowList.UnregisterWindow(this);
        USBBulkConnection.GetConnection().removeConnectionStatusListener(this);
        dispose();
    }

    @Override
    public boolean AskClose() {
        if (isDirty()) {
            Object[] options = {"Yes",
                "No",
                "Cancel"};
            String fn = FilenamePath;
            if (fn == null) {
                fn = "untitled";
            }
            int n = JOptionPane.showOptionDialog(
                    this,
                    "Save changes to " + fn + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[2]);
            switch (n) {
                case JOptionPane.YES_OPTION:
                    SaveAs();
                    Close();
                    return false;
                case JOptionPane.NO_OPTION:
                    Close();
                    return false;
                case JOptionPane.CANCEL_OPTION:
                    return true;
                default:
                    return false;
            }
        } else {
            Close();
            return false;
        }
    }


    @SuppressWarnings("unchecked")
    
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jButtonUploadIndex = new javax.swing.JButton();
        // jLabelDisclaimer = new javax.swing.JLabel();
        jUploadPatches = new javax.swing.JButton();
        jUploadAll = new javax.swing.JButton();
        jScrollPane1 = new ScrollPaneComponent();
        jTable1 = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jButtonUp = new javax.swing.JButton();
        jButtonDown = new javax.swing.JButton();
        jButtonRemove = new javax.swing.JButton();
        jButtonAdd = new javax.swing.JButton();
        jButtonOpen = new javax.swing.JButton();
        jButtonUpload = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu1 = new axoloti.menus.FileMenu();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemSaveAs = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        windowMenu1 = new axoloti.menus.WindowMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("untitled patchbank");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jButtonUploadIndex.setText("Upload Bank Index");
        jButtonUploadIndex.setToolTipText("Creates or updates a file called \"index.axb\" on SD card.\n"
                                        + "This is a simple text file containing an ordered list of patches.\n"
                                        + "You will also have to upload the patch binaries to SD via \"Upload Patches\"\n"
                                        + "Then you can load these patches via the index and a\"patch/load i\" object."
        );
        jButtonUploadIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUploadBankActionPerformed(evt);
            }
        });

        // jLabelDisclaimer.setText("Not (fully) implemented yet!");

        jUploadPatches.setText("Upload Patches");
        jUploadPatches.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUploadPatchesActionPerformed(evt);
            }
        });

        jUploadAll.setText("Upload Index & Patches");
        jUploadAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUploadAllActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1.setMinimumSize(new Dimension(256, 32));
        jPanel1.setMaximumSize(new Dimension(32768, 32));
        jPanel1.setPreferredSize(new Dimension(jPanel1.getWidth(), 32));
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                // .addComponent(jLabelDisclaimer)
                // .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonUploadIndex)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 108, Short.MAX_VALUE)
                .addComponent(jUploadPatches)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 108, Short.MAX_VALUE)
                .addComponent(jUploadAll)
                .addContainerGap()
            )
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(4)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    // .addComponent(jLabelDisclaimer, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonUploadIndex)
                    .addComponent(jUploadPatches)
                    .addComponent(jUploadAll))
                .addGap(4)
            )
        );

        getContentPane().add(jPanel1);

        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.setModel(new javax.swing.table.DefaultTableModel( new Object [][] { }, new String [] { "#", "File" }
        ) {
            Class<?>[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);

        getContentPane().add(jScrollPane1);

        jButtonUp.setText(" ⇑ Move Up ");
        jButtonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUpActionPerformed(evt);
            }
        });

        jButtonDown.setText("⇓ Move Down");
        jButtonDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDownActionPerformed(evt);
            }
        });

        jButtonRemove.setText("Remove Selected");
        jButtonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRemoveActionPerformed(evt);
            }
        });

        jButtonAdd.setText(" Add... ");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        jButtonOpen.setText("Open Selected");
        jButtonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenActionPerformed(evt);
            }
        });

        jButtonUpload.setText("Upload Selected");
        jButtonUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUploadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2.setMinimumSize(new Dimension(256, 32));
        jPanel2.setMaximumSize(new Dimension(32768, 32));
        jPanel2.setPreferredSize(new Dimension(jPanel2.getWidth(), 32));
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                .addComponent(jButtonAdd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRemove)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonOpen)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                .addComponent(jButtonUpload)
                .addContainerGap()
            )
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                .addGap(4)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jButtonUp)
                    .addComponent(jButtonDown)
                    .addComponent(jButtonAdd)
                    .addComponent(jButtonRemove)
                    .addComponent(jButtonOpen)
                    .addComponent(jButtonUpload)
                )
                .addGap(4)
            )
        );

        getContentPane().add(jPanel2);

        fileMenu1.setText("File");
        fileMenu1.add(jSeparator1);

        jMenuItemSave.setText("Save");
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        fileMenu1.add(jMenuItemSave);

        jMenuItemSaveAs.setText("Save as...");
        jMenuItemSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveAsActionPerformed(evt);
            }
        });
        fileMenu1.add(jMenuItemSaveAs);

        jMenuBar1.add(fileMenu1);

        jMenu2.setText("Edit");
        jMenu2.setDelay(300);
        jMenuBar1.add(jMenu2);
        jMenuBar1.add(windowMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }

    private void jButtonUploadBankActionPerformed(java.awt.event.ActionEvent evt) {
        LOGGER.log(Level.INFO, "Uploading patchbank index...");
        QCmdProcessor processor = MainFrame.mainframe.getQcmdprocessor();
        if (USBBulkConnection.GetConnection().isConnected()) {
            processor.AppendToQueue(new QCmdUploadFile(new ByteArrayInputStream(GetContents()), "/index.axb"));
        }
        LOGGER.log(Level.INFO, "Done uploading index.");
        refresh();
    }

    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {
        Save();
    }

    private void jMenuItemSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
        SaveAs();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        AskClose();
    }

    private void jButtonUpActionPerformed(java.awt.event.ActionEvent evt) {
        int[] rows = jTable1.getSelectedRows();
        Arrays.sort(rows);
        jTable1.clearSelection();
        for (int row : rows) {
            if (row < 1) {
                return;
            }
            File o = files.remove(row);
            files.add(row - 1, o);
            jTable1.addRowSelectionInterval(row - 1, row - 1);
        }
        setDirty();
        refresh();
    }

    private void jButtonDownActionPerformed(java.awt.event.ActionEvent evt) {
        int[] rows = jTable1.getSelectedRows();
        jTable1.clearSelection();
        /* Count backwards so entries are moved highest index first (furthest down the table first) */
        for (int i=rows.length-1; i>=0; i--) {
            if (rows[i] < 0) {
                return;
            }
            if (rows[i] > (files.size() - 1)) {
                return;
            }
            File o = files.remove(rows[i]);
            files.add(rows[i] + 1, o);
            jTable1.addRowSelectionInterval(rows[i] + 1, rows[i] + 1);
        }
        setDirty();
        refresh();
    }

    private void jButtonRemoveActionPerformed(java.awt.event.ActionEvent evt) {
        int[] rows = jTable1.getSelectedRows();
        /* Count backwards so entries are removed highest index first (furthest down in table first) */
        for (int i=rows.length-1; i>=0; i--) {
            if (rows[i] < 0) {
                return;
            }
            files.remove(rows[i]);
        }
        setDirty();
        refresh();
    }

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(prefs.getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Open...");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Axoloti Files", "axp"));
        fc.addChoosableFileFilter(axpFileFilter);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] fs = fc.getSelectedFiles();
            if (fs[0] != null) {
                prefs.setCurrentFileDirectory(fs[0].getParentFile().toString());
            }
            for (File f : fs) {
                if (f != null && f.exists()) {
                    files.add(f);
                }
            }
            setDirty();
            refresh();
        }
        fc.setMultiSelectionEnabled(false);
        fc.updateCurrentSize();
    }

    private void jButtonOpenActionPerformed(java.awt.event.ActionEvent evt) {
        for (int row : jTable1.getSelectedRows()) {
            if (row >= 0) {
                File f = files.get(row);
                if (f.isFile() && f.canRead()) {
                    PatchGUI.OpenPatch(f);
                }
            }
        }
    }

    void UploadOneFile(File f) {
        if (!f.isFile() || !f.canRead()) {
            return;
        }
        PatchFrame pf = PatchGUI.OpenPatchInvisible(f);
        if (pf != null) {
            boolean isVisible = pf.isVisible();
            PatchGUI p = pf.getPatch();
            p.UploadToSDCard();
            if (!isVisible) {
                pf.Close();
            }

            //FIXME: workaround waitQueueFinished bug
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                ;
            }

            QCmdProcessor.getQCmdProcessor().WaitQueueFinished();
        }
        refresh();
    }


    private void jButtonUploadActionPerformed(java.awt.event.ActionEvent evt) {
        for (int row : jTable1.getSelectedRows()) {
            File f = files.get(row);
            UploadOneFile(f);
        }
        refresh();
    }

    private void jUploadPatchesActionPerformed(java.awt.event.ActionEvent evt) {
        class Thd extends Thread {
            public void run() {
                for (File f : files) {
                    LOGGER.log(Level.INFO, "Compiling and uploading: {0}", f.getName());
                    UploadOneFile(f);
                }
                LOGGER.log(Level.INFO, "Done uploading patches in patchbank.");
            }
        }
        Thd thread = new Thd();
        thread.start();

        refresh();
    }

    private void jUploadAllActionPerformed(java.awt.event.ActionEvent evt) {
        class Thd extends Thread {
            public void run() {
                QCmdProcessor processor = MainFrame.mainframe.getQcmdprocessor();
                if (USBBulkConnection.GetConnection().isConnected()) {
                    processor.AppendToQueue(new QCmdUploadFile(new ByteArrayInputStream(GetContents()), "/index.axb"));
                }

                for (File f : files) {
                    LOGGER.log(Level.INFO, "Compiling and uploading: {0}...", f.getName());
                    UploadOneFile(f);
                }
                LOGGER.log(Level.INFO, "Done uploading index and patches.");
            }
        }
        Thd thread = new Thd();
        thread.start();

        refresh();
    }

    private axoloti.menus.FileMenu fileMenu1;
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonDown;
    private javax.swing.JButton jButtonOpen;
    private javax.swing.JButton jButtonRemove;
    private javax.swing.JButton jButtonUp;
    private javax.swing.JButton jButtonUpload;
    private javax.swing.JButton jButtonUploadIndex;
    // private javax.swing.JLabel jLabelDisclaimer;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItemSave;
    private javax.swing.JMenuItem jMenuItemSaveAs;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private ScrollPaneComponent jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton jUploadPatches;
    private javax.swing.JButton jUploadAll;
    private axoloti.menus.WindowMenu windowMenu1;

    @Override
    public JFrame GetFrame() {
        return this;
    }

    public void ShowConnect1(boolean status) {
        jButtonUploadIndex.setEnabled(status);
        jUploadPatches.setEnabled(status);
        jUploadAll.setEnabled(status);
        jButtonUpload.setEnabled(status);
    }

    @Override
    public void ShowConnect() {
        ShowConnect1(true);
    }

    @Override
    public void ShowDisconnect() {
        ShowConnect1(false);
    }

    static public void OpenBank(File f) {
        PatchBank pb = new PatchBank();
        try {
            pb.Open(f);
            MainFrame.prefs.addRecentFile(f.getAbsolutePath());
            pb.setVisible(true);
        } catch (IOException ex) {
            pb.Close();
            LOGGER.log(Level.SEVERE, "Patchbank file not found or not accessible: {0}", f.getName());
        }
    }

    @Override
    public File getFile() {
        if (FilenamePath == null) {
            return null;
        } else {
            return new File(FilenamePath);
        }
    }

    @Override
    public ArrayList<DocumentWindow> GetChildDocuments() {
        return null;
    }

    @Override
    public void ShowSDCardMounted() {
        ShowConnect1(true);
    }

    @Override
    public void ShowSDCardUnmounted() {
        ShowConnect1(false);
    }
}
