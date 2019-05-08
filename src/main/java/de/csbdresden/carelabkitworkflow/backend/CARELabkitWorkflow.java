package de.csbdresden.carelabkitworkflow.backend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;

import de.csbdresden.carelabkitworkflow.model.InputStep;
import de.csbdresden.carelabkitworkflow.model.NetworkStep;
import de.csbdresden.carelabkitworkflow.model.OutputStep;
import de.csbdresden.carelabkitworkflow.model.SegmentationStep;
import de.csbdresden.csbdeep.commands.GenericNetwork;
import net.imagej.ops.OpService;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class CARELabkitWorkflow
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

	private Map< String, InputCache > inputs = new HashMap<>();

	InputCache input;

	private InputStep inputStep;

	private NetworkStep networkStep;

	private SegmentationStep segmentationStep;

	private OutputStep outputStep;

	private boolean loadChachedCARE;

	public CARELabkitWorkflow( final boolean loadChachedCARE )
	{
		this.loadChachedCARE = loadChachedCARE;
		inputStep = new InputStep();
		networkStep = new NetworkStep();
		segmentationStep = new SegmentationStep();
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
		Img segmentedInput = segmentationStep.getImg();
		if ( !outputStep.isActivated() || segmentedInput == null )
		{
			outputStep.setResult( -1 );
			return;
		} ;
		ImgLabeling cca = opService.labeling().cca( segmentedInput,
				ConnectedComponents.StructuringElement.FOUR_CONNECTED );
		LabelRegions< IntegerType > regions = new LabelRegions( cca );
		outputStep.setResult( regions.getExistingLabels().size() );
		System.out.println(
				"Threshold: " + segmentationStep.getThreshold() + ", calculated output " + outputStep.getResult() );
	}

	private void runLabkit()
	{
		// TODO
	}

	private < T extends RealType< T >, I extends IntegerType<I> > void runThreshold()
	{
		if ( getSegmentationInput() != null )
		{
			Pair< T, T > minMax = getMinMax( getSegmentationInput() );
			T threshold = minMax.getB().copy();
			threshold.sub( minMax.getA() );
			threshold.mul( segmentationStep.getThreshold() );
			threshold.add( minMax.getA() );
			segmentationStep.setImage( ( Img ) opService.threshold().apply( getSegmentationInput(), threshold ) );
			segmentationStep.setSegmentation( (ImgLabeling< String, I >)opService.labeling().cca( segmentationStep.getImg(), StructuringElement.FOUR_CONNECTED ) );
		}
	}

	private Img getSegmentationInput()
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
			runLabkit();
		else
			runThreshold();
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
		Img denoisedImg = input.getDenoised( networkStep.getModelUrl() );
		if ( denoisedImg != null )
		{
			networkStep.setImage( denoisedImg );
			return;
		}
		try
		{
			final CommandModule module = commandService.run( GenericNetwork.class, false, "input", getInput(),
					"modelUrl", networkStep.getModelUrl(), "nTiles", 10, "showProgressDialog", false ).get();
			networkStep.setImage( ( Img ) module.getOutput( "output" ) );
			input.setDenoised( networkStep.getModelUrl(), networkStep.getImg() );
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

	public void setInput( final int id )
	{
		String url = "";
		if ( id == 0 )
			url = "tribolium.tif";
		if ( id == 1 )
			url = "planaria.tif";
		inputStep.setCurrentId( id );
		if ( inputs.containsKey( url ) )
		{
			input = inputs.get( url );
			inputStep.setImage( input.getInput() );
			if ( url == "tribolium.tif" )
			{
				inputStep.setInfo( "Tribolium information text. Image acquired with microscope xyz, exposure, staining... Some interesting fact is... maybe you want to know" );
			}
			else if ( url == "planaria.tif" )
			{
				inputStep.setInfo( "Planaria information text. Image acquired with balbla blup..." );
			}
			return;
		}
		try
		{
			Img inputimg = ( Img ) ioService.open( this.getClass().getResource( url ).getPath() );
			if ( inputimg != null )
			{
				input = new InputCache( inputimg );
				inputs.put( url, input );
				inputStep.setImage( input.getInput() );
				if ( url == "tribolium.tif" )
				{
					inputStep.setInfo( "Tribolium information text. Image acquired with microscope xyz, exposure, staining..." );
				}
				else if ( url == "planaria.tif" )
				{
					inputStep.setInfo( "Planaria information text. Image acquired with balbla blup..." );
				}
				if ( loadChachedCARE )
				{
					input.setDenoised( TRIBOLIUM_NET, ( Img ) ioService.open( this.getClass()
							.getResource( url.substring( 0, url.length() - 4 ) + "_triboliumNet.tif" ).getPath() ) );
					input.setDenoised( PLANARIA_NET, ( Img ) ioService.open( this.getClass()
							.getResource( url.substring( 0, url.length() - 4 ) + "_planariaNet.tif" ).getPath() ) );
				}
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public Img getInput()
	{
		return inputStep.getImg();
	}

	public Img getDenoisedInput()
	{
		return networkStep.getImg();
	}

	public Img getSegmentedInput()
	{
		return segmentationStep.getImg();
	}

	public void setNetwork( int id )
	{
		if ( id == 0 )
		{
			networkStep.setModelUrl( TRIBOLIUM_NET );
			networkStep.setInfo( TRIBOLIUM_NET_INFO );
		}
		if ( id == 1 )
		{
			networkStep.setModelUrl( PLANARIA_NET );
			networkStep.setInfo( PLANARIA_NET_INFO );
		}
		networkStep.setCurrentId( id );
	}

	public void setSegmentation( int id )
	{
		segmentationStep.setUseLabkit( id == 1 );
		segmentationStep.setCurrentId( id );
		segmentationStep.setInfo( SEGMENTATION_INFO );
	}

	public float getThreshold()
	{
		return segmentationStep.getThreshold();
	}

	public void setThreshold( float threshold )
	{
		if ( threshold >= 0 && threshold <= 1 )
			segmentationStep.setThreshold( threshold );
	}

	public < T extends RealType< T > > Pair< T, T > getMinMax( final Img< T > input )
	{
		return opService.stats().minMax( input );
	}

	public < T extends RealType< T > > Pair< T, T > getLowerUpperPerc( final Img< T > input )
	{
		return new ValuePair<>( opService.stats().percentile( input, 3.0 ), opService.stats().percentile( input, 99.0 ) );
	}

	public InputStep getInputStep()
	{
		return inputStep;
	}

	public NetworkStep getNetworkStep()
	{
		return networkStep;
	}

	public SegmentationStep getSegmentationStep()
	{
		return segmentationStep;
	}

	public OutputStep getOutputStep()
	{
		return outputStep;
	}
}
