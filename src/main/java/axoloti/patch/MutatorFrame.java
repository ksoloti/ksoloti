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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import axoloti.Patch;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.parameters.ParameterInstance;

/**
 * A GUI frame for the Ksoloti Patch Randomizer.
 * Provides buttons and a parameter list to apply randomization to patch parameters.
 * @author Ksoloti
 */
public class MutatorFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(MutatorFrame.class.getName());

    private Patch patch;
    private JList<ParameterInstance> parameterList;

    /**
     * Constructs a new MutatorFrame.
     * @param patch The patch to be randomized.
     */
    public MutatorFrame(Patch patch) {
        super("Ksoloti Patch Mutator");
        this.patch = patch;
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(350, 450));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("Randomize selected parameters:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(titleLabel, gbc);

        JLabel listLabel = new JLabel("Parameters:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(listLabel, gbc);
        listLabel.setVisible(false);
        
        DefaultListModel<ParameterInstance> listModel = new DefaultListModel<>();
        if (patch.objectInstances.size() > 0) {
            for (AxoObjectInstanceAbstract obj : patch.objectInstances) {
                for (ParameterInstance param : obj.getParameterInstances()) {
                    listModel.addElement(param);
                }
            }
        }
        
        parameterList = new JList<>(listModel);
        parameterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
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
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        mainPanel.add(scrollPane, gbc);

        JButton button10 = new JButton("10%");
        button10.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                randomizeSelected(0.10f);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.33;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(button10, gbc);

        JButton button25 = new JButton("25%");
        button25.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                randomizeSelected(0.25f);
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 3;
        mainPanel.add(button25, gbc);

        JButton button50 = new JButton("50%");
        button50.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                randomizeSelected(0.50f);
            }
        });
        gbc.gridx = 2;
        gbc.gridy = 3;
        mainPanel.add(button50, gbc);
        
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
            LOGGER.log(Level.INFO, "Randomizing " + selectedParameters.size() + " selected parameters by " + (percent * 100) + "%");
            PatchRandomizer.randomizeParameters(selectedParameters, percent);
        }
    }
}
