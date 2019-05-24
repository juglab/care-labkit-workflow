package de.csbdresden.carelabkitworkflow.backend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;

import de.csbdresden.carelabkitworkflow.model.AbstractWorkflowImgStep;
import de.csbdresden.carelabkitworkflow.model.InputStep;
import de.csbdresden.carelabkitworkflow.model.NetworkStep;
import de.csbdresden.carelabkitworkflow.model.OutputStep;
import de.csbdresden.carelabkitworkflow.model.SegmentationStep;
import de.csbdresden.csbdeep.commands.GenericNetwork;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

public class CARELabkitWorkflow< T extends RealType< T > & NativeType< T >, I extends IntegerType< I > >
{

	private static final String PLANARIA_NET_INFO = "CARE network trained on pairs of low- and high-quality images. The low quality images were acquired with a x-times lower exposure time and a y-times lower laser power.";

	private static final String TRIBOLIUM_NET_INFO = "CARE network trained on pairs of low- and high-quality images. The low quality images were acquired with a x-times lower exposure time and a y-times lower laser power.";

	private static final String TRIBOLIUM_NET = "http://csbdeep.bioimagecomputing.com/model-tribolium.zip";

	private static final String PLANARIA_NET = "http://csbdeep.bioimagecomputing.com/model-planaria.zip";

	private static final String SEGMENTATION_INFO = "A straight forward way to obtain a segmentation, i. e. detection of individual cells, is to threshold the image. This means all regions with brightness over a given threshold value are considered foreground and all values below are considered background. \n"
			+ "Current threshold: {0}";

	@Parameter
	private IOService ioService;

	@Parameter
	private OpService opService;

	@Parameter
	private CommandService commandService;

	private final Map< String, InputCache< T > > inputs = new HashMap<>();

	private final InputStep< T > inputStep;

	private final NetworkStep< T > networkStep;

	private final SegmentationStep< T, I > segmentationStep;

	private final OutputStep outputStep;

	private final boolean loadChachedCARE;

	private String url;

	public CARELabkitWorkflow( final boolean loadChachedCARE )
	{
		this.loadChachedCARE = loadChachedCARE;
		inputStep = new InputStep<>();
		networkStep = new NetworkStep<>();
		segmentationStep = new SegmentationStep<>();
		outputStep = new OutputStep();
		outputStep.setActivated( true );
	}

	public void run()
	{
		runNetwork();
		runSegmentation();
		calculateOutput();
	}

	public void calculateOutput()
	{
		final RandomAccessibleInterval< T > segmentedInput = segmentationStep.getImg();
		if ( !outputStep.isActivated() || segmentedInput == null )
		{
			outputStep.setResult( -1 );
			return;
		} ;

		final LabelRegions< String > regions = new LabelRegions< String >( segmentationStep.getSegmentation() );
		outputStep.setResult( regions.getExistingLabels().size() );
		System.out.println(
				"Threshold: " + segmentationStep.getThreshold() + ", calculated output " + outputStep.getResult() );
	}

	private void runLabkit()
	{
		// TODO
	}

	private void runThreshold()
	{
		if ( getSegmentationInput() != null )
		{
			final Pair< T, T > minMax = getMinMax( getSegmentationInput() );
			final T threshold = minMax.getB();
			threshold.sub( minMax.getA() );
			threshold.mul( segmentationStep.getThreshold() );
			threshold.add( minMax.getA() );
			segmentationStep.setImage( ( Img< T > ) opService.threshold().apply( Views.iterable( getSegmentationInput() ), threshold ) );
			segmentationStep.setSegmentation( ( ImgLabeling< String, I > ) opService.labeling().cca( ( RandomAccessibleInterval< IntegerType > ) segmentationStep.getImg(), StructuringElement.FOUR_CONNECTED ) );
			setPercentiles( segmentationStep );
		}
	}

	private Pair< T, T > getMinMax( final Img< T > img )
	{
		return opService.stats().minMax( img );
	}

	private Img< T > getSegmentationInput()
	{
		return networkStep.isActivated() ? networkStep.getImg() : inputStep.getImg();
	}

	public void runSegmentation()
	{
		if ( !segmentationStep.isActivated() || getSegmentationInput() == null || !inputStep.isActivated() )
		{
			segmentationStep.setImage( null );
			return;
		}
		if ( segmentationStep.isUseLabkit() )
		{
			runLabkit();
		}
		else
		{
			runThreshold();
		}
	}

	public void runNetwork()
	{
		if ( !networkStep.isActivated() || inputStep.getImg() == null || !inputStep.isActivated() )
		{
			networkStep.setImage( null );
			return;
		}
		if ( networkStep.getModelUrl() == null )
			return;
		if ( networkStep.getImg() != null ) { return; }
		if ( loadChachedCARE )
		{
			networkStep.setImage( inputs.get( url ).getDenoised( networkStep.getModelUrl() ) );
			setPercentiles( networkStep );
			return;
		}
		try
		{
			final CommandModule module = commandService.run( GenericNetwork.class, false, "input", getInput(),
					"modelUrl", networkStep.getModelUrl(), "nTiles", 10, "showProgressDialog", false ).get();
			networkStep.setImage( ( Img< T > ) module.getOutput( "output" ) );
			setPercentiles( networkStep );
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}
	}

	public int getOutput()
	{
		return outputStep.getResult();
	}

	@SuppressWarnings( "unchecked" )
	public void setInput( final int id )
	{
		url = "";
		if ( id == 0 )
			url = "tribolium.tif";
		if ( id == 1 )
			url = "planaria.tif";
		inputStep.setCurrentId( id );
		if ( inputs.containsKey( url ) )
		{
			inputStep.setImage( inputs.get( url ).getInput() );
			setPercentiles( inputStep );
			if ( url == "tribolium.tif" )
			{
				inputStep.setInfo( "Tribolium information text. Image acquired with microscope xyz, exposure, staining... Some interesting fact is... maybe you want to know" );
				inputStep.setName( "Tribolium" );
			}
			else if ( url == "planaria.tif" )
			{
				inputStep.setInfo( "Planaria information text. Image acquired with balbla blup..." );
				inputStep.setName( "Planaria" );
			}
			return;
		}
		try
		{
			final Img< T > inputimg = ( Img< T > ) ioService.open( this.getClass().getResource( url ).getPath() );
			if ( inputimg != null )
			{
				final InputCache< T > input = new InputCache<>( inputimg );
				inputs.put( url, input );
				inputStep.setImage( input.getInput() );
				setPercentiles( inputStep );
				computeInputPercentiles( inputStep.getImg() );
				if ( url == "tribolium.tif" )
				{
					inputStep.setInfo( "Tribolium information text. Image acquired with microscope xyz, exposure, staining..." );
					inputStep.setName( "Tribolium" );
				}
				else if ( url == "planaria.tif" )
				{
					inputStep.setInfo( "Planaria information text. Image acquired with balbla blup..." );
					inputStep.setName( "Planaria" );
				}
				if ( loadChachedCARE )
				{
					input.setDenoised( TRIBOLIUM_NET, ( Img< T > ) ioService.open( this.getClass()
							.getResource( url.substring( 0, url.length() - 4 ) + "_triboliumNet.tif" ).getPath() ) );
					input.setDenoised( PLANARIA_NET, ( Img< T > ) ioService.open( this.getClass()
							.getResource( url.substring( 0, url.length() - 4 ) + "_planariaNet.tif" ).getPath() ) );
				}
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	private void setPercentiles( final AbstractWorkflowImgStep< T > step )
	{
		final ValuePair< T, T > percentiles = computeInputPercentiles( step.getImg() );
		step.setLowerPercentile( percentiles.getA().getRealFloat() );
		step.setUpperPercentile( percentiles.getB().getRealFloat() );
	}

	public RandomAccessibleInterval< T > getInput()
	{
		return inputStep.getImg();
	}

	public RandomAccessibleInterval< T > getDenoisedInput()
	{
		return networkStep.getImg();
	}

	public RandomAccessibleInterval< T > getSegmentedInput()
	{
		return segmentationStep.getImg();
	}

	public void setNetwork( final int id )
	{
		if ( id == 0 )
		{
			networkStep.setModelUrl( TRIBOLIUM_NET );
			networkStep.setInfo( TRIBOLIUM_NET_INFO );
			networkStep.setName( "Trained on Tribolium" );
			networkStep.setImage( inputs.get( url ).getDenoised( TRIBOLIUM_NET ) );
		}
		if ( id == 1 )
		{
			networkStep.setModelUrl( PLANARIA_NET );
			networkStep.setInfo( PLANARIA_NET_INFO );
			networkStep.setName( "Trained on Planaria" );
			networkStep.setImage( inputs.get( url ).getDenoised( PLANARIA_NET ) );
		}
		networkStep.setCurrentId( id );
		setPercentiles( networkStep );
	}

	public void setSegmentation( final int id )
	{
		segmentationStep.setUseLabkit( id == 1 );
		segmentationStep.setCurrentId( id );
		segmentationStep.setInfo( SEGMENTATION_INFO );
		if ( id == 0 )
		{
			segmentationStep.setName( "Manual Threshold" );
		}
	}

	public float getThreshold()
	{
		return segmentationStep.getThreshold();
	}

	public void setThreshold( final float threshold )
	{
		if ( threshold >= 0 && threshold <= 1 )
			segmentationStep.setThreshold( threshold );
	}

	private ValuePair< T, T > computeInputPercentiles( final RandomAccessibleInterval< T > input )
	{

		final T lp = input.randomAccess().get().createVariable();
		opService.stats().percentile( lp, Views.iterable( input ), 3.0 );
		final T up = input.randomAccess().get().createVariable();
		opService.stats().percentile( up, Views.iterable( input ), 99.0 );
		return new ValuePair< T, T >( lp, up );
	}

	public InputStep< T > getInputStep()
	{
		return inputStep;
	}

	public NetworkStep< T > getNetworkStep()
	{
		return networkStep;
	}

	public SegmentationStep< T, I > getSegmentationStep()
	{
		return segmentationStep;
	}

	public OutputStep getOutputStep()
	{
		return outputStep;
	}
}
