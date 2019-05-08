package de.csbdresden.carelabkitworkflow.util;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

/**
 * Accumulate projector which performs alpha blending with labelings and simple
 * addition with images.
 * 
 * If images and labelings are present, the labelings are added with alpha
 * blending on top of the images.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class AccumulateProjectorAlphaBlendingARGB extends AccumulateProjector<ARGBType, ARGBType> {

	/**
	 * {@inheritDoc}
	 * 
	 * @param labelingLookup
	 *            indicates if an access is from a labeling
	 * @param startImgs
	 *            index of the first image
	 * @param startLabs
	 *            index of the first labeling
	 */
	public AccumulateProjectorAlphaBlendingARGB(final ArrayList<VolatileProjector> sourceProjectors,
			final ArrayList<? extends RandomAccessible<? extends ARGBType>> sources,
			final RandomAccessibleInterval<ARGBType> target, final int numThreads,
			final ExecutorService executorService) {
		super(sourceProjectors, sources, target, numThreads, executorService);
	}

	@Override
	protected void accumulate(final Cursor<? extends ARGBType>[] accesses, final ARGBType target) {

		target.set( ColorUtils.combineColors( accesses[0].get().get(), accesses[1].get().get() ) );
	}

}
