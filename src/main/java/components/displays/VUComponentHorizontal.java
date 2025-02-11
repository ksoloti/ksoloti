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
package components.displays;

import axoloti.MainFrame;
import axoloti.Theme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class VUComponentHorizontal extends ADispComponent {

    private double value;
    private double accumvalue;
    private double peakaccumvalue;
    private static int w = 64;
    private static int h = 7;
    private static Dimension dim = new Dimension(w+3, h);

    public VUComponentHorizontal() {
        value = 0;
        setMinimumSize(dim);
        setPreferredSize(dim);
        setMaximumSize(dim);
        setSize(dim);
    }

    public VUComponentHorizontal(int width, int height) {
        value = 0;
        w = width;
        h = height;
        dim = new Dimension(width+3, height);
        setMinimumSize(dim);
        setPreferredSize(dim);
        setMaximumSize(dim);
        setSize(dim);
    }

    double decay = 0.5;

    @Override
    public void setValue(double value) {
        this.value = value / 256.0;
        double valuesq = this.value * this.value;
        accumvalue = (accumvalue * decay) + (valuesq * (1.0 - decay));

        double peakdecay = 0.75;
        peakaccumvalue = (peakaccumvalue * peakdecay) + (valuesq * (1.0 - peakdecay));
        // peak
        if (valuesq > peakaccumvalue) {
            peakaccumvalue = valuesq;
        }
        repaint();
    }

    int valueToPos(double v) {
        //TODO lindb: String.format("%.1fdB", maxGain + 20 * Math.log10(Math.abs(v / 64.0)));
        double dB = Math.log10(Math.abs(v) + 0.000000001);
        int i = (int) (-dB * 12);
        
        // Logger.getLogger(MainFrame.class.getName()).log(Level.WARNING, "dB:" + dB + " i:" + i);
        // 12 pixels per 10 dB
        // with w = 44 this is 50dB range
        if (i > w) {
            i = w;
        }
        return w - i;
    }

    static Color CDarkGreen = Theme.VU_Dark_Green;
    static Color CDarkYellow = Theme.VU_Dark_Yellow;
    static Color CDarkRed = Theme.VU_Dark_Red;

    static Color CBrightGreen = Theme.VU_Bright_Green;
    static Color CBrightYellow = Theme.VU_Bright_Yellow;
    static Color CBrightRed = Theme.VU_Bright_Red;

    static int segmentsGreen = w - 20;
    static int segmentsYellow = 12;
    static int segmentsRed = 8;

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setPaint(CDarkGreen);
        g2.fillRect(0, 0 , segmentsGreen, h);

        g2.setPaint(CDarkYellow);
        g2.fillRect(segmentsGreen, 0, segmentsYellow, h);

        g2.setPaint(CDarkRed);
        g2.fillRect(segmentsGreen + segmentsYellow, 0, segmentsRed, h);

        int pa = valueToPos(accumvalue);
        if (pa < segmentsGreen) {
            g2.setPaint(CBrightGreen);
            g2.fillRect(1, 1, pa, h - 2);
        }
        else {
            g2.setPaint(CBrightGreen);
            g2.fillRect(1, 1, segmentsGreen, h - 2);

            if (pa < (segmentsYellow + segmentsGreen)) {
                g2.setPaint(CBrightYellow);
                g2.fillRect(segmentsGreen, 1, pa - segmentsGreen, h - 2);
            }
            else {
                g2.setPaint(CBrightYellow);
                g2.fillRect(segmentsGreen, 1, segmentsYellow, h - 2);
                g2.setPaint(CBrightRed);
                g2.fillRect(segmentsGreen + segmentsYellow, 1, 2 + (pa - segmentsGreen - segmentsYellow) * 3, h - 2);
            }
        }

        int pp = valueToPos(peakaccumvalue);
        if (pp < segmentsGreen) {
            g2.setPaint(CBrightGreen);
            g2.fillRect(pp, 1, 1, h - 2);
        }
        else if (pp < (segmentsGreen + segmentsYellow)) {
            g2.setPaint(CBrightYellow);
            g2.fillRect(pp, 1, 1, h - 2);
        }
        else {
            g2.setPaint(CBrightRed);
            g2.fillRect(segmentsGreen + segmentsYellow + (pp - segmentsGreen - segmentsYellow) * 3, 1, 1, h - 2);
        }
    }

}
