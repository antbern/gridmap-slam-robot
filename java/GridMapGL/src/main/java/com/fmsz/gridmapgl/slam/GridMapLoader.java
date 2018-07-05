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
package com.fmsz.gridmapgl.slam;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.fmsz.gridmapgl.app.ObjectSerializer;
import com.fmsz.gridmapgl.slam.GridMap.GridMapData;

public class GridMapLoader {
	private static FileOutputStream fos = null;
	private static DataOutputStream dos = null;

	public static void beginSave(String filename) {

		try {
			fos = new FileOutputStream(filename, false);

			// create data output stream
			dos = new DataOutputStream(fos);

			// write header
			dos.writeByte(0xff);

		} catch (IOException e) {
			System.err.println("Error opening file: " + filename);
			e.printStackTrace();
		}
	}

	public static void saveGridMap(GridMap map) {
		try {

			ObjectSerializer.writeGridMap(dos, map);

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	public static void saveGridMapData(GridMapData map) {
		try {
			ObjectSerializer.writeGridMapData(dos, map);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void endSave() {
		try {
			dos.flush();
			fos.close();

			dos = null;
			fos = null;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static FileInputStream fis = null;
	private static DataInputStream dis = null;

	public static void beginLoad(String filename) {
		try {

			// open the file
			fis = new FileInputStream(filename);
			dis = new DataInputStream(fis);

			// correct "header" byte?
			byte b;
			if ((b = dis.readByte()) != (byte) 0xff) {
				dis.close();
				throw new IllegalStateException("Error opening file, header byte is not correct! Wanted " + 0xff + ", got " + b);
			}

		} catch (IOException e) {
			System.err.println("Error opening file " + filename);
			e.printStackTrace();
		}
	}

	public static GridMap loadGridMap() {
		try {

			return ObjectSerializer.readGridMap(dis);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static GridMapData loadGridMapData(GridMap gridMap) {

		try {
			return ObjectSerializer.readGridMapData(dis, gridMap);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;

	}

	public static void endLoad() {
		try {
			dis.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
