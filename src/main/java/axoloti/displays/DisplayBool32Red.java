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
package axoloti.displays;

import axoloti.datatypes.Int32;
import java.security.MessageDigest;

/**
 *
 * @author Johannes Taelman
 */
public class DisplayBool32Red extends Display {

    public DisplayBool32Red() {
    }

    public DisplayBool32Red(String name) {
        super(name);
    }

    @Override
    public DisplayInstanceBool32Red InstanceFactory() {
        return new DisplayInstanceBool32Red();
    }

    @Override
    public void updateSHA(MessageDigest md) {
        super.updateSHA(md);
        md.update("bool32.red".getBytes());
    }

    @Override
    public Int32 getDatatype() {
        return Int32.d;
    }

    static public final String TypeName = "bool32.red";

    @Override
    public String getTypeName() {
        return TypeName;
    }

}
