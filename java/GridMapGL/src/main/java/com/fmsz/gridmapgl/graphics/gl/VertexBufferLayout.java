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

import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;

/**
 * Holds the Layout of the VertexBuffer
 * 
 * @author Anton
 *
 */
public class VertexBufferLayout {
	static class LayoutElement {
		int type; // OpenGl type
		int count; // Number of the type
		boolean normalized;

		public LayoutElement(int type, int count, boolean normalized) {
			this.type = type;
			this.count = count;
			this.normalized = normalized;
		}

		public static int getGLSize(int glType) {
			switch (glType) {
			case GL_FLOAT:
				return 4;
			case GL_UNSIGNED_INT:
				return 4;
			case GL_UNSIGNED_BYTE:
				return 1;
			}
			return 0;
		}

	}

	private ArrayList<LayoutElement> elements = new ArrayList<>();

	// keep track of the stride (the total size of the vertex)
	private int stride = 0;

	public VertexBufferLayout pushf(int count) {
		elements.add(new LayoutElement(GL_FLOAT, count, false));
		stride += LayoutElement.getGLSize(GL_FLOAT) * count;
		return this;
	}

	public VertexBufferLayout pushui(int count) {
		elements.add(new LayoutElement(GL_UNSIGNED_INT, count, false));
		stride += LayoutElement.getGLSize(GL_UNSIGNED_INT) * count;
		return this;
	}

	public VertexBufferLayout pushuc(int count) {
		elements.add(new LayoutElement(GL_UNSIGNED_BYTE, count, true));
		stride += LayoutElement.getGLSize(GL_UNSIGNED_BYTE) * count;
		return this;
	}
	
	public VertexBufferLayout push(int gltype, int count, boolean normailze) {
		elements.add(new LayoutElement(gltype, count, normailze));
		stride += LayoutElement.getGLSize(gltype) * count;
		return this;
	}

	public int getStride() {
		return stride;
	}

	public ArrayList<LayoutElement> getElements() {
		return elements;
	}

}
