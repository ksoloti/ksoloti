package test.java.axoloti.atom;

import axoloti.atom.AtomDefinition;
import axoloti.atom.AtomInstance;
import axoloti.object.AxoObjectInstanceAbstract;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AtomInstanceTest {

    @Mock
    private AtomInstance mockAtomInstance;

    @Mock
    private AtomDefinition mockAtomDefinition;

    @Mock
    private AxoObjectInstanceAbstract mockAxoObjectInstanceAbstract;

    @Before
    public void setUp() {
        when(mockAtomInstance.GetDefinition()).thenReturn(mockAtomDefinition);
        when(mockAtomInstance.GetObjectInstance()).thenReturn(mockAxoObjectInstanceAbstract);
    }

    @Test
    public void testGetDefinition() {
        AtomDefinition definition = mockAtomInstance.GetDefinition();
        verify(mockAtomInstance, times(1)).GetDefinition();
        assertSame(mockAtomDefinition, definition);
    }

    @Test
    public void testGetObjectInstance() {
        AxoObjectInstanceAbstract objectInstance = mockAtomInstance.GetObjectInstance();
        verify(mockAtomInstance, times(1)).GetObjectInstance();
        assertSame(mockAxoObjectInstanceAbstract, objectInstance);
    }
}