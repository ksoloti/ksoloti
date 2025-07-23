package axoloti.utils;

import axoloti.Axoloti;
import axoloti.Version;

import static axoloti.MainFrame.prefs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Represents a location for objects and patches to be picked up
 */
// this will become abstract
// (currently this is used to hold the data, so that the library editor is is typeless)
@Root(name = "library")
public class AxolotiLibrary {

    @Element(required = true)
    private String Id;
    @Element(required = true)
    private String Type;
    @Element(required = true)
    private String LocalLocation;
    @Element(required = true)
    private Boolean Enabled;

    // these are only requird for remote libraries
    @Element(required = false)
    private String RemoteLocation;
    @Element(required = false)
    private String UserId;
    @Element(required = false)
    private char[] Password;
    @Element(required = false)
    private boolean AutoSync;
    @Element(required = false)
    private String Revision;
    @Element(required = false)
    private String ContributorPrefix;

    public static String FACTORY_ID = "axoloti-factory";
    public static String USER_LIBRARY_ID = "axoloti-contrib";
    public static String KSOLOTI_LIBRARY_ID = "ksoloti-objects";
    public static String KSOLOTI_CONTRIB_LIBRARY_ID = "ksoloti-contrib";

    public AxolotiLibrary() {
        Id = "";
        Type = "local";
        Enabled = true;
        LocalLocation = "";
        RemoteLocation = "";
        UserId = "";
        AutoSync = false;
        Revision = "";
        ContributorPrefix = "";
    }

    public AxolotiLibrary(String id, String type, String lloc, boolean e, String rloc, boolean auto) {
        Id = id;
        Type = type;
        LocalLocation = lloc;
        Enabled = e;
        RemoteLocation = rloc;
        UserId = "";
        AutoSync = auto;
    }

    public void clone(AxolotiLibrary lib) {
        Id = lib.Id;
        Type = lib.Type;
        Enabled = lib.Enabled;
        LocalLocation = lib.LocalLocation;
        RemoteLocation = lib.RemoteLocation;
        UserId = lib.UserId;
        if (lib.Password != null) { /* Deep copy password */
            this.Password = new char[lib.Password.length];
            System.arraycopy(lib.Password, 0, this.Password, 0, lib.Password.length);
        }
        else {
            this.Password = null;
        }
        AutoSync = lib.AutoSync;
        Revision = lib.Revision;
        ContributorPrefix = lib.ContributorPrefix;
    }

    public boolean isReadOnly() {
        return (Id.equals(FACTORY_ID) || Id.equals(KSOLOTI_LIBRARY_ID)) && !(Axoloti.isDeveloper() || prefs.getExpertMode());
    }

    public void setId(String Id) {
        this.Id = Id;
    }

    public void setType(String Type) {
        this.Type = Type;
    }

    public void setLocalLocation(String LocalLocation) {
        this.LocalLocation = LocalLocation;
    }

    public void setEnabled(Boolean Enabled) {
        this.Enabled = Enabled;
    }

    public void setRemoteLocation(String RemoteLocation) {
        this.RemoteLocation = RemoteLocation;
    }

    public String getId() {
        return Id;
    }

    public String getType() {
        return Type;
    }

    public String getLocalLocation() {
        return LocalLocation;
    }

    public Boolean getEnabled() {
        return Enabled;
    }

    public String getRemoteLocation() {
        return RemoteLocation;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String UserId) {
        this.UserId = UserId;
    }

    public char[] getPassword() {
        /* For best safety, consumer of this char[] should clear it
           by filling it with zeros/spaces as soon as it's no longer needed. */
        return Password;
    }

    public void setPassword(char[] password) {
        /* For best safety, the caller of setPassword(char[] password)
           should immediately clear their source 'char[]' array. */
        if (password != null) {
            this.Password = new char[password.length];
            System.arraycopy(password, 0, this.Password, 0, password.length);
        } else {
            if (this.Password != null) {
                java.util.Arrays.fill(this.Password, '\0'); /* Clear the old password */
            }
            this.Password = null;
        }
    }

    public void clearPassword() {
        if (this.Password != null) {
            java.util.Arrays.fill(this.Password, '\0');
            this.Password = null;
        }
    }

    public boolean isAutoSync() {
        return AutoSync;
    }

    public void setAutoSync(boolean AutoSync) {
        this.AutoSync = AutoSync;
    }

    // interface to libraries
    public void sync() {
    }

    public void init(boolean delete) {
    }

    public void reportStatus() {
    }

    protected void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        if (!f.delete()) {
            throw new FileNotFoundException("Failed to delete file: " + f);
        }
    }

    public String getBranch() {
        String branch = getRevision();
        if (branch == null || branch.length() == 0) {
            boolean isOfficial = getId().equals(FACTORY_ID) || 
                                getId().equals(USER_LIBRARY_ID) || 
                                getId().equals(KSOLOTI_LIBRARY_ID) ||
                                getId().equals(KSOLOTI_CONTRIB_LIBRARY_ID);
            if (isOfficial) {
                branch = Version.AXOLOTI_SHORT_VERSION;
            }
            else {
                branch = "master";
            }
        }
        return branch;
    }

    public String getCurrentBranch() {
        return getBranch();
    }

    public String getRevision() {
        return Revision;
    }

    public void setRevision(String Revision) {
        this.Revision = Revision;
    }

    public String getContributorPrefix() {
        return ContributorPrefix;
    }

    public void setContributorPrefix(String ContributorPrefix) {
        this.ContributorPrefix = ContributorPrefix;
    }

    public boolean stashChanges(String ref) {
        return true;
    }

    public boolean applyStashedChanges(String ref) {
        return true;
    }

    public void upgrade() {
    }
}
