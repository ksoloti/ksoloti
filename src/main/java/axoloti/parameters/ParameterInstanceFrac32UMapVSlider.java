/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
package axoloti.parameters;

import axoloti.Preset;
import axoloti.ui.Theme;
import components.control.VSliderComponent;
import org.simpleframework.xml.Attribute;

/**
 *
 * @author Johannes Taelman
 */
public class ParameterInstanceFrac32UMapVSlider extends ParameterInstanceFrac32U<ParameterFrac32UMapVSlider> {
    
    public ParameterInstanceFrac32UMapVSlider() {
    }

    public ParameterInstanceFrac32UMapVSlider(@Attribute(name = "value") double v) {
        super(v);
    }

    @Override
    public void PostConstructor() {
        super.PostConstructor();
    }

    @Override
    public void updateV() {
        if (ctrl != null) {
            ctrl.setValue(value.getDouble());
        }
    }

    @Override
    public String GenerateCodeInit(String vprefix, String StructAccces) {
        String s = /*I+I+I + "SetKVP_IPVP(&" + StructAccces + KVPName(vprefix) + ", ObjectKvpRoot, "
                 + "&" + PExName(vprefix) + ", "
                 + (((ParameterFrac32UMapVSlider) parameter).MinValue.getRaw()) + ", "
                 + (((ParameterFrac32UMapVSlider) parameter).MaxValue.getRaw()) + ");\n"
                 + I+I+I + "KVP_RegisterObject(&" + StructAccces + KVPName(vprefix) + ");\n"*/ "";
        return s;
    }

    @Override
    public String GenerateCodeDeclaration(String vprefix) {
        return ""; //  "KeyValuePair " + KVPName(vprefix) + ";\n";
    }

    @Override
    public String GenerateCodeMidiHandler(String vprefix) {
        return GenerateMidiCCCodeSub(vprefix, "(data2!=127)?data2<<20:0x07FFFFFF");
    }

    /*
     *  Preset logic
     */
    @Override
    public void ShowPreset(int i) {
        this.presetEditActive = i;
        if (i > 0) {
            Preset p = GetPreset(presetEditActive);
            if (p != null) {
                // setForeground(Theme.Parameter_Preset_Highlight_Foreground);
                ctrl.setValue(p.value.getDouble());
            } else {
                // setForeground(Theme.Parameter_Default_Foreground);
                ctrl.setValue(value.getDouble());
            }
        } else {
            setForeground(Theme.Parameter_Default_Foreground);
            ctrl.setValue(value.getDouble());
        }
        if ((presets != null) && (!presets.isEmpty())) {
//            lblPreset.setVisible(true);
        } else {
//            lblPreset.setVisible(false);
        }
    }

    @Override
    public VSliderComponent CreateControl() {
        VSliderComponent v = new VSliderComponent(0.0, 0.0, 64, 0.5);
        v.setParentAxoObjectInstance(axoObj);
        return v;
    }

    @Override
    public VSliderComponent getControlComponent() {
        return (VSliderComponent) ctrl;
    }
}
