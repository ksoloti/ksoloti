package test.java.axoloti;

import axoloti.Connection;
import axoloti.Patch;
import axoloti.listener.ConnectionStatusListener;
import axoloti.listener.BoardIDNameListener;
import axoloti.targetprofile.ksoloti_core;
import axoloti.utils.Preferences;
import qcmds.SCmd;

import org.junit.Test;
import org.junit.Before;
import org.mockito.MockedStatic;

import javax.swing.SwingUtilities;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ConnectionTest {
    private ConnectionTestImpl connection;

    @Before
    public void setup() {
        connection = new ConnectionTestImpl();

        try (MockedStatic<SwingUtilities> swingUtilMock = mockStatic(SwingUtilities.class)) {
            swingUtilMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    Runnable r = invocation.getArgument(0);
                    r.run();
                    return null;
                });
        }
    }

    @Test
    public void testAddConnectionStatusListener_Connected() {
        try (MockedStatic<SwingUtilities> swingUtilMock = mockStatic(SwingUtilities.class)) {
            swingUtilMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable r = invocation.getArgument(0);
                        r.run();
                        return null;
                    });

            // Arrange
            ConnectionStatusListener mockListener = mock(ConnectionStatusListener.class);
            connection.isConnected = true;

            // Act
            connection.addConnectionStatusListener(mockListener);

            // Assert
            verify(mockListener, times(1)).ShowConnect();

            try {
                Field field = Connection.class.getDeclaredField("csls");
                field.setAccessible(true);
                List<?> listeners = (List<?>) field.get(connection);
                assertEquals(1, listeners.size());
                assertEquals(mockListener, listeners.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testAddConnectionStatusListener_Disconnected() {
        try (MockedStatic<SwingUtilities> swingUtilMock = mockStatic(SwingUtilities.class)) {
            swingUtilMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable r = invocation.getArgument(0);
                        r.run();
                        return null;
                    });
            // Arrange
            ConnectionStatusListener mockListener = mock(ConnectionStatusListener.class);
            connection.isConnected = false;

            // Act
            connection.addConnectionStatusListener(mockListener);

            // Assert
            verify(mockListener, times(1)).ShowDisconnect();
        }
    }

    @Test
    public void testRemoveConnectionStatusListener() {
        try (MockedStatic<SwingUtilities> swingUtilMock = mockStatic(SwingUtilities.class)) {
            swingUtilMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable r = invocation.getArgument(0);
                        r.run();
                        return null;
                    });

            // Arrange
            ConnectionStatusListener mockListener = mock(ConnectionStatusListener.class);
            connection.addConnectionStatusListener(mockListener);
            reset(mockListener);

            // Act
            connection.removeConnectionStatusListener(mockListener);
            connection.ShowConnect();

            // Assert
            verify(mockListener, never()).ShowConnect();
        }
    }

    @Test
    public void testShowDisconnect() {
        try (MockedStatic<SwingUtilities> swingUtilMock = mockStatic(SwingUtilities.class)) {
            swingUtilMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable r = invocation.getArgument(0);
                        r.run();
                        return null;
                    });

            // Arrange
            ConnectionStatusListener mockListener1 = mock(ConnectionStatusListener.class);
            ConnectionStatusListener mockListener2 = mock(ConnectionStatusListener.class);
            connection.addConnectionStatusListener(mockListener1);
            connection.addConnectionStatusListener(mockListener2);
            reset(mockListener1, mockListener2);

            // Act
            connection.ShowDisconnect();

            // Assert
            verify(mockListener1, times(1)).ShowDisconnect();
            verify(mockListener2, times(1)).ShowDisconnect();
        }
    }

    @Test
    public void testAddBoardIDNameListener_withPreferences() {
        try (MockedStatic<SwingUtilities> swingUtilMock = mockStatic(SwingUtilities.class);
             MockedStatic<Preferences> prefsMock = mockStatic(Preferences.class)) {
            swingUtilMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable r = invocation.getArgument(0);
                        r.run();
                        return null;
                    });

            // Arrange
            BoardIDNameListener mockListener = mock(BoardIDNameListener.class);
            Preferences mockPrefs = mock(Preferences.class);
            prefsMock.when(Preferences::getInstance).thenReturn(mockPrefs);
            when(mockPrefs.getBoardName("test-cpu-id")).thenReturn("My Board");

            connection.isConnected = true;

            // Act
            connection.addBoardIDNameListener(mockListener);

            // Assert
            verify(mockListener).ShowBoardIDName("test-cpu-id", "My Board");
        }
    }

    private static class ConnectionTestImpl extends Connection {
        private boolean isConnected = false;
        private boolean isSDCardPresent = false;
        private String detectedCpuId = "test-cpu-id";

        @Override
        public boolean isConnected() {
            return this.isConnected;
        }

        @Override
        public boolean GetSDCardPresent() {
            return this.isSDCardPresent;
        }

        @Override
        public String getDetectedCpuId() {
            return this.detectedCpuId;
        }

        @Override public String getTargetCpuId() {
            return "test-target-cpu-id";
        }

        @Override public void setDisconnectRequested(boolean requested) {}
        @Override public boolean isDisconnectRequested() { return false; }
        @Override public void disconnect() {}
        @Override public boolean connect() { return false; }
        @Override public void selectPort() {}
        @Override public List<String[]> getDeviceList() { return null; }
        @Override public int TransmitStart() { return 0; }
        @Override public int TransmitStop() { return 0; }
        @Override public int TransmitPing() { return 0; }
        @Override public int TransmitRecallPreset(int presetNo) { return 0; }
        @Override public int TransmitStartMemWrite(int startAddr, int totalLen) { return 0; }
        @Override public int TransmitAppendMemWrite(byte[] data) { return 0; }
        @Override public int TransmitCloseMemWrite(int startAddr, int totalLen) { return 0; }
        @Override public int TransmitGetFileList() { return 0; }
        @Override public int TransmitGetFileInfo(String filename) { return 0; }
        @Override public int TransmitCreateFile(String filename, int size, Calendar date) { return 0; }
        @Override public int TransmitCreateDirectory(String filename, Calendar date) { return 0; }
        @Override public int TransmitDeleteFile(String filename) { return 0; }
        @Override public int TransmitChangeWorkingDirectory(String path) { return 0; }
        @Override public int TransmitAppendFile(byte[] data) { return 0; }
        @Override public int TransmitCloseFile(String filename, Calendar date) { return 0; }
        @Override public int TransmitCopyToFlash() { return 0; }
        @Override public int TransmitMemoryRead(int addr, int length) { return 0; }
        @Override public int TransmitMemoryRead1Word(int addr) { return 0; }
        @Override public int TransmitUpdatedPreset(byte[] data) { return 0; }
        @Override public int TransmitMidi(int m0, int m1, int m2) { return 0; }
        @Override public int TransmitGetFWVersion() { return 0; }
        @Override public int TransmitGetSpilinkSynced() { return 0; }
        @Override public int TransmitBringToDFU() { return 0; }
        @Override public int TransmitStartMounter() { return 0; }
        @Override public boolean AppendToQueue(SCmd cmd) { return false; }
        @Override public void ClearSync() {}
        @Override public boolean WaitSync(int msec) { return false; }
        @Override public boolean WaitSync() { return false; }
        @Override public void setPatch(Patch patch) {}
        @Override public ksoloti_core getTargetProfile() { return null; }
        @Override public byte[] getFwVersion() { return null; }
        @Override public String getFwVersionString() { return null; }
        @Override public ByteBuffer getMemReadBuffer() { return null; }
        @Override public int GetConnectionFlags() { return 0; }
        @Override public void setCurrentExecutingCommand(SCmd command) {}
        @Override public SCmd getCurrentExecutingCommand() { return null; }
        @Override public int writeBytes(ByteBuffer data) { return 0; }
    }
}