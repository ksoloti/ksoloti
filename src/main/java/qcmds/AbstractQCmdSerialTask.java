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
    protected volatile byte mcuStatusCode = (byte) 0xFF; /* Stores the FatFs status code received from MCU */
    protected char expectedAckCommandByte = 0; /* Default value, only used by - and will be set by - subclasses that use AxoR<expectedAckCommandByte><statusbyte> */

    @Override
    public void setCompletedWithStatus(boolean success) {
        this.commandSuccess = success;
        latch.countDown(); // Signal that the command completed (successfully or not)
    }

    @Override
    public void setMcuStatusCode(byte statusCode) {
        this.mcuStatusCode = statusCode;
    }

    @Override
    public boolean waitForCompletion(long timeoutMs) throws InterruptedException {
        // This method returns 'true' if the latch counted down (command finished),
        // and 'false' if the timeout occurred before the latch counted down.
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean waitForCompletion() throws InterruptedException {
        /* standard 5-second timeout */
        return latch.await(5000, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isSuccessful() {
        // For AxoR-based commands, success means both commandSuccess (no Java-side comms error)
        // AND mcuStatusCode is FR_OK (0x00).
        return commandSuccess && mcuStatusCode == 0x00;
    }

    // New: Get the raw MCU status code
    public byte getMcuStatusCode() {
        return mcuStatusCode;
    }

    @Override
    public char getExpectedAckCommandByte() {
        return expectedAckCommandByte;
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.setCurrentExecutingCommand(this);
        return this;
    }

    @Override
    public abstract String GetStartMessage();
    
    @Override
    public abstract String GetDoneMessage();
}