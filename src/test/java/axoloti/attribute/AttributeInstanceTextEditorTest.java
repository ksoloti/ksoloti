package test.java.axoloti.attribute;

import axoloti.TextEditor;
import axoloti.attributedefinition.AxoAttributeTextEditor;
import axoloti.attribute.AttributeInstanceTextEditor;
import axoloti.object.AxoObjectInstance;
import axoloti.Patch;
import axoloti.PatchFrame;
import axoloti.utils.StringRef;
import components.ButtonComponent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AttributeInstanceTextEditorTest {

    @Mock
    private AxoAttributeTextEditor mockAxoAttributeTextEditor;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Mock
    private Patch mockPatch;

    private AttributeInstanceTextEditor attributeInstance;

    @Before
    public void setUp() {
        when(mockAxoObjectInstance.getPatch()).thenReturn(mockPatch);
        when(mockAxoObjectInstance.getInstanceName()).thenReturn("testInstance");
        when(mockAxoAttributeTextEditor.getName()).thenReturn("testAttr");

        when(mockAxoObjectInstance.getPatch().getPatchframe()).thenReturn(mock(PatchFrame.class));

        attributeInstance = new AttributeInstanceTextEditor(mockAxoAttributeTextEditor, mockAxoObjectInstance);
    }

    private StringRef getSRef(AttributeInstanceTextEditor instance) throws NoSuchFieldException, IllegalAccessException {
        Field sRefField = AttributeInstanceTextEditor.class.getDeclaredField("sRef");
        sRefField.setAccessible(true);
        return (StringRef) sRefField.get(instance);
    }

    private ButtonComponent getBEdit(AttributeInstanceTextEditor instance) throws NoSuchFieldException, IllegalAccessException {
        Field bEditField = AttributeInstanceTextEditor.class.getDeclaredField("bEdit");
        bEditField.setAccessible(true);
        return (ButtonComponent) bEditField.get(instance);
    }

    private TextEditor getEditor(AttributeInstanceTextEditor instance) throws NoSuchFieldException, IllegalAccessException {
        Field editorField = AttributeInstanceTextEditor.class.getDeclaredField("editor");
        editorField.setAccessible(true);
        return (TextEditor) editorField.get(instance);
    }

    @Test
    public void testConstructor_stringParameter_null() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor instance = new AttributeInstanceTextEditor((String) null);
        assertEquals("", getSRef(instance).s);
    }

    @Test
    public void testConstructor_stringParameter_nonNull() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor instance = new AttributeInstanceTextEditor("initial text");
        assertEquals("initial text", getSRef(instance).s);
    }

    @Test
    public void testPostConstructor() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);

        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<ButtonComponent> mockedButtonComponent = Mockito.mockConstruction(ButtonComponent.class)) {
            
            spyAttributeInstance.PostConstructor();

            List<ButtonComponent> constructedButtons = mockedButtonComponent.constructed();
            assertEquals(1, constructedButtons.size());
            ButtonComponent createdButton = constructedButtons.get(0);

            verify(spyAttributeInstance, times(2)).add(any(Component.class));
            verify(spyAttributeInstance).add(createdButton);

            verify(createdButton).addActListener(any(ButtonComponent.ActListener.class));

            assertEquals(createdButton, getBEdit(spyAttributeInstance));
        }
    }

    @Test
    public void testShowEditor_firstCall() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));
        spyAttributeInstance.PostConstructor();

        
        try (MockedConstruction<TextEditor> mockedTextEditor = Mockito.mockConstruction(TextEditor.class)) {
            spyAttributeInstance.showEditor();

            List<TextEditor> constructedEditors = mockedTextEditor.constructed();
            assertEquals(1, constructedEditors.size());
            TextEditor createdEditor = constructedEditors.get(0);

            verify(createdEditor).setTitle("testInstance/testAttr");
            verify(createdEditor).addWindowFocusListener(any(WindowFocusListener.class));
            verify(createdEditor).setState(Frame.NORMAL);
            verify(createdEditor).setVisible(true);

            assertEquals(createdEditor, getEditor(spyAttributeInstance));
        }
    }

    @Test
    public void testShowEditor_subsequentCalls() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));
        spyAttributeInstance.PostConstructor();

        try (MockedConstruction<TextEditor> mockedTextEditor = Mockito.mockConstruction(TextEditor.class)) {
            spyAttributeInstance.showEditor();
            TextEditor firstEditor = mockedTextEditor.constructed().get(0);

            reset(firstEditor); 

            spyAttributeInstance.showEditor();

            assertEquals(1, mockedTextEditor.constructed().size()); 
            verify(firstEditor).setState(Frame.NORMAL);
            verify(firstEditor).setVisible(true);
            verify(firstEditor, never()).setTitle(anyString());
            verify(firstEditor, never()).addWindowFocusListener(any(WindowFocusListener.class));
        }
    }

    @Test
    public void testButtonActListener_triggersShowEditor() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));
        
        try (MockedConstruction<ButtonComponent> mockedButtonComponent = Mockito.mockConstruction(ButtonComponent.class)) {
            spyAttributeInstance.PostConstructor();

            ButtonComponent bEdit = mockedButtonComponent.constructed().get(0);

            ArgumentCaptor<ButtonComponent.ActListener> listenerCaptor = ArgumentCaptor.forClass(ButtonComponent.ActListener.class);
            verify(bEdit).addActListener(listenerCaptor.capture());
            ButtonComponent.ActListener listener = listenerCaptor.getValue();

            doNothing().when(spyAttributeInstance).showEditor(); 

            listener.OnPushed();

            verify(spyAttributeInstance, times(1)).showEditor();
        }
    }

    @Test
    public void testWindowFocusListener_setsDirtyWhenChanged() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));
        spyAttributeInstance.PostConstructor();

        
        try (MockedConstruction<TextEditor> mockedTextEditor = Mockito.mockConstruction(TextEditor.class)) {
            spyAttributeInstance.showEditor();
            TextEditor editor = mockedTextEditor.constructed().get(0);

            ArgumentCaptor<WindowFocusListener> listenerCaptor = ArgumentCaptor.forClass(WindowFocusListener.class);
            verify(editor).addWindowFocusListener(listenerCaptor.capture());
            WindowFocusListener listener = listenerCaptor.getValue();
            
            getSRef(spyAttributeInstance).s = "initial content";
            listener.windowGainedFocus(mock(WindowEvent.class));
            
            getSRef(spyAttributeInstance).s = "changed content";
            listener.windowLostFocus(mock(WindowEvent.class));

            verify(mockPatch, times(1)).SetDirty();
        }
    }

    @Test
    public void testWindowFocusListener_doesNotSetDirtyWhenUnchanged() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));
        spyAttributeInstance.PostConstructor();

        
        try (MockedConstruction<TextEditor> mockedTextEditor = Mockito.mockConstruction(TextEditor.class)) {
            spyAttributeInstance.showEditor();
            TextEditor editor = mockedTextEditor.constructed().get(0);

            ArgumentCaptor<WindowFocusListener> listenerCaptor = ArgumentCaptor.forClass(WindowFocusListener.class);
            verify(editor).addWindowFocusListener(listenerCaptor.capture());
            WindowFocusListener listener = listenerCaptor.getValue();
            
            getSRef(spyAttributeInstance).s = "same content";
            listener.windowGainedFocus(mock(WindowEvent.class));
            
            getSRef(spyAttributeInstance).s = "same content";
            listener.windowLostFocus(mock(WindowEvent.class));

            verify(mockPatch, never()).SetDirty();
        }
    }

    @Test
    public void testCValue() throws NoSuchFieldException, IllegalAccessException {
        getSRef(attributeInstance).s = "some value";
        assertEquals("some value", attributeInstance.CValue());

        getSRef(attributeInstance).s = "";
        assertEquals("", attributeInstance.CValue());
    }

    @Test
    public void testLock() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));
        
        try (MockedConstruction<ButtonComponent> mockedButtonComponent = Mockito.mockConstruction(ButtonComponent.class)) {
            spyAttributeInstance.PostConstructor();
            ButtonComponent bEdit = getBEdit(spyAttributeInstance);

            spyAttributeInstance.Lock();
            verify(bEdit).setEnabled(false);
        }
    }

    @Test
    public void testUnLock() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));
        
        try (MockedConstruction<ButtonComponent> mockedButtonComponent = Mockito.mockConstruction(ButtonComponent.class)) {
            spyAttributeInstance.PostConstructor();
            ButtonComponent bEdit = getBEdit(spyAttributeInstance);

            spyAttributeInstance.UnLock();
            verify(bEdit).setEnabled(true);
        }
    }

    @Test
    public void testGetStringAndSetString() throws NoSuchFieldError, IllegalAccessException, NoSuchFieldException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);

        spyAttributeInstance.setString("First Text");
        assertEquals("First Text", getSRef(spyAttributeInstance).s);

        try (MockedConstruction<TextEditor> mockedTextEditor = Mockito.mockConstruction(TextEditor.class)) {
            spyAttributeInstance.showEditor();
            TextEditor editor = mockedTextEditor.constructed().get(0);
            reset(editor);

            spyAttributeInstance.setString("Second Text");
            assertEquals("Second Text", getSRef(spyAttributeInstance).s);
            verify(editor).SetText("Second Text");

            getSRef(spyAttributeInstance).s = "Text from sRef";
            assertEquals("Text from sRef", spyAttributeInstance.getString());
        }
    }

    @Test
    public void testClose() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> invocation.getArgument(0)).when(spyAttributeInstance).add(any(Component.class));
        
        
        try (MockedConstruction<TextEditor> mockedTextEditor = Mockito.mockConstruction(TextEditor.class)) {
            spyAttributeInstance.showEditor();
            TextEditor editor = mockedTextEditor.constructed().get(0);

            assertNotNull(getEditor(spyAttributeInstance));

            spyAttributeInstance.Close();

            verify(editor).Close();
            assertNull(getEditor(spyAttributeInstance));
        }
    }

    @Test
    public void testClose_noEditor() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceTextEditor spyAttributeInstance = Mockito.spy(attributeInstance);

        spyAttributeInstance.Close();
        assertNull(getEditor(spyAttributeInstance));
    }
}
