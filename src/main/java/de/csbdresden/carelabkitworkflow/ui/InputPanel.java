package de.csbdresden.carelabkitworkflow.ui;

import de.csbdresden.carelabkitworkflow.model.InputStep;

import java.awt.*;

public class InputPanel extends AbstractBDVPanel {

	private final InputStep inputStep;

	InputPanel(final InputStep inputStep) {
		this.inputStep = inputStep;
		bgColor = new Color(255, 49, 117);
		setBackground(bgColor);
	}

	public void update() {
		if(!inputStep.isActivated()) showInBdv(null);
		else showInBdv(inputStep.getImage());
	}

}
