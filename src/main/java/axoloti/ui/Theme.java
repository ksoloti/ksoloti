package axoloti.ui;

import static axoloti.MainFrame.fc;
import static axoloti.utils.FileUtils.axtFileFilter;

import axoloti.dialogs.KeyboardNavigableOptionPane;
import axoloti.utils.FileUtils;
import axoloti.utils.Preferences;

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
import org.simpleframework.xml.stream.Format;

@Root
public class Theme {

    private static final Logger LOGGER = Logger.getLogger(Theme.class.getName());

    public Theme() {
        super();
    }

    private static final Registry REGISTRY = new Registry();
    private static final Strategy STRATEGY = new RegistryStrategy(Theme.REGISTRY);
    private static final Serializer SERIALIZER = new Persister(Theme.STRATEGY, new Format(2));

    static {
        try {
            REGISTRY.bind(Color.class, ColorConverter.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error trying to bind ColorConverter: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    private static Theme currentTheme;

    @Element
    public String Theme_Name = "Default";

    private static int bgRGB = UIManager.getColor("Viewport.background").getRGB();
    private static boolean isBgDark = (((bgRGB & 0xFF0000) >> 16) + ((bgRGB & 0x00FF00) >> 8) + ((bgRGB & 0x0000FF))) / 3 < 0x80;
    private static int cable_opacity = 0xFF;

    /* UI */
    @Element
    public static final Color Console_Background = UIManager.getColor("Viewport.background");
    @Element
    public static final Color Patch_Unlocked_Background = isBgDark ? UIManager.getColor("Viewport.background").brighter() : UIManager.getColor("Viewport.background").darker();
    @Element
    public static final Color Patch_Locked_Background = isBgDark ? UIManager.getColor("Viewport.background").darker().darker() : UIManager.getColor("Viewport.background").darker().darker().darker();
    @Element
    public static final Color Button_Accent_Background = UIManager.getColor("Component.accentColor");
    @Element
    public static final Color Button_Accent_Foreground = UIManager.getColor("ProgressBar.selectionForeground");
    @Element
    public static final Color ProgressBar_Overload_Foreground = new Color(0x90, 0, 0);
    @Element
    public static final Color Button_Default_Background = UIManager.getColor("Button.background");
    @Element
    public static final Color Button_Default_Foreground = UIManager.getColor("Button.foreground");
    @Element
    public static final Color Selection_Rectangle_Fill = ColorConverter.withAlpha(Patch_Unlocked_Background.brighter(), 0x40);

    /* Text */
    @Element
    public static final Color Error_Text = MaterialColors.RED_A700;
    @Element
    public static final Color Console_Normal_Text = UIManager.getColor("Viewport.foreground");
    @Element
    public static final Color Console_Warning_Text = UIManager.getColor("Component.accentColor");

    /* Nets */
    @Element
    public static final Color Cable_Default = MaterialColors.GREY_800;
    @Element
    public static final Color Cable_Default_Highlighted = MaterialColors.GREY_600;

    @Element
    public static final Color Cable_Sourceless = MaterialColors.BLACK;
    @Element
    public static final Color Cable_Sourceless_Highlighted = MaterialColors.GREY_800;

    @Element
    public static final Color Cable_Bool32 = ColorConverter.withAlpha(MaterialColors.YELLOW_A400, cable_opacity);
    @Element
    public static final Color Cable_Bool32_Highlighted = MaterialColors.YELLOW_A100;

    @Element
    public static final Color Cable_CharPointer32 = new Color(0xFF, 0x00, 0xFF, cable_opacity);
    @Element
    public static final Color Cable_CharPointer32_Highlighted = MaterialColors.PINK_100;

    @Element
    public static final Color Cable_Zombie = MaterialColors.WHITE;
    @Element
    public static final Color Cable_Zombie_Highlighted = MaterialColors.RED_A700;

    @Element
    public static final Color Cable_Frac32 = ColorConverter.withAlpha(MaterialColors.BLUE_A400, cable_opacity);
    @Element
    public static final Color Cable_Frac32_Highlighted = MaterialColors.BLUE_A100;

    @Element
    public static final Color Cable_Frac32Buffer = ColorConverter.withAlpha(MaterialColors.RED_A400, cable_opacity);
    @Element
    public static final Color Cable_Frac32Buffer_Highlighted = new Color(0xFF, 0x90, 0x90); /* almost RED_A100 but less orangy */

    @Element
    public static final Color Cable_Int32 = ColorConverter.withAlpha(MaterialColors.GREEN_A700, cable_opacity);
    @Element
    public static final Color Cable_Int32_Highlighted = MaterialColors.GREEN_A200;

    @Element
    public static final Color Cable_Int32Pointer = ColorConverter.withAlpha(MaterialColors.PINK_100, cable_opacity);
    @Element
    public static final Color Cable_Int32Pointer_Highlighted = MaterialColors.RED_50;

    @Element
    public static final Color Cable_Int8Array = Cable_Int32Pointer;
    @Element
    public static final Color Cable_Int8Array_Highlighted = Cable_Int32Pointer_Highlighted;

    @Element
    public static final Color Cable_Int8Pointer = Cable_Int32Pointer;
    @Element
    public static final Color Cable_Int8Pointer_Highlighted = Cable_Int32Pointer_Highlighted;

    /* Objects */
    @Element
    public static final Color Object_Label_Text = UIManager.getColor("Panel.foreground");
    @Element
    public static final Color Object_Default_Background = UIManager.getColor("Panel.background");
    @Element
    public static final Color Object_Default_Foreground = UIManager.getColor("Panel.foreground");
    @Element
    public static final Color Object_TitleBar_Background = UIManager.getColor("Panel.foreground"); /* Titlebar Inverted */
    @Element
    public static final Color Object_TitleBar_Subpatch_Background = new Color(
        Math.max(Object_TitleBar_Background.getRed()-0x20, 0x20),
        Math.max(Object_TitleBar_Background.getGreen()-0x20, 0x20),
        Math.min(Object_TitleBar_Background.getBlue()+0x80, 0xFF)
    );
    @Element
    public static final Color Object_TitleBar_Embedded_Background = new Color(
        Math.min(Object_TitleBar_Background.getRed()+0x60, 0xFF),
        Math.min(Object_TitleBar_Background.getGreen()+0x30, 0xFF),
        Math.max(Object_TitleBar_Background.getBlue()-0x20, 0x20) 
    );
    @Element
    public static final Color Object_TitleBar_Foreground = UIManager.getColor("Panel.background"); /* Titlebar Inverted */
    @Element
    public static final Color Object_Border_Unselected = Patch_Unlocked_Background;
    @Element
    public static final Color Object_Border_Unselected_Locked = Patch_Locked_Background;
    @Element
    public static final Color Object_Border_Selected = UIManager.getColor("Component.accentColor");
    @Element
    public static final Color Object_Zombie_Background = MaterialColors.RED_A700;

    @Element
    public static final Color Parameter_Default_Background = Object_Default_Background;
    @Element
    public static final Color Parameter_Default_Foreground = Object_Default_Foreground;
    @Element
    public static final Color Parameter_On_Parent_Background = isBgDark ? ColorConverter.withAlpha(MaterialColors.CYAN_900, 0x80) : ColorConverter.withAlpha(MaterialColors.CYAN_A200, 0x80);
    @Element
    public static final Color Parameter_On_Parent_Foreground = isBgDark ? MaterialColors.CYAN_A200 : MaterialColors.CYAN_900;

    @Element
    public static final Color Parameter_Frozen_Background = isBgDark ? Parameter_Default_Background.brighter().brighter().brighter() : Parameter_Default_Background.darker();
    // @Element /* not used */
    // public static final Color Parameter_Frozen_Foreground = isBgDark ? new Color(0x60, 0x60, 0xFF) : Color.ORANGE;

    @Element
    public static final Color Component_Foreground = Object_Default_Foreground;
    @Element
    public static final Color Component_Mid_Dark = Color.getHSBColor(0.0f, 0.0f, isBgDark ? 0.66f : 0.33f); /* Invert grey values when dark theme is active */
    @Element
    public static final Color Component_Mid = MaterialColors.GREY_600;
    @Element
    public static final Color Component_Mid_Light = Color.getHSBColor(0.0f, 0.0f, isBgDark ? 0.33f : 0.66f);
    @Element
    public static final Color Component_Background = isBgDark ? Object_Default_Background.darker() : Object_Default_Background.brighter();
    @Element
    public static final Color Component_Illuminated = MaterialColors.ORANGE_A700;

    @Element
    public static final Color Keyboard_Light = MaterialColors.WHITE;
    @Element
    public static final Color Keyboard_Mid = MaterialColors.GREY_600;
    @Element
    public static final Color Keyboard_Dark = MaterialColors.BLACK;

    @Element
    public static final Color Led_Strip_On = new Color(0.f, 1.f, 0.f, 1.0f);
    @Element
    public static final Color Led_Strip_Off = new Color(0.f, 0.f, 0.f, 0.5f);

    @Element
    public static final Color VU_Dark_Green = new Color(0.0f, 0.3f, 0.0f);
    @Element
    public static final Color VU_Dark_Yellow = new Color(0.4f, 0.4f, 0.0f);
    @Element
    public static final Color VU_Dark_Red = new Color(0.4f, 0.0f, 0.0f);
    @Element
    public static final Color VU_Bright_Green = new Color(0.0f, 0.8f, 0.0f);
    @Element
    public static final Color VU_Bright_Yellow = new Color(0.8f, 0.8f, 0.0f);
    @Element
    public static final Color VU_Bright_Red = new Color(1.0f, 0.0f, 0.0f);

    private File FileChooserSave(JFrame frame) {
        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(Preferences.getInstance().getCurrentFileDirectory()));
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
                Object[] options = {"Change",
                    "Cancel"};
                int n = KeyboardNavigableOptionPane.showOptionDialog(frame,
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
                Object[] options = {"Overwrite", "Cancel"};
                int n = KeyboardNavigableOptionPane.showOptionDialog(frame,
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
        fc.setCurrentDirectory(new File(Preferences.getInstance().getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setDialogTitle("Theme...");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(axtFileFilter);
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Preferences.getInstance().setCurrentFileDirectory(fc.getCurrentDirectory().getPath());
            File f = fc.getSelectedFile();
            if (axtFileFilter.accept(f)) {
                try {
                    FileInputStream inputStream = new FileInputStream(f);
                    currentTheme = Theme.SERIALIZER.read(Theme.class, inputStream);
                    Preferences.getInstance().setThemePath(f.getAbsolutePath());
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error trying to open theme: " + ex.getMessage());
                    ex.printStackTrace(System.out);
                }
            }
        }
    }

    public void save(JFrame frame) {
        File fileToBeSaved = FileChooserSave(frame);
        if (fileToBeSaved != null) {
            try {
                Theme.SERIALIZER.write(this, fileToBeSaved);
                Preferences.getInstance().setThemePath(fileToBeSaved.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error trying to save theme: " + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }

    public static void loadDefaultTheme() {
        currentTheme = new Theme();
        Preferences.getInstance().setThemePath(null);
    }

    public static Theme getCurrentTheme() {
        if (currentTheme == null) {
            String themePath = Preferences.getInstance().getThemePath();
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
