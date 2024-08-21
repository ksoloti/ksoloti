package axoloti.utils;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AxoFileLibrary extends AxolotiLibrary {

    private static final Logger LOGGER = Logger.getLogger(AxoFileLibrary.class.getName());

    public static String TYPE="local";
    
    public AxoFileLibrary() {
        super();
    }

    public AxoFileLibrary(String id, String type, String lloc, boolean e) {
        super(id, type, lloc, e, null, false);
    }

    @Override
    public void sync() {
        // NOP
    }
    
    public void reportStatus() {
        File f = new File(getLocalLocation()); 
        if(!f.exists()) {
           LOGGER.log(Level.WARNING, "Library status: {0} - Local directory missing", getId());
        }
        LOGGER.log(Level.INFO, "Library status: {0} (local) - OK", getId());
    }
    
    @Override
    public void init(boolean delete) {
        // NOP 
        // would be dangerous to delete local files
        // we should assume they are not backed up

        File ldir = new File(getLocalLocation());
        if (!ldir.exists()) {
            ldir.mkdirs();
        }

        // default directory structure
        File odir = new File(ldir, "objects");
        if (!odir.exists()) {
            odir.mkdirs();
        }
        File pdir = new File(ldir, "patches");
        if (!pdir.exists()) {
            pdir.mkdirs();
        }
    }
}
