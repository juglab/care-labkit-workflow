package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class DenoisingStep< T extends RealType< T > & NativeType< T > > extends AbstractWorkflowImgStep< T >
{
	private Img< T > image;

	private String modelUrl;
	
	private float gaussSigma = 2;
	
	private boolean isGauss = false;

	@Override
	public Img< T > getImg()
	{
		return image;
	}

	public void setImage( final Img< T > image )
	{
		this.image = image;
	}

	public String getModelUrl()
	{
		return modelUrl;
	}

	public void setModelUrl( final String modelUrl )
	{
		this.modelUrl = modelUrl;
	}

	public float getGaussSigma()
	{
		return this.gaussSigma;
	}

	public void setGaussSigma( final float sigma )
	{
		this.gaussSigma = sigma;
	}

	public boolean isGauss()
	{
		return this.isGauss;
	}
	
	public void useGaussianFilter(final boolean enable) {
		this.isGauss = enable;
	}
}
