/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
package axoloti.dialogs;

import axoloti.Version;
import axoloti.utils.Constants;
import components.ScrollPaneComponent;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 *
 * @author Johannes Taelman
 */
public class ShortcutsFrame extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(ShortcutsFrame.class.getName());

    public static ShortcutsFrame shortcutsFrame = new ShortcutsFrame();

    /**
     * Creates new form ShortcutsFrame
     */
    public ShortcutsFrame() {
        initComponents();
        setIconImage(Constants.APP_ICON.getImage());
        try {
            URL shortcutsHtmlUrl = getClass().getResource("/resources/shortcuts.html");
            if (shortcutsHtmlUrl != null) {
                jTextPaneShortcutsHtml.setPage(shortcutsHtmlUrl);
            } else {
                LOGGER.log(Level.WARNING, "shortcuts.html not found in resources.");
                jTextPaneShortcutsHtml.setText("<html><body><h1>Greetings.</h1>Error: Shortcuts content not found.</body></html>");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load shortcuts.html: " + ex.getMessage());
            ex.printStackTrace(System.out);
            jTextPaneShortcutsHtml.setText("<html><body><h1>Greetings.</h1>Error loading shortcuts.</body></html>");
        }

        jVersionTxt.setText(Version.AXOLOTI_VERSION);
        jDateTxt.setText(Version.AXOLOTI_BUILD_TIME);

        jTextPaneShortcutsHtml.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    System.out.println(hle.getURL());
                    Desktop desktop = Desktop.getDesktop();
                    if (hle.getURL().getProtocol().equals("file")) {
                        // hack for relative paths
                        String path = hle.getURL().getPath();
                        File f = new File(path);
                        try {
                            desktop.browse(f.toURI());
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "Error trying to access hyperlink: " + ex.getMessage());
                            ex.printStackTrace(System.out);
                        }
                    } else {
                        try {
                            desktop.browse(hle.getURL().toURI());
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Error trying to access hyperlink event: " + ex.getMessage());
                            ex.printStackTrace(System.out);
                        }
                    }
                }
            }
        });
    }


    private void initComponents() {

        jScrollPaneShortcutsHtml = new ScrollPaneComponent();
        jTextPaneShortcutsHtml = new javax.swing.JTextPane();
        jLabelBuildVersion = new javax.swing.JLabel();
        jVersionTxt = new javax.swing.JLabel();
        jLabelBuildDate = new javax.swing.JLabel();
        jDateTxt = new javax.swing.JLabel();

        setTitle("Keyboard Shortcuts");
        setMinimumSize(new java.awt.Dimension(320, 180));
        setPreferredSize(new java.awt.Dimension(560, 360));

        jTextPaneShortcutsHtml.setEditable(false);
        jTextPaneShortcutsHtml.setContentType("text/html"); // NOI18N
        jTextPaneShortcutsHtml.setText("");
        jTextPaneShortcutsHtml.setRequestFocusEnabled(false);
        jScrollPaneShortcutsHtml.setViewportView(jTextPaneShortcutsHtml);

        jLabelBuildVersion.setText("Build Version:");
        jVersionTxt.setText("test");
        jLabelBuildDate.setText("Build Date:");
        jDateTxt.setText("test");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneShortcutsHtml, javax.swing.GroupLayout.Alignment.TRAILING)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabelBuildVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jVersionTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE))

                    .addGroup(layout.createSequentialGroup()

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelBuildDate, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        )
                        .addGap(18, 18, 18)

                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jDateTxt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
                    )
                )
            )
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)

            .addGroup(layout.createSequentialGroup()
                .addContainerGap()

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelBuildVersion)
                    .addComponent(jVersionTxt)
                )
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelBuildDate)
                    .addComponent(jDateTxt)
                )
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)

                .addComponent(jScrollPaneShortcutsHtml, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                .addContainerGap()
            )
        );

        pack();
    }

    private javax.swing.JLabel jDateTxt;
    private javax.swing.JLabel jLabelBuildVersion;
    private javax.swing.JLabel jLabelBuildDate;
    private ScrollPaneComponent jScrollPaneShortcutsHtml;
    private javax.swing.JTextPane jTextPaneShortcutsHtml;
    private javax.swing.JLabel jVersionTxt;
}
