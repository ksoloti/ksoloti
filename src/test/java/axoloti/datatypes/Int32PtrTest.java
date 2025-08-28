package test.java.axoloti.datatypes;

import axoloti.datatypes.Bool32;
import axoloti.datatypes.CharPtr32;
import axoloti.datatypes.DataType;
import axoloti.datatypes.Frac32;
import axoloti.datatypes.Int32;
import axoloti.datatypes.Int32Ptr;
import axoloti.ui.Theme;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class Int32PtrTest {

    private Int32Ptr int32PtrInstance;

    @Before
    public void setUp() {
        int32PtrInstance = Int32Ptr.d;
    }

    @Test
    public void testIsConvertableToType() {
        assertFalse(int32PtrInstance.IsConvertableToType(Int32Ptr.d));
        assertFalse(int32PtrInstance.IsConvertableToType(Bool32.d));
        assertFalse(int32PtrInstance.IsConvertableToType(Int32.d));
        assertFalse(int32PtrInstance.IsConvertableToType(Frac32.d));
        assertFalse(int32PtrInstance.IsConvertableToType(CharPtr32.d));
        assertFalse(int32PtrInstance.IsConvertableToType(mock(DataType.class))); // Generic mock DataType
    }

    @Test
    public void testCType() {
        assertEquals("int32_t*", int32PtrInstance.CType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateConversionToType_toUnsupportedType() {
        int32PtrInstance.GenerateConversionToType(Bool32.d, "in");
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_Int32Pointer, int32PtrInstance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_Int32Pointer_Highlighted, int32PtrInstance.GetColorHighlighted());
    }

    @Test
    public void testEquals_toSelf() {
        assertTrue(int32PtrInstance.equals(Int32Ptr.d));
    }

    @Test
    public void testEquals_toOtherInt32PtrInstance() {
        assertTrue(int32PtrInstance.equals(new Int32Ptr()));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(int32PtrInstance.equals(Int32.d));
        assertFalse(int32PtrInstance.equals(null));
    }

    @Test
    public void testGenerateCopyCode() {
        assertNull(int32PtrInstance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testHasDefaultValue() {
        assertFalse(int32PtrInstance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertNull(int32PtrInstance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testHashCode() {
        assertEquals(13, int32PtrInstance.hashCode());
        assertEquals(int32PtrInstance.hashCode(), new Int32Ptr().hashCode());
    }

    @Test
    public void testIsPointer() {
        assertTrue(int32PtrInstance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("", int32PtrInstance.UnconnectedSink());
    }
}
