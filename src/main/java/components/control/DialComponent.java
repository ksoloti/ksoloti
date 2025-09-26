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
import axoloti.realunits.NativeToReal;
import axoloti.ui.Theme;
import axoloti.utils.Constants;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.SwingWorker;

/**
 *
 * @author Johannes Taelman
 */
public class DialComponent extends ACtrlComponent {

    private double value;
    private double max;
    private double min;
    private double tick;
    private NativeToReal convs[];
    private String keybBuffer = "";
    private Robot robot;
    private int MousePressedCoordX = 0;
    private int MousePressedCoordY = 0;
    private int MouseLastPhysicalY = 0;
    private int MousePressedBtn = MouseEvent.NOBUTTON;

    private double dragAccumulator = 0;
    private static final int DRAG_DEAD_ZONE = 8;
    private static final int DRAG_PIXELS_PER_STEP = 10;
    private static final double FAST_DRAG_ACCELERATION_FACTOR = 3.0;

    private static final Stroke strokeThin = new BasicStroke(1);
    private static final Stroke strokeThick = new BasicStroke(2);
    private static final int layoutTick = 3;

    public DialComponent(double value, double min, double max, double tick) {
        super();
        setInheritsPopupMenu(true);
        this.value = value;
        this.min = min;
        this.max = max;
        this.tick = tick;
        Dimension d = new Dimension(29, 33);
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
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

    public void setNative(NativeToReal convs[]) {
        this.convs = convs;
    }

    @Override
    protected void mouseDragged(MouseEvent e) {
        if (isEnabled() && MousePressedBtn == MouseEvent.BUTTON1) {
            double t = tick;
            if (this.doubleClickSlowDrag) {
                t = 0.01;
            } else {
                if (KeyUtils.isControlOrCommandDown(e)) {
                    t = t * 0.1;
                }
                if (e.isShiftDown()) {
                    t = t * 0.1;
                }
            }

            if (Preferences.getInstance().getMouseDialAngular()) {
                int y = e.getY();
                int x = e.getX();
                int radius = Math.min(getSize().width, getSize().height) / 2 - layoutTick;
                double th = Math.atan2(x - radius, radius - y);
                double v = min + (max - min) * (th + 0.75 * Math.PI) / (1.5 * Math.PI);
                v = Math.round(v / t) * t;
                setValue(v);
            } else {
                int currentPhysicalY = MouseInfo.getPointerInfo().getLocation().y;
                double deltaY = MouseLastPhysicalY - currentPhysicalY;
                MouseLastPhysicalY = currentPhysicalY;

                if (Preferences.getInstance().getMouseDoNotRecenterWhenAdjustingControls()) { /* Touchscreen mode */
                    double change = deltaY * t;
                    change = Math.round(change / t) * t;
                    setValue(getValue() + change);
                } else { /* Regular 'hide cursor and drag' mode */
                    dragAccumulator += deltaY;

                    if (Math.abs(dragAccumulator) > DRAG_DEAD_ZONE) {
                        double change;
                        if (Math.abs(dragAccumulator) < DRAG_DEAD_ZONE + DRAG_PIXELS_PER_STEP * 2) {
                            change = Math.signum(dragAccumulator) * t;
                            dragAccumulator = 0;
                        } else {
                            double excessDrag = Math.abs(dragAccumulator) - DRAG_DEAD_ZONE;
                            int numSteps = (int) (excessDrag / DRAG_PIXELS_PER_STEP );
                            change = Math.signum(dragAccumulator) * numSteps * t * FAST_DRAG_ACCELERATION_FACTOR;
                            dragAccumulator = 0;
                        }
                        change = Math.round(change / t) * t;
                        setValue(getValue() + change);
                    }

                    if (robot != null) {
                        robot.mouseMove(MousePressedCoordX, MousePressedCoordY);
                        MouseLastPhysicalY = MousePressedCoordY;
                    }
                }
            }
            e.consume();
        }
    }

    @Override
    protected void mousePressed(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            if (isEnabled()) {
                robot = createRobot();
                grabFocus();
                MousePressedCoordX = MouseInfo.getPointerInfo().getLocation().x;
                MousePressedCoordY = MouseInfo.getPointerInfo().getLocation().y;
                MouseLastPhysicalY = MousePressedCoordY;
                dragAccumulator = 0;

                int lastBtn = MousePressedBtn;
                MousePressedBtn = e.getButton();

                if (lastBtn == MouseEvent.BUTTON1) {
                    // now have both mouse buttons pressed...
                    getRootPane().setCursor(Cursor.getDefaultCursor());
                }

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
            dragAccumulator = 0;
            this.doubleClickSlowDrag = false;

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Thread.sleep(20); /* A tiny delay to let the event queue clear */
                    return null;
                }

                @Override
                protected void done() {
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
        if (isEnabled()) {
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
                    boolean converted = false;

                    /* Iterate through the converters for this specific dial */
                    if (convs != null) {
                        for (NativeToReal c : convs) {
                            try {
                                setValue(c.FromReal(keybBuffer));
                                converted = true;
                                break; /* Found a matching converter so exit the loop */
                            } catch (ParseException ex) {
                                /* This converter didn't match, try the next one */
                            }
                        }
                    }

                    /* If no converter matched, try to parse as a plain number */
                    if (!converted) {
                        try {
                            setValue(Double.parseDouble(keybBuffer));
                            converted = true;
                        } catch (java.lang.NumberFormatException ex) {
                            /* It's not a valid number either so do nothing */
                        }
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
                        repaint();
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
                case 'b': /* BPM */
                case 'B':
                case 'h': /* Hertz */
                case 'H':
                case 'k': /* Kilo-Hertz */
                case 'K':
                case 'm': /* Milli-Hertz, BP[M] */
                case 'M':
                case 'p': /* B[P]M */
                case 'P':
                case 'q': /* Filter Q */
                case 'Q':
                case 's': /* Seconds */
                case 'S':
                    if (!KeyUtils.isControlOrCommandDown(ke)) {
                        keybBuffer += ke.getKeyChar();
                        ke.consume();
                        repaint();
                    }
                    break;
                default:
            }
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int radius = Math.min(getSize().width, getSize().height) / 2 - layoutTick;
        g2.setPaint(getForeground());
        g2.drawLine(radius, radius, 0, 2 * radius);
        g2.drawLine(radius, radius, 2 * radius, 2 * radius);
        if (isFocusOwner()) {
            g2.setStroke(strokeThick);
        } else {
            g2.setStroke(strokeThin);
        }
        if (isEnabled()) {
            if (this.customBackgroundColor != null) {
                g2.setColor(this.customBackgroundColor);
            }
            else {
                g2.setColor(getBackground());
            }
        }
        else {
            g2.setColor(Theme.Object_Default_Background);
        }
        g2.fillOval(1, 1, radius * 2 - 2, radius * 2 - 2);
        if (isEnabled()) {
            g2.setPaint(getForeground());
        } else {
            g2.setPaint(Theme.Component_Mid);
        }
        g2.drawOval(1, 1, radius * 2 - 2, radius * 2 - 2);
        if (isEnabled()) {
            double th = 0.75 * Math.PI + (value - min) * (1.5 * Math.PI) / (max - min);
            int x = (int) (Math.cos(th) * radius),
                    y = (int) (Math.sin(th) * radius);
            g2.setStroke(strokeThick);
            g2.drawLine(radius + x/3, radius + y/3, radius + (int)(x*.9f), radius + (int)(y*.9f));
            if (keybBuffer.isEmpty()) {
                String s = String.format("%6.2f", value);
                g2.setPaint(getForeground());
                g2.setFont(Constants.FONT);
                g2.drawString(s, 0, getSize().height);
            } else {
                g2.setColor(Theme.Error_Text);
                g2.setFont(Constants.FONT);
                g2.drawString(keybBuffer, 0, getSize().height);
            }
        }
    }

    @Override
    public void setValue(double value) {
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        this.value = value;

        repaint();
        fireEvent();
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    @Override
    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getTick() {
        return tick;
    }

    public void setTick(double tick) {
        this.tick = tick;
    }
}
