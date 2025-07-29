/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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
package axoloti;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

import axoloti.dialogs.KeyboardNavigableOptionPane;
import axoloti.utils.LinkUtils;

import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

/**
 *
 * @author jtaelman
 */
public class CheckForUpdates {

    private static final Logger LOGGER = Logger.getLogger(CheckForUpdates.class.getName());        

    static public void checkForUpdates() {
        if (Version.AXOLOTI_VERSION.equalsIgnoreCase("(git missing)")) {
            KeyboardNavigableOptionPane.showMessageDialog(null, "No version info found.", "Checking for updates", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            URI uri = new URI("http://www.axoloti.com/updates/" + Version.AXOLOTI_SHORT_VERSION + "-2");
            BufferedReader in = new BufferedReader(new InputStreamReader(uri.toURL().openStream()));
            in.close();

            int result = KeyboardNavigableOptionPane.showOptionDialog(null, "There is an update available", null, DEFAULT_OPTION, INFORMATION_MESSAGE, null,
                    new String[]{"Take me to the website", "Not now..."
                    /*, "Remind me next week", "Never remind me again"*/
                    }, null);
            switch (result) {
                case 0: {
                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(uri);
                        }
                        else {
                            LinkUtils.openLinkUsingSystemBrowser(uri.toString());
                        }
                    }
                    catch (IOException ex) {
                        LinkUtils.openLinkUsingSystemBrowser(uri.toString()); /* Try fallback even after IOException */
                    }
                }
                break;
                default:
            }
        }
        catch (MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        catch (URISyntaxException ex) {
            LOGGER.log(Level.WARNING, "Hyperlink: Invalid update link", ex);
        }
        catch (FileNotFoundException ex) {
            KeyboardNavigableOptionPane.showMessageDialog(null, "No new release available", "Checking for updates", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (UnknownHostException ex) {
            KeyboardNavigableOptionPane.showMessageDialog(null, "Server not reachable", "Checking for updates", JOptionPane.ERROR_MESSAGE);
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
