/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package de.csbdresden.carelabkitworkflow.command;

import de.csbdresden.carelabkitworkflow.backend.CARELabkitWorkflow;
import de.csbdresden.carelabkitworkflow.ui.WorkflowFrame;
import net.imagej.ImageJ;
import org.scijava.Context;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;

@Plugin(type = Command.class, menuPath = "Plugins>Outreach>CARE Labkit Workflow")
public class WorkflowCommand implements Command, Initializable {

    @Parameter
    private Context context;

    @Override
    public void initialize() {
    }

    @Override
    public void run() {
        CARELabkitWorkflow wf = new CARELabkitWorkflow();
        context.inject(wf);

        WorkflowFrame frame = new WorkflowFrame(wf);
        context.inject(frame);
        frame.setPreferredSize( new Dimension( 800, 600 ) );
        frame.pack();
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // invoke the plugin
        ij.command().run(WorkflowCommand.class, true);
    }

}
