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
public class Bool32Test {

    private Bool32 bool32Instance;

    @Before
    public void setUp() {
        bool32Instance = Bool32.d;
    }

    @Test
    public void testIsConvertableToType_toSelf() {
        assertTrue(bool32Instance.IsConvertableToType(Bool32.d));
    }

    @Test
    public void testIsConvertableToType_toInt32() {
        assertTrue(bool32Instance.IsConvertableToType(Int32.d));
    }

    @Test
    public void testIsConvertableToType_toFrac32() {
        assertTrue(bool32Instance.IsConvertableToType(Frac32.d));
    }

    @Test
    public void testIsConvertableToType_toOtherType() {
        assertFalse(bool32Instance.IsConvertableToType(CharPtr32.d));
        assertFalse(bool32Instance.IsConvertableToType(mock(DataType.class)));
    }

    @Test
    public void testGenerateConversionToType_toSelf() {
        assertEquals("in", bool32Instance.GenerateConversionToType(Bool32.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toInt32() {
        assertEquals("(in?1:0)", bool32Instance.GenerateConversionToType(Int32.d, "in"));
    }

    @Test
    public void testGenerateConversionToType_toFrac32() {
        assertEquals("(in?(1<<27)-1:0)", bool32Instance.GenerateConversionToType(Frac32.d, "in"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateConversionToType_toUnsupportedType() {
        bool32Instance.GenerateConversionToType(CharPtr32.d, "in");
    }

    @Test
    public void testCType() {
        assertEquals("bool", bool32Instance.CType());
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_Bool32, bool32Instance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_Bool32_Highlighted, bool32Instance.GetColorHighlighted());
    }

    @Test
    public void testEquals_toSelf() {
        assertTrue(bool32Instance.equals(Bool32.d));
    }

    @Test
    public void testEquals_toOtherBool32Instance() {
        assertTrue(bool32Instance.equals(new Bool32()));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(bool32Instance.equals(Int32.d));
        assertFalse(bool32Instance.equals(null));
    }

    @Test
    public void testGenerateCopyCode() {
        assertEquals("dest = source;\n", bool32Instance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testHasDefaultValue() {
        assertTrue(bool32Instance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertEquals("0", bool32Instance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testHashCode() {
        assertEquals(7, bool32Instance.hashCode());
        assertEquals(bool32Instance.hashCode(), new Bool32().hashCode());
    }

    @Test
    public void testIsPointer() {
        assertFalse(bool32Instance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("(bool &)UNCONNECTED_OUTPUT", bool32Instance.UnconnectedSink());
    }
}
