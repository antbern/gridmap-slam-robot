package com.fmsz.gridmapgl.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import imgui.ImGui;

public class NetworkConnection implements IConnection {
	// GUI Stuff
	private char[] hostBuff = new char[64];
	private char[] portBuff = new char[32];

	// connection stuff
	private static final String defaultHost = "192.168.0.185";
	private static final String defaultPort = "5555";

	private Socket socket = null;
	private InputStream is;
	private OutputStream os;

	@Override
	public String getName() {
		return "Network";
	}

	@Override
	public void init() {
		// fill buffer with default host name
		defaultHost.getChars(0, defaultHost.length(), hostBuff, 0);
		defaultPort.getChars(0, defaultPort.length(), portBuff, 0);
	}

	@Override
	public void doGUI(ImGui imgui) {
		// very simple ui for now
		imgui.inputText("Host", hostBuff, 0);
		imgui.inputText("Port", portBuff, 0);
	}

	@Override
	public void connect() {
		// get the entered host name
		String host = new String(hostBuff).trim();
		String portStr = new String(portBuff).trim();

		System.out.println("Connection to: " + host + ":" + portStr);
		try {
			// try to parse the port as an integer
			int port = Integer.parseInt(portStr);

			// try to create a new socket
			socket = new Socket(host, port);

			// store local pointers to the respective streams
			is = socket.getInputStream();
			os = socket.getOutputStream();

		} catch (NumberFormatException nfe) {
			System.err.println("[NetworkConnection] Ill-formed port number!");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	@Override
	public void disconnect() {
		if (!isConnected())
			return;

		// try to close the socket
		try {
			socket.close();

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			is = null;
			os = null;
			socket = null;
		}
	}

	@Override
	public boolean isConnected() {
		return socket != null;
	}

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public OutputStream getOutputStream() {
		return os;
	}

}
