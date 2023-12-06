/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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

import axoloti.ConnectionStatusListener;
import axoloti.USBBulkConnection;
import components.PianoComponent;
import components.control.ACtrlEvent;
import components.control.ACtrlListener;
import components.control.DialComponent;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

/**
 *
 * @author Johannes Taelman
 */
public class KeyboardFrame extends javax.swing.JFrame implements ConnectionStatusListener {

    /**
     * Creates new form PianoFrame
     */
    PianoComponent piano;

    DialComponent pbenddial;

    // Preferences prefs = Preferences.LoadPreferences();

    public KeyboardFrame() {
        initComponents();
        setIconImage(new ImageIcon(getClass().getResource("/resources/ksoloti_keyboard_icon.png")).getImage());
        piano = new PianoComponent() {
            @Override
            public void KeyDown(int key) {
                USBBulkConnection.GetConnection().SendMidi(0x90 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, key & 0x7F, jSliderVelocity.getValue());
            }

            @Override
            public void KeyUp(int key) {
                USBBulkConnection.GetConnection().SendMidi(0x80 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, key & 0x7F, 80);
            }

        };
        setResizable(false);
        Dimension d = new Dimension(910, 92);
        piano.setMinimumSize(d);
        piano.setSize(d);
        piano.setPreferredSize(d);
        piano.setMaximumSize(d);
        piano.setVisible(true);
        jPanelPiano.add(piano);
        pbenddial = new DialComponent(0.0, -64, 64, 1);
        pbenddial.addACtrlListener(new ACtrlListener() {
            @Override
            public void ACtrlAdjusted(ACtrlEvent e) {
                USBBulkConnection.GetConnection().SendMidi(0xE0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, 0, 0x07F & (int) (pbenddial.getValue() - 64.0));
            }

            @Override
            public void ACtrlAdjustmentBegin(ACtrlEvent e) {
            }

            @Override
            public void ACtrlAdjustmentFinished(ACtrlEvent e) {
            }
        });
        jPanelMain.add(new JLabel("Pitch Bend "));
        jPanelMain.add(pbenddial);
        USBBulkConnection.GetConnection().addConnectionStatusListener(this);
    }


    private void initComponents() {

        jPanelPiano = new javax.swing.JPanel();
        jPanelMain = new javax.swing.JPanel();
        jLabelMidiChannel = new javax.swing.JLabel();
        jSpinnerMidiChannel = new javax.swing.JSpinner();
        jLabelVelocity = new javax.swing.JLabel();
        jSliderVelocity = new javax.swing.JSlider();
        jButtonAllNotesOff = new javax.swing.JButton();

        Dimension di = new java.awt.Dimension(5,0);
        filler1 = new javax.swing.Box.Filler(di, di, di);
        filler2 = new javax.swing.Box.Filler(di, di, di);
        filler4 = new javax.swing.Box.Filler(di, di, di);

        Dimension dv = new java.awt.Dimension(0,50);
        filler5 = new javax.swing.Box.Filler(dv, dv, dv);

        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0), new java.awt.Dimension(32767, 0));

        setMaximumSize(null);
        setMinimumSize(new java.awt.Dimension(200, 60));
        setModalExclusionType(java.awt.Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        setName("Keyboard"); // NOI18N
        setPreferredSize(new java.awt.Dimension(917, 150));
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
              jSliderVelocity.requestFocus();
            }
        });
        
        jPanelPiano.setAlignmentX(LEFT_ALIGNMENT);
        jPanelPiano.setAlignmentY(TOP_ALIGNMENT);
        jPanelPiano.setMaximumSize(new java.awt.Dimension(905, 72));
        jPanelPiano.setMinimumSize(new java.awt.Dimension(905, 72));
        jPanelPiano.setPreferredSize(new java.awt.Dimension(905, 72));
        jPanelPiano.setLayout(new javax.swing.BoxLayout(jPanelPiano, javax.swing.BoxLayout.LINE_AXIS));
        getContentPane().add(jPanelPiano);

        jPanelMain.setAlignmentX(LEFT_ALIGNMENT);
        jPanelMain.setAlignmentY(TOP_ALIGNMENT);
        // jPanelMain.setMinimumSize(new java.awt.Dimension(500, 50));
        // jPanelMain.setPreferredSize(new java.awt.Dimension(500, 50));
        jPanelMain.setLayout(new javax.swing.BoxLayout(jPanelMain, javax.swing.BoxLayout.LINE_AXIS));
        // jPanelMain.add(Box.createRigidArea(new Dimension(500, 50)));
        jPanelMain.add(filler5);

        jLabelMidiChannel.setText("MIDI Channel");
        jPanelMain.add(jLabelMidiChannel);
        jPanelMain.add(filler1);

        jSpinnerMidiChannel.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        jSpinnerMidiChannel.setMaximumSize(new java.awt.Dimension(60, 30));
        jSpinnerMidiChannel.setMinimumSize(new java.awt.Dimension(60, 30));
        jSpinnerMidiChannel.setPreferredSize(new java.awt.Dimension(60, 30));
        jPanelMain.add(jSpinnerMidiChannel);
        jPanelMain.add(filler2);

        jLabelVelocity.setText("Velocity");
        jPanelMain.add(jLabelVelocity);

        jSliderVelocity.setMajorTickSpacing(25);
        jSliderVelocity.setMaximum(127);
        jSliderVelocity.setMinimum(1);
        jSliderVelocity.setPaintTicks(true);
        jSliderVelocity.setValue(100);
        jSliderVelocity.setMinimumSize(new java.awt.Dimension(128, 31));
        jPanelMain.add(jSliderVelocity);
        jPanelMain.add(filler4);

        jButtonAllNotesOff.setText("All Notes Off");
        jButtonAllNotesOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAllNotesOffActionPerformed(evt);
            }
        });
        jPanelMain.add(jButtonAllNotesOff);
        jPanelMain.add(filler3);

        getContentPane().add(jPanelMain);

        pack();
    }

    private void jButtonAllNotesOffActionPerformed(java.awt.event.ActionEvent evt) {
        USBBulkConnection.GetConnection().SendMidi(0xB0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, 0x7B, 80);
        piano.clear();
    }

    // Variables declaration - do not modify
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.JButton jButtonAllNotesOff;
    private javax.swing.JLabel jLabelMidiChannel;
    private javax.swing.JLabel jLabelVelocity;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelPiano;
    private javax.swing.JSlider jSliderVelocity;
    private javax.swing.JSpinner jSpinnerMidiChannel;
    // End of variables declaration

    @Override
    public void ShowConnect() {
        piano.clear();
        piano.setEnabled(true);
        pbenddial.setEnabled(true);
    }

    @Override
    public void ShowDisconnect() {
        piano.clear();
        piano.setEnabled(false);
        pbenddial.setEnabled(false);
    }
}
