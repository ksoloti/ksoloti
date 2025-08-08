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

package qcmds;

/**
 *
 * @author Ksoloti
 */
public class CommandManager {

    // private static final Logger LOGGER = Logger.getLogger(CommandManager.class.getName());

    private volatile boolean longOperationInProgress = false;
    // private volatile long lastLongOperationEndTime = 0;

    // private static final long COOLDOWN_MILLIS = 2000;

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
        // System.out.println(Instant.now() + " [DEBUG] CommandManager: Long operation started. Suppressing pings.");
    }

    public void endLongOperation() {
        longOperationInProgress = false;
        // lastLongOperationEndTime = System.currentTimeMillis();
        // System.out.println(Instant.now() + " [DEBUG] CommandManager: Long operation ended. Cooldown started. Resuming pings.");
    }

    /* Checks if the system is currently busy with a long operation or in cooldown. */
    public boolean isLongOperationActive() {
        return longOperationInProgress;
    }

    /* Determines if a given QCmdSerialTask should be offered to the USB queue
       based on the current busy state. */
    public boolean shouldOfferCommand(QCmdSerialTask command) {

        /* Suppress ALL commands if a long operation is currently active */
        if (isLongOperationActive()) {
            // System.out.println(Instant.now() + " [DEBUG] CommandManager: Suppressing command " + command.getClass().getSimpleName() + " because long operation is in progress.");
            return false;
        }

        /* Otherwise, the command is allowed */
        return true;
    }
}
