#include "usb_msd.h"

#define CH_FAILED               TRUE

/* Request types */
#define MSD_REQ_RESET   0xFF
#define MSD_GET_MAX_LUN 0xFE

/* CBW/CSW block signatures */
#define MSD_CBW_SIGNATURE 0x43425355
#define MSD_CSW_SIGNATURE 0x53425355

/* Setup packet access macros */
#define MSD_SETUP_WORD(setup, index) (uint16_t)(((uint16_t)setup[index + 1] << 8) | (setup[index] & 0x00FF))
#define MSD_SETUP_VALUE(setup)       MSD_SETUP_WORD(setup, 2)
#define MSD_SETUP_INDEX(setup)       MSD_SETUP_WORD(setup, 4)
#define MSD_SETUP_LENGTH(setup)      MSD_SETUP_WORD(setup, 6)

/* Command statuses */
#define MSD_COMMAND_PASSED      0x00
#define MSD_COMMAND_FAILED      0x01
#define MSD_COMMAND_PHASE_ERROR 0x02

/* SCSI commands */
#define SCSI_CMD_TEST_UNIT_READY              0x00
#define SCSI_CMD_REQUEST_SENSE                0x03
#define SCSI_CMD_FORMAT_UNIT                  0x04
#define SCSI_CMD_INQUIRY                      0x12
#define SCSI_CMD_MODE_SENSE_6                 0x1A
#define SCSI_CMD_START_STOP_UNIT              0x1B
#define SCSI_CMD_SEND_DIAGNOSTIC              0x1D
#define SCSI_CMD_PREVENT_ALLOW_MEDIUM_REMOVAL 0x1E
#define SCSI_CMD_READ_FORMAT_CAPACITIES       0x23
#define SCSI_CMD_READ_CAPACITY_10             0x25
#define SCSI_CMD_READ_10                      0x28
#define SCSI_CMD_WRITE_10                     0x2A
#define SCSI_CMD_VERIFY_10                    0x2F
#define SCSI_CMD_SYNCHRONIZE_CACHE            0x35 

/* SCSI sense keys */
#define SCSI_SENSE_KEY_GOOD                            0x00
#define SCSI_SENSE_KEY_RECOVERED_ERROR                 0x01
#define SCSI_SENSE_KEY_NOT_READY                       0x02
#define SCSI_SENSE_KEY_MEDIUM_ERROR                    0x03
#define SCSI_SENSE_KEY_HARDWARE_ERROR                  0x04
#define SCSI_SENSE_KEY_ILLEGAL_REQUEST                 0x05
#define SCSI_SENSE_KEY_UNIT_ATTENTION                  0x06
#define SCSI_SENSE_KEY_DATA_PROTECT                    0x07
#define SCSI_SENSE_KEY_BLANK_CHECK                     0x08
#define SCSI_SENSE_KEY_VENDOR_SPECIFIC                 0x09
#define SCSI_SENSE_KEY_COPY_ABORTED                    0x0A
#define SCSI_SENSE_KEY_ABORTED_COMMAND                 0x0B
#define SCSI_SENSE_KEY_VOLUME_OVERFLOW                 0x0D
#define SCSI_SENSE_KEY_MISCOMPARE                      0x0E

#define SCSI_ASENSE_NO_ADDITIONAL_INFORMATION          0x00
#define SCSI_ASENSE_WRITE_FAULT                        0x03
#define SCSI_ASENSE_LOGICAL_UNIT_NOT_READY             0x04
#define SCSI_ASENSE_READ_ERROR                         0x11
#define SCSI_ASENSE_INVALID_COMMAND                    0x20
#define SCSI_ASENSE_LOGICAL_BLOCK_ADDRESS_OUT_OF_RANGE 0x21
#define SCSI_ASENSE_INVALID_FIELD_IN_CDB               0x24
#define SCSI_ASENSE_WRITE_PROTECTED                    0x27
#define SCSI_ASENSE_NOT_READY_TO_READY_CHANGE          0x28
#define SCSI_ASENSE_FORMAT_ERROR                       0x31
#define SCSI_ASENSE_MEDIUM_NOT_PRESENT                 0x3A

#define SCSI_ASENSEQ_NO_QUALIFIER                      0x00
#define SCSI_ASENSEQ_FORMAT_COMMAND_FAILED             0x01
#define SCSI_ASENSEQ_INITIALIZING_COMMAND_REQUIRED     0x02
#define SCSI_ASENSEQ_OPERATION_IN_PROGRESS             0x07

#define N_BLOCKS_PER_WRITE 1 /* Currently only 1 works ¯\_(ツ)_/¯ */

/**
 * @brief Response to a READ_CAPACITY_10 SCSI command
 */
PACK_STRUCT_BEGIN typedef struct {
    uint32_t last_block_addr;
    uint32_t block_size;
} PACK_STRUCT_STRUCT msd_scsi_read_capacity_10_response_t PACK_STRUCT_END;

/**
 * @brief Response to a READ_FORMAT_CAPACITIES SCSI command
 */
PACK_STRUCT_BEGIN typedef struct {
    uint8_t reserved[3];
    uint8_t capacity_list_length;
    uint32_t block_count;
    uint32_t desc_and_block_length;
} PACK_STRUCT_STRUCT msd_scsi_read_format_capacities_response_t PACK_STRUCT_END;

/**
 * @brief   Read-write buffers
 */
static uint8_t rw_buf[2][N_BLOCKS_PER_WRITE * MMCSD_BLOCK_SIZE];

/**
 * @brief Byte-swap a 32 bits unsigned integer
 */
#define swap_uint32(x) ((((x) & 0x000000FF) << 24) \
                      | (((x) & 0x0000FF00) << 8) \
                      | (((x) & 0x00FF0000) >> 8) \
                      | (((x) & 0xFF000000) >> 24))

/**
 * @brief Byte-swap a 16 bits unsigned integer
 */
#define swap_uint16(x) ((((x) & 0x00FF) << 8) \
                      | (((x) & 0xFF00) >> 8))

static void msd_handle_end_point_notification(USBDriver *usbp, usbep_t ep);

/**
 * @brief IN end-point 1 state
 */
static USBInEndpointState ep1_in_state;

/**
 * @brief OUT end-point 1 state
 */
static USBOutEndpointState ep1_out_state;

/**
 * @brief End-point 1 initialization structure
 */
static const USBEndpointConfig ep_data_config = {
    USB_EP_MODE_TYPE_BULK,
    NULL,
    msd_handle_end_point_notification,
    msd_handle_end_point_notification,
    64,
    64,
    &ep1_in_state,
    &ep1_out_state,
    1,
    NULL
};

/**
 * @brief   USB device configured handler.
 *
 * @param[in] msdp      pointer to the @p USBMassStorageDriver object
 *
 * @iclass
 */
void msdConfigureHookI(USBMassStorageDriver *msdp)
{
    usbInitEndpointI(msdp->config->usbp, msdp->config->bulk_ep, &ep_data_config);
    chBSemSignalI(&msdp->bsem);
    chEvtBroadcastI(&msdp->evt_connected);
}

/**
 * @brief   Default requests hook.
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 * @return              The hook status.
 * @retval TRUE         Message handled internally.
 * @retval FALSE        Message not handled.
 */
bool_t msdRequestsHook(USBDriver *usbp) {

    /* check that the request is of type Class / Interface */
    if (((usbp->setup[0] & USB_RTYPE_TYPE_MASK) == USB_RTYPE_TYPE_CLASS) &&
        ((usbp->setup[0] & USB_RTYPE_RECIPIENT_MASK) == USB_RTYPE_RECIPIENT_INTERFACE)) {

        /* check that the request is for interface 0 */
        if (MSD_SETUP_INDEX(usbp->setup) != 0)
            return FALSE;

        /* act depending on bRequest = setup[1] */
        switch (usbp->setup[1]) {
        case MSD_REQ_RESET:
            /* check that it is a HOST2DEV request */
            if (((usbp->setup[0] & USB_RTYPE_DIR_MASK) != USB_RTYPE_DIR_HOST2DEV) ||
               (MSD_SETUP_LENGTH(usbp->setup) != 0) ||
               (MSD_SETUP_VALUE(usbp->setup) != 0))
            {
                return FALSE;
            }

            /* reset all endpoints */
            /* TODO!*/
            /* The device shall NAK the status stage of the device request until
             * the Bulk-Only Mass Storage Reset is complete.
             */
            return TRUE;
        case MSD_GET_MAX_LUN:
            /* check that it is a DEV2HOST request */
            if (((usbp->setup[0] & USB_RTYPE_DIR_MASK) != USB_RTYPE_DIR_DEV2HOST) ||
               (MSD_SETUP_LENGTH(usbp->setup) != 1) ||
               (MSD_SETUP_VALUE(usbp->setup) != 0))
            {
                return FALSE;
            }

            static uint8_t len_buf[1] = {0};
            /* stall to indicate that we don't support LUN */
            usbSetupTransfer(usbp, len_buf, 1, NULL);
            return TRUE;
        default:
            return FALSE;
            break;
        }
    }

    return FALSE;
}

/**
 * @brief Wait until the end-point interrupt handler has been called
 */
static void msd_wait_for_isr(USBMassStorageDriver *msdp) {

    /* sleep until it completes */
    chSysLock();
    chBSemWaitS(&msdp->bsem);
    chSysUnlock();
}

/**
 * @brief Called when data can be read or written on the endpoint -- wakes the thread up
 */
static void msd_handle_end_point_notification(USBDriver *usbp, usbep_t ep) {

    (void)usbp;
    (void)ep;

    chSysLockFromIsr();
    chBSemSignalI(&((USBMassStorageDriver *)usbp->in_params[ep])->bsem);
    chSysUnlockFromIsr();
}

/**
 * @brief Starts sending data
 */
static void msd_start_transmit(USBMassStorageDriver *msdp, const uint8_t* buffer, size_t size) {
    chSysLock();
    usbStartTransmitI(msdp->config->usbp, msdp->config->bulk_ep, buffer, size);
    chSysUnlock();
}

/**
 * @brief Starts receiving data
 */
static void msd_start_receive(USBMassStorageDriver *msdp, uint8_t* buffer, size_t size) {
    chSysLock();
    usbStartReceiveI(msdp->config->usbp, msdp->config->bulk_ep, buffer, size);
    chSysUnlock();
}

/**
 * @brief Changes the SCSI sense information
 */
static inline void msd_scsi_set_sense(USBMassStorageDriver *msdp, uint8_t key, uint8_t acode, uint8_t aqual) {
    msdp->sense.byte[2] = key;
    msdp->sense.byte[12] = acode;
    msdp->sense.byte[13] = aqual;
}

/**
 * @brief Processes an INQUIRY SCSI command
 */
bool_t msd_scsi_process_inquiry(USBMassStorageDriver *msdp) {

    msd_cbw_t *cbw = &(msdp->cbw);

    /* check the EVPD bit (Vital Product Data) */
    if (cbw->scsi_cmd_data[1] & 0x01) {

        /* check the Page Code byte to know the type of product data to reply */
        switch (cbw->scsi_cmd_data[2]) {

        /* unit serial number */
        case 0x80: {
            uint8_t response[] = {'0'}; /* TODO */
            msd_start_transmit(msdp, response, sizeof(response));
            msdp->result = TRUE;

            /* wait for ISR */
            return TRUE;
        }

        /* unhandled */
        default:
            msd_scsi_set_sense(msdp,
                               SCSI_SENSE_KEY_ILLEGAL_REQUEST,
                               SCSI_ASENSE_INVALID_FIELD_IN_CDB,
                               SCSI_ASENSEQ_NO_QUALIFIER);
            return FALSE;
        }
    }
    else
    {
        msd_start_transmit(msdp, (const uint8_t *)&msdp->inquiry, sizeof(msdp->inquiry));
        msdp->result = TRUE;

        /* wait for ISR */
        return TRUE;
    }
}

/**
 * @brief Processes a REQUEST_SENSE SCSI command
 */
bool_t msd_scsi_process_request_sense(USBMassStorageDriver *msdp) {

    msd_start_transmit(msdp, (const uint8_t *)&msdp->sense, sizeof(msdp->sense));
    msdp->result = TRUE;

    /* wait for ISR immediately, otherwise the caller may reset the sense bytes before they are sent to the host! */
    msd_wait_for_isr(msdp);

    /* ... don't wait for ISR, we just did it */
    return FALSE;
}

/**
 * @brief Processes a READ_CAPACITY_10 SCSI command
 */
bool_t msd_scsi_process_read_capacity_10(USBMassStorageDriver *msdp) {

    static msd_scsi_read_capacity_10_response_t response;

    response.block_size = swap_uint32(msdp->block_dev_info.blk_size);
    response.last_block_addr = swap_uint32(msdp->block_dev_info.blk_num-1);

    msd_start_transmit(msdp, (const uint8_t *)&response, sizeof(response));
    msdp->result = TRUE;

    /* wait for ISR */
    return TRUE;
}

/**
 * @brief Processes a SEND_DIAGNOSTIC SCSI command
 */
bool_t msd_scsi_process_send_diagnostic(USBMassStorageDriver *msdp) {

    msd_cbw_t *cbw = &(msdp->cbw);

    if (!(cbw->scsi_cmd_data[1] & (1 << 2))) {
        /* only self-test supported - update SENSE key and fail the command */
        msd_scsi_set_sense(msdp,
                           SCSI_SENSE_KEY_ILLEGAL_REQUEST,
                           SCSI_ASENSE_INVALID_FIELD_IN_CDB,
                           SCSI_ASENSEQ_NO_QUALIFIER);
        msdp->result = FALSE;
        return FALSE;
    }

    /* TODO: actually perform the test */
    msdp->result = TRUE;

    /* don't wait for ISR */
    return FALSE;
}

/**
 * @brief Processes a READ_WRITE_10 SCSI command
 */
bool_t msd_scsi_process_start_read_write_10(USBMassStorageDriver *msdp) {

    msd_cbw_t *cbw = &(msdp->cbw);

    if ((cbw->scsi_cmd_data[0] == SCSI_CMD_WRITE_10) && blkIsWriteProtected(msdp->config->bbdp)) {
        /* device is write protected and a write has been issued */
        /* block address is invalid, update SENSE key and return command fail */
        msd_scsi_set_sense(msdp,
                           SCSI_SENSE_KEY_DATA_PROTECT,
                           SCSI_ASENSE_WRITE_PROTECTED,
                           SCSI_ASENSEQ_NO_QUALIFIER);
        msdp->result = FALSE;

        /* don't wait for ISR */
        return FALSE;
    }

    uint32_t rw_block_address = swap_uint32(*(uint32_t *)&cbw->scsi_cmd_data[2]);
    uint16_t total = swap_uint16(*(uint16_t *)&cbw->scsi_cmd_data[7]);
    uint16_t current_buf_idx = 0;
    uint16_t i = 0;

    if (rw_block_address >= msdp->block_dev_info.blk_num) {
        /* block address is invalid, update SENSE key and return command fail */
        msd_scsi_set_sense(msdp,
                           SCSI_SENSE_KEY_ILLEGAL_REQUEST,
                           SCSI_ASENSE_LOGICAL_BLOCK_ADDRESS_OUT_OF_RANGE,
                           SCSI_ASENSEQ_NO_QUALIFIER);
        msdp->result = FALSE;

        /* don't wait for ISR */
        return FALSE;
    }

    if (cbw->scsi_cmd_data[0] == SCSI_CMD_WRITE_10) {
        /* process a write command */

        /* get the first packet */
        msd_start_receive(msdp, rw_buf[current_buf_idx], N_BLOCKS_PER_WRITE * MMCSD_BLOCK_SIZE);
        msd_wait_for_isr(msdp);

        /* loop over all blocks, processing them in chunks of N_BLOCKS_PER_WRITE (currently only 1 works) */
        for (i = 0; i < total;) {
            /* How many blocks to write in this iteration (could be less than N_BLOCKS_PER_WRITE for the last chunk) */
            uint16_t blocks_to_write = (total - i > N_BLOCKS_PER_WRITE) ? N_BLOCKS_PER_WRITE : (total - i);

            uint8_t *buffer_to_process = rw_buf[current_buf_idx];

            if (i + blocks_to_write < total) {
                /* Switch to the other buffer */
                current_buf_idx = (current_buf_idx == 0) ? 1 : 0;
                /* Start receiving the next chunk into the other buffer */
                msd_start_receive(msdp, rw_buf[current_buf_idx], N_BLOCKS_PER_WRITE * MMCSD_BLOCK_SIZE);
            }

            chThdSleepMicroseconds(5); /* Yields a slight speed increase: typically 210 kB/s VS 175 kb/s */

            /* now write the block to the block device */
            if (blkWrite(msdp->config->bbdp, rw_block_address, buffer_to_process, blocks_to_write) == CH_FAILED) {
                /* write failed */
                msd_scsi_set_sense(msdp,
                                   SCSI_SENSE_KEY_MEDIUM_ERROR,
                                   SCSI_ASENSE_WRITE_FAULT,
                                   SCSI_ASENSEQ_NO_QUALIFIER);
                msdp->result = FALSE;

                /* don't wait for ISR */
                return FALSE;
            }

            if (sdcSync(&SDCD1) == HAL_FAILED) {
                msd_scsi_set_sense(msdp,
                                   SCSI_SENSE_KEY_MEDIUM_ERROR,
                                   SCSI_ASENSE_WRITE_FAULT,
                                   SCSI_ASENSEQ_NO_QUALIFIER);
                msdp->result = FALSE;
                return FALSE;
            }

            /* Increment block address and 'i' by the number of blocks just written */
            rw_block_address += blocks_to_write;
            i += blocks_to_write;

            if (i < total) {
                /* If there is a pending USB receive for the next chunk, wait for it to complete */
                msd_wait_for_isr(msdp);
            }
        }
    }
    else { // SCSI_CMD_READ_10
        /* process a read command */

        i = 0;

        /* read the first block from block device */
        if (blkRead(msdp->config->bbdp, rw_block_address++, rw_buf[i % 2], 1) == CH_FAILED) {
            /* read failed */
            msd_scsi_set_sense(msdp,
                               SCSI_SENSE_KEY_MEDIUM_ERROR,
                               SCSI_ASENSE_READ_ERROR,
                               SCSI_ASENSEQ_NO_QUALIFIER);
            msdp->result = FALSE;

            /* don't wait for ISR */
            return FALSE;
        }

        /* loop over each block */
        for (i = 0; i < total; i++) {
            /* transmit the block */
            msd_start_transmit(msdp, rw_buf[i % 2], msdp->block_dev_info.blk_size);

            chThdSleepMicroseconds(10); /* Required for stability. Possibly waiting for cache/prefetch...*/

            if (i < (total - 1)) {
                /* there is at least one more block to be read from device */
                /* so read that whilst the USB transfer takes place */
                if (blkRead(msdp->config->bbdp, rw_block_address++, rw_buf[(i + 1) % 2], 1) == CH_FAILED) {
                    /* read failed */
                    msd_scsi_set_sense(msdp,
                                       SCSI_SENSE_KEY_MEDIUM_ERROR,
                                       SCSI_ASENSE_READ_ERROR,
                                       SCSI_ASENSEQ_NO_QUALIFIER);
                    msdp->result = FALSE;

                    /* DON'T wait for ISR (the previous transmission is still running, but we must return FALSE to prevent the system from getting stuck waiting indefinitely in msd_wait_for_isr) */
                    return FALSE;
                }

                if (sdcSync(&SDCD1) == HAL_FAILED) {
                    msd_scsi_set_sense(msdp,
                                       SCSI_SENSE_KEY_MEDIUM_ERROR,
                                       SCSI_ASENSE_READ_ERROR,
                                       SCSI_ASENSEQ_NO_QUALIFIER);
                    msdp->result = FALSE;
                    return FALSE;
                }
            }

            /* wait for the USB event to complete */
            msd_wait_for_isr(msdp);
        }
    }

    msdp->result = TRUE;

    /* don't wait for ISR */
    return FALSE;
}

/**
 * @brief Processes a START_STOP_UNIT SCSI command
 */
bool_t msd_scsi_process_start_stop_unit(USBMassStorageDriver *msdp) {

    if ((msdp->cbw.scsi_cmd_data[4] & 0x03) == 0x02) {
        /* device has been ejected */
        chEvtBroadcast(&msdp->evt_ejected);
        msdp->state = MSD_EJECTED;
    }

    msdp->result = TRUE;

    /* don't wait for ISR */
    return FALSE;
}

/**
 * @brief Processes a MODE_SENSE_6 SCSI command
 */
bool_t msd_scsi_process_mode_sense_6(USBMassStorageDriver *msdp) {

    static uint8_t response[4] = {
        0x03, /* number of bytes that follow                    */
        0x00, /* medium type is SBC                             */
        0x00, /* not write protected (TODO handle it correctly) */
        0x00  /* no block descriptor                            */
    };

    msd_start_transmit(msdp, response, sizeof(response));
    msdp->result = TRUE;

    /* wait for ISR */
    return TRUE;
}

/**
 * @brief Processes a READ_FORMAT_CAPACITIES SCSI command
 */
bool_t msd_scsi_process_read_format_capacities(USBMassStorageDriver *msdp) {

    msd_scsi_read_format_capacities_response_t response;
    response.capacity_list_length = 1;
    response.block_count = swap_uint32(msdp->block_dev_info.blk_num);
    response.desc_and_block_length = swap_uint32((0x02 << 24) | (msdp->block_dev_info.blk_size & 0x00FFFFFF));

    msd_start_transmit(msdp, (const uint8_t*)&response, sizeof(response));
    msdp->result = TRUE;

    /* wait for ISR */
    return TRUE;
}

/**
 * @brief Processes a TEST_UNIT_READY SCSI command
 */
bool_t msd_scsi_process_test_unit_ready(USBMassStorageDriver *msdp) {

    if (blkIsInserted(msdp->config->bbdp)) {
        /* device inserted and ready */
        msdp->result = TRUE;
    } else {
        /* device not present or not ready */
        msd_scsi_set_sense(msdp,
                           SCSI_SENSE_KEY_NOT_READY,
                           SCSI_ASENSE_MEDIUM_NOT_PRESENT,
                           SCSI_ASENSEQ_NO_QUALIFIER);
        msdp->result = FALSE;
    }

    /* don't wait for ISR */
    return FALSE;
}

/**
 * @brief Waits for a new command block
 */
bool_t msd_wait_for_command_block(USBMassStorageDriver *msdp) {

    msd_start_receive(msdp, (uint8_t *)&msdp->cbw, sizeof(msdp->cbw));
    msdp->state = MSD_READ_COMMAND_BLOCK;

    /* wait for ISR */
    return TRUE;
}

/**
 * @brief Reads a newly received command block
 */
bool_t msd_read_command_block(USBMassStorageDriver *msdp) {

    msd_cbw_t *cbw = &(msdp->cbw);

    /* by default transition back to the idle state */
    msdp->state = MSD_IDLE;

    /* check the command */
    if ((cbw->signature != MSD_CBW_SIGNATURE) ||
        (cbw->lun > 0) ||
        ((cbw->data_len > 0) && (cbw->flags & 0x1F)) ||
        (cbw->scsi_cmd_len == 0) ||
        (cbw->scsi_cmd_len > 16)) {

        /* stall both IN and OUT endpoints */
        chSysLock();
        usbStallReceiveI(msdp->config->usbp, msdp->config->bulk_ep);
        usbStallTransmitI(msdp->config->usbp, msdp->config->bulk_ep);
        chSysUnlock();

        /* don't wait for ISR */
        return FALSE;
    }

    bool_t sleep = FALSE;

    /* check the command */
    switch (cbw->scsi_cmd_data[0]) {
    case SCSI_CMD_INQUIRY:
        sleep = msd_scsi_process_inquiry(msdp);
        break;
    case SCSI_CMD_REQUEST_SENSE:
        sleep = msd_scsi_process_request_sense(msdp);
        break;
    case SCSI_CMD_READ_CAPACITY_10:
        sleep = msd_scsi_process_read_capacity_10(msdp);
        break;
    case SCSI_CMD_READ_10:
    case SCSI_CMD_WRITE_10:
        if (msdp->config->rw_activity_callback)
            msdp->config->rw_activity_callback(TRUE);
        sleep = msd_scsi_process_start_read_write_10(msdp);
        if (msdp->config->rw_activity_callback)
            msdp->config->rw_activity_callback(FALSE);
        break;
    case SCSI_CMD_SEND_DIAGNOSTIC:
        sleep = msd_scsi_process_send_diagnostic(msdp);
        break;
    case SCSI_CMD_MODE_SENSE_6:
        sleep = msd_scsi_process_mode_sense_6(msdp);
        break;
    case SCSI_CMD_START_STOP_UNIT:
        sleep = msd_scsi_process_start_stop_unit(msdp);
        break;
    case SCSI_CMD_READ_FORMAT_CAPACITIES:
        sleep = msd_scsi_process_read_format_capacities(msdp);
        break;
    case SCSI_CMD_TEST_UNIT_READY:
        sleep = msd_scsi_process_test_unit_ready(msdp);
        break;
    case SCSI_CMD_FORMAT_UNIT:
        /* don't handle */
        msdp->result = TRUE;
        break;
    case SCSI_CMD_PREVENT_ALLOW_MEDIUM_REMOVAL:
        /* don't handle */
        msdp->result = TRUE;
        break;
    case SCSI_CMD_VERIFY_10:
        /* don't handle */
        msdp->result = TRUE;
        break;
    case SCSI_CMD_SYNCHRONIZE_CACHE:
        if (blkSync(msdp->config->bbdp)) {
            msdp->result = TRUE; /* Indicate success to the host */
        } else {
            /* Handle error if blkSync fails */
            msd_scsi_set_sense(msdp,
                            SCSI_SENSE_KEY_MEDIUM_ERROR,
                            SCSI_ASENSE_WRITE_FAULT,
                            SCSI_ASENSEQ_NO_QUALIFIER);
            msdp->result = FALSE; /* Indicate failure to the host */
        }
        sleep = FALSE; /* blkSync is blocking, no further wait needed here. */
        break;
    default:
        msd_scsi_set_sense(msdp,
                           SCSI_SENSE_KEY_ILLEGAL_REQUEST,
                           SCSI_ASENSE_INVALID_COMMAND,
                           SCSI_ASENSEQ_NO_QUALIFIER);

        /* stall IN endpoint */
        chSysLock();
        usbStallTransmitI(msdp->config->usbp, msdp->config->bulk_ep);
        chSysUnlock();

        return FALSE;
    }

    if (msdp->result) {
        /* update sense with success status */
        msd_scsi_set_sense(msdp,
                           SCSI_SENSE_KEY_GOOD,
                           SCSI_ASENSE_NO_ADDITIONAL_INFORMATION,
                           SCSI_ASENSEQ_NO_QUALIFIER);

        /* reset data length left */
        cbw->data_len = 0;
    }

    /* wait for ISR if needed */
    if (sleep)
        msd_wait_for_isr(msdp);

    msd_csw_t *csw = &(msdp->csw);

    if (!msdp->result && cbw->data_len) {
        /* still bytes left to send, this is too early to send CSW? */
        chSysLock();
        usbStallReceiveI(msdp->config->usbp, msdp->config->bulk_ep);
        usbStallTransmitI(msdp->config->usbp, msdp->config->bulk_ep);
        chSysUnlock();

        /*return FALSE;*/
    }

    /* update the command status wrapper and send it to the host */
    csw->status = (msdp->result) ? MSD_COMMAND_PASSED : MSD_COMMAND_FAILED;
    csw->signature = MSD_CSW_SIGNATURE;
    csw->data_residue = cbw->data_len;
    csw->tag = cbw->tag;

    msd_start_transmit(msdp, (const uint8_t *)csw, sizeof(*csw));

    /* wait for ISR */
    return TRUE;
}

/**
 * @brief Mass storage thread that processes commands
 */
static WORKING_AREA(mass_storage_thread_wa, 2048);
static msg_t mass_storage_thread(void *arg) {

    USBMassStorageDriver *msdp = (USBMassStorageDriver *)arg;

    chRegSetThreadName("USB-MSD");

    bool_t wait_for_isr = FALSE;

    /* wait for the usb to be initialised */
    msd_wait_for_isr(msdp);

    while (!chThdShouldTerminate()) {
        wait_for_isr = FALSE;

        /* wait on data depending on the current state */
        switch (msdp->state) {
        case MSD_IDLE:
            wait_for_isr = msd_wait_for_command_block(msdp);
            break;
        case MSD_READ_COMMAND_BLOCK:
            wait_for_isr = msd_read_command_block(msdp);
            break;
        case MSD_EJECTED:
            /* disconnect usb device */
            usbDisconnectBus(msdp->config->usbp);
            usbStop(msdp->config->usbp);
            chThdExit(0);
            return 0;
        }

        /* wait until the ISR wakes thread */
        if (wait_for_isr)
            msd_wait_for_isr(msdp);
    }

    return 0;
}

/**
 * @brief Initializse a USB mass storage driver
 */
void msdInit(USBMassStorageDriver *msdp) {

    chDbgCheck(msdp != NULL);

    msdp->config = NULL;
    msdp->thread = NULL;
    msdp->state = MSD_IDLE;

    /* initialize the driver events */
    chEvtInit(&msdp->evt_connected);
    chEvtInit(&msdp->evt_ejected);

    /* initialise the binary semaphore as taken */
    chBSemObjectInit(&msdp->bsem, TRUE);

    /* initialise the sense data structure */
    size_t i;
    for (i = 0; i < sizeof(msdp->sense.byte); i++)
        msdp->sense.byte[i] = 0x00;
    msdp->sense.byte[0] = 0x70; /* response code */
    msdp->sense.byte[7] = 0x0A; /* additional sense length */

    /* initialize the inquiry data structure */
    msdp->inquiry.peripheral = 0x00;           /* direct access block device  */
    msdp->inquiry.removable = 0x80;            /* removable                   */
    msdp->inquiry.version = 0x04;              /* SPC-2                       */
    msdp->inquiry.response_data_format = 0x02; /* response data format        */
    msdp->inquiry.additional_length = 0x20;    /* response has 0x20 + 4 bytes */
    msdp->inquiry.sccstp = 0x00;
    msdp->inquiry.bqueetc = 0x00;
    msdp->inquiry.cmdque = 0x00;
}

/**
 * @brief Starts a USB mass storage driver
 */
int msdStart(USBMassStorageDriver *msdp, const USBMassStorageConfig *config) {

    chDbgCheck(msdp != NULL);
    chDbgCheck(config != NULL);
    chDbgCheck(msdp->thread == NULL);

    /* save the configuration */
    msdp->config = config;

    /* copy the config strings to the inquiry response structure */
    size_t i;
    for (i = 0; i < sizeof(msdp->config->short_vendor_id); ++i)
        msdp->inquiry.vendor_id[i] = config->short_vendor_id[i];
    for (i = 0; i < sizeof(msdp->config->short_product_id); ++i)
        msdp->inquiry.product_id[i] = config->short_product_id[i];
    for (i = 0; i < sizeof(msdp->config->short_product_version); ++i)
        msdp->inquiry.product_rev[i] = config->short_product_version[i];

    /* set the initial state */
    msdp->state = MSD_IDLE;

    /* make sure block device is working */
    i = 0;
    while (blkGetDriverState(config->bbdp) != BLK_READY) {
        chThdSleepMilliseconds(50);
        i++;
        if (i>100) {
            return -1;
        }
    }

    /* get block device information */
    blkGetInfo(config->bbdp, &msdp->block_dev_info);

    /* store the pointer to the mass storage driver into the user param
       of the USB driver, so that we can find it back in callbacks */
    config->usbp->in_params[config->bulk_ep] = (void *)msdp;
    config->usbp->out_params[config->bulk_ep] = (void *)msdp;

    /* run the thread */
    msdp->thread = chThdCreateStatic(mass_storage_thread_wa, sizeof(mass_storage_thread_wa), NORMALPRIO, (void*) mass_storage_thread, msdp);
    
    return 0;
}

/**
 * @brief Stops a USB mass storage driver
 */
void msdStop(USBMassStorageDriver *msdp) {

    chDbgCheck(msdp->thread != NULL);

    /* notify the thread that it's over */
    chThdTerminate(msdp->thread);

    /* wake the thread up and wait until it ends */
    chBSemSignal(&msdp->bsem);
    chThdWait(msdp->thread);
    msdp->thread = NULL;

    /* release the user params in the USB driver */
    msdp->config->usbp->in_params[msdp->config->bulk_ep] = NULL;
    msdp->config->usbp->out_params[msdp->config->bulk_ep] = NULL;
}
