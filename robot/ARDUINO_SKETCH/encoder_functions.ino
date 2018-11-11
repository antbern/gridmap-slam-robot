
// interrupt routine called to update encoder counters
void int_encoder_left(){    
    // if the states are the same, increment; otherwise decrement
    if((ENC_PORT & _BV(LEFT_ENCA_BIT)) >> LEFT_ENCA_BIT == (ENC_PORT & _BV(LEFT_ENCB_BIT)) >> LEFT_ENCB_BIT ){
        motor_left.current_encoder_counter++;
    }else{
        motor_left.current_encoder_counter--;
    }
}

// interrupt routine called to update encoder counters
void int_encoder_right(){    
    // if the states are the same, increment; otherwise decrement
    if((ENC_PORT & _BV(RIGHT_ENCA_BIT)) >> RIGHT_ENCA_BIT == (ENC_PORT & _BV(RIGHT_ENCB_BIT)) >> RIGHT_ENCB_BIT )
        motor_right.current_encoder_counter++;
    else
        motor_right.current_encoder_counter--;

}

double getMotorRotationSpeed(motor_t* motor, double dt){

    // calculate difference in encoder counts since last time
    double position_delta = motor->current_encoder_counter - (double)motor->last_encoder_counter;

     // save current position
    motor->last_encoder_counter = motor->current_encoder_counter;

    // increase odometry counter
    motor->odometry_counter += position_delta;

    // calculate and return current speed in rad/s
    return (double) 2.0 * PI * position_delta / ENC_COUNTS_PER_REV / dt;

}

double getMotorPosition(motor_t* motor){
    return (double)2.0 * PI * motor->current_encoder_counter / ENC_COUNTS_PER_REV;
}