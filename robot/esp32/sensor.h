#pragma once

#ifndef SENSOR_H
#define SENSOR_H

#include "Arduino.h"
#include "WiFiClient.h"


enum sensor_command {
    ENABLE_ONCE,
    ENABLE_CONTINOUSLY,
    DISABLE,
    SET_STEP_LENGTH,
    HOME_SENSOR,
    TERMINATE
};

typedef struct {
	sensor_command command;
	short data;
} sensor_queue_item_t;

typedef struct {
    QueueHandle_t queueHandle;
    WiFiClient* client;
} sensor_loop_parameters_t;

void initSensor();
void handleCommands(sensor_loop_parameters_t *params);
void doSensorLoop(void* parameter);

float readFloat(WiFiClient* stream);
void homeSensor();
void resetSensor();

#endif // SENSOR_H