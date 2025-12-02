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

package axoloti.object;

import axoloti.Patch;
import axoloti.PatchGUI;
import axoloti.attribute.*;
import axoloti.datatypes.Value;
import axoloti.datatypes.ValueFrac32;
import axoloti.datatypes.ValueInt32;
import axoloti.inlets.InletInstance;
import axoloti.inlets.InletInstanceZombie;
import axoloti.outlets.OutletInstance;
import axoloti.outlets.OutletInstanceZombie;
import axoloti.parameters.*;
import axoloti.ui.Theme;
import axoloti.utils.Constants;
import components.LabelComponent;
import components.PopupIcon;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

import org.simpleframework.xml.Root;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "zombie")
public class AxoObjectInstanceZombie extends AxoObjectInstanceAbstract {

    public ArrayList<InletInstance> inletInstances = new ArrayList<InletInstance>();
    public ArrayList<OutletInstance> outletInstances = new ArrayList<OutletInstance>();
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
        @ElementList(entry = "bin8", type = ParameterInstanceBin8.class, inline = true, required = false),
        @ElementList(entry = "bin12", type = ParameterInstanceBin12.class, inline = true, required = false),
        @ElementList(entry = "bin16", type = ParameterInstanceBin16.class, inline = true, required = false),
        @ElementList(entry = "bin32", type = ParameterInstanceBin32.class, inline = true, required = false),
        @ElementList(entry = "bool32.tgl", type = ParameterInstanceBin1.class, inline = true, required = false),
        @ElementList(entry = "bool32.mom", type = ParameterInstanceBin1Momentary.class, inline = true, required = false)})
    public ArrayList<ParameterInstance> parameterInstances = new ArrayList<ParameterInstance>();

    @Path("attribs")
    @ElementListUnion({
        @ElementList(entry = "objref", type = AttributeInstanceObjRef.class, inline = true, required = false),
        @ElementList(entry = "table", type = AttributeInstanceTablename.class, inline = true, required = false),
        @ElementList(entry = "combo", type = AttributeInstanceComboBox.class, inline = true, required = false),
        @ElementList(entry = "int", type = AttributeInstanceInt32.class, inline = true, required = false),
        @ElementList(entry = "spinner", type = AttributeInstanceSpinner.class, inline = true, required = false),
        @ElementList(entry = "file", type = AttributeInstanceSDFile.class, inline = true, required = false),
        @ElementList(entry = "text", type = AttributeInstanceTextEditor.class, inline = true, required = false)})
    public ArrayList<AttributeInstance> attributeInstances = new ArrayList<AttributeInstance>();

    public final JPanel p_attribs = new JPanel();
    public final JPanel p_params = new JPanel();

    public AxoObjectInstanceZombie() {
    }

    public AxoObjectInstanceZombie(AxoObjectAbstract type, Patch patch1, String InstanceName1, Point location) {
        super(type, patch1, InstanceName1, location);
    }

    @Override
    public void PostConstructor() {
        super.PostConstructor();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setOpaque(true);
        setBackground(Theme.Object_Zombie_Background);

        final PopupIcon popupIcon = new PopupIcon();
        popupIcon.setPopupIconListener(
            new PopupIcon.PopupIconListener() {
                @Override
                public void ShowPopup() {
                    JPopupMenu popup = CreatePopupMenu();
                    popupIcon.add(popup);
                    popup.show(popupIcon,
                            0, popupIcon.getHeight());
                }
            });
        popupIcon.setAlignmentX(LEFT_ALIGNMENT);
        Titlebar.add(popupIcon);

        LabelComponent idlbl = new LabelComponent("");
        if (typeName != null) {
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
        }
        idlbl.setForeground(Theme.Object_TitleBar_Foreground);
        idlbl.setAlignmentX(LEFT_ALIGNMENT);
        idlbl.setFont(Constants.FONT_BOLD);
        Titlebar.add(idlbl);

        Titlebar.setToolTipText("<html><b>" + typeName + "</b><br/><br/><b>Unresolved object!</b><br/>The object/subpatch could not be created.<br/>This could be either because the library this object/subpatch<br/>belongs to is not set up in the preferences, or because<br/>the patch depends on additional local files (.axo or .axs)<br/>which were not found.");
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

        p_attribs.removeAll();
        p_params.removeAll();

        p_attribs.setLayout(new BoxLayout(p_attribs, BoxLayout.PAGE_AXIS));
        p_params.setLayout(new BoxLayout(p_params, BoxLayout.PAGE_AXIS));

        p_attribs.add(Box.createHorizontalGlue());
        p_params.add(Box.createHorizontalGlue());

        if (attributeInstances != null) {
            for (AttributeInstance attr : attributeInstances) {
                String nameValue = "attrib " + attr.getName();
                JLabel attrlbl = new JLabel(nameValue);
                attrlbl.setFont(Constants.FONT);
                attrlbl.setAlignmentX(LEFT_ALIGNMENT);
                p_attribs.add(attrlbl);
            }
        }

        if (parameterInstances != null) {
            for (ParameterInstance param : parameterInstances) {
                Value vl = param.getValue();
                String paramVal = "";
                if (vl instanceof ValueFrac32) {
                    paramVal += vl.getDouble();
                }
                else if (vl instanceof ValueInt32) {
                    paramVal += vl.getInt();
                }
                String nameValue = "param " + param.getName() + " = " + paramVal;
                JLabel paramlbl = new JLabel(nameValue);
                paramlbl.setFont(Constants.FONT);
                paramlbl.setAlignmentX(LEFT_ALIGNMENT);
                p_params.add(paramlbl);
            }
        }

        /* ... */

        add(p_attribs);
        add(p_params);
        
        setLocation(x, y);
        resizeToGrid();
    }

    @Override
    JPopupMenu CreatePopupMenu() {
        JPopupMenu popup = super.CreatePopupMenu();
        JMenuItem popm_substitute = new JMenuItem("Replace");
        popm_substitute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ((PatchGUI) patch).ShowClassSelector(AxoObjectInstanceZombie.this.getLocation(), AxoObjectInstanceZombie.this, null, true);
            }
        });
        popup.add(popm_substitute);
        JMenuItem popm_editInstanceName = new JMenuItem("Edit Instance Name");
        popm_editInstanceName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addInstanceNameEditor();
            }
        });
        popup.add(popm_editInstanceName);
        return popup;
    }

    @Override
    public void setInstanceName(String s) {
        super.setInstanceName(s);
        resizeToGrid();
        repaint();
    }

    @Override
    public String getCInstanceName() {
        return "";
    }

    @Override
    public InletInstance getInletInstance(String n) {
        if (inletInstances != null) {
            for (InletInstance i : inletInstances) {
                if (i.GetLabel().equals(n)) {
                    return i;
                }
            }
        }
        InletInstance i = new InletInstanceZombie(this, n);
        add(i);
        inletInstances.add(i);
        resizeToGrid();
        return i;
    }

    @Override
    public OutletInstance getOutletInstance(String n) {
        if (outletInstances != null) {
            for (OutletInstance i : outletInstances) {
                if (n.equals(i.GetLabel())) {
                    return i;
                }
            }
        }
        OutletInstance i = new OutletInstanceZombie(this, n);
        add(i);
        outletInstances.add(i);
        resizeToGrid();
        return i;
    }
    
    @Override
    public String GenerateClass(String ClassName) {
        return "#error \"Unresolved (zombie) object: \"" + typeName + "\", labeled \"" + getInstanceName() + "\", in patch: " + getPatch().getFileNamePath() + "\"\n";
    }

    @Override
    public ArrayList<InletInstance> getInletInstances() {
        return inletInstances;
    }

    @Override
    public ArrayList<OutletInstance> getOutletInstances() {
        return outletInstances;
    }
}
