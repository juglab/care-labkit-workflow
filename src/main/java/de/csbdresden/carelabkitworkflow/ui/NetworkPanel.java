package de.csbdresden.carelabkitworkflow.ui;

import de.csbdresden.carelabkitworkflow.model.NetworkStep;

import java.awt.*;

public class NetworkPanel extends AbstractBDVPanel {

	private final NetworkStep networkStep;

	NetworkPanel(final NetworkStep networkStep) {
		this.networkStep = networkStep;
		setBackground(new Color(197, 49, 255));
	}

	public void update() {
		if(!networkStep.isActivated()) showInBdv(null);
		else showInBdv(networkStep.getImage());
	}
}
