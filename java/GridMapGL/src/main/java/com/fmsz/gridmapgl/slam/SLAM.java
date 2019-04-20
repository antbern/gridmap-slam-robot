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

import com.fmsz.gridmapgl.math.MathUtil;
import com.fmsz.gridmapgl.slam.GridMap.GridMapData;

import glm_.vec2.Vec2;

/** Class implementing the SLAM algorithm using particle filters */
public class SLAM {
	private GridMap gridMap;

	// particle stuff
	public class Particle {
		public double weight;
		public Pose pose;
		public GridMapData m;

		public Particle(Pose x, GridMapData m) {
			this.pose = x;
			this.m = m;
		}

		// constructor that copies the other particle
		public Particle(Particle other) {
			this.weight = other.weight;
			this.pose = new Pose(other.pose);
			this.m = gridMap.createMapData(other.m);
		}

	}

	private ArrayList<Particle> particles;
	private int numParticles = 500;

	private Particle strongestParticle = null;

	// private Random rand = new Random();

	public SLAM() {
		gridMap = new GridMap(6.0f, 6.0f, 0.05f, new Vec2(-3.0f, -3.0f));

		particles = new ArrayList<>(numParticles);

		reset();
	}

	/** Resets all the particles to an initial Pose of (0, 0, 0) and with a blank map */
	public void reset() {
		for (int i = 0; i < numParticles; i++) {
			Particle p = new Particle(new Pose(0, 0, 0), gridMap.createMapData(null));

			// uniform weight initially
			p.weight = 1.0 / numParticles;

			particles.add(p);
		}
		strongestParticle = particles.get(0);

	}

	/** The main SLAM update loop */
	public double update(Observation z, Odometry u) {
		// skip map integration if this measurement involved a large rotation as the measurements are very uncertain
		boolean skipUpdate = Math.abs(u.dTheta) > MathUtil.DEG_TO_RAD * 30;

		strongestParticle = null;

		// do logic for all particles
		double weightSum = 0;
		for (Particle p : particles) {
			// first sample a new pose from the motion model based on the given controls (odometry)
			p.pose = sampleMotionModel(p.pose, u);

			// calculate the weight of this particle as p(z|x,m)
			gridMap.computeLikelihoodMap(p.m);
			
			// optimize pose position to maximize measurement likelihood
			p.pose = gridMap.findBestPose(p.m, z, p.pose);

			p.weight = gridMap.probabilityOf(p.m, z, p.pose);
			weightSum += p.weight;

			if (!skipUpdate) {

				// integrate the measurement into the particles map
				gridMap.integrateObservation(p.m, z, p.pose);

			}

			// store the particle with highest weight
			if (strongestParticle == null)
				strongestParticle = p;
			else {
				if (p.weight > strongestParticle.weight)
					strongestParticle = p;
			}

		}

		// normalize particle weights
		for (Particle p : particles)
			p.weight /= weightSum;

		// resample
		double neff = calculateNeff();

		// if (neff < numParticles / 2)
		// resample();

		return neff;

	}

	public void resample() {
		ArrayList<Particle> newParticles = new ArrayList<>(numParticles);

		double r = Math.random() * 1.0 / numParticles;
		double c = particles.get(0).weight;
		int i = 0;

		for (int m = 1; m <= numParticles; m++) {
			double U = r + (m - 1) * 1.0 / numParticles;
			while (U > c) {
				i++;
				c += particles.get(i).weight;
			}
			// add the i:th particle to the new generation (note that this a copying operation)
			newParticles.add(new Particle(particles.get(i)));

		}

		// make the new generation the current one
		particles = newParticles;
	}

	private Pose sampleMotionModel(Pose x, Odometry u) {
		Pose p = new Pose(x);
		
		// apply odometry motion
		if (u != null)
			u.apply(p);

		return p;
	}

	public Pose getWeightedPose() {
		// calculate a weighted average of the position
		double xSum = 0, ySum = 0, thetaSum = 0, weightSum = 0;

		for (Particle p : particles) {
			xSum += p.pose.x * p.weight;
			ySum += p.pose.y * p.weight;
			thetaSum += MathUtil.angleConstrain(p.pose.theta) * p.weight;
			weightSum += p.weight;
		}

		return new Pose((float) (xSum / weightSum), (float) (ySum / weightSum), (float) (thetaSum / weightSum));

	}

	public double calculateNeff() {
		double sum = 0;
		for (Particle p : particles)
			sum += p.weight;

		double sqaredSum = 0;
		for (Particle p : particles)
			sqaredSum += (p.weight / sum) * (p.weight / sum);

		return 1.0 / sqaredSum;
	}

	public ArrayList<Particle> getParticles() {
		return particles;
	}

	public Particle getStrongestParticle() {
		return strongestParticle;
	}

	public GridMap getGridMap() {
		return gridMap;
	}

}
