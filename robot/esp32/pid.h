#pragma once

#ifndef PID_H
#define PID_H

typedef struct {
    double Kp = 0.0;
    double Ki = 0.0;
    double Kd = 0.0;
    double Tf = 0.0;

    double P = 0.0;
    double I = 0.0;
    double D = 0.0;

    double e_old = 0.0;
} PID_t;

double calculate_pid(PID_t* pid, double error, double h);

#endif PID_H