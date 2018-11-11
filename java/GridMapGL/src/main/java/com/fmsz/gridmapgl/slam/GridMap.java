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

import com.fmsz.gridmapgl.app.Util;
import com.fmsz.gridmapgl.graphics.Color;
import com.fmsz.gridmapgl.graphics.ShapeRenderer;
import com.fmsz.gridmapgl.graphics.ShapeRenderer.ShapeType;
import com.fmsz.gridmapgl.slam.Observation.Measurement;

import glm_.vec2.Vec2;
import glm_.vec2.Vec2i;

/**
 * A data class for storing information about multiple grid maps with the same physical size and resolution. Also contains a lot of static
 * functions for manipulating the map in different ways.
 * 
 * Possible new name "GridMapManager"
 */
public class GridMap {

	/** the position of this map in the world (lower left corner) */
	private Vec2 position = new Vec2();

	/** the size of the map in world coordinates */
	private Vec2 worldSize = new Vec2();

	/** the size of the map in cells */
	private Vec2i gridSize = new Vec2i();

	/** array for storing the probability data */
	private double[] probData;

	/** the resolution of this GridMap, given in meters per cell */
	private float resolution;

	/** The RayIterator for finding all cells that overlap with the measurement ray */
	private static RayIterator rayIterator;

	public ArrayList<Vec2i> rays = new ArrayList<>();
	private final Vec2 tmp = new Vec2();

	private double[] likelihoodKernel;

	public static class GridMapData {
		public double[] logData, likelihoodData;
	}

	/** Create a new GridMap with the given width and height in meters using the the given resolution */
	public GridMap(float width, float height, float resolution, Vec2 position) {
		this.resolution = resolution;
		this.position.put(position);

		// calculate the required size in cells to fill the desired area based on the resolution
		gridSize.put(Math.ceil(width / resolution), Math.ceil(height / resolution));

		// calculate the "real" size of this grid map (potentially caused by Math.Ceil() above)
		worldSize.put(gridSize.x * resolution, gridSize.y * resolution);

		// create a temporary data array with the correct size
		probData = new double[(int) (gridSize.x * gridSize.y)];

		// compute the likelihood kernel
		double sigma = Math.sqrt(0.05 / resolution);
		likelihoodKernel = Util.generateGaussianKernel(sigma, (int) Math.ceil(sigma * 3));

		// create the correct ray iterator
		rayIterator = new RayIterator(gridSize.x, gridSize.y);

		// initialize with starting probability
		// reset();
	}

	/**
	 * Creates a new GridMapData object by copying the data from {@code other}. If {@code other} is {@code null}, a map with default values
	 * is created.
	 */
	public GridMapData createMapData(GridMapData other) {
		GridMapData map = new GridMapData();

		// create data structure
		map.logData = new double[(int) (gridSize.x * gridSize.y)];
		map.likelihoodData = new double[(int) (gridSize.x * gridSize.y)];

		// is this a copy operation or not?
		if (other == null) {
			// create new data; initialize fields
			Util.fastFillDouble(map.logData, Util.logOdds(0.5));
		} else {
			// copy data instead
			System.arraycopy(other.logData, 0, map.logData, 0, map.logData.length);
			System.arraycopy(other.likelihoodData, 0, map.likelihoodData, 0, map.likelihoodData.length);
		}

		return map;
	}

	/**
	 * Resets this GridMap by clearing all probability values to 0.5
	 */
	public void reset(GridMapData map) {
		// fill with default probability
		Util.fastFillDouble(map.logData, Util.logOdds(0.5));
	}

	public double getRawAt(GridMapData map, int x, int y) {
		return map.logData[x + y * gridSize.x];
	}

	public double getProbAt(GridMapData map, int x, int y) {
		return Util.invLogOdds(map.logData[x + y * gridSize.x]);
	}

	public double getRawAt(GridMapData map, Vec2 point) {
		tmp.put(point);
		tmp.minusAssign(position);
		tmp.divAssign(resolution);
		return map.logData[tmp.x.intValue() + tmp.y.intValue() * gridSize.x];

	}

	public double getLikelihood(GridMapData map, Vec2 point) {
		tmp.put(point);
		tmp.minusAssign(position);
		tmp.divAssign(resolution);
		return map.likelihoodData[tmp.x.intValue() + tmp.y.intValue() * gridSize.x];

	}

	/**
	 * Returns whether or not the given point is in this map
	 * 
	 * @param point
	 * @return
	 */
	public boolean pointInMap(Vec2 point) {
		tmp.put(point);
		tmp.minusAssign(position);
		tmp.divAssign(resolution);

		return !(tmp.x < 0 || tmp.y < 0 || tmp.x >= gridSize.x || tmp.y >= gridSize.y);
	}

	/** Processes a complete Observation packet and integrates the measurements into this map */
	public void integrateObservation(GridMapData map, Observation obs, Pose p) {
		for (Measurement m : obs.getMeasurements())
			applyMeasurement(map, m, p);
	}

	/** Updates this GridMap with one measurement */
	public void applyMeasurement(GridMapData map, Measurement m, Pose p) {
		// calculate start and end points in GRID coordinates (has to be done since the ray iterator works in grid coordinates)
		float startX = ((p.x - position.x) / resolution);
		float startY = ((p.y - position.y) / resolution);
		float endX = (m.getEndPointX(p) - position.x) / resolution;
		float endY = (m.getEndPointY(p) - position.y) / resolution;

		// calculate the measured distance (in grid coordinates)
		float measuredDistance = (float) m.distance / resolution; // Math.sqrt((startX - endX) * (startX - endX) + (startY - endY) * (startY
																	// - endY));

		rayIterator.init(startX, startY, endX, endY);
		while (rayIterator.hasNext()) {
			Vec2i cell = rayIterator.next();

			// calculate the distance to the center of the visited cell
			float distance = (float) Math
					.sqrt((startX - cell.x - 0.5f) * (startX - cell.x - 0.5f) + (startY - cell.y - 0.5f) * (startY - cell.y - 0.5f));
			map.logData[cell.x + cell.y * gridSize.x] += Util.logOdds(SensorModel.inverseSensorModel(distance, measuredDistance, m.wasHit,
					1 /* because we are in grid coordinates, and we want one cell to be occupied */));
		}
	}

	/**
	 * Computes the probability likelihood map based on this map
	 */
	public void computeLikelihoodMap(GridMapData map) {

		// Util.invLogOdds(this.logData, this.probData);

		// apply a "hard" filter that rounds the probability values to either 0, 0.5 or 1
		for (int i = 0; i < map.logData.length; i++) {
			if (map.logData[i] > Util.logOdds(0.5))
				probData[i] = 1;
			else if (map.logData[i] < Util.logOdds(0.5))
				probData[i] = 0;
			else
				probData[i] = 0.5;
		}

		// performs the gaussian bluring to create the likelihood field
		Util.doGaussianBlurdSeparable(this.probData, map.likelihoodData, gridSize.x, gridSize.y, likelihoodKernel);

	}

	/**
	 * Computes the probability of the observation given the map and the pose: p(z | m, x)
	 * 
	 * @param obs
	 *            the Observation, z
	 * @param p
	 *            the Pose, x
	 * @return p(z | m, x)
	 */
	private double zHit = 0.9, zRandom = 1 - zHit;

	public double probabilityOf(GridMapData map, Observation obs, Pose p) {
		double product = 1;

		for (Measurement m : obs.getMeasurements()) {
			// only care about measurements that hit something
			if (!m.wasHit)
				continue;

			// look up the probability of the end point being occupied and multiply by the product of the others
			int gridX = (int) ((m.getEndPointX(p) - position.x) / resolution);
			int gridY = (int) ((m.getEndPointY(p) - position.y) / resolution);

			if (!(gridX < 0 || gridY < 0 || gridX >= gridSize.x || gridY >= gridSize.y)) {
				double val = map.likelihoodData[gridX + gridY * gridSize.x];
				/*
				if (val == 0.0f) {
					System.out.println("Zero @ " + gridX + ", " + gridY);
					rays.add(new Vec2i(gridX, gridY));
				}
				*/
				
				//if(m.wasHit) val = 1-val;

				// multiply all probabilities together

				// if this is an unexplored cell, assume uniform distribution
				if (val == 0.5)
					product *= 1.0 / SensorModel.SENSOR_MAX_RANGE;
				else
					product *= zHit * val + zRandom * 1.0 / SensorModel.SENSOR_MAX_RANGE;
			}

		}

		return product;
	}
	/*

	public double probabilityOf(GridMapData map, Measurement m, Pose p) {

		// look up the probability of the end point being occupied and multiply by the product of the others
		int gridX = (int) ((m.getEndPointX(p) - position.x) / resolution);
		int gridY = (int) ((m.getEndPointY(p) - position.y) / resolution);

		if (!(gridX < 0 || gridY < 0 || gridX >= gridSize.x || gridY >= gridSize.y)) {
			double val = map.likelihoodData[gridX + gridY * gridSize.x];
			/*
			if (val == 0.0f) {
				System.out.println("Zero @ " + gridX + ", " + gridY);
				rays.add(new Vec2i(gridX, gridY));
			}
			*

			return zHit * val + zRandom * 1.0 / SensorModel.SENSOR_MAX_RANGE;
		}
		return 0;
	}
	*/

	/** Finds the pose that maximizes the observation likelihood using the given pose as a starting point */
	public Pose findBestPose(GridMapData map, Observation obs, Pose startPose) {
		Pose best = startPose;
		double maxProb = 0;
		Pose currentPose = new Pose(startPose);

		float xSpan = 0.2f, ySpan = 0.2f, thetaSpan = (float) (15 / 360f * 2 * Math.PI);
		float transStep = 0.04f, thetaStep = thetaSpan / 5;

		// test all combinations
		for (float dx = -xSpan; dx < xSpan; dx += transStep) {
			for (float dy = -ySpan; dy < ySpan; dy += transStep) {
				for (float dTheta = -thetaSpan; dTheta < thetaSpan; dTheta += thetaStep) {

					Pose p = new Pose(currentPose.x + dx, currentPose.y + dy, currentPose.theta + dTheta);
					double prob = probabilityOf(map, obs, p);
					if (prob > maxProb) {
						maxProb = prob;
						best = p;
					}

				}
			}
		}

		System.out.println(maxProb);

		return best;
	}

	public void render(ShapeRenderer rend, GridMapData map, boolean renderLines, boolean renderLikelihood) {
		rend.begin(ShapeType.FILLED);

		float x, y, value;
		for (int i = 0; i < map.logData.length; i++) {
			x = i % gridSize.x;
			y = i / gridSize.x;
			// float r = Util.invLogOdds(data[i]);

			// draw a rect for each grid cell, with the correct color
			if (renderLikelihood)
				value = (float) (map.likelihoodData[i]);
			else
				value = (float) (1.0f - Util.invLogOdds(map.logData[i]));

			rend.rect(x * resolution + position.x, y * resolution + position.y, resolution, resolution, Util.getColorBitsGrayscale(value));

		}

		for (Vec2i v : rays) {
			rend.rect(v.x * resolution + position.x, v.y * resolution + position.y, resolution, resolution,
					Color.colorToFloatBits(1, 0, 0, 1));
		}

		rend.end();

		if (renderLines) {
			rend.begin(ShapeType.LINE);

			for (x = 0; x <= gridSize.x; x++)
				rend.line(x * resolution + position.x, 0.0f + position.y, x * resolution + position.x, gridSize.y * resolution + position.x,
						Color.BLACK);

			for (y = 0; y <= gridSize.y; y++)
				rend.line(0.0f + position.x, y * resolution + position.y, gridSize.x * resolution + position.x, y * resolution + position.y,
						Color.BLACK);

			rend.end();
		}

		/*
		
		rend.begin(ShapeType.LINE);
		Vec2 start = new Vec2((gridSize.x / 2 + 0.5f) * resolution + position.x, (gridSize.y / 2 + 0.5f) * resolution + position.y);
		
		for (Vec2 p : rays)
			rend.line(start, p, Color.GREEN);
		
		rend.end();
		 */

		// TODO: do GUI?
	}

	public Vec2 getWorldSize() {
		return worldSize;
	}

	public float getResolution() {
		return resolution;
	}

	public Vec2 getPosition() {
		return position;
	}

}
