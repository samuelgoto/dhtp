package com.kumbaya.common.monitor;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Sampler {
	public static final long SAMPLE_RATE_MS = 1000;
	private Map<String, List<Sample>> qps = Maps.newHashMap();
	private final Clock clock;

	static class Clock {
		Date now() {
			return new Date();
		}
	}

	@Inject
	Sampler(Clock clock) {
		this.clock = clock;
	}

	public synchronized Set<String> keys() {
		return qps.keySet();
	}

	@VisibleForTesting
	private List<Sample> interpolate(List<Sample> samples) {
		List<Sample> result = Lists.newArrayList();
		
		long now = clock.now().getTime();
		if (samples.get(samples.size() - 1).date().compareTo(new Date(now)) < 0) {
			samples.add(Sample.of(new Date(now), 0));
		}
		
		for (int i = 0; i < samples.size(); i++) {
			result.add(samples.get(i));

			if (i < (samples.size() - 1)) {
				long current = samples.get(i).date().getTime() + SAMPLE_RATE_MS;
				long next = samples.get(i + 1).date().getTime();
				while (new Date(current).compareTo(new Date(next)) < 0) {
					// In the absence of data, we interpolate with 0s.
					result.add(new Sample(new Date(current), 0));
					current += SAMPLE_RATE_MS;
				}
			}
		}
		return result;
	}

	public synchronized void log(String key) {
		Date now = clock.now();
		long thisSecond = now.getTime() / 1000;
		if (!qps.containsKey(key)) {
			qps.put(key, Lists.<Sample>newArrayList());
		}
		List<Sample> values = qps.get(key);

		if (values.isEmpty()) {
			// If this is the first value, log it.
			values.add(Sample.of(new Date(thisSecond * 1000), 1));

			// Also, create an empty slot for the next second.
			values.add(Sample.of(new Date((thisSecond + 1) * 1000), 0));	
		} else {
			// Because one zeroed entry is always added at every entry, the
			// last second is 2 positions before.
			Sample last = values.get(values.size() - 2);
			long lastSecond = last.date.getTime() / 1000;
			if (lastSecond >= thisSecond) {
				last.value++;
			} else {
				// Adds zeroes around the new sample if needed.
				
				// If there was at least one slot between the last second and this
				// second, add an empty zeroed slot right before this second.
				if ((thisSecond - lastSecond) >= 2) {
					values.add(Sample.of(new Date((thisSecond - 1) * 1000), 0));
				}

				// If enough time has passed (i.e. 1 second), log it.
				values.add(Sample.of(new Date(thisSecond * 1000), 1));

				// Also, create an empty slot for the next second.
				values.add(Sample.of(new Date((thisSecond + 1) * 1000), 0));	
			}
		}
	}

	public synchronized List<Sample> get(String key) {
		if (qps.containsKey(key)) {
			return qps.get(key);
		} else {
			return ImmutableList.<Sample>of();
		}
	}

	@VisibleForTesting
	private synchronized List<Sample> interpolate(String key) {
		return interpolate(qps.get(key));
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

		@Override
		public boolean equals(Object b) {
			if (!(b instanceof Sample)) {
				return false;
			}
			Sample to = (Sample) b;
			return Objects.equal(this.date, to.date()) &&
					Objects.equal(this.value, to.value());
		}
	}
}
