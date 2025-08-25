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

/**
 *
 * @author Johannes Taelman
 */
public class SCmdBringToDFUMode extends AbstractSCmd {

    @Override
    public String GetStartMessage() {
        return "Enabling Rescue Mode...";
    }

    @Override
    public String GetDoneMessage() {
        return "Done enabling Rescue Mode. Connection will now break and firmware can be flashed with DFU.\n";
    }

    @Override
    public SCmd Do(Connection connection) {
        LOGGER.info(GetStartMessage());
        connection.TransmitBringToDFU();
        setCompletedWithStatus(0); /* Does not expect a response */
        LOGGER.info(GetDoneMessage());
        return this;
    }
}
