package axoloti.patch;

import java.time.Instant;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.Patch;
import axoloti.datatypes.Value;
import axoloti.datatypes.ValueFrac32;
import axoloti.datatypes.ValueInt32;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.parameters.ParameterInstance;

public class PatchRandomizer {
    private static final Logger LOGGER = Logger.getLogger(PatchRandomizer.class.getName());
    private static Random random = new Random(Instant.EPOCH.getEpochSecond());

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
}