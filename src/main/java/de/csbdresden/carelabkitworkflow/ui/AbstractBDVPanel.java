package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import de.csbdresden.carelabkitworkflow.model.AbstractWorkflowImgStep;
import de.csbdresden.carelabkitworkflow.util.AccumulateProjectorAlphaBlendingARGB;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.miginfocom.swing.MigLayout;

public abstract class AbstractBDVPanel< T extends RealType< T > & NativeType< T > > extends AbstractProgressPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected String EMPTY_INFO_TEXT = "No item selected.";

	protected BdvHandlePanel bdv;

	protected Color bgColor;

	protected BdvStackSource< T > source;

	private JPanel infoPanel;

	protected JTextPane infoTextPane;

	final AccumulateProjectorFactory< ARGBType > myFactory = new AccumulateProjectorFactory< ARGBType >()
	{

		@Override
		public synchronized AccumulateProjectorAlphaBlendingARGB createAccumulateProjector(
				final ArrayList< VolatileProjector > sourceProjectors, final ArrayList< Source< ? > > sources,
				final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
				final RandomAccessibleInterval< ARGBType > targetScreenImages, final int numThreads,
				final ExecutorService executorService )
		{

			return new AccumulateProjectorAlphaBlendingARGB( sourceProjectors, sourceScreenImages, targetScreenImages,
					numThreads, executorService );
		}

	};

	public void init( final WorkflowFrame< T, ? extends IntegerType< ? > > parent, final String title )
	{
		setLayout( new MigLayout( "fillx, insets 10 10 10 10", "[]", "[][]" ) );
		final JLabel titleLabel = new JLabel( title );
		titleLabel.setBorder( BorderFactory.createEmptyBorder( 20, 20, 20, 20 ) );
		add( titleLabel, "dock south" );
		super.initStep();
		InputTriggerConfig config = null;
		try
		{
			config = new InputTriggerConfig( YamlConfigIO.read( this.getClass().getResource( "block_bdv_config.yaml" ).getPath() ) );
		}
		catch ( IllegalArgumentException | IOException e )
		{
			e.printStackTrace();
		}
		bdv = new BdvHandlePanel( parent, Bdv.options().is2D().inputTriggerConfig( config ).preferredSize( 200, 200 ).accumulateProjectorFactory( myFactory ) );
		if ( bgColor != null )
		{
			bdv.getViewerPanel().setBackground( bgColor );
		}
		add( bdv.getViewerPanel(), "push, span, grow, wrap", 0 );
		infoPanel = new JPanel( new MigLayout( "fillx", "[]", "[]" ) );
		infoPanel.setBackground( Color.DARK_GRAY );
		infoPanel.setPreferredSize( new Dimension( 200, 200 ) );
		infoTextPane = new JTextPane();
		infoTextPane.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 24 ) );
		infoTextPane.setBackground( Color.DARK_GRAY );
		infoTextPane.setForeground( Color.white );
		infoTextPane.setText( EMPTY_INFO_TEXT );
		infoPanel.add( infoTextPane );
		add( infoPanel, "grow" );
		initStep();
		revalidate();
	}

	public void showInBdv( final AbstractWorkflowImgStep< T > step )
	{
		bdv.getBdvHandle().getViewerPanel().removeAllSources();
		if ( step != null )
		{

			if ( step.getImg() != null )
			{
				source = BdvFunctions.show( step.getImg(), String.valueOf( step.getCurrentId() ), Bdv.options().addTo( bdv ) );
				source.setDisplayRange( step.getLowerPercentile(), step.getUpperPercentile() );
			}
		}
	}

	protected abstract void initStep();

}
