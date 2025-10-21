/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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
package axoloti.dialogs;

import axoloti.Patch;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.parameters.ParameterInstance;
import axoloti.utils.Constants;
import components.ScrollPaneComponent;

import java.awt.Dimension;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author jtaelman
 */
public class MidiAssignments extends javax.swing.JDialog {

    /**
     * Creates new form MidiAssignments
     */
    public MidiAssignments(java.awt.Frame parent, boolean modal, ParameterInstance param) {
        super(parent, modal);
        setLocationRelativeTo(param.GetObjectInstance().getPatch().getPatchframe());
        setPreferredSize(new Dimension(640, 480));
        setTitle("MIDI CC# Assignment for [ " + param.GetObjectInstance().getInstanceName() + ":" + param.getName() + " ]");
        initComponents();
        setIconImage(Constants.APP_ICON.getImage());
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        Patch patch = param.GetObjectInstance().patch;
        String CCObj[] = new String[128];
        String CCParam[] = new String[128];
        for (AxoObjectInstanceAbstract obj : patch.objectInstances) {
            Collection<ParameterInstance> params = obj.getParameterInstances();
            if (params != null) {
                for (ParameterInstance<?> param1 : params) {
                    int cc = param1.getMidiCC();
                    if (cc >= 0) {
                        CCObj[cc] = obj.getInstanceName();
                        CCParam[cc] = param1.getName();
                    }
                }
            }
        }
        result = param.getMidiCC();
        for (int i = 0; i < 128; i++) {
            String name = "";
            if (i < MidiCCToName.length) {
                name = MidiCCToName[i];
            }
            model.addRow(new Object[]{i, name, CCObj[i], CCParam[i]});
        }
        if (result >= 0) {
            jTable1.setRowSelectionInterval(result, result);
            jTable1.scrollRectToVisible(jTable1.getCellRect(result, 0, true));
        }
        setVisible(true);
    }

    private int result;

    public int getResult() {
        return result;
    }


    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new ScrollPaneComponent();
        jTable1 = new javax.swing.JTable();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10));
        jPanel2 = new javax.swing.JPanel();
        jButtonCancel = new javax.swing.JButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(0, 0));
        jButtonDeassign = new javax.swing.JButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(0, 0));
        jButtonAssign = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jPanel1.setLayout(new java.awt.BorderLayout());

        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "CC#", "GM Default", "-> Object", "-> Parameter"
            }
        ) {
            Class<?>[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        /* Center integers (CC numbers) */
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        jTable1.setDefaultRenderer(Integer.class, centerRenderer);
        jTable1.setRowHeight(24);

        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(32);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(140);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(140);
            jTable1.getColumnModel().getColumn(3).setPreferredWidth(140);

        }
        jScrollPane1.setViewportView(jTable1);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1);
        getContentPane().add(filler1);

        jPanel2.setLayout(new java.awt.GridBagLayout());
        jPanel2.setMaximumSize(new Dimension(640, 32));

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });
        jPanel2.add(jButtonCancel, new java.awt.GridBagConstraints());
        jPanel2.add(filler3, new java.awt.GridBagConstraints());

        jButtonDeassign.setText("Unassign");
        jButtonDeassign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeassignActionPerformed(evt);
            }
        });
        jPanel2.add(jButtonDeassign, new java.awt.GridBagConstraints());
        jPanel2.add(filler4, new java.awt.GridBagConstraints());

        jButtonAssign.setText("Assign");
        jButtonAssign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAssignActionPerformed(evt);
            }
        });
        jPanel2.add(jButtonAssign, new java.awt.GridBagConstraints());

        getContentPane().add(jPanel2);
        getContentPane().add(filler2);

        pack();
    }


    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    private void jButtonDeassignActionPerformed(java.awt.event.ActionEvent evt) {
        result = -1;
        dispose();
    }

    private void jButtonAssignActionPerformed(java.awt.event.ActionEvent evt) {
        result = jTable1.getSelectedRow();
        dispose();
    }


    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.JButton jButtonAssign;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonDeassign;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private ScrollPaneComponent jScrollPane1;
    private javax.swing.JTable jTable1;

    static String[] MidiCCToName = {
        "Bank Select",
        "Modulation",
        "Breath Controller",
        "Undefined",
        "Foot Controller",
        "Portamento Time",
        "Data Entry MSB",
        "Volume",
        "Balance",
        "Undefined",
        "Pan",
        "Expression",
        "Effect Controller 1",
        "Effect Controller 2",
        "Undefined",
        "Undefined",
        "General Purpose",
        "General Purpose",
        "General Purpose",
        "General Purpose",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Controller 0 LSB",
        "Controller 1 LSB",
        "Controller 2 LSB",
        "Controller 3 LSB",
        "Controller 4 LSB",
        "Controller 5 LSB",
        "Controller 6 LSB",
        "Controller 7 LSB",
        "Controller 8 LSB",
        "Controller 9 LSB",
        "Controller 10 LSB",
        "Controller 11 LSB",
        "Controller 12 LSB",
        "Controller 13 LSB",
        "Controller 14 LSB",
        "Controller 15 LSB",
        "Controller 16 LSB",
        "Controller 17 LSB",
        "Controller 18 LSB",
        "Controller 19 LSB",
        "Controller 20 LSB",
        "Controller 21 LSB",
        "Controller 22 LSB",
        "Controller 23 LSB",
        "Controller 24 LSB",
        "Controller 25 LSB",
        "Controller 26 LSB",
        "Controller 27 LSB",
        "Controller 28 LSB",
        "Controller 29 LSB",
        "Controller 30 LSB",
        "Controller 31 LSB",
        "Sustain Pedal",
        "Portamento On/Off",
        "Sostenuto On/Off",
        "Soft Pedal On/Off",
        "Legato FootSwitch",
        "Hold 2",
        "Sound Controller 1",
        "Sound Controller 2",
        "Sound Controller 3",
        "Sound Controller 4",
        "Sound Controller 5",
        "Sound Controller 6",
        "Sound Controller 7",
        "Sound Controller 8",
        "Sound Controller 9",
        "Sound Controller 10",
        "General Purpose MIDI CC",
        "General Purpose MIDI CC",
        "General Purpose MIDI CC",
        "General Purpose MIDI CC",
        "Portamento CC Control",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Effect 1 Depth",
        "Effect 2 Depth",
        "Effect 3 Depth",
        "Effect 4 Depth",
        "Effect 5 Depth",
        "Data Increment",
        "Data Decrement",
        "NRPN LSB",
        "NRPN MSB",
        "RPN LSB",
        "RPN MSB",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "Undefined",
        "All Sound Off",
        "Reset All Controllers",
        "Local On/Off Switch",
        "All Notes Off",
        "Omni Mode Off",
        "Omni Mode On",
        "Mono Mode",
        "Poly Mode"};
}
