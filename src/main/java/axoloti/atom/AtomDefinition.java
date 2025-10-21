/**
 * Copyright (C) 2016 Johannes Taelman
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
package axoloti.atom;

import axoloti.object.AxoObjectInstance;
import java.util.List;

/**
 *
 * @author jtaelman
 */
/**
 * An Axoloti Object Definition is composed out of AtomDefinition
**/
public interface AtomDefinition {
    abstract String getName();
    abstract void setName(String name);
    abstract AtomInstance CreateInstance(AxoObjectInstance o);
    abstract String getDescription();
    abstract void setDescription(String description);
    abstract Boolean getNoLabel();
    abstract void setNoLabel(Boolean noLabel);
    abstract String getTypeName();
    abstract List<String> getEditableFields();
//    abstract public AtomDefinition Factory(String name);    
}
