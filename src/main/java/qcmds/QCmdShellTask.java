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
package qcmds;

// import static axoloti.MainFrame.prefs;
// import static axoloti.MainFrame.mainframe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Johannes Taelman
 */
public abstract class QCmdShellTask implements QCmd {

    private static final Logger LOGGER = Logger.getLogger(QCmdShellTask.class.getName());

    abstract String[] GetExec();
    boolean success;

    class StreamHandlerThread implements Runnable {

        InputStream in;
        QCmdProcessor shellProcessor;

        public StreamHandlerThread(QCmdProcessor shellProcessor, InputStream in) {
            this.in = in;
            this.shellProcessor = shellProcessor;
        }

        @Override
        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            try {
                line = br.readLine();
                while (line != null) {
                    if (line.contains("overflowed by")) {
                        LOGGER.log(Level.SEVERE, "{0}\n>>> Patch is too complex to fit in internal RAM. <<<", line);
                    }
                    else if (line.contains("has no member named \'objectinstance__i\'")) {
                        LOGGER.log(Level.SEVERE, "{0}\n>>> A required reference text field in the patch has been left empty. (table, delay read/write, filename, ...) <<<", line);
                    }
                    else if (line.contains("one or more PCH files were found, but they were invalid")) {
                        LOGGER.log(Level.SEVERE, "{0}\n>>> Go to " + LibrariesDir() + File.separator + "build and manually delete all files inside it. <<<", line);
                    }
                    else if (line.contains("error:") || line.contains("#error")) {
                        LOGGER.log(Level.SEVERE, "{0}", line);
                    }
                    else if (line.contains("warning:")) {
                        LOGGER.log(Level.WARNING, "{0}", line);
                    }
                    else if (line.contains("          0 GB")) {
                        /* little modification for --print-memory-usage format */
                        LOGGER.log(Level.INFO, "{0}", line.replaceAll("          0 GB", "           0 B"));
                    }
                    // else if (line.startsWith("Erase   \t[") || line.startsWith("Download\t[")) {
                    //     /* Avoid printing multple lines of progress bars (dfu-util) */
                    //     // BUGGY
                    //     mainframe.consoleRemoveLastLine();
                    //     LOGGER.log(Level.INFO, "{0}", line);
                    // }
                    else {
                        LOGGER.log(Level.INFO, "{0}", line);
                    }
                    line = br.readLine();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean success() {
        return success;
    }
    
    public String HomeDir() {
        return System.getProperty(axoloti.Axoloti.HOME_DIR);
    }

    public String LibrariesDir() {
        return System.getProperty(axoloti.Axoloti.LIBRARIES_DIR);
    }
            
    public String FirmwareDir() {
        String str = System.getProperty(axoloti.Axoloti.FIRMWARE_DIR);
        return str;
    }
    
    public String PlatformDir() {
        String str = System.getProperty(axoloti.Axoloti.PLATFORM_DIR);
        return str;
    }
    
    public String[] GetEnv() {
        ArrayList<String> list = new ArrayList<String>();
        Map<String, String> env = System.getenv();
        for (String v : env.keySet()) {
            list.add((v + "=" + env.get(v)));
        }
        list.add((axoloti.Axoloti.HOME_DIR + "=" + HomeDir()));
        list.add((axoloti.Axoloti.LIBRARIES_DIR + "=" + LibrariesDir()));
        list.add((axoloti.Axoloti.FIRMWARE_DIR + "=" + FirmwareDir()));
        list.add((axoloti.Axoloti.PLATFORM_DIR + "=" + PlatformDir()));

        String vars[] = new String[list.size()];
        list.toArray(vars);
        return vars;
    }

    public File GetWorkingDir() {
        return new File(HomeDir()+ File.separator + "build");
    }

    public QCmd Do(QCmdProcessor shellProcessor) {
        try {
            Process p1;
            p1 = Runtime.getRuntime().exec(GetExec(), GetEnv(), GetWorkingDir());

            Thread thd_out = new Thread(new StreamHandlerThread(shellProcessor, p1.getInputStream()));
            thd_out.start();
            Thread thd_err = new Thread(new StreamHandlerThread(shellProcessor, p1.getErrorStream()));
            thd_err.start();
            p1.waitFor();
            thd_out.join();
            thd_err.join();
            if (p1.exitValue() == 0) {
                success = true;
            } else {
                LOGGER.log(Level.SEVERE, "Shell task failed, exit value: {0}", p1.exitValue());
                success = false;
                return err();
            }
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    abstract QCmd err();
}
