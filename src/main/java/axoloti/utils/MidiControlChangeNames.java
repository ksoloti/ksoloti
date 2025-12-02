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
public class MidiControlChangeNames {

    static String[] ccnames = {
        "Bank select",          //   0
        "Modulation",           //   1
        "Breath Controller",    //   2
        "",                     //   3
        "Foot Controller",      //   4
        "Portamento Time",      //   5
        "Data Entry MSB",       //   6
        "Channel Volume",       //   7
        "Balance",              //   8
        "",                     //   9
        "Pan",                  //  10
        "Expression",           //  11
        "FX Control 1",         //  12
        "FX Control 2",         //  13
        "",                     //  14
        "",                     //  15
        "General Purpose 1",    //  16
        "General Purpose 2",    //  17
        "General Purpose 3",    //  18
        "General Purpose 4",    //  19
        "",                     //  20
        "",                     //  21
        "",                     //  22
        "",                     //  23
        "",                     //  24
        "",                     //  25
        "",                     //  26
        "",                     //  27
        "",                     //  28
        "",                     //  29
        "",                     //  30
        "",                     //  31
        "",                     //  32
        "",                     //  33
        "",                     //  34
        "",                     //  35
        "",                     //  36
        "",                     //  37
        "",                     //  38
        "",                     //  39
        "",                     //  40
        "",                     //  41
        "",                     //  42
        "",                     //  43
        "",                     //  44
        "",                     //  45
        "",                     //  46
        "",                     //  47
        "",                     //  48
        "",                     //  49
        "",                     //  50
        "",                     //  51
        "",                     //  52
        "",                     //  53
        "",                     //  54
        "",                     //  55
        "",                     //  56
        "",                     //  57
        "",                     //  58
        "",                     //  59
        "",                     //  60
        "",                     //  61
        "",                     //  62
        "",                     //  63
        "Sustain",              //  64
        "Portamento",           //  65
        "Sostenuto",            //  66
        "Soft Pedal",           //  67
        "Legato",               //  68
        "Hold 2",               //  69
        "Sound Controller 1",   //  70
        "Sound Controller 2",   //  71
        "Sound Controller 3",   //  72
        "Sound Controller 4",   //  73
        "Sound Controller 5",   //  74
        "Sound Controller 6",   //  75
        "Sound Controller 7",   //  76
        "Sound Controller 8",   //  77
        "Sound Controller 9",   //  78
        "Sound Controller 10",  //  79
        "Switch 1",             //  80
        "Switch 2",             //  81
        "Switch 3",             //  82
        "Switch 4",             //  83
        "Portamento Amount",    //  84
        "",                     //  85
        "",                     //  86
        "",                     //  87
        "",                     //  88
        "",                     //  89
        "",                     //  90
        "Effect 1 Depth",       //  91
        "Effect 2 Depth",       //  92
        "Effect 3 Depth",       //  93
        "Effect 4 Depth",       //  94
        "Effect 5 Depth",       //  95
        "(+1) Data Inc",        //  96
        "(-1) Data Dec",        //  97
        "NRPN LSB",             //  98
        "NRPN MSB",             //  99
        "RPN LSB",              // 100
        "RPN MSB",              // 101
        "",                     // 102
        "",                     // 103
        "",                     // 104
        "",                     // 105
        "",                     // 106
        "",                     // 107
        "",                     // 108
        "",                     // 109
        "",                     // 110
        "",                     // 111
        "",                     // 112
        "",                     // 113
        "",                     // 114
        "",                     // 115
        "",                     // 116
        "",                     // 117
        "",                     // 118
        "",                     // 119
        "All Sound Off",        // 120
        "Reset Controllers",    // 121
        "Local On/Off",         // 122
        "All Notes Off",        // 123
        "Omni Mode Off",        // 124
        "Omni Mode On",         // 125
        "Mono Mode",            // 126
        "Poly Mode"             // 127
    };

    public static String GetNameFromCC(int cc) {
        if ((cc > 0) && (cc < 128)) {
            return ccnames[cc];
        }
        else {
            return "";
        }
    }
}
