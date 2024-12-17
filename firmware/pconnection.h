/*
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
#ifndef __PCONNECTION_H
#define __PCONNECTION_H

typedef  union{
    struct {
      bool dspOverload : 1;
      bool usbBuild    : 1;
      bool usbActive   : 1;
      bool usbUnder    : 1;
      bool usbOver     : 1;
      bool usbError    : 1;
    };
    unsigned int value;
} connectionflags_t;

extern connectionflags_t connectionFlags;

// void USBDMidiPoll(void);
void PExTransmit(void);
void PExReceive(void);
void InitPConnection(void);
extern void BootLoaderInit(void);
void LogTextMessage(const char* format, ...);
int GetFirmwareID(void);

#endif
