package de.csbdresden.carelabkitworkflow.model;

public abstract class AbstractWorkflowStep
{
	private int currentId = -1;

	private boolean activated = false;

	public synchronized boolean isActivated()
	{
		return activated;
	}

	public synchronized void setActivated( final boolean activated )
	{
		this.activated = activated;
	}

	public synchronized int getCurrentId()
	{
		return currentId;
	}

	public synchronized void setCurrentId( final int id )
	{
		currentId = id;
	}

}
