package de.csbdresden.carelabkitworkflow.util;

import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.Arrays;

public class DampedValue< T extends NumericType< T > & RealType< T >> {

	T value;
	double damp_dt = 1;
	double dampB = 1;

	public T get() {
		return value;
	}

	public T set(T value) {
		if(this.value == null) this.value = value.copy();
		State s = new State();
		T newVal = value.copy();
		newVal.sub(this.value);
		s.v = newVal.getRealDouble();
		s.x = 0;
		this.value.setReal(this.value.getRealDouble() + Damper.integrate(s, 1, damp_dt, dampB).v);
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
		DampedValue<FloatType> dampedValue = new DampedValue();
		for (int i = 0; i < values.length; i++) {
			damped[i] = dampedValue.set(values[i]).copy();
		}
		System.out.println(Arrays.toString(damped));
	}
}
