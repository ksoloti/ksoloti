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
public class PitchToRatio implements NativeToReal {

    @Override
    public String ToReal(Value v) {
        double ratio = Math.pow(2.0, (v.getDouble()) / 12.0);
        return RealUnitFormatter.formatRatio(ratio);
    }

    @Override
    public String ToRealHighPrecision(Value v) {
        double ratio = Math.pow(2.0, (v.getDouble()) / 12.0);
        return RealUnitFormatter.formatRatioHighPrecision(ratio);
    }

    @Override
    public double FromReal(String s) throws ParseException {
        Pattern pattern = Pattern.compile("(?<unit1>[xX\\*]?)\\p{Space}*(?<num>[\\d\\.\\-\\+]+)\\p{Space}*(?<unit2>[xX\\*]?)");
        Matcher matcher = pattern.matcher(s);
        if (matcher.matches()) {
            double num;

            try {
                num = Double.parseDouble(matcher.group("num"));
            } catch (java.lang.NumberFormatException ex) {
                throw new ParseException("Not PitchToRatio", 0);
            }

            String units1 = matcher.group("unit1");
            String units2 = matcher.group("unit2");
            if (!(units1.toLowerCase().contains("x") || units1.contains("*") || units1.contains("×")
               || units2.toLowerCase().contains("x") || units2.contains("*") || units2.contains("×"))
            )
                throw new ParseException("Not PitchToRatio", 0);

            return (Math.log(num) / Math.log(2)) * 12.0;
        }

        throw new ParseException("Not PitchToRatio", 0);
    }
}
