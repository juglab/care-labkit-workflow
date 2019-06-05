package de.csbdresden.carelabkitworkflow.util;

import org.scijava.log.LogService;

import de.mpicbg.ulman.workers.TrackDataCache;
import de.mpicbg.ulman.workers.TrackDataCache.TemporalLevel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

public class SEG_Score
{
	private TrackDataCache cache;

	public SEG_Score(final LogService log)
	{
		cache = new TrackDataCache(log);
		
	}
	
	public double calculate(final RandomAccessibleInterval< UnsignedShortType > gtImg, final RandomAccessibleInterval< UnsignedShortType > resImg) {
		
		double seg = 0.0;
		long counter = 0;
		
		cache.ClassifyLabels( Views.iterable( gtImg ), resImg );
		
		final TemporalLevel level = cache.levels.lastElement();
		
		final int m_match_lineSize = level.m_gt_lab.length;
		for ( int i = 0; i < level.m_gt_lab.length; i++ ) {
			//Jaccard for this GT label at this time point
			double acc = 0.0;

			if (level.m_gt_match[i] > -1)
			{
				//actually, we have a match,
				//update the Jaccard accordingly
				final int intersectSize
					= level.m_match[i + m_match_lineSize*level.m_gt_match[i]];

				acc  = (double)intersectSize;
				acc /= (double)level.m_gt_size[i]
				          + (double)level.m_res_size[level.m_gt_match[i]] - acc;
			}

			//update overall stats
			seg += acc;
			counter++;
		}
		
		return counter > 0 ? seg/counter : 0.0;
	}
}
