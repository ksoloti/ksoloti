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
package axoloti.parameters;

import axoloti.datatypes.ValueInt32;
import java.security.MessageDigest;
import java.util.List;
import org.simpleframework.xml.Element;

/**
 *
 * @author Johannes Taelman
 */
public class ParameterInt32BoxSmall extends Parameter<ParameterInstanceInt32BoxSmall> {

    @Element
    public ValueInt32 MinValue;
    @Element
    public ValueInt32 MaxValue;

    public ParameterInt32BoxSmall() {
        this.MinValue = new ValueInt32(0);
        this.MaxValue = new ValueInt32(65535);
    }

    public ParameterInt32BoxSmall(String name, int MinValue, int MaxValue) {
        super(name);
        this.MinValue = new ValueInt32(MinValue);
        this.MaxValue = new ValueInt32(MaxValue);
    }

    @Override
    public ParameterInstanceInt32BoxSmall InstanceFactory() {
        ParameterInstanceInt32BoxSmall b = new ParameterInstanceInt32BoxSmall();
        b.min = MinValue.getInt();
        b.max = MaxValue.getInt();
        return b;
    }

    @Override
    public void updateSHA(MessageDigest md) {
        super.updateSHA(md);
        md.update("int32.dial.small".getBytes());
    }

    static public final String TypeName = "int32.mini";

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<String> getEditableFields() {
        List<String> l = super.getEditableFields();
        l.add("MinValue");
        l.add("MaxValue");
        return l;
    }
}
