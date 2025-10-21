/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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
 * NOTE: JOptionPane.showInputDialog() is NOT overridden,
 * so that the default focus stays inside the respective text entry field,
 * not on any of the buttons.
 * 
 * @author Ksoloti
 */
public class KeyboardNavigableOptionPane extends JOptionPane {

    public KeyboardNavigableOptionPane(Object message) {
        super(message);
    }

    public KeyboardNavigableOptionPane(Object message, int messageType) {
        super(message, messageType);
    }

    public KeyboardNavigableOptionPane(Object message, int messageType, int optionType) {
        super(message, messageType, optionType);
    }

    public KeyboardNavigableOptionPane(Object message, int messageType, int optionType, Icon icon) {
        super(message, messageType, optionType, icon);
    }

    public KeyboardNavigableOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options) {
        super(message, messageType, optionType, icon, options);
    }

    public KeyboardNavigableOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options, Object initialValue) {
        super(message, messageType, optionType, icon, options, initialValue);
    }

    /**
     * Overrides the public createDialog method from JOptionPane,
     * matching the signature found in JOptionPane.java source code (JDK at this time: 21.0.7).
     *
     * @param parentComponent determines the frame in which the dialog is displayed
     * @param title the title string for the dialog
     * @return a new JDialog containing this instance
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns true
     */
    @Override
    public JDialog createDialog(Component parentComponent, String title) throws HeadlessException {
        JDialog dialog = super.createDialog(parentComponent, title);

        List<JButton> buttons = findButtonsInComponent(this);
        if (!buttons.isEmpty()) {
            setupButtonFocus(buttons, dialog);
        }

        return dialog;
    }

    /**
     * Recursively finds all JButtons within a given component and its sub-components.
     *
     * @param comp The component to search within.
     * @return A list of found JButton instances.
     */
    private List<JButton> findButtonsInComponent(Component comp) {
        List<JButton> foundButtons = new ArrayList<>();
        if (comp instanceof JButton) {
            foundButtons.add((JButton) comp);
        } else if (comp instanceof Container) {
            Container container = (Container) comp;
            for (Component c : container.getComponents()) {
                foundButtons.addAll(findButtonsInComponent(c));
            }
        }
        return foundButtons;
    }

    /**
     * Sets up initial focus on the default button (or first button) and adds
     * arrow key navigation between buttons.
     *
     * @param buttons The list of JButtons found in the option pane.
     * @param dialog The JDialog containing the option pane.
     */
    private void setupButtonFocus(List<JButton> buttons, JDialog dialog) {
        if (buttons.isEmpty()) {
            return;
        }

        JButton initialFocusButton = null;

        JButton rootPaneDefaultButton = dialog.getRootPane().getDefaultButton();

        if (rootPaneDefaultButton != null && buttons.contains(rootPaneDefaultButton)) {
            initialFocusButton = rootPaneDefaultButton;
        } else {
            if (!buttons.isEmpty()) {
                initialFocusButton = buttons.get(0);
            }
        }

        if (initialFocusButton != null) {
            initialFocusButton.requestFocusInWindow();
        }

        for (int i = 0; i < buttons.size(); i++) {
            JButton button = buttons.get(i);
            int currentIdx = i;

            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "noop");
            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "noop");
            
            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "navigateLeft");
            button.getActionMap().put("navigateLeft", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int nextIdx = (currentIdx - 1 + buttons.size()) % buttons.size();
                    buttons.get(nextIdx).requestFocusInWindow();
                }
            });

            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "navigateRight");
            button.getActionMap().put("navigateRight", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int nextIdx = (currentIdx + 1) % buttons.size();
                    buttons.get(nextIdx).requestFocusInWindow();
                }
            });
        }
    }

    public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
        KeyboardNavigableOptionPane pane = new KeyboardNavigableOptionPane(message, messageType);
        JDialog dialog = pane.createDialog(parentComponent, title); 
        dialog.setModal(true); 
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        dialog.pack(); 
        dialog.setLocationRelativeTo(parentComponent); 
        
        dialog.setVisible(true);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType, int messageType) {
        KeyboardNavigableOptionPane pane = new KeyboardNavigableOptionPane(message, messageType, optionType);
        JDialog dialog = pane.createDialog(parentComponent, title); 
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); 
        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);
        
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (pane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
                     pane.setValue(null); 
                }
            }
        });

        dialog.setVisible(true); 
        
        Object selectedValue = pane.getValue();
        if (selectedValue == null || selectedValue == JOptionPane.UNINITIALIZED_VALUE) {
            return JOptionPane.CLOSED_OPTION; 
        }
        if (selectedValue instanceof Integer) {
            return (Integer) selectedValue;
        }
        return JOptionPane.CLOSED_OPTION; 
    }

    /**
     * Displays an option dialog that allows for keyboard navigation using arrow keys.
     * This method correctly instantiates KeyboardNavigableOptionPane to ensure
     * the overridden createDialog method and custom focus logic are applied.
     *
     * @param parentComponent determines the Frame in which the dialog is displayed;
     * if null, or if the parentComponent has no Frame, a default Frame is used.
     * @param message         the Object to display.
     * @param title           the title string for the dialog.
     * @param optionType      an int designating the options available on the dialog:
     * <code>YES_NO_OPTION</code>, <code>YES_NO_CANCEL_OPTION</code>, or
     * <code>OK_CANCEL_OPTION</code>.
     * @param messageType     an int designating the type of message to be displayed:
     * <code>ERROR_MESSAGE</code>, <code>INFORMATION_MESSAGE</code>,
     * <code>WARNING_MESSAGE</code>, <code>QUESTION_MESSAGE</code>, or
     * <code>PLAIN_MESSAGE</code>.
     * @param icon            the Icon to display in the dialog.
     * @param options         an array of Objects that too can be selected in the dialog.
     * @param initialValue    the Object that is initially selected in the dialog.
     * @return an int indicating the option selected by the user.
     * @throws HeadlessException if <code>GraphicsEnvironment.isHeadless()</code> returns <code>true</code>.
     */
    public static int showOptionDialog(Component parentComponent,
                                       Object message, String title, int optionType, int messageType,
                                       Icon icon, Object[] options, Object initialValue)
                                       throws HeadlessException {

        KeyboardNavigableOptionPane pane = new KeyboardNavigableOptionPane(message, messageType, optionType, icon, options, initialValue);

        JDialog dialog = pane.createDialog(parentComponent, title);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (pane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
                     pane.setValue(null); 
                }
            }
        });

        dialog.setVisible(true);

        Object selectedValue = pane.getValue();
        if (selectedValue == null || selectedValue == JOptionPane.UNINITIALIZED_VALUE) {
            return JOptionPane.CLOSED_OPTION; 
        }
        
        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(selectedValue)) {
                    return i; 
                }
            }
        }
        
        if (selectedValue instanceof Integer) {
            return (Integer) selectedValue;
        }
        
        return JOptionPane.CLOSED_OPTION;
    }
}