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

import java.util.ArrayList;
import java.util.List;

import com.fmsz.gridmapgl.app.DataEventHandler.IDataSubscriber;
import com.fmsz.gridmapgl.graphics.Camera;
import com.fmsz.gridmapgl.graphics.Color;
import com.fmsz.gridmapgl.graphics.ShapeRenderer;
import com.fmsz.gridmapgl.graphics.ShapeRenderer.ShapeType;
import com.fmsz.gridmapgl.math.MathUtil;
import com.fmsz.gridmapgl.slam.GridMap.GridMapData;
import com.fmsz.gridmapgl.slam.Observation;
import com.fmsz.gridmapgl.slam.Observation.Measurement;
import com.fmsz.gridmapgl.slam.Pose;
import com.fmsz.gridmapgl.slam.SLAM;
import com.fmsz.gridmapgl.slam.SLAM.Particle;
import com.fmsz.gridmapgl.slam.TimeFrame;

import glm_.vec2.Vec2;
import imgui.Cond;
import imgui.ImGui;

/**
 * The main "brain" of the system that instantiates and handles all other objects involved in the SLAM procedure.
 * 
 * @author Anton
 *
 */
public class GridMapApp implements IApplication, IDataSubscriber {

	// the global ShapeRenderer used for drawing primitive types
	private ShapeRenderer rend;

	///////////////// GUI STUFFS ////////////////////////
	// global ImGUI instance
	private ImGui imgui = ImGui.INSTANCE;

	private static final int TYPE_OCCUPANCY = 0;
	private static final int TYPE_LIKELIHOOD = 1;

	private static final int MAP_STRONGEST = 0;
	private static final int MAP_SPECIFIC = 1;
	private static final int MAP_COMBINED = 2;

	private boolean[] drawGridLines = new boolean[] { false };
	private boolean[] drawLikelihood = new boolean[] { false };
	private boolean[] drawLastObservation = new boolean[] { true };
	private boolean[] drawCombinedPose = new boolean[] { true };
	private boolean[] drawParticles = new boolean[] { true };
	private boolean[] drawStrongestPose = new boolean[] { true };
	private boolean[] drawSpecificPose = new boolean[] { true };

	private boolean[] automaticResampling = new boolean[] { true };

	private int[] selectedParticle = new int[] { 0 };

	private List<String> mapDrawSelectStrings = new ArrayList<>();
	private int[] mapDrawSelectArray = { 0 };

	private int[] mapDrawTypeSelectArray = { 0 };

	///////////////////////////////////////////////////

	// store the current mouse position
	private final Vec2 mousePos = new Vec2();

	// GridMap instance
	// private GridMap gridMap;
	// private GridMapData mapData;

	private SerialConnection serial;

	private SLAM slam;
	private double neff;

	private DataRecorder recorder;

	private Observation lastObservation;
	// private float lastObservationLikelihood = 0;

	private Pose currentCombinedPose = null;;

	private Particle strongestParticle = null;

	private GridMapData combinedGrid = null;

	@Override
	public void init() {
		mapDrawSelectStrings.add(MAP_STRONGEST, "Strongest Particle");
		mapDrawSelectStrings.add(MAP_SPECIFIC, "Specific Particle");
		mapDrawSelectStrings.add(MAP_COMBINED, "Combined Map");

		// initialize the global ShapeRenderer
		rend = new ShapeRenderer();

		// gridMap = new GridMap(6.0f, 6.0f, 0.05f, new Vec2(-3, -3));
		// mapData = gridMap.createMapData(null);

		serial = new SerialConnection();

		slam = new SLAM();
		strongestParticle = slam.getStrongestParticle();

		recorder = new DataRecorder();

		DataEventHandler.getInstance().subscribe(this);

	}

	@Override
	public void onHandleData(TimeFrame frame) {
		lastObservation = frame.z;

		neff = slam.update(lastObservation, frame.u);

		// only resample if supposed to
		if (automaticResampling[0] && neff < slam.getParticles().size() / 2)
			slam.resample();

		strongestParticle = slam.getStrongestParticle();

		currentCombinedPose = slam.getWeightedPose();

		System.out.println(currentCombinedPose);

		/*
		// lastObservation.computeEndPoints();
		// gmap.processObservation(lastObservation);
		
		currentPose = gridMap.findBestPose(mapData, lastObservation, currentPose);
		
		// calculate probability of observation
		gridMap.rays.clear();
		// double prob = gmap.probabilityOf(lastObservation, currentPose);
		
		// System.out.println(prob);
		
		gridMap.integrateObservation(mapData, lastObservation, currentPose);
		gridMap.computeLikelihoodMap(mapData);
		
		*/
	}

	@Override
	public void render(float delta, Camera cam) {
		// process data first
		DataEventHandler.getInstance().handleEvents(1);

		// do a GUI for the GridMap
		if (imgui.begin("Grid Map", null, 0)) {
			/*
			if (gridMap.pointInMap(mousePos)) {
				
				double value = gridMap.getRawAt(mapData, mousePos);
			
				imgui.text("Prob: %.2f (%.2f)", Util.invLogOdds(value), value);
				imgui.text("LHFD: %.2f", gridMap.getLikelihood(mapData, mousePos));
			
			} else {
				imgui.text("Prob: -.-- (-.--)");
				imgui.text("LHFD: -.-- ");
			}
			*
			*/
			/*
			KMutableProperty0<Boolean> prop = new JavaProp<>(() -> {
				//System.out.println("Get");
				return bool;
			}, (val) -> {
				System.out.println("Set");
				bool = val;
			});
			*/

			imgui.setNextTreeNodeOpen(true, Cond.FirstUseEver);
			if (imgui.collapsingHeader("Render Options", null, 0)) {

				imgui.setNextTreeNodeOpen(true, Cond.FirstUseEver);
				if (imgui.treeNode("Map")) {

					// what map content to show
					imgui.radioButton("Occupancy", mapDrawTypeSelectArray, TYPE_OCCUPANCY);
					imgui.sameLine(0);
					imgui.radioButton("Likelihood", mapDrawTypeSelectArray, TYPE_LIKELIHOOD);

					drawLikelihood[0] = mapDrawTypeSelectArray[0] == TYPE_LIKELIHOOD;

					// select which map to show
					imgui.combo("Which?", mapDrawSelectArray, mapDrawSelectStrings, 5);

					switch (mapDrawSelectArray[0]) {
					case MAP_STRONGEST:
						break;

					case MAP_SPECIFIC:
						imgui.sliderInt("Particle", selectedParticle, 0, slam.getParticles().size() - 1, "%.0f");
						break;

					case MAP_COMBINED:
						if (imgui.button("Calculate Combined Grid", new Vec2())) {
							calculateCombined();
						}
						break;
					}

					// end node
					imgui.treePop();
				}
				imgui.newLine();

				imgui.setNextTreeNodeOpen(true, Cond.FirstUseEver);
				if (imgui.treeNode("Features")) {

					imgui.checkbox("Grid Lines", drawGridLines);
					imgui.checkbox("Observation Data", drawLastObservation);

					imgui.checkbox("Particles", drawParticles);

					imgui.checkbox("Combined Pose", drawCombinedPose);
					imgui.checkbox("Strongest Pose", drawStrongestPose);
					imgui.checkbox("Specific Pose", drawSpecificPose);

					// end node
					imgui.treePop();
				}
			}

			imgui.newLine();

			imgui.setNextTreeNodeOpen(true, Cond.FirstUseEver);
			if (imgui.collapsingHeader("SLAM Options", 0)) {
				imgui.text("Neff: %.3f", neff);

				if (imgui.button("Resample", new Vec2())) {
					slam.resample();
				}

				imgui.checkbox("Automatic Resampling", automaticResampling);
			}

			/*
			imgui.sameLine(0);
			if (imgui.button("Recompute", new Vec2())) {
			gridMap.computeLikelihoodMap(mapData);
			}*/

			// imgui.sameLine(0);
			// imgui.text("P: %.4f", lastObservationLikelihood);

			/*
			if (imgui.button("Reset", new Vec2())) {
				gridMap.reset(mapData);
				lastObservation = null;
				OBSERVATION_QUEUE.clear();
			}
			*/
			/*
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
			}*/

			// System.out.println(selectedParticle[0]);
		}
		// end the GridMap GUI Window
		imgui.end();

		// create SerialConnection GUI
		serial.doGUI(imgui);
		recorder.doGUI(imgui);
		recorder.update(delta);

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

		GridMapData mapToRender = null;

		switch (mapDrawSelectArray[0]) {
		case MAP_STRONGEST:
			mapToRender = strongestParticle.m;
			break;

		case MAP_SPECIFIC:
			mapToRender = slam.getParticles().get(selectedParticle[0]).m;
			break;

		case MAP_COMBINED:
			mapToRender = combinedGrid;
			break;
		}

		if (mapToRender != null)
			slam.getGridMap().render(rend, mapToRender, drawGridLines[0], drawLikelihood[0]);

		// draw the last observation
		if (lastObservation != null && drawLastObservation[0]) {
			Pose basePose = mapDrawSelectArray[0] == MAP_SPECIFIC ? slam.getParticles().get(selectedParticle[0]).pose
					: strongestParticle.pose;
			rend.begin(ShapeType.LINE);

			for (Measurement m : lastObservation.getMeasurements()) {
				rend.line(basePose.x, basePose.y, m.getEndPointX(basePose), m.getEndPointY(basePose), (m.wasHit ? Color.GREEN : Color.RED));
			}

			rend.end();
		}

		// render poses
		rend.begin(ShapeType.FILLED);
		if (drawCombinedPose[0] && currentCombinedPose != null)
			renderPose(currentCombinedPose, Color.YELLOW);
		if (drawStrongestPose[0])
			renderPose(strongestParticle.pose, Color.MAGENTA);

		if (drawSpecificPose[0])
			renderPose(slam.getParticles().get(selectedParticle[0]).pose, Color.TEAL);
		rend.end();

		// draw all particle positions as quads
		if (drawParticles[0]) {
			rend.begin(ShapeType.FILLED);
			for (Particle p : slam.getParticles()) {
				rend.rect(p.pose.x - 0.005f, p.pose.y - 0.005f, 0.01f, 0.01f, Color.BLUE.toFloatBits());
			}
			rend.end();
		}

	}

	private void renderPose(Pose pose, Color col) {
		rend.circle(pose.x, pose.y, 0.05f, col, 10);
		rend.line(pose.x, pose.y, pose.x + 0.1f * MathUtil.cos(pose.theta), pose.y + 0.1f * MathUtil.sin(pose.theta), Color.BLACK);
	}

	private void calculateCombined() {
		// create empty map object
		combinedGrid = slam.getGridMap().createMapData(null);

		final double[] combinedData = combinedGrid.logData;

		for (int i = 0; i < combinedData.length; i++) {
			double product = 1;

			for (Particle p : slam.getParticles()) {
				// final double[] particleData = p.m.logData;

				product *= 1 - Util.invLogOdds(p.m.logData[i]);

			}
			combinedData[i] = Util.logOdds(1 - product);
		}

		slam.getGridMap().computeLikelihoodMap(combinedGrid);
	}

	@Override
	public void mouseMove(Vec2 mousePosition, boolean drag) {
		this.mousePos.put(mousePosition);
		// System.out.println("Move:" + mousePosition + ", " + drag);
		if (drag) {

			// gridMap.applyMeasurement(mapData, new Measurement(mousePosition.x, mousePosition.y, true, currentPose), currentPose);

		}
	}

	@Override
	public void mouseClick(Vec2 mousePosition) {
		// System.out.println("Click: " + mousePosition);
		// Measurement z = new Measurement(mousePosition.x, mousePosition.y, true, currentPose);

		// System.out.println(gridMap.probabilityOf(mapData, z, currentPose));
		// gridMap.applyMeasurement(mapData, z, currentPose);
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
