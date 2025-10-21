/**
 * Copyright (C) 2015 Johannes Taelman
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

import java.util.logging.Level;

import axoloti.Connection;

/**
 *
 * @author Johannes Taelman
 */
public class SCmdGetFWVersion extends AbstractSCmd {
    
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
        connection.TransmitGetFWVersion();

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "Get firmware version command timed out.");
                setCompletedWithStatus(1);
                return this;
            }
            else if (!isSuccessful()) {
                LOGGER.log(Level.SEVERE, "Failed to get firmware version.");
                return this;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Get firmware version command interrupted: " +  e.getMessage());
            e.printStackTrace(System.out);
            setCompletedWithStatus(1);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during get firmware version command: " + e.getMessage());
            e.printStackTrace(System.out);
            setCompletedWithStatus(1);
        }
        return this;
    }
}
