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
package components.control;

import axoloti.MainFrame;
import axoloti.ui.Theme;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Johannes Taelman
 */
public class VSliderComponent extends ACtrlComponent {

    private double value;
    private double max;
    private double min;
    private double tick;

    private int MousePressedCoordX = 0;
    private int MousePressedCoordY = 0;
    private int MousePressedBtn = MouseEvent.NOBUTTON;
    private int lastMouseY;

    private static final int height = 128;
    private static final int width = 12;
    private static final Dimension dim = new Dimension(width, height);
    private String keybBuffer = "";
    private Robot robot;

    private PopupFactory popupFactory = PopupFactory.getSharedInstance();
    private static final Stroke strokeThin = new BasicStroke(1);
    private static final Stroke strokeThick = new BasicStroke(2);

    private Popup popup;
    private JToolTip popupTip = createToolTip();

    public VSliderComponent(double value, double min, double max, double tick) {
        this.max = max;
        this.min = min;
        this.value = value;
        this.tick = tick;

        popupTip.setBorder(new EmptyBorder(0,3,0,0));
        popupTip.setPreferredSize(new Dimension(52,20));

        setPreferredSize(dim);
        setMaximumSize(dim);
        setMinimumSize(dim);
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                keybBuffer = "";
            }

            @Override
            public void focusLost(FocusEvent e) {
                keybBuffer = "";
            }
        });
        SetupTransferHandler();
    }

    @Override
    protected void mouseDragged(MouseEvent e) {
        if (isEnabled() && MousePressedBtn == MouseEvent.BUTTON1) {
            double t = tick;
            if (KeyUtils.isControlOrCommandDown(e)) {
                t = t * 0.1;
            }
            if (e.isShiftDown()) {
                t = t * 0.1;
            }
            
            double v = getValue() + t * ((int) Math.round((lastMouseY - e.getYOnScreen())));
            lastMouseY = e.getYOnScreen();
            setValue(v);
            e.consume();
        }
    }

    @Override
    protected void mousePressed(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            if (isEnabled()) {
                robot = createRobot();
                grabFocus();
                MousePressedCoordX = e.getXOnScreen();
                MousePressedCoordY = e.getYOnScreen();
                lastMouseY = MousePressedCoordY;

                int lastBtn = MousePressedBtn;
                MousePressedBtn = e.getButton();

                if (lastBtn == MouseEvent.BUTTON1) {
                    // now have both mouse buttons pressed...
                    getRootPane().setCursor(Cursor.getDefaultCursor());
                }

                popup = popupFactory.getPopup(this, popupTip, MousePressedCoordX+8, MousePressedCoordY);
                popup.show();
                if (MousePressedBtn == MouseEvent.BUTTON1) {
                    if (!Preferences.getInstance().getMouseDoNotRecenterWhenAdjustingControls()
                        && !Preferences.getInstance().getMouseDialAngular()) {
                        JComponent glassPane = (JComponent) getRootPane().getGlassPane();
                        glassPane.setCursor(MainFrame.transparentCursor);
                        glassPane.setVisible(true);
                    }
                    fireEventAdjustmentBegin();
                } else {
                    getRootPane().setCursor(Cursor.getDefaultCursor());
                }
            }
            e.consume();
        }
    }

    @Override
    protected void mouseReleased(MouseEvent e) {
        if (isEnabled() && !e.isPopupTrigger()) {
        
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Thread.sleep(20); /* A tiny delay to let the event queue clear */
                    return null;
                }
                @Override
                protected void done() {
                    if (popup != null) {
                        popup.hide();
                    }
                    if (robot != null) {
                        robot.mouseMove(MousePressedCoordX, MousePressedCoordY);
                        robot = null;
                    }

                    JComponent glassPane = (JComponent) getRootPane().getGlassPane();
                    glassPane.setCursor(Cursor.getDefaultCursor());
                    glassPane.setVisible(false);
                }
            }.execute();
            
            MousePressedBtn = MouseEvent.NOBUTTON;
            fireEventAdjustmentFinished();
            e.consume();
        }
    }
    
    @Override
    public void keyPressed(KeyEvent ke) {
        double steps = tick;
        if (ke.isShiftDown()) {
            steps = steps * 0.1; // mini steps!
            if (KeyUtils.isControlOrCommandDown(ke)) {
                steps = steps * 0.1; // micro steps!                
            }
        } else if (KeyUtils.isControlOrCommandDown(ke)) {
            steps = steps * 10.0; //accelerate!
        }
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_RIGHT:
                fireEventAdjustmentBegin();
                setValue(getValue() + steps);
                ke.consume();
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_LEFT:
                fireEventAdjustmentBegin();
                setValue(getValue() - steps);
                ke.consume();
                break;
            case KeyEvent.VK_PAGE_UP:
                fireEventAdjustmentBegin();
                setValue(getValue() + 5 * steps);
                ke.consume();
                break;
            case KeyEvent.VK_PAGE_DOWN:
                fireEventAdjustmentBegin();
                setValue(getValue() - 5 * steps);
                ke.consume();
                break;
            case KeyEvent.VK_HOME:
                fireEventAdjustmentBegin();
                setValue(max);
                fireEventAdjustmentFinished();
                ke.consume();
                break;
            case KeyEvent.VK_END:
                fireEventAdjustmentBegin();
                setValue(min);
                fireEventAdjustmentFinished();
                ke.consume();
                break;
            case KeyEvent.VK_ENTER:
                fireEventAdjustmentBegin();
                try {
                    setValue(Double.parseDouble(keybBuffer));
                } catch (java.lang.NumberFormatException ex) {
                }
                fireEventAdjustmentFinished();
                keybBuffer = "";
                ke.consume();
                repaint();
                break;
            case KeyEvent.VK_BACK_SPACE:
                if (keybBuffer.length() > 0) {
                    keybBuffer = keybBuffer.substring(0, keybBuffer.length() - 1);
                }
                ke.consume();
                repaint();
                break;
            case KeyEvent.VK_ESCAPE:
                keybBuffer = "";
                ke.consume();
                repaint();
                break;
            default:
        }
        switch (ke.getKeyChar()) {
            case ',': /* Comma is decimal "dot" in some countries - convert to '.' */
                if (!KeyUtils.isControlOrCommandDown(ke)) {
                    keybBuffer += '.';
                    ke.consume();
                }
                break;
            case '-':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '0':
            case '.':
                if (!KeyUtils.isControlOrCommandDown(ke)) {
                    keybBuffer += ke.getKeyChar();
                    ke.consume();
                }
                break;
            default:
        }
    }

    @Override
    void keyReleased(KeyEvent ke) {
        if (isEnabled()) {
            switch (ke.getKeyCode()) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_PAGE_DOWN:
                    fireEventAdjustmentFinished();
                    ke.consume();
                    break;
                default:
            }
        }
    }

    final int margin = 2;

    int ValToPos(double v) {
        return (int) (margin + ((max - v) * (height - 2 * margin)) / (max - min));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (isEnabled()) {
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), height);
            g2.setPaint(getForeground());
            if (isFocusOwner()) {
                g2.setStroke(strokeThick);
            } else {
                g2.setStroke(strokeThin);
            }
            g2.drawRect(0, 0, getWidth(), height);
            int p = ValToPos(value);
            int p1 = ValToPos(0);
            // g2.drawLine(1, p, 1, p1);
            // g2.drawLine(width -1, p, width -1, p1);
            if (p1 - p > 0) {
                g2.fillRect(3, p, width - 5, p1 - p + 1);
            } else {
                g2.fillRect(3, p1, width - 5, p - p1 + 1);
            }

            g2.setStroke(strokeThin);
            g2.drawLine(0, p1, getWidth(), p1);
            //String s = String.format("%5.2f", value);
            //Rectangle2D r = g2.getFontMetrics().getStringBounds(s, g);
            //g2.drawString(s, bwidth+(margin/2)-(int)(0.5 + r.getWidth()/2), getHeight());
        } else {
            g2.setColor(Theme.Object_Default_Background);
            g2.fillRect(0, 0, getWidth(), height);
            g2.setPaint(getForeground());
            g2.setStroke(strokeThin);
            g2.drawRect(0, 0, getWidth(), height);
        }
    }

    @Override
    public void setValue(double value) {
        if (value > max) {
            value = max;
        }
        if (value < min) {
            value = min;
        }
        this.value = value;
        String str = String.format("%3.2f", value);
        setToolTipText(str);
        popupTip.setTipText(str);
        repaint();
        fireEvent();
    }

    @Override
    public double getValue() {
        return value;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMin() {
        return min;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMax() {
        return max;
    }
}
