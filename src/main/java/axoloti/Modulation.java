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
package axoloti;

import axoloti.datatypes.ValueFrac32;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.parameters.ParameterInstanceFrac32;
import org.simpleframework.xml.Attribute;

/**
 *
 * @author Johannes Taelman
 */
public class Modulation {

    @Attribute
    public String sourceName; // object instance name

    @Attribute(required = false)
    public String modName; // name of modulation (can be null or empty)

    @Attribute(name = "value", required = false)
    public double getValueForXml() {
        return value.getDouble();
    }

    final ValueFrac32 value = new ValueFrac32();

    public Modulation(@Attribute(name = "value") double v) {
        value.setDouble(v);
    }

    public Modulation() {
    }

    public ValueFrac32 getValue() {
        return value;
    }

    public void PostConstructor(ParameterInstanceFrac32 p) {
        System.out.println("Modulation postconstructor");
        destination = p;
        source = p.GetObjectInstance().patch.GetObjectInstance(sourceName);
        if (source == null) {
            System.out.println("modulation source missing!");
        } else {
            System.out.println("modulation source found " + source.getInstanceName());
        }
        Modulator m = null;
        for (Modulator m1 : p.GetObjectInstance().patch.Modulators) {
            System.out.println("modulator match? " + m1.getObjInst().getInstanceName());
            if (m1.getObjInst() == source) {
                if ((m1.getName() != null) && (!m1.getName().isEmpty())) {
                    if (m1.getName().equals(modName)) {
                        m = m1;
                    }
                } else {
                    m = m1;
                }
            }
        }
        if (m == null) {
            System.out.println("Modulation source missing");
        } else {
            if (m.getModulationList() == null) {
                m.resetModulationList();
            }
            if (!m.getModulationList().contains(this)) {
                m.getModulationList().add(this);
            }
        }
    }
    public AxoObjectInstanceAbstract source;
    public ParameterInstanceFrac32 destination;
}
