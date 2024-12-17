#if FW_USBAUDIO

#ifndef _AUDIO_USB_H_
#define _AUDIO_USB_H_

#include <stdint.h>

/*============================================================================*/
/* Defines and macros taken from TinyUSB : https://github.com/hathach/tinyusb */
/* Thanks to Ha Thach :                                                       */
/*   '(Good programmer write good codes. Great programmer copy great codes)'  */
/*============================================================================*/

/// A.2 - Audio Function Subclass Codes
typedef enum
{
  AUDIO_FUNCTION_SUBCLASS_UNDEFINED = 0x00,
} audio_function_subclass_type_t;

/// A.3 - Audio Function Protocol Codes
typedef enum
{
  AUDIO_FUNC_PROTOCOL_CODE_UNDEF       = 0x00,
  AUDIO_FUNC_PROTOCOL_CODE_V2          = 0x20, ///< Version 2.0
} audio_function_protocol_code_t;

/// A.5 - Audio Interface Subclass Codes
typedef enum
{
  AUDIO_SUBCLASS_UNDEFINED = 0x00,
  AUDIO_SUBCLASS_CONTROL         , ///< Audio Control
  AUDIO_SUBCLASS_STREAMING       , ///< Audio Streaming
  AUDIO_SUBCLASS_MIDI_STREAMING  , ///< MIDI Streaming
} audio_subclass_type_t;

/// A.6 - Audio Interface Protocol Codes
typedef enum
{
  AUDIO_INT_PROTOCOL_CODE_UNDEF       = 0x00,
  AUDIO_INT_PROTOCOL_CODE_V2          = 0x20, ///< Version 2.0
} audio_interface_protocol_code_t;

/// A.7 - Audio Function Category Codes
typedef enum
{
  AUDIO_FUNC_UNDEF              = 0x00,
  AUDIO_FUNC_DESKTOP_SPEAKER    = 0x01,
  AUDIO_FUNC_HOME_THEATER       = 0x02,
  AUDIO_FUNC_MICROPHONE         = 0x03,
  AUDIO_FUNC_HEADSET            = 0x04,
  AUDIO_FUNC_TELEPHONE          = 0x05,
  AUDIO_FUNC_CONVERTER          = 0x06,
  AUDIO_FUNC_SOUND_RECODER      = 0x07,
  AUDIO_FUNC_IO_BOX             = 0x08,
  AUDIO_FUNC_MUSICAL_INSTRUMENT = 0x09,
  AUDIO_FUNC_PRO_AUDIO          = 0x0A,
  AUDIO_FUNC_AUDIO_VIDEO        = 0x0B,
  AUDIO_FUNC_CONTROL_PANEL      = 0x0C,
  AUDIO_FUNC_OTHER              = 0xFF,
} audio_function_code_t;

/// A.9 - Audio Class-Specific AC Interface Descriptor Subtypes UAC2
typedef enum
{
  AUDIO_CS_AC_INTERFACE_AC_DESCRIPTOR_UNDEF   = 0x00,
  AUDIO_CS_AC_INTERFACE_HEADER                = 0x01,
  AUDIO_CS_AC_INTERFACE_INPUT_TERMINAL        = 0x02,
  AUDIO_CS_AC_INTERFACE_OUTPUT_TERMINAL       = 0x03,
  AUDIO_CS_AC_INTERFACE_MIXER_UNIT            = 0x04,
  AUDIO_CS_AC_INTERFACE_SELECTOR_UNIT         = 0x05,
  AUDIO_CS_AC_INTERFACE_FEATURE_UNIT          = 0x06,
  AUDIO_CS_AC_INTERFACE_EFFECT_UNIT           = 0x07,
  AUDIO_CS_AC_INTERFACE_PROCESSING_UNIT       = 0x08,
  AUDIO_CS_AC_INTERFACE_EXTENSION_UNIT        = 0x09,
  AUDIO_CS_AC_INTERFACE_CLOCK_SOURCE          = 0x0A,
  AUDIO_CS_AC_INTERFACE_CLOCK_SELECTOR        = 0x0B,
  AUDIO_CS_AC_INTERFACE_CLOCK_MULTIPLIER      = 0x0C,
  AUDIO_CS_AC_INTERFACE_SAMPLE_RATE_CONVERTER = 0x0D,
} audio_cs_ac_interface_subtype_t;

/// A.10 - Audio Class-Specific AS Interface Descriptor Subtypes UAC2
typedef enum
{
  AUDIO_CS_AS_INTERFACE_AS_DESCRIPTOR_UNDEF   = 0x00,
  AUDIO_CS_AS_INTERFACE_AS_GENERAL            = 0x01,
  AUDIO_CS_AS_INTERFACE_FORMAT_TYPE           = 0x02,
  AUDIO_CS_AS_INTERFACE_ENCODER               = 0x03,
  AUDIO_CS_AS_INTERFACE_DECODER               = 0x04,
} audio_cs_as_interface_subtype_t;

/// A.11 - Effect Unit Effect Types
typedef enum
{
  AUDIO_EFFECT_TYPE_UNDEF                     = 0x00,
  AUDIO_EFFECT_TYPE_PARAM_EQ_SECTION          = 0x01,
  AUDIO_EFFECT_TYPE_REVERBERATION             = 0x02,
  AUDIO_EFFECT_TYPE_MOD_DELAY                 = 0x03,
  AUDIO_EFFECT_TYPE_DYN_RANGE_COMP            = 0x04,
} audio_effect_unit_effect_type_t;

/// A.12 - Processing Unit Process Types
typedef enum
{
  AUDIO_PROCESS_TYPE_UNDEF                    = 0x00,
  AUDIO_PROCESS_TYPE_UP_DOWN_MIX              = 0x01,
  AUDIO_PROCESS_TYPE_DOLBY_PROLOGIC           = 0x02,
  AUDIO_PROCESS_TYPE_STEREO_EXTENDER          = 0x03,
} audio_processing_unit_process_type_t;

/// A.13 - Audio Class-Specific EP Descriptor Subtypes UAC2
typedef enum
{
  AUDIO_CS_EP_SUBTYPE_UNDEF                   = 0x00,
  AUDIO_CS_EP_SUBTYPE_GENERAL                 = 0x01,
} audio_cs_ep_subtype_t;

/// A.14 - Audio Class-Specific Request Codes
typedef enum
{
  AUDIO_CS_REQ_UNDEF                          = 0x00,
  AUDIO_CS_REQ_CUR                            = 0x01,
  AUDIO_CS_REQ_RANGE                          = 0x02,
  AUDIO_CS_REQ_MEM                            = 0x03,
} audio_cs_req_t;

/// A.17 - Control Selector Codes

/// A.17.1 - Clock Source Control Selectors
typedef enum
{
  AUDIO_CS_CTRL_UNDEF                         = 0x00,
  AUDIO_CS_CTRL_SAM_FREQ                      = 0x01,
  AUDIO_CS_CTRL_CLK_VALID                     = 0x02,
} audio_clock_src_control_selector_t;

/// A.17.2 - Clock Selector Control Selectors
typedef enum
{
  AUDIO_CX_CTRL_UNDEF                         = 0x00,
  AUDIO_CX_CTRL_CONTROL                       = 0x01,
} audio_clock_sel_control_selector_t;

/// A.17.3 - Clock Multiplier Control Selectors
typedef enum
{
  AUDIO_CM_CTRL_UNDEF                         = 0x00,
  AUDIO_CM_CTRL_NUMERATOR_CONTROL             = 0x01,
  AUDIO_CM_CTRL_DENOMINATOR_CONTROL           = 0x02,
} audio_clock_mul_control_selector_t;

/// A.17.4 - Terminal Control Selectors
typedef enum
{
  AUDIO_TE_CTRL_UNDEF                         = 0x00,
  AUDIO_TE_CTRL_COPY_PROTECT                  = 0x01,
  AUDIO_TE_CTRL_CONNECTOR                     = 0x02,
  AUDIO_TE_CTRL_OVERLOAD                      = 0x03,
  AUDIO_TE_CTRL_CLUSTER                       = 0x04,
  AUDIO_TE_CTRL_UNDERFLOW                     = 0x05,
  AUDIO_TE_CTRL_OVERFLOW                      = 0x06,
  AUDIO_TE_CTRL_LATENCY                       = 0x07,
} audio_terminal_control_selector_t;

/// A.17.5 - Mixer Control Selectors
typedef enum
{
  AUDIO_MU_CTRL_UNDEF                         = 0x00,
  AUDIO_MU_CTRL_MIXER                         = 0x01,
  AUDIO_MU_CTRL_CLUSTER                       = 0x02,
  AUDIO_MU_CTRL_UNDERFLOW                     = 0x03,
  AUDIO_MU_CTRL_OVERFLOW                      = 0x04,
  AUDIO_MU_CTRL_LATENCY                       = 0x05,
} audio_mixer_control_selector_t;

/// A.17.6 - Selector Control Selectors
typedef enum
{
  AUDIO_SU_CTRL_UNDEF                         = 0x00,
  AUDIO_SU_CTRL_SELECTOR                      = 0x01,
  AUDIO_SU_CTRL_LATENCY                       = 0x02,
} audio_sel_control_selector_t;

/// A.17.7 - Feature Unit Control Selectors
typedef enum
{
  AUDIO_FU_CTRL_UNDEF                         = 0x00,
  AUDIO_FU_CTRL_MUTE                          = 0x01,
  AUDIO_FU_CTRL_VOLUME                        = 0x02,
  AUDIO_FU_CTRL_BASS                          = 0x03,
  AUDIO_FU_CTRL_MID                           = 0x04,
  AUDIO_FU_CTRL_TREBLE                        = 0x05,
  AUDIO_FU_CTRL_GRAPHIC_EQUALIZER             = 0x06,
  AUDIO_FU_CTRL_AGC                           = 0x07,
  AUDIO_FU_CTRL_DELAY                         = 0x08,
  AUDIO_FU_CTRL_BASS_BOOST                    = 0x09,
  AUDIO_FU_CTRL_LOUDNESS                      = 0x0A,
  AUDIO_FU_CTRL_INPUT_GAIN                    = 0x0B,
  AUDIO_FU_CTRL_GAIN_PAD                      = 0x0C,
  AUDIO_FU_CTRL_INVERTER                      = 0x0D,
  AUDIO_FU_CTRL_UNDERFLOW                     = 0x0E,
  AUDIO_FU_CTRL_OVERVLOW                      = 0x0F,
  AUDIO_FU_CTRL_LATENCY                       = 0x10,
} audio_feature_unit_control_selector_t;

/// A.17.8 Effect Unit Control Selectors

/// A.17.8.1 Parametric Equalizer Section Effect Unit Control Selectors
typedef enum
{
  AUDIO_PE_CTRL_UNDEF                         = 0x00,
  AUDIO_PE_CTRL_ENABLE                        = 0x01,
  AUDIO_PE_CTRL_CENTERFREQ                    = 0x02,
  AUDIO_PE_CTRL_QFACTOR                       = 0x03,
  AUDIO_PE_CTRL_GAIN                          = 0x04,
  AUDIO_PE_CTRL_UNDERFLOW                     = 0x05,
  AUDIO_PE_CTRL_OVERFLOW                      = 0x06,
  AUDIO_PE_CTRL_LATENCY                       = 0x07,
} audio_parametric_equalizer_control_selector_t;

/// A.17.8.2 Reverberation Effect Unit Control Selectors
typedef enum
{
  AUDIO_RV_CTRL_UNDEF                         = 0x00,
  AUDIO_RV_CTRL_ENABLE                        = 0x01,
  AUDIO_RV_CTRL_TYPE                          = 0x02,
  AUDIO_RV_CTRL_LEVEL                         = 0x03,
  AUDIO_RV_CTRL_TIME                          = 0x04,
  AUDIO_RV_CTRL_FEEDBACK                      = 0x05,
  AUDIO_RV_CTRL_PREDELAY                      = 0x06,
  AUDIO_RV_CTRL_DENSITY                       = 0x07,
  AUDIO_RV_CTRL_HIFREQ_ROLLOFF                = 0x08,
  AUDIO_RV_CTRL_UNDERFLOW                     = 0x09,
  AUDIO_RV_CTRL_OVERFLOW                      = 0x0A,
  AUDIO_RV_CTRL_LATENCY                       = 0x0B,
} audio_reverberation_effect_control_selector_t;

/// A.17.8.3 Modulation Delay Effect Unit Control Selectors
typedef enum
{
  AUDIO_MD_CTRL_UNDEF                         = 0x00,
  AUDIO_MD_CTRL_ENABLE                        = 0x01,
  AUDIO_MD_CTRL_BALANCE                       = 0x02,
  AUDIO_MD_CTRL_RATE                          = 0x03,
  AUDIO_MD_CTRL_DEPTH                         = 0x04,
  AUDIO_MD_CTRL_TIME                          = 0x05,
  AUDIO_MD_CTRL_FEEDBACK                      = 0x06,
  AUDIO_MD_CTRL_UNDERFLOW                     = 0x07,
  AUDIO_MD_CTRL_OVERFLOW                      = 0x08,
  AUDIO_MD_CTRL_LATENCY                       = 0x09,
} audio_modulation_delay_control_selector_t;

/// A.17.8.4 Dynamic Range Compressor Effect Unit Control Selectors
typedef enum
{
  AUDIO_DR_CTRL_UNDEF                         = 0x00,
  AUDIO_DR_CTRL_ENABLE                        = 0x01,
  AUDIO_DR_CTRL_COMPRESSION_RATE              = 0x02,
  AUDIO_DR_CTRL_MAXAMPL                       = 0x03,
  AUDIO_DR_CTRL_THRESHOLD                     = 0x04,
  AUDIO_DR_CTRL_ATTACK_TIME                   = 0x05,
  AUDIO_DR_CTRL_RELEASE_TIME                  = 0x06,
  AUDIO_DR_CTRL_UNDERFLOW                     = 0x07,
  AUDIO_DR_CTRL_OVERFLOW                      = 0x08,
  AUDIO_DR_CTRL_LATENCY                       = 0x09,
} audio_dynamic_range_compression_control_selector_t;

/// A.17.9 Processing Unit Control Selectors

/// A.17.9.1 Up/Down-mix Processing Unit Control Selectors
typedef enum
{
  AUDIO_UD_CTRL_UNDEF                         = 0x00,
  AUDIO_UD_CTRL_ENABLE                        = 0x01,
  AUDIO_UD_CTRL_MODE_SELECT                   = 0x02,
  AUDIO_UD_CTRL_CLUSTER                       = 0x03,
  AUDIO_UD_CTRL_UNDERFLOW                     = 0x04,
  AUDIO_UD_CTRL_OVERFLOW                      = 0x05,
  AUDIO_UD_CTRL_LATENCY                       = 0x06,
} audio_up_down_mix_control_selector_t;

/// A.17.9.2 Dolby Prologic ™ Processing Unit Control Selectors
typedef enum
{
  AUDIO_DP_CTRL_UNDEF                         = 0x00,
  AUDIO_DP_CTRL_ENABLE                        = 0x01,
  AUDIO_DP_CTRL_MODE_SELECT                   = 0x02,
  AUDIO_DP_CTRL_CLUSTER                       = 0x03,
  AUDIO_DP_CTRL_UNDERFLOW                     = 0x04,
  AUDIO_DP_CTRL_OVERFLOW                      = 0x05,
  AUDIO_DP_CTRL_LATENCY                       = 0x06,
} audio_dolby_prologic_control_selector_t;

/// A.17.9.3 Stereo Extender Processing Unit Control Selectors
typedef enum
{
  AUDIO_ST_EXT_CTRL_UNDEF                     = 0x00,
  AUDIO_ST_EXT_CTRL_ENABLE                    = 0x01,
  AUDIO_ST_EXT_CTRL_WIDTH                     = 0x02,
  AUDIO_ST_EXT_CTRL_UNDERFLOW                 = 0x03,
  AUDIO_ST_EXT_CTRL_OVERFLOW                  = 0x04,
  AUDIO_ST_EXT_CTRL_LATENCY                   = 0x05,
} audio_stereo_extender_control_selector_t;

/// A.17.10 Extension Unit Control Selectors
typedef enum
{
  AUDIO_XU_CTRL_UNDEF                         = 0x00,
  AUDIO_XU_CTRL_ENABLE                        = 0x01,
  AUDIO_XU_CTRL_CLUSTER                       = 0x02,
  AUDIO_XU_CTRL_UNDERFLOW                     = 0x03,
  AUDIO_XU_CTRL_OVERFLOW                      = 0x04,
  AUDIO_XU_CTRL_LATENCY                       = 0x05,
} audio_extension_unit_control_selector_t;

/// A.17.11 AudioStreaming Interface Control Selectors
typedef enum
{
  AUDIO_AS_CTRL_UNDEF                         = 0x00,
  AUDIO_AS_CTRL_ACT_ALT_SETTING               = 0x01,
  AUDIO_AS_CTRL_VAL_ALT_SETTINGS              = 0x02,
  AUDIO_AS_CTRL_AUDIO_DATA_FORMAT             = 0x03,
} audio_audiostreaming_interface_control_selector_t;

/// A.17.12 Encoder Control Selectors
typedef enum
{
  AUDIO_EN_CTRL_UNDEF                         = 0x00,
  AUDIO_EN_CTRL_BIT_RATE                      = 0x01,
  AUDIO_EN_CTRL_QUALITY                       = 0x02,
  AUDIO_EN_CTRL_VBR                           = 0x03,
  AUDIO_EN_CTRL_TYPE                          = 0x04,
  AUDIO_EN_CTRL_UNDERFLOW                     = 0x05,
  AUDIO_EN_CTRL_OVERFLOW                      = 0x06,
  AUDIO_EN_CTRL_ENCODER_ERROR                 = 0x07,
  AUDIO_EN_CTRL_PARAM1                        = 0x08,
  AUDIO_EN_CTRL_PARAM2                        = 0x09,
  AUDIO_EN_CTRL_PARAM3                        = 0x0A,
  AUDIO_EN_CTRL_PARAM4                        = 0x0B,
  AUDIO_EN_CTRL_PARAM5                        = 0x0C,
  AUDIO_EN_CTRL_PARAM6                        = 0x0D,
  AUDIO_EN_CTRL_PARAM7                        = 0x0E,
  AUDIO_EN_CTRL_PARAM8                        = 0x0F,
} audio_encoder_control_selector_t;

/// A.17.13 Decoder Control Selectors

/// A.17.13.1 MPEG Decoder Control Selectors
typedef enum
{
  AUDIO_MPD_CTRL_UNDEF                        = 0x00,
  AUDIO_MPD_CTRL_DUAL_CHANNEL                 = 0x01,
  AUDIO_MPD_CTRL_SECOND_STEREO                = 0x02,
  AUDIO_MPD_CTRL_MULTILINGUAL                 = 0x03,
  AUDIO_MPD_CTRL_DYN_RANGE                    = 0x04,
  AUDIO_MPD_CTRL_SCALING                      = 0x05,
  AUDIO_MPD_CTRL_HILO_SCALING                 = 0x06,
  AUDIO_MPD_CTRL_UNDERFLOW                    = 0x07,
  AUDIO_MPD_CTRL_OVERFLOW                     = 0x08,
  AUDIO_MPD_CTRL_DECODER_ERROR                = 0x09,
} audio_MPEG_decoder_control_selector_t;

/// A.17.13.2 AC-3 Decoder Control Selectors
typedef enum
{
  AUDIO_AD_CTRL_UNDEF                         = 0x00,
  AUDIO_AD_CTRL_MODE                          = 0x01,
  AUDIO_AD_CTRL_DYN_RANGE                     = 0x02,
  AUDIO_AD_CTRL_SCALING                       = 0x03,
  AUDIO_AD_CTRL_HILO_SCALING                  = 0x04,
  AUDIO_AD_CTRL_UNDERFLOW                     = 0x05,
  AUDIO_AD_CTRL_OVERFLOW                      = 0x06,
  AUDIO_AD_CTRL_DECODER_ERROR                 = 0x07,
} audio_AC3_decoder_control_selector_t;

/// A.17.13.3 WMA Decoder Control Selectors
typedef enum
{
  AUDIO_WD_CTRL_UNDEF                         = 0x00,
  AUDIO_WD_CTRL_UNDERFLOW                     = 0x01,
  AUDIO_WD_CTRL_OVERFLOW                      = 0x02,
  AUDIO_WD_CTRL_DECODER_ERROR                 = 0x03,
} audio_WMA_decoder_control_selector_t;

/// A.17.13.4 DTS Decoder Control Selectors
typedef enum
{
  AUDIO_DD_CTRL_UNDEF                         = 0x00,
  AUDIO_DD_CTRL_UNDERFLOW                     = 0x01,
  AUDIO_DD_CTRL_OVERFLOW                      = 0x02,
  AUDIO_DD_CTRL_DECODER_ERROR                 = 0x03,
} audio_DTS_decoder_control_selector_t;

/// A.17.14 Endpoint Control Selectors
typedef enum
{
  AUDIO_EP_CTRL_UNDEF                         = 0x00,
  AUDIO_EP_CTRL_PITCH                         = 0x01,
  AUDIO_EP_CTRL_DATA_OVERRUN                  = 0x02,
  AUDIO_EP_CTRL_DATA_UNDERRUN                 = 0x03,
} audio_EP_control_selector_t;

/// Terminal Types

/// 2.1 - Audio Class-Terminal Types UAC2
typedef enum
{
  AUDIO_TERM_TYPE_USB_UNDEFINED       = 0x0100,
  AUDIO_TERM_TYPE_USB_STREAMING       = 0x0101,
  AUDIO_TERM_TYPE_USB_VENDOR_SPEC     = 0x01FF,
} audio_terminal_type_t;

/// 2.2 - Audio Class-Input Terminal Types UAC2
typedef enum
{
  AUDIO_TERM_TYPE_IN_UNDEFINED        = 0x0200,
  AUDIO_TERM_TYPE_IN_GENERIC_MIC      = 0x0201,
  AUDIO_TERM_TYPE_IN_DESKTOP_MIC      = 0x0202,
  AUDIO_TERM_TYPE_IN_PERSONAL_MIC     = 0x0203,
  AUDIO_TERM_TYPE_IN_OMNI_MIC         = 0x0204,
  AUDIO_TERM_TYPE_IN_ARRAY_MIC        = 0x0205,
  AUDIO_TERM_TYPE_IN_PROC_ARRAY_MIC   = 0x0206,
} audio_terminal_input_type_t;

/// 2.3 - Audio Class-Output Terminal Types UAC2
typedef enum
{
  AUDIO_TERM_TYPE_OUT_UNDEFINED               = 0x0300,
  AUDIO_TERM_TYPE_OUT_GENERIC_SPEAKER         = 0x0301,
  AUDIO_TERM_TYPE_OUT_HEADPHONES              = 0x0302,
  AUDIO_TERM_TYPE_OUT_HEAD_MNT_DISP_AUIDO     = 0x0303,
  AUDIO_TERM_TYPE_OUT_DESKTOP_SPEAKER         = 0x0304,
  AUDIO_TERM_TYPE_OUT_ROOM_SPEAKER            = 0x0305,
  AUDIO_TERM_TYPE_OUT_COMMUNICATION_SPEAKER   = 0x0306,
  AUDIO_TERM_TYPE_OUT_LOW_FRQ_EFFECTS_SPEAKER = 0x0307,
} audio_terminal_output_type_t;

/// Rest is yet to be implemented

/// Additional Audio Device Class Codes - Source: Audio Data Formats

/// A.1 - Audio Class-Format Type Codes UAC2
typedef enum
{
  AUDIO_FORMAT_TYPE_UNDEFINED     = 0x00,
  AUDIO_FORMAT_TYPE_I             = 0x01,
  AUDIO_FORMAT_TYPE_II            = 0x02,
  AUDIO_FORMAT_TYPE_III           = 0x03,
  AUDIO_FORMAT_TYPE_IV            = 0x04,
  AUDIO_EXT_FORMAT_TYPE_I         = 0x81,
  AUDIO_EXT_FORMAT_TYPE_II        = 0x82,
  AUDIO_EXT_FORMAT_TYPE_III       = 0x83,
} audio_format_type_t;

/// A.2.1 - Audio Class-Audio Data Format Type I UAC2
typedef enum
{
  AUDIO_DATA_FORMAT_TYPE_I_PCM            = (1 << 0),
  AUDIO_DATA_FORMAT_TYPE_I_PCM8           = (1 << 1),
  AUDIO_DATA_FORMAT_TYPE_I_IEEE_FLOAT     = (1 << 2),
  AUDIO_DATA_FORMAT_TYPE_I_ALAW           = (1 << 3),
  AUDIO_DATA_FORMAT_TYPE_I_MULAW          = (1 << 4),
  AUDIO_DATA_FORMAT_TYPE_I_RAW_DATA       = 0x80000000,
} audio_data_format_type_I_t;

/// All remaining definitions are taken from the descriptor descriptions in the UAC2 main specification

/// Audio Class-Control Values UAC2
typedef enum
{
  AUDIO_CTRL_NONE     = 0x00,         ///< No Host access
  AUDIO_CTRL_R        = 0x01,         ///< Host read access only
  AUDIO_CTRL_RW       = 0x03,         ///< Host read write access
} audio_control_t;

/// Audio Class-Specific AC Interface Descriptor Controls UAC2
typedef enum
{
  AUDIO_CS_AS_INTERFACE_CTRL_LATENCY_POS  = 0,
} audio_cs_ac_interface_control_pos_t;

/// Audio Class-Specific AS Interface Descriptor Controls UAC2
typedef enum
{
  AUDIO_CS_AS_INTERFACE_CTRL_ACTIVE_ALT_SET_POS   = 0,
  AUDIO_CS_AS_INTERFACE_CTRL_VALID_ALT_SET_POS    = 2,
} audio_cs_as_interface_control_pos_t;

/// Audio Class-Specific AS Isochronous Data EP Attributes UAC2
typedef enum
{
  AUDIO_CS_AS_ISO_DATA_EP_ATT_MAX_PACKETS_ONLY    = 0x80,
  AUDIO_CS_AS_ISO_DATA_EP_ATT_NON_MAX_PACKETS_OK  = 0x00,
} audio_cs_as_iso_data_ep_attribute_t;

/// Audio Class-Specific AS Isochronous Data EP Controls UAC2
typedef enum
{
  AUDIO_CS_AS_ISO_DATA_EP_CTRL_PITCH_POS          = 0,
  AUDIO_CS_AS_ISO_DATA_EP_CTRL_DATA_OVERRUN_POS   = 2,
  AUDIO_CS_AS_ISO_DATA_EP_CTRL_DATA_UNDERRUN_POS  = 4,
} audio_cs_as_iso_data_ep_control_pos_t;

/// Audio Class-Specific AS Isochronous Data EP Lock Delay Units UAC2
typedef enum
{
  AUDIO_CS_AS_ISO_DATA_EP_LOCK_DELAY_UNIT_UNDEFINED       = 0x00,
  AUDIO_CS_AS_ISO_DATA_EP_LOCK_DELAY_UNIT_MILLISEC        = 0x01,
  AUDIO_CS_AS_ISO_DATA_EP_LOCK_DELAY_UNIT_PCM_SAMPLES     = 0x02,
} audio_cs_as_iso_data_ep_lock_delay_unit_t;

/// Audio Class-Clock Source Attributes UAC2
typedef enum
{
  AUDIO_CLOCK_SOURCE_ATT_EXT_CLK      = 0x00,
  AUDIO_CLOCK_SOURCE_ATT_INT_FIX_CLK  = 0x01,
  AUDIO_CLOCK_SOURCE_ATT_INT_VAR_CLK  = 0x02,
  AUDIO_CLOCK_SOURCE_ATT_INT_PRO_CLK  = 0x03,
  AUDIO_CLOCK_SOURCE_ATT_CLK_SYC_SOF  = 0x04,
} audio_clock_source_attribute_t;

/// Audio Class-Clock Source Controls UAC2
typedef enum
{
  AUDIO_CLOCK_SOURCE_CTRL_CLK_FRQ_POS     = 0,
  AUDIO_CLOCK_SOURCE_CTRL_CLK_VAL_POS     = 2,
} audio_clock_source_control_pos_t;

/// Audio Class-Clock Selector Controls UAC2
typedef enum
{
  AUDIO_CLOCK_SELECTOR_CTRL_POS   = 0,
} audio_clock_selector_control_pos_t;

/// Audio Class-Clock Multiplier Controls UAC2
typedef enum
{
  AUDIO_CLOCK_MULTIPLIER_CTRL_NUMERATOR_POS       = 0,
  AUDIO_CLOCK_MULTIPLIER_CTRL_DENOMINATOR_POS     = 2,
} audio_clock_multiplier_control_pos_t;

/// Audio Class-Input Terminal Controls UAC2
typedef enum
{
  AUDIO_IN_TERM_CTRL_CPY_PROT_POS     = 0,
  AUDIO_IN_TERM_CTRL_CONNECTOR_POS    = 2,
  AUDIO_IN_TERM_CTRL_OVERLOAD_POS     = 4,
  AUDIO_IN_TERM_CTRL_CLUSTER_POS      = 6,
  AUDIO_IN_TERM_CTRL_UNDERFLOW_POS    = 8,
  AUDIO_IN_TERM_CTRL_OVERFLOW_POS     = 10,
} audio_terminal_input_control_pos_t;

/// Audio Class-Output Terminal Controls UAC2
typedef enum
{
  AUDIO_OUT_TERM_CTRL_CPY_PROT_POS    = 0,
  AUDIO_OUT_TERM_CTRL_CONNECTOR_POS   = 2,
  AUDIO_OUT_TERM_CTRL_OVERLOAD_POS    = 4,
  AUDIO_OUT_TERM_CTRL_UNDERFLOW_POS   = 6,
  AUDIO_OUT_TERM_CTRL_OVERFLOW_POS    = 8,
} audio_terminal_output_control_pos_t;

/// Audio Class-Feature Unit Controls UAC2
typedef enum
{
  AUDIO_FEATURE_UNIT_CTRL_MUTE_POS            = 0,
  AUDIO_FEATURE_UNIT_CTRL_VOLUME_POS          = 2,
  AUDIO_FEATURE_UNIT_CTRL_BASS_POS            = 4,
  AUDIO_FEATURE_UNIT_CTRL_MID_POS             = 6,
  AUDIO_FEATURE_UNIT_CTRL_TREBLE_POS          = 8,
  AUDIO_FEATURE_UNIT_CTRL_GRAPHIC_EQU_POS     = 10,
  AUDIO_FEATURE_UNIT_CTRL_AGC_POS             = 12,
  AUDIO_FEATURE_UNIT_CTRL_DELAY_POS           = 14,
  AUDIO_FEATURE_UNIT_CTRL_BASS_BOOST_POS      = 16,
  AUDIO_FEATURE_UNIT_CTRL_LOUDNESS_POS        = 18,
  AUDIO_FEATURE_UNIT_CTRL_INPUT_GAIN_POS      = 20,
  AUDIO_FEATURE_UNIT_CTRL_INPUT_GAIN_PAD_POS  = 22,
  AUDIO_FEATURE_UNIT_CTRL_PHASE_INV_POS       = 24,
  AUDIO_FEATURE_UNIT_CTRL_UNDERFLOW_POS       = 26,
  AUDIO_FEATURE_UNIT_CTRL_OVERFLOW_POS        = 28,
} audio_feature_unit_control_pos_t;

/// Audio Class-Audio Channel Configuration UAC2
typedef enum
{
  AUDIO_CHANNEL_CONFIG_NON_PREDEFINED             = 0x00000000,
  AUDIO_CHANNEL_CONFIG_FRONT_LEFT                 = 0x00000001,
  AUDIO_CHANNEL_CONFIG_FRONT_RIGHT                = 0x00000002,
  AUDIO_CHANNEL_CONFIG_FRONT_CENTER               = 0x00000004,
  AUDIO_CHANNEL_CONFIG_LOW_FRQ_EFFECTS            = 0x00000008,
  AUDIO_CHANNEL_CONFIG_BACK_LEFT                  = 0x00000010,
  AUDIO_CHANNEL_CONFIG_BACK_RIGHT                 = 0x00000020,
  AUDIO_CHANNEL_CONFIG_FRONT_LEFT_OF_CENTER       = 0x00000040,
  AUDIO_CHANNEL_CONFIG_FRONT_RIGHT_OF_CENTER      = 0x00000080,
  AUDIO_CHANNEL_CONFIG_BACK_CENTER                = 0x00000100,
  AUDIO_CHANNEL_CONFIG_SIDE_LEFT                  = 0x00000200,
  AUDIO_CHANNEL_CONFIG_SIDE_RIGHT                 = 0x00000400,
  AUDIO_CHANNEL_CONFIG_TOP_CENTER                 = 0x00000800,
  AUDIO_CHANNEL_CONFIG_TOP_FRONT_LEFT             = 0x00001000,
  AUDIO_CHANNEL_CONFIG_TOP_FRONT_CENTER           = 0x00002000,
  AUDIO_CHANNEL_CONFIG_TOP_FRONT_RIGHT            = 0x00004000,
  AUDIO_CHANNEL_CONFIG_TOP_BACK_LEFT              = 0x00008000,
  AUDIO_CHANNEL_CONFIG_TOP_BACK_CENTER            = 0x00010000,
  AUDIO_CHANNEL_CONFIG_TOP_BACK_RIGHT             = 0x00020000,
  AUDIO_CHANNEL_CONFIG_TOP_FRONT_LEFT_OF_CENTER   = 0x00040000,
  AUDIO_CHANNEL_CONFIG_TOP_FRONT_RIGHT_OF_CENTER  = 0x00080000,
  AUDIO_CHANNEL_CONFIG_LEFT_LOW_FRQ_EFFECTS       = 0x00100000,
  AUDIO_CHANNEL_CONFIG_RIGHT_LOW_FRQ_EFFECTS      = 0x00200000,
  AUDIO_CHANNEL_CONFIG_TOP_SIDE_LEFT              = 0x00400000,
  AUDIO_CHANNEL_CONFIG_TOP_SIDE_RIGHT             = 0x00800000,
  AUDIO_CHANNEL_CONFIG_BOTTOM_CENTER              = 0x01000000,
  AUDIO_CHANNEL_CONFIG_BACK_LEFT_OF_CENTER        = 0x02000000,
  AUDIO_CHANNEL_CONFIG_BACK_RIGHT_OF_CENTER       = 0x04000000,
  AUDIO_CHANNEL_CONFIG_RAW_DATA                   = 0x80000000,
} audio_channel_config_t;

enum
{
  VOLUME_CTRL_0_DB = 0,
  VOLUME_CTRL_10_DB = 2560,
  VOLUME_CTRL_20_DB = 5120,
  VOLUME_CTRL_30_DB = 7680,
  VOLUME_CTRL_40_DB = 10240,
  VOLUME_CTRL_50_DB = 12800,
  VOLUME_CTRL_60_DB = 15360,
  VOLUME_CTRL_70_DB = 17920,
  VOLUME_CTRL_80_DB = 20480,
  VOLUME_CTRL_90_DB = 23040,
  VOLUME_CTRL_100_DB = 25600,
  VOLUME_CTRL_SILENCE = 0x8000,
};


// 5.2.2 Control Request Layout
typedef struct __attribute__ ((packed))
{
    union
    {
        struct __attribute__ ((packed))
        {
            uint8_t recipient :  5; ///< Recipient type tusb_request_recipient_t.
            uint8_t type      :  2; ///< Request type tusb_request_type_t.
            uint8_t direction :  1; ///< Direction type. tusb_dir_t
        } bmRequestType_bit;

        uint8_t bmRequestType;
    };

    uint8_t bRequest;  ///< Request type audio_cs_req_t
    uint8_t bChannelNumber;
    uint8_t bControlSelector;
    union
    {
        uint8_t bInterface;
        uint8_t bEndpoint;
    };
    uint8_t bEntityID;
    uint16_t wLength;
} audio_control_request_t;

// 5.2.3.1 1-byte Control CUR Parameter Block
typedef struct __attribute__ ((packed))
{
  int8_t bCur               ;   ///< The setting for the CUR attribute of the addressed Control
} audio_control_cur_1_t;

// 5.2.3.3 4-byte Control CUR Parameter Block
typedef struct __attribute__ ((packed))
{
  int32_t bCur              ;   ///< The setting for the CUR attribute of the addressed Control
} audio_control_cur_4_t;

// 5.2.3.3 4-byte Control RANGE Parameter Block
#define audio_control_range_4_n_t(numSubRanges) \
    struct __attribute__ ((packed)) {                     \
  uint16_t wNumSubRanges;                       \
  struct __attribute__ ((packed)) {                       \
      int32_t bMin          ; /*The setting for the MIN attribute of the nth subrange of the addressed Control*/\
    int32_t bMax            ; /*The setting for the MAX attribute of the nth subrange of the addressed Control*/\
    uint32_t bRes           ; /*The setting for the RES attribute of the nth subrange of the addressed Control*/\
    } subrange[numSubRanges];                   \
}

/// 5.2.3.2 2-byte Control RANGE Parameter Block
#define audio_control_range_2_n_t(numSubRanges) \
    struct __attribute__ ((packed)) {                     \
  uint16_t wNumSubRanges;                       \
  struct __attribute__ ((packed)) {                       \
      int16_t bMin          ; /*The setting for the MIN attribute of the nth subrange of the addressed Control*/\
    int16_t bMax            ; /*The setting for the MAX attribute of the nth subrange of the addressed Control*/\
    uint16_t bRes           ; /*The setting for the RES attribute of the nth subrange of the addressed Control*/\
    } subrange[numSubRanges];                   \
}

// 5.2.3.2 2-byte Control CUR Parameter Block
typedef struct __attribute__ ((packed))
{
  int16_t bCur              ;   ///< The setting for the CUR attribute of the addressed Control
} audio_control_cur_2_t;




// Bodge for Linux for testing
#define DISREGARD_ACTIVE_PAIRING    1

#define USB_DESC_DEVICE_LEN         9
#define USB_DESC_CONFIGURATION_LEN  8

#define ITF_NUM_AUDIO_STREAMING_CONTROL    0
#define ITF_NUM_AUDIO_STREAMING_SPEAKER    1
#define ITF_NUM_AUDIO_STREAMING_MICROPHONE 2
#define AUDIO_FUNCTION_UNIT_ID      2



#define USBD_XFER_ISOCHRONOUS 1
#define USBD_ISO_EP_ATT_ADAPTIVE 0x08
#define USBD_ISO_EP_ATT_DATA 0
#define USBD_ISO_EP_ATT_ASYNCHRONOUS 0x04

#define USB_CLASS_AUDIO 1
#define AUDIO_FUNCTION_SUBCLASS_UNDEFINED 0
#define AUDIO_FUNC_PROTOCOL_CODE_V2 0x20

#define USBD_DESC_INTERFACE     0x04
#define USBD_DESC_CS_INTERFACE  0x24
#define USBD_DESC_ENDPOINT 0x05
#define USBD_DESC_CS_ENDPOINT 0x25

#define CFG_USBD_AUDIO_FUNC_1_N_CHANNELS_TX  2
#define CFG_USBD_AUDIO_FUNC_1_N_CHANNELS_RX  2


#define CFG_USBD_AUDIO_FUNC_1_MAX_SAMPLE_RATE 48000

// 16bit in 16bit slots
#define CFG_USBD_AUDIO_FUNC_1_FORMAT_1_N_BYTES_PER_SAMPLE_TX          2
#define CFG_USBD_AUDIO_FUNC_1_FORMAT_1_RESOLUTION_TX                  16
#define CFG_USBD_AUDIO_FUNC_1_FORMAT_1_N_BYTES_PER_SAMPLE_RX          2
#define CFG_USBD_AUDIO_FUNC_1_FORMAT_1_RESOLUTION_RX                  16

#define CFG_USBD_AUDIO_FUNC_1_FORMAT_2_N_BYTES_PER_SAMPLE_TX          4
#define CFG_USBD_AUDIO_FUNC_1_FORMAT_2_RESOLUTION_TX                  24
#define CFG_USBD_AUDIO_FUNC_1_FORMAT_2_N_BYTES_PER_SAMPLE_RX          4
#define CFG_USBD_AUDIO_FUNC_1_FORMAT_2_RESOLUTION_RX                  24


#define TU_U16(_high, _low)   ((uint16_t) (((_high) << 8) | (_low)))
#define TU_U16_HIGH(_u16)     ((uint8_t) (((_u16) >> 8) & 0x00ff))
#define TU_U16_LOW(_u16)      ((uint8_t) ((_u16)       & 0x00ff))
#define U16_TO_U8S_BE(_u16)   TU_U16_HIGH(_u16), TU_U16_LOW(_u16)
#define U16_TO_U8S_LE(_u16)   TU_U16_LOW(_u16), TU_U16_HIGH(_u16)

#define TU_U32_BYTE3(_u32)    ((uint8_t) ((((uint32_t) _u32) >> 24) & 0x000000ff)) // MSB
#define TU_U32_BYTE2(_u32)    ((uint8_t) ((((uint32_t) _u32) >> 16) & 0x000000ff))
#define TU_U32_BYTE1(_u32)    ((uint8_t) ((((uint32_t) _u32) >>  8) & 0x000000ff))
#define TU_U32_BYTE0(_u32)    ((uint8_t) (((uint32_t)  _u32)        & 0x000000ff)) // LSB

#define U32_TO_U8S_BE(_u32)   TU_U32_BYTE3(_u32), TU_U32_BYTE2(_u32), TU_U32_BYTE1(_u32), TU_U32_BYTE0(_u32)
#define U32_TO_U8S_LE(_u32)   TU_U32_BYTE0(_u32), TU_U32_BYTE1(_u32), TU_U32_BYTE2(_u32), TU_U32_BYTE3(_u32)

#define TU_BIT(n)             (1UL << (n))

// Generate a mask with bit from high (31) to low (0) set, e.g TU_GENMASK(3, 0) = 0b1111
#define TU_GENMASK(h, l)      ( (UINT32_MAX << (l)) & (UINT32_MAX >> (31 - (h))) )

#define USBD_AUDIO_DESC_CS_AC_LEN_CONTENT_LEN (USBD_AUDIO_DESC_CLK_SRC_LEN+USBD_AUDIO_DESC_FEATURE_UNIT_TWO_CHANNEL_LEN+USBD_AUDIO_DESC_INPUT_TERM_LEN+USBD_AUDIO_DESC_OUTPUT_TERM_LEN+USBD_AUDIO_DESC_INPUT_TERM_LEN+USBD_AUDIO_DESC_OUTPUT_TERM_LEN)

#define USBD_AUDIO_EP_SIZE(_maxFrequency, _nBytesPerSample, _nChannels) ((((_maxFrequency + 999) / 1000) + 1) * _nBytesPerSample * _nChannels)

#define USBD_AUDIO_HEADSET_STEREO_DESC_LEN (USBD_AUDIO_DESC_STD_AC_LEN\
    + USBD_AUDIO_DESC_CS_AC_LEN\
    + USBD_AUDIO_DESC_CLK_SRC_LEN\
    + USBD_AUDIO_DESC_INPUT_TERM_LEN\
    + USBD_AUDIO_DESC_FEATURE_UNIT_TWO_CHANNEL_LEN\
    + USBD_AUDIO_DESC_OUTPUT_TERM_LEN\
    + USBD_AUDIO_DESC_INPUT_TERM_LEN\
    + USBD_AUDIO_DESC_OUTPUT_TERM_LEN\
    /* Interface 1, Alternate 0 */\
    + USBD_AUDIO_DESC_STD_AS_INT_LEN\
    /* Interface 1, Alternate 0 */\
    + USBD_AUDIO_DESC_STD_AS_INT_LEN\
    + USBD_AUDIO_DESC_CS_AS_INT_LEN\
    + USBD_AUDIO_DESC_TYPE_I_FORMAT_LEN\
    + USBD_AUDIO_DESC_STD_AS_ISO_EP_LEN\
    + USBD_AUDIO_DESC_CS_AS_ISO_EP_LEN\
    /* Interface 1, Alternate 2 */\
    + USBD_AUDIO_DESC_STD_AS_INT_LEN\
    + USBD_AUDIO_DESC_CS_AS_INT_LEN\
    + USBD_AUDIO_DESC_TYPE_I_FORMAT_LEN\
    + USBD_AUDIO_DESC_STD_AS_ISO_EP_LEN\
    + USBD_AUDIO_DESC_CS_AS_ISO_EP_LEN\
    /* Interface 2, Alternate 0 */\
    + USBD_AUDIO_DESC_STD_AS_INT_LEN\
    /* Interface 2, Alternate 1 */\
    + USBD_AUDIO_DESC_STD_AS_INT_LEN\
    + USBD_AUDIO_DESC_CS_AS_INT_LEN\
    + USBD_AUDIO_DESC_TYPE_I_FORMAT_LEN\
    + USBD_AUDIO_DESC_STD_AS_ISO_EP_LEN\
    + USBD_AUDIO_DESC_CS_AS_ISO_EP_LEN\
    /* Interface 2, Alternate 2 */\
    + USBD_AUDIO_DESC_STD_AS_INT_LEN\
    + USBD_AUDIO_DESC_CS_AS_INT_LEN\
    + USBD_AUDIO_DESC_TYPE_I_FORMAT_LEN\
    + USBD_AUDIO_DESC_STD_AS_ISO_EP_LEN\
    + USBD_AUDIO_DESC_CS_AS_ISO_EP_LEN)

    
// Unit numbers are arbitrary selected
#define UAC2_ENTITY_CLOCK               0x04

// Speaker path
#define UAC2_ENTITY_SPK_INPUT_TERMINAL  0x01
#define UAC2_ENTITY_SPK_FEATURE_UNIT    0x02
#define UAC2_ENTITY_SPK_OUTPUT_TERMINAL 0x03

// Microphone path
#define UAC2_ENTITY_MIC_INPUT_TERMINAL  0x11
#define UAC2_ENTITY_MIC_OUTPUT_TERMINAL 0x13

/* Standard Interface Association Descriptor (IAD) */
#define USBD_AUDIO_DESC_IAD_LEN 8
#define USBD_AUDIO_DESC_IAD(_firstitf, _nitfs, _stridx) \
  USB_AUDIO_DESC_IAD_LEN, USB_DESC_INTERFACE_ASSOCIATION, _firstitf, _nitfs, USB_CLASS_AUDIO, AUDIO_FUNCTION_SUBCLASS_UNDEFINED, AUDIO_FUNC_PROTOCOL_CODE_V2, _stridx

/* Standard AC Interface Descriptor(4.7.1) */
#define USBD_AUDIO_DESC_STD_AC_LEN 9
#define USBD_AUDIO_DESC_STD_AC(_itfnum, _nEPs, _stridx) /* _nEPs is 0 or 1 */\
  USBD_AUDIO_DESC_STD_AC_LEN, USBD_DESC_INTERFACE, _itfnum, /* fixed to zero */ 0x00, _nEPs, USB_CLASS_AUDIO, AUDIO_SUBCLASS_CONTROL, AUDIO_INT_PROTOCOL_CODE_V2, _stridx

/* Class-Specific AC Interface Header Descriptor(4.7.2) */
#define USBD_AUDIO_DESC_CS_AC_LEN 9
#define USBD_AUDIO_DESC_CS_AC(_bcdADC, _category, _totallen, _ctrl) /* _bcdADC : Audio Device Class Specification Release Number in Binary-Coded Decimal, _category : see audio_function_t, _totallen : Total number of bytes returned for the class-specific AudioControl interface i.e. Clock Source, Unit and Terminal descriptors - Do not include USBD_AUDIO_DESC_CS_AC_LEN, we already do this here*/ \
  USBD_AUDIO_DESC_CS_AC_LEN, USBD_DESC_CS_INTERFACE, AUDIO_CS_AC_INTERFACE_HEADER, U16_TO_U8S_LE(_bcdADC), _category, U16_TO_U8S_LE(_totallen + USBD_AUDIO_DESC_CS_AC_LEN), _ctrl

/* Clock Source Descriptor(4.7.2.1) */
#define USBD_AUDIO_DESC_CLK_SRC_LEN 8
#define USBD_AUDIO_DESC_CLK_SRC(_clkid, _attr, _ctrl, _assocTerm, _stridx) \
  USBD_AUDIO_DESC_CLK_SRC_LEN, USBD_DESC_CS_INTERFACE, AUDIO_CS_AC_INTERFACE_CLOCK_SOURCE, _clkid, _attr, _ctrl, _assocTerm, _stridx

/* Input Terminal Descriptor(4.7.2.4) */
#define USBD_AUDIO_DESC_INPUT_TERM_LEN 17
#define USBD_AUDIO_DESC_INPUT_TERM(_termid, _termtype, _assocTerm, _clkid, _nchannelslogical, _channelcfg, _idxchannelnames, _ctrl, _stridx) \
  USBD_AUDIO_DESC_INPUT_TERM_LEN, USBD_DESC_CS_INTERFACE, AUDIO_CS_AC_INTERFACE_INPUT_TERMINAL, _termid, U16_TO_U8S_LE(_termtype), _assocTerm, _clkid, _nchannelslogical, U32_TO_U8S_LE(_channelcfg), _idxchannelnames, U16_TO_U8S_LE(_ctrl), _stridx

/* Output Terminal Descriptor(4.7.2.5) */
#define USBD_AUDIO_DESC_OUTPUT_TERM_LEN 12
#define USBD_AUDIO_DESC_OUTPUT_TERM(_termid, _termtype, _assocTerm, _srcid, _clkid, _ctrl, _stridx) \
  USBD_AUDIO_DESC_OUTPUT_TERM_LEN, USBD_DESC_CS_INTERFACE, AUDIO_CS_AC_INTERFACE_OUTPUT_TERMINAL, _termid, U16_TO_U8S_LE(_termtype), _assocTerm, _srcid, _clkid, U16_TO_U8S_LE(_ctrl), _stridx

// 2 - Channels
#define USBD_AUDIO_DESC_FEATURE_UNIT_TWO_CHANNEL_LEN (6+(2+1)*4)
#define USBD_AUDIO_DESC_FEATURE_UNIT_TWO_CHANNEL(_unitid, _srcid, _ctrlch0master, _ctrlch1, _ctrlch2, _stridx) \
		USBD_AUDIO_DESC_FEATURE_UNIT_TWO_CHANNEL_LEN, USBD_DESC_CS_INTERFACE, AUDIO_CS_AC_INTERFACE_FEATURE_UNIT, _unitid, _srcid, U32_TO_U8S_LE(_ctrlch0master), U32_TO_U8S_LE(_ctrlch1), U32_TO_U8S_LE(_ctrlch2), _stridx

// 4 - Channels
#define USBD_AUDIO_DESC_FEATURE_UNIT_FOUR_CHANNEL_LEN (6+(4+1)*4)
#define USBD_AUDIO_DESC_FEATURE_UNIT_FOUR_CHANNEL(_unitid, _srcid, _ctrlch0master, _ctrlch1, _ctrlch2, _ctrlch3, _ctrlch4, _stridx) \
                    USBD_AUDIO_DESC_FEATURE_UNIT_FOUR_CHANNEL_LEN, TUSB_DESC_CS_INTERFACE, AUDIO_CS_AC_INTERFACE_FEATURE_UNIT, _unitid, _srcid, U32_TO_U8S_LE(_ctrlch0master), U32_TO_U8S_LE(_ctrlch1), U32_TO_U8S_LE(_ctrlch2), U32_TO_U8S_LE(_ctrlch3), U32_TO_U8S_LE(_ctrlch4), _stridx

/* Standard AS Interface Descriptor(4.9.1) */
#define USBD_AUDIO_DESC_STD_AS_INT_LEN 9
#define USBD_AUDIO_DESC_STD_AS_INT(_itfnum, _altset, _nEPs, _stridx) \
  USBD_AUDIO_DESC_STD_AS_INT_LEN, USBD_DESC_INTERFACE, _itfnum, _altset, _nEPs, USB_CLASS_AUDIO, AUDIO_SUBCLASS_STREAMING, AUDIO_INT_PROTOCOL_CODE_V2, _stridx

/* Class-Specific AS Interface Descriptor(4.9.2) */
#define USBD_AUDIO_DESC_CS_AS_INT_LEN 16
#define USBD_AUDIO_DESC_CS_AS_INT(_termid, _ctrl, _formattype, _formats, _nchannelsphysical, _channelcfg, _stridx) \
  USBD_AUDIO_DESC_CS_AS_INT_LEN, USBD_DESC_CS_INTERFACE, AUDIO_CS_AS_INTERFACE_AS_GENERAL, _termid, _ctrl, _formattype, U32_TO_U8S_LE(_formats), _nchannelsphysical, U32_TO_U8S_LE(_channelcfg), _stridx

/* Type I Format Type Descriptor(2.3.1.6 - Audio Formats) */
#define USBD_AUDIO_DESC_TYPE_I_FORMAT_LEN 6
#define USBD_AUDIO_DESC_TYPE_I_FORMAT(_subslotsize, _bitresolution) /* _subslotsize is number of bytes per sample (i.e. subslot) and can be 1,2,3, or 4 */\
  USBD_AUDIO_DESC_TYPE_I_FORMAT_LEN, USBD_DESC_CS_INTERFACE, AUDIO_CS_AS_INTERFACE_FORMAT_TYPE, AUDIO_FORMAT_TYPE_I, _subslotsize, _bitresolution

/* Standard AS Isochronous Audio Data Endpoint Descriptor(4.10.1.1) */
#define USBD_AUDIO_DESC_STD_AS_ISO_EP_LEN 7
#define USBD_AUDIO_DESC_STD_AS_ISO_EP(_ep, _attr, _maxEPsize, _interval) \
  USBD_AUDIO_DESC_STD_AS_ISO_EP_LEN, USBD_DESC_ENDPOINT, _ep, _attr, U16_TO_U8S_LE(_maxEPsize), _interval

/* Class-Specific AS Isochronous Audio Data Endpoint Descriptor(4.10.1.2) */
#define USBD_AUDIO_DESC_CS_AS_ISO_EP_LEN 8
#define USBD_AUDIO_DESC_CS_AS_ISO_EP(_attr, _ctrl, _lockdelayunit, _lockdelay) \
  USBD_AUDIO_DESC_CS_AS_ISO_EP_LEN, USBD_DESC_CS_ENDPOINT, AUDIO_CS_EP_SUBTYPE_GENERAL, _attr, _ctrl, _lockdelayunit, U16_TO_U8S_LE(_lockdelay)




/*===========================================================================*/
/* Driver constants.                                                         */
/*===========================================================================*/

#define MAX_AUDIO_SAMPLING_FREQUENCY    96000U
#define MAX_AUDIO_RESOLUTION            32U
#define MAX_AUDIO_CHANNELS              2U
#define MAX_AUDIO_SAMPLES_PER_FRAME     (MAX_AUDIO_SAMPLING_FREQUENCY / 1000)
#define MAX_AUDIO_PACKET_SIZE           (MAX_AUDIO_SAMPLES_PER_FRAME * MAX_AUDIO_CHANNELS \
                                         * MAX_AUDIO_RESOLUTION / 8)
#define AUDIO_MAX_PACKET_SIZE           (MAX_AUDIO_PACKET_SIZE)
#define ADU_AUDIO_CHANNELS              2U

#define AUDIO_EVENT                 EVENT_MASK(0)
#define AUDIO_EVENT_OUTPUT          EVENT_MASK(1)
#define AUDIO_EVENT_INPUT           EVENT_MASK(2)
#define AUDIO_EVENT_MUTE            EVENT_MASK(3)
#define AUDIO_EVENT_VOLUME          EVENT_MASK(4)
#define AUDIO_EVENT_USB_CONGIGURED  EVENT_MASK(5)
#define AUDIO_EVENT_USB_SUSPEND     EVENT_MASK(6)
#define AUDIO_EVENT_USB_WAKEUP      EVENT_MASK(7)
#define AUDIO_EVENT_USB_STALLED     EVENT_MASK(8)
#define AUDIO_EVENT_USB_RESET       EVENT_MASK(9)
#define AUDIO_EVENT_USB_ENABLE      EVENT_MASK(10)
#define AUDIO_EVENT_FORMAT          EVENT_MASK(11)


// samples to try to keep in buffer
#define TX_RING_BUFFER_UNDERFLOW_SIZE (96)

// normal ring buffer sample size
#define TX_RING_BUFFER_NORMAL_SIZE    (96*2)

// Total allocated size in samples
#define TX_RING_BUFFER_FULL_SIZE (TX_RING_BUFFER_UNDERFLOW_SIZE + TX_RING_BUFFER_NORMAL_SIZE)

// debugging defines
#define ADU_LOGGING 0
#define CODEC_METICS_MS (100)
//#define ADU_TRANSFER_LOG_SIZE 4000
//#define CHECK_USB_DATA 1
//#define ADU_OVERRUN_LOG_SIZE 2600

#define USE_TRANSFER_SAMPLE_SIZE 2
#define USE_TRANSFER_CHANNEL_SIZE 2
#define USE_TRANSFER_SAMPLES_MS 48

#define USE_TRANSFER_SIZE_SAMPLES (USE_TRANSFER_CHANNEL_SIZE * USE_TRANSFER_SAMPLES_MS)
#define USE_TRANSFER_SIZE_BYTES   (USE_TRANSFER_SAMPLE_SIZE * USE_TRANSFER_CHANNEL_SIZE * USE_TRANSFER_SAMPLES_MS)


/*===========================================================================*/
/* Driver pre-compile time settings.                                         */
/*===========================================================================*/

/**
 * @name    AUDIO_USB configuration options
 * @{
 */
/**
 * @brief   Audio USB buffers size.
 * @details Configuration parameter, the buffer size must be a multiple of
 *          the USB data endpoint maximum packet size.
 * @note    The default is 256 bytes for both the transmission and receive
 *          buffers.
 */
#if !defined(AUDIO_USB_BUFFERS_SIZE) || defined(__DOXYGEN__)
#define AUDIO_USB_BUFFERS_SIZE          (MAX_AUDIO_SAMPLES_PER_FRAME * MAX_AUDIO_CHANNELS * 4)
#endif
/** @} */

/*===========================================================================*/
/* Derived constants and error checks.                                       */
/*===========================================================================*/

#if !HAL_USE_USB || !CH_CFG_USE_QUEUES || !CH_CFG_USE_EVENTS
#error "Audio USB Driver requires HAL_USE_USB, CH_CFG_USE_QUEUES, CH_CFG_USE_EVENTS"
#endif

/*===========================================================================*/
/* Driver data structures and types.                                         */
/*===========================================================================*/

/**
 * @brief Driver state machine possible states.
 */
typedef enum {
  ADU_UNINIT = 0,                   /**< Not initialized.                   */
  ADU_STOP = 1,                     /**< Stopped.                           */
  ADU_READY = 2                     /**< Ready.                             */
} adustate_t;

/**
 * @brief   Structure representing an audio USB driver.
 */
typedef struct AudioUSBDriver AudioUSBDriver;

/**
 * @brief   Audio USB Driver configuration structure.
 * @details An instance of this structure must be passed to @p aduStart()
 *          in order to configure and start the driver operations.
 */
typedef struct {
  /**
   * @brief   USB driver to use.
   */
  USBDriver                 *usbp;
  /**
   * @brief   Bulk IN endpoint used for outgoing data transfer.
   */
  usbep_t                   iso_in;
  /**
   * @brief   Bulk OUT endpoint used for incoming data transfer.
   */
  usbep_t                   iso_out;
} AudioUSBConfig;

/**
 * @brief   @p AudioUSBDriver specific data.
 */
#define _audio_usb_driver_data                                              \
  _base_asynchronous_channel_data                                           \
  /* Driver state.*/                                                        \
  adustate_t                state;                                          \
  /* Input queue.*/                                                         \
  InputQueue                iqueue;                                         \
  /* Output queue.*/                                                        \
  OutputQueue               oqueue;                                         \
  /* Input buffer.*/                                                        \
  uint8_t                   ib[AUDIO_USB_BUFFERS_SIZE];                     \
  /* Output buffer.*/                                                       \
  uint8_t                   ob[AUDIO_USB_BUFFERS_SIZE];                     \
  /* End of the mandatory fields.*/                                         \
  /* Current configuration data.*/                                          \
  const AudioUSBConfig     *config;

/**
 * @brief   @p AudioUSBDriver specific methods.
 */
#define _audio_usb_driver_methods                                           \
  _base_asynchronous_channel_methods

/**
 * @extends BaseAsynchronousChannelVMT
 *
 * @brief   @p AudioUSBDriver virtual methods table.
 */
struct AudioUSBDriverVMT {
  _audio_usb_driver_methods
};

/**
 * @extends BaseAsynchronousChannel
 *
 * @brief   Audio driver class.
 * @details This class extends @p BaseAsynchronousChannel by adding physical
 *          I/O queues.
 */
struct AudioUSBDriver {
  /** @brief Virtual Methods Table.*/
  const struct AudioUSBDriverVMT *vmt;
  _audio_usb_driver_data
};

typedef enum _adustate
{
  asNeedsReset,
  asInit,
  asFillingUnderflow,
  asNormal,
  asCodecRemove,
  asCodecDuplicate,
} ADUState;


typedef struct 
{
  uint_fast32_t currentSampleRate;
  int_fast8_t   mute[ADU_AUDIO_CHANNELS + 1];
  int_fast16_t  volume[ADU_AUDIO_CHANNELS + 1];
  bool          isOutputActive;
  bool          isInputActive;
  size_t        lastTransferSize;
  uint_fast16_t codecFrameSampleCount;
  uint_fast32_t currentFrame;
  uint_fast32_t lastOverunFrame;
  uint_fast32_t sampleAdjustEveryFrame;
  uint_fast32_t sampleAdjustFrameCounter;
  int_fast16_t  sampleOffset;
  int_fast16_t  codecMetricsSampleOffset;
  int_fast16_t  codecMetricsBlocksOkCount;

  uint_fast16_t txRingBufferWriteOffset;
  uint_fast16_t txRingBufferReadOffset;
  uint_fast16_t txRingBufferUsedSize;

  uint_fast16_t rxRingBufferWriteOffset;
  uint_fast16_t rxRingBufferReadOffset;
  uint_fast16_t rxRingBufferUsedSize;

  ADUState      state;
} AduState;

/*===========================================================================*/
/* Driver macros.                                                            */
/*===========================================================================*/

/*===========================================================================*/
/* External declarations.                                                    */
/*===========================================================================*/
extern AduState aduState;

void aduEnableInput(USBDriver *usbp, bool bEnable);
void aduEnableOutput(USBDriver *usbp, bool bEnable);
void aduDataTransmitted(USBDriver *usbp, usbep_t ep); 
void aduDataReceived(USBDriver *usbp, usbep_t ep);
void aduInitiateReceiveI(USBDriver *usbp);
void aduInitiateTransmitI(USBDriver *usbp);
void aduResetBuffers(void);
void aduReset(void);


#ifdef __cplusplus
extern "C" {
#endif
  void   aduInit(void);
  void   aduStart(AudioUSBDriver *adup, const AudioUSBConfig *config);
  void   aduStop(AudioUSBDriver *adup);
  void   aduConfigureHookI(AudioUSBDriver *adup);
  bool_t aduRequestsHook(USBDriver *usbp);
  void   aduDataTransmitted(USBDriver *usbp, usbep_t ep);
  void   aduDataReceived(USBDriver *usbp, usbep_t ep);
  void   aduSofHookI(AudioUSBDriver *adup);

  bool   aduControl(USBDriver *usbp);
  bool   aduSwitchInterface(USBDriver *usbp, uint8_t iface, uint8_t entity, uint8_t req, uint16_t wValue, uint16_t length);

  bool   aduIsUsbInUse(void);
  bool   aduIsUsbOutputEnabled(void);
#ifdef __cplusplus
}

#endif

#endif // _AUDIO_USB_H_

#endif