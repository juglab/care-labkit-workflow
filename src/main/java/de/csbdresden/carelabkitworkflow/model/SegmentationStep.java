package de.csbdresden.carelabkitworkflow.model;

import javax.swing.JTextPane;

import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;

public class SegmentationStep< T extends RealType< T > & NativeType< T >, I extends IntegerType< I > > extends AbstractWorkflowImgStep< T >
{
	private Img< T > image;

	private ImgLabeling< String, I > segmentation;

	private float threshold = 0.5f;

	private boolean useLabkit = false; // otherwise threshold

	private JTextPane infoTextPanel;

	@Override
	public Img< T > getImg()
	{
		return image;
	}

	public ImgLabeling< String, I > getSegmentation()
	{
		return segmentation;
	}

	public void setImage( final Img< T > image )
	{
		this.image = image;
	}

	public void setSegmentation( final ImgLabeling< String, I > seg )
	{
		this.segmentation = seg;
	}

	public float getThreshold()
	{
		return threshold;
	}

	public synchronized void setThreshold( final float threshold )
	{
		this.threshold = threshold;
		updateInfoText();
	}

	public synchronized void updateInfoText()
	{
		infoTextPanel.setText( java.text.MessageFormat.format( getCurrentInfoText(), new String[] { String.valueOf( Math.round( this.threshold * 1000 ) / 1000.0 ) } ) );
	}

	public boolean isUseLabkit()
	{
		return useLabkit;
	}

	public void setUseLabkit( final boolean useLabkit )
	{
		this.useLabkit = useLabkit;
	}

	public void setInfoTextPanel( final JTextPane infoTextPanel )
	{
		this.infoTextPanel = infoTextPanel;
	}
}
