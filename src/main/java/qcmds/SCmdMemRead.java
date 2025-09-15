/**
 * Copyright (C) 2015 Johannes Taelman
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;

/**
 *
 * @author Johannes Taelman
 */
public class SCmdMemRead extends AbstractSCmd {

    final int addr;
    final int length;
    ByteBuffer values;

    public SCmdMemRead(int addr, int length) {
        this.addr = addr;
        this.length = length;
        this.values = ByteBuffer.allocateDirect(length).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public String GetStartMessage() {
        return null;
    }

    @Override
    public String GetDoneMessage() {
        return null;
    }

    @Override
    public SCmd Do(Connection connection) {
        connection.setCurrentExecutingCommand(this);
        connection.TransmitMemoryRead(addr, length);

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "MemRead command timed out.");
                setCompletedWithStatus(1);
                return this;
            }
            else if (!isSuccessful()) {
                LOGGER.log(Level.SEVERE, "MemRead command failed.");
                return this;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "MemRead command interrupted: " +  e.getMessage());
            setCompletedWithStatus(1);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during MemRead command: " + e.getMessage());
            setCompletedWithStatus(1);
        }
        return this;
    }

    public ByteBuffer getValuesRead() {
        return values;
    }

    public void setValuesRead(ByteBuffer values) {
        this.values = values.duplicate();
    }
}
