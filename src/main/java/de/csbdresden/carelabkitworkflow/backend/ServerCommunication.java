package de.csbdresden.carelabkitworkflow.backend;

import ij.IJ;
import ij.ImagePlus;
import io.scif.services.DatasetIOService;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.view.IntervalView;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import java.io.IOException;

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

	public static String LABKITLABELINGFILE = "/home/random/Development/imagej/project/outreach/labkit-remote/public/img/labeling.png";
	public static String LABKITINPUTFILE = "/home/random/Development/imagej/project/outreach/labkit-remote/public/img/segmentationinput.png";

	public <T extends NativeType< T > & RealType< T >> void uploadLabkitInputToServer(Img<T> img) {
		ImgFactory<ARGBType> factory = new ArrayImgFactory(new ARGBType());
		Img<ARGBType> output = factory.create(img);
		Localizable cursor = img.cursor();
		RandomAccess<T> raIn = img.randomAccess();
		RandomAccess<ARGBType> raOut = output.randomAccess();
		while (((Cursor) cursor).hasNext()) {
			((Cursor) cursor).next();
			raIn.setPosition(cursor);
			raOut.setPosition(cursor);
			int val = (int) (raIn.get().getRealFloat() * 255);
			raOut.get().set(ARGBType.rgba(val, val, val, 255));
//			System.out.println(raOut.get());
		}
		ImagePlus wrappedImage = ImgToVirtualStack.wrap(ImgPlus.wrap(output));
		IJ.run( wrappedImage, "PNG...", "save=" + LABKITINPUTFILE );
		System.out.println("saved " + LABKITINPUTFILE);
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
			if(raInBlue.get().compareTo(border) > 0) {
				raOut.get().setReal(1);
			}
			else if(raInRed.get().compareTo(border) > 0) {
				raOut.get().setReal(2);
			}
			else {
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
