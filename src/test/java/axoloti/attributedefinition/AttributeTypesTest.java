package test.java.axoloti.attributedefinition;

import axoloti.attributedefinition.AttributeTypes;
import axoloti.attributedefinition.AxoAttribute;
import axoloti.attributedefinition.AxoAttributeComboBox;
import axoloti.attributedefinition.AxoAttributeObjRef;
import axoloti.attributedefinition.AxoAttributeSDFile;
import axoloti.attributedefinition.AxoAttributeSpinner;
import axoloti.attributedefinition.AxoAttributeTablename;
import axoloti.attributedefinition.AxoAttributeTextEditor;

import org.junit.Test;
import static org.junit.Assert.*;

public class AttributeTypesTest {

    @Test
    public void testGetTypes() {
        AxoAttribute[] types = AttributeTypes.getTypes();

        assertNotNull(types);
        assertEquals(6, types.length);

        assertTrue(types[0] instanceof AxoAttributeComboBox);
        assertTrue(types[1] instanceof AxoAttributeObjRef);
        assertTrue(types[2] instanceof AxoAttributeSpinner);
        assertTrue(types[3] instanceof AxoAttributeTablename);
        assertTrue(types[4] instanceof AxoAttributeSDFile);
        assertTrue(types[5] instanceof AxoAttributeTextEditor);
    }
}