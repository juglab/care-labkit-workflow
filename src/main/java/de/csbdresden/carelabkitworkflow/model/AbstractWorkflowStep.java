package de.csbdresden.carelabkitworkflow.model;

public abstract class AbstractWorkflowStep
{
	private int currentId = -1;

	private boolean activated = false;

	public boolean isActivated()
	{
		return activated;
	}

	public void setActivated( boolean activated )
	{
		this.activated = activated;
	}

	public int getCurrentId()
	{
		return currentId;
	}

	public void setCurrentId( int id )
	{
		currentId = id;
	}

}
