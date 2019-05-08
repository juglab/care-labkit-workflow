package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;

import de.csbdresden.carelabkitworkflow.model.NetworkStep;
import net.imglib2.type.numeric.RealType;

public class NetworkPanel< T extends RealType< T > > extends AbstractBDVPanel< T >
{

	private final NetworkStep< T > networkStep;

	NetworkPanel( final NetworkStep< T > networkStep )
	{
		this.networkStep = networkStep;
		setBackground( new Color( 197, 49, 255 ) );
	}

	public void update()
	{
		if ( !networkStep.isActivated() )
		{
			showInBdv( null );
			infoTextPane.setText( EMPTY_INFO_TEXT );
		}
		else
		{
			showInBdv( networkStep );
			infoTextPane.setText( networkStep.getCurrentInfoText() );
		}
	}
	
	@Override
	protected void initStep()
	{
		// nothing to do
	}
}
