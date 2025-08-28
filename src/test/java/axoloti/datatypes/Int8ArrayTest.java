package test.java.axoloti.datatypes;

import axoloti.datatypes.Bool32;
import axoloti.datatypes.CharPtr32;
import axoloti.datatypes.DataType;
import axoloti.datatypes.Frac32;
import axoloti.datatypes.Int32;
import axoloti.datatypes.Int32Ptr;
import axoloti.datatypes.Int8Array;
import axoloti.ui.Theme;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class Int8ArrayTest {

    private Int8Array int8ArrayInstance;

    @Before
    public void setUp() {
        int8ArrayInstance = Int8Array.d;
    }

    @Test
    public void testIsConvertableToType() {
        assertFalse(int8ArrayInstance.IsConvertableToType(Int8Array.d));
        assertFalse(int8ArrayInstance.IsConvertableToType(Bool32.d));
        assertFalse(int8ArrayInstance.IsConvertableToType(Int32.d));
        assertFalse(int8ArrayInstance.IsConvertableToType(Frac32.d));
        assertFalse(int8ArrayInstance.IsConvertableToType(CharPtr32.d));
        assertFalse(int8ArrayInstance.IsConvertableToType(mock(DataType.class)));
    }

    @Test
    public void testCType() {
        assertEquals("int8_t[128]", int8ArrayInstance.CType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateConversionToType_toUnsupportedType() {
        int8ArrayInstance.GenerateConversionToType(Bool32.d, "in");
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_Int8Array, int8ArrayInstance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_Int8Array_Highlighted, int8ArrayInstance.GetColorHighlighted());
    }

    @Test
    public void testEquals_toInt32Ptr() {
        assertTrue(int8ArrayInstance.equals(new Int32Ptr()));
    }

    @Test
    public void testEquals_toSelf_basedOnImplementation() {
        assertFalse(int8ArrayInstance.equals(Int8Array.d));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(int8ArrayInstance.equals(null));
        assertFalse(int8ArrayInstance.equals(Bool32.d));
        assertFalse(int8ArrayInstance.equals(Int32.d));
    }

    @Test
    public void testGenerateCopyCode() {
        assertNull(int8ArrayInstance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testHasDefaultValue() {
        assertFalse(int8ArrayInstance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertNull(int8ArrayInstance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testHashCode() {
        assertEquals(10, int8ArrayInstance.hashCode());
        assertEquals(10, new Int8Array().hashCode());
    }

    @Test
    public void testIsPointer() {
        assertTrue(int8ArrayInstance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("", int8ArrayInstance.UnconnectedSink());
    }
}
