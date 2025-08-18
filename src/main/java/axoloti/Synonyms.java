package axoloti;


import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Preferences;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

public class Synonyms {

    private static final Logger LOGGER = Logger.getLogger(Synonyms.class.getName());

    public static synchronized Synonyms instance() {
        if (instance == null) {
//            instance = new Synonyms();
//            instance.inlet("pitchm", "pitch");
//            instance.outlet("out", "o");
//            save();
            load();
        }
        return instance;
    }

    public String inlet(String a) {
        return inlets.get(a);
    }

    public void inlet(String a, String b) {
        inlets.put(a,b);
    }

    public String outlet(String a) {
        return outlets.get(a);
    }

    public void outlet(String a, String b) {
        outlets.put(a, b);
    }

    static void load() {
        Serializer serializer = new Persister(new Format(2));
        try {
            AxolotiLibrary lib = Preferences.getInstance().getLibrary(AxolotiLibrary.AXOLOTI_FACTORY_ID);
            if(lib != null) {
                instance = serializer.read(Synonyms.class, new File(lib.getLocalLocation(), filename));
            } else {
                LOGGER.log(Level.WARNING,"Not loading synonyms: cannot find factory library");
            }

            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during synonyms load: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    static void save() {
        Serializer serializer = new Persister(new Format(2));
        try {
            serializer.write(instance, new File(filename));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during synonyms save: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    static Synonyms instance = null;
    static String filename = "objects/synonyms.xml";

    protected Synonyms() {
        inlets = new HashMap<String, String>();
        outlets= new HashMap<String, String>();
    }

    @ElementMap(entry = "inlet", key = "a", value = "b", attribute=true ,inline = false)
    HashMap<String, String> inlets;
    @ElementMap(entry = "outlet", key = "a", value = "b", attribute=true ,inline = false)
    HashMap<String, String> outlets;
}
