package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

public class NetworkStep<T extends RealType< T >> extends AbstractWorkflowImgStep<T> {
	private Img<T> image;
	private String modelUrl;

	@Override
	public Img<T> getImg() {
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
