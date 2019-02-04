package de.csbdresden.carelabkitworkflow.backend;

import de.csbdresden.carelabkitworkflow.model.InputStep;
import de.csbdresden.carelabkitworkflow.model.NetworkStep;
import de.csbdresden.carelabkitworkflow.model.OutputStep;
import de.csbdresden.carelabkitworkflow.model.SegmentationStep;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class CARELabkitWorkflow {

	@Parameter
	private IOService ioService;

	@Parameter
	private OpService opService;

	@Parameter
	private CommandService commandService;

	private Map<String, InputCache> inputs = new HashMap<>();
	InputCache input;

	private InputStep inputStep;
	private NetworkStep networkStep;
	private SegmentationStep segmentationStep;
	private OutputStep outputStep;

	public CARELabkitWorkflow() {
		inputStep = new InputStep();
		networkStep = new NetworkStep();
		segmentationStep = new SegmentationStep();
		outputStep = new OutputStep();
		outputStep.setActivated(true);
	}

	public void run() {
		runNetwork();
		runSegmentation();
		calculateOutput();
	}

	public void calculateOutput() {
		Img segmentedInput = segmentationStep.getImage();
		if(!outputStep.isActivated() || segmentedInput == null) {
			outputStep.setResult(-1);
			return;
		};
		ImgLabeling cca = opService.labeling().cca(segmentedInput, ConnectedComponents.StructuringElement.FOUR_CONNECTED);
		LabelRegions<IntegerType> regions = new LabelRegions(cca);
		outputStep.setResult(regions.getExistingLabels().size());
		System.out.println("Threshold: " + segmentationStep.getThreshold() + ", calculated output " + outputStep.getResult());
	}

	private void runLabkit() {
		//TODO
	}

	private  <T extends RealType<T>> void runThreshold() {
		if(getSegmentationInput() != null) {
			Pair<T, T> minMax = getMinMax(getSegmentationInput());
			T threshold = minMax.getB().copy();
			threshold.sub(minMax.getA());
			threshold.mul(segmentationStep.getThreshold());
			threshold.add(minMax.getA());
			segmentationStep.setImage((Img) opService.threshold().apply(getSegmentationInput(), threshold));
		}
	}

	private Img getSegmentationInput() {
		return networkStep.isActivated()? networkStep.getImage() : inputStep.getImage();
	}

	public void runSegmentation() {
		if(!segmentationStep.isActivated()
			|| getSegmentationInput() == null
			|| !inputStep.isActivated()) {
			segmentationStep.setImage(null);
			return;
		}
		if(segmentationStep.isUseLabkit()) runLabkit();
		else runThreshold();
	}

	public void runNetwork() {
		if(!networkStep.isActivated()
				|| inputStep.getImage() == null
				|| !inputStep.isActivated()) {
			networkStep.setImage(null);
			return;
		}
		if(networkStep.getModelUrl() == null) return;
		Img denoisedImg = input.getDenoised(networkStep.getModelUrl());
		if(denoisedImg != null) {
			networkStep.setImage(denoisedImg);
			return;
		}
		try {
			final CommandModule module = commandService.run(
					GenericNetwork.class, false,
					"input", getInput(),
					"modelUrl", networkStep.getModelUrl(),
					"nTiles", 10,
					"showProgressDialog", false).get();
			networkStep.setImage((Img) module.getOutput("output"));
			input.setDenoised(networkStep.getModelUrl(), networkStep.getImage());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	public int getOutput() {
		return outputStep.getResult();
	}

	public void setInput(int id) {
		String url = "";
		if(id == 0) url = "tribolium.tif";
		if(id == 1) url = "planaria.tif";
		inputStep.setCurrentId(id);
		if(inputs.containsKey(url)) {
			input = inputs.get(url);
			inputStep.setImage(input.getInput());
			return;
		}
		try {
			Img inputimg = (Img) ioService.open(this.getClass().getResource(url).getPath());
			if(inputimg != null) {
				input =  new InputCache(inputimg);
				inputs.put(url, input);
				inputStep.setImage(input.getInput());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Img getInput() {
		return inputStep.getImage();
	}

	public Img getDenoisedInput() {
		return networkStep.getImage();
	}

	public Img getSegmentedInput() {
		return segmentationStep.getImage();
	}

	public void setNetwork(int id) {
		if(id == 0) networkStep.setModelUrl("http://csbdeep.bioimagecomputing.com/model-tribolium.zip");
		if(id == 1) networkStep.setModelUrl("http://csbdeep.bioimagecomputing.com/model-planaria.zip");
		networkStep.setCurrentId(id);
	}

	public void setSegmentation(int id) {
		segmentationStep.setUseLabkit(id == 1);
		segmentationStep.setCurrentId(id);
	}

	public float getThreshold() {
		return segmentationStep.getThreshold();
	}

	public void setThreshold(float threshold) {
		if(threshold >= 0 && threshold <= 1)
			segmentationStep.setThreshold(threshold);
	}

	public <T extends RealType<T>> Pair<T, T> getMinMax(Img input) {
		return opService.stats().minMax(input);
	}

	public InputStep getInputStep() {
		return inputStep;
	}

	public NetworkStep getNetworkStep() {
		return networkStep;
	}

	public SegmentationStep getSegmentationStep() {
		return segmentationStep;
	}

	public OutputStep getOutputStep() {
		return outputStep;
	}
}
