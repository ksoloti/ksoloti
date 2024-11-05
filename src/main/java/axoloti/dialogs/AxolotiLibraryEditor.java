/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axoloti.dialogs;

import axoloti.Axoloti;
import axoloti.MainFrame;

import static axoloti.MainFrame.fc;
import static axoloti.MainFrame.prefs;
import axoloti.utils.AxoFileLibrary;
import axoloti.utils.AxoGitLibrary;
import axoloti.utils.AxolotiLibrary;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author kodiak
 */
public class AxolotiLibraryEditor extends JDialog {

    private AxolotiLibrary library;

    /**
     * Creates new form AxolotiLibrary
     *
     * @param parent
     * @param modal
     */
    public AxolotiLibraryEditor(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("Add Library");
        library = new AxolotiLibrary();
        populate();
        setVisible(true);
    }

    public AxolotiLibraryEditor(java.awt.Frame parent, boolean modal, AxolotiLibrary lib) {
        super(parent, modal);
        initComponents();
        setTitle("Edit Library");
        library = lib;
        populate();
        jId.setEnabled(false);
        setVisible(true);
    }

    final void populate() {
        jId.setText(library.getId());
        jEnabled.setSelected(library.getEnabled());
        jLocalDir.setText(library.getLocalLocation());
        jRemotePath.setText(library.getRemoteLocation());
        jUserId.setText(library.getUserId());
        jPassword.setText(library.getPassword());
        jAutoSync.setSelected(library.isAutoSync());
        jRevision.setText(library.getRevision());
        jPrefix.setText(library.getContributorPrefix());

        String[] types = {AxoFileLibrary.TYPE, AxoGitLibrary.TYPE};
        jTypeCombo.removeAllItems();
        for (String t : types) {
            jTypeCombo.addItem(t);
        }
        jTypeCombo.setSelectedItem(library.getType());

        boolean expert = MainFrame.prefs.getExpertMode() || Axoloti.isDeveloper();

        boolean isOfficial = AxolotiLibrary.FACTORY_ID.equals(library.getId()) ||
                            AxolotiLibrary.USER_LIBRARY_ID.equals(library.getId()) ||
                            AxolotiLibrary.KSOLOTI_LIBRARY_ID.equals(library.getId()) ||
                            AxolotiLibrary.KSOLOTI_CONTRIB_LIBRARY_ID.equals(library.getId());

        boolean lockDown = !expert && isOfficial;

        jRevision.setEditable(!lockDown);
        jRemotePath.setEditable(!lockDown);
    }


    private void initComponents() {

        jTypeCombo = new JComboBox<String>();
        jLabelType = new JLabel();
        jId = new JTextField();
        jLocalDir = new JTextField();
        jLabelId = new JLabel();
        jLabelDirectory = new JLabel();
        jRemotePath = new JTextField();
        jLabelRemotePath = new JLabel();
        jLabelRemoteLibrary = new JLabel();
        jSeparator1 = new JSeparator();
        jUserId = new JTextField();
        jLabelUserId = new JLabel();
        jLabelPassword = new JLabel();
        jEnabled = new JCheckBox();
        jPassword = new JPasswordField();
        jOK = new JButton();
        jCancel = new JButton();
        jInitRepo = new JButton();
        jAutoSync = new JCheckBox();
        jSelectDirBtn = new JButton();
        filler1 = new Box.Filler(new java.awt.Dimension(0, 3), new java.awt.Dimension(0, 3), new java.awt.Dimension(32767, 3));
        jLabelRevision = new JLabel();
        jRevision = new JTextField();
        jLabelContributorsOnly = new JLabel();
        jLabelTypeOptional = new JLabel();
        jLabelTypeContributorPrefix = new JLabel();
        jPrefix = new JTextField();
        jSyncBtn = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        jTypeCombo.setPreferredSize(new java.awt.Dimension(100, 28));
        jTypeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTypeComboActionPerformed(evt);
            }
        });

        jLabelType.setText("Type");

        jId.setMinimumSize(new java.awt.Dimension(100, 28));
        jId.setPreferredSize(new java.awt.Dimension(100, 28));

        jLocalDir.setPreferredSize(new java.awt.Dimension(400, 28));

        jLabelId.setText("ID");

        jLabelDirectory.setText("Directory");

        jRemotePath.setPreferredSize(new java.awt.Dimension(400, 28));

        jLabelRemotePath.setMinimumSize(new java.awt.Dimension(100, 28));
        jLabelRemotePath.setText("Remote Path");

        jLabelRemoteLibrary.setText("Remote Library");

        jUserId.setMinimumSize(new java.awt.Dimension(14, 50));
        jUserId.setPreferredSize(new java.awt.Dimension(150, 28));

        jLabelUserId.setText("User Id");

        jLabelPassword.setText("Password");

        jEnabled.setText("Enabled");

        jPassword.setText("jPasswordField1");
        jPassword.setPreferredSize(new java.awt.Dimension(150, 28));

        jOK.setText("OK");
        jOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jOKActionPerformed(evt);
            }
        });

        jCancel.setText("Cancel");
        jCancel.setDefaultCapable(false);
        jCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCancelActionPerformed(evt);
            }
        });

        jInitRepo.setText("Init");
        jInitRepo.setDefaultCapable(false);
        jInitRepo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jInitRepoActionPerformed(evt);
            }
        });

        jAutoSync.setText("Auto Sync");

        jSelectDirBtn.setText("Browse...");
        jSelectDirBtn.setDefaultCapable(false);
        jSelectDirBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSelectDirBtnActionPerformed(evt);
            }
        });

        jLabelRevision.setText("Branch");

        jRevision.setMinimumSize(new java.awt.Dimension(100, 28));
        jRevision.setPreferredSize(new java.awt.Dimension(100, 28));

        jLabelContributorsOnly.setText("Contributors only:");

        jLabelTypeOptional.setText("(optional)");

        jLabelTypeContributorPrefix.setText("Contributor Prefix");

        jPrefix.setPreferredSize(new java.awt.Dimension(100, 28));

        jSyncBtn.setText("Sync");
        jSyncBtn.setDefaultCapable(false);
        jSyncBtn.addActionListener(new java.awt.event.ActionListener() {
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
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jCancel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(jOK)
                    )

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
                                        .addComponent(jRevision, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(jLabelTypeOptional)
                                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jAutoSync))
                                    .addComponent(jRemotePath, GroupLayout.PREFERRED_SIZE, 394, GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jLabelContributorsOnly, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 185, GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabelTypeContributorPrefix)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(jPrefix, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabelUserId)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(jUserId, GroupLayout.PREFERRED_SIZE, 196, GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(jLabelPassword)
                                .addGap(1, 1, 1)
                                .addComponent(jPassword, GroupLayout.PREFERRED_SIZE, 173, GroupLayout.PREFERRED_SIZE))
                        )

                        .addPreferredGap(ComponentPlacement.RELATED, 29, Short.MAX_VALUE)

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jInitRepo, Alignment.TRAILING)
                            .addComponent(jSyncBtn, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
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
                                            .addComponent(jTypeCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jId, GroupLayout.PREFERRED_SIZE, 215, GroupLayout.PREFERRED_SIZE))
                                        .addGap(104, 104, 104)
                                        .addComponent(jEnabled))
                                    .addComponent(jLocalDir, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jSelectDirBtn, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
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
                    .addComponent(jTypeCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelType)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jId, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelId)
                    .addComponent(jEnabled)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLocalDir, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelDirectory)
                    .addComponent(jSelectDirBtn)
                )

                .addGap(18, 18, 18)

                .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                .addPreferredGap(ComponentPlacement.RELATED)

                .addComponent(jLabelRemoteLibrary, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelRemotePath, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jRemotePath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jInitRepo)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelRevision)
                    .addComponent(jRevision, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jAutoSync)
                    .addComponent(jLabelTypeOptional)
                    .addComponent(jSyncBtn)
                )

                .addPreferredGap(ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                .addComponent(jLabelContributorsOnly)
                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelTypeContributorPrefix)
                    .addComponent(jPrefix, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addGap(2, 2, 2)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jUserId, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelUserId)
                    .addComponent(jPassword, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelPassword)
                )

                .addPreferredGap(ComponentPlacement.UNRELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jOK)
                    .addComponent(jCancel)
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

    private void jTypeComboActionPerformed(java.awt.event.ActionEvent evt) {

        /* Lazy hack to make library edit screen a bit less overwhelming */
        if (jTypeCombo.getSelectedItem() == "local") {
            /* Grey out Git part of settings window */
            jLabelRemotePath.setEnabled(false);
            jRemotePath.setEnabled(false);
            jLabelRemoteLibrary.setEnabled(false);
            jLabelUserId.setEnabled(false);
            jUserId.setEnabled(false);
            jLabelUserId.setEnabled(false);
            jLabelPassword.setEnabled(false);
            jPassword.setEnabled(false);
            jLabelContributorsOnly.setEnabled(false);
            jLabelTypeOptional.setEnabled(false);
            jLabelTypeContributorPrefix.setEnabled(false);
            jPrefix.setEnabled(false);
            jRevision.setEnabled(false);
            jLabelRevision.setEnabled(false);
            jInitRepo.setVisible(false);
            jAutoSync.setVisible(false);
            jSyncBtn.setVisible(false);
        } else {
            jLabelRemotePath.setEnabled(true);
            jRemotePath.setEnabled(true);
            jLabelRemoteLibrary.setEnabled(true);
            jLabelUserId.setEnabled(true);
            jUserId.setEnabled(true);
            jLabelUserId.setEnabled(true);
            jLabelPassword.setEnabled(true);
            jPassword.setEnabled(true);
            jLabelContributorsOnly.setEnabled(true);
            jLabelTypeOptional.setEnabled(true);
            jLabelTypeContributorPrefix.setEnabled(true);
            jPrefix.setEnabled(true);
            jRevision.setEnabled(true);
            jLabelRevision.setEnabled(true);
            jInitRepo.setVisible(true);
            jAutoSync.setVisible(true);
            jSyncBtn.setVisible(true);
        }
    }

    private void jOKActionPerformed(java.awt.event.ActionEvent evt) {
        if (jId.getText() == null || jId.getText().length() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Id is required, and must be unique",
                    "Invalid Library",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (jLocalDir.getText() == null || jLocalDir.getText().length() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Local directory is required, and must be valid",
                    "Invalid Library",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (jId.getText().equals(AxolotiLibrary.USER_LIBRARY_ID)
                && (jUserId.getText() != null && jUserId.getText().length() > 0)) {
            char[] p = jPassword.getPassword();
            if ((jPrefix.getText() == null || jPrefix.getText().length() == 0)
                    || (p == null || p.length == 0)) {
                JOptionPane.showMessageDialog(this,
                        "Contributors for the community library need to specify username, password and prefix",
                        "Invalid Library",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        populateLib(library);
        setVisible(false);
        dispose();
    }

    private void jCancelActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
        dispose();
    }

    private void jInitRepoActionPerformed(java.awt.event.ActionEvent evt) {
        boolean delete;
        int options = JOptionPane.OK_CANCEL_OPTION;
        int res = JOptionPane.showConfirmDialog(this, "Init will delete/overwrite the existing directory.\nContinue?", "Warning", options);
        if (res == JOptionPane.CANCEL_OPTION) {
            return;
        }
        delete = (res == JOptionPane.OK_OPTION);

        AxoGitLibrary gitlib = new AxoGitLibrary();
        populateLib(gitlib);
        gitlib.init(delete);
    }

    private void jSelectDirBtnActionPerformed(java.awt.event.ActionEvent evt) {
        String dir = jLocalDir.getText();
        if (dir == null || dir.length() == 0) {
            dir = prefs.getCurrentFileDirectory();
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

        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            seldir = fc.getSelectedFile();
            if (!seldir.exists()) {
                seldir = seldir.getParentFile();
            }
            try {
                dir = seldir.getCanonicalPath();
                jLocalDir.setText(dir + File.separator);
            } catch (IOException ex) {
                Logger.getLogger(AxolotiLibraryEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        fc.updateCurrentSize();

    }

    private void jSyncBtnActionPerformed(java.awt.event.ActionEvent evt) {
        AxoGitLibrary gitlib = new AxoGitLibrary();
        populateLib(gitlib);
        gitlib.sync();
    }


    private Box.Filler filler1;
    private JCheckBox jAutoSync;
    private JButton jCancel;
    private JCheckBox jEnabled;
    private JTextField jId;
    private JButton jInitRepo;
    private JLabel jLabelType;
    private JLabel jLabelTypeOptional;
    private JLabel jLabelTypeContributorPrefix;
    private JLabel jLabelId;
    private JLabel jLabelDirectory;
    private JLabel jLabelRemotePath;
    private JLabel jLabelRemoteLibrary;
    private JLabel jLabelUserId;
    private JLabel jLabelPassword;
    private JLabel jLabelRevision;
    private JLabel jLabelContributorsOnly;
    private JTextField jLocalDir;
    private JButton jOK;
    private JPasswordField jPassword;
    private JTextField jPrefix;
    private JTextField jRemotePath;
    private JTextField jRevision;
    private JButton jSelectDirBtn;
    private JSeparator jSeparator1;
    private JButton jSyncBtn;
    private JComboBox<String> jTypeCombo;
    private JTextField jUserId;

    public AxolotiLibrary getLibrary() {
        return library;
    }

    public void setLibrary(AxolotiLibrary library) {
        this.library = library;
    }

    private void populateLib(AxolotiLibrary library) {
        library.setId(jId.getText().trim());
        library.setLocalLocation(jLocalDir.getText().trim());
        library.setRemoteLocation(jRemotePath.getText().trim());
        library.setUserId(jUserId.getText().trim());
        library.setPassword(new String(jPassword.getPassword()));
        library.setEnabled(jEnabled.isSelected());
        library.setType((String) jTypeCombo.getSelectedItem());
        library.setAutoSync(jAutoSync.isSelected());
        library.setContributorPrefix(jPrefix.getText().trim());
        library.setRevision(jRevision.getText().trim());
    }
}
