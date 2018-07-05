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

import static org.lwjgl.opengl.GL11.*;

import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;

/**
 * Implements the rendering interface in IRenderer using the primitive renderer (a lot faster than immediate mode)
 * 
 * Lots of inspiration taken from https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/glutils/ShapeRenderer.java
 */
public class ShapeRenderer {
	public static enum ShapeType {
		POINT(GL_POINTS), LINE(GL_LINES), FILLED(GL_TRIANGLES);

		int primitiveType;

		private ShapeType(int primitiveType) {
			this.primitiveType = primitiveType;
		}
	}

	private ShapeType currentShapeType = null;

	private PrimitiveRenderer renderer;

	public ShapeRenderer() {
		renderer = new PrimitiveRenderer(1000);
	}

	public void line(Vec2 start, Vec2 end, Color color) {
		line(start.x, start.y, end.x, end.y, color);
	}

	public void line(float x1, float y1, float x2, float y2, Color color) {
		check(ShapeType.LINE, ShapeType.POINT, 2);
		renderer.color(color);
		renderer.vertex(x1, y1, 0);

		renderer.color(color);
		renderer.vertex(x2, y2, 0);
	}

	public void begin(ShapeType type) {
		currentShapeType = type;
		renderer.begin(currentShapeType.primitiveType);
	}

	public void end() {
		renderer.end();
		currentShapeType = null;
	}

	public void rect(Vec2 pos, Vec2 size, Color color) {
		rect(pos.x, pos.y, size.x, size.y, color.r, color.b, color.g, color.a);
	}

	public void rect(float x, float y, float width, float height, float r, float g, float b, float a) {
		rect(x, y, width, height, Color.colorToFloatBits(r, g, b, a));
	}

	public void rect(float x, float y, float width, float height, float colorBits) {
		check(ShapeType.LINE, ShapeType.FILLED, 8);

		if (currentShapeType == ShapeType.LINE) {
			renderer.color(colorBits);
			renderer.vertex(x, y, 0);
			renderer.color(colorBits);
			renderer.vertex(x + width, y, 0);

			renderer.color(colorBits);
			renderer.vertex(x + width, y, 0);
			renderer.color(colorBits);
			renderer.vertex(x + width, y + height, 0);

			renderer.color(colorBits);
			renderer.vertex(x + width, y + height, 0);
			renderer.color(colorBits);
			renderer.vertex(x, y + height, 0);

			renderer.color(colorBits);
			renderer.vertex(x, y + height, 0);
			renderer.color(colorBits);
			renderer.vertex(x, y, 0);
		} else {
			renderer.color(colorBits);
			renderer.vertex(x, y, 0);
			renderer.color(colorBits);
			renderer.vertex(x + width, y, 0);
			renderer.color(colorBits);
			renderer.vertex(x + width, y + height, 0);

			renderer.color(colorBits);
			renderer.vertex(x + width, y + height, 0);
			renderer.color(colorBits);
			renderer.vertex(x, y + height, 0);
			renderer.color(colorBits);
			renderer.vertex(x, y, 0);
		}

	}

	// draws a circle at the specified position

	public void circle(Vec2 pos, float radius, Color color) {
		circle(pos.x, pos.y, radius, color);
	}

	public void circle(float x, float y, float radius, Color color) {
		// calculate the number of segments needed for a "good" circle
		int numberOfSegments = Math.max(1, (int) (/*6*/ 12 * (float) Math.cbrt(radius))); // 10;// (int) (circumference / 20);
		circle(x, y, radius, color, numberOfSegments);

	}

	public void circle(float x, float y, float radius, Color color, int numberOfSegments) {
		// pre compute color bits
		final float colorBits = color.toFloatBits();
		
		// the angle between each circle segment
		double anglePerSegment = 2 * Math.PI / numberOfSegments;

		// precompute sin and cos
		float c = (float) Math.cos(anglePerSegment);
		float s = (float) Math.sin(anglePerSegment);

		// starting point
		float pointX = radius;
		float pointY = 0;

		if (currentShapeType == ShapeType.LINE) {
			check(ShapeType.LINE, null, numberOfSegments * 2 + 2);
			// place one vertex for each segment
			for (int i = 0; i < numberOfSegments; i++) {
				// place a point
				// glVertex2f(pointX, pointY);
				renderer.color(colorBits);
				renderer.vertex(pointX + x, pointY + y, 0);

				// rotate point using the "rotation matrix" multiplication to get to the next
				float pointXtmp = c * pointX - s * pointY;
				pointY = s * pointX + c * pointY;
				pointX = pointXtmp;

				renderer.color(colorBits);
				renderer.vertex(pointX + x, pointY + y, 0);

			}
			// add last point too
			renderer.color(colorBits);
			renderer.vertex(pointX + x, pointY + y, 0);

		} else {
			check(ShapeType.LINE, ShapeType.FILLED, numberOfSegments * 3 + 3);
			// place one vertex for each segment
			for (int i = 0; i < numberOfSegments; i++) {
				renderer.color(colorBits);
				renderer.vertex(x, y, 0);

				renderer.color(colorBits);
				renderer.vertex(pointX + x, pointY + y, 0);

				// rotate point using the "rotation matrix" multiplication to get to the next
				float pointXtmp = c * pointX - s * pointY;
				pointY = s * pointX + c * pointY;
				pointX = pointXtmp;

				renderer.color(colorBits);
				renderer.vertex(pointX + x, pointY + y, 0);

			}
			// add last point too
			renderer.color(colorBits);
			renderer.vertex(x, y, 0);
			renderer.color(colorBits);
			renderer.vertex(pointX + x, pointY + y, 0);
		}
		
		renderer.color(colorBits);
		renderer.vertex(x, y, 0);

	}

	private void check(ShapeType desiredShapeType, ShapeType other, int numberOfNewVertices) {
		if (currentShapeType == null)
			throw new IllegalStateException("ShapeRenderer: Begin must be called first!");

		// do we need to "restart" ?
		if (currentShapeType != desiredShapeType && currentShapeType != other) {
			end();
			begin(desiredShapeType);
		} else if (renderer.getVertexCount() + numberOfNewVertices > renderer.getMaxVertices()) {
			ShapeType type = currentShapeType;
			end();
			begin(type);
		}
	}

	public void setMVP(Mat4 mvp) {
		renderer.setMVP(mvp);
	}

	public void dispose() {
		renderer.dispose();
	}

}
