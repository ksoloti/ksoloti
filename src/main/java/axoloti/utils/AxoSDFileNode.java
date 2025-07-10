package axoloti.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import axoloti.SDFileInfo;

/**
 *
 * @author Ksoloti
 */
public class AxoSDFileNode {

    private SDFileInfo fileInfo; // The actual file/directory data
    private AxoSDFileNode parent; // Reference to its parent node (optional, but useful)
    private List<AxoSDFileNode> children; // List of direct children (only for directories)

    public AxoSDFileNode(SDFileInfo fileInfo) {
        this.fileInfo = fileInfo;
        this.children = new ArrayList<>();
    }

    public SDFileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean isDirectory() {
        return fileInfo.isDirectory();
    }

    // Add a child node
    public void addChild(AxoSDFileNode child) {
        if (this.isDirectory()) {
            this.children.add(child);
            child.setParent(this); // Set parent reference
        }
        else {
            // Log a warning or throw an exception: trying to add child to a file node
            System.err.println("Warning: Attempted to add child to a file node: " + this.fileInfo.getFilename());
        }
    }

    public List<AxoSDFileNode> getChildren() {
        return children;
    }

    public AxoSDFileNode getParent() {
        return parent;
    }

    public void setParent(AxoSDFileNode parent) {
        this.parent = parent;
    }

    // You might want a method to sort the children of this specific node
    public void sortChildren(Comparator<AxoSDFileNode> comparator) {
        if (this.isDirectory()) {
            Collections.sort(this.children, comparator);
        }
    }
}
