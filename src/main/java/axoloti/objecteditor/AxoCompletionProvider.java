/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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
package axoloti.objecteditor;

import java.util.HashSet;
import java.util.Set;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

/**
 *
 * @author Ksoloti
 */
public final class AxoCompletionProvider extends DefaultCompletionProvider {

    private Set<String> keywords = new HashSet<>();

    public AxoCompletionProvider() {
        keywords.add("uint8_t"); keywords.add("uint16_t"); keywords.add("uint32_t");
        keywords.add("int8_t"); keywords.add("int16_t"); keywords.add("int32_t");
    }

    public void addACKeyword(String w) {
        if (!keywords.contains(w)) {
            keywords.add(w);
            addCompletion(new BasicCompletion(this, w));
        }
    }

    // public void addACKeywords(Set words) {
    //     for (Object w : words) {
    //         if (!keywords.contains(w)) {
    //             keywords.add(w);
    //             addCompletion(new BasicCompletion(this, w.toString()));
    //         }
    //     }
    // }

    // public AxoCompletionProvider get() {
    //     return this;
    // }
}
