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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
    DialComponent moddial;
    DialComponent ccdial;
    DialComponent velodial;

    // Preferences prefs = Preferences.LoadPreferences();

    public KeyboardFrame() {
        initComponents();
        setIconImage(new ImageIcon(getClass().getResource("/resources/ksoloti_keyboard_icon.png")).getImage());
        piano = new PianoComponent() {
            @Override
            public void KeyDown(int key) {
                USBBulkConnection.GetConnection().SendMidi(0x90 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, key & 0x7F, (int)velodial.getValue());
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
        jPanelPiano.add(filler_x_fivepx1);
        jPanelPiano.add(piano);

        USBBulkConnection.GetConnection().addConnectionStatusListener(this);
    }


    private void initComponents() {

        jPanelPiano = new javax.swing.JPanel();
        jPanelMain = new javax.swing.JPanel();
        jLabelMidiChannel = new javax.swing.JLabel();
        jSpinnerMidiChannel = new javax.swing.JSpinner();
        jLabelControlChange = new javax.swing.JLabel();
        jSpinnerControlChange = new javax.swing.JSpinner();
        jLabelVelocity = new javax.swing.JLabel();
        jButtonAllNotesOff = new javax.swing.JButton();

        Dimension di = new java.awt.Dimension(5,0);
        filler_x_fivepx1 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx2 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx3 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx4 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx5 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx6 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx7 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx8 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx9 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx10 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx11 = new javax.swing.Box.Filler(di, di, di);
        filler_x_fivepx12 = new javax.swing.Box.Filler(di, di, di);

        Dimension dv = new java.awt.Dimension(0,50);
        filler_y_fiftypx1 = new javax.swing.Box.Filler(dv, dv, dv);

        filler_x_stretch1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0), new java.awt.Dimension(32767, 0));
        filler_x_stretch2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0), new java.awt.Dimension(32767, 0));

        setMaximumSize(null);
        setModalExclusionType(java.awt.Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        setName("Keyboard"); // NOI18N
        setPreferredSize(new java.awt.Dimension(914, 155));
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
              velodial.requestFocus();
            }
        });
        
        jPanelPiano.setAlignmentX(LEFT_ALIGNMENT);
        jPanelPiano.setAlignmentY(TOP_ALIGNMENT);
        jPanelPiano.setMaximumSize(new java.awt.Dimension(910, 72));
        jPanelPiano.setMinimumSize(new java.awt.Dimension(910, 72));
        jPanelPiano.setPreferredSize(new java.awt.Dimension(910, 72));
        jPanelPiano.setLayout(new javax.swing.BoxLayout(jPanelPiano, javax.swing.BoxLayout.LINE_AXIS));
        getContentPane().add(jPanelPiano);

        jPanelMain.setAlignmentX(LEFT_ALIGNMENT);
        jPanelMain.setAlignmentY(TOP_ALIGNMENT);
        // jPanelMain.setMinimumSize(new java.awt.Dimension(500, 50));
        // jPanelMain.setPreferredSize(new java.awt.Dimension(500, 50));
        jPanelMain.setLayout(new javax.swing.BoxLayout(jPanelMain, javax.swing.BoxLayout.LINE_AXIS));
        // jPanelMain.add(Box.createRigidArea(new Dimension(500, 50)));

        jPanelMain.add(filler_y_fiftypx1);

        jPanelMain.add(filler_x_fivepx2);
        pbenddial = new DialComponent(0.0, -64, 63, 1);
        pbenddial.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {
                    pbenddial.setValue(0.0);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
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
        jPanelMain.add(new JLabel("Pitchbend "));
        jPanelMain.add(filler_x_fivepx3);
        jPanelMain.add(pbenddial);
        jPanelMain.add(filler_x_fivepx4);

        moddial = new DialComponent(0, 0, 127, 1);
        moddial.addACtrlListener(new ACtrlListener() {
            @Override
            public void ACtrlAdjusted(ACtrlEvent e) {
                USBBulkConnection.GetConnection().SendMidi(0xB0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, 1, 0x07F & (int) (moddial.getValue()));
            }

            @Override
            public void ACtrlAdjustmentBegin(ACtrlEvent e) {
            }

            @Override
            public void ACtrlAdjustmentFinished(ACtrlEvent e) {
            }
        });
        jPanelMain.add(new JLabel("Modwheel "));
        jPanelMain.add(filler_x_fivepx5);
        jPanelMain.add(moddial);
        jPanelMain.add(filler_x_stretch2);

        jLabelControlChange.setText("CC#");
        jPanelMain.add(jLabelControlChange);
        jPanelMain.add(filler_x_fivepx6);

        jSpinnerControlChange.setModel(new javax.swing.SpinnerNumberModel(7, 0, 127, 1));
        jSpinnerControlChange.setMaximumSize(new java.awt.Dimension(70, 30));
        jSpinnerControlChange.setMinimumSize(new java.awt.Dimension(70, 30));
        jSpinnerControlChange.setPreferredSize(new java.awt.Dimension(70, 30));
        jPanelMain.add(jSpinnerControlChange);
        jPanelMain.add(filler_x_fivepx7);

        ccdial = new DialComponent(0, 0, 127, 1);
        ccdial.addACtrlListener(new ACtrlListener() {
            @Override
            public void ACtrlAdjusted(ACtrlEvent e) {
                USBBulkConnection.GetConnection().SendMidi(0xB0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, ((SpinnerNumberModel) jSpinnerControlChange.getModel()).getNumber().intValue(), 0x07F & (int) (ccdial.getValue()));
            }

            @Override
            public void ACtrlAdjustmentBegin(ACtrlEvent e) {
            }

            @Override
            public void ACtrlAdjustmentFinished(ACtrlEvent e) {
            }
        });
        jPanelMain.add(ccdial);
        jPanelMain.add(filler_x_stretch1);

        jLabelMidiChannel.setText("MIDI Channel");
        jPanelMain.add(jLabelMidiChannel);
        jPanelMain.add(filler_x_fivepx8);

        jSpinnerMidiChannel.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        jSpinnerMidiChannel.setMaximumSize(new java.awt.Dimension(60, 30));
        jSpinnerMidiChannel.setMinimumSize(new java.awt.Dimension(60, 30));
        jSpinnerMidiChannel.setPreferredSize(new java.awt.Dimension(60, 30));
        jPanelMain.add(jSpinnerMidiChannel);
        jPanelMain.add(filler_x_fivepx9);

        jLabelVelocity.setText("Velocity");
        jPanelMain.add(jLabelVelocity);
        jPanelMain.add(filler_x_fivepx10);

        velodial = new DialComponent(100, 0, 127, 1);
        velodial.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {
                    velodial.setValue(100.0);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        // velodial.addACtrlListener(new ACtrlListener() {
        //     @Override
        //     public void ACtrlAdjusted(ACtrlEvent e) {
        //         USBBulkConnection.GetConnection().SendMidi(0xB0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, ((SpinnerNumberModel) jSpinnerControlChange.getModel()).getNumber().intValue(), 0x07F & (int) (ccdial.getValue()));
        //     }

        //     @Override
        //     public void ACtrlAdjustmentBegin(ACtrlEvent e) {
        //     }

        //     @Override
        //     public void ACtrlAdjustmentFinished(ACtrlEvent e) {
        //     }
        // });
        jPanelMain.add(velodial);
        jPanelMain.add(filler_x_fivepx11);

        jButtonAllNotesOff.setText("All Notes Off");
        jButtonAllNotesOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAllNotesOffActionPerformed(evt);
            }
        });
        jPanelMain.add(jButtonAllNotesOff);
        jPanelMain.add(filler_x_fivepx12);

        getContentPane().add(jPanelMain);

        pack();
    }

    private void jButtonAllNotesOffActionPerformed(java.awt.event.ActionEvent evt) {
        USBBulkConnection.GetConnection().SendMidi(0xB0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, 0x7B, 80);
        piano.clear();
    }

    // Variables declaration - do not modify
    private javax.swing.Box.Filler filler_x_fivepx1;
    private javax.swing.Box.Filler filler_x_fivepx2;
    private javax.swing.Box.Filler filler_x_fivepx3;
    private javax.swing.Box.Filler filler_x_fivepx4;
    private javax.swing.Box.Filler filler_x_fivepx5;
    private javax.swing.Box.Filler filler_x_fivepx6;
    private javax.swing.Box.Filler filler_x_fivepx7;
    private javax.swing.Box.Filler filler_x_fivepx8;
    private javax.swing.Box.Filler filler_x_fivepx9;
    private javax.swing.Box.Filler filler_x_fivepx10;
    private javax.swing.Box.Filler filler_x_fivepx11;
    private javax.swing.Box.Filler filler_x_fivepx12;

    private javax.swing.Box.Filler filler_y_fiftypx1;

    private javax.swing.Box.Filler filler_x_stretch1;
    private javax.swing.Box.Filler filler_x_stretch2;

    private javax.swing.JButton jButtonAllNotesOff;
    private javax.swing.JLabel jLabelMidiChannel;
    private javax.swing.JLabel jLabelControlChange;
    private javax.swing.JLabel jLabelVelocity;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelPiano;
    private javax.swing.JSpinner jSpinnerMidiChannel;
    private javax.swing.JSpinner jSpinnerControlChange;
    // End of variables declaration

    @Override
    public void ShowConnect() {
        piano.clear();
        piano.setEnabled(true);
        pbenddial.setEnabled(true);
        ccdial.setEnabled(true);
        moddial.setEnabled(true);
    }

    @Override
    public void ShowDisconnect() {
        piano.clear();
        piano.setEnabled(false);
        pbenddial.setEnabled(false);
        ccdial.setEnabled(false);
        moddial.setEnabled(false);
    }
}
