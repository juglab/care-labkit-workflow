package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;

import de.csbdresden.carelabkitworkflow.model.InputStep;

public class InputPanel extends AbstractBDVPanel
{

	private final InputStep inputStep;

	InputPanel( final InputStep inputStep )
	{
		this.inputStep = inputStep;
		bgColor = new Color( 255, 49, 117 );
		setBackground( bgColor );
	}

	public void update()
	{
		if ( !inputStep.isActivated() )
		{
			showInBdv( null );
			infoTextPane.setText( EMPTY_INFO_TEXT );
		}
		else
		{
			showInBdv( inputStep );
			infoTextPane.setText( inputStep.getCurrentInfoText() );
			
		}
	}

	@Override
	protected void initStep()
	{
		// nothing to do
	}
}
