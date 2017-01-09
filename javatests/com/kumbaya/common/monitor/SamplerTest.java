package com.kumbaya.common.monitor;

import java.util.Date;
import java.util.List;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.kumbaya.common.monitor.Sampler.Clock;
import com.kumbaya.common.monitor.Sampler.Sample;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SamplerTest {
	private IMocksControl control = EasyMock.createControl();
	private Clock clock = control.createMock(Clock.class);
	
	@Before
	public void setUp() {
		control.reset();
	}
	
	@After
	public void tearDown() {
		control.verify();
	}
	
	
	@Test
	public void testInterpolation_justOneSample() {
		Sampler sampler = new Sampler(clock);
		
		expect(clock.now()).andReturn(new Date(0));
		
		control.replay();
		
		sampler.log("foo");
		
		List<Sample> result = sampler.get("foo");
		
		assertArrayEquals(Lists.newArrayList(
				Sample.of(new Date(0), 1),
				Sample.of(new Date(1000), 0)).toArray(), 
				result.toArray());
	}

	@Test
	public void testInterpolation_justTwoSamples() {
		Sampler sampler = new Sampler(clock);
		
		expect(clock.now()).andReturn(new Date(0001));
		expect(clock.now()).andReturn(new Date(3000));

		control.replay();
		
		sampler.log("foo");
		sampler.log("foo");
		
		List<Sample> result = sampler.get("foo");
		
		assertArrayEquals(Lists.newArrayList(
				Sample.of(new Date(0000), 1),
				Sample.of(new Date(1000), 0),
				Sample.of(new Date(2000), 0),
				Sample.of(new Date(3000), 1),
				Sample.of(new Date(4000), 0)).toArray(), 
				result.toArray());
	}

	@Test
	public void testInterpolation_justQuickEvents() {
		Sampler sampler = new Sampler(clock);
		
		expect(clock.now()).andReturn(new Date(1000));
		expect(clock.now()).andReturn(new Date(1001));

		control.replay();
		
		sampler.log("foo");
		sampler.log("foo");
		
		List<Sample> result = sampler.get("foo");
		
		assertArrayEquals(Lists.newArrayList(
				Sample.of(new Date(1000), 2),
				Sample.of(new Date(2000), 0)).toArray(), 
				result.toArray());
	}

	@Test
	public void testInterpolation_threeSamples() {
		Sampler sampler = new Sampler(clock);
		
		expect(clock.now()).andReturn(new Date(1000));
		expect(clock.now()).andReturn(new Date(5000));
		expect(clock.now()).andReturn(new Date(9000));

		control.replay();
		
		sampler.log("foo");
		sampler.log("foo");
		sampler.log("foo");
		
		List<Sample> result = sampler.get("foo");
		
		assertArrayEquals(Lists.newArrayList(
				Sample.of(new Date(1000), 1),
				Sample.of(new Date(2000), 0),
				Sample.of(new Date(4000), 0),
				Sample.of(new Date(5000), 1),
				Sample.of(new Date(6000), 0),
				Sample.of(new Date(8000), 0),
				Sample.of(new Date(9000), 1),
				Sample.of(new Date(10000), 0)).toArray(), 
				result.toArray());
	}
}
