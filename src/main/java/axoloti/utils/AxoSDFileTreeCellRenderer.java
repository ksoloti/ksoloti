package axoloti.utils;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Ksoloti
 */
public class AxoSDFileTreeCellRenderer extends DefaultTableCellRenderer {

    // private static final String VERTICAL_LINE = "│   ";
    // private static final String T_JUNCTION = "├─  ";
    // private static final String L_JUNCTION = "└─  ";
    private static final String INDENT = "    "; // Three spaces for standard indent

    private static final EmptyBorder paddingBorder = new EmptyBorder(0, 10, 0, 10); /* 10px padding, left and right */


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        // Call super to get the default JLabel component and styling
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        AxoSDFileTableModel model = (AxoSDFileTableModel) table.getModel(); // Cast to your custom TableModel
        DisplayTreeNode node = model.getDisplayTreeNode(row); // You'd add this method to MyTableModel

        if (node != null) {
            StringBuilder prefix = new StringBuilder();

            // Build the indentation prefix based on depth and parent flags
            for (int i = 0; i < node.depth; i++) {
                if (i < node.parentIsLastChildFlags.size()) {
                    // if (node.parentIsLastChildFlags.get(i)) {
                        prefix.append(INDENT); // Ancestor was last, so no vertical line
                    // } else {
                    //     prefix.append(VERTICAL_LINE); // Ancestor was not last, so draw vertical line
                    // }
                } else {
                    // This case generally shouldn't happen if parentIsLastChildFlags is correctly populated
                    prefix.append(INDENT);
                }
            }
            
            // Add the specific junction for the current node
            // if (node.depth > 0) { // Don't add junction for root level items
            //     if (node.isLastChild) {
            //         prefix.append(L_JUNCTION);
            //     } else {
            //         prefix.append(T_JUNCTION);
            //     }
            // }
            if (node.fileInfo.isDirectory()) {
                prefix.append("▼");
            }

            label.setText(prefix.toString() + node.fileInfo.getPureName());
        }

        label.setBorder(paddingBorder);

        return label;
    }
}