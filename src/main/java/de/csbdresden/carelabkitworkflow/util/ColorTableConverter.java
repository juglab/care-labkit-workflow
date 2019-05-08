package de.csbdresden.carelabkitworkflow.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.imglib2.converter.Converter;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;

/**
 * This {@link Converter} aggregates multiple color lookup tables into one by
 * adding the aRGB values component wise.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class ColorTableConverter< L > implements Converter< LabelingType< L >, ARGBType >
{

	/**
	 * The {@link ImgLabeling} mapping the indices to label-sets.
	 */
	private final LabelingMapping< L > labelingMapping;

	/**
	 * All luts.
	 */
	private final ArrayList< ColorTable > colorTables;

	/**
	 * The combined lut.
	 */
	private int[] lut;

	/**
	 * Map from the lut-value to index in the imgLabeling.
	 */
	private Map< Integer, Set< L > > reverseLut;

	/**
	 * This color table converter aggregates all given luts and converts
	 * imgLabeling-pixels to the corresponding aggregated ARGB-LUT.
	 * 
	 * @param mapping
	 *            index image of the labeling
	 */
	public ColorTableConverter( final LabelingMapping< L > mapping )
	{
		this.labelingMapping = mapping;
		colorTables = new ArrayList<>();

	}

	/**
	 * Update the aggregated LUT.
	 */
	public synchronized void update()
	{
		final int[] newlut = new int[ labelingMapping.numSets() ];
		reverseLut = new HashMap<>();

		for ( final ColorTable colorTable : colorTables )
		{
			final int[] ct = colorTable.getLut();
			if ( ct == null )
				continue;

			for ( int i = 0; i < ct.length; i++ )
			{
				final int acc = newlut[ i ];
				final int col = ct[ i ];
				newlut[ i ] = combineColors( acc, col );
				reverseLut.put( newlut[ i ], labelingMapping.labelsAtIndex( i ) );

			}
		}

		lut = newlut;
	}

	public static int combineColors(int a, int b) {
		final int rTarget = Math.min(255, (ARGBType.red(a) + ARGBType.red(b)));
		final int gTarget = Math.min(255, (ARGBType.green(a) + ARGBType.green(b)));
		final int bTarget = Math.min(255, (ARGBType.blue(a) + ARGBType.blue(b)));
		final int aTarget = Math.min(255, ARGBType.alpha(a) + ARGBType.alpha(b));
		return ARGBType.rgba(rTarget, gTarget, bTarget, aTarget);
}
	
	@Override
	public void convert( LabelingType< L > input, ARGBType output )
	{
		output.set( lut[ input.getIndex().getInteger() ] );
	}

	/**
	 * Add another LUT.
	 * 
	 * @param colorTable
	 *            LUT
	 * @return success
	 */
	public synchronized boolean addColorTable( final ColorTable colorTable )
	{
		if ( !colorTables.contains( colorTable ) )
		{
			colorTables.add( colorTable );
			return true;
		}
		return false;
	}

	/**
	 * Remove LUT.
	 * 
	 * @param colortable
	 *            to remove
	 * @return success
	 */
	public synchronized boolean removeColorTable( final ColorTable colortable )
	{
		return colorTables.remove( colortable );
	}

	/**
	 * Get {@link ImgLabeling} index for a given aRGB value.
	 * 
	 * @param argbValue
	 *            index
	 * @return corresponding index-value in {@link ImgLabeling}
	 */
	public Set< L > getLabelIndex( final int argbValue )
	{
		return reverseLut.get( argbValue );
	}

	/**
	 * Alpha blend colors.
	 * 
	 * @param a
	 * @param b
	 * @return blended color
	 */
	public static int combineAlphaColors( int a, int b )
	{
		final int rA = ARGBType.red( a );
		final int rB = ARGBType.red( b );
		final int gA = ARGBType.green( a );
		final int gB = ARGBType.green( b );
		final int bA = ARGBType.blue( a );
		final int bB = ARGBType.blue( b );

		final double aA = ARGBType.alpha( a ) / 255.0;
		final double aB = ARGBType.alpha( b ) / 255.0;

		final int aTarget = ( int ) ( ( aA + aB - aA * aB ) * 255 );
		final int rTarget = ( int ) ( ( rA * aA ) + ( rB * aB * ( 1.0 - aA ) ) );
		final int gTarget = ( int ) ( ( gA * aA ) + ( gB * aB * ( 1.0 - aA ) ) );
		final int bTarget = ( int ) ( ( bA * aA ) + ( bB * aB * ( 1.0 - aA ) ) );
		return ARGBType.rgba( rTarget, gTarget, bTarget, aTarget );
	}
}
