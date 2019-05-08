package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

public class InputStep< T extends RealType< T > > extends AbstractWorkflowImgStep< T >
{
	private Img< T > image;

	@Override
	public Img< T > getImg()
	{
		return image;
	}

	public void setImage( Img< T > image )
	{
		this.image = image;
	}
}
