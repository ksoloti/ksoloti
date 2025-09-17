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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.googlecode.jfilechooserbookmarks.DefaultBookmarksPanel;

public class AxoBookmarksPanel extends DefaultBookmarksPanel {

    private static final long serialVersionUID = 5409354653351018163L;

    @Override
protected void initGUI() {
        super.initGUI();

        ImageIcon newAddIcon = loadImageIcon("/resources/icons/AxoBookmarksPanel/add.gif");
        ImageIcon newRemoveIcon = loadImageIcon("/resources/icons/AxoBookmarksPanel/remove.gif");
        ImageIcon newMoveUpIcon = loadImageIcon("/resources/icons/AxoBookmarksPanel/arrow_up.gif");
        ImageIcon newMoveDownIcon = loadImageIcon("/resources/icons/AxoBookmarksPanel/arrow_down.gif");
        ImageIcon newCopyIcon = loadImageIcon("/resources/icons/AxoBookmarksPanel/copy.gif");
        ImageIcon newPasteIcon = loadImageIcon("/resources/icons/AxoBookmarksPanel/paste.gif");

        if (newAddIcon != null) m_ButtonAdd.setIcon(newAddIcon);
        if (newRemoveIcon != null) m_ButtonRemove.setIcon(newRemoveIcon);
        if (newMoveUpIcon != null) m_ButtonMoveUp.setIcon(newMoveUpIcon);
        if (newMoveDownIcon != null) m_ButtonMoveDown.setIcon(newMoveDownIcon);
        if (newCopyIcon != null) m_ButtonCopy.setIcon(newCopyIcon);
        if (newPasteIcon != null) m_ButtonPaste.setIcon(newPasteIcon);

        TitledBorder bookmarksTitle = BorderFactory.createTitledBorder("Bookmarks");

        this.setBorder(bookmarksTitle);

        this.remove(m_PanelButtons);

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 0));
        buttonsPanel.setBorder(new EmptyBorder(1, 1, 1, 1));

        buttonsPanel.add(m_ButtonAdd);
        buttonsPanel.add(m_ButtonRemove);
        buttonsPanel.add(m_ButtonMoveUp);
        buttonsPanel.add(m_ButtonMoveDown);
        buttonsPanel.add(m_ButtonCopy);
        buttonsPanel.add(m_ButtonPaste);

        this.add(buttonsPanel, BorderLayout.SOUTH);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(141, super.getPreferredSize().height);
    }

    private ImageIcon loadImageIcon(String path) {
        URL url = getClass().getResource(path);
        if (url != null) {
            return new ImageIcon(url);
        } else {
            System.err.println("Could not find icon: " + path);
            return null;
        }
    }
}