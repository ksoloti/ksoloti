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
public class LinearTimeReverse implements NativeToReal {

    @Override
    public String ToReal(Value v) {
        double t = (1.0 / v.getDouble()) * (16 / 48000.0) * 8192;
        return RealUnitFormatter.formatPeriod(t);
    }

    @Override
    public String ToRealHighPrecision(Value v) {
        double t = (1.0 / v.getDouble()) * (16 / 48000.0) * 8192;
        return RealUnitFormatter.formatPeriodHighPrecision(t);
    }

    @Override
    public double FromReal(String s) throws ParseException {
        Pattern pattern = Pattern.compile("(?<num>[\\d\\.\\-\\+]+)\\p{Space}*(?<unit>(?:[mM][sS]?|[sS]))");
        Matcher matcher = pattern.matcher(s);

        if (matcher.matches()) {
            double num, mul = 1.0;

            try {
                num = Double.parseDouble(matcher.group("num"));
            } catch (java.lang.NumberFormatException ex) {
                throw new ParseException("Not LinearTimeReverse", 0);
            }

            String units = matcher.group("unit");
            if (units.contains("m") || units.contains("M"))
                mul = 0.001;

            double t = num * mul;
            return 1.0 / (t / ((16 / 48000.0) * 8192));
        }

        throw new ParseException("Not LinearTimeReverse", 0);
    }
}
