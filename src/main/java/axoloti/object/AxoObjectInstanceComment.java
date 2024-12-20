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
package axoloti.object;

import axoloti.Patch;
import axoloti.PatchGUI;
import axoloti.utils.Constants;
import components.TextPaneComponent;
import components.TextFieldComponent;

import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BoxLayout;
import javax.swing.UIManager;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "comment")
public class AxoObjectInstanceComment extends AxoObjectInstanceAbstract {

    TextPaneComponent InstanceTextPane;

    @Attribute(name = "text", required = false)
    private String commentText;

    public AxoObjectInstanceComment() {
        if (InstanceName != null) {
            commentText = InstanceName;
            InstanceName = null;
        }
    }

    public AxoObjectInstanceComment(AxoObjectAbstract type, Patch patch1, String InstanceName1, Point location) {
        super(type, patch1, InstanceName1, location);
        if (InstanceName != null) {
            commentText = InstanceName;
            InstanceName = null;
        }
    }

    @Override
    public boolean IsLocked() {
        return false;
    }

    @Override
    public void PostConstructor() {
        super.PostConstructor();

        if (InstanceName != null) {
            commentText = InstanceName;
            InstanceName = null;
        }

        setOpaque(true);
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        InstanceTextPane = new TextPaneComponent();

        InstanceTextPane.setAlignmentX(CENTER_ALIGNMENT);
        InstanceTextPane.setAlignmentY(CENTER_ALIGNMENT);
        InstanceTextPane.setMargin(new Insets(-3, 5, -1, 5));

        InstanceTextPane.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (patch != null) {
                    if (!me.isShiftDown() && me.getClickCount() == 2) {
                        addInstanceNameEditor();
                    }
                    // if (me.getClickCount() == 1) {
                        // if (me.isShiftDown()) {
                        //    SetSelected(!isSelected());
                        // }
                        // else if (Selected == false) {
                        //    ((PatchGUI) patch).SelectNone();
                        //    SetSelected(true);
                        // }
                    // }
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {
                AxoObjectInstanceComment.this.mousePressed(me);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                AxoObjectInstanceComment.this.mouseReleased(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        InstanceTextPane.addMouseMotionListener(this);

        setInstanceName(commentText);

        add(InstanceTextPane);
        
        setLocation(x, y);
        resizeToGrid();
    }

    @Override
    public void addInstanceNameEditor() {
        InstanceNameTF = new TextFieldComponent(commentText);
        InstanceNameTF.setMargin(new Insets(-3, 5, -1, 5));
        InstanceNameTF.selectAll();
        InstanceNameTF.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                String s = InstanceNameTF.getText();
                setInstanceName(s);
                getParent().remove(InstanceNameTF);
                getParent().repaint();
            }

            @Override
            public void focusGained(FocusEvent e) {
            }
        });
        InstanceNameTF.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent ke) {
            }

            @Override
            public void keyReleased(KeyEvent ke) {
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    String s = InstanceNameTF.getText();
                    setInstanceName(s);
                    getParent().remove(InstanceNameTF);
                    getParent().repaint();
                }
            }
        });
        getParent().add(InstanceNameTF, 0);
        InstanceNameTF.setLocation(getLocation().x, getLocation().y + InstanceTextPane.getLocation().y);
        InstanceNameTF.setSize(getWidth(), 14);
        InstanceNameTF.setVisible(true);
        InstanceNameTF.requestFocus();
    }

    @Override
    public void setInstanceName(String s) {
        if (!s.equals(commentText))
            patch.SetDirty();

        this.commentText = s;

        if (InstanceTextPane != null) {
            if (commentText.toLowerCase().contains(("<html>"))) {
                InstanceTextPane.setContentType("text/html");
                InstanceTextPane.setFont(UIManager.getFont("defaultFont"));
            }
            else {
                InstanceTextPane.setContentType("text/plain");
                InstanceTextPane.setFont(Constants.FONT);
            }
            InstanceTextPane.setText(commentText);
            InstanceTextPane.setSize((int)InstanceTextPane.getPreferredSize().getWidth(), InstanceTextPane.getHeight());
        }
        revalidate();
        if (getParent() != null) {
            getParent().repaint();
        }
        resizeToGrid();
    }

    @Override
    public String getCInstanceName() {
        return "";
    }
}