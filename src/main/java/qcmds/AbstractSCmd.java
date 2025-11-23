/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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
import axoloti.USBBulkConnection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class AbstractSCmd implements SCmd {

    private final CountDownLatch latch = new CountDownLatch(1);
    protected volatile boolean commandSuccess = false;
    protected volatile int mcuStatusCode = 0xFF; /* Stores the (often FatFs-type) status code received from MCU */
    protected char expectedAckCommandByte = '\0'; /* Default value, only used by - and will be set by - subclasses that use AxoR<expectedAckCommandByte><statusbyte> */

    @Override
    public boolean setCompletedWithStatus(int mcuStatusCode) {
        this.mcuStatusCode = mcuStatusCode;
        this.commandSuccess = mcuStatusCode == 0;
        latch.countDown();
        return true;
    }

    @Override
    public boolean waitForCompletion(long timeoutMs) throws InterruptedException {
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean waitForCompletion() throws InterruptedException {
        /* standard 5-second timeout */
        return latch.await(5, TimeUnit.SECONDS);
    }

    @Override
    public boolean isSuccessful() {
        return commandSuccess;
    }

    public int getMcuStatusCode() {
        return mcuStatusCode;
    }

    @Override
    public char getExpectedAckCommandByte() {
        return expectedAckCommandByte;
    }

    @Override
    public SCmd Do(Connection connection) {
        return this;
    }

    @Override
    public SCmd Do() {
        return this.Do(USBBulkConnection.getInstance());
    }

    @Override
    public abstract String GetStartMessage();
    
    @Override
    public abstract String GetDoneMessage();
}