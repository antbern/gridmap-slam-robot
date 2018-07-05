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
package com.fmsz.gridmapgl.core;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import com.fmsz.gridmapgl.app.GridMapApp;
import com.fmsz.gridmapgl.app.IApplication;
import com.fmsz.gridmapgl.graphics.IRenderer;
import com.fmsz.gridmapgl.graphics.ImmedateModeRenderer;
import com.fmsz.gridmapgl.graphics.PrimitiveRenderer;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;

import java.io.IOException;
import java.nio.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

@Deprecated
public class Main {
	// mklink /J jdk_current jdkXXXXXX

	// The window handle
	private long window;

	// the application
	private IApplication app = null;
	private IRenderer renderer = null;

	public void run() {
		// initialize
		init();

		// lwjglGL3.init(window, true);

		// do stuff!
		loop();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		System.out.println(glfwGetVersionString());
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

		// Create the window
		window = glfwCreateWindow(800, 600, "GridMapGL", NULL, NULL);
		// GlfwWin
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated
		// or released.
		glfwSetKeyCallback(window, (long window, int key, int scancode, int action, int mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		});

		// setup window resize callback
		glfwSetWindowSizeCallback(window, (long window, int width, int height) -> {
			System.out.println("Resize: " + width + "x" + height);

		});

		// mouse move callback
		glfwSetCursorPosCallback(window, (long window, double mouseX, double mouseY) -> {
			// System.out.println(mouseX + ":" + mouseY);
		});

		// mouse button callback
		glfwSetMouseButtonCallback(window, (long window, int button, int action, int mods) -> {
			if ((button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS))
				System.out.println("Press");
			// System.out.println("Mouse button:" + button + ", " + action + ", " + mods);
		});

		// mouse scroll callback
		glfwSetScrollCallback(window, (long window, double xOffset, double yOffset) -> {
			// System.out.println("Scroll: " + xOffset + ", " + yOffset);

		});

		// System.out.println(glfwGetTime());

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);

		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
	}

	private void loop() {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		// initialize app and rendering
		app = new GridMapApp();
		app.init();

		renderer = new ImmedateModeRenderer();
		renderer.init();

		PrimitiveRenderer pRend = new PrimitiveRenderer(100);

		// create a Vertex Array to bind vbo and attributes together
		// int vao = GL30.glGenVertexArrays();
		// GL30.glBindVertexArray(vao);

		// VBO TEST
/*
		//@formatter:off
		FloatBuffer b = BufferUtils.createFloatBuffer(60);
		b.put(new float[]{
			1.0f, 1.0f,
			0f, 0f,
			-1.0f, 0f
		});
		
		b.flip();
		*/
		//@formatter:on

		/*
		int buffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
		
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 2, 0);
		
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, b, GL15.GL_STATIC_DRAW);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		*/
		/*
		float[] verticies = new float[] { 1.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f };
		
		VertexBuffer vb = new VertexBuffer(20);
		vb.bind();
		vb.setVertices(verticies);
		
		VertexArray va = new VertexArray();
		va.addBuffer(vb, new VertexBufferLayout().push(GL_FLOAT, 2));
		*/
		/*
				// shader
				Shader shader = new Shader("res/shaders/basic.shader");
				shader.bind();
				shader.setUniform4f("u_Color", 1.0f, 0.0f, 0.0f, 1.0f);
				shader.setUniform4f("u_Color", 0.0f, 1.0f, 0.0f, 1.0f);
		*/
		////////////

		// setup drawing
		/*
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0.0, 800, 600, 0.0, -1.0, 1.0);
		glMatrixMode(GL_MODELVIEW);
		*/
		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		glClearColor(43f / 255f, 43f / 255f, 43f / 255f, 0f); // BG color

		// for measuring the time taken for update and render
		double time, lastTime, updateTime, renderTime;

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			lastTime = glfwGetTime();
			//// UPDATING ////

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();

			// app.update(0.0f);

			//////////////////

			time = glfwGetTime();
			updateTime = time - lastTime;
			lastTime = time;

			//// RENDER ////

			// draw out Vertex Array object
			// GL30.glBindVertexArray(vao);
			// glDrawArrays(GL_TRIANGLES, 0, 3);

			/*
			vb.bind();
			verticies[1] -= 0.01f;
			vb.setVertices(verticies);
			
			va.bind();
			
			// issue a draw call
			glDrawArrays(GL_TRIANGLES, 0, 3);
			*/

			// app.render(renderer);
			/*
			 * 
			 * glBegin(GL11.GL_TRIANGLES); glColor3f(0, 1, 0); glVertex2f(0, 0); glVertex2f(100, 0); glVertex2f(0, 100); glEnd();
			 * 
			 * 
			 * glColor3f(1, 0, 0); circle(100, 200, 100, true);
			 */
			Vec4 color = new Vec4(1.0f, 1.0f, 0.0f, 1.0f);

			pRend.begin(GL_TRIANGLES);
			pRend.color(color);
			pRend.vertex(new Vec3(0.0f, 0.0f, 0.0f));
			pRend.color(color);
			pRend.vertex(new Vec3(0.0f, 0.5f, 0.0f));
			pRend.color(color);
			pRend.vertex(new Vec3(0.3f, 0.3f, 0.0f));
			pRend.end();

			// renderer.text(Vec2f.tmp.set(100, 0), "This is a very long text", Color.RED);

			////////////////
			renderTime = glfwGetTime() - lastTime;

			// draw update and render time (OBS! this is not included in the rendering time for practical reasons)
			// renderer.text(Vec2f.tmp.set(2, 5), String.format("U:%6.3fms%nR:%6.3fms", updateTime * 1000, renderTime * 1000), Color.WHITE);

			// do swapBuffers outside time measurement loop to only mesure my own drawing
			// code
			glfwSwapBuffers(window); // swap the color buffers
		}
		/*
		shader.dispose();
		va.dispose();
		vb.dispose();
		*/

	}

	public static void main(String[] args) throws IOException {
		new Main().run();
	}
}