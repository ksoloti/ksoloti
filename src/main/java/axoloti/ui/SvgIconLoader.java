/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

import axoloti.utils.GraphicsUtils;


/**
 * A utility class to load SVG files from the classpath and convert them into 
 * a Swing Icon. It uses the JSVG library for rendering.
 * 
 * @author Ksoloti
 */
public class SvgIconLoader {
    private static final Logger LOGGER = Logger.getLogger(SvgIconLoader.class.getName());

    public static Icon load(String path, int size, Color color) {
        try (InputStream inputStream = SvgIconLoader.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                LOGGER.log(Level.SEVERE, "Error: SVG resource not found at " + path);
                return null;
            }

            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            SVGDocument svgDocument = factory.createSVGDocument(path, inputStream);

            applyColorToSvg(svgDocument.getDocumentElement(), color);

            return new SvgImageIcon(svgDocument, size);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading SVG: " + e.getMessage());
            e.printStackTrace(System.out);
            return null;
        }
    }

    public static Icon load(String path, int size) {
        try (InputStream inputStream = SvgIconLoader.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                LOGGER.log(Level.SEVERE, "Error: SVG resource not found at " + path);
                return null;
            }

            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            SVGDocument svgDocument = factory.createSVGDocument(path, inputStream);

            return new SvgImageIcon(svgDocument, size);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading SVG: " + e.getMessage());
            e.printStackTrace(System.out);
            return null;
        }
    }

    private static void applyColorToSvg(Node node, Color color) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;

            String colorHex = "#" + Integer.toHexString(color.getRGB()).substring(2);

            if (element.hasAttribute("fill") && !element.getAttribute("fill").equalsIgnoreCase("none")) {
                element.setAttribute("fill", colorHex);
            }
            if (element.hasAttribute("stroke") && !element.getAttribute("stroke").equalsIgnoreCase("none")) {
                element.setAttribute("stroke", colorHex);
            }

            if (element.hasAttribute("style")) {
                String style = element.getAttribute("style");
                if (!style.contains("fill:none")) {
                    style = style.replaceAll("fill:#[a-fA-F0-9]{6}|fill:rgb\\(.*\\)", "fill:" + colorHex);
                }
                if (!style.contains("stroke:none")) {
                    style = style.replaceAll("stroke:#[a-fA-F0-9]{6}|stroke:rgb\\(.*\\)", "stroke:" + colorHex);
                }
                element.setAttribute("style", style);
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            applyColorToSvg(children.item(i), color);
        }
    }

    public static Image toBufferedImage(Icon icon) {
        if (icon == null) {
            return null;
        }
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        }
        BufferedImage bufferedImage = new BufferedImage(
                icon.getIconWidth(),
                icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = bufferedImage.getGraphics();
        icon.paintIcon(new Component() {}, g, 0, 0);
        g.dispose();
        return bufferedImage;
    }

    private static class SvgImageIcon implements Icon {
        private final SVGDocument document;
        private final int size;
        private final GraphicsNode graphicsNode;

        public SvgImageIcon(SVGDocument doc, int size) {
            this.document = doc;
            this.size = size;

            UserAgent userAgent = new UserAgentAdapter();
            BridgeContext bridgeContext = new BridgeContext(userAgent);
            GVTBuilder builder = new GVTBuilder();
            this.graphicsNode = builder.build(bridgeContext, doc);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (graphicsNode == null) return;

            Graphics2D g2 = GraphicsUtils.configureGraphics((Graphics2D) g.create());

            String widthAttr = document.getDocumentElement().getAttribute("width");
            String heightAttr = document.getDocumentElement().getAttribute("height");

            float svgWidth = 0;
            float svgHeight = 0;

            try {
                if (widthAttr != null && !widthAttr.isEmpty()) {
                    svgWidth = Float.parseFloat(widthAttr.replaceAll("[^\\d.]", ""));
                }
                if (heightAttr != null && !heightAttr.isEmpty()) {
                    svgHeight = Float.parseFloat(heightAttr.replaceAll("[^\\d.]", ""));
                }
            } catch (NumberFormatException e) {
                System.err.println("Could not parse SVG dimensions. Using default viewbox.");
            }

            if (svgWidth <= 0 || svgHeight <= 0) {
                if (graphicsNode.getPrimitiveBounds() != null) {
                    svgWidth = (float) graphicsNode.getPrimitiveBounds().getWidth();
                    svgHeight = (float) graphicsNode.getPrimitiveBounds().getHeight();
                } else {
                    svgWidth = size;
                    svgHeight = size;
                }
            }

            double scaleX = (double) size / svgWidth;
            double scaleY = (double) size / svgHeight;
            double scale = Math.min(scaleX, scaleY);

            double scaledWidth = svgWidth * scale;
            double scaledHeight = svgHeight * scale;

            int offsetX = (int) Math.round(x + (size - scaledWidth) / 2.0);
            int offsetY = (int) Math.round(y + (size - scaledHeight) / 2.0);

            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);

            graphicsNode.paint(g2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}