package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import de.csbdresden.carelabkitworkflow.model.OutputStep;
import net.miginfocom.swing.MigLayout;

public class ResultPanel extends AbstractProgressPanel
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final OutputStep outputStep;

	private JLabel result;

	private final Font font = new Font( Font.MONOSPACED, Font.PLAIN, 123 );

	public ResultPanel( final OutputStep outputStep )
	{
		this.outputStep = outputStep;
		setBackground( new Color( 255, 246, 49 ) );
		setLayout( new MigLayout( "fill" ) );
	}

	public void init( final String title )
	{
		result = new JLabel();
		result.setFont( font );
		add( result, "pos 0.5al 0.5al" );
		JLabel titleLabel = new JLabel( title );
		titleLabel.setBorder( BorderFactory.createEmptyBorder( 20, 20, 20, 20 ) );
		add( titleLabel, "dock south" );
		super.initStep();
	}

	public void update()
	{
		showOutput( outputStep.getResult() );
	}

	private void showOutput( final int output )
	{
		result.setText( output >= 0 ? String.valueOf( output ) : "?" );
	}

	public void reset()
	{
		result.setText( "" );
	}

}
