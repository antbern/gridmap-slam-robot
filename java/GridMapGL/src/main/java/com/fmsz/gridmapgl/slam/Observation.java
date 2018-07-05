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

import java.util.ArrayList;
import java.util.List;

import com.fmsz.gridmapgl.math.MathUtil;

/**
 * Class containing all data for a single lidar scan (a complete revolution)
 * 
 * @author Anton
 *
 */
public class Observation {

	/**
	 * Class for storing a single measurement
	 * 
	 * @author Anton
	 *
	 */
	public static class Measurement {
		public double angle, distance;
		public boolean wasHit;

		public Measurement(double angle, double distance, boolean wasHit) {
			this.angle = angle;
			this.distance = distance;
			this.wasHit = wasHit;
		}

		/** Constructs a new measurement based on a given pose and world coordinates of end point */
		public Measurement(float x, float y, boolean wasHit, Pose p) {
			float dx = x - p.x;
			float dy = y - p.y;
			float a = MathUtil.atan2(dy, dx);

			this.distance = (float) Math.sqrt(dx * dx + dy * dy);
			this.angle = a - p.theta;
			this.wasHit = wasHit;
		}

		public float getEndPointX(Pose pose) {
			return (float) (pose.x + distance * MathUtil.cos(angle + pose.theta));
		}

		public float getEndPointY(Pose pose) {
			return (float) (pose.y + distance * MathUtil.sin(angle + pose.theta));
		}

	}

	private ArrayList<Measurement> measurements;

	public Observation() {
		measurements = new ArrayList<>();

	}

	public void addMeasurement(float angle, float distance, boolean wasHit) {
		measurements.add(new Measurement(angle, distance, wasHit));
	}

	public void addMeasurement(Measurement m) {
		measurements.add(m);
	}

	public List<Measurement> getMeasurements() {
		return measurements;
	}

	public int getNumberOfMeasurements() {
		return measurements.size();
	}

	public void reset() {
		measurements.clear();
	}
}
