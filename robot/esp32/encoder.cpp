#include "Arduino.h"

#include "encoder.h"
#include "pins.h"

// define variables here
encoder_t encLeft, encRight;

// Adapted from: https://github.com/espressif/esp-idf/blob/master/examples/peripherals/pcnt/main/pcnt_example_main.c
void init_pcnt(encoder_t *enc) {

    pinMode(enc->encoderAPin, INPUT);
	pinMode(enc->encoderBPin, INPUT);

    pcnt_config_t pcnt_config = {
        // Set PCNT input signal and control GPIOs
        .pulse_gpio_num = enc->encoderAPin,
        .ctrl_gpio_num = enc->encoderBPin,
        
        
        // What to do when control input is low or high?
        .lctrl_mode = PCNT_MODE_REVERSE, // Reverse counting direction if low
        .hctrl_mode = PCNT_MODE_KEEP,    // Keep the primary counter mode if high
        // What to do on the positive / negative edge of pulse input?
        .pos_mode = PCNT_COUNT_INC,   // Count up on the positive edge
        .neg_mode = PCNT_COUNT_DIS,   // Keep the counter value on the negative edge
        
        // Set the maximum and minimum limit values to watch (NOT USED!!)
        .counter_h_lim = PCNT_MAX_VAL,
        .counter_l_lim = PCNT_MIN_VAL,

        .unit = enc->pcnt_unit,
        .channel = enc->pcnt_channel,
    };

    /* Initialize PCNT unit */
    ESP_ERROR_CHECK( pcnt_unit_config(&pcnt_config) );
    
    /* Configure and enable the input filter */
    pcnt_set_filter_value(pcnt_config.unit, 100);
    pcnt_filter_enable(pcnt_config.unit);

    // lets go!
    pcnt_counter_clear(pcnt_config.unit);
    pcnt_counter_resume(pcnt_config.unit);
}

void initEncoders(){

    // set up pin definitions
    encLeft.encoderAPin = MOTOR_LEFT_ENCA;
    encLeft.encoderBPin = MOTOR_LEFT_ENCB;
    encLeft.pcnt_unit = PCNT_UNIT_0;
    encLeft.pcnt_channel = PCNT_CHANNEL_0;

    encRight.encoderAPin = MOTOR_RIGHT_ENCA;
    encRight.encoderBPin = MOTOR_RIGHT_ENCB;
    encLeft.pcnt_unit = PCNT_UNIT_1;
    encLeft.pcnt_channel = PCNT_CHANNEL_0;

    // and initialize
    init_pcnt(&encLeft);
    init_pcnt(&encRight);

}

int16_t readEncoder(encoder_t* enc){
    int16_t count;
    pcnt_get_counter_value(enc->pcnt_unit, &count);
    return count;
}

// resets the encoder
void resetEncoder(encoder_t* enc){
    pcnt_counter_clear(enc->pcnt_unit);
}

int16_t readAndResetEncoder(encoder_t* enc) {
    int16_t count;
    pcnt_get_counter_value(enc->pcnt_unit, &count);

    pcnt_counter_clear(enc->pcnt_unit);

    return count;
}