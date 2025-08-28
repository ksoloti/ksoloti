package axoloti.datatypes;

import java.awt.Color;

import axoloti.ui.Theme;

/**
 *
 * @author Johannes Taelman
 */
public class DTZombie implements DataType {

    @Override
    public boolean IsConvertableToType(DataType dest) {
        return false;
    }

    @Override
    public boolean HasDefaultValue() {
        return false;
    }

    @Override
    public String GenerateSetDefaultValueCode() {
        return "";
    }

    @Override
    public String GenerateConversionToType(DataType dest, String in) {
        return "";
    }

    @Override
    public String CType() {
        return "";
    }

    @Override
    public Color GetColor() {
        return Theme.Cable_Zombie;
    }

    @Override
    public Color GetColorHighlighted() {
        return Theme.Cable_Zombie_Highlighted;
    }

    @Override
    public String GenerateCopyCode(String dest, String source) {
        return "";
    }

    @Override
    public boolean isPointer() {
        return false;
    }

    @Override
    public String UnconnectedSink() {
        return "";
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof DTZombie);
    }

    @Override
    public int hashCode() {
        return 7; /* Hard-coded prime number */
    }
}
