#include "Arduino.h"

#include "encoder.h"
#include "pins.h"

// define variables here
encoder_t encLeft, encRight;

// based on code from https://www.circuitsathome.com/mcu/reading-rotary-encoder-on-arduino/
void inline handleEncoder(encoder_t* enc){
    portENTER_CRITICAL_ISR(&(enc->mux));
        
    //remember previous state
    enc->old_AB <<= 2;                   

    // read new state
    int8_t ENC_PORT = ((digitalRead(enc->encoderBPin)) ? (1 << 1) : 0) | ((digitalRead(enc->encoderAPin)) ? (1 << 0) : 0);

    // add current state		
    enc->old_AB |= ( ENC_PORT & 0x03 );

    // increment counter
    enc->value += enc_states[( enc->old_AB & 0x0f )];

    portEXIT_CRITICAL_ISR(&(enc->mux));
}

void IRAM_ATTR readEncoderLeft_ISR() {
    handleEncoder(&encLeft);
}

void IRAM_ATTR readEncoderRight_ISR() {
    handleEncoder(&encRight);
}


void initEncoders(){

    // setup pin directions
	pinMode(MOTOR_LEFT_ENCA, INPUT);
	pinMode(MOTOR_LEFT_ENCB, INPUT);

    pinMode(MOTOR_RIGHT_ENCA, INPUT);
	pinMode(MOTOR_RIGHT_ENCB, INPUT);

    // set up pin definitions
    encLeft.encoderAPin = MOTOR_LEFT_ENCA;
    encLeft.encoderBPin = MOTOR_LEFT_ENCB;

    encRight.encoderAPin = MOTOR_RIGHT_ENCA;
    encRight.encoderBPin = MOTOR_RIGHT_ENCB;


    // attach all four interrupts
    attachInterrupt(digitalPinToInterrupt(encLeft.encoderAPin), readEncoderLeft_ISR, CHANGE);
	attachInterrupt(digitalPinToInterrupt(encLeft.encoderBPin), readEncoderLeft_ISR, CHANGE);

    attachInterrupt(digitalPinToInterrupt(encRight.encoderAPin), readEncoderRight_ISR, CHANGE);
	attachInterrupt(digitalPinToInterrupt(encRight.encoderBPin), readEncoderRight_ISR, CHANGE);
}

// resets the encoder
void resetEncoder(encoder_t* enc){
    portENTER_CRITICAL(&enc->mux);

    enc->old_AB = 0;
    enc->value = 0;

    portEXIT_CRITICAL(&enc->mux);
}