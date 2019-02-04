package de.csbdresden.carelabkitworkflow.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class ResultPanel extends JPanel {

	private JLabel result;
	private Font font = new Font(Font.MONOSPACED, Font.PLAIN, 136);

	ResultPanel() {
		setBackground(new Color(255, 246, 49));
		setLayout(new MigLayout("fill, flowy"));
	}

	void init(String title) {
		JPanel resultPanel = new JPanel();
		resultPanel.setBackground(null);
		result = new JLabel();
		result.setFont(font);
		result.setAlignmentX(CENTER_ALIGNMENT);
		resultPanel.add(result);
		add(resultPanel, "push, align 50% 50%");
		add(new JLabel(title));
	}

	public void showOutput(int output) {
		result.setText(String.valueOf(output));
		revalidate();
	}
}
