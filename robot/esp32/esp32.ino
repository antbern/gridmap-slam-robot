#include "WiFi.h"
#include <pthread.h>

#include "pins.h"
#include "encoder.h"
#include "pid.h"
#include "motors.h"
#include "sensor.h"

#include "mdns.h"

#define SERVER_PORT 5555

static const char* TAG = "Main";

//// VARIABLES ////

// vairables for handling WiFi connection
#include "wifi_settings.h"

// webserver handling the connection on port 5555
WiFiServer server(SERVER_PORT);

// thread for handling the motor pid control
pthread_t motorThread;

void setup() {
    // enable debug serial
    Serial.begin(115200);

	pinMode(BUILTIN_LED, OUTPUT);

	//we must initialize rorary encoder and motors first
	initEncoders();
	initMotors();
	initSensor();

	// set up networked connection
	
	startWifi();
	initialize_mdns();
}

// initializes mDNS to make the device discoverable by others on the network, taken from the examples/protocols/mdns example in esp-idf
void initialize_mdns() {
	// initialize mDNS
	ESP_ERROR_CHECK( mdns_init() );

	// set mDNS hostname
	ESP_ERROR_CHECK( mdns_hostname_set("esp32robot") );

	// set friendly name
	ESP_ERROR_CHECK( mdns_instance_name_set("ESP32 Robot") );

	// finally advertise our service
	ESP_ERROR_CHECK( mdns_service_add(NULL, "_controller", "_tcp", SERVER_PORT, NULL, 0) );

	Serial.println("Successfully registered mDNS service");
}

void startWifi(){
	// Connect to Wi-Fi network with SSID and password
	Serial.print("Connecting to ");
	Serial.println(ssid);
	WiFi.begin(ssid, password);

	int failureCounter = 6000 / 200; // 6 seconds
	while (WiFi.status() != WL_CONNECTED && --failureCounter > 0) {
		// blink built in LED to signal that we are trying to connect
		blinkBuiltinLED(1, 200);
	}

	// if failure, start internal Wifi Server instead
	if(failureCounter == 0){

		// indicate using built in LED
		blinkBuiltinLED(10, 50);
		
		// start SoftAP instead
		WiFi.mode(WIFI_AP);
		WiFi.softAP(apSSID, apPwd);
		Serial.println("Wait 100 ms for AP_START...");
		delay(100);
		
		//Serial.println("Set softAPConfig");
		//WiFi.softAPConfig(local_IP, local_IP, subnet);
		
		
		IPAddress IP = WiFi.softAPIP();
		Serial.print("AP IP address: ");
		Serial.println(IP);

		// indicate using built in LED
		blinkBuiltinLED(10, 50);
	} else {
		// Configures static IP address
		//if (!WiFi.config(local_IP, gateway, subnet, primaryDNS, secondaryDNS)) {
		//Serial.println("STA Failed to configure");
		//}
		
		// Print local IP address and start web server
		Serial.println("");
		Serial.println("WiFi connected.");
		Serial.println("IP address: ");
		Serial.println(WiFi.localIP());
	}

	Serial.println("Starting listening...");


	// start server listening
	server.begin();
}


void loop() {
	// Listen for incoming clients
	WiFiClient client = server.available();   

	// a new client connects?
	if (client) {                             
		Serial.println("Client Connected");    

		// home the sensor
		homeSensor();

		// start a new thread for the motors and for reading the sensors
		xTaskCreate(motorLoop, "motorLoop", 10000, NULL, 1, NULL);
		xTaskCreate(doSensorLoop, "sensorLoop", 10000, &client, 1, NULL);

		// do regular loop to handle the incoming control messages
		while (client.connected()) { 
			handleCommands(&client);
		}

		Serial.println("Client Disconnected");

		// stop motors and reset sensor
		stopMotorLoop();
		resetSensor();
	} 
}

void blinkBuiltinLED(unsigned int times, unsigned int period){
	period /= 2;
	for(int i = 0; i < times; i++){
		digitalWrite(BUILTIN_LED, HIGH);
		delay(period);
		digitalWrite(BUILTIN_LED, LOW);
		delay(period);
	}
}
