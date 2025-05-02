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
package axoloti.utils;

/**
 *
 * @author Johannes Taelman
 */
public class CharEscape {

    static public String charEscape(String s) {
        s = s.replaceAll("_", "__");
        s = s.replaceAll(" ", "_SPACE_");
        s = s.replaceAll("\\*", "_STAR_");
        s = s.replaceAll("/", "_SLASH_");
        s = s.replaceAll("-", "_DASH_");
        s = s.replaceAll("\\+", "_PLUS_");
        s = s.replaceAll("\\~", "_TILDE_");
        s = s.replaceAll("%", "_PCT_");
        s = s.replaceAll("@", "_AT_");
        s = s.replaceAll("!", "_EXCL_");
        s = s.replaceAll("#", "_HASH_");
        s = s.replaceAll("\\$", "_DOLLAR_");
        s = s.replaceAll("&", "_AMP_");
        s = s.replaceAll("\\(", "_BO_");
        s = s.replaceAll("\\)", "_BC_");
        s = s.replaceAll("\\>", "_GT_");
        s = s.replaceAll("\\<", "_LT_");
        s = s.replaceAll("=", "_EQ_");
        s = s.replaceAll(":", "_COLON_");
        s = s.replaceAll("\\.", "_DOT_");
        s = s.replaceAll("\\,", "_COMMA_");    /* Mildly tested! might break patch compilation */
        s = s.replaceAll("\\?", "_QUESTN_");   /* Mildly tested! might break patch compilation */
        s = s.replaceAll("\\;", "_SEMICLN_");  /* Mildly tested! might break patch compilation */
        s = s.replaceAll("\\\'", "_SQUOT_");   /* Mildly tested! might break patch compilation */
        s = s.replaceAll("\\\"", "_DQUOT_");   /* Mildly tested! might break patch compilation */
        return s;
    }
}
