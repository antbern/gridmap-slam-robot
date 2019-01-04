#pragma once

#ifndef SENSOR_H
#define SENSOR_H

#include "Arduino.h"
#include "WiFiClient.h"


void initSensor();
void doSensorLoop(WiFiClient* stream);

void homeSensor();

#endif SENSOR_H