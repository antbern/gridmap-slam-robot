#include "sensor.h"

#include "Arduino.h"
#include "WiFiClient.h"
#include "TFmini.h"

#include "pins.h"
#include "motors.h"

// struct holding a single measurement (note that the struct's variables must be aligned on a 2-byte boundary, or padding bytes will be added)
typedef struct {
	short header = 0x55AA;
	short steps;
	short frontDistance, backDistance;
} measurement_t;

// buffer storing all measurements taken during this loop (with a maximum of STEPS_PER_ROTATION samples)
measurement_t measurements[STEPS_PER_ROTATION];
int measurement_count = 0;

// variables for the "LiDAR" unit
TFmini tfmini;

// private function prototypes
void step_motor(unsigned short steps);

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

void resetSensor(){
	// doOnce = 0;
	// doContinously = 0;
}

sensor_queue_item_t handleCommandsItem;

void handleCommands(sensor_loop_parameters_t* params) {


	WiFiClient* stream = params->client;
	
	while(stream->available() > 0){ 

		// read one byte
		char input = stream->read();
		
		if(input == 0x01 || input == 'O'){ // "do once"-command?
			handleCommandsItem.command = ENABLE_ONCE;
			xQueueSendToBack( params->queueHandle, &handleCommandsItem, 0);


		}else if (input == 0x02 || input == 'E'){ // "enable continous"-command?
			handleCommandsItem.command = ENABLE_CONTINOUSLY;
			xQueueSendToBack( params->queueHandle, &handleCommandsItem, 0);
		}else if (input == 0x04 || input == 'D'){ // "disable continous"-command?
			handleCommandsItem.command = DISABLE;
			xQueueSendToBack( params->queueHandle, &handleCommandsItem, 0);
		}else if (input == 0x05 || input == 'H'){ // "home sensor"-command?
			handleCommandsItem.command = HOME_SENSOR;
			xQueueSendToBack( params->queueHandle, &handleCommandsItem, 0);
		}else if (input == 0x08){ // "set resolution"-command?
			char d;
			// wait for next byte
			while ((d = stream->read()) == -1);
			
			short next_steps = (short) (STEPS_PER_ROTATION * ((float)d / 360.0f));

			handleCommandsItem.command = SET_STEP_LENGTH;
			handleCommandsItem.data = next_steps;
			xQueueSendToBack( params->queueHandle, &handleCommandsItem, 0);
			
		}else if (input == 0x10){ // "set motor speed" - command?
			motor_left.speed_reference = (double) readFloat(stream);
			motor_right.speed_reference = (double) readFloat(stream);

			// Serial.printf("New motor speed: %5.3f, %5.3f\n", motor_left.speed_reference, motor_right.speed_reference);

		} else if (input == 0x15) { // set K_P command?
			motor_left.pid.Kp = motor_right.pid.Kp = (double) readFloat(stream);
		} else if (input == 0x16) { // set K_I command?
			motor_left.pid.Ki = motor_right.pid.Ki = (double) readFloat(stream);
		}else if (input == 0x17) { // set K_D command?
			motor_left.pid.Kd = motor_right.pid.Kd = (double) readFloat(stream);
		}else if (input == 0x18) { // set T_f command?
			motor_left.pid.Tf = motor_right.pid.Tf = (double) readFloat(stream);
		}
	}
}


void doSensorLoop(void* parameter){
	sensor_loop_parameters_t* params  = (sensor_loop_parameters_t*) parameter;

	QueueHandle_t queue = params->queueHandle;
	WiFiClient* stream = params->client;

	// internal state variables
	bool doOnce = 0;
	bool doContinously = 0;
	unsigned short step_counter = 0, next_steps = 4;

	bool shouldTerminate = false;

	BaseType_t xStatus;
	sensor_queue_item_t item;
	while (!shouldTerminate){	
		
		// if we are not actively taking measurements, then wait indefinitely
		// otherwise don't wait for anything to appear in the queue

		TickType_t ticksToWait = doOnce ? 0 : portMAX_DELAY;
		// Serial.printf("Entering queue receive with %d ticksToWait\n", ticksToWait);
		
		xStatus = xQueueReceive(queue, &item, ticksToWait);

		// Serial.printf("Status: %d, doOnce=%d\n", xStatus, doOnce);
		if (xStatus == pdPASS) {
			// Serial.println("Got new message!");
			switch (item.command) {
			case ENABLE_ONCE:
				doOnce = true;
				break;
			case ENABLE_CONTINOUSLY:
				doOnce = true;
				doContinously = true;
				break;
			case DISABLE:
				doContinously = false;
				break;
			case SET_STEP_LENGTH:
				next_steps = item.data;
				break;
			case HOME_SENSOR:
				homeSensor();
				step_counter = 0;
				break;
			case TERMINATE:
				shouldTerminate = true;
				break;
			default:
				break;
			}
		}

		// Serial.printf("After: doOnce=%d, doContinously=%d\n", doOnce, doContinously);

		if (doOnce){

			// make sure the motor is enabled
			digitalWrite(STEPPER_EN, LOW);

			// move the motor
			step_motor(next_steps);
			
			// increase the counter and "loop back" if we exceed the number of steps for one compleete sensor revolution
			step_counter += next_steps;
			
			// check if we have done a complete revolution
			if(step_counter > STEPS_PER_ROTATION){
				step_counter -= STEPS_PER_ROTATION;

				// TODO: make this data be available to a secondary task that collates all the information

				// yes, send message (with odometry information) to indicate that
				measurements[measurement_count].steps = -1;
				measurements[measurement_count].frontDistance = motor_left.odometry_counter;
				measurements[measurement_count].backDistance = motor_right.odometry_counter;
				measurement_count++;

				// write all data to client
				stream->write((const uint8_t*) &measurements, sizeof(measurement_t) * measurement_count);

				// reset buffer counter
				measurement_count = 0;
				
				// reset odometry counters
				motor_left.odometry_counter = 0;
				motor_right.odometry_counter = 0;
				
				// this allows the code to either do repeated measurements (if doContinously = true) or only do a measurement once (if doContinously = false and doOnce is set to true once)
				doOnce = doContinously;
				
				// if stopped, disable the motor
				if(!doOnce)
					digitalWrite(STEPPER_EN, HIGH);
			}
			
			// Serial.println("Before reading TFMini");

			// take reading
			while(!tfmini.available());

			// Serial.println("After reading TFMini");
			
			// save the reading to our buffer
			measurements[measurement_count].steps = step_counter;
			measurements[measurement_count].frontDistance = tfmini.getDistance() * 10;
			measurements[measurement_count].backDistance = tfmini.getStrength();
			measurement_count++;	

		}
	}

	digitalWrite(STEPPER_EN, HIGH);
	
	vTaskDelete(NULL);
}

// reads a single float value from the provided WiFiClient (blocking)
float readFloat(WiFiClient* stream) {
	// wait for 4 bytes
	while(stream->available() < 4);
	
	// get the four bytes and convert them to a float
	char buffer[4];
	buffer[3] = stream->read();
	buffer[2] = stream->read();
	buffer[1] = stream->read();
	buffer[0] = stream->read();
	
	return *((float*)&buffer);
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
}

void step_motor(unsigned short steps){
	for(unsigned short i = 0; i < steps; i++){
		digitalWrite(STEPPER_STEP, HIGH);
		delayMicroseconds(800);
		digitalWrite(STEPPER_STEP, LOW);
		delayMicroseconds(800);
	}
}
