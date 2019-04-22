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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.fmsz.gridmapgl.app.DataEventHandler.IDataSubscriber;
import com.fmsz.gridmapgl.slam.Observation;
import com.fmsz.gridmapgl.slam.Odometry;
import com.fmsz.gridmapgl.slam.TimeFrame;

import glm_.vec2.Vec2;
import imgui.ImGui;
import imgui.InputTextFlag;
import imgui.internal.ButtonFlag;

/**
 * A class for recording incoming observation and odometry information. Also does playback and saving/loading from file
 */
public class DataRecorder implements IDataSubscriber {

	/** Class for holding one recorded TimeFrame object together with a timestamp */
	private class RecordedTimeFrame {
		TimeFrame frame;
		float timeStamp;
	}

	/** Stores all recorded TimeFrames */
	private ArrayList<RecordedTimeFrame> frames = new ArrayList<RecordedTimeFrame>();

	/** Weather or not we are currently recording */
	// private boolean isRecording = false;

	private enum State {
		IDLE("Idle"), RECORD("Recording..."), REPLAY("Replaying...");
		String description;

		private State(String description) {
			this.description = description;
		}
	}

	private State currentState = State.IDLE;

	/** Holds the current time, in seconds */
	private float currentTime = 0;

	/** The number of frames recorded or replayed */
	private int frameCounter = 0;

	private boolean forceNext = false;
	private boolean paused = false;
	private boolean running = false;

	private boolean[] pausedArray = { false };
	private int[] modeSelectArray = { 1 };

	// variables for selecting the appropriate file slot
	private int[] currentSelectedFileIndex = { 0 };
	private List<String> availableFileNames = new ArrayList<>();
	private List<Path> availableFilePaths = new ArrayList<>();
	private Path currentSelectedFile = null;
	// private boolean[] fileChoosed = new boolean[] { true };
	private char[] fileNameBuffer = new char[32];
	// private char[] lastFileNameBuffer = new char[32];

	private String rootDirectory = "maps";

	public DataRecorder() {
		DataEventHandler.getInstance().subscribe(this);
		refreshFileList();
	}

	public void doGUI(ImGui imgui) {
		if (imgui.begin("Recorder", null, 0)) {
			// select recording slot?

			/*
			if (imgui.inputText("Filename:", fileNameBuffer, 0)) {
				// change in input?
				if (!Arrays.equals(fileNameBuffer, lastFileNameBuffer)) {
			
					refreshFileList(new String(fileNameBuffer).trim());
			
					System.arraycopy(fileNameBuffer, 0, lastFileNameBuffer, 0, fileNameBuffer.length);
				}
			
				imgui.setNextWindowPos(new Vec2(imgui.getItemRectMin().x, imgui.getItemRectMax().y), Cond.Always, new Vec2());
			
				imgui.beginTooltip();
				for (String name : availableFileNames) {
					if (imgui.selectable(name, false, 0, new Vec2())) {
						System.out.println("Selectable");
					}
				}
			
				imgui.endTooltip();
			}
			
			// autocomplete popup
			if (imgui.beginPopup("autocomplete", WindowFlags.Modal.getI())) {
			
				imgui.selectable("Test", true, 0, new Vec2());
			
				imgui.endPopup();
			}
			*/

			if (imgui.combo("File", currentSelectedFileIndex, availableFileNames, 7)) {
				// did we select the "add new file" - action?
				if (currentSelectedFileIndex[0] == 0) {
					// show add file popup
					imgui.openPopup("filename");
				} else
					currentSelectedFile = availableFilePaths.get(currentSelectedFileIndex[0] - 1);
			}

			if (imgui.beginPopup("filename", 0)) {

				if (imgui.inputText("Filename:", fileNameBuffer, InputTextFlag.EnterReturnsTrue.i, (data) -> 0, null)) {
					String filename = new String(fileNameBuffer).trim();

					availableFileNames.add(filename);
					availableFilePaths.add(Paths.get(rootDirectory, filename));
					currentSelectedFileIndex[0] = availableFileNames.size() - 1;
					currentSelectedFile = availableFilePaths.get(currentSelectedFileIndex[0] - 1);

					imgui.closeCurrentPopup();
				}

				imgui.endPopup();
			}

			if (imgui.button("Save", new Vec2())) {
				save(currentSelectedFile);
			}
			imgui.sameLine(0);
			if (imgui.button("Load", new Vec2())) {
				load(currentSelectedFile);
			}
			imgui.sameLine(0);
			if (imgui.button("Clear", new Vec2())) {
				currentTime = 0;
				frameCounter = 0;
				frames.clear();
			}

			/*
			if (imgui.button("Choose", new Vec2())) {
				JFileChooser chooser = new JFileChooser(System.getProperty("java.class.path"));
				FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV files", "csv");
				chooser.setFileFilter(filter);
				int returnVal = chooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					System.out.println(chooser.getSelectedFile().getAbsolutePath());
				}
			}
			*/
			// show status, how many samples we have taken and have left
			imgui.text("State: %s", currentState.description);
			imgui.text("Frames: %d/%d", frameCounter, frames.size());

			// show current playback and recording time
			imgui.text("Time: %4.2f s", currentTime);

			imgui.text("Mode: ");
			imgui.sameLine(0);
			imgui.radioButton("Record", modeSelectArray, 0);
			imgui.sameLine(0);
			imgui.radioButton("Replay", modeSelectArray, 1);

			// record?
			if (modeSelectArray[0] == 0) { // record
				// test setup, Start, Pause, Stop
				if (imgui.buttonEx("Start", new Vec2(), (running ? ButtonFlag.Disabled.getI() : 0))) {
					// begin recording
					beginRecording();
				}
				imgui.sameLine(0);
				imgui.checkbox("Pause", pausedArray);
				paused = pausedArray[0];
				/*
				if (imgui.buttonEx((paused ? "Resume" : "Pause"), new Vec2(), (running ? 0 : ButtonFlags.Disabled.getI()))) {
					paused = !paused;
				}
				*/
				imgui.sameLine(0);
				if (imgui.buttonEx("Stop", new Vec2(), (running ? 0 : ButtonFlag.Disabled.getI()))) {
					endRecording();
				}
			} else { // replay
				if (imgui.buttonEx("Start", new Vec2(), (running ? ButtonFlag.Disabled.getI() : 0))) {
					// begin replay
					beginPlayback();
				}
				imgui.sameLine(0);
				imgui.checkbox("Pause", pausedArray);
				paused = pausedArray[0];
				/*
				if (imgui.buttonEx((paused ? "Resume" : "Pause"), new Vec2(), (running ? 0 : ButtonFlags.Disabled.getI()))) {
					paused = !paused;
				}*/
				imgui.sameLine(0);
				if (imgui.buttonEx("Step", new Vec2(), (running ? 0 : ButtonFlag.Disabled.getI()))) {
					forceNextPlayback();
				}
				imgui.sameLine(0);
				if (imgui.buttonEx("Stop", new Vec2(), (running ? 0 : ButtonFlag.Disabled.getI()))) {
					endPlayback();
				}

				if (frameCounter < frames.size())
					imgui.text("Next Frame: %.2f", frames.get(frameCounter).timeStamp);
			}
		}
		// end the window
		imgui.end();

		/*
		// file picker
		if (imgui.button("Select", new Vec2())) {
			imgui.openPopup("FileSelector");
		}
		if (imgui.beginPopupModal("FileSelector", fileChoosed, WindowFlags.NoTitleBar.getI())) {
		
			// draw file contents here?
			imgui.text("File");
		
			if (imgui.isMouseDoubleClicked(0))
				imgui.closeCurrentPopup();
		
			imgui.endPopup();
		}
		*/
	}

	private void refreshFileList(/*String fileStart*/) {
		Path root = Paths.get(rootDirectory);
		System.out.println(root.toAbsolutePath());
		availableFileNames.clear();
		availableFilePaths.clear();

		// add a "Add new File" - action
		availableFileNames.add("Add File...");

		// iterate through all files in directory
		try {
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					// if (fileStart == null || fileStart == "" || file.getFileName().toString().startsWith(fileStart)) {
					availableFilePaths.add(file);
					availableFileNames.add(file.getFileName().toString());
					// }

					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		// reset variables
		if (availableFilePaths.size() > 0) {
			currentSelectedFileIndex[0] = 0;
			currentSelectedFile = availableFilePaths.get(currentSelectedFileIndex[0]);
		}

	}

	private void beginRecording() {
		running = true;
		currentState = State.RECORD;
		currentTime = 0;
		frameCounter = 0;
		frames.clear();
	}

	private void endRecording() {
		running = false;
		paused = false;

		currentState = State.IDLE;
	}

	private void beginPlayback() {
		running = true;
		currentState = State.REPLAY;
		currentTime = 0;
		frameCounter = 0;
	}

	private void endPlayback() {
		running = false;
		paused = false;

		currentState = State.IDLE;
	}

	private void forceNextPlayback() {
		if (currentState == State.REPLAY && running)
			forceNext = true;
	}

	/** Allows this recorder to update its' internal state and publish data if playback is active */
	public void update(float delta) {
		if (running && !paused)
			currentTime += delta;

		if (currentState == State.REPLAY)
			// exit replay state if there are no more to replay
			if (frameCounter >= frames.size()) {
				endPlayback();
			} else {

				// handle posting possible new data
				RecordedTimeFrame frame = frames.get(frameCounter);
				if (currentTime >= frame.timeStamp || forceNext) {

					// move time forwards if we forced
					if (forceNext)
						currentTime = frame.timeStamp;

					forceNext = false;

					// post this frame
					DataEventHandler.getInstance().publish(frame.frame);

					frameCounter++;

				}
			}

	}

	@Override
	public void onHandleData(TimeFrame frame) {
		// handle new measurement here ( only in recording mode)
		if (currentState == State.RECORD && !paused) {

			RecordedTimeFrame recFrame = new RecordedTimeFrame();
			recFrame.frame = frame;
			recFrame.timeStamp = currentTime;
			frames.add(recFrame);

			frameCounter++;
		}
	}

	/** Saves the current recording to disk */
	public void save(Path file) {
		// use try-with-resource statement (ensures the file is closed even after possible exception
		try (FileOutputStream fos = new FileOutputStream(file.toFile(), false); DataOutputStream dos = new DataOutputStream(fos);) {

			// write header
			dos.writeByte(0xff);

			// write all data
			dos.writeShort(frames.size());
			for (RecordedTimeFrame rtf : frames) {
				dos.writeFloat(rtf.timeStamp);
				ObjectSerializer.writeOdometry(dos, rtf.frame.u);
				ObjectSerializer.writeObservation(dos, rtf.frame.z);
			}
			fos.flush();
		} catch (IOException e) {
			System.err.println("Error opening file: " + file.toAbsolutePath().toString());
			e.printStackTrace();
		}
	}

	/** loads a recording from disk */
	public void load(Path file) {

		try (FileInputStream fis = new FileInputStream(file.toFile()); DataInputStream dis = new DataInputStream(fis)) {

			// open the file
			// fis = new FileInputStream(filename);
			// dis = new DataInputStream(fis);

			// correct "header" byte?
			byte b;
			if ((b = dis.readByte()) != (byte) 0xff) {
				dis.close();
				throw new IllegalStateException("Error opening file, header byte is not correct! Wanted " + 0xff + ", got " + b);
			}

			// load stuff here
			short length = dis.readShort();
			frames.clear();
			for (int i = 0; i < length; i++) {
				RecordedTimeFrame rtf = new RecordedTimeFrame();
				rtf.timeStamp = dis.readFloat();
				Odometry u = ObjectSerializer.readOdometry(dis);
				Observation z = ObjectSerializer.readObservation(dis);
				rtf.frame = new TimeFrame(z, u);

				frames.add(rtf);
			}

		} catch (IOException e) {
			System.err.println("Error opening file " + file.toAbsolutePath().toString());
			e.printStackTrace();
		}
	}

}
