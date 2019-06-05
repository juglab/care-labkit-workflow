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

		final String keySigmaValue = "sigmaValue";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_T, 0 ), keySigmaValue + "0");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_A, 0 ), keySigmaValue + "1");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_S, 0 ), keySigmaValue + "2");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_D, 0 ), keySigmaValue + "3");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_H, 0 ), keySigmaValue + "4");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_J, 0 ), keySigmaValue + "5");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_K, 0 ), keySigmaValue + "6");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_L, 0 ), keySigmaValue + "7");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_MINUS, 0 ), keySigmaValue + "8");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_PERIOD, 0 ), keySigmaValue + "9");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_COMMA, 0 ), keySigmaValue + "10");

		actionMap.put( keySigmaValue+"0", new SetSigmaAction( keySigmaValue+"0", 0) );
		actionMap.put( keySigmaValue+"1", new SetSigmaAction( keySigmaValue+"1", 1) );
		actionMap.put( keySigmaValue+"2", new SetSigmaAction( keySigmaValue+"2", 2) );
		actionMap.put( keySigmaValue+"3", new SetSigmaAction( keySigmaValue+"3", 3) );
		actionMap.put( keySigmaValue+"4", new SetSigmaAction( keySigmaValue+"4", 4) );
		actionMap.put( keySigmaValue+"5", new SetSigmaAction( keySigmaValue+"5", 5) );
		actionMap.put( keySigmaValue+"6", new SetSigmaAction( keySigmaValue+"6", 6) );
		actionMap.put( keySigmaValue+"7", new SetSigmaAction( keySigmaValue+"7", 7) );
		actionMap.put( keySigmaValue+"8", new SetSigmaAction( keySigmaValue+"8", 8) );
		actionMap.put( keySigmaValue+"9", new SetSigmaAction( keySigmaValue+"9", 9) );
		actionMap.put( keySigmaValue+"10", new SetSigmaAction( keySigmaValue+"10", 10) );

//		final String keySegmentation1 = "segmentation1";
//		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_Z, 0 ), keySegmentation1 );
//		actionMap.put( keySegmentation1, new ChangeSegmentationAction( keySegmentation1, 0 ) );

		final String keyThresholdDown = "thresholdDown";
		final String keyThresholdUp = "thresholdUp";
		inputMap.put( KeyStroke.getKeyStroke( "released LEFT" ), keyThresholdDown );
		inputMap.put( KeyStroke.getKeyStroke( "released RIGHT" ), keyThresholdUp );
		actionMap.put( keyThresholdDown, new ChangeThresholdAction( keyThresholdDown, -0.05f ) );
		actionMap.put( keyThresholdUp, new ChangeThresholdAction( keyThresholdUp, +0.05f ) );

		final String keyThresholdManual = "manualThreshold";
		final String keyThresholdOtsu = "otsuThreshold";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_Z, 0 ), keyThresholdManual );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_X, 0 ), keyThresholdOtsu );
		actionMap.put( keyThresholdManual, new ChangeSegmentationAction( keyThresholdManual, 0 ) );
		actionMap.put( keyThresholdOtsu, new ChangeSegmentationAction( keyThresholdOtsu, 1 ) );

		final String keyThresholdValue = "thresholdValue";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_1, 0 ), keyThresholdValue + "0");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_2, 0 ), keyThresholdValue + "1");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_3, 0 ), keyThresholdValue + "2");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_4, 0 ), keyThresholdValue + "3");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_5, 0 ), keyThresholdValue + "4");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_6, 0 ), keyThresholdValue + "5");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_7, 0 ), keyThresholdValue + "6");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_8, 0 ), keyThresholdValue + "7");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_9, 0 ), keyThresholdValue + "8");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_0, 0 ), keyThresholdValue + "9");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_M, 0 ), keyThresholdValue + "10");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_N, 0 ), keyThresholdValue + "11");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_B, 0 ), keyThresholdValue + "12");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_V, 0 ), keyThresholdValue + "13");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_C, 0 ), keyThresholdValue + "14");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_P, 0 ), keyThresholdValue + "15");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_O, 0 ), keyThresholdValue + "16");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_I, 0 ), keyThresholdValue + "17");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_U, 0 ), keyThresholdValue + "18");
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_Y, 0 ), keyThresholdValue + "19");

		actionMap.put( keyThresholdValue+"0", new SetThresholdAction( keyThresholdValue+"0", 0) );
		actionMap.put( keyThresholdValue+"1", new SetThresholdAction( keyThresholdValue+"1", 1) );
		actionMap.put( keyThresholdValue+"2", new SetThresholdAction( keyThresholdValue+"2", 2) );
		actionMap.put( keyThresholdValue+"3", new SetThresholdAction( keyThresholdValue+"3", 3) );
		actionMap.put( keyThresholdValue+"4", new SetThresholdAction( keyThresholdValue+"4", 4) );
		actionMap.put( keyThresholdValue+"5", new SetThresholdAction( keyThresholdValue+"5", 5) );
		actionMap.put( keyThresholdValue+"6", new SetThresholdAction( keyThresholdValue+"6", 6) );
		actionMap.put( keyThresholdValue+"7", new SetThresholdAction( keyThresholdValue+"7", 7) );
		actionMap.put( keyThresholdValue+"8", new SetThresholdAction( keyThresholdValue+"8", 8) );
		actionMap.put( keyThresholdValue+"9", new SetThresholdAction( keyThresholdValue+"9", 9) );
		actionMap.put( keyThresholdValue+"10", new SetThresholdAction( keyThresholdValue+"10", 10) );
		actionMap.put( keyThresholdValue+"11", new SetThresholdAction( keyThresholdValue+"11", 11) );
		actionMap.put( keyThresholdValue+"12", new SetThresholdAction( keyThresholdValue+"12", 12) );
		actionMap.put( keyThresholdValue+"13", new SetThresholdAction( keyThresholdValue+"13", 13) );
		actionMap.put( keyThresholdValue+"14", new SetThresholdAction( keyThresholdValue+"14", 14) );
		actionMap.put( keyThresholdValue+"15", new SetThresholdAction( keyThresholdValue+"15", 15) );
		actionMap.put( keyThresholdValue+"16", new SetThresholdAction( keyThresholdValue+"16", 16) );
		actionMap.put( keyThresholdValue+"17", new SetThresholdAction( keyThresholdValue+"17", 17) );
		actionMap.put( keyThresholdValue+"18", new SetThresholdAction( keyThresholdValue+"18", 18) );
		actionMap.put( keyThresholdValue+"19", new SetThresholdAction( keyThresholdValue+"19", 19) );


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
				outputPanel.reset();
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
				inputPanel.startProgress();
				inputPanel.update();
				inputPanel.endProgress();
				networkPanel.startProgress();
				wf.runDenoising();
				networkPanel.update();
				networkPanel.endProgress();
				segmentationPanel.startProgress();
				wf.runSegmentation();
				segmentationPanel.update();
				segmentationPanel.endProgress();
				outputPanel.startProgress();
				wf.calculateOutput();
				outputPanel.update();
				outputPanel.endProgress();
			} ).start();
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
				networkPanel.startProgress();
				wf.runDenoising();
				networkPanel.update();
				networkPanel.endProgress();
				segmentationPanel.startProgress();
				wf.runSegmentation();
				segmentationPanel.update();
				segmentationPanel.endProgress();
				outputPanel.startProgress();
				wf.calculateOutput();
				outputPanel.update();
				outputPanel.endProgress();
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
				segmentationPanel.startProgress();
				wf.runSegmentation();
				segmentationPanel.update();
				segmentationPanel.endProgress();
				outputPanel.startProgress();
				wf.calculateOutput();
				outputPanel.update();
				outputPanel.endProgress();
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
				synchronized ( wf )
				{
					networkPanel.startProgress();
					wf.setGaussSigma( wf.getGaussSigma() + change );
					wf.runDenoising();
					networkPanel.update();
					networkPanel.endProgress();
					segmentationPanel.startProgress();
					wf.runSegmentation();
					segmentationPanel.update();
					segmentationPanel.endProgress();
					outputPanel.startProgress();
					wf.calculateOutput();
					outputPanel.update();
					outputPanel.endProgress();
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
				synchronized ( wf )
				{
					segmentationPanel.startProgress();
					wf.setThreshold( wf.getThreshold() + change );
					wf.runSegmentation();
					segmentationPanel.update();
					segmentationPanel.endProgress();
					outputPanel.startProgress();
					wf.calculateOutput();
					outputPanel.update();
					outputPanel.endProgress();
				}
			} ).start();
		}
	}

	private class SetSigmaAction extends AbstractAction
	{
		
		private final float value;
		
		public SetSigmaAction( final String actionCommand, final float value ) {
			this.value = value/2.f;
			putValue( ACTION_COMMAND_KEY, actionCommand );
		}

		@Override
		public void actionPerformed( final ActionEvent actionEvt )
		{
			System.out.println( actionEvt.getActionCommand() + " pressed " + value);
			new Thread( () ->  {
				networkPanel.startProgress();
				wf.setGaussSigma(value);
				wf.runDenoising();
				networkPanel.update();
				networkPanel.endProgress();
				segmentationPanel.startProgress();
				wf.runSegmentation();
				segmentationPanel.update();
				segmentationPanel.endProgress();
				outputPanel.startProgress();
				wf.calculateOutput();
				outputPanel.update();
				outputPanel.endProgress();
			} ).start();
		}
	}

	private class SetThresholdAction extends AbstractAction
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final float value;

		public SetThresholdAction( String actionCommand, final float value )
		{
			this.value = value/20.f;
			putValue( ACTION_COMMAND_KEY, actionCommand );
		}

		@Override
		public void actionPerformed( ActionEvent actionEvt )
		{
			System.out.println( actionEvt.getActionCommand() + " pressed" );
			new Thread( () -> {
				segmentationPanel.startProgress();
				wf.setThreshold( value );
				wf.runSegmentation();
				segmentationPanel.update();
				segmentationPanel.endProgress();
				outputPanel.startProgress();
				wf.calculateOutput();
				outputPanel.update();
				outputPanel.endProgress();
			} ).start();
		}
	}

	private void close()
	{
		dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
	}

}
