package com.fmsz.gridmapgl.slam;

import com.fmsz.gridmapgl.math.MathUtil;

/** Stores robot configuration parameters */
public class Robot {
	/** The distance between the robots wheels [m] */
	public static final double WHEEL_DISTANCE = 0.22;

	/** The diameter of robots wheels [m] */
	public static final double WHEEL_DIAMETER = 0.063;

	/** Number of encoder steps per wheel revolution */
	public static final int MOTOR_STEPS_PER_REVOLUTION = 32 * 30;

	/** Number of stepper motor steps per sensor revolution */
	public static final int SENSOR_STEPS_PER_REVOLUTION = 96 * 5 * 2;

	/** The initial angle of the front sensor with respect to the forward direction of the robot */
	public static final float SENSOR_ANGLE_OFFSET = -MathUtil.PI / 2; // -90 deg

	private Robot() {

	}
}
