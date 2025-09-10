package axoloti.patch;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import axoloti.Patch;

/**
 * A GUI frame for the Ksoloti Patch Randomizer.
 * Provides buttons to apply randomization to patch parameters.
 */
public class MutatorFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(MutatorFrame.class.getName());

    private Patch patch;

    /**
     * Constructs a new MutatorFrame.
     * @param patch The patch to be randomized.
     */
    public MutatorFrame(Patch patch) {
        super("Ksoloti Patch Mutator");
        this.patch = patch;
        
        // Set up the frame properties
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(300, 150));
        
        // Create the main panel with GridBagLayout for flexible arrangement
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Add some padding

        // Add a descriptive label
        JLabel titleLabel = new JLabel("Randomize all non-frozen parameters:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(titleLabel, gbc);

        // Add 10% randomization button
        JButton button10 = new JButton("10%");
        button10.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LOGGER.log(Level.INFO, "Randomizing by 10%");
                PatchRandomizer.randomizeAllParameters(patch, 0.10f);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(button10, gbc);

        // Add 25% randomization button
        JButton button25 = new JButton("25%");
        button25.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LOGGER.log(Level.INFO, "Randomizing by 25%");
                PatchRandomizer.randomizeAllParameters(patch, 0.25f);
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 1;
        mainPanel.add(button25, gbc);

        // Add 50% randomization button
        JButton button50 = new JButton("50%");
        button50.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LOGGER.log(Level.INFO, "Randomizing by 50%");
                PatchRandomizer.randomizeAllParameters(patch, 0.50f);
            }
        });
        gbc.gridx = 2;
        gbc.gridy = 1;
        mainPanel.add(button50, gbc);

        // Add the panel to the frame
        add(mainPanel);
        
        // Finalize and show the frame
        pack();
        setLocationRelativeTo(null); // Center the frame on the screen
    }
}
