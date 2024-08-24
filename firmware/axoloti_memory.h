/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
#ifndef AXOLOTI_MEMORY_H
#define AXOLOTI_MEMORY_H

/*
 * pseudo dynamic memory allocator for sdram used in patches
 * can only free the last allocated memory
 * it is recommended to only use this only during patch object initialization
 */

#ifdef __cplusplus
extern "C" {
#endif


/*
 * sdram_init initializes the bounds of the allocator
 * called in patch startup
 */
void sdram_init(char *base_addr, char *end_addr);

/*
 * sdram_malloc allocates a segment of memory
 */
void* sdram_malloc(size_t size);

/*
 * sdram_free frees a slice of allocated memory
 * can only free the last allocated segment!
 */
void sdram_free(void *ptr);

/*
 * returns size available, <0 when overflow happened
 */
int32_t sdram_get_free(void);

#ifdef __cplusplus
}
#endif

#endif
