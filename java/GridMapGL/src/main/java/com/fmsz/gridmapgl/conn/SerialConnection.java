package com.fmsz.gridmapgl.conn;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

import glm_.vec2.Vec2;
import imgui.ImGui;

public class SerialConnection implements IConnection {

	private int[] currentSelectedPortName = new int[] { 0 };
	private List<String> portNames = new ArrayList<>();

	private int[] currentSelectedBaudRate = new int[] { 7 };
	private int[] baudRates = new int[] { 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200 };
	private List<String> baudRateNames = new ArrayList<>();

	private SerialPort[] availablePorts = null;
	private SerialPort currentPort = null;

	@Override
	public String getName() {
		return "Serial";
	}

	@Override
	public void init() {

		// create baud rate stuff
		for (int baudRate : baudRates)
			baudRateNames.add(String.valueOf(baudRate));

		refreshList();
	}

	@Override
	public void doGUI(ImGui imgui) {
		// drop-down menu for selecting a port
		imgui.combo("Port", currentSelectedPortName, portNames, 5);
		imgui.combo("Baud", currentSelectedBaudRate, baudRateNames, 7);

		if (imgui.button("Refresh", new Vec2())) {
			refreshList();
		}
		imgui.sameLine(0, 4);
		if (imgui.button("Reset", new Vec2())) {
			if (currentPort != null) {
				currentPort.clearDTR();
				currentPort.setDTR();
				currentPort.clearDTR();
			}
		}
	}

	private void refreshList() {

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

	@Override
	public void connect() {
		if (currentSelectedPortName[0] != -1)
			connect(availablePorts[currentSelectedPortName[0]], baudRates[currentSelectedBaudRate[0]]);
	}

	private void connect(SerialPort port, int baudRate) {
		if (currentPort != null)
			return;

		// deactivate RTS line to avoid reset on the Arduino (does not currently work)
		port.setBaudRate(baudRate);

		port.clearRTS();

		// try to open the port
		if (!port.openPort()) {
			// there was an error!
			System.err.println("[SerialConnection] There was an error opening the port " + port.getSystemPortName() + " (" + port.getDescriptivePortName() + ")");
			return;
		}

		// this is now our active port
		currentPort = port;

		// wait for robot to become ready...( could probably be skipped)
		/*
		try {
			Thread.sleep(2500);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		*/
	}

	@Override
	public void disconnect() {

		if (currentPort != null) {

			// try to close port
			if (!currentPort.closePort())
				throw new IllegalStateException("SerialPort: Could not close port " + currentPort.getSystemPortName());

			currentPort = null;
		}
	}

	@Override
	public boolean isConnected() {
		return currentPort != null;
	}

	@Override
	public InputStream getInputStream() {
		return currentPort.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return currentPort.getOutputStream();
	}
}
