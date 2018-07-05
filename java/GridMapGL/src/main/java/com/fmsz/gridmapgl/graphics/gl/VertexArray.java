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

//import static org.lwjgl.opengl.GL11.*;
//import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;

import com.fmsz.gridmapgl.graphics.gl.VertexBufferLayout.LayoutElement;

public class VertexArray {

	private int vaoId = -1;

	public VertexArray() {
		// generate buffer
		vaoId = glGenVertexArrays();
		glBindVertexArray(vaoId);

	}

	/**
	 * Adds a buffer to this VertexArray together with the specified layout
	 * 
	 */
	public void addBuffer(VertexBuffer vb, VertexBufferLayout layout) {
		bind();
		vb.bind();

		// attach the layout (this binds the layout and VBO to the VAO)

		int offset = 0;

		ArrayList<LayoutElement> elements = layout.getElements();
		for (int i = 0; i < elements.size(); i++) {
			LayoutElement element = elements.get(i);

			glEnableVertexAttribArray(i);
			glVertexAttribPointer(i, element.count, element.type, element.normalized, layout.getStride(), offset);

			offset += element.count * LayoutElement.getGLSize(element.type);
		}

	}

	public void bind() {
		glBindVertexArray(vaoId);
	}

	public void unbind() {
		glBindVertexArray(0);
	}

	public void dispose() {
		glDeleteVertexArrays(vaoId);
	}

}
