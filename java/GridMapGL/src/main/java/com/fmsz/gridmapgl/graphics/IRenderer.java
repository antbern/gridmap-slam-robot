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

import com.fmsz.gridmapgl.math.Vec2f;

@Deprecated
public interface IRenderer {
	public void init();

	// drawing methods
	public void line(Vec2f start, Vec2f end, Color color);

	public void rect(Vec2f pos, Vec2f size, Color color, boolean fill);

	public void circle(Vec2f pos, float radius, Color color, boolean fill);

	public void text(Vec2f pos, String text, Color color);

	// controlling methods

	public void centerAt(Vec2f pos);

	public void pan(Vec2f change);

	public void zoom(Vec2f mousePos, float amount);
	
	public void resize(Vec2f newSize);

}
