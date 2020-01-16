/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.istl.afis.algorithm;

import java.util.Objects;

/**
 *
 * @author Arun
 */
public class FingerprintImage {
    
    double dpi = 500;
    DoubleMatrix matrix;

    public FingerprintImage() {
    }
    
    public FingerprintImage dpi(double dpi) {
		if (dpi < 20 || dpi > 20_000)
			throw new IllegalArgumentException();
		this.dpi = dpi;
		return this;
	}
    
    
    public FingerprintImage decode(byte[] image) {
		Objects.requireNonNull(image);
		ImageDecoder.DecodedImage decoded = ImageDecoder.decodeAny(image);
		matrix = new DoubleMatrix(decoded.width, decoded.height);
		for (int y = 0; y < decoded.height; ++y) {
			for (int x = 0; x < decoded.width; ++x) {
				int pixel = decoded.pixels[y * decoded.width + x];
				int color = (pixel & 0xff) + ((pixel >> 8) & 0xff) + ((pixel >> 16) & 0xff);
				matrix.set(x, y, 1 - color * (1.0 / (3.0 * 255.0)));
			}
		}
		return this;
	}
    
    
    public FingerprintImage grayscale(int width, int height, byte[] pixels) {
		Objects.requireNonNull(pixels);
		if (width <= 0 || height <= 0 || pixels.length != width * height)
			throw new IndexOutOfBoundsException();
		matrix = new DoubleMatrix(width, height);
		for (int y = 0; y < height; ++y)
			for (int x = 0; x < width; ++x)
				matrix.set(x, y, 1 - Byte.toUnsignedInt(pixels[y * width + x]) / 255.0);
		return this;
	}
    
    
}
