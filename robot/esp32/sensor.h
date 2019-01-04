#pragma once

#ifndef SENSOR_H
#define SENSOR_H

#include "Arduino.h"


void initSensor();
void doSensorLoop(Stream* stream);

void homeSensor();

#endif SENSOR_H