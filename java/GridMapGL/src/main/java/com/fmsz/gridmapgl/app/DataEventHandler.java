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
import java.util.concurrent.ArrayBlockingQueue;

import com.fmsz.gridmapgl.slam.TimeFrame;

/**
 * Handles the robot data by using a publish/subscribe model. Tries to be thread safe by using thread safe queues for data and calling
 * subscribers only from the main thread.
 */
public class DataEventHandler {
	// static instance variable
	private static DataEventHandler instance = null;

	/** Returns the Singleton instance of the current DataEventHandler */
	public static DataEventHandler getInstance() {
		if (instance == null)
			instance = new DataEventHandler();

		return instance;
	}

	private final ArrayBlockingQueue<TimeFrame> frameQueue = new ArrayBlockingQueue<>(20);

	private ArrayList<IDataSubscriber> subscribers = new ArrayList<>();

	// Private constructor
	private DataEventHandler() {
	}

	/** Interface for receiving events */
	public interface IDataSubscriber {
		public void onHandleData(TimeFrame frame);
	}

	/** Subscribe to get notified when new data arrives */
	public void subscribe(IDataSubscriber sub) {
		if (!subscribers.contains(sub))
			subscribers.add(sub);
	}

	/** Remove subscription to get notified when new data arrives */
	public void unSubscribe(IDataSubscriber sub) {
		subscribers.remove(sub);
	}

	/** Publish new data */
	public void publish(TimeFrame frame) {
		try {
			frameQueue.put(frame);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if any new data events has happened and calls all subscribers accordingly. maxEvents is the maximum number of events to be processed at once or 0 for infinity.
	 */
	public void handleEvents(int maxEvents) {

		while (maxEvents-- > 0 && !frameQueue.isEmpty()) {
			try {
				TimeFrame frame = frameQueue.take();

				for (IDataSubscriber sub : subscribers)
					sub.onHandleData(frame);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
