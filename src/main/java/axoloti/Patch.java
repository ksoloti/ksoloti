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
package axoloti;

import axoloti.attributedefinition.AxoAttributeComboBox;
import axoloti.displays.DisplayInstance;
import axoloti.inlets.InletBool32;
import axoloti.inlets.InletCharPtr32;
import axoloti.inlets.InletFrac32;
import axoloti.inlets.InletFrac32Buffer;
import axoloti.inlets.InletInstance;
import axoloti.inlets.InletInt32;
import axoloti.iolet.IoletAbstract;
import static axoloti.MainFrame.prefs;
import axoloti.object.AxoObject;
import axoloti.object.AxoObjectAbstract;
import axoloti.object.AxoObjectFile;
import axoloti.object.AxoObjectInstance;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.object.AxoObjectInstanceComment;
import axoloti.object.AxoObjectInstanceHyperlink;
import axoloti.object.AxoObjectInstancePatcher;
import axoloti.object.AxoObjectInstancePatcherObject;
import axoloti.object.AxoObjectInstanceZombie;
import axoloti.object.AxoObjectPatcher;
import axoloti.object.AxoObjectPatcherObject;
import axoloti.object.AxoObjectZombie;
import axoloti.outlets.OutletBool32;
import axoloti.outlets.OutletCharPtr32;
import axoloti.outlets.OutletFrac32;
import axoloti.outlets.OutletFrac32Buffer;
import axoloti.outlets.OutletInstance;
import axoloti.outlets.OutletInt32;
import axoloti.parameters.ParameterInstance;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.simpleframework.xml.*;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Complete;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.core.Validate;
import org.simpleframework.xml.strategy.Strategy;
import qcmds.QCmdChangeWorkingDirectory;
import qcmds.QCmdCompilePatch;
import qcmds.QCmdCreateDirectory;
import qcmds.QCmdLock;
import qcmds.QCmdProcessor;
import qcmds.QCmdRecallPreset;
import qcmds.QCmdStart;
import qcmds.QCmdStop;
import qcmds.QCmdUploadFile;
import qcmds.QCmdUploadPatch;

/**
 *
 * @author Johannes Taelman
 */
@Root
public class Patch {

    private static final Logger LOGGER = Logger.getLogger(Patch.class.getName());

    @Attribute(required = false)
    String appVersion;

    public @ElementListUnion({
        @ElementList(entry = "obj", type = AxoObjectInstance.class, inline = true, required = false),
        @ElementList(entry = "patcher", type = AxoObjectInstancePatcher.class, inline = true, required = false),
        @ElementList(entry = "patchobj", type = AxoObjectInstancePatcherObject.class, inline = true, required = false),
        @ElementList(entry = "comment", type = AxoObjectInstanceComment.class, inline = true, required = false),
        @ElementList(entry = "hyperlink", type = AxoObjectInstanceHyperlink.class, inline = true, required = false),
        @ElementList(entry = "zombie", type = AxoObjectInstanceZombie.class, inline = true, required = false)
    }) ArrayList<AxoObjectInstanceAbstract> objectInstances = new ArrayList<AxoObjectInstanceAbstract>();

    @ElementList(name = "nets")
    public ArrayList<Net> nets = new ArrayList<Net>();

    @Element(required = false)
    PatchSettings settings;
    @Element(required = false, data = true)
    String notes = "";
    @Element(required = false)
    Rectangle windowPos;

    private String FileNamePath;
    PatchFrame patchframe;

    ArrayList<ParameterInstance> ParameterInstances = new ArrayList<ParameterInstance>();
    ArrayList<DisplayInstance> DisplayInstances = new ArrayList<DisplayInstance>();
    public ArrayList<Modulator> Modulators = new ArrayList<Modulator>();

    public int presetNo = 0;
    boolean locked = false;
    private boolean dirty = false;

    private final String I = "\t"; /* Convenient for (I)ndentation of auto-generated code */

    /*
     *  0 = A_STEREO
     *  1 = A_MONO
     *  2 = A_BALANCED
     */
    private int audioInputMode = 0;
    private int audioOutputMode = 0;

    @Element(required = false)
    private String helpPatch;

    /* Specifies the patch that this patch is contained in (as a subpatch) */
    private Patch container = null;
    private AxoObjectInstanceAbstract controllerInstance;

    public boolean presetUpdatePending = false;

    private List<String> previousStates = new ArrayList<String>();
    private int currentState = 0;

    static public class PatchVersionException extends RuntimeException {
        PatchVersionException(String msg) {
            super(msg);
        }
    }

    private static final int AVX = getVersionX(Version.AXOLOTI_SHORT_VERSION),
                             AVY = getVersionY(Version.AXOLOTI_SHORT_VERSION),
                             AVZ = getVersionZ(Version.AXOLOTI_SHORT_VERSION);

    private static int getVersionX(String vS) {
        if (vS != null) {
            int i = vS.indexOf('.');
            if (i > 0) {
                String v = vS.substring(0, i);
                try {
                    return Integer.valueOf(v);
                }
                catch (NumberFormatException e) {
                }
            }
        }
        return -1;
    }

    private static int getVersionY(String vS) {
        if (vS != null) {
            int i = vS.indexOf('.');
            if (i > 0) {
                int j = vS.indexOf('.', i + 1);
                if (j > 0) {
                    String v = vS.substring(i + 1, j);
                    try {
                        return Integer.valueOf(v);
                    }
                    catch (NumberFormatException e) {
                    }
                }
            }
        }
        return -1;
    }

    private static int getVersionZ(String vS) {
        if (vS != null) {
            int i = vS.indexOf('.');
            if (i > 0) {
                int j = vS.indexOf('.', i + 1);
                if (j > 0) {
                    String v = vS.substring(j + 1);
                    try {
                        return Integer.valueOf(v);
                    }
                    catch (NumberFormatException e) {
                    }
                }
            }
        }
        return -1;
    }

    @Validate
    public void Validate() {
        /* Called after deserialializtion, stops validation */
        if (appVersion != null && !appVersion.equals(Version.AXOLOTI_SHORT_VERSION)) {
            int vX = getVersionX(appVersion);
            int vY = getVersionY(appVersion);
            int vZ = getVersionZ(appVersion);

            if (AVX > vX) {
                return;
            }
            if (AVX == vX) {
                if (AVY > vY) {
                    return;
                }
                if (AVY == vY) {
                    if (AVZ > vZ) {
                        return;
                    }
                    if (AVZ == vZ) {
                        return;
                    }
                }
            }

            throw new PatchVersionException(appVersion);
        }
    }

    @Complete
    public void Complete() {
        /* called after deserialializtion */
    }

    @Persist
    public void Persist() {
        /* called prior to serialization */
        appVersion = Version.AXOLOTI_SHORT_VERSION;
    }

    MainFrame GetMainFrame() {
        return MainFrame.mainframe;
    }

    QCmdProcessor GetQCmdProcessor() {
        if (patchframe == null) {
            return null;
        }
        return patchframe.qcmdprocessor;
    }

    public PatchSettings getSettings() {
        return settings;
    }

    void UploadDependentFiles(String sdpath) {
        ArrayList<SDFileReference> files = GetDependendSDFiles();
        for (SDFileReference fref : files) {
            File f = fref.localfile;
            if (f == null) {
                LOGGER.log(Level.SEVERE, "Couldn''t resolve file: {0}", fref.targetPath);
                continue;
            }
            if (!f.exists()) {
                LOGGER.log(Level.SEVERE, "File does not exist: {0}", f.getName());
                continue;
            }
            if (!f.canRead()) {
                LOGGER.log(Level.SEVERE, "Cannot read file {0}", f.getName());
                continue;
            }

            String targetfn = fref.targetPath;
            if (targetfn.isEmpty()) {
                LOGGER.log(Level.SEVERE, "Target filename is empty: {0}", f.getName());
                continue;
            }
            if (targetfn.charAt(0) != '/') {
                targetfn = sdpath + "/" + fref.targetPath;
            }

            if (!SDCardInfo.getInstance().exists(targetfn, f.lastModified(), f.length())) {
                GetQCmdProcessor().AppendToQueue(new qcmds.QCmdGetFileInfo(targetfn));
                GetQCmdProcessor().WaitQueueFinished();
                GetQCmdProcessor().AppendToQueue(new qcmds.QCmdPing());
                GetQCmdProcessor().WaitQueueFinished();

                if (!SDCardInfo.getInstance().exists(targetfn, f.lastModified(), f.length())) {
                    if (f.length() > 8 * 1024 * 1024) {
                        LOGGER.log(Level.INFO, "File {0} is larger than 8MB, skipping upload.", f.getName());
                        continue;
                    }

                    for (int i = 1; i < targetfn.length(); i++) {
                        if (targetfn.charAt(i) == '/') {
                            GetQCmdProcessor().AppendToQueue(new qcmds.QCmdCreateDirectory(targetfn.substring(0, i)));
                            GetQCmdProcessor().WaitQueueFinished();
                        }
                    }
                    GetQCmdProcessor().AppendToQueue(new QCmdUploadFile(f, targetfn));
                    GetQCmdProcessor().WaitQueueFinished();
                }
                else {
                    LOGGER.log(Level.INFO, "File {0} matches timestamp and size, skipping upload.", f.getName());
                }
            }
            else {
                LOGGER.log(Level.INFO, "File {0} matches timestamp and size, skipping upload.", f.getName());
            }
        }
    }

    public ArrayList<AxoObjectInstanceAbstract> GetObjectInstancesWithoutComments() {
        ArrayList<AxoObjectInstanceAbstract> objectInstancesWithoutComments = new ArrayList<AxoObjectInstanceAbstract>();
        for (AxoObjectInstanceAbstract oa : objectInstances) {
            if (!(oa instanceof AxoObjectInstanceComment || oa instanceof AxoObjectInstanceHyperlink)) {
                objectInstancesWithoutComments.add(oa);
            }
        }
        return objectInstancesWithoutComments;
    }

    void GoLive() {
        GetQCmdProcessor().AppendToQueue(new QCmdStop());
        if (USBBulkConnection.GetConnection().GetSDCardPresent()) {
            String f = "/" + getSDCardPath();
            // System.out.println("pathf" + f);
            if (SDCardInfo.getInstance().find(f) == null) {
                GetQCmdProcessor().AppendToQueue(new QCmdCreateDirectory(f));
            }
            GetQCmdProcessor().AppendToQueue(new QCmdChangeWorkingDirectory(f));
            UploadDependentFiles("/" + getSDCardPath());
        }
        else {
            /* issue warning when there are dependent files */
            ArrayList<SDFileReference> files = GetDependendSDFiles();
            if (files.size() > 0) {
                LOGGER.log(Level.SEVERE, "Patch requires file {0} on SD card, but no SD card connected.", files.get(0).targetPath);
            }
        }
        ShowPreset(0);
        presetUpdatePending = false;
        for (AxoObjectInstanceAbstract o : objectInstances) {
            for (ParameterInstance pi : o.getParameterInstances()) {
                if (!pi.isFrozen()) {
                    pi.ClearNeedsTransmit();
                }
            }
        }
        WriteCode();
        GetQCmdProcessor().SetPatch(null);
        GetQCmdProcessor().AppendToQueue(new QCmdCompilePatch(this));
        GetQCmdProcessor().AppendToQueue(new QCmdUploadPatch());
        GetQCmdProcessor().AppendToQueue(new QCmdStart(this));
        GetQCmdProcessor().AppendToQueue(new QCmdLock(this));
    }

    public void ShowCompileFail() {
        Unlock();
    }

    void ShowDisconnect() {
        if (patchframe != null) {
            patchframe.ShowDisconnect();
        }
    }

    void ShowConnect() {
        if (patchframe != null) {
            patchframe.ShowConnect();
        }
    }

    public void setFileNamePath(String FileNamePath) {
        this.FileNamePath = FileNamePath;
    }

    public String getFileNamePath() {
        return FileNamePath;
    }

    public Patch() {
        super();
    }

    public void PostContructor() {
        for (AxoObjectInstanceAbstract o : objectInstances) {
            o.patch = this;
            AxoObjectAbstract t = o.resolveType();
            if ((t != null) && (t.providesModulationSource())) {
                o.PostConstructor();

                Modulator[] m = t.getModulators();
                if (Modulators == null) {
                    Modulators = new ArrayList<Modulator>();
                }
                for (Modulator mm : m) {
                    mm.objInst = o;
                    Modulators.add(mm);
                }
            }
        }

        ArrayList<AxoObjectInstanceAbstract> obj2 = (ArrayList<AxoObjectInstanceAbstract>) objectInstances.clone();
        for (AxoObjectInstanceAbstract o : obj2) {

            AxoObjectAbstract t = o.getType();

            boolean isTypeNull = (t == null);
            boolean isHardZombie = (o instanceof AxoObjectInstanceZombie);

            if (!isTypeNull && (!t.providesModulationSource())) {
                o.patch = this;
                o.PostConstructor();
                // System.out.println("Obj added " + o.getInstanceName());
            }
            else if (isTypeNull || isHardZombie) {

                if (isHardZombie) {
                    LOGGER.log(Level.SEVERE, "The patch was previously saved while \"" + o.typeName + "\", labeled \"" + o.getInstanceName() + "\", was a zombie. This has turned it into a \"hard\" zombie. You have to replace it manually to be able to go live again.\n");
                }
                objectInstances.remove(o);
                AxoObjectInstanceZombie zombie = new AxoObjectInstanceZombie(new AxoObjectZombie(), this, o.getInstanceName(), new Point(o.getX(), o.getY()));
                zombie.patch = this;
                zombie.typeName = o.typeName;
                zombie.typeUUID = o.typeUUID;
                zombie.inletInstances = o.GetInletInstances();
                zombie.outletInstances = o.GetOutletInstances();
                zombie.attributeInstances = o.getAttributeInstances();
                zombie.parameterInstances = o.getParameterInstances();
                zombie.PostConstructor();
                objectInstances.add(zombie);
            }
        }

        ArrayList<Net> nets2 = (ArrayList<Net>) nets.clone();
        for (Net n : nets2) {
            n.patch = this;
            n.PostConstructor();
        }

        PromoteOverloading(true);
        ShowPreset(0);

        if (settings == null) {
            settings = new PatchSettings();
        }

        ClearDirty();
        saveState();
    }

    public ArrayList<ParameterInstance> getParameterInstances() {
        return ParameterInstances;
    }

    public AxoObjectInstanceAbstract GetObjectInstance(String n) {
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (n.equals(o.getInstanceName())) {
                return o;
            }
        }
        return null;
    }

    public void ClearDirty() {
        dirty = false;
        if (patchframe != null) {
            patchframe.setUnsavedAsterisk(false);
        }
    }

    public void SetDirty() {
        if (patchframe != null)
            SetDirty(true);
    }

    public void SetDirty(boolean shouldSaveState) {
        dirty = true;
        if (patchframe != null) {
            patchframe.setUnsavedAsterisk(true);
        }

        if (container != null) {
            container.SetDirty(shouldSaveState);
        }

        if (shouldSaveState) {
            currentState += 1;
            saveState();
        }

        if (patchframe != null) {
            patchframe.updateUndoRedoEnabled();
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean hasZombies() {

        boolean result = false;
        
        for (AxoObjectInstanceAbstract o : objectInstances) {

            AxoObjectAbstract t = o.getType();
            boolean isTypeNull = (t == null);
            boolean isHardZombie = (o instanceof AxoObjectInstanceZombie);

            result |= (isTypeNull || isHardZombie);
        }

        return result;
    }

    public void setAudioInputMode(int mode) {
        audioInputMode = mode;
        if (audioInputMode > 2) {
            audioInputMode = 2;
        }
        else if (audioInputMode < 0) {
            audioInputMode = 0;
        }
    }

    public void setAudioOutputMode(int mode) {
        audioOutputMode = mode;
        if (audioOutputMode > 2) {
            audioOutputMode = 2;
        }
        else if (audioOutputMode < 0) {
            audioOutputMode = 0;
        }
    }

    public Patch getContainer() {
        return container;
    }

    public void setContainer(Patch c) {
        container = c;
    }

    public AxoObjectInstanceAbstract AddObjectInstance(AxoObjectAbstract obj, Point loc) {
        if (!IsLocked()) {
            if (obj == null) {
                LOGGER.log(Level.SEVERE, "AddObjectInstance NULL");
                return null;
            }

            int i = 1;
            String n = obj.getDefaultInstanceName() + "_";
            while (GetObjectInstance(n + i) != null) {
                i++;
            }
            AxoObjectInstanceAbstract oi = obj.CreateInstance(this, n + i, loc);

            SetDirty();

            Modulator[] m = obj.getModulators();
            if (m != null) {
                if (Modulators == null) {
                    Modulators = new ArrayList<Modulator>();
                }
                for (Modulator mm : m) {
                    mm.objInst = oi;
                    Modulators.add(mm);
                }
            }

            return oi;
        }
        else {
            LOGGER.log(Level.INFO, "Cannot add instance: locked!");
        }
        return null;
    }

    public Net GetNet(IoletAbstract io) {
        for (Net net : nets) {
            for (InletInstance d : net.dest) {
                if (d == io) {
                    return net;
                }
            }

            for (OutletInstance d : net.source) {
                if (d == io) {
                    return net;
                }
            }
        }
        return null;
    }

    /*
     private boolean CompatType(DataType source, DataType d2) {
     if (d1 == d2) return true;
     if ((d1 == DataType.bool32)&&(d2 == DataType.frac32)) return true;
     if ((d1 == DataType.frac32)&&(d2 == DataType.bool32)) return true;
     return false;
     }*/

    public Net AddConnection(InletInstance il, OutletInstance ol) {
        if (!IsLocked()) {
            if (il.GetObjectInstance().patch != this) {
                LOGGER.log(Level.INFO, "Cannot connect: different patch");
                return null;
            }
            if (ol.GetObjectInstance().patch != this) {
                LOGGER.log(Level.INFO, "Cannot connect: different patch");
                return null;
            }
            Net n1, n2;
            n1 = GetNet(il);
            n2 = GetNet(ol);
            if ((n1 == null) && (n2 == null)) {
                Net n = new Net(this);
                nets.add(n);
                n.connectInlet(il);
                n.connectOutlet(ol);
                LOGGER.log(Level.FINE, "Connect: new net added");
                return n;
            }
            else if (n1 == n2) {
                LOGGER.log(Level.INFO, "Cannot connect: already connected");
                return null;
            }
            else if ((n1 != null) && (n2 == null)) {
                if (n1.source.isEmpty()) {
                    LOGGER.log(Level.FINE, "Connect: adding outlet to inlet net");
                    n1.connectOutlet(ol);
                    return n1;
                }
                else {
                    disconnect(il);
                    Net n = new Net(this);
                    nets.add(n);
                    n.connectInlet(il);
                    n.connectOutlet(ol);
                    LOGGER.log(Level.FINE, "Connect: replace inlet with new net");
                    return n;
                }
            }
            else if ((n1 == null) && (n2 != null)) {
                n2.connectInlet(il);
                LOGGER.log(Level.FINE, "Connect: add additional outlet");
                return n2;
            }
            else if ((n1 != null) && (n2 != null)) {
                /* inlet already has connect, and outlet has another */
                /* replace */
                disconnect(il);
                n2.connectInlet(il);
                LOGGER.log(Level.FINE, "Connect: replace inlet with existing net");
                return n2;
            }
        }
        else {
            LOGGER.log(Level.INFO, "Cannot add connection: locked!");
        }
        return null;
    }

    public Net AddConnection(InletInstance il, InletInstance ol) {
        if (!IsLocked()) {
            if (il == ol) {
                LOGGER.log(Level.INFO, "Cannot connect: same inlet");
                return null;
            }
            if (il.GetObjectInstance().patch != this) {
                LOGGER.log(Level.INFO, "Cannot connect: different patch");
                return null;
            }
            if (ol.GetObjectInstance().patch != this) {
                LOGGER.log(Level.INFO, "Cannot connect: different patch");
                return null;
            }

            Net n1, n2;
            n1 = GetNet(il);
            n2 = GetNet(ol);
            if ((n1 == null) && (n2 == null)) {
                Net n = new Net(this);
                nets.add(n);
                n.connectInlet(il);
                n.connectInlet(ol);
                LOGGER.log(Level.FINE, "Connect: new net added");
                return n;
            }
            else if (n1 == n2) {
                LOGGER.log(Level.INFO, "Cannot connect: already connected");
            }
            else if ((n1 != null) && (n2 == null)) {
                n1.connectInlet(ol);
                LOGGER.log(Level.FINE, "Connect: inlet added");
                return n1;
            }
            else if ((n1 == null) && (n2 != null)) {
                n2.connectInlet(il);
                LOGGER.log(Level.FINE, "Connect: inlet added");
                return n2;
            }
            else if ((n1 != null) && (n2 != null)) {
                LOGGER.log(Level.INFO, "Cannot connect: both inlets included in net");
                return null;
            }
        }
        else {
            LOGGER.log(Level.INFO, "Cannot add connection: locked!");
        }
        return null;
    }

    public Net disconnect(IoletAbstract io) {
        if (!IsLocked()) {
            Net n = GetNet(io);
            if (n != null) {
                if (io instanceof OutletInstance) {
                    n.source.remove((OutletInstance) io);
                }
                else if (io instanceof InletInstance) {
                    n.dest.remove((InletInstance) io);
                }
                if (n.source.size() + n.dest.size() <= 1) {
                    delete(n);
                }
                return n;
            }
        }
        else {
            LOGGER.log(Level.INFO, "Cannot disconnect: locked!");
        }
        return null;
    }

    public Net delete(Net n) {
        if (!IsLocked()) {
            nets.remove(n);
            return n;
        }
        else {
            LOGGER.log(Level.INFO, "Cannot disconnect: locked!");
        }
        return null;
    }

    public void delete(AxoObjectInstanceAbstract o) {
        if (o == null) {
            return;
        }

        for (InletInstance ii : o.GetInletInstances()) {
            disconnect(ii);
        }

        for (OutletInstance oi : o.GetOutletInstances()) {
            disconnect(oi);
        }

        int i; for (i = Modulators.size() - 1; i >= 0; i--) {
            Modulator m1 = Modulators.get(i);
            if (m1.objInst == o) {
                Modulators.remove(m1);
                for (Modulation mt : m1.Modulations) {
                    mt.destination.removeModulation(mt);
                }
            }
        }

        objectInstances.remove(o);
        AxoObjectAbstract t = o.getType();
        if (o != null) {
            o.Close();
            t.DeleteInstance(o);
            t.removeObjectModifiedListener(o);
        }
    }

    public void updateModulation(Modulation n) {
        /* find modulator */
        Modulator m = null;
        for (Modulator m1 : Modulators) {
            if (m1.objInst == n.source) {
                if ((m1.name == null) || (m1.name.isEmpty())) {
                    m = m1;
                    break;
                }
                else if (m1.name.equals(n.modName)) {
                    m = m1;
                    break;
                }
            }
        }

        if (m == null) {
            throw new UnsupportedOperationException("Modulator not found");
        }

        if (!m.Modulations.contains(n)) {
            m.Modulations.add(n);
            System.out.println("modulation added to Modulator " + Modulators.indexOf(m));
        }
    }

    void deleteSelectedAxoObjInstances() {
        LOGGER.log(Level.FINE, "deleteSelectedAxoObjInstances()");
        if (!IsLocked()) {
            boolean cont = true;
            while (cont) {
                cont = false;
                for (AxoObjectInstanceAbstract o : objectInstances) {
                    if (o.isSelected()) {
                        this.delete(o);
                        cont = true;
                        break;
                    }
                }
            }
            SetDirty();
        }
        else {
            LOGGER.log(Level.INFO, "Cannot delete: locked!");
        }
    }

    void PreSerialize() {
    }

    void saveState() {
        SortByPosition();
        PreSerialize();

        Serializer serializer = new Persister();
        ByteArrayOutputStream b = new ByteArrayOutputStream();

        try {
            serializer.write(this, b);
            try {
                previousStates.set(currentState, b.toString());
                if (cleanDanglingStates) {
                    try {
                        // if we've saved a new edit
                        // after some undoing,
                        // cleanup dangling states
                        previousStates.subList(currentState + 1, previousStates.size()).clear();
                    }
                    catch (IndexOutOfBoundsException e) {
                    }
                }
                this.cleanDanglingStates = true;
            }
            catch (IndexOutOfBoundsException e) {
                previousStates.add(b.toString());
            }
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void cleanUpIntermediateChangeStates(int n) {
        int length = previousStates.size();
        if (length >= n) {
            previousStates.subList(length - n, length).clear();
            this.currentState -= n - 1;
            saveState();
        }
    }

    private boolean cleanDanglingStates = true;

    void loadState() {
        Serializer serializer = new Persister();
        ByteArrayInputStream b = new ByteArrayInputStream(previousStates.get(currentState).getBytes());

        try {
            Patch p = serializer.read(Patch.class, b);
            /* prevent detached sub-windows */
            Close();
            this.objectInstances = p.objectInstances;
            this.nets = p.nets;
            this.Modulators = p.Modulators;
            this.cleanDanglingStates = false;
            this.PostContructor();
            AdjustSize();
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    boolean save(File f) {
        if (hasZombies()) {
            Object[] options = {"Save Anyway",
                "Cancel"};
            int n = JOptionPane.showOptionDialog(
                    this.getPatchframe(),
                    this.FileNamePath + " contains one or more unresolved (zombie) objects.\n\nSaving the patch now will overwrite the unresolved objects with hard zombies. You will have to replace them manually later for the patch to be usable, even if the object library is found later.\n\nThe zombie objects\' names and connections will be preserved, but you may lose some parameter and attribute values you had originally set when the objects were alive, depending on how closely the manually replaced object matches the original one.",
                    "Zombie Infestation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);
            switch (n) {
                case JOptionPane.YES_OPTION:
                    /* Save Anyway, do not display warning anymore */
                    break;
                case JOptionPane.NO_OPTION:
                    /* Cancel */
                    return false;
                default:
                    return false;
            }
        }

        SortByPosition();
        PreSerialize();

        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy);

        try {
            serializer.write(this, f);
            prefs.addRecentFile(f.getAbsolutePath());
            dirty = false;
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }

        if (patchframe != null) {
            patchframe.setUnsavedAsterisk(false);
        }

        return true;
//        if (settings == null) {
//            return;
//        }
//        if (settings.subpatchmode == SubPatchMode.no) {
//            return;
//        }

        /*
         String axoObjPath = getFileNamePath();
         int i = axoObjPath.lastIndexOf(".axp");
         axoObjPath = axoObjPath.substring(0, i) + ".axo";
         LOGGER.log(Level.INFO, "Exporting axo to " + axoObjPath);
         File f2 = new File(axoObjPath);
         ExportAxoObj(f2);
         MainFrame.axoObjects.LoadAxoObjects();
         */

    }

    int displayDataLength = 0;

    void refreshIndexes() {
        for (AxoObjectInstanceAbstract o : objectInstances) {
            o.refreshIndex();
        }
        int i = 0;
        ParameterInstances = new ArrayList<ParameterInstance>();
        for (AxoObjectInstanceAbstract o : objectInstances) {
            for (ParameterInstance p : o.getParameterInstances()) {
                if (!p.isFrozen()) {
                    p.setIndex(i);
                    i++;
                    ParameterInstances.add(p);
                }
            }
        }
        int offset = 0;
        // 0 : header
        // 1 : patchref
        // 2 : length

        DisplayInstances = new ArrayList<DisplayInstance>();
        for (AxoObjectInstanceAbstract o : objectInstances) {
            for (DisplayInstance p : o.GetDisplayInstances()) {
                p.setOffset(offset + 3);
                int l = p.getLength();
                offset += l;
                DisplayInstances.add(p);
            }
        }
        displayDataLength = offset;
    }

    Dimension GetSize() {
        int nx = 0;
        int ny = 0;
        /* negative coordinates? */
        for (AxoObjectInstanceAbstract o : objectInstances) {
            Point p = o.getLocation();
            if (p.x < nx) {
                nx = p.x;
            }
            if (p.y < ny) {
                ny = p.y;
            }
        }
        if ((nx < 0) || (ny < 0)) {
            /* move all to positive coordinates */
            for (AxoObjectInstanceAbstract o : objectInstances) {
                Point p = o.getLocation();
                o.SetLocation(p.x - nx, p.y - ny);
            }
        }

        int mx = 0;
        int my = 0;
        for (AxoObjectInstanceAbstract o : objectInstances) {
            Point p = o.getLocation();
            Dimension s = o.getSize();
            int px = p.x + s.width;
            int py = p.y + s.height;
            if (px > mx) {
                mx = px;
            }
            if (py > my) {
                my = py;
            }
        }
        return new Dimension(mx, my);
    }

    void SortByPosition() {
        Collections.sort(this.objectInstances);
        refreshIndexes();
    }
    
    void SortParentsByExecution(AxoObjectInstanceAbstract o, LinkedList<AxoObjectInstanceAbstract> result) {
        LinkedList<AxoObjectInstanceAbstract> before = new LinkedList<AxoObjectInstanceAbstract>(result);
        LinkedList<AxoObjectInstanceAbstract> parents = new LinkedList<AxoObjectInstanceAbstract>();

        /* get the parents */
        for (InletInstance il : o.GetInletInstances()) {
            Net n = GetNet(il);
            if (n != null) {
                for (OutletInstance ol: n.GetSource()) {
                    AxoObjectInstanceAbstract i = ol.GetObjectInstance();
                    if (!parents.contains(i)) {
                        parents.add(i);
                    }
                }
            }
        }

        /* sort the parents */
        Collections.sort(parents);
        /* prepend any we haven't seen before */
        for (AxoObjectInstanceAbstract c: parents) {
            if (!result.contains(c))
                result.addFirst(c);
        }

        /* prepend their parents */
        for (AxoObjectInstanceAbstract c: parents) {
            if (!before.contains(c))
                SortParentsByExecution(c, result);
        }
    }

    void SortByExecution() {
        LinkedList<AxoObjectInstanceAbstract> endpoints = new LinkedList<AxoObjectInstanceAbstract>();
        LinkedList<AxoObjectInstanceAbstract> result = new LinkedList<AxoObjectInstanceAbstract>();

        /* start with all objects without outlets (end points) */
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.GetOutletInstances().isEmpty()) {
                endpoints.add(o);
            }
            else {
                int count = 0;
                for (OutletInstance ol : o.GetOutletInstances()) {
                    if (GetNet(ol) != null)
                        count++;
                }
                if (count == 0)
                    endpoints.add(o);
            }
        }

        /* sort them by position */
        Collections.sort(endpoints);

        /* walk their inlets */
        for (AxoObjectInstanceAbstract o : endpoints) {
            SortParentsByExecution(o, result);
        }

        /* add the end points */
        result.addAll(endpoints);

        /* turn it back into a freshly sorted array */
        objectInstances = new ArrayList<AxoObjectInstanceAbstract>(result);
        refreshIndexes();
    }

    public Modulator GetModulatorOfModulation(Modulation modulation) {
        if (Modulators == null) {
            return null;
        }
        for (Modulator m : Modulators) {
            if (m.Modulations.contains(modulation)) {
                return m;
            }
        }
        return null;
    }

    public int GetModulatorIndexOfModulation(Modulation modulation) {
        if (Modulators == null) {
            return -1;
        }
        for (Modulator m : Modulators) {
            int i = m.Modulations.indexOf(modulation);
            if (i >= 0) {
                return i;
            }
        }
        return -1;
    }

    List<AxoObjectAbstract> GetUsedAxoObjects() {
        ArrayList<AxoObjectAbstract> aos = new ArrayList<AxoObjectAbstract>();
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (!aos.contains(o.getType())) {
                aos.add(o.getType());
            }
        }
        return aos;
    }

    public void AdjustSize() {
    }

    public HashSet<String> getIncludes() {
        HashSet<String> includes = new HashSet<String>();
        if (controllerInstance != null) {
            Set<String> i = controllerInstance.getType().GetIncludes(getFileNamePath());
            if (i != null) {
                includes.addAll(i);
            }
        }
        for (AxoObjectInstanceAbstract o : objectInstances) {
            Set<String> i = o.getType().GetIncludes(getFileNamePath());
            if (i != null) {
                includes.addAll(i);
            }
        }

        return includes;
    }

    public HashSet<String> getDepends() {
        HashSet<String> depends = new HashSet<String>();
        for (AxoObjectInstanceAbstract o : objectInstances) {
            Set<String> i = o.getType().GetDepends();
            if (i != null) {
                depends.addAll(i);
            }
        }
        return depends;
    }

    public String generateIncludes() {
        String inc = "";
        Set<String> includes = getIncludes();
        for (String s : includes) {
            if (s.startsWith("\"")) {
                inc += "#include " + s + "\n";
            }
            else {
                inc += "#include \"" + s + "\"\n";
            }
        }
        return inc.replace('\\', '/') + "\n";
    }

    /* the c++ code generator */
    String GeneratePexchAndDisplayCode() {
        String c = GeneratePexchAndDisplayCodeV();
        c += I + "int32_t PExModulationPrevVal[attr_poly][NMODULATIONSOURCES];\n";
        return c;
    }

    String GeneratePexchAndDisplayCodeV() {
        String c = "";
        c += I + "static const uint16_t NPEXCH = " + ParameterInstances.size() + ";\n";
        c += I + "ParameterExchange_t PExch[NPEXCH];\n";
        c += I + "int32_t displayVector[" + (displayDataLength + 3) + "];\n";

        c += I + "static const uint8_t NPRESETS = " + settings.GetNPresets() + ";\n";
        c += I + "static const uint8_t NPRESET_ENTRIES = " + settings.GetNPresetEntries() + ";\n";

        c += I + "static const uint8_t NMODULATIONSOURCES = " + settings.GetNModulationSources() + ";\n";
        c += I + "static const uint8_t NMODULATIONTARGETS = " + settings.GetNModulationTargetsPerSource() + ";\n";
        return c;
    }

    String GenerateObjectCode(String classname) {
        String c = "";
        
        if (Modulators.size() > 0) {
            int k = 0;
            c += "\n" + I + "/* Modsource defines */\n";
            for (Modulator m : Modulators) {
                c += I + "static const int32_t " + m.getCName() + " = " + k + ";\n";
                k++;
            }
        }

        if (ParameterInstances.size() > 0) {
            int k = 0;
            c += "\n" + I + "/* Parameter instance indices */\n";
            for (ParameterInstance p : ParameterInstances) {
                if (!p.isFrozen()) {
                    c += I + "static const uint16_t PARAM_INDEX_" + p.GetObjectInstance().getLegalName() + "_" + p.getLegalName() + " = " + k + ";\n";
                    k++;
                }
            }
        }

        if (controllerInstance != null) {
            c += "\n" + I + "/* Controller class */\n";
            c += controllerInstance.GenerateClass(classname);
        }

        c += "\n" + I + "/* Object classes */\n";

        for (AxoObjectInstanceAbstract o : objectInstances) {
            c += o.GenerateClass(classname);
        }

        if (controllerInstance != null) {
            c += "\n" + I + "/* Controller instance */\n";
            String s = controllerInstance.getCInstanceName();
            if (!s.isEmpty()) {
                c += I + s + " " + s + "_i;\n";
            }
        }

        c += "\n" + I + "/* Object instances */\n";
        for (AxoObjectInstanceAbstract o : objectInstances) {
            String s = o.getCInstanceName();
            if (!s.isEmpty()) {
                c += I + s + " " + s + "_i;\n";
            }
        }

        c += "\n" + I + "/* Net latches */\n";
        for (Net n : nets) {
            /* check if net has multiple sources */
            if ((n.CType() != null) && n.NeedsLatch()) {
                c += I + n.CType() + " " + n.CName() + "Latch" + ";\n";
            }
        }

        return c + "\n";
    }

    String GenerateStructCodePlusPlusSub(String classname) {
        String c = "";
        c += GeneratePexchAndDisplayCode();
        c += GenerateObjectCode(classname);
        return c;
    }

    String GenerateStructCodePlusPlus(String classname, String parentclassname) {
        String c = "";
        c += "class " + classname + " {\n\n";
        c += I + "public:\n";
        c += GenerateStructCodePlusPlusSub(parentclassname);
        return c;
    }

    String GeneratePresetCode3(String ClassName) {

        String c = I + "static const int32_t* GetPresets(void) {\n";
        c += I+I + "static const int32_t p[NPRESETS][NPRESET_ENTRIES][2] = {\n";

        for (int i = 0; i < settings.GetNPresets(); i++) {
            int[] dp = DistillPreset(i + 1);
            c += I+I+I + "{\n";
            for (int j = 0; j < settings.GetNPresetEntries(); j++) {
                c += I+I+I+I + "{" + dp[j * 2] + ", " + dp[j * 2 + 1] + "}";
                if (j != settings.GetNPresetEntries() - 1) {
                    c += ",\n";
                }
                else {
                    c += "\n";
                }
            }
            if (i != settings.GetNPresets() - 1) {
                c += I+I+I + "},\n";
            }
            else {
                c += I+I+I + "}\n";
            }
        }
        c += I+I + "};\n";
        c += I+I + "return &p[0][0][0];\n";
        c += I + "};\n\n";

        c += I + "void ApplyPreset(uint8_t index) {\n"
           + I+I + "if (!index) {\n"
           + I+I+I + "int32_t* p = GetInitParams();\n"
           + I+I+I + "uint32_t i; for (i = 0; i < NPEXCH; i++) {\n"
           + I+I+I+I + "PExParameterChange(&PExch[i], p[i], 0xFFEF);\n"
           + I+I+I + "}\n"
           + I+I + "}\n"
           + I+I + "index--;\n"
           + I+I + "if (index < NPRESETS) {\n"
           + I+I+I + "PresetParamChange_t* pa = (PresetParamChange_t*) (GetPresets());\n"
           + I+I+I + "PresetParamChange_t* p = &pa[index * NPRESET_ENTRIES];\n"
           + I+I+I + "uint32_t i; for (i = 0; i < NPRESET_ENTRIES; i++) {\n"
           + I+I+I+I + "PresetParamChange_t* pp = &p[i];\n"
           + I+I+I+I + "if ((pp->pexIndex >= 0) && (pp->pexIndex < NPEXCH)) {\n"
           + I+I+I+I+I + "PExParameterChange(&PExch[pp->pexIndex], pp->value, 0xFFEF);\n"
           + I+I+I+I + "}\n"
           + I+I+I+I + "else break;\n"
           + I+I+I + "}\n"
           + I+I + "}\n"
           + I + "}\n\n";

        return c;
    }

    String GenerateModulationCode3() {

        String s = I + "static PExModulationTarget_t* GetModulationTable(void) {\n";
        s += I+I + "static const PExModulationTarget_t PExModulationSources[NMODULATIONSOURCES][NMODULATIONTARGETS] = {\n";
        for (int i = 0; i < settings.GetNModulationSources(); i++) {
            s += I+I+I + "{";
            if (i < Modulators.size()) {
                Modulator m = Modulators.get(i);
                for (int j = 0; j < settings.GetNModulationTargetsPerSource(); j++) {
                    if (j < m.Modulations.size()) {
                        Modulation n = m.Modulations.get(j);
                        if (n.destination.isFrozen()) {
                            s += "{-1, 0}";
                        }
                        else {
                            s += "{" + n.destination.indexName() + ", " + n.value.getRaw() + "}";
                        }
                    }
                    else {
                        s += "{-1, 0}";
                    }
                    if (j != settings.GetNModulationTargetsPerSource() - 1) {
                        s += ", ";
                    }
                    else {
                        s += "\n}";
                    }
                }
            }
            else {
                for (int j = 0; j < settings.GetNModulationTargetsPerSource() - 1; j++) {
                    s += "{-1, 0}, ";
                }
                s += "{-1, 0}}";
            }
            if (i != settings.GetNModulationSources() - 1) {
                s += ",\n";
            }
        }
        s += I+I + "};\n\n";
        s += I+I + "return (PExModulationTarget_t*) &PExModulationSources[0][0];\n";
        s += I + "};\n";

        return s;
    }

    String GenerateParamInitCode3(String ClassName) {
        int s = ParameterInstances.size();
        String c = I + "static int32_t* GetInitParams(void) {\n"
                 + I+I + "static const int32_t p[NPEXCH] = {\n";
        for (int i = 0; i < s; i++) {
            c += I+I+I + ParameterInstances.get(i).GetValueRaw();
            if (i != s - 1) {
                c += ",\n";
            }
            else {
                c += "\n";
            }
        }
        c += I+I + "};\n"
                + I+I + "return (int32_t*) &p[0];\n"
                + I + "}\n\n";
        return c;
    }

    String GenerateObjInitCodePlusPlusSub(String className, String parentReference) {
        String c = "";
        if (controllerInstance != null) {
            String s = controllerInstance.getCInstanceName();
            if (!s.isEmpty()) {
                c += "\n" + I+I + s + "_i.Init(" + parentReference;
                for (DisplayInstance i : controllerInstance.GetDisplayInstances()) {
                    if (i.display.getLength() > 0) {
                        c += ", ";
                        c += i.valueName("");
                    }
                }
                c += ");\n\n";
            }
        }

        for (AxoObjectInstanceAbstract o : objectInstances) {
            String s = o.getCInstanceName();
            if (!s.isEmpty()) {
                c += I+I + o.getCInstanceName() + "_i.Init(" + parentReference;
                for (DisplayInstance i : o.GetDisplayInstances()) {
                    if (i.display.getLength() > 0) {
                        c += ", ";
                        c += i.valueName("");
                    }
                }
                c += ");\n";
            }
        }
        c += "\n" + I+I + "uint32_t k; for (k = 0; k < NPEXCH; k++) {\n"
           + I+I+I + "if (PExch[k].pfunction) {\n"
           + I+I+I+I + "(PExch[k].pfunction)(&PExch[k]);\n"
           + I+I+I + "}\n"
           + I+I+I + "else {\n"
           + I+I+I+I + "PExch[k].finalvalue = PExch[k].value;\n"
           + I+I+I + "}\n"
           + I+I + "}\n";
        return c;
    }

    String GenerateParamInitCodePlusPlusSub(String className, String parentReference) {
        String c = "";
        c += I+I + "uint32_t i, j;\n";
        c += I+I + "const int32_t* p;\n";
        c += I+I + "p = GetInitParams();\n\n";
        c += I+I + "for (j = 0; j < NPEXCH; j++) {\n";
        c += I+I+I + "PExch[j].value = p[j];\n";
        c += I+I+I + "PExch[j].modvalue = p[j];\n";
        c += I+I+I + "PExch[j].signals = 0;\n";
        c += I+I+I + "PExch[j].pfunction = 0;\n";
//        c += I+I+I + "PExch[j].finalvalue = p[j];\n"; /*TBC*/
        c += I+I + "}\n\n";
        c += I+I + "int32_t* pp = &PExModulationPrevVal[0][0];\n";
        c += I+I + "for (j = 0; j < (attr_poly * NMODULATIONSOURCES); j++) {\n";
        c += I+I+I + "*pp = 0; pp++;\n";
        c += I+I + "}\n\n";
        c += I+I + "displayVector[0] = 0x446F7841;\n"; // "AxoD"
        c += I+I + "displayVector[1] = 0;\n";
        c += I+I + "displayVector[2] = " + displayDataLength + ";\n";
        return c;
    }

    String GenerateInitCodePlusPlus(String className) {
        String c = "\n";
        c += I + "/* Patch init */\n";
        c += I + "void Init() {\n";
        c += GenerateParamInitCodePlusPlusSub("", "this");
        c += GenerateObjInitCodePlusPlusSub("", "this");
        c += I + "}\n\n";

        return c;
    }

    String GenerateDisposeCodePlusPlusSub(String className) {
        // reverse order
        String c = "";
        int l = objectInstances.size();
        for (int i = l - 1; i >= 0; i--) {
            AxoObjectInstanceAbstract o = objectInstances.get(i);
            String s = o.getCInstanceName();
            if (!s.isEmpty()) {
                c += I+I + o.getCInstanceName() + "_i.Dispose();\n";
            }
        }
        if (controllerInstance != null) {
            String s = controllerInstance.getCInstanceName();
            if (!s.isEmpty()) {
                c += I+I + controllerInstance.getCInstanceName() + "_i.Dispose();\n";
            }
        }

        return c;
    }

    String GenerateDisposeCodePlusPlus(String className) {
        String c = "\n";
        c += I + "/* Patch dispose */\n";
        c += I + "void Dispose() {\n";
        c += GenerateDisposeCodePlusPlusSub(className);
        c += I + "}\n\n";
        return c;
    }

    String GenerateDSPCodePlusPlusSub(String ClassName) {
        String c = "\n";
        c += I+I + "/* <nets> */\n";
        for (Net n : nets) {
            if (n.CType() != null) {
                c += I+I + n.CType() + " " + n.CName() + ";\n";
            }
            else {
                LOGGER.log(Level.INFO, "Net has no data type!");
            }
        }
        c += I+I + "/* </nets> */\n\n";
        c += I+I + "/* <zero> */\n";
        c += I+I + "int32_t UNCONNECTED_OUTPUT;\n";
        c += I+I + "static const int32_t UNCONNECTED_INPUT = 0;\n";
        c += I+I + "static const int32buffer ZEROBUFFER = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};\n";
        c += I+I + "int32buffer UNCONNECTED_OUTPUT_BUFFER;\n";
        c += I+I + "/* </zero> */\n\n";

        if (controllerInstance != null) {
            c += I+I + "/* <controller calls> */\n";
            c += GenerateDSPCodePlusPlusSubObj(controllerInstance, ClassName);
            c += I+I + "/* </controller calls> */\n\n";
        }

        c += I+I + "/* <object calls> */\n";
        for (AxoObjectInstanceAbstract o : objectInstances) {
            c += GenerateDSPCodePlusPlusSubObj(o, ClassName);
        }
        c += I+I + "/* </object calls> */\n\n";

        c += I+I + "/* <net latch copy> */\n";
        for (Net n : nets) {
            // check if net has multiple sources
            if (n.NeedsLatch()) {
                if (n.GetDataType() != null) {
                    c += I+I + n.GetDataType().GenerateCopyCode(n.CName() + "Latch", n.CName());
                }
                else {
                    LOGGER.log(Level.SEVERE, "Only inlets connected on net!");
                }
            }
        }
        c += I+I + "/* </net latch copy> */\n";
        return c;
    }

    String GenerateDSPCodePlusPlusSubObj(AxoObjectInstanceAbstract o, String ClassName) {
        String c = "";
        String s = o.getCInstanceName();

        if (s.isEmpty()) {
            return c;
        }
        c += I+I + o.getCInstanceName() + "_i.dsp(";
//            c += I+I + o.GenerateDoFunctionName() + "(this";
        boolean needsComma = false;

        for (InletInstance i : o.GetInletInstances()) {
            if (needsComma) {
                c += ", ";
            }
            Net n = GetNet(i);
            if ((n != null) && (n.isValidNet())) {
                if (i.GetDataType().equals(n.GetDataType())) {
                    if (n.NeedsLatch()
                            && (objectInstances.indexOf(n.source.get(0).GetObjectInstance()) >= objectInstances.indexOf(o))) {
                        c += n.CName() + "Latch";
                    }
                    else {
                        c += n.CName();
                    }
                }
                else if (n.NeedsLatch()
                        && (objectInstances.indexOf(n.source.get(0).GetObjectInstance()) >= objectInstances.indexOf(o))) {
                    c += n.GetDataType().GenerateConversionToType(i.GetDataType(), n.CName() + "Latch");
                }
                else {
                    c += n.GetDataType().GenerateConversionToType(i.GetDataType(), n.CName());
                }
            }
            else if (n == null) {
                /* unconnected input */
                c += i.GetDataType().GenerateSetDefaultValueCode();
            }
            else if (!n.isValidNet()) {
                c += i.GetDataType().GenerateSetDefaultValueCode();
                LOGGER.log(Level.SEVERE, "Patch contains invalid net! {0}", i.objname + ":" + i.getInletname());
            }
            needsComma = true;
        }
        for (OutletInstance i : o.GetOutletInstances()) {
            if (needsComma) {
                c += ", ";
            }
            Net n = GetNet(i);
            if ((n != null) && n.isValidNet()) {
                if (n.IsFirstOutlet(i)) {
                    c += n.CName();
                }
                else {
                    c += n.CName() + "+";
                }
            }
            else {
                c += i.GetDataType().UnconnectedSink();
            }
            needsComma = true;
        }
        for (ParameterInstance i : o.getParameterInstances()) {
            if (!i.isFrozen()) {
                if (i.parameter.PropagateToChild == null) {
                    if (needsComma) {
                        c += ", ";
                    }
                    c += i.variableName("");
                    needsComma = true;
                }
            }
        }
        for (DisplayInstance i : o.GetDisplayInstances()) {
            if (i.display.getLength() > 0) {
                if (needsComma) {
                    c += ", ";
                }
                c += i.valueName("");
                needsComma = true;
            }
        }
        c += ");\n";
        return c;
    }

    String GenerateMidiInCodePlusPlus() {
        String c = "";
        if (controllerInstance != null) {
            c += controllerInstance.GenerateCallMidiHandler();
        }
        for (AxoObjectInstanceAbstract o : objectInstances) {
            c += o.GenerateCallMidiHandler();
        }
        return c;
    }

    String GenerateDSPCodePlusPlus(String ClassName) {
        String c = "\n";
        c = I + "void __attribute__((optimize(\"-O2\"))) clearBuffers(void) {\n"
        + I+I + "uint32_t u;\n"
        + I+I + "for(u=0; u < BUFSIZE; u++) {\n"
        + I+I+I + "AudioOutputLeft[u] = 0;\n"
        + I+I+I + "AudioOutputRight[u] = 0;\n";
        if (prefs.getFirmwareMode().contains("USBAudio")) {
            c += I+I+I + "UsbOutputLeft[u] = 0;\n"
               + I+I+I + "UsbOutputRight[u] = 0;\n"
               + "#if USB_AUDIO_CHANNELS == 4\n"
               + I+I+I + "UsbOutput2Left[u] = 0;\n"
               + I+I+I + "UsbOutput2Right[u] = 0;\n"
               + "#endif\n";
        }
        if (prefs.getFirmwareMode().contains("I2SCodec")) {
            c += I+I+I + "i2sOutputLeft[u] = 0;\n"
               + I+I+I + "i2sOutputRight[u] = 0;\n";
        }
        c += I+I + "}\n"
        + I + "}\n\n"

        + I + "/* Patch k-rate */\n"
        + I + "void dsp(void) {\n"
        + I+I + "uint32_t i;\n"
        + I+I + "clearBuffers();\n";
 
        c += GenerateDSPCodePlusPlusSub(ClassName);
        c += I + "}\n\n";
        return c;
    }

    String GenerateMidiCodePlusPlus(String ClassName) {
        String c = "";
        c += I + "void MidiInHandler(midi_device_t dev, uint8_t port, uint8_t status, uint8_t data1, uint8_t data2) {\n";
        c += GenerateMidiInCodePlusPlus();
        c += I + "}\n\n";
        return c;
    }

    String GeneratePatchCodePlusPlus(String ClassName) {
        String c = "";
        c += "};\n\n";
        c += "static rootc root;\n\n";

        if (prefs.getFirmwareMode().contains("USBAudio")) {
            c += "void PatchProcess(int32_t* inbuf, int32_t* outbuf, int32_t* inbufUsb, int32_t* outbufUsb) {\n";
        }
        else if (prefs.getFirmwareMode().contains("I2SCodec")) {
            c += "void PatchProcess(int32_t* inbuf, int32_t* outbuf, int32_t* i2s_inbuf, int32_t* i2s_outbuf) {\n";
        }
        else {
            c += "void PatchProcess(int32_t* inbuf, int32_t* outbuf) {\n";
        }

        c += I + "uint32_t i;\n";

        /* audioInputMode and audioOutputMode are modified during
           object init code generation in AxoObjectInstance.java.
           This saves a bit of memory and instructions in the patch. */

        c += I + "for (i = 0; i < BUFSIZE; i++) {\n";
        if (audioInputMode == 1) {
            c += I+I + "/* AudioInputMode == A_MONO */\n"
               + I+I + "AudioInputLeft[i] = inbuf[(i<<1)] >> 4;\n"
               + I+I + "AudioInputRight[i] = AudioInputLeft[i];\n";
        }
        else if (audioInputMode == 2) {
            c += I+I + "/* AudioInputMode == A_BALANCED */\n"
               + I+I + "AudioInputLeft[i] = inbuf[(i<<1)] >> 4;\n"
               + I+I + "AudioInputLeft[i] = (AudioInputLeft[i] - (inbuf[(i<<1) + 1] >> 4) ) >> 1;\n"
               + I+I + "AudioInputRight[i] = AudioInputLeft[i];\n";
        }
        else {
            c += I+I + "/* AudioInputMode == A_STEREO */\n"
               + I+I + "AudioInputLeft[i] = inbuf[(i<<1)] >> 4;\n"
               + I+I + "AudioInputRight[i] = inbuf[(i<<1) + 1] >> 4;\n";
        }

        if (prefs.getFirmwareMode().contains("USBAudio")) {
            c += "\n";
            c += "#if USB_AUDIO_CHANNELS == 2\n"
            + I+I + "UsbInputLeft[i]  = inbufUsb[i*2]>>4;\n"
            + I+I + "UsbInputRight[i] = inbufUsb[i*2+1]>>4;\n"
            + "#endif\n\n";

            c += "#if USB_AUDIO_CHANNELS == 4\n"
            + I+I + "UsbInputLeft[i]   = inbufUsb[i*4]>>4;\n"
            + I+I + "UsbInputRight[i]  = inbufUsb[i*4+1]>>4;\n"
            + I+I + "UsbInput2Left[i]  = inbufUsb[i*4+2]>>4;\n"
            + I+I + "UsbInput2Right[i] = inbufUsb[i*4+3]>>4;\n"
            + "#endif\n\n";
        }
        if (prefs.getFirmwareMode().contains("I2SCodec")) {
            c += "\n"
               + I+I + "i2sInputLeft[i] = ___ROR(i2s_inbuf[(i<<1)], 16) >> 4;\n"
               + I+I + "i2sInputRight[i] = ___ROR(i2s_inbuf[(i<<1) + 1], 16) >> 4;\n";

        }
        c += I + "}\n";

        c += "\n" + I + "root.dsp();\n\n";

        if (settings.getSaturate()) {

            c += I + "for (i = 0; i < BUFSIZE; i++) {\n";
            if (audioOutputMode == 1) {
                c += I+I + "/* AudioOutputMode == A_MONO */\n"
                   + I+I + "outbuf[(i<<1)] = __SSAT(AudioOutputLeft[i], 28) << 4;\n"
                   + I+I + "outbuf[(i<<1) + 1] = 0;\n";
            }
            else if (audioOutputMode == 2) {
                c += I+I + "/* AudioOutputMode == A_BALANCED */\n"
                   + I+I + "outbuf[(i<<1)] = __SSAT(AudioOutputLeft[i], 28) << 4;\n"
                    + I+I + "outbuf[(i<<1) + 1] = ~outbuf[(i<<1)];\n";
            }
            else {
                c += I+I + "/* AudioOutputMode == A_STEREO */\n"
                   + I+I + "outbuf[(i<<1)] = __SSAT(AudioOutputLeft[i], 28) << 4;\n"
                   + I+I + "outbuf[(i<<1) + 1] = __SSAT(AudioOutputRight[i], 28) << 4;\n";
            }
            
            if (prefs.getFirmwareMode().contains("USBAudio")) {
                c += "\n";
                c += "#if USB_AUDIO_CHANNELS == 2\n"
                + I+I + "outbufUsb[i*2]   = __SSAT(UsbOutputLeft[i],28)<<4;\n"
                + I+I + "outbufUsb[i*2+1] = __SSAT(UsbOutputRight[i],28)<<4;\n"
                + "#endif\n";
    
                c += "#if USB_AUDIO_CHANNELS == 4\n"
                + I+I + "outbufUsb[i*4]   = __SSAT(UsbOutputLeft[i],28)<<4;\n"
                + I+I + "outbufUsb[i*4+1] = __SSAT(UsbOutputRight[i],28)<<4;\n"
                + I+I + "outbufUsb[i*4+2] = __SSAT(UsbOutput2Left[i],28)<<4;\n"
                + I+I + "outbufUsb[i*4+3] = __SSAT(UsbOutput2Right[i],28)<<4;\n"
                + "#endif\n";
            }
            if (prefs.getFirmwareMode().contains("I2SCodec")) {
                c += "\n"
                   + I+I + "i2s_outbuf[(i<<1)] = ___ROR(__SSAT(i2sOutputLeft[i], 28) << 4, 16);\n"
                   + I+I + "i2s_outbuf[(i<<1) + 1] = ___ROR(__SSAT(i2sOutputRight[i], 28) << 4, 16);\n";
            }
            c += I + "}\n";
        }
        else {
            c += I + "for (i = 0; i < BUFSIZE; i++) {\n";
            if (audioOutputMode == 1) {
                    c += I+I + "/* AudioOutputMode == A_MONO, unsaturated */\n"
                       + I+I + "outbuf[(i<<1)] = AudioOutputLeft[i];\n"
                       + I+I + "outbuf[(i<<1) + 1] = 0;\n";
            }
            else if (audioOutputMode == 2) {
                c += I+I + "/* AudioOutputMode == A_BALANCED, unsaturated */\n"
                   + I+I + "outbuf[(i<<1)] = AudioOutputLeft[i];\n"
                   + I+I + "outbuf[(i<<1) + 1] = ~outbuf[(i<<1)];\n";
            }
            else {
                c += I+I + "/* AudioOutputMode == A_STEREO, unsaturated */\n"
                   + I+I + "outbuf[(i<<1)] = AudioOutputLeft[i];\n"
                   + I+I + "outbuf[(i<<1) + 1] = AudioOutputRight[i];\n";
            }
                
            if (prefs.getFirmwareMode().contains("USBAudio")) {
                c += "\n";
                c += "#if USB_AUDIO_CHANNELS == 2\n"
                + I+I + "outbufUsb[i*2]   = UsbOutputLeft[i];\n"
                + I+I + "outbufUsb[i*2+1] = UsbOutputRight[i];\n"
                + "#endif\n";
    
                c += "#if USB_AUDIO_CHANNELS == 4\n"
                + I+I + "outbufUsb[i*4]   = UsbOutputLeft[i];\n"
                + I+I + "outbufUsb[i*4+1] = UsbOutputRight[i];\n"
                + I+I + "outbufUsb[i*4+2] = UsbOutput2Left[i];\n"
                + I+I + "outbufUsb[i*4+3] = UsbOutput2Right[i];\n"
                + "#endif\n";
            }
            if (prefs.getFirmwareMode().contains("I2SCodec")) {
                c += "\n"
                   + I+I + "i2s_outbuf[(i<<1)] = ___ROR(i2sOutputLeft[i], 16);\n"
                   + I+I + "i2s_outbuf[(i<<1) + 1] = ___ROR(i2sOutputRight[i], 16);\n";
            }
            c += I + "}\n";
        }
        c += "}\n\n";
        
        c += "void ApplyPreset(uint8_t i) {\n"
            + I + "root.ApplyPreset(i);\n"
            + "}\n\n";

        c += "void PatchMidiInHandler(midi_device_t dev, uint8_t port, uint8_t status, uint8_t data1, uint8_t data2) {\n"
           + I + "root.MidiInHandler(dev, port, status, data1, data2);\n"
           + "}\n\n";

        c += "typedef void (*funcp_t) (void);\n"
           + "typedef funcp_t* funcpp_t;\n\n"
           + "extern funcp_t __ctor_array_start;\n"
           + "extern funcp_t __ctor_array_end;\n"
           + "extern funcp_t __dtor_array_start;\n"
           + "extern funcp_t __dtor_array_end;\n\n";

        c += "void PatchDispose( ) {\n"
           + I + "root.Dispose();\n\n"
           + I + "{\n"
           + I+I + "funcpp_t fpp = &__dtor_array_start;\n"
           + I+I + "while (fpp < &__dtor_array_end) {\n"
           + I+I+I + "(*fpp)();\n"
           + I+I+I + "fpp++;\n"
           + I+I + "}\n"
           + I + "}\n"
           + "}\n\n";

        c += "void xpatch_init2(uint32_t fwid) {\n"
           + I + "if (fwid != 0x" + MainFrame.mainframe.LinkFirmwareID + ") {\n"
           + I+I + "// LogTextMessage(\"Patch firmware mismatch\");\n"
           + I+I + "return;\n"
           + I + "}\n\n"
           + I + "extern uint32_t _pbss_start;\n"
           + I + "extern uint32_t _pbss_end;\n"
           + I + "volatile uint32_t* p;\n"
           + I + "for (p = &_pbss_start; p < &_pbss_end; p++) {\n"
           + I+I + "*p = 0;\n"
           + I + "}\n\n"
           + I + "{\n"
           + I+I + "funcpp_t fpp = &__ctor_array_start;\n"
           + I+I + "while (fpp < &__ctor_array_end) {\n"
           + I+I+I + "(*fpp)();\n"
           + I+I+I + "fpp++;\n"
           + I+I + "}\n"
           + I + "}\n\n";

        c += I + "patchMeta.npresets = " + settings.GetNPresets() + ";\n"
           + I + "patchMeta.npreset_entries = " + settings.GetNPresetEntries() + ";\n"
           + I + "patchMeta.pPresets = (PresetParamChange_t*) root.GetPresets();\n";

        c += I + "patchMeta.pPExch = &root.PExch[0];\n"
           + I + "patchMeta.pDisplayVector = &root.displayVector[0];\n"
           + I + "patchMeta.numPEx = " + ParameterInstances.size() + ";\n"
           + I + "patchMeta.patchID = " + GetIID() + ";\n\n"
           + I + "extern char _sdram_dyn_start;\n"
           + I + "extern char _sdram_dyn_end;\n"
           + I + "sdram_init(&_sdram_dyn_start, &_sdram_dyn_end);\n\n"
           + I + "root.Init();\n\n";

        c += I + "patchMeta.fptr_applyPreset   = ApplyPreset;\n";

        c += I + "patchMeta.fptr_patch_dispose = PatchDispose;\n"
           + I + "patchMeta.fptr_MidiInHandler = PatchMidiInHandler;\n"
           + I + "patchMeta.fptr_dsp_process   = PatchProcess;\n"
           + "}\n";
        return c;
    }

    int IID = -1; // iid identifies the patch

    public int GetIID() {
        return IID;
    }

    void CreateIID() {
        java.util.Random r = new java.util.Random();
        IID = r.nextInt();
    }

    String GenerateCode3() {
        controllerInstance = null;
        String cobjstr = prefs.getControllerObject();

        if (prefs.isControllerEnabled() && cobjstr != null && !cobjstr.isEmpty()) {
            AxoObjectAbstract x = null;
            ArrayList<AxoObjectAbstract> objs = MainFrame.axoObjects.GetAxoObjectFromName(cobjstr, GetCurrentWorkingDirectory());
            if ((objs != null) && (!objs.isEmpty())) {
                x = objs.get(0);
            }
            if (x != null) {
                LOGGER.log(Level.WARNING, "Using controller object: {0}", cobjstr);
                controllerInstance = x.CreateInstance(null, "ctrl0x123", new Point(0, 0));
            }
            else {
                LOGGER.log(Level.SEVERE, "Unable to create controller object for: {0}", cobjstr);
            }
        }

        CreateIID();
        SortByPosition();

        String c = "/*\n"
        + " * Generated using Ksoloti Patcher v" + Version.AXOLOTI_VERSION + " on " + System.getProperty("os.name") + "\n"
        + " * File: " + getFileNamePath() + "\n"
        + " * Generated: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "\n"
        + " */\n\n"
        + "#pragma GCC diagnostic ignored \"-Wunused-variable\"\n"
        + "#pragma GCC diagnostic ignored \"-Wunused-parameter\"\n\n";

        c += generateIncludes();

        if (settings == null) {
            c += "#define MIDICHANNEL 0 // DEPRECATED\n\n";
        }
        else {
            c += "#define MIDICHANNEL " + (settings.GetMidiChannel() - 1) + " // DEPRECATED\n\n";
        }

        c += "int32buffer AudioInputLeft, AudioInputRight, AudioOutputLeft, AudioOutputRight;\n";
        if (prefs.getFirmwareMode().contains("USBAudio")) {
            c += "#if USB_AUDIO_CHANNELS==2\n";
            c += "  int32buffer UsbInputLeft, UsbInputRight, UsbOutputLeft, UsbOutputRight;\n";
            c += "#elif USB_AUDIO_CHANNELS==4\n";
            c += "  int32buffer UsbInputLeft, UsbInputRight, UsbOutputLeft, UsbOutputRight, UsbInput2Left, UsbInput2Right, UsbOutput2Left, UsbOutput2Right;\n";
            c += "#endif\n";
        }
        if (prefs.getFirmwareMode().contains("I2SCodec")) {
            c += "int32buffer i2sInputLeft, i2sInputRight, i2sOutputLeft, i2sOutputRight;\n";
        }

        c += "\nvoid xpatch_init2(uint32_t fwid);\n\n"
                + "extern \"C\" __attribute__((section(\".boot\"))) void xpatch_init(uint32_t fwid) {\n"
           + I + "xpatch_init2(fwid);\n"
                + "}\n\n";

        c += "void PatchMidiInHandler(midi_device_t dev, uint8_t port, uint8_t status, uint8_t data1, uint8_t data2);\n\n";

        c += "static void PropagateToSub(ParameterExchange_t* origin) {\n"
           + I + "ParameterExchange_t* pex = (ParameterExchange_t*) origin->finalvalue;\n"
           + I + "PExParameterChange(pex, origin->modvalue, 0xFFFFFFEE);\n"
                + "}\n\n";

        c += GenerateStructCodePlusPlus("rootc", "rootc")
                + I + "static const uint32_t polyIndex = 0;\n\n"
                + GenerateParamInitCode3("rootc")
                + GeneratePresetCode3("rootc")
                + GenerateModulationCode3()
                + GenerateInitCodePlusPlus("rootc")
                + GenerateDisposeCodePlusPlus("rootc")
                + GenerateDSPCodePlusPlus("rootc")
                + GenerateMidiCodePlusPlus("rootc")
                + GeneratePatchCodePlusPlus("rootc");

        c = c.replace("attr_poly", "1");

        if (settings == null) {
            c = c.replace("attr_midichannel", "0");
        }
        else {
            c = c.replace("attr_midichannel", Integer.toString(settings.GetMidiChannel() - 1));
        }

        if (settings == null || !settings.GetMidiSelector()) {
            c = c.replace("attr_mididevice", "0");
            c = c.replace("attr_midiport", "0");
        }

        return c;
    }

    public AxoObject GenerateAxoObjNormal(AxoObject template) {
        SortByPosition();
        AxoObject ao = template;
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.typeName.equals("patch/inlet f")) {
                ao.inlets.add(new InletFrac32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/inlet i")) {
                ao.inlets.add(new InletInt32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/inlet b")) {
                ao.inlets.add(new InletBool32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/inlet a")) {
                ao.inlets.add(new InletFrac32Buffer(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/inlet string")) {
                ao.inlets.add(new InletCharPtr32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet f")) {
                ao.outlets.add(new OutletFrac32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet i")) {
                ao.outlets.add(new OutletInt32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet b")) {
                ao.outlets.add(new OutletBool32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet a")) {
                ao.outlets.add(new OutletFrac32Buffer(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet string")) {
                ao.outlets.add(new OutletCharPtr32(o.getInstanceName(), o.getInstanceName()));
            }

            for (ParameterInstance p : o.getParameterInstances()) {
                if (!p.isFrozen()) {
                    if (p.isOnParent()) {
                        ao.params.add(p.getParameterForParent());
                    }
                }
            }
        }
        /* object structures */
        ao.sLocalData = GenerateStructCodePlusPlusSub("attr_parent")
                + I + "static const uint32_t polyIndex = 0;\n\n";
        ao.sLocalData += GenerateParamInitCode3("");

        ao.sLocalData += GeneratePresetCode3("");

        ao.sLocalData += GenerateModulationCode3();
        ao.sLocalData = ao.sLocalData.replaceAll("attr_poly", "1");
        ao.sInitCode = GenerateParamInitCodePlusPlusSub("attr_parent", "this");
        ao.sInitCode += GenerateObjInitCodePlusPlusSub("attr_parent", "this");
        ao.sDisposeCode = GenerateDisposeCodePlusPlusSub("attr_parent");
        ao.includes = getIncludes();
        ao.depends = getDepends();
        if ((notes != null) && (!notes.isEmpty())) {
            ao.sDescription = notes;
        }
        else {
            ao.sDescription = "no description";
        }

        ao.sKRateCode = "uint32_t i;\n";
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.typeName.equals("patch/inlet f") || o.typeName.equals("patch/inlet i") || o.typeName.equals("patch/inlet b")) {
                ao.sKRateCode += I + o.getCInstanceName() + "_i._inlet = inlet_" + o.getLegalName() + ";\n";
            }
            else if (o.typeName.equals("patch/inlet string")) {
                ao.sKRateCode += I + o.getCInstanceName() + "_i._inlet = (char*) inlet_" + o.getLegalName() + ";\n";
            }
            else if (o.typeName.equals("patch/inlet a")) {
                ao.sKRateCode += I + "for (i = 0; i < BUFSIZE; i++) " + o.getCInstanceName() + "_i._inlet[i] = inlet_" + o.getLegalName() + "[i];\n";
            }

        }

        ao.sKRateCode += GenerateDSPCodePlusPlusSub("attr_parent");
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.typeName.equals("patch/outlet f") || o.typeName.equals("patch/outlet i") || o.typeName.equals("patch/outlet b")) {
                ao.sKRateCode += I + "outlet_" + o.getLegalName() + " = " + o.getCInstanceName() + "_i._outlet;\n";
            }
            else if (o.typeName.equals("patch/outlet string")) {
                ao.sKRateCode += I + "outlet_" + o.getLegalName() + " = (char*) " + o.getCInstanceName() + "_i._outlet;\n";
            }
            else if (o.typeName.equals("patch/outlet a")) {
                ao.sKRateCode += I + "for (i = 0; i < BUFSIZE; i++) outlet_" + o.getLegalName() + "[i] = " + o.getCInstanceName() + "_i._outlet[i];\n";
            }
        }

        ao.sMidiCode = ""
                + "if (attr_mididevice > 0 && dev > 0 && dev != attr_mididevice) return;\n"
                + "if (attr_midiport > 0 && port > 0 && port != attr_midiport) return;\n"
                + GenerateMidiInCodePlusPlus();

        if ((settings != null) && (settings.GetMidiSelector())) {

            String cch[] = {"attr_midichannel", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
            String uch[] = {"inherit", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};
            ao.attributes.add(new AxoAttributeComboBox("midichannel", uch, cch));

            /* use a cut down list of those currently supported */
            String cdev[] = {"0", "1", "2", "3", "15"};
            String udev[] = {"omni", "din", "usb device", "usb host", "internal"};
            ao.attributes.add(new AxoAttributeComboBox("mididevice", udev, cdev));

            String cport[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};
            String uport[] = {"omni", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};
            ao.attributes.add(new AxoAttributeComboBox("midiport", uport, cport));

        }
        return ao;
    }

    public AxoObject GenerateAxoObj(AxoObject template) {
        AxoObject ao;
        if (settings == null) {
            ao = GenerateAxoObjNormal(template);
        }
        else {
            switch (settings.subpatchmode) {
                case no:
                case normal:
                    ao = GenerateAxoObjNormal(template);
                    break;
                case polyphonic:
                    ao = GenerateAxoObjPoly(template);
                    break;
                case polychannel:
                    ao = GenerateAxoObjPolyChannel(template);
                    break;
                case polyexpression:
                    ao = GenerateAxoObjPolyExpression(template);
                    break;
                default:
                    return null;
            }
        }

        if (settings != null) {
            ao.sAuthor = settings.getAuthor();
            ao.sLicense = settings.getLicense();
            ao.sDescription = notes;
            ao.helpPatch = helpPatch;
        }
        return ao;
    }

    void ExportAxoObj(File f1) {
        String fnNoExtension = f1.getName().substring(0, f1.getName().lastIndexOf(".axo"));
        AxoObject ao = GenerateAxoObj(new AxoObject());
        ao.sDescription = FileNamePath;
        ao.id = fnNoExtension;

        AxoObjectFile aof = new AxoObjectFile();
        aof.objs.add(ao);
        Serializer serializer = new Persister();

        try {
            serializer.write(aof, f1);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        LOGGER.log(Level.INFO, "Export obj complete");
    }

//    void ExportAxoObjPoly2(File f1) {
//        String fnNoExtension = f1.getName().substring(0, f1.getName().lastIndexOf(".axo"));
//    }

    /* Poly voices from one (or omni) midi channel */
    AxoObject GenerateAxoObjPoly(AxoObject template) {
        SortByPosition();
        AxoObject ao = template;
        ao.id = "unnamedobject";
        ao.sDescription = FileNamePath;
        ao.includes = getIncludes();
        ao.depends = getDepends();

        if ((notes != null) && (!notes.isEmpty())) {
            ao.sDescription = notes;
        }
        else {
            ao.sDescription = "no description";
        }

        String centries[] = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};
        ao.attributes.add(new AxoAttributeComboBox("poly", centries, centries));

        if ((settings != null) && (settings.GetMidiSelector())) {

            String cch[] = {"attr_midichannel", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
            String uch[] = {"inherit", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};
            ao.attributes.add(new AxoAttributeComboBox("midichannel", uch, cch));

            /* use a cut down list of those currently supported */
            String cdev[] = {"0", "1", "2", "3", "15"};
            String udev[] = {"omni", "din", "usb device", "usb host", "internal"};
            ao.attributes.add(new AxoAttributeComboBox("mididevice", udev, cdev));

            String cport[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};
            String uport[] = {"omni", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};
            ao.attributes.add(new AxoAttributeComboBox("midiport", uport, cport));

        }

        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.typeName.equals("patch/inlet f")) {
                ao.inlets.add(new InletFrac32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/inlet i")) {
                ao.inlets.add(new InletInt32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/inlet b")) {
                ao.inlets.add(new InletBool32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/inlet a")) {
                ao.inlets.add(new InletFrac32Buffer(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/inlet string")) {
                ao.inlets.add(new InletCharPtr32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet f")) {
                ao.outlets.add(new OutletFrac32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet i")) {
                ao.outlets.add(new OutletInt32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet b")) {
                ao.outlets.add(new OutletBool32(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet a")) {
                ao.outlets.add(new OutletFrac32Buffer(o.getInstanceName(), o.getInstanceName()));
            }
            else if (o.typeName.equals("patch/outlet string")) {
                LOGGER.log(Level.SEVERE, "String outlet impossible in poly subpatches!");
                // ao.outlets.add(new OutletCharPtr32(o.getInstanceName(), o.getInstanceName()));                
            }

            for (ParameterInstance p : o.getParameterInstances()) {
                if (!p.isFrozen()) {
                    if (p.isOnParent()) {
                        ao.params.add(p.getParameterForParent());
                    }
                }
            }
        }

        ao.sLocalData = GenerateParamInitCode3("");
        ao.sLocalData += GeneratePexchAndDisplayCode();
        ao.sLocalData += "\n" + I + "/* Parameter instance indices */\n";

        int k = 0;
        for (ParameterInstance p : ParameterInstances) {
            if (!p.isFrozen()) {
                ao.sLocalData += I + "static const uint16_t PARAM_INDEX_" + p.GetObjectInstance().getLegalName() + "_" + p.getLegalName() + " = " + k + ";\n";
                k++;
            }
        }

        ao.sLocalData += "\n";

        ao.sLocalData += GeneratePresetCode3("");

        ao.sLocalData += GenerateModulationCode3();
        ao.sLocalData += "class voice {\n";
        ao.sLocalData += "  public:\n";
        ao.sLocalData += "  uint32_t polyIndex;\n";
        ao.sLocalData += GeneratePexchAndDisplayCodeV();
        ao.sLocalData += GenerateObjectCode("voice");
        ao.sLocalData += "  attr_parent* common;\n";
        ao.sLocalData += "  void Init(voice* parent) {\n";
        ao.sLocalData += "    uint32_t i; for (i = 0; i < NPEXCH; i++) {\n"
                       + "      PExch[i].pfunction = 0;\n"
                       + "    }\n";
        ao.sLocalData += GenerateObjInitCodePlusPlusSub("voice", "parent");
        ao.sLocalData += "}\n\n";
        ao.sLocalData += "void dsp(void) {\n int32_t i;\n";
        ao.sLocalData += GenerateDSPCodePlusPlusSub("");
        ao.sLocalData += "}\n";
        ao.sLocalData += "void dispose(void) {\n int32_t i;\n";
        ao.sLocalData += GenerateDisposeCodePlusPlusSub("");
        ao.sLocalData += "}\n";
        ao.sLocalData += GenerateMidiCodePlusPlus("attr_parent");
        ao.sLocalData += "};\n\n";
        ao.sLocalData += "static voice* getVoices(void) {\n"
                       + "  static voice v[attr_poly];\n"
                       + "  return v;\n"
                       + "}\n";

        ao.sLocalData += "static void PropagateToVoices(ParameterExchange_t* origin) {\n"
                       + "  ParameterExchange_t *pex = (ParameterExchange_t*) origin->finalvalue;\n"
                       + "  uint32_t vi; for (vi = 0; vi < attr_poly; vi++) {\n"
                       + "    PExParameterChange(pex,origin->modvalue, 0xFFFFFFEE);\n"
                       + "    pex = (ParameterExchange_t*) ((int32_t) pex + sizeof(voice)); // dirty trick...\n"
                       + "  }"
                       + "}\n";

        ao.sLocalData += "uint8_t voiceNotePlaying[attr_poly];\n";
        ao.sLocalData += "uint32_t voicePriority[attr_poly];\n";
        ao.sLocalData += "uint32_t priority;\n";
        ao.sLocalData += "uint8_t sustain;\n";
        ao.sLocalData += "uint8_t pressed[attr_poly];\n";

        ao.sLocalData = ao.sLocalData.replaceAll("parent->PExModulationSources", "parent->common->PExModulationSources");
        ao.sLocalData = ao.sLocalData.replaceAll("parent->PExModulationPrevVal", "parent->common->PExModulationPrevVal");
        ao.sLocalData = ao.sLocalData.replaceAll("parent->GetModulationTable", "parent->common->GetModulationTable");

        ao.sInitCode = GenerateParamInitCodePlusPlusSub("", "parent");
        ao.sInitCode += "uint32_t k; for (k = 0; k < NPEXCH; k++) {\n"
                     + "  PExch[k].pfunction = PropagateToVoices;\n"
                     + "  PExch[k].finalvalue = (int32_t) (&(getVoices()[0].PExch[k]));\n"
                     + "}\n\n";

        ao.sInitCode += "uint32_t vi; for (vi = 0; vi < attr_poly; vi++) {\n"
                     + "  voice* v = &getVoices()[vi];\n"
                     + "  v->polyIndex = vi;\n"
                     + "  v->common = this;\n"
                     + "  v->Init(&getVoices()[vi]);\n"
                     + "  voiceNotePlaying[vi] = 0;\n"
                     + "  voicePriority[vi] = 0;\n"
                     + "  for (j = 0; j < v->NPEXCH; j++) {\n"
                     + "    v->PExch[j].value = 0;\n"
                     + "    v->PExch[j].modvalue = 0;\n"
                     + "  }\n"
                     + "}\n\n"
                     + "for (k = 0; k < NPEXCH; k++) {\n"
                     + "  if (PExch[k].pfunction) {\n"
                     + "    (PExch[k].pfunction)(&PExch[k]);\n"
                     + "  }\n"
                     + "  else {\n"
                     + "    PExch[k].finalvalue = PExch[k].value;\n"
                     + "  }\n"
                     + "}\n\n"
                     + "priority = 0;\n"
                     + "sustain = 0;\n";

        ao.sDisposeCode = "uint32_t vi; for (vi = 0; vi < attr_poly; vi++) {\n"
                     + "  voice* v = &getVoices()[vi];\n"
                     + "  v->dispose();\n"
                     + "}\n";

        ao.sKRateCode = "";

        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.typeName.equals("patch/outlet f") || o.typeName.equals("patch/outlet i")
                    || o.typeName.equals("patch/outlet b") || o.typeName.equals("patch/outlet string")) {
                ao.sKRateCode += "  outlet_" + o.getLegalName() + " = 0;\n";
            }
            else if (o.typeName.equals("patch/outlet a")) {
                ao.sKRateCode += "{\n"
                               + "  uint32_t j; for (j = 0; j < BUFSIZE; j++) outlet_" + o.getLegalName() + "[j] = 0;\n"
                               + "}\n";
            }
        }
        ao.sKRateCode += "uint32_t vi; for (vi = 0; vi < attr_poly; vi++) {";

        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.typeName.equals("inlet") || o.typeName.equals("inlet_i") || o.typeName.equals("inlet_b") || o.typeName.equals("inlet_")
                    || o.typeName.equals("patch/inlet f") || o.typeName.equals("patch/inlet i") || o.typeName.equals("patch/inlet b")) {
                ao.sKRateCode += "  getVoices()[vi]." + o.getCInstanceName() + "_i._inlet = inlet_" + o.getLegalName() + ";\n";
            }
            else if (o.typeName.equals("inlet_string") || o.typeName.equals("patch/inlet string")) {
                ao.sKRateCode += "  getVoices()[vi]." + o.getCInstanceName() + "_i._inlet = (char*) inlet_" + o.getLegalName() + ";\n";
            }
            else if (o.typeName.equals("inlet~") || o.typeName.equals("patch/inlet a")) {
                ao.sKRateCode += "{\n"
                               + "  uint32_t j; for (j = 0; j < BUFSIZE; j++) getVoices()[vi]." + o.getCInstanceName() + "_i._inlet[j] = inlet_" + o.getLegalName() + "[j];\n"
                               + "}\n";
            }
        }
        ao.sKRateCode += "getVoices()[vi].dsp();\n";
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.typeName.equals("outlet") || o.typeName.equals("patch/outlet f")
                    || o.typeName.equals("patch/outlet i")
                    || o.typeName.equals("patch/outlet b")) {
                ao.sKRateCode += "  outlet_" + o.getLegalName() + " += getVoices()[vi]." + o.getCInstanceName() + "_i._outlet;\n";
            }
            else if (o.typeName.equals("patch/outlet string")) {
                ao.sKRateCode += "  outlet_" + o.getLegalName() + " = (char*) getVoices()[vi]." + o.getCInstanceName() + "_i._outlet;\n";
            }
            else if (o.typeName.equals("patch/outlet a")) {
                ao.sKRateCode += "{\n"
                               + "  uint32_t j; for (j = 0; j < BUFSIZE; j++) outlet_" + o.getLegalName() + "[j] += getVoices()[vi]." + o.getCInstanceName() + "_i._outlet[j];\n"
                               + "}\n";
            }
        }

        ao.sKRateCode += "}\n";
        ao.sMidiCode = ""
                     + "if (attr_mididevice > 0 && dev > 0 && attr_mididevice != dev) return;\n"
                     + "if (attr_midiport > 0 && port > 0 && attr_midiport != port) return;\n\n"
                     + "if ((status == MIDI_NOTE_ON + attr_midichannel) && (data2)) {\n"
                     + "  int32_t min = 1<<30;\n"
                     + "  int32_t min_i = 0;\n"
                     + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                     + "    if (voicePriority[i] < min) {\n"
                     + "      min = voicePriority[i];\n"
                     + "      min_i = i;\n"
                     + "    }\n"
                     + "  }\n\n"
                     + "  voicePriority[min_i] = 100000 + priority++;\n"
                     + "  voiceNotePlaying[min_i] = data1;\n"
                     + "  pressed[min_i] = 1;\n\n"
                     + "  getVoices()[min_i].MidiInHandler(dev, port, status, data1, data2);\n"
                     + "} else if (((status == MIDI_NOTE_ON + attr_midichannel) && (!data2)) || (status == MIDI_NOTE_OFF + attr_midichannel)) {\n"
                     + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                     + "    if ((voiceNotePlaying[i] == data1) && pressed[i]) {\n"
                     + "      voicePriority[i] = priority++;\n"
                     + "      pressed[i] = 0;\n"
                     + "      if (!sustain)\n"
                     + "        getVoices()[i].MidiInHandler(dev, port, status, data1, data2);\n"
                     + "      }\n"
                     + "  }\n"
                     + "} else if (status == attr_midichannel + MIDI_CONTROL_CHANGE) {\n"
                     + "  uint32_t i; for (i = 0; i < attr_poly; i++) getVoices()[i].MidiInHandler(dev, port, status, data1, data2);\n"
                     + "  if (data1 == 64) {\n"
                     + "    if (data2>0) {\n"
                     + "      sustain = 1;\n"
                     + "    } else if (sustain == 1) {\n"
                     + "      sustain = 0;\n"
                     + "      for (i = 0; i < attr_poly; i++) {\n"
                     + "        if (pressed[i] == 0) {\n"
                     + "          getVoices()[i].MidiInHandler(dev, port, MIDI_NOTE_ON + attr_midichannel, voiceNotePlaying[i], 0);\n"
                     + "        }\n"
                     + "      }\n"
                     + "    }\n"
                     + "  }\n"
                     + "} else {"
                     + "  uint32_t i; for (i = 0; i < attr_poly; i++) getVoices()[i].MidiInHandler(dev, port, status, data1, data2);\n"
                     + "}\n";
        return ao;
    }

    /*
     * Poly (Multi) Channel supports per Channel CC/Touch.
     * All channels are independent.
     */
    AxoObject GenerateAxoObjPolyChannel(AxoObject template) {
        AxoObject o = GenerateAxoObjPoly(template);

        o.sLocalData
                += "int8_t voiceChannel[attr_poly];\n";

        o.sInitCode
                += "uint32_t vc; for (vc = 0; vc < attr_poly; vc++) {\n"
                + "   voiceChannel[vc] = 0xFF;\n"
                + "}\n";

        o.sMidiCode = ""
                + "if ( attr_mididevice > 0 && dev > 0 && dev != attr_mididevice) return;\n"
                + "if ( attr_midiport > 0 && port > 0 && port != attr_midiport) return;\n"
                + "uint8_t msg = (status & 0xF0);\n"
                + "uint8_t chnl = (status & 0x0F);\n"
                + "if ((msg == MIDI_NOTE_ON) && (data2)) {\n"
                + "  int32_t min = 1<<30;\n"
                + "  int32_t min_i = 0;\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if (voicePriority[i] < min) {\n"
                + "      min = voicePriority[i];\n"
                + "      min_i = i;\n"
                + "    }\n"
                + "  }\n"
                + "  voicePriority[min_i] = 100000 + priority++;\n"
                + "  voiceNotePlaying[min_i] = data1;\n"
                + "  pressed[min_i] = 1;\n"
                + "  voiceChannel[min_i] = chnl;\n"
                + "  getVoices()[min_i].MidiInHandler(dev, port, msg, data1, data2);\n"
                + "} else if (((msg == MIDI_NOTE_ON) && (!data2))||\n"
                + "            (msg == MIDI_NOTE_OFF)) {\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if (voiceNotePlaying[i] == data1) {\n"
                + "      voicePriority[i] = priority++;\n"
                + "      voiceChannel[i] = 0xFF;\n"
                + "      pressed[i] = 0;\n"
                + "      if (!sustain)\n"
                + "         getVoices()[i].MidiInHandler(dev, port, msg + attr_midichannel, data1, data2);\n"
                + "      }\n"
                + "  }\n"
                + "} else if (msg == MIDI_CONTROL_CHANGE) {\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if (voiceChannel[i] == chnl) {\n"
                + "      getVoices()[i].MidiInHandler(dev, port, MIDI_CONTROL_CHANGE + attr_midichannel, data1, data2);\n"
                + "    }\n"
                + "  }\n"
                + "  if (data1 == 64) {\n"
                + "    if (data2>0) {\n"
                + "      sustain = 1;\n"
                + "    } else if (sustain == 1) {\n"
                + "      sustain = 0;\n"
                + "      for (i = 0; i < attr_poly; i++) {\n"
                + "        if (pressed[i] == 0) {\n"
                + "          getVoices()[i].MidiInHandler(dev, port, MIDI_NOTE_ON + attr_midichannel, voiceNotePlaying[i], 0);\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "} else if (msg == MIDI_PITCH_BEND) {\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if (voiceChannel[i] == chnl) {\n"
                + "      getVoices()[i].MidiInHandler(dev, port, MIDI_PITCH_BEND + attr_midichannel, data1, data2);\n"
                + "    }\n"
                + "  }\n"
                + "} else {"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if (voiceChannel[i] == chnl) {\n"
                + "         getVoices()[i].MidiInHandler(dev, port, msg + attr_midichannel, data1, data2);\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
        return o;
    }

    /*
     * Poly Expression supports the Midi Polyphonic Expression (MPE) Spec.
     * Can be used with (or without) the MPE objects.
     * the Global channel of the patch is channel 1 (lower zone) or 16 (upper zone)
     * Channels from 2 upwards (or from 15 downwards) are Member channels
     * which handle the individual voice control.
     * The zone used and amount of Member channels can be changed in the subpatch settings.
     */
    AxoObject GenerateAxoObjPolyExpression(AxoObject template) {
        AxoObject o = GenerateAxoObjPoly(template);

        o.sLocalData
                += "uint8_t voiceChannel[attr_poly];\n"
                 + "uint8_t pitchbendRange;\n"
                 + "uint8_t lowChannel;\n"
                 + "uint8_t highChannel;\n"
                 + "uint8_t lastRPN_LSB;\n"
                 + "uint8_t lastRPN_MSB;\n";

        o.sInitCode
                += "uint32_t vc; for (vc = 0; vc < attr_poly; vc++) {\n"
                 + "  voiceChannel[vc] = 0xFF;\n"
                 + "}\n"
                 + "pitchbendRange = 48;\n"
                 + "lastRPN_LSB = 0xFF;\n"
                 + "lastRPN_MSB = 0xFF;\n";

        if (settings != null && settings.getMPEZone() == 1) {
            /* Upper zone selected in settings */
            o.sInitCode
                += "lowChannel = " + (15 - settings.getMPENumberOfMemberChannels()) + ";\n"
                 + "highChannel = 14;\n";
        }
        else {
            /* Else default to lower zone */
            o.sInitCode
                += "lowChannel = 1;\n"
                 + "highChannel = " + settings.getMPENumberOfMemberChannels() + ";\n";
        }

        o.sMidiCode = ""
                + "if ((attr_mididevice > 0) && (dev > 0) && (dev != attr_mididevice)) return;\n"
                + "if ((attr_midiport > 0) && (port > 0) && (port != attr_midiport)) return;\n\n"
                + "uint8_t msg = (status & 0xF0);\n"
                + "uint8_t chnl = (status & 0x0F);\n\n"
                + "if ((msg == MIDI_NOTE_ON) && (data2)) {\n"
                + "  if ((chnl == attr_midichannel) || (chnl < lowChannel) || (chnl > highChannel)) return;\n"
                + "  int32_t min = 1<<30;\n"
                + "  int32_t min_i = 0;\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if (voicePriority[i] < min) {\n"
                + "      min = voicePriority[i];\n"
                + "      min_i = i;\n"
                + "    }\n"
                + "  }\n"
                + "  voicePriority[min_i] = 100000 + priority++;\n"
                + "  voiceNotePlaying[min_i] = data1;\n"
                + "  pressed[min_i] = 1;\n"
                + "  voiceChannel[min_i] = chnl;\n\n"
                + "  getVoices()[min_i].MidiInHandler(dev, port, msg + attr_midichannel, data1, data2);\n\n"
                + "} else if (((msg == MIDI_NOTE_ON) && (!data2)) || (msg == MIDI_NOTE_OFF)) {\n"
                + "  if ((chnl == attr_midichannel) || (chnl < lowChannel) || (chnl > highChannel)) return;\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if (data1 == voiceNotePlaying[i] && chnl == voiceChannel[i]) {\n"
                + "      voicePriority[i] = priority++;\n"
                + "      voiceChannel[i] = 0xFF;\n"
                + "      pressed[i] = 0;\n"
                + "      if (!sustain) {\n"
                + "        getVoices()[i].MidiInHandler(dev, port, msg + attr_midichannel, data1, data2);\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "} else if (msg == MIDI_CONTROL_CHANGE) {\n"
                + "  if ((chnl == attr_midichannel) && (data1 == MIDI_C_POLY)) {\n" /* CC#127 MPE enable mode (seb: not really conforming to MPE spec?) */
                + "    if (data2 > 0) {\n"
                + "      if (chnl == 0) {\n" // e.g ch 1 (g), we use 2-N notes
                + "        lowChannel = 1;\n"
                + "        highChannel = data2;\n"
                + "      } else if (chnl == 15) {\n" // ch 16, we use 16(g) 15-N notes
                + "        lowChannel = 15 - data2;\n"
                + "        highChannel = 14;\n"
                + "      }\n"
                + "      uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "        getVoices()[i].MidiInHandler(dev, port, MIDI_CONTROL_CHANGE + attr_midichannel, MIDI_C_RPN_LSB, lastRPN_LSB);\n"
                + "        getVoices()[i].MidiInHandler(dev, port, MIDI_CONTROL_CHANGE + attr_midichannel, MIDI_C_RPN_MSB, lastRPN_MSB);\n"
                + "        getVoices()[i].MidiInHandler(dev, port, MIDI_CONTROL_CHANGE + attr_midichannel, MIDI_C_DATA_ENTRY, pitchbendRange);\n"
                + "      }\n"
                + "    } else {\n"
                + "      lowChannel = 0;\n" /* disable, we may in the future want to turn this in to normal poly mode */
                + "      highChannel = 0;\n"
                + "    }\n"
                + "  }\n"
                + "  if ((chnl != attr_midichannel) && (chnl < lowChannel || chnl > highChannel)) return;\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if ((chnl == voiceChannel[i]) || (chnl == attr_midichannel)) {\n"
                + "      getVoices()[i].MidiInHandler(dev, port, MIDI_CONTROL_CHANGE + attr_midichannel, data1, data2);\n"
                + "    }\n"
                + "  }\n"
                + "  if ((data1 == MIDI_C_RPN_MSB) || (data1 == MIDI_C_RPN_LSB) || (data1 == MIDI_C_DATA_ENTRY)) {\n"
                + "    switch (data1) {\n"
                + "      case MIDI_C_RPN_LSB: lastRPN_LSB = data2; break;\n"
                + "      case MIDI_C_RPN_MSB: lastRPN_MSB = data2; break;\n"
                + "      case MIDI_C_DATA_ENTRY: {\n"
                + "        if ((lastRPN_LSB == 0) && (lastRPN_MSB == 0)) {\n"
                + "          uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "            if (chnl != voiceChannel[i]) {\n" // because already sent above
                + "              pitchbendRange = data2;\n"
                + "              getVoices()[i].MidiInHandler(dev, port, MIDI_CONTROL_CHANGE + attr_midichannel, MIDI_C_RPN_LSB, lastRPN_LSB);\n"
                + "              getVoices()[i].MidiInHandler(dev, port, MIDI_CONTROL_CHANGE + attr_midichannel, MIDI_C_RPN_MSB, lastRPN_MSB);\n"
                + "              getVoices()[i].MidiInHandler(dev, port, MIDI_CONTROL_CHANGE + attr_midichannel, MIDI_C_DATA_ENTRY, pitchbendRange);\n"
                + "            }\n"
                + "          }\n"
                + "        }\n"
                + "      }\n"
                + "      break;\n"
                + "      default: break;\n"
                + "    }\n"
                + "  } else if (data1 == 64) {\n" /* CC#64 Sustain */
                + "    if (data2 > 0) {\n"
                + "      sustain = 1;\n"
                + "    } else if (sustain == 1) {\n"
                + "      sustain = 0;\n"
                + "      for (i = 0; i < attr_poly; i++) {\n"
                + "        if (pressed[i] == 0) {\n"
                + "          getVoices()[i].MidiInHandler(dev, port, MIDI_NOTE_ON + attr_midichannel, voiceNotePlaying[i], 0);\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "} else if (msg == MIDI_PITCH_BEND) {\n"
                + "  if ((chnl != attr_midichannel) && (chnl < lowChannel || chnl > highChannel)) return;\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if ((chnl == voiceChannel[i]) || (chnl == attr_midichannel)) {\n"
                + "      getVoices()[i].MidiInHandler(dev, port, MIDI_PITCH_BEND + attr_midichannel, data1, data2);\n" /* Would have to send on chnl here to be able to isolate global pitch bend */
                + "    }\n"
                + "  }\n"
                + "} else {" /* Any other MIDI messages: forward */
                + "  if ((chnl != attr_midichannel) && (chnl < lowChannel || chnl > highChannel)) return;\n"
                + "  uint32_t i; for (i = 0; i < attr_poly; i++) {\n"
                + "    if ((chnl == voiceChannel[i]) || (chnl == attr_midichannel)) {\n"
                + "      getVoices()[i].MidiInHandler(dev, port, msg + attr_midichannel, data1, data2);\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        /* Override Global MIDI channel for this subpatch. */
        String glch = "0"; /* MPE specifies that either channel 1 or 16 is the Global channel. Use lower zone by default */
        if (settings != null && settings.getMPEZone() == 1) {
            /* Set to channel 16 if upper zone is selected in settings. */
            glch = "15";
        }

        o.sLocalData = o.sLocalData.replace("attr_midichannel", glch);
        o.sInitCode = o.sInitCode.replace("attr_midichannel", glch);
        o.sDisposeCode = o.sDisposeCode.replace("attr_midichannel", glch);
        o.sKRateCode = o.sKRateCode.replace("attr_midichannel", glch);
        o.sMidiCode = o.sMidiCode.replace("attr_midichannel", glch);

        return o;
    }

    public void WriteCode() {
        LOGGER.log(Level.INFO, "\nGenerating code...");

        String c = GenerateCode3();

        try {
            String buildDir = System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + "build";
            FileOutputStream f = new FileOutputStream(buildDir + File.separator + "xpatch.cpp");
            f.write(c.getBytes());
            f.close();
        }
        catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, ex.toString());
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString());
        }
        // LOGGER.log(Level.INFO, "Done generating code.\n");
    }

    public void Compile() {
        GetQCmdProcessor().AppendToQueue(new QCmdCompilePatch(this));
    }

    public void ShowPreset(int i) {
        presetNo = i;

        Collection<AxoObjectInstanceAbstract> c = (Collection<AxoObjectInstanceAbstract>) objectInstances.clone();
        for (AxoObjectInstanceAbstract o : c) {
            for (ParameterInstance p : o.getParameterInstances()) {
                if (!p.isFrozen()) {
                    p.ShowPreset(i);
                }
            }
        }
        repaint();
    }

    void ClearCurrentPreset() {
    }

    void CopyCurrentToInit() {
    }

    void DifferenceToPreset() {
    }

    public int[] DistillPreset(int i) {
        int[] pdata;
        pdata = new int[settings.GetNPresetEntries() * 2];
        for (int j = 0; j < settings.GetNPresetEntries(); j++) {
            pdata[j * 2] = -1;
        }

        int index = 0;
        for (AxoObjectInstanceAbstract o : objectInstances) {
            for (ParameterInstance param : o.getParameterInstances()) {
                ParameterInstance p7 = (ParameterInstance) param;
                Preset p = p7.GetPreset(i);

                if (p != null) {
                    pdata[index * 2] = p7.getIndex();
                    pdata[index * 2 + 1] = p.value.getRaw();
                    index++;

                    if (index == settings.GetNPresetEntries()) {
                        LOGGER.log(Level.SEVERE, "More than {0} entries in preset, skipping...", settings.GetNPresetEntries());
                        return pdata;
                    }
                }
            }
        }

        /*
         System.out.format("preset data: %d\n",i);
         for (int j=0; j<pdata.length/2; j++) {
         System.out.format("  %d : %d\n",pdata[j*2],pdata[j*2+1] );
         }
         */

        return pdata;
    }

    public void Lock() {
        locked = true;
        for (AxoObjectInstanceAbstract o : objectInstances) {
            o.Lock();
        }
    }

    public void Unlock() {
        locked = false;
        ArrayList<AxoObjectInstanceAbstract> objInstsClone = (ArrayList<AxoObjectInstanceAbstract>) objectInstances.clone();
        for (AxoObjectInstanceAbstract o : objInstsClone) {
            o.Unlock();
        }
    }

    public boolean IsLocked() {
        return locked;
    }

    public AxoObjectInstanceAbstract ChangeObjectInstanceType1(AxoObjectInstanceAbstract obj, AxoObjectAbstract objType) {
        if ((obj instanceof AxoObjectInstancePatcher) && (objType instanceof AxoObjectPatcher)) {
            return obj;
        }
        else if ((obj instanceof AxoObjectInstancePatcherObject) && (objType instanceof AxoObjectPatcherObject)) {
            return obj;
        }
        else if (obj instanceof AxoObjectInstance) {
            String n = obj.getInstanceName();
            obj.setInstanceName(n + "__temp");
            AxoObjectInstanceAbstract obj1 = AddObjectInstance(objType, obj.getLocation());
            if ((obj1 instanceof AxoObjectInstance)) {
                AxoObjectInstance new_obj = (AxoObjectInstance) obj1;
                AxoObjectInstance old_obj = (AxoObjectInstance) obj;
                new_obj.outletInstances = old_obj.outletInstances;
                new_obj.inletInstances = old_obj.inletInstances;
                new_obj.parameterInstances = old_obj.parameterInstances;
                new_obj.attributeInstances = old_obj.attributeInstances;
            }
            obj1.setName(n);
            return obj1;
        }
        else if (obj instanceof AxoObjectInstanceZombie) {
            String n = obj.getInstanceName();
            obj.setInstanceName(n + "__temp");
            AxoObjectInstanceAbstract obj1 = AddObjectInstance(objType, obj.getLocation());
            if ((obj1 instanceof AxoObjectInstance)) {
                AxoObjectInstance new_obj = (AxoObjectInstance) obj1;
                AxoObjectInstanceZombie old_obj = (AxoObjectInstanceZombie) obj;
                new_obj.outletInstances = old_obj.outletInstances;
                new_obj.inletInstances = old_obj.inletInstances;
                new_obj.parameterInstances = old_obj.parameterInstances;
                new_obj.attributeInstances = old_obj.attributeInstances;
            }
            obj1.setName(n);
            return obj1;
        }
        return obj;
    }

    public AxoObjectInstanceAbstract ChangeObjectInstanceType(AxoObjectInstanceAbstract obj, AxoObjectAbstract objType) {
        AxoObjectInstanceAbstract obj1 = ChangeObjectInstanceType1(obj, objType);
        if (obj1 != obj) {
            obj1.PostConstructor();
            delete(obj);
            SetDirty();
        }
        return obj1;
    }

    void invalidate() {
    }

    void UpdateDSPLoad(int val200, boolean overload) {
    }

    public void repaint() {
    }

    public void RecallPreset(int i) {
        GetQCmdProcessor().AppendToQueue(new QCmdRecallPreset(i));
    }

    /**
     *
     * @param initial If true, only objects restored from object name reference
     * (not UUID) will promote to a variant with the same name.
     */
    public void PromoteOverloading(boolean initial) {
        refreshIndexes();
        Set<String> ProcessedInstances = new HashSet<String>();
        boolean p = true;
        while (p && !(ProcessedInstances.size() == objectInstances.size())) {
            p = false;
            for (AxoObjectInstanceAbstract o : objectInstances) {
                if (!ProcessedInstances.contains(o.getInstanceName())) {
                    ProcessedInstances.add(o.getInstanceName());
                    if (!initial || o.isTypeWasAmbiguous()) {
                        o.PromoteToOverloadedObj();
                    }
                    p = true;
                    break;
                }
            }
        }

        if (!(ProcessedInstances.size() == objectInstances.size())) {
            for (AxoObjectInstanceAbstract o : objectInstances) {
                if (!ProcessedInstances.contains(o.getInstanceName())) {
                    LOGGER.log(Level.SEVERE, "PromoteOverloading: fault in {0}", o.getInstanceName());
                }
            }
        }
    }

    public InletInstance getInletByReference(String objname, String inletname) {
        if (objname == null) {
            return null;
        }

        if (inletname == null) {
            return null;
        }

        AxoObjectInstanceAbstract o = GetObjectInstance(objname);
        if (o == null) {
            return null;
        }

        return o.GetInletInstance(inletname);
    }

    public OutletInstance getOutletByReference(String objname, String outletname) {
        if (objname == null) {
            return null;
        }

        if (outletname == null) {
            return null;
        }

        AxoObjectInstanceAbstract o = GetObjectInstance(objname);
        if (o == null) {
            return null;
        }

        return o.GetOutletInstance(outletname);
    }

    public String GetCurrentWorkingDirectory() {
        if (FileNamePath == null) {
            return null;
        }

        int i = FileNamePath.lastIndexOf(File.separatorChar);
        if (i < 0) {
            return null;
        }

        return FileNamePath.substring(0, i);
    }

    public Rectangle getWindowPos() {
        return windowPos;
    }

    public PatchFrame getPatchframe() {
        return patchframe;
    }

    public String getNotes() {
        return notes;
    }

    public ArrayList<SDFileReference> GetDependendSDFiles() {
        ArrayList<SDFileReference> files = new ArrayList<SDFileReference>();

        for (AxoObjectInstanceAbstract o : objectInstances) {
            ArrayList<SDFileReference> f2 = o.GetDependendSDFiles();
            if (f2 != null) {
                files.addAll(f2);
            }
        }

        return files;
    }

    public File getBinFile() {
        String buildDir = System.getProperty(axoloti.Axoloti.LIBRARIES_DIR) + File.separator + "build";

        return new File(buildDir + File.separator + "xpatch.bin");
//            LOGGER.log(Level.INFO, "bin path: {0}", f.getAbsolutePath());        
    }

    public void UploadToSDCard(String sdfilename) {
        WriteCode();
        LOGGER.log(Level.INFO, "SD card filename: {0}", sdfilename);

        QCmdProcessor qcmdprocessor = QCmdProcessor.getQCmdProcessor();
        qcmdprocessor.AppendToQueue(new qcmds.QCmdStop());
        qcmdprocessor.AppendToQueue(new qcmds.QCmdCompilePatch(this));
        // create subdirs...

        for (int i = 1; i < sdfilename.length(); i++) {
            if (sdfilename.charAt(i) == '/') {
                qcmdprocessor.AppendToQueue(new qcmds.QCmdCreateDirectory(sdfilename.substring(0, i)));
                qcmdprocessor.WaitQueueFinished();
            }
        }

        qcmdprocessor.WaitQueueFinished();
        
        Calendar cal;
        File f = new File(FileNamePath);
        if (dirty) {
            cal = Calendar.getInstance();
        }
        else {
            cal = Calendar.getInstance();
            if (FileNamePath != null && !FileNamePath.isEmpty()) {
                if (f.exists()) {
                    cal.setTimeInMillis(f.lastModified());
                }
            }
        }
        qcmdprocessor.AppendToQueue(new qcmds.QCmdUploadFile(getBinFile(), sdfilename, cal));
        qcmdprocessor.WaitQueueFinished();
        
        String dir;
        int i = sdfilename.lastIndexOf("/");
        if (i > 0) {
            dir = sdfilename.substring(0, i);
        }
        else {
            dir = "";
        }

        UploadDependentFiles(dir);
        qcmdprocessor.WaitQueueFinished();

        if (prefs.isBackupPatchesOnSDEnabled() && getBinFile().exists() && FileNamePath != null && !FileNamePath.isEmpty()) {
            if (f.exists()) {
                qcmdprocessor.AppendToQueue(new qcmds.QCmdUploadFile(f,
                    dir + "/" +
                    f.getName() + ".backup" +
                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(ZonedDateTime.now()) +
                    f.getName().substring(f.getName().lastIndexOf(".")),
                    cal));
            }
        }
        qcmdprocessor.WaitQueueFinished();

    }

    public void UploadToSDCard() {
        UploadToSDCard("/" + getSDCardPath() + "/patch.bin");
    }

    public String getSDCardPath() {
        String FileNameNoPath = getFileNamePath();
        String separator = System.getProperty("file.separator");

        int lastSeparatorIndex = FileNameNoPath.lastIndexOf(separator);
        if (lastSeparatorIndex > 0) {
            FileNameNoPath = FileNameNoPath.substring(lastSeparatorIndex + 1);
        }

        String FileNameNoExt = FileNameNoPath;
        if (FileNameNoExt.endsWith(".axp") || FileNameNoExt.endsWith(".axs") || FileNameNoExt.endsWith(".axh")) {
            FileNameNoExt = FileNameNoExt.substring(0, FileNameNoExt.length() - 4);
        }

        return FileNameNoExt;
    }

    public void Close() {
        Unlock();

        Collection<AxoObjectInstanceAbstract> c = (Collection<AxoObjectInstanceAbstract>) objectInstances.clone();
        for (AxoObjectInstanceAbstract o : c) {
            o.Close();
        }
    }

    public boolean canUndo() {
        return !this.IsLocked() && (currentState > 0);
    }

    public boolean canRedo() {
        return !this.IsLocked() && (currentState < previousStates.size() - 1);
    }

    public void undo() {
        if (canUndo()) {
            currentState -= 1;
            loadState();
            SetDirty(false);
            patchframe.updateUndoRedoEnabled();
        }
    }

    public void redo() {
        if (canRedo()) {
            currentState += 1;
            loadState();
            SetDirty(false);
            patchframe.updateUndoRedoEnabled();
        }
    }
}
