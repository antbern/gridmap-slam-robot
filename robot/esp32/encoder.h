#ifndef ENCODER_H
#define ENCODER_H

#include <driver/pcnt.h>

#define PCNT_MAX_VAL  (32767)
#define PCNT_MIN_VAL (-32768)

// a struct holding the encoder values
typedef struct {
    int encoderAPin, encoderBPin;
    pcnt_unit_t pcnt_unit;
    pcnt_channel_t pcnt_channel;

} encoder_t;

// theese are defined in encoder.cpp
extern encoder_t encLeft, encRight;

void initEncoders();
void resetEncoder(encoder_t* enc);
int16_t readEncoder(encoder_t* enc);
int16_t readAndResetEncoder(encoder_t* enc);

#endif // ENCODER_H
