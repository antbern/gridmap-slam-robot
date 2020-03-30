package com.fmsz.gridmapgl.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import imgui.ImGui;

public class NetworkConnection implements IConnection {
	// GUI Stuff
	private byte[] hostBuff = new byte[64];
	private byte[] portBuff = new byte[32];

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
		System.arraycopy(defaultHost.getBytes(), 0, hostBuff, 0, defaultHost.length());
		System.arraycopy(defaultPort.getBytes(), 0, portBuff, 0, defaultPort.length());
	}

	@Override
	public void doGUI(ImGui imgui) {
		// very simple ui for now
		imgui.inputText("Host", hostBuff, 0, null, null);
		imgui.inputText("Port", portBuff, 0, null, null);
		
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
