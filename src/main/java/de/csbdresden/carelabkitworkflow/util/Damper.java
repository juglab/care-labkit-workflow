package de.csbdresden.carelabkitworkflow.util;

/// 4th order Runge-Kutta integrator

import java.util.Arrays;

class State {
	double x;          // position
	double v;          // velocity
};

class Derivative {
	double dx;          // derivative of position: velocity
	double dv;          // derivative of velocity: acceleration
};


public class Damper {

	static double acceleration(State state, double k, double b) {
		return -k * state.x - b * state.v;
	}

	static Derivative evaluate(State initial, double dt, Derivative d, double dampK, double dampB) {

		State state = new State();
		state.x = initial.x + d.dx * dt;
		state.v = initial.v + d.dv * dt;

		Derivative output = new Derivative();
		output.dx = state.v;
		output.dv = acceleration(state, dampK, dampB);
		return output;

	}

	static State integrate(State inState, double dt, double dampK, double dampB) {

		Derivative start = new Derivative();
		start.dv = 0;
		start.dx = 0;
		Derivative a = evaluate(inState, 0.0, start, dampK, dampB);
		Derivative b = evaluate(inState, dt * 0.5, a, dampK, dampB);
		Derivative c = evaluate(inState, dt * 0.5, b, dampK, dampB);
		Derivative d = evaluate(inState, dt, c, dampK, dampB);

		double dxdt = 1.0 / 6.0 * (a.dx + 2.0 * (b.dx + c.dx) + d.dx);
		double dvdt = 1.0 / 6.0 * (a.dv + 2.0 * (b.dv + c.dv) + d.dv);

		State outState = new State();
		outState.x = inState.x + dxdt * dt;
		outState.v = inState.v + dvdt * dt;

		return outState;

	}

	public static void main(final String... args) {

		int[] values = new int[] {2,2,2,2,2,2,2,2,2,2,9,2,-4,-3,4,1,2,2,2,2,2,3,6,7,9,14,2,15,20};
		System.out.println(Arrays.toString(values));
		double damp_dt = 1;
		double dampB = 1;
		double lastVal = values[0];
		double[] damped = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			State s = new State();
	        s.v = values[i]-lastVal;
	        s.x = 0;
			damped[i] = lastVal + Damper.integrate(s, 1, damp_dt, dampB).v;
			lastVal = damped[i];
		}
		System.out.println(Arrays.toString(damped));
	}

}
