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

/** Class implementing the fundamental methods for creating a Particle Filter */
public class ParticleFilter {

	public static class Particle {
		// mandatory
		public double weight = 0;

		// particle members
		public Pose pose;

		public Particle(Particle other) {
			this.weight = other.weight;
			this.pose = new Pose(other.pose);
		}

		public Particle(double weight, Pose pose) {
			this.weight = weight;
			this.pose = pose;
		}

	}

	private Particle[] particles;
	private int numberOfParticles;

	public ParticleFilter(int numberOfParticles) {
		this.numberOfParticles = numberOfParticles;

		// create list to hold our particles
		particles = new Particle[numberOfParticles];
	}

	public Particle[] getParticles() {
		return particles;
	}

	/**
	 * Does the resampling step of the particle filter (i.e selects numberOfParticles particles with a random chance
	 * proportional to their weight). Based on an implementation of the "low variance sampling" technique, see the book
	 * "Probabilistic Robotics" p. 110
	 */
	public void resample() {

		Particle[] newParticles = new Particle[numberOfParticles];
		int index = 0;

		double r = Math.random() * 1.0 / numberOfParticles;
		double c = particles[0].weight;
		int i = 1;

		for (int m = 1; m <= numberOfParticles; m++) {
			double U = r + (m - 1) * 1.0 / numberOfParticles;
			while (U > c) {
				i++;
				c += particles[i].weight;
			}
			// add the i:th particle to the new generation (note that this i a copy operation)
			newParticles[index++] = new Particle(particles[i]);

		}

		// make the new generation the current one
		particles = newParticles;

	}

}
