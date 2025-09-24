/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
package axoloti.dialogs;

import axoloti.USBBulkConnection;
import axoloti.listener.ConnectionStatusListener;
// import axoloti.ui.SvgIconLoader;
import axoloti.utils.KeyUtils;
import components.PianoComponent;
import components.control.ACtrlEvent;
import components.control.ACtrlListener;
import components.control.DialComponent;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

// import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.AbstractAction;

/**
 *
 * @author Johannes Taelman
 */
public class KeyboardFrame extends javax.swing.JFrame implements ConnectionStatusListener {

    /**
     * Creates new form PianoFrame
     */
    PianoComponent piano;
    private int pianoTranspose = 48; /* base octave at C3 */

    private Map<Integer, Integer> keyNoteMap;

    DialComponent pbenddial;
    DialComponent moddial;
    DialComponent ccdial;
    DialComponent velodial;

    public KeyboardFrame() {
        initComponents();
        setupKeyNoteMap();

        // Icon icon = SvgIconLoader.load("/resources/appicons/ksoloti_keyboard_icon.svg", 32);
        // if (icon != null) {
        //     if (icon instanceof ImageIcon) {
        //         setIconImage(((ImageIcon) icon).getImage());
        //     } else {
        //         setIconImage(SvgIconLoader.toBufferedImage(icon));
        //     }
        // } else {
            // System.err.println("Failed to load SVG icon. Falling back to PNG.");
            setIconImage(new ImageIcon(getClass().getResource("/resources/appicons/ksoloti_keyboard_icon.png")).getImage());
        // }

        piano = new PianoComponent() {
            @Override
            public void KeyDown(int key) {
                piano.setSelection(key);
                USBBulkConnection.getInstance().TransmitMidi(0x90 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, key & 0x7F, (int)velodial.getValue());
            }

            @Override
            public void KeyUp(int key) {
                piano.clearSelection(key);
                USBBulkConnection.getInstance().TransmitMidi(0x80 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, key & 0x7F, 80);
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

        USBBulkConnection.getInstance().addConnectionStatusListener(this);

        setupKeyBindings();
    }

    /**
     * Initializes the map for keyboard key to MIDI note number mapping.
     */
    private void setupKeyNoteMap() {
        keyNoteMap = new HashMap<>();

        // Lower octave
        keyNoteMap.put(KeyEvent.VK_Z, 0);
        keyNoteMap.put(KeyEvent.VK_S, 1);
        keyNoteMap.put(KeyEvent.VK_X, 2);
        keyNoteMap.put(KeyEvent.VK_D, 3);
        keyNoteMap.put(KeyEvent.VK_C, 4);
        keyNoteMap.put(KeyEvent.VK_V, 5);
        keyNoteMap.put(KeyEvent.VK_G, 6);
        keyNoteMap.put(KeyEvent.VK_B, 7);
        keyNoteMap.put(KeyEvent.VK_H, 8);
        keyNoteMap.put(KeyEvent.VK_N, 9);
        keyNoteMap.put(KeyEvent.VK_J, 10);
        keyNoteMap.put(KeyEvent.VK_M, 11);
        keyNoteMap.put(KeyEvent.VK_COMMA, 12);

        // Upper octave
        keyNoteMap.put(KeyEvent.VK_Q, 12);
        keyNoteMap.put(KeyEvent.VK_2, 13);
        keyNoteMap.put(KeyEvent.VK_W, 14);
        keyNoteMap.put(KeyEvent.VK_3, 15);
        keyNoteMap.put(KeyEvent.VK_E, 16);
        keyNoteMap.put(KeyEvent.VK_R, 17);
        keyNoteMap.put(KeyEvent.VK_5, 18);
        keyNoteMap.put(KeyEvent.VK_T, 19);
        keyNoteMap.put(KeyEvent.VK_6, 20);
        keyNoteMap.put(KeyEvent.VK_Y, 21);
        keyNoteMap.put(KeyEvent.VK_7, 22);
        keyNoteMap.put(KeyEvent.VK_U, 23);
        keyNoteMap.put(KeyEvent.VK_I, 24);
    }


    private void initComponents() {

        jPanelPiano = new javax.swing.JPanel();
        jPanelMain = new javax.swing.JPanel();
        jLabelMidiChannel = new javax.swing.JLabel();
        jSpinnerMidiChannel = new javax.swing.JSpinner();
        jLabelControlChange = new javax.swing.JLabel();
        jSpinnerControlChange = new javax.swing.JSpinner();
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

        Dimension d10 = new java.awt.Dimension(10,0);
        filler_x_tenpx1 = new javax.swing.Box.Filler(d10, d10, d10);
        filler_x_tenpx2 = new javax.swing.Box.Filler(d10, d10, d10);
        filler_x_tenpx3 = new javax.swing.Box.Filler(d10, d10, d10);

        Dimension dv = new java.awt.Dimension(0,50);
        filler_y_fiftypx1 = new javax.swing.Box.Filler(dv, dv, dv);

        filler_x_stretch1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0), new java.awt.Dimension(32767, 0));
        filler_x_stretch2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0), new java.awt.Dimension(32767, 0));

        setMaximumSize(null);
        setModalExclusionType(java.awt.Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        setName("Virtual Keyboard"); // NOI18N
        setPreferredSize(new java.awt.Dimension(928, 155));
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
              jPanelPiano.requestFocus();
            }
        });

        jPanelPiano.setAlignmentX(LEFT_ALIGNMENT);
        jPanelPiano.setAlignmentY(TOP_ALIGNMENT);
        jPanelPiano.setMaximumSize(new java.awt.Dimension(910, 72));
        jPanelPiano.setMinimumSize(new java.awt.Dimension(910, 72));
        jPanelPiano.setPreferredSize(new java.awt.Dimension(910, 72));
        jPanelPiano.setLayout(new javax.swing.BoxLayout(jPanelPiano, javax.swing.BoxLayout.LINE_AXIS));
        jPanelPiano.requestFocus();

        jPanelPiano.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent ke) {
                int key = ke.getKeyCode();
                
                if (ke.isControlDown() || ke.isShiftDown() || ke.isAltDown() || ke.isMetaDown()) {
                    return;
                }

                Integer note = keyNoteMap.get(key);
                if (note != null && !piano.isKeyDown(pianoTranspose + note)) {
                    piano.KeyDown(pianoTranspose + note);
                    ke.consume();
                } else if (key == KeyEvent.VK_DOWN) {
                    int val = ke.isShiftDown() ? 5 : 1;
                    moddial.setValue(moddial.getValue() - val);
                    ke.consume();
                } else if (key == KeyEvent.VK_UP) {
                    int val = ke.isShiftDown() ? 5 : 1;
                    moddial.setValue(moddial.getValue() + val);
                    ke.consume();
                } else if (key == KeyEvent.VK_LEFT) {
                    pianoTranspose -= 12;
                    pianoTranspose = pianoTranspose < 0 ? 0 : pianoTranspose > 96 ? 96 : pianoTranspose;
                    piano.setTranspose(pianoTranspose);
                    ke.consume();
                } else if (key == KeyEvent.VK_RIGHT) {
                    pianoTranspose += 12;
                    pianoTranspose = pianoTranspose < 0 ? 0 : pianoTranspose > 96 ? 96 : pianoTranspose;
                    piano.setTranspose(pianoTranspose);
                    ke.consume();
                } else if (key == KeyEvent.VK_MINUS) {
                    int val = ke.isShiftDown() ? 5 : 1;
                    velodial.setValue(velodial.getValue() - val);
                    ke.consume();
                } else if (key == KeyEvent.VK_EQUALS) {
                    int val = ke.isShiftDown() ? 5 : 1;
                    velodial.setValue(velodial.getValue() + val);
                    ke.consume();
                } else if (key == KeyEvent.VK_OPEN_BRACKET) {
                    int val = ke.isShiftDown() ? 5 : 1;
                    ccdial.setValue(ccdial.getValue() - val);
                    ke.consume();
                } else if (key == KeyEvent.VK_CLOSE_BRACKET) {
                    int val = ke.isShiftDown() ? 5 : 1;
                    ccdial.setValue(ccdial.getValue() + val);
                    ke.consume();
                } else if (key == KeyEvent.VK_PAGE_DOWN) {
                    try {
                        jSpinnerMidiChannel.setValue(jSpinnerMidiChannel.getModel().getPreviousValue());
                    } catch (Exception e) {
                        /* Silent */
                    }
                    ke.consume();
                } else if (key == KeyEvent.VK_PAGE_UP) {
                    try {
                        jSpinnerMidiChannel.setValue(jSpinnerMidiChannel.getModel().getNextValue());
                    } catch (Exception e) {
                        /* Silent */
                    }
                    ke.consume();
                } else if (key == KeyEvent.VK_SPACE) {
                    jButtonAllNotesOffActionPerformed(null);
                    ke.consume();
                }

                piano.repaint();
            }

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.isControlDown() || ke.isShiftDown() || ke.isAltDown() || ke.isMetaDown()) {
                    return;
                }

                int key = ke.getKeyCode();
                Integer note = keyNoteMap.get(key);
                if (note != null) {
                    piano.KeyUp(pianoTranspose + note);
                    ke.consume();
                }
                piano.repaint();
            }

            @Override
            public void keyTyped(KeyEvent ke) {
            }
        });

        getContentPane().add(jPanelPiano);

        jPanelMain.setAlignmentX(LEFT_ALIGNMENT);
        jPanelMain.setAlignmentY(TOP_ALIGNMENT);
        // jPanelMain.setMinimumSize(new java.awt.Dimension(500, 50));
        // jPanelMain.setPreferredSize(new java.awt.Dimension(500, 50));
        jPanelMain.setLayout(new javax.swing.BoxLayout(jPanelMain, javax.swing.BoxLayout.LINE_AXIS));
        // jPanelMain.add(Box.createRigidArea(new Dimension(500, 50)));

        jPanelMain.add(filler_y_fiftypx1);

        jPanelMain.add(filler_x_tenpx1);
        pbenddial = new DialComponent(0.0, -64, 63, 1);
        pbenddial.setFocusable(false);
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
                USBBulkConnection.getInstance().TransmitMidi(0xE0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, 0, 0x07F & (int) (pbenddial.getValue() - 64.0));
            }

            @Override
            public void ACtrlAdjustmentBegin(ACtrlEvent e) {
            }

            @Override
            public void ACtrlAdjustmentFinished(ACtrlEvent e) {
            }
        });
        jPanelMain.add(new JLabel("Pitchbend"));
        jPanelMain.add(filler_x_fivepx2);
        jPanelMain.add(pbenddial);
        jPanelMain.add(filler_x_tenpx2);

        moddial = new DialComponent(0, 0, 127, 1);
        moddial.setFocusable(false);
        moddial.addACtrlListener(new ACtrlListener() {
            @Override
            public void ACtrlAdjusted(ACtrlEvent e) {
                USBBulkConnection.getInstance().TransmitMidi(0xB0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, 1, 0x07F & (int) (moddial.getValue()));
            }

            @Override
            public void ACtrlAdjustmentBegin(ACtrlEvent e) {
            }

            @Override
            public void ACtrlAdjustmentFinished(ACtrlEvent e) {
            }
        });
        jPanelMain.add(new JLabel("Modwheel"));
        jPanelMain.add(filler_x_fivepx3);
        jPanelMain.add(moddial);
        jPanelMain.add(filler_x_tenpx3);

        velodial = new DialComponent(100, 0, 127, 1);
        velodial.setFocusable(false);
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
        jPanelMain.add(new JLabel("Velocity"));
        jPanelMain.add(filler_x_fivepx4);
        jPanelMain.add(velodial);
        jPanelMain.add(filler_x_stretch2);

        jLabelControlChange.setText("CC#");
        jPanelMain.add(jLabelControlChange);
        jPanelMain.add(filler_x_fivepx5);

        jSpinnerControlChange.setModel(new javax.swing.SpinnerNumberModel(7, 0, 127, 1));
        jSpinnerControlChange.setMaximumSize(new java.awt.Dimension(70, 30));
        jSpinnerControlChange.setMinimumSize(new java.awt.Dimension(70, 30));
        jSpinnerControlChange.setPreferredSize(new java.awt.Dimension(70, 30));
        jPanelMain.add(jSpinnerControlChange);
        jPanelMain.add(filler_x_fivepx6);

        ccdial = new DialComponent(0, 0, 127, 1);
        ccdial.setFocusable(false);
        ccdial.addACtrlListener(new ACtrlListener() {
            @Override
            public void ACtrlAdjusted(ACtrlEvent e) {
                USBBulkConnection.getInstance().TransmitMidi(0xB0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, ((SpinnerNumberModel) jSpinnerControlChange.getModel()).getNumber().intValue(), 0x07F & (int) (ccdial.getValue()));
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
        jPanelMain.add(filler_x_fivepx7);

        jSpinnerMidiChannel.setModel(new javax.swing.SpinnerNumberModel(1, 1, 16, 1));
        jSpinnerMidiChannel.setMaximumSize(new java.awt.Dimension(60, 30));
        jSpinnerMidiChannel.setMinimumSize(new java.awt.Dimension(60, 30));
        jSpinnerMidiChannel.setPreferredSize(new java.awt.Dimension(60, 30));
        jPanelMain.add(jSpinnerMidiChannel);
        jPanelMain.add(filler_x_fivepx8);

        jButtonAllNotesOff.setText("All Notes Off");
        jButtonAllNotesOff.setFocusable(false);
        jButtonAllNotesOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAllNotesOffActionPerformed(evt);
            }
        });
        jPanelMain.add(jButtonAllNotesOff);
        jPanelMain.add(filler_x_fivepx9);

        getContentPane().add(jPanelMain);

        pack();
    }

    private void jButtonAllNotesOffActionPerformed(java.awt.event.ActionEvent evt) {
        USBBulkConnection.getInstance().TransmitMidi(0xB0 + ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).getNumber().intValue() - 1, 0x7B, 80);
        piano.clear();
    }

    private javax.swing.Box.Filler filler_x_fivepx1;
    private javax.swing.Box.Filler filler_x_fivepx2;
    private javax.swing.Box.Filler filler_x_fivepx3;
    private javax.swing.Box.Filler filler_x_fivepx4;
    private javax.swing.Box.Filler filler_x_fivepx5;
    private javax.swing.Box.Filler filler_x_fivepx6;
    private javax.swing.Box.Filler filler_x_fivepx7;
    private javax.swing.Box.Filler filler_x_fivepx8;
    private javax.swing.Box.Filler filler_x_fivepx9;

    private javax.swing.Box.Filler filler_x_tenpx1;
    private javax.swing.Box.Filler filler_x_tenpx2;
    private javax.swing.Box.Filler filler_x_tenpx3;

    private javax.swing.Box.Filler filler_y_fiftypx1;

    private javax.swing.Box.Filler filler_x_stretch1;
    private javax.swing.Box.Filler filler_x_stretch2;

    private javax.swing.JButton jButtonAllNotesOff;
    private javax.swing.JLabel jLabelMidiChannel;
    private javax.swing.JLabel jLabelControlChange;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelPiano;
    private javax.swing.JSpinner jSpinnerMidiChannel;
    private javax.swing.JSpinner jSpinnerControlChange;

    @Override
    public void ShowConnect() {
        piano.clear();
        piano.setEnabled(true);
        pbenddial.setEnabled(true);
        ccdial.setEnabled(true);
        moddial.setEnabled(true);
        jButtonAllNotesOff.setEnabled(true);
    }

    @Override
    public void ShowDisconnect() {
        piano.clear();
        piano.setEnabled(false);
        pbenddial.setEnabled(false);
        ccdial.setEnabled(false);
        moddial.setEnabled(false);
        jButtonAllNotesOff.setEnabled(false);
    }

    /**
     * Sets up keyboard shortcuts for the frame.
     */
    private void setupKeyBindings() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        KeyStroke closeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyUtils.CONTROL_OR_CMD_MASK);
        inputMap.put(closeKeyStroke, "closeWindow");
        actionMap.put("closeWindow", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setVisible(false);
            }
        });
    }
}
