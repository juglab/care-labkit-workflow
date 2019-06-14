package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.csbdresden.carelabkitworkflow.model.AbstractWorkflowImgStep;
import de.csbdresden.carelabkitworkflow.model.InputStep;
import de.csbdresden.carelabkitworkflow.model.DenoisingStep;
import de.csbdresden.carelabkitworkflow.model.ForwardingRandomAccessibleInterval;
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

	private ForwardingRandomAccessibleInterval< LabelingType< String > > proxyLabeling;

	SegmentationPanel( final SegmentationStep< T, I > segmentationStep, final InputStep< T > inputStep, final DenoisingStep< T > networkStep )
	{
		this.segmentationStep = segmentationStep;
		this.inputStep = inputStep;
		this.networkStep = networkStep;
		setBackground( new Color( 49, 193, 255 ) );

	}

	public void update()
	{
		runOnEventDispatchThread( () -> {
			if ( !segmentationStep.isActivated() || !inputStep.isActivated() )
			{
				showInBdv( null );
				reset();
			}
			else
			{
				showSegmentation();
			}
		} );
	}

	private void showSegmentation()
	{
		if ( inputStep.isActivated() )
		{

			if ( segmentationStep.getLabeling() != null )
			{
				synchronized ( segmentationStep )
				{
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
					displayLabelingSource( labeling, conv );
					bdv.getViewerPanel().requestRepaint();
				}
			}
			else
			{
				bdv.getViewerPanel().removeAllSources();
				proxySource = null;
			}
		}
		else
		{
			bdv.getViewerPanel().removeAllSources();
			proxySource = null;
		}
	}

	private void displayLabelingSource( final RandomAccessibleInterval< LabelingType< String > > labeling, final ColorTableConverter< String > conv )
	{
		// Ask toby how one should do that?
//		if (proxyLabeling == null ) {
//			setProxyLabeling( labeling );
//			labelingSource = ( BdvStackSource< ARGBType > ) BdvFunctions.show( Converters.convert( proxyLabeling, conv, new ARGBType() ), String.valueOf( segmentationStep.getCurrentId() ), Bdv.options().addTo( bdv ) );
//			labelingSource.setDisplayRange( 0, 255 );
//		} else {
//			setProxyLabeling( labeling ); 
//			labelingSource.setDisplayRange( 0, 255 );
//		}
		if ( labelingSource != null )
		{
			labelingSource.removeFromBdv();
		}
		labelingSource = ( BdvStackSource< ARGBType > ) BdvFunctions.show( Converters.convert( labeling, conv, new ARGBType() ), String.valueOf( segmentationStep.getCurrentId() ), Bdv.options().addTo( bdv ) );
		labelingSource.setDisplayRange( 0, 255 );
	}

	private void setProxyLabeling( final RandomAccessibleInterval< LabelingType< String > > labeling )
	{
		if ( proxyLabeling == null )
		{
			proxyLabeling = new ForwardingRandomAccessibleInterval<>( labeling );
		}
		else
		{
			proxyLabeling.setSource( labeling );
		}
	}

	private void displayInput( final AbstractWorkflowImgStep< T > step )
	{
		if ( step.getImg() == null )
			return;
		if ( proxySource == null )
		{
			setProxySource( ( RandomAccessibleInterval< T > ) step.getImg() );
			source = BdvFunctions.show( proxySource, "", Bdv.options().addTo( bdv ) );
			source.setDisplayRange( step.getLowerPercentile(), step.getUpperPercentile() );
		}
		else
		{
			setProxySource( ( RandomAccessibleInterval< T > ) step.getImg() );
			source.setDisplayRange( step.getLowerPercentile(), step.getUpperPercentile() );
		}
	}

	public void reset()
	{
		runOnEventDispatchThread( () -> {
			bdv.getBdvHandle().getViewerPanel().removeAllSources();
			numberLabel.setText( "" );
			methodLabel.setText( "" );
			repaint();
		} );
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
