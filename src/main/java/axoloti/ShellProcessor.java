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
package axoloti;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import qcmds.QCmdShellTask;

/**
 *
 * @author Johannes Taelman
 */
public class ShellProcessor extends SwingWorker<Integer, String> {

    private static final Logger LOGGER = Logger.getLogger(ShellProcessor.class.getName());

    private final BlockingQueue<QCmdShellTask> queueShellTasks;

    public ShellProcessor() {
        super();
        queueShellTasks = new ArrayBlockingQueue<QCmdShellTask>(99);
    }

    public boolean AppendToQueue(QCmdShellTask cmd) {
        // LOGGER.log(Level.INFO, "ShellProcessor queue: "+ cmd.GetStartMessage());
        try {
            boolean added = queueShellTasks.offer(cmd, 100, TimeUnit.MILLISECONDS);
            if (!added) {
                LOGGER.log(Level.WARNING, "ShellProcessor queue full, command not appended: " + cmd.getClass().getSimpleName());
            }
            return added;
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            LOGGER.log(Level.SEVERE, "ShellProcessor AppendToQueue interrupted while offering command: " + cmd.getClass().getSimpleName(), ex);
            return false; // Command not added due to interruption
        }
    }

    @Override
    public Integer doInBackground() {
        while (true) {
            // LOGGER.log(Level.INFO, "ShellProcessor Waiting");
            QCmdShellTask qc = null;
            try {
                qc = queueShellTasks.poll(5, TimeUnit.SECONDS);
                if (qc != null) {
                    // TODO Not doing anything with the poll result here... why?
                    // LOGGER.log(Level.INFO, "ShellProcessor: "+ qc.GetStartMessage());
                }
            }
            catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "ShellProcessor interrupted while waiting for task.", ex);
                Thread.currentThread().interrupt();
                break;
            }
            catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Unexpected error in ShellProcessor doInBackground.", ex);
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return 0;
    }

    public void println(String s) {
        publish(s);
    }

    @Override
    protected void process(List<String> chunks) {
        for (String s : chunks) {
            LOGGER.log(Level.INFO, s);
        }
    }

    @Override
    protected void done() {
        LOGGER.log(Level.SEVERE, "ShellProcessor Terminated!");
    }

    public void Panic() {
        queueShellTasks.clear();
    }
}
