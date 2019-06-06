package de.csbdresden.carelabkitworkflow.model;

public class OutputStep extends AbstractWorkflowStep
{
	private double result = -1;
	
	private String name;

	public double getResult()
	{
		return result;
	}

	public void setResult( final double result )
	{
		this.result = result;
	}
	
	public void setInputName(final String name) {
		this.name = name;
	}
	
	public String getInputName() {
		return this.name;
	}
}
