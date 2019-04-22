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
package com.fmsz.gridmapgl.graphics;

import glm_.vec4.Vec4;

/**
 * Holds a single color value
 * 
 */
public class Color {
	// some standard predefined colors
	public static final Color RED = new Color(1, 0, 0);
	public static final Color GREEN = new Color(0, 1, 0);
	public static final Color BLUE = new Color(0, 0, 1);
	public static final Color YELLOW = new Color(1, 1, 0);
	public static final Color MAGENTA = new Color(1, 0, 1);
	public static final Color TEAL = new Color(0, 1, 1);

	public static final Color WHITE = new Color(1, 1, 1);
	public static final Color BLACK = new Color(0, 0, 0);
	// public static final Color MAGENTA = new Color(1, 0, 0);

	public float r, g, b, a;

	public Color(float r, float g, float b) {
		this(r, g, b, 1);
	}

	public Color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public Color(Vec4 color) {
		this.r = color.getX();
		this.g = color.getY();
		this.b = color.getZ();
		this.a = color.getW();
	}

	/** https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils/NumberUtils.java */
	public float toFloatBits() {
		return colorToFloatBits(r, g, b, a);
	}

	public static float colorToFloatBits(float r, float g, float b, float a) {
		int colorI = ((int) (255 * a) << 24) | ((int) (255 * b) << 16) | ((int) (255 * g) << 8) | ((int) (255 * r));

		return Float.intBitsToFloat(colorI & 0xfeffffff);
	}
}
