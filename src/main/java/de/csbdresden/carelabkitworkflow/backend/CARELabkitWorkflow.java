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
import de.csbdresden.carelabkitworkflow.model.DenoisingStep;
import de.csbdresden.carelabkitworkflow.model.InputStep;
import de.csbdresden.carelabkitworkflow.model.OutputStep;
import de.csbdresden.carelabkitworkflow.model.SegmentationStep;
import de.csbdresden.carelabkitworkflow.util.SEG_Score;
import de.csbdresden.csbdeep.commands.GenericNetwork;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

public class CARELabkitWorkflow< T extends NativeType< T > & RealType< T >, I extends IntegerType< I > >
{

	private static final String PLANARIA_NET_INFO = "CARE network trained on pairs of low- and high-quality images. The low quality images were acquired with a x-times lower exposure time and a y-times lower laser power.";

	private static final String TRIBOLIUM_NET_INFO = "CARE network trained on pairs of low- and high-quality images. The low quality images were acquired with a x-times lower exposure time and a y-times lower laser power.";

	private static final String TRIBOLIUM_NET = "http://csbdeep.bioimagecomputing.com/model-tribolium.zip";

	private static final String PLANARIA_NET = "http://csbdeep.bioimagecomputing.com/model-planaria.zip";

	private static final String SEGMENTATION_INFO = "A straight forward way to obtain a segmentation, i. e. detection of individual cells, is to threshold the image. This means all regions with brightness over a given threshold value are considered foreground and all values below are considered background. \n"
			+ "Current threshold: {0}";

	private static final String GAUSS_FILTER = "Gauss_Filter";

	@Parameter
	private IOService ioService;

	@Parameter
	private OpService opService;

	@Parameter
	private CommandService commandService;

	private final Map< String, InputCache< T > > inputs = new HashMap<>();

	private final InputStep< T > inputStep;

	private final DenoisingStep< T > denoisingStep;

	private final SegmentationStep< T, I > segmentationStep;

	private final OutputStep outputStep;

	private final boolean loadChachedCARE;

	private String url;

	private final Converter< I, UnsignedShortType > conv;

	private boolean updated = false;

	public CARELabkitWorkflow( final boolean loadChachedCARE )
	{
		this.loadChachedCARE = loadChachedCARE;
		inputStep = new InputStep<>();
		denoisingStep = new DenoisingStep<>();
		segmentationStep = new SegmentationStep<>();
		outputStep = new OutputStep();
		outputStep.setActivated( true );
		conv = new Converter< I, UnsignedShortType >()
		{

			@Override
			public void convert( I input, UnsignedShortType output )
			{
				output.set( input.getInteger() );
			}
		};
	}

	public void run()
	{
		runDenoising();
		runSegmentation();
		calculateOutput();
	}

	public void calculateOutput()
	{
		if ( !outputStep.isActivated() || segmentationStep.getLabeling() == null )
		{
			outputStep.setResult( -1 );
			return;
		} ;
		final SEG_Score seg = new SEG_Score( opService.log() );
		double score = seg.calculate( inputStep.getGT(), ( RandomAccessibleInterval< UnsignedShortType > ) Converters.convert( segmentationStep.getLabeling().getIndexImg(), conv, new UnsignedShortType() ) );

		outputStep.setResult( score );
		System.out.println(
				"Threshold: " + segmentationStep.getThreshold() + ", calculated output " + outputStep.getResult() );
	}

	private void runLabkit()
	{
		// TODO
	}

	private void runManualThreshold()
	{
		if ( getSegmentationInput() != null )
		{
			final Pair< T, T > minMax = getMinMax( getSegmentationInput() );
			final T threshold = minMax.getB();
			threshold.sub( minMax.getA() );
			threshold.mul( segmentationStep.getThreshold() );
			threshold.add( minMax.getA() );
			final IterableInterval< BitType > thresholded = opService.threshold().apply( Views.iterable( getSegmentationInput() ), threshold );
			segmentationStep.setLabeling( ( ImgLabeling< String, I > ) opService.labeling().cca( ( RandomAccessibleInterval< IntegerType > ) thresholded, StructuringElement.FOUR_CONNECTED ) );
		}
	}

	private void runOtsuThreshold()
	{
		if ( getSegmentationInput() != null )
		{
			final IterableInterval< BitType > thresholded = opService.threshold().otsu( Views.iterable( getSegmentationInput() ) );
			segmentationStep.setLabeling( opService.labeling().cca( ( RandomAccessibleInterval< IntegerType > ) thresholded, StructuringElement.FOUR_CONNECTED ) );
		}
	}

	private Pair< T, T > getMinMax( final Img< T > img )
	{
		return opService.stats().minMax( img );
	}

	private Img< T > getSegmentationInput()
	{
		return denoisingStep.isActivated() ? denoisingStep.getImg() : inputStep.getImg();
	}

	public void runSegmentation()
	{
		if ( !segmentationStep.isActivated() || getSegmentationInput() == null || !inputStep.isActivated() ) { return; }
		if ( segmentationStep.isUseLabkit() )
		{
			runLabkit();
		}
		else if ( segmentationStep.getCurrentId() == 0 )
		{
			runManualThreshold();
		}
		else if ( segmentationStep.getCurrentId() == 1 )
		{
			runOtsuThreshold();
		}
	}

	public void runDenoising()
	{
		if ( !denoisingStep.isActivated() || inputStep.getImg() == null || !inputStep.isActivated() )
		{
			denoisingStep.setImage( null );
			return;
		}
		if ( denoisingStep.getModelUrl() == null )
			return;
		if ( loadChachedCARE )
		{
			if ( denoisingStep.isGauss() )
			{
				System.out.println( "gauss" );
				run_gaussFilter();
				setPercentiles( denoisingStep, denoisingStep.getImg() );
			}
			else
			{
				System.out.println( "network" );
				denoisingStep.setImage( inputs.get( url ).getDenoised( denoisingStep.getModelUrl() ) );
				setPercentiles( denoisingStep, denoisingStep.getImg() );
			}
			return;
		}
		if ( denoisingStep.getImg() != null ) { return; }
		try
		{
			final CommandModule module = commandService.run( GenericNetwork.class, false, "input", getInput(),
					"modelUrl", denoisingStep.getModelUrl(), "nTiles", 10, "showProgressDialog", false ).get();
			denoisingStep.setImage( ( Img< T > ) module.getOutput( "output" ) );
			setPercentiles( denoisingStep, denoisingStep.getImg() );
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}
	}

	public synchronized double getOutput()
	{
		return outputStep.getResult();
	}

	@SuppressWarnings( "unchecked" )
	public synchronized void setInput( final int id )
	{
		url = "";
		if ( id == 0 )
			url = "tribolium.tif";
		if ( id == 1 )
			url = "planaria.tif";
		inputStep.setCurrentId( id );
		if ( inputs.containsKey( url ) )
		{
			inputStep.setImage( inputs.get( url ).getInput(), inputs.get( url ).getGT() );
			setPercentiles( inputStep, inputStep.getImg() );
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
			final Img< UnsignedShortType > gt = ( Img< UnsignedShortType > ) ioService.open( this.getClass().getResource( url.substring( 0, url.length() - 4 ) + "_gt_labeling.tif" ).getPath() );
			if ( inputimg != null )
			{
				final InputCache< T > input = new InputCache<>( inputimg, gt );
				inputs.put( url, input );
				inputStep.setImage( input.getInput(), input.getGT() );
				setPercentiles( inputStep, inputStep.getImg() );
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

	private void setPercentiles( final AbstractWorkflowImgStep< T > step, final IterableInterval< T > img )
	{
		final ValuePair< T, T > percentiles = computeInputPercentiles( img );
		step.setLowerPercentile( percentiles.getA().getRealFloat() );
		step.setUpperPercentile( percentiles.getB().getRealFloat() );
	}

	public synchronized RandomAccessibleInterval< T > getInput()
	{
		return inputStep.getImg();
	}

	public synchronized RandomAccessibleInterval< T > getDenoisedInput()
	{
		return denoisingStep.getImg();
	}

	public synchronized void setDenoisingMethod( final int id )
	{
		if ( id == 0 )
		{
			denoisingStep.setModelUrl( TRIBOLIUM_NET );
			denoisingStep.setInfo( TRIBOLIUM_NET_INFO );
			denoisingStep.setName( "Trained on Tribolium" );
			denoisingStep.setImage( inputs.get( url ).getDenoised( TRIBOLIUM_NET ) );
			denoisingStep.useGaussianFilter( false );
		}else if ( id == 1 )
		{
			denoisingStep.setModelUrl( PLANARIA_NET );
			denoisingStep.setInfo( PLANARIA_NET_INFO );
			denoisingStep.setName( "Trained on Planaria" );
			denoisingStep.setImage( inputs.get( url ).getDenoised( PLANARIA_NET ) );
			denoisingStep.useGaussianFilter( false );
		}else if ( id == 2 )
		{
			denoisingStep.setModelUrl( GAUSS_FILTER );
			denoisingStep.setInfo( "Gauss Filtering" );
			denoisingStep.setName( "Gauss Filter" );
			denoisingStep.useGaussianFilter( true );
			run_gaussFilter();
		}
		denoisingStep.setCurrentId( id );
		setPercentiles( denoisingStep, denoisingStep.getImg() );
	}

	private void run_gaussFilter()
	{
		final Img< T > input = inputs.get( url ).getInput();
		final Img< T > out = input.factory().create( input );
		opService.filter().gauss( out, input, denoisingStep.getGaussSigma() );
		denoisingStep.setImage( out );
	}

	public synchronized void setSegmentation( final int id )
	{
		segmentationStep.setUseLabkit( id == 2 );
		segmentationStep.setManual( id == 0 );
		segmentationStep.setCurrentId( id );
		if ( id == 0 )
		{
			segmentationStep.setName( "Manual Threshold" );
		}
		else if ( id == 1 )
		{
			segmentationStep.setName( "Otsu Threshold" );
		}
	}

	public synchronized float getThreshold()
	{
		return segmentationStep.getThreshold();
	}

	public synchronized void setThreshold( final float threshold )
	{
		if ( threshold >= 0 && threshold <= 1 )
			segmentationStep.setThreshold( threshold );
	}

	private ValuePair< T, T > computeInputPercentiles( final IterableInterval< T > iterableInterval )
	{

		final T lp = iterableInterval.firstElement().createVariable();
		opService.stats().percentile( lp, iterableInterval, 3.0 );
		final T up = iterableInterval.firstElement().createVariable();
		opService.stats().percentile( up, iterableInterval, 99.0 );
		return new ValuePair< T, T >( lp, up );
	}

	public synchronized InputStep< T > getInputStep()
	{
		return inputStep;
	}

	public synchronized DenoisingStep< T > getNetworkStep()
	{
		return denoisingStep;
	}

	public synchronized SegmentationStep< T, I > getSegmentationStep()
	{
		return segmentationStep;
	}

	public synchronized OutputStep getOutputStep()
	{
		return outputStep;
	}

	public synchronized float getGaussSigma()
	{
		return denoisingStep.getGaussSigma();
	}

	public synchronized void setGaussSigma( final float sigma )
	{
		if ( sigma > 0 )
		{
			System.out.println( sigma );
			denoisingStep.setGaussSigma( sigma );
		}
	}

	public synchronized void requestUpdate()
	{
		updated = false;
	}

	public synchronized boolean needsUpdate()
	{
		return !updated;
	}
	
	public synchronized void updated() {
		updated = true;
	}
}
