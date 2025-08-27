package test.java.axoloti.attributedefinition;

import axoloti.attribute.AttributeInstance;
import axoloti.attributedefinition.AxoAttribute;
import axoloti.object.AxoObjectInstance;
import java.security.MessageDigest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AxoAttributeTest {

    private static class ConcreteAxoAttribute extends AxoAttribute {

        public ConcreteAxoAttribute(String name) {
            super(name);
        }

        @Override
        public AttributeInstance InstanceFactory(AxoObjectInstance o) {
            return mock(AttributeInstance.class);
        }

        @Override
        public String getTypeName() {
            return "ConcreteAttribute";
        }
    }

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Mock
    private AttributeInstance mockAttributeInstance;

    @Mock
    private MessageDigest mockMessageDigest;

    private AxoAttribute attribute;

    @Before
    public void setUp() {
        attribute = new ConcreteAxoAttribute("testName");
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals("testName", attribute.getName());
        assertNull(attribute.getDescription());
    }

    @Test
    public void testSetters() {
        attribute.setName("newName");
        attribute.setDescription("a test description");

        assertEquals("newName", attribute.getName());
        assertEquals("a test description", attribute.getDescription());
    }

    @Test
    public void testCreateInstance() {
        AxoAttribute spyAttribute = spy(attribute);
        doReturn(mockAttributeInstance).when(spyAttribute).InstanceFactory(mockAxoObjectInstance);

        spyAttribute.CreateInstance(mockAxoObjectInstance);

        verify(spyAttribute, times(1)).InstanceFactory(mockAxoObjectInstance);
        verify(mockAxoObjectInstance, times(1)).add(mockAttributeInstance);
        verify(mockAttributeInstance, times(1)).PostConstructor();
    }

    @Test
    public void testCreateInstance_withCopy() {
        AttributeInstance mockSourceInstance = mock(AttributeInstance.class);
        AxoAttribute spyAttribute = spy(attribute);
        doReturn(mockAttributeInstance).when(spyAttribute).InstanceFactory(mockAxoObjectInstance);

        spyAttribute.CreateInstance(mockAxoObjectInstance, mockSourceInstance);

        verify(mockAttributeInstance, times(1)).CopyValueFrom(mockSourceInstance);
        verify(spyAttribute, times(1)).InstanceFactory(mockAxoObjectInstance);
        verify(mockAxoObjectInstance, times(1)).add(mockAttributeInstance);
        verify(mockAttributeInstance, times(1)).PostConstructor();
    }

    @Test
    public void testUpdateSHA() {
        attribute.updateSHA(mockMessageDigest);
        verify(mockMessageDigest, times(1)).update(attribute.getName().getBytes());
    }

    @Test
    public void testGetCName() {
        assertEquals("attr_testName", attribute.GetCName());
    }

    @Test
    public void testClone() {
        AxoAttribute clonedAttribute = attribute.clone();

        assertNotSame(attribute, clonedAttribute);
        assertEquals(attribute.getName(), clonedAttribute.getName());
        assertEquals(attribute.getDescription(), clonedAttribute.getDescription());
    }
}