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
package com.fmsz.gridmapgl.graphics.gl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
//import org.lwjgl.opengl.GL11;
//import org.lwjgl.opengl.GL15;
//import org.lwjgl.opengl.GL20;
//import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

@Deprecated
public class VertexBufferObjectWithVAO {

	private ByteBuffer byteBuffer;
	private FloatBuffer buffer;

	private int vaoId = -1, bufferId = -1;

	public VertexBufferObjectWithVAO(int numVerticies) {
		// 3 is the size of the attributes
		byteBuffer = BufferUtils.createByteBuffer(numVerticies * 3 * 4);
		buffer = byteBuffer.asFloatBuffer();

		// buffer.flip();
		// byteBuffer.flip();

		// create the VAO
		vaoId = glGenVertexArrays();
		// GL30.glBindVertexArray(vaoId);

		// create the VBO
		bufferId = glGenBuffers();

		// bind buffer to attach layout
		bind();

		// attach the layout (this binds the layout and VBO to the VAO)
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 2, 0);

		// bind();
	}

	public void setVertices(float[] vertices) {
		setVertices(vertices, 0, vertices.length);
	}

	public void setVertices(float[] vertices, int offset, int count) {
		buffer.position(0);
		buffer.put(vertices, offset, count);
		buffer.limit(count);

		// send new vertices to GPU
		glBufferData(GL_ARRAY_BUFFER, byteBuffer, GL_DYNAMIC_DRAW);
	}

	public void draw() {
		glDrawArrays(GL_TRIANGLES, 0, 3);
	}

	public void bind() {
		glBindVertexArray(vaoId);
		glBindBuffer(GL_ARRAY_BUFFER, bufferId);
	}

	public void unbind() {
		glBindVertexArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	public void dispose() {
		glDeleteBuffers(bufferId);
		glDeleteVertexArrays(vaoId);

		// remove the buffer?
	}
}
