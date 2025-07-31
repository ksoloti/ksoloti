package axoloti.utils;

import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
// import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.swing.table.AbstractTableModel;

import axoloti.SDFileInfo;

public class AxoSDFileTableModel extends AbstractTableModel {

    private static char decimalSeparator = new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator();

    private final String[] columnNames = {"Name", "Type", "Size", "Modified"};
    private List<DisplayTreeNode> data;

    public AxoSDFileTableModel(List<DisplayTreeNode> fileTreeData) {
        this.data = (fileTreeData != null) ? fileTreeData : new ArrayList<>();
        // System.out.println(Instant.now() + " AxoSDFileTableModel: Constructor received list of size: " + (data != null ? data.size() : "null"));

    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return String.class;
    }

    @Override
    public int getRowCount() {
        int count = (data != null) ? data.size() : 0;
        // System.out.println(Instant.now() + " AxoSDFileTableModel: getRowCount() called, returning: " + count + (data == null ? " (data is null)" : ""));
        return count;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object returnValue = null;

        DisplayTreeNode displayNode = data.get(rowIndex);
        SDFileInfo f = displayNode.fileInfo; // Get the original SDFileInfo

        switch (columnIndex) {
            case 0: {
                returnValue = f.getPureName();
                break;
            }
            case 1: {
                if (f.isDirectory()) {
                    returnValue = "[ D ]";
                }
                else {
                    returnValue = f.getExtension();
                }
                break;
            }
            case 2: {
                if (f.isDirectory()) {
                    returnValue = "";
                }
                else {
                    long kilo = 1024;
                    long mega = 1024 * 1024;
                    long size = f.getSize();
                    if (size < kilo) {
                        returnValue = "" + size + " B";
                    }
                    else if (size < mega / 10) {
                        returnValue = "" + (size / kilo) + decimalSeparator + (size % kilo) / 103 + " kB";
                    }
                    else if (size < mega) {
                        returnValue = "" + (size / kilo) + " kB";
                    }
                    else if (size < mega * 100) {
                        returnValue = "" + (size / mega) + decimalSeparator + (size % mega) / (mega / 10) + " MB";
                    }
                    else {
                        returnValue = "" + (size / mega) + " MB";
                    }
                }
                break;
            }
            case 3: {
                Calendar c = f.getTimestamp();
                if (c != null && c.get(Calendar.YEAR) > 1980) {
                    returnValue = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault(Locale.Category.FORMAT)).format(c.getTime());
                }
                else {
                    returnValue = "";
                }
                break;
            }
        }

        return returnValue;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public DisplayTreeNode getDisplayTreeNode(int row) {
        if (data != null && row >= 0 && row < data.size()) {
            return data.get(row);
        }
        return null;
    }

    public void setData(List<DisplayTreeNode> newData) {
        this.data = (newData != null) ? newData : new ArrayList<>();
        fireTableDataChanged();
    }
}    
