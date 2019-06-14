package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import javax.swing.*;

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
import de.csbdresden.carelabkitworkflow.model.ForwardingRandomAccessibleInterval;
import de.csbdresden.carelabkitworkflow.util.AccumulateProjectorAlphaBlendingARGB;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
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
	
	protected JLabel methodLabel;
	
	protected JLabel numberLabel;
	
	protected ForwardingRandomAccessibleInterval<T> proxySource;

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
//		runOnEventDispatchThread(() -> {
			setLayout(new MigLayout("insets 10 10 10 10", "[center]", "[][][]"));
			final JLabel titleLabel = new JLabel(title);
			titleLabel.setFont(new Font("Ubuntu", Font.BOLD, 52));
			titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 2, 20, 2));
			add(titleLabel, "wrap");
			super.initStep();
			InputTriggerConfig config = null;
			try {
				config = new InputTriggerConfig(YamlConfigIO.read(this.getClass().getResource("block_bdv_config.yaml").getPath()));
			} catch (IllegalArgumentException | IOException e) {
				e.printStackTrace();
			}
			bdv = new BdvHandlePanel(parent, Bdv.options().is2D().inputTriggerConfig(config).preferredSize(200, 200).accumulateProjectorFactory(myFactory));
			if (bgColor != null) {
				bdv.getViewerPanel().setBackground(bgColor);
			}
			bdv.getViewerPanel().setMinimumSize(new Dimension(20, 20));
			add(bdv.getViewerPanel(), "push, span, grow, wrap");
			infoPanel = new JPanel(new MigLayout("fillx", "[]", "[]"));
			infoPanel.setBackground(Color.DARK_GRAY);
			infoPanel.setPreferredSize(new Dimension(200, 200));
			methodLabel = new JLabel("");
			methodLabel.setFont(new Font("Ubuntu", Font.BOLD, 48));
			methodLabel.setForeground(Color.WHITE);
			numberLabel = new JLabel("");
			numberLabel.setFont(new Font("Ubuntu", Font.BOLD, 32));
			numberLabel.setForeground(Color.WHITE);
			infoPanel.add(methodLabel, "wrap");
			infoPanel.add(numberLabel, "wrap");
			add(infoPanel, "grow");
			initStep();
			revalidate();
//		});
	}

	public void showInBdv( final AbstractWorkflowImgStep< T > step )
	{
		if ( step != null )
		{

			if ( step.getImg() != null )
			{
				if ( proxySource == null ) {
					setProxySource( ( RandomAccessibleInterval< T > ) step.getImg() );
					source = BdvFunctions.show( proxySource, "", Bdv.options().addTo( bdv ) );
					source.setDisplayRange( step.getLowerPercentile(), step.getUpperPercentile() );
				} else {
					setProxySource( ( RandomAccessibleInterval< T > ) step.getImg() );
					source.setDisplayRange( step.getLowerPercentile(), step.getUpperPercentile() );
					bdv.getViewerPanel().requestRepaint();
				}
			} else {
				bdv.getViewerPanel().removeAllSources();
				proxySource = null;
			}
		} else {
			bdv.getViewerPanel().removeAllSources();
			proxySource = null;
		}
	}
	
	protected void setProxySource(final RandomAccessibleInterval< T > source) {
		if (proxySource == null ) {
			proxySource = new ForwardingRandomAccessibleInterval<>( source );
		} else {
			proxySource.setSource( source );
		}
	}

	protected abstract void updateMethodLabel();
	
	protected abstract void updateNumberLabel();
	
	protected abstract void initStep();

}
