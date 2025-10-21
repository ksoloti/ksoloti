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

package axoloti.ui;

import java.awt.Color;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class ColorConverter implements Converter<Color> {
    @Override
    public Color read(InputNode node) throws Exception {
        Integer red = Integer.parseInt(node.getAttributes().get("red").getValue());
        Integer green = Integer.parseInt(node.getAttributes().get("green").getValue());
        Integer blue = Integer.parseInt(node.getAttributes().get("blue").getValue());
        Integer alpha = Integer.parseInt(node.getAttributes().get("alpha").getValue());
        return new Color(red, green, blue, alpha);
    }

    @Override
    public void write(OutputNode node, Color color) {
        Integer red = color.getRed();
        Integer green = color.getGreen();
        Integer blue = color.getBlue();
        Integer alpha = color.getAlpha();

        node.setAttribute("red", red.toString());
        node.setAttribute("green", green.toString());
        node.setAttribute("blue", blue.toString());
        node.setAttribute("alpha", alpha.toString());
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
