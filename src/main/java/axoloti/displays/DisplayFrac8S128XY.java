package axoloti.displays;

import java.security.MessageDigest;

import axoloti.datatypes.DataType;
import axoloti.datatypes.Int8Ptr;

public class DisplayFrac8S128XY extends Display {

    public DisplayFrac8S128XY() {
    }

    public DisplayFrac8S128XY(String name) {
        super(name);
    }

    @Override
    public DisplayInstanceFrac8S128XY InstanceFactory() {
        return new DisplayInstanceFrac8S128XY();
    }

    @Override
    public void updateSHA(MessageDigest md) {
        super.updateSHA(md);
        md.update("frac8.s.128.xy".getBytes()); 
    }

    @Override
    public DataType getDatatype() {
        return Int8Ptr.d; 
    }

    static public final String TypeName = "int8array128.xy";

    @Override
    public String getTypeName() {
        return TypeName;
    }
    
    @Override
    public int getLength() {
        return 256 / 4; 
    }
}