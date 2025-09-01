package test.java.axoloti.attribute;

import axoloti.attribute.AttributeInstance;
import axoloti.attributedefinition.AxoAttribute;
import axoloti.object.AxoObjectInstance;
import axoloti.Patch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Component;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttributeInstanceTest {

    private static class ConcreteAttributeInstance extends AttributeInstance<AxoAttribute> {
        public ConcreteAttributeInstance(AxoAttribute attr, AxoObjectInstance axoObj1) {
            super(attr, axoObj1);
        }
        @Override
        public void Lock() {}
        @Override
        public void UnLock() {}
        @Override
        public String CValue() { return "mock_c_value"; }
        @Override
        public void CopyValueFrom(AttributeInstance a1) {}
    }

    @Mock
    private AxoAttribute mockAxoAttribute;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Mock
    private Patch mockAxoPatch;

    private ConcreteAttributeInstance attributeInstance;

    @Before
    public void setUp() {
        when(mockAxoAttribute.getName()).thenReturn("mockAttribute");
        when(mockAxoAttribute.getDescription()).thenReturn("A mock attribute description");
        when(mockAxoObjectInstance.getPatch()).thenReturn(mockAxoPatch);

        attributeInstance = new ConcreteAttributeInstance(mockAxoAttribute, mockAxoObjectInstance);
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals("mockAttribute", attributeInstance.getName());
        assertSame(mockAxoAttribute, attributeInstance.GetDefinition());
        assertSame(mockAxoObjectInstance, attributeInstance.GetObjectInstance());
    }

    @Test
    public void testPostConstructor() {
        attributeInstance.PostConstructor();

        assertTrue(attributeInstance.getLayout() instanceof BoxLayout);

        assertEquals(1, attributeInstance.getComponentCount());
        Component component = attributeInstance.getComponent(0);
        assertTrue(component instanceof JLabel);
        assertEquals("mockAttribute", ((JLabel) component).getText());

        assertEquals("A mock attribute description", attributeInstance.getToolTipText());
    }

    @Test
    public void testGetCName() {
        assertEquals("attr_mockAttribute", attributeInstance.GetCName());
    }

    @Test
    public void testSetDirty_withPatch() {
        attributeInstance.SetDirty();

        verify(mockAxoObjectInstance, times(1)).getPatch();
        verify(mockAxoPatch, times(1)).SetDirty();
    }

    @Test
    public void testSetDirty_noPatch() {
        when(mockAxoObjectInstance.getPatch()).thenReturn(null);

        attributeInstance.SetDirty();

        verify(mockAxoObjectInstance, times(1)).getPatch();
        verifyNoInteractions(mockAxoPatch);
    }
}