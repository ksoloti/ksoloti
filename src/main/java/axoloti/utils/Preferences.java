/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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

// import axoloti.Axoloti;
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
    Boolean AxolotiLegacyMode;
    @Element(required = false)
    Boolean MouseDoNotRecenterWhenAdjustingControls;
    @Element(required = false)
    Boolean ExpertMode;
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
    int CodeFontSize = 14; /* default to 14pt */
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

    final int nRecentFiles = 16;

    final int minimumPollInterval = 20;

    public static final String THEMELIST[] = {
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
        if (AxolotiLegacyMode == null) {
            AxolotiLegacyMode = false;
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
        if (libraries == null) {
            libraries = new ArrayList<AxolotiLibrary>();
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
                        Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
                        Logger.getLogger(Preferences.class.getName()).log(Level.INFO,"Attempting to load preferences in relaxed mode");
                        prefs = serializer.read(Preferences.class, p,false);
                    } catch (Exception ex1) {
                        Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex1);
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
        Logger.getLogger(Preferences.class .getName()).log(Level.INFO, "Saving preferences...");
        if (restartRequired) {
            Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, ">>> RESTART REQUIRED <<<");
        }
        Serializer serializer = new Persister();
        File f = new File(GetPrefsFileLoc());

        System.out.println(f.getAbsolutePath());

        try {
            serializer.write(this, f);
        }
        catch (Exception ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
        }
        ClearDirty();
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

    public Boolean getAxolotiLegacyMode() {
        return AxolotiLegacyMode;
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

    public void setAxolotiLegacyMode(boolean AxolotiLegacyMode) {
        if (this.AxolotiLegacyMode == AxolotiLegacyMode) {
            return;
        }
        this.AxolotiLegacyMode = AxolotiLegacyMode;
        restartRequired = true;
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
        ControllerObject = s;
    }

    public void setControllerEnabled(boolean b) {
        ControllerEnabled = b;
    }

    public boolean isControllerEnabled() {
        return ControllerEnabled;
    }

    public final void ResetLibraries(boolean delete) {
        libraries = new ArrayList<AxolotiLibrary>();

        AxoGitLibrary factory = new AxoGitLibrary(
                AxolotiLibrary.FACTORY_ID,
                "git",
                System.getProperty(axoloti.Axoloti.LIBRARIES_DIR) + File.separator
                    + Version.AXOLOTI_SHORT_VERSION + File.separator
                    + "axoloti-factory" + File.separator,
                true,
                "https://github.com/axoloti/axoloti-factory.git",
                false
        );
        libraries.add(factory);

        libraries.add(new AxoGitLibrary(
                AxolotiLibrary.USER_LIBRARY_ID,
                "git",
                System.getProperty(axoloti.Axoloti.LIBRARIES_DIR) + File.separator
                    + Version.AXOLOTI_SHORT_VERSION + File.separator
                    + "axoloti-contrib" + File.separator,
                true,
                "https://github.com/axoloti/axoloti-contrib.git",
                false
        ));

        libraries.add(new AxoGitLibrary(
                AxolotiLibrary.KSOLOTI_LIBRARY_ID,
                "git",
                System.getProperty(axoloti.Axoloti.LIBRARIES_DIR) + File.separator
                    + Version.AXOLOTI_SHORT_VERSION + File.separator
                    + "ksoloti-objects" + File.separator,
                true,
                "https://github.com/ksoloti/ksoloti-objects.git",
                false
        ));

        // initialise the libraries
        for (AxolotiLibrary lib : libraries) {
            if (lib.getEnabled()) {
                lib.init(delete);
            }
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
