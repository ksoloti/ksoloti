/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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

import axoloti.parameters.ParameterInstance;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 *
 * @author Johannes Taelman
 */
public class AssignPresetMenuItems {

    final ParameterInstance param;
    final JComponent parent;

    public AssignPresetMenuItems(ParameterInstance param, JComponent parent) {
        this.param = param;
        this.parent = parent;

        if (param.GetObjectInstance().getPatch().getSettings().GetNPresets() < 1) {
            JMenuItem mi = new JMenuItem("Presets set to 0 in patch settings");
            mi.setEnabled(false);
            parent.add(mi);
            return;
        }

        //sub2 = new JPopupMenu();
        {
            JMenuItem mi = new JMenuItem("Track in Current Preset");
            if (param.presetEditActive == 0) {
                mi.setEnabled(false);
            }
            mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AssignPresetMenuItems.this.param.IncludeInPreset();
                }
            });
            parent.add(mi);
        }
        {
            JMenuItem mi = new JMenuItem("Track in All Presets");
            mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AssignPresetMenuItems.this.param.IncludeInAllPresets();
                }
            });
            parent.add(mi);
        }
        {
            JMenuItem mi = new JMenuItem("Untrack from Current Preset");
            if (param.presetEditActive == 0) {
                mi.setEnabled(false);
            }
            mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AssignPresetMenuItems.this.param.ExcludeFromPreset();
                    AssignPresetMenuItems.this.param.ShowPreset(AssignPresetMenuItems.this.param.presetEditActive);
                }
            });
            parent.add(mi);
        }
        {
            JMenuItem mi = new JMenuItem("Untrack from All Presets");
            mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (AssignPresetMenuItems.this.param.getPresets() != null) {
                        AssignPresetMenuItems.this.param.getPresets().clear();
                        AssignPresetMenuItems.this.param.ShowPreset(AssignPresetMenuItems.this.param.presetEditActive);
                    }
                }
            });
            parent.add(mi);
        }

        JPanel panel = new AssignPresetPanel(param);
        parent.add(panel);
    }

}
