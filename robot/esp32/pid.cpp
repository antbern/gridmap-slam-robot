#include "pid.h"

// does the PID calculation and returns the new control output
double calculate_pid(PID_t* pid, double error, double h){
    // proportional part
    pid->P = pid->Kp * error;

    // derivative part
    pid->D = pid->Tf / (pid->Tf + h) * pid->D + pid->Kd / (pid->Tf + h) * (error - pid->e_old);

    // calculate output
    double u = pid->P + pid->I + pid->D;

    // integral part
    pid->I += pid->Ki * h * error;

    // save error
    pid->e_old = error;

    // return control signal
    return u;
}

void reset_pid(PID_t* pid){
    pid->e_old = 0;
    pid->P = 0;
    pid->I = 0;
    pid->D = 0;
}