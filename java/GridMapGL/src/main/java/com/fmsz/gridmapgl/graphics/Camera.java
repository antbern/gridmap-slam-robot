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
package com.fmsz.gridmapgl.graphics;

import glm_.glm;
import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;

/**
 * The Camera class is responsible for creating the correct MVP-matrix to show the desired area of the "world" on
 * screen.
 * 
 * Inspiration taken from libgdx's Camera and OrthographicCamera classes.
 * https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/Camera.java
 * https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/OrthographicCamera.java
 * 
 * @author Anton
 *
 */
public class Camera {

	private Mat4 projection = new Mat4();

	public Mat4 combined = new Mat4();
	public Mat4 invCombined = new Mat4();

	// stores where the current center of the camera is
	public Vec2 position = new Vec2(0, 0);
	public float zoom = 1f;
	public Mat4 view = new Mat4();

	private boolean hasChanged = true;

	float viewportWidth, viewportHeight;

	private Vec2 currentScreenSize = new Vec2();

	public Camera() {

	}

	public void centerAt(Vec2 pos) {
		position.put(pos);
	}

	public void pan(Vec2 screenChange) {
		// convert the change from screen units to viewport units
		screenChange.divAssign(currentScreenSize);
		screenChange.timesAssign(viewportWidth, viewportHeight);
		screenChange.timesAssign(zoom);

		// apply the change
		position.plusAssign(screenChange);
		hasChanged = true;
	}

	public void zoom(Vec2 mousePos, float amount) {
		zoom *= (1 - 0.1f * amount);

		// clamp zoom
		if (zoom < 0.1)
			zoom = 0.1f;

		hasChanged = true;
	}

	public void resize(Vec2 newSize) {
		currentScreenSize.put(newSize);

		// recalculate the size of the viewport here
		viewportWidth = 10f;
		viewportHeight = viewportWidth * currentScreenSize.y / currentScreenSize.x;

		// need to recalculate the projection matrix here...

		// float height = 10f;
		// float width = height * newSize.x / newSize.y;
		// projection = (glm.INSTANCE.ortho(-width / 2, width / 2, -height / 2, height / 2));
		// projection = (glm.INSTANCE.ortho(0, newSize.x, 0, newSize.y));
		// invProj = projection.inverse();

		hasChanged = true;
	}

	public Vec2 unproject(Vec2 screenCoords) {

		screenCoords.put(screenCoords.x / currentScreenSize.x * viewportWidth * zoom, (currentScreenSize.y - screenCoords.y - 1) / currentScreenSize.y * viewportHeight * zoom);

		// adjust for the viewport size
		screenCoords.minusAssign(viewportWidth * zoom / 2, viewportHeight * zoom / 2);

		// adjust for the fact that the center of the screen is at "position"
		screenCoords.minusAssign(position);

		return screenCoords;
	}

	public void update() {
		if (!hasChanged)
			return;

		hasChanged = false;

		// recreate the projection matrix
		projection = (glm.INSTANCE.ortho(zoom * -viewportWidth / 2, zoom * viewportWidth / 2, zoom * -viewportHeight / 2, zoom * viewportHeight / 2));

		// recreate the view matrix
		//@formatter:off
		view.put(
			1,  0,  0,   position.x,
			0,  1,	0,   position.y, 
			0,  0,  1,   0,
			0,  0,  0,   1				
		).transposeAssign();
		//@formatter:on
		// calculate the combined transformation
		combined.put(projection);
		combined.timesAssign(view);

		// combined.inverse(invCombined);

	}
}
