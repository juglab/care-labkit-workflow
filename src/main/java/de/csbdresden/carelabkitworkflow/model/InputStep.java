package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class InputStep< T extends RealType< T > & NativeType< T > > extends AbstractWorkflowImgStep< T >
{
	private Img< T > image;

	private Img< UnsignedShortType > gt;
	
	@Override
	public Img< T > getImg()
	{
		return image;
	}

	public void setImage( final Img< T > image, final Img< UnsignedShortType > gt )
	{
		this.image = image;
		this.gt = gt;
	}
	
	public Img<UnsignedShortType> getGT() {
		return gt;
	}
}
