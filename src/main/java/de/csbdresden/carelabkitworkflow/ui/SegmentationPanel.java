package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.csbdresden.carelabkitworkflow.model.NetworkStep;
import de.csbdresden.carelabkitworkflow.model.SegmentationStep;
import de.csbdresden.carelabkitworkflow.util.ColorTableConverter;
import de.csbdresden.carelabkitworkflow.util.RandomColorTable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;

public class SegmentationPanel< T extends RealType< T >, I extends IntegerType< I > > extends AbstractBDVPanel< T >
{

	private final SegmentationStep< T, I > segmentationStep;

	private final NetworkStep< T > inputStep;

	private BdvStackSource< T > labelingSource;

	SegmentationPanel( final SegmentationStep< T, I > segmentationStep, final NetworkStep< T > networkStep )
	{
		this.segmentationStep = segmentationStep;
		this.inputStep = networkStep;
		setBackground( new Color( 49, 193, 255 ) );

	}

	protected void initStep()
	{
		this.segmentationStep.setInfoTextPanel( infoTextPane );
	}

	public void update()
	{
		if ( !segmentationStep.isActivated() )
		{
			showInBdv( null );
			infoTextPane.setText( EMPTY_INFO_TEXT );
		}
		else
		{
			showSegmentation();
			segmentationStep.updateInfoText();
		}
	}
	
	private void showSegmentation()
	{
		bdv.getBdvHandle().getViewerPanel().removeAllSources();
		if ( segmentationStep != null )
		{

			if ( segmentationStep.getImg() != null )
			{
				synchronized ( segmentationStep )
				{
					RandomAccessibleInterval< LabelingType< String > > labeling = segmentationStep.getSegmentation();
					
					final LabelingMapping< String > mapping = Util.getTypeFromInterval( labeling ).getMapping();
					final ColorTableConverter< String > conv = new ColorTableConverter< String >( mapping );
					final RandomColorTable< T, String, I > segmentColorTable = new RandomColorTable<>( mapping, conv, bdv.getViewerPanel() );
					conv.addColorTable( segmentColorTable );
					segmentColorTable.fillLut();
					segmentColorTable.update();

					Pair< T, T > minMax = parent.getLowerUpperPerc( inputStep.getImg() );
					
					source = BdvFunctions.show( inputStep.getImg(), String.valueOf( inputStep.getCurrentId() ), Bdv.options().addTo( bdv ) );
					labelingSource = BdvFunctions.show( ( RandomAccessibleInterval< T > ) Converters.convert( labeling, conv, new ARGBType() ), String.valueOf( inputStep.getCurrentId() ), Bdv.options().addTo( bdv ));

					source.setDisplayRange( minMax.getA().getRealFloat(), minMax.getB().getRealFloat());
					labelingSource.setDisplayRange( 0, 255 );
				}
			}
		}
	}

}
