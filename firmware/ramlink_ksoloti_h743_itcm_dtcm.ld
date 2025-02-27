ENTRY(xpatch_init)

/* Linkfile for h743 Code in ITCM, Data in DTCM */

MEMORY
{
    CODE (wx)  : org = 0x00000000, len = 64k        /* ITCM 64 kb for code*/
    SRAM1 (wx) : org = 0x24040000, len = 256k       /* AXI SRAM 256KB Free */
    SRAM2 (wx) : org = 0x30000000, len = 288k       /* AHB SRAM1+SRAM2+SRAM3 */
    SRAM3 (wx) : org = 0x38000000, len = 64k        /* AHB SRAM4 */
    DATA (rw)  : org = 0x20010000, len = 64k        /* 64KB DTCM for data, second half*/
    SDRAM      : org = 0xC0000000, len = 32m        /* 32 MB */
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
    } > CODE
    
    constructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__ctor_array_start = .);
        KEEP(*(SORT(.init_array.*)))
        KEEP(*(.init_array))
        PROVIDE(__ctor_array_end = .);
    } > CODE

    destructors : ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(__dtor_array_start = .);
        KEEP(*(.fini_array))
        KEEP(*(SORT(.fini_array.*)))
        PROVIDE(__dtor_array_end = .);
    } > CODE

    .rodata : ALIGN(4) SUBALIGN(4) 
    {
        * (.rodata);
        * (.rodata.*);
    } > CODE

    .data (NOLOAD): ALIGN(4) SUBALIGN(4)
    {
        sdata = .;
        * (.data);
        edata = .;
    } > CODE

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
   
    .bss (NOLOAD): ALIGN(4) SUBALIGN(4)
    {
        PROVIDE(_pbss_start = .);        
        sbss = .;
        * (.bss);
        ebss = .;
        PROVIDE(_pbss_end = .);        
    } > DATA
    
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
