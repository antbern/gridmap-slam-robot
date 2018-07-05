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

@Deprecated
public class TestIterator implements Iterator<Vec2i> {
	public static final TestIterator INSTANCE = new TestIterator();
	
	private final Vec2i vector = new Vec2i();
	private int x, y, startX, startY, dx, dy, x_inc, y_inc, width, height, error, n;

	// private constructor
	private TestIterator() {
	}

	public void startIteration(int startX, int startY, int endX, int endY, int width, int height) {
		this.width = width;
		this.height = height;
		// set up stuff
		dx = Math.abs(endX - startX);
		dy = Math.abs(endY - startY);

		x = startX;
		y = startY;
		n = 1 + dx + dy;

		x_inc = (endX > startX ? 1 : -1);
		y_inc = (endY > startY ? 1 : -1);

		error = dx - dy;
		dx *= 2;
		dy *= 2;
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
			x += x_inc;
			error -= dy;
		} else {
			y += y_inc;
			error += dx;
		}

		// decrease number of cells
		n -= 1;

		// return cell coordinates
		return vector;
	}

}
