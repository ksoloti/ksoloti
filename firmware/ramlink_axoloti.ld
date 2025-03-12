ENTRY(xpatch_init)

MEMORY
{
    /*
    originally 
        24 kB for program
        64 kB for variables
    new:
        44+8 = 52 kB for program
        64 kB for variables 
    next:
        44+8+16 = 68 kB for program?? (sram3?)
    */

    CCMSRAM (rw) : ORIGIN = 0x10000000, LENGTH = 0x0000C800 /* CCM - ccmramend */
    SRAM1 (rwx)  : ORIGIN = 0x00011000, LENGTH = 0x0000B000 /* 44kB in RAM1 */
    SRAM2 (rwx)  : ORIGIN = 0x2001C000, LENGTH = 0x00002000 /* first 8kB in SRAM2 */
    SRAM3 (rwx)  : ORIGIN = 0x20020000, LENGTH = 0x00010000 /* 64kB */
    SDRAM        : ORIGIN = 0xC0000000, LENGTH = 0x00800000 /* 8 MB */
}

SECTIONS
{

    .text : ALIGN(16) SUBALIGN(16)
    {
    /*
    Note: the .boot section _must_ be first in the resulting binary,
    as that is where the firmware calls the patch when loaded.
    */
        * (.boot);
        * (.text);
        etext = .;
    } > SRAM1
    
    .bss (NOLOAD) : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(_pbss_start = .);        
        sbss = .;
        * (.bss);
        ebss = .;
        PROVIDE(_pbss_end = .);        
    } > CCMSRAM
    
    constructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__ctor_array_start = .);
        KEEP(*(SORT(.init_array.*)))
        KEEP(*(.init_array))
        PROVIDE(__ctor_array_end = .);
    } > SRAM1

    destructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__dtor_array_start = .);
        KEEP(*(.fini_array))
        KEEP(*(SORT(.fini_array.*)))
        PROVIDE(__dtor_array_end = .);
    } > SRAM1

    .rodata : ALIGN(4) SUBALIGN(4) 
    {
        * (.rodata);
        * (.rodata.*);
    } > SRAM1

    .data (NOLOAD) : ALIGN(4) SUBALIGN(4)
    {
        sdata = .;
        * (.data);
        edata = .;
    } > SRAM1

    .sram2 (NOLOAD) : ALIGN(4) SUBALIGN(4) 
    {
        * (.sram2);
        * (.sram2.*);
    } > SRAM2

    .sram3 (NOLOAD) : ALIGN(4) SUBALIGN(4) 
    {
        * (.sram3);
        * (.sram3.*);
    } > SRAM3
   
    .sdram (NOLOAD) : ALIGN(4) SUBALIGN(4)
    {
        KEEP(*(.sdram))
        KEEP(*(.sdram.*))
        PROVIDE(_sdram_dyn_start = .);
        PROVIDE(_sdram_dyn_end = ORIGIN(SDRAM) + LENGTH(SDRAM));
    } > SDRAM


 /*   
    .ARM.exidx : 
    {
       __exidx_start = .;
       *(.ARM.exidx* .gnu.linkonce.armexidx.*)
       __exidx_end = .;
    } >CCMSRAM
    _etext = .;
*/
}
