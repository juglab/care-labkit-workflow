package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;

public class NetworkStep extends AbstractWorkflowStep {
	private Img image;
	private String modelUrl;

	public Img getImage() {
		return image;
	}

	public void setImage(Img image) {
		this.image = image;
	}

	public String getModelUrl() {
		return modelUrl;
	}

	public void setModelUrl(String modelUrl) {
		this.modelUrl = modelUrl;
	}
}
