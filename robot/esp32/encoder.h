
typedef struct {
    portMUX_TYPE mux = portMUX_INITIALIZER_UNLOCKED;
    uint8_t old_AB = 0;
    int32_t value = 0;
    int encoderAPin, encoderBPin;
} encoder_t;

const int8_t enc_states[16] = {0,-1,1,0,1,0,0,-1,-1,0,0,1,0,1,-1,0};

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



