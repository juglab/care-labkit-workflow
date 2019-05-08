package de.csbdresden.carelabkitworkflow.model;


public class OutputStep extends AbstractWorkflowStep {
	private int result = -1;

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}
}
