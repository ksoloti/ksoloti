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

    public static void randomizeAllParameters(Patch patch, float factor) { /* factor: 0.25f equals 25% */
        if (patch.objectInstances.size() > 0) {
            for (AxoObjectInstanceAbstract obj : patch.objectInstances) {
                for (ParameterInstance param : obj.getParameterInstances()) {
                    if (!param.isFrozen()) {

                        double mutatedNominalValue = getMutatedNominalValue(param, factor);

                        double oldValue = param.getValue().getDouble();
                        Value newValue;
                        if (param.getValue() instanceof ValueInt32) {
                            newValue = new ValueInt32((int) mutatedNominalValue);
                        } else if (param.getValue() instanceof ValueFrac32) {
                             newValue = new ValueFrac32(mutatedNominalValue);
                        } else {
                             continue;
                        }

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
                        LOGGER.log(Level.INFO, "Randomize " + obj.getCInstanceName() + ":" + param.GetCName() + " (" + String.format("%+.2f", vllog.getDouble() - oldValue) + ") to " + paramVal);
                    }
                }
            }
            LOGGER.log(Level.INFO, "");
            patch.SetDirty(true);
        }
    }

    public static void randomizeParameters(List<ParameterInstance> selectedParameters, float factor) {
        if (selectedParameters != null && !selectedParameters.isEmpty()) {
            for (ParameterInstance param : selectedParameters) {
                if (!param.isFrozen()) {

                    double mutatedNominalValue = getMutatedNominalValue(param, factor);
                    double oldValue = param.getValue().getDouble();
                    Value newValue;
                    if (param.getValue() instanceof ValueInt32) {
                        newValue = new ValueInt32((int) mutatedNominalValue); 
                    } else if (param.getValue() instanceof ValueFrac32) {
                         newValue = new ValueFrac32(mutatedNominalValue);
                    } else {
                         newValue = param.getValue();
                    }

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
                    LOGGER.log(Level.INFO, "Randomize " + param.GetObjectInstance().getInstanceName() + ":" + param.GetCName() + " (" + String.format("%+.2f", vllog.getDouble() - oldValue) + ") to " + paramVal);
                } else {
                    LOGGER.log(Level.INFO, "Skipping frozen parameter: " + param.GetObjectInstance().getInstanceName() + ":" + param.getName());
                }
            }
        }
    }

    public static void randomizeParametersWithConstraint(List<ParameterInstance> selectedParameters, Map<ParameterInstance, double[]> constraints, float factor) {
        if (selectedParameters == null || selectedParameters.isEmpty()) {
            return;
        }

        Random random = new Random();

        for (ParameterInstance param : selectedParameters) {
            if (!param.isFrozen() && constraints.containsKey(param)) {
                double[] nominalMinMax = constraints.get(param);

                double minNominalConstraint = nominalMinMax[0];
                double maxNominalConstraint = nominalMinMax[1];
                double currentNominalValue = param.getValue().getDouble();
                double actualMin = Math.min(minNominalConstraint, maxNominalConstraint);
                double actualMax = Math.max(minNominalConstraint, maxNominalConstraint);
                double fullRange = actualMax - actualMin;
                double mutationAmount = fullRange * factor;
                double mutationHalf = mutationAmount / 2.0;
                double tempMin = currentNominalValue - mutationHalf;
                double tempMax = currentNominalValue + mutationHalf;
                double constrainedMin = Math.max(actualMin, tempMin);
                double constrainedMax = Math.min(actualMax, tempMax);

                double range = constrainedMax - constrainedMin;
                if (range <= 0.0) {
                    continue;
                }

                double mutatedNominalValue = random.nextDouble() * range + constrainedMin;

                Value vl = param.getValue();
                Value newValue;

                if (vl instanceof ValueInt32) {
                    mutatedNominalValue = Math.round(mutatedNominalValue);
                    mutatedNominalValue = Math.min(Math.max(mutatedNominalValue, actualMin), actualMax);
                    newValue = new ValueInt32((int) mutatedNominalValue); 
                } else if (vl instanceof ValueFrac32) {
                    newValue = new ValueFrac32(mutatedNominalValue);
                } else {
                    continue;
                }

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
        double minConstraint = param.getControlComponent().getMin();
        double maxConstraint = param.getControlComponent().getMax();
        double currentValue = param.getValue().getDouble();
        
        String paramInstanceClassName = param.getClass().getSimpleName();

        if (paramInstanceClassName.equals("ParameterInstanceBin1Momentary")) {
            return currentValue;
        }
        
        boolean isParameterInstanceBin1 = paramInstanceClassName.equals("ParameterInstanceBin1");

        if (isParameterInstanceBin1 && minConstraint == 0.0 && maxConstraint == 1.0) {
            
            if (random.nextDouble() <= factor) {
                return 1.0 - currentValue;
            } else {
                return currentValue;
            }
        }
        
        double fullRange = maxConstraint - minConstraint;
        double totalRandomRange = fullRange * factor;
        
        double centerValue = (maxConstraint + minConstraint) / 2.0;
        
        double halfRandomRange = totalRandomRange / 2.0;

        double lowerBound = centerValue - halfRandomRange;
        double upperBound = centerValue + halfRandomRange;
        
        lowerBound = Math.max(minConstraint, lowerBound);
        upperBound = Math.min(maxConstraint, upperBound);

        if (factor >= 1.0f) {
            lowerBound = minConstraint;
            upperBound = maxConstraint;
        } else {
            double maxDeviation = fullRange * factor / 2.0; 

            lowerBound = Math.max(minConstraint, currentValue - maxDeviation);
            upperBound = Math.min(maxConstraint, currentValue + maxDeviation);
        }
        
        double range = upperBound - lowerBound;
        double mutatedValue = lowerBound;
        
        if (range > 0) {
            mutatedValue = random.nextDouble() * range + lowerBound;
        }
        
        if (param.getValue() instanceof ValueInt32) {
            mutatedValue = Math.round(mutatedValue);
            mutatedValue = Math.min(Math.max(mutatedValue, minConstraint), maxConstraint);
        }
        
        return mutatedValue;
    }
}