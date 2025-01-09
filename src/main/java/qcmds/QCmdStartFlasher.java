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

/**
 *
 * @author Johannes Taelman
 */
public class QCmdStartFlasher extends QCmdStart {

    public QCmdStartFlasher() {
        super(null);
    }

    @Override
    public String GetStartMessage() {
        return "Sending firmware packets...";
    }

    @Override
    public String GetDoneMessage() {
        return "Done sending firmware packets.\n\n>>> FIRMWARE UPDATE IN PROGRESS. DO NOT UNPLUG THE BOARD! <<<\nYou can connect again after the LEDs stop blinking.\n";
    }

    @Override
    public String GetTimeOutMessage() {
        /* Should never get here */
        return "\n>>> FIRMWARE FLASHING IN PROGRESS, DO NOT UNPLUG THE BOARD! <<<\nYou can connect again after the LEDs stop blinking.\n";
    }
}
