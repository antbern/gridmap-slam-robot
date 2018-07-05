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

//import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
//import static org.lwjgl.opengl.GL20.*;
//import static org.lwjgl.opengl.GL30.*;

/**
 * A class wrapping an OpenGL VertexBuffer. It's basically a buffer of data.
 * 
 */
public class VertexBuffer {

	// a byte buffer for the data and FloatBuffer view of it
	private ByteBuffer byteBuffer;
	private FloatBuffer buffer;

	private int bufferId = -1;
	private boolean isBound = false;

	public VertexBuffer(int size) {
		// 3 is the size of the attributes
		byteBuffer = BufferUtils.createByteBuffer(size);
		buffer = byteBuffer.asFloatBuffer();

		// buffer.flip();
		// byteBuffer.flip();

		// generate buffer
		bufferId = glGenBuffers();

	}

	public void setVertices(float[] vertices) {
		setVertices(vertices, 0, vertices.length);
	}

	public void setVertices(float[] vertices, int offset, int count) {
		buffer.position(0);
		buffer.limit(count);
		buffer.put(vertices, offset, count);
		

		// send new vertices to GPU
		if (!isBound)
			bind();
		glBufferData(GL_ARRAY_BUFFER, byteBuffer, GL_DYNAMIC_DRAW);
	}

	public void bind() {
		glBindBuffer(GL_ARRAY_BUFFER, bufferId);
		isBound = true;
	}

	public void unbind() {
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		isBound = false;
	}

	public void dispose() {
		glDeleteBuffers(bufferId);
	}
}
