package de.csbdresden.carelabkitworkflow.ui;

import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractBDVPanel extends JPanel {

	protected BdvHandlePanel bdv;
	private WorkflowFrame parent;
	protected Color bgColor;

	public void init(WorkflowFrame parent, String title) {
		this.parent = parent;
		setLayout(new MigLayout("fill"));
		JLabel titleLabel = new JLabel(title);
		titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		add(titleLabel, "aligny bottom");
	}

	public<T extends RealType<T>>  void showInBdv(Img input) {
		removeBdv();
		if(input != null) {
			bdv = new BdvHandlePanel( parent, Bdv.options().is2D() );
			if(bgColor != null) bdv.getViewerPanel().setBackground(bgColor);
			add( bdv.getViewerPanel(), "push, span, grow", 0);
			BdvFunctions.show( input, "input", Bdv.options().addTo( bdv ) );
			final SetupAssignments sa = bdv.getBdvHandle().getSetupAssignments();
			final MinMaxGroup group = sa.getMinMaxGroups().get(0);
			Pair<T, T> minMax = parent.getMinMax(input);
			group.getMinBoundedValue().setCurrentValue( minMax.getA().getRealDouble() );
			group.getMaxBoundedValue().setCurrentValue( minMax.getB().getRealDouble() );
			revalidate();
		}
	}

	public void removeBdv() {
		if(bdv != null) {
			remove(bdv.getViewerPanel());
			bdv.close();
			revalidate();
			repaint();
		}
	}
}
