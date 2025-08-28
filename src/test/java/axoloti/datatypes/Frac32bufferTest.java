package test.java.axoloti.datatypes;

import axoloti.datatypes.Bool32;
import axoloti.datatypes.CharPtr32;
import axoloti.datatypes.DataType;
import axoloti.datatypes.Frac32;
import axoloti.datatypes.Frac32buffer;
import axoloti.datatypes.Int32;
import axoloti.ui.Theme;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class Frac32bufferTest {

    private Frac32buffer frac32bufferInstance;

    private static final String I = "\t";

    @Before
    public void setUp() {
        frac32bufferInstance = Frac32buffer.d;
    }

    @Test
    public void testIsConvertableToType_toSelf() {
        assertTrue(frac32bufferInstance.IsConvertableToType(Frac32buffer.d));
    }

    @Test
    public void testIsConvertableToType_toBool32() {
        assertTrue(frac32bufferInstance.IsConvertableToType(Bool32.d));
    }

    @Test
    public void testIsConvertableToType_toFrac32() {
        assertTrue(frac32bufferInstance.IsConvertableToType(Frac32.d));
    }

    @Test
    public void testIsConvertableToType_toInt32() {
        assertTrue(frac32bufferInstance.IsConvertableToType(Int32.d));
    }

    @Test
    public void testIsConvertableToType_toOtherType() {
        assertFalse(frac32bufferInstance.IsConvertableToType(CharPtr32.d));
        assertFalse(frac32bufferInstance.IsConvertableToType(mock(DataType.class))); // Generic mock DataType
    }

    @Test
    public void testGenerateConversionToType_toSelf() {
        assertEquals("in", frac32bufferInstance.GenerateConversionToType(Frac32buffer.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toBool32() {
        assertEquals("(in[0]>0)", frac32bufferInstance.GenerateConversionToType(Bool32.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toFrac32() {
        assertEquals("(in[0])", frac32bufferInstance.GenerateConversionToType(Frac32.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toInt32() {
        assertEquals("(in[0]>>21)", frac32bufferInstance.GenerateConversionToType(Int32.d, "in"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateConversionToType_toUnsupportedType() {
        frac32bufferInstance.GenerateConversionToType(CharPtr32.d, "in");
    }

    @Test
    public void testCType() {
        assertEquals("int32buffer", frac32bufferInstance.CType());
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_Frac32Buffer, frac32bufferInstance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_Frac32Buffer_Highlighted, frac32bufferInstance.GetColorHighlighted());
    }

    @Test
    public void testEquals_toSelf() {
        assertTrue(frac32bufferInstance.equals(Frac32buffer.d));
    }

    @Test
    public void testEquals_toOtherFrac32bufferInstance() {
        assertTrue(frac32bufferInstance.equals(new Frac32buffer()));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(frac32bufferInstance.equals(Int32.d));
        assertFalse(frac32bufferInstance.equals(null));
    }

    @Test
    public void testGenerateCopyCode() {
        String expectedCode = "for (i=0; i<BUFSIZE; i++) {\n"
                            + I + I + I + "dest[i] = source[i];\n"
                            + I + I + "}\n";
        assertEquals(expectedCode, frac32bufferInstance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testHasDefaultValue() {
        assertFalse(frac32bufferInstance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertEquals("ZEROBUFFER", frac32bufferInstance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testHashCode() {
        assertEquals(3, frac32bufferInstance.hashCode());
        assertEquals(frac32bufferInstance.hashCode(), new Frac32buffer().hashCode());
    }

    @Test
    public void testIsPointer() {
        assertFalse(frac32bufferInstance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("UNCONNECTED_OUTPUT_BUFFER", frac32bufferInstance.UnconnectedSink());
    }

    @Test
    public void testGetIndex() {
        assertEquals("[5]", frac32bufferInstance.GetIndex("5"));
        assertEquals("[idx]", frac32bufferInstance.GetIndex("idx"));
        assertEquals("[0]", frac32bufferInstance.GetIndex("0"));
    }
}
