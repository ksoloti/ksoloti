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
package components;

import axoloti.Modulation;
import axoloti.Modulator;
import axoloti.datatypes.ValueFrac32;
import axoloti.parameters.ParameterFrac32;
import axoloti.parameters.ParameterInstanceFrac32UMap;
import components.control.ACtrlEvent;
import components.control.ACtrlListener;
import components.control.HSliderComponent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 *
 * @author Johannes Taelman
 */
public class AssignModulatorMenuItems {

    double valueBeforeAdjustment;

    public AssignModulatorMenuItems(final ParameterInstanceFrac32UMap<ParameterFrac32> param, JComponent parent) {
        final ArrayList<HSliderComponent> hsls = new ArrayList<HSliderComponent>();

        //this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        hsls.clear();

        for (Modulator m : param.GetObjectInstance().patch.Modulators) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
            String modlabel;
            if ((m.name == null) || (m.name.isEmpty())) {
                modlabel = m.objInst.getInstanceName();
            } else {
                modlabel = m.objInst.getInstanceName() + ":" + m.name;
            }
            JLabel ml = new JLabel(modlabel + "  ");
            p.add(ml);
            p.add(Box.createGlue());
            HSliderComponent hsl = new HSliderComponent();
            if (param.getModulators() != null) {
                for (Modulation n : param.getModulators()) {
                    if (m.Modulations.contains(n)) {
                        System.out.println("modulation restored " + n.getValue().getDouble());
                        hsl.setValue(n.getValue().getDouble());
                    }
                }
            }
            hsl.addACtrlListener(new ACtrlListener() {
                @Override
                public void ACtrlAdjusted(ACtrlEvent e) {
                    int i = hsls.indexOf(e.getSource());
                    // System.out.println("ctrl " + i + parameterInstance.axoObj.patch.Modulators.get(i).objInst.InstanceName);
                    ValueFrac32 v = new ValueFrac32(((HSliderComponent) e.getSource()).getValue());
                    param.updateModulation(i, v.getDouble());
                }

                @Override
                public void ACtrlAdjustmentBegin(ACtrlEvent e) {
                    valueBeforeAdjustment = ((HSliderComponent) e.getSource()).getValue();
                }

                @Override
                public void ACtrlAdjustmentFinished(ACtrlEvent e) {
                    double vnew = ((HSliderComponent) e.getSource()).getValue();
                    if (vnew != valueBeforeAdjustment) {
                        param.SetDirty();
                    }
                }
            });
            hsls.add(hsl);
            p.add(hsl);
            parent.add(p);
        }
        if (param.GetObjectInstance().patch.Modulators.isEmpty()) {
            JMenuItem d = new JMenuItem("No modulation sources in patch");
            d.setToolTipText("<html>Place a <b><i>modsource</b></i> or related object in your patch to be able to apply it here.");
            d.setEnabled(false);
            parent.add(d);
        }
    }
}
