#pragma once

#ifndef MOTORS_H
#define MOTORS_H

#include "encoder.h"
#include "pid.h"


#define MOTOR_LOOP_FREQ 100
#define MOTOR_LOOP_PERIOD_US 1000/MOTOR_LOOP_FREQ

// PID values for the motor controllers
#define PID_KP 0.55276367534483     /* 0.610694929511361;//0.641817786149385 ;//6.458906368104240;//5.061601496636267;//3.286079178973016; */
#define PID_KI 1.64455966045303     /* 1.34329498731559;//1.169731890184110 ;//21.544597297186854;//59.064657944882540;//70.241507066863450; */
#define PID_KD 0.0101674410396297   /* 0.0220997968974464; */
#define PID_TF 1/11.8209539589613   /* 1/5.57670843490099; */



// struct defining the motor properties
typedef struct {
	encoder_t* enc;
	uint8_t pwm_channel;    
    int32_t odometry_counter = 0;
	PID_t pid;                                  // PID controller for velocity control
    double speed_reference = 0;                 // the reference value used for the PID-controller    
    struct{
        char PIN_EN, PIN_DIRA, PIN_DIRB;
    } pins;                                     // struct for the motors' pins
    
} motor_t;

// these are defined in motors.cpp
extern motor_t motor_left, motor_right;


void initMotors();
void resetMotors();

void stopMotorLoop();

void* motorLoop(void* parameter);
void handle_motor(motor_t* motor, double h);
void actuate_motor(motor_t* motor, double u);
double getMotorRotationSpeed(motor_t* motor, double dt);


#endif // MOTORS_H