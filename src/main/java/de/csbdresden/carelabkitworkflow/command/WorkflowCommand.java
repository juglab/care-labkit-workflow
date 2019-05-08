/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package de.csbdresden.carelabkitworkflow.command;

import java.awt.Dimension;

import org.scijava.Context;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.csbdresden.carelabkitworkflow.backend.CARELabkitWorkflow;
import de.csbdresden.carelabkitworkflow.ui.WorkflowFrame;
import net.imagej.ImageJ;

@Plugin( type = Command.class, menuPath = "Plugins>Outreach>CARE Labkit Workflow" )
public class WorkflowCommand implements Command, Initializable
{

	@Parameter
	private boolean loadChachedCARE = true;

	@Parameter
	private Context context;

	@Override
	public void initialize()
	{}

	@Override
	public void run()
	{
		CARELabkitWorkflow wf = new CARELabkitWorkflow( loadChachedCARE );
		context.inject( wf );

		WorkflowFrame frame = new WorkflowFrame( wf, loadChachedCARE );
		context.inject( frame );
		frame.setPreferredSize( new Dimension( 1200, 600 ) );
		frame.pack();
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setVisible( true );
	}

	public static void main( final String... args ) throws Exception
	{
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		ij.command().run( WorkflowCommand.class, true );
	}

}
