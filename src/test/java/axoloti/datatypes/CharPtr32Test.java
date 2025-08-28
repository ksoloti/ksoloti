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
public class CharPtr32Test {

    private CharPtr32 charPtr32Instance;

    @Before
    public void setUp() {
        charPtr32Instance = CharPtr32.d;
    }

    @Test
    public void testIsConvertableToType_toSelf() {
        assertTrue(charPtr32Instance.IsConvertableToType(CharPtr32.d));
    }

    @Test
    public void testIsConvertableToType_toInt32() {
        assertTrue(charPtr32Instance.IsConvertableToType(Int32.d));
    }

    @Test
    public void testIsConvertableToType_toOtherType() {
        assertFalse(charPtr32Instance.IsConvertableToType(Bool32.d));
        assertFalse(charPtr32Instance.IsConvertableToType(Frac32.d));
        assertFalse(charPtr32Instance.IsConvertableToType(mock(DataType.class)));
    }

    @Test
    public void testGenerateConversionToType_toInt32() {
        assertEquals("(int32_t)(in[0])", charPtr32Instance.GenerateConversionToType(Int32.d, "in"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateConversionToType_toUnsupportedType() {
        charPtr32Instance.GenerateConversionToType(Bool32.d, "in");
    }

    @Test
    public void testCType() {
        assertEquals("char*", charPtr32Instance.CType());
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_CharPointer32, charPtr32Instance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_CharPointer32_Highlighted, charPtr32Instance.GetColorHighlighted());
    }

    @Test
    public void testEquals_toSelf() {
        assertTrue(charPtr32Instance.equals(CharPtr32.d));
    }

    @Test
    public void testEquals_toOtherCharPtr32Instance() {
        assertTrue(charPtr32Instance.equals(new CharPtr32()));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(charPtr32Instance.equals(Int32.d));
        assertFalse(charPtr32Instance.equals(null));
    }

    @Test
    public void testGenerateCopyCode() {
        assertEquals("dest = source;\n", charPtr32Instance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testHasDefaultValue() {
        assertFalse(charPtr32Instance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertEquals("0", charPtr32Instance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testHashCode() {
        assertEquals(9, charPtr32Instance.hashCode());
        assertEquals(charPtr32Instance.hashCode(), new CharPtr32().hashCode());
    }

    @Test
    public void testIsPointer() {
        assertTrue(charPtr32Instance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("(char * &)UNCONNECTED_OUTPUT", charPtr32Instance.UnconnectedSink());
    }
}
