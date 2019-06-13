package de.csbdresden.carelabkitworkflow.util;

import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AveragedValue< T extends NumericType< T > & RealType< T >> {

	T value;
	static int HISTORY_LENGTH = 20;
	List<T> history = new ArrayList<>();
	int i = 0;

	public T get() {
		return value;
	}

	public T set(T value) {
		history.add(value);
		if(history.size() > HISTORY_LENGTH) history.remove(0);
		T sum = history.get(0).copy();
		sum.setZero();
		history.forEach(val -> sum.add(val));
		T div = sum.copy();
		div.setReal(history.size());
		sum.div(div);
		this.value = sum;
		return this.value;
	}

	public static void main(final String... args) {

		FloatType[] values = new FloatType[] {new FloatType(2),new FloatType(2),new FloatType(2),new FloatType(2),
				new FloatType(2),new FloatType(2),new FloatType(2),new FloatType(2),new FloatType(2),
				new FloatType(2),new FloatType(9),new FloatType(2),new FloatType(-4),new FloatType(-3),
				new FloatType(4),new FloatType(1),new FloatType(2),new FloatType(2),new FloatType(2),
				new FloatType(2),new FloatType(2),new FloatType(3),new FloatType(6),new FloatType(7),
				new FloatType(9),new FloatType(14),new FloatType(2),new FloatType(15),new FloatType(20)};
		System.out.println(Arrays.toString(values));
		FloatType[] damped = new FloatType[values.length];
		AveragedValue<FloatType> averagedValue = new AveragedValue();
		for (int i = 0; i < values.length; i++) {
			damped[i] = averagedValue.set(values[i]).copy();
		}
		System.out.println(Arrays.toString(damped));
	}
}
