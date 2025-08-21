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
package axoloti.menus;

import axoloti.DocumentWindow;
import axoloti.DocumentWindowList;
import axoloti.MainFrame;
import axoloti.utils.KeyUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
// import java.awt.event.KeyListener;
import java.util.ArrayList;
import javax.swing.KeyStroke;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JSeparator;

/**
 *
 * @author Johannes Taelman
 */
public class WindowMenu extends JMenu {

    public WindowMenu() {
        setDelay(300);
        initComponents();
    }

    private void initComponents() {
        setText("Window");
        setMnemonic('W');
        addMenuListener(new javax.swing.event.MenuListener() {
            @Override
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }

            @Override
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
                jMenuWindowMenuDeselected(evt);
            }

            @Override
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenuWindowMenuSelected(evt);
            }
        });
    }

    private void jMenuWindowMenuSelected(javax.swing.event.MenuEvent evt) {
        PopulateWindowMenu(this);
    }

    private void jMenuWindowMenuDeselected(javax.swing.event.MenuEvent evt) {
        removeAll();
    }

    static class WindowMenuItemActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame frame = ((WindowMenuItem) e.getSource()).getFrame();
            frame.setVisible(true);
            frame.setState(java.awt.Frame.NORMAL);
            frame.toFront();
        }
    }
    static private WindowMenuItemActionListener wmiAL = new WindowMenuItemActionListener();

    static private class WindowMenuItem extends JCheckBoxMenuItem {

        private final JFrame frame;

        public WindowMenuItem(JFrame frame, String itemname) {
            super(itemname);
            this.frame = frame;
            addActionListener(wmiAL);
        }

        // public WindowMenuItem(JFrame frame) {
        //     super(frame.getTitle());
        //     this.frame = frame;
        //     addActionListener(wmiAL);
        // }

        public JFrame getFrame() {
            return frame;
        }
    }

    static void PopulateDocuments(JMenu jMenuWindow, String prefix, ArrayList<DocumentWindow> dwl) {
        for (DocumentWindow p : dwl) {
            JFrame frame = p.GetFrame();
            WindowMenuItem wmi = new WindowMenuItem(frame, prefix + frame.getTitle());
            jMenuWindow.add(wmi);
            if (p.GetChildDocuments()!=null) {
                PopulateDocuments(jMenuWindow, "> " + prefix, p.GetChildDocuments());
            }
        }
    }

    static void PopulateWindowMenu(JMenu jMenuWindow) {
        jMenuWindow.removeAll();
        {
            WindowMenuItem a = new WindowMenuItem(MainFrame.mainframe, "Main Window");
            a.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
            KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
            a.setMnemonic('M');
            jMenuWindow.add(a);
        }
        {
            WindowMenuItem a = new WindowMenuItem(MainFrame.mainframe.getKeyboard(), "Keyboard");
            a.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
            KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
            a.setMnemonic('Y');
            jMenuWindow.add(a);
        }
        {
            WindowMenuItem a = new WindowMenuItem(MainFrame.mainframe.getFilemanager(), "File Manager");
            a.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
            KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
            a.setMnemonic('F');
            jMenuWindow.add(a);
        }

        jMenuWindow.add(new JSeparator());
        PopulateDocuments(jMenuWindow, "", DocumentWindowList.GetList());
    }
}
