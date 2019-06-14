package de.csbdresden.carelabkitworkflow.model;

import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;

public class ForwardingRandomAccessibleInterval< T > implements RandomAccessibleInterval< T >
{

	private RandomAccessibleInterval< T > source;

	public ForwardingRandomAccessibleInterval( final RandomAccessibleInterval< T > source )
	{
		super();
		this.source = source;
	}

	public void setSource( final RandomAccessibleInterval< T > source )
	{
		this.source = source;
	}

	@Override
	public RandomAccess< T > randomAccess()
	{
		return source.randomAccess();
	}

	@Override
	public RandomAccess< T > randomAccess( final Interval interval )
	{
		return source.randomAccess( interval );
	}

	@Override
	public int numDimensions()
	{
		return source.numDimensions();
	}

	@Override
	public long min( int d )
	{
		return source.min( d );
	}

	@Override
	public void min( long[] min )
	{
		source.min( min );
	}

	@Override
	public void min( Positionable min )
	{
		source.min( min );
	}

	@Override
	public long max( int d )
	{
		return source.max( d );
	}

	@Override
	public void max( long[] max )
	{
		source.max( max );
	}

	@Override
	public void max( Positionable max )
	{
		source.max( max );
	}

	@Override
	public void dimensions( long[] dimensions )
	{
		source.dimensions( dimensions );
	}

	@Override
	public long dimension( int d )
	{
		return source.dimension( d );
	}

	@Override
	public double realMin( int d )
	{
		return source.realMin( d );
	}

	@Override
	public void realMin( double[] min )
	{
		source.realMin( min );
	}

	@Override
	public void realMin( RealPositionable min )
	{
		source.realMin( min );
	}

	@Override
	public double realMax( int d )
	{
		return source.realMax( d );
	}

	@Override
	public void realMax( double[] max )
	{
		source.realMax( max );
	}

	@Override
	public void realMax( RealPositionable max )
	{
		source.realMax( max );
	}
}
