package de.csbdresden.carelabkitworkflow.model;

public class OutputStep extends AbstractWorkflowStep
{
	private double result = -1;

	public double getResult()
	{
		return result;
	}

	public void setResult( final double result )
	{
		this.result = result;
	}
}
