package de.csbdresden.carelabkitworkflow.ui;

public class SegStats
{

	private int[] stats = new int[ 101 ];

	private long totalCount = 0;

	private long maxCount = 0;

	private int current = 0;
	
	public SegStats()
	{
	}
	
	public void update(final double d) {
		current = ( int ) Math.round( 100 * d );
		stats[ current ] += 1;
		totalCount += 1;
		maxCount = stats[ current ] > maxCount ? stats[ current ] : maxCount;
	}

	public int getCurrent()
	{
		return current;
	}

	public long getTotalCount()
	{
		return totalCount;
	}

	public long getMaxCount()
	{
		return maxCount;
	}

	public double getStatsFor( int i )
	{
		return stats[i];
	}
}
