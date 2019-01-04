/* Links
	Static IP: https://randomnerdtutorials.com/esp32-static-fixed-ip-address-arduino-ide/
	Soft AP: https://randomnerdtutorials.com/esp32-access-point-ap-web-server/
	Board Pinout (30 GPIOs): https://randomnerdtutorials.com/getting-started-with-esp32/
*/

#include <pthread.h>

#include "pins.h"
#include "encoder.h"
#include "pid.h"
#include "motors.h"
#include "sensor.h"

//// VARIABLES ////

// vairables for handling WiFi connection
#include "wifi_settings.h"


// thread for handling the motor pid control
pthread_t motorThread;

void setup() {
    // enable debug serial
    Serial.begin(115200);

	//we must initialize rorary encoder and motors first
	initEncoders();
	initMotors();
	initSensor();

	// home sensor
	homeSensor();

	// start a new thread for the motor
	int ret = pthread_create(&motorThread, NULL, motorLoop, NULL);
	if(ret){
		Serial.println("Error creating thread");
	}
	
}


void loop() {
	doSensorLoop(&Serial);    
}


