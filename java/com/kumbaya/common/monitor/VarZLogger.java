package com.kumbaya.common.monitor;

import com.google.inject.Inject;

public class VarZLogger {
	private final Sampler sampler;

	@Inject
	VarZLogger(Sampler sampler) {
		this.sampler = sampler;
	}
	
	public void log(String key) {
		sampler.log(key);
	}
}
