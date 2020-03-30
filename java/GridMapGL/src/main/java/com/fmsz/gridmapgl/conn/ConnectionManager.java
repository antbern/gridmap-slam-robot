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
package com.fmsz.gridmapgl.conn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;
import imgui.Col;
import imgui.Dir;
import imgui.FocusedFlag;
import imgui.ImGui;
import imgui.MouseButton;
import imgui.MutableProperty0;

/**
 * This class handles the connection (only serial for now) to the robot. Has its own GUI for establishing the serial
 * connection to the correct port and with the correct speed settings
 * 
 * @author Anton
 *
 */
public class ConnectionManager {
	private static final byte COMMAND_ONCE = 0x01;
	private static final byte COMMAND_ENABLE = 0x02;
	private static final byte COMMAND_DISABLE = 0x04;
	private static final byte COMMAND_HOME_SENSOR = 0x05;
	private static final byte COMMAND_SET_RES = 0x08;

	private ConnectionThread thread = null;

	private int[] sensorDegreeResolutions = { 2, 3, 4, 5, 8, 10, 15, 20, 30, 45 };
	private List<String> sensorDegreeResolutionNames = new ArrayList<>();
	private int[] currentSelectedSensorDegreeResolution = { 2 };

	// for selecting the type of connection to make
	private int[] selectedConn = { 1 };
	private List<String> selectedConnNames = new ArrayList<>();
	private IConnection[] conn;

	// window open variables
	private MutableProperty0<Boolean> connectionOpen = new MutableProperty0<>(true);
	private MutableProperty0<Boolean> controlsOpen = new MutableProperty0<>(true);

	// Controls
	private final float[] selectedSpeed = { 10.0f };
	private final Vec2 arrowPadding = new Vec2(30, 30);
	private Dir lastDirection = Dir.None;

	// Pid values
	private final float[] pidTuningP = { 2.5f /*0.55f*/ };
	private final float[] pidTuningI = { 1.5f/*1.6445f*/ };
	private final float[] pidTuningD = { 0/*0.01016f*/ };
	private final float[] pidTuningTf = { 11.82f };

	public ConnectionManager() {
		// create sensor rate stuff
		for (int sensorDegreeResolution : sensorDegreeResolutions)
			sensorDegreeResolutionNames.add(String.valueOf(sensorDegreeResolution));

		// create connections
		conn = new IConnection[] { new SerialConnection(), new NetworkConnection() };

		// add their names and initialize
		for (int i = 0; i < conn.length; i++) {
			selectedConnNames.add(conn[i].getName());
			conn[i].init();
		}

	}

	public void doGUI(ImGui imgui) {
		if (imgui.begin("Connection", connectionOpen, 0)) {

			// let user select connection to use
			imgui.combo("Connection", selectedConn, selectedConnNames, 5);

			int selected = selectedConn[0];

			// let the selected connection draw its GUI
			conn[selected].doGUI(imgui);

			// button for connecting/disconnecting
			if (conn[selected].isConnected()) {
				if (imgui.button("Disconnect", new Vec2()))
					disconnect(conn[selected]);
			} else {
				if (imgui.button("Connect", new Vec2()))
					connect(conn[selected]);
			}
		}

		imgui.end();

		// controls window
		if (imgui.begin("Controls", controlsOpen, 0)) {

			if (imgui.button("Single", new Vec2())) {
				sendCommand(COMMAND_ONCE);
			}
			imgui.sameLine(0, 4);
			if (imgui.button("Enable", new Vec2())) {
				sendCommand(COMMAND_ENABLE);
			}
			imgui.sameLine(0, 4);
			if (imgui.button("Disable", new Vec2())) {
				sendCommand(COMMAND_DISABLE);
			}
			imgui.sameLine(0, 4);
			if (imgui.button("Home", new Vec2())) {
				sendCommand(COMMAND_HOME_SENSOR);
			}

			if (imgui.combo("Res", currentSelectedSensorDegreeResolution, sensorDegreeResolutionNames, 7)) {
				int selectedResolution = sensorDegreeResolutions[currentSelectedSensorDegreeResolution[0]];
				sendCommand(new byte[] { 0x08, (byte) selectedResolution });
			}

			imgui.separator();

			boolean controlsActive = imgui.isWindowFocused(FocusedFlag.ChildWindows);

			imgui.text("Status: %s", controlsActive ? "active" : "inactive");
			if (controlsActive)
				imgui.pushStyleColor(Col.Button, new Vec4(0.5, 0.5, 0.5, 1));

			Dir selectedDirection = Dir.None;
			// Up arrow
			imgui.newLine();
			imgui.sameLine(45, 0);

			imgui.arrowButtonEx("11", Dir.Up, arrowPadding, 0);
			if (imgui.isItemHovered(0) && imgui.isMouseDown(MouseButton.Left)) {
				selectedDirection = Dir.Up;
			}

			// Left arrow
			imgui.arrowButtonEx("10", Dir.Left, arrowPadding, 0);
			if (imgui.isItemHovered(0) && imgui.isMouseDown(MouseButton.Left)) {
				selectedDirection = Dir.Left;
			}

			// middle button...
			// imgui.sameLine(41 + 4);
			// imgui.button("O", new Vec2(41-8, 41-8));

			// Right arrow
			imgui.sameLine(82, 0);
			imgui.arrowButtonEx("13", Dir.Right, arrowPadding, 0);
			if (imgui.isItemHovered(0) && imgui.isMouseDown(MouseButton.Left)) {
				selectedDirection = Dir.Right;
			}

			// Down arrow
			imgui.newLine();
			imgui.sameLine(45, 0);
			imgui.arrowButtonEx("12", Dir.Down, arrowPadding, 0);
			if (imgui.isItemHovered(0) && imgui.isMouseDown(MouseButton.Left)) {
				selectedDirection = Dir.Down;
			}

			if (controlsActive)
				imgui.popStyleColor(1);

			// speed select slider
			imgui.dragFloat("Speed", selectedSpeed, 0, 0.01f, 0.0f, 14.0f, "%.2f", 2f);

			// only send to robot if there was a change
			if (selectedDirection != lastDirection) {
				lastDirection = selectedDirection;

				// update speeds
				float speedLeft = 0, speedRight = 0;
				float speed = selectedSpeed[0];

				switch (selectedDirection) {
				case Up:
					speedLeft = speedRight = speed;
					break;
				case Down:
					speedLeft = speedRight = -speed;
					break;
				case Left:
					speedLeft = -speed;
					speedRight = speed;
					break;
				case Right:
					speedLeft = speed;
					speedRight = -speed;
					break;

				default:
					break;

				}

				// send speed values to robot
				sendFloat(0x10, speedLeft);
				sendFloat(0x11, speedRight);
			}

			// PID tuning sliders
			if (imgui.dragFloat("Kp", pidTuningP, 0, 0.001f, 0, 10, "%.4f", 1f)) {
				sendFloat(0x15, pidTuningP[0]);
			}
			if (imgui.dragFloat("Ki", pidTuningI, 0, 0.001f, 0, 10, "%.4f", 1f)) {
				sendFloat(0x16, pidTuningI[0]);
			}
			if (imgui.dragFloat("Kd", pidTuningD, 0, 0.001f, 0, 10, "%.4f", 1f)) {
				sendFloat(0x17, pidTuningD[0]);
			}
			if (imgui.dragFloat("Tf = 1/x", pidTuningTf, 0, 0.01f, 0, 100, "%.2f", 1f)) {
				sendFloat(0x18, pidTuningTf[0]);
			}
		}
		imgui.end();

	}

	private void connect(IConnection conn) {
		// let the connection do the connecting
		conn.connect();

		// if it was successful in connecting, start the thread reading from the streams
		if (conn.isConnected()) {
			thread = new ConnectionThread(conn.getInputStream());
			thread.start();

			// send the initial configuration parameters
			sendCommand(new byte[] { COMMAND_SET_RES, (byte) sensorDegreeResolutions[currentSelectedSensorDegreeResolution[0]] });
			sendFloat(0x15, pidTuningP[0]);
			sendFloat(0x16, pidTuningI[0]);
			sendFloat(0x17, pidTuningD[0]);
			sendFloat(0x18, pidTuningTf[0]);

		} else {
			System.err.println("[ConnectionManager] Did not succeed in opening the connection!");
		}

	}

	private void disconnect(IConnection conn) {
		// check if the connection is open
		if (conn.isConnected()) {

			// send disable command first
			sendCommand(COMMAND_DISABLE);

			// stop the reading thread
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			thread = null;

			// let it disconnect
			conn.disconnect();
		}
	}

	/** Writes the given bytes to the robot */
	private void sendCommand(byte... command) {
		IConnection c = conn[selectedConn[0]];
		if (c.isConnected()) {
			try {
				c.getOutputStream().write(command, 0, command.length);
				c.getOutputStream().flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendFloat(int command, float value) {
		int bits = Float.floatToIntBits(value);
		sendCommand((byte) command, (byte) ((bits >> 24) & 0xff), (byte) ((bits >> 16) & 0xff), (byte) ((bits >> 8) & 0xff), (byte) (bits & 0xff));
	}

	public void dispose() {
		disconnect(conn[selectedConn[0]]);
	}
}
