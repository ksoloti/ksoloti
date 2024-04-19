/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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

import axoloti.Patch;
import axoloti.Theme;
import axoloti.objecteditor.AxoObjectEditor;
import components.ButtonComponent;
import components.ButtonComponent.ActListener;
// import java.awt.Component;
import java.awt.Point;
import java.io.File;

import javax.swing.SwingUtilities;
import org.simpleframework.xml.Element;

/**
 *
 * @author Johannes Taelman
 */
public class AxoObjectInstancePatcherObject extends AxoObjectInstance {

    AxoObjectEditor aoe;
    @Element(name = "object")
    AxoObjectPatcherObject ao;
    ButtonComponent BtnEdit;

    public AxoObjectInstancePatcherObject() {
    }

    public AxoObjectInstancePatcherObject(AxoObject type, Patch patch1, String InstanceName1, Point location) {
        super(type, patch1, InstanceName1, location);
    }

    @Override
    public void updateObj1() {
        if (ao == null) {
            ao = new AxoObjectPatcherObject();
            ao.id = "patch/object";
            ao.sDescription = "";
        }
        setType(ao);
        /*
         if (pg != null) {
         AxoObject ao = pg.GenerateAxoObj();
         setType(ao);
         pg.container(patch);
         }
         */
    }

    @Override
    public void updateObj() {
        if (ao != null) {
            ao.id = "patch/object";
            setType(ao);
            PostConstructor();
        }
        validate();
    }

    @Override
    public void OpenEditor() {
        edit();
    }

    public void edit() {
        if (ao == null) {
            ao = new AxoObjectPatcherObject();
            // ao.id = "id";
            ao.sDescription = "";
        }

        if (aoe == null) {
            aoe = new AxoObjectEditor(ao);

            String t; /* Build window frame title from object instance name and patch path*/
            if (getInstanceName() != null) {
                t = getInstanceName();
            }
            else {
                t = "patch/object";
            }
            /* Get last occurrence of slash or backslash (separating path and filename) */
            if (getPatch().getFileNamePath() != null) {
                String fpath = getPatch().getFileNamePath();
                int brk = fpath.lastIndexOf(File.separator) + 1;
                if (brk != 0) {
                    t += "   [" + fpath.substring(brk) + "]";
                }
            }

            aoe.setTitle(t);

        }
        else {
            aoe.updateReferenceXML();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                aoe.setState(java.awt.Frame.NORMAL);
                aoe.setVisible(true);
            }
        });
    }

    public boolean isEditorOpen() {
        return aoe != null && aoe.isVisible();
    }

    @Override
    public void PostConstructor() {
        super.PostConstructor();
        Titlebar.setBackground(Theme.getCurrentTheme().Object_TitleBar_Embedded_Background);
        //updateObj();
        BtnEdit = new ButtonComponent("Edit");
        BtnEdit.setForeground(Theme.getCurrentTheme().Component_Foreground);
        BtnEdit.setBackground(Theme.getCurrentTheme().Component_Background);
        BtnEdit.setAlignmentX(LEFT_ALIGNMENT);
        BtnEdit.setAlignmentY(TOP_ALIGNMENT);
        BtnEdit.addActListener(new ActListener() {
            @Override
            public void OnPushed() {
                edit();
            }
        });
        add(BtnEdit);
        resizeToGrid();
    }

    @Override
    public void Close() {
        super.Close();
        if (aoe != null) {
            aoe.Close();
        }
    }
}
