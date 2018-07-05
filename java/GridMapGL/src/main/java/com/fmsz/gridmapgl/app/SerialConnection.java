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
package com.fmsz.gridmapgl.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;
import imgui.Col;
import imgui.FocusedFlags;
import imgui.ImGui;
import imgui.internal.Dir;

/**
 * This class handles the serial connection to the robot. It parses the incoming data and generates ScanPackets of the lidar scans (as well
 * as odometry information in the future)
 * 
 * Has its own GUI for establishing the serial connection to the correct port and with the corrent speed settings
 * 
 * @author Anton
 *
 */
public class SerialConnection implements SerialPortPacketListener {
	private static final byte COMMAND_ONCE = 0x01;
	private static final byte COMMAND_ENABLE = 0x02;
	private static final byte COMMAND_DISABLE = 0x04;
	private static final byte COMMAND_HOME_SENSOR = 0x05;
	private static final byte COMMAND_SET_RES = 0x08;

	private int[] currentSelectedPortName = new int[] { 0 };
	private List<String> portNames = new ArrayList<>();

	private int[] currentSelectedBaudRate = new int[] { 7 };
	private int[] baudRates = new int[] { 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200 };
	private List<String> baudRateNames = new ArrayList<>();

	private SerialPort[] availablePorts = null;
	private SerialPort currentPort = null;

	private SerialConnectionThread thread = null;

	private int[] sensorDegreeResolutions = { 2, 3, 4, 5, 8, 10, 15, 20, 30, 45 };
	private List<String> sensorDegreeResolutionNames = new ArrayList<>();
	private int[] currentSelectedSensorDegreeResolution = { 2 };

	// Controls
	private final float[] selectedSpeed = { 100.0f };
	private final Vec2 arrowPadding = new Vec2(10, 10);
	private Dir lastDirection = Dir.None;

	public SerialConnection() {

		// create baud rate stuff
		for (int baudRate : baudRates)
			baudRateNames.add(String.valueOf(baudRate));

		// create baud rate stuff
		for (int sensorDegreeResolution : sensorDegreeResolutions)
			sensorDegreeResolutionNames.add(String.valueOf(sensorDegreeResolution));

		refreshList();
	}

	public void doGUI(ImGui imgui) {
		if (imgui.begin("Serial", null, 0)) {

			// drop-down menu for selecting a port
			imgui.combo("Port", currentSelectedPortName, portNames, 5);
			imgui.combo("Baud", currentSelectedBaudRate, baudRateNames, 7);
			if (currentPort == null) {
				if (imgui.button("Connect", new Vec2())) {
					if (currentSelectedPortName[0] != -1)
						connect(availablePorts[currentSelectedPortName[0]], baudRates[currentSelectedBaudRate[0]]);
				}
			} else {
				if (imgui.button("Disconnect", new Vec2())) {
					disconnect();
				}
			}
			imgui.sameLine(0);
			if (imgui.button("Refresh", new Vec2())) {
				refreshList();
			}

			imgui.text("Sensor controls:");
			if (imgui.button("Single", new Vec2())) {
				sendCommand(COMMAND_ONCE);
			}
			imgui.sameLine(0);
			if (imgui.button("Enable", new Vec2())) {
				sendCommand(COMMAND_ENABLE);
			}
			imgui.sameLine(0);
			if (imgui.button("Disable", new Vec2())) {
				sendCommand(COMMAND_DISABLE);
			}
			imgui.sameLine(0);
			if (imgui.button("Reset", new Vec2())) {
				if (currentPort != null) {
					currentPort.clearDTR();
					currentPort.setDTR();
					currentPort.clearDTR();
				}
			}

			if (imgui.combo("Res", currentSelectedSensorDegreeResolution, sensorDegreeResolutionNames, 7)) {
				int selectedResolution = sensorDegreeResolutions[currentSelectedSensorDegreeResolution[0]];
				sendCommand(new byte[] { 0x08, (byte) selectedResolution });
			}

			if (imgui.button("Home", new Vec2())) {
				sendCommand(COMMAND_HOME_SENSOR);
			}

			// imgui.beginColumns("ID", 3, ColumnsFlags.NoResize.getI() | ColumnsFlags.GrowParentContentsSize.getI());

		}

		imgui.end();

		// controls window

		if (imgui.begin("Controls", null, 0)) {
			boolean controlsActive = imgui.isWindowFocused(FocusedFlags.ChildWindows);

			Dir selectedDirection = Dir.None;

			imgui.text("Status: %s", controlsActive ? "active" : "inactive");

			if (controlsActive)
				imgui.pushStyleColor(Col.Button, new Vec4(0.5, 0.5, 0.5, 1));

			// Up arrow
			imgui.newLine();
			imgui.sameLine(45);
			imgui.arrowButton(11, Dir.Up, arrowPadding, 0);
			if (imgui.isItemHovered(0) && imgui.isMouseDown(0)) {
				selectedDirection = Dir.Up;
			}

			// Left arrow
			imgui.arrowButton(10, Dir.Left, arrowPadding, 0);
			if (imgui.isItemHovered(0) && imgui.isMouseDown(0)) {
				selectedDirection = Dir.Left;
			}

			// middle button...
			// imgui.sameLine(41 + 4);
			// imgui.button("O", new Vec2(41-8, 41-8));

			// Right arrow
			imgui.sameLine(82);
			imgui.arrowButton(13, Dir.Right, arrowPadding, 0);
			if (imgui.isItemHovered(0) && imgui.isMouseDown(0)) {
				selectedDirection = Dir.Right;
			}

			// Down arrow
			imgui.newLine();
			imgui.sameLine(45);
			imgui.arrowButton(12, Dir.Down, arrowPadding, 0);
			if (imgui.isItemHovered(0) && imgui.isMouseDown(0)) {
				selectedDirection = Dir.Down;
			}

			if (controlsActive)
				imgui.popStyleColor(1);

			// speed select slider
			imgui.dragFloat("Speed", selectedSpeed, 0.1f, 0.0f, 255.0f, "%.2f", 2f);

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
				sendSpeedCommand(true, speedLeft);
				sendSpeedCommand(false, speedRight);
			}

		}
		imgui.end();

	}

	private void connect(SerialPort port, int baudRate) {
		if (currentPort != null)
			return;

		// deactivate RTS line to avoid reset on the Arduino (does not currently work)
		port.setBaudRate(baudRate);

		port.clearRTS();

		// System.out.println(port.setDTR());
		// try to open the port
		if (!port.openPort()) {
			// there was an error!
		}
		// System.out.println(port.clearDTR());

		// tell the port we are listening for data!
		// port.addDataListener(this);
		// new DataInputStream(port.getInputStream()).
		// new ByteArra

		// start reading thread
		thread = new SerialConnectionThread(port.getInputStream());
		thread.start();

		// send sync packet
		/*
		try {
			port.getOutputStream().write(new byte[] { 0x00, (byte) 0xff, 0x00 });
		} catch (IOException e) {
			System.err.println("Error writing sync packet!");
			e.printStackTrace();
		}
		*/

		// this is now our active port
		currentPort = port;

		// wait for arduino to begome ready...
		try {
			Thread.sleep(2500);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}

		// send some initial configuration parameters
		sendCommand(new byte[] { COMMAND_SET_RES, (byte) sensorDegreeResolutions[currentSelectedSensorDegreeResolution[0]] });

	}

	/** Writes the given bytes to the sensor board */
	private void sendCommand(byte... command) {
		if (currentPort != null) {
			try {
				currentPort.getOutputStream().write(command, 0, command.length);
				currentPort.getOutputStream().flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendSpeedCommand(boolean leftMotor, float speed) {
		int bits = Float.floatToIntBits(speed);
		sendCommand((byte) (leftMotor ? 0x10 : 0x11), (byte) ((bits >> 24) & 0xff), (byte) ((bits >> 16) & 0xff),
				(byte) ((bits >> 8) & 0xff), (byte) (bits & 0xff));
	}

	private void disconnect() {

		if (currentPort != null) {

			// send disable command first
			sendCommand(COMMAND_DISABLE);

			// currentPort.removeDataListener();

			// stop the reading thread
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			thread = null;

			// try to close port
			if (!currentPort.closePort())
				throw new IllegalStateException("SerialPort: Could not close port " + currentPort.getSystemPortName());

			currentPort = null;
		}
	}

	public void refreshList() {

		// fetch available ports
		availablePorts = SerialPort.getCommPorts();

		// clear current names
		portNames.clear();

		// add each available ports name to the selectable list
		for (SerialPort port : availablePorts) {
			portNames.add(port.getSystemPortName() + "(" + port.getPortDescription() + ")");
		}

		// set the preselected one to the last one
		currentSelectedPortName[0] = availablePorts.length - 1;
	}

	public void dispose() {
		disconnect();
	}

	@Override
	public int getPacketSize() {
		return 5;
	}

	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		byte[] newData = event.getReceivedData();

		if (newData[0] == 0x55) {
			System.out.println("yes");
		}

		System.out.println(Util.bytesToHex(newData));
	}

}
