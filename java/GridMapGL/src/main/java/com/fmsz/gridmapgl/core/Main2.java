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

import glm_.vec2.Vec2;
import glm_.vec2.Vec2d;
import glm_.vec2.Vec2i;
import glm_.vec4.Vec4;
import imgui.Cond;
import imgui.Context;
import imgui.ContextKt;
import imgui.IO;
import imgui.ImGui;
import imgui.impl.LwjglGL3;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;

import org.lwjgl.opengl.GL;
import com.fmsz.gridmapgl.app.GridMapApp;
import com.fmsz.gridmapgl.app.IApplication;
import com.fmsz.gridmapgl.graphics.Camera;

import uno.glfw.GlfwWindow;

import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;

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
	private uno.glfw.windowHint windowHint = uno.glfw.windowHint.INSTANCE;
	private LwjglGL3 lwjglGL3 = LwjglGL3.INSTANCE;
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
	private Vec2 currentWindowSize = new Vec2();

	// the application
	private IApplication app = null;

	// the camera and stuff
	private Camera cam;

	// variable to hold whether or not the loop is executed for the first time
	boolean first = true;

	// keep track of the last update time
	private double lastTime;

	private Vec4 clearColor = new Vec4(0.45f, 0.55f, 0.6f, 1f);

	public void run() {

		// initialize stuff
		init();

		// call the main loop until window is closed
		while (window.isOpen())
			loop();

		// cleanup
		dispose();

	}

	private void init() {

		glfw.init();
		windowHint.getContext().setVersion("3.2");
		windowHint.setProfile("core");

		currentWindowSize.put(1280, 720);
		window = new GlfwWindow(currentWindowSize.x.intValue(), currentWindowSize.y.intValue(), "GridMapGL using ImGUI");

		window.makeContextCurrent();
		glfw.setSwapInterval(1); // Enable vsync
		window.show();

		/*
		 * This line is critical for LWJGL's interoperation with GLFW's OpenGL context, or any context that is managed
		 * externally. LWJGL detects the context that is current in the current thread, creates the GLCapabilities instance and
		 * makes the OpenGL bindings available for use.
		 */
		GL.createCapabilities();

		// Setup ImGui binding
		// setGlslVersion(330); // set here your desidered glsl version
		ctx = new Context(null);
		lwjglGL3.init(window, true);

		// set correct .ini file
		io = igui.getIo();
		io.setIniFilename("config/imgui.ini");

		// Setup style
		igui.styleColorsDark(null);

		// imgui.styleColorsClassic(null);

		// Load Fonts
		/*
		 * - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use
		 * pushFont()/popFont() to select them. - addFontFromFileTTF() will return the Font so you can store it if you need to
		 * select the font among multiple. - If the file cannot be loaded, the function will return null. Please handle those
		 * errors in your application (e.g. use an assertion, or display an error and quit). - The fonts will be rasterized at a
		 * given size (w/ oversampling) and stored into a texture when calling FontAtlas.build()/getTexDataAsXXXX(), which
		 * ImGui_ImplXXXX_NewFrame below will call. - Read 'misc/fonts/README.txt' for more instructions and details. - Remember
		 * that in C/C++ if you want to include a backslash \ in a string literal you need to write a double backslash \\ !
		 */
		// ImGuiIO& io = ImGui::GetIO();
		// io.Fonts->AddFontDefault();
		// io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
		// io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
		// io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
		// io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
		// Font font = io.getFonts().addFontFromFileTTF("misc/fonts/ArialUni.ttf", 18f, new FontConfig(),
		// io.getFonts().getGlyphRangesJapanese());
		// assert (font != null);

		// more initializations

		// the view camera
		cam = new Camera();

		// set up input handling
		window.setCursorPosCallback(new Function1<Vec2d, Unit>() {
			@Override
			public Unit invoke(Vec2d pos) {
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

		window.setMouseButtonCallback(new Function3<Integer, Integer, Integer, Unit>() {
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

		window.setScrollCallback(new Function1<Vec2d, Unit>() {
			@Override
			public Unit invoke(Vec2d amount) {
				// if imgui wants the mouse, let it have it :)
				if (io.getWantCaptureMouse())
					return null;

				cam.zoom(currentMousePosition, amount.y.floatValue());
				return null;
			}
		});

		window.setFramebufferSizeCallback(new Function1<Vec2i, Unit>() {
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

		/*
		 * You can read the IO.wantCaptureMouse, IO.wantCaptureKeyboard flags to tell if dear imgui wants to use your inputs. -
		 * when IO.wantCaptureMouse is true, do not dispatch mouse input data to your main application. - when
		 * Io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application. Generally you may
		 * always pass all inputs to dear imgui, and hide them from your application based on those two flags.
		 */
		glfw.pollEvents();
		lwjglGL3.newFrame();

		// really bad workaround for ImGUI:s bad file loading (which loads from classpath, and saves outside the classpath...)
		// OBS! This has to be done AFTER the first lwjglGL3.newFrame() since it is responsible for doing the actual loadings
		if (first) {
			first = false;
			io.setIniFilename("res/config/imgui.ini");
		}

		// some default behavior
		igui.setWindowCollapsed(false, Cond.FirstUseEver);
		igui.setWindowSize(new Vec2(0, 0), Cond.FirstUseEver);
		igui.setNextWindowCollapsed(false, Cond.FirstUseEver);
		/*
		 * imgui.text("Hello, world!"); // Display some text (you can use a format string too) imgui.sliderFloat("float", f, 0f,
		 * 1f, "%.3f", 1f); // Edit 1 float using a slider from 0.0f to 1.0f imgui.colorEdit3("clear color", clearColor, 0); //
		 * Edit 3 floats representing a color imgui.checkbox("Demo Window", showDemo); // Edit bools storing our windows
		 * open/close state imgui.checkbox("Another Window", showAnotherWindow);
		 */
		/*
		 * if (imgui.button("Button", new Vec2())) // Buttons return true when clicked (NB: most widgets return true when
		 * edited/activated) counter[0]++;
		 * 
		 * imgui.sameLine(0f, -1f); imgui.text("counter = %d", counter[0]);
		 */

		// imgui.begin("Test", new boolean[] {true}, 0);

		// imgui.end();

		// Rendering
		gln.GlnKt.glViewport(window.getFramebufferSize());
		gln.GlnKt.glClearColor(clearColor);
		glClear(GL_COLOR_BUFFER_BIT);

		// let the camera recalculate stuff if needed
		cam.update();

		double currentTime = glfwGetTime();

		float delta = (float) (currentTime - lastTime);
		app.render(delta, cam);

		float updateTime = (float) (glfwGetTime() - currentTime);

		lastTime = currentTime;

		// show some debug info in the "Debug" Window:
		igui.text("App Delta:  %.2f ms (%.2f FPS)", delta * 1000, 1.0f / delta);
		igui.text("App Update: %.2f ms (%.2f FPS)", updateTime * 1000, 1.0f / updateTime);
		igui.text("Mouse: [%.2f, %.2f] m", currentMouseWorldPosition.x, currentMouseWorldPosition.y);

		igui.render();
		lwjglGL3.renderDrawData(igui.getDrawData());
		window.swapBuffers();

		gln.GlnKt.checkError("loop", true); // TODO remove

	}

	private void dispose() {
		app.dispose();

		// terminate stuff (this should be done lastly!)
		lwjglGL3.shutdown();
		ContextKt.destroy(ctx);

		glfw.terminate();

	}
}