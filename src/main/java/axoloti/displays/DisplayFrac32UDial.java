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
package axoloti.displays;

import axoloti.datatypes.Int32;
import java.security.MessageDigest;

/**
 *
 * @author Johannes Taelman
 */
public class DisplayFrac32UDial extends Display {

    public DisplayFrac32UDial() {
    }

    public DisplayFrac32UDial(String name) {
        super(name);
    }

    @Override
    public DisplayInstanceFrac32UDial InstanceFactory() {
        return new DisplayInstanceFrac32UDial();
    }

    @Override
    public void updateSHA(MessageDigest md) {
        super.updateSHA(md);
        md.update("frac32.u.dial".getBytes());
    }

    @Override
    public Int32 getDatatype() {
        return Int32.d;
    }

    static public final String TypeName = "frac32.u.dial";

    @Override
    public String getTypeName() {
        return TypeName;
    }

}
