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
import java.text.ParseException;

/**
 *
 * @author Johannes Taelman
 */
public class LFOPeriod implements NativeToReal {

    @Override
    public String ToReal(Value v) {
        double hz = 440.0 * Math.pow(2.0, (v.getDouble() + 64 - 69) / 12.0) / 64;
        double t = 1.0 / hz;

        /* Round the period to a clean precision before performing checks */
        double roundedT = Math.round(t * 10000.0) / 10000.0;

        if (roundedT >= 100.0) {
            return (String.format("%.1f s", t)); // "123.4 s"
        } else if (roundedT >= 10.0) {
            return (String.format("%.2f s", t)); // "12.34 s"
        } else if (roundedT >= 1.0) {
            return (String.format("%.3f s", t)); // "1.234 s"
        } else {
            /* Less than 1s displayed in milliseconds */
            double ms = t * 1000;
            double roundedMs = Math.round(ms * 10000.0) / 10000.0;
            
            if (roundedMs >= 100.0) {
                return (String.format("%.1f ms", ms)); // "123.4 ms"
            } else if (roundedMs >= 10.0) {
                return (String.format("%.2f ms", ms)); // "12.34 ms"
            } else {
                return (String.format("%.3f ms", ms)); // "1.234 ms"
            }
        }
    }

    @Override
    public double FromReal(String s) throws ParseException {
        Pattern pattern = Pattern.compile("(?<num>[\\d\\.\\-\\+]+)\\p{Space}*(?<unit>(?:[sS]|[mM][sS]))");
        Matcher matcher = pattern.matcher(s);

        if (matcher.matches()) {
            double num, mul = 1.0;

            try {
                num = Double.parseDouble(matcher.group("num"));
            } catch (java.lang.NumberFormatException ex) {
                throw new ParseException("Not LFOPeriod", 0);
            }

            String units = matcher.group("unit");
            if (units.contains("m") || units.contains("M")) {
                mul = 0.001;
            }

            double hz = 1.0 / (num * mul);
            return ((Math.log((hz * 64) / 440.0) / Math.log(2)) * 12.0) - 64 + 69;
        }

        throw new ParseException("Not LFOPeriod", 0);
    }
}
