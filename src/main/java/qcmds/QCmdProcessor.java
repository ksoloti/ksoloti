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
import axoloti.PatchGUI;
import axoloti.USBBulkConnection;
import axoloti.utils.Preferences;

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

    private static QCmdProcessor singleton = null;
    private final BlockingQueue<QCmd> queue;
    private final BlockingQueue<QCmd> queueResponse;
    protected Connection serialconnection;
    private final PeriodicPinger pinger;
    private final Thread pingerThread;
    private final PeriodicDialTransmitter dialTransmitter;
    private final Thread dialTransmitterThread;

    private final Object queueLock = new Object();

    class PeriodicPinger implements Runnable {

        private long lastSendAttemptTime = 0; /* When the last attempt to send a ping was made */
        private long currentSendInterval = 0; /* The dynamic interval for actually sending a ping */

        public PeriodicPinger() {
            this.currentSendInterval = Preferences.getInstance().getPollInterval();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(Preferences.getInstance().getPollInterval());
    
                    if (queue.isEmpty() && serialconnection.isConnected()) {
                        if (CommandManager.getInstance().isLongOperationActive()) {
                            /* If long operation, reset interval and continue to next loop iteration */
                            currentSendInterval = Preferences.getInstance().getPollInterval();
                            lastSendAttemptTime = System.currentTimeMillis();
                            // System.out.println(Instant.now() + " [DEBUG] PeriodicPinger: Suppressing ping as long operation is in progress.");
                            continue; /* Skip offering a ping this cycle */
                        }

                        long now = System.currentTimeMillis();
                        /* Check if enough time has passed since the last attempt to send a command */
                        if (now - lastSendAttemptTime >= currentSendInterval) {

                            boolean added = queue.offer(new QCmdPing(), 10, TimeUnit.MILLISECONDS);
                            if (!added) {
                                // System.out.println(Instant.now() + " [DEBUG] QCmd queue full, dropping ping command. Backing off outbound rate.");
                                /* If the queue is full (command not added), back off
                                   and increase and cap the currentSendInterval */
                                currentSendInterval = Math.min(currentSendInterval * 2, 500); /* Cap at 500ms */
                            }
                            else {
                                /* If the command was successfully added, reset the send interval */
                                currentSendInterval = Preferences.getInstance().getPollInterval();
                            }
                            /* Update the time of this sending attempt */
                            lastSendAttemptTime = now;
                        }
                    }
                    else {
                        /* If disconnected, reset the send interval and timer */
                        currentSendInterval = Preferences.getInstance().getPollInterval();
                        lastSendAttemptTime = 0;
                    }
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error in PeriodicPinger thread.", ex);
                    /* On a general error, add a small sleep to prevent tight looping */
                    try {
                        Thread.sleep(Preferences.getInstance().getPollInterval() * 5);
                    }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    class PeriodicDialTransmitter implements Runnable {

        private long lastSendAttemptTime = 0;
        private long currentSendInterval = 0;

        public PeriodicDialTransmitter() {
            this.currentSendInterval = Preferences.getInstance().getPollInterval();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5);

                    if (queue.isEmpty() && serialconnection.isConnected()) {
                        if (CommandManager.getInstance().isLongOperationActive()) {
                            currentSendInterval = Preferences.getInstance().getPollInterval(); /* Reset interval */
                            lastSendAttemptTime = System.currentTimeMillis(); /* Reset timer */
                            // System.out.println(Instant.now() + " [DEBUG] PeriodicDialTransmitter: Suppressing dial command as long operation is in progress.");
                            continue; /* Skip offering a dial command this cycle */
                        }

                        /* Get the currently live patch from the MainFrame */
                        PatchGUI currentLivePatch = MainFrame.mainframe.getCurrentLivePatch();

                        /* Only send dial commands if a patch is live */
                        if (currentLivePatch != null) {
                            long now = System.currentTimeMillis();
                            if (now - lastSendAttemptTime >= currentSendInterval) {

                                /* Pass the live patch to the QCmdGuiDialTx constructor */
                                boolean added = queue.offer(new QCmdGuiDialTx(currentLivePatch), 10, TimeUnit.MILLISECONDS);
                                if (!added) {
                                    // System.out.println(Instant.now() + " [DEBUG] QCmd queue full, dropping dial command. Backing off outbound rate.");
                                    currentSendInterval = Math.min(currentSendInterval * 2, 500);
                                }
                                else {
                                    currentSendInterval = Preferences.getInstance().getPollInterval();
                                }
                                lastSendAttemptTime = now;
                            }
                        } else {
                            /* If no patch is live, reset the interval and timer */
                            currentSendInterval = Preferences.getInstance().getPollInterval();
                            lastSendAttemptTime = 0;
                        }
                    }
                    else {
                        currentSendInterval = Preferences.getInstance().getPollInterval();
                        lastSendAttemptTime = 0;
                    }
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error in PeriodicDialTransmitter thread.", ex);
                    try {
                        Thread.sleep(Preferences.getInstance().getPollInterval() * 5);
                    }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    protected QCmdProcessor() {
        queue = new ArrayBlockingQueue<QCmd>(99);
        queueResponse = new ArrayBlockingQueue<QCmd>(99);
        serialconnection = USBBulkConnection.getInstance();
        pinger = new PeriodicPinger();
        pingerThread = new Thread(pinger);
        dialTransmitter = new PeriodicDialTransmitter();
        dialTransmitterThread = new Thread(dialTransmitter);
    }

    public void setConnection(Connection conn) {
        this.serialconnection = conn;
    }

    public static QCmdProcessor getInstance() {
        if (singleton == null)
            singleton = new QCmdProcessor();
        return singleton;
    }

    public Object getQueueLock() {
        return queueLock;
    }

    public boolean AppendToQueue(QCmd cmd) {
        try {
            boolean added = queue.offer(cmd, 100, TimeUnit.MILLISECONDS);
            if (added) {
                synchronized (queueLock) {
                    queueLock.notifyAll();
                }
            }
            return added;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void publish(final QCmd cmd) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (QCmdGUITask.class.isInstance(cmd)) {
                    ((QCmdGUITask) cmd).DoGUI(QCmdProcessor.this);
                }
            }
        });
    }

    public void WaitQueueFinished() {
        synchronized (queueLock) {
            while (!queue.isEmpty() || !queueResponse.isEmpty()) {
                try {
                    /* We can wait longer here, as the CommandManager handles ping suppression? */
                    queueLock.wait(10000);
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
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
            try {
                QCmd response = queueResponse.poll();
                while (response != null) {
                    publish(response);
                    response = queueResponse.poll();
                }

                QCmd cmd = queue.take();

                if (cmd instanceof QCmdSerialTask) {
                    serialconnection.AppendToQueue((AbstractQCmdSerialTask) cmd);

                } else if (cmd instanceof QCmdShellTask) {
                    QCmd shellResponse = ((QCmdShellTask) cmd).Do(this);
                    if ((shellResponse != null)) {
                        ((QCmdGUITask) shellResponse).DoGUI(this);
                    }
                } else if (cmd instanceof QCmdGUITask) {
                    publish(cmd);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                synchronized (queueLock) {
                    queueLock.notifyAll();
                }
            }
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

    public BlockingQueue<QCmd> getQueueResponse() {
        return queueResponse;
    }

    public void clearQueues() {
        queue.clear();
        queueResponse.clear();
    }

    public boolean isQueueEmpty() {
        return queue.isEmpty();
    }
    
    public boolean hasQueueSpaceLeft() {
        return (queue.remainingCapacity()>3);
    }
}
