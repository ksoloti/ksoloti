package axoloti;

import static axoloti.FileUtils.axtFileFilter;
import static axoloti.MainFrame.prefs;
import axoloti.object.AxoObjects;
import axoloti.utils.ColorConverter;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;

@Root
public class Theme {

    public Theme() {
        super();
    }

    private static final Registry REGISTRY = new Registry();
    private static final Strategy STRATEGY = new RegistryStrategy(Theme.REGISTRY);
    private static final Serializer SERIALIZER = new Persister(Theme.STRATEGY);

    static {
        try {
            REGISTRY.bind(Color.class, ColorConverter.class);
        } catch (Exception e) {
            Logger.getLogger(AxoObjects.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private static Theme currentTheme;

    @Element
    public String Theme_Name = "Default";

    /* UI */
    @Element
    public Color Console_Background = UIManager.getColor("Viewport.background");
    @Element
    public Color Patch_Unlocked_Background = UIManager.getColor("Viewport.background").darker();
    @Element
    public Color Patch_Locked_Background = new Color(66,66,64); /* Dark grey with slight brown tint */
    @Element
    public Color Button_Accent_Background = UIManager.getColor("Component.accentColor");
    @Element
    public Color Button_Accent_Foreground = UIManager.getColor("ProgressBar.selectionForeground");
    @Element
    public Color Button_Default_Background = UIManager.getColor("Button.background");
    @Element
    public Color Button_Default_Foreground = UIManager.getColor("Button.foreground");

    /* Text */
    @Element
    public Color Error_Text = Color.RED;
    @Element
    public Color Console_Normal_Text = UIManager.getColor("Viewport.foreground");
    @Element
    public Color Console_Warning_Text = UIManager.getColor("Component.accentColor");

    /* Nets */
    @Element
    public Color Cable_Default = Color.DARK_GRAY;
    @Element
    public Color Cable_Shadow = Color.BLACK;
    @Element
    public Color Cable_Bool32 = new Color(0xFF, 0xEF, 0x40);
    @Element
    public Color Cable_CharPointer32 = Color.PINK;
    @Element
    public Color Cable_Zombie = Color.WHITE;
    @Element
    public Color Cable_Frac32 = new Color(0x20, 0x40, 0xFF);
    @Element
    public Color Cable_Frac32Buffer = new Color(0xFF, 0x20, 0x40);
    @Element
    public Color Cable_Int32 = new Color(0x20, 0xFF, 0x40);
    @Element
    public Color Cable_Int32Pointer = Color.MAGENTA;
    @Element
    public Color Cable_Int8Array = Color.MAGENTA;
    @Element
    public Color Cable_Int8Pointer = Color.MAGENTA;

    /* Objects */
    @Element
    public Color Object_Label_Text = UIManager.getColor("Panel.foreground");
    @Element
    public Color Object_Default_Background = UIManager.getColor("Panel.background");
    @Element
    public Color Object_Default_Foreground = UIManager.getColor("Panel.foreground");
    @Element
    public Color Object_TitleBar_Background = UIManager.getColor("Panel.foreground"); /* Titlebar Inverted */
    @Element
    public Color Object_TitleBar_Subpatch_Background = new Color(0.45f, 0.5f, 0.6f); /* Blueish titlebar for subpatches */
    @Element
    public Color Object_TitleBar_Embedded_Background = new Color(0.6f, 0.5f, 0.45f); /* Brownish titlebar for embedded objects */
    @Element
    public Color Object_TitleBar_Foreground = UIManager.getColor("Panel.background"); /* Titlebar Inverted */
    @Element
    public Color Object_Border_Unselected = Patch_Unlocked_Background;
    @Element
    public Color Object_Border_Unselected_Locked = Patch_Locked_Background;
    @Element
    public Color Object_Border_Selected = Color.ORANGE;
    @Element
    public Color Object_Zombie_Background = Color.RED;

    @Element
    public Color Parameter_Default_Background = Object_Default_Background;
    @Element
    public Color Parameter_Default_Foreground = Object_Default_Foreground;
    @Element
    public Color Parameter_On_Parent_Highlight = Cable_Frac32;
    @Element
    public Color Parameter_Preset_Highlight_Foreground = UIManager.getColor("Component.accentColor");

    @Element
    public Color Component_Foreground = Color.BLACK;
    @Element
    public Color Component_Mid_Dark = Color.getHSBColor(0.f, 0.0f, 0.66f);
    @Element
    public Color Component_Mid = Color.GRAY;
    @Element
    public Color Component_Mid_Light = Color.getHSBColor(0.f, 0.0f, 0.33f);
    @Element
    public Color Component_Background = Color.WHITE;
    @Element
    public Color Component_Illuminated = Color.ORANGE;

    @Element
    public Color Keyboard_Light = Color.WHITE;
    @Element
    public Color Keyboard_Mid = Color.GRAY;
    @Element
    public Color Keyboard_Dark = Color.BLACK;

    @Element
    public Color Led_Strip_On = new Color(0.f, 1.f, 0.f, 1.0f);
    @Element
    public Color Led_Strip_Off = new Color(0.f, 0.f, 0.f, 0.5f);

    @Element
    public Color VU_Dark_Green = new Color(0.0f, 0.3f, 0.0f);
    @Element
    public Color VU_Dark_Yellow = new Color(0.4f, 0.4f, 0.0f);
    @Element
    public Color VU_Dark_Red = new Color(0.4f, 0.0f, 0.0f);
    @Element
    public Color VU_Bright_Green = new Color(0.0f, 0.8f, 0.0f);
    @Element
    public Color VU_Bright_Yellow = new Color(0.8f, 0.8f, 0.0f);
    @Element
    public Color VU_Bright_Red = new Color(0.8f, 0.0f, 0.0f);

    private File FileChooserSave(JFrame frame) {
        final JFileChooser fc = new JFileChooser(MainFrame.prefs.getCurrentFileDirectory());
        fc.setPreferredSize(new java.awt.Dimension(640, 640));
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(FileUtils.axtFileFilter);

        String fn = this.Theme_Name;

        File f = new File(fn);
        fc.setSelectedFile(f);

        String ext = "";
        int dot = fn.lastIndexOf('.');
        if (dot > 0 && fn.length() > dot + 3) {
            ext = fn.substring(dot);
        }
        if (ext.equalsIgnoreCase(".axt")) {
            fc.setFileFilter(FileUtils.axtFileFilter);
        }

        int returnVal = fc.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String filterext = ".axt";
            if (fc.getFileFilter() == FileUtils.axtFileFilter) {
                filterext = ".axt";
            }

            File fileToBeSaved = fc.getSelectedFile();
            ext = "";
            String fname = fileToBeSaved.getAbsolutePath();
            dot = fname.lastIndexOf('.');
            if (dot > 0 && fname.length() > dot + 3) {
                ext = fname.substring(dot);
            }

            if (ext.equalsIgnoreCase(".axt")) {
                fileToBeSaved = new File(fc.getSelectedFile().toString());
            } else if (!ext.equals(filterext)) {
                Object[] options = {"Yes",
                    "No"};
                int n = JOptionPane.showOptionDialog(frame,
                        "File does not match filter. Change extension to " + filterext + "?",
                        "File Extension",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                switch (n) {
                    case JOptionPane.YES_OPTION:
                        fileToBeSaved = new File(fname.substring(0, fname.length() - ext.length()) + filterext);
                        break;
                    case JOptionPane.NO_OPTION:
                        return null;
                }
            }

            if (fileToBeSaved.exists()) {
                Object[] options = {"Yes",
                    "No"};
                int n = JOptionPane.showOptionDialog(frame,
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
                        return null;
                }
            }
            return fileToBeSaved;
        } else {
            return null;
        }
    }

    public JFileChooser GetFileChooser() {
        JFileChooser fc = new JFileChooser(prefs.getCurrentFileDirectory());
        fc.setPreferredSize(new java.awt.Dimension(640, 640));
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(axtFileFilter);
        return fc;
    }

    public void load(JFrame frame) {
        JFileChooser fc = GetFileChooser();
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            prefs.setCurrentFileDirectory(fc.getCurrentDirectory().getPath());
            prefs.SavePrefs();
            File f = fc.getSelectedFile();
            if (axtFileFilter.accept(f)) {
                try {
                    FileInputStream inputStream = new FileInputStream(f);
                    currentTheme = Theme.SERIALIZER.read(Theme.class, inputStream);
                    MainFrame.prefs.setThemePath(f.getAbsolutePath());
                } catch (Exception ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Unable to open theme {0}", new Object[]{ex});
                }
            }
        }
    }

    public void save(JFrame frame) {
        File fileToBeSaved = FileChooserSave(frame);
        if (fileToBeSaved != null) {
            try {
                Theme.SERIALIZER.write(this, fileToBeSaved);
                MainFrame.prefs.setThemePath(fileToBeSaved.getAbsolutePath());
            } catch (Exception e) {
                Logger.getLogger(AxoObjects.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    public static void loadDefaultTheme() {
        currentTheme = new Theme();
        MainFrame.prefs.setThemePath(null);
    }

    public static Theme getCurrentTheme() {
        if (currentTheme == null) {
            String themePath = MainFrame.prefs.getThemePath();
            if (themePath == null) {
                loadDefaultTheme();
            } else {
                try {
                    FileInputStream inputStream = new FileInputStream(new File(themePath));
                    currentTheme = Theme.SERIALIZER.read(Theme.class, inputStream);
                } catch (Exception ex) {
                    loadDefaultTheme();
                }
            }
        }
        return currentTheme;
    }
}
