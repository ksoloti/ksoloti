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

import static axoloti.MainFrame.mainframe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jtaelman
 */
public class SDCardInfo {

    // private static final Logger LOGGER = Logger.getLogger(SDCardInfo.class.getName());

    private final ArrayList<SDFileInfo> files = new ArrayList<SDFileInfo>();
    private AxoSDFileNode rootNode;
    private Map<String, AxoSDFileNode> pathToNodeMap;
    private List<DisplayTreeNode> sortedDisplayNodes;

    private int clusters = 0;
    private int clustersize = 0;
    private int sectorsize = 0;

    private boolean busy = false;

    private static SDCardInfo instance = null;

    protected SDCardInfo() {
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy() {
        this.busy = true;
    }

    public void clearBusy() {
        this.busy = false;
    }

    public synchronized static SDCardInfo getInstance() {
        if (instance == null) {
            instance = new SDCardInfo();
        }
        return instance;
    }

    public synchronized void SetInfo(int clusters, int clustersize, int sectorsize) {
        this.clusters = clusters;
        this.clustersize = clustersize;
        this.sectorsize = sectorsize;
        files.clear();
        sortedDisplayNodes = null;
        // System.out.println(Instant.now() + " [DEBUG] SDCardInfo.SetInfo(): clusters=" + this.clusters + " clustersize=" + this.clustersize + " sectorsize=" + this.sectorsize);
    }

    private void buildTreeNodes(List<SDFileInfo> currentLevelFiles, int currentDepth,
                                List<Boolean> parentIsLastFlags, List<DisplayTreeNode> resultList,
                                Map<String, List<SDFileInfo>> childrenMap) {

        for (int i = 0; i < currentLevelFiles.size(); i++) {
            SDFileInfo file = currentLevelFiles.get(i);
            boolean isLastChild = (i == currentLevelFiles.size() - 1);

            /* Create a new list for the next level's parentIsLastFlags */
            List<Boolean> nextParentIsLastFlags = new ArrayList<>(parentIsLastFlags);
            nextParentIsLastFlags.add(isLastChild);

            /* Create the DisplayTreeNode */
            DisplayTreeNode node = new DisplayTreeNode(file, currentDepth, isLastChild, parentIsLastFlags);
            resultList.add(node); // Add to the flat list for the table model

            /* If it's a directory, recursively build its children */
            if (file.isDirectory()) {
                List<SDFileInfo> children = childrenMap.getOrDefault(file.getFilename(), Collections.emptyList());
                buildTreeNodes(children, currentDepth + 1, nextParentIsLastFlags, resultList, childrenMap);
            }
        }
    }

    public void buildFileTree() {
        pathToNodeMap = new HashMap<>();
        sortedDisplayNodes = new ArrayList<>();

        /* Create a 'dummy' root node for "/" */
        SDFileInfo rootInfo = new SDFileInfo("/", Calendar.getInstance(), 0, true);
        rootNode = new AxoSDFileNode(rootInfo);
        pathToNodeMap.put("/", rootNode);

        Collections.sort(files, Comparator.comparingInt(f -> f.getFilename().length()));

        for (SDFileInfo file : files) {
            String fullPath = file.getFilename();
            if (fullPath.equals("/")) { /* Skip if is root */
                continue;
            }

            String parentPath = getParentPath(fullPath);
            AxoSDFileNode parentNode = pathToNodeMap.get(parentPath);

            if (parentNode == null) {
                System.err.println(Instant.now() + " Warning: Parent node not found for: " + fullPath + " (parent: " + parentPath + ")");
                continue;
            }

            AxoSDFileNode currentNode = new AxoSDFileNode(file);
            parentNode.addChild(currentNode);
            pathToNodeMap.put(fullPath, currentNode);
        }

        /* After building the raw tree, sort children within each node */
        sortTreeChildren(rootNode, new AxoSDFileComparator());
        traverseTreeForDisplay(rootNode, 0, new ArrayList<>(), sortedDisplayNodes);
    }

    private boolean isLastChildInParentList(AxoSDFileNode childNode, AxoSDFileNode parentNode) {
        if (parentNode == null || parentNode.getChildren() == null || parentNode.getChildren().isEmpty()) {
            return true; /* If no children or no parent, it is the 'last' (or the only) */
        }

        List<AxoSDFileNode> children = parentNode.getChildren();
        return children.get(children.size() - 1) == childNode;
    }


    private void traverseTreeForDisplay(AxoSDFileNode node, int currentDepth,
                                        List<Boolean> parentIsLastChildFlags,
                                        List<DisplayTreeNode> displayList) {

        if (!node.getFileInfo().getFilename().equals("/") || currentDepth > 0) {
            DisplayTreeNode displayNode = new DisplayTreeNode(
                node.getFileInfo(),
                currentDepth,
                node.getParent() != null && isLastChildInParentList(node, node.getParent()),
                new ArrayList<>(parentIsLastChildFlags)
            );
            displayList.add(displayNode);
        }

        if (node.isDirectory() && node.getChildren() != null) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                AxoSDFileNode child = node.getChildren().get(i);
                boolean childIsLast = (i == node.getChildren().size() - 1);

                List<Boolean> childParentFlags = new ArrayList<>(parentIsLastChildFlags);
                if (!node.getFileInfo().getFilename().equals("/")) {
                    childParentFlags.add(childIsLast);
                }

                traverseTreeForDisplay(child, currentDepth + 1, childParentFlags, displayList);
            }
        }
    }

    private String getParentPath(String filePath) {
        if (filePath.equals("/")) {
            return null; /* Root has no 'parent path' */
        }

        /* Handle trailing slash for directories */
        String path = filePath;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            return path.substring(0, lastSlash) + "/";
        }
        else if (lastSlash == 0) {
            return "/";
        }

        return "/"; /* Default to root if no parent found */
    }

    private void sortTreeChildren(AxoSDFileNode node, Comparator<AxoSDFileNode> comparator) {
        if (node.isDirectory() && node.getChildren() != null) {
            Collections.sort(node.getChildren(), comparator);
            for (AxoSDFileNode child : node.getChildren()) {
                sortTreeChildren(child, comparator);
            }
        }
    }

    public ArrayList<SDFileInfo> getSortedDisplayFiles() {
        ArrayList<SDFileInfo> displayList = new ArrayList<>();
        if (rootNode != null) {
            traverseTreeForDisplay(rootNode, displayList);
        }
        if (!displayList.isEmpty() && displayList.get(0).getFilename().equals("/")) {
            displayList.remove(0);
        }
        return displayList;
    }

    private void traverseTreeForDisplay(AxoSDFileNode node, List<SDFileInfo> displayList) {
        if (!node.getFileInfo().getFilename().equals("/") || node == rootNode) {
            // System.out.println(Instant.now() + " SDCardInfo: traverseTreeForDisplay(): added node " + node.getFileInfo().getFilename());
            displayList.add(node.getFileInfo());
        }

        if (node.isDirectory() && node.getChildren() != null) {
            for (AxoSDFileNode child : node.getChildren()) {
                traverseTreeForDisplay(child, displayList);
            }
        }
    }

    public synchronized List<DisplayTreeNode> getDisplayTreeNodes() {
        return sortedDisplayNodes;
    }

    public synchronized List<DisplayTreeNode> getSortedDisplayNodes() {
        if (sortedDisplayNodes == null) {
            // System.out.println(Instant.now() + " SDCardInfo: Rebuilding sortedDisplayNodes. Current files size: " + files.size());

            Map<String, SDFileInfo> fileMap = new HashMap<>();
            for (SDFileInfo file : files) {
                fileMap.put(file.getFilename(), file);
            }

            Map<String, List<SDFileInfo>> childrenMap = new HashMap<>();
            childrenMap.put("/", new ArrayList<>());

            for (SDFileInfo file : files) {
                String parentPath = getParentPath(file.getFilename());
                if (parentPath != null) {
                    childrenMap.computeIfAbsent(parentPath, k -> new ArrayList<>()).add(file);
                }
            }

            /* Sort children within each directory */
            for (List<SDFileInfo> childList : childrenMap.values()) {
                Collections.sort(childList, (f1, f2) -> {
                    /* Directories first, then alphabetically */
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getFilename().compareToIgnoreCase(f2.getFilename());
                });
            }

            List<SDFileInfo> rootLevelSDFileInfos = childrenMap.getOrDefault("/", Collections.emptyList());
            List<DisplayTreeNode> tempNodes = new ArrayList<>();

            buildTreeNodes(rootLevelSDFileInfos, 0, new ArrayList<Boolean>(), tempNodes, childrenMap);

            sortedDisplayNodes = tempNodes;
            // System.out.println(Instant.now() + " SDCardInfo: Finished rebuilding sortedDisplayNodes. New list size: " + sortedDisplayNodes.size());
        }
        return sortedDisplayNodes;
    }

    public synchronized ArrayList<SDFileInfo> getFiles() {
        buildFileTree();
        return getSortedDisplayFiles();
    }

    public int getClusters() {
        return clusters;
    }

    public int getClustersize() {
        return clustersize;
    }

    public int getSectorsize() {
        return sectorsize;
    }

    public synchronized void AddFile(String fname, int size, int timestamp) {
        int DY = 1980 + ((timestamp & 0x0FE00) >> 9);
        int DM = ((timestamp & 0x01E0) >> 5);
        int DD = (timestamp & 0x001F);
        int TH = (int) ((timestamp & 0x0F8000000l) >> 27);
        int TM = (timestamp & 0x07E00000) >> 21;
        int TS = (timestamp & 0x001F0000) >> 15;
        Calendar date = Calendar.getInstance();
        date.set(DY, DM - 1, DD, TH, TM, TS);
        AddFile(fname, size, date);
    }

    public synchronized void AddFile(String fname, int size, Calendar date) {
        // if (fname.lastIndexOf(0) > 0) { <--- SEB What's this call doing??
        //     fname = fname.substring(0, fname.lastIndexOf(0));
        // }

        if (fname.equals("/")) {
            /* Ignore root entry */
            return;
        }

        SDFileInfo sdf = null;
        for (SDFileInfo f : files) {
            if (f.getFilename().equalsIgnoreCase(fname)) {
                // already present
                sdf = f;
            }
        }

        if (sdf != null) {
            /* Create a new SDFileInfo object with updated data */
            SDFileInfo updatedSdf = new SDFileInfo(sdf.getFilename(), date, size, sdf.isDirectory());

            int index = files.indexOf(sdf);
            if (index != -1) {
                files.set(index, updatedSdf); /* Replace old with the new object */
            }
            sortedDisplayNodes = null; /* Invalidate cache */
            mainframe.getFilemanager().getAxoSDFileTableModel().setData(sortedDisplayNodes);
            return;
        }

        boolean isDirectory = fname.endsWith("/");
        sdf = new SDFileInfo(fname, date, size, isDirectory);
        // System.out.println(Instant.now() + " SDCardInfo.AddFile(), fname=" + fname + " size=" + size + " date=" + date.getTime().toString() + " isDirectory=" + isDirectory);

        files.add(sdf);
        sortedDisplayNodes = null;
        mainframe.getFilemanager().getAxoSDFileTableModel().setData(sortedDisplayNodes);
    }

    public synchronized void Delete(String fname) {
        SDFileInfo f1 = null;
        for (SDFileInfo f : files) {
            if (f.getFilename().equalsIgnoreCase(fname)
                    || f.getFilename().equalsIgnoreCase(fname + "/")) {
                f1 = f;
                break;
            }
        }
        if (f1 != null) {
            // System.out.println(Instant.now() + " SDCardInfo.Delete(), fname=" + fname);
            files.remove(f1);
            sortedDisplayNodes = null;
        }
    }

    public synchronized void clear() {
        // System.out.println(Instant.now() + " SDCardInfo.clear(), cleared file list.");
        files.clear();
        sortedDisplayNodes = null;
    }

    public synchronized SDFileInfo find(String name) {
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        for (SDFileInfo f : files) {
            if (f.getFilename().equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }

    public synchronized boolean exists(String name, long timestampEpoch, long size) {
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        for (SDFileInfo f : files) {
            if (f.getFilename().equalsIgnoreCase(name) && f.getSize() == size && (Math.abs(f.getTimestamp().getTimeInMillis() - timestampEpoch) < 1000)) {
                return true;
            }
        }
        return false;
    }

    public static String getFatFsErrorString(int error_code) {
        switch (error_code) {
            case 0:  return "FR_OK";                   /* (0) Succeeded */
            case 1:  return "FR_DISK_ERR";             /* (1) A hard error occurred in the low level disk I/O layer */
            case 2:  return "FR_INT_ERR";              /* (2) Assertion failed */
            case 3:  return "FR_NOT_READY";            /* (3) The physical drive cannot work */
            case 4:  return "FR_NO_FILE";              /* (4) Could not find the file */
            case 5:  return "FR_NO_PATH";              /* (5) Could not find the path */
            case 6:  return "FR_INVALID_NAME";         /* (6) The path name format is invalid */
            case 7:  return "FR_DENIED";               /* (7) Access denied due to prohibited access or directory full */
            case 8:  return "FR_EXIST";                /* (8) Access denied due to prohibited access */
            case 9:  return "FR_INVALID_OBJECT";       /* (9) The file/directory object is invalid */
            case 10: return "FR_WRITE_PROTECTED";     /* (10) The physical drive is write protected */
            case 11: return "FR_INVALID_DRIVE";       /* (11) The logical drive number is invalid */
            case 12: return "FR_NOT_ENABLED";         /* (12) The volume has no work area */
            case 13: return "FR_NO_FILESYSTEM";       /* (13) There is no valid FAT volume */
            case 14: return "FR_MKFS_ABORTED";        /* (14) The f_mkfs() aborted due to any parameter error */
            case 15: return "FR_TIMEOUT";             /* (15) Could not get a grant to access the volume within defined period */
            case 16: return "FR_LOCKED";              /* (16) The operation is rejected according to the file sharing policy */
            case 17: return "FR_NOT_ENOUGH_CORE";     /* (17) LFN working buffer could not be allocated */
            case 18: return "FR_TOO_MANY_OPEN_FILES"; /* (18) Number of open files > _FS_SHARE */
            case 19: return "FR_INVALID_PARAMETER";   /* (19) Given parameter is invalid */
            default: return "FR_??? (" + error_code + ")";
        }
    }
}
