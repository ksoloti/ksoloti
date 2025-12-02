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

package axoloti.utils;

/**
 *
 * @author Ksoloti
 */
public class StringUtils {

    public static String wrapStringLines(String str, int wrapLength) {
        String putBackTogetherString = "";

        if (str == null) { return null; }
        else if (str.isEmpty()) { return ""; }

        /* Chop up string and put it back together with line breaks */
        String[] splitStrings = str.split(" ");
        int lineLength = 0;
        for (String s : splitStrings) {
            putBackTogetherString += s + " ";
            lineLength += (s.length()+1);
            if (s.contains("\n")) {
                lineLength = 0; /* Reset line length counter if there is going to be a formatted line break */
            }
            if (lineLength > wrapLength) {
                putBackTogetherString += "\n"; /* Insert line break to make text wrap around */
                lineLength = 0; /* Reset line length counter */
            }
        }
        return putBackTogetherString;
    }
}
