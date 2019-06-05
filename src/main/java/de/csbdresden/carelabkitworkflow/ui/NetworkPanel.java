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

	public void reset()
	{
		bdv.getBdvHandle().getViewerPanel().removeAllSources();
	}
}
