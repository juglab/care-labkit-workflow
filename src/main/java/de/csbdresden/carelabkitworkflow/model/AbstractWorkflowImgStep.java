package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractWorkflowImgStep< T extends RealType< T > > extends AbstractWorkflowStep
{

	private String infoText;

	public abstract Img< T > getImg();

	public void setInfo( final String info )
	{
		this.infoText = info;
	}

	public String getCurrentInfoText()
	{
		return this.infoText;
	}

}
