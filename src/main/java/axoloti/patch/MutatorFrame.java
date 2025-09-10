/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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

package axoloti.patch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import axoloti.Patch;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.parameters.ParameterInstance;

/**
 * A GUI frame for the Ksoloti Patch Mutator.
 * Provides buttons and a parameter list to apply randomization to patch parameters,
 * as well as functionality for saving and loading variations.
 * @author Ksoloti
 */
public class MutatorFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(MutatorFrame.class.getName());

    private Patch patch;
    private JList<ParameterInstance> parameterList;
    private DefaultListModel<PatchVariation> variationListModel;
    private JList<PatchVariation> variationList;
    private int variationCounter = 0;

    private static class PatchVariation {
        private String name;
        private List<ParameterState> states;

        public PatchVariation(String name) {
            this.name = name;
            this.states = new ArrayList<>();
        }

        public void addState(ParameterState state) {
            states.add(state);
        }

        public List<ParameterState> getStates() {
            return states;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class ParameterState {
        private ParameterInstance parameter;
        private int value;

        public ParameterState(ParameterInstance parameter, int value) {
            this.parameter = parameter;
            this.value = value;
        }

        public ParameterInstance getParameter() {
            return parameter;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Constructs a new MutatorFrame.
     * @param patch The patch to be randomized.
     */
    public MutatorFrame(Patch patch) {
        super("Ksoloti Patch Mutator");
        this.patch = patch;
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
        setMinimumSize(new Dimension(500, 500));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Randomize selected parameters:"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        DefaultListModel<ParameterInstance> parameterListModel = new DefaultListModel<>();
        if (patch.objectInstances.size() > 0) {
            for (AxoObjectInstanceAbstract obj : patch.objectInstances) {
                for (ParameterInstance param : obj.getParameterInstances()) {
                    parameterListModel.addElement(param);
                }
            }
        }
        
        parameterList = new JList<>(parameterListModel);
        parameterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        parameterList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isShiftDown() && !e.isControlDown()) {
                    parameterList.clearSelection();
                }
                int index = parameterList.locationToIndex(e.getPoint());
                if (index != -1) {
                    parameterList.addSelectionInterval(index, index);
                }
            }
        });

        parameterList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int index = parameterList.locationToIndex(e.getPoint());
                if (index != -1) {
                    int anchorIndex = parameterList.getAnchorSelectionIndex();
                    if (anchorIndex != -1) {
                        parameterList.setSelectionInterval(anchorIndex, index);
                    }
                }
            }
        });

        parameterList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                ParameterInstance param = (ParameterInstance) value;
                setText(param.GetObjectInstance().getInstanceName() + ":" + param.getName());
                if (param.isFrozen()) {
                    c.setForeground(Color.GRAY);
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(parameterList);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        mainPanel.add(scrollPane, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.33;
        gbc.gridwidth = 1;
        
        gbc.gridx = 0;
        JButton button10 = new JButton("10%");
        button10.addActionListener(e -> randomizeSelected(0.10f));
        mainPanel.add(button10, gbc);

        gbc.gridx = 1;
        JButton button25 = new JButton("25%");
        button25.addActionListener(e -> randomizeSelected(0.25f));
        mainPanel.add(button25, gbc);

        gbc.gridx = 2;
        JButton button50 = new JButton("50%");
        button50.addActionListener(e -> randomizeSelected(0.50f));
        mainPanel.add(button50, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Saved Variations:"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        variationListModel = new DefaultListModel<>();
        variationList = new JList<>(variationListModel);
        variationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        variationList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadVariation();
                }
            }
        });

        JScrollPane variationScrollPane = new JScrollPane(variationList);
        variationScrollPane.setPreferredSize(new Dimension(300, 200));
        mainPanel.add(variationScrollPane, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.33;
        gbc.weighty = 0;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        JButton storeButton = new JButton("Store");
        storeButton.addActionListener(e -> storeVariation());
        mainPanel.add(storeButton, gbc);

        gbc.gridx = 1;
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> loadVariation());
        mainPanel.add(loadButton, gbc);

        gbc.gridx = 2;
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteVariation());
        mainPanel.add(deleteButton, gbc);
        
        add(mainPanel);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    /**
     * Helper method to get selected parameters and call the randomizer.
     * @param percent The randomization percentage.
     */
    private void randomizeSelected(float percent) {
        List<ParameterInstance> selectedParameters = parameterList.getSelectedValuesList();
        if (selectedParameters.isEmpty()) {
            LOGGER.log(Level.WARNING, "No parameters selected. Please select one or more parameters to randomize.");
        } else {
            LOGGER.log(Level.INFO, "Randomizing " + selectedParameters.size() + " selected parameter(s) by " + (int)(percent * 100) + "%");
            PatchRandomizer.randomizeParameters(selectedParameters, percent);
            if (this.patch != null) {
                this.patch.SetDirty(true);
            }
        }
    }

    /**
     * Stores the current patch's parameter states as a new variation.
     */
    private void storeVariation() {
        if (this.patch != null) {
            PatchVariation newVariation = new PatchVariation("Variation " + (++variationCounter));
            for (AxoObjectInstanceAbstract obj : patch.objectInstances) {
                for (ParameterInstance param : obj.getParameterInstances()) {
                    newVariation.addState(new ParameterState(param, param.GetValueRaw()));
                }
            }
            variationListModel.addElement(newVariation);
            LOGGER.log(Level.INFO, "Variation '" + newVariation.name + "' stored.");
        } else {
            LOGGER.log(Level.WARNING, "No active patch to store as a variation.");
        }
    }

    /**
     * Loads the selected variation, replacing the current patch's parameter states.
     */
    private void loadVariation() {
        PatchVariation selectedVariation = variationList.getSelectedValue();
        if (selectedVariation != null && this.patch != null) {
            for (ParameterState state : selectedVariation.getStates()) {
                state.getParameter().SetValueRaw(state.getValue());
                state.getParameter().SetNeedsTransmit(true);
            }
            LOGGER.log(Level.INFO, "Variation '" + selectedVariation.name + "' loaded.");
            this.patch.SetDirty(true);
        } else {
            LOGGER.log(Level.WARNING, "No variation selected to load.");
        }
    }

    /**
     * Deletes the selected variation.
     */
    private void deleteVariation() {
        int selectedIndex = variationList.getSelectedIndex();
        if (selectedIndex != -1) {
            variationListModel.remove(selectedIndex);
            LOGGER.log(Level.INFO, "Selected variation deleted.");
        } else {
            LOGGER.log(Level.WARNING, "No variation selected to delete.");
        }
    }
}
