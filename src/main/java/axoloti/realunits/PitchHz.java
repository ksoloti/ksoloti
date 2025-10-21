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
package axoloti.realunits;

import axoloti.datatypes.Value;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;

/**
 *
 * @author Johannes Taelman
 */
public class PitchHz implements NativeToReal {

    @Override
    public String ToReal(Value v) {
        double hz = 440.0 * Math.pow(2.0, (v.getDouble() + 64 - 69) / 12.0);
        return RealUnitFormatter.formatFrequency(hz);
    }

    @Override
    public String ToRealHighPrecision(Value v) {
        double hz = 440.0 * Math.pow(2.0, (v.getDouble() + 64 - 69) / 12.0);
        return RealUnitFormatter.formatFrequencyHighPrecision(hz);
    }

    @Override
    public double FromReal(String s) throws ParseException {
        /* Improved regex: triggers Hertz input if at least one unit character is used (k, m, or h)
           Handles the following formats (plus optional decimals):
           '1h' -> 1 Hertz
           '1k', '1kh', '1khz' -> 1 KiloHertz
           '1m', '1mh', '1mhz' -> 1 MilliHertz (practically not achievable but maybe for future implementation in LFO) */
        Pattern pattern = Pattern.compile("(?<num>[\\d\\.\\-\\+]+)\\p{Space}*(?<unit>(?:[kKmM][hH]?[zZ]?|[hH][zZ]?))");
        Matcher matcher = pattern.matcher(s);
        if (matcher.matches()) {
            double num, mul = 1.0;

            try {
                num = Double.parseDouble(matcher.group("num")); /* Use Double for higher precision */
            } catch (java.lang.NumberFormatException ex) {
                throw new ParseException("Not PitchHz", 0);
            }

            String units = matcher.group("unit");
            if (units.contains("m") || units.contains("M"))
                mul = 0.001;
            if (units.contains("k") || units.contains("K"))
                mul = 1000;

            double hz = num * mul;
            return ((Math.log(hz / 440.0) / Math.log(2)) * 12.0) - 64 + 69;
        }

        throw new ParseException("Not PitchHz", 0);
    }
}
