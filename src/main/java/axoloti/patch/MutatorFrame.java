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
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.AbstractAction;
import javax.swing.JComponent;

import axoloti.Patch;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.parameters.ParameterInstance;
import axoloti.utils.KeyUtils;

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
    private final List<PatchVariation> selectedVariationsHistory = new ArrayList<>();

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

        public void setName(String name) {
            this.name = name;
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
        super("Patch Mutator - " + patch.getPatchframe().getTitle());
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
        gbc.gridwidth = 4;
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
                    c.setForeground(Theme.Component_Mid); /* grey out */
                } else {
                    c.setForeground(list.getForeground());
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(parameterList);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        mainPanel.add(scrollPane, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.25;
        gbc.weighty = 0;
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
        
        gbc.gridx = 3;
        JButton button100 = new JButton("100%");
        button100.addActionListener(e -> randomizeSelected(1.00f));
        mainPanel.add(button100, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Stored Variations:"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        variationListModel = new DefaultListModel<>();
        variationList = new JList<>(variationListModel);
        variationList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        variationList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                
                List<PatchVariation> currentSelection = variationList.getSelectedValuesList();

                selectedVariationsHistory.retainAll(currentSelection);

                for (PatchVariation v : currentSelection) {
                    if (!selectedVariationsHistory.contains(v)) {
                        selectedVariationsHistory.add(v);
                    }
                }
                
                /* If more than 2 items are selected, unselect the oldest one */
                if (selectedVariationsHistory.size() > 2) {
                    PatchVariation oldest = selectedVariationsHistory.remove(0);
                    int oldestIndex = variationListModel.indexOf(oldest);
                    if (oldestIndex != -1) {
                        variationList.removeSelectionInterval(oldestIndex, oldestIndex);
                    }
                }
            }
        });
        
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
        JLabel variationsInfoLabel = new JLabel("<html>Select any two stored Variations to constrain the randomization<br>to the range between their parameter values.");
        mainPanel.add(variationsInfoLabel, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.25;
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
        JButton renameButton = new JButton("Rename");
        renameButton.addActionListener(e -> renameVariation());
        mainPanel.add(renameButton, gbc);
        
        gbc.gridx = 3;
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteVariation());
        mainPanel.add(deleteButton, gbc);
        
        add(mainPanel);
        
        setupKeyBindings();

        pack();
        setLocationRelativeTo(null);
    }
    
    /**
     * Helper method to get selected parameters and call the randomizer.
     * Updated to support constrained randomization.
     * @param factor The randomization percentage.
     */
    private void randomizeSelected(float factor) {
        List<ParameterInstance> selectedParameters = parameterList.getSelectedValuesList();
        List<PatchVariation> selectedVariations = variationList.getSelectedValuesList();

        if (selectedParameters.isEmpty()) {
            LOGGER.log(Level.WARNING, "No parameters selected. Please select one or more parameters to randomize.");
            return;
        }

        if (selectedVariations.size() == 2) {
            LOGGER.log(Level.INFO, "Randomizing with constraint from two variations.");
            PatchVariation v1 = selectedVariations.get(0);
            PatchVariation v2 = selectedVariations.get(1);

            Map<ParameterInstance, int[]> constraints = new HashMap<>();

            // Find the min/max values for each parameter based on the two variations
            for (ParameterInstance param : selectedParameters) {
                int val1 = -1;
                int val2 = -1;
                
                // Find the values for the current parameter in both variations
                for (ParameterState state : v1.getStates()) {
                    if (state.getParameter().equals(param)) {
                        val1 = state.getValue();
                        break;
                    }
                }
                for (ParameterState state : v2.getStates()) {
                    if (state.getParameter().equals(param)) {
                        val2 = state.getValue();
                        break;
                    }
                }
                
                if (val1 != -1 && val2 != -1) {
                    int min = Math.min(val1, val2);
                    int max = Math.max(val1, val2);
                    constraints.put(param, new int[]{min, max});
                }
            }
            
            PatchRandomizer.randomizeParametersWithConstraint(selectedParameters, constraints, factor);
            
        } else if (selectedVariations.size() > 2) {
             LOGGER.log(Level.WARNING, "Please select exactly two variations for constrained randomization.");
        } else {
            // Default behavior if not two variations are selected
            LOGGER.log(Level.INFO, "Randomizing " + selectedParameters.size() + " selected parameter(s) by " + (int)(factor * 100) + "%");
            PatchRandomizer.randomizeParameters(selectedParameters, factor);
        }
        
        if (this.patch != null) {
            this.patch.SetDirty(true);
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
            variationListModel.add(0, newVariation);
            LOGGER.log(Level.INFO, "Variation '" + newVariation.name + "' stored.");
        } else {
            LOGGER.log(Level.WARNING, "No active patch to store as a variation.");
        }
    }

    /**
     * Loads the selected variation, replacing the current patch's parameter states.
     */
    private void loadVariation() {
        List<PatchVariation> selectedVariations = variationList.getSelectedValuesList();

        if (selectedVariations.size() != 1) {
            LOGGER.log(Level.WARNING, "Please select exactly one variation to load.");
            return;
        }

        PatchVariation selectedVariation = selectedVariations.get(0);
        
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

    /**
     * Renames the selected variation.
     */
    private void renameVariation() {
        int selectedIndex = variationList.getSelectedIndex();
        if (selectedIndex != -1) {
            PatchVariation selectedVariation = variationListModel.getElementAt(selectedIndex);
            String newName = JOptionPane.showInputDialog(this, "Enter a new name for the variation:", selectedVariation.toString());
            if (newName != null && !newName.trim().isEmpty()) {
                selectedVariation.setName(newName.trim());
                variationListModel.set(selectedIndex, selectedVariation);
                LOGGER.log(Level.INFO, "Variation renamed to '" + newName + "'.");
            }
        } else {
            LOGGER.log(Level.WARNING, "No variation selected to rename.");
        }
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