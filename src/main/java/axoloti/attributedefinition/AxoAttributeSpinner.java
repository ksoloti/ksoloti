/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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
package axoloti.attributedefinition;

import axoloti.attribute.AttributeInstanceSpinner;
import axoloti.object.AxoObjectInstance;
import java.util.List;
import org.simpleframework.xml.Attribute;

/**
 *
 * @author Johannes Taelman
 */
public class AxoAttributeSpinner extends AxoAttribute {

    @Attribute
    public int MinValue;
    @Attribute
    public int MaxValue;
    @Attribute
    public int DefaultValue;

    public AxoAttributeSpinner() {
    }

    public AxoAttributeSpinner(String name, int MinValue, int MaxValue, int DefaultValue) {
        super(name);
        this.MinValue = MinValue;
        this.MaxValue = MaxValue;
        this.DefaultValue = DefaultValue;
    }

    public int getMinValue() {
        return MinValue;
    }

    public int getMaxValue() {
        return MaxValue;
    }

    public int getDefaultValue() {
        return DefaultValue;
    }

    @Override
    public AttributeInstanceSpinner InstanceFactory(AxoObjectInstance o) {
        return new AttributeInstanceSpinner(this, o);
    }

    static public final String TypeName = "spinner";

    @Override
    public String getTypeName() {
        return TypeName;
    }
    
    @Override
    public List<String> getEditableFields(){
        List<String> l = super.getEditableFields();
        l.add("MinValue");
        l.add("MaxValue");
        l.add("DefaultValue");
        return l;
    }
    
}
