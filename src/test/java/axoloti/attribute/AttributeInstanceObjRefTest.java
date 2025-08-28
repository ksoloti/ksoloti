package test.java.axoloti.attribute;

import axoloti.SubPatchMode;
import axoloti.attributedefinition.AxoAttributeObjRef;
import axoloti.attribute.AttributeInstanceObjRef;
import axoloti.object.AxoObjectInstance;
import axoloti.Patch;
import axoloti.PatchSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.JTextField;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AttributeInstanceObjRefTest {

    @Mock
    private AxoAttributeObjRef mockAxoAttribute;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Mock
    private Patch mockPatch;

    @Mock
    private PatchSettings mockSettings;

    private AttributeInstanceObjRef attributeInstance;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        when(mockAxoObjectInstance.getPatch()).thenReturn(mockPatch);

        Field patchField = AxoObjectInstance.class.getField("patch");
        patchField.set(mockAxoObjectInstance, mockPatch);

        when(mockPatch.getSettings()).thenReturn(mockSettings);
        when(mockAxoAttribute.getName()).thenReturn("mockObjRef");

        attributeInstance = new AttributeInstanceObjRef(mockAxoAttribute, mockAxoObjectInstance);
    }

    @Test
    public void testPostConstructor() {
        attributeInstance.PostConstructor();

        assertEquals(2, attributeInstance.getComponentCount());
        Component textFieldComponent = attributeInstance.getComponent(1);
        assertTrue(textFieldComponent instanceof JTextField);
    }

    @Test
    public void testGetStringAndSetString() {
        attributeInstance.setString("some/path/to/object");
        assertEquals("some/path/to/object", attributeInstance.getString());

        attributeInstance.PostConstructor();
        attributeInstance.setString("another/path");
        assertEquals("another/path", attributeInstance.getString());
        JTextField tf = (JTextField) attributeInstance.getComponent(1);
        assertEquals("another/path", tf.getText());
    }

    @Test
    public void testLockAndUnlock() {
        attributeInstance.PostConstructor();
        JTextField textField = (JTextField) attributeInstance.getComponent(1);

        attributeInstance.Lock();
        assertFalse(textField.isEnabled());

        attributeInstance.UnLock();
        assertTrue(textField.isEnabled());
    }

    @Test
    public void testCValue_normalPath() {
        attributeInstance.setString("some/object");
        assertEquals("parent->objectinstance_some_i.objectinstance_object_i", attributeInstance.CValue());
    }

    @Test
    public void testCValue_subpatchModes() {
        attributeInstance.setString("../polyobject/child");

        PatchSettings polyphonicSettings = new PatchSettings();
        polyphonicSettings.subpatchmode = SubPatchMode.polyphonic;
        when(mockPatch.getSettings()).thenReturn(polyphonicSettings);
        assertEquals("parent->common->parent->objectinstance_polyobject_i.objectinstance_child_i", attributeInstance.CValue());

        PatchSettings polychannelSettings = new PatchSettings();
        polychannelSettings.subpatchmode = SubPatchMode.polychannel;
        when(mockPatch.getSettings()).thenReturn(polychannelSettings);
        assertEquals("parent->common->parent->objectinstance_polyobject_i.objectinstance_child_i", attributeInstance.CValue());

        PatchSettings polyexpressionSettings = new PatchSettings();
        polyexpressionSettings.subpatchmode = SubPatchMode.polyexpression;
        when(mockPatch.getSettings()).thenReturn(polyexpressionSettings);
        assertEquals("parent->common->parent->objectinstance_polyobject_i.objectinstance_child_i", attributeInstance.CValue());
        
        PatchSettings normalSettings = new PatchSettings();
        normalSettings.subpatchmode = SubPatchMode.normal;
        when(mockPatch.getSettings()).thenReturn(normalSettings);
        assertEquals("parent->parent->objectinstance_polyobject_i.objectinstance_child_i", attributeInstance.CValue());
    }
    
    @Test
    public void testCValue_doubleParent() {
        attributeInstance.setString("../../path/to/object");

        
        PatchSettings polyphonicSettings = new PatchSettings();
        polyphonicSettings.subpatchmode = SubPatchMode.polyphonic;
        when(mockPatch.getSettings()).thenReturn(polyphonicSettings);
        assertEquals("parent->common->parent->parent->objectinstance_path_i.objectinstance_to_i.objectinstance_object_i", attributeInstance.CValue());

        PatchSettings normalSettings = new PatchSettings();
        normalSettings.subpatchmode = SubPatchMode.normal;
        when(mockPatch.getSettings()).thenReturn(normalSettings);
        assertEquals("parent->parent->parent->objectinstance_path_i.objectinstance_to_i.objectinstance_object_i", attributeInstance.CValue());
    }

    @Test
    public void testFocusLost_setsDirtyWhenChanged() {
        attributeInstance.PostConstructor();
        JTextField tf = (JTextField) attributeInstance.getComponent(1);
        
        FocusListener[] listeners = tf.getListeners(FocusListener.class);
        assertTrue("Expected at least one FocusListener, but found " + listeners.length, listeners.length >= 1);
        
        FocusListener customFocusListener = null;
        for (FocusListener listener : listeners) {
            if (listener.getClass().getName().contains("AttributeInstanceObjRef$")) {
                customFocusListener = listener;
                break;
            }
        }
        assertNotNull("Could not find the custom FocusListener", customFocusListener);

        attributeInstance.setString("initial");
        customFocusListener.focusGained(new FocusEvent(tf, FocusEvent.FOCUS_GAINED));
        
        tf.setText("changed");
        customFocusListener.focusLost(new FocusEvent(tf, FocusEvent.FOCUS_LOST));

        verify(mockPatch, times(1)).SetDirty();
    }
    
    @Test
    public void testFocusLost_doesNotSetDirtyWhenUnchanged() {
        attributeInstance.PostConstructor();
        JTextField tf = (JTextField) attributeInstance.getComponent(1);
        
        FocusListener[] listeners = tf.getListeners(FocusListener.class);
        assertTrue("Expected at least one FocusListener, but found " + listeners.length, listeners.length >= 1);
        
        FocusListener customFocusListener = null;
        for (FocusListener listener : listeners) {
            if (listener.getClass().getName().contains("AttributeInstanceObjRef$")) {
                customFocusListener = listener;
                break;
            }
        }
        assertNotNull("Could not find the custom FocusListener", customFocusListener);

        attributeInstance.setString("initial");
        customFocusListener.focusGained(new FocusEvent(tf, FocusEvent.FOCUS_GAINED));
        
        customFocusListener.focusLost(new FocusEvent(tf, FocusEvent.FOCUS_LOST));

        verify(mockPatch, never()).SetDirty();
    }
}
