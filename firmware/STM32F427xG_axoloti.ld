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
 * ST32F427xG memory setup.
 */

__main_stack_size__     = 0x400;
__process_stack_size__  = 0x400;

MEMORY
{
    FLASH (rx) : ORIGIN = 0x08000000, LENGTH = 896k       /* 896 kB (minus last 128 kB for patch storage) */
    SRAM1      : ORIGIN = 0x20000200, LENGTH = 0x0000AE00 /* 44 kB (minus first 0x200 for vector table) */
    SRAM2      : ORIGIN = 0x2001E000, LENGTH = 0x00002000 /* 8 kB (minus second half (8 kB) for DMA) */
    SRAM3      : ORIGIN = 0x20020000, LENGTH = 0x00010000 /* 64 kB */
    CCMRAMEND  : ORIGIN = 0x1000C800, LENGTH = 0x00003800 /* 14 kB for stacks (minus first 50 kB for patch RAM) */
    SDRAM      : ORIGIN = 0xC0000000, LENGTH = 0x00800000 /* 32 MB */
}

__ram_start__   = ORIGIN(SRAM1);
__ram_size__    = LENGTH(SRAM1);
__ram_end__     = __ram_start__ + __ram_size__;
__ccram_start__ = ORIGIN(CCMRAMEND);
__ccram_size__  = LENGTH(CCMRAMEND);
__ccram_end__   = __ccram_start__ + __ccram_size__;
__sdram_start__ = ORIGIN(SDRAM);
__sdram_size__  = LENGTH(SDRAM);
__sdram_end__   = __sdram_start__ + __sdram_size__;

ENTRY(Reset_Handler)

SECTIONS
{
    . = 0;
    _text = .;

    startup : ALIGN(16) SUBALIGN(16)
    {
        KEEP(*(.vectors))
    } > FLASH

    constructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__init_array_start = .);
        KEEP(*(SORT(.init_array.*)))
        KEEP(*(.init_array))
        PROVIDE(__init_array_end = .);
    } > FLASH

    destructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__fini_array_start = .);
        KEEP(*(.fini_array))
        KEEP(*(SORT(.fini_array.*)))
        PROVIDE(__fini_array_end = .);
    } > FLASH

    .text : ALIGN(16) SUBALIGN(16)
    {
        *(.text.startup.*)
        *(.text)
        *(.text.*)
        *(.rodata)
        *(.rodata.*)
        *(.glue_7t)
        *(.glue_7)
        *(.gcc*)
    } > FLASH

    .ARM.extab :
    {
        *(.ARM.extab* .gnu.linkonce.armextab.*)
    } > FLASH

    .ARM.exidx : {
        PROVIDE(__exidx_start = .);
        *(.ARM.exidx* .gnu.linkonce.armexidx.*)
        PROVIDE(__exidx_end = .);
     } > FLASH

    .eh_frame_hdr :
    {
        *(.eh_frame_hdr)
    } > FLASH

    .eh_frame : ONLY_IF_RO
    {
        *(.eh_frame)
    } > FLASH

    .textalign : ONLY_IF_RO
    {
        . = ALIGN(8);
    } > FLASH

    .samples :  ONLY_IF_RO
    {
        . = ALIGN(8);
    } > FLASH


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
    } > CCMRAMEND

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
    } > SRAM1 AT > FLASH

    .flashend :
    {
        . = ALIGN(4);
        LONG(0x12345678)
        PROVIDE(_flash_end = .);
    } > FLASH

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
    } > SRAM1

    .sram2 (NOLOAD): ALIGN(4) SUBALIGN(4)
    {
        * (.sram2);
        * (.sram2.*);
    } > SRAM2

    .sram3 (NOLOAD): ALIGN(4) SUBALIGN(4)
    {
        * (.sram3);
        * (.sram3.*);
    } > SRAM3

    .ccmramend (NOLOAD): ALIGN(4) SUBALIGN(4)
    {
        * (.ccmramend);
        * (.ccmramend.*);
    } > CCMRAMEND

    .sdram (NOLOAD) : ALIGN(4) SUBALIGN(4)
    {
        KEEP(*(.sdram))
        KEEP(*(.sdram.*))
    } > SDRAM
}

PROVIDE(end = .);
_end = .;

__heap_base__ = _end;
__heap_end__  = __ram_end__;
