package test.java.axoloti.atom;

import axoloti.atom.AtomDefinition;
import axoloti.atom.AtomInstance;
import axoloti.object.AxoObjectInstance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AtomDefinitionTest {

    @Mock
    private AtomDefinition mockAtomDefinition;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Before
    public void setUp() {
        when(mockAtomDefinition.getName()).thenReturn("mockAtom");
        when(mockAtomDefinition.getDescription()).thenReturn("A mock description.");
        when(mockAtomDefinition.getTypeName()).thenReturn("mockType");
        when(mockAtomDefinition.getEditableFields()).thenReturn(Arrays.asList("field1", "field2"));
    }

    @Test
    public void testGetName() {
        String name = mockAtomDefinition.getName();
        assertEquals("mockAtom", name);
        verify(mockAtomDefinition, times(1)).getName();
    }

    @Test
    public void testSetName() {
        String newName = "updatedAtom";
        mockAtomDefinition.setName(newName);
        verify(mockAtomDefinition, times(1)).setName(newName);
    }

    @Test
    public void testGetDescription() {
        String description = mockAtomDefinition.getDescription();
        assertEquals("A mock description.", description);
        verify(mockAtomDefinition, times(1)).getDescription();
    }

    @Test
    public void testSetDescription() {
        String newDescription = "A new description.";
        mockAtomDefinition.setDescription(newDescription);
        verify(mockAtomDefinition, times(1)).setDescription(newDescription);
    }

    @Test
    public void testGetTypeName() {
        String typeName = mockAtomDefinition.getTypeName();
        assertEquals("mockType", typeName);
        verify(mockAtomDefinition, times(1)).getTypeName();
    }

    @Test
    public void testGetEditableFields() {
        List<String> editableFields = mockAtomDefinition.getEditableFields();
        assertEquals(2, editableFields.size());
        assertEquals("field1", editableFields.get(0));
        verify(mockAtomDefinition, times(1)).getEditableFields();
    }

    @Test
    @SuppressWarnings("unused")
    public void testCreateInstance() {
        when(mockAtomDefinition.CreateInstance(mockAxoObjectInstance)).thenReturn(mock(AtomInstance.class));
        AtomInstance instance = mockAtomDefinition.CreateInstance(mockAxoObjectInstance);
        verify(mockAtomDefinition, times(1)).CreateInstance(mockAxoObjectInstance);
    }
}