package axoloti;

import static axoloti.FileUtils.axtFileFilter;
import static axoloti.MainFrame.fc;
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

    int bgRGB = UIManager.getColor("Viewport.background").getRGB();
    boolean isBgDark = (((bgRGB & 0xFF0000) >> 16) + ((bgRGB & 0x00FF00) >> 8) + ((bgRGB & 0x0000FF))) / 3 < 0x80;

    /* UI */
    @Element
    public Color Console_Background = UIManager.getColor("Viewport.background");
    @Element
    public Color Patch_Unlocked_Background = isBgDark ? UIManager.getColor("Viewport.background").brighter() : UIManager.getColor("Viewport.background").darker();
    @Element
    public Color Patch_Locked_Background = isBgDark ? UIManager.getColor("Viewport.background").darker().darker() : UIManager.getColor("Viewport.background").darker().darker().darker();
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
    public Color Cable_Default_Highlighted = Color.GRAY;

    @Element
    public Color Cable_Shadow = Color.BLACK;
    @Element
    public Color Cable_Shadow_Highlighted = Color.DARK_GRAY;

    @Element
    public Color Cable_Bool32 = new Color(0xEF, 0xEF, 0x50);
    @Element
    public Color Cable_Bool32_Highlighted = Cable_Bool32.brighter();

    @Element
    public Color Cable_CharPointer32 = Color.PINK;
    @Element
    public Color Cable_CharPointer32_Highlighted = new Color(0xFF, 0xCF, 0xCF);

    @Element
    public Color Cable_Zombie = Color.WHITE;
    @Element
    public Color Cable_Zombie_Highlighted = new Color(0x9F, 0x00, 0x00);

    @Element
    public Color Cable_Frac32 = new Color(0x30, 0x50, 0xFF);
    @Element
    public Color Cable_Frac32_Highlighted = Cable_Frac32.brighter().brighter();

    @Element
    public Color Cable_Frac32Buffer = new Color(0xFF, 0x30, 0x50);
    @Element
    public Color Cable_Frac32Buffer_Highlighted = new Color(0xFF, 0x70, 0x90);

    @Element
    public Color Cable_Int32 = new Color(0x30, 0xFF, 0x50);
    @Element
    public Color Cable_Int32_Highlighted = Cable_Int32.brighter().brighter();

    @Element
    public Color Cable_Int32Pointer = Color.MAGENTA;
    @Element
    public Color Cable_Int32Pointer_Highlighted = Cable_Int32Pointer.brighter().brighter();

    @Element
    public Color Cable_Int8Array = Cable_Int32Pointer;
    @Element
    public Color Cable_Int8Array_Highlighted = Cable_Int32Pointer_Highlighted;

    @Element
    public Color Cable_Int8Pointer = Cable_Int32Pointer;
    @Element
    public Color Cable_Int8Pointer_Highlighted = Cable_Int32Pointer_Highlighted;;

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
    public Color Object_TitleBar_Subpatch_Background = new Color(
        Math.max(Object_TitleBar_Background.getRed()-0x20, 0x20),
        Math.max(Object_TitleBar_Background.getGreen()-0x20, 0x20),
        Math.min(Object_TitleBar_Background.getBlue()+0x80, 0xFF)
    );
    @Element
    public Color Object_TitleBar_Embedded_Background = new Color(
        Math.min(Object_TitleBar_Background.getRed()+0x60, 0xFF),
        Math.min(Object_TitleBar_Background.getGreen()+0x30, 0xFF),
        Math.max(Object_TitleBar_Background.getBlue()-0x20, 0x20) 
    );
    @Element
    public Color Object_TitleBar_Foreground = UIManager.getColor("Panel.background"); /* Titlebar Inverted */
    @Element
    public Color Object_Border_Unselected = Patch_Unlocked_Background;
    @Element
    public Color Object_Border_Unselected_Locked = Patch_Locked_Background;
    @Element
    public Color Object_Border_Selected = new Color(0xFF, 0x80, 0x00); /* Juicy orange */
    @Element
    public Color Object_Zombie_Background = Color.RED;

    @Element
    public Color Parameter_Default_Background = Object_Default_Background;
    @Element
    public Color Parameter_Default_Foreground = Object_Default_Foreground;
    @Element
    public Color Parameter_On_Parent_Background = new Color(
        Math.max(Object_TitleBar_Foreground.getRed()-0x20, 0x20),
        Math.max(Object_TitleBar_Foreground.getGreen()-0x20, 0x20),
        Math.min(Object_TitleBar_Foreground.getBlue()+0x80, 0xFF)
    );
    @Element
    public Color Parameter_Preset_Highlight_Foreground = UIManager.getColor("Component.accentColor");

    @Element
    public Color Component_Foreground = Object_Default_Foreground;
    @Element
    public Color Component_Mid_Dark = Color.getHSBColor(0.0f, 0.0f, isBgDark ? 0.66f : 0.33f); /* Invert grey values when dark theme is active */
    @Element
    public Color Component_Mid = Color.GRAY;
    @Element
    public Color Component_Mid_Light = Color.getHSBColor(0.0f, 0.0f, isBgDark ? 0.33f : 0.66f);
    @Element
    public Color Component_Background = isBgDark ? Object_Default_Background.darker() : Object_Default_Background.brighter();
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
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(prefs.getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Save Theme...");
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
            } 
            else if (!ext.equals(filterext)) {
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
            fc.updateCurrentSize();
            return fileToBeSaved;
        }
        else {
            fc.updateCurrentSize();
            return null;
        }
    }

    public void load(JFrame frame) {
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(prefs.getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setDialogTitle("Theme...");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(axtFileFilter);
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            prefs.setCurrentFileDirectory(fc.getCurrentDirectory().getPath());
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
