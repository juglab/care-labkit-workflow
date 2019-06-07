package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.netlib.util.floatW;

import de.csbdresden.carelabkitworkflow.backend.CARELabkitWorkflow;
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

	private List< SegStats > stats = new ArrayList<>();

	private final Font font = new Font( Font.MONOSPACED, Font.PLAIN, 123 );

	private JLabel nameLabel;

	private int stats_idx;

	private JLabel statLabel;

	private boolean active;

	public ResultPanel( final OutputStep outputStep )
	{
		this.outputStep = outputStep;
		setBackground( new Color( 90, 255, 126 ) );
		stats.add( new SegStats() ); // Tribolium
		stats.add( new SegStats() ); // Planaria
	}

	public void init( final String title )
	{
		setLayout( new MigLayout( "fillx, insets 10 10 10 10", "[center]", "[][][]" ) );
		final JLabel titleLabel = new JLabel( title );
		titleLabel.setFont( new Font( "Ubuntu", Font.BOLD, 52 ) );
		titleLabel.setBorder( BorderFactory.createEmptyBorder( 20, 20, 20, 20 ) );
		add( titleLabel, "wrap" );
		result = new JLabel();
		result.setFont( font );
		add( result, "pos 0.5al 0.5al, wrap" );

		statLabel = new JLabel( "SEG-Score Statistik" );
		statLabel.setFont( new Font( "Ubuntu", Font.BOLD, 48 ) );
		add( statLabel, "pos 0.5al 0.70al" );

		nameLabel = new JLabel( "" );
		nameLabel.setFont( new Font( "Ubuntu", Font.BOLD, 44 ) );
		add( nameLabel, "pos 0.5al 0.75al" );

		super.initStep();
	}

	public void update()
	{
		showOutput( outputStep.getResult() );
		nameLabel.setText( outputStep.getInputName() );
		if ( outputStep.getInputName() == CARELabkitWorkflow.TRIBOLIUM_NAME )
		{
			active = true;
			stats_idx = 0;
		}
		else if ( outputStep.getInputName() == CARELabkitWorkflow.PLANARIA_NAME )
		{
			active = true;
			stats_idx = 1;
		}
		else
		{
			active = false;
			stats_idx = -1;
		}
		stats.get( stats_idx ).update( outputStep.getResult() );
	}

	private void showOutput( final double output )
	{
		result.setText( output >= 0 ? String.valueOf( Math.round( output * 10000 ) / 100.0 ) + "%" : "?" );
		statLabel.setVisible( true );
		nameLabel.setVisible( true );
		repaint();
	}

	@Override
	protected void paintComponent( Graphics g )
	{
		// TODO Auto-generated method stub
		super.paintComponent( g );
		doDrawing( g );
	}

	private void doDrawing( Graphics g )
	{

		Graphics2D g2d = ( Graphics2D ) g;

		g2d.setPaint( Color.darkGray );

		int w = getWidth();
		int h = getHeight();

		int increment = ( int ) ( ( w - 20 ) / 101.0 );
		int spacer = ( int ) ( w - 101 * increment ) / 2;

		int current = active ? stats.get( stats_idx ).getCurrent() : -1;
		long totalCount = stats.get( stats_idx ).getTotalCount();
		long maxCount = stats.get( stats_idx ).getMaxCount();

		for ( int i = 0; i < 101; i++ )
		{
			if ( i == current )
			{
				g2d.setPaint( Color.blue );
			}
			else
			{
				g2d.setPaint( Color.darkGray );
			}
			if ( totalCount > 0 && active )
			{
				int bh = ( int ) ( stats.get( stats_idx ).getStatsFor( i ) * ( 0.2 * ( float ) h / ( float ) maxCount ) );
				g2d.fillRect( spacer + i * increment, h - bh - 20, increment, bh );
			}
			g2d.setPaint( Color.darkGray );
			g2d.fillRect( spacer + i * increment, h - 19, increment, 9 );
			g2d.setPaint( Color.lightGray );
			g2d.fillRect( spacer + i * increment, h - 20, increment, 1 );
		}
	}

	public void reset()
	{
		result.setText( "" );
		active = false;
		statLabel.setVisible( false );
		nameLabel.setVisible( false );
		repaint();
	}

}
