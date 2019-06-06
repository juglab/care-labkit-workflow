package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.csbdresden.carelabkitworkflow.model.AbstractWorkflowImgStep;
import de.csbdresden.carelabkitworkflow.model.InputStep;
import de.csbdresden.carelabkitworkflow.model.DenoisingStep;
import de.csbdresden.carelabkitworkflow.model.SegmentationStep;
import de.csbdresden.carelabkitworkflow.util.ColorTableConverter;
import de.csbdresden.carelabkitworkflow.util.RandomColorTable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class SegmentationPanel< T extends RealType< T > & NativeType< T >, I extends IntegerType< I > > extends AbstractBDVSegmentationPanel< T, I >
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final SegmentationStep< T, I > segmentationStep;

	private final DenoisingStep< T > networkStep;

	private BdvStackSource< ARGBType > labelingSource;

	private InputStep< T > inputStep;

	SegmentationPanel( final SegmentationStep< T, I > segmentationStep, final InputStep< T > inputStep, final DenoisingStep< T > networkStep )
	{
		this.segmentationStep = segmentationStep;
		this.inputStep = inputStep;
		this.networkStep = networkStep;
		setBackground( new Color( 49, 193, 255 ) );

	}

	public void update()
	{
		if ( !segmentationStep.isActivated() )
		{
			showInBdv( null );
		}
		else
		{
			showSegmentation();
		}
	}

	private void showSegmentation()
	{
		bdv.getBdvHandle().getViewerPanel().removeAllSources();
		if ( inputStep.isActivated() )
		{

			if ( segmentationStep.getLabeling() != null )
			{
//				synchronized ( segmentationStep )
//				{
				RandomAccessibleInterval< LabelingType< String > > labeling = segmentationStep.getLabeling();

				final LabelingMapping< String > mapping = Util.getTypeFromInterval( labeling ).getMapping();
				final ColorTableConverter< String > conv = new ColorTableConverter< String >( mapping );
				final RandomColorTable< T, String, I > segmentColorTable = new RandomColorTable<>( mapping, conv, bdv.getViewerPanel() );
				conv.addColorTable( segmentColorTable );
				segmentColorTable.fillLut();
				segmentColorTable.update();

				if ( networkStep.isActivated() )
				{
					displayInput( networkStep );
				}
				else
				{
					displayInput( inputStep );
				}
				labelingSource = ( BdvStackSource< ARGBType > ) BdvFunctions.show( Converters.convert( labeling, conv, new ARGBType() ), String.valueOf( segmentationStep.getCurrentId() ), Bdv.options().addTo( bdv ) );
				labelingSource.setDisplayRange( 0, 255 );
				updateMethodLabel();
				updateNumberLabel();
//				}
			}
		}
	}

	private void displayInput( final AbstractWorkflowImgStep< T > step )
	{
		source = BdvFunctions.show( ( RandomAccessibleInterval< T > ) step.getImg(), segmentationStep.getName(), Bdv.options().addTo( bdv ) );
		source.setDisplayRange( step.getLowerPercentile(), step.getUpperPercentile() );
	}

	public void reset()
	{
		bdv.getBdvHandle().getViewerPanel().removeAllSources();
	}

	@Override
	protected void initStep()
	{
		// NB: Nothing to do
	}

	@Override
	protected void updateMethodLabel()
	{
		if ( segmentationStep.isActivated() )
		{
			methodLabel.setText( segmentationStep.getName() );
		}
		else
		{
			methodLabel.setText( "" );
		}
	}

	@Override
	protected void updateNumberLabel()
	{
		if ( segmentationStep.isActivated() && segmentationStep.useManual() )
		{
			numberLabel.setText( String.valueOf( "   Schwellwert = " + Math.round( segmentationStep.getThreshold() * 100 ) / 100.0 ) );
		}
		else
		{
			numberLabel.setText( "" );
		}
	}

}
