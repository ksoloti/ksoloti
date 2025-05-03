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
package axoloti.object;

import axoloti.MainFrame;
import axoloti.Net;
import axoloti.Patch;
import axoloti.PatchFrame;
import axoloti.PatchGUI;
import axoloti.SDFileReference;
import axoloti.Synonyms;
import axoloti.Theme;
import axoloti.attribute.*;
import axoloti.attributedefinition.AxoAttribute;
import axoloti.datatypes.DataType;
import axoloti.datatypes.Frac32buffer;
import axoloti.displays.Display;
import axoloti.displays.DisplayInstance;
import axoloti.inlets.Inlet;
import axoloti.inlets.InletInstance;
import axoloti.outlets.Outlet;
import axoloti.outlets.OutletInstance;
import axoloti.parameters.*;
import axoloti.utils.Constants;
import components.LabelComponent;
import components.PopupIcon;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

import org.simpleframework.xml.*;
import org.simpleframework.xml.core.Persist;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "obj")
public class AxoObjectInstance extends AxoObjectInstanceAbstract {

    private static final Logger LOGGER = Logger.getLogger(AxoObjectInstance.class.getName());

    public ArrayList<InletInstance> inletInstances;
    public ArrayList<OutletInstance> outletInstances;

    @Path("params")
    @ElementListUnion({
        @ElementList(entry = "frac32.u.map", type = ParameterInstanceFrac32UMap.class, inline = true, required = false),
        @ElementList(entry = "frac32.s.map", type = ParameterInstanceFrac32SMap.class, inline = true, required = false),
        @ElementList(entry = "frac32.u.mapvsl", type = ParameterInstanceFrac32UMapVSlider.class, inline = true, required = false),
        @ElementList(entry = "frac32.s.mapvsl", type = ParameterInstanceFrac32SMapVSlider.class, inline = true, required = false),
        @ElementList(entry = "int32", type = ParameterInstanceInt32Box.class, inline = true, required = false),
        @ElementList(entry = "int32.small", type = ParameterInstanceInt32BoxSmall.class, inline = true, required = false),
        @ElementList(entry = "int32.hradio", type = ParameterInstanceInt32HRadio.class, inline = true, required = false),
        @ElementList(entry = "int32.vradio", type = ParameterInstanceInt32VRadio.class, inline = true, required = false),
        @ElementList(entry = "int2x16", type = ParameterInstance4LevelX16.class, inline = true, required = false),
        @ElementList(entry = "bin12", type = ParameterInstanceBin12.class, inline = true, required = false),
        @ElementList(entry = "bin16", type = ParameterInstanceBin16.class, inline = true, required = false),
        @ElementList(entry = "bin32", type = ParameterInstanceBin32.class, inline = true, required = false),
        @ElementList(entry = "bool32.tgl", type = ParameterInstanceBin1.class, inline = true, required = false),
        @ElementList(entry = "bool32.mom", type = ParameterInstanceBin1Momentary.class, inline = true, required = false)})
    public ArrayList<ParameterInstance> parameterInstances;

    @Path("attribs")
    @ElementListUnion({
        @ElementList(entry = "objref", type = AttributeInstanceObjRef.class, inline = true, required = false),
        @ElementList(entry = "table", type = AttributeInstanceTablename.class, inline = true, required = false),
        @ElementList(entry = "combo", type = AttributeInstanceComboBox.class, inline = true, required = false),
        @ElementList(entry = "int", type = AttributeInstanceInt32.class, inline = true, required = false),
        @ElementList(entry = "spinner", type = AttributeInstanceSpinner.class, inline = true, required = false),
        @ElementList(entry = "file", type = AttributeInstanceSDFile.class, inline = true, required = false),
        @ElementList(entry = "text", type = AttributeInstanceTextEditor.class, inline = true, required = false)})
    public ArrayList<AttributeInstance> attributeInstances;

    public ArrayList<DisplayInstance> displayInstances;

    LabelComponent IndexLabel;
    
    private final String I = "\t"; /* Convenient for (I)ndentation of auto-generated code */

    boolean deferredObjTypeUpdate = false;


    private void refreshTooltip() {
        String tooltiptxt = "<html>";
        tooltiptxt += "<b>" + typeName + "</b>";
        if (getType().sDescription != null && !getType().sDescription.isEmpty()) {
            /* Chop up string and put it back together with line breaks */
            /* TODO: turn this into a callable routine */
            String[] splitStrings = getType().sDescription.split(" ");
            String putBackTogetherString = "";
            int lineLength = 0;
            for (String s : splitStrings) {
                putBackTogetherString += s + " ";
                lineLength += (s.length()+1);
                if (s.contains("\n")) {
                    lineLength = 0; /* Reset line length counter if there is going to be a formatted line break */
                }
                if (lineLength > 80) {
                    putBackTogetherString += "\n"; /* Insert line break to make text wrap around */
                    lineLength = 0; /* Reset line length counter */
                }
            }
            tooltiptxt += "<p><br/>" + putBackTogetherString.replaceAll("\n", "<br/>") + "<br/>";
        }
        if (getType().sAuthor != null && !getType().sAuthor.isEmpty()) {
            tooltiptxt += "<p><br/><i>Author: " + getType().sAuthor + "</p>";
        }
        if (getType().sObjFilePath != null && !getType().sObjFilePath.isEmpty()) {
            tooltiptxt += "<p><i>Path: " + getType().sObjFilePath + "</p>";
        }
        if (IndexLabel != null && !IndexLabel.getText().equals("")) {
            tooltiptxt += "<p><i>Execution order: " + (IndexLabel.getText()) + " / " + patch.GetObjectInstancesWithoutComments().size() + "</p>";
        }
        tooltiptxt += "</div>";
        Titlebar.setToolTipText(tooltiptxt);
    }

    @Override
    public void refreshIndex() {
        if (patch != null && IndexLabel != null) {
            IndexLabel.setText("" + (patch.GetObjectInstancesWithoutComments().indexOf(this) + 1)); /* Add 1, so Index 0 means 1st object */
            refreshTooltip();
        }
    }

    @Override
    public ArrayList<ParameterInstance> getParameterInstances() {
        return parameterInstances;
    }

    @Override
    public ArrayList<AttributeInstance> getAttributeInstances() {
        return attributeInstances;
    }

    public final JPanel p_params = new JPanel();
    public final JPanel p_displays = new JPanel();
    public final JPanel p_iolets = new JPanel();
    public final JPanel p_inlets = new JPanel();
    public final JPanel p_outlets = new JPanel();

    void updateObj1() {
        getType().addObjectModifiedListener(this);
    }

    @Override
    public void PostConstructor() {
        super.PostConstructor();
        updateObj1();
        ArrayList<ParameterInstance> pParameterInstances = parameterInstances;
        ArrayList<AttributeInstance> pAttributeInstances = attributeInstances;
        ArrayList<InletInstance> pInletInstances = inletInstances;
        ArrayList<OutletInstance> pOutletInstances = outletInstances;
        parameterInstances = new ArrayList<ParameterInstance>();
        attributeInstances = new ArrayList<AttributeInstance>();
        displayInstances = new ArrayList<DisplayInstance>();
        inletInstances = new ArrayList<InletInstance>();
        outletInstances = new ArrayList<OutletInstance>();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        final PopupIcon popupIcon = new PopupIcon();
        popupIcon.setPopupIconListener(new PopupIcon.PopupIconListener() {
            @Override
            public void ShowPopup() {
                JPopupMenu popup = CreatePopupMenu();
                popupIcon.add(popup);
                popup.show(popupIcon, 0, popupIcon.getHeight());
            }
        });

        popupIcon.setAlignmentX(LEFT_ALIGNMENT);
        Titlebar.add(popupIcon);

        LabelComponent idlbl = new LabelComponent("");
        if (typeName.length() <= 20) {
            idlbl.setText(typeName); /* if not too long, use full object name */
        }
        else {
            String[] ssubs = typeName.split("/"); /* else split path of full object name */
            String slbl = ssubs[ssubs.length-1]; /* start with "most signinficant" part */

            for (int i=ssubs.length-2; i>0; i--) {
                if (slbl.length() >= 16) break; /* it object name is too long already, leave */
                slbl = ssubs[i] + "/" + slbl; /* else keep adding subpaths until it is too long */
            }
            idlbl.setText("…/" + slbl);
        }
        idlbl.setForeground(Theme.Object_TitleBar_Foreground);
        idlbl.setAlignmentX(LEFT_ALIGNMENT);
        idlbl.setFont(Constants.FONT_BOLD);
        Titlebar.add(idlbl);

        /* Execution Index shown in object tooltip for now */
        IndexLabel = new LabelComponent("");
        refreshIndex();
        
        /* IndexLabel only shown in object tooltip for now ...
        Titlebar.add(Box.createHorizontalStrut(3));
        Titlebar.add(Box.createHorizontalGlue());
        Titlebar.add(new JSeparator(SwingConstants.VERTICAL));
        IndexLabel = new LabelComponent("");
        IndexLabel.setSize(IndexLabel.getMinimumSize());
        refreshIndex();
        IndexLabel.setForeground(Theme.Object_TitleBar_Foreground);
        // idlbl.setFont(Constants.FONT_BOLD);
        IndexLabel.setAlignmentX(RIGHT_ALIGNMENT);
        Titlebar.add(IndexLabel);
        */

        Titlebar.setAlignmentX(LEFT_ALIGNMENT);
        add(Titlebar);

        InstanceLabel = new LabelComponent(getInstanceName());
        InstanceLabel.setBorder(new EmptyBorder(-3,1,-2,0));
        InstanceLabel.setAlignmentX(LEFT_ALIGNMENT);
        InstanceLabel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addInstanceNameEditor();
                    e.consume();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        add(InstanceLabel);

        p_iolets.removeAll();
        p_inlets.removeAll();
        p_outlets.removeAll();
        p_params.removeAll();

        if (getType().getRotatedParams()) {
            p_params.setLayout(new BoxLayout(p_params, BoxLayout.LINE_AXIS));
        } else {
            p_params.setLayout(new BoxLayout(p_params, BoxLayout.PAGE_AXIS));
        }

        p_displays.removeAll();

        if (getType().getRotatedParams()) {
            p_displays.setLayout(new BoxLayout(p_displays, BoxLayout.LINE_AXIS));
        } else {
            p_displays.setLayout(new BoxLayout(p_displays, BoxLayout.PAGE_AXIS));
        }
        p_displays.add(Box.createHorizontalGlue());
        p_params.add(Box.createHorizontalGlue());

        for (Inlet inl : getType().inlets) {
            InletInstance inlinp = null;
            for (InletInstance inlin1 : pInletInstances) {
                if (inlin1.GetLabel().equals(inl.getName())) {
                    inlinp = inlin1;
                }
            }
            InletInstance inlin = new InletInstance(inl, this);
            if (inlinp != null) {
                Net n = getPatch().GetNet(inlinp);
                if (n != null) {
                    n.connectInlet(inlin);
                }
            }
            inletInstances.add(inlin);
            inlin.setAlignmentX(LEFT_ALIGNMENT);
            p_inlets.add(inlin);
        }
        // disconnect stale inlets from nets
        for (InletInstance inlin1 : pInletInstances) {
            getPatch().disconnect(inlin1);
        }

        for (Outlet o : getType().outlets) {
            OutletInstance oinp = null;
            for (OutletInstance oinp1 : pOutletInstances) {
                if (oinp1.GetLabel().equals(o.getName())) {
                    oinp = oinp1;
                }
            }
            OutletInstance oin = new OutletInstance(o, this);
            if (oinp != null) {
                Net n = getPatch().GetNet(oinp);
                if (n != null) {
                    n.connectOutlet(oin);
                }
            }
            outletInstances.add(oin);
            oin.setAlignmentX(RIGHT_ALIGNMENT);
            p_outlets.add(oin);
        }
        // disconnect stale outlets from nets
        for (OutletInstance oinp1 : pOutletInstances) {
            getPatch().disconnect(oinp1);
        }

        /*
         if (p_inlets.getComponents().length == 0){
         p_inlets.add(Box.createHorizontalGlue());
         }
         if (p_outlets.getComponents().length == 0){
         p_outlets.add(Box.createHorizontalGlue());
         }*/
        p_iolets.add(p_inlets);
        p_iolets.add(Box.createHorizontalGlue());
        p_iolets.add(p_outlets);
        add(p_iolets);

        for (AxoAttribute a : getType().attributes) {
            AttributeInstance attrp1 = null;
            for (AttributeInstance attrp : pAttributeInstances) {
                if (attrp.getName().equals(a.getName())) {
                    attrp1 = attrp;
                }
            }
            AttributeInstance attri = a.CreateInstance(this, attrp1);
            attri.setAlignmentX(LEFT_ALIGNMENT);
            add(attri);
            attributeInstances.add(attri);
        }

        /* Sort attributes by name length, longest first. Ensures proper replacement */
        Comparator<AttributeInstance> attrComp = new Comparator<AttributeInstance>() {
            public int compare(AttributeInstance a1, AttributeInstance a2) {

                /* Compare the lengths of two AttributeInstance's C names (attr_xxxx).
                 * If a1 is longer than a2, sort it first.
                 */
                if (a1.GetCName().length() > a2.GetCName().length()) {
                    return -1;
                }
                /* Else don't change the order */
                return 0;
            }
        };
        attributeInstances.sort(attrComp); 

        for (Parameter p : getType().params) {
            ParameterInstance pin = p.CreateInstance(this);
            for (ParameterInstance pinp : pParameterInstances) {
                if (pinp.getName().equals(pin.getName())) {
                    pin.CopyValueFrom(pinp);
                }
            }
            pin.PostConstructor();
            pin.setAlignmentX(RIGHT_ALIGNMENT);
            parameterInstances.add(pin);
        }

        for (Display p : getType().displays) {
            DisplayInstance pin = p.CreateInstance(this);
            pin.setAlignmentX(RIGHT_ALIGNMENT);
            displayInstances.add(pin);
        }
//        p_displays.add(Box.createHorizontalGlue());
//        p_params.add(Box.createHorizontalGlue());
        add(p_params);
        add(p_displays);

        getType().addObjectModifiedListener(this);

        synchronized (getTreeLock()) {
            validateTree();
        }
        setLocation(x, y);
        resizeToGrid();
    }

    @Override
    JPopupMenu CreatePopupMenu() {
        JPopupMenu popup = super.CreatePopupMenu();
        JMenuItem popm_edit = new JMenuItem("Edit object definition");
        popm_edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                OpenEditor();
            }
        });
        popm_edit.setMnemonic('E');
        popup.add(popm_edit);

        JMenuItem popm_editInstanceName = new JMenuItem("Edit instance name");
        popm_editInstanceName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addInstanceNameEditor();
            }
        });
        popm_editInstanceName.setMnemonic('N');
        popm_editInstanceName.setDisplayedMnemonicIndex(14);
        popup.add(popm_editInstanceName);

        if (parameterInstances.size() > 0) {
            JMenuItem popm_freezeAllParameters = new JMenuItem("Freeze all parameters");
            if (getType().toString().equals("patch/patcher")) {
                popm_freezeAllParameters.setEnabled(false);
                popm_freezeAllParameters.setToolTipText("Parameters cannot be frozen because this object is a subpatch.\n" +
                                        "You can freeze parameters from inside the subpatch. If you do so, they will\n" +
                                        "temporarily disappear from the parent and their value will revert to the\n" + 
                                        "knob position inside the subpatch. Unfreeze a parameter to make it reappear on\n" +
                                        "the parent and become editable again.");
            }
            else {
                popm_freezeAllParameters.setToolTipText("While frozen, a parameter consumes less memory and DSP but cannot be\n" +
                                        "changed while the patch is live (essentially acting as an attribute).\n" +
                                        "Any modulation, preset change, and MIDI assignments to the parameter\n" +
                                        "will have no effect while it is frozen.");
            }

            if (getPatch().IsLocked()) {
                popm_freezeAllParameters.setEnabled(false);
            }
            /* Check if all parameters are frozen, if yes change menu entry to unfreeze */
            boolean isAllFrozen = true;
            for (ParameterInstance pi : parameterInstances) {
                isAllFrozen &= pi.isFrozen();
            }
            if (isAllFrozen) {
                popm_freezeAllParameters.setText("Unfreeze all parameters");
            }
            
            final boolean f = isAllFrozen; /* "variable defined in an enclosing scope must be final" workaround */
            popm_freezeAllParameters.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    for (ParameterInstance pi : parameterInstances) {
                        pi.setFrozen(!f);
                    }
                    getPatch().SetDirty();
                }
            });
            popm_freezeAllParameters.setMnemonic('F');
            popup.add(popm_freezeAllParameters);
        }

        JMenuItem popm_replace = new JMenuItem("Replace");
        popm_replace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ((PatchGUI) patch).ShowClassSelector(AxoObjectInstance.this.getLocation(), AxoObjectInstance.this, null, true);
            }
        });
        popm_replace.setMnemonic('R');
        popup.add(popm_replace);

        if (getType().GetHelpPatchFile() != null) {
            JMenuItem popm_help = new JMenuItem("Help patch");
            popm_help.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    PatchGUI.OpenPatch(getType().GetHelpPatchFile());
                }
            });
            popm_help.setMnemonic('H');
            popup.add(popm_help);
        }

        if (type instanceof AxoObjectFromPatch) {
            JMenuItem popm_embed = new JMenuItem("Embed as Subpatch");
            popm_embed.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    ConvertToPatchPatcher();
                }
            });
            popm_embed.setMnemonic('B');
            popup.add(popm_embed);
        } else if (!(this instanceof AxoObjectInstancePatcherObject)) {
            JMenuItem popm_embed = new JMenuItem("Embed as Object");
            popm_embed.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    ConvertToEmbeddedObj();
                }
            });
            popm_embed.setMnemonic('B');
            popup.add(popm_embed);
        }
        return popup;
    }

    final void init1() {
        inletInstances = new ArrayList<InletInstance>();
        outletInstances = new ArrayList<OutletInstance>();
        displayInstances = new ArrayList<DisplayInstance>();
        parameterInstances = new ArrayList<ParameterInstance>();
        attributeInstances = new ArrayList<AttributeInstance>();

        p_iolets.setBackground(Theme.Object_Default_Background);
        p_iolets.setLayout(new BoxLayout(p_iolets, BoxLayout.LINE_AXIS));
        p_iolets.setAlignmentX(LEFT_ALIGNMENT);
        p_iolets.setAlignmentY(TOP_ALIGNMENT);

        p_inlets.setBackground(Theme.Object_Default_Background);
        p_inlets.setLayout(new BoxLayout(p_inlets, BoxLayout.PAGE_AXIS));
        p_inlets.setAlignmentX(LEFT_ALIGNMENT);
        p_inlets.setAlignmentY(TOP_ALIGNMENT);

        p_outlets.setBackground(Theme.Object_Default_Background);
        p_outlets.setLayout(new BoxLayout(p_outlets, BoxLayout.PAGE_AXIS));
        p_outlets.setAlignmentX(RIGHT_ALIGNMENT);
        p_outlets.setAlignmentY(TOP_ALIGNMENT);

        p_params.setBackground(Theme.Object_Default_Background);
        p_params.setAlignmentX(LEFT_ALIGNMENT);

        p_displays.setBackground(Theme.Object_Default_Background);
        p_displays.setAlignmentX(LEFT_ALIGNMENT);
    }

    public AxoObjectInstance() {
        super();
        init1();
    }

    public AxoObjectInstance(AxoObject type, Patch patch1, String InstanceName1, Point location) {
        super(type, patch1, InstanceName1, location);
        init1();
    }

    public void OpenEditor() {
        getType().OpenEditor(editorBounds, editorActiveTabIndex);
    }

    @Override
    public void setInstanceName(String s) {
        super.setInstanceName(s);
        for (InletInstance i : inletInstances) {
            i.RefreshName();
        }
        for (OutletInstance i : outletInstances) {
            i.RefreshName();
        }
    }

    @Override
    public InletInstance GetInletInstance(String n) {
        for (InletInstance o : inletInstances) {
            if (n.equals(o.GetLabel())) {
                return o;
            }
        }
        for (InletInstance o : inletInstances) {
            String s = Synonyms.instance().inlet(n);
            if (o.GetLabel().equals(s)) {
                return o;
            }
        }
        return null;
    }

    @Override
    public OutletInstance GetOutletInstance(String n) {
        for (OutletInstance o : outletInstances) {
            if (n.equals(o.GetLabel())) {
                return o;
            }
        }
        for (OutletInstance o : outletInstances) {
            String s = Synonyms.instance().outlet(n);
            if (o.GetLabel().equals(s)) {
                return o;
            }
        }
        return null;
    }

    public ParameterInstance GetParameterInstance(String n) {
        for (ParameterInstance o : parameterInstances) {
            if (n.equals(o.parameter.name)) {
                return o;
            }
        }
        return null;
    }

    @Override
    public void Lock() {
        super.Lock();
        for (AttributeInstance a : attributeInstances) {
            a.Lock();
        }
        for (ParameterInstance p : parameterInstances) {
            if (p.isFrozen()) {
                p.Lock();
            }
        }
    }

    public void updateObj() {
        getPatch().ChangeObjectInstanceType(this, this.getType());
        getPatch().cleanUpIntermediateChangeStates(3);
    }

    @Override
    public void Unlock() {
        super.Unlock();
        for (AttributeInstance a : attributeInstances) {
            a.UnLock();
        }
        for (ParameterInstance p : parameterInstances) {
            if (p.isFrozen()) {
                p.UnLock();
            }
        }
        if (deferredObjTypeUpdate) {
            updateObj();
            deferredObjTypeUpdate = false;
        }
    }

    @Override
    public ArrayList<InletInstance> GetInletInstances() {
        return inletInstances;
    }

    @Override
    public ArrayList<OutletInstance> GetOutletInstances() {
        return outletInstances;
    }

    @Override
    public String GenerateInstanceDataDeclaration2() {
        String c = "\n";
        if (getType().sLocalData != null && !getType().sLocalData.isEmpty()) {
            c += I+I + "/* Object Local Code Tab */\n";
            String s = getType().sLocalData;
            s = I+I + s.replace("\n", "\n\t\t");
            s = s.replaceAll("attr_parent", getCInstanceName());
            c += s + "\n";
        }
        return c;
    }

    @Override
    public boolean hasStruct() {
        if (getParameterInstances() != null && !(getParameterInstances().isEmpty())) {
            return true;
        }
        if (getType().sLocalData == null) {
            return false;
        }
        return getType().sLocalData.length() != 0;
    }

    @Override
    public boolean hasInit() {
        if (getType().sInitCode == null) {
            return false;
        }
        return getType().sInitCode.length() != 0;
    }

    public String GenerateInstanceCodePlusPlus(String classname) {
        String c = "";
        for (ParameterInstance p : parameterInstances) {
            if (p.isFrozen()) {
                c += I+I + "int32_t " + p.GetCName() + "; /* Frozen parameter: " + p.GetObjectInstance().getCInstanceName() + ":" + p.getLegalName() + " */\n";
            }
            else {
                c += I+I + p.GenerateCodeDeclaration(classname);
            }
        }
        c += GenerateInstanceDataDeclaration2();
        for (AttributeInstance a : attributeInstances) {
            if (a.CValue() != null) {
                c = c.replaceAll(a.GetCName(), a.CValue());
            }
        }
        return c + "\n";
    }

    @Override
    public String GenerateInitCodePlusPlus(String classname) {
        String c = "";
//        if (hasStruct())
//            c = "  void " + GenerateInitFunctionName() + "(" + GenerateStructName() + " * x ) {\n";
//        else
//        if (!classname.equals("one"))
        c += I+I+I + "parent = _parent;\n";
        for (ParameterInstance p : parameterInstances) {
            if (p.isFrozen()) {
                
                c += I+I+I + p.GetCName() + " = ";
                /* Do parameter value mapping in Java so save MCU memory.
                 * These are the same functions like in firmware/parameter_functions.h.
                 */
                String pfun = p.GetPFunction();
                if (pfun != null && !pfun.equals("")) {
                    int S28_MAX = (1<<27)-1;
                    int S28_MIN = -(1<<27);

                    int signedClampedVal = p.GetValueRaw();
                    signedClampedVal = signedClampedVal < S28_MIN ? S28_MIN : signedClampedVal > S28_MAX ? S28_MAX : signedClampedVal;

                    int unsignedClampedVal = p.GetValueRaw();
                    unsignedClampedVal = unsignedClampedVal < S28_MIN ? S28_MIN : unsignedClampedVal > S28_MAX ? S28_MAX : unsignedClampedVal;

                    if (pfun.equals("pfun_signed_clamp")) {
                        c += signedClampedVal + "; /* pfun_signed_clamp */";
                    }

                    else if (pfun.equals("pfun_unsigned_clamp")) {
                        c += unsignedClampedVal + "; /* pfun_unsigned_clamp */";
                    }

                    else if (pfun.equals("pfun_signed_clamp_fullrange")) {
                        c += (signedClampedVal << 4) + "; /* pfun_signed_clamp_fullrange */";
                    }

                    else if (pfun.equals("pfun_unsigned_clamp_fullrange")) {
                        c += (unsignedClampedVal << 4) + "; /* pfun_unsigned_clamp_fullrange */";
                    }

                    else if (pfun.equals("pfun_signed_clamp_squarelaw")) {
                        long psat = (long) signedClampedVal;
                        long mappedVal = 0;
                        if (psat > 0) {
                            mappedVal = ((psat * psat) >> 31);
                        }
                        else {
                            mappedVal = -((psat * psat) >> 31);
                        }
                        c += (int) mappedVal + "; /* pfun_signed_clamp_squarelaw */";
                    }

                    else if (pfun.equals("pfun_unsigned_clamp_squarelaw")) {
                        long psat = unsignedClampedVal;
                        long mappedVal = ((psat * psat) >> 31);
                        c += (int) mappedVal + "; /* pfun_unsigned_clamp_squarelaw */";
                    }

                    else if (pfun.equals("pfun_signed_clamp_fullrange_squarelaw")) {
                        long psat = (long) signedClampedVal;
                        long mappedVal;
                        if (psat > 0) {
                            mappedVal = ((psat * psat) >> 23);
                        }
                        else {
                            mappedVal = -((psat * psat) >> 23);
                        }
                        c += (int) mappedVal + "; /* pfun_signed_clamp_fullrange_squarelaw */";
                    }

                    else if (pfun.equals("pfun_unsigned_clamp_fullrange_squarelaw")) {
                        long psat = (long) unsignedClampedVal;
                        long mappedVal = ((psat * psat) >> 23);
                        c += (int) mappedVal + "; /* pfun_unsigned_clamp_fullrange_squarelaw */";
                    }

                    else if (pfun.equals("pfun_kexpltime") || pfun.equals("pfun_kexpdtime")) {

                        /* After a calculated table using Java double math proved "too precise" to emulate Cortex-M signed integer math, this exponential parameter value lookup table has been extracted from a Ksoloti Core using a printout of klineartime.exp2 dial values. */
                        int[] intTable = {
                            /* 130 entries: 129 integer pitch values (-64..+64) + 1 extra for interpolation */
                            0x11B83BF4, 0x10B9A2D0, 0x0FC9535E, 0x0EE680DA, 0x0E10698E, 0x0D465619, 0x0C879A1C, 0x0BD392DB,
                            0x0B29A60E, 0x0A8942D0, 0x09F1E04C, 0x0962FC95, 0x08DC1E14, 0x085CD147, 0x07E4A9AF, 0x07734077,
                            0x070834B1, 0x06A32B0C, 0x0643CD23, 0x05E9C962, 0x0594D307, 0x0544A17E, 0x04F8F010, 0x04B17E4A,
                            0x046E0F09, 0x042E68A3, 0x03F254D7, 0x03B9A03B, 0x03841A58, 0x03519586, 0x0321E691, 0x02F4E4B1,
                            0x02CA6983, 0x02A250BE, 0x027C7807, 0x0258BF25, 0x02370782, 0x02173457, 0x01F92A6B, 0x01DCD01D,
                            0x01C20D2E, 0x01A8CAC2, 0x0190F346, 0x017A725B, 0x016534C1, 0x0151285C, 0x013E3C06, 0x012C5F92,
                            0x011B83C0, 0x010B9A2A, 0x00FC9535, 0x00EE680E, 0x00E10695, 0x00D46561, 0x00C879A2, 0x00BD392D,
                            0x00B29A60, 0x00A8942E, 0x009F1E03, 0x00962FC9, 0x008DC1E0, 0x0085CD14, 0x007E4A9A, 0x00773407,
                            0x0070834B, 0x006A32B0, 0x00643CD1, 0x005E9C96, 0x00594D30, 0x00544A16, 0x004F8F00, 0x004B17E4,
                            0x0046E0F0, 0x0042E68A, 0x003F254D, 0x003B9A03, 0x003841A5, 0x00351958, 0x00321E68, 0x002F4E4B,
                            0x002CA697, 0x002A250B, 0x0027C780, 0x00258BF2, 0x00237078, 0x00217344, 0x001F92A6, 0x001DCD01,
                            0x001C20D2, 0x001A8CAB, 0x00190F34, 0x0017A725, 0x0016534B, 0x00151285, 0x0013E3C0, 0x0012C5F8,
                            0x0011B83B, 0x0010B9A2, 0x000FC953, 0x000EE680, 0x000E1069, 0x000D4655, 0x000C879A, 0x000BD392,
                            0x000B29A5, 0x000A8942, 0x0009F1DF, 0x000962FC, 0x0008DC1D, 0x00085CD1, 0x0007E4A9, 0x00077340,
                            0x00070834, 0x0006A32A, 0x000643CC, 0x0005E9C9, 0x000594D2, 0x000544A1, 0x0004F8EF, 0x0004B17E,
                            0x00046E0E, 0x00042E68, 0x0003F254, 0x0003B99F, 0x0003841A, 0x00035195, 0x000321E6, 0x0002F4E4,
                            0x0002CA69, 0x0002CA69
                        };
                        
                        // /* Too accurate java version of pitch table (firmware/axoloti_math.c): */
                        // /* Attempt to emulate single precision and "round to zero" behaviour of Cortex FPU */
                        // MathContext mc = new MathContext(10, RoundingMode.DOWN);

                        // for (int i = 0; i < pitchTable.length; i++) {
                        //     BigDecimal frq_hz = new BigDecimal(440.0 * Math.pow(2.0, (i - 69.0 - 64.0) / 12.0), mc);
                        //     BigDecimal phi = new BigDecimal(4.0 * (double) (1 << 30) * frq_hz.floatValue() / 48000.0, mc);
                        //     pitchTable[i] = phi.intValue();
                        //     // LOGGER.log(Level.INFO, "pitchtable: " + pitchTable[i]);
                        // }

                        /* Calculate "integer" and "fractional" pitch */
                        /* Scale parameter value to -64.00..64.00  */
                        /* Using BigDecimal for its easy rounding functions. */
                        BigDecimal pindex = BigDecimal.valueOf(signedClampedVal / 2097152.0d).setScale(2, RoundingMode.HALF_UP);
                        
                        /* Get integer part of index */
                        int pindex_int = (int) pindex.doubleValue();
                        /* Emulate round down on negative values */
                        if (pindex.doubleValue() < 0.0d) {
                            pindex_int -= 1;
                        }
                        /* Clamp */
                        if (pindex_int < -64) pindex_int = -64;
                        if (pindex_int > 64) pindex_int = 64;

                        /* Fetch two adjacent indices from pitch table */
                        double y1 = intTable[64 + pindex_int];
                        double y2 = intTable[64 + 1 + pindex_int];

                        /* Interpolate between integer and fractional parts of index */
                        BigDecimal p_dec_ratio = BigDecimal.valueOf(pindex.doubleValue() - pindex_int).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal p_int_ratio = BigDecimal.valueOf(1 - p_dec_ratio.doubleValue()).setScale(2, RoundingMode.HALF_UP);

                        BigDecimal r  = BigDecimal.valueOf(y1 * p_int_ratio.doubleValue() + y2 * p_dec_ratio.doubleValue());

                        BigDecimal final_bd = BigDecimal.valueOf(r.doubleValue()).setScale(0, RoundingMode.HALF_UP);
                        int final_val = final_bd.intValue();

                        if (pfun.equals("pfun_kexpltime")) {
                            c += final_val + "; /* pfun_kexpltime) */";
                        }
                        else if (pfun.equals("pfun_kexpdtime")) {
                            c += (0x7FFFFFFF - final_val) + "; /* pfun_kexpdtime) */";
                        }
                    }
                }
                else {
                    c += p.GetValueRaw() + ";"; 
                }
                c += "\n";

                if (p.parameter.PropagateToChild != null) {
                    // TODO: This is probably where forwarding the frozen value to the child parameter should happen?
                }
            }
            else {
                if (p.parameter.PropagateToChild != null) {
                    c += I+I+I + p.PExName("parent->") + ".pfunction = PropagateToSub; // on Parent: " + p.GetObjectInstance().getLegalName() + ":" + p.getLegalName() + " (" + p.parameter.PropagateToChild + ")\n";
                    c += I+I+I + p.PExName("parent->") + ".finalvalue = (int32_t)(&(parent->objectinstance_"
                               + getLegalName() + "_i.PExch[objectinstance_" + getLegalName() + "::PARAM_INDEX_"
                               + p.parameter.PropagateToChild + "]));\n";
                }
                else {
                    c += I+I+I + p.GenerateCodeInit("parent->", "");
                }
                c += I+I+I + p.GenerateCodeInitModulator("parent->", "");
            }
        }

        c += I+I+I;
        for (DisplayInstance p : displayInstances) {
            String s = "";
            s += p.GenerateCodeInit("");
            c += s.replace("\n", "\n\t\t\t");
        }

        if (getType().sInitCode != null && !getType().sInitCode.isEmpty()) {
            c += "\n" + I+I+I + "/* Object Init Code Tab */\n";
            String s = getType().sInitCode;
            for (AttributeInstance a : attributeInstances) {
                s = s.replace(a.GetCName(), a.CValue());
            }
            s = I+I+I + s.replace("\n", "\n\t\t\t");
            
            /* Reverse-analyze if generated init code contains code which changes
             * the audio input or output config. This is a workaround to ensure
             * compatibility with axoloti-factory inconfig and outconfig objects.*/
            String opt = "/* Code was optimized out during code generation */";
            if (s.contains("AudioInputMode = A_STEREO;")) {
                patch.setAudioInputMode(0);
                s = s.replace("AudioInputMode = A_STEREO;", opt);
                LOGGER.log(Level.INFO, "audio/inconfig: input mode set to STEREO");
            }
            if (s.contains("AudioInputMode = A_MONO;")) {
                patch.setAudioInputMode(1);
                s = s.replace("AudioInputMode = A_MONO;", opt);
                LOGGER.log(Level.INFO, "audio/inconfig: input mode set to MONO");
            }
            if (s.contains("AudioInputMode = A_BALANCED;")) {
                patch.setAudioInputMode(2);
                s = s.replace("AudioInputMode = A_BALANCED;", opt);
                LOGGER.log(Level.INFO, "audio/inconfig: input mode set to BALANCED");
            }

            if (s.contains("AudioOutputMode = A_MONO;")) {
                patch.setAudioOutputMode(1);
                s = s.replace("AudioOutputMode = A_MONO;", opt);
                LOGGER.log(Level.INFO, "audio/outconfig: output mode set to MONO");
            }
            if (s.contains("AudioOutputMode = A_BALANCED;")) {
                patch.setAudioOutputMode(2);
                s = s.replace("AudioOutputMode = A_BALANCED;", opt);
                LOGGER.log(Level.INFO, "audio/outconfig: output mode set to BALANCED");
            }
            if (s.contains("AudioOutputMode = A_STEREO;")) {
                patch.setAudioOutputMode(0);
                s = s.replace("AudioOutputMode = A_STEREO;", opt);
                LOGGER.log(Level.INFO, "audio/outconfig: output mode set to STEREO");
            }

            c += s;
        }

        String d = I+I + "public: void Init(" + classname + " *_parent";

        if (!displayInstances.isEmpty()) {
            for (DisplayInstance p : displayInstances) {
                if (p.display.getLength() > 0) {
                    d += ", ";
                    if (p.display.getDatatype().isPointer()) {
                        d += p.display.getDatatype().CType() + " " + p.GetCName();
                    }
                    else {
                        d += p.display.getDatatype().CType() + " &" + p.GetCName();
                    }
                }
            }
        }
        d += ") {\n" + c;
        d += "\n" + I+I + "}\n\n";
        return d;
    }

    @Override
    public String GenerateDisposeCodePlusPlus(String classname) {
        String c = I+I + "public: void Dispose() {\n";
        if (getType().sDisposeCode != null && !getType().sDisposeCode.isEmpty()) {
            c += "\n" + I+I+I + "/* Object Dispose Code Tab */\n";
            String s = getType().sDisposeCode;
            s = I+I+I + s.replace("\n", "\n\t\t\t");
            for (AttributeInstance a : attributeInstances) {
                s = I+I + s.replaceAll(a.GetCName(), a.CValue());
            }
            c += s + "\n\n";
        }
        c += I+I + "}\n\n";
        return c;
    }

    public String GenerateKRateCodePlusPlus(String vprefix) {
        if (getType().sKRateCode != null && !getType().sKRateCode.isEmpty()) {
            String s = getType().sKRateCode;
            s = I+I+I + s.replace("\n", "\n\t\t\t");
            
            for (AttributeInstance a : attributeInstances) {
                s = s.replaceAll(a.GetCName(), a.CValue());
            }
            
            s = s.replace("CGENATTR_instancename", getCInstanceName());
            s = s.replace("CGENATTR_legalname", getLegalName());
            
            String h = "\n" + I+I+I + "/* Object K-Rate Code Tab */\n";
            return h + s + "\n";
        }
        return "";
    }

    public String GenerateSRateCodePlusPlus(String vprefix) {
        if (getType().sSRateCode != null && !getType().sSRateCode.isEmpty()) {
            String s = "\n" + I+I+I + "uint32_t buffer_index;\n"
                            + I+I+I + "for (buffer_index = 0; buffer_index < BUFSIZE; buffer_index++) {\n"
                     + "\n" + I+I+I+I + "/* Object S-Rate Code Tab */\n"
                            + I+I+I+I + getType().sSRateCode.replace("\n", "\n\t\t\t\t")
                     + "\n" + I+I+I + "}\n";

            for (AttributeInstance a : attributeInstances) {
                s = s.replaceAll(a.GetCName(), a.CValue());
            }
            for (InletInstance i : inletInstances) {
                if (i.GetDataType() instanceof Frac32buffer) {
                    s = s.replaceAll(i.GetCName(), i.GetCName() + "[buffer_index]");
                }
            }
            for (OutletInstance i : outletInstances) {
                if (i.GetDataType() instanceof Frac32buffer) {
                    s = s.replaceAll(i.GetCName(), i.GetCName() + "[buffer_index]");
                }
            }

            s = s.replace("CGENATTR_instancename", getCInstanceName());
            s = s.replace("CGENATTR_legalname", getLegalName());

            return s;
        }
        return "";
    }

    public String GenerateDoFunctionPlusPlus(String ClassName) {
        String s = "";
        boolean comma = false;
        s += I+I + "public: void dsp(";

        for (InletInstance i : inletInstances) {
            if (comma) {
                s += ", ";
            }
            s += "const " + i.GetDataType().CType() + " " + i.GetCName();
            comma = true;
        }

        for (OutletInstance i : outletInstances) {
            if (comma) {
                s += ", ";
            }
            s += i.GetDataType().CType() + " &" + i.GetCName();
            comma = true;
        }

        for (ParameterInstance i : parameterInstances) {
            if (!i.isFrozen() && i.parameter.PropagateToChild == null) {
                if (comma) {
                    s += ", ";
                }
                s += i.parameter.CType() + " " + i.GetCName();
                comma = true;
            }
        }

        for (DisplayInstance i : displayInstances) {
            if (i.display.getLength() > 0) {
                if (comma) {
                    s += ", ";
                }
                if (i.display.getDatatype().isPointer()) {
                    s += i.display.getDatatype().CType() + " " + i.GetCName();
                } else {
                    s += i.display.getDatatype().CType() + " &" + i.GetCName();
                }
                comma = true;
            }
        }

        s += ") {\n";//\n" + I+I+I + "/* Object DSP Loop */\n";
        s += GenerateKRateCodePlusPlus("");
        s += GenerateSRateCodePlusPlus("");
        s += I+I + "}\n\n";

        return s;
    }

    public final static String MidiHandlerFunctionHeader = "void MidiInHandler(midi_device_t dev, uint8_t port, uint8_t status, uint8_t data1, uint8_t data2) {\n";

    @Override
    public String GenerateClass(String ClassName) {
        String s  = I + "class " + getCInstanceName() + " {\n\n"
                  + I+I + "public: // v1\n"
                  + I+I + ClassName + " *parent;\n";

        s += GenerateInstanceCodePlusPlus(ClassName);
        s += GenerateInitCodePlusPlus(ClassName);
        s += GenerateDisposeCodePlusPlus(ClassName);
        s += GenerateDoFunctionPlusPlus(ClassName);

        String d3 = GenerateCodeMidiHandler("");
        if (!d3.isEmpty()) {
            s += I+I + MidiHandlerFunctionHeader;
            s += d3;
            s += I+I + "}\n";
        }

        s += I + "};\n\n";

        return s;
    }

    @Override
    public String GenerateCodeMidiHandler(String vprefix) {
        String s = "";
        if (getType().sMidiCode != null && !getType().sMidiCode.isEmpty()) {
            s += I+I+I + "/* Object Midi Handler */\n";
            s += I+I+I + getType().sMidiCode.replace("\n", "\n\t\t\t");
        }
        for (ParameterInstance i : parameterInstances) {
            if (!i.isFrozen()) {
                s += i.GenerateCodeMidiHandler("");
            }
        }
        for (AttributeInstance a : attributeInstances) {
            s = s.replaceAll(a.GetCName(), a.CValue());
        }

        s = s.replace("CGENATTR_instancename", getCInstanceName());
        s = s.replace("CGENATTR_legalname", getLegalName());

        if (s.length() > 0) {
            return "\n" + s + "\n";
        }
        else {
            return "";
        }
    }

    @Override
    public String GenerateCallMidiHandler() {
        if (getType().sMidiCode != null && !getType().sMidiCode.isEmpty()) {
            return I+I + getCInstanceName() + "_i.MidiInHandler(dev, port, status, data1, data2);\n";
        }
        for (ParameterInstance pi : getParameterInstances()) {
            if (!pi.GenerateCodeMidiHandler("").isEmpty()) {
                return I+I + getCInstanceName() + "_i.MidiInHandler(dev, port, status, data1, data2);\n";
            }
        }
        return "";
    }

    @Override
    public boolean providesModulationSource() {
        AxoObject atype = getType();
        if (atype == null) {
            return false;
        } else {
            return atype.providesModulationSource();
        }
    }

    @Override
    public AxoObject getType() {
        return (AxoObject) super.getType();
    }

    @Override
    public void PromoteToOverloadedObj() {
        if (getType() instanceof AxoObjectFromPatch) {
            return;
        }
        if (getType() instanceof AxoObjectPatcher) {
            return;
        }
        if (getType() instanceof AxoObjectPatcherObject) {
            return;
        }
        String id = typeName;
        ArrayList<AxoObjectAbstract> candidates = MainFrame.axoObjects.GetAxoObjectFromName(id, patch.GetCurrentWorkingDirectory());
        if (candidates == null) {
            return;
        }
        if (candidates.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Could not resolve any candidates {0}", id);
        }
        if (candidates.size() == 1) {
            return;
        }

        int ranking[];
        ranking = new int[candidates.size()];
        /* auto-choose depending on 1st connected inlet */

        //      InletInstance i = null;// = GetInletInstances().get(0);
        for (InletInstance j : GetInletInstances()) {
            Net n = patch.GetNet(j);
            if (n == null) {
                continue;
            }
            DataType d = n.GetDataType();
            if (d == null) {
                continue;
            }
            String name = j.getInlet().getName();
            for (int i = 0; i < candidates.size(); i++) {
                AxoObjectAbstract o = candidates.get(i);
                Inlet i2 = o.GetInlet(name);
                if (i2 == null) {
                    continue;
                }
                if (i2.getDatatype().equals(d)) {
                    ranking[i] += 10;
                } else if (d.IsConvertableToType(i2.getDatatype())) {
                    ranking[i] += 2;
                }
            }
        }

        int max = -1;
        int maxi = 0;
        for (int i = 0; i < candidates.size(); i++) {
            if (ranking[i] > max) {
                max = ranking[i];
                maxi = i;
            }
        }
        AxoObjectAbstract selected = candidates.get(maxi);
        int rindex = candidates.indexOf(getType());
        if (rindex >= 0) {
            if (ranking[rindex] == max) {
                selected = getType();
            }
        }

        if (selected == null) {
            //LOGGER.log(Level.INFO,"No promotion to null" + this + " to " + selected);            
            return;
        }
        if (selected != getType()) {
            LOGGER.log(Level.FINE, "Promoting " + this + " to " + selected);
            patch.ChangeObjectInstanceType(this, selected);
            patch.cleanUpIntermediateChangeStates(4);
        } else {
//            LOGGER.log(Level.INFO, "No promotion for {0}", typeName);
        }
    }

    @Override
    public ArrayList<DisplayInstance> GetDisplayInstances() {
        return displayInstances;
    }

    Rectangle editorBounds;
    Integer editorActiveTabIndex;

    @Override
    public void ObjectModified(Object src) {
        if (getPatch() != null) {
            if (!getPatch().IsLocked()) {
                updateObj();
            } else {
                deferredObjTypeUpdate = true;
            }
        }

        try {
            AxoObject o = (AxoObject) src;
            if (o.editor != null && o.editor.getBounds() != null) {
                editorBounds = o.editor.getBounds();
                editorActiveTabIndex = o.editor.getActiveTabIndex();
                this.getType().editorBounds = editorBounds;
                this.getType().editorActiveTabIndex = editorActiveTabIndex;
            }
        } catch (ClassCastException ex) {
        }
    }

    @Override
    public ArrayList<SDFileReference> GetDependendSDFiles() {
        ArrayList<SDFileReference> files = getType().filedepends;
        if (files == null){
            files = new ArrayList<SDFileReference>();
        } else {
            String p1 = getType().sObjFilePath;
            if (p1==null) {
                /* embedded object, reference path is of the patch */
                p1 = getPatch().getFileNamePath();
                if (p1 == null) {
                    p1 = "";
                }
            }
            File f1 = new File(p1);
            java.nio.file.Path p = f1.toPath().getParent();
            for (SDFileReference f: files){                
                f.Resolve(p);
            }
        }
        for (AttributeInstance a : attributeInstances) {
            ArrayList<SDFileReference> f2 = a.GetDependendSDFiles();
            if (f2 != null) {
                files.addAll(f2);
            }
        }
        return files;
    }

    void ConvertToPatchPatcher() {
        if (IsLocked()) {
            return;
        }
        // ArrayList<AxoObjectAbstract> ol = MainFrame.mainframe.axoObjects.GetAxoObjectFromName("patch/patcher", null);
        ArrayList<AxoObjectAbstract> ol = MainFrame.axoObjects.GetAxoObjectFromName("patch/patcher", null);
        assert (!ol.isEmpty());
        AxoObjectAbstract o = ol.get(0);
        String iname = getInstanceName();
        AxoObjectInstancePatcher oi = (AxoObjectInstancePatcher) getPatch().ChangeObjectInstanceType1(this, o);
        AxoObjectFromPatch ao = (AxoObjectFromPatch) getType();
        PatchFrame pf = PatchGUI.OpenPatch(ao.f);
        oi.pf = pf;
        oi.pg = pf.getPatch();
        oi.setInstanceName(iname);
        oi.updateObj();
        getPatch().delete(this);
        getPatch().SetDirty();
    }

    void ConvertToEmbeddedObj() {
        if (IsLocked()) {
            return;
        }
        try {

            ArrayList<AxoObjectAbstract> ol = MainFrame.axoObjects.GetAxoObjectFromName("patch/object", null);
            assert (!ol.isEmpty());
            AxoObjectAbstract o = ol.get(0);
            String iname = getInstanceName();
            AxoObjectInstancePatcherObject oi = (AxoObjectInstancePatcherObject) getPatch().ChangeObjectInstanceType1(this, o);
            AxoObject ao = getType();
            oi.ao = new AxoObjectPatcherObject(ao.id, ao.sDescription);
            oi.ao.copy(ao);
            oi.ao.sObjFilePath = "";
            oi.ao.upgradeSha = null;
            oi.ao.CloseEditor();
            oi.setInstanceName(iname);
            oi.updateObj();
            getPatch().delete(this);
            getPatch().SetDirty();
        } catch (CloneNotSupportedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Persist
    public void Persist() {
        AxoObject o = getType();
        if (o != null) {
            if (o.uuid != null && !o.uuid.isEmpty()) {
                typeUUID = o.uuid;
                typeSHA = null;
            }
        }
    }

    @Override
    public void Close() {
        super.Close();
        for (AttributeInstance a : attributeInstances) {
            a.Close();
        }
    }
}
