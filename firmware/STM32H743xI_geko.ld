/*
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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

/*
 * STM32H743 Firmware Memory Setup
 */

__main_stack_size__     = 0x1000; /* 4 kB main sTACK */
__PROCESS_STACK_SIZE__  = 0X1000; /* 4 Kb process stack */

MEMORY
{
    FLASH (rx) : ORIGIN = 0x08000000, LENGTH = 0x00100000 /* Entire 1MB flash bank. Internal patch is saved in second 1MB bank */
    ITCM (rwx) : ORIGIN = 0x00000000, LENGTH = 0x00010000 /* 64kB fast code execution ITCM */
    DTCM       : ORIGIN = 0x20000000, LENGTH = 0x00008000 /* 32kB for Stacks & fast DTCM */
    AXISRAM    : ORIGIN = 0x24000000, LENGTH = 0x00020000 /* 128kB AXI SRAM for FW .data, .bss */
    SRAM2      : ORIGIN = 0x30000000, LENGTH = 0x00020000 /* 128kB D2 SRAM for FW DMA Buffers */
    SRAM4      : ORIGIN = 0x38000000, LENGTH = 0x00010000 /* 64kB D3 SRAM only when FW Debug is defined (.sram3) */
    SDRAM      : ORIGIN = 0xD0000000, LENGTH = 0x02000000 /* 32MB */
}

__ram_start__   = ORIGIN(AXISRAM);
__ram_size__    = LENGTH(AXISRAM);
__ram_end__     = __ram_start__ + __ram_size__;
__dtcm_start__  = ORIGIN(DTCM);
__dtcm_size__   = LENGTH(DTCM);
__dtcm_end__    = __dtcm_start__ + __dtcm_size__;
__sdram_start__ = ORIGIN(SDRAM);
__sdram_size__  = LENGTH(SDRAM);
__sdram_end__   = __sdram_start__ + __sdram_size__;

/*
 * STM32H743 Firmware Memory Setup
 */

__main_stack_size__     = 0x1000; /* 4 kB Main Stack */
__process_stack_size__  = 0x1000; /* 4 kB Process Stack */

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
        PROVIDE(__init_array_base__ = .);
        KEEP(*(SORT(.init_array.*)))
        KEEP(*(.init_array))
        PROVIDE(__init_array_end = .);
        PROVIDE(__init_array_end__ = .);
    } > FLASH

    destructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__fini_array_start = .);
        PROVIDE(__fini_array_base__ = .);
        KEEP(*(.fini_array))
        KEEP(*(SORT(.fini_array.*)))
        PROVIDE(__fini_array_end = .);
        PROVIDE(__fini_array_end__ = .);
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

    .itcm_text : ALIGN(16) SUBALIGN(16) /* Critical firmware code copied to ITCM at boot */
    {
        . = ALIGN(4);
        _itcm_text_start = .;
        *(.itcm_text)
        *(.itcm_text.*)
        *(.ramtext)        /* Re-routing ChibiOS / fast RAM routines here */
        . = ALIGN(4);
        _itcm_text_end = .;
    } > ITCM AT > FLASH

    _itcm_text_load = LOADADDR(.itcm_text);

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

    .samples : ONLY_IF_RO
    {
        . = ALIGN(8);
    } > FLASH


    _etext = .;
    _textdata = _etext;

    .stacks : /* Stacks placed in Fast DTCM */
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
    } > DTCM

    PROVIDE(__core_init = __ram_end__);

    .data : /* Firmware Initialized Data -> AXISRAM */
    {
        . = ALIGN(4);
        PROVIDE(_data = .);
        __textdata_base__ = LOADADDR(.data);
        __data_base__ = .;
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
        __data_end__ = .;
    } > AXISRAM AT > FLASH

    .fram_text :
    {
        . = ALIGN(4);
        PROVIDE(_fram_text = .);
        _fram_text_start_flash = LOADADDR(.fram_text);
        _fram_text_start = .;
        *(.framtext)
        _fram_text_end = .;
    } > SRAM4 AT > FLASH

    .flashend :
    {
        . = ALIGN(4);
        LONG(0x12345678)
        PROVIDE(_flash_end = .);
    } > FLASH

    .bss : /* Firmware Uninitialized Data -> AXISRAM */
    {
        . = ALIGN(4);
        PROVIDE(_bss_start = .);
        __bss_base__ = .;
        *(.bss)
        . = ALIGN(4);
        *(.bss.*)
        . = ALIGN(4);
        *(COMMON)
        . = ALIGN(4);
        __bss_end__ = .;        
        PROVIDE(_bss_end = .);
    } > AXISRAM

    .sram2 (NOLOAD): ALIGN(32) SUBALIGN(32) /* Firmware DMA Buffers (SAI, SD card, ...) -> D2 SRAM Domain. Align 32 because of DCache */
    {
        * (.sram2);
        * (.sram2.*);
    } > SRAM2

    .sram3 (NOLOAD): ALIGN(32) SUBALIGN(32) /* Only used when firmware debug is defined -> D3 SRAM4 (0x38000000) */
    {
        * (.sram3);
        * (.sram3.*);
    } > SRAM4

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

/* Match up ChibiOS multi-RAM layout expectations with actual memory boundaries */
__ram0_free__      = __bss_end__;
__ram0_end__       = __ram_end__;
__ram0_init__      = __textdata_base__;
__ram0_clear__     = __bss_base__;
__ram0_noinit__    = __ram_end__;
__ram0_init_text__ = __textdata_base__;

/* Provide safe NULL/empty defaults for upper slots so the linker ceases fire */
__ram1_init__ = 0; __ram1_clear__ = 0; __ram1_noinit__ = 0; __ram1_init_text__ = 0;
__ram2_init__ = 0; __ram2_clear__ = 0; __ram2_noinit__ = 0; __ram2_init_text__ = 0;
__ram3_init__ = 0; __ram3_clear__ = 0; __ram3_noinit__ = 0; __ram3_init_text__ = 0;
__ram4_init__ = 0; __ram4_clear__ = 0; __ram4_noinit__ = 0; __ram4_init_text__ = 0;
__ram5_init__ = 0; __ram5_clear__ = 0; __ram5_noinit__ = 0; __ram5_init_text__ = 0;
__ram6_init__ = 0; __ram6_clear__ = 0; __ram6_noinit__ = 0; __ram6_init_text__ = 0;
__ram7_init__ = 0; __ram7_clear__ = 0; __ram7_noinit__ = 0; __ram7_init_text__ = 0;