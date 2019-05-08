package de.csbdresden.carelabkitworkflow.util;

import java.awt.Color;
import java.util.Random;

import bdv.util.VirtualChannels.VirtualChannel;
import bdv.viewer.ViewerPanel;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;

/**
 * 
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 * 
 */
public class RandomColorTable<T extends NumericType<T>, L, I extends IntegerType<I>>
		implements ColorTable, VirtualChannel {

	private final double GOLDEN_RATIO_CONJUGATE = 0.61803398874988749895;

	/**
	 * Index image of the labeling.
	 */
	protected final LabelingMapping<L> labelingMapping;

	/**
	 * Converter combining multiple LUTs.
	 */
	private final ColorTableConverter<L> converter;

	/**
	 * Lookup table.
	 */
	protected int[] lut;

	/**
	 * BDV viewer panel.
	 */
	private ViewerPanel viewer;

	/**
	 * Random numbers.
	 */
	private Random r;

	/**
	 * Alpha scale for aRGB colors.
	 */
	protected int alpha = 255;


	/**
	 * Creates a random coloring for a given labeling.
	 * 
	 * @param mapping
	 *            index image of labeling
	 * @param converter which combines multiple LUTs
	 * @param es event service
	 */
	public RandomColorTable(final LabelingMapping<L> mapping, final ColorTableConverter<L> converter, final ViewerPanel viewer) {
		this.labelingMapping = mapping;
		this.converter = converter;
		this.viewer = viewer;
		lut = new int[labelingMapping.numSets()];
		r = new Random();
	}

	@Override
	public int[] getLut() {
		return lut;
	}

	/**
	 * Create colors for LUT with golden ratio distribution.
	 */
	public void fillLut() {
		// Zero-Label is transparent background.
		lut[0] = ARGBType.rgba(0, 0, 0, 0);
		float h = r.nextFloat();
		for (int i = 1; i < labelingMapping.numSets(); i++) {
			h += GOLDEN_RATIO_CONJUGATE;
			h %= 1;
			lut[i] = Color.HSBtoRGB(h, 0.75f, 1f);
		}
	}


	/**
	 * Update LUT.
	 */
	public void update() {
		converter.update();
		if (viewer != null) {
			viewer.requestRepaint();
		}
	}

	@Override
	public void updateVisibility() {
		update();
	}

	@Override
	public void updateSetupParameters() {
		update();
	}
}
