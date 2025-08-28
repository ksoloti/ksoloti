package test.java.axoloti.attribute;

import axoloti.attributedefinition.AxoAttributeTablename;
import axoloti.attribute.AttributeInstanceTablename;
import axoloti.object.AxoObjectInstance;
import axoloti.Patch; // Assuming Patch is needed for SetDirty()

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.Component; // Needed for doAnswer on add()
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttributeInstanceTablenameTest {

    @Mock
    private AxoAttributeTablename mockAxoAttributeTablename;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Mock
    private Patch mockPatch; // Assuming SetDirty() is called on the Patch

    private AttributeInstanceTablename attributeInstance;

    @Before
    public void setUp() {
        when(mockAxoObjectInstance.getPatch()).thenReturn(mockPatch);

        attributeInstance = new AttributeInstanceTablename(mockAxoAttributeTablename, mockAxoObjectInstance);
    }

    private JTextField getTFtableName(AttributeInstanceTablename instance) throws NoSuchFieldException, IllegalAccessException {
        Field tfField = AttributeInstanceTablename.class.getDeclaredField("TFtableName");
        tfField.setAccessible(true);
        return (JTextField) tfField.get(instance);
    }

    private String getInternalTableName(AttributeInstanceTablename instance) throws NoSuchFieldException, IllegalAccessException {
        Field tableNameField = AttributeInstanceTablename.class.getDeclaredField("tableName");
        tableNameField.setAccessible(true);
        return (String) tableNameField.get(instance);
    }

    private void setInternalTableName(AttributeInstanceTablename instance, String name) throws NoSuchFieldException, IllegalAccessException {
        Field tableNameField = AttributeInstanceTablename.class.getDeclaredField("tableName");
        tableNameField.setAccessible(true);
        tableNameField.set(instance, name);
    }

    @Test
    public void testConstructorInitialization() throws NoSuchFieldException, IllegalAccessException {
        assertEquals("", getInternalTableName(attributeInstance));
    }

    @Test
    public void testPostConstructor() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTablename spyAttributeInstance = Mockito.spy(attributeInstance);

        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            
            spyAttributeInstance.PostConstructor();

            List<JTextField> constructedTextFields = mockedJTextField.constructed();
            assertEquals(1, constructedTextFields.size());
            JTextField createdTF = constructedTextFields.get(0);

            verify(createdTF).setSize(any(Dimension.class));
            verify(createdTF).setFont(any()); // We can't verify Constants.FONT directly if not loaded
            verify(createdTF).setMaximumSize(any(Dimension.class));
            verify(createdTF).setMinimumSize(any(Dimension.class));
            verify(createdTF).setPreferredSize(any(Dimension.class));

            verify(spyAttributeInstance, times(2)).add(any(Component.class)); // 1 from super, 1 from this class
            verify(spyAttributeInstance).add(createdTF);

            verify(createdTF.getDocument()).addDocumentListener(any(DocumentListener.class));
            verify(createdTF).addFocusListener(any(FocusListener.class));

            assertEquals(createdTF, getTFtableName(spyAttributeInstance));
        }
    }

    @Test
    public void testDocumentListener_updatesTableName() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTablename spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);
            Document mockDocument = tf.getDocument(); // Get the mocked document

            ArgumentCaptor<DocumentListener> listenerCaptor = ArgumentCaptor.forClass(DocumentListener.class);
            verify(mockDocument).addDocumentListener(listenerCaptor.capture());
            DocumentListener listener = listenerCaptor.getValue();

            when(tf.getText()).thenReturn("newTable1");
            listener.insertUpdate(mock(DocumentEvent.class));
            assertEquals("newTable1", getInternalTableName(spyAttributeInstance));

            when(tf.getText()).thenReturn("newTable2");
            listener.removeUpdate(mock(DocumentEvent.class));
            assertEquals("newTable2", getInternalTableName(spyAttributeInstance));

            when(tf.getText()).thenReturn("newTable3");
            listener.changedUpdate(mock(DocumentEvent.class));
            assertEquals("newTable3", getInternalTableName(spyAttributeInstance));
        }
    }

    @Test
    public void testFocusListener_setsDirtyWhenChanged() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTablename spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);

            ArgumentCaptor<FocusListener> focusListenerCaptor = ArgumentCaptor.forClass(FocusListener.class);
            verify(tf).addFocusListener(focusListenerCaptor.capture());
            FocusListener listener = focusListenerCaptor.getValue();
            
            setInternalTableName(spyAttributeInstance, "initial");
            when(tf.getText()).thenReturn("initial"); 
            
            listener.focusGained(mock(FocusEvent.class)); // Captures "initial" as valueBeforeAdjustment
            
            when(tf.getText()).thenReturn("changed");
            setInternalTableName(spyAttributeInstance, "changed"); // Ensure internal state is also changed

            listener.focusLost(mock(FocusEvent.class));

            verify(mockPatch, times(1)).SetDirty();
        }
    }

    @Test
    public void testFocusListener_doesNotSetDirtyWhenUnchanged() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTablename spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);

            ArgumentCaptor<FocusListener> focusListenerCaptor = ArgumentCaptor.forClass(FocusListener.class);
            verify(tf).addFocusListener(focusListenerCaptor.capture());
            FocusListener listener = focusListenerCaptor.getValue();
            
            setInternalTableName(spyAttributeInstance, "sameName");
            when(tf.getText()).thenReturn("sameName"); 
            
            listener.focusGained(mock(FocusEvent.class)); // Captures "sameName" as valueBeforeAdjustment
            
            when(tf.getText()).thenReturn("sameName"); 
            setInternalTableName(spyAttributeInstance, "sameName"); // Internal state also remains same

            listener.focusLost(mock(FocusEvent.class));

            verify(mockPatch, never()).SetDirty();
        }
    }

    @Test
    public void testCValue() throws NoSuchFieldException, IllegalAccessException {
        setInternalTableName(attributeInstance, "MyTable");
        assertEquals("MyTable", attributeInstance.CValue());

        setInternalTableName(attributeInstance, "");
        assertEquals("", attributeInstance.CValue());
    }

    @Test
    public void testLock() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTablename spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = getTFtableName(spyAttributeInstance); // Get the mock from the instance

            spyAttributeInstance.Lock();
            verify(tf).setEnabled(false);
        }
    }

    @Test
    public void testUnLock() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTablename spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = getTFtableName(spyAttributeInstance); // Get the mock from the instance

            spyAttributeInstance.UnLock();
            verify(tf).setEnabled(true);
        }
    }

    @Test
    public void testGetStringAndSetString() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTablename spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = getTFtableName(spyAttributeInstance); // Get the mock from the instance
            reset(tf); // Clear interactions from PostConstructor

            spyAttributeInstance.setString("NewName");
            assertEquals("NewName", getInternalTableName(spyAttributeInstance));
            verify(tf).setText("NewName"); // Verify JTextField was updated

            when(tf.getText()).thenReturn("ReadName"); // Simulate JTextField text for getString
            assertEquals("NewName", spyAttributeInstance.getString()); // getString directly returns tableName
            assertEquals("NewName", getInternalTableName(spyAttributeInstance)); // Verify internal state matches
            
            AttributeInstanceTablename testInstanceNoPost = new AttributeInstanceTablename(mockAxoAttributeTablename, mockAxoObjectInstance);
            testInstanceNoPost.setString("AnotherName");
            assertEquals("AnotherName", getInternalTableName(testInstanceNoPost));
        }
    }

    @Test
    public void testCopyValueFrom() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTablename spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = getTFtableName(spyAttributeInstance);
            reset(tf); // Clear interactions from PostConstructor

            AttributeInstanceTablename mockSource = mock(AttributeInstanceTablename.class);
            when(mockSource.getString()).thenReturn("SourceTableName");

            spyAttributeInstance.CopyValueFrom(mockSource);

            assertEquals("SourceTableName", getInternalTableName(spyAttributeInstance));
            verify(spyAttributeInstance).setString("SourceTableName"); // Verify setString was called
            verify(tf).setText("SourceTableName"); // Verify the underlying TF was updated
        }
    }

    @Test
    public void testPersist_nullTableName() throws NoSuchFieldException, IllegalAccessException {
        setInternalTableName(attributeInstance, null);
        
        attributeInstance.Persist();
        assertEquals("", getInternalTableName(attributeInstance));
    }

    @Test
    public void testPersist_nonNullTableName() throws NoSuchFieldException, IllegalAccessException {
        setInternalTableName(attributeInstance, "existingName");
        
        attributeInstance.Persist();
        assertEquals("existingName", getInternalTableName(attributeInstance));
    }
}
