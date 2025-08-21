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

/**
 *
 * @author Johannes Taelman
 */
public class QCmdMemRead extends AbstractQCmdSerialTask {

    final int addr;
    final int length;
    ByteBuffer values = null;

    public QCmdMemRead(int addr, int length) {
        this.addr = addr;
        this.length = length;
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.setCurrentExecutingCommand(this);
        connection.TransmitMemoryRead(addr, length);
        return this;
    }

    public ByteBuffer getValuesRead() {
        return values;
    }

    public void setValuesRead(ByteBuffer values) {
        this.values = values.duplicate();
    }

    @Override
    public String GetStartMessage() {
        return null;
    }

    @Override
    public String GetDoneMessage() {
        return null;
    }
}
