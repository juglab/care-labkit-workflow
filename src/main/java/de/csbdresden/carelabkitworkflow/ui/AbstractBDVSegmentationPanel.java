package de.csbdresden.carelabkitworkflow.ui;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractBDVSegmentationPanel< T extends RealType< T > & NativeType< T >, I extends IntegerType< I > > extends AbstractBDVPanel< T >
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
