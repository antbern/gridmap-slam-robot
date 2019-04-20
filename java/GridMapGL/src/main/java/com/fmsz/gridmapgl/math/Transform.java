package com.fmsz.gridmapgl.math;

import com.fmsz.gridmapgl.slam.Pose;

public abstract class Transform {

	public abstract double transformX(double x, double y);

	public abstract double transformY(double x, double y);

	// creates a transform that converts from local robot coordinates to world coordinates based
	// on the given robot pose
	public static Transform fromRobotToWorld(Pose p) {
		// pre-compute sin and cos for speed
		double cos = MathUtil.cos(p.theta);
		double sin = MathUtil.sin(p.theta);

		// return new Transform object with specific transform
		return new Transform() {

			@Override
			public double transformX(double x, double y) {
				return x * cos - y * sin + p.x;
			}

			@Override
			public double transformY(double x, double y) {
				return x * sin + y * cos + p.y;
			}

		};
	}

}
