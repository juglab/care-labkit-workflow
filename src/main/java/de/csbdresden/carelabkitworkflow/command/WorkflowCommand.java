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
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = Command.class, menuPath = "Plugins>Outreach>CARE Labkit Workflow" )
public class WorkflowCommand< T extends RealType< T > & NativeType< T >, I extends IntegerType< I >> implements Command, Initializable
{

	@Parameter
	private boolean loadChachedCARE = true;
	
	@Parameter
	private String port = "/dev/pts/7";

	@Parameter
	private Context context;

	@Override
	public void initialize()
	{}

	@Override
	public void run()
	{
		final CARELabkitWorkflow<T, I> wf = new CARELabkitWorkflow<>( loadChachedCARE );
		context.inject( wf );

		final WorkflowFrame<T, I> frame = new WorkflowFrame<>( wf, port );
		context.inject( frame );
		frame.setPreferredSize( new Dimension( 1200, 600 ) );
		frame.pack();
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
