package de.csbdresden.carelabkitworkflow.ui;

import de.csbdresden.carelabkitworkflow.backend.CARELabkitWorkflow;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.miginfocom.swing.MigLayout;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

public class WorkflowFrame extends JFrame {

	@Parameter
	private ThreadService threadService;

	private final CARELabkitWorkflow wf;

	private InputPanel inputPanel;
	private NetworkPanel networkPanel;
	private SegmentationPanel segmentationPanel;
	private ResultPanel outputPanel;

	private JPanel workflows;

	private Font font = new Font(Font.MONOSPACED, Font.PLAIN, 16);

	public WorkflowFrame(CARELabkitWorkflow wf) {
		super("CARE Labkit workflow");
		this.wf = wf;
		createWorkflowPanels();
		setKeyBindings();
	}

	private void createWorkflowPanels() {

		workflows = new JPanel();
		workflows.setLayout(new MigLayout("fill"));

		inputPanel = new InputPanel();
		networkPanel = new NetworkPanel();
		segmentationPanel = new SegmentationPanel();
		outputPanel = new ResultPanel();

		workflows.add(inputPanel, "grow, width 25%:25%:25%");
		workflows.add(networkPanel, "grow, width 25%:25%:25%");
		workflows.add(segmentationPanel, "grow, width 25%:25%:25%");
		workflows.add(outputPanel, "grow, width 25%:25%:25%");

		this.setContentPane(workflows);
		inputPanel.init(this, "INPUT [q,w]");
		networkPanel.init(this, "DENOISE [e,r]");
		segmentationPanel.init(this, "SEGMENTATION [z,x, <- ->]");
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

		String keyThresholdDown = "thresholdDown";
		String keyThresholdUp = "thresholdUp";
		inputMap.put(KeyStroke.getKeyStroke("released LEFT"), keyThresholdDown);
		inputMap.put(KeyStroke.getKeyStroke("released RIGHT"), keyThresholdUp);
		actionMap.put(keyThresholdDown, new ChangeThresholdAction(keyThresholdDown, -0.05f));
		actionMap.put(keyThresholdUp, new ChangeThresholdAction(keyThresholdUp, +0.05f));
	}

	public <T extends RealType<T>> Pair<T, T> getMinMax(Img input) {
		return wf.getMinMax(input);
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
				wf.setInput(id);
				wf.run();
				updateContent();
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
				wf.setNetwork(id);
				wf.run();
				updateContent();
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
				wf.setThreshold(wf.getThreshold()+change);
				wf.runSegmentation();
				wf.calculateOutput();
				segmentationPanel.showInput(wf.getSegmentedInput());
				outputPanel.showOutput(wf.getOutput());
			}).start();
		}
	}

	private void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private void updateContent() {
		inputPanel.showInput(wf.getInput());
		networkPanel.showInput(wf.getDenoisedInput());
		segmentationPanel.showInput(wf.getSegmentedInput());
		outputPanel.showOutput(wf.getOutput());
	}
}
