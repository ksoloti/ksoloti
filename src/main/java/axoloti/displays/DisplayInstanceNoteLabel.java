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
package axoloti.displays;

import java.awt.Dimension;

import axoloti.realunits.NativeToReal;
import axoloti.realunits.PitchToNote;
import components.LabelComponent;

/**
 *
 * @author Johannes Taelman
 */
public class DisplayInstanceNoteLabel extends DisplayInstanceFrac32<DisplayNoteLabel> {

    private LabelComponent readout;
    private final NativeToReal conv;

    public DisplayInstanceNoteLabel() {
        super();
        conv = new PitchToNote();
    }

    @Override
    public void PostConstructor() {
        super.PostConstructor();

        Dimension fll = new Dimension(1,0);
        add(new javax.swing.Box.Filler(fll, fll, fll));
        readout = new LabelComponent("xxxxx");
        add(readout);
        Dimension di = new Dimension(40, 13);
        readout.setMinimumSize(di);
        setPreferredSize(di);
    }

    @Override
    public void updateV() {
        readout.setText(conv.ToReal(value));
    }
}

