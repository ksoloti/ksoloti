/**
 * Copyright (C) 2015 Johannes Taelman
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
 */package axoloti.datatypes;

import java.awt.Color;

import axoloti.ui.Theme;

/**
 *
 * @author jtaelman
 */
public class Int8Ptr implements DataType {

    public static final Int8Ptr d = new Int8Ptr();

    @Override
    public boolean IsConvertableToType(DataType dest) {
        return false;
    }

    @Override
    public String CType() {
        return "int8_t*";
    }

    @Override
    public String GenerateConversionToType(DataType dest, String in) {
        throw new IllegalArgumentException("No conversion for " + dest);
    }

    @Override
    public Color GetColor() {
        return Theme.Cable_Int8Pointer;
    }

    @Override
    public Color GetColorHighlighted() {
        return Theme.Cable_Int8Pointer_Highlighted;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Int32Ptr);
    }

    @Override
    public String GenerateCopyCode(String dest, String source) {
        return null;
    }

    @Override
    public boolean HasDefaultValue() {
        return false;
    }

    @Override
    public String GenerateSetDefaultValueCode() {
        return null;
    }

    @Override
    public int hashCode() {
        int hash = 10;
        return hash;
    }

    @Override
    public boolean isPointer() {
        return true;
    }

    @Override
    public String UnconnectedSink() {
        return "";
    }
}
