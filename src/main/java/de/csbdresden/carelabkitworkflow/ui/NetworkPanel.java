package de.csbdresden.carelabkitworkflow.ui;

import java.awt.Color;

import de.csbdresden.carelabkitworkflow.model.DenoisingStep;
import de.csbdresden.carelabkitworkflow.model.InputStep;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class NetworkPanel< T extends RealType< T > & NativeType< T > > extends AbstractBDVPanel< T >
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final DenoisingStep< T > networkStep;

	private InputStep< T > inputStep;

	public NetworkPanel( final DenoisingStep< T > networkStep, final InputStep< T > inputStep )
	{
		this.networkStep = networkStep;
		this.inputStep = inputStep;
		setBackground( new Color( 255, 246, 49 ) );
	}

	public void update()
	{
		runOnEventDispatchThread(() -> {
			if (!networkStep.isActivated() || !inputStep.isActivated()) {
				showInBdv(null);
				reset();
			} else {
				showInBdv(networkStep);
			}
		});
	}

	@Override
	protected void initStep()
	{
		// nothing to do
	}

	public void reset()
	{
		runOnEventDispatchThread(() -> {
			bdv.getBdvHandle().getViewerPanel().removeAllSources();
			numberLabel.setText("");
			methodLabel.setText("");
			repaint();
		});
	}

	@Override
	protected void updateMethodLabel()
	{
		runOnEventDispatchThread(() -> {
			if (networkStep.isActivated()) {
				methodLabel.setText(networkStep.getName());
			} else {
				methodLabel.setText("");
			}
		});
	}

	@Override
	protected void updateNumberLabel()
	{
		runOnEventDispatchThread(() -> {
			if (networkStep.isActivated() && networkStep.isGauss()) {
				numberLabel.setText("   Sigma = " + String.valueOf(Math.round(networkStep.getGaussSigma() * 100) / 100.0));
			} else {
				numberLabel.setText("");
			}
		});
	}
}
