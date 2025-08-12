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

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;


/**
 * A utility class to load SVG files from the classpath and convert them into 
 * a Swing Icon. It uses the JSVG library for rendering.
 * 
 * @author Ksoloti
 */
public class SvgIconLoader {

    private static final Logger LOGGER = Logger.getLogger(SvgIconLoader.class.getName());

    /**
     * Loads an SVG file from the classpath and returns it as an Icon, preserving its original colors.
     *
     * @param path The path to the SVG file, e.g., "/resources/icons/my-icon.svg".
     * @param size The desired size (width and height) of the icon in pixels.
     * @return A Swing Icon object, or null if loading fails.
     */
    public static Icon load(String path, int size) {
        try (InputStream inputStream = SvgIconLoader.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                LOGGER.log(Level.SEVERE, "Error: SVG resource not found at " + path);
                return null;
            }

            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            SVGDocument svgDocument = factory.createSVGDocument(path, inputStream);

            CustomImageTranscoder transcoder = new CustomImageTranscoder();
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) size);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) size);

            TranscoderInput input = new TranscoderInput(svgDocument);
            transcoder.transcode(input, null);
            
            BufferedImage image = transcoder.getBufferedImage();
            return new ImageIcon(image);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading or transcoding SVG: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads an SVG file from the classpath, transcodes it to an Icon, and dynamically
     * applies a color to all of its visible elements.
     *
     * @param path The path to the SVG file, e.g., "/resources/icons/my-icon.svg".
     * @param size The desired size (width and height) of the icon in pixels.
     * @param color The color to apply to the icon's 'fill' and 'stroke' properties.
     * @return A Swing Icon object, or null if loading fails.
     */
    public static Icon load(String path, int size, Color color) {
        try (InputStream inputStream = SvgIconLoader.class.getResourceAsStream(path)) {
            // Check if the resource stream is available
            if (inputStream == null) {
                LOGGER.log(Level.SEVERE, "Error: SVG resource not found at " + path);
                return null;
            }

            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            SVGDocument svgDocument = factory.createSVGDocument(path, inputStream);

            // Get the root element of the SVG document
            Element rootElement = svgDocument.getDocumentElement();

            // Recursively apply the new color to all relevant SVG elements
            applyColorToSvg(rootElement, color);

            // Create a custom transcoder to convert the SVG to a BufferedImage
            CustomImageTranscoder transcoder = new CustomImageTranscoder();
            
            // Set the desired size for the transcoded image
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) size);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) size);

            // Transcode the SVG document
            TranscoderInput input = new TranscoderInput(svgDocument);
            transcoder.transcode(input, null);
            
            // Get the transcoded image and return it as an Icon
            BufferedImage image = transcoder.getBufferedImage();
            return new ImageIcon(image);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading or transcoding SVG: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Image loadIcon(String svgPath, String pngPath, int size, Color color) {
        Icon svgIcon = SvgIconLoader.load(svgPath, size, color);
        if (svgIcon instanceof ImageIcon) {
            return ((ImageIcon) svgIcon).getImage();
        }
        return new ImageIcon(getClass().getResource(pngPath)).getImage();
    }

    /**
     * Helper method to recursively traverse the SVG DOM and change the color of elements.
     * This method looks for a 'fill' or 'stroke' attribute and sets it to the specified color,
     * but only if the existing attribute is not set to "none".
     *
     * @param node The current node in the DOM tree.
     * @param color The color to apply.
     */
    private static void applyColorToSvg(Node node, Color color) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;

            // Convert the color to a hex string format, e.g., "#FF0000"
            String colorHex = "#" + Integer.toHexString(color.getRGB()).substring(2);

            // Apply color to the fill property ONLY IF it's not set to "none"
            if (element.hasAttribute("fill") && !element.getAttribute("fill").equalsIgnoreCase("none")) {
                element.setAttribute("fill", colorHex);
            }
            // Apply color to the stroke property ONLY IF it's not set to "none"
            if (element.hasAttribute("stroke") && !element.getAttribute("stroke").equalsIgnoreCase("none")) {
                element.setAttribute("stroke", colorHex);
            }
            
            // Also handle the 'style' attribute carefully
            if (element.hasAttribute("style")) {
                String style = element.getAttribute("style");
                // Check if the style contains a fill that is not 'none' before replacing
                if (!style.contains("fill:none")) {
                    style = style.replaceAll("fill:#[a-fA-F0-9]{6}|fill:rgb\\(.*\\)", "fill:" + colorHex);
                }
                if (!style.contains("stroke:none")) {
                    style = style.replaceAll("stroke:#[a-fA-F0-9]{6}|stroke:rgb\\(.*\\)", "stroke:" + colorHex);
                }
                element.setAttribute("style", style);
            }
        }

        // Recursively call for all child nodes
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            applyColorToSvg(children.item(i), color);
        }
    }

    /**
     * A simple, named class to extend the abstract ImageTranscoder and allow us
     * to capture the transcoded BufferedImage.
     */
    private static class CustomImageTranscoder extends ImageTranscoder {
        private BufferedImage image;

        @Override
        public BufferedImage createImage(int w, int h) {
            this.image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            return this.image;
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            // Do nothing, we will use the internal image buffer
        }

        public BufferedImage getBufferedImage() {
            return image;
        }
    }
}