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

/*
TODO look at these
MEMORY
{
    flash (rx) : org = 0x08000000, len = 896k
    ram : org = 0x20000200, len = 67k
    SRAM2 : org = 0x2001E000, len = 0x00002000
    SRAM3 : org = 0x20020000, len = 0x00010000
    CCMRAMEND : org = 0x1000C000, len = 16k
    sdram : org = 0xC0000000, len = 0x002000000
}
*/ 
MEMORY
{
    FLASH (rx) : org = 0x08000000, len = 896k       /* last 128kB for patch storage*/
    RAM        : org = 0x20000200, len = 0x0000AE00 /* 44k - first 0x200 for vector table */
    SRAM2      : org = 0x2001E000, len = 0x00002000 /* second half (8kB) of SRAM2 for DMA*/
    SRAM3      : org = 0x20020000, len = 0x00010000 /* 64 kB */
    CCMRAMEND  : org = 0x1000C000, len = 10k        /* for stacks */
    SDRAM      : org = 0xC0000000, len = 0x02000000 /* 32MB */
}

__ram_start__   = ORIGIN(RAM);
__ram_size__    = LENGTH(RAM);
__ram_end__     = __ram_start__ + __ram_size__;
__ccram_start__ = ORIGIN(CCMRAMEND);
__ccram_size__ = LENGTH(CCMRAMEND);
__ccram_end__ = __ccram_start__ + __ccram_size__;
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
    } > RAM AT > FLASH
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
    } > RAM

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
