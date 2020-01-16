/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.istl.afis.algorithm;

import com.machinezoo.noexception.Exceptions;
import static java.util.stream.Collectors.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import org.jnbis.api.Jnbis;
import org.jnbis.api.model.Bitmap;


/**
 *
 * @author Arun
 */
abstract class ImageDecoder {
    	static class DecodedImage {
		int width;
		int height;
                int[] pixels;
		DecodedImage(int width, int height, int[] pixels) {
			this.width = width;
			this.height = height;
			this.pixels = pixels;
		}
	}
        abstract boolean available();
        abstract String name();
        abstract DecodedImage decode(byte[] image);
        
        
        private static final List<ImageDecoder> all = Arrays.asList(
		new ImageIODecoder(),
                new WsqDecoder());
	static DecodedImage decodeAny(byte[] image) {
		Map<ImageDecoder, Throwable> exceptions = new HashMap<>();
		for (ImageDecoder decoder : all) {
			try {
				if (!decoder.available())
					throw new UnsupportedOperationException("Image decoder is not available.");
				return decoder.decode(image);
			} catch (Throwable ex) {
				exceptions.put(decoder, ex);
			}
		}
		/*
		 * We should create an exception type that contains a lists of exceptions from all decoders.
		 * But for now we don't want to complicate SourceAFIS API.
		 * It will wait until this code gets moved to a separate image decoding library.
		 * For now, we just summarize all the exceptions in a long message.
		 */
		throw new IllegalArgumentException(String.format("Unsupported image format [%s].", all.stream()
			.map(d -> String.format("%s = '%s'", d.name(), formatError(exceptions.get(d))))
			.collect(joining(", "))));
	}
	private static String formatError(Throwable exception) {
		List<Throwable> ancestors = new ArrayList<>();
		for (Throwable ancestor = exception; ancestor != null; ancestor = ancestor.getCause())
			ancestors.add(ancestor);
		return ancestors.stream()
			.map(ex -> ex.toString())
			.collect(joining(" -> "));
	}
	static boolean hasClass(String name) {
		try {
			Class.forName(name);
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}
        
        private static class ImageIODecoder extends ImageDecoder {
		@Override boolean available() {
			return hasClass("javax.imageio.ImageIO");
		}
		@Override String name() {
			return "ImageIO";
		}
		@Override DecodedImage decode(byte[] image) {
			return Exceptions.sneak().get(() -> {
				BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(image));
				if (buffered == null)
					throw new IllegalArgumentException("Unsupported image format.");
				int width = buffered.getWidth();
				int height = buffered.getHeight();
				int[] pixels = new int[width * height];
				buffered.getRGB(0, 0, width, height, pixels, 0, width);
				return new DecodedImage(width, height, pixels);
			});
		}
	}
        
        	private static class WsqDecoder extends ImageDecoder {
		@Override boolean available() {
			/*
			 * JNBIS WSQ decoder is pure Java, which means it is always available.
			 */
			return true;
		}
		@Override String name() {
			return "WSQ";
		}
		@Override DecodedImage decode(byte[] image) {
			if (image.length < 2 || image[0] != (byte)0xff || image[1] != (byte)0xa0)
				throw new IllegalArgumentException("This is not a WSQ image.");
			return Exceptions.sneak().get(() -> {
				Bitmap bitmap = Jnbis.wsq().decode(image).asBitmap();
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				byte[] buffer = bitmap.getPixels();
				int[] pixels = new int[width * height];
				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {
						int gray = buffer[y * width + x] & 0xff;
						pixels[y * width + x] = 0xff00_0000 | (gray << 16) | (gray << 8) | gray;
					}
				}
				return new DecodedImage(width, height, pixels);
			});
		}
	}
    
}
