/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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
public class QCmdStartMounter extends QCmdStart {

    boolean inbuiltMounterFlasher;

    public QCmdStartMounter() {
        super(null);
        inbuiltMounterFlasher = false;
    }

    public QCmdStartMounter(boolean useInbuiltMounterFlasher) {
        super(null);
        inbuiltMounterFlasher = useInbuiltMounterFlasher;
    }

    @Override
    public String GetStartMessage() {
        return "Mounting SD card...";
    }

    @Override
    public String GetDoneMessage() {
        return "Unmount/eject the SD card in your OS file manager to re-enable Patcher connection.\n";
    }


    public QCmd Do(Connection connection) {
        if(inbuiltMounterFlasher) {
            connection.ClearSync();
            connection.TransmitStartMounter();
        }
        
        return this;
    }
 
}
