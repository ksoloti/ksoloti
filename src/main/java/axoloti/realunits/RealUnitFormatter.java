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

import java.text.DecimalFormat;

public class RealUnitFormatter {

    private RealUnitFormatter() {}

    public static String formatFrequency(double hz) {
        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedHz = Math.round(hz * 10000.0) / 10000.0;

        if (roundedHz >= 10000.0) {
            return (String.format("%.2f kHz", hz / 1000)); // "12.34 kHz"
        } else if (roundedHz >= 1000.0) {
            return (String.format("%.3f kHz", hz / 1000)); // "1.234 kHz"
        } else if (roundedHz >= 100.0) {
            return (String.format("%.1f Hz", hz)); // "123.4 Hz"
        } else if (roundedHz >= 10.0) {
            return (String.format("%.2f Hz", hz)); // "12.34 Hz"
        } else {
            return (String.format("%.3f Hz", hz)); // "1.234 Hz"
        }
    }

    public static String formatFrequencyHighPrecision(double hz) {
        DecimalFormat df = new DecimalFormat("0.###");
        return df.format(hz) + " hz"; // Always display Hz with up to 3 decimals precision
    }

    public static String formatPeriod(double t) {
        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedT = Math.round(t * 10000.0) / 10000.0;

        if (roundedT >= 100.0) {
            return (String.format("%.1f s", t)); // "123.4 s"
        } else if (roundedT >= 10.0) {
            return (String.format("%.2f s", t)); // "12.34 s"
        } else if (roundedT >= 1.0) {
            return (String.format("%.3f s", t)); // "1.234 s"
        } else {
            /* Less than 1s is displayed in milliseconds */
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

    public static String formatPeriodHighPrecision(double t) {
        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedT = Math.round(t * 10000.0) / 10000.0;

        DecimalFormat df = new DecimalFormat("0.###");
        if (roundedT >= 1.0) {
            return df.format(t) + " s"; // Display seconds with up to 3 decimals precision
        } else {
            /* Less than 1s is displayed in milliseconds */
            double ms = t * 1000;
            return df.format(ms) + " ms"; // Display milliseconds with up to 3 decimals precision
        }
    }

    public static String formatBPM(double bpm) {
        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedBpm = Math.round(bpm * 10000.0) / 10000.0;

        if (roundedBpm <= 0.0) {
            return "    0 BPM"; /* Return padded plain 0 for readability */
        }

        /* Get the number of digits before the decimal point */
        int preDecimalDigits = (int) Math.log10(roundedBpm) + 1;

        if (preDecimalDigits >= 4) {
            return (String.format("%.0f BPM", bpm)); // "12345 BPM", "1234 BPM"
        } else if (preDecimalDigits >= 3) {
            return (String.format("%.1f BPM", bpm)); // "123.4 BPM"
        } else if (preDecimalDigits >= 2) {
            return (String.format("%.2f BPM", bpm)); // "12.34 BPM"
        } else {
            return (String.format("%.3f BPM", bpm)); // "1.234 BPM"
        }
    }

    public static String formatBPMHighPrecision(double bpm) {
        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedBpm = Math.round(bpm * 10000.0) / 10000.0;

        if (roundedBpm <= 0.0) {
            return "0 BPM"; /* Return plain 0 for readability */
        }

        DecimalFormat df = new DecimalFormat("0.###");
        return df.format(bpm) + " BPM"; // Always display with up to 3 decimals precision
    }

    public static String formatQ(double q) {
        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedQ = Math.round(q * 10000.0) / 10000.0;

        if (roundedQ > 99.99) {
            return "Q max";
        }
        
        if (roundedQ >= 10.0) {
            return (String.format("Q %.2f", q)); // "Q 12.34"
        } else {
            return (String.format("Q %.3f", q)); // "Q 1.234"
        }
    }

    /* No formatQHighPrecision necessary */

    public static String formatRatio(double ratio) {
        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedRatio = Math.round(ratio * 10000.0) / 10000.0;

        if (roundedRatio >= 10.0) {
            return (String.format("× %.2f", ratio)); // "× 12.34"
        } else {
            return (String.format("× %.3f", ratio)); // "× 1.234"
        }
    }

    public static String formatRatioHighPrecision(double ratio) {
        DecimalFormat df = new DecimalFormat("0.######");
        return "× " + df.format(ratio); // Always display with up to 6 decimals precision
    }

    public static String formatDb(double db) {
        /* Round to avoid floating point inaccuracies at boundaries */
        double roundedDb = Math.round(db * 10000.0) / 10000.0;

        if (roundedDb <= 0.0) {
            return "-inf dB";
        } else if (roundedDb >= 100.0) {
            return (String.format("%.0f dB", db)); // "120 dB"
        } else if (roundedDb >= 10.0) {
            return (String.format("%.1f dB", db)); // "12.3 dB"
        } else {
            return (String.format("%.2f dB", db)); // "1.23 dB"
        }
    }

    /* No formatDbHighPrecision necessary */
}
