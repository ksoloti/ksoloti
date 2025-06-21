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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class AbstractQCmdSerialTask implements QCmdSerialTask {

    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile boolean commandSuccess = false;

    @Override
    public void setCommandCompleted(boolean success) {
        this.commandSuccess = success;
        latch.countDown(); // Signal completion
    }

    @Override
    public boolean waitForCompletion(long timeoutMs) throws InterruptedException {
        latch.await(timeoutMs, TimeUnit.MILLISECONDS); // Wait for the signal
        return commandSuccess; // Return the final status
    }

    @Override
    public boolean isSuccessful() {
        return commandSuccess;
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.SetCurrentExecutingCommand(this);
        return this;
    }

    @Override
    public abstract String GetStartMessage();
    
    @Override
    public abstract String GetDoneMessage();
}