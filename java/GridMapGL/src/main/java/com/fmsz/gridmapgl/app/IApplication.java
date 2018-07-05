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

import com.fmsz.gridmapgl.graphics.Camera;

import glm_.vec2.Vec2;

public interface IApplication {
	public void init();

	public void render(float delta, Camera cam);

	public abstract void keydown();

	public abstract void keyup();

	public void mouseMove(Vec2 mousePosition, boolean drag);

	public void mouseClick(Vec2 mousePosition);

	public void dispose();
}
