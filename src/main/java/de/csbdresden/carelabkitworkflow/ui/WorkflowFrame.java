package de.csbdresden.carelabkitworkflow.ui;

import de.csbdresden.carelabkitworkflow.backend.CARELabkitWorkflow;
import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
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

	public WorkflowFrame(final CARELabkitWorkflow<T, I> wf, String port1, final String port2)
	{
		super( "Bio-Image Analysis Workflow" );
		this.wf = wf;
		createWorkflowPanels();
		setKeyBindings();
		initSerialPort(port1, port2);
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

	private void initSerialPort(String port1, final String port2)
	{
		sp1 = setupSerialPort(port1);
		sp2 = setupSerialPort(port2);

	}

	private SerialPort setupSerialPort(String port) {
		SerialPort serialPort = new SerialPort(port);
		try {
			serialPort.openPort();

			serialPort.setParams(SerialPort.BAUDRATE_9600,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
					SerialPort.FLOWCONTROL_RTSCTS_OUT);

			serialPort.addEventListener(new PortReader(serialPort), SerialPort.MASK_RXCHAR);

		}
		catch (SerialPortException ex) {
			System.out.println("There are an error on writing string to port т: " + ex);
		}
		return serialPort;
	}

	private class PortReader implements SerialPortEventListener {

		private final SerialPort serialPort;
		String msg = "";

		public PortReader(SerialPort serialPort) {
			this.serialPort = serialPort;
		}

		@Override
		public void serialEvent(jssc.SerialPortEvent event) {
			if(event.getEventValue() > 0) {
				try {
					msg += serialPort.readString(event.getEventValue());
					String delim = "\r\n";
					boolean endPresent = msg.endsWith(delim);
					String [] parts = msg.split(delim);
						for (int i = 0; i < parts.length; i++) {
							if(i != parts.length-1) {
								if(parts[i] != null) handleMsg(parts[i]);
							} else {
								if(parts[i] == null) msg = "";
								if(endPresent) {
									handleMsg(parts[i]);
									msg = "";
								} else {
									msg = parts[i];
								}
							}
						}
				}
				catch (SerialPortException ex) {
					System.out.println("Error in receiving string from COM-port: " + ex);
				}
			}
		}

		private void handleMsg(String text) {
			text = text.trim().replace("\r", "").replace("\n", "");
			if(text.isEmpty()) return;
//			System.out.println("Received from " + serialPort.getPortName() + ": " + text);
			if(text.startsWith("R")) {
				sendToSerialPort(sp1, text);
				sendToSerialPort(sp2, text);
			}
			switch ( text )
			{
			case "R1_T0":
				inputChanged( "input1", 0 );
				break;
			case "R1_T1":
				inputChanged( "input2", 1 );
				break;
			case "R1_NO":
				removeInput("input removed");
				break;
			case "R0_T0":
				changeNetworkAction( "network1", 0 );
				break;
			case "R0_T1":
				changeNetworkAction( "network2", 1 );
				break;
			case "R0_T2":
				changeNetworkAction( "gaussFilter", 2 );
				break;
			case "R0_NO":
				removeNetwork("network removed");
				break;
			case "R2_T0":
				changeSegmentationAction( "manualThreshold", 0 );
				break;
			case "R2_T1":
				changeSegmentationAction( "otsuThreshold", 1 );
				break;
			case "R2_NO":
				removeSegmentation("segmentation removed");
				break;
			default:
				if (text.contains( "S1" )) {
					float sigma = Float.parseFloat( text.substring( 3 ) ) / 1024.f*10.f;
//						System.out.println("sigma: " + sigma);
					setSigmaAction( "sigmaChanged_"+String.valueOf( sigma ), sigma );
				} else if (text.contains( "S2" )) {
					float ts = Float.parseFloat( text.substring( 3 ) ) / 1024.f;
//						System.out.println("threshold: " + ts);
					setThresholdAction( "thresholdValue_"+String.valueOf( ts ), ts );
				}
				break;
			}
		}

		private void sendToSerialPort(SerialPort sp, String msg) {
			System.out.println("Sending to " + sp.getPortName() + ": " + msg);
			try {
				sp.writeString(msg+"\r\n");
			} catch (SerialPortException e) {
				e.printStackTrace();
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
			wf.getInputStep().setActivated( true );
			wf.setInput( id );
			if ( wf.getNetworkStep().isActivated() )
			{
				wf.setDenoisingMethod( wf.getNetworkStep().getCurrentId() );
			}
			wf.requestUpdate();
			updateOnInputChange();
		} ).start();
	}

	private void removeInput(String command) {
		System.out.println( command + " pressed" );
		new Thread( () -> {
			wf.getInputStep().setActivated( false );
			wf.requestUpdate();
			updateOnInputChange();
		} ).start();
	}

	private void changeNetworkAction(final String command, final int id) {
		System.out.println( command + " pressed" );
		new Thread( () -> {
			outputPanel.reset();
			wf.getNetworkStep().setActivated( true );
			wf.setDenoisingMethod( id );
			wf.requestUpdate();
			updateOnDenoiseChange();
		} ).start();
	}

	private void removeNetwork(final String command) {
		System.out.println( command + " pressed" );
		new Thread( () -> {
			outputPanel.reset();
			wf.getNetworkStep().setActivated( false );
			wf.requestUpdate();
			updateOnDenoiseChange();
		} ).start();
	}
	
	private void changeSegmentationAction(final String command, final int id) {
		System.out.println( command + " pressed" );
		new Thread( () -> {
			outputPanel.reset();
			wf.getSegmentationStep().setActivated( true );
			wf.setSegmentation( id );
			wf.requestUpdate();
			updateOnThresholdChange();
		} ).start();
	}

	private void removeSegmentation(final String command) {
		System.out.println( command + " pressed" );
		new Thread( () -> {
			outputPanel.reset();
			wf.getSegmentationStep().setActivated( false );
			wf.requestUpdate();
			updateOnThresholdChange();
		} ).start();
	}

	private void setSigmaAction(final String command, final float sigma) {
		if(Math.abs(wf.getGaussSigma()-sigma)<0.5) return;
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
		if(Math.abs(wf.getThreshold()-ts)<0.05) return;
		System.out.println( command + " pressed" );
		new Thread( () -> {
			wf.setThreshold( ts );
			if(wf.getSegmentationStep().useManual()) {
				wf.requestUpdate();
				updateOnThresholdChange();
			}
		} ).start();
	}

	private void close()
	{
		dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
	}

}
