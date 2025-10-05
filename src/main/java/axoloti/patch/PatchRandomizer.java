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

import javax.swing.SwingWorker;

import java.util.Map;
import java.lang.Math;

import axoloti.Patch;
import axoloti.datatypes.Value;
import axoloti.datatypes.ValueFrac32;
import axoloti.datatypes.ValueInt32;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.parameters.ParameterInstance;
import components.control.ACtrlComponent;
import components.control.Checkbox4StatesComponent;
import components.control.CheckboxComponent;
import components.control.HRadioComponent;
import components.control.VRadioComponent;

/**
 * 
 * @author Ksoloti
 */
public class PatchRandomizer {
    private static final Logger LOGGER = Logger.getLogger(PatchRandomizer.class.getName());
    private static Random random = new Random(Instant.EPOCH.getEpochSecond());

    private static class RandomizationWorker extends SwingWorker<Void, String> {
        private final List<ParameterInstance> selectedParameters;
        private final float factor;

        public RandomizationWorker(List<ParameterInstance> selectedParameters, float factor) {
            this.selectedParameters = selectedParameters;
            this.factor = factor;
        }

        @Override
        protected Void doInBackground() throws Exception {

            publish("INFO: Randomizing " + selectedParameters.size() + " selected parameter(s) by " + (int)(factor * 100) + "%");

            for (ParameterInstance param : selectedParameters) {
                if (isCancelled()) {
                    break;
                }

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

                    String logMessage = "INFO: Randomize " + param.GetObjectInstance().getInstanceName() + ":" + param.GetCName() + " (" + String.format("%+.2f", vllog.getDouble() - oldValue) + ") to " + paramVal;

                    publish(logMessage); 
                } else {
                    publish("INFO: Skipping frozen parameter: " + param.GetObjectInstance().getInstanceName() + ":" + param.getName());
                }
            }

            return null;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String logMessage : chunks) {
                if (logMessage.startsWith("INFO: ")) {
                    LOGGER.log(Level.INFO, logMessage.substring(6)); 
                }
            }
        }

        @Override
        protected void done() {
            LOGGER.log(Level.INFO, "Randomization finished.");
        }
    }

    public static void randomizeAllParameters(Patch patch, float factor) { /* factor: for example 0.25f equals 25% */
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
        if (selectedParameters == null || selectedParameters.isEmpty()) {
            return;
        }

        new RandomizationWorker(selectedParameters, factor).execute();
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

    private static int getPackedBitCount(ParameterInstance param) {
        String paramInstanceClassName = param.getClass().getSimpleName();
        ACtrlComponent component = param.getControlComponent();

        if (paramInstanceClassName.startsWith("ParameterInstanceBin")) {
            if (component instanceof CheckboxComponent) {
                return ((CheckboxComponent) component).getN();
            }
        }

        if (paramInstanceClassName.equals("ParameterInstance4LevelX16")) {
            if (component instanceof Checkbox4StatesComponent) {
                return ((Checkbox4StatesComponent) component).getN() * 2;
            }
        }

        return 0;
    }

    private static double mutatePackedBinaryValue(ParameterInstance param, float factor) {
        String paramInstanceClassName = param.getClass().getSimpleName();
        int packedValue = param.getValue().getInt();
        int newPackedValue = packedValue;

        if (paramInstanceClassName.startsWith("ParameterInstanceBin")) {
            int totalBits = getPackedBitCount(param);
            for (int i = 0; i < totalBits; i++) {
                if (random.nextDouble() <= factor) { 
                    int newState = random.nextInt(2); 
                    int mask = (1 << i);
                    newPackedValue &= ~mask; 
                    if (newState == 1) {
                        newPackedValue |= mask;
                    }
                }
            }
        }
        else if (paramInstanceClassName.equals("ParameterInstance4LevelX16")) {
            Checkbox4StatesComponent control = (Checkbox4StatesComponent) param.getControlComponent();

            int numSwitches = control.getN(); 

            for (int i = 0; i < numSwitches; i++) {
                if (random.nextDouble() <= factor) {
                    int randomLevel = random.nextInt(4);
                    int shift = i * 2;
                    int clearMask = ~(0b11 << shift);
                    newPackedValue &= clearMask;
                    newPackedValue |= (randomLevel << shift);
                }
            }
        }

        return (double) newPackedValue;
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

        if (isParameterInstanceBin1 && (param.getValue() instanceof ValueInt32)) {
            if (random.nextDouble() <= factor) {
                return (random.nextDouble() < 0.5) ? 1.0 : 0.0;
            } 
            else {
                return currentValue;
            }
        }

        if (paramInstanceClassName.startsWith("ParameterInstanceBin") ||
            paramInstanceClassName.equals("ParameterInstance4LevelX16")) {

            return mutatePackedBinaryValue(param, factor);
        }

        if (paramInstanceClassName.equals("ParameterInstanceInt32Selection") ||
            param.getControlComponent() instanceof HRadioComponent ||
            param.getControlComponent() instanceof VRadioComponent) {

            int numOptions;

            if (param.getControlComponent() instanceof HRadioComponent) {
                numOptions = ((HRadioComponent) param.getControlComponent()).getN();
            } else if (param.getControlComponent() instanceof VRadioComponent) {
                numOptions = ((VRadioComponent) param.getControlComponent()).getN();
            } else {
                return currentValue;
            }

            if (random.nextDouble() <= factor) {
                int range = (int) Math.round(numOptions * factor);
                if (range < 2) range = 2;

                int newRawValue;

                if (factor >= 1.0f) {
                    newRawValue = random.nextInt(numOptions);
                } else {
                    int currentRawValue = (int) Math.round(currentValue);
                    int minBound = Math.max(0, currentRawValue - range / 2);
                    int maxBound = Math.min(numOptions - 1, currentRawValue + range / 2);
                    int actualRange = maxBound - minBound + 1;

                    newRawValue = random.nextInt(actualRange) + minBound;
                }

                if (numOptions > 1 && newRawValue == (int) Math.round(currentValue)) {
                    newRawValue = (newRawValue + 1) % numOptions;
                }

                return (double) newRawValue;
            }

            return currentValue; 
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