package de.csbdresden.carelabkitworkflow.backend;

import java.util.HashMap;
import java.util.Map;

import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class InputCache< T extends RealType< T > & NativeType< T > >
{
	private final Img< T > input;
	
	private final Img< UnsignedShortType > gt;

	private final Map< String, Img< T > > denoised;

	public InputCache( final Img< T > input, final Img< UnsignedShortType > gt )
	{
		this.input = input;
		this.gt = gt;
		denoised = new HashMap<>();
	}

	public Img< T > getInput()
	{
		return input;
	}

	public Img< T > getDenoised( final String network )
	{
		return denoised.get( network );
	}

	public void setDenoised( final String modelUrl, final Img< T > denoisedInput )
	{
		denoised.put( modelUrl, denoisedInput );
	}

	public Img<UnsignedShortType> getGT()
	{
		return gt;
	}
}
