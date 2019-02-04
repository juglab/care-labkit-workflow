package de.csbdresden.carelabkitworkflow.backend;

import net.imglib2.img.Img;

import java.util.HashMap;
import java.util.Map;

public class InputCache {
	private Img input;
	private Map<String, Img> denoised;

	public InputCache(Img input) {
		this.input = input;
		denoised = new HashMap<>();
	}

	public Img getInput() {
		return input;
	}

	public Img getDenoised(String network) {
		return denoised.get(network);
	}

	public void setDenoised(String modelUrl, Img denoisedInput) {
		denoised.put(modelUrl, denoisedInput);
	}
}
