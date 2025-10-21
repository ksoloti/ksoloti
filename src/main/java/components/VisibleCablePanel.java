/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
package components;

import axoloti.PatchGUI;
import axoloti.datatypes.Bool32;
import axoloti.datatypes.CharPtr32;
import axoloti.datatypes.Frac32;
import axoloti.datatypes.Frac32buffer;
import axoloti.datatypes.Int32;
// import axoloti.datatypes.Int32Ptr;
import axoloti.ui.Theme;

public class VisibleCablePanel extends javax.swing.JPanel {

    PatchGUI p;

    public VisibleCablePanel(PatchGUI p) {
        this.p = p;
        initComponents();

        /* Red wires */
        jCheckBoxFrac32Buffer.setBackground(Theme.Cable_Frac32Buffer);
        jCheckBoxFrac32Buffer.setOpaque(true);
        jCheckBoxFrac32Buffer.setSelected(true);
        jCheckBoxFrac32Buffer.setRolloverEnabled(false);
        jCheckBoxFrac32Buffer.setToolTipText("Audio rate signals, 48 kHz sampling rate.");

        /* Blue wires */
        jCheckBoxFrac32.setBackground(Theme.Cable_Frac32);
        jCheckBoxFrac32.setOpaque(true);
        jCheckBoxFrac32.setSelected(true);
        jCheckBoxFrac32.setRolloverEnabled(false);
        jCheckBoxFrac32.setToolTipText("Control rate signals, 3 kHz sampling rate.");

        /* Yellow wires */
        jCheckBoxBool32.setBackground(Theme.Cable_Bool32);
        jCheckBoxBool32.setOpaque(true);
        jCheckBoxBool32.setSelected(true);
        jCheckBoxBool32.setRolloverEnabled(false);
        jCheckBoxBool32.setToolTipText("Boolean signals (true/false, on/off etc.) at control rate, 3 kHz.");

        /* Green wires */
        jCheckBoxInt32.setBackground(Theme.Cable_Int32);
        jCheckBoxInt32.setOpaque(true);
        jCheckBoxInt32.setSelected(true);
        jCheckBoxInt32.setRolloverEnabled(false);
        jCheckBoxInt32.setToolTipText("Integer signals (whole numbers) at control rate , 3 kHz.");

        
        /* Pink wires */
        jCheckBoxCharPointer32.setBackground(Theme.Cable_CharPointer32);
        jCheckBoxCharPointer32.setOpaque(true);
        jCheckBoxCharPointer32.setSelected(true);
        jCheckBoxCharPointer32.setRolloverEnabled(false);
        jCheckBoxCharPointer32.setToolTipText("String pointers (strings, character arrays) at control rate, 3 kHz.");
        
        /* Rose... Salmon(?) wires (currently not used?) */
        // jCheckBoxInt32Pointer.setBackground(Theme.Cable_Int32Pointer);
        // jCheckBoxInt32Pointer.setOpaque(true);
        // jCheckBoxInt32Pointer.setSelected(true);
        // jCheckBoxInt32Pointer.setRolloverEnabled(false);
        // jCheckBoxInt32Pointer.setToolTipText("Int32 Pointers (pointers to integer arrays), 3 kHz sampling rate. Currently not used?");
    }


    private void initComponents() {

        jPanelCheckboxes = new javax.swing.JPanel();
        jLabelVisibleCables = new javax.swing.JLabel();
        jCheckBoxFrac32Buffer = new javax.swing.JCheckBox();
        jCheckBoxFrac32 = new javax.swing.JCheckBox();
        jCheckBoxBool32 = new javax.swing.JCheckBox();
        jCheckBoxInt32 = new javax.swing.JCheckBox();
        jCheckBoxCharPointer32 = new javax.swing.JCheckBox();
        // jCheckBoxInt32Pointer = new javax.swing.JCheckBox();

        setAlignmentX(LEFT_ALIGNMENT);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));

        jLabelVisibleCables.setText("Visible Cables");
        jLabelVisibleCables.setAlignmentX(CENTER_ALIGNMENT);
        jLabelVisibleCables.setMaximumSize(null);
        jLabelVisibleCables.setMinimumSize(null);
        jLabelVisibleCables.setPreferredSize(null);
        add(jLabelVisibleCables);

        jPanelCheckboxes.setMaximumSize(null);
        jPanelCheckboxes.setMinimumSize(null);
        jPanelCheckboxes.setPreferredSize(null);
        jPanelCheckboxes.setLayout(new javax.swing.BoxLayout(jPanelCheckboxes, javax.swing.BoxLayout.LINE_AXIS));

        jCheckBoxFrac32Buffer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxFrac32BufferActionPerformed(evt);
            }
        });
        jPanelCheckboxes.add(jCheckBoxFrac32Buffer);

        jCheckBoxFrac32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxFrac32ActionPerformed(evt);
            }
        });
        jPanelCheckboxes.add(jCheckBoxFrac32);

        jCheckBoxBool32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxBool32ActionPerformed(evt);
            }
        });
        jPanelCheckboxes.add(jCheckBoxBool32);

        jCheckBoxInt32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxInt32ActionPerformed(evt);
            }
        });
        jPanelCheckboxes.add(jCheckBoxInt32);
    
        jCheckBoxCharPointer32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxCharPointer32ActionPerformed(evt);
            }
        });
        jPanelCheckboxes.add(jCheckBoxCharPointer32);

        // jCheckBoxInt32Pointer.addActionListener(new java.awt.event.ActionListener() {
        //     public void actionPerformed(java.awt.event.ActionEvent evt) {
        //         jCheckBoxInt32PointerActionPerformed(evt);
        //     }
        // });
        // jPanelCheckboxes.add(jCheckBoxInt32Pointer);

        add(jPanelCheckboxes);
    }

    private void jCheckBoxFrac32BufferActionPerformed(java.awt.event.ActionEvent evt) {
        p.setCableTypeEnabled(Frac32buffer.d, jCheckBoxFrac32Buffer.isSelected());
        p.updateNetVisibility();
    }

    private void jCheckBoxFrac32ActionPerformed(java.awt.event.ActionEvent evt) {
        p.setCableTypeEnabled(Frac32.d, jCheckBoxFrac32.isSelected());
        p.updateNetVisibility();
    }

    private void jCheckBoxBool32ActionPerformed(java.awt.event.ActionEvent evt) {
        p.setCableTypeEnabled(Bool32.d, jCheckBoxBool32.isSelected());
        p.updateNetVisibility();
    }

    private void jCheckBoxInt32ActionPerformed(java.awt.event.ActionEvent evt) {
        p.setCableTypeEnabled(Int32.d, jCheckBoxInt32.isSelected());
        p.updateNetVisibility();
    }

    private void jCheckBoxCharPointer32ActionPerformed(java.awt.event.ActionEvent evt) {
        p.setCableTypeEnabled(CharPtr32.d, jCheckBoxCharPointer32.isSelected());
        p.updateNetVisibility();
    }
    
    // private void jCheckBoxInt32PointerActionPerformed(java.awt.event.ActionEvent evt) {
    //     p.setCableTypeEnabled(Int32Ptr.d, jCheckBoxInt32Pointer.isSelected());
    //     p.updateNetVisibility();
    // }

    private javax.swing.JPanel jPanelCheckboxes;
    private javax.swing.JLabel jLabelVisibleCables;
    private javax.swing.JCheckBox jCheckBoxFrac32Buffer;
    private javax.swing.JCheckBox jCheckBoxFrac32;
    private javax.swing.JCheckBox jCheckBoxBool32;
    private javax.swing.JCheckBox jCheckBoxInt32;
    private javax.swing.JCheckBox jCheckBoxCharPointer32;
    // private javax.swing.JCheckBox jCheckBoxInt32Pointer;
}
