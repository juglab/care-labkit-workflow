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

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

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

	private SerialPort sp;

	static GraphicsDevice device = GraphicsEnvironment
			.getLocalGraphicsEnvironment().getScreenDevices()[ 0 ];

	public WorkflowFrame( final CARELabkitWorkflow< T, I > wf, final String port )
	{
		super( "Bio-Image Analysis Workflow" );
		this.wf = wf;
		createWorkflowPanels();
		setKeyBindings();
		initSerialPort(port);
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
		inputPanel.init( this, "Eingabe" );
		networkPanel.init( this, "Entrauschen" );
		segmentationPanel.init( this, "Segmentierung" );
		outputPanel.init( "Validierung" );
	}

	private void initSerialPort(final String port)
	{
		sp = SerialPort.getCommPort( port ); // device name
														// TODO:
														// must be
														// changed
		sp.setComPortParameters( 9600, 8, 1, 0 ); // default connection settings
													// for Arduino
		sp.setComPortTimeouts( SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0 ); // block
																			// until
																			// bytes
																			// can
																			// be
																			// written

		if ( sp.openPort() )
		{
			System.out.println( "Port is open :)" );
		}
		else
		{
			System.out.println( "Failed to open port :(" );
			return;
		}

		MessageListener listener = new MessageListener();
		sp.addDataListener( listener );

	}

	private final class MessageListener implements SerialPortMessageListener
	{
		@Override
		public int getListeningEvents()
		{
			return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
		}

		@Override
		public byte[] getMessageDelimiter()
		{
			return new byte[] { ( byte ) 0x0a };
		}

		@Override
		public boolean delimiterIndicatesEndOfMessage()
		{
			return true;
		}

		@Override
		public void serialEvent( SerialPortEvent event )
		{
			byte[] delimitedMessage = event.getReceivedData();
			String msg = new String( delimitedMessage );
			msg = msg.replace( "\n", "" );

			switch ( msg )
			{
			case "q":
				inputChanged( "input1", 0 );
				break;
			case "w":
				inputChanged( "input2", 1 );
				break;
			case "e":
				changeNetworkAction( "network1", 0 );
				break;
			case "r":
				changeNetworkAction( "network2", 1 );
				break;
			case "g":
				changeNetworkAction( "gaussFilter", 2 );
				break;
			case "z":
				changeSegmentationAction( "manualThreshold", 0 );
				break;
			case "x":
				changeSegmentationAction( "otsuThreshold", 1 );
				break;
			default:
				if (msg.contains( "sigma: " )) {
					float sigma = Float.parseFloat( msg.substring( 7 ) );
					setSigmaAction( "sigmaChanged_"+String.valueOf( sigma ), sigma );
				} else if (msg.contains( "threshold: " )) {
					float ts = Float.parseFloat( msg.substring( 11 ) );
					setThresholdAction( "thresholdValue_"+String.valueOf( ts ), ts );
				}
				break;
			}

		}
	}

	private void setKeyBindings()
	{
		final ActionMap actionMap = workflows.getActionMap();
		int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
		final InputMap inputMap = workflows.getInputMap( condition );

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
	
	private void inputChanged(final String command, final int id) {
		System.out.println( command + " pressed" );
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

	private void changeNetworkAction(final String command, final int id) {
		System.out.println( command + " pressed" );
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
	
	private void changeSegmentationAction(final String command, final int id) {
		System.out.println( command + " pressed" );
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

	private void setSigmaAction(final String command, final float sigma) {
		System.out.println( command + " pressed " + sigma );
		new Thread( () -> {
			wf.setGaussSigma( sigma );
			if ( wf.getNetworkStep().isGauss() )
			{
				wf.requestUpdate();
				updateOnDenoiseChange();
			}
		} ).start();
	}

	private void setThresholdAction(final String command, final float ts) {
		System.out.println( command + " pressed" );
		new Thread( () -> {
			wf.setThreshold( ts );
			wf.requestUpdate();
			updateOnThresholdChange();
		} ).start();
	}

	private void close()
	{
		dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
	}

}
