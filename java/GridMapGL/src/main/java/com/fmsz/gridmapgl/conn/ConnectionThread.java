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
package com.fmsz.gridmapgl.conn;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fmsz.gridmapgl.app.DataEventHandler;
import com.fmsz.gridmapgl.math.MathUtil;
import com.fmsz.gridmapgl.slam.Observation;
import com.fmsz.gridmapgl.slam.Odometry;
import com.fmsz.gridmapgl.slam.Robot;
import com.fmsz.gridmapgl.slam.SensorModel;
import com.fmsz.gridmapgl.slam.TimeFrame;

public class ConnectionThread extends Thread {

	private InputStream is;
	private Observation currentObservation;

	public ConnectionThread(InputStream is) {
		this.is = is;
		currentObservation = new Observation();
	}

	@Override
	public void run() {

		DataInputStream dis = new DataInputStream(this.is);

		while (!isInterrupted()) {
			try {
				// do we have the 5 bytes needed for a packet?
				if (dis.available() >= 7) {
					if (dis.read() == 0x55) {
						short steps = dis.readShort();
						short frontDistance = dis.readShort();
						short backDistance = dis.readShort();

						// System.out.println(steps + "-> " + frontDistance + ", " + backDistance);

						// if this is a "new packet" indicator?
						if (steps < 0) {
							// this packet also holds the odometry information

							// post the old packet, start creating a new one
							DataEventHandler.getInstance()
									.publish(new TimeFrame(currentObservation, new Odometry(frontDistance, backDistance)));
							currentObservation = new Observation();

						} else {
							// add a measurement for the front sensor
							float rad = steps / (float) Robot.SENSOR_STEPS_PER_REVOLUTION * MathUtil.PI2;
							// correct for the fact that the sensor is oriented with an angle offset
							rad += Robot.SENSOR_ANGLE_OFFSET;

							float dist = frontDistance / 1000f;
							if (dist < 0)
								currentObservation.addMeasurement(rad, SensorModel.SENSOR_MAX_RANGE, false);
							else
								currentObservation.addMeasurement(rad, dist, true);

							/*
							// add a measurement for the back sensor (+180 deg)
							rad += MathUtil.PI;
							dist = backDistance / 1000f;
							if (dist < 0)
								currentObservation.addMeasurement(rad, SensorModel.SENSOR_MAX_RANGE, false);
							else
								currentObservation.addMeasurement(rad, dist, true);
							*/
						}

					}
				}

			} catch (IOException ioe) {
				ioe.printStackTrace();
				break;
			}
		}
	}
}
