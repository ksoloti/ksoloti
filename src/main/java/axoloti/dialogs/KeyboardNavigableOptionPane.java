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

package axoloti.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to provide JOptionPane-like static methods
 * for dialogs with enhanced button navigation using Left/Right arrow keys.
 * This aims to be a close mimic of JOptionPane's common methods.
 */
public class KeyboardNavigableOptionPane {

    /**
     * Shows a message dialog.
     * Mimics JOptionPane.showMessageDialog syntax.
     * Keyboard navigation (Left/Right) is not applicable here as there's only one button.
     * The 'OK' button will respond to 'Enter'.
     *
     * @param parentComponent Determines the Frame in which the dialog is displayed.
     * @param message The Object to display.
     * @param title The title string for the dialog.
     * @param messageType An int designating the kind of message this is.
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
        showOptionDialog(parentComponent, message, title, JOptionPane.DEFAULT_OPTION, messageType, null, new String[]{"OK"}, "OK");
    }

    /**
     * Shows an input dialog.
     * Mimics JOptionPane.showInputDialog syntax.
     * The text field will have initial focus. Left/Right arrows navigate within the text field.
     * Tab/Shift+Tab can move focus to buttons. When buttons are focused, Left/Right arrows navigate them.
     *
     * @param parentComponent Determines the Frame in which the dialog is displayed.
     * @param message The Object to display.
     * @return A String containing the input, or null if the user canceled or closed the dialog.
     */
    public static String showInputDialog(Component parentComponent, Object message) {
        return (String) showInputDialog(parentComponent, message, "Input", JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Shows an input dialog with a custom title and message type.
     * Mimics JOptionPane.showInputDialog(parentComponent, message, title, messageType) syntax.
     *
     * @param parentComponent Determines the Frame in which the dialog is displayed.
     * @param message The Object to display.
     * @param title The title string for the dialog.
     * @param messageType An int designating the kind of message this is.
     * @return A String containing the input, or null if the user canceled or closed the dialog.
     */
    public static Object showInputDialog(Component parentComponent, Object message, String title, int messageType) {
        final JDialog dialog = createDialog(parentComponent, title, messageType, null, message);

        final JTextField inputField = new JTextField(25);
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.add(inputField);
        dialog.getContentPane().add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        List<JButton> buttons = new ArrayList<>();
        final String[] result = new String[1];
        result[0] = null;

        String[] optionLabels = {"OK", "Cancel"};
        for (int i = 0; i < optionLabels.length; i++) {
            String label = optionLabels[i];
            JButton button = new JButton(label);
            buttonPanel.add(button);
            buttons.add(button);

            if ("OK".equals(label)) {
                dialog.getRootPane().setDefaultButton(button);
                button.addActionListener(e -> {
                    result[0] = inputField.getText();
                    dialog.dispose();
                });
            } else {
                button.addActionListener(e -> {
                    result[0] = null;
                    dialog.dispose();
                });
            }
        }

        setupButtonNavigation(buttons);

        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parentComponent);

        inputField.requestFocusInWindow();

        dialog.setVisible(true);
        return result[0];
    }

    /**
     * Shows a confirmation dialog with predefined options and Left/Right arrow navigation.
     * Mimics JOptionPane.showConfirmDialog syntax.
     *
     * @param parentComponent Determines the Frame in which the dialog is displayed.
     * @param message The Object to display.
     * @param title The title string for the dialog.
     * @param optionType An int designating the options to display in the dialog.
     * (e.g., JOptionPane.YES_NO_OPTION, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.OK_CANCEL_OPTION)
     * @return An int indicating the option selected by the user, or CLOSED_OPTION if the dialog was closed.
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
        return showOptionDialog(parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE, null, null, null);
    }

    /**
     * Shows a custom option dialog with arbitrary options and Left/Right arrow navigation.
     * Mimics JOptionPane.showOptionDialog syntax.
     *
     * @param parentComponent Determines the Frame in which the dialog is displayed.
     * @param message The Object to display.
     * @param title The title string for the dialog.
     * @param optionType An int designating the options to display in the dialog.
     * (e.g., JOptionPane.DEFAULT_OPTION, JOptionPane.YES_NO_OPTION, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.OK_CANCEL_OPTION)
     * @param messageType An int designating the kind of message this is.
     * @param icon The Icon to display in the dialog.
     * @param options An array of Objects that gives the possible choices the user can make.
     * @param initialValue The Object that represents the default selection for the dialog.
     * @return An int indicating the option selected by the user, or CLOSED_OPTION if the dialog was closed.
     */
    public static int showOptionDialog(Component parentComponent, Object message, String title, int optionType,
                                       int messageType, Icon icon, Object[] options, Object initialValue) {

        final JDialog dialog = createDialog(parentComponent, title, messageType, icon, message);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        List<JButton> buttons = new ArrayList<>();
        final int[] result = new int[1];
        result[0] = JOptionPane.CLOSED_OPTION;

        Object[] actualOptions;
        if (options != null) {
            actualOptions = options;
        } else {
            switch (optionType) {
                case JOptionPane.YES_NO_OPTION:
                    actualOptions = new String[]{"Yes", "No"};
                    break;
                case JOptionPane.YES_NO_CANCEL_OPTION:
                    actualOptions = new String[]{"Yes", "No", "Cancel"};
                    break;
                case JOptionPane.OK_CANCEL_OPTION:
                    actualOptions = new String[]{"OK", "Cancel"};
                    break;
                case JOptionPane.DEFAULT_OPTION:
                    actualOptions = new String[]{"OK"};
                    break;
                default:
                    actualOptions = new String[]{"OK"};
                    break;
            }
        }

        for (int i = 0; i < actualOptions.length; i++) {
            Object option = actualOptions[i];
            JButton button;
            if (option instanceof Icon) {
                button = new JButton((Icon) option);
            } else {
                button = new JButton(option.toString());
            }

            final int optionIndex = i;
            button.addActionListener(e -> {
                result[0] = mapOptionResult(optionType, optionIndex, actualOptions);
                dialog.dispose();
            });
            buttonPanel.add(button);
            buttons.add(button);
        }

        JButton defaultButton = null;
        if (initialValue != null) {

            for (int i = 0; i < actualOptions.length; i++) {
                if (actualOptions[i].equals(initialValue)) {
                    defaultButton = buttons.get(i);
                    break;
                }
            }
        }

        if (defaultButton == null && !buttons.isEmpty()) {
            defaultButton = buttons.get(0);
        }

        final JButton finalDefaultButton = defaultButton;
        if (finalDefaultButton != null) {
            dialog.getRootPane().setDefaultButton(finalDefaultButton);

            SwingUtilities.invokeLater(() -> finalDefaultButton.requestFocusInWindow());
        }

        setupButtonNavigation(buttons);

        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parentComponent);
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Helper to map selected option to JOptionPane constants (e.g., YES_OPTION, NO_OPTION)
     * or custom option index.
     */
    private static int mapOptionResult(int optionType, int selectedIndex, Object[] actualOptions) {
        if (actualOptions == null || selectedIndex < 0 || selectedIndex >= actualOptions.length) {
            return JOptionPane.CLOSED_OPTION;
        }

        Object selectedOption = actualOptions[selectedIndex];

        if (optionType == JOptionPane.YES_NO_OPTION || optionType == JOptionPane.YES_NO_CANCEL_OPTION) {
            if ("Yes".equals(selectedOption)) {
                return JOptionPane.YES_OPTION;
            } else if ("No".equals(selectedOption)) {
                return JOptionPane.NO_OPTION;
            } else if ("Cancel".equals(selectedOption)) {
                return JOptionPane.CANCEL_OPTION;
            }
        } else if (optionType == JOptionPane.OK_CANCEL_OPTION) {
            if ("OK".equals(selectedOption)) {
                return JOptionPane.OK_OPTION;
            } else if ("Cancel".equals(selectedOption)) {
                return JOptionPane.CANCEL_OPTION;
            }
        }
        return selectedIndex;
    }

    /**
     * Creates the basic JDialog structure with message and icon.
     */
    private static JDialog createDialog(Component parentComponent, String title, int messageType, Icon icon, Object message) {
        Frame owner = JOptionPane.getFrameForComponent(parentComponent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        Icon actualIcon = icon;
        if (actualIcon == null) {
            actualIcon = UIManager.getIcon(getUIManagerIconKey(messageType));
        }
        if (actualIcon != null) {
            JLabel iconLabel = new JLabel(actualIcon);
            messagePanel.add(iconLabel, BorderLayout.WEST);
        }

        JComponent messageDisplayComponent;
        if (message instanceof Component) {
            messageDisplayComponent = (JComponent) message;
        } else {

            JTextArea textArea = new JTextArea(message.toString());
            textArea.setEditable(false);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setBackground(new Color(0,0,0,0));
            textArea.setOpaque(false);
            textArea.setFont(UIManager.getFont("Label.font"));

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(null);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setPreferredSize(new Dimension(300, 100));

            messageDisplayComponent = scrollPane;
        }
        messagePanel.add(messageDisplayComponent, BorderLayout.CENTER);
        dialog.getContentPane().add(messagePanel, BorderLayout.NORTH);
        return dialog;
    }

    /**
     * Returns the UIManager key for the default icon based on message type.
     */
    private static String getUIManagerIconKey(int messageType) {
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                return "OptionPane.errorIcon";
            case JOptionPane.INFORMATION_MESSAGE:
                return "OptionPane.informationIcon";
            case JOptionPane.WARNING_MESSAGE:
                return "OptionPane.warningIcon";
            case JOptionPane.QUESTION_MESSAGE:
                return "OptionPane.questionIcon";
            default:
                return "OptionPane.informationIcon";
        }
    }

    /**
     * Configures Left/Right arrow key navigation for a list of buttons.
     */
    private static void setupButtonNavigation(List<JButton> buttons) {
        for (int i = 0; i < buttons.size(); i++) {
            JButton currentButton = buttons.get(i);
            final int currentIndex = i;

            InputMap inputMap = currentButton.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap actionMap = currentButton.getActionMap();

            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "none");
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "none");

            KeyStroke leftArrow = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
            String leftActionKey = "navigateLeft" + currentIndex;
            inputMap.put(leftArrow, leftActionKey);
            actionMap.put(leftActionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int prevIndex = (currentIndex - 1 + buttons.size()) % buttons.size();
                    buttons.get(prevIndex).requestFocusInWindow();
                }
            });

            KeyStroke rightArrow = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
            String rightActionKey = "navigateRight" + currentIndex;
            inputMap.put(rightArrow, rightActionKey);
            actionMap.put(rightActionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int nextIndex = (currentIndex + 1) % buttons.size();
                    buttons.get(nextIndex).requestFocusInWindow();
                }
            });
        }
    }
}