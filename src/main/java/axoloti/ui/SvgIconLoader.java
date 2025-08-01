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

package axoloti.ui;

    
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * A utility class to load SVG files from the classpath and convert them into 
 * a Swing Icon. It uses the JSVG library for rendering.
 * 
 * @author Ksoloti
 */
public class SvgIconLoader {

    private static final Logger LOGGER = Logger.getLogger(SvgIconLoader.class.getName());

    /**
     * Loads an SVG file from the specified resource path, resizes it,
     * and returns it as an ImageIcon.
     * @param resourcePath The path to the SVG file (e.g., "/resources/icons/myicon.svg").
     * @param size The desired size (width and height) of the icon in pixels.
     * @return An ImageIcon representing the rendered SVG, or null if loading fails.
     */
    public static Icon load(String resourcePath, int size) {
        try (InputStream in = SvgIconLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                LOGGER.log(Level.SEVERE, "Could not find SVG resource at: " + resourcePath);
                return null;
            }
            
            byte[] svgBytes = in.readAllBytes();
            String base64Svg = Base64.getEncoder().encodeToString(svgBytes);
            String svgData = "data:image/svg+xml;base64," + base64Svg;
            URI tempUri = new URI(svgData);
            URL tempUrl = tempUri.toURL();

            SVGLoader loader = new SVGLoader();
            SVGDocument document = loader.load(tempUrl);

            if (document == null) {
                LOGGER.log(Level.SEVERE, "Failed to parse SVG document from URL: " + tempUrl);
                return null;
            }

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            document.render(null, g2d, new ViewBox(0, 0, size, size));
            
            g2d.dispose();
            
            return new ImageIcon(image);

        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error loading SVG from resource: " + resourcePath, e);
            return null;
        }
    }
}