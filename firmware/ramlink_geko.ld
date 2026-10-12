ENTRY(xpatch_init)

MEMORY
{
    DTCM (rw)     : ORIGIN = 0x20008000, LENGTH = 0x00018000 /* CCMSRAM replacement: 96kB fast DTCM (Offset by 32kB) */
    AXISRAM (rwx) : ORIGIN = 0x24020000, LENGTH = 0x00040000 /* SRAM1 replacement: 256kB fast system RAM (Offset by 128kB) */
    SRAM2 (rwx)   : ORIGIN = 0x30020000, LENGTH = 0x00028000 /* DMA-safe RAM: 160kB D2 Domain SRAM (Offset by 128kB) */
    SRAM4 (rwx)   : ORIGIN = 0x38000000, LENGTH = 0x00010000 /* SRAM3 replacement: 64kB D3 SRAM  */
    SDRAM         : ORIGIN = 0xD0000000, LENGTH = 0x02000000 /* 32MB external SDRAM */
}

SECTIONS
{
    /*
    Note: the .boot section _must_ be first in the resulting binary,
    as that is where the firmware calls the patch when loaded.
    */
    .boot : ALIGN(16)
    {
        * (.boot);
    } > AXISRAM

    .text : ALIGN(16) SUBALIGN(16)
    {
        * (.boot);
        * (.text);
        * (.text.*);
        * (.rodata);
        * (.rodata.*);
        etext = .;
    } > AXISRAM

    .bss (NOLOAD) : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(_pbss_start = .);        
        sbss = .;
        * (.bss);
        * (.bss.*);
        *(COMMON)
        ebss = .;
        PROVIDE(_pbss_end = .);        
    } > DTCM

    constructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__ctor_array_start = .);
        KEEP(*(SORT(.init_array.*)))
        KEEP(*(.init_array))
        PROVIDE(__ctor_array_end = .);
    } > AXISRAM

    destructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__dtor_array_start = .);
        KEEP(*(.fini_array))
        KEEP(*(SORT(.fini_array.*)))
        PROVIDE(__dtor_array_end = .);
    } > AXISRAM

    .rodata : ALIGN(4) SUBALIGN(4) 
    {
        * (.rodata);
        * (.rodata.*);
    } > AXISRAM

    .data (NOLOAD) : ALIGN(4) SUBALIGN(4)
    {
        sdata = .;
        * (.data);
        * (.data.*);
        edata = .;
    } > AXISRAM

    .sram2 (NOLOAD) : ALIGN(32) SUBALIGN(32) /* Align to 32-byte DCache boundary */
    {
        * (.sram2);
        * (.sram2.*);
    } > SRAM2

    .sram3 : ALIGN(32) SUBALIGN(32)
    {
        . = ALIGN(32);
        PROVIDE(_sram3_start = .);
        KEEP(* (.sram3));
        KEEP(* (.sram3.*));
        PROVIDE(_sram3_end = .);
    } > AXISRAM

    .sdram (NOLOAD) : ALIGN(4) SUBALIGN(4)
    {
        KEEP(*(.sdram))
        KEEP(*(.sdram.*))
        PROVIDE(_sdram_dyn_start = .);
        PROVIDE(_sdram_dyn_end = ORIGIN(SDRAM) + LENGTH(SDRAM));
    } > SDRAM
}