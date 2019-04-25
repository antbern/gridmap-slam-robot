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

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well1024a;

/** Class for storing the odometry measurement associated with this observation */
public class Odometry {
	// shared random generator for the normal distributions
	private static RandomGenerator rndGen = new Well1024a();

	// used to add noise when applying to Pose
	private NormalDistribution ndCenter, ndTheta = new NormalDistribution();

	public double dCenter, dTheta;
	private double dCenterSD, dThetaSD;

	public Odometry(double dCenter, double dTheta) {
		this.dCenter = dCenter;
		this.dTheta = dTheta;
		recalculateStdDev();
	}

	public Odometry(int leftCount, int rightCount) {
		// http://faculty.salina.k-state.edu/tim/robotics_sg/Control/kinematics/odometry.html

		// convert encoder values to traveled distance
		double dLeft = (double) leftCount / Robot.MOTOR_STEPS_PER_REVOLUTION * MathUtil.PI * Robot.WHEEL_DIAMETER;
		double dRight = (double) rightCount / Robot.MOTOR_STEPS_PER_REVOLUTION * MathUtil.PI * Robot.WHEEL_DIAMETER;

		// the amount the center has moved
		dCenter = (dLeft + dRight) / 2;

		// the angle moved
		dTheta = (dRight - dLeft) / Robot.WHEEL_DISTANCE;

		recalculateStdDev();
	}

	/**
	 * Calculates the standard deviation used when applying random Gaussian noise to a Pose
	 */
	public void recalculateStdDev() {
		// calculate desired standard deviations, +/- 2SD contains 95.4%
		// basic principle: default base case + % of changed amount
		dCenterSD = (0.01 + Math.abs(dCenter) * 0.05) / 2;
		dThetaSD = 5 * MathUtil.DEG_TO_RAD + 0.1 * Math.abs(dTheta);

		ndCenter = new NormalDistribution(rndGen, dCenter, dCenterSD);
		ndTheta = new NormalDistribution(rndGen, dTheta, dThetaSD);

	}

	/**
	 * Applies this Odometry to the given pose, thus updating it
	 * 
	 * @param p
	 *            the Pose to change
	 */
	public void apply(Pose p) {

		// take a sample from this very simple motion model
		double d = ndCenter.sample();
		double theta = ndTheta.sample();

		// do not add noise when we have not moved!
		/*
		if (dCenter == 0) {
			d = 0;
			theta = 0;
		}
		*/

		// apply movement, angle first since this controls the direction of the traveled distance
		p.theta = (float) MathUtil.angleConstrain(p.theta + theta);
		p.x += MathUtil.cos(p.theta) * d;
		p.y += MathUtil.sin(p.theta) * d;

	}

	// Calculates the probability of being at pose p when starting at pose start given this odometry
	public double probabiliyOf(Pose start, Pose p) {
		// calculate moved distance
		double dist = Math.sqrt(start.x - p.x) * (start.x - p.x) + (start.y - p.y) * (start.y - p.y);
		return ndCenter.probability(dist) * ndTheta.probability(p.theta);
	}
}