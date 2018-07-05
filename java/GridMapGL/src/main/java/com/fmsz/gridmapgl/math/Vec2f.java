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
package com.fmsz.gridmapgl.math;

@Deprecated
public class Vec2f {
	// useful temporary variables to avoid instancing
	public static final Vec2f tmp = new Vec2f();
	public static final Vec2f tmp2 = new Vec2f();

	// predefined vectors
	public static final Vec2f posX = new Vec2f(1, 0);
	public static final Vec2f posY = new Vec2f(0, 1);
	public static final Vec2f negX = new Vec2f(-1, 0);
	public static final Vec2f negY = new Vec2f(0, -1);

	// members
	public float x, y;

	// constructors
	public Vec2f() {
		this.x = 0;
		this.y = 0;
	}

	public Vec2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vec2f(Vec2f other) {
		this.x = other.x;
		this.y = other.y;
	}

	// setters
	public Vec2f setFromAngle(float angle, float lenght) {

		return this;
	}

	public Vec2f set(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	public Vec2f set(Vec2f other) {
		this.x = other.x;
		this.y = other.y;
		return this;
	}

	// operators
	public Vec2f add(Vec2f other) {
		this.x += other.x;
		this.y += other.y;
		return this;
	}

	public Vec2f sub(Vec2f other) {
		this.x -= other.x;
		this.y -= other.y;
		return this;
	}

	public Vec2f scl(float value) {
		this.x *= value;
		this.y *= value;
		return this;
	}

	public Vec2f normalize() {
		float invLen = (float) (1 / Math.sqrt(this.x * this.x + this.y * this.y));
		this.x *= invLen;
		this.y *= invLen;

		return this;
	}

	public float dot(Vec2f other) {
		return this.x * other.x + this.y * other.y;
	}

	public float len2() {
		return this.x * this.x + this.y * this.y;
	}

	public float len() {
		return (float) Math.sqrt(this.x * this.x + this.y * this.y);
	}

	public Vec2f copy() {
		return new Vec2f(this);
	}

}
