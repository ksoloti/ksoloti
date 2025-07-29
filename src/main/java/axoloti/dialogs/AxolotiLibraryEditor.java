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

import axoloti.Axoloti;

import static axoloti.MainFrame.fc;
import axoloti.utils.AxoFileLibrary;
import axoloti.utils.AxoGitLibrary;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author kodiak
 */
public class AxolotiLibraryEditor extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(AxolotiLibraryEditor.class.getName());

    private AxolotiLibrary library;
    private static final List<AxolotiLibraryEditor> openEditors = new ArrayList<>();
    private boolean dirty = false;

    private Box.Filler filler1;
    private JButton jButtonInitRepo;
    private JButton jButtonSelectDir;
    private JButton jButtonSync;
    private JCheckBox jCheckBoxAutoSync;
    private JCheckBox jCheckBoxEnabled;
    private JComboBox<String> jComboBoxType;
    private JLabel jLabelContributorsOnly;
    private JLabel jLabelDirectory;
    private JLabel jLabelId;
    private JLabel jLabelPassword;
    private JLabel jLabelRemoteLibrary;
    private JLabel jLabelRemotePath;
    private JLabel jLabelRevision;
    private JLabel jLabelType;
    private JLabel jLabelTypeContributorPrefix;
    private JLabel jLabelTypeOptional;
    private JLabel jLabelUserId;
    private JPasswordField jPasswordField;
    private JSeparator jSeparator1;
    private JTextField jTextFieldId;
    private JTextField jTextFieldLocalDir;
    private JTextField jTextFieldPrefix;
    private JTextField jTextFieldRemotePath;
    private JTextField jTextFieldRevision;
    private JTextField jTextFieldUserId;

    /**
     * Creates new form AxolotiLibrary
     *
     * @param parent
     * @param modal
     */
    public AxolotiLibraryEditor(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        setLocationRelativeTo(parent);
        initComponents();
        setTitle("Add Library");
        library = new AxolotiLibrary();
        populate();
        addChangeListeners();
        setupWindowClosingListener();
        setupKeyboardShortcuts();
        openEditors.add(this);
        setVisible(true);
    }

    public AxolotiLibraryEditor(java.awt.Frame parent, boolean modal, AxolotiLibrary lib) {
        super(parent, modal);
        setLocationRelativeTo(parent);
        initComponents();
        setTitle("Edit Library");
        library = lib;
        populate();
        jTextFieldId.setEnabled(false);
        addChangeListeners();
        setupWindowClosingListener();
        setupKeyboardShortcuts();
        openEditors.add(this);
        setVisible(true);
    }

    public static List<AxolotiLibraryEditor> getOpenEditors() {
        return new ArrayList<>(openEditors);
    }

    public void reload() {
        AxolotiLibrary reloadedLib = null;
        if (this.library.getId() != null && !this.library.getId().isEmpty()) {
            reloadedLib = Preferences.getInstance().getLibrary(this.library.getId());
        }

        if (reloadedLib != null) {
            LOGGER.log(Level.INFO, "AxolotiLibraryEditor: Reloading editor for ID: {0}", this.library.getId());
            LOGGER.log(Level.INFO, "  Old local dir (before reload): {0}", this.library.getLocalLocation());
            LOGGER.log(Level.INFO, "  New local dir (from Preferences): {0}", reloadedLib.getLocalLocation());

            this.library = reloadedLib;

            populate();
            this.dirty = false;
            LOGGER.log(Level.INFO, "Editor {0} reloaded successfully. Dirty status: {1}", new Object[]{this.library.getId(), this.dirty});
        }
        else {
            LOGGER.log(Level.INFO, "AxolotiLibraryEditor: Library ID {0} not found after preferences reload. Closing editor.", this.library.getId());
            KeyboardNavigableOptionPane.showMessageDialog(this,
                    "The library '" + (this.library.getId() != null ? this.library.getId() : "New Library") + "' was discarded or removed from preferences. Closing its editor.",
                    "Library Discarded/Removed",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }

    final void populate() {
        jTextFieldId.setText(library.getId() != null ? library.getId() : "");
        jCheckBoxEnabled.setSelected(library.getEnabled());
        jTextFieldLocalDir.setText(library.getLocalLocation() != null ? library.getLocalLocation() : "");
        jTextFieldRemotePath.setText(library.getRemoteLocation() != null ? library.getRemoteLocation() : "");
        jTextFieldUserId.setText(library.getUserId() != null ? library.getUserId() : "");

        char[] passwordChars = library.getPassword();
        if (passwordChars != null && passwordChars.length > 0) {
            String passwordString = new String(passwordChars);
            jPasswordField.setText(passwordString);
            Arrays.fill(passwordChars, '\0');
        }
        else {
            jPasswordField.setText("");
        }

        jCheckBoxAutoSync.setSelected(library.isAutoSync());
        jTextFieldRevision.setText(library.getRevision() != null ? library.getRevision() : "");
        jTextFieldPrefix.setText(library.getContributorPrefix() != null ? library.getContributorPrefix() : "");

        String[] types = {AxoFileLibrary.TYPE, AxoGitLibrary.TYPE};
        jComboBoxType.removeAllItems();
        for (String t : types) {
            jComboBoxType.addItem(t);
        }
        jComboBoxType.setSelectedItem(library.getType() != null ? library.getType() : AxoFileLibrary.TYPE);

        boolean expert = Preferences.getInstance().getExpertMode() || Axoloti.isDeveloper();

        boolean isOfficial = AxolotiLibrary.AXOLOTI_FACTORY_ID.equals(library.getId()) ||
                             AxolotiLibrary.AXOLOTI_CONTRIB_ID.equals(library.getId()) ||
                             AxolotiLibrary.KSOLOTI_OBJECTS_ID.equals(library.getId()) ||
                             AxolotiLibrary.KSOLOTI_CONTRIB_ID.equals(library.getId());

        boolean lockDown = !expert && isOfficial;

        jTextFieldRevision.setEditable(!lockDown);
        jTextFieldRemotePath.setEditable(!lockDown);
        jTypeComboActionPerformed(null);
    }

    private void initComponents() {
        Dimension d = new java.awt.Dimension(0, 3);
        filler1 = new Box.Filler(d, d, d);
        jButtonInitRepo = new JButton();
        jButtonSelectDir = new JButton();
        jButtonSync = new JButton();
        jCheckBoxAutoSync = new JCheckBox();
        jCheckBoxEnabled = new JCheckBox();
        jComboBoxType = new JComboBox<String>();
        jLabelContributorsOnly = new JLabel();
        jLabelDirectory = new JLabel();
        jLabelId = new JLabel();
        jLabelPassword = new JLabel();
        jLabelRemoteLibrary = new JLabel();
        jLabelRemotePath = new JLabel();
        jLabelRevision = new JLabel();
        jLabelType = new JLabel();
        jLabelTypeContributorPrefix = new JLabel();
        jLabelTypeOptional = new JLabel();
        jLabelUserId = new JLabel();
        jPasswordField = new JPasswordField();
        jSeparator1 = new JSeparator();
        jTextFieldId = new JTextField();
        jTextFieldLocalDir = new JTextField();
        jTextFieldPrefix = new JTextField();
        jTextFieldRemotePath = new JTextField();
        jTextFieldRevision = new JTextField();
        jTextFieldUserId = new JTextField();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        jComboBoxType.setPreferredSize(new java.awt.Dimension(100, 28));
        jComboBoxType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTypeComboActionPerformed(evt);
            }
        });

        jLabelType.setText("Type");

        jTextFieldId.setMinimumSize(new java.awt.Dimension(100, 28));
        jTextFieldId.setPreferredSize(new java.awt.Dimension(100, 28));

        jTextFieldLocalDir.setPreferredSize(new java.awt.Dimension(400, 28));

        jLabelId.setText("ID");

        jLabelDirectory.setText("Directory");

        jTextFieldRemotePath.setPreferredSize(new java.awt.Dimension(400, 28));

        jLabelRemotePath.setMinimumSize(new java.awt.Dimension(100, 28));
        jLabelRemotePath.setText("Remote Path");

        jLabelRemoteLibrary.setText("Remote Library");

        jTextFieldUserId.setMinimumSize(new java.awt.Dimension(14, 50));
        jTextFieldUserId.setPreferredSize(new java.awt.Dimension(150, 28));

        jLabelUserId.setText("User Id");

        jLabelPassword.setText("Password");

        jCheckBoxEnabled.setText("Enabled");

        jPasswordField.setPreferredSize(new java.awt.Dimension(150, 28));

        jButtonInitRepo.setText("Init");
        jButtonInitRepo.setDefaultCapable(false);
        jButtonInitRepo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jInitRepoActionPerformed(evt);
            }
        });

        jCheckBoxAutoSync.setText("Auto Sync");

        jButtonSelectDir.setText("Browse...");
        jButtonSelectDir.setDefaultCapable(false);
        jButtonSelectDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSelectDirBtnActionPerformed(evt);
            }
        });

        jLabelRevision.setText("Branch");

        jTextFieldRevision.setMinimumSize(new java.awt.Dimension(100, 28));
        jTextFieldRevision.setPreferredSize(new java.awt.Dimension(100, 28));

        jLabelContributorsOnly.setText("Contributors only:");

        jLabelTypeOptional.setText("(optional)");

        jLabelTypeContributorPrefix.setText("Contributor Prefix");

        jTextFieldPrefix.setPreferredSize(new java.awt.Dimension(100, 28));

        jButtonSync.setText("Sync");
        jButtonSync.setDefaultCapable(false);
        jButtonSync.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSyncBtnActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(

            layout.createParallelGroup(Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)

                    .addComponent(jSeparator1)

                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(jLabelRemotePath, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabelRevision))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jTextFieldRevision, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(jLabelTypeOptional)
                                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jCheckBoxAutoSync))
                                    .addComponent(jTextFieldRemotePath, GroupLayout.PREFERRED_SIZE, 394, GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jLabelContributorsOnly, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 185, GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabelTypeContributorPrefix)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldPrefix, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabelUserId)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldUserId, GroupLayout.PREFERRED_SIZE, 196, GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jLabelPassword)
                                .addGap(1, 1, 1)
                                .addComponent(jPasswordField, GroupLayout.PREFERRED_SIZE, 173, GroupLayout.PREFERRED_SIZE))
                        )

                        .addPreferredGap(ComponentPlacement.RELATED, 29, Short.MAX_VALUE)

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jButtonInitRepo, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonSync, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                        )
                    )
                    .addGroup(layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(jLabelType)
                                    .addComponent(jLabelId)
                                    .addComponent(jLabelDirectory, GroupLayout.PREFERRED_SIZE, 73, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                            .addComponent(jComboBoxType, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jTextFieldId, GroupLayout.PREFERRED_SIZE, 215, GroupLayout.PREFERRED_SIZE))
                                        .addGap(104, 104, 104)
                                        .addComponent(jCheckBoxEnabled))
                                    .addComponent(jTextFieldLocalDir, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButtonSelectDir, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelRemoteLibrary, GroupLayout.PREFERRED_SIZE, 179, GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))))
                )
                .addContainerGap()
            )

            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 306, Short.MAX_VALUE)
                    .addComponent(filler1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 306, Short.MAX_VALUE))
            )
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jComboBoxType, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelType)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jTextFieldId, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelId)
                    .addComponent(jCheckBoxEnabled)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jTextFieldLocalDir, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelDirectory)
                    .addComponent(jButtonSelectDir)
                )

                .addGap(18, 18, 18)

                .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                .addPreferredGap(ComponentPlacement.RELATED)

                .addComponent(jLabelRemoteLibrary, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelRemotePath, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldRemotePath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonInitRepo)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelRevision)
                    .addComponent(jTextFieldRevision, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxAutoSync)
                    .addComponent(jLabelTypeOptional)
                    .addComponent(jButtonSync)
                )

                .addPreferredGap(ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                .addComponent(jLabelContributorsOnly)
                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelTypeContributorPrefix)
                    .addComponent(jTextFieldPrefix, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addGap(2, 2, 2)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jTextFieldUserId, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelUserId)
                    .addComponent(jPasswordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelPassword)
                )

                .addGap(19, 19, 19)
            )

            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 198, Short.MAX_VALUE)
                    .addComponent(filler1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 198, Short.MAX_VALUE)
                )
            )
        );

        pack();
    }

    private void setButtonsEnabled(boolean enabled) {
        jButtonInitRepo.setEnabled(enabled);
        jButtonSelectDir.setEnabled(enabled);
        jButtonSync.setEnabled(enabled);
    }

    private void jTypeComboActionPerformed(java.awt.event.ActionEvent evt) {
        /* Lazy hack to make library edit screen a bit less overwhelming */
        boolean isGit = jComboBoxType.getSelectedItem().equals("git");
        /* If local, grey out Git part of settings window */
        jButtonInitRepo.setVisible(isGit);
        jButtonSync.setVisible(isGit);
        jCheckBoxAutoSync.setVisible(isGit);
        jLabelContributorsOnly.setEnabled(isGit);
        jLabelPassword.setEnabled(isGit);
        jLabelRemoteLibrary.setEnabled(isGit);
        jLabelRemotePath.setEnabled(isGit);
        jLabelRevision.setEnabled(isGit);
        jLabelTypeContributorPrefix.setEnabled(isGit);
        jLabelTypeOptional.setEnabled(isGit);
        jLabelUserId.setEnabled(isGit);
        jPasswordField.setEnabled(isGit);
        jTextFieldPrefix.setEnabled(isGit);
        jTextFieldRemotePath.setEnabled(isGit);
        jTextFieldRevision.setEnabled(isGit);
        jTextFieldUserId.setEnabled(isGit);
    }

    private void jInitRepoActionPerformed(java.awt.event.ActionEvent evt) {

        boolean delete;
        Object[] options = {"Init", "Cancel"};
        int res = KeyboardNavigableOptionPane.showOptionDialog(this,
                                               "The existing directory will be replaced/overwritten.\nContinue?",
                                               "Initialise Library",
                                               JOptionPane.OK_CANCEL_OPTION,
                                               JOptionPane.WARNING_MESSAGE,
                                               null,
                                               options,
                                               options[1]);
        if (res == JOptionPane.CANCEL_OPTION) {
            return;
        }
        delete = (res == JOptionPane.OK_OPTION);

        /* Disable UI elements while in progress */
        setButtonsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AxoGitLibrary gitlib = new AxoGitLibrary();
                populateLib(gitlib);
                gitlib.init(delete);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                }
                catch (Exception e) {
                    Logger.getLogger(AxolotiLibraryEditor.class.getName()).log(Level.SEVERE, "Git init failed", e);
                }
                finally {
                    setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    private void jSelectDirBtnActionPerformed(java.awt.event.ActionEvent evt) {
        String dir = jTextFieldLocalDir.getText();
        if (dir == null || dir.length() == 0) {
            dir = Preferences.getInstance().getCurrentFileDirectory();
        }

        File seldir = new File(dir).getParentFile();
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(seldir);
        fc.restoreCurrentSize();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select Directory...");
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Any folder";
            }
        });

        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            seldir = fc.getSelectedFile();
            if (!seldir.exists()) {
                seldir = seldir.getParentFile();
            }
            try {
                dir = seldir.getCanonicalPath();
                jTextFieldLocalDir.setText(dir + File.separator);
            }
            catch (IOException ex) {
                Logger.getLogger(AxolotiLibraryEditor.class.getName()).log(Level.SEVERE, "Error getting canonical path for selected directory", ex);
            }
        }
        fc.updateCurrentSize();

    }

    private void jSyncBtnActionPerformed(java.awt.event.ActionEvent evt) {
        /* Disable UI elements while in progress */
        setButtonsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AxoGitLibrary gitlib = new AxoGitLibrary();
                populateLib(gitlib);
                gitlib.sync();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                }
                catch (Exception e) {
                    Logger.getLogger(AxolotiLibraryEditor.class.getName()).log(Level.SEVERE, "Git sync failed", e);
                }
                finally {
                    setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    public AxolotiLibrary getLibrary() {
        return library;
    }

    public void setLibrary(AxolotiLibrary library) {
        this.library = library;
    }

    private void populateLib(AxolotiLibrary lib) {
        lib.setId(jTextFieldId.getText().trim());
        lib.setLocalLocation(jTextFieldLocalDir.getText().trim());
        lib.setRemoteLocation(jTextFieldRemotePath.getText().trim());
        lib.setUserId(jTextFieldUserId.getText().trim());

        char[] passwordChars = jPasswordField.getPassword();
        if (passwordChars != null && passwordChars.length > 0) {
            lib.setPassword(passwordChars);
            java.util.Arrays.fill(passwordChars, '\0'); /* Clear the password variable */
        }
        else {
            lib.setPassword(null);
        }

        lib.setEnabled(jCheckBoxEnabled.isSelected());
        lib.setType((String) jComboBoxType.getSelectedItem());
        lib.setAutoSync(jCheckBoxAutoSync.isSelected());
        lib.setContributorPrefix(jTextFieldPrefix.getText().trim());
        lib.setRevision(jTextFieldRevision.getText().trim());
    }

    private void addChangeListeners() {
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { setDirty(true); }
            @Override
            public void removeUpdate(DocumentEvent e) { setDirty(true); }
            @Override
            public void changedUpdate(DocumentEvent e) { setDirty(true); }
        };
        jTextFieldId.getDocument().addDocumentListener(documentListener);
        jTextFieldLocalDir.getDocument().addDocumentListener(documentListener);
        jTextFieldPrefix.getDocument().addDocumentListener(documentListener);
        jTextFieldRemotePath.getDocument().addDocumentListener(documentListener);
        jTextFieldRevision.getDocument().addDocumentListener(documentListener);
        jTextFieldUserId.getDocument().addDocumentListener(documentListener);
        jPasswordField.getDocument().addDocumentListener(documentListener);

        jCheckBoxEnabled.addActionListener(e -> setDirty(true));
        jCheckBoxAutoSync.addActionListener(e -> setDirty(true));

        jComboBoxType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setDirty(true);
                    jTypeComboActionPerformed(null);
                }
            }
        });
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    private boolean isLibraryValid() {
        if (jTextFieldId.getText().trim().isEmpty()) {
            KeyboardNavigableOptionPane.showMessageDialog(this,
                    "ID is required and must be unique.",
                    "Invalid ID",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (jTextFieldLocalDir.getText().trim().isEmpty()) { 
            KeyboardNavigableOptionPane.showMessageDialog(this,
                    "Local Directory is required and must be valid.",
                    "Invalid Local Directory",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (jTextFieldId.getText().equals(AxolotiLibrary.AXOLOTI_CONTRIB_ID) &&
            (jTextFieldUserId.getText() != null && jTextFieldUserId.getText().length() > 0)) {
            char[] p = jPasswordField.getPassword();
            if (jTextFieldPrefix.getText().trim().isEmpty() || (p == null || p.length == 0)) {
                KeyboardNavigableOptionPane.showMessageDialog(this,
                        "Contributors to the community library need to specify\n" +
                        "username, password and user prefix.",
                        "Invalid Contributor Data",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    public AxolotiLibrary getEditedLibrary() {
        return this.library;
    }

    private void saveLibrary() {

        if (!isLibraryValid()) { return; }

        String currentSelectedType = (String) jComboBoxType.getSelectedItem();
        AxolotiLibrary libraryToProcess = this.library; // Start with the current 'this.library'

        if (libraryToProcess == null || libraryToProcess.getId() == null || !currentSelectedType.equals(libraryToProcess.getType())) {
            AxolotiLibrary newTypedInstance; // This will hold the new instance of the correct type

            if (currentSelectedType.equals(AxoGitLibrary.TYPE)) {
                newTypedInstance = new AxoGitLibrary();
            } else { // Default or AxoFileLibrary.TYPE
                newTypedInstance = new AxoFileLibrary();
            }

            if (libraryToProcess != null && libraryToProcess.getId() != null) {
                newTypedInstance.setId(libraryToProcess.getId());
            }
            libraryToProcess = newTypedInstance; // Now, 'libraryToProcess' points to the new, correctly typed instance
        }

        populateLib(libraryToProcess);

        this.library = libraryToProcess;
        this.dirty = false; // Changes are now reflected in 'this.library'
        PreferencesFrame.GetPreferencesFrame().setDirty(true);
    }

    private void attemptCloseEditor() {
        if (dirty) {
            saveLibrary();
        }
        dispose();
    }

    private void setupWindowClosingListener() {
        // this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                attemptCloseEditor();
            }
            @Override
            public void windowClosed(WindowEvent e) {
                openEditors.remove(AxolotiLibraryEditor.this);
                LOGGER.log(Level.INFO, "AxolotiLibraryEditor {0} removed from openEditors list. List size: {1}",
                           new Object[]{AxolotiLibraryEditor.this.library.getId(), openEditors.size()});
            }
        });
    }

    private void setupKeyboardShortcuts() {
        AbstractAction closeAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                attemptCloseEditor();
            }
        };

        javax.swing.JRootPane rootPane = this.getRootPane();
        javax.swing.InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMap = rootPane.getActionMap();
        KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyUtils.CONTROL_OR_CMD_MASK);
        inputMap.put(ctrlW, "closeEditor");
        actionMap.put("closeEditor", closeAction);
    }
}