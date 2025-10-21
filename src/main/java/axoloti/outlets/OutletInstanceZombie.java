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
package axoloti.outlets;

import axoloti.datatypes.DataType;
import axoloti.object.AxoObjectInstanceZombie;
import components.LabelComponent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;

/**
 *
 * @author Johannes Taelman
 */
public class OutletInstanceZombie extends OutletInstance {

    public OutletInstanceZombie() {
    }

    public OutletInstanceZombie(AxoObjectInstanceZombie obj, String name) {
        this.axoObj = obj;
        this.outletname = name;
        this.objname = obj.getInstanceName();

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setAlignmentX(LEFT_ALIGNMENT);

        add(Box.createHorizontalGlue());

        LabelComponent olbl = new LabelComponent(this.outletname);
        olbl.setHorizontalAlignment(SwingConstants.RIGHT);
        add(olbl);

        add(Box.createHorizontalStrut(2));

        jack = new components.JackOutputComponent(this);
        add(jack);

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public DataType GetDataType() {
        return new axoloti.datatypes.DTZombie();
    }

    @Override
    public String GetLabel() {
        return outletname;
    }

}
