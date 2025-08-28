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
public class Int32Test {

    private Int32 int32Instance;

    @Before
    public void setUp() {
        int32Instance = Int32.d;
    }

    @Test
    public void testIsConvertableToType_toSelf() {
        assertTrue(int32Instance.IsConvertableToType(Int32.d));
    }

    @Test
    public void testIsConvertableToType_toFrac32() {
        assertTrue(int32Instance.IsConvertableToType(Frac32.d));
    }

    @Test
    public void testIsConvertableToType_toBool32() {
        assertTrue(int32Instance.IsConvertableToType(Bool32.d));
    }

    @Test
    public void testIsConvertableToType_toOtherType() {
        assertFalse(int32Instance.IsConvertableToType(CharPtr32.d));
        assertFalse(int32Instance.IsConvertableToType(mock(DataType.class)));
    }

    @Test
    public void testGenerateConversionToType_toSelf() {
        assertEquals("in", int32Instance.GenerateConversionToType(Int32.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toFrac32() {
        assertEquals("(in<<21)", int32Instance.GenerateConversionToType(Frac32.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toBool32() {
        assertEquals("(in>0)", int32Instance.GenerateConversionToType(Bool32.d, "in"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateConversionToType_toUnsupportedType() {
        int32Instance.GenerateConversionToType(CharPtr32.d, "in");
    }

    @Test
    public void testCType() {
        assertEquals("int32_t", int32Instance.CType());
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_Int32, int32Instance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_Int32_Highlighted, int32Instance.GetColorHighlighted());
    }

    @Test
    public void testEquals_toSelf() {
        assertTrue(int32Instance.equals(Int32.d));
    }

    @Test
    public void testEquals_toOtherInt32Instance() {
        assertTrue(int32Instance.equals(new Int32()));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(int32Instance.equals(Frac32.d));
        assertFalse(int32Instance.equals(null));
    }

    @Test
    public void testGenerateCopyCode() {
        assertEquals("dest = source;\n", int32Instance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testHasDefaultValue() {
        assertTrue(int32Instance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertEquals("0", int32Instance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testHashCode() {
        assertEquals(8, int32Instance.hashCode());
        assertEquals(int32Instance.hashCode(), new Int32().hashCode());
    }

    @Test
    public void testIsPointer() {
        assertFalse(int32Instance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("UNCONNECTED_OUTPUT", int32Instance.UnconnectedSink());
    }
}
