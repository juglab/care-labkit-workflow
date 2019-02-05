package de.csbdresden.carelabkitworkflow.ui;

import de.csbdresden.carelabkitworkflow.backend.CARELabkitWorkflow;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.miginfocom.swing.MigLayout;
import org.scijava.app.StatusService;
import org.scijava.event.*;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WorkflowFrame extends JFrame {

	private final CARELabkitWorkflow wf;

	private InputPanel inputPanel;
	private NetworkPanel networkPanel;
	private SegmentationPanel segmentationPanel;
	private ResultPanel outputPanel;

	private JPanel workflows;

	private Font font = new Font(Font.MONOSPACED, Font.PLAIN, 16);

	private boolean fullScreen = false;

	static GraphicsDevice device = GraphicsEnvironment
			.getLocalGraphicsEnvironment().getScreenDevices()[0];

	public WorkflowFrame(CARELabkitWorkflow wf) {
		super("CARE Labkit workflow");
		this.wf = wf;
		createWorkflowPanels();
		setKeyBindings();
	}

	private void createWorkflowPanels() {

		workflows = new JPanel();
		workflows.setLayout(new MigLayout("fill, gap 0, ins 20 0 20 0", "push[]push[]push[]push[]push"));

		inputPanel = new InputPanel(wf.getInputStep());
		networkPanel = new NetworkPanel(wf.getNetworkStep());
		segmentationPanel = new SegmentationPanel(wf.getSegmentationStep());
		outputPanel = new ResultPanel(wf.getOutputStep());

		String w = "(25%-25px)";
		workflows.add(inputPanel, "grow, width "+w+":"+w+":"+w);
		workflows.add(networkPanel, "grow, width "+w+":"+w+":"+w);
		workflows.add(segmentationPanel, "grow, width "+w+":"+w+":"+w);
		workflows.add(outputPanel, "grow, width "+w+":"+w+":"+w);

		this.setContentPane(workflows);
		inputPanel.init(this, "INPUT [q,w]");
		networkPanel.init(this, "DENOISE [e,r]");
		segmentationPanel.init(this, "SEGMENTATION [z, ← →]");
		outputPanel.init("RESULT");
	}

	private void setKeyBindings() {
		ActionMap actionMap = workflows.getActionMap();
		int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
		InputMap inputMap = workflows.getInputMap(condition );

		String keyInput1 = "input1";
		String keyInput2 = "input2";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), keyInput1);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), keyInput2);
		actionMap.put(keyInput1, new ChangeInputAction(keyInput1, 0));
		actionMap.put(keyInput2, new ChangeInputAction(keyInput2, 1));

		String keyNetwork1 = "network1";
		String keyNetwork2 = "network2";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), keyNetwork1);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), keyNetwork2);
		actionMap.put(keyNetwork1, new ChangeNetworkAction(keyNetwork1, 0));
		actionMap.put(keyNetwork2, new ChangeNetworkAction(keyNetwork2, 1));

		String keySegmentation1 = "segmentation1";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0), keySegmentation1);
		actionMap.put(keySegmentation1, new ChangeSegmentationAction(keySegmentation1, 0));

		String keyThresholdDown = "thresholdDown";
		String keyThresholdUp = "thresholdUp";
		inputMap.put(KeyStroke.getKeyStroke("released LEFT"), keyThresholdDown);
		inputMap.put(KeyStroke.getKeyStroke("released RIGHT"), keyThresholdUp);
		actionMap.put(keyThresholdDown, new ChangeThresholdAction(keyThresholdDown, -0.05f));
		actionMap.put(keyThresholdUp, new ChangeThresholdAction(keyThresholdUp, +0.05f));

		String keyFullScreen = "fullscreen";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), keyFullScreen);
		actionMap.put(keyFullScreen, new FullScreenAction(keyFullScreen));
	}

	public <T extends RealType<T>> Pair<T, T> getMinMax(Img input) {
		return wf.getMinMax(input);
	}

	private class FullScreenAction extends AbstractAction {
		public FullScreenAction(String actionCommand) {
			putValue(ACTION_COMMAND_KEY, actionCommand);
		}

		@Override
		public void actionPerformed(ActionEvent actionEvt) {
			System.out.println(actionEvt.getActionCommand() + " pressed");
			toggleFullScreen();
		}
	}

	private void toggleFullScreen() {
		fullScreen = !fullScreen;
		if(fullScreen) {
			device.setFullScreenWindow(this);
		}else {
			device.setFullScreenWindow(null);
		}
		setVisible(true);
	}

	private class ChangeInputAction extends AbstractAction {
		private final int id;
		public ChangeInputAction(String actionCommand, final int id) {
			this.id = id;
			putValue(ACTION_COMMAND_KEY, actionCommand);
		}

		@Override
		public void actionPerformed(ActionEvent actionEvt) {
			System.out.println(actionEvt.getActionCommand() + " pressed");
			new Thread(() -> {
				outputPanel.reset();
				segmentationPanel.removeBdv();
				networkPanel.removeBdv();
				inputPanel.removeBdv();
				if(wf.getInputStep().getCurrentId() == id &&
					wf.getInputStep().isActivated()) {
					wf.getInputStep().setActivated(false);
				} else {
					wf.getInputStep().setActivated(true);
					wf.setInput(id);
				}
				inputPanel.startProgress();
				inputPanel.update();
				inputPanel.endProgress();
				networkPanel.startProgress();
				wf.runNetwork();
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
			}).start();
		}
	}

	private class ChangeNetworkAction extends AbstractAction {
		private final int id;
		public ChangeNetworkAction(String actionCommand, final int id) {
			this.id = id;
			putValue(ACTION_COMMAND_KEY, actionCommand);
		}

		@Override
		public void actionPerformed(ActionEvent actionEvt) {
			System.out.println(actionEvt.getActionCommand() + " pressed");
			new Thread(() -> {
				outputPanel.reset();
				segmentationPanel.removeBdv();
				networkPanel.removeBdv();
				if(wf.getNetworkStep().getCurrentId() == id &&
						wf.getNetworkStep().isActivated()) {
					wf.getNetworkStep().setActivated(false);
				} else {
					wf.getNetworkStep().setActivated(true);
					wf.setNetwork(id);
				}
				networkPanel.startProgress();
				wf.runNetwork();
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
			}).start();
		}
	}

	private class ChangeSegmentationAction extends AbstractAction {
		private final int id;
		public ChangeSegmentationAction(String actionCommand, final int id) {
			this.id = id;
			putValue(ACTION_COMMAND_KEY, actionCommand);
		}

		@Override
		public void actionPerformed(ActionEvent actionEvt) {
			System.out.println(actionEvt.getActionCommand() + " pressed");
			new Thread(() -> {
				outputPanel.reset();
				segmentationPanel.removeBdv();
				if(wf.getSegmentationStep().getCurrentId() == id &&
						wf.getSegmentationStep().isActivated()) {
					wf.getSegmentationStep().setActivated(false);
				} else {
					wf.getSegmentationStep().setActivated(true);
					wf.setSegmentation(id);
				}
				segmentationPanel.startProgress();
				wf.runSegmentation();
				segmentationPanel.update();
				segmentationPanel.endProgress();
				outputPanel.startProgress();
				wf.calculateOutput();
				outputPanel.update();
				outputPanel.endProgress();
			}).start();
		}
	}

	private class ChangeThresholdAction extends AbstractAction {
		private final float change;
		public ChangeThresholdAction(String actionCommand, final float change) {
			this.change = change;
			putValue(ACTION_COMMAND_KEY, actionCommand);
		}

		@Override
		public void actionPerformed(ActionEvent actionEvt) {
			System.out.println(actionEvt.getActionCommand() + " pressed");
			new Thread(() -> {
				segmentationPanel.startProgress();
				wf.setThreshold(wf.getThreshold()+change);
				wf.runSegmentation();
				segmentationPanel.update();
				segmentationPanel.endProgress();
				outputPanel.startProgress();
				wf.calculateOutput();
				outputPanel.update();
				outputPanel.endProgress();
			}).start();
		}
	}

	private void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

}
