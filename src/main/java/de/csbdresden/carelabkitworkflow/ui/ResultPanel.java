package de.csbdresden.carelabkitworkflow.ui;

import de.csbdresden.carelabkitworkflow.model.OutputStep;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class ResultPanel extends JPanel {

	private final OutputStep outputStep;
	private JLabel result;
	private Font font = new Font(Font.MONOSPACED, Font.PLAIN, 136);

	ResultPanel(final OutputStep outputStep) {
		this.outputStep = outputStep;
		setBackground(new Color(255, 246, 49));
		setLayout(new MigLayout("fill, flowy"));
	}

	public void init(String title) {
		JPanel resultPanel = new JPanel();
		resultPanel.setBackground(null);
		result = new JLabel();
		result.setFont(font);
		result.setAlignmentX(CENTER_ALIGNMENT);
		resultPanel.add(result);
		add(resultPanel, "push, align 50% 50%");
		JLabel titleLabel = new JLabel(title);
		titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		add(titleLabel, "dock south");
	}

	public void update() {
		showOutput(outputStep.getResult());
	}

	private void showOutput(int output) {
		result.setText(output >= 0 ? String.valueOf(output) : "?");
	}

	public void reset() {
		result.setText("");
	}
}
