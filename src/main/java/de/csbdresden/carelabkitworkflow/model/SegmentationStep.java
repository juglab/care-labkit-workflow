package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;

public class SegmentationStep extends AbstractWorkflowStep {
	private Img image;
	private float threshold = 0.5f;
	private boolean useLabkit = false; // otherwise threshold

	public Img getImage() {
		return image;
	}

	public void setImage(Img image) {
		this.image = image;
	}

	public float getThreshold() {
		return threshold;
	}

	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}

	public boolean isUseLabkit() {
		return useLabkit;
	}

	public void setUseLabkit(boolean useLabkit) {
		this.useLabkit = useLabkit;
	}
}
