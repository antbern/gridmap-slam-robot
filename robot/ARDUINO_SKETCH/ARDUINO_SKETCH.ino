/* This example shows how to get single-shot range
 measurements from the VL53L0X. The sensor can optionally be
 configured with different ranging profiles, as described in
 the VL53L0X API user manual, to get better performance for
 a certain application. This code is based on the four
 "SingleRanging" examples in the VL53L0X API.

 The range readings are in units of mm. */

#include <Wire.h>
#include <VL53L0X.h>
#include <Servo.h>
#include "PID_v1.h"
#include <math.h>

VL53L0X frontSensor;
VL53L0X backSensor;

#define FRONT_SENSOR_SHUT A3
#define BACK_SENSOR_SHUT A2

// stepper motor defines
#define STEPPER_EN A1
#define STEPPER_DIR 13
#define STEPPER_STEP 12
#define STEPPER_SENSOR A0

#define MICROSTEPPING 2
#define STEPS_PER_ROTATION (96*5*MICROSTEPPING)

// motor defines
#define MOTOR_RIGHT_EN 6
#define MOTOR_RIGHT_DIRA 7
#define MOTOR_RIGHT_DIRB 8
#define MOTOR_RIGHT_ENCA 2 
#define MOTOR_RIGHT_ENCB 4

#define MOTOR_LEFT_EN 11
#define MOTOR_LEFT_DIRA 9
#define MOTOR_LEFT_DIRB 10
#define MOTOR_LEFT_ENCA 3 
#define MOTOR_LEFT_ENCB 5


// pin definitions using port and bit (FASTER :D)
#define ENC_PORT PIND
#define LEFT_ENCA_BIT 3
#define LEFT_ENCB_BIT 5

#define RIGHT_ENCA_BIT 2
#define RIGHT_ENCB_BIT 4

#define ENC_COUNTS_PER_REV (32 * 30)

double h = 0;
double last_timer_us = 0;


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


// sensor variables
unsigned short step_counter = 0, last_step_counter = 0;

char doOnce = 0;
char doContinously = 0;

// the number of steps needed for a resolution of 2 degrees
short next_steps = (short) (STEPS_PER_ROTATION * (2.0f / 360.0f));

// Uncomment this line to use long range mode. This
// increases the sensitivity of the sensor and extends its
// potential range, but increases the likelihood of getting
// an inaccurate reading because of reflections from objects
// other than the intended target. It works best in dark
// conditions.

#define LONG_RANGE


// Uncomment ONE of these two lines to get
// - higher speed at the cost of lower accuracy OR
// - higher accuracy at the cost of lower speed


//#define HIGH_SPEED
//#define HIGH_ACCURACY



void setup(){
	Serial.begin(115200);
	Wire.begin();
	
	///////////// STEPPER MOTOR /////////////

	//Serial.print(left.control_output);
	//Serial.println(left.pid->GetKp());

	// stepper motor:
	pinMode(STEPPER_EN, OUTPUT);
	pinMode(STEPPER_DIR, OUTPUT);
	pinMode(STEPPER_STEP, OUTPUT);
	pinMode(STEPPER_SENSOR, INPUT);
	
	// disable stepper motor
	digitalWrite(STEPPER_EN, HIGH);
	
	// select rotational direction
	digitalWrite(STEPPER_DIR, HIGH); 
	
	homeSensor();

	///////////// SENSORS ///////////////

	// disable both sensors first
	pinMode(FRONT_SENSOR_SHUT, OUTPUT);
	pinMode(BACK_SENSOR_SHUT, OUTPUT);

	digitalWrite(FRONT_SENSOR_SHUT, LOW);
	digitalWrite(BACK_SENSOR_SHUT, LOW);

	// initialize front sensor
	digitalWrite(FRONT_SENSOR_SHUT, HIGH); // enable
	frontSensor.init();
	frontSensor.setTimeout(500);
	frontSensor.setAddress(0x29 + 0x02);
  
	digitalWrite(BACK_SENSOR_SHUT, HIGH); // enable
	backSensor.init();
	backSensor.setTimeout(500);
	backSensor.setAddress(0x29 + 0x04);

	
#if defined LONG_RANGE
	// lower the return signal rate limit (default is 0.25 MCPS)
	frontSensor.setSignalRateLimit(0.1);
	backSensor.setSignalRateLimit(0.1);
	// increase laser pulse periods (defaults are 14 and 10 PCLKs)
	frontSensor.setVcselPulsePeriod(VL53L0X::VcselPeriodPreRange, 18);
	backSensor.setVcselPulsePeriod(VL53L0X::VcselPeriodPreRange, 18);

	frontSensor.setVcselPulsePeriod(VL53L0X::VcselPeriodFinalRange, 14);
	backSensor.setVcselPulsePeriod(VL53L0X::VcselPeriodFinalRange, 14);
#endif

#if defined HIGH_SPEED
	// reduce timing budget to 20 ms (default is about 33 ms)
	frontSensor.setMeasurementTimingBudget(20000);
	backSensor.setMeasurementTimingBudget(20000);
#elif defined HIGH_ACCURACY
	// increase timing budget to 200 ms
	frontSensor.setMeasurementTimingBudget(200000);
	backSensor.setMeasurementTimingBudget(200000);
#endif

	frontSensor.startContinuous();
	backSensor.startContinuous();

	// sensors initialized, lets init the motors

	///////////// DC MOTORs ///////////
	pinMode(MOTOR_LEFT_EN, OUTPUT);	
    digitalWrite(MOTOR_LEFT_EN, HIGH);
    pinMode(MOTOR_LEFT_DIRA, OUTPUT);
    pinMode(MOTOR_LEFT_DIRB, OUTPUT);
    pinMode(MOTOR_LEFT_ENCA, INPUT);
    pinMode(MOTOR_LEFT_ENCB, INPUT);

    pinMode(MOTOR_RIGHT_EN, OUTPUT);
    digitalWrite(MOTOR_RIGHT_EN, HIGH);
    pinMode(MOTOR_RIGHT_DIRA, OUTPUT);
    pinMode(MOTOR_RIGHT_DIRB, OUTPUT);
    pinMode(MOTOR_RIGHT_ENCA, INPUT);
    pinMode(MOTOR_RIGHT_ENCB, INPUT);

    attachInterrupt(digitalPinToInterrupt(MOTOR_LEFT_ENCA), int_encoder_left, CHANGE);
    attachInterrupt(digitalPinToInterrupt(MOTOR_RIGHT_ENCA), int_encoder_right, CHANGE);


	// set up motors and their PID-regulators
    motor_left.pid.Kp = 0.55276367534483;
    motor_left.pid.Ki = 1.64455966045303;
    motor_left.pid.Kd = 0.0101674410396297;
    motor_left.pid.Tf = 1/11.8209539589613;
    motor_left.pins = {MOTOR_LEFT_EN, MOTOR_LEFT_DIRA, MOTOR_LEFT_DIRB};

    motor_right.pid.Kp = 0.55276367534483;
    motor_right.pid.Ki = 1.64455966045303;
    motor_right.pid.Kd = 0.0101674410396297;
    motor_right.pid.Tf = 1/11.8209539589613;
    motor_right.pins = {MOTOR_RIGHT_EN, MOTOR_RIGHT_DIRA, MOTOR_RIGHT_DIRB};
    	
	motor_right.speed_reference = 0.0f;
	motor_left.speed_reference = 0.0f;
	
	// initialize the time counter
	last_timer_us = micros();	
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
		
		// continue while we see no "peak" in the sensor value
	}while(newVal - smoothed < 50);
			
	// do fine tuuning movements
	step_motor(4 * MICROSTEPPING);
	
	// disable stepper
	digitalWrite(STEPPER_EN, HIGH);
	
	// reset position step counter
	step_counter = 0;
}


//unsigned short frontAngle = 0, backAngle = 0;
void loop() {
 	// get current time
    unsigned long timer_us = micros();

    // calculate elapsed time in seconds 
    h = (double)(timer_us - last_timer_us) / 1000000.0; 

    // store current time for next iteration
    last_timer_us = timer_us; 

	// handle motors
    handle_motor(&motor_left, h);
    handle_motor(&motor_right, h);


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
		
		// store the number of steps before increasing
		last_step_counter = step_counter;
		
		// increase the counter and "loop back" if we exceed the number of steps for one compleete sensor revolution
		step_counter = (step_counter + next_steps) % STEPS_PER_ROTATION;
		
		
		// check if we crossed any halfway
		if(last_step_counter % (STEPS_PER_ROTATION / 2) > (last_step_counter + next_steps) % (STEPS_PER_ROTATION / 2)){
			// yes, send message (with odometry information) to indicate that
			sendData(-1, motor_left.odometry_counter, motor_right.odometry_counter);
			
			// reset odometry counters
			motor_left.odometry_counter = 0;
			motor_right.odometry_counter = 0;
			
			// this allows the code to either do repeated measurements (if doContinously = 1) or only do a measurement once (if doContinously = 0 and doOnce is set to 1 once)
			doOnce = doContinously;
			
			// if stopped, disable the motor
			if(doOnce == 0)
				digitalWrite(STEPPER_EN, HIGH);
		}
		
		// take and send the actual measurements
		sendData(step_counter, frontSensor.readRangeContinuousMillimeters(), backSensor.readRangeContinuousMillimeters());
		
		/*
		short lastFrontAngle = frontAngle % 180;
		
		frontAngle = (short) (step_counter * 360L / STEPS_PER_ROTATION);
		backAngle = (frontAngle + 180) % 360;
		
		// did we cross the 360 boudary?
		if(lastFrontAngle > (frontAngle % 180))
			sendData(500, 0);
		
		*/
		/*
		Serial.print(next_steps);
		Serial.print(':');
		Serial.print(step_counter);
		Serial.print(':');
		Serial.println(frontAngle);
		Serial.println();
		*/
	
	} else {
		delay(10);
	}

}



void step_motor(unsigned short steps){
	for(unsigned short i = 0; i < steps; i++){
		digitalWrite(STEPPER_STEP, HIGH);
		delayMicroseconds(1000);
		digitalWrite(STEPPER_STEP, LOW);
		delayMicroseconds(1000);
	}
}

inline void sendData(short steps, short frontDistance, short backDistance){
	Serial.write(0x55); // start byte
	Serial.write((steps >> 8) & 0xff);
	Serial.write((steps >> 0) & 0xff); 
	Serial.write((frontDistance >> 8) & 0xff);
	Serial.write((frontDistance >> 0) & 0xff);
	Serial.write((backDistance >> 8) & 0xff);
	Serial.write((backDistance >> 0) & 0xff);
 
}


void handle_motor(motor_t* motor, double h){
    double speed = getMotorRotationSpeed(motor, h);

    // calculate error
    double e = motor->speed_reference - speed;

    // calculate control signal
    double u = calculate_pid(&motor->pid, e, h);  

    // constrain control signal to within +-12 volts
    double saturated_u = constrain(u, -12.0, 12.0);
    
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
