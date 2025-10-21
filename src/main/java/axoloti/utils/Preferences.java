/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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
package axoloti.utils;

import axoloti.Axoloti;
import static axoloti.MainFrame.mainframe;
import axoloti.Version;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.*;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import com.formdev.flatlaf.FlatDarculaLaf;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneLightIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTLightOwlIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDeepOceanIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialLighterIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialOceanicIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialPalenightIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTNightOwlIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
/**
 *
 * @author Johannes Taelman
 */
@Root
public class Preferences {

    private static final Logger LOGGER = Logger.getLogger(Preferences.class.getName());

    @Attribute(required = false)
    String appVersion;

    @Element(required = false)
    String CurrentFileDirectory;

    // search path will be removed from persistence, 
    // here for compatibility only
    @Deprecated
    @Element(required = false)
    String ObjectSearchPath;
    @Deprecated
    @Element(required = false)
    String ComPortName;
    @Element(required = false)
    Integer PollInterval;
    @Element(required = false)
    Boolean MouseDialAngular;
    @Element(required = false)
    String FirmwareMode;
    @Element(required = false)
    Boolean MouseDoNotRecenterWhenAdjustingControls;
    @Element(required = false)
    Boolean ExpertMode;
    @Element(required = false)
    Boolean SortByExecution;
    @Element(required = false)
    Boolean FirmwareWarnDisable;
    @Element(required = false)
    Integer DspSafetyLimit;
    @ElementList(required = false)
    ArrayList<String> recentFiles = new ArrayList<String>();

    @Deprecated
    @Element(required = false)
    String MidiInputDevice;
    @Element(required = false)
    String FavouriteDir;
    @Element(required = false)
    String ControllerObject;
    @Element(required = false)
    Boolean ControllerEnabled;
    @Element(required = false)
    Boolean BackupPatchesOnSDEnabled;
    @Element(required = false)
    String themePath;
    @Element(required = false)
    String Theme;
    @Element(required = false)
    String codeSyntaxTheme;
    @Element(required = false)
    int CodeFontSize = 12; /* default to 12pt */
    @Element(required = false)
    String[] UserShortcuts = {"", "", "", ""}; /* Four user shortcuts in object finder */

    @ElementMap(required = false, entry = "Boards", key = "cpuid", attribute = true, inline = true)
    HashMap<String, String> BoardNames;

    @ElementListUnion({
        @ElementList(entry = "gitlib", type = AxoGitLibrary.class, inline = true, required = false),
        @ElementList(entry = "filelib", type = AxoFileLibrary.class, inline = true, required = false)
    }
    )
    ArrayList<AxolotiLibrary> libraries;

    String[] ObjectPath;

    private boolean restartRequired = false;

    private final int nRecentFiles = 20;

    private final int minimumPollInterval = 20;

    private static final String ThemeList[] = {
        // "Arc",
        // "Arc - Orange",
        // "Arc Dark",
        // "Arc Dark - Orange",
        // "Carbon",
        // "Dark purple",
        // "Dracula",
        "FlatLaf Light",
        "FlatLaf Dark",
        // "FlatLaf Darcula",
        // "FlatLaf IntelliJ",
        "FlatLaf macOS Light",
        "FlatLaf macOS Dark",
        "Cobalt 2",
        "Cyan Light",
        // "Gradianto Dark Fuchsia",
        // "Gradianto Deep Ocean",
        // "Gradianto Midnight Blue",
        // "Gradianto Nature Green",
        // "Flat Gray",
        "Gruvbox Dark Hard",
        "Hiberbee Dark",
        // "High Contrast",
        "Monocai",
        // "Monokai Pro",
        "Nord",
        // "One Dark",
        // "Solarized Dark",
        // "Solarized Light",
        "Spacegray",
        // "Vuesion",
        "Xcode Dark",
        // "Arc Dark (Material)",
        "Atom One Light (Material)",
        "Atom One Dark (Material)",
        "Dracula (Material)",
        // "GitHub (Material)",
        // "GitHub Dark (Material)",
        "Light Owl (Material)",
        "Night Owl (Material)",
        "Material Lighter (Material)",
        "Material Design Dark",
        "Material Darker (Material)",
        "Material Deep Ocean (Material)",
        "Material Oceanic (Material)",
        "Material Palenight (Material)",
        // "Monokai Pro (Material)",
        // "Moonlight (Material)",
        "Solarized Light (Material)",
        "Solarized Dark (Material)"
    };

    public Preferences() {
        if (CurrentFileDirectory == null) {
            CurrentFileDirectory = "";
        }
        ObjectSearchPath = null;

        if (PollInterval == null) {
            PollInterval = 30;
        }
        if (MouseDialAngular == null) {
            MouseDialAngular = false;
        }
        if (FirmwareMode == null) {
            FirmwareMode = "Ksoloti Core";
        }
        for (int i=0; i<4; i++) {
            if (UserShortcuts[i] == null) {
                UserShortcuts[i] = "";
            }
        }
        if (MouseDoNotRecenterWhenAdjustingControls == null) {
            MouseDoNotRecenterWhenAdjustingControls = false;
        }
        if (ExpertMode == null) {
            ExpertMode = false;
        }
        if (SortByExecution == null) {
            SortByExecution = false;
        }
        if (FirmwareWarnDisable == null) {
            FirmwareWarnDisable = false;
        }
        if (FavouriteDir == null) {
            FavouriteDir = "";
        }
        if (BoardNames == null) {
            BoardNames = new HashMap<String, String>();
        }
        if (ControllerObject == null) {
            ControllerObject = "";
            ControllerEnabled = false;
        }
        if (BackupPatchesOnSDEnabled == null) {
            BackupPatchesOnSDEnabled = true;
        }
        if (Theme == null) {
            Theme = "FlatLaf Light";
        }
        if (codeSyntaxTheme == null) {
            codeSyntaxTheme = "monokai-kso";
        }
        if (libraries == null) {
            libraries = new ArrayList<AxolotiLibrary>();
        }
        if(DspSafetyLimit == null) {
            DspSafetyLimit = 3; // Normal setting
        }
    }

    /* Makes a deep copy of the source Preferences object into this object. */
    public Preferences clone() {
        Preferences clonedPrefs = new Preferences();
        /* Manually copy all primitive fields: */
        clonedPrefs.PollInterval = this.PollInterval;
        clonedPrefs.CodeFontSize = this.CodeFontSize;
        clonedPrefs.FavouriteDir = this.FavouriteDir;
        clonedPrefs.MouseDialAngular = this.MouseDialAngular;
        clonedPrefs.MouseDoNotRecenterWhenAdjustingControls = this.MouseDoNotRecenterWhenAdjustingControls;
        clonedPrefs.FirmwareMode = this.FirmwareMode;
        clonedPrefs.ControllerObject = this.ControllerObject;
        clonedPrefs.ControllerEnabled = this.ControllerEnabled;
        clonedPrefs.BackupPatchesOnSDEnabled = this.BackupPatchesOnSDEnabled;
        clonedPrefs.Theme = this.Theme;
        clonedPrefs.DspSafetyLimit = this.DspSafetyLimit;
        clonedPrefs.ExpertMode = this.ExpertMode;
        clonedPrefs.SortByExecution = this.SortByExecution;
        clonedPrefs.FirmwareWarnDisable = this.FirmwareWarnDisable;
        clonedPrefs.themePath = this.themePath;
        clonedPrefs.codeSyntaxTheme = this.codeSyntaxTheme;
        clonedPrefs.CurrentFileDirectory = this.CurrentFileDirectory;
        clonedPrefs.restartRequired = this.restartRequired;

        if (this.BoardNames != null) {
            clonedPrefs.BoardNames = (HashMap<String, String>) this.BoardNames.clone();
        }
        else {
            clonedPrefs.BoardNames = new HashMap<String, String>();
        }

        if (this.UserShortcuts != null) {
            clonedPrefs.UserShortcuts = new String[this.UserShortcuts.length];
            System.arraycopy(this.UserShortcuts, 0, clonedPrefs.UserShortcuts, 0, this.UserShortcuts.length);
        }
        else {
            clonedPrefs.UserShortcuts = new String[4];
        }

        if (this.ObjectPath != null) {
            clonedPrefs.ObjectPath = new String[this.ObjectPath.length];
            System.arraycopy(this.ObjectPath, 0, clonedPrefs.ObjectPath, 0, this.ObjectPath.length);
        }
        else {
            clonedPrefs.ObjectPath = new String[0];
        }

        if (this.recentFiles != null) {
            clonedPrefs.recentFiles = (ArrayList<String>) this.recentFiles.clone();
        }
        else {
            clonedPrefs.recentFiles = new ArrayList<String>();
        }

        /* Deep copy library entries */
        clonedPrefs.libraries = new ArrayList<>();
        if (this.libraries != null) {
            for (AxolotiLibrary lib : this.libraries) {
                if (lib instanceof AxoGitLibrary) {
                    clonedPrefs.libraries.add(new AxoGitLibrary((AxoGitLibrary) lib));
                } else if (lib instanceof AxoFileLibrary) {
                    clonedPrefs.libraries.add(new AxoFileLibrary((AxoFileLibrary) lib));
                } else {
                    /* Fallback */
                    clonedPrefs.libraries.add(new AxolotiLibrary(lib));
                }
            }
        }

        return clonedPrefs;
    }

    @Persist
    public void Persist() {
        // called prior to serialization
        appVersion = Version.AXOLOTI_SHORT_VERSION;
    }

    public ArrayList<AxolotiLibrary> getLibraries() {
        return libraries;
    }

    public AxolotiLibrary getLibrary(String id) {
        if (id == null || libraries == null) return null;
        for (AxolotiLibrary lib : libraries) {
            if (id.equals(lib.getId())) {
                return lib;
            }
        }
        return null;
    }

    public String[] getObjectSearchPath() {
        return ObjectPath;
    }

    public void updateLibrary(String id, AxolotiLibrary newlib) {
        boolean found = false;
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
                if (lib != newlib) {
                    int idx = libraries.indexOf(lib);
                    libraries.set(idx, newlib);
                }
                found = true;
                break;
            }
        }
        if (!found) {
            libraries.add(newlib);
        }
        buildObjectSearchPatch();
    }

    public void removeLibrary(String id) {
        /* Iterate using index to avoid ConcurrentModificationException */
        for (int i = 0; i < libraries.size(); i++) {
            if (libraries.get(i).getId().equals(id)) {
                libraries.remove(i);
                buildObjectSearchPatch();
                return;
            }
        }
    }

    public void enableLibrary(String id, boolean e) {
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
                lib.setEnabled(e);
                buildObjectSearchPatch();
                return; /* Found and updated, no need to continue */
            }
        }
    }

    public String getCurrentFileDirectory() {
        return CurrentFileDirectory;
    }

    public int getPollInterval() {
        if (PollInterval != null && PollInterval > minimumPollInterval) {
            return PollInterval;
        }
        return minimumPollInterval;
    }

    public void setPollInterval(int i) {
        if (i < minimumPollInterval) {
            i = minimumPollInterval;
        }
        if (this.PollInterval == null || !this.PollInterval.equals(i)) {
            PollInterval = i;
        }
    }

    public short getUiMidiThreadCost() {
        short costs[] = {0, 280, 150, 100, 80, 60};
        if (DspSafetyLimit != null && DspSafetyLimit >= 0 && DspSafetyLimit < costs.length) {
            return costs[DspSafetyLimit];
        }
        return costs[3]; /* Default if DspSafetyLimit is invalid */
    }


    public byte getDspLimitPercent() {
        if(DspSafetyLimit == null || DspSafetyLimit == 0) {
            return 97;
        } else {
            return 100;
        }
    }

    public int getDspSafetyLimit() {
        if (DspSafetyLimit == null) return 3; // Default
        return DspSafetyLimit;
    }

    public void setDspSafetyLimit(int i) {
        if (this.DspSafetyLimit == null || !this.DspSafetyLimit.equals(i)) {
            DspSafetyLimit = i;
        }
    }

    public void setCurrentFileDirectory(String CurrentFileDirectory) {
        if (CurrentFileDirectory != null && this.CurrentFileDirectory.equals(CurrentFileDirectory)) {
            return;
        }
        this.CurrentFileDirectory = CurrentFileDirectory;
        // SavePrefs(); /* Removed: Saving should be explicit from PreferencesFrame */
    }

    public String getTheme() {
        return Theme;
    }

    public void setTheme(String Theme) {
        if (Theme != null && Theme.equals(this.Theme)) {
            return;
        }
        this.Theme = Theme;
        restartRequired = true;
    }

    public String getCodeSyntaxTheme() {
        return codeSyntaxTheme;
    }

    public void setCodeSyntaxTheme(String codeSyntaxTheme) {
        if (codeSyntaxTheme != null && codeSyntaxTheme.equals(this.codeSyntaxTheme)) {
            return;
        }
        this.codeSyntaxTheme = codeSyntaxTheme;
        restartRequired = true;
    }

    public void applyTheme() {
        /* Ugly and inefficient Theme switching ... only doing this once during startup */
        if      (this.Theme.equals("FlatLaf IntelliJ"))               FlatIntelliJLaf.setup();
        else if (this.Theme.equals("FlatLaf Dark"))                   FlatDarculaLaf.setup();
        else if (this.Theme.equals("FlatLaf macOS Light"))            FlatMacLightLaf.setup();
        else if (this.Theme.equals("FlatLaf macOS Dark"))             FlatMacDarkLaf.setup();
        else if (this.Theme.equals("Cobalt 2"))                       FlatCobalt2IJTheme.setup();
        else if (this.Theme.equals("Cyan Light"))                     FlatCyanLightIJTheme.setup();
        else if (this.Theme.equals("Gruvbox Dark Hard"))              FlatGruvboxDarkHardIJTheme.setup();
        else if (this.Theme.equals("Hiberbee Dark"))                  FlatHiberbeeDarkIJTheme.setup();
        else if (this.Theme.equals("Monocai"))                        FlatMonocaiIJTheme.setup();
        else if (this.Theme.equals("Nord"))                           FlatNordIJTheme.setup();
        else if (this.Theme.equals("Spacegray"))                      FlatSpacegrayIJTheme.setup();
        else if (this.Theme.equals("Xcode Dark"))                     FlatXcodeDarkIJTheme.setup();
        else if (this.Theme.equals("Atom One Light (Material)"))      FlatMTAtomOneLightIJTheme.setup();
        else if (this.Theme.equals("Atom One Dark (Material)"))       FlatMTAtomOneDarkIJTheme.setup();
        else if (this.Theme.equals("Dracula (Material)"))             FlatDraculaIJTheme.setup();
        else if (this.Theme.equals("Light Owl (Material)"))           FlatMTLightOwlIJTheme.setup();
        else if (this.Theme.equals("Night Owl (Material)"))           FlatMTNightOwlIJTheme.setup();
        else if (this.Theme.equals("Material Lighter (Material)"))    FlatMTMaterialLighterIJTheme.setup();
        else if (this.Theme.equals("Material Design Dark"))           FlatMaterialDesignDarkIJTheme.setup();
        else if (this.Theme.equals("Material Darker (Material)"))     FlatMTMaterialDarkerIJTheme.setup();
        else if (this.Theme.equals("Material Deep Ocean (Material)")) FlatMTMaterialDeepOceanIJTheme.setup();
        else if (this.Theme.equals("Material Oceanic (Material)"))    FlatMTMaterialOceanicIJTheme.setup();
        else if (this.Theme.equals("Material Palenight (Material)"))  FlatMTMaterialPalenightIJTheme.setup();
        else if (this.Theme.equals("Solarized Dark (Material)"))      FlatSolarizedDarkIJTheme.setup();
        else if (this.Theme.equals("Solarized Light (Material)"))     FlatSolarizedLightIJTheme.setup();
        /* Falling through - default to FlatLaf IntelliJ */
        else                                                          FlatIntelliJLaf.setup();
    }

    public int getCodeFontSize() {
        return CodeFontSize;
    }

    public void setCodeFontSize(int CodeFontSize) {
        int sz = CodeFontSize < 4 ? 4 : CodeFontSize > 64 ? 64 : CodeFontSize;
        if (this.CodeFontSize == sz) {
            return;
        }
        this.CodeFontSize = sz;
    }

    static String GetPrefsFileLoc() {
        return System.getProperty(Axoloti.HOME_DIR) + File.separator + "ksoloti.prefs";
    }

    private static Preferences singleton;

    public static Preferences LoadPreferences() {
        File p = new File(Preferences.GetPrefsFileLoc());
        if (p.exists()) {
            Preferences prefs = null;
            Serializer serializer = new Persister(new Format(2));
            try {
                prefs = serializer.read(Preferences.class, p);
            } catch (Exception ex) {
                try {
                    LOGGER.log(Level.SEVERE, "Error trying to load preferences: " + ex.getMessage());
                    LOGGER.log(Level.INFO, "Attempting to load preferences in relaxed mode.");
                    prefs = serializer.read(Preferences.class, p,false);
                } catch (Exception ex1) {
                    LOGGER.log(Level.SEVERE, "Error trying to load preferences in relaxed mode: " + ex1.getMessage());
                    ex1.printStackTrace(System.out);
                }
            }
            if (prefs == null) {
                prefs = new Preferences();
            }
            singleton = prefs;

            if (singleton.libraries == null) {
                singleton.libraries = new ArrayList<>();
            }
            if (singleton.libraries.isEmpty()) {
                singleton.ResetLibraries(false);
            }

            singleton.buildObjectSearchPatch();
            singleton.MidiInputDevice = null; // clear it out for the future
        }
        else {
            singleton = new Preferences();
            singleton.ResetLibraries(false);
        }
        return singleton;
    }

    public static Preferences getInstance() {
        if (singleton == null) {
            return LoadPreferences();
        }
        return singleton;
    }

    public static void setInstance(Preferences prefs) {
        singleton = prefs;
    }

    public void SavePrefs() {
        Serializer serializer = new Persister(new Format(2));
        File f = new File(GetPrefsFileLoc());
        System.out.println(f.getAbsolutePath());

        try {
            serializer.write(this, f);
            singleton = null; /* Invalidate current instance */
            LoadPreferences(); /* Reload the freshly saved prefs file */
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to save preferences: " + ex.getMessage());
            ex.printStackTrace(System.out);
            return;
        }
        LOGGER.log(Level.INFO, "Saved preferences.\n");

        if (restartRequired) {
            LOGGER.log(Level.SEVERE, "\n>>> RESTART REQUIRED <<<\n");
        }
    }


    public String[] getThemeList() {
        return ThemeList;
    }

    @Deprecated
    public String getComPortName() {
        return ComPortName;
    }

    @Deprecated
    public void setComPortName(String ComPortName) {
    }

    public Boolean getMouseDialAngular() {
        return MouseDialAngular;
    }

    public String getFirmwareMode() {
        return FirmwareMode;
    }

    public String getUserShortcut(int index) {
        if (index < 0 || index > 3) {
            /* Only four user shortcuts so far */
            return null;
        }
        return UserShortcuts[index];
    }

    public void setMouseDialAngular(boolean MouseDialAngular) {
        if (this.MouseDialAngular != null && this.MouseDialAngular.equals(MouseDialAngular)) {
            return;
        }
        this.MouseDialAngular = MouseDialAngular;
    }

    public void setFirmwareMode(String FirmwareMode) {
        if (FirmwareMode == null || this.FirmwareMode.equals(FirmwareMode)) {
            return;
        }

        boolean boardChanged = (FirmwareMode.contains("Axoloti") && !this.FirmwareMode.contains("Axoloti")) ||
                               (FirmwareMode.contains("Ksoloti") && !this.FirmwareMode.contains("Ksoloti"));

        this.FirmwareMode = FirmwareMode;
        if (boardChanged) {
            restartRequired = true;
            mainframe.disableConnectUntilRestart();
        }
    }

    public boolean getRestartRequired() {
        return restartRequired;
    }

    public void setUserShortcut(int index, String userShortcut) {
        if (index < 0 || index > 3) {
            return;
        }
        if (this.UserShortcuts[index] != null && this.UserShortcuts[index].equals(userShortcut)) {
            return;
        }
        this.UserShortcuts[index] = userShortcut;
    }

    public boolean getMouseDoNotRecenterWhenAdjustingControls() {
        return MouseDoNotRecenterWhenAdjustingControls;
    }

    public void setMouseDoNotRecenterWhenAdjustingControls(boolean MouseDoNotRecenterWhenAdjustingControls) {
        if (this.MouseDoNotRecenterWhenAdjustingControls != null && this.MouseDoNotRecenterWhenAdjustingControls.equals(MouseDoNotRecenterWhenAdjustingControls)) {
            return;
        }
        this.MouseDoNotRecenterWhenAdjustingControls = MouseDoNotRecenterWhenAdjustingControls;
    }

    public Boolean getExpertMode() {
        return ExpertMode;
    }

    public Boolean getSortByExecution() {
        return SortByExecution;
    }

    public Boolean getFirmwareWarnDisable() {
        return FirmwareWarnDisable;
    }

    public ArrayList<String> getRecentFiles() {
        return recentFiles;
    }

    public void addRecentFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) return;
        recentFiles.remove(filename); 
        while (recentFiles.size() >= nRecentFiles) {
            recentFiles.remove(0); /* Remove oldest */
        }

        recentFiles.add(filename); /* Add to top */
    }

    public void removeRecentFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) return;
        recentFiles.remove(filename);
    }

    public String getFavouriteDir() {
        return FavouriteDir;
    }

    public void setFavouriteDir(String favouriteDir) {
        if (favouriteDir != null && this.FavouriteDir.equals(favouriteDir)) {
            return;
        }
        this.FavouriteDir = favouriteDir;
        restartRequired = true;
    }

    public String getBoardName(String cpu) {
        if (cpu == null || cpu.trim().isEmpty() || BoardNames == null) {
            return null;
        }
        if (BoardNames.containsKey(cpu)) {
            return BoardNames.get(cpu);
        }
        return null;
    }

    public void setBoardName(String cpuid, String name) {
        if (BoardNames == null) {
            BoardNames = new HashMap<>();
        }
        if (name == null || name.trim().isEmpty()) {
            BoardNames.remove(cpuid);
        } else {
            BoardNames.put(cpuid, name);
        }
    }

    public String getControllerObject() {
        return ControllerObject;
    }

    public void setControllerObject(String s) {
        ControllerObject = s.strip();
    }

    public void setControllerEnabled(boolean b) {
        ControllerEnabled = b;
    }

    public boolean isControllerEnabled() {
        return ControllerEnabled;
    }

    public void setBackupPatchesOnSDEnabled(boolean b) {
        BackupPatchesOnSDEnabled = b;
    }

    public boolean isBackupPatchesOnSDEnabled() {
        return BackupPatchesOnSDEnabled;
    }

    public final void ResetLibraries(boolean delete) {

        /* Create default configs */
        AxoGitLibrary axo_fact = new AxoGitLibrary(
                AxolotiLibrary.AXOLOTI_FACTORY_ID,
                "git",
                System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + AxolotiLibrary.AXOLOTI_FACTORY_ID,
                true,
                "https://github.com/ksoloti/axoloti-factory.git", /* Notice we're now pointing to the forked&fixed /KSOLOTI/axoloti-factory! */
                false
        );

        AxoGitLibrary axo_comm = new AxoGitLibrary(
                AxolotiLibrary.AXOLOTI_CONTRIB_ID,
                "git",
                System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + AxolotiLibrary.AXOLOTI_CONTRIB_ID,
                true,
                "https://github.com/ksoloti/axoloti-contrib.git", /* Notice we're now pointing to the forked&fixed /KSOLOTI/axoloti-contrib! */
                false
        );

        AxoGitLibrary kso_fact = new AxoGitLibrary(
                AxolotiLibrary.KSOLOTI_OBJECTS_ID,
                "git",
                System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + AxolotiLibrary.KSOLOTI_OBJECTS_ID,
                true,
                "https://github.com/ksoloti/ksoloti-objects.git",
                false
        );

        AxoGitLibrary kso_comm = new AxoGitLibrary(
                AxolotiLibrary.KSOLOTI_CONTRIB_ID,
                "git",
                System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + AxolotiLibrary.KSOLOTI_CONTRIB_ID,
                true,
                "https://github.com/ksoloti/ksoloti-contrib.git",
                false
        );

        if (libraries == null) {
            libraries = new ArrayList<>();
        } else {
            libraries.clear();
        }

        /*
         * Check and remove old libraries if they exist.
         * Add back respective default config library, then initialize it.
         */
        libraries = getLibraries();
        if (getLibrary(AxoGitLibrary.AXOLOTI_FACTORY_ID) != null) {
            libraries.remove(getLibrary(AxoGitLibrary.AXOLOTI_FACTORY_ID));
        }
        libraries.add(axo_fact);
        if (axo_fact.getEnabled()) {
            axo_fact.init(delete);
        }

        if (getLibrary(AxoGitLibrary.AXOLOTI_CONTRIB_ID) != null) {
            libraries.remove(getLibrary(AxoGitLibrary.AXOLOTI_CONTRIB_ID));
        }
        libraries.add(axo_comm);
        if (axo_comm.getEnabled()) {
            axo_comm.init(delete);
        }

        if (getLibrary(AxoGitLibrary.KSOLOTI_OBJECTS_ID) != null) {
            libraries.remove(getLibrary(AxoGitLibrary.KSOLOTI_OBJECTS_ID));
        }
        libraries.add(kso_fact);
        if (kso_fact.getEnabled()) {
            kso_fact.init(delete);
        }

        if (getLibrary(AxoGitLibrary.KSOLOTI_CONTRIB_ID) != null) {
            libraries.remove(getLibrary(AxoGitLibrary.KSOLOTI_CONTRIB_ID));
        }
        libraries.add(kso_comm);
        if (kso_comm.getEnabled()) {
            kso_comm.init(delete);
        }

        buildObjectSearchPatch();
    }

    private void buildObjectSearchPatch() {
        ArrayList<String> objPath = new ArrayList<String>();

        if (libraries != null) {
            for (AxolotiLibrary lib : libraries) {
                if (lib.getEnabled()) {
                    String lpath = lib.getLocalLocation() + File.separator + "objects";

                    /* Might be two libs pointing to same place */
                    if (!objPath.contains(lpath)) {
                        objPath.add(lpath);
                    }
                }
            }
        }
        ObjectPath = objPath.toArray(new String[0]);
    }

    public String getThemePath() {
        return themePath;
    }

    public void setThemePath(String themePath) {
        if (themePath != null && this.themePath.equals(themePath)) {
            return;
        }
        this.themePath = themePath;
        // SavePrefs(); /* Removed: Saving should be explicit from PreferencesFrame */
    }
}
