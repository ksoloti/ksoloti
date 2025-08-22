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

import axoloti.Patch;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdShowCompileFail implements QCmdGUITask {

    Patch p;

    public QCmdShowCompileFail(Patch p) {
        this.p = p;
    }

    @Override
    public String GetStartMessage() {
        return "Reporting compile failure...";
    }

    @Override
    public String GetDoneMessage() {
        return "Done reporting compile failure.\n";
    }

    @Override
    public void DoGUI(QCmdProcessor processor) {
        processor.clearQueues();
        p.ShowCompileFail();
    }
}
