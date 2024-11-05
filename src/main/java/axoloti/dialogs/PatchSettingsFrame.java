/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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

import axoloti.DocumentWindow;
import axoloti.Patch;
import axoloti.PatchSettings;
import axoloti.SubPatchMode;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.GroupLayout;

/**
 *
 * @author Johannes Taelman
 */
public class PatchSettingsFrame extends javax.swing.JFrame implements DocumentWindow {

    private static final Logger LOGGER = Logger.getLogger(PatchSettingsFrame.class.getName());

    PatchSettings settings;

    final Patch patch;

    /**
     * Creates new form PatchSettingsFrame
     *
     * @param settings settings to load/save
     */
    public PatchSettingsFrame(PatchSettings settings, Patch patch) {
        initComponents();
        this.patch = patch;
        setTitle("Patch Settings");
        setIconImage(new ImageIcon(getClass().getResource("/resources/ksoloti_icon_axp.png")).getImage());
        this.settings = settings;
        ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).setValue(settings.GetMidiChannel());
        ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).setMinimum(1);
        ((SpinnerNumberModel) jSpinnerMidiChannel.getModel()).setMaximum(16);
        ((SpinnerNumberModel) jSpinnerMPENumberOfMemberChannels.getModel()).setValue(settings.getMPENumberOfMemberChannels());
        ((SpinnerNumberModel) jSpinnerMPENumberOfMemberChannels.getModel()).setMinimum(1);
        ((SpinnerNumberModel) jSpinnerMPENumberOfMemberChannels.getModel()).setMaximum(15);
        ((SpinnerNumberModel) jSpinnerNumPresets.getModel()).setValue(settings.GetNPresets());
        ((SpinnerNumberModel) jSpinnerNumPresets.getModel()).setMinimum(0);
        ((SpinnerNumberModel) jSpinnerNumPresets.getModel()).setMaximum(64);
        ((SpinnerNumberModel) jSpinnerPresetEntries.getModel()).setValue(settings.GetNPresetEntries());
        ((SpinnerNumberModel) jSpinnerPresetEntries.getModel()).setMinimum(0);
        ((SpinnerNumberModel) jSpinnerPresetEntries.getModel()).setMaximum(64);
        ((SpinnerNumberModel) jSpinnerModulationSources.getModel()).setValue(settings.GetNModulationSources());
        ((SpinnerNumberModel) jSpinnerModulationSources.getModel()).setMinimum(0);
        ((SpinnerNumberModel) jSpinnerModulationSources.getModel()).setMaximum(64);
        ((SpinnerNumberModel) jSpinnerModulationTargets.getModel()).setValue(settings.GetNModulationTargetsPerSource());
        ((SpinnerNumberModel) jSpinnerModulationTargets.getModel()).setMinimum(0);
        ((SpinnerNumberModel) jSpinnerModulationTargets.getModel()).setMaximum(64);
        jTextFieldAuthor.setText(settings.getAuthor());
        jComboBoxLicense.setSelectedItem(settings.getLicense());
        jTextFieldAttributions.setText(settings.getAttributions());
        switch (settings.subpatchmode) {
            case no:
                jComboBoxMode.setSelectedIndex(0);
                break;
            case normal:
                jComboBoxMode.setSelectedIndex(1);
                break;
            case normalBypass:
                jComboBoxMode.setSelectedIndex(2);
                break;
            case polyphonic:
                jComboBoxMode.setSelectedIndex(3);
                break;
            case polychannel:
                jComboBoxMode.setSelectedIndex(4);
                break;
            case polyexpression:
                jComboBoxMode.setSelectedIndex(5);
                break;
        }
        jCheckBoxHasMidiSelector.setSelected(settings.GetMidiSelector());
        jCheckBoxSaturate.setSelected(settings.getSaturate());
    }


    private void initComponents() {

        jLabelMIDIChannel = new javax.swing.JLabel();
        jLabelMPENumberOfMemberChannels = new javax.swing.JLabel();
        jLabelMPEZone = new javax.swing.JLabel();
        jSpinnerMidiChannel = new javax.swing.JSpinner();
        jSpinnerMPENumberOfMemberChannels = new javax.swing.JSpinner();
        jSpinnerNumPresets = new javax.swing.JSpinner();
        jLabelSubpatchMode = new javax.swing.JLabel();
        jComboBoxMode = new javax.swing.JComboBox<String>();
        jComboBoxMPEZone = new javax.swing.JComboBox<String>();
        jLabelNumberOfPresets = new javax.swing.JLabel();
        jLabelEntriesPerPreset = new javax.swing.JLabel();
        jSpinnerPresetEntries = new javax.swing.JSpinner();
        jLabelMaxModulationSources = new javax.swing.JLabel();
        jLabelMaxModulationTargets = new javax.swing.JLabel();
        jSpinnerModulationSources = new javax.swing.JSpinner();
        jSpinnerModulationTargets = new javax.swing.JSpinner();
        jCheckBoxHasMidiSelector = new javax.swing.JCheckBox();
        jTextFieldAuthor = new javax.swing.JTextField();
        jComboBoxLicense = new javax.swing.JComboBox<String>();
        jLabelAuthor = new javax.swing.JLabel();
        jLabelLicense = new javax.swing.JLabel();
        jCheckBoxSaturate = new javax.swing.JCheckBox();
        jLabelAttributions = new javax.swing.JLabel();
        jTextFieldAttributions = new javax.swing.JTextField();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                formComponentHidden(evt);
            }
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });

        jLabelMIDIChannel.setText("MIDI Channel");
        jLabelMIDIChannel.setToolTipText("<html>Defines the MIDI channel the patch should listen on. For subpatches this can be overridden by enabling<br/>\"Show Advanced MIDI Settings\" and freely selecting a device, port, and channel via the subpatch's representation in the parent patch.<br/>Note that if subpatch mode \"MPE\" is selected, this setting is ignored to conform with MPE specs.<br/>See the MPE-related tooltips for more info.");

        jSpinnerMidiChannel.setToolTipText(jLabelMIDIChannel.getToolTipText());
        jSpinnerMidiChannel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 15, 1));
        jSpinnerMidiChannel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerMidiChannelStateChanged(evt);
            }
        });


        jLabelMPEZone.setText("MPE Zone");
        jLabelMPEZone.setToolTipText("<html>Defines which MPE zone the subpatch should listen on.<br/>The Lower zone occupies MIDI channel 1 as Master channel for sending global controls<br/>and from channel 2 upwards as Member channels to control the individual voices.<br/>The upper zone occupies MIDI channel 16 as Master channel<br/>and from channel 15 downwards as Member channels.");
        jComboBoxMPEZone.setToolTipText(jLabelMPEZone.getToolTipText());;

        jLabelMPENumberOfMemberChannels.setText("Number of MPE Member Channels");
        jLabelMPENumberOfMemberChannels.setToolTipText("<html>Defines how many Member channels are to be used by the MPE zone.<br/><br/>Example: <i>Set MPE Zone to \"Lower\" and the number of Member channels to 4</i> --<br/> Master channel occupies MIDI channel 1. Four Member channels occupy MIDI channels 2 to 5.<br/>Channels 6 to 16 are free for playing the main patch, other subpatches, or other devices.");

        jSpinnerMPENumberOfMemberChannels.setToolTipText(jLabelMPENumberOfMemberChannels.getToolTipText());
        jSpinnerMPENumberOfMemberChannels.setModel(new javax.swing.SpinnerNumberModel(15, 1, 15, 1));
        jSpinnerMPENumberOfMemberChannels.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerMPENumberOfMemberChannelsChanged(evt);
            }
        });

        jSpinnerNumPresets.setModel(new javax.swing.SpinnerNumberModel(0, 0, 64, 1));
        jSpinnerNumPresets.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerNumPresetsStateChanged(evt);
            }
        });

        jLabelSubpatchMode.setText("Subpatch Mode");

        jComboBoxMode.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Off", "Mono", "(Reserved)", "Polyphonic", "Polyphonic Multichannel", "MPE (Polyphonic Expression)" }));
        jComboBoxMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxModeActionPerformed(evt);
            }
        });

        jComboBoxMPEZone.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] {"Lower (from Channel 1 upwards)", "Upper (from Channel 16 downwards)"}));
        jComboBoxMPEZone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxMPEZoneActionPerformed(evt);
            }
        });

        jLabelNumberOfPresets.setText("Number of Presets");

        jLabelEntriesPerPreset.setText("Entries per Preset");

        jSpinnerPresetEntries.setModel(new javax.swing.SpinnerNumberModel(0, 0, 64, 1));
        jSpinnerPresetEntries.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerPresetEntriesStateChanged(evt);
            }
        });

        jLabelMaxModulationSources.setText("Maximum Number of Modulation Sources");
        jLabelMaxModulationSources.setToolTipText("Set this to (or higher than) the number of modsource objects you are using in your patch.");

        jLabelMaxModulationTargets.setText("Maximum Number of Targets per Source");
        jLabelMaxModulationTargets.setToolTipText("Set this to the maximum number of targets each modsource should be able to affect.");

        jSpinnerModulationSources.setModel(new javax.swing.SpinnerNumberModel(0, 0, 15, 1));
        jSpinnerModulationSources.setToolTipText(jLabelMaxModulationSources.getToolTipText());
        jSpinnerModulationSources.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerModulationSourcesStateChanged(evt);
            }
        });

        jSpinnerModulationTargets.setModel(new javax.swing.SpinnerNumberModel(0, 0, 15, 1));
        jSpinnerModulationTargets.setToolTipText(jLabelMaxModulationTargets.getToolTipText());
        jSpinnerModulationTargets.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerModulationTargetsStateChanged(evt);
            }
        });

        jCheckBoxHasMidiSelector.setText("Show Advanced MIDI Settings");
        jCheckBoxHasMidiSelector.setToolTipText("<html>Shows a set of attributes (MIDI channel, MIDI port, MIDI device)<br/> on the subpatch, allowing you to override<br/>the MIDI settings for this subpatch independent from its parent.");
        jCheckBoxHasMidiSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxHasChannelAttribActionPerformed(evt);
            }
        });

        jTextFieldAuthor.setText("jTextField1");
        jTextFieldAuthor.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextFieldAuthorFocusLost(evt);
            }
        });

        jComboBoxLicense.setEditable(true);
        jComboBoxLicense.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Undefined", "CC0", "CC BY 3.0", "CC BY SA 3.0", "GPL", "LGPL", "Confidential", "Secret", "Top Secret", " " }));
        jComboBoxLicense.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxLicenseActionPerformed(evt);
            }
        });

        jLabelAuthor.setText("Author");

        jLabelLicense.setText("License");

        jCheckBoxSaturate.setText("Saturate Audio Output");
        jCheckBoxSaturate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSaturateActionPerformed(evt);
            }
        });

        jLabelAttributions.setText("Attributions");

        jTextFieldAttributions.setText("jTextField1");
        jTextFieldAttributions.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextFieldAttributionsFocusLost(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(

            layout.createParallelGroup(Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()

                .addGap(15, 15, 15)

                .addGroup(layout.createParallelGroup(Alignment.LEADING)

                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jLabelAuthor, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelLicense, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelAttributions, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )

                        .addPreferredGap(ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(jTextFieldAuthor, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                            .addComponent(jComboBoxLicense, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                            .addComponent(jTextFieldAttributions, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                        )

                    )

                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jCheckBoxSaturate, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
                    )

                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jLabelMIDIChannel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelNumberOfPresets, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelEntriesPerPreset, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelMaxModulationSources, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelMaxModulationTargets, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )

                        .addPreferredGap(ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(jSpinnerMidiChannel, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinnerNumPresets, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinnerPresetEntries, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinnerModulationSources, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinnerModulationTargets, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                        )
                    )

                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()

                        .addComponent(jLabelSubpatchMode, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)

                        .addPreferredGap(ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(jComboBoxMode, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                            .addComponent(jCheckBoxHasMidiSelector, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                        )
                    )

                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jLabelMPEZone, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelMPENumberOfMemberChannels, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )

                        .addPreferredGap(ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(jComboBoxMPEZone, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                            .addComponent(jSpinnerMPENumberOfMemberChannels, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                        )
                    )
                )

                .addGap(15, 15, 15)
            )
        );

        layout.setVerticalGroup(

            layout.createParallelGroup(Alignment.LEADING)

            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()

                .addGap(15, 15, 15)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelAuthor)
                    .addComponent(jTextFieldAuthor, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelLicense)
                    .addComponent(jComboBoxLicense, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelAttributions)
                    .addComponent(jTextFieldAttributions, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jCheckBoxSaturate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelMIDIChannel)
                    .addComponent(jSpinnerMidiChannel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelNumberOfPresets)
                    .addComponent(jSpinnerNumPresets, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelEntriesPerPreset)
                    .addComponent(jSpinnerPresetEntries, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelMaxModulationSources)
                    .addComponent(jSpinnerModulationSources, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelMaxModulationTargets)
                    .addComponent(jSpinnerModulationTargets, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelSubpatchMode)
                    .addComponent(jComboBoxMode, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jCheckBoxHasMidiSelector)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelMPEZone)
                    .addComponent(jComboBoxMPEZone, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addPreferredGap(ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelMPENumberOfMemberChannels)
                    .addComponent(jSpinnerMPENumberOfMemberChannels, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )

                .addGap(15,15,15)
            )
        );

        pack();
    }

    private void setEnabledMPEOptions(boolean b) {
        jLabelMPEZone.setEnabled(b);
        jComboBoxMPEZone.setEnabled(b);

        jLabelMPENumberOfMemberChannels.setEnabled(b);
        jSpinnerMPENumberOfMemberChannels.setEnabled(b);

        jLabelMIDIChannel.setEnabled(!b);
        jSpinnerMidiChannel.setEnabled(!b);
    }

    private void jSpinnerMidiChannelStateChanged(javax.swing.event.ChangeEvent evt) {
        SpinnerModel nModel = jSpinnerMidiChannel.getModel();
        if (nModel instanceof SpinnerNumberModel) {
            settings.SetMidiChannel(((SpinnerNumberModel) nModel).getNumber().intValue());
        }
    }

    private void jSpinnerMPENumberOfMemberChannelsChanged(javax.swing.event.ChangeEvent evt) {
        SpinnerModel nModel = jSpinnerMPENumberOfMemberChannels.getModel();
        if (nModel instanceof SpinnerNumberModel) {
            settings.setMPENumberOfMemberChannels(((SpinnerNumberModel) nModel).getNumber().intValue());
        }
    }

    private void jComboBoxModeActionPerformed(java.awt.event.ActionEvent evt) {

        /* Set subpatch items as enabled unless overridden by subpatch mode "no" */
        jCheckBoxHasMidiSelector.setEnabled(true);
        /* Set MPE eoptions as disables unless subpatch mode "polyexpression" overrides them */
        setEnabledMPEOptions(false);

        switch (jComboBoxMode.getSelectedIndex()) {
            case 0:
                jCheckBoxHasMidiSelector.setEnabled(false);
                settings.subpatchmode = SubPatchMode.no;
                break;
            case 1:
                settings.subpatchmode = SubPatchMode.normal;
                break;
            case 2:
                settings.subpatchmode = SubPatchMode.normalBypass;
                break;
            case 3:
                settings.subpatchmode = SubPatchMode.polyphonic;
                break;
            case 4:
                settings.subpatchmode = SubPatchMode.polychannel;
                break;
            case 5:
                setEnabledMPEOptions(true);
                settings.subpatchmode = SubPatchMode.polyexpression;
                break;
            default:
                LOGGER.severe("Undefined subpatch mode");
        }
    }

    private void jComboBoxMPEZoneActionPerformed(java.awt.event.ActionEvent evt) {
        int val = jComboBoxMPEZone.getSelectedIndex();
        if (val < 0 || val > 1) {
            LOGGER.severe("Invalid MPE zone");
        }
        else {
            settings.setMPEZone(val);
        }
    }

    private void jSpinnerNumPresetsStateChanged(javax.swing.event.ChangeEvent evt) {
        SpinnerModel nModel = jSpinnerNumPresets.getModel();
        if (nModel instanceof SpinnerNumberModel) {
            settings.SetNPresets(((SpinnerNumberModel) nModel).getNumber().intValue());
            patch.getPatchframe().showPresetPanel(settings.GetNPresets() > 0);
        }
    }

    private void jSpinnerPresetEntriesStateChanged(javax.swing.event.ChangeEvent evt) {
        SpinnerModel nModel = jSpinnerPresetEntries.getModel();
        if (nModel instanceof SpinnerNumberModel) {
            settings.SetNPresetEntries(((SpinnerNumberModel) nModel).getNumber().intValue());
        }
    }

    private void jSpinnerModulationSourcesStateChanged(javax.swing.event.ChangeEvent evt) {
        SpinnerModel nModel = jSpinnerModulationSources.getModel();
        if (nModel instanceof SpinnerNumberModel) {
            settings.SetNModulationSources(((SpinnerNumberModel) nModel).getNumber().intValue());
        }
    }

    private void jSpinnerModulationTargetsStateChanged(javax.swing.event.ChangeEvent evt) {
        SpinnerModel nModel = jSpinnerModulationTargets.getModel();
        if (nModel instanceof SpinnerNumberModel) {
            settings.SetNModulationTargetsPerSource(((SpinnerNumberModel) nModel).getNumber().intValue());
        }
    }

    private void jCheckBoxHasChannelAttribActionPerformed(java.awt.event.ActionEvent evt) {
        settings.SetMidiSelector(jCheckBoxHasMidiSelector.isSelected());
    }

    private void jComboBoxLicenseActionPerformed(java.awt.event.ActionEvent evt) {
        if (jComboBoxLicense.getSelectedItem() != null) {
            settings.setLicense(jComboBoxLicense.getSelectedItem().toString());
        }
    }

    private void jTextFieldAuthorFocusLost(java.awt.event.FocusEvent evt) {
        settings.setAuthor(jTextFieldAuthor.getText());
    }

    private void jCheckBoxSaturateActionPerformed(java.awt.event.ActionEvent evt) {
        settings.setSaturate(jCheckBoxSaturate.isSelected());
    }

    private void jTextFieldAttributionsFocusLost(java.awt.event.FocusEvent evt) {
        settings.setAttributions(jTextFieldAttributions.getText());
    }

    private void formComponentHidden(java.awt.event.ComponentEvent evt) {
        patch.getPatchframe().GetChildDocuments().remove(this);
    }

    private void formComponentShown(java.awt.event.ComponentEvent evt) {
        patch.getPatchframe().GetChildDocuments().add(this);
    }

    private javax.swing.JCheckBox jCheckBoxHasMidiSelector;
    private javax.swing.JCheckBox jCheckBoxSaturate;
    private javax.swing.JComboBox<String> jComboBoxLicense;
    private javax.swing.JComboBox<String> jComboBoxMode;
    private javax.swing.JComboBox<String> jComboBoxMPEZone;
    private javax.swing.JLabel jLabelMIDIChannel;
    private javax.swing.JLabel jLabelMPENumberOfMemberChannels;
    private javax.swing.JLabel jLabelMPEZone;
    private javax.swing.JLabel jLabelAttributions;
    private javax.swing.JLabel jLabelSubpatchMode;
    private javax.swing.JLabel jLabelNumberOfPresets;
    private javax.swing.JLabel jLabelEntriesPerPreset;
    private javax.swing.JLabel jLabelMaxModulationSources;
    private javax.swing.JLabel jLabelMaxModulationTargets;
    private javax.swing.JLabel jLabelAuthor;
    private javax.swing.JLabel jLabelLicense;
    private javax.swing.JSpinner jSpinnerMidiChannel;
    private javax.swing.JSpinner jSpinnerMPENumberOfMemberChannels;
    private javax.swing.JSpinner jSpinnerModulationSources;
    private javax.swing.JSpinner jSpinnerModulationTargets;
    private javax.swing.JSpinner jSpinnerNumPresets;
    private javax.swing.JSpinner jSpinnerPresetEntries;
    private javax.swing.JTextField jTextFieldAttributions;
    private javax.swing.JTextField jTextFieldAuthor;

    @Override
    public JFrame GetFrame() {
        return this;
    }

    @Override
    public boolean AskClose() {
        return false;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public ArrayList<DocumentWindow> GetChildDocuments() {
        return null;
    }
}
