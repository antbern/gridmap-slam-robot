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

import java.util.concurrent.ArrayBlockingQueue;

import com.fmsz.gridmapgl.graphics.Camera;
import com.fmsz.gridmapgl.graphics.Color;
import com.fmsz.gridmapgl.graphics.ShapeRenderer;
import com.fmsz.gridmapgl.graphics.ShapeRenderer.ShapeType;
import com.fmsz.gridmapgl.math.MathUtil;
import com.fmsz.gridmapgl.slam.GridMap;
import com.fmsz.gridmapgl.slam.GridMap.GridMapData;
import com.fmsz.gridmapgl.slam.GridMapLoader;
import com.fmsz.gridmapgl.slam.Observation;
import com.fmsz.gridmapgl.slam.Observation.Measurement;
import com.fmsz.gridmapgl.slam.Pose;
import com.fmsz.gridmapgl.slam.SLAM;

import glm_.vec2.Vec2;
import imgui.ImGui;

/**
 * The main "brain" of the system that instantiates and handles all other objects involved in the SLAM procedure.
 * 
 * @author Anton
 *
 */
 @Deprecated
public class GridMapAppBackup implements IApplication {
	public static final ArrayBlockingQueue<Observation> OBSERVATION_QUEUE = new ArrayBlockingQueue<>(10);

	// the global ShapeRenderer used for drawing primitive types
	private ShapeRenderer rend;

	///////////////// GUI STUFFS ////////////////////////
	// global ImGUI instance
	private ImGui imgui = ImGui.INSTANCE;

	private boolean[] drawGridLines = new boolean[] { false };
	private boolean[] drawLikelihood = new boolean[] { false };
	private boolean[] drawLastObservation = new boolean[] { true };

	///////////////////////////////////////////////////

	// store the current mouse position
	private final Vec2 mousePos = new Vec2();

	// GridMap instance
	private GridMap gridMap;
	private GridMapData mapData;

	private SerialConnection serial;

	private Observation lastObservation;
	private float lastObservationLikelihood = 0;

	private Pose currentPose;

	private SLAM slam;

	@Override
	public void init() {
		// initialize the global ShapeRenderer
		rend = new ShapeRenderer();

		gridMap = new GridMap(6.0f, 6.0f, 0.05f, new Vec2(-3, -3));
		mapData = gridMap.createMapData(null);

		serial = new SerialConnection();

		currentPose = new Pose(0, 0, 0);

		slam = new SLAM();

	}

	@Override
	public void render(float delta, Camera cam) {
		// process data first

		if (!OBSERVATION_QUEUE.isEmpty()) {
			try {
				lastObservation = OBSERVATION_QUEUE.take();
				// lastObservation.computeEndPoints();
				// gmap.processObservation(lastObservation);

				currentPose = gridMap.findBestPose(mapData, lastObservation, currentPose);

				// calculate probability of observation
				gridMap.rays.clear();
				// double prob = gmap.probabilityOf(lastObservation, currentPose);

				// System.out.println(prob);

				gridMap.integrateObservation(mapData, lastObservation, currentPose);
				gridMap.computeLikelihoodMap(mapData);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/*
		// show our window
		if (imgui.begin("GridMap", null, 0)) {
			imgui.setWindowCollapsed(false, Cond.FirstUseEver);
			imgui.setWindowPos(new Vec2(0, 0), Cond.FirstUseEver);
		
			imgui.spacing();
		
			if (imgui.treeNode("Hej")) {
				imgui.text("Inside loop!");
				imgui.button("Hello World", new Vec2());
				imgui.treePop();
			}
			if (imgui.treeNode("Hejsan")) {
				imgui.text("Inside loop!");
				imgui.button("Hello World", new Vec2());
		
				imgui.treePop();
			}
		
			// KMutableProperty0Impl<Boolean> open = new KMutableProperty0Impl<>(container, descriptor)
			if (imgui.collapsingHeader("Collapsing header", 0)) {
				imgui.text("Headre loop!");
				imgui.button("Hello World", new Vec2());
				// imgui_utilities.
			}
		
		}
		// end the window
		imgui.end();
		*/

		// do a GUI for the GridMap
		if (imgui.begin("Grid Map", null, 0)) {
			if (gridMap.pointInMap(mousePos)) {
				double value = gridMap.getRawAt(mapData, mousePos);

				imgui.text("Prob: %.2f (%.2f)", Util.invLogOdds(value), value);
				imgui.text("LHFD: %.2f", gridMap.getLikelihood(mapData, mousePos));

			} else {
				imgui.text("Prob: -.-- (-.--)");
				imgui.text("LHFD: -.-- ");
			}
			imgui.checkbox("GridLines", drawGridLines);

			imgui.checkbox("Likelihood", drawLikelihood);
			imgui.sameLine(0);
			if (imgui.button("Recompute", new Vec2())) {
				gridMap.computeLikelihoodMap(mapData);
			}

			imgui.checkbox("Observation", drawLastObservation);
			imgui.sameLine(0);
			imgui.text("P: %.4f", lastObservationLikelihood);

			if (imgui.button("Reset", new Vec2())) {
				gridMap.reset(mapData);
				lastObservation = null;
				OBSERVATION_QUEUE.clear();
			}

			if (imgui.button("Save", new Vec2())) {
				GridMapLoader.beginSave("maps/gmap.bin");
				GridMapLoader.saveGridMap(gridMap);
				GridMapLoader.saveGridMapData(mapData);
				GridMapLoader.endSave();
			}

			if (imgui.button("Load", new Vec2())) {
				GridMapLoader.beginLoad("maps/gmap.bin");
				gridMap = GridMapLoader.loadGridMap();
				mapData = GridMapLoader.loadGridMapData(gridMap);
				GridMapLoader.endLoad();
			}

		}
		// end the GridMap GUI Window
		imgui.end();

		// create SerialConnection GUI
		serial.doGUI(imgui);

		// give the renderer the correct model-view-projection matrix from the camera
		rend.setMVP(cam.combined);

		/*
		// draw a cross in the center to indicate origo
		rend.begin(ShapeType.LINE);
		rend.line(-0.2f, 0, 0.2f, 0, Color.GREEN);
		rend.line(0, -0.2f, 0, 0.2f, Color.GREEN);
		rend.end();
		*/

		// draw the grid map
		gridMap.render(rend, mapData, drawGridLines[0], drawLikelihood[0]);

		// draw the last observation
		if (lastObservation != null && drawLastObservation[0]) {
			rend.begin(ShapeType.LINE);

			for (Measurement m : lastObservation.getMeasurements()) {
				rend.line(currentPose.x, currentPose.y, m.getEndPointX(currentPose), m.getEndPointY(currentPose),
						(m.wasHit ? Color.GREEN : Color.RED));
			}

			rend.end();
		}

		rend.begin(ShapeType.FILLED);
		rend.circle(currentPose.x, currentPose.y, 0.1f, Color.YELLOW);
		rend.line(currentPose.x, currentPose.y, currentPose.x + 0.1f * MathUtil.cos(currentPose.theta),
				currentPose.y + 0.1f * MathUtil.sin(currentPose.theta), Color.BLACK);
		rend.end();

	}

	@Override
	public void mouseMove(Vec2 mousePosition, boolean drag) {
		this.mousePos.put(mousePosition);
		// System.out.println("Move:" + mousePosition + ", " + drag);
		if (drag) {

			gridMap.applyMeasurement(mapData, new Measurement(mousePosition.x, mousePosition.y, true, currentPose), currentPose);

		}
	}

	@Override
	public void mouseClick(Vec2 mousePosition) {
		// System.out.println("Click: " + mousePosition);
		Measurement z = new Measurement(mousePosition.x, mousePosition.y, true, currentPose);

		System.out.println(gridMap.probabilityOf(mapData, z, currentPose));
		gridMap.applyMeasurement(mapData, z, currentPose);
	}

	@Override
	public void keydown() {

	}

	@Override
	public void keyup() {

	}

	@Override
	public void dispose() {
		rend.dispose();
		serial.dispose();
	}

}
