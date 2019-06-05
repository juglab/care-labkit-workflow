package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;

import de.csbdresden.carelabkitworkflow.model.DenoisingStep;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class NetworkPanel< T extends RealType< T > & NativeType< T > > extends AbstractBDVPanel< T >
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final DenoisingStep< T > networkStep;

	public NetworkPanel( final DenoisingStep< T > networkStep )
	{
		this.networkStep = networkStep;
		setBackground( new Color( 197, 49, 255 ) );
	}

	public void update()
	{
		if ( !networkStep.isActivated() )
		{
			showInBdv( null );
		}
		else
		{
			showInBdv( networkStep );
		}
	}

	@Override
	protected void initStep()
	{
		// nothing to do
	}

	public void reset()
	{
		bdv.getBdvHandle().getViewerPanel().removeAllSources();
	}

	@Override
	protected void updateMethodLabel()
	{
		if ( networkStep.isActivated() )
		{
			methodLabel.setText( networkStep.getName() );
		}
		else
		{
			methodLabel.setText( "" );
		}
	}

	@Override
	protected void updateNumberLabel()
	{
		if ( networkStep.isActivated() && networkStep.isGauss() )
		{
			numberLabel.setText( "   Sigma = " + String.valueOf( Math.round( networkStep.getGaussSigma() * 100)/100.0 ) );
		} else {
			numberLabel.setText( "" );
		}
	}
}
