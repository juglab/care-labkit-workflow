package de.csbdresden.carelabkitworkflow.backend;

import bdv.cache.CacheControl;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.render.VolatileProjector;
import bdv.viewer.state.ViewerState;
import de.csbdresden.carelabkitworkflow.util.AccumulateProjectorAlphaBlendingARGB;
import io.scif.services.DatasetIOService;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.view.IntervalView;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class ServerCommunication {

	@Parameter
	IOService ioService;

	@Parameter
	OpService opService;

	@Parameter
	DatasetIOService datasetIOService;

	@Parameter
	DatasetService datasetService;

	@Parameter
	UIService ui;

	@Parameter
	LogService log;

	public static String LABKITLABELINGFILE = "/home/random/Development/imagej/project/outreach/labkit-remote/public/img/labeling.png";
	public static String LABKITINPUTFILE = "/home/random/Development/imagej/project/outreach/labkit-remote/public/img/segmentationinput.png";

	final AccumulateProjectorFactory<ARGBType> myFactory = new AccumulateProjectorFactory<ARGBType>() {

		@Override
		public synchronized AccumulateProjectorAlphaBlendingARGB createAccumulateProjector(
				final ArrayList<VolatileProjector> sourceProjectors, final ArrayList<Source<?>> sources,
				final ArrayList<? extends RandomAccessible<? extends ARGBType>> sourceScreenImages,
				final RandomAccessibleInterval<ARGBType> targetScreenImages, final int numThreads,
				final ExecutorService executorService) {

			return new AccumulateProjectorAlphaBlendingARGB(sourceProjectors, sourceScreenImages, targetScreenImages,
					numThreads, executorService);
		}

	};

	public <T extends NativeType<T> & RealType<T>> void uploadLabkitInputToServer(Img<T> img, float lowerPercentile, float upperPercentile) {

		BdvHandlePanel bdv = new BdvHandlePanel(null, Bdv.options().is2D().preferredSize((int) img.dimension(0), (int) img.dimension(1)).accumulateProjectorFactory(myFactory));
		BdvStackSource<T> source = BdvFunctions.show(img, "", Bdv.options().addTo(bdv));
		source.setDisplayRange(lowerPercentile, upperPercentile);
		bdv.getViewerPanel().updateUI();
		ViewerPanel viewer = bdv.getViewerPanel();
		final ViewerState renderState = viewer.getState();

		class MyTarget implements RenderTarget {
			BufferedImage bi;

			@Override
			public BufferedImage setBufferedImage(final BufferedImage bufferedImage) {
				bi = bufferedImage;
				return null;
			}

			@Override
			public int getWidth() {
				return (int) img.dimension(0);
			}

			@Override
			public int getHeight() {
				return (int) img.dimension(1);
			}
		}
		final MyTarget target = new MyTarget();
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
				target, new PainterThread(null), new double[]{1}, 0, false, 1, null, false,
				viewer.getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy());
		renderState.setCurrentTimepoint(0);
		renderer.requestRepaint();
		renderer.paint(renderState);
		try {
			ImageIO.write(target.bi, "png", new File(LABKITINPUTFILE));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("saved to " + LABKITINPUTFILE);
	}

	public String labelingPNG2TIF() {
		String path = "labeling.tif";
		String pathIn = LABKITLABELINGFILE;
		Img in = null;
		try {
			in = (Img) ioService.open(pathIn);

		} catch (IOException e) {
//			e.printStackTrace();
			System.out.println("Cannot read " + LABKITLABELINGFILE + ", maybe it is currently being written, trying again.");
			return labelingPNG2TIF();
		}
		Img int16 = opService.convert().int16(in.copy());
		IntervalView blueChannel = opService.transform().hyperSliceView(int16, 2, 2);
		IntervalView redChannel = opService.transform().hyperSliceView(int16, 2, 0);
		ImgFactory<ShortType> factory = new ArrayImgFactory(new ShortType());
		Img<ShortType> output = factory.create(int16.dimension(0), int16.dimension(1));
		Localizable cursor = blueChannel.cursor();
		RandomAccess<ShortType> raInRed = redChannel.randomAccess();
		RandomAccess<ShortType> raInBlue = blueChannel.randomAccess();
		RandomAccess<ShortType> raOut = output.randomAccess();
		while (((Cursor) cursor).hasNext()) {
			((Cursor) cursor).next();
			raInBlue.setPosition(cursor);
			raInRed.setPosition(cursor);
			raOut.setPosition(cursor.getIntPosition(0), 0);
			raOut.setPosition(cursor.getIntPosition(1), 1);
			ShortType border = new ShortType();
			border.setReal(125);
			if (raInBlue.get().compareTo(border) > 0) {
				raOut.get().setReal(1);
			} else if (raInRed.get().compareTo(border) > 0) {
				raOut.get().setReal(2);
			} else {
				raOut.get().setZero();
			}
		}
		try {
			datasetIOService.save(datasetService.create(output), path);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		ImagePlus wrappedImage = ImgToVirtualStack.wrap(ImgPlus.wrap(output));
//		FileSaver fs = new FileSaver( wrappedImage );
//		boolean ok = fs.saveAsTiff( path );
		System.out.println("saved " + path);
		return path;
	}

}
