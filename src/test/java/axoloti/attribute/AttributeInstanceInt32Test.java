package test.java.axoloti.attribute;

import axoloti.attributedefinition.AxoAttributeInt32;
import axoloti.attribute.AttributeInstanceInt32;
import axoloti.object.AxoObjectInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.JSlider;
import javax.swing.JLabel;
import java.awt.Component;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttributeInstanceInt32Test {

    @Mock
    private AxoAttributeInt32 mockAxoAttribute;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    private AttributeInstanceInt32 attributeInstance;

    @Before
    public void setUp() {
        when(mockAxoAttribute.getMinValue()).thenReturn(0);
        when(mockAxoAttribute.getMaxValue()).thenReturn(100);

        attributeInstance = new AttributeInstanceInt32(mockAxoAttribute, mockAxoObjectInstance);
    }

    @Test
    public void testPostConstructor() {
        attributeInstance.PostConstructor();

        assertEquals(3, attributeInstance.getComponentCount());

        Component sliderComponent = attributeInstance.getComponent(1);
        assertTrue(sliderComponent instanceof JSlider);
        JSlider slider = (JSlider) sliderComponent;
        assertEquals(0, slider.getMinimum());
        assertEquals(100, slider.getMaximum());
        assertEquals(0, slider.getValue());

        Component labelComponent = attributeInstance.getComponent(2);
        assertTrue(labelComponent instanceof JLabel);
        JLabel label = (JLabel) labelComponent;
        assertEquals("       0", label.getText());
    }

    @Test
    public void testCValue() {
        attributeInstance.setValue(50);
        assertEquals("50", attributeInstance.CValue());

        attributeInstance.setValue(0);
        assertEquals("0", attributeInstance.CValue());
    }

    @Test
    public void testLockAndUnlock() {
        attributeInstance.PostConstructor();
        JSlider slider = (JSlider) attributeInstance.getComponent(1); 

        attributeInstance.Lock();
        assertFalse(slider.isEnabled());

        attributeInstance.UnLock();
        assertTrue(slider.isEnabled());
    }

    @Test
    public void testCopyValueFrom() {
        AttributeInstanceInt32 source = new AttributeInstanceInt32(mockAxoAttribute, mockAxoObjectInstance);
        source.setValue(75);

        attributeInstance.CopyValueFrom(source);
        assertEquals(75, attributeInstance.getValue());
    }

    @Test
    public void testSliderChangesValueAndLabel() {
        attributeInstance.PostConstructor();
        JSlider slider = (JSlider) attributeInstance.getComponent(1); 
        JLabel label = (JLabel) attributeInstance.getComponent(2); 

        slider.setValue(42);

        assertEquals(42, attributeInstance.getValue());
        assertEquals("42", label.getText());
    }
}