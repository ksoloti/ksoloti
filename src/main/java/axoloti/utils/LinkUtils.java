package axoloti.utils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.utils.OSDetect.OS;

public final class LinkUtils {

    private static final Logger LOGGER = Logger.getLogger(LinkUtils.class.getName());
    
    public static void openLinkUsingSystemBrowser(String urlString) {
        OS os = OSDetect.getOS();
        Runtime rt = Runtime.getRuntime();

        try {
            if (os.equals(OS.LINUX)) {
                /* Try xdg-open first (recommended for Linux) */
                if (Runtime.getRuntime().exec(new String[]{"which", "xdg-open"}).getInputStream().read() != -1) {
                    rt.exec(new String[]{"xdg-open", urlString});
                }
                else {
                    /* Fallback for older systems */
                    String[] browsers = {"google-chrome", "firefox", "chromium", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
                    String browserCommand = null;
                    for (String b : browsers) {
                        if (Runtime.getRuntime().exec(new String[]{"which", b}).getInputStream().read() != -1) {
                            browserCommand = b;
                            break;
                        }
                    }
                    if (browserCommand != null) {
                        rt.exec(new String[]{browserCommand, urlString});
                    } else {
                        LOGGER.log(Level.WARNING, "Hyperlink: No suitable browser command found on Linux to open: {0}", urlString);
                    }
                }
            }
            else if (os.equals(OS.MAC)) {
                /* For MacOS, 'open' command */
                rt.exec(new String[]{"open", urlString});
            }
            else if (os.equals(OS.WIN)) {
                /* For Windows, 'start' command */
                rt.exec(new String[]{"cmd", "/c", "start", urlString.replace("&", "^&")}); /* & needs escaping on Windows? */
            }
            else {
                LOGGER.log(Level.WARNING, "Hyperlink: Unsupported operating system for direct link opening: {0}", os);
            }
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "Hyperlink: Failed to open link via external command: " + urlString + ", " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
