package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.python.modules.synchronize;

import de.csbdresden.carelabkitworkflow.backend.CARELabkitWorkflow;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.miginfocom.swing.MigLayout;

public class WorkflowFrame< T extends RealType< T > & NativeType< T >, I extends IntegerType< I > > extends JFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final CARELabkitWorkflow< T, I > wf;

	private InputPanel< T > inputPanel;

	private NetworkPanel< T > networkPanel;

	private SegmentationPanel< T, I > segmentationPanel;

	private ResultPanel outputPanel;

	private JPanel workflows;

	private boolean fullScreen = false;

	static GraphicsDevice device = GraphicsEnvironment
			.getLocalGraphicsEnvironment().getScreenDevices()[ 0 ];

	public WorkflowFrame( final CARELabkitWorkflow< T, I > wf )
	{
		super( "Bio-Image Analysis Workflow" );
		this.wf = wf;
		createWorkflowPanels();
		setKeyBindings();
	}

	private void createWorkflowPanels()
	{

		workflows = new JPanel();
		workflows.setBackground( Color.DARK_GRAY );
		workflows.setLayout( new MigLayout( "fill, gap 0, ins 20 0 20 0", "push[]push[]push[]push[]push" ) );

		inputPanel = new InputPanel<>( wf.getInputStep() );
		networkPanel = new NetworkPanel< T >( wf.getNetworkStep() );
		segmentationPanel = new SegmentationPanel< T, I >( wf.getSegmentationStep(), wf.getInputStep(), wf.getNetworkStep() );
		outputPanel = new ResultPanel( wf.getOutputStep() );

		final String w = "(25%-25px)";
		workflows.add( inputPanel, "grow, width " + w + ":" + w + ":" + w );
		workflows.add( networkPanel, "grow, width " + w + ":" + w + ":" + w );
		workflows.add( segmentationPanel, "grow, width " + w + ":" + w + ":" + w );
		workflows.add( outputPanel, "grow, width " + w + ":" + w + ":" + w );

		this.setContentPane( workflows );
		inputPanel.init( this, "INPUT [q,w]" );
		networkPanel.init( this, "DENOISE [e,r]" );
		segmentationPanel.init( this, "SEGMENTATION [z, ← →]" );
		outputPanel.init( "RESULT" );
	}

	private void setKeyBindings()
	{
		final ActionMap actionMap = workflows.getActionMap();
		int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
		final InputMap inputMap = workflows.getInputMap( condition );

		final String keyInput1 = "input1";
		final String keyInput2 = "input2";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_Q, 0 ), keyInput1 );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_W, 0 ), keyInput2 );
		actionMap.put( keyInput1, new ChangeInputAction( keyInput1, 0 ) );
		actionMap.put( keyInput2, new ChangeInputAction( keyInput2, 1 ) );

		final String keyNetwork1 = "network1";
		final String keyNetwork2 = "network2";
		final String keyGaussFilter = "gaussFilter";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_E, 0 ), keyNetwork1 );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_R, 0 ), keyNetwork2 );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_G, 0 ), keyGaussFilter );
		actionMap.put( keyNetwork1, new ChangeNetworkAction( keyNetwork1, 0 ) );
		actionMap.put( keyNetwork2, new ChangeNetworkAction( keyNetwork2, 1 ) );
		actionMap.put( keyGaussFilter, new ChangeNetworkAction( keyGaussFilter, 2 ) );

		final String keySigmaDown = "sigmaDown";
		final String keySigmaUp = "sigmaUp";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_N, 0 ), keySigmaDown );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_M, 0 ), keySigmaUp );
		actionMap.put( keySigmaDown, new ChangeSigmaAction( keySigmaDown, -1.0f ) );
		actionMap.put( keySigmaUp, new ChangeSigmaAction( keySigmaUp, 1.0f ) );

//		final String keySegmentation1 = "segmentation1";
//		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_Z, 0 ), keySegmentation1 );
//		actionMap.put( keySegmentation1, new ChangeSegmentationAction( keySegmentation1, 0 ) );

		final String keyThresholdManual = "manualThreshold";
		final String keyThresholdOtsu = "otsuThreshold";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_Z, 0 ), keyThresholdManual );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_X, 0 ), keyThresholdOtsu );
		actionMap.put( keyThresholdManual, new ChangeSegmentationAction( keyThresholdManual, 0 ) );
		actionMap.put( keyThresholdOtsu, new ChangeSegmentationAction( keyThresholdOtsu, 1 ) );

		final String keyThresholdDown = "thresholdDown";
		final String keyThresholdUp = "thresholdUp";
		inputMap.put( KeyStroke.getKeyStroke( "released LEFT" ), keyThresholdDown );
		inputMap.put( KeyStroke.getKeyStroke( "released RIGHT" ), keyThresholdUp );
		actionMap.put( keyThresholdDown, new ChangeThresholdAction( keyThresholdDown, -0.05f ) );
		actionMap.put( keyThresholdUp, new ChangeThresholdAction( keyThresholdUp, +0.05f ) );

		final String keyFullScreen = "fullscreen";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F, 0 ), keyFullScreen );
		actionMap.put( keyFullScreen, new FullScreenAction( keyFullScreen ) );
	}

	private class FullScreenAction extends AbstractAction
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public FullScreenAction( String actionCommand )
		{
			putValue( ACTION_COMMAND_KEY, actionCommand );
		}

		@Override
		public void actionPerformed( ActionEvent actionEvt )
		{
			System.out.println( actionEvt.getActionCommand() + " pressed" );
			toggleFullScreen();
		}
	}

	private void toggleFullScreen()
	{
		fullScreen = !fullScreen;
		if ( fullScreen )
		{
			device.setFullScreenWindow( this );
		}
		else
		{
			device.setFullScreenWindow( null );
		}
		setVisible( true );
	}

	private class ChangeInputAction extends AbstractAction
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final int id;

		public ChangeInputAction( String actionCommand, final int id )
		{
			this.id = id;
			putValue( ACTION_COMMAND_KEY, actionCommand );
		}

		@Override
		public void actionPerformed( ActionEvent actionEvt )
		{
			System.out.println( actionEvt.getActionCommand() + " pressed" );
			new Thread( () -> {
				if ( wf.getInputStep().getCurrentId() == id &&
						wf.getInputStep().isActivated() )
				{
					wf.getInputStep().setActivated( false );
				}
				else
				{
					wf.getInputStep().setActivated( true );
					wf.setInput( id );
					if ( wf.getNetworkStep().isActivated() )
					{
						wf.setDenoisingMethod( wf.getNetworkStep().getCurrentId() );
					}
				}
				wf.requestUpdate();
				updateOnInputChange();
			} ).start();
		}
	}

	private synchronized void updateOnThresholdChange()
	{
		if ( wf.needsUpdate() )
		{
			int segmentationID = -1;
			float ts = -1;
			outputPanel.reset();
			segmentationPanel.reset();
			while ( segmentationID != wf.getSegmentationStep().getCurrentId() && ts != wf.getThreshold() )
			{
				segmentationID = wf.getSegmentationStep().getCurrentId();
				ts = wf.getThreshold();

				wf.runSegmentation();
				final boolean correctThreshold = wf.getSegmentationStep().getCurrentId() == 0 ? ts == wf.getThreshold() : true;
				if ( segmentationID == wf.getSegmentationStep().getCurrentId() && correctThreshold )
				{
					segmentationPanel.update();
					wf.calculateOutput();
					if ( wf.getSegmentationStep().isActivated() && wf.getInputStep().isActivated() )
					{
						outputPanel.update();
					}
					else
					{
						outputPanel.reset();
					}

				}
			}

			wf.updated();
		}
	}

	private synchronized void updateOnDenoiseChange()
	{
		if ( wf.needsUpdate() )
		{
			int networkID = -1;
			float sigma = -1;
			outputPanel.reset();
			segmentationPanel.reset();
			networkPanel.reset();
			while ( networkID != wf.getNetworkStep().getCurrentId() && sigma != wf.getGaussSigma() )
			{
				networkID = wf.getNetworkStep().getCurrentId();
				sigma = wf.getGaussSigma();
				wf.runDenoising();
				if ( networkID == wf.getNetworkStep().getCurrentId() && sigma == wf.getGaussSigma() )
				{
					networkPanel.update();
					updateOnThresholdChange();
				}
			}
		}

		wf.updated();
	}

	private synchronized void updateOnInputChange()
	{
		if ( wf.needsUpdate() )
		{

			int inputID = -1;
			outputPanel.reset();
			segmentationPanel.reset();
			networkPanel.reset();
			inputPanel.reset();
			while ( inputID != wf.getInputStep().getCurrentId() )
			{
				inputID = wf.getInputStep().getCurrentId();
				if ( inputID == wf.getInputStep().getCurrentId() )
				{
					inputPanel.update();
					updateOnDenoiseChange();
				}
				else
				{
					inputID = wf.getInputStep().getCurrentId();
				}
			}

			wf.updated();
		}
	}

	private class ChangeNetworkAction extends AbstractAction
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final int id;

		public ChangeNetworkAction( String actionCommand, final int id )
		{
			this.id = id;
			putValue( ACTION_COMMAND_KEY, actionCommand );
		}

		@Override
		public void actionPerformed( ActionEvent actionEvt )
		{
			System.out.println( actionEvt.getActionCommand() + " pressed" );
			new Thread( () -> {
				outputPanel.reset();
				if ( wf.getNetworkStep().getCurrentId() == id &&
						wf.getNetworkStep().isActivated() )
				{
					wf.getNetworkStep().setActivated( false );
				}
				else
				{
					wf.getNetworkStep().setActivated( true );
					wf.setDenoisingMethod( id );
				}
				wf.requestUpdate();
				updateOnDenoiseChange();
			} ).start();
		}
	}

	private class ChangeSegmentationAction extends AbstractAction
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final int id;

		public ChangeSegmentationAction( String actionCommand, final int id )
		{
			this.id = id;
			putValue( ACTION_COMMAND_KEY, actionCommand );
		}

		@Override
		public void actionPerformed( ActionEvent actionEvt )
		{
			System.out.println( actionEvt.getActionCommand() + " pressed" );
			new Thread( () -> {
				outputPanel.reset();
				if ( wf.getSegmentationStep().getCurrentId() == id &&
						wf.getSegmentationStep().isActivated() )
				{
					wf.getSegmentationStep().setActivated( false );
				}
				else
				{
					wf.getSegmentationStep().setActivated( true );
					wf.setSegmentation( id );
				}
				wf.requestUpdate();
				updateOnThresholdChange();
			} ).start();
		}
	}

	private class ChangeSigmaAction extends AbstractAction
	{

		private final float change;

		public ChangeSigmaAction( final String actionCommand, final float change )
		{
			this.change = change;
			putValue( ACTION_COMMAND_KEY, actionCommand );
		}

		@Override
		public void actionPerformed( final ActionEvent actionEvt )
		{
			System.out.println( actionEvt.getActionCommand() + " pressed " + change );
			new Thread( () -> {
				wf.setGaussSigma( wf.getGaussSigma() + change );
				if ( wf.getNetworkStep().isGauss() )
				{
					wf.requestUpdate();
					updateOnDenoiseChange();
				}
			} ).start();
		}
	}

	private class ChangeThresholdAction extends AbstractAction
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final float change;

		public ChangeThresholdAction( String actionCommand, final float change )
		{
			this.change = change;
			putValue( ACTION_COMMAND_KEY, actionCommand );
		}

		@Override
		public void actionPerformed( ActionEvent actionEvt )
		{
			System.out.println( actionEvt.getActionCommand() + " pressed" );
			new Thread( () -> {
				wf.setThreshold( wf.getThreshold() + change );
				wf.requestUpdate();
				updateOnThresholdChange();
			} ).start();
		}
	}

	private void close()
	{
		dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
	}

}
