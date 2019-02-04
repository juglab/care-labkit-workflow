package de.csbdresden.carelabkitworkflow.backend;

import de.csbdresden.csbdeep.commands.GenericNetwork;
import net.imagej.ops.OpService;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class CARELabkitWorkflow {

	@Parameter
	private IOService ioService;

	@Parameter
	private OpService opService;

	@Parameter
	private CommandService commandService;

	private Img input;
	private Img denoisedInput;
	private Img segmentedInput;
	private String modelUrl;
	private boolean useLabkit = false; // otherwise threshold
	private boolean doDenoising = false;
	private int output;
	private float threshold = 0.5f;

	public void run() {
		runNetwork();
		runSegmentation();
		calculateOutput();
	}

	public void calculateOutput() {
		if(segmentedInput != null) {
			ImgLabeling cca = opService.labeling().cca(segmentedInput, ConnectedComponents.StructuringElement.FOUR_CONNECTED);
			LabelRegions<IntegerType> regions = new LabelRegions(cca);
			output = regions.getExistingLabels().size();
			System.out.println("Threshold: " + threshold + ", calculated output " + output);
		}
	}

	private void runLabkit() {
		//TODO
	}

	private  <T extends RealType<T>> void runThreshold() {
		if(getSegmentationInput() != null) {
			Pair<T, T> minMax = getMinMax(getSegmentationInput());
			T threshold = minMax.getB().copy();
			threshold.sub(minMax.getA());
			threshold.mul(this.threshold);
			threshold.add(minMax.getA());
			segmentedInput = (Img) opService.threshold().apply(getSegmentationInput(), threshold);
		}
	}

	private Img getSegmentationInput() {
		return doDenoising? denoisedInput : input;
	}

	public void runSegmentation() {
		if(useLabkit) runLabkit();
		else runThreshold();
	}

	public void runNetwork() {
		if(input == null) return;
		try {
			final CommandModule module = commandService.run(
					GenericNetwork.class, false,
					"input", input,
					"modelUrl", modelUrl,
					"nTiles", 10,
					"showProgressDialog", false).get();
			denoisedInput = (Img) module.getOutput("output");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public boolean isUseLabkit() {
		return useLabkit;
	}

	public void setUseLabkit(boolean useLabkit) {
		this.useLabkit = useLabkit;
	}

	public int getOutput() {
		return output;
	}

	public void setInput(int id) {
		String url = "";
		if(id == 0) url = "tribolium.tif";
		if(id == 1) url = "planaria.tif";
		try {
			input = (Img) ioService.open(this.getClass().getResource(url).getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Img getInput() {
		return input;
	}

	public Img getDenoisedInput() {
		return denoisedInput;
	}

	public Img getSegmentedInput() {
		return segmentedInput;
	}

	public void setNetwork(int id) {
		if(id == 0) modelUrl = "http://csbdeep.bioimagecomputing.com/model-tribolium.zip";
		if(id == 1) modelUrl = "http://csbdeep.bioimagecomputing.com/model-planaria.zip";
	}

	public float getThreshold() {
		return threshold;
	}

	public void setThreshold(float threshold) {
		if(threshold >= 0 && threshold <= 1)
			this.threshold = threshold;
	}

	public <T extends RealType<T>> Pair<T, T> getMinMax(Img input) {
		return opService.stats().minMax(input);
	}
}
