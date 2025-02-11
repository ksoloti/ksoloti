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

/*
 * ST32F427xG flasher memory setup.
 */


MEMORY
{
    SRAM (rwx) : ORIGIN = 0x00011000, LENGTH = 0x00009000 
    CCMSRAM (rwx) : ORIGIN = 0x10000000, LENGTH = 0x0000E000 /* CCM - ccmramend */
}

__main_stack_size__     = 0x400;
__process_stack_size__  = 0x400;

__ram_start__   = ORIGIN(SRAM);
__ram_size__    = LENGTH(SRAM);
__ram_end__     = __ram_start__ + __ram_size__;

ENTRY(Reset_Handler)

SECTIONS
{
    . = 0;
    _text = .;
    
    .text : ALIGN(16) SUBALIGN(16)
    {
        *(.text)
        *(.text.startup.*)
        *(.text.*)
        *(.rodata)
        *(.rodata.*)
        *(.glue_7t)
        *(.glue_7)
        *(.gcc*)
        *(.startup)
    } > SRAM

    constructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__init_array_start = .);
        KEEP(*(SORT(.init_array.*)))
        KEEP(*(.init_array))
        PROVIDE(__init_array_end = .);
    } > SRAM

    destructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__fini_array_start = .);
        KEEP(*(.fini_array))
        KEEP(*(SORT(.fini_array.*)))
        PROVIDE(__fini_array_end = .);
    } > SRAM

    .rodata : ALIGN(4) SUBALIGN(4) 
    {
        * (.rodata);
        * (.rodata.*);
    } > SRAM

    .startup : ALIGN(16) SUBALIGN(16)
    {
        KEEP(*(.vectors))
    } > SRAM

    .data : ALIGN(4) SUBALIGN(4)
    {
        sdata = .;
        * (.data);
        edata = .;
    } > SRAM

    _etext = .;
    _textdata = _etext;

    .stacks :
    {
        . = ALIGN(8);
        __main_stack_base__ = .;
        . += __main_stack_size__;
        . = ALIGN(8);
        __main_stack_end__ = .;
        __process_stack_base__ = .;
        __main_thread_stack_base__ = .;
        . += __process_stack_size__;
        . = ALIGN(8);
        __process_stack_end__ = .;
        __main_thread_stack_end__ = .;
    } > CCMSRAM

    .data :
    {
        . = ALIGN(4);
        PROVIDE(_data = .);
	_textdata_start = LOADADDR(.data);
        _data_start = .;
        *(.data)
        . = ALIGN(4);
        *(.data.*)
        . = ALIGN(4);
        *(.ramtext)
        . = ALIGN(4);
        PROVIDE(_edata = .);
	_data_end = .;
    } > SRAM

    .bss :
    {
        . = ALIGN(4);
        PROVIDE(_bss_start = .);
        *(.bss)
        . = ALIGN(4);
        *(.bss.*)
        . = ALIGN(4);
        *(COMMON)
        . = ALIGN(4);
        PROVIDE(_bss_end = .);
    } > SRAM    
 
}

PROVIDE(end = .);
_end = .;

__heap_base__ = _end;
__heap_end__  = __ram_end__;
