package com.kumbaya.monitor;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Sampler {
	private static final long SAMPLE_RATE_MS = 1000;
	Clock clock;

	static class Clock {
		Date now() {
			return new Date();
		}
	}

	public static class Sample {
		Date date;
		int value;
		Sample(Date date, int value) {
			this.date = date;
			this.value = value;
		}
		
		public String toString() {
			return date.getTime() + ": " + value;
		}
		
		public Date date() {
			return date;
		}
		
		public int value() {
			return value;
		}
		
		static Sample of(Date date, int value) {
			return new Sample(date, value);
		}
	}
	
	@Inject
	Sampler(Clock clock) {
		this.clock = clock;
	}
	
	Map<String, List<Sample>> samples = Maps.newHashMap();
	
	public synchronized Map<String, List<Sample>> samples() {
		return samples;
	}
	
	public synchronized void qps(String key) {
		Date now = clock.now();
		long thisSecond = now.getTime() / 1000;
		if (!samples.containsKey(key)) {
			samples.put(key, Lists.<Sample>newArrayList());
		}
		List<Sample> values = samples.get(key);
		if (values.isEmpty()) {
			// If this is the first value, log it.
			values.add(Sample.of(new Date(thisSecond * 1000), 1));
		} else {
			Sample last = values.get(values.size() - 1);
			long lastSecond = last.date.getTime() / 1000;
			if (lastSecond >= thisSecond) {
				last.value++;
			} else {
				// If enough time has passed (i.e. 1 second), log it.
				values.add(Sample.of(new Date(thisSecond * 1000), 1));
			}
		}
	}
	
	public synchronized void sample(String key, int value) {
		Date now = clock.now();
		if (!samples.containsKey(key)) {
			samples.put(key, Lists.<Sample>newArrayList());
		}
		List<Sample> values = samples.get(key);
		if (values.isEmpty()) {
			// If this is the first value, log it.
			values.add(Sample.of(now, value));
		} else {
			Sample last = values.get(values.size() - 1);
			if ((now.getTime() - last.date.getTime()) > SAMPLE_RATE_MS) {
				// If enough time has passed, log it.
				values.add(Sample.of(now, value));
			}
		}
	}
	
	public synchronized List<Sample> get(String key) {
		return samples.get(key);
	}
}
