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

import axoloti.Modulator;
import axoloti.Patch;
import axoloti.inlets.Inlet;
import axoloti.listener.ObjectModifiedListener;
import axoloti.outlets.Outlet;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "objdef")
public abstract class AxoObjectAbstract implements Comparable, Cloneable {

    @Attribute
    public String id = "";

    @Attribute(required = false)
    String uuid;

    @Deprecated
    @Attribute(required = false)
    String sha;

    @Deprecated
    @ElementListUnion({
        @ElementList(entry = "upgradeSha", type = String.class, inline = true, required = false),})
    HashSet<String> upgradeSha;

    @Element(name = "sDescription", required = false)
    public String sDescription;

    public String shortId;

    public boolean createdFromRelativePath = false;

    @Element(name = "author", required = false)
    public String sAuthor;
    @Element(name = "license", required = false)
    public String sLicense;
    public String sObjFilePath;

    @Commit
    public void Commit() {
        // called after deserialializtion
        this.sha = null;
        this.upgradeSha = null;
    }

    @Persist
    public void Persist() {
        // called prior to serialization
        this.sha = null;
        this.upgradeSha = null;
    }

    public AxoObjectAbstract() {
        this.sha = null;
        this.upgradeSha = null;
    }

    public AxoObjectAbstract(String id, String sDescription) {
        this.sDescription = sDescription;
        this.id = id;
        this.sha = null;
        this.upgradeSha = null;
    }

    Inlet GetInlet(String n) {
        return null;
    }

    Outlet GetOutlet(String n) {
        return null;
    }

    public ArrayList<Inlet> GetInlets() {
        return null;
    }

    public ArrayList<Outlet> GetOutlets() {
        return null;
    }

    public AxoObjectInstanceAbstract CreateInstance(Patch patch, String InstanceName1, Point location) {
        return null;
    }

    public void DeleteInstance(AxoObjectInstanceAbstract o) {
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int compareTo(Object t) {
        String tn = ((AxoObject) t).id;
        if (id.startsWith(tn)) {
            return 1;
        }
        if (tn.startsWith(id)) {
            return -1;
        }
        return id.compareTo(tn);
    }

    public boolean providesModulationSource() {
        return false;
    }

    public String getCName() {
        return "noname";
    }

    public String getUUID() {
        if (uuid == null) {
            uuid = GenerateUUID();
        }
        return uuid;
    }

    public HashSet<String> GetIncludes(String patchFilePath) {
        /* Optional String argument makes is possible to
         * search includes relative to containing patch file.
         * Can be passed null.
         */
        return null;
    }

    public HashSet<String> GetIncludes(String patchFilePath, HashSet<String> includes) {
        /* Optional String argument makes is possible to
         * search includes relative to containing patch file.
         * Can be passed null.
         * Optional HashSet<String> argument makes is possible
         * to pass a custom includes list to be resolved.
         */
        return null;
    }

    public void SetIncludes(HashSet<String> includes) {
    }

    public Set<String> GetDepends() {
        return null;
    }

    public String getDefaultInstanceName() {
        if (shortId == null) {
            return "obj";
        }
        int i = shortId.indexOf(' ');
        if (i > 0) {
            return shortId.substring(0, i);
        }
        return shortId;
    }

    public Modulator[] getModulators() {
        return null;
    }

    public abstract String GenerateUUID();

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public void FireObjectModified(Object src) {
    }

    public void addObjectModifiedListener(ObjectModifiedListener oml) {
    }

    public void removeObjectModifiedListener(ObjectModifiedListener oml) {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        /* Check if the object is an instance of AxoObjectAbstract, allowing comparison between subclasses */
        if (!(obj instanceof AxoObjectAbstract)) {
            return false;
        }
        AxoObjectAbstract other = (AxoObjectAbstract) obj;

        /* Check to see if the UUIDs are valid before comparing them */
        boolean hasValidUUID = (uuid != null && !this.uuid.equals("unloaded"));
        boolean otherHasValidUUID = (other.uuid != null && !other.uuid.equals("unloaded"));
        
        /* Compare by UUID if both objects have a valid one */
        if (hasValidUUID && otherHasValidUUID) {
            return uuid.equals(other.uuid);
        }
        
        /* Fallback to sObjFilePath if UUID is not available */
        return Objects.equals(sObjFilePath, other.sObjFilePath);
    }

    @Override
    public int hashCode() {
        /* Hash by UUID if available, otherwise fall back to file path */
        if (uuid != null) {
            return uuid.hashCode();
        }
        return Objects.hash(sObjFilePath);
    }

}
