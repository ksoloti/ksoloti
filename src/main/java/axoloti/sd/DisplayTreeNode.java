package axoloti.sd;

import java.util.List;

/**
 *
 * @author Ksoloti
 */
public class DisplayTreeNode {

    public SDFileInfo fileInfo;
    public int depth;
    public boolean isLastChild;
    public List<Boolean> parentIsLastChildFlags;

    public DisplayTreeNode(SDFileInfo fileInfo, int depth, boolean isLastChild, List<Boolean> parentIsLastChildFlags) {
        this.fileInfo = fileInfo;
        this.depth = depth;
        this.isLastChild = isLastChild;
        this.parentIsLastChildFlags = parentIsLastChildFlags;
    }

    public boolean isLastChild() {
        return isLastChild;
    }
}
