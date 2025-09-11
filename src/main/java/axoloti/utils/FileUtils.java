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
package axoloti.utils;

import static axoloti.MainFrame.fc;

import axoloti.DocumentWindow;
import axoloti.DocumentWindowList;
import axoloti.PatchGUI;
import axoloti.dialogs.PatchBank;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author jtaelman
 */
public class FileUtils {

    public final static FileFilter axpFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file.getName().endsWith(".axp")) {
                return true;
            } else if (file.isDirectory()) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Axoloti Patch (.axp)";
        }
    };

    public final static FileFilter axhFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file.getName().endsWith(".axh")) {
                return true;
            } else if (file.isDirectory()) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Axoloti Help Patch (.axh)";
        }
    };

    public final static FileFilter axsFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file.getName().endsWith(".axs")) {
                return true;
            } else if (file.isDirectory()) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Axoloti Subpatch (.axs)";
        }
    };

    public final static FileFilter axbFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file.getName().endsWith(".axb")) {
                return true;
            } else if (file.isDirectory()) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Axoloti Patchbank (.axb)";
        }
    };

    public final static FileFilter axoFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file.getName().endsWith(".axo")) {
                return true;
            } else if (file.isDirectory()) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Axoloti Object (.axo)";
        }
    };
    
    public final static FileFilter axtFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file.getName().endsWith(".axt")) {
                return true;
            } else if (file.isDirectory()) {
                return false;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Axoloti Patcher Theme (.axt)";
        }
    };

    public static void Open(JFrame anc) {
        // JFrame frame = new JFrame();
        // frame.setSize(0,0);
        // frame.setLocationRelativeTo(anc);
        // frame.setUndecorated(true);
        // frame.setVisible(true);

        fc.resetChoosableFileFilters();
        fc.setMultiSelectionEnabled(true);
        fc.setCurrentDirectory(new File(Preferences.getInstance().getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setDialogTitle("Open...");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Axoloti Files", "axp", "axh", "axs", "axb"));
        fc.addChoosableFileFilter(axpFileFilter);
        fc.addChoosableFileFilter(axhFileFilter);
        fc.addChoosableFileFilter(axsFileFilter);
        fc.addChoosableFileFilter(axbFileFilter);

        int returnVal = fc.showOpenDialog(anc);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] fs = fc.getSelectedFiles();
            if (fs[0] != null) {
                Preferences.getInstance().setCurrentFileDirectory(fs[0].getParentFile().getPath());
            }

            for (File f : fs) {
                for (DocumentWindow dw : DocumentWindowList.GetList()) {
                    if (f.equals(dw.getFile())) {
                        JFrame frame1 = dw.GetFrame();
                        frame1.setVisible(true);
                        frame1.setState(java.awt.Frame.NORMAL);
                        frame1.toFront();
                        return;
                    }
                }
                if (axpFileFilter.accept(f)
                        || axsFileFilter.accept(f)
                        || axhFileFilter.accept(f)) {
                    PatchGUI.OpenPatch(f);
                } else if (axbFileFilter.accept(f)) {
                    PatchBank.OpenBank(f);
                }
            }
        }
        fc.setMultiSelectionEnabled(false);
        fc.updateCurrentSize();
    }

    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static String toUnixPath(String path) {
        if (path == null) {
            return null;
        }
        return path.replace(File.separatorChar, '/');
    }
}
