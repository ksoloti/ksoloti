package test.java.axoloti.attribute;

import axoloti.attributedefinition.AxoAttributeComboBox;
import axoloti.attribute.AttributeInstanceComboBox;
import axoloti.object.AxoObjectInstance;
import axoloti.Patch;
import components.DropDownComponent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.BoxLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttributeInstanceComboBoxTest {

    @Mock
    private AxoAttributeComboBox mockAxoAttribute;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Mock
    private Patch mockAxoPatch;

    private AttributeInstanceComboBox attributeInstance;
    private ArrayList<String> menuEntries = new ArrayList<>(Arrays.asList("Option A", "Option B", "Option C"));
    private ArrayList<String> cEntries = new ArrayList<>(Arrays.asList("C_A", "C_B", "C_C"));

    @Before
    public void setUp() {
        when(mockAxoAttribute.getName()).thenReturn("mockComboBox");
        when(mockAxoAttribute.getDescription()).thenReturn("A mock combo box attribute");
        when(mockAxoAttribute.getMenuEntries()).thenReturn(menuEntries);
        when(mockAxoAttribute.getCEntries()).thenReturn(cEntries);
        when(mockAxoObjectInstance.getPatch()).thenReturn(mockAxoPatch);
        when(mockAxoObjectInstance.getInstanceName()).thenReturn("mockInstance");

        attributeInstance = new AttributeInstanceComboBox(mockAxoAttribute, mockAxoObjectInstance);
    }

    @Test
    public void testPostConstructor() {
        attributeInstance.PostConstructor();

        assertTrue(attributeInstance.getLayout() instanceof BoxLayout);

        assertEquals(2, attributeInstance.getComponentCount());
        Component component = attributeInstance.getComponent(1);
        assertTrue(component instanceof DropDownComponent);
        DropDownComponent comboBox = (DropDownComponent) component;
        assertEquals(menuEntries.size(), comboBox.getItemCount());
        for (int i = 0; i < menuEntries.size(); i++) {
            assertEquals(menuEntries.get(i), comboBox.getItemAt(i));
        }
    }

    @Test
    public void testLockAndUnlock() {
        attributeInstance.PostConstructor();
        DropDownComponent comboBox = (DropDownComponent) attributeInstance.getComponent(1);

        attributeInstance.Lock();
        assertFalse(comboBox.isEnabled());

        attributeInstance.UnLock();
        assertTrue(comboBox.isEnabled());
    }

    @Test
    public void testCValue() {
        attributeInstance.PostConstructor();

        DropDownComponent comboBox = (DropDownComponent) attributeInstance.getComponent(1);
        comboBox.setSelectedItem("Option B");
        
        assertEquals("C_B", attributeInstance.CValue());
    }

    @Test
    public void testSetString() {
        attributeInstance.PostConstructor();

        attributeInstance.setString("Option C");
        DropDownComponent comboBox = (DropDownComponent) attributeInstance.getComponent(1);
        assertEquals("Option C", comboBox.getSelectedItem());

        attributeInstance.setString(null);
        assertEquals("Option A", comboBox.getSelectedItem());
    }

    @Test
    public void testSelectionChangedListener_callsSetDirty() {
        attributeInstance.PostConstructor();

        String initialSelection = attributeInstance.getString();
        assertEquals("Option A", initialSelection);

        DropDownComponent comboBox = (DropDownComponent) attributeInstance.getComponent(1);

        comboBox.setSelectedItem("Option B");

        assertEquals("Option B", attributeInstance.getString());
        verify(mockAxoPatch, times(1)).SetDirty();
    }

    @Test
    public void testSetString_logsErrorForUnmatchedSelection() {
        attributeInstance.PostConstructor();
        
        attributeInstance.setString("Non-existent option");

        verify(mockAxoObjectInstance, times(1)).getInstanceName();
    }
}