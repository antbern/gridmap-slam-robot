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
package com.fmsz.gridmapgl.math;

import org.apache.commons.math3.util.FastMath;

public class MathUtil {
	public static final float PI = (float) Math.PI;
	public static final float PI2 = (float) Math.PI * 2;

	public static final double RAD_TO_DEG = 180.0 / Math.PI;
	public static final double DEG_TO_RAD = Math.PI / 180.0;

	/**
	 * Methods for calculating sin and cos with float values instead of using double
	 */
	public static float sin(float radians) {
		return (float) FastMath.sin(radians);
	}

	public static double sin(double radians) {
		return FastMath.sin(radians);
	}

	public static float cos(float radians) {
		return (float) FastMath.cos(radians);
	}

	public static double cos(double radians) {
		return FastMath.cos(radians);
	}

	public static float atan2(float y, float x) {
		return (float) FastMath.atan2(y, x);
	}

	public static double atan2(double y, double x) {
		return FastMath.atan2(y, x);
	}

	// TODO: check this, do we want angles in the range [-PI, PI] instead of [0, 2PI] ???
	public static double angleDiff(double a, double b) {
		double dif = ((b - a) % (2 * Math.PI)); // in range
		if (a > b)
			dif += 2 * Math.PI;
		if (dif >= Math.PI)
			dif = -(dif - 2 * Math.PI);

		return dif;
	}

	public static double angleConstrain(double a) {
		while (a < Math.PI)
			a += Math.PI * 2;
		while (a > Math.PI)
			a -= Math.PI * 2;

		return a;
	}

}
