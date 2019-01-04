#include "sensor.h"

#include "Arduino.h"
#include "TFmini.h"

#include "pins.h"
#include "motors.h"


// variables for the "LiDAR" unit
TFmini tfmini;
char doOnce = 0;
char doContinously = 0;
unsigned short step_counter = 0, next_steps = 1;

// private function prototypes
void step_motor(unsigned short steps);
void sendData(Stream* stream, short steps, short frontDistance, short backDistance);

void initSensor(){
    // stepper motor pin definitions
	pinMode(STEPPER_EN, OUTPUT);
	pinMode(STEPPER_DIR, OUTPUT);
	pinMode(STEPPER_STEP, OUTPUT);
	pinMode(STEPPER_SENSOR, INPUT);

    // disable stepper motor
	digitalWrite(STEPPER_EN, HIGH);
	
	// select rotational direction
	digitalWrite(STEPPER_DIR, HIGH); 

    ///// SETUP TFMini /////
	Serial2.begin(TFmini::DEFAULT_BAUDRATE, SERIAL_8N1, RDX2, TXD2);
	
	delay(100); // Give a little time for things to start

	// Set to Standard Output mode
	tfmini.attach(Serial2);
	tfmini.setDetectionPattern(TFmini::DetectionPattern::Fixed);
	tfmini.setDistanceMode(TFmini::DistanceMode::Meduim);

	////////////////////////
}




void doSensorLoop(Stream* stream){
    // check if there are any incoming bytes on the serial port
	if(stream->available() > 0){
		// read one byte
		char input = stream->read();
		
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
			while ((d = stream->read()) == -1);
			
			next_steps = (short) (STEPS_PER_ROTATION * ((float)d / 360.0f));
			
		}else if (input == 0x10){ // "set left motor speed" - command?
			// wait for 4 bytes
			while(stream->available() < 4);
			
			// get the four bytes and convert them to a float
			char buffer[4];
			buffer[3] = stream->read();
			buffer[2] = stream->read();
			buffer[1] = stream->read();
			buffer[0] = stream->read();
			
			float value = *((float*)&buffer);
			
			motor_left.speed_reference = (double) value;
		}else if (input == 0x11){ // "set right motor speed" - command?
			// wait for 4 bytes
			while(stream->available() < 4);
			
			// get the four bytes and convert them to a float
			char buffer[4];
			buffer[3] = stream->read();
			buffer[2] = stream->read();
			buffer[1] = stream->read();
			buffer[0] = stream->read();
			
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
		    sendData(stream, -1, motor_left.odometry_counter, motor_right.odometry_counter);
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
		sendData(stream, step_counter, tfmini.getDistance() * 10, 0);
	
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

inline void sendData(Stream* stream, short steps, short frontDistance, short backDistance){
	stream->write(0x55); // start byte
	stream->write((steps >> 8) & 0xff);
	stream->write((steps >> 0) & 0xff); 
	stream->write((frontDistance >> 8) & 0xff);
	stream->write((frontDistance >> 0) & 0xff);
	stream->write((backDistance >> 8) & 0xff);
	stream->write((backDistance >> 0) & 0xff);
}