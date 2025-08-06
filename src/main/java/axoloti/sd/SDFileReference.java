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
package axoloti.sd;

import java.io.File;
import java.nio.file.Path;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Persist;

/**
 *
 * @author jtaelman
 */
public class SDFileReference {

    @Attribute
    public String localFilename;

    @Attribute
    public String targetPath;

    public File localfile;

    public SDFileReference() {
    }

public SDFileReference(File localfile, String targetDirOnSDCard) {
        this.localfile = localfile;
        this.localFilename = (localfile != null) ? localfile.getName() : "";
        this.targetPath = targetDirOnSDCard;
    }

    @Override
    public SDFileReference clone() {
        try {
            return (SDFileReference) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Persist
    public void Persist() {
        /* nop */
    }

    public void Resolve(Path basePath) {
        if (this.localfile != null || this.localFilename == null) {
            return;
        }

        if (basePath != null) {
            this.localfile = basePath.resolve(this.localFilename).toFile();
        }
    }

    public File getResolvedFile(Path basePath) {
        Resolve(basePath);
        return localfile;
    }
}
