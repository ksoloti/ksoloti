/*
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

package axoloti.utils;

import java.time.Instant;
import qcmds.QCmdPing;
import qcmds.QCmdSerialTask;

/**
 *
 * @author Ksoloti
 */
public class CommandManager {

    // private static final Logger LOGGER = Logger.getLogger(CommandManager.class.getName());

    private volatile boolean longOperationInProgress = false;
    private volatile long lastLongOperationEndTime = 0;

    private static final long COOLDOWN_MILLIS = 2000;

    private static CommandManager instance;

    public static synchronized CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    private CommandManager() {
        /* Private constructor */
    }

    public void startLongOperation() {
        longOperationInProgress = true;
        // System.out.println(Instant.now() + " CommandManager: Long operation started.");
    }

    public void endLongOperation() {
        longOperationInProgress = false;
        lastLongOperationEndTime = System.currentTimeMillis();
        // System.out.println(Instant.now() + " CommandManager: Long operation ended. Cooldown started.");
    }

    /* Checks if the system is currently busy with a long operation or in cooldown. */
    public boolean isBusy() {
        return longOperationInProgress || (System.currentTimeMillis() - lastLongOperationEndTime < COOLDOWN_MILLIS);
    }

    /* Determines if a given QCmdSerialTask should be offered to the USB queue
       based on the current busy state and cooldown period. */
    public boolean shouldOfferCommand(QCmdSerialTask command) {
        if (isBusy()) {
            if (command instanceof QCmdPing || command.getClass().getSimpleName().equals("QCmdGuiDialTx")) {
                System.out.println(Instant.now() + " CommandManager: Suppressing " + command.getClass().getSimpleName() + " due to busy state.");
                return false;
            }
        }
        return true;
    }
}

