#pragma once

#ifndef SENSOR_H
#define SENSOR_H

#include "Arduino.h"
#include "WiFiClient.h"


void initSensor();
void doSensorLoop(WiFiClient* stream);

float readFloat(WiFiClient* stream);
void homeSensor();
void resetSensor();

#endif // SENSOR_H