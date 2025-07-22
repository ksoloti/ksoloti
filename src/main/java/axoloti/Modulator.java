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
package axoloti;

import axoloti.object.AxoObjectInstanceAbstract;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Johannes Taelman
 */
public class Modulator {

    private String name;
    private AxoObjectInstanceAbstract objInst;
    private ArrayList<Modulation> Modulations = new ArrayList<Modulation>();

    public String getName() {
        return name;
    }

    public void setName(String str) {
        if (str != null) {
            name = str;
        }
    }

    public AxoObjectInstanceAbstract getObjInst() {
        return objInst;
    }

    public void setObjInst(AxoObjectInstanceAbstract oia) {
        objInst = oia;
    }

    public ArrayList<Modulation> getModulationList() {
        return Modulations;
    }

    public void resetModulationList() {
            Modulations = new ArrayList<Modulation>();
    }

    public Modulation getModulation(int index) {
        return Modulations.get(index);
    }

    public void addModulation(Modulation modulation) {
        if (modulation != null && !Modulations.contains(modulation)) {
            Modulations.add(modulation);
        }
    }

    public void removeModulation(Modulation modulation) {
        Modulations.remove(modulation);
    }

    public String getCName() {
        if ((name != null) && (!name.isEmpty())) {
            return "MODULATOR_" + objInst.getCInstanceName() + "_" + name;
        } else {
            return "MODULATOR_" + objInst.getCInstanceName();
        }
    }
}
