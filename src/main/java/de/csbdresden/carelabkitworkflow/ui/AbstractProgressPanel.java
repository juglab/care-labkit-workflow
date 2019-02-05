package de.csbdresden.carelabkitworkflow.ui;

import javax.swing.*;

public abstract class AbstractProgressPanel extends JPanel {
	private JProgressBar progressBar;

	protected void init() {
		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		add(progressBar, "pos 0.5al 0.5al, w 42:42:42, h 42:42:42");
	}

	public void startProgress() {
		progressBar.setVisible(true);
	}

	public void endProgress() {
		progressBar.setVisible(false);
	}
}