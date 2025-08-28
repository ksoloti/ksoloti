package test.java.axoloti.datatypes;

import axoloti.datatypes.Bool32;
import axoloti.datatypes.DataType;
import axoloti.datatypes.DTZombie;
import axoloti.ui.Theme;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DTZombieTest {

    private DTZombie dtZombieInstance;

    @Before
    public void setUp() {
        dtZombieInstance = new DTZombie();
    }

    @Test
    public void testIsConvertableToType() {
        assertFalse(dtZombieInstance.IsConvertableToType(Bool32.d));
        assertFalse(dtZombieInstance.IsConvertableToType(mock(DataType.class)));
    }

    @Test
    public void testHasDefaultValue() {
        assertFalse(dtZombieInstance.HasDefaultValue());
    }

    @Test
    public void testGenerateSetDefaultValueCode() {
        assertEquals("", dtZombieInstance.GenerateSetDefaultValueCode());
    }

    @Test
    public void testGenerateConversionToType() {
        assertEquals("", dtZombieInstance.GenerateConversionToType(Bool32.d, "in"));
        assertEquals("", dtZombieInstance.GenerateConversionToType(mock(DataType.class), "in"));
    }

    @Test
    public void testCType() {
        assertEquals("", dtZombieInstance.CType());
    }

    @Test
    public void testGetColor() {
        assertEquals(Theme.Cable_Zombie, dtZombieInstance.GetColor());
    }

    @Test
    public void testGetColorHighlighted() {
        assertEquals(Theme.Cable_Zombie_Highlighted, dtZombieInstance.GetColorHighlighted());
    }

    @Test
    public void testGenerateCopyCode() {
        assertEquals("", dtZombieInstance.GenerateCopyCode("dest", "source"));
    }

    @Test
    public void testIsPointer() {
        assertFalse(dtZombieInstance.isPointer());
    }

    @Test
    public void testUnconnectedSink() {
        assertEquals("", dtZombieInstance.UnconnectedSink());
    }

    @Test
    public void testEquals_toSelf() {
        assertTrue(dtZombieInstance.equals(dtZombieInstance));
    }

    @Test
    public void testEquals_toDifferentType() {
        assertFalse(dtZombieInstance.equals(Bool32.d));
        assertFalse(dtZombieInstance.equals(null));
    }

    @Test
    public void testHashCode() {
        assertEquals(dtZombieInstance.hashCode(), new DTZombie().hashCode());
    }
}
