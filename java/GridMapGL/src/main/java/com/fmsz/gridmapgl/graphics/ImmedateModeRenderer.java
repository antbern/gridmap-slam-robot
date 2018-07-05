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

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBEasyFont.stb_easy_font_print;

import org.lwjgl.opengl.GL11;

import com.fmsz.gridmapgl.math.Vec2f;

/**
 * Implements the rendering interface in IRenderer using OpenGL immediate mode (a.k.a slow as hell xD)
 * 
 * 
 */
@Deprecated
public class ImmedateModeRenderer implements IRenderer {

	// the size of the buffer, around 270 bytes/character
	private static final int CHAR_BUFFER_SIZE = 270 * 20;
	private ByteBuffer charBuffer = null;

	@Override
	public void init() {
		// init bytebuffer for font rendering
		charBuffer = BufferUtils.createByteBuffer(CHAR_BUFFER_SIZE);

		// set up and link font buffer
		//glEnableClientState(GL_VERTEX_ARRAY);
		//glVertexPointer(2, GL_FLOAT, 16, charBuffer);
	}

	@Override
	public void line(Vec2f start, Vec2f end, Color color) {
		glColor3f(color.r, color.g, color.b);
		glBegin(GL11.GL_LINES);

		glVertex2f(start.x, start.y);
		glVertex2f(end.x, end.y);

		glEnd();
	}

	@Override
	public void rect(Vec2f pos, Vec2f size, Color color, boolean fill) {

	}

	// draws a circle at the specified position
	@Override
	public void circle(Vec2f pos, float radius, Color color, boolean fill) {
		// the circumference
		float circumference = (float) (Math.PI * 2 * radius);

		// calculate the number of segments needed for a "good" circle
		int numberOfSegments = (int) (circumference / 20);

		// the angle between each circle segment
		double anglePerSegment = 2 * Math.PI / numberOfSegments;

		// precompute sin and cos
		float c = (float) Math.cos(anglePerSegment);
		float s = (float) Math.sin(anglePerSegment);

		// starting point
		float pointX = radius;
		float pointY = 0;


		// save state
		glPushMatrix();

		// set color
		glColor3f(color.r, color.g, color.b);

		// move to circle center
		glTranslatef(pos.x, pos.y, 0);

		// begin drawing based on the fill flag
		glBegin((fill ? GL11.GL_TRIANGLE_FAN : GL11.GL_LINE_STRIP));

		// place one vertex for each segment
		for (int i = 0; i < numberOfSegments; i++) {
			// place a point
			glVertex2f(pointX, pointY);

			// rotate point using the "rotation matrix" multiplication to get to the next
			float pointXtmp = c * pointX - s * pointY;
			pointY = s * pointX + c * pointY;
			pointX = pointXtmp;
		}
		// add last point too
		glVertex2f(pointX, pointY);
		
		glEnd();

		// restore state
		glPopMatrix();

	}

	@Override
	public void text(Vec2f pos, String text, Color color) {

		// create quads
		int quads = stb_easy_font_print(0, 0, text, null, charBuffer);

		// save state
		glPushMatrix();

		// Zoom
		glScalef(2, 2, 1f);

		// move to correct location
		glTranslatef(pos.x, pos.y, 0);

		// set text color
		glColor3f(color.r, color.g, color.b);

		// draw call
		glDrawArrays(GL_QUADS, 0, quads * 4);

		// restore state
		glPopMatrix();

	}

	@Override
	public void centerAt(Vec2f pos) {
	}

	@Override
	public void pan(Vec2f change) {
	}

	@Override
	public void zoom(Vec2f mousePos, float amount) {
	}

	@Override
	public void resize(Vec2f newSize) {

	}
}
