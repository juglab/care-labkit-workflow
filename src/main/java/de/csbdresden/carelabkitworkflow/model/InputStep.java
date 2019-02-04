package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;

public class InputStep extends AbstractWorkflowStep {
	private Img image;

	public Img getImage() {
		return image;
	}

	public void setImage(Img image) {
		this.image = image;
	}
}
