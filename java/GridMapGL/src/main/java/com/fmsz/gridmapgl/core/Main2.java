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

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;

import org.lwjgl.glfw.GLFW;

import com.fmsz.gridmapgl.app.GridMapApp;
import com.fmsz.gridmapgl.app.IApplication;
import com.fmsz.gridmapgl.graphics.Camera;

import glm_.vec2.Vec2;
import glm_.vec2.Vec2d;
import glm_.vec2.Vec2i;
import glm_.vec4.Vec4;
import imgui.Cond;
import imgui.classes.Context;
import imgui.impl.gl.ImplGL3;
import imgui.classes.IO;
import imgui.ImGui;
import imgui.MutableProperty0;
import imgui.impl.glfw.ImplGlfw;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;
import uno.glfw.GlfwWindow;
import uno.glfw.VSync;
import uno.glfw.windowHint.Profile;


import static gln.GlnKt.glClearColor;
import static gln.GlnKt.glViewport;

public class Main2 {

	public static void main(String[] args) throws IOException {
		/*
		 * ClassLoader cl = ClassLoader.getSystemClassLoader();
		 * 
		 * URL[] urls = ((URLClassLoader)cl).getURLs();
		 * 
		 * for(URL url: urls){ System.out.println(url.getFile()); }
		 * 
		 * 
		 * System.out.println(ClassLoader.getSystemResource("config/imgui.ini"));
		 * //System.out.println(ClassLoader.getSystemClassLoader().getResources("Main2").hasMoreElements());
		 * System.out.println(ClassLoader.getSystemResource(
		 * "C:\\Users\\Anton\\Dropbox\\Programmering\\Java\\Projekt\\GridMapGL\\bin\\imgui.ini"));
		 * 
		 */
		// System.out.println(new
		// File(Paths.get("C:\\Users\\Anton\\Dropbox\\Programmering\\Java\\Projekt\\GridMapGL\\bin\\imgui.ini").toUri()));

		/*
		int size = 10;
		//@formatter:off
		float[] in = new float[] { 
			0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		};
		//@formatter:on

		print(in, size);

		float[] out = new float[in.length];

		System.out.println("Sum:");
		print(Util.doBoxBlur(size, size, in, out, 1), size);

		System.out.println("out:");
		print(out, size);

		Vec3 v1 = new Vec3(1, 2, 3);
		Vec3 v2 = new Vec3(4, 5, 6);

		System.out.println(v1.times(v2));

		Mat3 m = new Mat3();
		
		*/
		// System.out.println(Arrays.toString(Util.generateGaussianKernel(2, 5)));
		// System.out.println(Arrays.toString(Util.generateGaussianKernelIntegrate(2, 5)));

		// System.out.println(MathUtil.angleDiff(-Math.PI, Math.PI / 4));
		// System.out.println(3*Math.PI / 4);
		new Main2().run();

	}
	/*
		private static void print(float[] data, int width) {
			for (int i = 0; i < data.length; i++) {
				if (i % width == 0)
					System.out.println();
				System.out.print(String.format("%5.2f : ", data[i]));
			}
			System.out.println();
		}
		*/

	// The window handle
	private GlfwWindow window;
	private uno.glfw.glfw glfw = uno.glfw.glfw.INSTANCE;

	private ImplGlfw implGlfw;
	private ImplGL3 implGl3;
	private ImGui igui = ImGui.INSTANCE;
	private IO io;
	private Context ctx;

	// for handling input smoothly
	private Vec2 currentMouseWorldPosition = new Vec2();
	private Vec2 currentMousePosition = new Vec2();
	private Vec2 tmp = new Vec2();
	private boolean mousePressed = false;
	private boolean mouseDragged = false;

	// keep track of the current window size (in pixels)
	private Vec2 currentWindowSize = new Vec2(1280, 720);

	// the application
	private IApplication app = null;

	// the camera and stuff
	private Camera cam;

	// keep track of the last update time
	private double lastTime;

	private Vec4 clearColor = new Vec4(0.45f, 0.55f, 0.6f, 1f);
	
	private MutableProperty0<Boolean> debugOpen = new MutableProperty0<>(true);

	public void run() {

		// initialize stuff
		init();

		// call the main loop until window is closed
		window.loop((MemoryStack) -> {
			loop();
		});

		// cleanup
		dispose();

	}

	private void init() {
		////// PLATFORM SETUP //////

		// initialize glfw
		GLFW.glfwSetErrorCallback((error, description) -> System.out.println("Glfw Error " + error + ": " + description));
		glfw.init("3.3", Profile.core, true);

		// create GLFW window
		window = new GlfwWindow(1280, 720, "GridMapGL using ImGUI", null, new Vec2i(Integer.MIN_VALUE), true);
		window.init(true);

		// Enable vsync
		glfw.setSwapInterval(VSync.ON);

		// create context
		ctx = new Context(null);

		// set correct .ini file
		io = igui.getIo();
		// TODO: take this back once bug has been fixed!
		// io.setIniFilename("res/config/imgui.ini");

		// Setup style
		igui.styleColorsDark(null);

		// Setup Platform/Renderer bindings
		implGlfw = new ImplGlfw(window, true, null);
        implGl3 = new ImplGL3();


		/////// APPLICATION SPECIFIC SETUP ////////

		// create the view camera
		cam = new Camera();

		// set up input handling
		
		window.setCursorPosCB(new Function1<Vec2, Unit>() {
			@Override
			public Unit invoke(Vec2 pos) {
				// if imgui wants the mouse, let it have it :)
				if (io.getWantCaptureMouse())
					return null;

				// is this a "drag" event?
				if (mousePressed) {

					// only pan if the CTRL key is not down
					if (!io.getKeyCtrl()) {
						// yes! do panning:
						tmp.put(pos);
						tmp.minusAssign(currentMousePosition);
						tmp.timesAssign(1, -1);

						// tmp is now the dx and dy
						cam.pan(tmp);
					}

					mouseDragged = true;

				}

				// save current mouse position
				currentMousePosition.put(pos);

				// save current mouse position i world coordinates
				currentMouseWorldPosition.put(pos);
				cam.unproject(currentMouseWorldPosition);

				// if this was not a dragging, let the app know
				if (!mouseDragged || io.getKeyCtrl())
					app.mouseMove(currentMouseWorldPosition, mouseDragged);

				return null;
			}
		});

		window.setMouseButtonCB(new Function3<Integer, Integer, Integer, Unit>() {
			@Override
			public Unit invoke(Integer button, Integer action, Integer flags) {
				// if imgui wants the mouse, let it have it :)
				if (io.getWantCaptureMouse())
					return null;

				if (button == GLFW_MOUSE_BUTTON_LEFT) {
					mousePressed = action == GLFW_PRESS;

					if (action == GLFW_RELEASE) {
						if (!mouseDragged)
							app.mouseClick(currentMouseWorldPosition);

						mouseDragged = false;

					}

				}
				return null;
			}
		});

		window.setScrollCB(new Function1<Vec2d, Unit>() {
			@Override
			public Unit invoke(Vec2d amount) {
				// if imgui wants the mouse, let it have it :)
				if (io.getWantCaptureMouse())
					return null;

				cam.zoom(currentMousePosition, amount.getY().floatValue());
				return null;
			}
		});

		window.setFramebufferSizeCB(new Function1<Vec2i, Unit>() {
			public Unit invoke(Vec2i size) {
				// store current window size
				currentWindowSize.put(size);

				// call cameras resize-method
				cam.resize(currentWindowSize);
				return null;
			};
		});

		cam.resize(currentWindowSize);

		app = new GridMapApp();
		app.init();

		lastTime = glfwGetTime();

	}

	private void loop() {
		
		implGl3.newFrame();
		implGlfw.newFrame();
		igui.newFrame();

		// some default behavior (TODO: probablit not needed anymore)
		igui.setWindowCollapsed(false, Cond.FirstUseEver);
		igui.setWindowSize(new Vec2(0, 0), Cond.FirstUseEver);
		igui.setNextWindowCollapsed(false, Cond.FirstUseEver);

		// Rendering
		glViewport(window.getFramebufferSize());
		glClearColor(clearColor);
		glClear(GL_COLOR_BUFFER_BIT);
		glFrontFace(GL_CCW);
		glEnable(GL_CULL_FACE);

		// let the camera recalculate stuff if needed
		cam.update();

		double currentTime = glfwGetTime();

		float delta = (float) (currentTime - lastTime);

		// let the app do its thing: update, rendering etc.
		app.render(delta, cam);

		float updateTime = (float) (glfwGetTime() - currentTime);

		lastTime = currentTime;
		

		// show some debug info
		if (igui.begin("Debug", debugOpen, 0)) {
			igui.text("App Delta:  %.2f ms (%.2f FPS)", delta * 1000, 1.0f / delta);
			igui.text("App Update: %.2f ms (%.2f FPS)", updateTime * 1000, 1.0f / updateTime);
			igui.text("Mouse: [%.2f, %.2f] m", currentMouseWorldPosition.getX(), currentMouseWorldPosition.getY());

			igui.end();
		}

		// render ImGUI
		igui.render();
		glViewport(window.getFramebufferSize());
		implGl3.renderDrawData(igui.getDrawData());

		// this is from the example, not sure if necessary
		//if (ImguiKt.getDEBUG())
		//	gln.GlnKt.checkError("loop", true);

	}

	private void dispose() {
		app.dispose();

		// terminate stuff (this should be done lastly!)
		implGlfw.shutdown();
		implGl3.shutdown();
		ctx.destroy();

		window.destroy();
		glfw.terminate();

	}
}