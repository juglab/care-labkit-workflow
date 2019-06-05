package de.csbdresden.carelabkitworkflow.model;

import javax.swing.JTextPane;

import net.imglib2.IterableInterval;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;

public class SegmentationStep< T extends RealType< T > & NativeType< T >, I extends IntegerType< I >> extends AbstractWorkflowLabelingStep
{

	private IterableInterval< BitType > thresholded;
	
	private ImgLabeling< String, I > segmentation;

	private float threshold = 0.5f;

	private boolean useLabkit = false; // otherwise threshold

	private boolean manual;

	public ImgLabeling< String, I > getLabeling()
	{
		return segmentation;
	}
	
	public void setThresholdedImg( final IterableInterval< BitType > thresholded  ) {
		this.thresholded = thresholded;
	}

	public IterableInterval< BitType > getThresholdedImg() {
		return this.thresholded;
	}
	
	public void setLabeling( final ImgLabeling< String, I > seg )
	{
		this.segmentation = seg;
	}

	public float getThreshold()
	{
		return threshold;
	}

	public synchronized void setThreshold( final float threshold )
	{
		this.threshold = threshold;
	}

	public boolean isUseLabkit()
	{
		return useLabkit;
	}

	public void setUseLabkit( final boolean useLabkit )
	{
		this.useLabkit = useLabkit;
	}

	public boolean useManual()
	{
		return this.manual;
	}
	
	public void setManual(final boolean manual) {
		this.manual = manual;
	}
}
