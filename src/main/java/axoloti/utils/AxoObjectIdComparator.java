/**
 * Copyright (C) 2023 - 2024 by Ksoloti
 *
 * This file is part of Ksoloti.
 *
 * Ksoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Ksoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Ksoloti. If not, see <http://www.gnu.org/licenses/>.
 */
package axoloti.utils;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import axoloti.object.AxoObjectAbstract;
 
/**
 *
 * @author Ksoloti
 */
public class AxoObjectIdComparator implements Comparator<AxoObjectAbstract> {

    private final String searchTerm;
    private final boolean caseSensitive;
    private final Pattern numericNearEndPattern = Pattern.compile("(\\D*)(\\d+)([^\\d]*)$");

    public AxoObjectIdComparator(String searchTerm, boolean caseSensitive) {
        this.searchTerm = caseSensitive ? searchTerm : searchTerm.toLowerCase();
        this.caseSensitive = caseSensitive;
    }

    private int getPathDepth(String s, String term) {
        String prefix = "";
        String lower_s = caseSensitive ? s : s.toLowerCase();
        String lower_term = caseSensitive ? term : term.toLowerCase();
        int index = lower_s.indexOf(lower_term);
        if (index != -1) {
            prefix = s.substring(0, index);
        } else if ((caseSensitive ? s.endsWith(term) : lower_s.endsWith(lower_term))) {
            prefix = s.substring(0, caseSensitive ? s.length() - term.length() : s.length() - lower_term.length());
        } else {
            prefix = s;
        }
        int count = 0;
        for (int i = 0; i < prefix.length(); i++) {
            if (prefix.charAt(i) == '/') {
                count++;
            }
        }
        return count;
    }

    @Override
    public int compare(AxoObjectAbstract o1, AxoObjectAbstract o2) {

        /* The two object IDs (= path names) to compare */
        String s1 = o1.id;
        String s2 = o2.id;
        String lower_s1 = s1.toLowerCase();
        String lower_s2 = s2.toLowerCase();

        String term = searchTerm;
        String lower_term = searchTerm.toLowerCase();

        String name1 = caseSensitive ? s1.substring(s1.lastIndexOf('/') + 1) : lower_s1.substring(lower_s1.lastIndexOf('/') + 1);
        String name2 = caseSensitive ? s2.substring(s2.lastIndexOf('/') + 1) : lower_s2.substring(lower_s2.lastIndexOf('/') + 1);

        boolean name1Equals = name1.equals(caseSensitive ? term : lower_term);
        boolean name2Equals = name2.equals(caseSensitive ? term : lower_term);

        boolean name1StartsWithWord = (caseSensitive ? name1.matches("\\b" + Pattern.quote(term) + "\\b.*") : name1.matches("\\b" + Pattern.quote(lower_term) + "\\b.*"));
        boolean name2StartsWithWord = (caseSensitive ? name2.matches("\\b" + Pattern.quote(term) + "\\b.*") : name2.matches("\\b" + Pattern.quote(lower_term) + "\\b.*"));

        /* Conditions for a match as well as its relevance (higher relevance = match shown higher up in the list) */
        boolean name1StartsWith = name1.startsWith(caseSensitive ? term : lower_term);
        boolean name2StartsWith = name2.startsWith(caseSensitive ? term : lower_term);

        boolean name1Contains = name1.contains(caseSensitive ? term : lower_term);
        boolean name2Contains = name2.contains(caseSensitive ? term : lower_term);

        boolean s1StartsWith = caseSensitive ? s1.startsWith(term) : lower_s1.startsWith(lower_term);
        boolean s2StartsWith = caseSensitive ? s2.startsWith(term) : lower_s2.startsWith(lower_term);

        boolean s1ContainsAfter = caseSensitive ? s1.matches(".*/" + Pattern.quote(term) + ".*") : lower_s1.matches(".*/" + Pattern.quote(lower_term) + ".*");
        boolean s2ContainsAfter = caseSensitive ? s2.matches(".*/" + Pattern.quote(term) + ".*") : lower_s2.matches(".*/" + Pattern.quote(lower_term) + ".*");

        boolean s1StartsWithOrAfterSlash = s1StartsWith || s1ContainsAfter;
        boolean s2StartsWithOrAfterSlash = s2StartsWith || s2ContainsAfter;

        /* Calculate relevance */
        int relevance1 = 0;
        int relevance2 = 0;

        if      (name1Equals)               relevance1 = 6;
        else if (name1StartsWithWord)       relevance1 = 5;
        else if (name1StartsWith)           relevance1 = 4;
        else if (s1StartsWithOrAfterSlash)  relevance1 = 3;
        else if (name1Contains)             relevance1 = 2;
        else                                relevance1 = 1; /* Lowest priority for other matches */

        if      (name2Equals)               relevance2 = 6;
        else if (name2StartsWithWord)       relevance2 = 5;
        else if (name2StartsWith)           relevance2 = 4;
        else if (s2StartsWithOrAfterSlash)  relevance2 = 3;
        else if (name2Contains)             relevance2 = 2;
        else                                relevance2 = 1;

        int relevanceComparison = Integer.compare(relevance2, relevance1); /* Higher relevance first */
        if (relevanceComparison != 0) return relevanceComparison;

        /* If same relevance, sort by path depth, then leading path */
        int depth1 = getPathDepth(s1, term);
        int depth2 = getPathDepth(s2, term);
        int depthComparison = Integer.compare(depth1, depth2);
        if (depthComparison != 0) return depthComparison;

        /* Compare leading paths */
        String leadingPath1 = "";
        String leadingPath2 = "";
        int index1 = caseSensitive ? s1.indexOf(term) : lower_s1.indexOf(lower_term);
        int index2 = caseSensitive ? s2.indexOf(term) : lower_s2.indexOf(lower_term);

        if (index1 > 0) leadingPath1 = s1.substring(0, index1);
        if (index2 > 0) leadingPath2 = s2.substring(0, index2);

        int leadingPathComparison = leadingPath1.compareTo(leadingPath2);
        if (leadingPathComparison != 0) return leadingPathComparison;

        /* Finally, apply the general alphabetical/numerical sorting logic as a tie-breaker */
        Matcher m1 = numericNearEndPattern.matcher(s1);
        Matcher m2 = numericNearEndPattern.matcher(s2);

        String prefix1WithoutNumber = s1;
        String prefix2WithoutNumber = s2;
        Integer num1 = null;
        Integer num2 = null;
        String suffix1 = "";
        String suffix2 = "";

        if (m1.find()) {
            prefix1WithoutNumber = m1.group(1);
            try {
                num1 = Integer.parseInt(m1.group(2));
            } catch (NumberFormatException ignored) {}
            suffix1 = m1.group(3);
        }

        if (m2.find()) {
            prefix2WithoutNumber = m2.group(1);
            try {
                num2 = Integer.parseInt(m2.group(2));
            } catch (NumberFormatException ignored) {}
            suffix1 = m2.group(3);
        }

        int prefixComparison = prefix1WithoutNumber.compareTo(prefix2WithoutNumber);
        if (prefixComparison != 0) return prefixComparison;

        if (num1 != null && num2 != null) {
            int numberComparison = num1.compareTo(num2);
            if (numberComparison != 0) return numberComparison;
            return suffix1.compareTo(suffix2);
        } else if (num1 != null) return -1;
        else if (num2 != null) return 1;

        return s1.compareTo(s2);
    }
}