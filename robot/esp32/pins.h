#ifndef PINS_H
#define PINS_H

// Serial2 pins, cannot be changed
#define RDX2 16
#define TXD2 17

// stepper motor defines
#define STEPPER_EN 23
#define STEPPER_DIR 27
#define STEPPER_STEP 26
#define STEPPER_SENSOR 34

#define MICROSTEPPING 2
#define STEPS_PER_ROTATION (360*MICROSTEPPING)

// dc motor defines
#define MOTOR_RIGHT_EN 21
#define MOTOR_RIGHT_DIRA 19 
#define MOTOR_RIGHT_DIRB 18
#define MOTOR_RIGHT_ENCA 32
#define MOTOR_RIGHT_ENCB 35

#define MOTOR_LEFT_EN 22
#define MOTOR_LEFT_DIRA 5
#define MOTOR_LEFT_DIRB 4
#define MOTOR_LEFT_ENCA 25
#define MOTOR_LEFT_ENCB 33


#define ENC_COUNTS_PER_REV (32 * 30 * 2)


#endif