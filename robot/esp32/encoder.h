#ifndef ENCODER_H
#define ENCODER_H

// a struct holding the encoder values
typedef struct {
    portMUX_TYPE mux = portMUX_INITIALIZER_UNLOCKED;
    uint8_t old_AB = 0;
    int32_t value = 0;
    int encoderAPin, encoderBPin;
} encoder_t;

// constant look-up table used by the ISR
const int8_t enc_states[16] = {0,-1,1,0,1,0,0,-1,-1,0,0,1,0,1,-1,0};

// theese are defined in encoder.cpp
extern encoder_t encLeft, encRight;

void initEncoders();

#endif ENCODER_H
