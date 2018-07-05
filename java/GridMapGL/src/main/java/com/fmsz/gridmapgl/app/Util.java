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

import com.fmsz.gridmapgl.graphics.Color;


public class Util {

	// hide constructor
	private Util() {

	}

	/**
	 * Converts a probability into log odds form
	 */
	public static float logOdds(float odds) {
		return (float) Math.log(odds / (1.0f - odds));
	}

	public static double logOdds(double odds) {
		return Math.log(odds / (1.0f - odds));
	}

	/**
	 * Converts a probability from log odds form
	 */
	public static float invLogOdds(float log) {
		return (float) (1.0f - 1.0f / (1 + Math.exp(log)));
	}

	public static double invLogOdds(double log) {
		return (1.0f - 1.0f / (1 + Math.exp(log)));
	}

	public static void invLogOdds(float[] in, float[] out) {
		for (int i = 0; i < in.length; i++)
			out[i] = invLogOdds(in[i]);
	}

	public static void invLogOdds(double[] in, double[] out) {
		for (int i = 0; i < in.length; i++)
			out[i] = invLogOdds(in[i]);
	}

	/**
	 * See https://stackoverflow.com/a/25508988 for more information.
	 * 
	 * Initialize a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an expanding binary
	 * fashion
	 */
	public static void fastFillFloat(float[] array, float value) {
		int len = array.length;

		if (len > 0) {
			array[0] = value;
		}

		for (int i = 1; i < len; i += i) {
			System.arraycopy(array, 0, array, i, ((len - i) < i) ? (len - i) : i);
		}
	}

	public static void fastFillDouble(double[] array, double value) {
		int len = array.length;

		if (len > 0) {
			array[0] = value;
		}

		for (int i = 1; i < len; i += i) {
			System.arraycopy(array, 0, array, i, ((len - i) < i) ? (len - i) : i);
		}
	}

	private static float[] colorBitsGrayscaleLUT;
	static {
		colorBitsGrayscaleLUT = new float[256];
		for (int i = 0; i < colorBitsGrayscaleLUT.length; i++) {
			float ratio = i / (float) colorBitsGrayscaleLUT.length;
			colorBitsGrayscaleLUT[i] = Color.colorToFloatBits(ratio, ratio, ratio, 1.0f);
		}
	}

	/**
	 * Converts a value from 0.0f - 1.0f to the corresponding grayscale color bits where 0.0f = black
	 * 
	 * @param p
	 *            the grayscale value
	 * @return the colorbits
	 */
	public static float getColorBitsGrayscale(float p) {
		return colorBitsGrayscaleLUT[(int) (p * 255)];
	}

	/////////////////////////////////////// ***************//////////////////////////////
	public static interface Visitor {
		void visit(int x, int y, int dx, int dy);
	}

	public static void gridRayTraceCentered(int startX, int startY, int endX, int endY, int width, int height, Visitor visitor) {
		int dx = Math.abs(endX - startX);
		int dy = Math.abs(endY - startY);

		int x = startX;
		int y = startY;
		int n = 1 + dx + dy;

		int x_inc = (endX > startX ? 1 : -1);
		int y_inc = (endY > startY ? 1 : -1);

		int error = dx - dy;
		dx *= 2;
		dy *= 2;

		while (n > 0) {
			// visit (x,y)
			visitor.visit(x, y, x - startX, y - startY);
			// visited.append( (Vector2(x, y), Vector2(x - startX, y - startY)) )

			if (error > 0) {
				x += x_inc;
				error -= dy;
			} else {
				y += y_inc;
				error += dx;
			}

			// check for boundaries and exit if necessary
			if (x < 0 || x >= width || y < 0 || y >= width)
				break;
			n -= 1;
		}
	}
	/////////////////////////////////////// ***************//////////////////////////////

	/**
	 * Taken from: https://stackoverflow.com/a/9855338
	 * https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
	 */
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Computes a simple box blur of the given array
	 *  https://www.gamasutra.com/view/feature/131511/four_tricks_for_fast_blurring_in_.php
	 */
	public static float[] doBoxBlur(int width, int height, float[] in, float[] out, int k) {
		// check arguments
		if (in.length != out.length)
			throw new IllegalArgumentException("doBoxBlur: length of in and out must be the same");

		if (in.length != width * height)
			throw new IllegalArgumentException(
					"doBoxBlur: length of grid is not correct! Got " + in.length + ", should be " + (width * height));

		float[] sum = new float[in.length];

		float total;
		int x, y;
		for (int i = 0; i < in.length; i++) {
			x = i % width;
			y = i / width;

			total = in[i];

			if (y > 0)
				total += sum[(x) + (y - 1) * width];
			if (x > 0)
				total += sum[(x - 1) + (y) * width];
			if (x > 0 && y > 0)
				total -= sum[(x - 1) + (y - 1) * width];

			sum[i] = total;
		}
		/*
		for index, v in np.ndenumerate(data):
			        row = index[0]
			        col = index[1]
			        #print(index, v)
			        tot = v
			        if row > 0: tot += sums[row - 1, col]
			        if col > 0: tot += sums[row, col - 1]
			        if row > 0 and col > 0: tot -= sums[row - 1, col - 1]
		
			        sums[index] = tot
		*/
		// # compute the box blur
		// result = np.zeros_like(data)

		float scaleFactor = 1 / ((k * 2f + 1f) * (k * 2f + 1f));
		/*
		for (int i = 0; i < in.length; i++) {
			x = i % width;
			y = i / width;
		
			out[i] = sum[boxBlurReadIndex(width, height, x + k, y + k)] + sum[boxBlurReadIndex(width, height, x - k, y - k)]
					- sum[boxBlurReadIndex(width, height, x - k, y + k)] - sum[boxBlurReadIndex(width, height, x + k, y - k)];
			/*
			boxBlurRead(sums, width, height, index[0] + k, index[1] + k) +
			boxBlurRead(sums, width, height, index[0] - k, index[1] - k) - 
			boxBlurRead(sums, width, height, index[0] - k, index[1] + k) - 
			boxBlurRead(sums, width, height, index[0] + k, index[1] - k)
			*
			out[i] *= scaleFactor;
		}
		*/
		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				out[x + y * width] = sum[boxBlurReadIndex(width, height, x + k, y + k)] + sum[boxBlurReadIndex(width, height, x - k, y - k)]
						- sum[boxBlurReadIndex(width, height, x - k, y + k)] - sum[boxBlurReadIndex(width, height, x + k, y - k)];
				out[x + y * width] *= scaleFactor;
			}
		}

		return sum;
		/*
		    for index, v in np.ndenumerate(result):
		        result[index] = boxBlurRead(sums, width, height, index[0] + k, index[1] + k) + boxBlurRead(sums, width, height, index[0] - k, index[1] - k) - boxBlurRead(sums, width, height, index[0] - k, index[1] + k) - boxBlurRead(sums, width, height, index[0] + k, index[1] - k)
		        result[index] /= (k*2+1)**2
		        
		    return result
		
		
		
		def boxBlurRead(sums, width, height, row, col):
		    if row < 0: row = 0
		    if col < 0: col = 0
		    if row >= height: row = height - 1
		    if col >= width: col = width - 1
		    return sums[row, col]
		    
		    */

	}

	private static int boxBlurReadIndex(int width, int height, int x, int y) {
		if (y < 0)
			y = 0;
		if (x < 0)
			x = 0;
		if (x >= width)
			x = width - 1;
		if (y >= height)
			y = height - 1;
		return (x) + (y) * width;
	}

	//@formatter:off
	private static final float[] gaussianKernel3 = new float[] {
		0,   0,   0,   5,   0,   0,  0,
		0,   5,  18,  32,  18,   5,  0,
		0,  18,  64, 100,  64,  18,  0,
		5,  32, 100, 100, 100,  32,  5,
		0,  18,  64, 100,  64,  18,  0,
		0,   5,  18,  32,  18,   5,  0,
		0,   0,   0,   5,   0,   0,  0,
	};	
	private static final float[] gaussianKernel2 = new float[] {
		1,  4,  7,  4, 1,
		4, 16, 26, 16, 4,
		7, 26, 41, 26, 7,
		4, 16, 26, 16, 4,
		1,  4,  7,  4, 1,
	};
	
	private static final float[] gaussianKernel1 = new float[] {
		1, 2, 1,
		2, 4, 2,
		1, 2, 1		
	};
	//@formatter:on
	static {
		// normalize the kernels
		float sum = 0;
		for (int i = 0; i < gaussianKernel3.length; i++) {
			sum += gaussianKernel3[i];
		}
		for (int i = 0; i < gaussianKernel3.length; i++) {
			gaussianKernel3[i] /= sum;
		}

		sum = 0;
		for (int i = 0; i < gaussianKernel2.length; i++) {
			sum += gaussianKernel2[i];
		}
		for (int i = 0; i < gaussianKernel2.length; i++) {
			gaussianKernel2[i] /= sum;
		}

		sum = 0;
		for (int i = 0; i < gaussianKernel1.length; i++) {
			sum += gaussianKernel1[i];
		}
		for (int i = 0; i < gaussianKernel1.length; i++) {
			gaussianKernel1[i] /= sum;
		}
	}

	/**
	 * Computes a gaussian blur of the input data and stores the result in out Inspired by
	 * <a href="https://www.gamasutra.com/view/feature/131511/four_tricks_for_fast_blurring_in_.php" > this page</a>
	 */
	public static void doGaussianBlurf(float[] in, float[] out, int width, int height) {
		int k = 3;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float total = 0;
				int kIndex = 0;
				for (int j = -k; j <= k; j++)
					for (int i = -k; i <= k; i++) {
						int x2 = x + i;
						int y2 = y + j;
						if (x2 >= 0 && x2 < width && y2 >= 0 && y2 < height)
							total += gaussianKernel3[kIndex++] * in[y2 * width + x2];
					}
				out[x + width * y] = total;
			}
		}
	}

	public static void doGaussianBlurd(double[] in, double[] out, int width, int height) {
		int k = 2;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double total = 0;
				int kIndex = 0;
				for (int j = -k; j <= k; j++)
					for (int i = -k; i <= k; i++) {
						int x2 = x + i;
						int y2 = y + j;
						if (x2 >= 0 && x2 < width && y2 >= 0 && y2 < height)
							total += gaussianKernel2[kIndex++] * in[y2 * width + x2];
					}
				out[x + width * y] = total;
			}
		}
	}

	private static double[] separableKernel = { 0, 1, 2, 1, 0 };
	static {
		float sum = 0;
		for (int i = 0; i < separableKernel.length; i++) {
			sum += separableKernel[i];
		}
		for (int i = 0; i < separableKernel.length; i++) {
			separableKernel[i] /= sum;
		}
	}

	/** temporary array used in the separable gaussian calculation since we don't want to overwrite the input */
	private static double[] tempArray = new double[10];
	public static final double[] testKernel = generateGaussianKernel(3, 5 * 3);

	public static void doGaussianBlurdSeparable(double[] in, double[] out, int width, int height, double[] kernel) {
		// make sure we have room
		if (out.length > tempArray.length)
			tempArray = new double[out.length];

		// calculate the size of the kernel
		int k = (kernel.length - 1) / 2;

		// do horizontal blur first
		for (int y = 0; y < height; y++) {
			int yIndex = y * width;

			for (int x = 0; x < width; x++) {
				double total = 0;

				for (int i = -k; i <= k; i++) {
					int x2 = x + i;

					if (x2 >= 0 && x2 < width) {
						// if (in[yIndex + x2] != 0.5)
						total += kernel[i + k] * in[yIndex + x2];

					}
				}
				out[yIndex + x] = total;
			}
		}

		// copy the output values
		System.arraycopy(out, 0, tempArray, 0, out.length);

		// then vertical blur second
		for (int y = 0; y < height; y++) {

			for (int x = 0; x < width; x++) {
				double total = 0;

				for (int i = -k; i <= k; i++) {
					int y2 = y + i;

					if (y2 >= 0 && y2 < height)
						// if (in[x + y2 * width] != 0.5)
						total += kernel[i + k] * tempArray[x + y2 * width];

				}
				out[x + y * width] = total;
			}
		}
	}

	public static double[] generateGaussianKernel(double sigma, int size) {

		// kernel has a middle cell and size on either side
		int kSize = size * 2 + 1;

		double[] values = new double[kSize];

		// normalisation constant makes sure total of matrix is 1
		double norm = 1.0 / (Math.sqrt(2 * Math.PI) * sigma);

		// the bit you divide x^2 by in the exponential
		double coeff = 2 * sigma * sigma;

		// keep track of the total
		double total = 0;

		for (int x = -size; x <= size; x++) {
			double g = norm * Math.exp(-x * x / coeff);
			values[x + size] = g;
			total += g;
		}

		// normalize values
		for (int i = 0; i < values.length; i++) {
			values[i] /= total;
		}

		return values;

		/*
		width=Ceil(radius)	'kernel will have a middle cell, and width on either side
				Local matrix#[width*2+1]
				sigma# = radius/3		'apparently this is all you need to get a good approximation
				norm# = 1.0 / (Sqr(2*Pi) * sigma)		'normalisation constant makes sure total of matrix is 1
				coeff# = 2*sigma*sigma	'the bit you divide x^2 by in the exponential
				total#=0
				For x = -width To width	'fill in matrix!
					g# = norm * Exp( -x*x/coeff )
					matrix[x+width] = g
					total:+g
				Next
				For x=0 To 2*width	'rescale things to get a total of 1, because of discretisation error
					matrix[x]:/total
				Next
				Return matrix
				*/
	}

	public static double[] generateGaussianKernelIntegrate(double sigma, int size) {

		// kernel has a middle cell and size on either side
		int kSize = size * 2 + 1;

		double[] values = new double[kSize];

		// normalisation constant makes sure total of matrix is 1
		double norm = 1.0 / (Math.sqrt(2 * Math.PI) * sigma);

		// the bit you divide x^2 by in the exponential
		double coeff = 2 * sigma * sigma;

		// keep track of the total
		double total = 0;

		double dx = 0.001;
		for (int i = -size; i <= size; i++) {
			// use a simple "integration" to calculate the probability that goes into each bin
			double area = 0;
			for (double x = i - 0.5; x < i + 0.5; x += dx)
				area += norm * Math.exp(-x * x / coeff) * dx;

			values[i + size] += area;
			total += area;
		}

		// normalize values
		for (int i = 0; i < values.length; i++) {
			values[i] /= total;
		}

		return values;
	}

}
