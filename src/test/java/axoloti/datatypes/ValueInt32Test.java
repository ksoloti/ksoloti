package test.java.axoloti.datatypes;

import axoloti.datatypes.Int32;
import axoloti.datatypes.ValueInt32;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ValueInt32Test {

    private ValueInt32 valueInt32;

    @Before
    public void setUp() {
        valueInt32 = new ValueInt32();
    }

    @Test
    public void testDefaultConstructor() {
        assertEquals(0, valueInt32.getInt());
        assertEquals(0.0, valueInt32.getDouble(), 0.0001);
    }

    @Test
    public void testIntConstructor() {
        ValueInt32 instance = new ValueInt32(123);
        assertEquals(123, instance.getInt());
        assertEquals(123.0, instance.getDouble(), 0.0001);
    }

    @Test
    public void testCopyConstructor() {
        ValueInt32 original = new ValueInt32(456);
        ValueInt32 copy = new ValueInt32(original);
        assertEquals(456, copy.getInt());
        assertEquals(456.0, copy.getDouble(), 0.0001);
    }

    @Test
    public void testSetAndGetInt() {
        valueInt32.setInt(5);
        assertEquals(5, valueInt32.getInt());

        valueInt32.setInt(-10);
        assertEquals(-10, valueInt32.getInt());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetFrac_unsupported() {
        valueInt32.getFrac();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetFrac_unsupported() {
        valueInt32.setFrac(123);
    }

    @Test
    public void testSetAndGetDouble() {
        valueInt32.setDouble(0.123);
        assertEquals(0, valueInt32.getInt());
        assertEquals(0.0, valueInt32.getDouble(), 0.0001);

        valueInt32.setDouble(5.432);
        assertEquals(5, valueInt32.getInt());
        assertEquals(5.0, valueInt32.getDouble(), 0.0001);

        valueInt32.setDouble(-5.432);
        assertEquals(-5, valueInt32.getInt());
        assertEquals(-5.0, valueInt32.getDouble(), 0.0001);
    }

    @Test
    public void testGetAndSetRaw() {
        valueInt32.setInt(12345);
        assertEquals(12345, valueInt32.getRaw());

        valueInt32.setRaw(54321);
        assertEquals(54321, valueInt32.getInt());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCompareTo_unsupported() {
        valueInt32.compareTo(Int32.d);
    }
    
    @Test
    public void testEquals_defaultObjectBehavior() {
        ValueInt32 v1 = new ValueInt32(10);
        ValueInt32 v2 = new ValueInt32(10);
        assertNotEquals(v1, v2);
    }

    @Test
    public void testHashCode_defaultObjectBehavior() {
        ValueInt32 v1 = new ValueInt32(10);
        ValueInt32 v2 = new ValueInt32(10);
        assertNotEquals(v1.hashCode(), v2.hashCode());
    }
}
