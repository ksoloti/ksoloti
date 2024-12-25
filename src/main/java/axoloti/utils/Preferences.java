/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
package axoloti.utils;

import axoloti.MainFrame;
import axoloti.USBBulkConnection;
import axoloti.Version;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.*;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.Persister;

import com.formdev.flatlaf.FlatDarculaLaf;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkSoftIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatNightOwlIJTheme;
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

//    @Path("outlets")
    @ElementListUnion({
        @ElementList(entry = "gitlib", type = AxoGitLibrary.class, inline = true, required = false),
        @ElementList(entry = "filelib", type = AxoFileLibrary.class, inline = true, required = false)
    }
    )
    ArrayList<AxolotiLibrary> libraries;

    String[] ObjectPath;

    boolean isDirty = false;
    private boolean restartRequired = false;

    private final int nRecentFiles = 16;

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
        "Gruvbox Dark Soft",
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


    protected Preferences() {
        if (CurrentFileDirectory == null) {
            CurrentFileDirectory = "";
        }
        ObjectSearchPath = null;

        if (PollInterval == null) {
            PollInterval = 50;
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

    @Persist
    public void Persist() {
        // called prior to serialization
        appVersion = Version.AXOLOTI_SHORT_VERSION;
    }

    void SetDirty() {
        isDirty = true;
    }

    void ClearDirty() {
        isDirty = false;
    }

    public ArrayList<AxolotiLibrary> getLibraries() {
        return libraries;
    }

    public AxolotiLibrary getLibrary(String id) {
        if(libraries == null) return null;
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
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
            }
        }
        if (!found) {
            libraries.add(newlib);
        }
        buildObjectSearchPatch();
        SetDirty();
    }

    public void removeLibrary(String id) {
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
                libraries.remove(lib);
                return;
            }
        }
        SetDirty();
        buildObjectSearchPatch();
    }

    public void enableLibrary(String id, boolean e) {
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
                lib.setEnabled(e);
            }
        }
        SetDirty();
        buildObjectSearchPatch();
    }

    public String getCurrentFileDirectory() {
        return CurrentFileDirectory;
    }

    public int getPollInterval() {
        if (PollInterval > minimumPollInterval) {
            return PollInterval;
        }
        return minimumPollInterval;
    }

    public void setPollInterval(int i) {
        if (i < minimumPollInterval) {
            i = minimumPollInterval;
        }
        PollInterval = i;
        SetDirty();
    }

    public short getUiMidiThreadCost() {
        short costs[] = {0, 280, 150, 100, 80, 60};
        return costs[DspSafetyLimit];
    }


    public byte getDspLimitPercent() {
        if(DspSafetyLimit == 0) {
            return 97;
        } else {
            return 100;
        }
    }

    public int getDspSafetyLimit() {
        return DspSafetyLimit;
    }

    public void setDspSafetyLimit(int i) {
        DspSafetyLimit = i;
        SetDirty();
    }

    public void setCurrentFileDirectory(String CurrentFileDirectory) {
        if (this.CurrentFileDirectory.equals(CurrentFileDirectory)) {
            return;
        }
        this.CurrentFileDirectory = CurrentFileDirectory;
        SavePrefs();
        SetDirty();
    }

    public String getTheme() {
        return Theme;
    }

    public void setTheme(String Theme) {
        if (Theme.equals(this.Theme)) {
            return;
        }
        this.Theme = Theme;
        restartRequired = true;
        SetDirty();
    }

    public String getCodeSyntaxTheme() {
        return codeSyntaxTheme;
    }

    public void setCodeSyntaxTheme(String codeSyntaxTheme) {
        if (codeSyntaxTheme.equals(this.codeSyntaxTheme)) {
            return;
        }
        this.codeSyntaxTheme = codeSyntaxTheme;
        restartRequired = true;
        SetDirty();
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
        else if (this.Theme.equals("Gruvbox Dark Soft"))              FlatGruvboxDarkSoftIJTheme.setup();
        else if (this.Theme.equals("Hiberbee Dark"))                  FlatHiberbeeDarkIJTheme.setup();
        else if (this.Theme.equals("Light Owl (Material)"))           FlatLightOwlIJTheme.setup();
        else if (this.Theme.equals("Night Owl (Material)"))           FlatNightOwlIJTheme.setup();
        else if (this.Theme.equals("Monocai"))                        FlatMonocaiIJTheme.setup();
        else if (this.Theme.equals("Nord"))                           FlatNordIJTheme.setup();
        else if (this.Theme.equals("Spacegray"))                      FlatSpacegrayIJTheme.setup();
        else if (this.Theme.equals("Xcode Dark"))                     FlatXcodeDarkIJTheme.setup();
        else if (this.Theme.equals("Atom One Light (Material)"))      FlatAtomOneLightIJTheme.setup();
        else if (this.Theme.equals("Atom One Dark (Material)"))       FlatAtomOneDarkIJTheme.setup();
        else if (this.Theme.equals("Dracula (Material)"))             FlatDraculaIJTheme.setup();
        else if (this.Theme.equals("Material Lighter (Material)"))    FlatMaterialLighterIJTheme.setup();
        else if (this.Theme.equals("Material Design Dark"))           FlatMaterialDesignDarkIJTheme.setup();
        else if (this.Theme.equals("Material Darker (Material)"))     FlatMaterialDarkerIJTheme.setup();
        else if (this.Theme.equals("Material Deep Ocean (Material)")) FlatMaterialDeepOceanIJTheme.setup();
        else if (this.Theme.equals("Material Oceanic (Material)"))    FlatMaterialOceanicIJTheme.setup();
        else if (this.Theme.equals("Material Palenight (Material)"))  FlatMaterialPalenightIJTheme.setup();
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
        SetDirty();
    }

    static String GetPrefsFileLoc() {
        return System.getProperty(axoloti.Axoloti.HOME_DIR) + File.separator + "ksoloti.prefs";
    }

    private static Preferences singleton;

    public static Preferences LoadPreferences() {
        if (singleton == null) {
            File p = new File(Preferences.GetPrefsFileLoc());
            if (p.exists()) {
                Preferences prefs = null;
                Serializer serializer = new Persister();
                try {
                    prefs = serializer.read(Preferences.class, p);
                } catch (Exception ex) {
                    try {
                        LOGGER.log(Level.SEVERE, null, ex);
                        LOGGER.log(Level.INFO,"Attempting to load preferences in relaxed mode");
                        prefs = serializer.read(Preferences.class, p,false);
                    } catch (Exception ex1) {
                        LOGGER.log(Level.SEVERE, null, ex1);
                    }
                }
                if (prefs == null){
                    prefs = new Preferences();
                }
                singleton = prefs;

                if (prefs.libraries.isEmpty()) {
                    prefs.ResetLibraries(false);
                }

                prefs.buildObjectSearchPatch();

                singleton.MidiInputDevice = null; // clear it out for the future
            }
            else {
                singleton = new Preferences();
                singleton.ResetLibraries(false);
            }
        }
        return singleton;
    }

    public void SavePrefs() {

        LOGGER.log(Level.INFO, "Saving preferences...");

        Serializer serializer = new Persister();
        File f = new File(GetPrefsFileLoc());
        System.out.println(f.getAbsolutePath());

        try {
            serializer.write(this, f);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        if (restartRequired) {
            MainFrame.mainframe.disableConnectUntilRestart();
            LOGGER.log(Level.SEVERE, ">>> RESTART REQUIRED <<<");
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
        if (this.MouseDialAngular == MouseDialAngular) {
            return;
        }
        this.MouseDialAngular = MouseDialAngular;
        SetDirty();
    }

    public void setFirmwareMode(String FirmwareMode) {

        if (this.FirmwareMode.equals(FirmwareMode)) {
            return;
        }

        if (FirmwareMode.contains("Axoloti") && !this.FirmwareMode.contains("Axoloti") || FirmwareMode.contains("Ksoloti") && !this.FirmwareMode.contains("Ksoloti")) {
            /* If switching to another board model... */
            
            this.FirmwareMode = FirmwareMode;
            MainFrame.mainframe.updateMainframeTitle();
            MainFrame.mainframe.refreshAppIcon();

            restartRequired = true;
            
            /* Disconnect automatically. User will have to restart the Patcher anyway. */
            if (USBBulkConnection.GetConnection().isConnected()) {
                USBBulkConnection.GetConnection().disconnect();
            }
        }
        else {

            this.FirmwareMode = FirmwareMode;
            MainFrame.mainframe.updateMainframeTitle();

            /* If connected, offer automatic firmware update */
            if (USBBulkConnection.GetConnection().isConnected()) {
                MainFrame.mainframe.interactiveFirmwareUpdate();
            }
        }

        SetDirty();
    }

    public void setUserShortcut(int index, String userShortcut) {
        if (index < 0 || index > 3) {
            return;
        }
        if (this.UserShortcuts[index] == userShortcut) {
            return;
        }
        this.UserShortcuts[index] = userShortcut;
        SetDirty();
    }

    public boolean getMouseDoNotRecenterWhenAdjustingControls() {
        return MouseDoNotRecenterWhenAdjustingControls;
    }

    public void setMouseDoNotRecenterWhenAdjustingControls(boolean MouseDoNotRecenterWhenAdjustingControls) {
        if (MouseDoNotRecenterWhenAdjustingControls == this.MouseDoNotRecenterWhenAdjustingControls) {
            return;
        }
        this.MouseDoNotRecenterWhenAdjustingControls = MouseDoNotRecenterWhenAdjustingControls;
        SetDirty();
    }

    public Boolean getExpertMode() {
        return ExpertMode;
    }

    public ArrayList<String> getRecentFiles() {
        return recentFiles;
    }

    public void addRecentFile(String filename) {
        boolean alreadyInMenu = false;
        for (String r : recentFiles) {
            if (r.equals(filename)) {
                /* Schedule to remove from current position, will be added to top */
                /* Can't remove while iterating - will trigger ConcurrentModificationException */
                alreadyInMenu = true;
            }
        }
        if (alreadyInMenu) {
            recentFiles.remove(filename);
        }
        else if (recentFiles.size() == nRecentFiles) {
            recentFiles.remove(0);
        }
        /* Add to top */
        recentFiles.add(filename);
        SetDirty();
    }

    public void removeRecentFile(String filename) {
        boolean alreadyInMenu = false;
        for (String r : recentFiles) {
            if (r.equals(filename)) {
                /* Schedule to remove from current position, will be added to top */
                /* Can't remove while iterating - will trigger ConcurrentModificationException */
                alreadyInMenu = true;
            }
        }
        if (alreadyInMenu) {
            recentFiles.remove(filename);
        }
    }

    public String getFavouriteDir() {
        return FavouriteDir;
    }

    public void setFavouriteDir(String favouriteDir) {
        if (this.FavouriteDir.equals(favouriteDir)) {
            return;
        }
        this.FavouriteDir = favouriteDir;
        restartRequired = true;
        SetDirty();
    }

    public String getBoardName(String cpu) {
        if (cpu == null) {
            return null;
        }
        if (BoardNames.containsKey(cpu)) {
            return BoardNames.get(cpu);
        }
        return null;
    }

    public void setBoardName(String cpuid, String name) {
        if (name == null) {
            BoardNames.remove(cpuid);
        } else {
            BoardNames.put(cpuid, name);
        }
        SetDirty();
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

    public final void ResetLibraries(boolean delete) {

        /* Create default configs */
        AxoGitLibrary axo_fact = new AxoGitLibrary(
                AxolotiLibrary.FACTORY_ID,
                "git",
                System.getProperty(axoloti.Axoloti.LIBRARIES_DIR) + File.separator
                    + "axoloti-factory" + File.separator,
                true,
                "https://github.com/ksoloti/axoloti-factory.git", /* Notice we're now pointing to the forked&fixed /KSOLOTI/axoloti-factory! */
                false
        );

        AxoGitLibrary axo_comm = new AxoGitLibrary(
                AxolotiLibrary.USER_LIBRARY_ID,
                "git",
                System.getProperty(axoloti.Axoloti.LIBRARIES_DIR) + File.separator
                    + "axoloti-contrib" + File.separator,
                true,
                "https://github.com/ksoloti/axoloti-contrib.git", /* Will point to /KSOLOTI/axoloti-contrib in the future */
                false
        );

        AxoGitLibrary kso_fact = new AxoGitLibrary(
                AxolotiLibrary.KSOLOTI_LIBRARY_ID,
                "git",
                System.getProperty(axoloti.Axoloti.LIBRARIES_DIR) + File.separator
                    + "ksoloti-objects" + File.separator,
                true,
                "https://github.com/ksoloti/ksoloti-objects.git", /* Should be merged into ksoloti-contrib? */
                false
        );

        AxoGitLibrary kso_comm = new AxoGitLibrary(
                AxolotiLibrary.KSOLOTI_CONTRIB_LIBRARY_ID,
                "git",
                System.getProperty(axoloti.Axoloti.LIBRARIES_DIR) + File.separator
                    + "ksoloti-contrib" + File.separator,
                true,
                "https://github.com/ksoloti/ksoloti-contrib.git",
                false
        );

        /*
         * Check and remove old libraries if they exist.
         * Add back respective default config library, then initialize it.
         */
        libraries = getLibraries();
        if (getLibrary(AxoGitLibrary.FACTORY_ID) != null) {
            libraries.remove(getLibrary(AxoGitLibrary.FACTORY_ID));
        }
        libraries.add(axo_fact);
        if (axo_fact.getEnabled()) {
            axo_fact.init(delete);
        }

        if (getLibrary(AxoGitLibrary.USER_LIBRARY_ID) != null) {
            libraries.remove(getLibrary(AxoGitLibrary.USER_LIBRARY_ID));
        }
        libraries.add(axo_comm);
        if (axo_comm.getEnabled()) {
            axo_comm.init(delete);
        }

        if (getLibrary(AxoGitLibrary.KSOLOTI_LIBRARY_ID) != null) {
            libraries.remove(getLibrary(AxoGitLibrary.KSOLOTI_LIBRARY_ID));
        }
        libraries.add(kso_fact);
        if (kso_fact.getEnabled()) {
            kso_fact.init(delete);
        }

        if (getLibrary(AxoGitLibrary.KSOLOTI_CONTRIB_LIBRARY_ID) != null) {
            libraries.remove(getLibrary(AxoGitLibrary.KSOLOTI_CONTRIB_LIBRARY_ID));
        }
        libraries.add(kso_comm);
        if (kso_comm.getEnabled()) {
            kso_comm.init(delete);
        }

        buildObjectSearchPatch();
    }

    private void buildObjectSearchPatch() {
        ArrayList<String> objPath = new ArrayList<String>();

        for (AxolotiLibrary lib : libraries) {
            if (lib.getEnabled()) {
                String lpath = lib.getLocalLocation() + "objects";

                //might be two libs pointing to same place
                if (!objPath.contains(lpath)) {
                    objPath.add(lpath);
                }
            }
        }
        ObjectPath = objPath.toArray(new String[0]);
    }
    
    public String getThemePath() {
        return themePath;
    }
    
    public void setThemePath(String themePath) {
        this.themePath = themePath;
        SavePrefs();
    }
}
