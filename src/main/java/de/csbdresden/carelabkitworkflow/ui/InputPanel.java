package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;

import de.csbdresden.carelabkitworkflow.model.InputStep;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class InputPanel< T extends RealType< T > & NativeType< T > > extends AbstractBDVPanel< T >
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final InputStep< T > inputStep;

	public InputPanel( final InputStep< T > inputStep )
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
		}
		else
		{
			showInBdv( inputStep );

		}
	}

	@Override
	protected void initStep()
	{
		// nothing to do
	}

	public void reset()
	{
		bdv.getViewerPanel().removeAllSources();
	}

	@Override
	protected void updateMethodLabel()
	{
		if ( inputStep.isActivated() )
		{
			methodLabel.setText( inputStep.getName() );
		}
		else
		{
			methodLabel.setText( "" );
		}
	}

	@Override
	protected void updateNumberLabel()
	{
		// nothing to do
	}

}
