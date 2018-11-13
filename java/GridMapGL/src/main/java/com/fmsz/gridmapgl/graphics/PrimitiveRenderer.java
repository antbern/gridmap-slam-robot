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

import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;

import static org.lwjgl.opengl.GL11.*;
//import static org.lwjgl.opengl.GL15.*;

import com.fmsz.gridmapgl.graphics.gl.Shader;
import com.fmsz.gridmapgl.graphics.gl.VertexArray;
import com.fmsz.gridmapgl.graphics.gl.VertexBuffer;
import com.fmsz.gridmapgl.graphics.gl.VertexBufferLayout;

public class PrimitiveRenderer {

	// the layout used by this renderer
	private final VertexBufferLayout layout = new VertexBufferLayout().push(GL_FLOAT, 3, false).push(GL_UNSIGNED_BYTE, 4, true);

	// the vertex array, vertex buffer and shader used by this renderer
	private VertexArray va;
	private VertexBuffer vb;
	private Shader shader;

	// stores and keeps track of vertices to draw
	private float vertices[];
	private final int maxVertices;
	private int vertexCount = 0;
	private int index = 0;

	// the projection*model*view matrix
	private final Mat4 projModelView = new Mat4();

	// the primitive type being drawn
	private int primitiveType = 0;

	public PrimitiveRenderer(int maxVertices) {
		this.maxVertices = maxVertices;

		// load our shader
		shader = new Shader("res/shaders/basic.shader");
		shader.bind();


		// create a vertex buffer to hold our data (size in bytes)
		vb = new VertexBuffer(maxVertices * layout.getStride());

		// create our view of the vertices (size in float = size in bytes / 4)
		vertices = new float[maxVertices * 4];

		// create vertex array and combine our vertex buffer with the layout
		va = new VertexArray();
		va.addBuffer(vb, layout);

	}

	public void setMVP(Mat4 projModelView) {
		this.projModelView.put(projModelView);
	}

	// begins a new drawing session of the specified primitive type
	public void begin(int primitiveType) {
		this.primitiveType = primitiveType;
	}

	public void vertex(Vec3 vertex) {
		vertex(vertex.x, vertex.y, vertex.z);
	}

	public void vertex(float x, float y, float z) {
		// if the buffer is full, do a "flush"
		if (vertexCount >= maxVertices) {
			end();
			begin(primitiveType);
		}

		vertices[index + 0] = x;
		vertices[index + 1] = y;
		vertices[index + 2] = z;

		index += 4;// 7;
		vertexCount++;
	}

	public void color(Vec4 color) {
		color(color.x, color.y, color.z, color.w);
	}

	public void color(Color color) {
		color(color.r, color.g, color.b, color.a);
	}

	public void color(float r, float g, float b) {
		color(r, g, b, 1.0f);
	}

	public void color(float r, float g, float b, float a) {

		// int colorI = ((int) (255 * a) << 24) | ((int) (255 * b) << 16) | ((int) (255 * g) << 8) | ((int) (255 * r));

		color(Color.colorToFloatBits(r, g, b, a));// Float.intBitsToFloat(colorI & 0xfeffffff);

		/*
				vertices[index + 3] = r;
				vertices[index + 4] = g;
				vertices[index + 5] = b;
				vertices[index + 6] = a;
		*/
	}

	public void color(float colorBits) {
		vertices[index + 3] = colorBits;
	}

	public void end() {

		// use our shader
		shader.bind();
		if (projModelView != null)
			shader.setUniformMat4("u_projModelView", projModelView);

		// upload our data
		vb.bind();
		vb.setVertices(vertices, 0, index);

		// do the actual drawing using a draw call
		va.bind();
		glDrawArrays(primitiveType, 0, vertexCount);

		// reset state
		vertexCount = 0;
		index = 0;
	}

	public int getVertexCount() {
		return vertexCount;
	}

	public int getMaxVertices() {
		return maxVertices;
	}

	public void dispose() {
		va.dispose();
		vb.dispose();
		shader.dispose();
	}

}
