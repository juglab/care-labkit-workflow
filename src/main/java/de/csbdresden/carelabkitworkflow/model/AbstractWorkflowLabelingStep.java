package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.IterableInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractWorkflowLabelingStep< T extends RealType< T > & NativeType< T > > extends AbstractWorkflowStep
{

	private String sourceName;
	
	private float lp;
	
	private float up;

	public abstract IterableInterval< T > getLabeling();
	
	public void setName(final String name) {
		this.sourceName = name;
	}
	
	public String getName() {
		return this.sourceName;
	}

	public void setLowerPercentile(final float lp) {
		this.lp = lp;
	}
	
	public void setUpperPercentile(final float up) {
		this.up = up;
	}
	
	public float getLowerPercentile() {
		return this.lp;
	}

	public float getUpperPercentile() {
		return this.up;
	}
	
}
