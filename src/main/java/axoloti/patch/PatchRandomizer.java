/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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

package axoloti.patch;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.Patch;
import axoloti.datatypes.Value;
import axoloti.datatypes.ValueFrac32;
import axoloti.datatypes.ValueInt32;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.parameters.ParameterInstance;

/**
 * 
 * @author Ksoloti
 */
public class PatchRandomizer {
    private static final Logger LOGGER = Logger.getLogger(PatchRandomizer.class.getName());
    private static Random random = new Random(Instant.EPOCH.getEpochSecond());

    /**
     * Randomizes all non-frozen parameters in a patch.
     * @param patch The patch to randomize.
     * @param percent The percentage to apply randomization by.
     */
    public static void randomizeAllParameters(Patch patch, float percent) { /* percent: 0.25f equals 25% */
        if (patch.objectInstances.size() > 0) {
            for (AxoObjectInstanceAbstract obj : patch.objectInstances) {
                for (ParameterInstance param : obj.getParameterInstances()) {
                    if (!param.isFrozen()) {
                        int randomValue = (int)(random.nextInt() * percent)/16;
                        param.SetValueRaw(param.GetValueRaw() + randomValue);
                        param.SetNeedsTransmit(true);

                        String paramVal = "";
                        Value vl = param.getValue();
                        if (vl instanceof ValueFrac32) {
                            paramVal += String.format("%.2f", vl.getDouble());
                        }
                        else if (vl instanceof ValueInt32) {
                            paramVal += vl.getInt();
                        }
                        LOGGER.log(Level.INFO, "Randomize " + obj.getCInstanceName() + ":" + param.GetCName() + " " + paramVal);
                    }
                }
            }
            LOGGER.log(Level.INFO, "");
            patch.SetDirty(true);
        }
    }

    /**
     * Randomizes a list of selected parameters.
     * @param selectedParameters The list of parameters to randomize.
     * @param percent The percentage to apply randomization by.
     */
    public static void randomizeParameters(List<ParameterInstance> selectedParameters, float percent) {
        if (selectedParameters != null && !selectedParameters.isEmpty()) {
            for (ParameterInstance param : selectedParameters) {
                if (!param.isFrozen()) {
                    int randomValue = (int)(random.nextInt() * percent)/16;
                    param.SetValueRaw(param.GetValueRaw() + randomValue);
                    param.SetNeedsTransmit(true);
    
                    String paramVal = "";
                    Value vl = param.getValue();
                    if (vl instanceof ValueFrac32) {
                        paramVal += String.format("%.2f", vl.getDouble());
                    }
                    else if (vl instanceof ValueInt32) {
                        paramVal += vl.getInt();
                    }
                    LOGGER.log(Level.INFO, "Randomize " + param.GetObjectInstance().getInstanceName() + ":" + param.getName() + " " + paramVal);
                } else {
                    LOGGER.log(Level.INFO, "Skipping frozen parameter: " + param.GetObjectInstance().getInstanceName() + ":" + param.getName());
                }
            }
        }
    }
}
