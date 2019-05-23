package de.csbdresden.carelabkitworkflow.ui;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

public abstract class AbstractProgressPanel extends JPanel
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JProgressBar progressBar;

	protected void initStep()
	{
		progressBar = new JProgressBar();
		progressBar.setIndeterminate( true );
		progressBar.setVisible( false );
		add( progressBar, "pos 0.5al 0.5al, w 42:42:42, h 42:42:42" );
	}

	public void startProgress()
	{
		progressBar.setVisible( true );
	}

	public void endProgress()
	{
		progressBar.setVisible( false );
	}
}
