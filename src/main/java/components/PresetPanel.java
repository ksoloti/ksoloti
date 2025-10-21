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

import axoloti.Patch;

/**
 *
 * @author Johannes Taelman
 */
public class PresetPanel extends javax.swing.JPanel {

    Patch p;

    /**
     * Creates new form PresetPanel
     *
     * @param p patch to recall preset
     */
    public PresetPanel(Patch p) {
        this.p = p;
        initComponents();
    }

    public void GUIShowLiveState(boolean live) {
        jButtonRI.setEnabled(live);
        jButtonR1.setEnabled(live);
        jButtonR2.setEnabled(live);
        jButtonR3.setEnabled(live);
        jButtonR4.setEnabled(live);
        jButtonR5.setEnabled(live);
        jButtonR6.setEnabled(live);
        jButtonR7.setEnabled(live);
        jButtonR8.setEnabled(live);
    }


    private void initComponents() {

        // buttonGroup1 = new javax.swing.ButtonGroup();
        jPanelLabels = new javax.swing.JPanel();
        jPanelPresetButtons = new javax.swing.JPanel();
        jLabelRecall = new javax.swing.JLabel();
        jLabelEdit = new javax.swing.JLabel();
        jLabelTitle = new javax.swing.JLabel();
        jButtonRI = new javax.swing.JButton();
        jButtonR1 = new javax.swing.JButton();
        jButtonR2 = new javax.swing.JButton();
        jButtonR3 = new javax.swing.JButton();
        jButtonR4 = new javax.swing.JButton();
        jButtonR5 = new javax.swing.JButton();
        jButtonR6 = new javax.swing.JButton();
        jButtonR7 = new javax.swing.JButton();
        jButtonR8 = new javax.swing.JButton();
        jButtonEI = new javax.swing.JButton();
        jToggleButtonE1 = new javax.swing.JToggleButton();
        jToggleButtonE2 = new javax.swing.JToggleButton();
        jToggleButtonE3 = new javax.swing.JToggleButton();
        jToggleButtonE4 = new javax.swing.JToggleButton();
        jToggleButtonE5 = new javax.swing.JToggleButton();
        jToggleButtonE6 = new javax.swing.JToggleButton();
        jToggleButtonE7 = new javax.swing.JToggleButton();
        jToggleButtonE8 = new javax.swing.JToggleButton();

        setAlignmentX(LEFT_ALIGNMENT);
        setMaximumSize(new java.awt.Dimension(400, 40));
        setMinimumSize(new java.awt.Dimension(400, 40));
        setPreferredSize(new java.awt.Dimension(400, 40));
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));

        jLabelTitle.setText("Presets");
        add(jLabelTitle);

        jPanelLabels.setLayout(new java.awt.GridLayout(2, 1));

        jLabelRecall.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabelRecall.setText("Recall ");
        jPanelLabels.add(jLabelRecall);

        jLabelEdit.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabelEdit.setText("Edit ");
        jPanelLabels.add(jLabelEdit);

        add(jPanelLabels);

        jPanelPresetButtons.setMinimumSize(new java.awt.Dimension(275, 38));
        jPanelPresetButtons.setPreferredSize(new java.awt.Dimension(275, 38));
        jPanelPresetButtons.setLayout(new java.awt.GridLayout(2, 9));

        jButtonRI.setText("i");
        jButtonRI.setDefaultCapable(false);
        jButtonRI.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonRI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRIActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonRI);

        jButtonR1.setText("1");
        jButtonR1.setDefaultCapable(false);
        jButtonR1.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonR1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonR1ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonR1);

        jButtonR2.setText("2");
        jButtonR2.setDefaultCapable(false);
        jButtonR2.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonR2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonR2ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonR2);

        jButtonR3.setText("3");
        jButtonR3.setDefaultCapable(false);
        jButtonR3.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonR3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonR3ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonR3);

        jButtonR4.setText("4");
        jButtonR4.setDefaultCapable(false);
        jButtonR4.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonR4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonR4ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonR4);

        jButtonR5.setText("5");
        jButtonR5.setDefaultCapable(false);
        jButtonR5.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonR5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonR5ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonR5);

        jButtonR6.setText("6");
        jButtonR6.setDefaultCapable(false);
        jButtonR6.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonR6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonR6ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonR6);

        jButtonR7.setText("7");
        jButtonR7.setDefaultCapable(false);
        jButtonR7.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonR7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonR7ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonR7);

        jButtonR8.setText("8");
        jButtonR8.setDefaultCapable(false);
        jButtonR8.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonR8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonR8ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonR8);

        jButtonEI.setText("i");
        jButtonEI.setSelected(true);
        jButtonEI.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jButtonEI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEIActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jButtonEI);

        jToggleButtonE1.setText("1");
        jToggleButtonE1.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jToggleButtonE1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonE1ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jToggleButtonE1);

        jToggleButtonE2.setText("2");
        jToggleButtonE2.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jToggleButtonE2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonE2ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jToggleButtonE2);

        jToggleButtonE3.setText("3");
        jToggleButtonE3.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jToggleButtonE3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonE3ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jToggleButtonE3);

        jToggleButtonE4.setText("4");
        jToggleButtonE4.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jToggleButtonE4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonE4ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jToggleButtonE4);

        jToggleButtonE5.setText("5");
        jToggleButtonE5.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jToggleButtonE5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonE5ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jToggleButtonE5);

        jToggleButtonE6.setText("6");
        jToggleButtonE6.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jToggleButtonE6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonE6ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jToggleButtonE6);

        jToggleButtonE7.setText("7");
        jToggleButtonE7.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jToggleButtonE7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonE7ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jToggleButtonE7);

        jToggleButtonE8.setText("8");
        jToggleButtonE8.setMargin(new java.awt.Insets(-1, -1, -1, -1));
        jToggleButtonE8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonE8ActionPerformed(evt);
            }
        });
        jPanelPresetButtons.add(jToggleButtonE8);

        add(jPanelPresetButtons);
    }

    private void jButtonRIActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(0);
    }

    private void jButtonR1ActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(1);
    }

    private void jButtonR2ActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(2);
    }

    private void jButtonR3ActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(3);
    }

    private void jButtonR4ActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(4);
    }

    private void jButtonR5ActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(5);
    }

    private void jButtonR6ActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(6);
    }

    private void jButtonR7ActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(7);
    }

    private void jButtonR8ActionPerformed(java.awt.event.ActionEvent evt) {
        p.RecallPreset(8);
    }

    private void jButtonEIActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(true);
        jToggleButtonE1.setSelected(false);
        jToggleButtonE2.setSelected(false);
        jToggleButtonE3.setSelected(false);
        jToggleButtonE4.setSelected(false);
        jToggleButtonE5.setSelected(false);
        jToggleButtonE6.setSelected(false);
        jToggleButtonE7.setSelected(false);
        jToggleButtonE8.setSelected(false);
        p.ShowPreset(0);
    }

    private void jToggleButtonE1ActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(false);
        jToggleButtonE1.setSelected(true);
        jToggleButtonE2.setSelected(false);
        jToggleButtonE3.setSelected(false);
        jToggleButtonE4.setSelected(false);
        jToggleButtonE5.setSelected(false);
        jToggleButtonE6.setSelected(false);
        jToggleButtonE7.setSelected(false);
        jToggleButtonE8.setSelected(false);
        p.ShowPreset(1);
    }

    private void jToggleButtonE2ActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(false);
        jToggleButtonE1.setSelected(false);
        jToggleButtonE2.setSelected(true);
        jToggleButtonE3.setSelected(false);
        jToggleButtonE4.setSelected(false);
        jToggleButtonE5.setSelected(false);
        jToggleButtonE6.setSelected(false);
        jToggleButtonE7.setSelected(false);
        jToggleButtonE8.setSelected(false);
        p.ShowPreset(2);
    }

    private void jToggleButtonE3ActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(false);
        jToggleButtonE1.setSelected(false);
        jToggleButtonE2.setSelected(false);
        jToggleButtonE3.setSelected(true);
        jToggleButtonE4.setSelected(false);
        jToggleButtonE5.setSelected(false);
        jToggleButtonE6.setSelected(false);
        jToggleButtonE7.setSelected(false);
        jToggleButtonE8.setSelected(false);
        p.ShowPreset(3);
    }

    private void jToggleButtonE4ActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(false);
        jToggleButtonE1.setSelected(false);
        jToggleButtonE2.setSelected(false);
        jToggleButtonE3.setSelected(false);
        jToggleButtonE4.setSelected(true);
        jToggleButtonE5.setSelected(false);
        jToggleButtonE6.setSelected(false);
        jToggleButtonE7.setSelected(false);
        jToggleButtonE8.setSelected(false);
        p.ShowPreset(4);
    }

    private void jToggleButtonE5ActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(false);
        jToggleButtonE1.setSelected(false);
        jToggleButtonE2.setSelected(false);
        jToggleButtonE3.setSelected(false);
        jToggleButtonE4.setSelected(false);
        jToggleButtonE5.setSelected(true);
        jToggleButtonE6.setSelected(false);
        jToggleButtonE7.setSelected(false);
        jToggleButtonE8.setSelected(false);
        p.ShowPreset(5);
    }

    private void jToggleButtonE6ActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(false);
        jToggleButtonE1.setSelected(false);
        jToggleButtonE2.setSelected(false);
        jToggleButtonE3.setSelected(false);
        jToggleButtonE4.setSelected(false);
        jToggleButtonE5.setSelected(false);
        jToggleButtonE6.setSelected(true);
        jToggleButtonE7.setSelected(false);
        jToggleButtonE8.setSelected(false);
        p.ShowPreset(6);
    }

    private void jToggleButtonE7ActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(false);
        jToggleButtonE1.setSelected(false);
        jToggleButtonE2.setSelected(false);
        jToggleButtonE3.setSelected(false);
        jToggleButtonE4.setSelected(false);
        jToggleButtonE5.setSelected(false);
        jToggleButtonE6.setSelected(false);
        jToggleButtonE7.setSelected(true);
        jToggleButtonE8.setSelected(false);
        p.ShowPreset(7);
    }

    private void jToggleButtonE8ActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonEI.setSelected(false);
        jToggleButtonE1.setSelected(false);
        jToggleButtonE2.setSelected(false);
        jToggleButtonE3.setSelected(false);
        jToggleButtonE4.setSelected(false);
        jToggleButtonE5.setSelected(false);
        jToggleButtonE6.setSelected(false);
        jToggleButtonE7.setSelected(false);
        jToggleButtonE8.setSelected(true);
        p.ShowPreset(8);
    }

    private javax.swing.JButton jButtonR1;
    private javax.swing.JButton jButtonR2;
    private javax.swing.JButton jButtonR3;
    private javax.swing.JButton jButtonR4;
    private javax.swing.JButton jButtonR5;
    private javax.swing.JButton jButtonR6;
    private javax.swing.JButton jButtonR7;
    private javax.swing.JButton jButtonR8;
    private javax.swing.JButton jButtonRI;
    private javax.swing.JLabel jLabelRecall;
    private javax.swing.JLabel jLabelEdit;
    private javax.swing.JLabel jLabelTitle;
    private javax.swing.JPanel jPanelLabels;
    private javax.swing.JPanel jPanelPresetButtons;
    private javax.swing.JToggleButton jToggleButtonE1;
    private javax.swing.JToggleButton jToggleButtonE2;
    private javax.swing.JToggleButton jToggleButtonE3;
    private javax.swing.JToggleButton jToggleButtonE4;
    private javax.swing.JToggleButton jToggleButtonE5;
    private javax.swing.JToggleButton jToggleButtonE6;
    private javax.swing.JToggleButton jToggleButtonE7;
    private javax.swing.JToggleButton jToggleButtonE8;
    private javax.swing.JButton jButtonEI;
}
