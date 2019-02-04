package de.csbdresden.carelabkitworkflow.ui;

import de.csbdresden.carelabkitworkflow.model.SegmentationStep;

import java.awt.*;

public class SegmentationPanel extends AbstractBDVPanel {

	private final SegmentationStep segmentationStep;

	SegmentationPanel(final SegmentationStep segmentationStep) {
		this.segmentationStep = segmentationStep;
		setBackground(new Color(49, 193, 255));
	}

	public void update() {
		if(!segmentationStep.isActivated()) showInBdv(null);
		else showInBdv(segmentationStep.getImage());
	}

}
