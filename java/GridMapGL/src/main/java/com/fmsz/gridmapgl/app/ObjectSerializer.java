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
package com.fmsz.gridmapgl.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import com.fmsz.gridmapgl.slam.GridMap;
import com.fmsz.gridmapgl.slam.GridMap.GridMapData;
import com.fmsz.gridmapgl.slam.Observation;
import com.fmsz.gridmapgl.slam.Odometry;
import com.fmsz.gridmapgl.slam.Observation.Measurement;

import glm_.vec2.Vec2;

public class ObjectSerializer {
	private ObjectSerializer() {
	};

	///////////////////////////////// ODOMETRY ///////////////////////////////////////////////
	public static void writeOdometry(DataOutputStream dos, Odometry o) {
		// TODO
	}

	public static Odometry readOdometry(DataInputStream dis) {
		// TODO
		return null;
	}

	/////////////////////////////////// OBSERVATION ////////////////////////////////////////////
	public static void writeObservation(DataOutputStream dos, Observation o) throws IOException {
		List<Measurement> measurements = o.getMeasurements();

		// write size first
		dos.writeShort(measurements.size());

		// then all measurements
		for (Measurement m : measurements)
			writeMeasurement(dos, m);
	}

	public static Observation readObservation(DataInputStream dis) throws IOException {
		Observation obs = new Observation();
		// read size first
		short length = dis.readShort();

		// then read all measurements
		for (int i = 0; i < length; i++)
			obs.addMeasurement(readMeasurement(dis));

		return obs;
	}

	///////////////////////////////// MEASUREMENT //////////////////////////////////////////
	public static void writeMeasurement(DataOutputStream dos, Measurement m) throws IOException {
		dos.writeDouble(m.angle);
		dos.writeDouble(m.distance);
		dos.writeBoolean(m.wasHit);
	}

	public static Measurement readMeasurement(DataInputStream dis) throws IOException {
		double angle = dis.readDouble();
		double distance = dis.readDouble();
		boolean wasHit = dis.readBoolean();
		return new Measurement(angle, distance, wasHit);
	}

	///////////////////////////////// GRID MAP ////////////////////////////////////////////
	public static void writeGridMap(DataOutputStream dos, GridMap map) throws IOException {
		// width, height, resolution, position, logData

		dos.writeFloat(map.getWorldSize().x); // map width (meters)
		dos.writeFloat(map.getWorldSize().y); // map height (meters)
		dos.writeFloat(map.getResolution()); // resolution (meters)
		dos.writeFloat(map.getPosition().x); // global map position x
		dos.writeFloat(map.getPosition().y); // global map position y
	}

	public static GridMap readGridMap(DataInputStream dis) throws IOException {
		// continue reading the parameters
		float width = dis.readFloat();
		float height = dis.readFloat();
		float resolution = dis.readFloat();
		float posX = dis.readFloat();
		float posY = dis.readFloat();

		// create the map object:
		GridMap map = new GridMap(width, height, resolution, new Vec2(posX, posY));

		return map;
	}

	///////////////////////////////// GRID MAP DATA ///////////////////////////////////////
	public static void writeGridMapData(DataOutputStream dos, GridMapData map) throws IOException {
		// write all the data:
		for (int i = 0; i < map.logData.length; i++) {
			dos.writeDouble(map.logData[i]);
		}
	}

	public static GridMapData readGridMapData(DataInputStream dis, GridMap gridMap) throws IOException {
		// create new data object
		GridMapData map = gridMap.createMapData(null);

		// then load in the data from the file
		for (int i = 0; i < map.logData.length; i++) {
			map.logData[i] = dis.readDouble();
		}

		return map;
	}

}
