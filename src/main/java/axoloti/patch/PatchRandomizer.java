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
import java.util.Map;
import java.lang.Math;

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
     * @param factor The percentage to apply randomization by.
     */
    public static void randomizeAllParameters(Patch patch, float factor) { /* factor: 0.25f equals 25% */
        if (patch.objectInstances.size() > 0) {
            for (AxoObjectInstanceAbstract obj : patch.objectInstances) {
                for (ParameterInstance param : obj.getParameterInstances()) {
                    if (!param.isFrozen()) {
                        
                        // 1. Calculate the new nominal value
                        double mutatedNominalValue = getMutatedNominalValue(param, factor);

                        // 2. Create the appropriate Value object (assuming a helper exists)
                        Value newValue;
                        if (param.getValue() instanceof ValueInt32) {
                            newValue = new ValueInt32((int) mutatedNominalValue);
                        } else if (param.getValue() instanceof ValueFrac32) {
                             newValue = new ValueFrac32(mutatedNominalValue);
                        } else {
                             // Skip or handle other types
                             continue;
                        }
                        
                        // 3. Set the new value, allowing the parameter to handle the raw conversion!
                        param.setValue(newValue); 
                        param.SetNeedsTransmit(true);
                        String paramVal = "";
                        Value vllog = param.getValue();
                        if (vllog instanceof ValueFrac32) {
                            paramVal += String.format("%.2f", vllog.getDouble());
                        }
                        else if (vllog instanceof ValueInt32) {
                            paramVal += vllog.getInt();
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
     * @param factor The percentage to apply randomization by.
     */
    public static void randomizeParameters(List<ParameterInstance> selectedParameters, float factor) {
        if (selectedParameters != null && !selectedParameters.isEmpty()) {
            for (ParameterInstance param : selectedParameters) {
                if (!param.isFrozen()) {
                    
                    // --- FIX BEGINS HERE ---
                    
                    // 1. Calculate the new nominal value using constraints (0.0 to 1.0, -64.0 to 64.0, etc.)
                    double mutatedNominalValue = getMutatedNominalValue(param, factor);

                    // 2. Create the appropriate Value object (must be done outside of ParameterInstance)
                    Value newValue;
                    if (param.getValue() instanceof ValueInt32) {
                        // Cast to int for ValueInt32 constructor
                        newValue = new ValueInt32((int) mutatedNominalValue); 
                    } else if (param.getValue() instanceof ValueFrac32) {
                        // Use the double directly for ValueFrac32 constructor
                         newValue = new ValueFrac32(mutatedNominalValue);
                    } else {
                         // Skip other types (Bool32, etc.) or just use the current value
                         newValue = param.getValue();
                    }
                    
                    // 3. Set the new value, allowing the parameter to handle the raw conversion
                    param.setValue(newValue); 
                    param.SetNeedsTransmit(true);
                    
                    // --- FIX ENDS HERE ---
    
                    String paramVal = "";
                    Value vllog = param.getValue();
                    if (vllog instanceof ValueFrac32) {
                        paramVal += String.format("%.2f", vllog.getDouble());
                    }
                    else if (vllog instanceof ValueInt32) {
                        paramVal += vllog.getInt();
                    }
                    LOGGER.log(Level.INFO, "Randomize " + param.GetObjectInstance().getInstanceName() + ":" + param.getName() + " " + paramVal);
                } else {
                    LOGGER.log(Level.INFO, "Skipping frozen parameter: " + param.GetObjectInstance().getInstanceName() + ":" + param.getName());
                }
            }
        }
    }
    
    /**
     * Randomizes a list of selected parameters with a mutation constrained by a min/max value.
     * @param selectedParameters The list of parameters to randomize.
     * @param constraints A map from ParameterInstance to an int[] with [min, max] values.
     * @param factor The percentage of the constrained range to use for mutation.
     */
    public static void randomizeParametersWithConstraint(List<ParameterInstance> selectedParameters, Map<ParameterInstance, double[]> constraints, float factor) {
        if (selectedParameters == null || selectedParameters.isEmpty()) {
            return;
        }

        Random random = new Random();

        for (ParameterInstance param : selectedParameters) {
            if (!param.isFrozen() && constraints.containsKey(param)) {
                // Get the nominal double constraints directly from the map
                double[] nominalMinMax = constraints.get(param);
                
                // 1. Get Limits and Current Value in Nominal (double) domain
                double minNominalConstraint = nominalMinMax[0];
                double maxNominalConstraint = nominalMinMax[1];
                double currentNominalValue = param.getValue().getDouble(); // NO GetValueRaw()

                // Ensure minConstraint is truly the minimum (in case the Variations were selected in reverse)
                double actualMin = Math.min(minNominalConstraint, maxNominalConstraint);
                double actualMax = Math.max(minNominalConstraint, maxNominalConstraint);

                // 2. Perform all calculation in the NOMINAL DOUBLE domain
                double fullRange = actualMax - actualMin;
                
                // Calculate the nominal mutation bounds
                double mutationAmount = fullRange * factor;
                double mutationHalf = mutationAmount / 2.0;
                
                double tempMin = currentNominalValue - mutationHalf;
                double tempMax = currentNominalValue + mutationHalf;

                // Constrain the mutation to the interpolation range
                double constrainedMin = Math.max(actualMin, tempMin);
                double constrainedMax = Math.min(actualMax, tempMax);

                double range = constrainedMax - constrainedMin;
                if (range <= 0.0) {
                    continue;
                }
                
                // 3. Calculate the final NOMINAL DOUBLE value
                double mutatedNominalValue = random.nextDouble() * range + constrainedMin;

                // 4. Handle Int32 Discreteness and create/set the Value object
                Value vl = param.getValue();
                Value newValue;
                
                if (vl instanceof ValueInt32) {
                    // Round and clamp for integer types
                    mutatedNominalValue = Math.round(mutatedNominalValue);
                    mutatedNominalValue = Math.min(Math.max(mutatedNominalValue, actualMin), actualMax);
                    newValue = new ValueInt32((int) mutatedNominalValue); 
                } else if (vl instanceof ValueFrac32) {
                    newValue = new ValueFrac32(mutatedNominalValue);
                } else {
                    continue;
                }
                
                // Apply the change using the proper setter
                param.setValue(newValue);
                param.SetNeedsTransmit(true);
    
                String paramVal = "";
                Value vllog = param.getValue();
                if (vllog instanceof ValueFrac32) {
                    paramVal += String.format("%.2f", vllog.getDouble());
                }
                else if (vllog instanceof ValueInt32) {
                    paramVal += vllog.getInt();
                }
                LOGGER.log(Level.INFO, "Constrained Randomize " + param.GetObjectInstance().getInstanceName() + ":" + param.getName() + " " + paramVal);
            } else if (param.isFrozen()) {
                 LOGGER.log(Level.INFO, "Skipping frozen parameter: " + param.GetObjectInstance().getInstanceName() + ":" + param.getName());
            } else {
                 LOGGER.log(Level.INFO, "Skipping parameter with no constraint data: " + param.GetObjectInstance().getInstanceName() + ":" + param.getName());
            }
        }
    }

    private static double getMutatedNominalValue(ParameterInstance param, float factor) {
        // 1. Get Limits and Current Value in Nominal (double) domain
        double minConstraint = param.getControlComponent().getMin();
        double maxConstraint = param.getControlComponent().getMax();
        double currentValue = param.getValue().getDouble(); // Use the nominal double value

        if (param.getValue() instanceof ValueInt32 && minConstraint == 0.0 && maxConstraint == 1.0) {
            
            // If the random roll is less than or equal to the percentage (e.g., 0.10 for 10%)
            if (random.nextDouble() <= factor) {
                // FLIP THE STATE: 0.0 becomes 1.0, 1.0 becomes 0.0
                return (currentValue == 0.0) ? 1.0 : 0.0;
            } else {
                // KEEP THE CURRENT STATE
                return currentValue;
            }
        }

        // 2. Define the Mutation Range based on percentage (Standard Logic for all others)
        double fullRange = maxConstraint - minConstraint;
        
        // Calculate the maximum deviation allowed (e.g., +/- 12.5% of the full range)
        double maxDeviation = fullRange * factor / 2.0; 

        double lowerBound = Math.max(minConstraint, currentValue - maxDeviation);
        double upperBound = Math.min(maxConstraint, currentValue + maxDeviation);

        // 3. Generate the Mutated Value (simple linear random choice within bounds)
        double range = upperBound - lowerBound;
        double mutatedValue = lowerBound;
        
        if (range > 0) {
            // Generates a random double between 0.0 (inclusive) and range (exclusive)
            mutatedValue = random.nextDouble() * range + lowerBound;
        }
        
        // 4. Handle Int32 Discreteness (Rounding for multi-step selectors, e.g., 0 to 4)
        if (param.getValue() instanceof ValueInt32) {
            // NOTE: This logic block now ONLY runs for ValueInt32 where maxConstraint > 1.0
            
            // For integer parameters, the final nominal value must be rounded to the nearest integer.
            mutatedValue = Math.round(mutatedValue);
            
            // Final clamp just in case rounding pushed it out
            mutatedValue = Math.min(Math.max(mutatedValue, minConstraint), maxConstraint);
        }
        
        return mutatedValue;
    }
}