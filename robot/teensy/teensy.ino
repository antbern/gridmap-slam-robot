
//// LIBRARIES ////

#include "Arduino.h"
#include <Encoder.h>

#include "TFmini.h"

//// PIN DEFINES ////

// stepper motor defines
#define STEPPER_EN 12
#define STEPPER_DIR 10
#define STEPPER_STEP 11
#define STEPPER_SENSOR A9

#define MICROSTEPPING 2
#define STEPS_PER_ROTATION (360*MICROSTEPPING)


// dc motor defines
#define MOTOR_RIGHT_EN 4
#define MOTOR_RIGHT_DIRA 3 
#define MOTOR_RIGHT_DIRB 2
#define MOTOR_RIGHT_ENCA 14
#define MOTOR_RIGHT_ENCB 15

#define MOTOR_LEFT_EN 5
#define MOTOR_LEFT_DIRA 7
#define MOTOR_LEFT_DIRB 6
#define MOTOR_LEFT_ENCA 16
#define MOTOR_LEFT_ENCB 17


#define ENC_COUNTS_PER_REV (32 * 30 * 2)

//// VARIABLES ////



TFmini tfmini;


char doOnce = 0;
char doContinously = 0;

unsigned short step_counter = 0, next_steps = 1;


// PID values for the motor controllers
double Kp = 0.55276367534483;//0.610694929511361;//0.641817786149385 ;//6.458906368104240;//5.061601496636267;//3.286079178973016;
double Ki = 1.64455966045303;//1.34329498731559;//1.169731890184110 ;//21.544597297186854;//59.064657944882540;//70.241507066863450; 
double Kd = 0.0101674410396297;//0.0220997968974464;
double Tf = 1/11.8209539589613;//1/5.57670843490099;


typedef struct {
    double Kp = 0.0;
    double Ki = 0.0;
    double Kd = 0.0;
    double Tf = 0.0;

    double P = 0.0;
    double I = 0.0;
    double D = 0.0;

    double e_old = 0.0;
} PID_t;

// struct defining the motor properties
typedef struct {
    Encoder* enc;
    int32_t current_encoder_counter = 0;       // current encoder count
    int32_t last_encoder_counter = 0;          // last encoder count, used for calculating rotational speed
    int32_t odometry_counter = 0;
	PID_t pid;                                  // PID controller for velocity control
    double speed_reference = 0;                 // the reference value used for the PID-controller    
    struct{
        char PIN_EN, PIN_DIRA, PIN_DIRB;
    } pins;                                     // struct for the motors' pins
    
} motor_t;

motor_t motor_left, motor_right;

Encoder encLeft(MOTOR_LEFT_ENCA, MOTOR_LEFT_ENCB);
Encoder encRight(MOTOR_RIGHT_ENCA, MOTOR_RIGHT_ENCB);


double last_timer_us = 0,  h = 0;

void setup(){

    // enable serial connection on USB port
    Serial.begin(115200);

    ///// SETUP PINS /////
    // stepper motor
	pinMode(STEPPER_EN, OUTPUT);
	pinMode(STEPPER_DIR, OUTPUT);
	pinMode(STEPPER_STEP, OUTPUT);
	pinMode(STEPPER_SENSOR, INPUT);

    // disable stepper motor
	digitalWrite(STEPPER_EN, HIGH);
	
	// select rotational direction
	digitalWrite(STEPPER_DIR, HIGH); 

    homeSensor();

    //digitalWrite(STEPPER_EN, LOW);
    //doContinously = 1;
    //doOnce = 1;

    // dc motors

    ///// SETUP TFMini /////
    Serial1.begin(TFmini::DEFAULT_BAUDRATE);
	tfmini.attach(Serial1);
	tfmini.setDetectionPattern(TFmini::DetectionPattern::Fixed);
	tfmini.setDistanceMode(TFmini::DistanceMode ::Meduim);


    ///////////// DC MOTORs ///////////
	pinMode(MOTOR_LEFT_EN, OUTPUT);	
    digitalWrite(MOTOR_LEFT_EN, HIGH);
    pinMode(MOTOR_LEFT_DIRA, OUTPUT);
    pinMode(MOTOR_LEFT_DIRB, OUTPUT);

    pinMode(MOTOR_RIGHT_EN, OUTPUT);
    digitalWrite(MOTOR_RIGHT_EN, HIGH);
    pinMode(MOTOR_RIGHT_DIRA, OUTPUT);
    pinMode(MOTOR_RIGHT_DIRB, OUTPUT);

	// set up motors and their PID-regulators
    motor_left.pid.Kp = 0.55276367534483;
    motor_left.pid.Ki = 1.64455966045303;
    motor_left.pid.Kd = 0.0101674410396297;
    motor_left.pid.Tf = 1/11.8209539589613;
    motor_left.pins = {MOTOR_LEFT_EN, MOTOR_LEFT_DIRA, MOTOR_LEFT_DIRB};
    motor_left.enc = &encLeft;

    motor_right.pid.Kp = 0.55276367534483;
    motor_right.pid.Ki = 1.64455966045303;
    motor_right.pid.Kd = 0.0101674410396297;
    motor_right.pid.Tf = 1/11.8209539589613;
    motor_right.pins = {MOTOR_RIGHT_EN, MOTOR_RIGHT_DIRA, MOTOR_RIGHT_DIRB};
    motor_right.enc =  &encRight;
    	
	motor_right.speed_reference = 0.0f;
	motor_left.speed_reference = 0.0f;

    // initialize the time counter
	last_timer_us = micros();	

/*
    digitalWrite(MOTOR_RIGHT_DIRA, HIGH);
    digitalWrite(MOTOR_RIGHT_DIRB, LOW);

    analogWrite(MOTOR_RIGHT_EN, HIGH);
*/

   // while(true);
}

void loop(){
    // get current time
    unsigned long timer_us = micros();

    // calculate elapsed time in seconds 
    h = (double)(timer_us - last_timer_us) / 1000000.0; 

    // store current time for next iteration
    last_timer_us = timer_us; 

    handle_motor(&motor_left, h);
    handle_motor(&motor_right, h);
    //Serial.println(encRight.read());
    //actuate_motor(&motor_left, 3);
    //step_motor(10);
	/*
    if (tfmini.available())	{
		//Serial.print("distance : ");
		Serial.print(tfmini.getDistance());
		Serial.print(",");
		Serial.println(tfmini.getStrength());
		//Serial.print("int time : ");
		//Serial.println(tfmini.getIntegrationTime());
	}
    */

   // check if there are any incoming bytes on the serial port
	if(Serial.available() > 0){
		// read one byte
		char input = Serial.read();
		
		if(input == 0x01 || input == 'O'){ // "do once"-command?
			doOnce = 1;
			digitalWrite(STEPPER_EN, LOW);
		}else if (input == 0x02 || input == 'E'){ // "enable continous"-command?
			doContinously = 1;
			doOnce = 1;
			digitalWrite(STEPPER_EN, LOW);
		}else if (input == 0x04 || input == 'D'){ // "disable continous"-command?
			doContinously = 0;
        }else if (input == 0x05 || input == 'H'){ // "disable continous"-command?
			homeSensor();
		}else if (input == 0x08){ // "set resolution"-command?
			char d;
			// wait for next byte
			while ((d = Serial.read()) == -1);
			
			next_steps = (short) (STEPS_PER_ROTATION * ((float)d / 360.0f));
			
		}else if (input == 0x10){ // "set left motor speed" - command?
			// wait for 4 bytes
			while(Serial.available() < 4);
			
			// get the four bytes and convert them to a float
			char buffer[4];
			buffer[3] = Serial.read();
			buffer[2] = Serial.read();
			buffer[1] = Serial.read();
			buffer[0] = Serial.read();
			
			float value = *((float*)&buffer);
			
			motor_left.speed_reference = (double) value;
		}else if (input == 0x11){ // "set right motor speed" - command?
			// wait for 4 bytes
			while(Serial.available() < 4);
			
			// get the four bytes and convert them to a float
			char buffer[4];
			buffer[3] = Serial.read();
			buffer[2] = Serial.read();
			buffer[1] = Serial.read();
			buffer[0] = Serial.read();
			
			float value = *((float*)&buffer);
			motor_right.speed_reference = (double) value;
		}
    }

   // only do measurement if 
	if(doOnce != 0){
				
		// move the motor
		step_motor(next_steps);
		
		// increase the counter and "loop back" if we exceed the number of steps for one compleete sensor revolution
		step_counter += next_steps;
		
		// check if we have done a complete revolution
		if(step_counter > STEPS_PER_ROTATION){
            step_counter -= STEPS_PER_ROTATION;

			// yes, send message (with odometry information) to indicate that
		    sendData(-1, motor_left.odometry_counter, motor_right.odometry_counter);
            //sendData(-1, 0, 0);
			
			// reset odometry counters
			motor_left.odometry_counter = 0;
			motor_right.odometry_counter = 0;
			
			// this allows the code to either do repeated measurements (if doContinously = 1) or only do a measurement once (if doContinously = 0 and doOnce is set to 1 once)
			doOnce = doContinously;
			
			// if stopped, disable the motor
			if(doOnce == 0)
				digitalWrite(STEPPER_EN, HIGH);
		}
		

		// take reading
        while(!tfmini.available());
		
		// take and send the actual measurements
		sendData(step_counter, tfmini.getDistance() * 10, 0);//tfmini.getStrength());
	
	} else {
		delay(10);
	}
}

void sendData(short steps, short frontDistance, short backDistance){
	Serial.write(0x55); // start byte
	Serial.write((steps >> 8) & 0xff);
	Serial.write((steps >> 0) & 0xff); 
	Serial.write((frontDistance >> 8) & 0xff);
	Serial.write((frontDistance >> 0) & 0xff);
	Serial.write((backDistance >> 8) & 0xff);
	Serial.write((backDistance >> 0) & 0xff);
    //Serial.flush();
}



void homeSensor(){
	// enable stepper
	digitalWrite(STEPPER_EN, LOW);
	
	int smoothed = 0, smoothed16 = 0, newVal;
	
	// startup value
	smoothed = analogRead(STEPPER_SENSOR);
	smoothed16 = smoothed << 4;
	do{
		// do some exponential filtering. See https://forum.arduino.cc/index.php?topic=445844.0 (last post)
		smoothed16 = smoothed16 - smoothed + analogRead(STEPPER_SENSOR);
		smoothed = smoothed16 >> 4;
		
		// step the motor	
		step_motor(1);
		
		// get a new reading 
		newVal = analogRead(STEPPER_SENSOR);

        //Serial.println(newVal);

		// continue while we see no "peak" in the sensor value
	}while(newVal - smoothed < 25);
	
	// disable stepper
	digitalWrite(STEPPER_EN, HIGH);
	
	// reset position step counter
	//step_counter = 0;
}

void step_motor(unsigned short steps){
	for(unsigned short i = 0; i < steps; i++){
		digitalWrite(STEPPER_STEP, HIGH);
		delayMicroseconds(800);
		digitalWrite(STEPPER_STEP, LOW);
		delayMicroseconds(800);
	}
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

// does the PID calculation and returns the new control output
double calculate_pid(PID_t* pid, double error, double h){
    // proportional part
    pid->P = pid->Kp * error;

    // derivative part
    pid->D = pid->Tf / (pid->Tf + h) * pid->D + pid->Kd / (pid->Tf + h) * (error - pid->e_old);

    // calculate output
    double u = pid->P + pid->I + pid->D;

    // integral part
    pid->I += pid->Ki * h * error;

    // save error
    pid->e_old = error;

    // return control signal
    return u;
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

    analogWrite(motor->pins.PIN_EN, PWM_VALUE);
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
    motor->current_encoder_counter = motor->enc->read();


    //Serial.println(motor->current_encoder_counter);

    // calculate difference in encoder counts since last time
    double position_delta = motor->current_encoder_counter - (double)motor->last_encoder_counter;

     // save current position
    motor->last_encoder_counter = motor->current_encoder_counter;

    // increase odometry counter
    motor->odometry_counter += position_delta;

    // calculate and return current speed in rad/s
    return (double) 2.0 * PI * position_delta / ENC_COUNTS_PER_REV / dt;

}