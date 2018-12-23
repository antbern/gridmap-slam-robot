package com.fmsz.gridmapgl.conn;

import java.io.InputStream;
import java.io.OutputStream;

import imgui.ImGui;

public interface IConnection {
	
	/** Returns the name for this connection */
	public String getName();

	/** Initializes this connection */
	public void init();

	/** Lets this connection draw some kind of GUI using ImGUI */
	public void doGUI(ImGui imgui);

	/** Called when the connection is to be made. This is done before the receiving thread is created. */
	public void connect();

	/** Called when the connection is to be closed. This is done after the receiving thread is closed.  */
	public void disconnect();

	/** returns whether or not this connection is connected */
	public boolean isConnected();

	/** returns the active InputStream associated with this connection */
	public InputStream getInputStream();

	/** returns the active OutputStream associated with this connection */
	public OutputStream getOutputStream();

	/** Invoked when this object is destroyed (often because of the application terminating) */
	// public void dispose();
}
