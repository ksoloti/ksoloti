package test.java.axoloti.datatypes;

import axoloti.datatypes.Bool32;
import axoloti.datatypes.CharPtr32;
import axoloti.datatypes.DataType;
import axoloti.datatypes.Frac32;
import axoloti.datatypes.Int32;
import axoloti.ui.Theme;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class Frac32Test {

    private Frac32 frac32Instance;

    @Before
    public void setUp() {
        frac32Instance = Frac32.d;
    }

    @Test
    public void testIsConvertableToType_toSelf() {
        assertTrue(frac32Instance.IsConvertableToType(Frac32.d));
    }

    @Test
    public void testIsConvertableToType_toInt32() {
        assertTrue(frac32Instance.IsConvertableToType(Int32.d));
    }

    @Test
    public void testIsConvertableToType_toBool32() {
        assertTrue(frac32Instance.IsConvertableToType(Bool32.d));
    }

    @Test
    public void testIsConvertableToType_toOtherType() {
        assertFalse(frac32Instance.IsConvertableToType(CharPtr32.d));
        assertFalse(frac32Instance.IsConvertableToType(mock(DataType.class)));
    }

    @Test
    public void testGenerateConversionToType_toSelf() {
        assertEquals("in", frac32Instance.GenerateConversionToType(Frac32.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toInt32() {
        assertEquals("(in>>21)", frac32Instance.GenerateConversionToType(Int32.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toBool32() {
        assertEquals("(in>0)", frac32Instance.GenerateConversionToType(Bool32.d, "in"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateConversionToType_toUnsupportedType() {
        frac32Instance.GenerateConversionToType(CharPtr32.d, "in");
    }

    @Test
    public void testCType() {
        assertEquals("int32_t", frac32Instance.CType());
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_Frac32, frac32Instance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_Frac32_Highlighted, frac32Instance.GetColorHighlighted());
    }

    @Test
    public void testEquals_toSelf() {
        assertTrue(frac32Instance.equals(Frac32.d));
    }

    @Test
    public void testEquals_toOtherFrac32Instance() {
        assertTrue(frac32Instance.equals(new Frac32()));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(frac32Instance.equals(Int32.d));
        assertFalse(frac32Instance.equals(null));
    }

    @Test
    public void testGenerateCopyCode() {
        assertEquals("dest = source;\n", frac32Instance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testHasDefaultValue() {
        assertTrue(frac32Instance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertEquals("0 ", frac32Instance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testHashCode() {
        assertEquals(5, frac32Instance.hashCode());
        assertEquals(frac32Instance.hashCode(), new Frac32().hashCode());
    }

    @Test
    public void testIsPointer() {
        assertFalse(frac32Instance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("UNCONNECTED_OUTPUT", frac32Instance.UnconnectedSink());
    }
}
