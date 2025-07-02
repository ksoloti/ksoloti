/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2024 by Ksoloti
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */
package qcmds;

import axoloti.Connection;
import axoloti.MainFrame;
import axoloti.Patch;
import axoloti.USBBulkConnection;

import static axoloti.MainFrame.prefs;

// import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(QCmdProcessor.class.getName());

    private final BlockingQueue<QCmd> queue;
    private final BlockingQueue<QCmd> queueResponse;
    protected Connection serialconnection;
    private Patch patch;
    private final PeriodicPinger pinger;
    private final Thread pingerThread;
    private final PeriodicDialTransmitter dialTransmitter;
    private final Thread dialTransmitterThread;

    class PeriodicPinger implements Runnable {

        private long lastSendAttemptTime = 0; /* When the last attempt to send a ping was made */
        private long currentSendInterval = 0; /* The dynamic interval for actually sending a ping */

        public PeriodicPinger() {
            this.currentSendInterval = prefs.getPollInterval();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(prefs.getPollInterval());
    
                    if (queue.isEmpty() && serialconnection.isConnected()) {

                        long now = System.currentTimeMillis();
    
                        /* Check if enough time has passed since the last attempt to send a command */
                        if (now - lastSendAttemptTime >= currentSendInterval) {

                            boolean added = queue.offer(new QCmdPing(), 10, TimeUnit.MILLISECONDS);
                            // boolean added = queue.offer(new QCmdPing(true), 10, TimeUnit.MILLISECONDS); // no-disconnect ping for debug
                            if (!added) {
                                // System.out.println(Instant.now() + " QCmd queue full, dropping ping command. Backing off outbound rate.");
                                /* If the queue is full (command not added), back off
                                   and increase and cap the currentSendInterval */
                                currentSendInterval = Math.min(currentSendInterval * 2, 500); /* Cap at 500ms */
                            }
                            else {
                                /* If the command was successfully added, reset the send interval */
                                currentSendInterval = prefs.getPollInterval();
                            }
                            /* Update the time of this sending attempt */
                            lastSendAttemptTime = now;
                        }
                    }
                    else {
                        /* If disconnected, reset the send interval and timer */
                        currentSendInterval = prefs.getPollInterval();
                        lastSendAttemptTime = 0;
                    }
                }
                catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "PeriodicPinger interrupted.", ex);
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error in PeriodicPinger thread.", ex);
                    /* On a general error, add a small sleep to prevent tight looping */
                    try { Thread.sleep(prefs.getPollInterval() * 5); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }

    class PeriodicDialTransmitter implements Runnable {

        private long lastSendAttemptTime = 0;
        private long currentSendInterval = 0;

        public PeriodicDialTransmitter() {
            this.currentSendInterval = prefs.getPollInterval();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (queue.isEmpty() && serialconnection.isConnected()) {

                        long now = System.currentTimeMillis();

                        if (now - lastSendAttemptTime >= currentSendInterval) {
    
                            boolean added = queue.offer(new QCmdGuiDialTx(), 10, TimeUnit.MILLISECONDS);
                            if (!added) {
                                // System.out.println(Instant.now() + " QCmd queue full, dropping dial command. Backing off outbound rate.");
                                currentSendInterval = Math.min(currentSendInterval * 2, 500);
                            }
                            else {
                                currentSendInterval = prefs.getPollInterval();
                            }
                            lastSendAttemptTime = now;
                        }
                    }
                    else {
                        currentSendInterval = prefs.getPollInterval();
                        lastSendAttemptTime = 0;
                    }
                    Thread.sleep(5);
                }
                catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "PeriodicDialTransmitter interrupted.", ex);
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error in PeriodicDialTransmitter thread.", ex);
                    try { Thread.sleep(prefs.getPollInterval() * 5); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }
    
    protected QCmdProcessor() {
        queue = new ArrayBlockingQueue<QCmd>(20);
        queueResponse = new ArrayBlockingQueue<QCmd>(20);
        serialconnection = USBBulkConnection.GetConnection();
        pinger = new PeriodicPinger();
        pingerThread = new Thread(pinger);
        dialTransmitter = new PeriodicDialTransmitter();
        dialTransmitterThread = new Thread(dialTransmitter);
    }

    private static QCmdProcessor singleton = null;
    
    public static QCmdProcessor getQCmdProcessor() {
        if (singleton == null)
            singleton = new QCmdProcessor();
        return singleton;
    }
    
    public Patch getPatch() {
        return patch;
    }

    public boolean AppendToQueue(QCmd cmd) {
        try {
            /* Try to add for up to 100 milliseconds.
             * Adjust the timeout based on how long you're willing for the UI/caller to wait.
             * A very short timeout (e.g., 0ms) or no timeout at all (just offer(cmd))
             * makes it purely non-blocking if the queue is full. */
            boolean added = queue.offer(cmd, 100, TimeUnit.MILLISECONDS);
            if (!added) {
                LOGGER.log(Level.WARNING, "Warning: QCmd queue full, command not appended: " + cmd);
            }
            return added;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            LOGGER.log(Level.SEVERE, "Error: AppendToQueue interrupted while offering command: " + cmd);
            return false;
        }
    }

    public void Abort() {
        queue.clear();
        queueResponse.clear();
    }

    public void Panic() {
        queue.clear();
//        shellprocessor.Panic();
//        serialconnection.Panic();
    }

    private void publish(final QCmd cmd) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (QCmdGUITask.class.isInstance(cmd)) {
                    ((QCmdGUITask) cmd).DoGUI(QCmdProcessor.this);
                }
                String m = ((QCmd) cmd).GetDoneMessage();
                if (m != null) {
                    MainFrame.mainframe.SetProgressMessage(m);
                }
            }
        });
    }
    private int progressValue = 0;

    private void setProgress(final int i) {
        if (i != progressValue) {
            progressValue = i;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    MainFrame.mainframe.SetProgressValue(i);
                }
            });
        }
    }

    private void publish(final String m) {
//        println(m);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame.mainframe.SetProgressMessage(m);
            }
        });
    }

    public void WaitQueueFinished() {
        while (true) {
            if (queue.isEmpty() && queueResponse.isEmpty()) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void run() {
        pingerThread.setName("PingerThread");
        pingerThread.start();
        dialTransmitterThread.setName("DialTransmitter");
        dialTransmitterThread.start();
        while (true) {
            setProgress(0);
            try {
                queueResponse.clear();
                QCmd cmd = queue.take();
                if (!((cmd instanceof QCmdPing) || (cmd instanceof QCmdGuiDialTx))) {
                    // System.out.println(cmd);
                    // setProgress((100 * (queue.size() + 1)) / (queue.size() + 2));
                }
                String m = cmd.GetStartMessage();
                if (m != null) {
                    publish(m);
                    println(m);
                }
                if (QCmdShellTask.class.isInstance(cmd)) {
                    // shellprocessor.AppendToQueue((QCmdShellTask)cmd);
                    // publish(queueResponse.take());
                    QCmd response = ((QCmdShellTask) cmd).Do(this);
                    if ((response != null)) {
                        ((QCmdGUITask) response).DoGUI(this);
                    }
                }
                if (QCmdSerialTask.class.isInstance(cmd)) {
                    if (serialconnection.isConnected()) {
                        boolean appended = serialconnection.AppendToQueue((QCmdSerialTask) cmd);
                        if (appended) {
                            /* Only proceed to wait for a response if the command was successfully queued */
                            try {
                                QCmd response = queueResponse.take(); // This might still block if no response arrives
                                publish(response);
                                if (response instanceof QCmdDisconnect){
                                    queue.clear();
                                }
                            }
                            catch (InterruptedException e) {
                                /* Handle interruption while waiting for response */
                                LOGGER.log(Level.SEVERE, "Interrupted while waiting for response to serial command: " + cmd.getClass().getSimpleName(), e);
                                Thread.currentThread().interrupt();
                            }
                        }
                        else {
                            LOGGER.log(Level.SEVERE, "Failed to append serial command to USBBulkConnection queue. Command: " + cmd.getClass().getSimpleName());
                        }
                    }
                    else {
                        /* Handle case where serialconnection is not connected when trying to send a serial command. */
                        LOGGER.log(Level.WARNING, "Attempted to send serial command " + cmd.getClass().getSimpleName() + " but connection is not active.");
                    }
                }
                if (QCmdGUITask.class.isInstance(cmd)) {
                    publish(cmd);
                }
                m = cmd.GetDoneMessage();
                if (m != null) {
                    println(m);
                    publish(m);
                }
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            setProgress(0);
        }
    }

    public void println(final String s) {
        if ((s == null) || s.isEmpty()) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LOGGER.log(Level.INFO, s);
            }
        });
    }

    public void SetPatch(Patch patch) {
        if (this.patch != null) {
            this.patch.Unlock();
        }
        this.patch = patch;
    }

    public BlockingQueue<QCmd> getQueueResponse() {
        return queueResponse;
    }

    public void ClearQueue() {
        queue.clear();
    }    

    public boolean isQueueEmpty() {
        return queue.isEmpty();
    }
    
    public boolean hasQueueSpaceLeft() {
        return (queue.remainingCapacity()>3);
    }
}
