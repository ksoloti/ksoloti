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
package axoloti.realunits;

import axoloti.datatypes.Value;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 *
 * @author Johannes Taelman
 */
public class LFOBPM implements NativeToReal {

    @Override
    public String ToReal(Value v) {
        double hz = 440.0 * Math.pow(2.0, (v.getDouble() + 64 - 69) / 12.0) / 64;
        double bpm = 60.0 * hz;

        /* Special case for zero */
        if (bpm <= 0.0) {
            return "   0 BPM";
        }

        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedBpm = Math.round(bpm * 10000.0) / 10000.0;

        /* Get the number of digits before the decimal point */
        int preDecimalDigits = (int) Math.log10(roundedBpm) + 1;

        if (preDecimalDigits >= 4) { // "99999 BPM" / "9999 BPM"
            return (String.format("%.0f BPM", bpm));
        } else if (preDecimalDigits >= 3) { // "999.9 BPM"
            return (String.format("%.1f BPM", bpm));
        } else if (preDecimalDigits >= 2) { // "99.99 BPM"
            return (String.format("%.2f BPM", bpm));
        } else { // '9.999 BPM"
            return (String.format("%.3f BPM", bpm));
        }
    }

    @Override
    public double FromReal(String s) throws ParseException {
        /* Updated regex to handle "bpm", "bp", or "b" */
        Pattern pattern = Pattern.compile("(?<num>[\\d\\.\\-\\+]+)\\p{Space}*(?<unit>[bB][pP]?[mM]?)");
        Matcher matcher = pattern.matcher(s);

        if (matcher.matches()) {
            double num = 1.0;

            try {
                num = Double.parseDouble(matcher.group("num"));
            } catch (java.lang.NumberFormatException ex) {
                throw new ParseException("Not LFOBPM", 0);
            }

            double hz = num / 60.0;
            return ((Math.log((hz * 64) / 440.0) / Math.log(2)) * 12.0) - 64 + 69;
        }

        throw new ParseException("Not LFOBPM", 0);
    }
}
