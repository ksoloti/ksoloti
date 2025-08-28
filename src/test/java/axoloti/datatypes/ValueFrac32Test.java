package test.java.axoloti.datatypes;

import axoloti.datatypes.Frac32;
import axoloti.datatypes.ValueFrac32;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ValueFrac32Test {

    private static final int FRAC_SHIFT = 21;
    private static final int FRAC_MAX_VAL = (1 << 27) - 1;

    private ValueFrac32 valueFrac32;

    @Before
    public void setUp() {
        valueFrac32 = new ValueFrac32();
    }

    @Test
    public void testDefaultConstructor() {
        assertEquals(0.0, valueFrac32.getDouble(), 0.0001);
    }

    @Test
    public void testCopyConstructor() {
        ValueFrac32 original = new ValueFrac32(0.75);
        ValueFrac32 copy = new ValueFrac32(original);
        assertEquals(0.75, copy.getDouble(), 0.0001);
    }

    @Test
    public void testDoubleConstructor() {
        ValueFrac32 instance = new ValueFrac32(1.25);
        assertEquals(1.25, instance.getDouble(), 0.0001);
    }

    @Test
    public void testSetAndGetInt() {
        valueFrac32.setInt(5);
        assertEquals(5.0, valueFrac32.getDouble(), 0.0001);
        assertEquals(5, valueFrac32.getInt());

        valueFrac32.setInt(-10);
        assertEquals(-10.0, valueFrac32.getDouble(), 0.0001);
        assertEquals(-10, valueFrac32.getInt());
    }

    @Test
    public void testSetAndGetFrac_zero() {
        valueFrac32.setFrac(0);
        assertEquals(0.0, valueFrac32.getDouble(), 0.0001);
        assertEquals(0, valueFrac32.getFrac());
    }

    @Test
    public void testSetAndGetFrac_positive() {
        int inputFrac = 1 << (FRAC_SHIFT - 1);
        valueFrac32.setFrac(inputFrac);
        assertEquals(0.5, valueFrac32.getDouble(), 0.0001);
        assertEquals(inputFrac, valueFrac32.getFrac());
    }

    @Test
    public void testSetAndGetFrac_maxClamped() {
        double veryLargeDouble = 1000.0;
        valueFrac32.setDouble(veryLargeDouble);
        assertEquals(FRAC_MAX_VAL, valueFrac32.getFrac());

        int hugeFrac = FRAC_MAX_VAL + 1000;
        valueFrac32.setDouble(((double)hugeFrac) / (1<<FRAC_SHIFT));
        assertEquals(FRAC_MAX_VAL, valueFrac32.getFrac());
    }
    
    @Test
    public void testSetAndGetFrac_negative() {
        int inputFrac = -(1 << (FRAC_SHIFT - 1));
        valueFrac32.setFrac(inputFrac);
        assertEquals(-0.5, valueFrac32.getDouble(), 0.0001);
        assertEquals(inputFrac, valueFrac32.getFrac());
    }

    @Test
    public void testSetAndGetFrac_boundaryValue() {
        int boundaryFrac = FRAC_MAX_VAL;
        valueFrac32.setFrac(boundaryFrac);
        double expectedDouble = (double) boundaryFrac / (1 << FRAC_SHIFT);
        assertEquals(expectedDouble, valueFrac32.getDouble(), 0.0000001);
        assertEquals(boundaryFrac, valueFrac32.getFrac());
    }

    @Test
    public void testSetAndGetDouble() {
        valueFrac32.setDouble(0.123);
        assertEquals(0.123, valueFrac32.getDouble(), 0.0001);

        valueFrac32.setDouble(-5.432);
        assertEquals(-5.432, valueFrac32.getDouble(), 0.0001);
    }

    @Test
    public void testGetAndSetRaw() {
        valueFrac32.setFrac(12345);
        assertEquals(12345, valueFrac32.getRaw());

        valueFrac32.setRaw(54321);
        assertEquals(54321, valueFrac32.getFrac());
    }

    @Test
    public void testCompareTo() {
        assertEquals(0, valueFrac32.compareTo(Frac32.d));
        assertEquals(0, new ValueFrac32(1.0).compareTo(Frac32.d));
        assertEquals(0, new ValueFrac32(-1.0).compareTo(Frac32.d));
    }
    
    @Test
    public void testEquals_defaultObjectBehavior() {
        ValueFrac32 v1 = new ValueFrac32(1.0);
        ValueFrac32 v2 = new ValueFrac32(1.0);
        assertNotEquals(v1, v2);
    }

    @Test
    public void testHashCode_defaultObjectBehavior() {
        ValueFrac32 v1 = new ValueFrac32(1.0);
        ValueFrac32 v2 = new ValueFrac32(1.0);
        assertNotEquals(v1.hashCode(), v2.hashCode());
    }
}
