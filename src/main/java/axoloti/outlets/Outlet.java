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
package axoloti.outlets;

import axoloti.atom.AtomDefinition;
import axoloti.atom.AtomInstance;
import axoloti.datatypes.DataType;
import axoloti.datatypes.SignalMetaData;
import axoloti.object.AxoObjectInstance;
import static axoloti.utils.CharEscape.charEscape;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.simpleframework.xml.Attribute;

/**
 *
 * @author Johannes Taelman
 */
public abstract class Outlet implements AtomDefinition, Cloneable {

    @Attribute
    String name;
    @Attribute(required = false)
    public String description;
    @Deprecated
    @Attribute(required = false)
    Boolean SumBuffer;
    @Attribute(required = false)
    public Boolean noLabel;

    public DataType getDatatype() {
        return null;
    }

    public Outlet() {
    }

    public Outlet(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return getTypeName();
    }

    public String GetCName() {
        return "outlet_" + charEscape(name);
    }

    SignalMetaData GetSignalMetaData() {
        return SignalMetaData.none;
    }

    public void updateSHA(MessageDigest md) {
        md.update(name.getBytes());
        md.update((byte) getDatatype().hashCode());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Boolean getNoLabel() {
        if (noLabel == null) {
            return false;
        }
        return noLabel;
    }

    @Override
    public void setNoLabel(Boolean noLabel) {
        if (noLabel != null && noLabel == true) {
            this.noLabel = true;
        }
        else {
            this.noLabel = null;
        }
    }


    @Override
    public AtomInstance CreateInstance(AxoObjectInstance o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Outlet clone() {
        try {
            Outlet clonedOutlet = (Outlet) super.clone();
            return clonedOutlet;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public List<String> getEditableFields() {
        return new ArrayList<String>();
    }
}
