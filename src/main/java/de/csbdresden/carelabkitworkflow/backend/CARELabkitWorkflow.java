package de.csbdresden.carelabkitworkflow.backend;

import de.csbdresden.carelabkitworkflow.model.*;
import de.csbdresden.carelabkitworkflow.util.SEG_Score;
import de.csbdresden.csbdeep.commands.GenericNetwork;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.labkit.BatchSegmenter;
import net.imglib2.labkit.inputimage.DefaultInputImage;
import net.imglib2.labkit.labeling.Labeling;
import net.imglib2.labkit.labeling.LabelingSerializer;
import net.imglib2.labkit.models.DefaultSegmentationModel;
import net.imglib2.labkit.models.ImageLabelingModel;
import net.imglib2.labkit.utils.progress.StatusServiceProgressWriter;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class CARELabkitWorkflow< T extends NativeType< T > & RealType< T >, I extends IntegerType< I > >
{

	public static final String PLANARIA_NAME = "Schmidtea";

	public static final String TRIBOLIUM_NAME = "Tribolium";

	private static final String TRIBOLIUM_NET = "http://csbdeep.bioimagecomputing.com/model-tribolium.zip";

	private static final String PLANARIA_NET = "http://csbdeep.bioimagecomputing.com/model-planaria.zip";

	private static final String GAUSS_FILTER = "Gauss_Filter";

	@Parameter
	private IOService ioService;

	@Parameter
	private OpService opService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private StatusService statusService;

	@Parameter
	private Context context;

	private final Map< String, InputCache< T > > inputs = new HashMap<>();

	private final InputStep< T > inputStep;

	private final DenoisingStep< T > denoisingStep;

	private final SegmentationStep< T, I > segmentationStep;

	private final OutputStep outputStep;

	private final boolean loadChachedCARE;

	private String url;

	private final Converter< I, UnsignedShortType > conv;

	private boolean updated = false;

	private Thread labkitThread;
	private Thread labkitSaveImgThread;

	private ArrayImgFactory< UnsignedByteType > facUB;

	public CARELabkitWorkflow( final boolean loadChachedCARE )
	{
		this.loadChachedCARE = loadChachedCARE;
		inputStep = new InputStep<>();
		denoisingStep = new DenoisingStep<>();
		segmentationStep = new SegmentationStep<>();
		outputStep = new OutputStep();
		outputStep.setActivated( true );
		facUB = new ArrayImgFactory< UnsignedByteType >(new UnsignedByteType());
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

	public synchronized void calculateOutput()
	{
		if ( !outputStep.isActivated() || segmentationStep.getLabeling() == null )
		{
			outputStep.setResult( -1 );
			outputStep.setInputName( "" );
			return;
		}

		final SEG_Score seg = new SEG_Score( opService.log() );
		double score = seg.calculate( inputStep.getGT(), ( RandomAccessibleInterval< UnsignedShortType > ) Converters.convert( segmentationStep.getLabeling().getIndexImg(), conv, new UnsignedShortType() ) );

		outputStep.setResult( score );
		outputStep.setInputName( inputStep.getName() );
		System.out.println(
				"Threshold: " + segmentationStep.getThreshold() + ", calculated output " + outputStep.getResult() );
	}

	private synchronized void runLabkit()
	{
		Img img = getSegmentationInput();
		if ( img != null )
		{

			ServerCommunication serverCommunication = new ServerCommunication();
			context.inject( serverCommunication );

			// init segmentation model, serializer, labeling model
			DefaultSegmentationModel segmentationModel = new DefaultSegmentationModel( new DefaultInputImage(
					img ), context );
			LabelingSerializer serializer = new LabelingSerializer( context );
			final ImageLabelingModel labelingModel = segmentationModel
					.imageLabelingModel();

			// load labeling from server file
			Labeling labeling = Labeling.createEmpty( new ArrayList<>(), img );
			try
			{
				labeling = serializer.open( serverCommunication.labelingPNG2TIF() );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
			labelingModel.labeling().set( labeling );
			if ( labelingModel.labeling().get().getLabels().size() == 0 )
			{
				System.out.println( "no labels" );
				return;
			}

			// train
			segmentationModel.train( segmentationModel
					.selectedSegmenter().get() );

			// run segmentation
			final ImagePlus segImgImagePlus = ImageJFunctions.wrap( img, "seginput" );
			final Img<ARGBType> segImg = ImageJFunctions.wrap(segImgImagePlus);
			Img< UnsignedByteType > segmentation = null;
			try
			{
				segmentation = BatchSegmenter.segment( segImg,
						segmentationModel.selectedSegmenter().get().segmenter(),
						Intervals.dimensionsAsIntArray( segImg ),
						new StatusServiceProgressWriter( statusService ) );
			}
			catch ( InterruptedException e )
			{
				segmentation = facUB.create( segImg );
				e.printStackTrace();
			}

			segmentationStep.setLabeling( opService.labeling().cca( segmentation, StructuringElement.FOUR_CONNECTED ) );
		}
	}

	private synchronized void runManualThreshold()
	{
		if ( getSegmentationInput() != null )
		{
			final T threshold = getSegmentationInput().firstElement().copy();
			threshold.setReal(mapToImgPercentiles(segmentationStep.getThreshold()));
			final IterableInterval< BitType > thresholded = opService.threshold().apply( Views.iterable( getSegmentationInput() ), threshold );
			segmentationStep.setLabeling( opService.labeling().cca( ( RandomAccessibleInterval< IntegerType > ) thresholded, StructuringElement.FOUR_CONNECTED ) );
		}
	}

	private float mapToImgPercentiles(float threshold) {
		//use minmax:
//		Pair<T, T> minmax = opService.stats().minMax(getSegmentationInput());
//		float res = minmax.getA().getRealFloat() + (minmax.getB().getRealFloat() - minmax.getA().getRealFloat())*threshold;
		float lower = getSegmentationInputStep().getLowerPercentile();
		float upper = getSegmentationInputStep().getUpperPercentile();
		float res = lower + (upper-lower)*threshold;
		System.out.println(res + " (" + lower + ", " + upper + ")");
		return res;
	}

	private synchronized void runOtsuThreshold()
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


	private AbstractWorkflowImgStep getSegmentationInputStep() {
		return denoisingStep.isActivated() ? denoisingStep : inputStep;
	}

	public synchronized void runSegmentation()
	{
		if ( !segmentationStep.isActivated() || getSegmentationInput() == null || !inputStep.isActivated() ) { return; }
		if ( segmentationStep.getCurrentId() == 2 )
		{
			if ( labkitThread != null )
				labkitThread.interrupt();
			labkitThread = new Thread( () -> runLabkit() );
			labkitThread.run();
			labkitThread = null;
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

	public synchronized void runDenoising()
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
			saveLabkitInput();
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
		saveLabkitInput();
	}

	private synchronized void saveLabkitInput() {
		//labkit is activated and needs an updated input image
		// upload segmentation input to server
		if(labkitSaveImgThread != null && labkitSaveImgThread.isAlive()) {
			labkitSaveImgThread.interrupt();
		}
		labkitSaveImgThread = new Thread( () -> {
			ServerCommunication serverCommunication = new ServerCommunication();
			context.inject( serverCommunication );
			serverCommunication.uploadLabkitInputToServer( getSegmentationInput(),
					getSegmentationInputStep().getLowerPercentile(),
					getSegmentationInputStep().getUpperPercentile());
		} );
		labkitSaveImgThread.start();
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
				inputStep.setName( TRIBOLIUM_NAME );
			}
			else if ( url == "planaria.tif" )
			{
				inputStep.setName( PLANARIA_NAME );
			}
			if(!getNetworkStep().isActivated()) {
				saveLabkitInput();
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
					inputStep.setName( TRIBOLIUM_NAME );
				}
				else if ( url == "planaria.tif" )
				{
					inputStep.setName( PLANARIA_NAME );
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
		if(!getNetworkStep().isActivated()) {
			saveLabkitInput();
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
			denoisingStep.setName( "CARE optimiert für Tribolium" );
			if ( url != null )
			{
				denoisingStep.setImage( inputs.get( url ).getDenoised( TRIBOLIUM_NET ) );
			}
			denoisingStep.useGaussianFilter( false );
		}
		else if ( id == 1 )
		{
			denoisingStep.setModelUrl( PLANARIA_NET );
			denoisingStep.setName( "CARE optimiert für Schmidtea" );
			if ( url != null )
			{
				denoisingStep.setImage( inputs.get( url ).getDenoised( PLANARIA_NET ) );
			}
			denoisingStep.useGaussianFilter( false );
		}
		else if ( id == 2 )
		{
			denoisingStep.setModelUrl( GAUSS_FILTER );
			denoisingStep.setName( "Gauss-Filter" );
			denoisingStep.useGaussianFilter( true );
			run_gaussFilter();
		}
		denoisingStep.setCurrentId( id );
		if ( denoisingStep.getImg() != null )
		{
			setPercentiles( denoisingStep, denoisingStep.getImg() );
		}
	}

	private void run_gaussFilter()
	{
		if ( url == null )
			return;
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
			segmentationStep.setName( "Manueller Schwellwert" );
		}
		else if ( id == 1 )
		{
			segmentationStep.setName( "Otsu Schwellwert" );
		}
		else if ( id == 2 )
		{
			segmentationStep.setName( "Labkit" );
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
		opService.stats().percentile( lp, iterableInterval, 1.0 );
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

	public synchronized void updated()
	{
		updated = true;
	}
}
