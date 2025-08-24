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
package qcmds;

import axoloti.Patch;
import axoloti.parameters.ParameterInstance;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdGuiDialTx implements QCmdGUITask {

    private final Patch patch;

    public QCmdGuiDialTx(Patch patch) {
        this.patch = patch;
    }

    @Override
    public String GetStartMessage() {
        return null;
    }

    @Override
    public String GetDoneMessage() {
        return null;
    }

    @Override
    public void DoGUI(QCmdProcessor processor) {
        if (processor.isQueueEmpty()) {
            if (this.patch != null) {
                for (ParameterInstance p : this.patch.getParameterInstances()) {
                    if (p.GetNeedsTransmit()) {
                        if (processor.hasQueueSpaceLeft()) {
                            processor.AppendToQueue(new QCmdSerialDialTX(p.TransmitParamData()));
                        } else {
                            break;
                        }
                    }
                }
                if (this.patch.presetUpdatePending && processor.hasQueueSpaceLeft()) {
                    byte pb[] = new byte[this.patch.getSettings().GetNPresets() * this.patch.getSettings().GetNPresetEntries() * 8];
                    int p = 0;
                    for (int i = 0; i < this.patch.getSettings().GetNPresets(); i++) {
                        int pi[] = this.patch.DistillPreset(i + 1);
                        for (int j = 0; j < this.patch.getSettings().GetNPresetEntries() * 2; j++) {
                            pb[p++] = (byte) (pi[j]);
                            pb[p++] = (byte) (pi[j] >> 8);
                            pb[p++] = (byte) (pi[j] >> 16);
                            pb[p++] = (byte) (pi[j] >> 24);
                        }
                    }
                    processor.AppendToQueue(new QCmdUpdatePreset(pb));
                    this.patch.presetUpdatePending = false;
                }
            }
        }
    }
}
