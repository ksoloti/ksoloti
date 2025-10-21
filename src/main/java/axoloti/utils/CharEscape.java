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
 * @author Johannes Taelman
 */
public class CharEscape {

    public static String charEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '_':
                    sb.append("__");
                    break;
                case ' ':
                    sb.append("_space_");
                    break;
                case '*':
                    sb.append("_star_");
                    break;
                case '/':
                    sb.append("_slash_");
                    break;
                case '-':
                    sb.append("_dash_");
                    break;
                case '+':
                    sb.append("_plus_");
                    break;
                case '~':
                    sb.append("_tilde_");
                    break;
                case '%':
                    sb.append("_pct_");
                    break;
                case '@':
                    sb.append("_at_");
                    break;
                case '!':
                    sb.append("_excl_");
                    break;
                case '#':
                    sb.append("_hash_");
                    break;
                case '$':
                    sb.append("_dollar_");
                    break;
                case '&':
                    sb.append("_amp_");
                    break;
                case '(':
                    sb.append("_bo_");
                    break;
                case ')':
                    sb.append("_bc_");
                    break;
                case '>':
                    sb.append("_gt_");
                    break;
                case '<':
                    sb.append("_lt_");
                    break;
                case '=':
                    sb.append("_eq_");
                    break;
                case ':':
                    sb.append("_colon_");
                    break;
                case '.':
                    sb.append("_dot_");
                    break;
                case ',':
                    sb.append("_comma_");
                    break;
                case '?':
                    sb.append("_questn_");
                    break;
                case ';':
                    sb.append("_semicln_");
                    break;
                case '\'':
                    sb.append("_squot_");
                    break;
                case '\"':
                    sb.append("_dquot_");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
