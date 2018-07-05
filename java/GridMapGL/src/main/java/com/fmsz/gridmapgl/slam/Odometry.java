/*******************************************************************************
 *  Copyright 2018 Anton Berneving
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
package com.fmsz.gridmapgl.slam;

import com.fmsz.gridmapgl.math.MathUtil;

/** Class for storing the odometry measurement associated with this observation */
public class Odometry {

	double dCenter, dTheta;

	public Odometry(int leftCount, int rightCount) {
		// http://faculty.salina.k-state.edu/tim/robotics_sg/Control/kinematics/odometry.html

		// convert encoder values to traveled distance
		double dLeft = (double) leftCount / Robot.MOTOR_STEPS_PER_REVOLUTION * MathUtil.PI * Robot.WHEEL_DIAMETER;
		double dRight = (double) rightCount / Robot.MOTOR_STEPS_PER_REVOLUTION * MathUtil.PI * Robot.WHEEL_DIAMETER;

		// the amount the center has moved
		dCenter = (dLeft + dRight) / 2;

		// the angle moved
		dTheta = (dRight - dLeft) / Robot.WHEEL_DISTANCE;

		System.out.println(String.format("%.2f, %.2f", dCenter, dTheta * MathUtil.RAD_TO_DEG));
	}

	/**
	 * Applies this Odometry to the given pose, thus updating it
	 * 
	 * @param p
	 *            the Pose to change
	 */
	public void apply(Pose p) {
		// do this very simple for now, TODO: add noise
		p.x += MathUtil.cos(p.theta) * dCenter;
		p.y += MathUtil.sin(p.theta) * dCenter;
		p.theta = (float) MathUtil.angleConstrain(p.theta + dTheta);
	}
}