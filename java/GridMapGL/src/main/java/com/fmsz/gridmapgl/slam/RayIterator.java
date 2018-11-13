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

import java.util.Iterator;

import glm_.vec2.Vec2i;

/**
 * A class for iterating over all grid cells that overlap with a specific line.
 * Lots of inspiration taken from {@link https://playtechs.blogspot.com/2007/03/raytracing-on-grid.html}
 * 
 * @author Anton
 *
 */
public class RayIterator implements Iterator<Vec2i> {

	private final Vec2i vector = new Vec2i();
	private int x, y, width, height, x_inc, y_inc, n;
	private float dx, dy, error;

	/**
	 * Construct a new {@link RayIterator} width the specified width and height boundaries
	 * 
	 * @param width
	 *            the width of the grid
	 * @param height
	 *            the height of the grid
	 * 
	 */
	public RayIterator(int width, int height) {
		// save parameters
		this.width = width;
		this.height = height;
	}

	/**
	 * Initializes this {@link RayIterator} to iterate over the line specified. The start and end points are specified in grid coordinates,
	 * with the origin at the bottom left.
	 * 
	 * @param x0
	 *            the x-coordinate of the start point
	 * @param y0
	 *            the y-coordinate of the start point
	 * @param x1
	 *            the x-coordinate of the end point
	 * @param y1
	 *            the y-coordinate of the end point
	 * @param additionalSteps
	 * 			  an extra number of steps to perform after the end point has been reached
	 */
	public void init(float x0, float y0, float x1, float y1, int additionalSteps) {

		// set up stuff
		dx = Math.abs(x1 - x0);
		dy = Math.abs(y1 - y0);

		x = (int) Math.floor(x0);
		y = (int) Math.floor(y0);

		// start with at least one
		n = 1 + additionalSteps;

		// decide based on case
		if (dx == 0) {
			x_inc = 0;
			error = Float.POSITIVE_INFINITY;
		} else if (x1 > x0) {
			x_inc = 1;
			n += (int) (Math.floor(x1) - x);
			error = (float) ((Math.floor(x0) + 1 - x0) * dy);
		} else {
			x_inc = -1;
			n += x - (int) Math.floor(x1);
			error = (float) ((x0 - Math.floor(x0)) * dy);
		}

		if (dy == 0) {
			y_inc = 0;
			error -= Float.POSITIVE_INFINITY;
		} else if (y1 > y0) {
			y_inc = 1;
			n += (int) (Math.floor(y1)) - y;
			error -= (Math.floor(y0) + 1 - y0) * dx;
		} else {
			y_inc = -1;
			n += y - (int) (Math.floor(y1));
			error -= (y0 - Math.floor(y0)) * dx;
		}

	}

	@Override
	public boolean hasNext() {
		return n > 0 && !(x < 0 || x >= width || y < 0 || y >= height);
	}

	@Override
	public Vec2i next() {
		// cell coordinates to return
		vector.put(x, y);

		// move to next position
		if (error > 0) {
			y += y_inc;
			error -= dx;
		} else {
			x += x_inc;
			error += dy;
		}

		// decrease number of cells
		n -= 1;

		// return cell coordinates
		return vector;
	}

}
