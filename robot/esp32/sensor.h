#pragma once

#ifndef SENSOR_H
#define SENSOR_H

#include "Arduino.h"
#include "WiFiClient.h"


void initSensor();
void handleCommands(WiFiClient* stream);
void doSensorLoop(void* parameter);

float readFloat(WiFiClient* stream);
void homeSensor();
void resetSensor();

#endif // SENSOR_H