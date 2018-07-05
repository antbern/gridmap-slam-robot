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

import static org.lwjgl.opengl.GL11.GL_FALSE;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.BufferUtils;

import glm_.mat4x4.Mat4;

import static org.lwjgl.opengl.GL20.*;

public class Shader {

	private enum ShaderType {
		NONE(-1), VERTEX(0), FRAGMENT(1);

		int index;

		private ShaderType(int index) {
			this.index = index;
		}
	}

	private final int id;
	private HashMap<String, Integer> cachedUniforms = new HashMap<>();

	private FloatBuffer mat4Buffer = BufferUtils.createFloatBuffer(16);

	public Shader(String filename) {
		// read file
		String[] source = parseShaderFile(filename);

		// create the shader program
		this.id = createShader(source[ShaderType.VERTEX.index], source[ShaderType.FRAGMENT.index]);
	}

	private int createShader(String vertexShader, String fragmentShader) {
		// create program
		int program = glCreateProgram();

		// create vertex shader
		int vs = compileShader(vertexShader, GL_VERTEX_SHADER);
		int fs = compileShader(fragmentShader, GL_FRAGMENT_SHADER);

		// attach our shaders to the program
		glAttachShader(program, vs);
		glAttachShader(program, fs);

		glLinkProgram(program);
		glValidateProgram(program);

		// delete the shaders, they are stored in our program now
		glDeleteShader(vs);
		glDeleteShader(fs);

		return program;
	}

	private int compileShader(String source, int shaderType) {
		// create vertex shader
		int id = glCreateShader(shaderType);
		glShaderSource(id, source);
		glCompileShader(id);

		// error handling
		int[] result = new int[1];
		glGetShaderiv(id, GL_COMPILE_STATUS, result);

		if (result[0] == GL_FALSE) {

			// get error message
			String error = glGetShaderInfoLog(id);

			System.out.println("Failed to compile " + (shaderType == GL_VERTEX_SHADER ? "vertex" : "fragment") + " shader");
			System.out.println(error);

			glDeleteShader(id);
			return 0;
		}

		return id;
	}

	public void setUniform4f(String name, float v0, float v1, float v2, float v3) {
		glUniform4f(getUniformLocation(name), v0, v1, v2, v3);
	}

	public void setUniformMat4(String name, Mat4 matrix4f) {
		matrix4f.to(mat4Buffer);
		glUniformMatrix4fv(getUniformLocation(name), false, mat4Buffer);
	}

	/**
	 * Retrieves the uniform location based on its name
	 * 
	 * @param name
	 *            the name of the uniform
	 * @return the location of the specified uniform
	 */
	private int getUniformLocation(String name) {
		if (cachedUniforms.containsKey(name))
			return cachedUniforms.get(name);

		int location = glGetUniformLocation(id, name);
		if (location == -1)
			System.out.println("Warning: uniform " + name + " does not exist!");

		cachedUniforms.put(name, location);

		return location;
	}

	public void bind() {
		glUseProgram(id);
	}

	public void unbind() {
		glUseProgram(0);
	}

	public void dispose() {
		glDeleteProgram(id);
	}

	private static String[] parseShaderFile(String filename) {

		List<String> lines = null;

		try {
			lines = Files.readAllLines(Paths.get(filename));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		ShaderType current = ShaderType.NONE;

		String[] contents = new String[] { "", "" };

		for (String line : lines) {
			if (line.startsWith("#shader")) {
				if (line.contains("vertex")) {
					// set mode to vertex
					current = ShaderType.VERTEX;
				} else if (line.contains("fragment")) {
					// set mode to vertex
					current = ShaderType.FRAGMENT;
				}
			} else {
				// add the read content
				contents[current.index] += line + "\n";
			}
		}

		return contents;
	}
}
