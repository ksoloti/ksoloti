/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
package axoloti.parameters;

import axoloti.Preset;
import axoloti.atom.AtomInstance;
import axoloti.datatypes.Value;
import axoloti.datatypes.ValueFrac32;
import axoloti.object.AxoObjectInstance;
import axoloti.realunits.NativeToReal;
import axoloti.ui.Theme;

import static axoloti.utils.CharEscape.charEscape;
import components.AssignMidiCCComponent;
import components.AssignPresetMenuItems;
import components.LabelComponent;
import components.control.ACtrlComponent;
import components.control.ACtrlEvent;
import components.control.ACtrlListener;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "param")
public abstract class ParameterInstance<T extends Parameter> extends JPanel implements ActionListener, AtomInstance<T> {

    @Attribute
    String name;

    @Attribute(required = false)
    private Boolean onParent;

    protected int index;
    public T parameter;

    @ElementList(required = false)
    ArrayList<Preset> presets;

    protected boolean needsTransmit = false;
    AxoObjectInstance axoObj;
    LabelComponent valuelbl = new LabelComponent("123456789");
    NativeToReal convs[];
    int selectedConv = 0;
    public int presetEditActive = 0;
    ACtrlComponent ctrl;

    @Attribute(required = false)
    Integer MidiCC = null;

    AssignMidiCCComponent midiAssign;

    @Attribute(required = false)
    private Boolean frozen;

    public final String I = "\t"; /* Convenient for (I)ndentation of auto-generated code */

    private final static byte[]  AxoP_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('P')};

    public ParameterInstance() {
    }

    public ParameterInstance(T param, AxoObjectInstance axoObj1) {
        super();
        parameter = param;
        axoObj = axoObj1;
        name = parameter.name;
    }

    void UpdateUnit() {
        if (convs != null) {
            valuelbl.setText(convs[selectedConv].ToReal(getValue()));
            setCtrlToolTip();
        }
    }

    public String GetCName() {
        return parameter.GetCName();
    }

    public void Lock() {
        if (ctrl != null) {
            ctrl.Lock();
        }
    }

    public void UnLock() {
        if (ctrl != null) {
            ctrl.UnLock();
        }
    }

    public void CopyValueFrom(ParameterInstance p) {
        if (p.onParent != null) {
            setOnParent(p.onParent);
        }
        if (p.frozen != null) {
            setFrozen(p.frozen);
        }
        SetMidiCC(p.MidiCC);
    }

    public void PostConstructor() {
        removeAll();
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JPanel lbls = null;
        if ((((parameter.noLabel == null) || (parameter.noLabel == false))) && (convs != null)) {
            lbls = new JPanel();
            lbls.setLayout(new BoxLayout(lbls, BoxLayout.Y_AXIS));
            this.add(lbls);
        }

        if ((parameter.noLabel == null) || (parameter.noLabel == false)) {
            LabelComponent paramlbl = new LabelComponent(GetDefinition().getName());
            paramlbl.setBorder(new EmptyBorder(0,1,0,0));
            if (lbls != null) {
                lbls.add(paramlbl);
            } else {
                add(paramlbl);
            }
        }
        if (convs != null) {
            if (lbls != null) {
                lbls.add(valuelbl);
            } else {
                add(valuelbl);
            }
            Dimension d = new Dimension(50, 10);
            valuelbl.setMinimumSize(d);
            valuelbl.setPreferredSize(d);
            valuelbl.setSize(d);
            valuelbl.setMaximumSize(d);
            valuelbl.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedConv = selectedConv + 1;
                    if (selectedConv >= convs.length) {
                        selectedConv = 0;
                    }
                    UpdateUnit();
                }
            });
            UpdateUnit();
        }
//        if (axoObj.patch != null)
//            ShowPreset(axoObj.patch.presetNo);

        ctrl = CreateControl();
        setCtrlToolTip();

        add(getControlComponent());
        getControlComponent().addMouseListener(popupMouseListener);
        getControlComponent().addACtrlListener(new ACtrlListener() {
            @Override
            public void ACtrlAdjusted(ACtrlEvent e) {
                handleAdjustment();
            }

            @Override
            public void ACtrlAdjustmentBegin(ACtrlEvent e) {
                valueBeforeAdjustment = getControlComponent().getValue();
                //System.out.println("begin "+value_before);
            }

            @Override
            public void ACtrlAdjustmentFinished(ACtrlEvent e) {
                if ((valueBeforeAdjustment != getControlComponent().getValue())
                        && (axoObj != null)
                        && (axoObj.getPatch() != null)) {
                    //System.out.println("finished" +getControlComponent().getValue());
                    SetDirty();
                }
            }
        });
        updateV();
        SetMidiCC(MidiCC);
    }

    double valueBeforeAdjustment;

    public void applyDefaultValue() {
    }

    public boolean GetNeedsTransmit() {
        return needsTransmit;
    }
    
    public void SetNeedsTransmit(boolean needs) {
        this.needsTransmit = needs;
    }
    
    public void ClearNeedsTransmit() {
        needsTransmit = false;
    }

    public void IncludeInPreset() {
        if (presetEditActive > 0) {
            Preset p = GetPreset(presetEditActive);
            if (p != null) {
                return;
            }
            if (presets == null) {
                presets = new ArrayList<Preset>();
            }
            p = new Preset(presetEditActive, getValue());
            presets.add(p);
        }
        ShowPreset(presetEditActive);
    }

    public void IncludeInAllPresets(int nPres) {
        if (presets == null) {
            presets = new ArrayList<Preset>();
        }

        for (int i = 1; i <= nPres; i++) {

            Preset p = GetPreset(i);
            if (p != null) {
                continue;
            }

            p = new Preset(i, getValue());
            presets.add(p);
        }
        ShowPreset(presetEditActive);
    }

    public void ExcludeFromPreset() {
        if (presetEditActive > 0) {
            Preset p = GetPreset(presetEditActive);
            if (p != null) {
                presets.remove(p);
                if (presets.isEmpty()) {
                    presets = null;
                }
            }
        }
        ShowPreset(presetEditActive);
    }

    public ByteBuffer TransmitParamData() {
        int pid = GetObjectInstance().getPatch().GetIID();
        int tvalue = GetValueRaw();
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(14).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoP_pckt);
        buffer.putInt(pid);
        buffer.putInt(tvalue);
        buffer.putShort((short) index);

        needsTransmit = false;
        return buffer;
    }

    public Preset GetPreset(int i) {
        if (presets == null) {
            return null;
        }
        for (Preset p : presets) {
            if (p.index == i) {
                return p;
            }
        }
        return null;
    }

    public ArrayList<Preset> getPresets() {
        return presets;
    }

    public Preset AddPreset(int index, Value value) {
        Preset p = GetPreset(index);
        if (p != null) {
            p.value = value;
            return p;
        }
        if (presets == null) {
            presets = new ArrayList<Preset>();
        }
        p = new Preset(index, value);
        presets.add(p);
        return p;
    }

    public void RemovePreset(int index) {
        Preset p = GetPreset(index);
        if (p != null) {
            presets.remove(p);
        }
    }

    public abstract Value getValue();

    public void setValue(Value value) {
        if (axoObj != null) {
            SetDirty();
        }
    }

    public void SetValueRaw(int v) {
        getValue().setRaw(v);
        updateV();
    }

    public int GetValueRaw() {
        return getValue().getRaw();
    }

    public void updateV() {
        UpdateUnit();
    }

    public String indexName() {
        return "PARAM_INDEX_" + axoObj.getLegalName() + "_" + getLegalName();
//        return ("" + index);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getLegalName() {
        return charEscape(name);
    }

    public String KVPName(String vprefix) {
        return ""; // "KVP_" + axoObj.getCInstanceName() + "_" + getLegalName();
    }

    public String PExName(String vprefix) {
        return vprefix + "PExch[" + indexName() + "]";
    }

    public String valueName(String vprefix) {
        return PExName(vprefix) + ".value";
    }

    public String ControlOnParentName() {
        if (axoObj.parameterInstances.size() == 1) {
            return axoObj.getInstanceName();
        } else {
            return axoObj.getInstanceName() + ":" + parameter.name;
        }
    }

    public String variableName(String vprefix) {
        // if ((onParent != null) && (onParent)) {
        //     return "%" + ControlOnParentName() + "%";
        // } else {
            return PExName(vprefix) + ".finalvalue";
        // }
    }

    public String signalsName(String vprefix) {
        return PExName(vprefix) + ".signals";
    }

    public String GetPFunction() {
        return "";
    }

    public String GenerateCodeDeclaration(String vprefix) {
        return "";//("#define " + indexName() + " " + index + "\n");
    }

    public abstract String GenerateCodeInit(String vprefix, String StructAccces);

    public abstract String GenerateCodeMidiHandler(String vprefix);

    // void SetPresetState(boolean b) { // OBSOLETE
    //     if (b) {
    //         setForeground(Theme.Parameter_Preset_Highlight_Foreground);
    //     } else {
    //         setForeground(Theme.Parameter_Default_Foreground);
    //     }
    // }

    public abstract void ShowPreset(int i);

    public void setIndex(int i) {
        index = i;
    }

    public int getIndex() {
        return index;
    }

    String GenerateMidiCCCodeSub(String vprefix, String value) {
        if (!isFrozen() && MidiCC != null) {
            return "        if ((status == attr_midichannel + MIDI_CONTROL_CHANGE)&&(data1 == " + MidiCC + ")) {\n"
                    + "            PExParameterChange(&parent->" + PExName(vprefix) + ", " + value + ", 0xFFFD);\n"
                    + "        }\n";
        } else {
            return "";
        }
    }

    public Parameter getParameterForParent() {
        Parameter pcopy = parameter.getClone();
        pcopy.name = ControlOnParentName();
        pcopy.noLabel = null;
        pcopy.PropagateToChild = axoObj.getLegalName() + "_" + getLegalName();
        return pcopy;
    }

    public boolean isOnParent() {
        if (onParent == null) {
            return false;
        } else {
            return onParent;
        }
    }

    public void setOnParent(Boolean b) {
        if (b == null) {
            setCtrlToolTip();
            return;
        }
        if (isOnParent() == b) {
            return;
        }
        if (b) {
            onParent = true;
        } else {
            onParent = null;
        }
        setCtrlToolTip();
    }

    public boolean isFrozen() {
        if (frozen == null || (parameter != null && parameter.PropagateToChild != null)) {
            return false;
        } else {
            return frozen;
        }
    }

    public void setFrozen(Boolean f) {
        if (f == null) {
            setCtrlToolTip();
            return;
        }
        if (isFrozen() == f) {
            return;
        }
        if (f) {
            frozen = true;
        } else {
            frozen = null;
        }
        setCtrlToolTip();
    }

    public abstract ACtrlComponent CreateControl();

    MouseListener popupMouseListener = new MouseListener() {

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!getControlComponent().isLocked() && e.isPopupTrigger()) {
                doPopup(e);
                e.consume();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!getControlComponent().isLocked() && e.isPopupTrigger()) {
                doPopup(e);
                e.consume();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    };

    public void doPopup(MouseEvent e) {
        JPopupMenu m = new JPopupMenu();
        populatePopup(m);
        m.show(this, 0, getHeight());
    }

    public void populatePopup(JPopupMenu m) {
        final JCheckBoxMenuItem m_onParent = new JCheckBoxMenuItem("Show Parameter on Parent");
        m_onParent.setToolTipText("When selected and the current patch is a subpatch, this parameter\n" +
                                  "will be shown on the parent patch and can be changed from there.n" +
                                  "Any changes to the parameter value from inside the subpatch\n" +
                                  "will have no effect while it is shown on the parent.");
        m_onParent.setSelected(isOnParent());
        m_onParent.setMnemonic('S');
        m.add(m_onParent);

        m_onParent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setOnParent(m_onParent.isSelected());
                SetDirty();
            }
        });

        final JCheckBoxMenuItem m_frozen = new JCheckBoxMenuItem("Freeze Parameter");
        m_frozen.setMnemonic('F');
        m_frozen.setSelected(frozen != null && frozen);
        m_frozen.setEnabled(parameter.PropagateToChild == null);
        if (m_frozen.isEnabled()) {
            m_frozen.setToolTipText("While frozen, a parameter consumes less memory and DSP but cannot be\n" +
                                    "changed while the patch is live (essentially acting as an attribute).\n" +
                                    "Any modulation, preset change, and MIDI assignments to the parameter\n" +
                                    "will have no effect while it is frozen.");
        }
        else {
            m_frozen.setToolTipText("This parameter cannot be frozen because it belongs to the subpatch.\n" +
                                    "You can freeze it from inside the subpatch. If you do so, it will\n" +
                                    "temporarily disappear from the parent and its value will revert to the\n" + 
                                    "knob position inside the subpatch. Unfreeze it to make it reappear on\n" +
                                    "the parent and become editable again.");

        }
        m.add(m_frozen);

        m_frozen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setFrozen(m_frozen.isSelected());
                SetDirty();
            }
        });

        if (GetObjectInstance().getPatch() != null) {
            JMenu m_preset = new JMenu("Preset");
            m_preset.setDelay(300);
            m_preset.setMnemonic('P');
            /* AssignPresetMenuItems, does stuff in ctor */
            new AssignPresetMenuItems(this, m_preset);
            m.add(m_preset);
        }
    }

    /**
     *
     * @return control component
     */
    abstract public ACtrlComponent getControlComponent();

    abstract public boolean handleAdjustment();

    void SetMidiCC(Integer cc) {
        if ((cc != null) && (cc >= 0)) {
            MidiCC = cc;
            if (midiAssign != null) {
                midiAssign.setCC(cc);
            }
        } else {
            MidiCC = null;
            if (midiAssign != null) {
                midiAssign.setCC(-1);
            }
        }
    }

    public int getMidiCC() {
        if (MidiCC == null) {
            return -1;
        } else {
            return MidiCC;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();
        if (s.startsWith("CC")) {
            int i = Integer.parseInt(s.substring(2));
            if (i != getMidiCC()) {
                SetMidiCC(i);
                SetDirty();
            }
        } else if (s.equals("none")) {
            if (-1 != getMidiCC()) {
                SetMidiCC(-1);
                SetDirty();
            }
        }
    }

    @Override
    public AxoObjectInstance GetObjectInstance() {
        return axoObj;
    }

    @Override
    public T GetDefinition() {
        return parameter;
    }

    public String GenerateCodeInitModulator(String vprefix, String StructAccces) {
        return "";
    }

    public void SetDirty() {
        // propagate dirty flag to patch if there is one
        if (axoObj.getPatch() != null) {
            axoObj.getPatch().SetDirty();
        }
    }

    private void setCtrlToolTip() {
        if(ctrl == null) return;
        
        StringBuilder tooltipBuilder = new StringBuilder("<html>");

        if (parameter.description != null) {
            tooltipBuilder.append(parameter.description);
        } else {
            tooltipBuilder.append(parameter.name);
        }

        ctrl.setForeground(Theme.Parameter_Default_Foreground);
        ctrl.setBackground(Theme.Parameter_Default_Background);

        if (isOnParent()) {
            ctrl.setForeground(Theme.Parameter_On_Parent_Foreground);
            tooltipBuilder.append("<p><b>This parameter is being controlled from the parent patch.</b>");
        }
        if (isFrozen()) {
            ctrl.setBackground(Theme.Parameter_Frozen_Background);
            tooltipBuilder.append("<p><b>This parameter is currently frozen to save memory and DSP power.</b>");
        }
        else {
            ctrl.setBackground(Theme.Component_Background);
        }

        tooltipBuilder.append("<p>");
        double currentValue = getValue().getDouble(); 
        DecimalFormat df = new DecimalFormat("0.00####"); 
        tooltipBuilder.append("<br>").append(df.format(currentValue)); 

        if (convs != null) {
            for (NativeToReal c : convs) { 
                tooltipBuilder.append("<br>").append(c.ToRealHighPrecision(new ValueFrac32(currentValue)));
            }
        }
        ctrl.setToolTipText(tooltipBuilder.toString());
    }
}
