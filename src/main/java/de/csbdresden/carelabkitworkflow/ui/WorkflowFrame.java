package de.csbdresden.carelabkitworkflow.ui;

import de.csbdresden.carelabkitworkflow.backend.CARELabkitWorkflow;
import de.csbdresden.carelabkitworkflow.util.AveragedValue;
import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

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

	private SerialPort sp1, sp2;

	static GraphicsDevice device = GraphicsEnvironment
			.getLocalGraphicsEnvironment().getScreenDevices()[ 0 ];

	AveragedValue< FloatType > thresholdSliderVal = new AveragedValue<>();

	AveragedValue< FloatType > sigmaSliderVal = new AveragedValue<>();

	public WorkflowFrame( final CARELabkitWorkflow< T, I > wf, String port1, final String port2 )
	{
		super( "Bio-Image Analysis Workflow" );
		this.wf = wf;
		createWorkflowPanels();
		setKeyBindings();
		initSerialPort( port1, port2 );
	}

	private void createWorkflowPanels()
	{

		workflows = new JPanel();
		workflows.setBackground( Color.DARK_GRAY );
		workflows.setLayout( new MigLayout( "fill, gap 0, ins 20 0 20 0", "push[]push[]push[]push[]push" ) );

		inputPanel = new InputPanel<>( wf.getInputStep() );
		networkPanel = new NetworkPanel< T >( wf.getNetworkStep(), wf.getInputStep() );
		segmentationPanel = new SegmentationPanel< T, I >( wf.getSegmentationStep(), wf.getInputStep(), wf.getNetworkStep() );
		outputPanel = new ResultPanel( wf.getOutputStep() );

		final String w = "(25%-25px)";
		workflows.add( inputPanel, "grow, width " + w + ":" + w + ":" + w );
		workflows.add( networkPanel, "grow, width " + w + ":" + w + ":" + w );
		workflows.add( segmentationPanel, "grow, width " + w + ":" + w + ":" + w );
		workflows.add( outputPanel, "grow, width " + w + ":" + w + ":" + w );

		this.setContentPane( workflows );
		inputPanel.init( this, "Eingabe" );
		networkPanel.init( this, "Entrauschen" );
		segmentationPanel.init( this, "Segmentierung" );
		outputPanel.init( "Validierung" );
	}

	private void initSerialPort( String port1, final String port2 )
	{
		sp1 = setupSerialPort( port1 );
		sp2 = setupSerialPort( port2 );

	}

	private SerialPort setupSerialPort( String port )
	{
		SerialPort serialPort = new SerialPort( port );
		try
		{
			serialPort.openPort();

			serialPort.setParams( SerialPort.BAUDRATE_9600,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE );

			serialPort.setFlowControlMode( SerialPort.FLOWCONTROL_RTSCTS_IN |
					SerialPort.FLOWCONTROL_RTSCTS_OUT );

			serialPort.addEventListener( new PortReader( serialPort ), SerialPort.MASK_RXCHAR );

		}
		catch ( SerialPortException ex )
		{
			System.out.println( "There are an error on writing string to port т: " + ex );
		}
		return serialPort;
	}

	private class PortReader implements SerialPortEventListener
	{

		private final SerialPort serialPort;

		String msg = "";

		public PortReader( SerialPort serialPort )
		{
			this.serialPort = serialPort;
		}

		@Override
		public void serialEvent( jssc.SerialPortEvent event )
		{
			if ( event.getEventValue() > 0 )
			{
				try
				{
					msg += serialPort.readString( event.getEventValue() );
					String delim = "\r\n";
					boolean endPresent = msg.endsWith( delim );
					String[] parts = msg.split( delim );
					for ( int i = 0; i < parts.length; i++ )
					{
						if ( i != parts.length - 1 )
						{
							if ( parts[ i ] != null )
								handleMsg( parts[ i ] );
						}
						else
						{
							if ( parts[ i ] == null )
								msg = "";
							if ( endPresent )
							{
								handleMsg( parts[ i ] );
								msg = "";
							}
							else
							{
								msg = parts[ i ];
							}
						}
					}
				}
				catch ( SerialPortException ex )
				{
					System.out.println( "Error in receiving string from COM-port: " + ex );
				}
			}
		}

		private void handleMsg( String text )
		{
			text = text.trim().replace( "\r", "" ).replace( "\n", "" );
			if ( text.isEmpty() )
				return;
//			System.out.println("Received from " + serialPort.getPortName() + ": " + text);
			if ( text.startsWith( "R" ) )
			{
				sendToSerialPort( sp1, text );
				sendToSerialPort( sp2, text );
			}
			switch ( text )
			{
			case "R1_T0":
				new Thread( () -> inputChanged( "input1", 0 ) ).start();
				break;
			case "R1_T1":
				new Thread( () -> inputChanged( "input2", 1 ) ).start();
				break;
			case "R1_NO":
			case "R1_??":
				new Thread( () -> inputChanged( "input removed", -1 ) ).start();
				break;
			case "R0_T0":
				new Thread( () -> changeNetworkAction( "network1", 0 ) ).start();
				break;
			case "R0_T1":
				new Thread( () -> changeNetworkAction( "network2", 1 ) ).start();
				break;
			case "R0_T2":
				new Thread( () -> changeNetworkAction( "gaussFilter", 2 ) ).start();
				break;
			case "R0_NO":
			case "R0_??":
				new Thread( () -> changeNetworkAction( "network removed", -1 ) ).start();
				break;
			case "R2_T0":
				new Thread( () -> changeSegmentationAction( "manualThreshold", 0 ) ).start();
				break;
			case "R2_T1":
				new Thread( () -> changeSegmentationAction( "otsuThreshold", 1 ) ).start();
				break;
			case "R2_NO":
			case "R2_??":
				new Thread( () -> changeSegmentationAction( "segmentation removed", -1 ) ).start();
				break;
			default:
				if ( text.contains( "S1" ) )
				{
					float sigma = Float.parseFloat( text.substring( 3 ) ) / 1023.f * 10.f;
					sigmaSliderVal.set( new FloatType( sigma ) );
					new Thread( () -> {
						// System.out.println("sigma: " + sigmaSliderVal.get());
						setSigmaAction( "sigmaChanged_" + String.valueOf( sigmaSliderVal.get() ), sigmaSliderVal.get().getRealFloat() );
					} ).start();
				}
				else if ( text.contains( "S2" ) )
				{
					thresholdSliderVal.set( new FloatType( Float.parseFloat( text.substring( 3 ) ) ) );
					new Thread( () -> {
						float ts = Math.round( 1000 * thresholdSliderVal.get().getRealFloat() / 1023.f ) / 1000.f;
						// System.out.println("threshold: " + text.substring( 3
						// ));
						setThresholdAction( "thresholdValue_" + String.valueOf( ts ), ts );
					} ).start();
				}
				break;
			}
		}

		private void sendToSerialPort( SerialPort sp, String msg )
		{
			System.out.println( "Sending to " + sp.getPortName() + ": " + msg );
			try
			{
				sp.writeString( msg + "\r\n" );
			}
			catch ( SerialPortException e )
			{
				e.printStackTrace();
			}
		}
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
		actionMap.put( keySigmaDown, new ChangeSigmaAction( keySigmaDown, -0.5f ) );
		actionMap.put( keySigmaUp, new ChangeSigmaAction( keySigmaUp, 0.5f ) );

		final String keyThresholdDown = "thresholdDown";
		final String keyThresholdUp = "thresholdUp";
		inputMap.put( KeyStroke.getKeyStroke( "released LEFT" ), keyThresholdDown );
		inputMap.put( KeyStroke.getKeyStroke( "released RIGHT" ), keyThresholdUp );
		actionMap.put( keyThresholdDown, new ChangeThresholdAction( keyThresholdDown, -0.01f ) );
		actionMap.put( keyThresholdUp, new ChangeThresholdAction( keyThresholdUp, +0.01f ) );

		final String keyThresholdManual = "manualThreshold";
		final String keyThresholdOtsu = "otsuThreshold";
		final String keyLabkit = "labkit";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_Z, 0 ), keyThresholdManual );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_X, 0 ), keyThresholdOtsu );
		actionMap.put( keyThresholdManual, new ChangeSegmentationAction( keyThresholdManual, 0 ) );
		actionMap.put( keyThresholdOtsu, new ChangeSegmentationAction( keyThresholdOtsu, 1 ) );
		actionMap.put( keyLabkit, new ChangeSegmentationAction( keyLabkit, 2 ) );

		final String keyFullScreen = "fullscreen";
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F, 0 ), keyFullScreen );
		actionMap.put( keyFullScreen, new FullScreenAction( keyFullScreen ) );
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
			new Thread( () -> {
				if ( wf.getInputStep().isActivated() && id == wf.getInputStep().getCurrentId() )
				{
					removeInput( actionEvt.getActionCommand() );
				}
				else
				{
					inputChanged( actionEvt.getActionCommand(), id );
				}
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
			new Thread( () -> {
				if ( wf.getNetworkStep().isActivated() && id == wf.getNetworkStep().getCurrentId() )
				{
					removeNetwork( actionEvt.getActionCommand() );
				}
				else
				{
					changeNetworkAction( actionEvt.getActionCommand(), id );
				}
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
			new Thread( () -> {
				if ( wf.getSegmentationStep().isActivated() && id == wf.getSegmentationStep().getCurrentId() )
				{
					removeSegmentation( actionEvt.getActionCommand() );
				}
				else
				{
					changeSegmentationAction( actionEvt.getActionCommand(), id );
				}
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
			new Thread( () -> setSigmaAction( actionEvt.getActionCommand(), ( float ) ( Math.round( ( wf.getGaussSigma() + change ) * 100 ) / 100.0 ) ) ).start();
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
			new Thread( () -> setThresholdAction( actionEvt.getActionCommand(), ( wf.getThreshold() + change ) ) ).start();
		}
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

	private synchronized void updateOnThresholdChange()
	{
		if ( wf.needsUpdate() )
		{
			int segmentationID = -1;
			float ts = -1;
			while ( segmentationID != wf.getSegmentationStep().getCurrentId() && ts != wf.getThreshold() )
			{
				segmentationID = wf.getSegmentationStep().getCurrentId();
				ts = wf.getThreshold();

				segmentationPanel.updateMethodLabel();
				segmentationPanel.updateNumberLabel();
				wf.runSegmentation();
				final boolean correctThreshold = wf.getSegmentationStep().getCurrentId() == 0 ? ts == wf.getThreshold() : true;
				if ( segmentationID == wf.getSegmentationStep().getCurrentId() && correctThreshold )
				{
					segmentationPanel.update();
					if ( wf.getSegmentationStep().isActivated() && wf.getInputStep().isActivated() )
					{
						wf.calculateOutput();
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
			while ( networkID != wf.getNetworkStep().getCurrentId() && sigma != wf.getGaussSigma() )
			{
				networkID = wf.getNetworkStep().getCurrentId();
				sigma = wf.getGaussSigma();
				networkPanel.updateMethodLabel();
				networkPanel.updateNumberLabel();
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
			while ( inputID != wf.getInputStep().getCurrentId() )
			{
				inputPanel.updateMethodLabel();
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

	private synchronized void inputChanged( final String command, final int id )
	{
		if ( id == -1 )
		{
			removeInput( command );
		}
		else
		{
			System.out.println( command + " pressed" );
			wf.getInputStep().setActivated( true );
			wf.setInput( id );
			if ( wf.getNetworkStep().isActivated() )
			{
				wf.setDenoisingMethod( wf.getNetworkStep().getCurrentId() );
			}
			wf.requestUpdate();
			updateOnInputChange();
		}
	}

	private synchronized void removeInput( String command )
	{
		System.out.println( command + " pressed" );
		if ( wf.getInputStep().isActivated() )
		{
			wf.getInputStep().setActivated( false );
			inputPanel.reset();
			networkPanel.reset();
			segmentationPanel.reset();
			outputPanel.reset();
			wf.requestUpdate();
			updateOnInputChange();
		}
	}

	private synchronized void changeNetworkAction( final String command, final int id )
	{
		if ( id == -1 )
		{
			removeNetwork( command );
		}
		else
		{
			System.out.println( command + " pressed" );
			wf.getNetworkStep().setActivated( true );
			wf.setDenoisingMethod( id );
			wf.requestUpdate();
			updateOnDenoiseChange();
		}
	}

	private synchronized void removeNetwork( final String command )
	{
		System.out.println( command + " pressed" );
//		outputPanel.reset();
		if ( wf.getNetworkStep().isActivated() )
		{
			wf.getNetworkStep().setActivated( false );
			wf.requestUpdate();
			updateOnDenoiseChange();
		}
	}

	private synchronized void changeSegmentationAction( final String command, final int id )
	{
		if ( id == -1 )
		{
			removeSegmentation( command );
		}
		else
		{
			System.out.println( command + " pressed" );
//		outputPanel.reset();
			wf.getSegmentationStep().setActivated( true );
			wf.setSegmentation( id );
			wf.requestUpdate();
			updateOnThresholdChange();
		}
	}

	private synchronized void removeSegmentation( final String command )
	{
		System.out.println( command + " pressed" );
//		outputPanel.reset();
		if ( wf.getSegmentationStep().isActivated() )
		{
			wf.getSegmentationStep().setActivated( false );
			wf.requestUpdate();
			updateOnThresholdChange();
		}
	}

	private void setSigmaAction( final String command, final float sigma )
	{
		if ( Math.abs( wf.getGaussSigma() - sigma ) < 0.25 )
			return;
		System.out.println( command + " pressed " + sigma );
		wf.setGaussSigma( sigma );
		if ( wf.getNetworkStep().isGauss() )
		{
			wf.requestUpdate();
			updateOnDenoiseChange();
		}
	}

	private void setThresholdAction( final String command, final float ts )
	{
		if ( Math.abs( wf.getThreshold() - ts ) < 0.01 )
			return;

		System.out.println( command + " pressed" );
		wf.setThreshold( ts );
		if ( wf.getSegmentationStep().useManual() )
		{
			wf.requestUpdate();
			updateOnThresholdChange();
		}
	}

	private void close()
	{
		dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
	}

}
