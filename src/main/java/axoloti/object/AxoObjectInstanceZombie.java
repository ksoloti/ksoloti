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
package axoloti.object;

import axoloti.Patch;
import axoloti.PatchGUI;
import axoloti.Theme;
import axoloti.inlets.InletInstance;
import axoloti.inlets.InletInstanceZombie;
import axoloti.outlets.OutletInstance;
import axoloti.outlets.OutletInstanceZombie;
import axoloti.utils.Constants;
import components.LabelComponent;
import components.PopupIcon;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

import org.simpleframework.xml.Root;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "zombie")
public class AxoObjectInstanceZombie extends AxoObjectInstanceAbstract {

    public ArrayList<InletInstance> inletInstances = new ArrayList<InletInstance>();
    public ArrayList<OutletInstance> outletInstances = new ArrayList<OutletInstance>();

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
        setBackground(Theme.getCurrentTheme().Object_Zombie_Background);

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
        idlbl.setAlignmentX(LEFT_ALIGNMENT);
        idlbl.setForeground(Theme.getCurrentTheme().Object_TitleBar_Foreground);
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
        
        setLocation(x, y);
        resizeToGrid();
    }

    @Override
    JPopupMenu CreatePopupMenu() {
        JPopupMenu popup = super.CreatePopupMenu();
        JMenuItem popm_substitute = new JMenuItem("replace");
        popm_substitute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ((PatchGUI) patch).ShowClassSelector(AxoObjectInstanceZombie.this.getLocation(), AxoObjectInstanceZombie.this, null, true);
            }
        });
        popup.add(popm_substitute);
        JMenuItem popm_editInstanceName = new JMenuItem("edit instance name");
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
    public InletInstance GetInletInstance(String n) {
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
    public OutletInstance GetOutletInstance(String n) {
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
    public String GenerateClass(String ClassName, String OnParentAccess, Boolean enableOnParent) {
        return "\n#error \"Unresolved (zombie) object: " + getInstanceName() + " in patch: " + getPatch().getFileNamePath() + "\"\n";
    }

    @Override
    public ArrayList<InletInstance> GetInletInstances() {
        return inletInstances;
    }

    @Override
    public ArrayList<OutletInstance> GetOutletInstances() {
        return outletInstances;
    }
}
