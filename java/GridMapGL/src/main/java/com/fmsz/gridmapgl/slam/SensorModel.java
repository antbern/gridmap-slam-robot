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

public class SensorModel {
	/** Defines the max sensing range for the sensor and what it returns if it didn't find anything */
	public static final float SENSOR_MAX_RANGE = 4.0f;
	//public static final float SENSOR_NO_RESPONSE_THRESHHOLD = 8.0f;

	public static final double P_FREE = 0.30f;
	public static final double P_OCCUPPIED = 0.9f;
	public static final double P_PRIOR = 0.5f;

	/**
	 * Returns the probability of the currentDistance being occupied, given the measured distance and whether or not it was
	 * a hit. Also called the "inverse sensor model". Inspired by <a href="https://youtu.be/Cj91xll94U4?list=PLgnQpQtFTOGQrZ4O5QzbIHgl3b1JHimN_&t=3123" >this video</a>. 
	 */
	public static double inverseSensorModel(float currentDistance, float measuredDistance, boolean wasHit, float hitTolerance) {
		if (!wasHit)
			return (currentDistance < measuredDistance ? P_FREE : P_PRIOR);

		if (currentDistance < measuredDistance - hitTolerance / 2)
			return P_FREE;
		if (currentDistance > measuredDistance + hitTolerance / 2)
			return P_PRIOR;

		return P_OCCUPPIED;
	}
	
	/**
	 * An implementation of the above method, but using squared distances instead to increase performance (avoid a sqrt for each visited cell)
	 * You need to provide your own maxDistSq = (measuredDistance + hitTolerance/2)^2 and  minDistSq = (measuredDistance - hitTolerance/2)^2
	 */
	public static double inverseSensorModelSq(float currentDistanceSq, float measuredDistanceSq, boolean wasHit, float maxDistSq, float minDistSq) {
		if (!wasHit)
			return (currentDistanceSq < measuredDistanceSq ? P_FREE : P_PRIOR);

		if (currentDistanceSq < minDistSq)
			return P_FREE;
		if (currentDistanceSq > maxDistSq)
			return P_PRIOR;

		return P_OCCUPPIED;
	}
}
