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
package axoloti.objecteditor;

import static axoloti.MainFrame.fc;

import axoloti.DocumentWindow;
import axoloti.DocumentWindowList;
import axoloti.MainFrame;
import axoloti.Patch;
import axoloti.attributedefinition.AxoAttribute;
import axoloti.dialogs.KeyboardNavigableOptionPane;
import axoloti.displays.Display;
import axoloti.inlets.Inlet;
import axoloti.listener.ObjectModifiedListener;
import axoloti.object.AxoObject;
import axoloti.object.AxoObjectAbstract;
import axoloti.object.AxoObjectInstance;
import axoloti.outlets.Outlet;
import axoloti.parameters.Parameter;
import axoloti.ui.CustomImageTabbedPaneUI;
// import axoloti.ui.MaterialColors;
import axoloti.ui.SvgIconLoader;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Constants;
import axoloti.utils.FileUtils;
import axoloti.utils.KeyUtils;
import axoloti.utils.OSDetect;
import axoloti.utils.OSDetect.OS;
import axoloti.utils.Preferences;
import components.ScrollPaneComponent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

/**
 *
 * @author Johannes Taelman
 */
public final class AxoObjectEditor extends JFrame implements DocumentWindow, ObjectModifiedListener {

    private static final Logger LOGGER = Logger.getLogger(AxoObjectEditor.class.getName());

    private final AxoObject editObj;
    private final AxoObjectInstance editObjInstance;
    private Patch patch;
    private boolean isDirty = false;
    private AxolotiLibrary sellib = null;

    private String origXML;
    private final RSyntaxTextArea jTextAreaLocalData;
    private final RSyntaxTextArea jTextAreaInitCode;
    private final RSyntaxTextArea jTextAreaKRateCode;
    private final RSyntaxTextArea jTextAreaSRateCode;
    private final RSyntaxTextArea jTextAreaDisposeCode;
    private final RSyntaxTextArea jTextAreaMidiCode;
    private boolean isUpdatingView = false;

    final String fileExtension = ".axo";

    private boolean readonly = false;
    private AxoCompletionProvider acProvider;

    private axoloti.objecteditor.AttributeDefinitionsEditorPanel attributeDefinitionsEditorPanel1;
    private axoloti.objecteditor.DisplayDefinitionsEditorPanel displayDefinitionsEditorPanel1;
    private axoloti.menus.FileMenu fileMenu1;
    private axoloti.menus.HelpMenu helpMenu1;
    private axoloti.objecteditor.InletDefinitionsEditorPanel inletDefinitionsEditor1;

    private javax.swing.JInternalFrame jInternalFrame1;
    private javax.swing.JLabel jLabelTitleLibrary;
    private javax.swing.JLabel jLabelDescription;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabelIncludes;
    private javax.swing.JLabel jLabelDepends;
    private javax.swing.JLabel jLabelTitleName;
    private javax.swing.JLabel jLabeltitleAuthor;
    private javax.swing.JLabel jLabelTitleLicense;
    private javax.swing.JLabel jLabelHelp;
    private javax.swing.JLabel jLabelLibrary;
    private javax.swing.JLabel jLabelMidiPrototype;
    private javax.swing.JLabel jLabelMidiPrototypeEnd;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JList jListDepends;
    private javax.swing.JList jListIncludes;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItemCopyToLibrary;
    private javax.swing.JMenuItem jMenuItemClose;
    private javax.swing.JMenuItem jMenuItemRevert;
    private javax.swing.JMenuItem jMenuItemSave;
    private javax.swing.JMenuItem jMenuItemSaveAs;
    private javax.swing.JPanel jPanelTabs;
    private javax.swing.JPanel jPanelBasicInfo;
    private javax.swing.JPanel jPanelDetails;
    private javax.swing.JPanel jPanelDisposeCode;
    private javax.swing.JPanel jPanelInitCode;
    private javax.swing.JPanel jPanelKRateCode;
    private javax.swing.JPanel jPanelKRateCode2;
    private javax.swing.JPanel jPanelLocalData;
    private javax.swing.JPanel jPanelMidiCode;
    private javax.swing.JPanel jPanelMidiCode2;
    private javax.swing.JPanel jPanelOverview;
    private javax.swing.JPanel jPanelSRateCode;
    private javax.swing.JPanel jPanelXML;
    private ScrollPaneComponent jScrollPaneDepends;
    private ScrollPaneComponent jScrollPaneDescription;
    private ScrollPaneComponent jScrollPaneIncludes;
    private ScrollPaneComponent jScrollPaneXML;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextDesc;
    private javax.swing.JTextField jTextFieldAuthor;
    private javax.swing.JTextField jTextFieldHelp;
    private javax.swing.JTextField jTextFieldLicense;
    private axoloti.objecteditor.OutletDefinitionsEditorPanel outletDefinitionsEditorPanel1;
    private axoloti.objecteditor.ParamDefinitionsEditorPanel paramDefinitionsEditorPanel1;
    private org.fife.ui.rsyntaxtextarea.RSyntaxTextArea rSyntaxTextAreaXML;
    private axoloti.menus.WindowMenu windowMenu1;

    static RSyntaxTextArea initCodeEditor(JPanel p, AxoCompletionProvider acpr) {
        RSyntaxTextArea rsta = new RSyntaxTextArea(20, 60);

        try {
            Theme theme = Theme.load(Theme.class.getResourceAsStream(
                "/resources/rsyntaxtextarea/themes/" + Preferences.getInstance().getCodeSyntaxTheme() + ".xml"));
            theme.apply(rsta);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error trying to apply theme: " + e.getMessage());
            e.printStackTrace(System.out);
        }

        rsta.setFont(Constants.FONT_MONO);
        rsta.setAntiAliasingEnabled(true);
        rsta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);

        rsta.setCodeFoldingEnabled(true);
        // rsta.setLineWrap(true);
        rsta.setAutoIndentEnabled(true);

        // rsta.setWhitespaceVisible(true);
        // rsta.setEOLMarkersVisible(true);
        rsta.setClearWhitespaceLinesEnabled(true);
        rsta.setPaintTabLines(true);

        rsta.setBracketMatchingEnabled(true);
        rsta.setShowMatchedBracketPopup(false);

        rsta.setMarkOccurrences(true);
        rsta.setPaintMarkOccurrencesBorder(true);

        RTextScrollPane sp = new RTextScrollPane(rsta);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getHorizontalScrollBar().setUnitIncrement(10);
        sp.getVerticalScrollBar().setUnitIncrement(10);
        p.setLayout(new BorderLayout());
        p.add(sp);

        rsta.setVisible(true);
        rsta.setToolTipText(null);

        if(OSDetect.getOS() == OS.MAC)
        {
            InputMap im = rsta.getInputMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "caret-begin-line");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, java.awt.event.InputEvent.SHIFT_DOWN_MASK), "selection-begin-line");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, java.awt.event.InputEvent.CTRL_DOWN_MASK), "caret-begin");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK), "selection-begin");

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "caret-end-line");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, java.awt.event.InputEvent.SHIFT_DOWN_MASK), "selection-end-line");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, java.awt.event.InputEvent.CTRL_DOWN_MASK), "caret-end");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK), "selection-end");
        }

        AutoCompletion ac = new AutoCompletion(acpr);
        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        ac.setAutoActivationDelay(500);
        ac.setShowDescWindow(false);
        ac.install(rsta);

        return rsta;
    }

    private abstract class DocumentChangeListener implements DocumentListener {

        abstract void update();

        @Override
        public void insertUpdate(DocumentEvent e) {
            if (!isUpdatingView) update();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (!isUpdatingView) update();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (!isUpdatingView) update();
        }
    }

    String CleanString(String s) {
        if (s == null) {
            return null;
        }

        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }

        return s;
    }

    public void updateReferenceXML() {
        Serializer serializer = new Persister(new Format(2));
        ByteArrayOutputStream origOS = new ByteArrayOutputStream(2048);

        try {
            serializer.write(editObj, origOS);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error trying to write reference XML: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }

        origXML = origOS.toString();
    }

    void SaveAsDialog() {
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(Preferences.getInstance().getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Save As...");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(FileUtils.axoFileFilter);

        String fn = editObj.sObjFilePath != null ? editObj.sObjFilePath : "untitled object";
        File f = new File(fn);
        fc.setSelectedFile(f);

        int returnVal = fc.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            fc.updateCurrentSize();
            return;
        }

        File fileToBeSaved = fc.getSelectedFile();
        String filterExt = fileExtension;

        String fname = fileToBeSaved.getAbsolutePath();
        if (!fname.toLowerCase().endsWith(filterExt.toLowerCase())) {
            fileToBeSaved = new File(fname + filterExt);
        }
        
        if (fileToBeSaved.exists()) {
            Object[] options = {"Overwrite", "Cancel"};
            int n = KeyboardNavigableOptionPane.showOptionDialog(this,
                    "File exists! Overwrite?",
                    "File Exists",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]);
            if (n != JOptionPane.YES_OPTION) {
                fc.updateCurrentSize();
                return;
            }
        }

        String filenamePath = fileToBeSaved.getPath();
        Preferences.getInstance().setCurrentFileDirectory(fileToBeSaved.getParentFile().getPath());

        if (isCompositeObject()) {
            JOptionPane.showMessageDialog(null, "The original object file '" + filenamePath + "' contains multiple objects, the object editor does not support this.\n"
                    + "Your changes are NOT saved!");
            fc.updateCurrentSize();
            return;
        }

        try {
            AxoObject saveObj = editObj.clone();
            String newId = fileToBeSaved.getName().replace(fileExtension, "");
            saveObj.id = newId;
            saveObj.shortId = newId;

            HashSet<String> tempIncludes = new HashSet<>();
            for (String str : editObj.includes) {
                if (str.startsWith("chibios")) {
                    continue; /* skip "firmware-internal" chibios includes */
                }
                else {
                    tempIncludes.add(str);
                }
            }
            HashSet<String> includes = editObj.GetIncludes(editObj.sObjFilePath, tempIncludes);
            if (includes != null && !includes.isEmpty()) {
                File destinationDir = fileToBeSaved.getParentFile();
                for (String sourcePath : includes) {
                    File incf = new File(sourcePath);
                    if (incf.exists() && incf.canRead()) {
                        File destFile = new File(destinationDir, incf.getName());
                        LOGGER.log(Level.INFO, "Copying include file: " + incf.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                        Files.copy(incf.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        LOGGER.log(Level.SEVERE, "Could not copy include file: " + sourcePath + " - File does not exist or is not readable.");
                    }
                }
            }
            
            MainFrame.axoObjects.WriteAxoObject(filenamePath, saveObj);
            updateReferenceXML();
            MainFrame.axoObjects.LoadAxoObjects();

        } catch (CloneNotSupportedException ex) {
            LOGGER.log(Level.SEVERE, "Error trying to clone object: " + ex.getMessage(), ex);
            ex.printStackTrace(System.out);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "I/O Error trying to save or copy files: " + ex.getMessage(), ex);
            ex.printStackTrace(System.out);
        }
        fc.updateCurrentSize();
    }

    void Revert() {
        try {
            Serializer serializer = new Persister(new Format(2));
            AxoObject objrev = serializer.read(AxoObject.class, origXML);
            editObj.copy(objrev);
            updateEditorView();
            setDirty(false);
            Close();
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error trying to revert data: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    public AxoObjectEditor(final Patch patch, final AxoObject origObj, final AxoObjectInstance origObjInstance) {
        this.patch = patch;
        this.editObjInstance = origObjInstance;
        
        initComponents();
        setLocationRelativeTo(getParent());
        acProvider = new AxoCompletionProvider();

        Map<Integer, Icon> customIcons = new HashMap<>();
        customIcons.put(0, SvgIconLoader.load("/resources/icons/message-square-info-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(1, SvgIconLoader.load("/resources/icons/tags-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(2, SvgIconLoader.load("/resources/icons/coins-alt-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(3, SvgIconLoader.load("/resources/icons/monitor-heart-rate-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(4, SvgIconLoader.load("/resources/icons/gem-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(5, SvgIconLoader.load("/resources/icons/arrow-right-to-bracket-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(6, SvgIconLoader.load("/resources/icons/arrow-right-from-bracket-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(7, SvgIconLoader.load("/resources/icons/sliders-up-svgrepo-com_mod.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(8, SvgIconLoader.load("/resources/icons/shuffle-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(9, SvgIconLoader.load("/resources/icons/wave-pulse-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(10, SvgIconLoader.load("/resources/icons/midi-player-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(11, SvgIconLoader.load("/resources/icons/trash-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));
        customIcons.put(12, SvgIconLoader.load("/resources/icons/code-svgrepo-com.svg", 18, axoloti.ui.Theme.Button_Accent_Background));

        CustomImageTabbedPaneUI customTabUI = new CustomImageTabbedPaneUI(customIcons);
        jTabbedPane1.setUI(customTabUI);
        
        fileMenu1.initComponents();
        DocumentWindowList.RegisterWindow(this);

        jTextAreaLocalData = initCodeEditor(jPanelLocalData, acProvider);
        jTextAreaInitCode = initCodeEditor(jPanelInitCode, acProvider);
        jTextAreaKRateCode = initCodeEditor(jPanelKRateCode2, acProvider);
        jTextAreaSRateCode = initCodeEditor(jPanelSRateCode, acProvider);
        jTextAreaDisposeCode = initCodeEditor(jPanelDisposeCode, acProvider);
        jTextAreaMidiCode = initCodeEditor(jPanelMidiCode2, acProvider);

        Icon icon = SvgIconLoader.load("/resources/appicons/ksoloti_icon_axo.svg", 32);
        if (icon != null) {
            if (icon instanceof ImageIcon) {
                setIconImage(((ImageIcon) icon).getImage());
            } else {
                setIconImage(SvgIconLoader.toBufferedImage(icon));
            }
        } else {
            System.err.println("Failed to load SVG icon. Falling back to PNG.");
            setIconImage(new ImageIcon(getClass().getResource("/resources/appicons/ksoloti_icon_axo.png")).getImage());
        }

        editObj = origObj;
        updateAcProvider(editObj);

        initEditFromOrig();
        updateReferenceXML();

        inletDefinitionsEditor1.initComponents(editObj);
        outletDefinitionsEditorPanel1.initComponents(editObj);
        paramDefinitionsEditorPanel1.initComponents(editObj);
        attributeDefinitionsEditorPanel1.initComponents(editObj);
        displayDefinitionsEditorPanel1.initComponents(editObj);

        jTextFieldAuthor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sAuthor = jTextFieldAuthor.getText().trim();
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextFieldLicense.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sLicense = jTextFieldLicense.getText().trim();
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextFieldHelp.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.helpPatch = jTextFieldHelp.getText().trim();
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextDesc.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sDescription = jTextDesc.getText().trim();
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextAreaLocalData.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sLocalData = CleanString(jTextAreaLocalData.getText());
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextAreaInitCode.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sInitCode = CleanString(jTextAreaInitCode.getText());
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextAreaKRateCode.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sKRateCode = CleanString(jTextAreaKRateCode.getText());
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextAreaSRateCode.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sSRateCode = CleanString(jTextAreaSRateCode.getText());
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextAreaDisposeCode.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sDisposeCode = CleanString(jTextAreaDisposeCode.getText());
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        jTextAreaMidiCode.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            void update() {
                editObj.sMidiCode = CleanString(jTextAreaMidiCode.getText());
                editObj.FireObjectModified(AxoObjectEditor.this);
            }
        });

        rSyntaxTextAreaXML.setFont(Constants.FONT_MONO);
        rSyntaxTextAreaXML.setEditable(false);

        // is it from the factory?
        for (AxolotiLibrary lib : Preferences.getInstance().getLibraries()) {
            if (editObj.sObjFilePath != null && editObj.sObjFilePath.startsWith(lib.getLocalLocation())) {
                if (sellib == null || sellib.getLocalLocation().length() < lib.getLocalLocation().length()) {
                    sellib = lib;
                }
            }
        }

        /* Build window title from object instance name, embedded status, and patch file */
        String t = "";
        String fpath = patch.getFileNamePath();
        
        if (IsEmbeddedObj()) {
            if (editObjInstance != null && editObjInstance.getInstanceName() != null) {
                t += editObjInstance.getInstanceName();
            } else {
                t += "patch/object";
            }
            if (fpath != null) {
                String fn;
                int brk = fpath.lastIndexOf(File.separator) + 1;
                if (brk != 0) {
                    fn = fpath.substring(brk);
                } else {
                    fn = fpath;
                }
                t += "  (embedded in " + fn + ")";
            } else {
                t += "  (embedded)";
            }
            jLabelLibrary.setText("none (embedded in patch)");

            /* Embedded objects have no use for help patches */
            jTextFieldHelp.setVisible(false);
            jLabelHelp.setVisible(false);
            jMenuItemSave.setToolTipText("You are editing an embedded object. This action will save the patch this object is embedded in.");

            /* Embedded objects cannot be reverted to library state as they are part of the patch file */
            jMenuItemRevert.setEnabled(false);
        } else { /* library objects */
            if (sellib != null) {
                jMenuItemSave.setEnabled(!sellib.isReadOnly());
                if (sellib.isReadOnly()) {
                    SetReadOnly(true);
                    jMenuItemSave.setToolTipText("Cannot save: this object belongs to a read-only library.");
                    jLabelLibrary.setText(sellib.getId() + " (readonly)");
                    t += sellib.getId() + ": " + origObj.id + " (readonly)";
                }
                else {
                    jMenuItemSave.setToolTipText("You are editing a library object. This action will save the object file, not the patch.");
                    jLabelLibrary.setText(sellib.getId());
                    t += sellib.getId() + ": " + origObj.id;
                }
            } else if (fpath != null) {
                int brk = fpath.lastIndexOf(File.separator) + 1;
                if (brk != 0) {
                    t += editObj.sObjFilePath + " - " + fpath.substring(brk);
                } else {
                    t += editObj.sObjFilePath + " - " + fpath;
                }
            }
        }
        setTitle(t);
        
        updateEditorView();
        jTextDesc.requestFocus();
    }

    boolean IsEmbeddedObj() {
        return (editObj.sObjFilePath == null || editObj.sObjFilePath.isEmpty());
    }

    void SetReadOnly(boolean readonly) {
        this.readonly = readonly;

        jTextDesc.setEditable(!readonly);
        jTextFieldAuthor.setEditable(!readonly);
        jTextFieldLicense.setEditable(!readonly);
        jTextFieldHelp.setEditable(!readonly);
        jTextAreaLocalData.setEditable(!readonly);
        jTextAreaInitCode.setEditable(!readonly);
        jTextAreaKRateCode.setEditable(!readonly);
        jTextAreaSRateCode.setEditable(!readonly);
        jTextAreaDisposeCode.setEditable(!readonly);
        jTextAreaMidiCode.setEditable(!readonly);
        inletDefinitionsEditor1.setEditable(!readonly);
        outletDefinitionsEditorPanel1.setEditable(!readonly);
        paramDefinitionsEditorPanel1.setEditable(!readonly);
        attributeDefinitionsEditorPanel1.setEditable(!readonly);
        displayDefinitionsEditorPanel1.setEditable(!readonly);
    }

    void initFields() {
        jLabelName.setText(editObj.getCName());
        jTextFieldLicense.setText(editObj.sLicense);
        jTextFieldLicense.setCaretPosition(0);
        jTextDesc.setText(editObj.sDescription);
        jTextDesc.setCaretPosition(0);
        jTextFieldAuthor.setText(editObj.sAuthor);
        jTextFieldAuthor.setCaretPosition(0);
        jTextFieldHelp.setText(editObj.helpPatch);
        jTextFieldHelp.setCaretPosition(0);

        ((DefaultListModel) jListIncludes.getModel()).removeAllElements();
        if (editObj.includes != null) {
            for (String i : editObj.includes) {
                ((DefaultListModel) jListIncludes.getModel()).addElement(i);
            }
        }

        ((DefaultListModel) jListDepends.getModel()).removeAllElements();
        if (editObj.depends != null) {
            for (String i : editObj.depends) {
                ((DefaultListModel) jListDepends.getModel()).addElement(i);
            }
        }

        // this updates text editors
        updateEditorView();
    }

    boolean compareField(String oVal, String nVal) {
        String ov = oVal, nv = nVal;
        if (ov == null) {
            ov = "";
        }
        return ov.equals(nv);
    }

    boolean hasChanged() {
        Serializer serializer = new Persister(new Format(2));
        ByteArrayOutputStream editOS = new ByteArrayOutputStream(2048);

        try {
            serializer.write(editObj, editOS);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error trying to write stream: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }

        return !(origXML.equals(editOS.toString()));
    }

    private void updateEditorView() {
        isUpdatingView = true;

        if (!jTextAreaLocalData.isFocusOwner()) {
            jTextAreaLocalData.setText(editObj.sLocalData == null ? "" : editObj.sLocalData);
            jTextAreaLocalData.setCaretPosition(0);
        }
        
        if (!jTextAreaInitCode.isFocusOwner()) {
            jTextAreaInitCode.setText(editObj.sInitCode == null ? "" : editObj.sInitCode);
            jTextAreaInitCode.setCaretPosition(0); 
        }
        
        if (!jTextAreaKRateCode.isFocusOwner()) {
            jTextAreaKRateCode.setText(editObj.sKRateCode == null ? "" : editObj.sKRateCode);
            jTextAreaKRateCode.setCaretPosition(0); 
        }
        
        if (!jTextAreaSRateCode.isFocusOwner()) {
            jTextAreaSRateCode.setText(editObj.sSRateCode == null ? "" : editObj.sSRateCode);
            jTextAreaSRateCode.setCaretPosition(0); 
        }
        
        if (!jTextAreaDisposeCode.isFocusOwner()) {
            jTextAreaDisposeCode.setText(editObj.sDisposeCode == null ? "" : editObj.sDisposeCode);
            jTextAreaDisposeCode.setCaretPosition(0); 
        }
        
        if (!jTextAreaMidiCode.isFocusOwner()) {
            jTextAreaMidiCode.setText(editObj.sMidiCode == null ? "" : editObj.sMidiCode);
            jTextAreaMidiCode.setCaretPosition(0);
        }
        
        Serializer serializer = new Persister(new Format(2));
        ByteArrayOutputStream os = new ByteArrayOutputStream(2048);

        try {
            serializer.write(editObj, os);
            Theme theme = Theme.load(Theme.class.getResourceAsStream(
                "/resources/rsyntaxtextarea/themes/" + Preferences.getInstance().getCodeSyntaxTheme() + ".xml"));
            theme.apply(rSyntaxTextAreaXML);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error trying to apply theme: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }

        rSyntaxTextAreaXML.setFont(Constants.FONT_MONO);
        rSyntaxTextAreaXML.setText(os.toString());
        rSyntaxTextAreaXML.setCaretPosition(0);
        rSyntaxTextAreaXML.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        rSyntaxTextAreaXML.setCodeFoldingEnabled(true);
        updateAcProvider(editObj);

        editObj.CreateInstance(null, "test", new Point(0, 0));

        isUpdatingView = false;
    }

    @Override
    public void ObjectModified(Object src) {
        syncEditorDataToModel();

        SwingUtilities.invokeLater(() -> {
            updateAcProvider(editObj);
            updateEditorView();

            boolean changed = hasChanged();
            if (changed != isDirty()) {
                setDirty(changed);
            }
            if (changed && IsEmbeddedObj() && patch != null) {
                patch.SetDirty();
            }
        });
    }

    private void syncEditorDataToModel() {
        editObj.sLocalData = jTextAreaLocalData.getText();
        editObj.sInitCode = jTextAreaInitCode.getText();
        editObj.sKRateCode = jTextAreaKRateCode.getText();
        editObj.sSRateCode = jTextAreaSRateCode.getText();
        editObj.sDisposeCode = jTextAreaDisposeCode.getText();
        editObj.sMidiCode = jTextAreaMidiCode.getText();
        
        editObj.sAuthor = jTextFieldAuthor.getText();
        editObj.sLicense = jTextFieldLicense.getText();
        editObj.sDescription = jTextDesc.getText();
        editObj.helpPatch = jTextFieldHelp.getText();
    }

    public void initEditFromOrig() {
        editObj.addObjectModifiedListener(this);
        initFields();
        updateEditorView();
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
        setUnsavedAsterisk(isDirty);
        if (isDirty && IsEmbeddedObj() && patch != null) {
            patch.SetDirty();
        }
    }

    public void setDirty() {
        setDirty(true);
    }

    public boolean isDirty() {
        return this.isDirty;
    }

    public void setUnsavedAsterisk(boolean b) {
        if (b && !getTitle().startsWith("*")) {
            setTitle("*" + getTitle()); /* asterisk to indicate unsaved state */
        }

        else if (!b && getTitle().startsWith("*")) {
            setTitle(getTitle().substring(1)); /* else clear asterisk */
        }
    }

    @Override
    public boolean AskClose() {
        // if it's an embedded object ("patch/object"), assume the parent patch is saving
        if (isDirty() || hasChanged()) {
            if (IsEmbeddedObj()) {
                Close();
                return false;
            } else { /* not an embedded object */
                if (!readonly) {
                    String lib = "";
                    if (sellib != null) {
                        lib += sellib.getId() + ": ";
                    }

                    Object[] options = {"Save", "Discard", "Cancel"};
                    int n = KeyboardNavigableOptionPane.showOptionDialog(
                            this,
                            "Save changes to \"" + lib + editObj.getCName() + "\" ?",
                            "Unsaved Changes",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);
                    switch (n) {
                        case JOptionPane.YES_OPTION:
                            jMenuItemSaveActionPerformed(null);
                            Close();
                            return false;
                        case JOptionPane.NO_OPTION:
                            Revert();
                            Close();
                            return false;
                        case JOptionPane.CANCEL_OPTION:
                        default: // closed
                            return true;
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "AxoObject was changed but is readonly: should not happen");
                    return true;
                }
            }
        } else {
            /* no changes */
            Close();
            return false;
        }
    }

    public void Close() {
        DocumentWindowList.UnregisterWindow(this);
        editObj.removeObjectModifiedListener(this);
        editObj.removeObjectModifiedListener(attributeDefinitionsEditorPanel1);
        editObj.removeObjectModifiedListener(displayDefinitionsEditorPanel1);
        editObj.removeObjectModifiedListener(inletDefinitionsEditor1);
        editObj.removeObjectModifiedListener(outletDefinitionsEditorPanel1);
        editObj.removeObjectModifiedListener(paramDefinitionsEditorPanel1);
        dispose();
    }

    public int getActiveTabIndex() {
        return this.jTabbedPane1.getSelectedIndex();
    }

    public void setActiveTabIndex(int n) {
        this.jTabbedPane1.setSelectedIndex(n);
    }

    boolean isCompositeObject() {
        if (editObj.sObjFilePath == null) {
            return false;
        }
        int count = 0;
        for (AxoObjectAbstract o : MainFrame.axoObjects.ObjectList) {
            if (editObj.sObjFilePath.equalsIgnoreCase(o.sObjFilePath)) {
                count++;
            }
        }
        return (count > 1);
    }

    void updateAcProvider(AxoObject obj) {
        /* add keywords to autocomplete list */
        if (obj == null) return;

        if (obj.inlets != null && obj.inlets.size() > 0) {
            for (Inlet i : obj.inlets) {
                acProvider.addACKeyword(i.GetCName());
            }
        }
        if (obj.outlets != null && obj.outlets.size() > 0) {
            for (Outlet o : obj.outlets) {
                acProvider.addACKeyword(o.GetCName());
            }
        }
        if (obj.attributes != null && obj.attributes.size() > 0) {
            for (AxoAttribute a : obj.attributes) {
                acProvider.addACKeyword(a.GetCName());
            }
        }
        if (obj.params != null && obj.params.size() > 0) {
            for (Parameter p : obj.params) {
                acProvider.addACKeyword(p.GetCName());
            }
        }
        if (obj.displays != null && obj.displays.size() > 0) {
            for (Display d : obj.displays) {
                acProvider.addACKeyword(d.GetCName());
            }
        }
    }


    // @SuppressWarnings("unchecked")
    
private void initComponents() {

        jInternalFrame1 = new javax.swing.JInternalFrame();
        jLabel4 = new javax.swing.JLabel();
        jPanelTabs = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelOverview = new javax.swing.JPanel();
        jPanelBasicInfo = new javax.swing.JPanel();
        jLabelTitleLibrary = new javax.swing.JLabel();
        jLabelLibrary = new javax.swing.JLabel();
        jLabelTitleName = new javax.swing.JLabel();
        jLabelName = new javax.swing.JLabel();
        jLabeltitleAuthor = new javax.swing.JLabel();
        jTextFieldAuthor = new javax.swing.JTextField();
        jLabelTitleLicense = new javax.swing.JLabel();
        jTextFieldLicense = new javax.swing.JTextField();
        jLabelHelp = new javax.swing.JLabel();
        jTextFieldHelp = new javax.swing.JTextField();
        jPanelDetails = new javax.swing.JPanel();
        jLabelDescription = new javax.swing.JLabel();
        jScrollPaneDescription = new ScrollPaneComponent();
        jTextDesc = new javax.swing.JTextArea();
        jLabelIncludes = new javax.swing.JLabel();
        jScrollPaneIncludes = new ScrollPaneComponent();
        jListIncludes = new javax.swing.JList<>();
        jLabelDepends = new javax.swing.JLabel();
        jScrollPaneDepends = new ScrollPaneComponent();
        jListDepends = new javax.swing.JList<>();
        inletDefinitionsEditor1 = new axoloti.objecteditor.InletDefinitionsEditorPanel();
        outletDefinitionsEditorPanel1 = new axoloti.objecteditor.OutletDefinitionsEditorPanel();
        attributeDefinitionsEditorPanel1 = new axoloti.objecteditor.AttributeDefinitionsEditorPanel();
        paramDefinitionsEditorPanel1 = new axoloti.objecteditor.ParamDefinitionsEditorPanel();
        displayDefinitionsEditorPanel1 = new axoloti.objecteditor.DisplayDefinitionsEditorPanel();
        jPanelLocalData = new javax.swing.JPanel();
        jPanelInitCode = new javax.swing.JPanel();
        jPanelKRateCode = new javax.swing.JPanel();
        jPanelKRateCode2 = new javax.swing.JPanel();
        jPanelSRateCode = new javax.swing.JPanel();
        jPanelDisposeCode = new javax.swing.JPanel();
        jPanelMidiCode = new javax.swing.JPanel();
        jLabelMidiPrototype = new javax.swing.JLabel();
        jLabelMidiPrototypeEnd = new javax.swing.JLabel();
        jPanelMidiCode2 = new javax.swing.JPanel();
        jPanelXML = new javax.swing.JPanel();
        jScrollPaneXML = new ScrollPaneComponent();
        rSyntaxTextAreaXML = new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea();
        jLabel2 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu1 = new axoloti.menus.FileMenu();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemSaveAs = new javax.swing.JMenuItem();
        jMenuItemRevert = new javax.swing.JMenuItem();
        jMenuItemCopyToLibrary = new javax.swing.JMenuItem();
        jMenuItemClose = new javax.swing.JMenuItem();
        windowMenu1 = new axoloti.menus.WindowMenu();
        helpMenu1 = new axoloti.menus.HelpMenu();

        jInternalFrame1.setVisible(true);

        GroupLayout jInternalFrame1Layout = new GroupLayout(jInternalFrame1.getContentPane());
        jInternalFrame1.getContentPane().setLayout(jInternalFrame1Layout);
        jInternalFrame1Layout.setHorizontalGroup(
            jInternalFrame1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jInternalFrame1Layout.setVerticalGroup(
            jInternalFrame1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(960, 480));

        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                formWindowLostFocus(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        getContentPane().add(jLabel4);
        
        jPanelTabs.setPreferredSize(new java.awt.Dimension(640, 100));
        jPanelTabs.setLayout(new BoxLayout(jPanelTabs, BoxLayout.PAGE_AXIS));
        
        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(640, 100));
        jTabbedPane1.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        jPanelOverview.setLayout(new BoxLayout(jPanelOverview, BoxLayout.Y_AXIS));

        jPanelBasicInfo.setLayout(new java.awt.GridLayout(5, 2));

        jLabelTitleLibrary.setText("Library:");
        jPanelBasicInfo.add(jLabelTitleLibrary);

        jLabelLibrary.setText("library");
        jPanelBasicInfo.add(jLabelLibrary);

        jLabelTitleName.setText("Name:");
        jPanelBasicInfo.add(jLabelTitleName);

        jLabelName.setText("object name");
        jPanelBasicInfo.add(jLabelName);

        jLabeltitleAuthor.setText("Author:");
        jPanelBasicInfo.add(jLabeltitleAuthor);

        jTextFieldAuthor.setText("author");
        jPanelBasicInfo.add(jTextFieldAuthor);

        jLabelTitleLicense.setText("License:");
        jPanelBasicInfo.add(jLabelTitleLicense);

        jTextFieldLicense.setText("license");
        jPanelBasicInfo.add(jTextFieldLicense);

        jLabelHelp.setText("Help patch");
        jPanelBasicInfo.add(jLabelHelp);

        jTextFieldHelp.setText("help");
        jPanelBasicInfo.add(jTextFieldHelp);

        jPanelOverview.add(jPanelBasicInfo);

        jPanelDetails.setLayout(new BoxLayout(jPanelDetails, BoxLayout.Y_AXIS));

        jLabelDescription.setText("Description:");
        jPanelDetails.add(jLabelDescription);

        jTextDesc.setColumns(20);
        jTextDesc.setLineWrap(true);
        jTextDesc.setRows(5);
        jTextDesc.setWrapStyleWord(true);
        jScrollPaneDescription.setPreferredSize(new Dimension(jScrollPaneDescription.getPreferredSize().width, 240));
        jScrollPaneDescription.setViewportView(jTextDesc);

        jPanelDetails.add(jScrollPaneDescription);

        Dimension jtb = new Dimension(jListIncludes.getPreferredSize().width, 120);

        jLabelIncludes.setText("Includes");
        jPanelDetails.add(jLabelIncludes);
        
        jListIncludes.setModel(new DefaultListModel<>());
        jScrollPaneIncludes.setPreferredSize(jtb);
        jScrollPaneIncludes.setViewportView(jListIncludes);

        jPanelDetails.add(jScrollPaneIncludes);

        jLabelDepends.setText("Dependencies");
        jPanelDetails.add(jLabelDepends);

        jListDepends.setModel(new DefaultListModel<>());
        jScrollPaneDepends.setPreferredSize(jtb);
        jScrollPaneDepends.setViewportView(jListDepends);

        jPanelDetails.add(jScrollPaneDepends);

        jPanelOverview.add(jPanelDetails);

        jTabbedPane1.addTab("Overview", jPanelOverview);
        jTabbedPane1.setToolTipTextAt(0,
            "<html><div width=640px><b>General object information.</b>" +
            "<br><br>Mostly self-explanatory, except:" +
            "<br><br>Includes: Shows a list of hard-coded file #includes for this object. These are added to the top of the generated C++ code (xpatch.cpp) and resolved to an absolute file path." +
            "<br>WIP! Currenly the only way to edit includes is by manually adding or editing XML tags inside the .axo file." +
            "<br><br>Dependencies: Shows a list of firmware components the patch is using, for example PWM, FatFS (SD card), I2C...<br>These entries are auto-generated by parsing the object code and for information only.</div></html>");

        GroupLayout attributeDefinitionsEditorPanel1Layout = new GroupLayout(attributeDefinitionsEditorPanel1);
        attributeDefinitionsEditorPanel1.setLayout(attributeDefinitionsEditorPanel1Layout);
        attributeDefinitionsEditorPanel1Layout.setHorizontalGroup(
            attributeDefinitionsEditorPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        attributeDefinitionsEditorPanel1Layout.setVerticalGroup(
            attributeDefinitionsEditorPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Attributes", attributeDefinitionsEditorPanel1);
        jTabbedPane1.setToolTipTextAt(1,
            "<html>TODO: attributes");

        GroupLayout jPanelLocalDataLayout = new GroupLayout(jPanelLocalData);
        jPanelLocalData.setLayout(jPanelLocalDataLayout);
        jPanelLocalDataLayout.setHorizontalGroup(
            jPanelLocalDataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        jPanelLocalDataLayout.setVerticalGroup(
            jPanelLocalDataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Local Data", jPanelLocalData);
        jTabbedPane1.setToolTipTextAt(2,
            "<html>TODO: local data");

        GroupLayout inletDefinitionsEditor1Layout = new GroupLayout(inletDefinitionsEditor1);
        inletDefinitionsEditor1.setLayout(inletDefinitionsEditor1Layout);
        inletDefinitionsEditor1Layout.setHorizontalGroup(
            inletDefinitionsEditor1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        inletDefinitionsEditor1Layout.setVerticalGroup(
            inletDefinitionsEditor1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        GroupLayout displayDefinitionsEditorPanel1Layout = new GroupLayout(displayDefinitionsEditorPanel1);
        displayDefinitionsEditorPanel1.setLayout(displayDefinitionsEditorPanel1Layout);
        displayDefinitionsEditorPanel1Layout.setHorizontalGroup(
            displayDefinitionsEditorPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        displayDefinitionsEditorPanel1Layout.setVerticalGroup(
            displayDefinitionsEditorPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Displays", displayDefinitionsEditorPanel1);
        jTabbedPane1.setToolTipTextAt(3,
            "<html>TODO: displays");

        GroupLayout jPanelInitCodeLayout = new GroupLayout(jPanelInitCode);
        jPanelInitCode.setLayout(jPanelInitCodeLayout);
        jPanelInitCodeLayout.setHorizontalGroup(
            jPanelInitCodeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        jPanelInitCodeLayout.setVerticalGroup(
            jPanelInitCodeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Init Code", jPanelInitCode);
        jTabbedPane1.setToolTipTextAt(4,
            "<html>TODO: init code");

        jTabbedPane1.addTab("Inlets", inletDefinitionsEditor1);
        jTabbedPane1.setToolTipTextAt(5,
            "<html>TODO: inlets");

        GroupLayout outletDefinitionsEditorPanel1Layout = new GroupLayout(outletDefinitionsEditorPanel1);
        outletDefinitionsEditorPanel1.setLayout(outletDefinitionsEditorPanel1Layout);
        outletDefinitionsEditorPanel1Layout.setHorizontalGroup(
            outletDefinitionsEditorPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        outletDefinitionsEditorPanel1Layout.setVerticalGroup(
            outletDefinitionsEditorPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Outlets", outletDefinitionsEditorPanel1);
        jTabbedPane1.setToolTipTextAt(6,
            "<html>TODO: outlets");

        GroupLayout paramDefinitionsEditorPanel1Layout = new GroupLayout(paramDefinitionsEditorPanel1);
        paramDefinitionsEditorPanel1.setLayout(paramDefinitionsEditorPanel1Layout);
        paramDefinitionsEditorPanel1Layout.setHorizontalGroup(
            paramDefinitionsEditorPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        paramDefinitionsEditorPanel1Layout.setVerticalGroup(
            paramDefinitionsEditorPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Parameters", paramDefinitionsEditorPanel1);
        jTabbedPane1.setToolTipTextAt(7,
            "<html>TODO: parameters");

        jPanelKRateCode.setLayout(new BoxLayout(jPanelKRateCode, BoxLayout.Y_AXIS));

        GroupLayout jPanelKRateCode2Layout = new GroupLayout(jPanelKRateCode2);
        jPanelKRateCode2.setLayout(jPanelKRateCode2Layout);
        jPanelKRateCode2Layout.setHorizontalGroup(
            jPanelKRateCode2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        jPanelKRateCode2Layout.setVerticalGroup(
            jPanelKRateCode2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jPanelKRateCode.add(jPanelKRateCode2);

        jTabbedPane1.addTab("K-rate Code", jPanelKRateCode);
        jTabbedPane1.setToolTipTextAt(8,
            "<html>TODO: k-rate code");

        GroupLayout jPanelSRateCodeLayout = new GroupLayout(jPanelSRateCode);
        jPanelSRateCode.setLayout(jPanelSRateCodeLayout);
        jPanelSRateCodeLayout.setHorizontalGroup(
            jPanelSRateCodeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        jPanelSRateCodeLayout.setVerticalGroup(
            jPanelSRateCodeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("S-rate Code", jPanelSRateCode);
        jTabbedPane1.setToolTipTextAt(9,
            "<html>TODO: s-rate code");

        jPanelMidiCode.setLayout(new BoxLayout(jPanelMidiCode, BoxLayout.Y_AXIS));

        jLabelMidiPrototype.setFont(Constants.FONT_MONO.deriveFont(11.0f));
        jLabelMidiPrototype.setText(AxoObjectInstance.MidiHandlerFunctionHeader);
        jPanelMidiCode.add(jLabelMidiPrototype);

        GroupLayout jPanelMidiCode2Layout = new GroupLayout(jPanelMidiCode2);
        jPanelMidiCode2.setLayout(jPanelMidiCode2Layout);
        jPanelMidiCode2Layout.setHorizontalGroup(
            jPanelMidiCode2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        jPanelMidiCode2Layout.setVerticalGroup(
            jPanelMidiCode2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 288, Short.MAX_VALUE)
        );

        jPanelMidiCode.add(jPanelMidiCode2);

        jLabelMidiPrototypeEnd.setFont(Constants.FONT_MONO.deriveFont(11.0f));
        jLabelMidiPrototypeEnd.setText("}");
        jPanelMidiCode.add(jLabelMidiPrototypeEnd);

        jTabbedPane1.addTab("MIDI Code", jPanelMidiCode);
        jTabbedPane1.setToolTipTextAt(10,
            "<html>TODO: midi code");

        rSyntaxTextAreaXML.setColumns(20);
        rSyntaxTextAreaXML.setRows(5);
        jScrollPaneXML.setViewportView(rSyntaxTextAreaXML);

        GroupLayout jPanelDisposeCodeLayout = new GroupLayout(jPanelDisposeCode);
        jPanelDisposeCode.setLayout(jPanelDisposeCodeLayout);
        jPanelDisposeCodeLayout.setHorizontalGroup(
            jPanelDisposeCodeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        jPanelDisposeCodeLayout.setVerticalGroup(
            jPanelDisposeCodeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Dispose Code", jPanelDisposeCode);
        jTabbedPane1.setToolTipTextAt(11,
            "<html>TODO: dispose code");

        GroupLayout jPanelXMLLayout = new GroupLayout(jPanelXML);
        jPanelXML.setLayout(jPanelXMLLayout);
        jPanelXMLLayout.setHorizontalGroup(
            jPanelXMLLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneXML, GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE)
        );
        jPanelXMLLayout.setVerticalGroup(
            jPanelXMLLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneXML, GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("XML Preview", jPanelXML);
        jTabbedPane1.setToolTipTextAt(12,
            "<html>TODO: xml preview");

        jPanelTabs.add(jTabbedPane1);

        jLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jPanelTabs.add(jLabel2);

        getContentPane().add(jPanelTabs);

        fileMenu1.setMnemonic('F');
        fileMenu1.setText("File");
        fileMenu1.add(jSeparator1);

        jMenuItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuItemSave.setText("Save");
        jMenuItemSave.setMnemonic('S');
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        fileMenu1.add(jMenuItemSave);

        jMenuItemSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
        jMenuItemSaveAs.setText("Save As...");
        jMenuItemSaveAs.setMnemonic('A');
        jMenuItemSaveAs.setDisplayedMnemonicIndex(5);
        jMenuItemSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveAsActionPerformed(evt);
            }
        });
        fileMenu1.add(jMenuItemSaveAs);

        jMenuItemRevert.setText("Revert");
        jMenuItemRevert.setMnemonic('R');
        jMenuItemRevert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRevertActionPerformed(evt);
            }
        });
        fileMenu1.add(jMenuItemRevert);

        jMenuItemCopyToLibrary.setText("Copy to Library...");
        jMenuItemCopyToLibrary.setMnemonic('L');
        jMenuItemCopyToLibrary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCopyToLibraryActionPerformed(evt);
            }
        });
        fileMenu1.add(jMenuItemCopyToLibrary);

        jMenuItemClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuItemClose.setText("Close");
        jMenuItemClose.setMnemonic('W');
        jMenuItemClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCloseActionPerformed(evt);
            }
        });
        fileMenu1.add(jMenuItemClose);

        jMenuBar1.add(fileMenu1);
        jMenuBar1.add(windowMenu1);

        helpMenu1.setMnemonic('H');
        helpMenu1.setText("Help");
        jMenuBar1.add(helpMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        AskClose();
    }

    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {
        syncEditorDataToModel();
        updateEditorView();

        if (isCompositeObject()) {
            JOptionPane.showMessageDialog(null, "The original object file " + editObj.sObjFilePath + " contains multiple objects, the object editor does not support this.\n"
                    + "Your changes are NOT saved!");
            return;
        }

        if (IsEmbeddedObj()) {
            if (patch != null && patch.getPatchframe() != null) {
                patch.getPatchframe().saveAction();
            } else {
                LOGGER.log(Level.SEVERE, "Cannot save embedded object: parent patch frame is null.");
            }
            setDirty(false); 
            return;
        }

        if (editObj.sObjFilePath == null || editObj.sObjFilePath.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Cannot save library object: sObjFilePath is missing.");
            return;
        }

        MainFrame.axoObjects.WriteAxoObject(editObj.sObjFilePath, editObj);
        setDirty(false);
        updateReferenceXML();
        MainFrame.axoObjects.LoadAxoObjects();
    }

    private void jMenuItemSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
        updateEditorView();
        SaveAsDialog();
    }

    private void jMenuItemCopyToLibraryActionPerformed(java.awt.event.ActionEvent evt) {
        AddToLibraryDlg dlg = new AddToLibraryDlg(this, true, editObj);
        dlg.setVisible(true);
        Close();
    }

    private void jMenuItemCloseActionPerformed(java.awt.event.ActionEvent evt) {
        AskClose();
    }

    private void jMenuItemRevertActionPerformed(java.awt.event.ActionEvent evt) {
        if (IsEmbeddedObj()) {
            return; /* should never reach here */
        }

        String info = "";
        if (sellib != null) {
            info = sellib.getId() + ": ";
        }
        info += editObj.getCName();

        Object[] options = {"Revert", "Cancel"};
        int n = KeyboardNavigableOptionPane.showOptionDialog(this,
                "Revert object \"" + info + "\" to saved state?",
                "Revert object",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
        if (n != JOptionPane.YES_OPTION) {
            return;
        }

        Rectangle editorBounds = this.getBounds();
        int activeTabIndex = this.getActiveTabIndex();
        Revert();
        AxoObjectEditor axoObjectEditor = new AxoObjectEditor(patch, editObj, editObjInstance);
        axoObjectEditor.setBounds(editorBounds);
        axoObjectEditor.setActiveTabIndex(activeTabIndex);
        axoObjectEditor.setVisible(true);
    }

    private void formWindowLostFocus(java.awt.event.WindowEvent evt) {
    }

    @Override
    public JFrame GetFrame() {
        return this;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public ArrayList<DocumentWindow> GetChildDocuments() {
        return null;
    }
}
