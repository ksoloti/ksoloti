package test.java.axoloti.attribute;

import axoloti.attributedefinition.AxoAttributeSpinner;
import axoloti.attribute.AttributeInstanceSpinner;
import axoloti.object.AxoObjectInstance;
import axoloti.Patch;
import components.control.ACtrlEvent;
import components.control.ACtrlListener;
import components.control.NumberBoxComponent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.Component;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttributeInstanceSpinnerTest {

    @Mock
    private AxoAttributeSpinner mockAxoAttributeSpinner;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Mock
    private Patch mockPatch;

    private AttributeInstanceSpinner attributeInstance;

    @Before
    public void setUp() {
        when(mockAxoAttributeSpinner.getMinValue()).thenReturn(0);
        when(mockAxoAttributeSpinner.getMaxValue()).thenReturn(100);
        when(mockAxoAttributeSpinner.getDefaultValue()).thenReturn(50);

        when(mockAxoObjectInstance.getPatch()).thenReturn(mockPatch);

        attributeInstance = new AttributeInstanceSpinner(mockAxoAttributeSpinner, mockAxoObjectInstance);
    }

    private NumberBoxComponent getSpinner(AttributeInstanceSpinner instance) throws NoSuchFieldException, IllegalAccessException {
        Field spinnerField = AttributeInstanceSpinner.class.getDeclaredField("spinner");
        spinnerField.setAccessible(true);
        return (NumberBoxComponent) spinnerField.get(instance);
    }

    private int getInternalValue(AttributeInstanceSpinner instance) throws NoSuchFieldException, IllegalAccessException {
        Field valueField = AttributeInstanceSpinner.class.getSuperclass().getDeclaredField("value");
        valueField.setAccessible(true);
        return (int) valueField.get(instance);
    }

    private void setInternalValue(AttributeInstanceSpinner instance, int val) throws NoSuchFieldException, IllegalAccessException {
        Field valueField = AttributeInstanceSpinner.class.getSuperclass().getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(instance, val);
    }


    @Test
    public void testConstructorInitialization_defaultValueWithinRange() throws NoSuchFieldException, IllegalAccessException {
        // Verifies the value set directly by the constructor (before PostConstructor clamping)
        assertEquals(50, getInternalValue(attributeInstance));
    }

    @Test
    public void testConstructorInitialization_defaultValueBelowMin() throws NoSuchFieldException, IllegalAccessException {
        when(mockAxoAttributeSpinner.getDefaultValue()).thenReturn(-10); // Default is -10
        // Re-initialize attributeInstance with the new default value.
        // The constructor sets 'value' directly to -10 here.
        attributeInstance = new AttributeInstanceSpinner(mockAxoAttributeSpinner, mockAxoObjectInstance);
        
        // Assert that the constructor correctly assigned the default value.
        // PostConstructor, which would clamp it, is NOT called here.
        assertEquals(-10, getInternalValue(attributeInstance)); 
    }

    @Test
    public void testConstructorInitialization_defaultValueAboveMax() throws NoSuchFieldException, IllegalAccessException {
        when(mockAxoAttributeSpinner.getDefaultValue()).thenReturn(150); // Default is 150
        // Re-initialize attributeInstance with the new default value.
        // The constructor sets 'value' directly to 150 here.
        attributeInstance = new AttributeInstanceSpinner(mockAxoAttributeSpinner, mockAxoObjectInstance);
        
        // Assert that the constructor correctly assigned the default value.
        // PostConstructor, which would clamp it, is NOT called here.
        assertEquals(150, getInternalValue(attributeInstance)); 
    }

    @Test
    public void testPostConstructor() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSpinner spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        // Test clamping of value in PostConstructor when initial value is outside range
        when(mockAxoAttributeSpinner.getMinValue()).thenReturn(10);
        when(mockAxoAttributeSpinner.getMaxValue()).thenReturn(90);
        when(mockAxoAttributeSpinner.getDefaultValue()).thenReturn(5); // Initial value 5 (below min 10)
        
        AttributeInstanceSpinner clampedBelowInstance = new AttributeInstanceSpinner(mockAxoAttributeSpinner, mockAxoObjectInstance);
        AttributeInstanceSpinner spyClampedBelowInstance = Mockito.spy(clampedBelowInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyClampedBelowInstance).add(any(Component.class));
        try (MockedConstruction<NumberBoxComponent> mockedNumberBox = Mockito.mockConstruction(NumberBoxComponent.class)) {
            spyClampedBelowInstance.PostConstructor();
            assertEquals(10, getInternalValue(spyClampedBelowInstance)); // Should be clamped to min
        }

        when(mockAxoAttributeSpinner.getDefaultValue()).thenReturn(95); // Initial value 95 (above max 90)
        AttributeInstanceSpinner clampedAboveInstance = new AttributeInstanceSpinner(mockAxoAttributeSpinner, mockAxoObjectInstance);
        AttributeInstanceSpinner spyClampedAboveInstance = Mockito.spy(clampedAboveInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyClampedAboveInstance).add(any(Component.class));
        try (MockedConstruction<NumberBoxComponent> mockedNumberBox = Mockito.mockConstruction(NumberBoxComponent.class)) {
            spyClampedAboveInstance.PostConstructor();
            assertEquals(90, getInternalValue(spyClampedAboveInstance)); // Should be clamped to max
        }


        // Now proceed with the original PostConstructor component verification using 'spyAttributeInstance'
        try (MockedConstruction<NumberBoxComponent> mockedNumberBox = Mockito.mockConstruction(NumberBoxComponent.class)) {
            
            spyAttributeInstance.PostConstructor(); // This uses the default 50 value from setUp

            List<NumberBoxComponent> constructedNumberBoxes = mockedNumberBox.constructed();
            assertEquals(1, constructedNumberBoxes.size());
            NumberBoxComponent createdSpinner = constructedNumberBoxes.get(0);

            verify(createdSpinner).setParentAxoObjectInstance(mockAxoObjectInstance);
            verify(spyAttributeInstance, times(2)).add(any(Component.class)); // 1 from super, 1 from this class
            verify(spyAttributeInstance).add(createdSpinner);

            verify(createdSpinner).addACtrlListener(any(ACtrlListener.class));

            assertEquals(createdSpinner, getSpinner(spyAttributeInstance));
        }
    }

    @Test
    public void testACtrlAdjusted() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSpinner spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<NumberBoxComponent> mockedNumberBox = Mockito.mockConstruction(NumberBoxComponent.class)) {
            spyAttributeInstance.PostConstructor();
            NumberBoxComponent spinner = mockedNumberBox.constructed().get(0);

            ArgumentCaptor<ACtrlListener> listenerCaptor = ArgumentCaptor.forClass(ACtrlListener.class);
            verify(spinner).addACtrlListener(listenerCaptor.capture());
            ACtrlListener listener = listenerCaptor.getValue();

            when(spinner.getValue()).thenReturn(75.0);
            listener.ACtrlAdjusted(mock(ACtrlEvent.class));

            assertEquals(75, getInternalValue(spyAttributeInstance));

            when(spinner.getValue()).thenReturn(25.0);
            listener.ACtrlAdjusted(mock(ACtrlEvent.class));
            assertEquals(25, getInternalValue(spyAttributeInstance));
        }
    }

    @Test
    public void testACtrlAdjustmentBeginAndFinish_valueChanged() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSpinner spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<NumberBoxComponent> mockedNumberBox = Mockito.mockConstruction(NumberBoxComponent.class)) {
            spyAttributeInstance.PostConstructor();
            NumberBoxComponent spinner = mockedNumberBox.constructed().get(0);

            ArgumentCaptor<ACtrlListener> listenerCaptor = ArgumentCaptor.forClass(ACtrlListener.class);
            verify(spinner).addACtrlListener(listenerCaptor.capture());
            ACtrlListener listener = listenerCaptor.getValue();

            setInternalValue(spyAttributeInstance, 30);
            
            listener.ACtrlAdjustmentBegin(mock(ACtrlEvent.class));

            when(spinner.getValue()).thenReturn(45.0);
            listener.ACtrlAdjusted(mock(ACtrlEvent.class));

            listener.ACtrlAdjustmentFinished(mock(ACtrlEvent.class));

            verify(mockPatch, times(1)).SetDirty();
        }
    }

    @Test
    public void testACtrlAdjustmentBeginAndFinish_valueUnchanged() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSpinner spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<NumberBoxComponent> mockedNumberBox = Mockito.mockConstruction(NumberBoxComponent.class)) {
            spyAttributeInstance.PostConstructor();
            NumberBoxComponent spinner = mockedNumberBox.constructed().get(0);

            ArgumentCaptor<ACtrlListener> listenerCaptor = ArgumentCaptor.forClass(ACtrlListener.class);
            verify(spinner).addACtrlListener(listenerCaptor.capture());
            ACtrlListener listener = listenerCaptor.getValue();

            setInternalValue(spyAttributeInstance, 30);
            
            listener.ACtrlAdjustmentBegin(mock(ACtrlEvent.class));

            when(spinner.getValue()).thenReturn(30.0);
            listener.ACtrlAdjusted(mock(ACtrlEvent.class));

            listener.ACtrlAdjustmentFinished(mock(ACtrlEvent.class));

            verify(mockPatch, never()).SetDirty();
        }
    }

    @Test
    public void testCValue() throws NoSuchFieldException, IllegalAccessException {
        setInternalValue(attributeInstance, 77);
        assertEquals("77", attributeInstance.CValue());

        setInternalValue(attributeInstance, 0);
        assertEquals("0", attributeInstance.CValue());

        setInternalValue(attributeInstance, -5);
        assertEquals("-5", attributeInstance.CValue());
    }

    @Test
    public void testLockAndUnlock() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSpinner spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<NumberBoxComponent> mockedNumberBox = Mockito.mockConstruction(NumberBoxComponent.class)) {
            spyAttributeInstance.PostConstructor();
            NumberBoxComponent spinner = mockedNumberBox.constructed().get(0);

            when(spinner.isEnabled()).thenReturn(true);
            when(spinner.isFocusable()).thenReturn(true);

            spyAttributeInstance.Lock();
            verify(spinner).setEnabled(false);
            verify(spinner).setFocusable(false);

            spyAttributeInstance.UnLock();
            verify(spinner).setEnabled(true);
            verify(spinner).setFocusable(true);
        }
    }

    @Test
    public void testGetValueAndSetValue() throws NoSuchFieldException, IllegalAccessException {
        attributeInstance.setValue(25);
        assertEquals(25, attributeInstance.getValue());
        assertEquals(25, getInternalValue(attributeInstance));

        attributeInstance.setValue(88);
        assertEquals(88, attributeInstance.getValue());
        assertEquals(88, getInternalValue(attributeInstance));
    }
}
