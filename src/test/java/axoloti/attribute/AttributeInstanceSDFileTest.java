package test.java.axoloti.attribute;

import axoloti.MainFrame;
import axoloti.Patch;
import axoloti.PatchFrame;
import axoloti.attributedefinition.AxoAttributeSDFile;
import axoloti.dialogs.AxoJFileChooser;
import axoloti.attribute.AttributeInstanceSDFile;
import axoloti.object.AxoObjectInstance;
import axoloti.sd.SDFileReference;
import axoloti.utils.Constants;
import components.ButtonComponent;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttributeInstanceSDFileTest {

    @Mock
    private AxoAttributeSDFile mockAxoAttribute;

    @Mock
    private AxoObjectInstance mockAxoObjectInstance;

    @Mock
    private Patch mockPatch;

    @Mock
    private PatchFrame mockPatchFrame;

    @Mock
    private AxoJFileChooser mockFileChooser;

    private AttributeInstanceSDFile attributeInstance;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        when(mockAxoAttribute.getName()).thenReturn("mockSDFile");
        when(mockAxoObjectInstance.getPatch()).thenReturn(mockPatch);
        when(mockPatch.getPatchframe()).thenReturn(mockPatchFrame);
        
        when(mockPatch.getFileNamePath()).thenReturn("/path/to/current/patch.axp");

        Field fcField = MainFrame.class.getDeclaredField("fc");
        fcField.setAccessible(true);
        fcField.set(null, mockFileChooser);

        attributeInstance = new AttributeInstanceSDFile(mockAxoAttribute, mockAxoObjectInstance);
    }

    private JTextField getTFFileName(AttributeInstanceSDFile instance) throws NoSuchFieldException, IllegalAccessException {
        Field tfField = AttributeInstanceSDFile.class.getDeclaredField("TFFileName");
        tfField.setAccessible(true);
        return (JTextField) tfField.get(instance);
    }

    private ButtonComponent getButtonChooseFile(AttributeInstanceSDFile instance) throws NoSuchFieldException, IllegalAccessException {
        Field buttonField = AttributeInstanceSDFile.class.getDeclaredField("ButtonChooseFile");
        buttonField.setAccessible(true);
        return (ButtonComponent) buttonField.get(instance);
    }


    @Test
    public void testPostConstructor() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSDFile spyTestInstance = Mockito.spy(new AttributeInstanceSDFile(mockAxoAttribute, mockAxoObjectInstance));
        
        doAnswer(invocation -> {
            return invocation.getArgument(0);
        }).when(spyTestInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                });
             MockedConstruction<ButtonComponent> mockedButtonComponent = Mockito.mockConstruction(ButtonComponent.class)) {

            spyTestInstance.PostConstructor();

            List<JTextField> constructedTextFields = mockedJTextField.constructed();
            List<ButtonComponent> constructedButtons = mockedButtonComponent.constructed();

            assertEquals(1, constructedTextFields.size());
            assertEquals(1, constructedButtons.size());

            JTextField createdTF = constructedTextFields.get(0);
            ButtonComponent createdButton = constructedButtons.get(0);

            assertEquals(createdTF, getTFFileName(spyTestInstance));
            assertEquals(createdButton, getButtonChooseFile(spyTestInstance));

            verify(spyTestInstance, times(3)).add(any(Component.class)); 
            verify(spyTestInstance).add(createdTF);
            verify(spyTestInstance).add(createdButton);

            verify(createdTF).setFont(Constants.FONT);
            verify(createdTF).setMaximumSize(any(Dimension.class));
            verify(createdTF).setMinimumSize(any(Dimension.class));
            verify(createdTF).setPreferredSize(any(Dimension.class));
            verify(createdTF).setSize(any(Dimension.class));
            verify(createdTF).getDocument(); 
            verify(createdTF).addFocusListener(any(FocusListener.class));
            verify(createdButton).addActListener(any(ButtonComponent.ActListener.class));
        }
    }


    @Test
    public void testGetStringAndSetString() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> { return invocation.getArgument(0); }).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getText()).thenReturn("");
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            
            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);
            reset(tf); 

            spyAttributeInstance.setString("testfile.wav");
            assertEquals("testfile.wav", spyAttributeInstance.getString());
            verify(tf).setText("testfile.wav"); 
            
            spyAttributeInstance.setString("anotherfile.mp3");
            assertEquals("anotherfile.mp3", spyAttributeInstance.getString());
            verify(tf).setText("anotherfile.mp3");
        }
    }

    @Test
    public void testLockAndUnlock() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> { return invocation.getArgument(0); }).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                });
             MockedConstruction<ButtonComponent> mockedButtonComponent = Mockito.mockConstruction(ButtonComponent.class)) {

            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);
            ButtonComponent button = mockedButtonComponent.constructed().get(0);

            reset(tf, button);

            spyAttributeInstance.Lock();
            verify(tf).setEnabled(false);
            verify(button).setEnabled(false);

            spyAttributeInstance.UnLock();
            verify(tf).setEnabled(true);
            verify(button).setEnabled(true);
        }
    }

    @Test
    public void testCValue_fileExists() {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getName()).thenReturn("mock_file.bin");
        doReturn(mockFile).when(spyAttributeInstance).getFile();

        assertEquals("mock_file.bin", spyAttributeInstance.CValue());
    }

    @Test
    public void testCValue_fileDoesNotExist() {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        doReturn(null).when(spyAttributeInstance).getFile();
        spyAttributeInstance.setString("non_existent_file.txt");

        assertEquals("non_existent_file.txt", spyAttributeInstance.CValue());
    }
    
    @Test
    public void testCValue_filePathWithBackslashes() {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getName()).thenReturn("folder\\subfolder\\my_file.wav");
        doReturn(mockFile).when(spyAttributeInstance).getFile();

        assertEquals("folder/subfolder/my_file.wav", spyAttributeInstance.CValue());
    }

    @Test
    public void testGetDependentSDFiles() {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);

        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getName()).thenReturn("dependent.sds");

        doReturn(mockFile).when(spyAttributeInstance).getFile();

        ArrayList<SDFileReference> files = spyAttributeInstance.GetDependentSDFiles();

        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("dependent.sds", files.get(0).localFilename);
        assertEquals(mockFile, files.get(0).localfile);
    }

    @Test
    public void testGetDependentSDFiles_noFile() {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        doReturn(null).when(spyAttributeInstance).getFile();

        ArrayList<SDFileReference> files = spyAttributeInstance.GetDependentSDFiles();

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testFocusLost_setsDirtyWhenChanged() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> { return invocation.getArgument(0); }).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);

            ArgumentCaptor<FocusListener> focusListenerCaptor = ArgumentCaptor.forClass(FocusListener.class);
            verify(tf).addFocusListener(focusListenerCaptor.capture());
            FocusListener customFocusListener = focusListenerCaptor.getValue();
            
            assertNotNull(customFocusListener);

            spyAttributeInstance.setString("initial.txt"); 
            when(tf.getText()).thenReturn("initial.txt"); 
            
            customFocusListener.focusGained(new FocusEvent(tf, FocusEvent.FOCUS_GAINED));
            
            when(tf.getText()).thenReturn("changed.txt"); 
            customFocusListener.focusLost(new FocusEvent(tf, FocusEvent.FOCUS_LOST));

            verify(mockPatch, times(1)).SetDirty();
        }
    }

    @Test
    public void testFocusLost_doesNotSetDirtyWhenUnchanged() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> { return invocation.getArgument(0); }).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                })) {
            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);
            
            ArgumentCaptor<FocusListener> focusListenerCaptor = ArgumentCaptor.forClass(FocusListener.class);
            verify(tf).addFocusListener(focusListenerCaptor.capture());
            FocusListener customFocusListener = focusListenerCaptor.getValue();
            
            assertNotNull(customFocusListener);

            spyAttributeInstance.setString("initial.txt"); 
            when(tf.getText()).thenReturn("initial.txt"); 
            
            customFocusListener.focusGained(new FocusEvent(tf, FocusEvent.FOCUS_GAINED));
            
            when(tf.getText()).thenReturn("initial.txt"); 
            customFocusListener.focusLost(new FocusEvent(tf, FocusEvent.FOCUS_LOST));

            verify(mockPatch, never()).SetDirty();
        }
    }

    @Test
    public void testButtonChooseFile_approvesSelection() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> { return invocation.getArgument(0); }).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                });
             MockedConstruction<ButtonComponent> mockedButtonComponent = Mockito.mockConstruction(ButtonComponent.class)) {

            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);
            ButtonComponent button = mockedButtonComponent.constructed().get(0);

            spyAttributeInstance.setString("original_file.wav"); 
            when(tf.getText()).thenReturn("original_file.wav"); 

            reset(tf); 
            
            File selectedFile = new File("/path/to/current/chosen_file.wav");
            
            when(mockFileChooser.showOpenDialog(any(PatchFrame.class))).thenReturn(AxoJFileChooser.APPROVE_OPTION);
            when(mockFileChooser.getSelectedFile()).thenReturn(selectedFile);
            
            when(mockPatch.getFileNamePath()).thenReturn("/path/to/current/patch.axp");
            when(mockPatch.getPatchframe()).thenReturn(mockPatchFrame);
            doNothing().when(mockPatchFrame).toFront(); 

            doReturn("chosen_file.wav").when(spyAttributeInstance).toRelative(any(File.class));

            ArgumentCaptor<ButtonComponent.ActListener> listenerCaptor = ArgumentCaptor.forClass(ButtonComponent.ActListener.class);
            verify(button).addActListener(listenerCaptor.capture());
            ButtonComponent.ActListener capturedListener = listenerCaptor.getValue();
            
            capturedListener.OnPushed();

            verify(mockFileChooser).resetChoosableFileFilters();
            verify(mockFileChooser).setCurrentDirectory(any(File.class));
            verify(mockFileChooser).restoreCurrentSize();
            verify(mockFileChooser).setFileSelectionMode(AxoJFileChooser.FILES_ONLY);
            verify(mockFileChooser).setDialogTitle("Select File...");
            verify(mockFileChooser).showOpenDialog(mockPatchFrame);
            verify(mockFileChooser).updateCurrentSize();
            
            verify(tf, times(1)).setText("chosen_file.wav"); 
            assertEquals("chosen_file.wav", spyAttributeInstance.getString());
            verify(mockPatch, times(1)).SetDirty();
        }
    }

    @Test
    public void testButtonChooseFile_cancelsSelection() throws NoSuchFieldException, IllegalAccessException {
        AttributeInstanceSDFile spyAttributeInstance = Mockito.spy(attributeInstance);
        doAnswer(invocation -> { return invocation.getArgument(0); }).when(spyAttributeInstance).add(any(Component.class));

        try (MockedConstruction<JTextField> mockedJTextField = Mockito.mockConstruction(JTextField.class,
                (mock, context) -> {
                    when(mock.getSize()).thenReturn(new Dimension(1, 1));
                    when(mock.getDocument()).thenReturn(mock(Document.class));
                });
             MockedConstruction<ButtonComponent> mockedButtonComponent = Mockito.mockConstruction(ButtonComponent.class)) {

            spyAttributeInstance.PostConstructor();
            JTextField tf = mockedJTextField.constructed().get(0);
            ButtonComponent button = mockedButtonComponent.constructed().get(0);

            spyAttributeInstance.setString("initial_file.wav"); 
            when(tf.getText()).thenReturn("initial_file.wav"); 

            reset(tf); 

            when(mockFileChooser.showOpenDialog(any(PatchFrame.class))).thenReturn(AxoJFileChooser.CANCEL_OPTION);
            when(mockPatch.getPatchframe()).thenReturn(mockPatchFrame);
            doNothing().when(mockPatchFrame).toFront(); 


            ArgumentCaptor<ButtonComponent.ActListener> listenerCaptor = ArgumentCaptor.forClass(ButtonComponent.ActListener.class);
            verify(button).addActListener(listenerCaptor.capture());
            ButtonComponent.ActListener capturedListener = listenerCaptor.getValue();
            
            capturedListener.OnPushed();

            verify(tf, never()).setText(anyString()); 
            assertEquals("initial_file.wav", spyAttributeInstance.getString());
            verify(mockPatch, never()).SetDirty();
        }
    }
}
