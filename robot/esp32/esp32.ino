
#include "TFmini.h"


//// PIN DEFINES ////

// Serial2 pins, cannot be changed
#define RDX2 16
#define TXD2 17


// stepper motor defines
#define STEPPER_EN 23
#define STEPPER_DIR 21
#define STEPPER_STEP 22
#define STEPPER_SENSOR 34

#define MICROSTEPPING 2
#define STEPS_PER_ROTATION (360*MICROSTEPPING)



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

void setup() {
    // enable debug serial
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
    Serial.println("Done homing!");


    ///// SETUP TFMini /////
	Serial2.begin(TFmini::DEFAULT_BAUDRATE, SERIAL_8N1, RDX2, TXD2);
	
	delay(100); // Give a little time for things to start

	// Set to Standard Output mode
	tfmini.attach(Serial2);
	tfmini.setDetectionPattern(TFmini::DetectionPattern::Fixed);
	tfmini.setDistanceMode(TFmini::DistanceMode::Meduim);

}

void loop() {


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
		sendData(step_counter, tfmini.getDistance() * 10, 0);
	
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