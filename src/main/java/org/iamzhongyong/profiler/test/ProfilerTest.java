package org.iamzhongyong.profiler.test;

import org.iamzhongyong.profiler.Profiler;
import org.iamzhongyong.profiler.ProfilerSwitch;

public class ProfilerTest {
	public static void main(String[] args) {
		ProfilerSwitch.getInstance().setOpenProfilerNanoTime(true);
		ProfilerTest t = new ProfilerTest();
		t.rootMethod();
	}

	public void rootMethod(){
		Profiler.start("rootMethod");
		firstMethod();
		Profiler.release();
		System.out.println(Profiler.dump());
		Profiler.reset();

	}
	public void firstMethod(){
		Profiler.enter("first");
		secondMethod();
		Profiler.release();

	}
	public void secondMethod(){
		Profiler.enter("second");
		Profiler.release();
	}
}
