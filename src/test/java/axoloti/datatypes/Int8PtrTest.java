package test.java.axoloti.datatypes;

import axoloti.datatypes.Bool32;
import axoloti.datatypes.CharPtr32;
import axoloti.datatypes.DataType;
import axoloti.datatypes.Frac32;
import axoloti.datatypes.Int32;
import axoloti.datatypes.Int32Ptr;
import axoloti.datatypes.Int8Ptr;
import axoloti.ui.Theme;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class Int8PtrTest {

    private Int8Ptr int8PtrInstance;

    @Before
    public void setUp() {
        int8PtrInstance = Int8Ptr.d;
    }

    @Test
    public void testIsConvertableToType() {
        assertFalse(int8PtrInstance.IsConvertableToType(Int8Ptr.d)); // Not even to itself based on current implementation
        assertFalse(int8PtrInstance.IsConvertableToType(Bool32.d));
        assertFalse(int8PtrInstance.IsConvertableToType(Int32.d));
        assertFalse(int8PtrInstance.IsConvertableToType(Frac32.d));
        assertFalse(int8PtrInstance.IsConvertableToType(CharPtr32.d));
        assertFalse(int8PtrInstance.IsConvertableToType(mock(DataType.class))); // Generic mock DataType
    }

    @Test
    public void testCType() {
        assertEquals("int8_t*", int8PtrInstance.CType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateConversionToType_toUnsupportedType() {
        int8PtrInstance.GenerateConversionToType(Bool32.d, "in");
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_Int8Pointer, int8PtrInstance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_Int8Pointer_Highlighted, int8PtrInstance.GetColorHighlighted());
    }

    @Test
    public void testEquals_toInt32Ptr() {
        assertTrue(int8PtrInstance.equals(new Int32Ptr()));
    }

    @Test
    public void testEquals_toSelf_basedOnImplementation() {
        assertFalse(int8PtrInstance.equals(Int8Ptr.d));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(int8PtrInstance.equals(null));
        assertFalse(int8PtrInstance.equals(Bool32.d));
        assertFalse(int8PtrInstance.equals(Int32.d));
    }

    @Test
    public void testGenerateCopyCode() {
        assertNull(int8PtrInstance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testHasDefaultValue() {
        assertFalse(int8PtrInstance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertNull(int8PtrInstance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testHashCode() {
        assertEquals(10, int8PtrInstance.hashCode());
        assertEquals(10, new Int8Ptr().hashCode());
    }

    @Test
    public void testIsPointer() {
        assertTrue(int8PtrInstance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("", int8PtrInstance.UnconnectedSink());
    }
}
