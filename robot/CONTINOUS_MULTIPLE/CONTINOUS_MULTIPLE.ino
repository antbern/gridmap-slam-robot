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

#define SPEED_LOOP_TIMING (1000.0/10.0)

long lastTimeMillis = 0;




// struct defining the motor properties
typedef struct {
	// Pin declarations
	const short PIN_EN, PIN_DIRA, PIN_DIRB, PIN_ENCA, PIN_ENCB;
	
	// variables
	double set_point; /* the desired revolution/s */
	double control_output; /* the pwm value to control the motor*/
	double control_adjust; /* the output from the pid */
	
	double speed; /* current speed in revolutions/s */
	long speed_counter; /* counter for determining speed */
 	long odometry_counter; /* counter for determining distance traveled */
	
	void* interruptHandler; /* function pointer to the interrupt routine */
	
	PID* pid; /* PID-controller for regulating the motor speed */
} motor_t;

// create our two motors with correct pin numbering
motor_t left = {
	.PIN_EN = MOTOR_LEFT_EN,
	.PIN_DIRA = MOTOR_LEFT_DIRA,
	.PIN_DIRB = MOTOR_LEFT_DIRB,
	.PIN_ENCA = MOTOR_LEFT_ENCA,
	.PIN_ENCB = MOTOR_LEFT_ENCB,
	//.set_point = 2.5,
};

motor_t right = {
	.PIN_EN = MOTOR_RIGHT_EN,
	.PIN_DIRA = MOTOR_RIGHT_DIRA,
	.PIN_DIRB = MOTOR_RIGHT_DIRB,
	.PIN_ENCA = MOTOR_RIGHT_ENCA,
	.PIN_ENCB = MOTOR_RIGHT_ENCB,
	//.set_point = 1.5
};

// the PID-controllers for the motors (has to be created separate from the struct unfortunately since it is referencing the struct itself)
PID leftPID(&left.speed, &left.control_adjust, &left.set_point, 5.0*8*8/2/10/*0.0065*/, /*0.16*8*8/2*/ 0.0, /*0.125/2*/ 62.5 / 10,P_ON_E, DIRECT);
PID rightPID(&right.speed, &right.control_adjust, &right.set_point, 5.0*8*8/2/10/*0.0065*/, /*0.16*8*8/2*/ 0.0, /*0.125/2*/ 62.5 / 10,P_ON_E, DIRECT);


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



void setup()
{
	Serial.begin(115200);
	Wire.begin();
	
	

	//Serial.print(left.control_output);
	//Serial.println(left.pid->GetKp());

	// stepper motor:
	pinMode(STEPPER_EN, OUTPUT);
	pinMode(STEPPER_DIR, OUTPUT);
	pinMode(STEPPER_STEP, OUTPUT);
	pinMode(STEPPER_SENSOR, INPUT);
	
	
	// disable motor
	digitalWrite(STEPPER_EN, HIGH);
	
	// select rotational direction
	digitalWrite(STEPPER_DIR, HIGH); 
	
	homeSensor();


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
	
	// initialize the motor structs	completely
	left.pid = &leftPID;
	right.pid = &rightPID;
	
	left.interruptHandler = motorLeftEncoder;
	right.interruptHandler = motorRightEncoder;
	
	// do the initialization
	initializeMotor(&left);
	initializeMotor(&right);
	
	
	// initialize the time counter
	lastTimeMillis = millis();	
}

// initializes a single motor
void initializeMotor(motor_t* motor){
	// setup pins
	pinMode(motor->PIN_EN, OUTPUT);
	pinMode(motor->PIN_DIRA, OUTPUT);
	pinMode(motor->PIN_DIRB, OUTPUT);
	pinMode(motor->PIN_ENCA, INPUT);
	pinMode(motor->PIN_ENCB, INPUT);
	
	// attach correct interrupt
	attachInterrupt(digitalPinToInterrupt(motor->PIN_ENCA), motor->interruptHandler, CHANGE);
	
	
	// set up PID-controller
	motor->pid->SetOutputLimits(-255, 255);
	motor->pid->SetSampleTime(100);
	motor->pid->SetMode(AUTOMATIC);	
}

// interrupt routines for the encoders
void motorLeftEncoder(){
	handleEncoder(&left);
}

void motorRightEncoder(){
	handleEncoder(&right);
}

inline void handleEncoder(motor_t* motor){
	if(digitalRead(motor->PIN_ENCA) == digitalRead(motor->PIN_ENCB)){
		motor->speed_counter++;
		motor->odometry_counter++;
	} else {
		motor->speed_counter--;
		motor->odometry_counter--;
	}
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

/*
void initSensor(VL53L0X sensor, uint8_t addr){

	sensor.init();
	sensor.setTimeout(500);
	sensor.setAddress(addr);
  
#if defined LONG_RANGE
	// lower the return signal rate limit (default is 0.25 MCPS)
	sensor.setSignalRateLimit(0.1);
	// increase laser pulse periods (defaults are 14 and 10 PCLKs)
	sensor.setVcselPulsePeriod(VL53L0X::VcselPeriodPreRange, 18);
	sensor.setVcselPulsePeriod(VL53L0X::VcselPeriodFinalRange, 14);
#endif

#if defined HIGH_SPEED
	// reduce timing budget to 20 ms (default is about 33 ms)
	sensor.setMeasurementTimingBudget(20000);
#elif defined HIGH_ACCURACY
	// increase timing budget to 200 ms
	sensor.setMeasurementTimingBudget(200000);
#endif
}
*/

//unsigned short frontAngle = 0, backAngle = 0;
void loop()
{
	
	// loop that runns at 10 Hz
	if((millis() - lastTimeMillis) > SPEED_LOOP_TIMING){
		lastTimeMillis = millis();
			
		calculateMotorSpeed(&left);
		calculateMotorSpeed(&right);

		//Serial.println(analogRead(A0));
	}
	
	doMotorLogic(&left);
	doMotorLogic(&right);
	
	//Serial.println(left.speed);
	 
	 /*
	// handle serial input
	if(Serial.available() > 0){
		left.set_point = (double)Serial.parseFloat();
		right.set_point = left.set_point / 2;
		//motorRightSetPoint = motorLeftSetPoint;
		
		while(Serial.available() > 0) Serial.read();
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
			left.set_point = (double) value;
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
			right.set_point = (double) value;
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
			// yes, send message to indicate that
			sendData(-1, -1, -1);
			
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
	
	}
	
}

void calculateMotorSpeed(motor_t* motor){
	// convert encoder pulses to revolutions
	double revs = (double)motor->speed_counter / (32 * 30);
	
	// the speed is calculated every 100 ms (10 Hz)
	double revs_per_second = revs * (1000.0 / SPEED_LOOP_TIMING);
	
	// update the motor speed, but also use a bit of smoothing
	motor->speed = revs_per_second * 0.95 + motor->speed * 0.05;
	//motorLeftSpeed = revs_per_second * 0.2 + motorLeftSpeed * 0.8;
	
	// reset counter
	motor->speed_counter = 0;
}

void doMotorLogic(motor_t* motor){
	if(motor->pid->Compute()){
		
		// add adjustment to control value
		motor->control_output += motor->control_adjust;// * 0.1;
		
		// cap control value
		motor->control_output = constrain(motor->control_output, -255, 255);
		
		writeMotorControl(motor);
		
		/*
		writeMotorLeft(motorLeftControl);
		//analogWrite(MOTOR_LEFT_EN, (int)motorLeftControl);
		Serial.print('L');
		Serial.print(motorLeftSpeed);
		Serial.print(':');
		Serial.print(motorLeftSetPoint);
		Serial.print(':');
		Serial.print(motorLeftControlAdjust);
		Serial.print(':');
		Serial.println(motorLeftControl);
		*/
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
 	
/*
  Serial.print(angle);
  Serial.print(':');
  Serial.println(distance);
  */
}

void writeMotorControl(motor_t* motor){
	if(abs(motor->set_point) < 0.2){
		digitalWrite(motor->PIN_DIRA, LOW);
		digitalWrite(motor->PIN_DIRB, LOW);
		analogWrite(motor->PIN_EN, 255);
		return;
	} 
	
	if(motor->control_output > 0){
		digitalWrite(motor->PIN_DIRA, HIGH);
		digitalWrite(motor->PIN_DIRB, LOW);
		analogWrite(motor->PIN_EN, (int)round(motor->control_output));
	}else{
		digitalWrite(motor->PIN_DIRA, LOW);
		digitalWrite(motor->PIN_DIRB, HIGH);
		analogWrite(motor->PIN_EN, -(int)round(motor->control_output));
	}
}
/*

void writeMotorLeft(float control){
	if(control >= 0){
		digitalWrite(MOTOR_LEFT_DIRA, HIGH);
		digitalWrite(MOTOR_LEFT_DIRB, LOW);
		analogWrite(MOTOR_LEFT_EN, (int)round(control));
	}else{
		digitalWrite(MOTOR_LEFT_DIRA, LOW);
		digitalWrite(MOTOR_LEFT_DIRB, HIGH);
		analogWrite(MOTOR_LEFT_EN, -(int)round(control));
	}
}

void writeMotorRight(float control){
	if(control >= 0){
		digitalWrite(MOTOR_RIGHT_DIRA, HIGH);
		digitalWrite(MOTOR_RIGHT_DIRB, LOW);
		analogWrite(MOTOR_RIGHT_EN, (int)round(control));
	}else{
		digitalWrite(MOTOR_RIGHT_DIRA, LOW);
		digitalWrite(MOTOR_RIGHT_DIRB, HIGH);
		analogWrite(MOTOR_RIGHT_EN, -(int)round(control));
	}
}
*/
