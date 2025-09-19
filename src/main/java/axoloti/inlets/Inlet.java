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
package axoloti.inlets;

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
public abstract class Inlet implements AtomDefinition, Cloneable {

    @Attribute
    String name;
    @Attribute(required = false)
    public String description;
    @Attribute(required = false)
    public Boolean noLabel;

    public Inlet() {
    }

    public Inlet(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return getTypeName();
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


    public String GetCName() {
        return "inlet_" + charEscape(name);
    }

    @Override
    public AtomInstance CreateInstance(AxoObjectInstance o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public abstract DataType getDatatype();

    SignalMetaData GetSignalMetaData() {
        return SignalMetaData.none;
    }

    public void updateSHA(MessageDigest md) {
        md.update(name.getBytes());
        md.update((byte) getDatatype().hashCode());
    }

    @Override
    public Inlet clone() {
        try {
            Inlet clonedInlet = (Inlet) super.clone();
            return clonedInlet;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public List<String> getEditableFields(){
        return new ArrayList<String>();
    }
}
