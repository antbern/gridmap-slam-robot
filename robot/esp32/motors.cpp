#include "motors.h"
#include "pid.h"
#include "pins.h"

motor_t motor_left, motor_right;
volatile bool running = false;

void initMotors(){

    ///// SETUP PINS /////
	pinMode(MOTOR_LEFT_EN, OUTPUT);	
    digitalWrite(MOTOR_LEFT_EN, HIGH);
    pinMode(MOTOR_LEFT_DIRA, OUTPUT);
    pinMode(MOTOR_LEFT_DIRB, OUTPUT);
	
    pinMode(MOTOR_RIGHT_EN, OUTPUT);
    digitalWrite(MOTOR_RIGHT_EN, HIGH);
    pinMode(MOTOR_RIGHT_DIRA, OUTPUT);
    pinMode(MOTOR_RIGHT_DIRB, OUTPUT);


    // set up motors and their PID-regulators
    motor_left.pid.Kp = PID_KP;
    motor_left.pid.Ki = PID_KI;
    motor_left.pid.Kd = PID_KD;
    motor_left.pid.Tf = PID_TF;
    motor_left.pins = {MOTOR_LEFT_EN, MOTOR_LEFT_DIRA, MOTOR_LEFT_DIRB};
    motor_left.enc = &encLeft;
	motor_left.pwm_channel = 0;

    motor_right.pid.Kp = PID_KP;
    motor_right.pid.Ki = PID_KI;
    motor_right.pid.Kd = PID_KD;
    motor_right.pid.Tf = PID_TF;
    motor_right.pins = {MOTOR_RIGHT_EN, MOTOR_RIGHT_DIRA, MOTOR_RIGHT_DIRB};
    motor_right.enc =  &encRight;
	motor_right.pwm_channel = 1;
    
    // set default speed reference
	motor_right.speed_reference = 0.0f;
	motor_left.speed_reference = 0.0f;


    // configure PWM chanels for controlling the motors
  	ledcSetup(motor_left.pwm_channel, 5000, 8);
	ledcAttachPin(motor_left.pins.PIN_EN, motor_left.pwm_channel);

	ledcSetup(motor_right.pwm_channel, 5000, 8);
	ledcAttachPin(motor_right.pins.PIN_EN, motor_right.pwm_channel);

}

void resetMotors() {
    // reset speed reference and the speed PIDs
    motor_left.speed_reference = 0;
    motor_right.speed_reference = 0;
    reset_pid(&motor_left.pid);
    reset_pid(&motor_right.pid);

    // stop motors
    actuate_motor(&motor_left, 0);
    actuate_motor(&motor_right, 0);
}

void stopMotorLoop(){
    running = false;
}

void* motorLoop(void* parameter) {
    // initialize the time counter
	unsigned long last_timer_us = micros();	
    
    unsigned long timer_us = 0;
    double h = 0;

    resetMotors();

    // discard first speed readings as they may be erroneous
    getMotorRotationSpeed(&motor_left, 0.1);
    getMotorRotationSpeed(&motor_right, 0.1);

    running = true;
	while(running){
		// get current time
		timer_us = micros();

		// calculate elapsed time in seconds 
		h = (double)(timer_us - last_timer_us) / 1000000.0; 

		// store current time for next iteration
		last_timer_us = timer_us; 

		//Serial.println(timer_us);
		//Serial.println(motor_right.enc->value);
		handle_motor(&motor_left, h);
		handle_motor(&motor_right, h);

		delay(MOTOR_LOOP_PERIOD_US);
	}

    // we are exiting, stop motors
    resetMotors();
}

void handle_motor(motor_t* motor, double h){
    double speed = getMotorRotationSpeed(motor, h);

    // calculate error
    double e = motor->speed_reference - speed;

    // calculate control signal
    double u = calculate_pid(&motor->pid, e, h);  

    // constrain control signal to within +-12 volts
    double saturated_u = constrain(u, -12.0, 12.0);

    //Serial.println(speed);
    
    // drive the motor with this signal
    actuate_motor(motor, saturated_u); 

}

// motor function, input voltage in range -12 to 12 volts
void actuate_motor(motor_t* motor, double u){
    // cap u in the range -12 to 12 volts
    u = constrain(u, -12.0, 12.0);


	// theese small voltages will only make the motors whine anyway
	if( abs(u) < 0.6)
		u = 0;

    // convert voltage to pwm duty cycle
    u = 100.0 * (u / 12.0);

    // convert pwm duty cycle to raw value
    uint8_t PWM_VALUE = (uint8_t) abs(u * (double) 255 / 100.0 );

/*
    Serial.print(motor->pins.PIN_EN);
    Serial.print(":");
    Serial.println(PWM_VALUE);
*/

    //analogWrite(motor->pins.PIN_EN, PWM_VALUE);
	ledcWrite(motor->pwm_channel, PWM_VALUE);


    if(u >= 0){
        // forward
        digitalWrite(motor->pins.PIN_DIRA, HIGH);
        digitalWrite(motor->pins.PIN_DIRB, LOW);
    }else{
        // backward
        digitalWrite(motor->pins.PIN_DIRA, LOW);
        digitalWrite(motor->pins.PIN_DIRB, HIGH);
    }
}

double getMotorRotationSpeed(motor_t* motor, double dt){
    // read motor encoder
    motor->current_encoder_counter = motor->enc->value;

    // calculate difference in encoder counts since last time
    double position_delta = motor->current_encoder_counter - (double)motor->last_encoder_counter;

     // save current position
    motor->last_encoder_counter = motor->current_encoder_counter;

    // increase odometry counter
    motor->odometry_counter += position_delta;

    // calculate and return current speed in rad/s
    return (double) 2.0 * PI * position_delta / ENC_COUNTS_PER_REV / dt;

}