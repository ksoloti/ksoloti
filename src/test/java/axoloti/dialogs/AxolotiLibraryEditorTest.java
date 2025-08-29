package test.java.axoloti.dialogs;

import axoloti.dialogs.AxolotiLibraryEditor;
import axoloti.dialogs.PreferencesFrame;
import axoloti.utils.AxoFileLibrary;
import axoloti.utils.AxoGitLibrary;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.KeyUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;
import java.awt.Toolkit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class AxolotiLibraryEditorTest {

    private AxolotiLibraryEditor mockEditorInstance;
    private AxolotiLibrary mockLibrary;

    private static MockedStatic<PreferencesFrame> mockedPreferencesFrame;
    private static MockedStatic<Logger> mockedLogger;
    private static MockedStatic<KeyUtils> mockedKeyUtils;
    private static MockedStatic<Toolkit> mockedToolkit;

    private static PreferencesFrame mockPreferencesFrameInstance;

    private MockedConstruction<AxoGitLibrary> mockedAxoGitLibraryConstruction;
    private MockedConstruction<AxoFileLibrary> mockedAxoFileLibraryConstruction;

    private JRootPane mockJRootPaneForEditor;
    private JComboBox<String> mockComboBoxType;

    private static <T> T getPrivateField(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

    private static void setPrivateField(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private static <T> T getPrivateStaticField(Class<?> clazz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(null);
    }

    @BeforeClass
    public static void setUpClass() {
        mockedToolkit = Mockito.mockStatic(Toolkit.class);
        Toolkit mockToolkitInstance = mock(Toolkit.class);
        mockedToolkit.when(Toolkit::getDefaultToolkit).thenReturn(mockToolkitInstance);
        when(mockToolkitInstance.getMenuShortcutKeyMaskEx()).thenReturn(KeyEvent.CTRL_DOWN_MASK);

        mockedKeyUtils = Mockito.mockStatic(KeyUtils.class);

        mockedPreferencesFrame = Mockito.mockStatic(PreferencesFrame.class);
        mockPreferencesFrameInstance = mock(PreferencesFrame.class);
        mockedPreferencesFrame.when(PreferencesFrame::getInstance).thenReturn(mockPreferencesFrameInstance);

        mockedLogger = Mockito.mockStatic(Logger.class);
        Logger mockLoggerInstance = mock(Logger.class);
        mockedLogger.when(() -> Logger.getLogger(anyString())).thenReturn(mockLoggerInstance);
    }

    @AfterClass
    public static void tearDownClass() {
        if (mockedToolkit != null) {
            mockedToolkit.close();
        }
        if (mockedKeyUtils != null) {
            mockedKeyUtils.close();
        }
        if (mockedPreferencesFrame != null) {
            mockedPreferencesFrame.close();
        }
        if (mockedLogger != null) {
            mockedLogger.close();
        }
    }

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        Mockito.reset(mockPreferencesFrameInstance);

        mockJRootPaneForEditor = Mockito.mock(JRootPane.class);
        mockComboBoxType = mock(JComboBox.class);

        mockEditorInstance = mock(AxolotiLibraryEditor.class);

        doReturn(mockJRootPaneForEditor).when(mockEditorInstance).getRootPane();
        lenient().doNothing().when(mockEditorInstance).dispose();
        lenient().doNothing().when(mockEditorInstance).setTitle(anyString());
        lenient().doNothing().when(mockEditorInstance).pack();
        lenient().doNothing().when(mockEditorInstance).setVisible(anyBoolean());
        doReturn(true).when(mockEditorInstance).isLibraryValid();
        doNothing().when(mockEditorInstance).populateLib(any(AxolotiLibrary.class));
        lenient().doNothing().when(mockEditorInstance).saveLibrary();
        lenient().doNothing().when(mockEditorInstance).attemptCloseEditor();

        setPrivateField(mockEditorInstance, "jComboBoxType", mockComboBoxType);
        setPrivateField(mockEditorInstance, "library", mockLibrary);
        setPrivateField(mockEditorInstance, "dirty", false);

        mockedAxoGitLibraryConstruction = Mockito.mockConstruction(AxoGitLibrary.class);
        mockedAxoFileLibraryConstruction = Mockito.mockConstruction(AxoFileLibrary.class);

        List<AxolotiLibraryEditor> openEditorsList = getPrivateStaticField(AxolotiLibraryEditor.class, "openEditors");
        openEditorsList.clear();
        openEditorsList.add(mockEditorInstance);
    }

    @After
    public void tearDown() {
        if (mockedAxoGitLibraryConstruction != null) {
            mockedAxoGitLibraryConstruction.close();
        }
        if (mockedAxoFileLibraryConstruction != null) {
            mockedAxoFileLibraryConstruction.close();
        }
    }

    @Test
    public void testSaveLibrary_noChangeInType_updatesExistingLibrary() throws NoSuchFieldException, IllegalAccessException {
        AxolotiLibrary initialLibrary = mock(AxolotiLibrary.class);
        when(initialLibrary.getId()).thenReturn("lib123");
        when(initialLibrary.getType()).thenReturn(AxoFileLibrary.TYPE);
        setPrivateField(mockEditorInstance, "library", initialLibrary);
        setPrivateField(mockEditorInstance, "dirty", true);

        when(mockComboBoxType.getSelectedItem()).thenReturn(AxoFileLibrary.TYPE);
        doCallRealMethod().when(mockEditorInstance).saveLibrary();

        mockEditorInstance.saveLibrary();

        verify(mockEditorInstance).populateLib(eq(initialLibrary));
        AxolotiLibrary finalLibrary = getPrivateField(mockEditorInstance, "library");
        assertEquals(initialLibrary, finalLibrary);
        assertFalse((Boolean) getPrivateField(mockEditorInstance, "dirty"));
        verify(mockPreferencesFrameInstance, times(1)).setDirty(true);
    }

    @Test
    public void testSaveLibrary_changeToGitType_createsNewGitLibrary() throws NoSuchFieldException, IllegalAccessException {
        AxolotiLibrary initialLibrary = mock(AxolotiLibrary.class);
        when(initialLibrary.getId()).thenReturn("oldLibId");
        when(initialLibrary.getType()).thenReturn(AxoFileLibrary.TYPE);
        setPrivateField(mockEditorInstance, "library", initialLibrary);
        setPrivateField(mockEditorInstance, "dirty", true);

        when(mockComboBoxType.getSelectedItem()).thenReturn(AxoGitLibrary.TYPE);
        doCallRealMethod().when(mockEditorInstance).saveLibrary();

        mockEditorInstance.saveLibrary();

        assertEquals(1, mockedAxoGitLibraryConstruction.constructed().size());
        AxoGitLibrary newGitLibrary = mockedAxoGitLibraryConstruction.constructed().get(0);
        verify(newGitLibrary).setId("oldLibId");

        AxolotiLibrary finalLibrary = getPrivateField(mockEditorInstance, "library");
        assertEquals(newGitLibrary, finalLibrary);
        assertFalse((Boolean) getPrivateField(mockEditorInstance, "dirty"));
        verify(mockPreferencesFrameInstance, times(1)).setDirty(true);
    }

    @Test
    public void testSaveLibrary_changeToFileType_createsNewFileLibrary() throws NoSuchFieldException, IllegalAccessException {
        AxolotiLibrary initialLibrary = mock(AxolotiLibrary.class);
        when(initialLibrary.getId()).thenReturn("oldGitId");
        when(initialLibrary.getType()).thenReturn(AxoGitLibrary.TYPE);
        setPrivateField(mockEditorInstance, "library", initialLibrary);
        setPrivateField(mockEditorInstance, "dirty", true);

        when(mockComboBoxType.getSelectedItem()).thenReturn(AxoFileLibrary.TYPE);
        doCallRealMethod().when(mockEditorInstance).saveLibrary();

        mockEditorInstance.saveLibrary();

        assertEquals(1, mockedAxoFileLibraryConstruction.constructed().size());
        AxoFileLibrary newFileLibrary = mockedAxoFileLibraryConstruction.constructed().get(0);
        verify(newFileLibrary).setId("oldGitId");

        AxolotiLibrary finalLibrary = getPrivateField(mockEditorInstance, "library");
        assertEquals(newFileLibrary, finalLibrary);
        assertFalse((Boolean) getPrivateField(mockEditorInstance, "dirty"));
        verify(mockPreferencesFrameInstance, times(1)).setDirty(true);
    }

    @Test
    public void testSaveLibrary_invalidLibrary_doesNotSave() throws NoSuchFieldException, IllegalAccessException {
        setPrivateField(mockEditorInstance, "dirty", true);

        lenient().when(mockComboBoxType.getSelectedItem()).thenReturn(AxoFileLibrary.TYPE);
        lenient().doReturn(false).when(mockEditorInstance).isLibraryValid();
        doCallRealMethod().when(mockEditorInstance).saveLibrary();

        mockEditorInstance.saveLibrary();

        verify(mockEditorInstance, never()).populateLib(any(AxolotiLibrary.class));
        assertTrue((Boolean) getPrivateField(mockEditorInstance, "dirty"));
        verify(mockPreferencesFrameInstance, never()).setDirty(anyBoolean());
    }

    @Test
    public void testAttemptCloseEditor_dirtyFlagTrue_savesAndDisposes() throws NoSuchFieldException, IllegalAccessException {
        setPrivateField(mockEditorInstance, "dirty", true);
        when(mockComboBoxType.getSelectedItem()).thenReturn(AxoFileLibrary.TYPE);

        doCallRealMethod().when(mockEditorInstance).saveLibrary();
        doCallRealMethod().when(mockEditorInstance).attemptCloseEditor();

        mockEditorInstance.attemptCloseEditor();

        verify(mockEditorInstance, times(1)).saveLibrary();
        verify(mockEditorInstance, times(1)).dispose();
        verify(mockPreferencesFrameInstance, times(1)).setDirty(true);
    }

    @Test
    public void testAttemptCloseEditor_dirtyFlagFalse_onlyDisposes() throws NoSuchFieldException, IllegalAccessException {
        setPrivateField(mockEditorInstance, "dirty", false);
        doCallRealMethod().when(mockEditorInstance).attemptCloseEditor();

        mockEditorInstance.attemptCloseEditor();

        verify(mockEditorInstance, never()).saveLibrary();
        verify(mockEditorInstance, times(1)).dispose();
        verify(mockPreferencesFrameInstance, never()).setDirty(anyBoolean());
    }

    @Test
    public void testSetupKeyboardShortcuts() throws NoSuchFieldException, IllegalAccessException {
        InputMap mockInputMap = mock(InputMap.class);
        ActionMap mockActionMap = mock(ActionMap.class);

        when(mockJRootPaneForEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)).thenReturn(mockInputMap);
        when(mockJRootPaneForEditor.getActionMap()).thenReturn(mockActionMap);

        ArgumentCaptor<AbstractAction> actionCaptor = ArgumentCaptor.forClass(AbstractAction.class);

        try (MockedStatic<KeyStroke> mockedKeyStrokeStatic = Mockito.mockStatic(KeyStroke.class)) {
            KeyStroke mockKeyStroke = mock(KeyStroke.class);
            mockedKeyStrokeStatic.when(() -> KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyUtils.CONTROL_OR_CMD_MASK))
                                          .thenReturn(mockKeyStroke);

            doNothing().when(mockEditorInstance).attemptCloseEditor();

            doCallRealMethod().when(mockEditorInstance).setupKeyboardShortcuts();

            mockEditorInstance.setupKeyboardShortcuts();

            verify(mockJRootPaneForEditor).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            verify(mockInputMap).put(eq(mockKeyStroke), eq("closeEditor"));
            verify(mockJRootPaneForEditor).getActionMap();
            verify(mockActionMap).put(eq("closeEditor"), actionCaptor.capture());

            AbstractAction capturedAction = actionCaptor.getValue();
            capturedAction.actionPerformed(mock(java.awt.event.ActionEvent.class));
            verify(mockEditorInstance).attemptCloseEditor();
        }
    }
}
