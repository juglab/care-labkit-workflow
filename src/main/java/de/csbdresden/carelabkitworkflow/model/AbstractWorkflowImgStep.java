package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractWorkflowImgStep< T extends RealType< T > & NativeType< T > > extends AbstractWorkflowStep
{

	private String infoText;
	
	private float lp;
	
	private float up;

	public abstract Img< T > getImg();

	public void setInfo( final String info )
	{
		this.infoText = info;
	}

	public String getCurrentInfoText()
	{
		return this.infoText;
	}
	
	public void setLowerPercentile(final float lp) {
		this.lp = lp;
	}
	
	public void setUpperPercentile(final float up) {
		this.up = up;
	}
	
	public float getLowerPercentile() {
		return this.lp;
	}

	public float getUpperPercentile() {
		return this.up;
	}
	
}
