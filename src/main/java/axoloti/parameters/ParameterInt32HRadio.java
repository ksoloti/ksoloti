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
package axoloti.parameters;

import axoloti.datatypes.ValueInt32;
import java.security.MessageDigest;
import java.util.List;
import org.simpleframework.xml.Element;

/**
 *
 * @author Johannes Taelman
 */
public class ParameterInt32HRadio extends Parameter<ParameterInstanceInt32HRadio> {

    @Element
    public ValueInt32 MaxValue;

    public ParameterInt32HRadio() {
        this.MaxValue = new ValueInt32(2);
    }

    public ParameterInt32HRadio(String name, int MinValue, int MaxValue) {
        super(name);
        this.MaxValue = new ValueInt32(MaxValue);
    }

    @Override
    public ParameterInstanceInt32HRadio InstanceFactory() {
        ParameterInstanceInt32HRadio b = new ParameterInstanceInt32HRadio();
        return b;
    }

    @Override
    public void updateSHA(MessageDigest md) {
        super.updateSHA(md);
        md.update(("int32.hradio" + MaxValue.getInt()).getBytes());
    }

    static public final String TypeName = "int32.hradio";

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<String> getEditableFields() {
        List<String> l = super.getEditableFields();
        l.add("MaxValue");
        return l;
    }
}
