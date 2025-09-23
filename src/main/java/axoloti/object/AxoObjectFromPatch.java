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
package axoloti.object;

import axoloti.MainFrame;
import axoloti.Patch;
import axoloti.PatchFrame;
import axoloti.PatchGUI;

import java.awt.Rectangle;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.stream.Format;

/**
 *
 * @author Johannes Taelman
 */
public class AxoObjectFromPatch extends AxoObject {

    private static final Logger LOGGER = Logger.getLogger(AxoObjectFromPatch.class.getName());

    Patch p;
    PatchGUI pg;
    PatchFrame pf;
    File f;

    public AxoObjectFromPatch(File f) {
        this.f = f;
        Serializer serializer = new Persister(new Format(2));
        try {
            p = serializer.read(Patch.class, f);
            p.setFileNamePath(f.getAbsolutePath());
            p.PostContructor();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while parsing patch: " + f.getAbsolutePath() + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        shortId = f.getName().substring(0, f.getName().lastIndexOf("."));
        sObjFilePath = f.getAbsolutePath();
        UpdateObject(p); 
        MainFrame.axoObjects.ObjectList.add(this);
    }

    final public void UpdateObject(Patch patchToCopyFrom) {
        AxoObject o = patchToCopyFrom.GenerateAxoObj(new AxoObject());
        attributes = o.attributes;
        depends = o.depends;
        displays = o.displays;
        // id = f.getName().substring(0, f.getName().length() - 4); // problematic
        includes = o.includes;
        inlets = o.inlets;
        outlets = o.outlets;
        params = o.params;
        sAuthor = o.sAuthor;
        sDescription = o.sDescription;
        sDisposeCode = o.sDisposeCode;
        sInitCode = o.sInitCode;
        sKRateCode = o.sKRateCode;
        sLicense = o.sLicense;
        sLocalData = o.sLocalData;
        sMidiCode = o.sMidiCode;
        sSRateCode = o.sSRateCode;
        helpPatch = o.helpPatch;

        FireObjectModified(this);
    }

    @Override
    public void OpenEditor(Rectangle editorBounds, Integer editorActiveTabIndex, String patchFileName) {
        if (pg == null) {
            Strategy strategy = new AnnotationStrategy();
            Serializer serializer = new Persister(strategy, new Format(2));
            try {
                pg = serializer.read(PatchGUI.class, f);
                pg.ObjEditor = this;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error while parsing patch: " + f.getAbsolutePath() + ", " + ex.getMessage());
                ex.printStackTrace(System.out);
            }
        }

        if (pf == null) {
            pf = new PatchFrame(pg);
            pg.PostContructor();
        }

        // String title = "";
        // String FileNamePath = f.getAbsolutePath();
        // int brk = FileNamePath.lastIndexOf(File.separator) + 1;
        // if (brk != 0) {
        //     title = FileNamePath.substring(brk) + "  [" + FileNamePath.substring(0, brk-1) + "]";
        // } else {
        //     title = FileNamePath;
        // }
        // pg.setFileNamePath(title);
        pg.setFileNamePath(f.getAbsolutePath());
        
        pf.setState(java.awt.Frame.NORMAL);
        pf.setVisible(true);
        pf.repositionIfOutsideScreen();
    }
}
